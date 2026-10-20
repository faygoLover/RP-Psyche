package faygolover.rppsyche.core;

import java.util.random.RandomGenerator;

/**
 * Заикание от стресса: начало слова повторяется через дефис («п-п-подожди»), иногда — обрыв «...». Сила 0–1: чем
 * больше, тем чаще и длиннее.
 */
public final class Stutter {
    private Stutter() {}

    public static String apply(String text, double level, RandomGenerator rnd) {
        if (text == null || text.isEmpty() || level <= 0) return text;
        level = Math.min(1, level);
        double wordChance = 0.15 + 0.35 * level;
        double pauseChance = 0.12 * level;
        StringBuilder out = new StringBuilder(text.length() + 16);
        int i = 0, n = text.length();
        boolean first = true;
        while (i < n) {
            char ch = text.charAt(i);
            if (!Character.isLetter(ch)) {
                out.append(ch);
                i++;
                continue;
            }
            int end = i;
            while (end < n && Character.isLetter(text.charAt(end))) end++;
            String word = text.substring(i, end);
            if (!first && rnd.nextDouble() < pauseChance && out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
                out.setLength(out.length() - 1);
                out.append("... ");
            }
            if (word.length() >= 2 && rnd.nextDouble() < wordChance) {
                int head = word.length() >= 4 && rnd.nextDouble() < 0.3 ? 2 : 1;
                String part = word.substring(0, head);
                int repeats = 1 + (rnd.nextDouble() < level * 0.6 ? 1 : 0);
                for (int r = 0; r < repeats; r++) out.append(part).append('-');
                out.append(r(part, word));
            } else {
                out.append(word);
            }
            first = false;
            i = end;
        }
        return out.toString();
    }

    /** Повтор в начале пишется как в слове, а само слово после дефиса — со строчной, если оно не всё заглавными. */
    private static String r(String part, String word) {
        if (word.equals(word.toUpperCase())) return word;
        return Character.toLowerCase(word.charAt(0)) + word.substring(1);
    }
}
