package faygolover.rppsyche.integration;

import faygolover.zoneartifacts.anomaly.AnomalyGeometry;
import faygolover.zoneartifacts.anomaly.AnomalyInstance;
import faygolover.zoneartifacts.anomaly.AnomalySavedData;
import faygolover.zoneartifacts.anomaly.AnomalyTypeIds;
import net.minecraft.server.level.ServerPlayer;

/** Zone Artifacts (необязательный): в Пси-зоне ли игрок. Класс грузится, только если мод стоит. */
public final class ZaCompat {
    private ZaCompat() {}

    /** Сила Пси-зоны вокруг игрока: 0 — вне зоны, 1 — обычная (сила аномалии 3 по умолчанию ZA). */
    public static double psi(ServerPlayer p) {
        double best = 0;
        for (AnomalyInstance a : AnomalySavedData.get(p.serverLevel()).instances()) {
            if (!a.enabled() || !AnomalyTypeIds.PSI.equals(a.typeId())) continue;
            if (AnomalyGeometry.zoneAabb(a).contains(p.getEyePosition())) best = Math.max(best, Math.max(1, a.intensity()) / 3.0);
        }
        return best;
    }
}
