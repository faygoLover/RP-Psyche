package faygolover.rppsyche.integration.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import faygolover.rppsyche.RpPsyche;

/**
 * Плагин Simple Voice Chat: только отмечает, кто и когда говорит и кому его голос реально ушёл (рядом, в
 * группе; шёпот и дальность считает сам Simple Voice Chat). Что это значит для психики — решает сервер ({@link faygolover.rppsyche.server.PsycheService}). Загружается самим Simple Voice Chat.
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
        reg.registerEvent(EntitySoundPacketEvent.class, this::onSound);
        reg.registerEvent(LocationalSoundPacketEvent.class, this::onSound);
        reg.registerEvent(StaticSoundPacketEvent.class, this::onSound);
    }

    private void onMicrophone(MicrophonePacketEvent e) {
        VoicechatConnection sender = e.getSenderConnection();
        if (sender == null || e.isCancelled()) return;
        VoiceTracker.onPacket(sender.getPlayer().getUuid());
    }

    /** Пакет голоса уходит одному получателю. Наблюдателям — не в счёт; голоса плагинов (рации) без отправителя. */
    private void onSound(SoundPacketEvent<?> e) {
        VoicechatConnection sender = e.getSenderConnection(), receiver = e.getReceiverConnection();
        if (sender == null || receiver == null || e.isCancelled()) return;
        if (SoundPacketEvent.SOURCE_SPECTATOR.equals(e.getSource())) return;
        VoiceTracker.onDelivered(sender.getPlayer().getUuid(), receiver.getPlayer().getUuid());
    }
}
