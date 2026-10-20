package faygolover.rppsyche.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Звук «в голове» по кругу (сердцебиение, гул): у самого слушателя, громкость меняется плавно. */
final class LoopSound extends AbstractTickableSoundInstance {
    float target;

    LoopSound(SoundEvent sound, SoundSource source) {
        super(sound, source, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0001f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = this.y = this.z = 0;
    }

    @Override
    public void tick() {
        volume += (target - volume) * 0.1f;
        if (volume < 0.0005f && target <= 0) stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    void finish() {
        stop();
    }
}
