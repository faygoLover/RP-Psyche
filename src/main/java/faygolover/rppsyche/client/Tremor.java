package faygolover.rppsyche.client;

import faygolover.rppsyche.core.Psyche;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Дрожь рук от стресса: взгляд (и прицел) мелко дрожит, на панике ещё и медленно «уводит». Смещение колеблется вокруг
 * нуля — взгляд никуда не уползает, только мешает целиться. Контроль не отбирается.
 */
public final class Tremor {
    private Tremor() {}

    private static float appliedYaw, appliedPitch;

    static void apply() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused()) return;
        float stress = ClientPsyche.stress();
        double t = System.nanoTime() / 1.0e9;
        float shake = (float) Psyche.ramp(stress, 60, 100) * 0.9f;
        float drift = (float) Psyche.ramp(stress, 80, 100) * 1.6f;
        float yaw = 0, pitch = 0;
        if ((shake > 0 || drift > 0) && mc.screen == null && ClientPsyche.active()) {
            yaw = shake * (float) (Math.sin(t * 47.0) * 0.5 + Math.sin(t * 29.3 + 1.7) * 0.5)
                    + drift * (float) Math.sin(t * 0.9 + 0.4);
            pitch = shake * (float) (Math.sin(t * 41.0 + 0.6) * 0.5 + Math.sin(t * 23.1) * 0.5) * 0.8f
                    + drift * 0.6f * (float) Math.sin(t * 0.7 + 2.1);
        }
        float dy = yaw - appliedYaw, dp = pitch - appliedPitch;
        if (dy == 0 && dp == 0) return;
        p.setYRot(p.getYRot() + dy);
        p.setXRot(Math.max(-90, Math.min(90, p.getXRot() + dp)));
        p.yRotO += dy;
        p.xRotO += dp;
        appliedYaw = yaw;
        appliedPitch = pitch;
    }
}
