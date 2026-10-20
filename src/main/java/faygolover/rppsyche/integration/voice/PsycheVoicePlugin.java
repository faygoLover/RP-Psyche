package faygolover.rppsyche.integration.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import faygolover.rppsyche.RpPsyche;

/**
 * Плагин Simple Voice Chat: только отмечает, кто и когда говорит (и шёпотом ли). Кто его слышит и что это значит для
 * психики — решает сервер ({@link faygolover.rppsyche.server.PsycheService}). Загружается самим Simple Voice Chat.
 */
@ForgeVoicechatPlugin
public class PsycheVoicePlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return RpPsyche.MODID;
    }

    @Override
    public void registerEvents(EventRegistration reg) {
        reg.registerEvent(VoicechatServerStartedEvent.class, e -> VoiceTracker.setDistance(e.getVoicechat().getVoiceChatDistance()));
        reg.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent e) {
        VoicechatConnection sender = e.getSenderConnection();
        if (sender == null || e.isCancelled()) return;
        VoiceTracker.onPacket(sender.getPlayer().getUuid(), e.getPacket().isWhispering());
    }
}
