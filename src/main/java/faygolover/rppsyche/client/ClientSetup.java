package faygolover.rppsyche.client;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Клиент: экран, звуки, мысли, дрожь рук. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::overlays);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(ClientSetup::onClientTick);
        bus.addListener(ClientSetup::onRenderTick);
        bus.addListener(ClientSetup::onGuiPre);
        bus.addListener(ClientSetup::onLogout);
    }

    private static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "psyche_blood", PsycheFx::renderBlood);
        e.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "psyche_thought", Thoughts::render);
    }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) ClientPsyche.tick();
    }

    private static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) Tremor.apply();
    }

    private static void onGuiPre(RenderGuiEvent.Pre e) {
        PsycheFx.renderPost(e.getPartialTick());
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientPsyche.reset();
    }
}
