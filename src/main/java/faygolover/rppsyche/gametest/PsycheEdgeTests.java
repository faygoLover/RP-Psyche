package faygolover.rppsyche.gametest;

import com.mojang.authlib.GameProfile;
import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.capability.PsycheData;
import faygolover.rppsyche.core.PsycheState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Края состояния психики: запись и чтение, смерть, возврат из Края, перезаход. Страховка перед переводом данных
 * на общий механизм RP Core (план {@code _family/refactor}, задача T0.3): эти тесты обязаны проходить и до, и после.
 */
@GameTestHolder(RpPsyche.MODID)
@PrefixGameTestTemplate(false)
public final class PsycheEdgeTests {
    private PsycheEdgeTests() {}

    static final String T = "platform";
    private static int counter;

    // ------------------------------------------------------------------ помощники

    static ServerPlayer player(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer p = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), "rpp-edge-" + (counter++)));
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(conn);
        server.getPlayerList().placeNewPlayer(conn, p);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        p.moveTo(pos.x, pos.y, pos.z, 0, 0);
        p.setOnGround(true);
        return p;
    }

    static void remove(GameTestHelper h, ServerPlayer p) {
        if (!p.isRemoved()) h.getLevel().getServer().getPlayerList().remove(p);
    }

    /** Каждое поле сохраняемого состояния — своим значением не по умолчанию. Новое поле попадёт сюда само. */
    static void fill(PsycheData d) {
        try {
            int i = 0;
            for (Field f : PsycheState.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                i++;
                if (f.getType() == double.class) f.setDouble(d.state, 1.25 + i);
                else if (f.getType() == long.class) f.setLong(d.state, 1000L + i);
                else if (f.getType() == int.class) f.setInt(d.state, 10 + i);
                else if (f.getType() == boolean.class) f.setBoolean(d.state, true);
                else throw new IllegalStateException("Тест не умеет заполнять поле " + f.getName() + " типа " + f.getType());
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        d.wasClinical = true;
    }

    /** Имена полей сохраняемого состояния, которые отличаются. */
    static List<String> differences(PsycheState a, PsycheState b) {
        List<String> out = new ArrayList<>();
        try {
            for (Field f : PsycheState.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!f.get(a).equals(f.get(b))) out.add(f.getName());
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    // ------------------------------------------------------------------ тесты

    /** Записал → прочитал: все поля сохраняемого состояния те же. Новое поле, забытое в save/load, уронит тест. */
    @GameTest(template = T, batch = "psyche_edge_roundtrip")
    public static void nbtRoundTrip(GameTestHelper h) {
        PsycheData a = new PsycheData();
        fill(a);
        PsycheData b = new PsycheData();
        b.load(a.save());
        List<String> diff = differences(a.state, b.state);
        if (!diff.isEmpty()) h.fail("После записи и чтения отличаются поля: " + diff);
        if (!b.wasClinical) h.fail("wasClinical не сохранился");
        h.succeed();
    }

    /** Старое сохранение без данных мода: читается без ошибок, состояние как у нового игрока. */
    @GameTest(template = T, batch = "psyche_edge_empty")
    public static void nbtEmptyLoad(GameTestHelper h) {
        PsycheData d = new PsycheData();
        d.load(new CompoundTag());
        List<String> diff = differences(new PsycheData().state, d.state);
        if (!diff.isEmpty()) h.fail("После чтения пустого тега поля не по умолчанию: " + diff);
        if (d.wasClinical) h.fail("wasClinical после пустого чтения");
        h.succeed();
    }

    /** Смерть: психика переживает её целиком (смерть не лечит). */
    @GameTest(template = T, batch = "psyche_edge_death")
    public static void deathKeepsState(GameTestHelper h) {
        respawnKeeps(h, false);
    }

    /** Возврат из Края (не смерть): сохраняется всё. */
    @GameTest(template = T, batch = "psyche_edge_end")
    public static void endReturnKeepsAll(GameTestHelper h) {
        respawnKeeps(h, true);
    }

    private static void respawnKeeps(GameTestHelper h, boolean keepEverything) {
        ServerPlayer p = player(h);
        PsycheData d = PsycheData.get(p);
        if (d == null) { h.fail("Нет данных психики у игрока"); return; }
        fill(d);
        CompoundTag before = d.save();
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, keepEverything);
        try {
            PsycheData nd = PsycheData.get(n);
            if (nd == null) { h.fail("Нет данных психики после возрождения"); return; }
            if (!before.equals(nd.save())) h.fail("Данные психики изменились: было " + before + ", стало " + nd.save());
        } finally {
            remove(h, n);
        }
        h.succeed();
    }

    /** Перезаход: данные игрока записаны и прочитаны целиком, ветка мода в сохранении та же. */
    @GameTest(template = T, batch = "psyche_edge_relog")
    public static void relogKeeps(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerPlayer q = player(h);
        try {
            PsycheData d = PsycheData.get(p);
            if (d == null) { h.fail("Нет данных психики у игрока"); return; }
            fill(d);
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            String key = PsycheData.ID.toString();
            if (!saved.getCompound("ForgeCaps").contains(key)) h.fail("В сохранении игрока нет ветки ForgeCaps." + key);
            q.load(saved);
            PsycheData qd = PsycheData.get(q);
            if (qd == null || !d.save().equals(qd.save())) h.fail("После перезахода данные психики изменились");
        } finally {
            remove(h, p);
            remove(h, q);
        }
        h.succeed();
    }

    /**
     * Связи через RP Core: другие моды давят на психику через {@code RpCoreAPI.stressImpulse} и {@code sanityHit}
     * (так делает RP Culinary), а тело психика читает через мод тела — у здорового игрока всё по нулям.
     */
    @GameTest(template = T, batch = "psyche_edge_core_links")
    public static void coreLinks(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            PsycheData d = PsycheData.get(p);
            if (d == null) { h.fail("У игрока нет данных психики"); return; }
            double impulse = d.impulse, hit = d.sanityHit;
            if (!faygolover.rpcore.api.RpCoreAPI.stressImpulse(p, 5)) h.fail("RP Core не видит мод психики");
            faygolover.rpcore.api.RpCoreAPI.sanityHit(p, 2);
            if (Math.abs(d.impulse - impulse - 5) > 1e-9) h.fail("Толчок стресса через RP Core не дошёл: " + (d.impulse - impulse));
            if (Math.abs(d.sanityHit - hit - 2) > 1e-9) h.fail("Удар по рассудку через RP Core не дошёл: " + (d.sanityHit - hit));
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            faygolover.rpcore.api.RpCoreAPI.stressImpulse(p, 5);
            faygolover.rpcore.api.RpCoreAPI.sanityHit(p, 2);
            if (Math.abs(d.impulse - impulse - 5) > 1e-9 || Math.abs(d.sanityHit - hit - 2) > 1e-9) h.fail("В креативе психика приняла удар");
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

            var body = faygolover.rppsyche.integration.BodyCompat.read(p);
            if (body.pain() != 0 || body.down() != 0 || body.withdrawal() || body.craving() || body.former() != null
                    || body.intoxication() != 0 || body.sedation() != 0 || body.antipsychotic() != 0)
                h.fail("У здорового игрока тело говорит психике не «всё спокойно»: " + body);
            if (faygolover.rppsyche.integration.BodyCompat.isDown(p) || faygolover.rppsyche.integration.BodyCompat.isClinical(p))
                h.fail("Здоровый игрок считается лежачим");
        } finally {
            remove(h, p);
        }
        h.succeed();
    }

    /**
     * Свет через RP Core: свой список светящих предметов виден общему вопросу «несёт свет», и свет, о котором
     * сказал другой мод (RP Lights), отменяет полную тьму без правки конфига психики.
     */
    @GameTest(template = T, batch = "psyche_edge_light")
    public static void lightThroughCore(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            if (faygolover.rpcore.api.RpCoreAPI.carriesLight(p)) h.fail("С пустыми руками игрок «несёт свет»");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));
            if (!faygolover.rpcore.api.RpCoreAPI.carriesLight(p)) h.fail("Факел в руке (список light_items) не виден через RP Core");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            // Чужой мод говорит, что игрок с палкой несёт свет: психика должна поверить.
            faygolover.rpcore.api.RpCoreAPI.registerLight(new net.minecraft.resources.ResourceLocation("rppsyche", "test_other_mod"),
                    e -> e.getMainHandItem().is(net.minecraft.world.item.Items.STICK));
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
            faygolover.rppsyche.core.Settings c = new faygolover.rppsyche.core.Settings();
            c.darkLight = 15;   // по свету вокруг — «тьма» всегда: проверяем только свет в руках
            if (faygolover.rppsyche.server.Lights.inTotalDarkness(p, c)) h.fail("Свет от другого мода не отменил полную тьму");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            if (!faygolover.rppsyche.server.Lights.inTotalDarkness(p, c)) h.fail("Без света в руках тьма не наступила");
        } finally {
            remove(h, p);
        }
        h.succeed();
    }

    /**
     * Правило тела «stress» (RP Boss через RP Core 0.17): толчки и удары по рассудку не принимаются, шаг не поднимает
     * стресс и не роняет рассудок. Без правила — как раньше.
     */
    @GameTest(template = T, batch = "psyche_edge_rules")
    public static void bodyRuleFreezesStress(GameTestHelper h) {
        ServerPlayer p = player(h);
        net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(RpPsyche.MODID, "test_rules");
        try {
            PsycheData d = PsycheData.get(p);
            if (d == null) { h.fail("У игрока нет данных психики"); return; }
            faygolover.rpcore.api.RpCoreAPI.registerBodyRules(id, new faygolover.rpcore.api.BodyRulesProvider() {
                @Override
                public boolean forbids(net.minecraft.world.entity.player.Player player, String rule) {
                    return player == p && rule.equals("stress");
                }
            });
            double impulse = d.impulse, hit = d.sanityHit;
            faygolover.rpcore.api.RpCoreAPI.stressImpulse(p, 30);
            faygolover.rpcore.api.RpCoreAPI.sanityHit(p, 10);
            if (d.impulse != impulse || d.sanityHit != hit) h.fail("С правилом толчок или удар приняты");
            d.state.stress = 20;
            d.state.sanity = 70;
            d.impulse = 50;
            d.sanityHit = 5;
            boolean running = faygolover.rpcore.api.RpCoreAPI.timeRunning();
            p.tickCount = 0;
            faygolover.rppsyche.server.PsycheService.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END, p));
            if (d.state.stress > 20 + 1e-9 || d.state.sanity < 70 - 1e-9)
                h.fail("С правилом стресс вырос или рассудок упал: " + d.state.stress + ", " + d.state.sanity);
            faygolover.rpcore.api.RpCoreAPI.registerBodyRules(id, null);
            faygolover.rpcore.api.RpCoreAPI.stressImpulse(p, 30);
            if (Math.abs(d.impulse - 30) > 1e-9) h.fail("Без правила толчок не принят: " + d.impulse);
            if (running) {
                p.tickCount = 0;
                faygolover.rppsyche.server.PsycheService.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                        net.minecraftforge.event.TickEvent.Phase.END, p));
                if (d.state.stress <= 20 - 1e-9 && d.state.pending <= 0) h.fail("Без правила толчок не дошёл: " + d.state.stress);
            }
        } finally {
            faygolover.rpcore.api.RpCoreAPI.registerBodyRules(id, null);
            remove(h, p);
        }
        h.succeed();
    }
}
