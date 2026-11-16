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
import faygolover.rppsyche.integration.BodyCompat;
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
    /** Когда бывшего зависимого последний раз тянуло (игровое время). */
    private static final java.util.Map<UUID, Long> FORMER_CRAVE = new java.util.HashMap<>();
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

    /** Разовый удар по рассудку от других модов (RP Core, {@code MindProvider}) — войдёт на ближайшем шаге. */
    public static void sanityHit(ServerPlayer p, double amount) {
        if (amount <= 0 || !affected(p)) return;
        PsycheData d = PsycheData.get(p);
        if (d != null) d.sanityHit += amount;
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

    /**
     * Прокрутка времени RP ({@code /rptime skip|sleep}, ускоренная ночь RP Time): прошедшее время — спокойное. Стресс
     * спадает, рассудок отходит, часы психики идут (курильщик потом захочет курить). Источники стресса за прокрутку не
     * копятся — после неё всё считается вживую. Пауза ГМа — ничего.
     */
    public static void onTimeSkip(faygolover.rpcore.api.event.RpTimeSkipEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || e.scope() != faygolover.rpcore.api.event.RpTimeSkipEvent.Scope.ALL) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null || paused(d) || !affected(sp)) return;
        Settings c = Settings.get();
        // Шаг считается «как при 3-часовых сутках»; секунды события — по длине суток без /time speed.
        double left = Math.min(e.seconds() * 10800.0 / RpCoreAPI.dayLengthSeconds(), 7 * 86400.0);
        while (left > 0) {
            double dt = Math.min(60, left);
            Psyche.step(d.state, new Psyche.Inputs(), c, dt);
            left -= dt;
        }
        sync(sp, d, 0, false);
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
        BodyCompat.Body b = BodyCompat.read(sp);
        if (b.pain() > c.painThreshold) {
            in.stressRate += (b.pain() - c.painThreshold) / Math.max(1, 100 - c.painThreshold) * c.painRate;
        }
        // Группа поддержки: после встречи тяга и ломка давят слабее (сама физиология не меняется).
        boolean supported = supportStep(sp, d, c, b.craving() || b.withdrawal() || b.former() != null);
        double crave = supported ? c.supportCraveMult : 1;
        if (b.withdrawal()) in.stressRate += c.withdrawalRate * crave;
        if (b.sedation() > 0) {
            double sed = Math.min(100, b.sedation());
            in.gainMul *= Math.max(0, 1 - c.sedationGainCut * sed / 100);
            in.decayMul *= 1 + sed / 100;
        }
        if (b.intoxication() > 0) in.decayMul *= 1 + c.alcoholDecay * Math.min(100, b.intoxication()) / 100;
        // Антипсихотик: рассудок понемногу +, галлюцинации приглушены (флаг клиенту).
        boolean calmed = b.antipsychotic() >= c.antipsychoticSuppress;
        if (b.antipsychotic() > 0) d.sanityBoost += c.antipsychoticSanityPerHour / 3600.0 * Math.min(1, b.antipsychotic());
        // Тяга зависимого (любое вещество, по Medicine): до ломки и после острой фазы — понемногу (ломку не дублируем).
        if (b.craving() && !b.withdrawal()) in.stressRate += c.smokerCraveRate * crave;
        // Бывший зависимый под сильным стрессом — «сейчас бы выпить». Без штрафов, повод для отыгрыша.
        if (b.former() != null && s.stress >= c.formerCraveStress) {
            Long last = FORMER_CRAVE.get(id);
            long now = level.getGameTime();
            if (last == null || now - last >= c.formerCraveCooldown * 20) {
                FORMER_CRAVE.put(id, now);
                int v = sp.getRandom().nextInt(3);
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("rppsyche.crave_former." + b.former()
                                + (v == 0 ? "" : "." + v))
                        .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), true);
            }
        }
        // Время тела (RP Core): рассудок теряется и возвращается по RP-времени.
        in.sanityTime = RpCoreAPI.bodyRate();
        boolean clinical = b.down() == 3;
        if (d.wasClinical && !clinical) d.sanityHit += c.clinicalReturnSanity * in.sanityLossMul;
        d.wasClinical = clinical;

        // Голод и жажда (мод питания).
        NutritionProvider n = RpCoreAPI.nutrition();
        if (n != null) {
            if (n.hungerStage(sp) >= 2) in.stressRate += c.hungerRate;
            if (n.thirstStage(sp) >= 2) in.stressRate += c.thirstRate;
        }

        // Недосып (RP Needs) и долгий холод или жара.
        int sleepStage = RpCoreAPI.sleepStage(sp);
        if (sleepStage == 2) in.gainMul *= c.sleepStage2Gain;
        else if (sleepStage >= 3) in.gainMul *= c.sleepStage3Gain;
        BodyProvider tempBody = RpCoreAPI.body();
        if (tempBody != null && tempBody.temperatureOffset(sp) != 0) d.temperatureSeconds++;
        else d.temperatureSeconds = 0;
        if (d.temperatureSeconds > c.temperatureDelay) in.stressRate += c.temperatureRate;

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
            if (d2 <= compR2 && !BodyCompat.isDown(o)) company = true;
            if (d2 <= contR2) {
                // Луч — последним: нужен только соседу, который что-то меняет.
                PsycheData od = PsycheData.get(o);
                if (od != null) {
                    boolean panic = od.state.stress >= c.panicLevel, calm = !panic && od.state.stress <= c.calmLevel;
                    if (((panic && !panicNear) || (calm && !calmNear)) && sp.hasLineOfSight(o)) {
                        if (panic) panicNear = true;
                        else calmNear = true;
                    }
                }
            }
            if (iSpoke && d2 <= voiceR * voiceR && VoiceTracker.spokeWithin(o.getUUID(), twoWay)) talkTogether = true;
        }
        if (alone) s.aloneSeconds += 1;
        else s.aloneSeconds = Math.max(0, s.aloneSeconds - c.aloneForget);
        boolean alonePress = s.aloneSeconds > c.aloneDelay;
        // Одиночество, как и тьма, давит только до своей отметки; вместе — до общей.
        if (alonePress) {
            in.cappedRate += lerp(c.aloneRate, c.aloneRateMax, Psyche.ramp(s.aloneSeconds - c.aloneDelay, 0, c.aloneRamp));
            in.cappedLimit = darkPress ? c.darkAloneCap : c.aloneStressCap;
        }
        if (panicNear) in.stressRate += c.contagionRate;
        if (calmNear) in.gainMul *= c.calmNearbyGain;
        if (talkTogether) in.decayMul *= c.voiceCompanyDecay;
        else if (company) in.decayMul *= c.companyDecay;

        // Лежачие рядом (видно): нокдаун, обморок, клиническая смерть, тело вышедшего.
        double downed = 0;
        AABB box = sp.getBoundingBox().inflate(c.downedRadius);
        for (Entity e : level.getEntities(sp, box, x -> x instanceof LivingEntity && BodyCompat.isDown(x))) {
            if (!sp.hasLineOfSight(e)) continue;
            downed = Math.max(downed, BodyCompat.isClinical(e) ? c.clinicalRate : c.downedRate);
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
        if (voice) voiceStep(sp, d, c, in.sanityLossMul);

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

    /**
     * Фраза кончилась и её никто не слышал — в счёт; несколько подряд за окно — рассудок минус, не чаще отката.
     * «Слышал» — голос реально дошёл до живого игрока (рядом, в группе, на любой дальности Simple Voice Chat) или
     * говорит в рацию.
     */
    private static void voiceStep(ServerPlayer sp, PsycheData d, Settings c, double lossMul) {
        PsycheState s = d.state;
        UUID id = sp.getUUID();
        if (VoiceTracker.talking(id, c.voiceUtteranceGapMs)) {
            boolean heard = Radios.holds(sp, c);
            if (!heard) {
                for (UUID r : VoiceTracker.receivers(id, c.voiceUtteranceGapMs)) {
                    ServerPlayer o = sp.server.getPlayerList().getPlayer(r);
                    if (o != null && affected(o)) {
                        heard = true;
                        break;
                    }
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
            if (s.clock - s.selfTalkAt >= c.selfTalkCooldown) {
                d.sanityHit += c.selfTalkLoss * lossMul;
                s.selfTalkAt = s.clock;
            }
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
        // С RP Needs лечь — не значит уснуть: сон засчитывает он (RESTED по тому, сколько проспал).
        if (net.minecraftforge.fml.ModList.get().isLoaded("rpneeds")) return;
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
        if (RpCoreAPI.nutrition() != null) return;
        FoodProperties food = e.getItem().getFoodProperties(sp);
        if (food != null) ate(sp, food.getNutrition() * 50.0);
    }

    /** Выпивка и курево долей (RP Core: глоток, укус или целиком). */
    public static void onConsume(faygolover.rpcore.api.event.RpConsumeEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && e.fraction() > 0)
            substances(sp, faygolover.rpcore.api.Consumables.forItem(e.stack()), e.fraction());
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
            // Перекур: рядом недавно курил кто-то ещё — отпускает чуть сильнее.
            double company = smokedNearby(sp, c) ? c.smokeCompanyBonus : 1;
            d.relief += c.nicotineRelief * x.nicotine() * scale * Psyche.cooldownShare(s.clock, s.smokedAt, c.nicotineCooldown) * company;
            s.smokedAt = s.clock;
            d.smokeTick = sp.level().getGameTime();
        }
        if (x.alcohol() > 0) {
            d.relief += c.alcoholRelief * x.alcohol() * scale * Psyche.cooldownShare(s.clock, s.drankAt, c.alcoholCooldown);
            s.drankAt = s.clock;
        }
    }

    private static boolean smokedNearby(ServerPlayer sp, Settings c) {
        long now = sp.level().getGameTime(), window = (long) (c.smokeCompanyWindow * 20);
        double r2 = c.smokeCompanyRadius * c.smokeCompanyRadius;
        for (ServerPlayer o : sp.serverLevel().players()) {
            if (o == sp || !affected(o) || o.distanceToSqr(sp) > r2) continue;
            PsycheData od = PsycheData.get(o);
            if (od != null && od.smokeTick != Long.MIN_VALUE && now - od.smokeTick <= window) return true;
        }
        return false;
    }

    /**
     * Группа поддержки: зависимый (любое вещество) или бывший зависимый вместе с ещё одним таким и психологом с
     * журналом в руке — {@link Settings#supportSeconds} в сумме (разошлись и вернулись — время не пропадает), и встреча
     * засчитана на RP-сутки. Возвращает, действует ли она сейчас.
     */
    private static boolean supportStep(ServerPlayer sp, PsycheData d, Settings c, boolean dependent) {
        PsycheState s = d.state;
        long day = sp.level().getDayTime();
        boolean active = s.supportUntil != Long.MIN_VALUE && day < s.supportUntil && s.supportUntil - day <= 24000L;
        if (!dependent || active) {
            d.supportSeconds = 0;
            return active;
        }
        boolean psychologist = false, peer = false;
        double r2 = c.supportRadius * c.supportRadius;
        for (ServerPlayer o : sp.serverLevel().players()) {
            if (o == sp || !affected(o) || o.distanceToSqr(sp) > r2) continue;
            if (o.getMainHandItem().getItem() instanceof faygolover.rppsyche.item.PsychologistJournalItem
                    || o.getOffhandItem().getItem() instanceof faygolover.rppsyche.item.PsychologistJournalItem) psychologist = true;
            else {
                BodyCompat.Body ob = BodyCompat.read(o);
                if (ob.craving() || ob.withdrawal() || ob.former() != null) peer = true;
            }
        }
        if (!psychologist || !peer) return false;
        if (++d.supportSeconds < c.supportSeconds) return false;
        d.supportSeconds = 0;
        s.supportUntil = day + 24000L;
        BodyProvider body = RpCoreAPI.body();
        if (body == null || !body.sensation(sp, "rppsyche.sense.support"))
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("rppsyche.sense.support"), true);
        return true;
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
            // RP Needs: кошмар — стресс; выспался в нормальном месте — немного отпускает.
            case NIGHTMARE -> impulse(sp, c.nightmareStress * k);
            case RESTED -> {
                d.relief += c.restedRelief * k;
                d.sanityBoost += c.sleepSanity * k;
            }
        }
    }

    /** Своей рукой довёл другого игрока до нокдауна или клиники. Повтор за RP-сутки — слабее. */
    public static void onHarm(faygolover.rpcore.api.event.RpHarmEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !affected(sp)) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null || paused(d)) return;
        Settings c = Settings.get();
        PsycheState s = d.state;
        double k = RpCoreAPI.hasTrait(sp, RpIds.PACIFIST) ? c.harmPacifist : RpCoreAPI.hasTrait(sp, RpIds.SOLDIER) ? c.harmSoldier : 1;
        long now = sp.server.overworld().getDayTime();
        if (s.harmAt != Long.MIN_VALUE && now >= s.harmAt && now - s.harmAt < 24000L) k *= c.harmRepeat;
        s.harmAt = now;
        boolean lethal = e.stage() == faygolover.rpcore.api.event.RpHarmEvent.Stage.LETHAL;
        d.impulse += (lethal ? c.harmLethalStress : c.harmKnockdownStress) * k;
        if (lethal) d.sanityHit += c.harmLethalSanity * k;
    }

    /** Вайп персонажа: психика как у нового. */
    public static void onWipe(faygolover.rpcore.api.event.RpCharacterWipeEvent e) {
        PsycheData d = PsycheData.get(e.getEntity());
        if (d == null) return;
        d.state.copyFrom(new PsycheState());
        d.impulse = d.relief = d.sanityHit = d.sanityBoost = 0;
        d.temperatureSeconds = 0;
        d.wasClinical = false;
        resync(e.getEntity());
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

    /** Игрок вышел (зовёт RP Core через {@code PsycheData.STORE}): убрать служебное по игроку. */
    public static void forget(ServerPlayer sp) {
        VoiceTracker.forget(sp.getUUID());
        FORMER_CRAVE.remove(sp.getUUID());
        PsycheData d = PsycheData.get(sp);
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
