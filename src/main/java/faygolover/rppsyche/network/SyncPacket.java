package faygolover.rppsyche.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Своя психика клиенту: стресс, рассудок и что сейчас вокруг (флаги). Клиент по ним рисует и звучит; цифры игроку
 * не показываются.
 */
public record SyncPacket(float stress, float sanity, int flags) {
    /** Полная тьма уже давит; один слишком долго; в Пси-зоне; резкий скачок стресса. */
    public static final int DARK = 1, ALONE = 2, PSI = 4, SPIKE = 8;

    public static void encode(SyncPacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.stress);
        buf.writeFloat(p.sanity);
        buf.writeByte(p.flags);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        return new SyncPacket(buf.readFloat(), buf.readFloat(), buf.readUnsignedByte());
    }

    public static void handle(SyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> faygolover.rppsyche.client.ClientPsyche.onSync(p.stress, p.sanity, p.flags)));
        ctx.get().setPacketHandled(true);
    }
}
