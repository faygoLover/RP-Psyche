package faygolover.rppsyche.server;

import faygolover.rpcore.api.BodyProvider;
import faygolover.rpcore.api.NutritionProvider;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
import faygolover.rpcore.api.event.FoodEatenEvent;
import faygolover.rppsyche.capability.PsycheData;
import faygolover.rppsyche.core.Psyche;
import faygolover.rppsyche.core.PsycheState;
import faygolover.rppsyche.core.Settings;
import faygolover.rppsyche.core.Stutter;
import faygolover.rppsyche.integration.MedicineCompat;
import faygolover.rppsyche.integration.ZaCompat;
import faygolover.rppsyche.integration.voice.VoiceTracker;
import faygolover.rppsyche.network.Net;
import faygolover.rppsyche.network.SyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Психика на сервере. Раз в секунду для каждого игрока собирает, что давит и что помогает ({@link Psyche.Inputs}),
 * делает шаг, шлёт своё состояние его клиенту и показывает ощущения в строке мода тела. События (выстрелы, взрывы,
 * удары, сон, еда) копят толчки и облегчение до ближайшего шага.
 */
public final class PsycheService {
    private PsycheService() {}

    private static boolean zoneArtifacts;
    private static boolean voice;
    private static boolean melodies;
    private static boolean tableTop;

    public static void init() {
        zoneArtifacts = ModList.get().isLoaded("fl_zone_arts");
        voice = ModList.get().isLoaded("voicechat");
        melodies = ModList.get().isLoaded("immersive_melodies");
        tableTop = ModList.get().isLoaded("table_top_craft");
    }

    /** Пауза ГМа идёт. */
    public static boolean paused(PsycheData d) {
        return d.pausedUntil != 0 && System.currentTimeMillis() < d.pausedUntil;
    }

    /** Психика действует: живой игрок, не креатив и не наблюдатель. */
    public static boolean affected(Player p) {
        return p instanceof ServerPlayer && p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    /** Разовый толчок стресса (выстрел, взрыв) — войдёт на ближайшем шаге. */
    public static void impulse(ServerPlayer p, double amount) {
        if (amount <= 0 || !affected(p)) return;
        PsycheData d = PsycheData.get(p);
        if (d != null && !paused(d)) d.impulse += amount;
    }

    // ---------------------------------------------------------------- шаг

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient() || !(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null) return;
        // Пауза ГМа: стресс и рассудок стоят, счётчики заморожены, эффекты у игрока выключены.
        if (d.pausedUntil != 0) {
            if (paused(d)) {
                d.impulse = d.relief = d.sanityHit = d.sanityBoost = 0;
                sync(sp, d, SyncPacket.PAUSED, false);
                return;
            }
            d.pausedUntil = 0;
            resync(sp);
        }
        if (!affected(sp) || !RpCoreAPI.timeRunning()) {
            d.impulse = d.relief = d.sanityHit = d.sanityBoost = 0;
            sync(sp, d, 0, false);
            return;
        }
        step(sp, d, Settings.get());
    }

