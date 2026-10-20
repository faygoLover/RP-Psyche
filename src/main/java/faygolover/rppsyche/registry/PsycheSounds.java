package faygolover.rppsyche.registry;

import faygolover.rppsyche.RpPsyche;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Звуки психики: сердцебиение, гул, шорох; далёкие выстрелы (из пака TaCZ по умолчанию). */
public final class PsycheSounds {
    private PsycheSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RpPsyche.MODID);

    public static final RegistryObject<SoundEvent> HEARTBEAT = sound("heartbeat");
    public static final RegistryObject<SoundEvent> HUM = sound("hum");
    public static final RegistryObject<SoundEvent> WHOOSH = sound("whoosh");
    public static final RegistryObject<SoundEvent> DISTANT_SHOT = sound("distant_shot");

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RpPsyche.MODID, name)));
    }
}
