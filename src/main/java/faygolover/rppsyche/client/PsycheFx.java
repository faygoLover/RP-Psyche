package faygolover.rppsyche.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.core.Psyche;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Что видно: мир выцветает и становится контрастнее (стресс, плохой рассудок), цвета расходятся к краям (с
 * Помешательства и на панике), края темнеют тоннелем (паника). Кровавые прожилки по краям — только у безумца (рассудок
 * 40 и ниже): пульсируют под стрессом и вспыхивают при резком скачке. Всё плавно.
 */
final class PsycheFx {
    private PsycheFx() {}

    private static final ResourceLocation BLOOD = new ResourceLocation(RpPsyche.MODID, "textures/overlay/blood_tendrils.png");

    private static PostPass in, out;
    private static RenderTarget swap;
    private static boolean failed;
    private static float time;

    static void renderPost(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (failed || mc.level == null || !ClientPsyche.active()) return;
        float stress = ClientPsyche.stress(), sanity = ClientPsyche.sanity();
        float desat = (float) Math.max(Psyche.ramp(stress, 60, 100) * 0.35, Psyche.ramp(60 - sanity, 0, 60) * 0.7);
        float chroma = (float) Math.max(Psyche.ramp(40 - sanity, 0, 40) * 0.1, Psyche.ramp(stress, 80, 100) * 0.04);
        float tunnel = (float) Psyche.ramp(stress, 75, 100) * 0.55f;
        if (desat < 0.005f && chroma < 0.001f && tunnel < 0.005f) return;
        RenderTarget main = mc.getMainRenderTarget();
        try {
            if (in == null) create(mc, main);
            if (swap.width != main.width || swap.height != main.height) resize(main);
            time += mc.getDeltaFrameTime();
            in.getEffect().safeGetUniform("Desaturate").set(desat);
            in.getEffect().safeGetUniform("Spread").set(desat * 2.05f);
            in.getEffect().safeGetUniform("Chroma").set(chroma);
            in.getEffect().safeGetUniform("Tunnel").set(tunnel);
            in.getEffect().safeGetUniform("PsyTime").set(time / 20f);
            in.process(partialTick);
            out.process(partialTick);
        } catch (Exception e) {
            RpPsyche.LOGGER.error("RP Psyche: шейдер психики не работает, выключен до перезапуска", e);
            failed = true;
            close();
        } finally {
            main.bindWrite(true);
            // Проход идёт уже в интерфейсе, после того как игра очистила глубину: blit пишет в неё 0.6 на весь
            // экран, и предметы в слотах, rphud — всё, что рисуется с проверкой глубины, — пропадали. Очищаем снова.
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void create(Minecraft mc, RenderTarget main) throws java.io.IOException {
        swap = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        swap.setClearColor(0, 0, 0, 1);
        in = new PostPass(mc.getResourceManager(), "rppsyche_psyche", main, swap);
        out = new PostPass(mc.getResourceManager(), "blit", swap, main);
        ortho(main);
    }

    private static void resize(RenderTarget main) {
        swap.resize(main.width, main.height, Minecraft.ON_OSX);
        ortho(main);
    }

    private static void ortho(RenderTarget main) {
        Matrix4f m = new Matrix4f().setOrtho(0, main.width, 0, main.height, 0.1f, 1000f);
        in.setOrthoMatrix(m);
        out.setOrthoMatrix(m);
    }

    static void close() {
        if (in != null) in.close();
        if (out != null) out.close();
        if (swap != null) swap.destroyBuffers();
        in = out = null;
        swap = null;
    }

    static void renderBlood(ForgeGui gui, GuiGraphics g, float partialTick, int sw, int sh) {
        if (!ClientPsyche.active()) return;
        float sanity = ClientPsyche.sanity(), stress = ClientPsyche.stress();
        float mad = (float) Psyche.ramp(40 - sanity, 0, 40);
        if (mad <= 0) return;
        double t = System.nanoTime() / 1.0e9;
        float pulse = (float) Psyche.ramp(stress, 40, 90) * (0.15f + 0.15f * (float) (0.5 - 0.5 * Math.cos(t * 2.0)));
        float alpha = Math.max(pulse * (0.5f + 0.5f * mad), ClientPsyche.spike * 0.7f * mad);
        if (alpha < 0.01f) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, Math.min(1, alpha));
        g.blit(BLOOD, 0, 0, sw, sh, 0, 0, 100, 58, 100, 58);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
