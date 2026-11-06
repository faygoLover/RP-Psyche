package faygolover.rppsyche.server;

import faygolover.rppsyche.core.Settings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Рация в руке ({@link Settings#voiceRadioItems}, Simple Radio): голос уходит через неё, а не напрямую игрокам, и
 * Simple Voice Chat не знает, кто услышал. Говорит в рацию — значит, говорит с кем-то, а не сам с собой.
 */
public final class Radios {
    private Radios() {}

    private static List<String> parsedFrom;
    private static Set<ResourceLocation> parsed = Set.of();

    public static boolean holds(ServerPlayer p, Settings c) {
        return isRadio(p.getMainHandItem(), c) || isRadio(p.getOffhandItem(), c);
    }

    private static boolean isRadio(ItemStack stack, Settings c) {
        if (stack.isEmpty()) return false;
        if (parsedFrom != c.voiceRadioItems) {
            parsed = c.voiceRadioItems.stream().map(s -> ResourceLocation.tryParse(s.strip()))
                    .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
            parsedFrom = c.voiceRadioItems;
        }
        return parsed.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
