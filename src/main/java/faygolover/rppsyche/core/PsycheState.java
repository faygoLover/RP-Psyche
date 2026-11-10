package faygolover.rppsyche.core;

/** Психика игрока: то, что сохраняется. Стресс и рассудок — 0–100 (рассудок 100 — норма). */
public final class PsycheState {
    public double stress;
    public double sanity = 100;
    /** Толчки, ещё не вошедшие в стресс (небольшая инерция). */
    public double pending;
    /** Секунд подряд в полной тьме без света; секунд одному. */
    public double darkSeconds;
    public double aloneSeconds;
    /** Секунд онлайна под психикой — часы для откатов. */
    public double clock;
    /** Когда (по {@link #clock}) было в последний раз: сон, еда, пик паники, штраф за разговор с собой. */
    public double sleptAt = -1e9;
    public double ateAt = -1e9;
    public double panicAt = -1e9;
    public double selfTalkAt = -1e9;
    /** По {@link #clock}: последняя сигарета, выпивка, сообщение с КПК, погладил животное. */
    public double smokedAt = -1e9;
    public double drankAt = -1e9;
    public double messagedAt = -1e9;
    public double pettedAt = -1e9;
    /** Время мира ({@code dayTime}) последней беседы с психологом и записи в дневнике — откаты в RP-часах. */
    public long therapyAt = Long.MIN_VALUE;
    /** Когда (время мира) последний раз ранил человека. */
    public long harmAt = Long.MIN_VALUE;
    /** До какого времени мира действует встреча группы поддержки. */
    public long supportUntil = Long.MIN_VALUE;
    public long diaryAt = Long.MIN_VALUE;
    /** RP-сутки ({@code dayTime / 24000}) и сколько рассудка за них дали музыка и настолки — дневной потолок. */
    public long leisureDay = -1;
    public double leisureToday;

    public void copyFrom(PsycheState o) {
        harmAt = o.harmAt;
        supportUntil = o.supportUntil;
        stress = o.stress;
        sanity = o.sanity;
        pending = o.pending;
        darkSeconds = o.darkSeconds;
        aloneSeconds = o.aloneSeconds;
        clock = o.clock;
        sleptAt = o.sleptAt;
        ateAt = o.ateAt;
        panicAt = o.panicAt;
        selfTalkAt = o.selfTalkAt;
        smokedAt = o.smokedAt;
        drankAt = o.drankAt;
        messagedAt = o.messagedAt;
        pettedAt = o.pettedAt;
        therapyAt = o.therapyAt;
        diaryAt = o.diaryAt;
        leisureDay = o.leisureDay;
        leisureToday = o.leisureToday;
    }
}
