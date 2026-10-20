package faygolover.rppsyche.config;

import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.core.Settings;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@code rppsyche-server.toml}: все числа {@link Settings}. Поле настроек и строка конфига связаны по имени
 * (camelCase → snake_case), комментарии — по-русски.
 */
public final class PsycheConfig {
    private PsycheConfig() {}

    private record Entry(Field field, Supplier<?> value) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        Settings d = new Settings();

        b.comment("Стресс (0–100): гибкий, растёт за десятки секунд, спадает за минуты.").push("stress");
        bind(b, d, "stressDecay", "Спад стресса в покое, ед./с (от 100 до 0 — около двух минут).");
        bind(b, d, "stressDecayWhilePressed", "Пока что-то давит, спад слабее: множитель.");
        bind(b, d, "impulseFlow", "Толчки (выстрел, взрыв, удар) входят в стресс не сразу: такая доля накопленного за секунду.");
        bind(b, d, "sanityStressGain", "Плохой рассудок ускоряет рост стресса: ×(1 + k × (1 − рассудок/100)).");
        bind(b, d, "stressFloor", "«Пол» стресса от плохого рассудка: (100 − рассудок) × k — ниже стресс не спадает.");
        bind(b, d, "stressFloorRise", "Снизу к полу стресс тянет медленно: ед./с.");
        bind(b, d, "cowardGain", "Трусливый: рост стресса ×.");
        bind(b, d, "braveGain", "Храбрый (и Волевой): рост стресса ×.");
        b.pop();

        b.comment("Темнота: только полная — свет ~0 и ни у кого рядом нет света в руках.").push("darkness");
        bind(b, d, "darkLight", "Полная тьма: свет у глаз не выше этого (0–15).");
        bind(b, d, "darkDelay", "Столько секунд во тьме — пока ничего.");
        bind(b, d, "darkRate", "Потом стресс растёт со столько ед./с…");
        bind(b, d, "darkRateMax", "…и через dark_ramp секунд — со столько.");
        bind(b, d, "darkRamp", "За сколько секунд рост доходит до наибольшего.");
        bind(b, d, "darkFriendLight", "Свет у игрока рядом (в этом радиусе) светит и тебе.");
        bind(b, d, "lightItems", "Что светит в руках: id предмета или id{ключ} — тогда только если в NBT предмета ключ включён"
                + " (фонари). Плюс тег предметов rppsyche:light_sources.");
        b.pop();

        b.comment("Одиночество: не сразу, а когда долго никого рядом.").push("loneliness");
        bind(b, d, "aloneRadius", "Один — нет живого игрока (не креатив и не наблюдатель) в этом радиусе.");
        bind(b, d, "aloneDelay", "Столько секунд одному — пока ничего.");
        bind(b, d, "aloneRate", "Потом стресс растёт со столько ед./с…");
        bind(b, d, "aloneRateMax", "…и через alone_ramp секунд — со столько.");
        bind(b, d, "aloneRamp", "За сколько секунд рост доходит до наибольшего.");
        bind(b, d, "aloneForget", "Рядом люди — счётчик одиночества тает с такой скоростью (секунд за секунду).");
        b.pop();

        b.comment("Компания и заразность психики.").push("company");
        bind(b, d, "companyRadius", "Люди в этом радиусе — компания.");
        bind(b, d, "companyDecay", "Компания: спад стресса ×.");
        bind(b, d, "voiceCompanyDecay", "Двусторонний разговор в голосовом чате: спад стресса ×.");
        bind(b, d, "contagionRadius", "Видно паникующего в этом радиусе — стресс растёт.");
        bind(b, d, "contagionRate", "Сколько ед./с даёт паникующий рядом.");
        bind(b, d, "panicLevel", "Паника — стресс не ниже этого.");
        bind(b, d, "calmLevel", "Спокоен — стресс не выше этого.");
        bind(b, d, "calmNearbyGain", "Рядом спокойный: рост стресса ×.");
        b.pop();

