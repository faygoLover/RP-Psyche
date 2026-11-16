package faygolover.rppsyche.integration;

import faygolover.rpcore.api.BodyProvider;
import faygolover.rpcore.api.RpCoreAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Что тело говорит психике: боль, лежачие, опьянение, седация, ломка. Только чтение, через мод тела RP Core
 * ({@link BodyProvider}; сейчас его даёт RP Medicine). Без мода тела — «всё спокойно»: источников стресса меньше,
 * мод работает.
 */
public final class BodyCompat {
    private BodyCompat() {}

    /**
     * {@code down}: 0 — на ногах, 1 — обморок, 2 — нокдаун, 3 — клиническая смерть.
     * {@code craving} — зависим, ломки нет, но тянет (за полсуток до неё или после острой фазы); {@code former} — id
     * вещества, к которому бывшего зависимого тянет под стрессом (или null).
     */
    public record Body(double pain, int down, double intoxication, double sedation, boolean withdrawal, double antipsychotic,
                       boolean craving, String former) {
        public static final Body NONE = new Body(0, 0, 0, 0, false, 0, false, null);
    }

    public static Body read(Entity e) {
        BodyProvider b = RpCoreAPI.body();
        if (b == null || !(e instanceof Player p)) return Body.NONE;
        return new Body(b.pain(p), b.down(p).ordinal(), b.intoxication(p), b.sedation(p), b.withdrawal(p),
                b.antipsychotic(p), b.craving(p), b.formerAddiction(p));
    }

    /** Лежит (нокдаун, обморок, клиническая смерть, тело вышедшего игрока). */
    public static boolean isDown(Entity e) {
        BodyProvider b = RpCoreAPI.body();
        return b != null && b.down(e) != BodyProvider.DownState.NONE;
    }

    /** Клиническая смерть (тело вышедшего без состояния тоже — оно лежит как мёртвое). */
    public static boolean isClinical(Entity e) {
        BodyProvider b = RpCoreAPI.body();
        return b != null && b.down(e) == BodyProvider.DownState.CLINICAL;
    }
}
