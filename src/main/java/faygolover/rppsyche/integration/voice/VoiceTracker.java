package faygolover.rppsyche.integration.voice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кто говорит в голосовой чат. Плагин Simple Voice Chat пишет сюда из своего потока (только время и шёпот), сервер
 * раз в секунду читает. Без Simple Voice Chat здесь пусто — голоса просто нет.
 */
public final class VoiceTracker {
    private VoiceTracker() {}

    public static final class Talk {
        volatile long lastPacketMs;
        volatile boolean whisper;
        /** Когда (мс) в последний раз говорил вообще — для «двустороннего разговора». */
        volatile long lastSpokeMs;
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

    public static void onPacket(UUID id, boolean whisper) {
        Talk t = TALK.computeIfAbsent(id, k -> new Talk());
        long now = System.currentTimeMillis();
        t.lastPacketMs = now;
        t.lastSpokeMs = now;
        t.whisper = whisper;
    }

    /** Говорит прямо сейчас (пакеты идут, пауза не дольше {@code gapMs}). */
    public static boolean talking(UUID id, long gapMs) {
        Talk t = TALK.get(id);
        return t != null && System.currentTimeMillis() - t.lastPacketMs <= gapMs;
    }

    public static boolean whispering(UUID id) {
        Talk t = TALK.get(id);
        return t != null && t.whisper;
    }

    /** Говорил за последние {@code windowMs}. */
    public static boolean spokeWithin(UUID id, long windowMs) {
        Talk t = TALK.get(id);
        return t != null && System.currentTimeMillis() - t.lastSpokeMs <= windowMs;
    }

    public static void forget(UUID id) {
        TALK.remove(id);
    }

    public static void clear() {
        TALK.clear();
    }
}