    private static void step(ServerPlayer sp, PsycheData d, Settings c) {
        PsycheState s = d.state;
        Psyche.Inputs in = new Psyche.Inputs();
        ServerLevel level = sp.serverLevel();
        UUID id = sp.getUUID();

        boolean willed = RpCoreAPI.hasTrait(sp, RpIds.STRONG_WILLED);
        boolean suggestible = !willed && RpCoreAPI.hasTrait(sp, RpIds.SUGGESTIBLE);
        if (RpCoreAPI.hasTrait(sp, RpIds.COWARD)) in.gainMul *= c.cowardGain;
        else if (RpCoreAPI.hasTrait(sp, RpIds.BRAVE)) in.gainMul *= c.braveGain;
        in.sanityLossMul = willed ? c.willedLoss : suggestible ? c.suggestibleLoss : 1;

        // Тело (RP Medicine).
        MedicineCompat.Body b = MedicineCompat.read(sp);
        if (b.pain() > c.painThreshold) {
            in.stressRate += (b.pain() - c.painThreshold) / Math.max(1, 100 - c.painThreshold) * c.painRate;
        }
        if (b.withdrawal()) in.stressRate += c.withdrawalRate;
        if (b.sedation() > 0) {
            double sed = Math.min(100, b.sedation());
            in.gainMul *= Math.max(0, 1 - c.sedationGainCut * sed / 100);
            in.decayMul *= 1 + sed / 100;
        }
        if (b.intoxication() > 0) in.decayMul *= 1 + c.alcoholDecay * Math.min(100, b.intoxication()) / 100;
        // Антипсихотик: рассудок понемногу +, галлюцинации приглушены (флаг клиенту).
        boolean calmed = b.antipsychotic() >= c.antipsychoticSuppress;
        if (b.antipsychotic() > 0) d.sanityBoost += c.antipsychoticSanityPerHour / 3600.0 * Math.min(1, b.antipsychotic());
        // Курильщик без курева — не сразу (ломку Medicine не дублируем).
        if (RpCoreAPI.hasTrait(sp, RpIds.SMOKER)) {
            if (s.smokedAt < -1e8) s.smokedAt = s.clock;
            if (!b.withdrawal() && s.clock - s.smokedAt > c.smokerCraveDelay) in.stressRate += c.smokerCraveRate;
        }
        boolean clinical = b.down() == 3;
        if (d.wasClinical && !clinical) d.sanityHit += c.clinicalReturnSanity * in.sanityLossMul;
        d.wasClinical = clinical;

        // Голод и жажда (мод питания).
        NutritionProvider n = RpCoreAPI.nutrition();
        if (n != null) {
            if (n.hungerStage(sp) >= 2) in.stressRate += c.hungerRate;
            if (n.thirstStage(sp) >= 2) in.stressRate += c.thirstRate;
        }

        // Полная тьма — копится, давит не сразу.
        if (Lights.inTotalDarkness(sp, c)) s.darkSeconds += 1;
        else s.darkSeconds = 0;
        boolean darkPress = s.darkSeconds > c.darkDelay;
        if (darkPress) {
            in.cappedRate = lerp(c.darkRate, c.darkRateMax, Psyche.ramp(s.darkSeconds - c.darkDelay, 0, c.darkRamp));
            in.cappedLimit = c.darkStressCap;
        }

        // Люди вокруг: одиночество, компания, заразность, разговор.
        boolean alone = true, company = false, calmNear = false, panicNear = false, talkTogether = false;
        double aloneR2 = c.aloneRadius * c.aloneRadius, compR2 = c.companyRadius * c.companyRadius;
        double contR2 = c.contagionRadius * c.contagionRadius;
        double voiceR = c.voiceRadius > 0 ? c.voiceRadius : VoiceTracker.distance();
        long twoWay = (long) (c.voiceTwoWayWindow * 1000);
        boolean iSpoke = voice && VoiceTracker.spokeWithin(id, twoWay);
        for (ServerPlayer o : level.players()) {
            if (o == sp || !affected(o)) continue;
            double d2 = o.distanceToSqr(sp);
            if (d2 <= aloneR2) alone = false;
            if (d2 <= compR2 && !MedicineCompat.isDown(o)) company = true;
            if (d2 <= contR2 && sp.hasLineOfSight(o)) {
                PsycheData od = PsycheData.get(o);
                if (od != null) {
                    if (od.state.stress >= c.panicLevel) panicNear = true;
                    else if (od.state.stress <= c.calmLevel) calmNear = true;
                }
            }
            if (iSpoke && d2 <= voiceR * voiceR && VoiceTracker.spokeWithin(o.getUUID(), twoWay)) talkTogether = true;
        }
        if (alone) s.aloneSeconds += 1;
        else s.aloneSeconds = Math.max(0, s.aloneSeconds - c.aloneForget);
        boolean alonePress = s.aloneSeconds > c.aloneDelay;
        if (alonePress) in.stressRate += lerp(c.aloneRate, c.aloneRateMax, Psyche.ramp(s.aloneSeconds - c.aloneDelay, 0, c.aloneRamp));
        if (panicNear) in.stressRate += c.contagionRate;
        if (calmNear) in.gainMul *= c.calmNearbyGain;
        if (talkTogether) in.decayMul *= c.voiceCompanyDecay;
        else if (company) in.decayMul *= c.companyDecay;

        // Лежачие рядом (видно): нокдаун, обморок, клиническая смерть, тело вышедшего.
        double downed = 0;
        AABB box = sp.getBoundingBox().inflate(c.downedRadius);
        for (Entity e : level.getEntities(sp, box, x -> x instanceof LivingEntity && MedicineCompat.isDown(x))) {
            if (!sp.hasLineOfSight(e)) continue;
            downed = Math.max(downed, MedicineCompat.isClinical(e) ? c.clinicalRate : c.downedRate);
        }
        in.stressRate += downed;

        // Пси-зона.
        double psi = zoneArtifacts ? ZaCompat.psi(sp) : 0;
        if (psi > 0) {
            in.stressRate += c.psiStress * psi;
            double k = willed ? c.psiWilled : suggestible ? c.psiSuggestible : 1;
            in.sanityDrain += c.psiSanityPerMinute / 60.0 * psi * k;
        }

        // Голос: разговор с собой.
        if (voice) voiceStep(sp, d, c, voiceR, in.sanityLossMul);

        // Досуг: музыка, настольные игры, дневник.
        leisure(sp, d, c);
        diary(sp, d, c);

        in.impulse = d.impulse;
        in.relief = d.relief;
        in.sanityHit = d.sanityHit;
        in.sanityBoost = d.sanityBoost;
        d.impulse = d.relief = d.sanityHit = d.sanityBoost = 0;

        double before = s.stress;
        boolean peak = Psyche.step(s, in, c, 1.0);
        if (peak && Psyche.cooldownShare(s.clock, s.panicAt, c.panicPeakCooldown) >= 1) {
            s.sanity = Psyche.clamp(s.sanity - c.panicPeakLoss * in.sanityLossMul);
            s.panicAt = s.clock;
        }

        int flags = (darkPress ? SyncPacket.DARK : 0) | (alonePress ? SyncPacket.ALONE : 0) | (psi > 0 ? SyncPacket.PSI : 0)
                | (calmed ? SyncPacket.CALMED : 0);
        sync(sp, d, flags, s.stress - before >= 8);
        sensations(sp, d);
    }

