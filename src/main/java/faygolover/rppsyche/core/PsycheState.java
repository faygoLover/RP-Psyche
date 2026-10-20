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

    public void copyFrom(PsycheState o) {
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
    }
}