        b.comment("Бой: выстрелы TaCZ, пули рядом, взрывы, свой урон.").push("combat");
        bind(b, d, "shotRadius", "Выстрел слышно в этом радиусе.");
        bind(b, d, "shotCloseRadius", "Ближе этого — выстрел рядом.");
        bind(b, d, "shotClose", "Толчок от выстрела рядом.");
        bind(b, d, "shotFar", "Толчок от выстрела вдалеке.");
        bind(b, d, "nearMissRadius", "Пуля прошла ближе этого (блоков) от головы — подавление.");
        bind(b, d, "nearMiss", "Толчок от пули рядом.");
        bind(b, d, "explosionRadius", "Взрыв действует в этом радиусе.");
        bind(b, d, "explosion", "Толчок от взрыва в упор (дальше — меньше).");
        bind(b, d, "hurtPerDamage", "Свой урон: толчок за 1 ед. урона.");
        bind(b, d, "hurtMax", "Свой урон: не больше столько за удар.");
        b.pop();

        b.comment("Тело (RP Medicine), голод и жажда.").push("body");
        bind(b, d, "painThreshold", "Боль выше этого давит на психику.");
        bind(b, d, "painRate", "При боли 100 — столько ед./с (ниже — меньше).");
        bind(b, d, "downedRadius", "Лежачий в этом радиусе (нокдаун, обморок, тело вышедшего) давит.");
        bind(b, d, "downedRate", "Лежачий рядом: ед./с.");
        bind(b, d, "clinicalRate", "Клиническая смерть рядом: ед./с.");
        bind(b, d, "hungerRate", "Голод со второй стадии: ед./с.");
        bind(b, d, "thirstRate", "Жажда со второй стадии: ед./с.");
        bind(b, d, "withdrawalRate", "Ломка: ед./с.");
        bind(b, d, "sedationGainCut", "Седация (диазепам) 0–100: рост стресса ×(1 − k × седация/100), спад ×(1 + седация/100).");
        bind(b, d, "alcoholDecay", "Опьянение 0–100: спад стресса ×(1 + k × опьянение/100).");
        bind(b, d, "clinicalReturnSanity", "Вернули из клинической смерти — рассудок минус столько.");
        b.pop();

        b.comment("Облегчение: сон и еда. Повтор раньше отката даёт долю (прошло / откат).").push("relief");
        bind(b, d, "sleepRelief", "Проспал ночь: стресс минус столько.");
        bind(b, d, "sleepSanity", "Проспал ночь: рассудок плюс столько.");
        bind(b, d, "sleepCooldown", "Откат сна, секунд онлайна.");
        bind(b, d, "foodRelief", "Еда: стресс минус столько за 100 ккал…");
        bind(b, d, "foodReliefMax", "…но не больше столько за раз.");
        bind(b, d, "foodCooldown", "Откат еды, секунд онлайна.");
        b.pop();

        b.comment("Дрожь рук от стресса: с 60 мелкая, с 80 ещё и уводит взгляд.").push("tremor");
        bind(b, d, "tremorScale", "Общая сила дрожи (1 — как в 0.1.0).");
        bind(b, d, "tremorBrave", "Храбрый: дрожь ×.");
        bind(b, d, "tremorCoward", "Трусливый: дрожь ×.");
        b.pop();

        b.comment("Рассудок (0–100, 100 — норма): инертный, портится за часы, лечится днями онлайна.").push("sanity");
        bind(b, d, "sanityLossThreshold", "Стресс выше этого съедает рассудок.");
        bind(b, d, "sanityLossPerHour", "При стрессе 100 — столько % в час.");
        bind(b, d, "sanityRecoverBelow", "Стресс ниже этого — рассудок восстанавливается…");
        bind(b, d, "sanityRecoverPerHour", "…на столько % в час.");
        bind(b, d, "panicPeak", "Пик паники — стресс дошёл до этого.");
        bind(b, d, "panicPeakLoss", "Пик паники: рассудок минус столько.");
        bind(b, d, "panicPeakCooldown", "Пик паники бьёт по рассудку не чаще, секунд.");
        bind(b, d, "willedLoss", "Волевой: потери рассудка ×.");
        bind(b, d, "suggestibleLoss", "Внушаемый: потери рассудка ×.");
        b.pop();

