package faygolover.rppsyche.client;

import faygolover.rppsyche.core.Psyche;
import faygolover.rppsyche.network.SyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Своя психика на клиенте: то, что прислал сервер, и плавные значения для эффектов (переходы без скачков). Цифры
 * нигде не показываются.
 */
public final class ClientPsyche {
    private ClientPsyche() {}

    private static boolean known;
    /** Прислал сервер. */
    static float stress, sanity = 100;
    static int flags;
    /** Сила дрожи рук (настройка сервера × черты). */
    static float tremor = 0.5f;
    /** Плавно идут к присланным. */
    static float shownStress, shownSanity = 100;
    /** Вспышка прожилок от резкого скачка стресса (1 → 0). */
    static float spike;

    public static void onSync(float st, float sa, int f, float tr) {
        stress = st;
        tremor = tr;
        sanity = sa;
        boolean wasDark = (flags & SyncPacket.DARK) != 0, wasAlone = (flags & SyncPacket.ALONE) != 0;
        boolean wasPaused = (flags & SyncPacket.PAUSED) != 0;
        flags = f & ~SyncPacket.SPIKE;
        if (!wasPaused && (flags & SyncPacket.PAUSED) != 0) {
            PhantomSounds.stop();
            Thoughts.clear();
        }
        if (!known) {
            shownStress = st;
            shownSanity = sa;
            known = true;
        }
        if ((f & SyncPacket.SPIKE) != 0) spike = 1;
        if (!wasDark && (flags & SyncPacket.DARK) != 0) Thoughts.context(Thoughts.Group.DARK);
        if (!wasAlone && (flags & SyncPacket.ALONE) != 0) Thoughts.context(Thoughts.Group.ALONE);
    }

    public static void reset() {
        known = false;
        stress = shownStress = 0;
        sanity = shownSanity = 100;
        flags = 0;
        spike = 0;
        PhantomSounds.stop();
        Thoughts.clear();
        PsycheFx.close();
    }

    /** Психика сейчас действует на своего игрока (сервер прислал, не креатив и не наблюдатель). */
    public static boolean active() {
        LocalPlayer p = Minecraft.getInstance().player;
        return known && p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && (flags & SyncPacket.PAUSED) == 0;
    }

    public static float stress() {
        return active() ? shownStress : 0;
    }

    /** Рассудок для эффектов. Антипсихотик приглушает галлюцинации — как у здорового (стадия 0). */
    public static float sanity() {
        if (!active()) return 100;
        return (flags & SyncPacket.CALMED) != 0 ? Math.max(shownSanity, 85) : shownSanity;
    }

    public static float tremor() {
        return tremor;
    }

    public static int sanityStage() {
        return Psyche.sanityStage(sanity());
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        shownStress += (stress - shownStress) * 0.08f;
        shownSanity += (sanity - shownSanity) * 0.05f;
        if (spike > 0) spike = Math.max(0, spike - 0.05f);
        PhantomSounds.tick(mc);
        Thoughts.tick();
    }
}
