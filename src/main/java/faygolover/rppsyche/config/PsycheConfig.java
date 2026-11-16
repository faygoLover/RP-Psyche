package faygolover.rppsyche.config;

import faygolover.rpcore.api.config.SettingsBinder;
import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.core.Settings;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code rppsyche-server.toml}: все числа {@link Settings}. Поле настроек и строка конфига связаны по имени
 * (camelCase → snake_case), комментарии — по-русски.
 */
public final class PsycheConfig {
    private PsycheConfig() {}

    /** Привязка полей {@link Settings} к конфигу (RP Core). Границы по умолчанию: числа −1e9…1e9, целые от 0. */
    private static final SettingsBinder<Settings> S = SettingsBinder.of(Settings.class, Settings::new);
    public static final ForgeConfigSpec SPEC;

    static {
        S.push("stress", "Стресс (0–100): гибкий, растёт за десятки секунд, спадает за минуты.");
        S.bind("stressDecay", "Спад стресса в покое, ед./с (от 100 до 0 — около двух минут).");
        S.bind("stressDecayWhilePressed", "Пока что-то давит, спад слабее: множитель.");
        S.bind("stressLeak", "Спад растёт с самим стрессом: + k × стресс ед./с — слабые источники дают умеренный стресс,"
                + " до паники доводят только сильные.");
        S.bind("impulseFlow", "Толчки (выстрел, взрыв, удар) входят в стресс не сразу: такая доля накопленного за секунду.");
        S.bind("sanityStressGain", "Плохой рассудок ускоряет рост стресса: ×(1 + k × (1 − рассудок/100)).");
        S.bind("stressFloor", "«Пол» стресса от плохого рассудка: (100 − рассудок) × k — ниже стресс не спадает.");
        S.bind("stressFloorRise", "Снизу к полу стресс тянет медленно: ед./с.");
        S.bind("cowardGain", "Трусливый: рост стресса ×.");
        S.bind("braveGain", "Храбрый (и Волевой): рост стресса ×.");
        S.pop();

        S.push("darkness", "Темнота: только полная — свет ~0 и ни у кого рядом нет света в руках.");
        S.bind("darkLight", "Полная тьма: свет у глаз не выше этого (0–15).");
        S.bind("darkDelay", "Столько секунд во тьме — пока ничего.");
        S.bind("darkRate", "Потом стресс растёт со столько ед./с…");
        S.bind("darkRateMax", "…и через dark_ramp секунд — со столько.");
        S.bind("darkRamp", "За сколько секунд рост доходит до наибольшего.");
        S.bind("darkStressCap", "Выше этой отметки тьма стресс не поднимает (дрожь — с 60).");
        S.bind("darkFriendLight", "Свет у игрока рядом (в этом радиусе) светит и тебе.");
        S.bind("lightItems", "Что светит в руках или надето в Curios (налобный, лампа на поясе): id предмета или id{ключ} — тогда только если в NBT предмета ключ включён"
                + " (фонари). Плюс тег предметов rppsyche:light_sources.");
        S.pop();

        S.push("loneliness", "Одиночество: не сразу, а когда долго никого рядом.");
        S.bind("aloneRadius", "Один — нет живого игрока (не креатив и не наблюдатель) в этом радиусе.");
        S.bind("aloneDelay", "Столько секунд одному — пока ничего.");
        S.bind("aloneRate", "Потом стресс растёт со столько ед./с…");
        S.bind("aloneRateMax", "…и через alone_ramp секунд — со столько.");
        S.bind("aloneRamp", "За сколько секунд рост доходит до наибольшего.");
        S.bind("aloneStressCap", "Выше этой отметки одиночество стресс не поднимает.");
        S.bind("darkAloneCap", "Один и в полной тьме — вместе поднимают до этой отметки.");
        S.bind("aloneForget", "Рядом люди — счётчик одиночества тает с такой скоростью (секунд за секунду).");
        S.pop();

        S.push("company", "Компания и заразность психики.");
        S.bind("companyRadius", "Люди в этом радиусе — компания.");
        S.bind("companyDecay", "Компания: спад стресса ×.");
        S.bind("voiceCompanyDecay", "Двусторонний разговор в голосовом чате: спад стресса ×.");
        S.bind("contagionRadius", "Видно паникующего в этом радиусе — стресс растёт.");
        S.bind("contagionRate", "Сколько ед./с даёт паникующий рядом.");
        S.bind("panicLevel", "Паника — стресс не ниже этого.");
        S.bind("calmLevel", "Спокоен — стресс не выше этого.");
        S.bind("calmNearbyGain", "Рядом спокойный: рост стресса ×.");
        S.pop();

        S.push("combat", "Бой: выстрелы TaCZ, пули рядом, взрывы, свой урон.");
        S.bind("shotRadius", "Выстрел слышно в этом радиусе.");
        S.bind("shotCloseRadius", "Ближе этого — выстрел рядом.");
        S.bind("shotClose", "Толчок от выстрела рядом.");
        S.bind("shotFar", "Толчок от выстрела вдалеке.");
        S.bind("nearMissRadius", "Пуля прошла ближе этого (блоков) от головы — подавление.");
        S.bind("nearMiss", "Толчок от пули рядом.");
        S.bind("explosionRadius", "Взрыв действует в этом радиусе.");
        S.bind("explosion", "Толчок от взрыва в упор (дальше — меньше).");
        S.bind("hurtPerDamage", "Свой урон: толчок за 1 ед. урона.");
        S.bind("hurtMax", "Свой урон: не больше столько за удар.");
        S.pop();

        S.push("body", "Тело (RP Medicine), голод и жажда.");
        S.bind("painThreshold", "Боль выше этого давит на психику.");
        S.bind("painRate", "При боли 100 — столько ед./с (ниже — меньше).");
        S.bind("downedRadius", "Лежачий в этом радиусе (нокдаун, обморок, тело вышедшего) давит.");
        S.bind("downedRate", "Лежачий рядом: ед./с.");
        S.bind("clinicalRate", "Клиническая смерть рядом: ед./с.");
        S.bind("hungerRate", "Голод со второй стадии: ед./с.");
        S.bind("sleepStage2Gain", "Сонливость (RP Needs): рост стресса ×.");
        S.bind("sleepStage3Gain", "Валится с ног и дальше: рост стресса ×.");
        S.bind("temperatureRate", "Долго холодно или жарко: ед./с.");
        S.bind("temperatureDelay", "Через столько секунд холод или жара начинают давить.");
        S.bind("harmKnockdownStress", "Своей рукой отправил человека в нокдаун: стресс.");
        S.bind("harmLethalStress", "Довёл до клинической смерти или смерти: стресс.");
        S.bind("harmLethalSanity", "То же: потеря рассудка.");
        S.bind("harmRepeat", "Повтор за RP-сутки: доля силы.");
        S.bind("harmSoldier", "Подготовленный солдат: множитель.");
        S.bind("harmPacifist", "Пацифист: множитель.");
        S.bind("smokeCompanyRadius", "Перекур: кто курил в этом радиусе, блоки.");
        S.bind("smokeCompanyWindow", "Перекур: кто курил за столько секунд до тебя.");
        S.bind("smokeCompanyBonus", "Перекур в компании: облегчение от никотина ×.");
        S.bind("supportRadius", "Группа поддержки: психолог с журналом и ещё один зависимый в этом радиусе, блоки.");
        S.bind("supportSeconds", "Группа поддержки: столько секунд вместе (копится, перерывы не сбрасывают) — встреча засчитана.");
        S.bind("supportCraveMult", "После встречи на RP-сутки: стресс от тяги и ломки ×.");
        S.bind("thirstRate", "Жажда со второй стадии: ед./с.");
        S.bind("withdrawalRate", "Ломка: ед./с.");
        S.bind("sedationGainCut", "Седация (диазепам) 0–100: рост стресса ×(1 − k × седация/100), спад ×(1 + седация/100).");
        S.bind("alcoholDecay", "Опьянение 0–100: спад стресса ×(1 + k × опьянение/100).");
        S.bind("clinicalReturnSanity", "Вернули из клинической смерти — рассудок минус столько.");
        S.pop();

        S.push("relief", "Облегчение: сон и еда. Повтор раньше отката даёт долю (прошло / откат).");
        S.bind("sleepRelief", "Проспал ночь: стресс минус столько.");
        S.bind("sleepSanity", "Проспал ночь: рассудок плюс столько.");
        S.bind("sleepCooldown", "Откат сна, секунд онлайна.");
        S.bind("foodRelief", "Еда: стресс минус столько за 100 ккал…");
        S.bind("foodReliefMax", "…но не больше столько за раз.");
        S.bind("foodCooldown", "Откат еды, секунд онлайна.");
        S.pop();

        S.push("tremor", "Дрожь рук от стресса: с 60 мелкая, с 80 ещё и уводит взгляд.");
        S.bind("tremorScale", "Общая сила дрожи (1 — как в 0.1.0).");
        S.bind("tremorBrave", "Храбрый: дрожь ×.");
        S.bind("tremorCoward", "Трусливый: дрожь ×.");
        S.pop();

        S.push("sanity", "Рассудок (0–100, 100 — норма): инертный, портится за часы, лечится днями онлайна.");
        S.bind("sanityLossThreshold", "Стресс выше этого съедает рассудок.");
        S.bind("sanityLossPerHour", "При стрессе 100 — столько % в час.");
        S.bind("sanityRecoverBelow", "Стресс ниже этого — рассудок восстанавливается…");
        S.bind("sanityRecoverPerHour", "…на столько % в час.");
        S.bind("panicPeak", "Пик паники — стресс дошёл до этого.");
        S.bind("panicPeakLoss", "Пик паники: рассудок минус столько.");
        S.bind("panicPeakCooldown", "Пик паники бьёт по рассудку не чаще, секунд.");
        S.bind("willedLoss", "Волевой: потери рассудка ×.");
        S.bind("suggestibleLoss", "Внушаемый: потери рассудка ×.");
        S.pop();

        S.push("psi", "Пси-зона (Zone Artifacts).");
        S.bind("psiStress", "В Пси-зоне стресс + ед./с.");
        S.bind("psiSanityPerMinute", "В Пси-зоне рассудок минус % в минуту (× сила аномалии).");
        S.bind("psiWilled", "Волевой: Пси-зона ×.");
        S.bind("psiSuggestible", "Внушаемый: Пси-зона ×.");
        S.pop();

        S.push("voice", "Голос (Simple Voice Chat): двусторонний разговор — компания, разговор с собой — минус рассудок.");
        S.bind("voiceRadius", "Радиус голоса для двустороннего разговора, блоков; 0 — как в Simple Voice Chat.");
        S.bind("voiceTwoWayWindow", "Разговор двусторонний, если оба говорили за последние столько секунд.");
        S.bind("voiceUtteranceGapMs", "Фраза кончилась, если тишина дольше, мс.");
        S.bind("selfTalkCount", "Разговор с собой: столько фраз подряд никто не услышал (голос не дошёл ни до кого"
                + " живого — ни рядом, ни в группе, и не в рацию)…");
        S.bind("selfTalkWindow", "…за столько секунд…");
        S.bind("selfTalkLoss", "…рассудок минус столько…");
        S.bind("selfTalkCooldown", "…не чаще раза в столько секунд.");
        S.bind("voiceRadioItems", "Рации (id предметов): говорит в такую, что в руке, — говорит с кем-то.");
        S.pop();

        S.push("pause", "Пауза ГМа (/rppsyche pause): психика стоит, эффекты выключены.");
        S.bind("pauseDefaultMinutes", "Без указанного времени — на столько минут.");
        S.pop();

        S.push("substances", "Курение и выпивка: что считается — общая база RP Core (rpcore/consumables).");
        S.bind("nicotineRelief", "Никотин: стресс − столько за дозу 1 (сигарета; Tobacconist — 0.4).");
        S.bind("nicotineCooldown", "Повтор раньше стольких секунд — доля облегчения.");
        S.bind("smokerCraveRate", "Тяга зависимого (RP Medicine, любое вещество; до ломки и после острой фазы): стресс + столько ед./с.");
        S.bind("formerCraveStress", "Бывший зависимый: со стольки стресса тянет к веществу (только строка, без штрафов).");
        S.bind("formerCraveCooldown", "…не чаще раза в столько секунд.");
        S.bind("alcoholRelief", "Алкоголь: стресс − столько за дозу 1 (пиво; вино 1.5, крепкое 2), сразу, без отката.");
        S.bind("alcoholCooldown", "Повтор раньше стольких секунд — доля облегчения.");
        S.pop();

        S.push("mood", "События настроения от других модов (КПК, еда).");
        S.bind("messageAloneForget", "Написал с КПК — одиночество отступает на столько секунд.");
        S.bind("messageCooldown", "Не чаще раза в столько секунд.");
        S.bind("fedContentRelief", "«Сыт и доволен» (RP Culinary) — стресс −.");
        S.bind("favouriteRelief", "Любимая еда — стресс − за порцию.");
        S.bind("dislikedStress", "Нелюбимая еда — толчок стресса + за порцию.");
        S.bind("nightmareStress", "RP Needs: приснился кошмар — толчок стресса +.");
        S.bind("restedRelief", "RP Needs: выспался в кровати или на койке — стресс −.");
        S.pop();

        S.push("antipsychotic", "Антипсихотик (RP Medicine, галоперидол).");
        S.bind("antipsychoticSuppress", "Сила эффекта не ниже этого — галлюцинации приглушены.");
        S.bind("antipsychoticSanityPerHour", "Рассудок + % в час × сила.");
        S.pop();

        S.push("therapy", "Журнал психолога: беседа с пациентом.");
        S.bind("therapySeconds", "Беседа длится столько секунд.");
        S.bind("therapyCheckSeconds", "Раз в столько секунд проверяется, что оба рядом.");
        S.bind("therapyRadius", "Рядом — не дальше стольких блоков.");
        S.bind("therapySanity", "Итог: рассудок +.");
        S.bind("therapyRelief", "Итог: стресс −.");
        S.bind("therapySkill", "Нужен навык «Медицина» не ниже (RP Perks; без него — любой).");
        S.bind("therapyJournalCooldown", "Откат журнала, секунд.");
        S.bind("therapyPatientTicks", "Откат пациента, тиков времени мира (12000 — 12 RP-часов).");
        S.pop();

        S.push("leisure", "Досуг: то, что держит рассудок.");
        S.bind("petSanity", "Погладить своё животное пустой рукой: рассудок +.");
        S.bind("petCooldown", "Откат, секунд.");
        S.bind("diaryMinChars", "Дневник: дописал в книге столько знаков…");
        S.bind("diarySanity", "…рассудок +.");
        S.bind("diaryCooldownTicks", "Не чаще раза в столько тиков времени мира (24000 — RP-сутки).");
        S.bind("musicRadius", "Музыка (Immersive Melodies): слышно в этом радиусе.");
        S.bind("musicSanityPerHour", "Играешь или слушаешь — рассудок + % в час.");
        S.bind("gameRadius", "Настольная игра (Table Top Craft): у доски — не дальше стольких блоков.");
        S.bind("gameSanityPerHour", "Ты и ещё кто-то у одной доски — рассудок + % в час.");
        S.bind("leisureDailyCap", "Музыка и настолки вместе — не больше столько рассудка за RP-сутки.");
        S.pop();

        S.push("speech", "Речь.");
        S.bind("stutterFrom", "Заикание в речи и КПК — со стресса.");
        S.pop();

        SPEC = S.build();
    }

    /** Переложить значения конфига в {@link Settings}. */
    public static void bake() {
        try {
            Settings.set(S.bake());
        } catch (RuntimeException ex) {
            RpPsyche.LOGGER.error("RP Psyche: настройки не прочитаны", ex);
        }
    }
}