        b.comment("Пси-зона (Zone Artifacts).").push("psi");
        bind(b, d, "psiStress", "В Пси-зоне стресс + ед./с.");
        bind(b, d, "psiSanityPerMinute", "В Пси-зоне рассудок минус % в минуту (× сила аномалии).");
        bind(b, d, "psiWilled", "Волевой: Пси-зона ×.");
        bind(b, d, "psiSuggestible", "Внушаемый: Пси-зона ×.");
        b.pop();

        b.comment("Голос (Simple Voice Chat): двусторонний разговор — компания, разговор с собой — минус рассудок.").push("voice");
        bind(b, d, "voiceRadius", "Радиус голоса, блоков; 0 — как в Simple Voice Chat.");
        bind(b, d, "voiceWhisperFactor", "Шёпот слышно на такой доле радиуса.");
        bind(b, d, "voiceTwoWayWindow", "Разговор двусторонний, если оба говорили за последние столько секунд.");
        bind(b, d, "voiceUtteranceGapMs", "Фраза кончилась, если тишина дольше, мс.");
        bind(b, d, "selfTalkCount", "Разговор с собой: столько фраз подряд никто не услышал…");
        bind(b, d, "selfTalkWindow", "…за столько секунд…");
        bind(b, d, "selfTalkLoss", "…рассудок минус столько.");
        b.pop();

        b.comment("Речь.").push("speech");
        bind(b, d, "stutterFrom", "Заикание в речи и КПК — со стресса.");
        b.pop();

        SPEC = b.build();
    }

    private static String key(String field) {
        StringBuilder sb = new StringBuilder();
        for (char ch : field.toCharArray()) {
            if (Character.isUpperCase(ch)) sb.append('_').append(Character.toLowerCase(ch));
            else sb.append(ch);
        }
        return sb.toString();
    }

    private static void bind(ForgeConfigSpec.Builder b, Settings d, String name, String comment) {
        try {
            Field f = Settings.class.getField(name);
            Object def = f.get(d);
            String key = key(name);
            Supplier<?> v;
            if (f.getType() == double.class) {
                v = b.comment(comment).defineInRange(key, (double) def, -1e9, 1e9);
            } else if (f.getType() == int.class) {
                v = b.comment(comment).defineInRange(key, (int) def, 0, Integer.MAX_VALUE);
            } else if (f.getType() == long.class) {
                v = b.comment(comment).defineInRange(key, (long) def, 0L, Long.MAX_VALUE);
            } else if (f.getType() == List.class) {
                @SuppressWarnings("unchecked") List<String> list = (List<String>) def;
                v = b.comment(comment).defineListAllowEmpty(List.of(key), () -> list, o -> o instanceof String);
            } else {
                throw new IllegalStateException("тип " + f.getType());
            }
            ENTRIES.add(new Entry(f, v));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("RP Psyche: нет настройки " + name, e);
        }
    }

    /** Переложить значения конфига в {@link Settings}. */
    public static void bake() {
        Settings s = new Settings();
        for (Entry e : ENTRIES) {
            try {
                Object v = e.value.get();
                if (e.field.getType() == List.class) v = List.copyOf((List<?>) v);
                e.field.set(s, v);
            } catch (ReflectiveOperationException ex) {
                RpPsyche.LOGGER.error("RP Psyche: настройка {} не прочитана", e.field.getName(), ex);
            }
        }
        Settings.set(s);
    }
}
