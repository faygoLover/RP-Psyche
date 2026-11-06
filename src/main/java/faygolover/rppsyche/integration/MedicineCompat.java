package faygolover.rppsyche.integration;

import faygolover.rpmedicine.core.DrugEffect;
import faygolover.rpmedicine.core.MedicalSettings;
import faygolover.rpmedicine.core.MedicalState;
import faygolover.rpmedicine.core.Substance;
import faygolover.rpmedicine.core.Substances;
import faygolover.rpmedicine.server.Medical;
import net.minecraft.world.entity.Entity;

/** RP Medicine (обязательный): боль, лежачие, опьянение, седация, ломка. Только чтение. */
public final class MedicineCompat {
    private MedicineCompat() {}

    /**
     * Что тело говорит психике. {@code down}: 0 — на ногах, 1 — обморок, 2 — нокдаун, 3 — клиническая смерть.
     * {@code craving} — зависим, ломки нет, но тянет (за полсуток до неё или после острой фазы); {@code former} — id
     * вещества, к которому бывшего зависимого тянет под стрессом (или null).
     */
    public record Body(double pain, int down, double intoxication, double sedation, boolean withdrawal, double antipsychotic,
                       boolean craving, String former) {
        public static final Body NONE = new Body(0, 0, 0, 0, false, 0, false, null);
    }

    public static Body read(Entity e) {
        MedicalState m = Medical.state(e);
        if (m == null) return Body.NONE;
        boolean withdrawal, craving = false;
        String former = null;
        try {
            MedicalSettings ms = MedicalSettings.get();
            withdrawal = Substances.anyWithdrawal(m, ms);
            if (!m.timePaused) {
                for (Substance x : Substance.VALUES) {
                    if (Substances.dependent(m, x) && !Substances.withdrawal(m, x, ms)
                            && (Substances.craving(m, x, ms) || m.sinceDose[x.ordinal()] >= Substances.onsetSeconds(x, ms) / 2)) craving = true;
                    if (former == null && Substances.formerAddict(m, x)) former = x.id;
                }
            }
        } catch (RuntimeException | LinkageError ex) {
            withdrawal = false;
        }
        return new Body(m.pain, m.down.ordinal(), m.intoxication, m.effect(DrugEffect.SEDATION), withdrawal,
                m.effect(DrugEffect.ANTIPSYCHOTIC), craving, former);
    }

    /** Лежит (нокдаун, обморок, клиническая смерть, тело вышедшего игрока). */
    public static boolean isDown(Entity e) {
        return Medical.isDown(e);
    }

    /** Клиническая смерть (тело вышедшего тоже — оно лежит как мёртвое). */
    public static boolean isClinical(Entity e) {
        MedicalState m = Medical.state(e);
        if (m != null) return m.down == MedicalState.Down.CLINICAL;
        return Medical.isDown(e);
    }
}
