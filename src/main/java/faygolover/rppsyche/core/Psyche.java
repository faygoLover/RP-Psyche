package faygolover.rppsyche.core;

/**
 * Шаг психики за секунду (без Minecraft). Сервер собирает {@link Inputs} — что давит и что помогает, — шаг двигает
 * стресс и рассудок. Стресс гибкий: толчки входят за несколько секунд, спадает за минуты. Рассудок инертный: его
 * съедает долгий высокий стресс и Пси-зона, лечат часы покоя.
 */
public final class Psyche {
    private Psyche() {}

    /** Что действует на человека в эту секунду. */
    public static final class Inputs {
        /** Постоянные источники стресса, ед./с (боль, одиночество…). */
        public double stressRate;
        /** Источники, которые поднимают стресс только до {@link #cappedLimit} (тьма, одиночество), ед./с. */
        public double cappedRate;
        public double cappedLimit = 100;
        /** Разовые толчки (выстрел, взрыв, удар). */
        public double impulse;
        /** Разовое облегчение (сон, еда). */
        public double relief;
        /** Рост стресса × (черты, спокойные рядом, седация). */
        public double gainMul = 1;
        /** Спад стресса × (компания, разговор, алкоголь, седация). */
        public double decayMul = 1;
        /** Потери рассудка от стресса × (черты). */
        public double sanityLossMul = 1;
        /** Прямая потеря рассудка за эту секунду (Пси-зона), уже с чертами. */
        public double sanityDrain;
        /** Разовые потери рассудка (разговор с собой, клиническая смерть). */
        public double sanityHit;
        /** Разовая прибавка рассудка (сон). */
        public double sanityBoost;
        /** Время тела для потери и восстановления рассудка: 1 — нынешние числа (3-часовые сутки), 0 — пауза. */
        public double sanityTime = 1;
    }

    /** Шаг на {@code dt} секунд. Возвращает true, если стресс только что дошёл до пика паники. */
    public static boolean step(PsycheState s, Inputs in, Settings c, double dt) {
        double before = s.stress;

        // Толчки входят не сразу.
        s.pending += Math.max(0, in.impulse);
        double flow = s.pending * Math.min(1, c.impulseFlow * dt);
        s.pending -= flow;

        double sanityK = 1 + c.sanityStressGain * (1 - s.sanity / 100.0);
        double gain = (Math.max(0, in.stressRate) * dt + flow) * Math.max(0, in.gainMul) * sanityK;
        boolean pressed = in.stressRate > 1e-3 || in.cappedRate > 1e-3 || flow > 1e-3;
        double decay = (c.stressDecay * (pressed ? c.stressDecayWhilePressed : 1) + c.stressLeak * s.stress)
                * Math.max(0, in.decayMul) * dt;

        double floor = (100 - s.sanity) * c.stressFloor;
        double next = s.stress + gain - decay - Math.max(0, in.relief);
        // Ограниченные источники (тьма, одиночество) дотягивают только до своей отметки: выше не поднимают, но и
        // спасть не дают.
        if (in.cappedRate > 0 && next < in.cappedLimit) {
            double cg = in.cappedRate * dt * Math.max(0, in.gainMul) * sanityK;
            next = Math.min(in.cappedLimit, next + cg);
        }
        // Пол: ниже не спадает, а снизу к нему тянет медленно.
        if (next < floor) next = Math.min(floor, Math.max(next, s.stress) + c.stressFloorRise * dt);
        s.stress = clamp(next);

        // Рассудок.
        double loss = 0;
        if (s.stress > c.sanityLossThreshold) {
            double share = (s.stress - c.sanityLossThreshold) / Math.max(1e-6, 100 - c.sanityLossThreshold);
            loss += share * c.sanityLossPerHour / 3600.0 * dt * Math.max(0, in.sanityTime) * Math.max(0, in.sanityLossMul);
        }
        loss += Math.max(0, in.sanityDrain) + Math.max(0, in.sanityHit);
        double boost = Math.max(0, in.sanityBoost);
        if (s.stress < c.sanityRecoverBelow && loss <= 0) boost += c.sanityRecoverPerHour / 3600.0 * dt * Math.max(0, in.sanityTime);
        s.sanity = clamp(s.sanity - loss + boost);

        s.clock += dt;
        return before < c.panicPeak && s.stress >= c.panicPeak;
    }

    /** Доля эффекта события с откатом: прошло / откат, не больше 1 (как в Sanity). */
    public static double cooldownShare(double now, double last, double cooldown) {
        if (cooldown <= 0) return 1;
        return Math.max(0, Math.min(1, (now - last) / cooldown));
    }

    /** Стадия рассудка: 0 — норма, 1 — надломлен, 2 — неустойчив, 3 — помешательство, 4 — безумие. */
    public static int sanityStage(double sanity) {
        if (sanity > 80) return 0;
        if (sanity > 60) return 1;
        if (sanity > 40) return 2;
        if (sanity > 20) return 3;
        return 4;
    }

    /** Уровень стресса: 0 — спокоен, 1 — напряжён, 2 — тревога, 3 — паника. */
    public static int stressLevel(double stress) {
        if (stress < 30) return 0;
        if (stress < 55) return 1;
        if (stress < 80) return 2;
        return 3;
    }

    /** Нарастание от 0 до 1 между {@code from} и {@code to}. */
    public static double ramp(double value, double from, double to) {
        if (to == from) return value >= to ? 1 : 0;
        return Math.max(0, Math.min(1, (value - from) / (to - from)));
    }

    public static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
