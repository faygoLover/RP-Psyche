package faygolover.rppsyche.server;

import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.core.Settings;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.Worn;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Полная тьма: свет у глаз почти ноль и ни у тебя, ни у игрока рядом нет света в руках или надетого в Curios
 * (налобный фонарь, лампа на поясе). Динамический свет (Sodium
 * Dynamic Lights, лучи фонарей) живёт только у клиента, поэтому сервер смотрит на предметы сам.
 */
public final class Lights {
    private Lights() {}

    public static final TagKey<Item> LIGHT_SOURCES = ItemTags.create(new ResourceLocation(RpPsyche.MODID, "light_sources"));

    /** Разобранный список {@link Settings#lightItems}: предмет → ключ NBT (null — светит всегда). */
    private static List<String> parsedFrom;
    private static Map<Item, String> parsed = Map.of();

    public static boolean inTotalDarkness(ServerPlayer p, Settings c) {
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        if (p.level().getMaxLocalRawBrightness(eye) > c.darkLight) return false;
        if (RpCoreAPI.carriesLight(p)) return false;
        double r2 = c.darkFriendLight * c.darkFriendLight;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o != p && !o.isSpectator() && o.distanceToSqr(p) <= r2 && RpCoreAPI.carriesLight(o)) return false;
        }
        return true;
    }

    /**
     * Свой список света ({@code light_items} в конфиге и тег {@code rppsyche:light_sources}): в руках или надето.
     * Зарегистрирован в RP Core как поставщик света ({@code RpPsyche}), поэтому его видят и другие моды; сама тьма
     * спрашивает общий ответ {@code RpCoreAPI.carriesLight} — в него входят и фонари RP Lights.
     */
    public static boolean carriesLight(LivingEntity e) {
        Settings c = Settings.get();
        return isLight(e.getMainHandItem(), c) || isLight(e.getOffhandItem(), c)
                || Worn.any(e, s -> isLight(s, c));
    }

    private static boolean isLight(ItemStack stack, Settings c) {
        if (stack.isEmpty()) return false;
        if (stack.is(LIGHT_SOURCES)) return true;
        Map<Item, String> map = items(c);
        if (!map.containsKey(stack.getItem())) return false;
        String key = map.get(stack.getItem());
        if (key == null) return true;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) return false;
        return tag.getBoolean(key) || tag.getInt(key) != 0;
    }

    private static Map<Item, String> items(Settings c) {
        if (parsedFrom == c.lightItems) return parsed;
        Map<Item, String> m = new HashMap<>();
        for (String s : c.lightItems) {
            String id = s.strip(), key = null;
            int brace = id.indexOf('{');
            if (brace > 0 && id.endsWith("}")) {
                key = id.substring(brace + 1, id.length() - 1).strip();
                id = id.substring(0, brace).strip();
                if (key.isEmpty()) key = null;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) continue;
            m.put(ForgeRegistries.ITEMS.getValue(rl), key);
        }
        parsed = m;
        parsedFrom = c.lightItems;
        return m;
    }
}
