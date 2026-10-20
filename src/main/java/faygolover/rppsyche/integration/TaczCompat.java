package faygolover.rppsyche.integration;

import com.tacz.guns.api.event.common.GunShootEvent;
import faygolover.rppsyche.core.Settings;
import faygolover.rppsyche.server.PsycheService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * TaCZ (необязательный): выстрел слышат все вокруг, а у кого пуля прошла у самой головы — подавление. Пулю не
 * ловим: от стрелка по направлению взгляда проводим луч и смотрим, близко ли он к голове и нет ли стены между.
 */
public final class TaczCompat {
    private TaczCompat() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TaczCompat::onShoot);
    }

    private static void onShoot(GunShootEvent e) {
        if (!e.getLogicalSide().isServer()) return;
        LivingEntity shooter = e.getShooter();
        if (shooter == null || !(shooter.level() instanceof ServerLevel level)) return;
        Settings c = Settings.get();
        Vec3 from = shooter.getEyePosition();
        Vec3 dir = shooter.getLookAngle().normalize();
        double r2 = c.shotRadius * c.shotRadius;
        for (ServerPlayer p : level.players()) {
            if (p == shooter || !PsycheService.affected(p)) continue;
            Vec3 head = p.getEyePosition();
            double d2 = head.distanceToSqr(from);
            if (d2 > r2) continue;
            double impulse = d2 <= c.shotCloseRadius * c.shotCloseRadius ? c.shotClose : c.shotFar;
            Vec3 v = head.subtract(from);
            double t = v.dot(dir);
            if (t > 0.5 && t < c.shotRadius) {
                Vec3 closest = from.add(dir.scale(t));
                double miss = closest.distanceTo(head);
                if (miss <= c.nearMissRadius && clear(level, from, closest, shooter)) {
                    impulse += c.nearMiss * (0.5 + 0.5 * (1 - miss / Math.max(0.01, c.nearMissRadius)));
                }
            }
            PsycheService.impulse(p, impulse);
        }
    }

    private static boolean clear(ServerLevel level, Vec3 from, Vec3 to, LivingEntity shooter) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)).getType()
                == HitResult.Type.MISS;
    }
}
