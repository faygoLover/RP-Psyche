package faygolover.rppsyche.registry;

import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.item.PsychologistJournalItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Предметы психики. Выдаёт ГМ: только во вкладке «ГМские штуки» RP Core, без рецептов. */
public final class PsycheItems {
    private PsycheItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RpPsyche.MODID);

    public static final RegistryObject<Item> PSYCHOLOGIST_JOURNAL = ITEMS.register("psychologist_journal",
            () -> new PsychologistJournalItem(new Item.Properties().stacksTo(1)));

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == RpCoreAPI.MISC_TAB) e.accept(PSYCHOLOGIST_JOURNAL.get());
    }
}
