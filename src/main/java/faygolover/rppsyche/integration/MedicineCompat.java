package faygolover.rppsyche.integration;

import faygolover.rpmedicine.core.DrugEffect;
import faygolover.rpmedicine.core.MedicalSettings;
import faygolover.rpmedicine.core.MedicalState;
import faygolover.rpmedicine.core.Substances;
import faygolover.rpmedicine.server.Medical;
import net.minecraft.world.entity.Entity;

/** RP Medicine (обязательный): боль, лежачие, опьянение, седация, ломка. Только чтение. */
public final class MedicineCompat {
    private MedicineCompat() {}

    /** Что тело говорит психике. {@code down}: 0 — на ногах, 1 — обморок, 2 — нокдаун, 3 — клиническая смерть. */
    public record Body(double pain, int down, double intoxication, double sedation, boolean withdrawal, double antipsychotic) {
        public static final Body NONE = new Body(0, 0, 0, 0, false, 0);
    }

    public static Body read(Entity e) {
        MedicalState m = Medical.state(e);
        if (m == null) return Body.NONE;
        boolean withdrawal;
        try {
            withdrawal = Substances.anyWithdrawal(m, MedicalSettings.get());
        } catch (RuntimeException ex) {
            withdrawal = false;
        }
        return new Body(m.pain, m.down.ordinal(), m.intoxication, m.effect(DrugEffect.SEDATION), withdrawal,
                m.effect(DrugEffect.ANTIPSYCHOTIC));
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
