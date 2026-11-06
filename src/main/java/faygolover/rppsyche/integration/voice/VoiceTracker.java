package faygolover.rppsyche.integration.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кто говорит в голосовой чат. Плагин Simple Voice Chat пишет сюда из своего потока (когда говорил и кому голос
 * реально ушёл), сервер раз в секунду читает. Без Simple Voice Chat здесь пусто — голоса просто нет.
 */
public final class VoiceTracker {
    private VoiceTracker() {}

    public static final class Talk {
        volatile long lastPacketMs;
        /** Когда (мс) в последний раз говорил вообще — для «двустороннего разговора». */
        volatile long lastSpokeMs;
        /** Кому и когда (мс) Simple Voice Chat отдал его голос: рядом, в группе — по-настоящему услышали. */
        final Map<UUID, Long> heardBy = new ConcurrentHashMap<>();
    }

    private static final Map<UUID, Talk> TALK = new ConcurrentHashMap<>();
    /** Радиус голоса из Simple Voice Chat (сервер), блоков. */
    private static volatile double distance = 48;

    public static void setDistance(double d) {
        if (d > 0) distance = d;
    }

    public static double distance() {
        return distance;
    }

    public static void onPacket(UUID id) {
        Talk t = TALK.computeIfAbsent(id, k -> new Talk());
        long now = System.currentTimeMillis();
        t.lastPacketMs = now;
        t.lastSpokeMs = now;
    }

    /** Голос {@code sender} ушёл игроку {@code receiver}. */
    public static void onDelivered(UUID sender, UUID receiver) {
        if (sender.equals(receiver)) return;
        TALK.computeIfAbsent(sender, k -> new Talk()).heardBy.put(receiver, System.currentTimeMillis());
    }

    /** Говорит прямо сейчас (пакеты идут, пауза не дольше {@code gapMs}). */
    public static boolean talking(UUID id, long gapMs) {
        Talk t = TALK.get(id);
        return t != null && System.currentTimeMillis() - t.lastPacketMs <= gapMs;
    }

    /** Кому голос ушёл за последние {@code windowMs}. */
    public static List<UUID> receivers(UUID id, long windowMs) {
        Talk t = TALK.get(id);
        List<UUID> out = new ArrayList<>();
        if (t == null) return out;
        long now = System.currentTimeMillis();
        t.heardBy.forEach((r, at) -> {
            if (now - at <= windowMs) out.add(r);
        });
        return out;
    }

    /** Говорил за последние {@code windowMs}. */
    public static boolean spokeWithin(UUID id, long windowMs) {
        Talk t = TALK.get(id);
        return t != null && System.currentTimeMillis() - t.lastSpokeMs <= windowMs;
    }

    public static void forget(UUID id) {
        TALK.remove(id);
        for (Talk t : TALK.values()) t.heardBy.remove(id);
    }

    public static void clear() {
        TALK.clear();
    }
}