    /** Фраза кончилась и её никто не слышал — в счёт; несколько подряд за окно — рассудок минус. */
    private static void voiceStep(ServerPlayer sp, PsycheData d, Settings c, double voiceR, double lossMul) {
        PsycheState s = d.state;
        UUID id = sp.getUUID();
        if (VoiceTracker.talking(id, c.voiceUtteranceGapMs)) {
            double r = voiceR * (VoiceTracker.whispering(id) ? c.voiceWhisperFactor : 1);
            boolean heard = false;
            for (ServerPlayer o : sp.serverLevel().players()) {
                if (o != sp && affected(o) && o.distanceToSqr(sp) <= r * r) {
                    heard = true;
                    break;
                }
            }
            d.inUtterance = true;
            d.utteranceHeard |= heard;
        } else if (d.inUtterance) {
            d.inUtterance = false;
            if (d.utteranceHeard) d.unheard.clear();
            else d.unheard.addLast(s.clock);
            d.utteranceHeard = false;
        }
        while (!d.unheard.isEmpty() && s.clock - d.unheard.peekFirst() > c.selfTalkWindow) d.unheard.pollFirst();
        if (d.unheard.size() >= Math.max(1, c.selfTalkCount)) {
            d.sanityHit += c.selfTalkLoss * lossMul;
            s.selfTalkAt = s.clock;
            d.unheard.clear();
        }
    }

    // ---------------------------------------------------------------- досуг

