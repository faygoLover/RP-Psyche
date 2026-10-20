package faygolover.rppsyche.client;

import faygolover.rppsyche.core.Psyche;
import faygolover.rppsyche.registry.PsycheSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

/**
 * Звуки, которых нет: только у своего клиента, никто больше их не слышит.
 * <ul>
 *   <li>сердцебиение — от стресса; гул — от плохого рассудка (плавно);</li>
 *   <li>шаги за спиной по тому, что под ними, — с первой стадии рассудка;</li>
 *   <li>двери, сундуки, шорох рядом — со второй;</li>
 *   <li>далёкие выстрелы очередью — только на Безумии.</li>
 * </ul>
 */
final class PhantomSounds {
    private PhantomSounds() {}

    private static final RandomSource RND = RandomSource.create();
    private static final SoundEvent[] MISC = {
            SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE,
            SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.BARREL_OPEN, SoundEvents.LADDER_STEP, SoundEvents.GRAVEL_STEP,
            SoundEvents.IRON_DOOR_CLOSE, SoundEvents.WOOD_BREAK};

    private static LoopSound heartbeat, hum;
    private static int stepTimer = 1200, miscTimer = 1800, shotTimer = 2400;
    /** Идут шаги: осталось шагов, тиков до следующего, где и каким звуком. */
    private static int steps, stepDelay;
    private static BlockPos stepPos;
    private static SoundType stepType;
    /** Идёт очередь далёких выстрелов. */
    private static int shots, shotDelay;
    private static Vec3 shotFrom;
    private static float shotPitch;

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        boolean on = ClientPsyche.active();
        float stress = ClientPsyche.stress(), sanity = ClientPsyche.sanity();
        heartbeat = loop(mc, heartbeat, PsycheSounds.HEARTBEAT.get(), SoundSource.PLAYERS,
                on ? (float) Psyche.ramp(stress, 55, 90) * 0.9f : 0);
        hum = loop(mc, hum, PsycheSounds.HUM.get(), SoundSource.AMBIENT,
                on ? (float) Psyche.ramp(45 - sanity, 0, 35) * 0.6f : 0);
        if (!on) {
            steps = shots = 0;
            return;
        }
        int stage = Psyche.sanityStage(sanity);

        if (stage >= 1 && --stepTimer <= 0) {
            stepTimer = interval(stage, 2400, 1200, 800, 500);
            stepPos = behind(p, 2 + RND.nextInt(2));
            stepType = p.level().getBlockState(stepPos.below()).getSoundType();
            if (stepType == SoundType.EMPTY) stepType = p.level().getBlockState(p.blockPosition().below()).getSoundType();
            steps = 3 + RND.nextInt(3);
            stepDelay = 0;
        }
        if (steps > 0 && --stepDelay <= 0) {
            p.level().playLocalSound(stepPos, stepType.getStepSound(), SoundSource.PLAYERS,
                    stepType.getVolume() * 0.35f, stepType.getPitch(), false);
            steps--;
            stepDelay = 7;
        }

        if (stage >= 2 && --miscTimer <= 0) {
            miscTimer = interval(stage, 0, 2400, 1400, 900);
            BlockPos at = behind(p, 4 + RND.nextInt(5));
            SoundEvent s = RND.nextInt(5) == 0 ? PsycheSounds.WHOOSH.get() : MISC[RND.nextInt(MISC.length)];
            p.level().playLocalSound(at, s, SoundSource.BLOCKS, 0.6f, 0.8f + RND.nextFloat() * 0.2f, false);
        }

        if (stage >= 4 && --shotTimer <= 0) {
            shotTimer = 1800 + RND.nextInt(1800);
            double a = RND.nextDouble() * Math.PI * 2;
            shotFrom = p.position().add(Math.cos(a) * 14, 2 + RND.nextInt(4), Math.sin(a) * 14);
            shots = 1 + RND.nextInt(6);
            shotDelay = 0;
            shotPitch = 0.85f + RND.nextFloat() * 0.15f;
        }
        if (shots > 0 && --shotDelay <= 0) {
            // Громкость больше 1 — дальше слышно (затухание 16 × громкость): с 14 блоков звучит как издалека.
            p.level().playLocalSound(shotFrom.x, shotFrom.y, shotFrom.z, PsycheSounds.DISTANT_SHOT.get(), SoundSource.PLAYERS,
                    1.5f, shotPitch, false);
            shots--;
            shotDelay = 3 + RND.nextInt(8);
        }
    }

    static void stop() {
        if (heartbeat != null) heartbeat.finish();
        if (hum != null) hum.finish();
        heartbeat = hum = null;
        steps = shots = 0;
    }

    /** Интервал, тиков, для стадии рассудка: от {@code base} до двух {@code base}. */
    private static int interval(int stage, int s1, int s2, int s3, int s4) {
        int base = switch (stage) {
            case 1 -> s1;
            case 2 -> s2;
            case 3 -> s3;
            default -> s4;
        };
        base = Math.max(200, base);
        return base + RND.nextInt(base);
    }

    /** Точка за спиной на {@code dist} блоков (чуть вбок). */
    private static BlockPos behind(LocalPlayer p, int dist) {
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        if (back.lengthSqr() < 1e-4) back = new Vec3(1, 0, 0);
        back = back.normalize().yRot((RND.nextFloat() - 0.5f) * 0.8f).scale(dist);
        return BlockPos.containing(p.position().add(back));
    }

    private static LoopSound loop(Minecraft mc, LoopSound s, SoundEvent ev, SoundSource src, float target) {
        if (target > 0.01f && (s == null || s.isStopped())) {
            s = new LoopSound(ev, src);
            mc.getSoundManager().play(s);
        }
        if (s != null) {
            s.target = target;
            if (s.isStopped()) s = null;
        }
        return s;
    }
}
