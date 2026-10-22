package faygolover.rppsyche.core;

import java.util.List;

/**
 * Все числа психики (черновик плана, {@code docs/00_plan.md}). Значения по умолчанию — здесь; сервер берёт их из
 * {@code rppsyche-server.toml} ({@link faygolover.rppsyche.config.PsycheConfig}). Стресс и рассудок — 0–100.
 */
public final class Settings {
    private static volatile Settings current = new Settings();

    public static Settings get() {
        return current;
    }

    public static void set(Settings s) {
        current = s;
    }

    // ---------------------------------------------------------------- стресс
    /** Спад стресса в покое, ед./с (от 100 до 0 — около двух минут). */
    public double stressDecay = 0.8;
    /** Пока что-то давит, спад слабее: множитель. */
    public double stressDecayWhilePressed = 0.25;
    /** Толчки (выстрел, взрыв, удар) входят в стресс не сразу: такая доля накопленного за секунду. */
    public double impulseFlow = 0.25;
    /** Плохой рассудок ускоряет рост стресса: ×(1 + k × (1 − рассудок/100)). */
    public double sanityStressGain = 0.6;
    /** «Пол» стресса от плохого рассудка: (100 − рассудок) × k — ниже не спадает. */
    public double stressFloor = 0.35;
    /** Пол поднимает стресс медленно: ед./с. */
    public double stressFloorRise = 0.2;
    /** Трусливый — рост стресса ×, Храбрый (и Волевой) — ×. */
    public double cowardGain = 1.3;
    public double braveGain = 0.75;

    // ---------------------------------------------------------------- темнота
    /** Полная тьма: свет у глаз не выше этого (0–15). */
    public int darkLight = 1;
    /** Сколько секунд во тьме без света — пока ничего. */
    public double darkDelay = 30;
    /**
     * Потом стресс растёт: сначала столько ед./с, через {@link #darkRamp} секунд — {@link #darkRateMax}. Выше
     * {@link #darkStressCap} тьма не поднимает: к ней спокойный человек подходит минуты за три с половиной.
     */
    public double darkRate = 0.3;
    public double darkRateMax = 0.85;
    public double darkRamp = 270;
    /** Выше этой отметки тьма стресс не поднимает (дрожь — с 60): напряжён, но не паникует. */
    public double darkStressCap = 50;
    /** Свет у игрока рядом (в этом радиусе) светит и тебе. */
    public double darkFriendLight = 6;
    /**
     * Что светит в руках: id предмета или id{ключ} — светит, только если в NBT ключ включён (фонари). Плюс тег
     * {@code rppsyche:light_sources}.
     */
    public List<String> lightItems = List.of(
            "minecraft:torch", "minecraft:soul_torch", "minecraft:lantern", "minecraft:soul_lantern",
            "minecraft:redstone_torch", "minecraft:glowstone", "minecraft:jack_o_lantern", "minecraft:sea_lantern",
            "minecraft:shroomlight", "minecraft:glow_berries", "minecraft:lava_bucket",
            "sa_combat:flashlight{FlashlightOn}");

    // ---------------------------------------------------------------- одиночество
    /** Один — значит, нет живого игрока (не креатив и не наблюдатель) в этом радиусе. */
    public double aloneRadius = 32;
    /** Сколько секунд одному — пока ничего. */
    public double aloneDelay = 600;
    public double aloneRate = 0.08;
    public double aloneRateMax = 0.25;
    public double aloneRamp = 1200;
    /** Рядом люди — счётчик одиночества тает с такой скоростью (с/с). */
    public double aloneForget = 5;

    // ---------------------------------------------------------------- компания и заразность
    /** Люди в этом радиусе — компания: спад стресса ×{@link #companyDecay}. */
    public double companyRadius = 8;
    public double companyDecay = 1.5;
    /** Двусторонний разговор в голосовом чате — спад ×. */
    public double voiceCompanyDecay = 2.0;
    /** Заразность: видно паникующего (стресс ≥ {@link #panicLevel}) в этом радиусе — стресс +ед./с. */
    public double contagionRadius = 12;
    public double contagionRate = 0.5;
    public double panicLevel = 80;
    /** Рядом спокойный (стресс ≤ этого) — рост стресса ×{@link #calmNearbyGain}. */
    public double calmLevel = 25;
    public double calmNearbyGain = 0.85;