    /** Музыка и настолки: рассудок понемногу +, не больше дневного потолка за RP-сутки. */
    private static void leisure(ServerPlayer sp, PsycheData d, Settings c) {
        if (!melodies && !tableTop) return;
        PsycheState s = d.state;
        long day = Math.floorDiv(sp.level().getDayTime(), 24000L);
        if (day != s.leisureDay) {
            s.leisureDay = day;
            s.leisureToday = 0;
        }
        if (s.leisureToday >= c.leisureDailyCap) return;
        double gain = 0;
        if (melodies && hearsMusic(sp, c)) gain += c.musicSanityPerHour / 3600.0;
        if (tableTop) {
            if (((long) s.clock) % 5 == 0 && atBoardGame(sp, c)) d.gameUntil = s.clock + 5;
            if (s.clock < d.gameUntil) gain += c.gameSanityPerHour / 3600.0;
        }
        gain = Math.min(gain, c.leisureDailyCap - s.leisureToday);
        if (gain <= 0) return;
        d.sanityBoost += gain;
        s.leisureToday += gain;
    }

    /** Кто-то в радиусе (или сам) играет на инструменте Immersive Melodies (в NBT предмета {@code playing}). */
    private static boolean hearsMusic(ServerPlayer sp, Settings c) {
        double r2 = c.musicRadius * c.musicRadius;
        for (ServerPlayer o : sp.serverLevel().players()) {
            if (o.isSpectator() || o.distanceToSqr(sp) > r2) continue;
            if (playing(o.getMainHandItem()) || playing(o.getOffhandItem())) return true;
        }
        return false;
    }

    private static boolean playing(net.minecraft.world.item.ItemStack st) {
        if (st.isEmpty() || !st.hasTag()) return false;
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(st.getItem());
        return id != null && "immersive_melodies".equals(id.getNamespace()) && st.getTag().getBoolean("playing");
    }

