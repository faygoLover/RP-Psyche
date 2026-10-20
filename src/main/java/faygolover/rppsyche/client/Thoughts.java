package faygolover.rppsyche.client;

import com.mojang.blaze3d.systems.RenderSystem;
import faygolover.rppsyche.core.Psyche;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Внутренний голос: мысли посреди экрана, только свои. Тревожные — от стресса, остальные — от стадии рассудка;
 * на поздних стадиях чаще, громче и дрожат. Плюс мысли к случаю: тьма давит, слишком долго один.
 */
final class Thoughts {
    private Thoughts() {}

    /** Группа фраз и сколько их в переводе ({@code rppsyche.thought.<группа>.<n>}). */
    enum Group {
        STRESS("stress", 6), PANIC("panic", 6), DARK("dark", 4), ALONE("alone", 4),
        S1("s1", 6), S2("s2", 6), S3("s3", 6), S4("s4", 6);

        final String key;
        final int count;

        Group(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    private static final RandomSource RND = RandomSource.create();
    private static final int SHOW = 90;

    private static Component current;
    private static boolean loud;
    private static int shown;
    private static int stressTimer = 600, sanityTimer = 2400, contextCooldown;
    private static Group last;
    private static int lastIndex = -1;

    static void tick() {
        if (current != null && ++shown >= SHOW) current = null;
        if (contextCooldown > 0) contextCooldown--;
        if (!ClientPsyche.active()) return;
        float stress = ClientPsyche.stress();
        int stage = ClientPsyche.sanityStage();

        if (stress >= 55 && --stressTimer <= 0) {
            boolean panic = stress >= 80;
            stressTimer = panic ? 500 + RND.nextInt(400) : 1200 + RND.nextInt(1200);
            show(panic ? Group.PANIC : Group.STRESS, panic);
        }
        if (stage >= 1 && --sanityTimer <= 0) {
            int base = switch (stage) {
                case 1 -> 4800;
                case 2 -> 3000;
                case 3 -> 1800;
                default -> 1000;
            };
            sanityTimer = base + RND.nextInt(base * 3 / 4);
            show(switch (stage) {
                case 1 -> Group.S1;
                case 2 -> Group.S2;
                case 3 -> Group.S3;
                default -> Group.S4;
            }, stage >= 3);
        }
    }

    /** Мысль к случаю (тьма, одиночество) — не каждый раз. */
    static void context(Group g) {
        if (contextCooldown > 0 || RND.nextFloat() > 0.6f || !ClientPsyche.active()) return;
        contextCooldown = 2400;
        show(g, false);
    }

    static void clear() {
        current = null;
    }

    private static void show(Group g, boolean isLoud) {
        if (current != null) return;
        int i = RND.nextInt(g.count);
        if (g == last && i == lastIndex) i = (i + 1) % g.count;
        last = g;
        lastIndex = i;
        current = Component.translatable("rppsyche.thought." + g.key + "." + i);
        loud = isLoud;
        shown = 0;
    }

    static void render(ForgeGui gui, GuiGraphics g, float partialTick, int sw, int sh) {
        if (current == null || Minecraft.getInstance().options.hideGui) return;
        float t = (shown + partialTick) / SHOW;
        float alpha = t < 0.15f ? t / 0.15f : t > 0.75f ? (1 - t) / 0.25f : 1;
        alpha = Math.max(0, Math.min(1, alpha));
        if (alpha < 0.05f) return;
        float sanity = ClientPsyche.sanity();
        float scale = loud ? 1.6f : 1.25f;
        int dx = 0, dy = 0;
        if (sanity <= 40) {
            dx = RND.nextInt(3) - 1;
            dy = RND.nextInt(3) - 1;
        }
        int color = loud ? 0xE8C4C4 : 0xD8D8D8;
        int a = Math.max(8, (int) (alpha * 230)) << 24;
        var font = gui.getFont();
        g.pose().pushPose();
        g.pose().translate(sw / 2.0 + dx, sh * 0.36 + dy, 0);
        g.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        g.drawString(font, current, -font.width(current) / 2, -font.lineHeight / 2, color | a, true);
        RenderSystem.disableBlend();
        g.pose().popPose();
        if (Psyche.sanityStage(sanity) >= 4 && loud && RND.nextInt(6) == 0) {
            // На Безумии мысль иногда двоится.
            g.pose().pushPose();
            g.pose().translate(sw / 2.0 + RND.nextInt(9) - 4, sh * 0.36 + RND.nextInt(5) - 2, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, current, -font.width(current) / 2, -font.lineHeight / 2, 0x9A6060 | (a >>> 1 & 0xFF000000), false);
            g.pose().popPose();
        }
    }
}
