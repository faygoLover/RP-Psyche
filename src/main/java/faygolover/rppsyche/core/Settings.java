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
    /**
     * Спад растёт с самим стрессом: + k × стресс ед./с. Слабые источники дают умеренный устойчивый стресс, до паники
     * доводят только сильные (бой, боль, Пси-зона).
     */
    public double stressLeak = 0.004;
    /** Толчки (выстрел, взрыв, удар) входят в стресс не сразу: такая доля накопленного за секунду. */
    public double impulseFlow = 0.25;
    /** Плохой рассудок ускоряет рост стресса: ×(1 + k × (1 − рассудок/100)). */
    public double sanityStressGain = 0.6;
    /** «Пол» стресса от плохого рассудка: (100 − рассудок) × k — ниже не спадает. */
    public double stressFloor = 0.25;
    /** Пол поднимает стресс медленно: ед./с. */
    public double stressFloorRise = 0.2;
    /** Трусливый — рост стресса ×, Храбрый — × (Волевой с RP Perks 2.8.1 — только рассудок). */
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
    public double aloneDelay = 900;
    public double aloneRate = 0.15;
    public double aloneRateMax = 0.45;
    public double aloneRamp = 1200;
    /** Выше этой отметки одиночество стресс не поднимает: не по себе, но без сердцебиения. */
    public double aloneStressCap = 45;
    /** Один и в полной тьме — вместе поднимают до этой отметки. */
    public double darkAloneCap = 60;
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
    public double sanityLossThreshold = 70;
    public double sanityLossPerHour = 10;
    /** Стресс ниже этого — рассудок восстанавливается: % в час. */
    public double sanityRecoverBelow = 35;
    public double sanityRecoverPerHour = 5;
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
    /** Радиус голоса для двустороннего разговора, блоков; 0 — как в Simple Voice Chat. */
    public double voiceRadius = 0;
    /** Разговор двусторонний, если оба говорили за последние столько секунд. */
    public double voiceTwoWayWindow = 20;
    /** Фраза кончилась, если тишина дольше, мс. */
    public long voiceUtteranceGapMs = 1200;
    /**
     * Разговор с собой: столько фраз подряд никто не услышал за окно — рассудок −, не чаще отката. «Услышал» — голос
     * Simple Voice Chat реально дошёл до живого игрока (рядом или в группе) или в руке рация.
     */
    public int selfTalkCount = 5;
    public double selfTalkWindow = 180;
    public double selfTalkLoss = 0.5;
    public double selfTalkCooldown = 600;
    /** Рации: говорит в такую (предмет в руке) — говорит с кем-то, а не сам с собой. */
    public List<String> voiceRadioItems = List.of("simpleradio:transceiver", "simpleradio:walkie_talkie", "simpleradio:spuddie_talkie");

    // ---------------------------------------------------------------- пауза ГМа
    /** {@code /rppsyche pause} без времени — на столько минут. */
    public double pauseDefaultMinutes = 10;

    // ---------------------------------------------------------------- курение и выпивка (общая база RP Core)
    /** Никотин: стресс − столько за дозу 1 (сигарета; Tobacconist — 0.4 дозы); повтор раньше отката — доля. */
    public double nicotineRelief = 10;
    public double nicotineCooldown = 300;
    /** Тяга зависимого (Medicine, любое вещество): за полсуток до ломки и после острой фазы стресс понемногу растёт. */
    public double smokerCraveRate = 0.08;
    /** Бывший зависимый: со стольки стресса — «сейчас бы…» (раз в столько секунд). */
    public double formerCraveStress = 70;
    public double formerCraveCooldown = 1200;
    /** Алкоголь: стресс − столько за дозу 1 (кружка пива; вино 1.5, крепкое 2) сразу, без отката; повтор — доля. */
    public double alcoholRelief = 6;
    public double alcoholCooldown = 120;

    // ---------------------------------------------------------------- события настроения (RP Core RpMoodEvent)
    /** Написал с КПК — счётчик одиночества − столько секунд; не чаще отката. */
    public double messageAloneForget = 120;
    public double messageCooldown = 30;
    /** «Сыт и доволен» (RP Culinary) — стресс −. */
    public double fedContentRelief = 15;
    /** Любимая еда — стресс − (за порцию), нелюбимая — толчок стресса +. */
    public double favouriteRelief = 5;
    public double dislikedStress = 4;
    /** RP Needs: приснился кошмар — толчок стресса +; выспался в кровати или на койке — стресс −. */
    public double nightmareStress = 15;
    public double restedRelief = 10;

    // ---------------------------------------------------------------- антипсихотик (RP Medicine, галоперидол)
    /** Пока действует (сила ≥ этого) — галлюцинации приглушены (экран и звуки как при здоровом рассудке). */
    public double antipsychoticSuppress = 0.5;
    /** И рассудок + столько % в час × сила. */
    public double antipsychoticSanityPerHour = 6;

    // ---------------------------------------------------------------- журнал психолога
    /** Беседа длится столько секунд; раз в столько секунд проверяется, что оба рядом (не дальше радиуса). */
    public double therapySeconds = 180;
    public double therapyCheckSeconds = 10;
    public double therapyRadius = 6;
    /** Итог беседы: рассудок +, стресс −. */
    public double therapySanity = 8;
    public double therapyRelief = 15;
    /** Нужен навык «Медицина» не ниже (RP Perks; без него — любой). */
    public int therapySkill = 4;
    /** Откат журнала, секунд; откат пациента — тиков времени мира (12000 — 12 RP-часов). */
    public double therapyJournalCooldown = 600;
    public long therapyPatientTicks = 12000;

    // ---------------------------------------------------------------- досуг: рассудок
    /** Погладить своё животное (пустой рукой): рассудок +, откат. */
    public double petSanity = 0.5;
    public double petCooldown = 300;
    /** Дневник: дописал в книге столько знаков — рассудок +; не чаще раза в RP-сутки (тиков времени мира). */
    public int diaryMinChars = 100;
    public double diarySanity = 1.5;
    public long diaryCooldownTicks = 24000;
    /** Музыка (Immersive Melodies): играешь или слышишь в радиусе — рассудок + % в час. */
    public double musicRadius = 16;
    public double musicSanityPerHour = 3;
    /** Настольная игра (Table Top Craft): ты и ещё кто-то у одной доски — рассудок + % в час. */
    public double gameRadius = 3;
    public double gameSanityPerHour = 3;
    /** Музыка и настолки вместе — не больше столько рассудка за RP-сутки. */
    public double leisureDailyCap = 6;

    // ---------------------------------------------------------------- речь
    /** Заикание в речи и КПК — со стресса. */
    public double stutterFrom = 65;
}