    /** У доски Table Top Craft рядом стоит ещё кто-то живой — идёт партия. */
    private static boolean atBoardGame(ServerPlayer sp, Settings c) {
        ServerLevel level = sp.serverLevel();
        int r = (int) Math.ceil(c.gameRadius);
        net.minecraft.core.BlockPos at = sp.blockPosition();
        double r2 = c.gameRadius * c.gameRadius;
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(at.offset(-r, -1, -r), at.offset(r, 1, r))) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            net.minecraft.resources.ResourceLocation type = net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
            if (type == null || !"table_top_craft".equals(type.getNamespace())) continue;
            Vec3 centre = Vec3.atCenterOf(pos);
            for (ServerPlayer o : level.players()) {
                if (o != sp && affected(o) && o.distanceToSqr(centre) <= r2) return true;
            }
        }
        return false;
    }

    /** Дневник: дописал в книгах не меньше {@link Settings#diaryMinChars} знаков — рассудок +, раз в RP-сутки. */
    private static void diary(ServerPlayer sp, PsycheData d, Settings c) {
        PsycheState s = d.state;
        if (((long) s.clock) % 10 != 0) return;
        int chars = bookChars(sp);
        if (d.bookChars < 0 || chars < d.bookChars) {
            d.bookChars = chars;
            return;
        }
        if (chars - d.bookChars < c.diaryMinChars) return;
        long day = sp.level().getDayTime();
        if (s.diaryAt != Long.MIN_VALUE && day >= s.diaryAt && day - s.diaryAt < c.diaryCooldownTicks) return;
        d.sanityBoost += c.diarySanity;
        s.diaryAt = day;
        d.bookChars = chars;
    }

    /** Знаков в книгах с пером и подписанных книгах в инвентаре. */
    private static int bookChars(ServerPlayer sp) {
        int n = 0;
        for (net.minecraft.world.item.ItemStack st : sp.getInventory().items) {
            if (!(st.is(net.minecraft.world.item.Items.WRITABLE_BOOK) || st.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) || !st.hasTag())
                continue;
            net.minecraft.nbt.ListTag pages = st.getTag().getList("pages", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < pages.size(); i++) n += pages.getString(i).length();
        }
        return n;
    }

    // ---------------------------------------------------------------- клиент и ощущения

    public static void sync(ServerPlayer sp, PsycheData d, int flags, boolean spike) {
        PsycheState s = d.state;
        float st = (float) s.stress, sa = (float) s.sanity;
        if (spike) flags |= SyncPacket.SPIKE;
        d.syncAge++;
        float tr = tremor(sp, Settings.get());
        if (spike || flags != d.sentFlags || d.syncAge >= 10 || Math.abs(st - d.sentStress) >= 0.5f || Math.abs(sa - d.sentSanity) >= 0.2f
                || tr != d.sentTremor) {
            Net.send(sp, new SyncPacket(st, sa, flags, tr));
            d.sentTremor = tr;
            d.sentStress = st;
            d.sentSanity = sa;
            d.sentFlags = flags & ~SyncPacket.SPIKE;
            d.syncAge = 0;
        }
    }

    /** Сила дрожи рук: настройка × черты (Храбрый — меньше, Трусливый — больше). */
    private static float tremor(ServerPlayer sp, Settings c) {
        double k = c.tremorScale;
        if (RpCoreAPI.hasTrait(sp, RpIds.COWARD)) k *= c.tremorCoward;
        else if (RpCoreAPI.hasTrait(sp, RpIds.BRAVE)) k *= c.tremorBrave;
        return (float) Math.max(0, k);
    }

    /** Строка ощущений мода тела: держится там 8 секунд — обновляем каждые 4. */
    private static void sensations(ServerPlayer sp, PsycheData d) {
        if (--d.sensationTimer > 0) return;
        d.sensationTimer = 4;
        BodyProvider body = RpCoreAPI.body();
        if (body == null) return;
        PsycheState s = d.state;
        if (s.stress >= 55) body.sensation(sp, "rppsyche.sense.anxious");
        if (s.stress >= 65) body.sensation(sp, "rppsyche.sense.tremor");
        if (s.sanity <= 40) body.sensation(sp, "rppsyche.sense.someone");
        else if (s.sanity <= 60) body.sensation(sp, "rppsyche.sense.seeing");
    }

    // ---------------------------------------------------------------- речь

    /** Фильтр речи (RP Core → RP PDA): заикание от стресса. */
    public static String filterSpeech(ServerPlayer sp, String text, RpCoreAPI.SpeechChannel channel) {
        if (!affected(sp)) return text;
        PsycheData d = PsycheData.get(sp);
        if (d == null) return text;
        Settings c = Settings.get();
        if (d.state.stress < c.stutterFrom) return text;
        return Stutter.apply(text, Psyche.ramp(d.state.stress, c.stutterFrom, 100), ThreadLocalRandom.current());
    }

    // ---------------------------------------------------------------- события

    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || e.getAmount() <= 0) return;
        Settings c = Settings.get();
        impulse(sp, Math.min(c.hurtMax, e.getAmount() * c.hurtPerDamage));
    }

    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        Settings c = Settings.get();
        Vec3 at = e.getExplosion().getPosition();
        for (ServerPlayer p : level.players()) {
            double dist = Math.sqrt(p.distanceToSqr(at));
            if (dist <= c.explosionRadius) impulse(p, c.explosion * (1 - dist / c.explosionRadius));
        }
    }

    public static void onWake(PlayerWakeUpEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !affected(sp) || sp.getSleepTimer() < 100) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null) return;
        Settings c = Settings.get();
        PsycheState s = d.state;
        double share = Psyche.cooldownShare(s.clock, s.sleptAt, c.sleepCooldown);
        d.relief += c.sleepRelief * share;
        d.sanityBoost += c.sleepSanity * share;
        s.sleptAt = s.clock;
    }

    /** Еда от мода питания (в ней ккал). */
    public static void onFoodEaten(FoodEatenEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) ate(sp, e.kcal());
    }

    /** Выпивка и курево по общей базе RP Core; без мода питания — ещё и ванильная еда (1 ед. сытости ≈ 50 ккал). */
    public static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        substances(sp, faygolover.rpcore.api.Consumables.forItem(e.getItem()), 1);
        if (RpCoreAPI.nutrition() != null) return;
        FoodProperties food = e.getItem().getFoodProperties(sp);
        if (food != null) ate(sp, food.getNutrition() * 50.0);
    }

    /** Сигарета без «допивания» (ПКМ). */
    public static void onRightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer sp) substances(sp, faygolover.rpcore.api.Consumables.forClick(e.getItemStack().getItem()), 1);
    }

    /** Наложенный эффект (никотин Tobacconist): уровень эффекта — доза больше на 50 % за уровень. */
    public static void onEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        int amp = Math.max(0, e.getEffectInstance().getAmplifier());
        substances(sp, faygolover.rpcore.api.Consumables.forEffect(e.getEffectInstance().getEffect()), 1 + 0.5 * amp);
    }

    /** Никотин и алкоголь снимают часть стресса сразу (алкоголь — без отката потом); повтор раньше отката — доля. */
    private static void substances(ServerPlayer sp, @javax.annotation.Nullable faygolover.rpcore.api.Consumables.Entry x, double scale) {
        if (x == null || !affected(sp)) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null || paused(d)) return;
        Settings c = Settings.get();
        PsycheState s = d.state;
        if (x.nicotine() > 0) {
            d.relief += c.nicotineRelief * x.nicotine() * scale * Psyche.cooldownShare(s.clock, s.smokedAt, c.nicotineCooldown);
            s.smokedAt = s.clock;
        }
        if (x.alcohol() > 0) {
            d.relief += c.alcoholRelief * x.alcohol() * scale * Psyche.cooldownShare(s.clock, s.drankAt, c.alcoholCooldown);
            s.drankAt = s.clock;
        }
    }

    /** Погладить своё животное пустой рукой — рассудок немного +. */
    public static void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !sp.getMainHandItem().isEmpty() || !affected(sp)) return;
        Entity t = e.getTarget();
        boolean own = t instanceof net.minecraft.world.entity.TamableAnimal ta && ta.isTame() && sp.getUUID().equals(ta.getOwnerUUID())
                || t instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h && h.isTamed() && sp.getUUID().equals(h.getOwnerUUID());
        if (!own) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null || paused(d)) return;
        Settings c = Settings.get();
        PsycheState s = d.state;
        if (s.clock - s.pettedAt < c.petCooldown) return;
        s.pettedAt = s.clock;
        d.sanityBoost += c.petSanity;
    }

    /** События настроения от других модов (RP Core): КПК, еда. */
    public static void onMood(faygolover.rpcore.api.event.RpMoodEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !affected(sp)) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null || paused(d)) return;
        Settings c = Settings.get();
        PsycheState s = d.state;
        double k = Math.max(0, e.strength());
        switch (e.kind()) {
            case MESSAGE_SENT -> {
                if (s.clock - s.messagedAt < c.messageCooldown) return;
                s.messagedAt = s.clock;
                s.aloneSeconds = Math.max(0, s.aloneSeconds - c.messageAloneForget);
            }
            case FED_CONTENT -> d.relief += c.fedContentRelief * k;
            case FAVOURITE_FOOD -> d.relief += c.favouriteRelief * k;
            case DISLIKED_FOOD -> impulse(sp, c.dislikedStress * k);
        }
    }

    private static void ate(ServerPlayer sp, double kcal) {
        if (kcal <= 0 || !affected(sp)) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null) return;
        Settings c = Settings.get();
        PsycheState s = d.state;
        double share = Psyche.cooldownShare(s.clock, s.ateAt, c.foodCooldown);
        d.relief += Math.min(c.foodReliefMax, c.foodRelief * kcal / 100.0) * share;
        s.ateAt = s.clock;
    }

    public static void onClone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        PsycheData from = PsycheData.get(e.getOriginal());
        PsycheData to = PsycheData.get(e.getEntity());
        if (from != null && to != null) {
            to.state.copyFrom(from.state);
            to.wasClinical = from.wasClinical;
        }
        e.getOriginal().invalidateCaps();
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        resync(e.getEntity());
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        resync(e.getEntity());
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        resync(e.getEntity());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        VoiceTracker.forget(e.getEntity().getUUID());
        PsycheData d = PsycheData.get(e.getEntity());
        if (d != null) {
            d.pausedUntil = 0;
            d.bookChars = -1;
        }
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        VoiceTracker.clear();
        Therapy.clear();
    }

    /** Отправить клиенту заново при следующем шаге (вход, смена мира, команда). */
    public static void resync(Player p) {
        PsycheData d = PsycheData.get(p);
        if (d != null) {
            d.sentFlags = -1;
            d.sensationTimer = 0;
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