    // ---------------------------------------------------------------- бой
    /** Выстрел слышно в этом радиусе; ближе {@link #shotCloseRadius} — сильнее. По умолчанию 0: давит только пуля рядом. */
    public double shotRadius = 64;
    public double shotCloseRadius = 16;
    public double shotClose = 0;
    public double shotFar = 0;
    /** Пуля прошла ближе этого (блоков) от головы — подавление. */
    public double nearMissRadius = 2.5;
    public double nearMiss = 12;
    /** Взрыв: радиус и толчок в упор (дальше — меньше). */
    public double explosionRadius = 24;
    public double explosion = 18;
    /** Свой урон: толчок за 1 ед. урона (ванильные полсердца) и предел за удар. */
    public double hurtPerDamage = 1.5;
    public double hurtMax = 15;

    // ---------------------------------------------------------------- тело (RP Medicine)
    /** Боль выше порога: стресс + (боль − порог)/(100 − порог) × ед./с. */
    public double painThreshold = 20;
    public double painRate = 0.6;
    /** Лежачий рядом (нокдаун, обморок, тело вышедшего) — ед./с; клиническая смерть — ед./с. */
    public double downedRadius = 12;
    public double downedRate = 0.4;
    public double clinicalRate = 0.8;
    /** Голод и жажда со второй стадии — ед./с. */
    public double hungerRate = 0.12;
    public double thirstRate = 0.12;
    /** Ломка (алкоголь, кофеин…) — ед./с. */
    public double withdrawalRate = 0.25;
    /** Седация 0–100 (диазепам): рост ×(1 − k × седация/100), спад ×(1 + седация/100). */
    public double sedationGainCut = 0.6;
    /** Опьянение 0–100: спад ×(1 + k × опьянение/100). */
    public double alcoholDecay = 0.8;
    /** Вернули из клинической смерти — рассудок −. */
    public double clinicalReturnSanity = 10;

    // ---------------------------------------------------------------- облегчение
    /** Проспал ночь — стресс −, рассудок +; откат (с онлайна) — повтор раньше даёт долю. */
    public double sleepRelief = 60;
    public double sleepSanity = 2;
    public double sleepCooldown = 1200;
    /** Еда: стресс − столько за 100 ккал, не больше {@link #foodReliefMax}; откат. */
    public double foodRelief = 2;
    public double foodReliefMax = 8;
    public double foodCooldown = 300;

    // ---------------------------------------------------------------- дрожь
    /** Дрожь рук от стресса (с 60, на панике ещё и уводит взгляд): общая сила, 1 — как в 0.1.0. */
    public double tremorScale = 0.5;
    /** Храбрый — дрожь ×, Трусливый — ×. */
    public double tremorBrave = 0.6;
    public double tremorCoward = 1.4;

    // ---------------------------------------------------------------- рассудок
    /** Стресс выше порога съедает рассудок: при стрессе 100 — столько % в час. */
    public double sanityLossThreshold = 60;
    public double sanityLossPerHour = 20;
    /** Стресс ниже этого — рассудок восстанавливается: % в час. */
    public double sanityRecoverBelow = 25;
    public double sanityRecoverPerHour = 3;
    /** Пик паники (стресс дошёл до этого) — рассудок −; не чаще отката. */
    public double panicPeak = 95;
    public double panicPeakLoss = 1;
    public double panicPeakCooldown = 300;
    /** Волевой и Внушаемый — потери рассудка ×. */
    public double willedLoss = 0.7;
    public double suggestibleLoss = 1.35;

    // ---------------------------------------------------------------- Пси-зона (Zone Artifacts)
    public double psiStress = 1.5;
    /** Рассудок −% в минуту (× сила аномалии). */
    public double psiSanityPerMinute = 4;
    public double psiWilled = 0.5;
    public double psiSuggestible = 1.6;

    // ---------------------------------------------------------------- голос (Simple Voice Chat)
    /** Радиус голоса, блоков; 0 — как в Simple Voice Chat. */
    public double voiceRadius = 0;
    /** Шёпот слышно на такой доле радиуса. */
    public double voiceWhisperFactor = 0.5;
    /** Разговор двусторонний, если оба говорили за последние столько секунд. */
    public double voiceTwoWayWindow = 20;
    /** Фраза кончилась, если тишина дольше, мс. */
    public long voiceUtteranceGapMs = 1200;
    /** Разговор с собой: столько фраз подряд никто не услышал за окно — рассудок −. */
    public int selfTalkCount = 3;
    public double selfTalkWindow = 120;
    public double selfTalkLoss = 1.5;

    // ---------------------------------------------------------------- речь
    /** Заикание в речи и КПК — со стресса. */
    public double stutterFrom = 65;
}
