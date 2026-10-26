package faygolover.rppsyche;

import com.mojang.logging.LogUtils;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rppsyche.capability.PsycheData;
import faygolover.rppsyche.config.PsycheConfig;
import faygolover.rppsyche.network.Net;
import faygolover.rppsyche.registry.PsycheSounds;
import faygolover.rppsyche.server.PsycheCommands;
import faygolover.rppsyche.server.PsycheService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * RP Psyche — стресс и рассудок, аддон к RP Medicine (план — {@code docs/00_plan.md}). Две шкалы: стресс гибкий,
 * рассудок инертный. Темнота, одиночество, бой, раны и аномалии давят; люди, свет, сон и еда помогают. Игрок видит
 * не цифры, а ощущения: дрожь, сердцебиение, выцветший мир, звуки за спиной, мысли.
 */
@Mod(RpPsyche.MODID)
public class RpPsyche {
    public static final String MODID = "rppsyche";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpPsyche() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PsycheConfig.SPEC);
        PsycheSounds.SOUNDS.register(modBus);
        faygolover.rppsyche.registry.PsycheItems.ITEMS.register(modBus);
        modBus.addListener(faygolover.rppsyche.registry.PsycheItems::onCreativeTab);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCaps);
        modBus.addListener(this::onConfig);
        modBus.addListener(this::onConfigReload);

        var bus = MinecraftForge.EVENT_BUS;
        bus.addGenericListener(Entity.class, this::attachCaps);
        bus.addListener(PsycheCommands::register);
        bus.addListener(PsycheService::onPlayerTick);
        // Удар читаем раньше всех: RP Medicine потом превращает урон в травмы.
        bus.addListener(EventPriority.HIGHEST, PsycheService::onHurt);
        bus.addListener(PsycheService::onExplosion);
        bus.addListener(PsycheService::onWake);
        bus.addListener(PsycheService::onFoodEaten);
        bus.addListener(PsycheService::onUseFinish);
        bus.addListener(PsycheService::onRightClick);
        bus.addListener(PsycheService::onConsume);
        bus.addListener(PsycheService::onEffectAdded);
        bus.addListener(PsycheService::onEntityInteract);
        bus.addListener(PsycheService::onMood);
        bus.addListener(faygolover.rppsyche.server.Therapy::onServerTick);
        bus.addListener(faygolover.rppsyche.server.Therapy::onLogout);
        bus.addListener(PsycheService::onClone);
        bus.addListener(PsycheService::onLogin);
        bus.addListener(PsycheService::onRespawn);
        bus.addListener(PsycheService::onChangeDimension);
        bus.addListener(PsycheService::onLogout);
        bus.addListener(PsycheService::onServerStopped);
        if (ModList.get().isLoaded("tacz")) faygolover.rppsyche.integration.TaczCompat.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> faygolover.rppsyche.client.ClientSetup.init(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        Net.register();
        e.enqueueWork(() -> {
            PsycheService.init();
            RpCoreAPI.registerModule(MODID, "психика: стресс и рассудок");
            RpCoreAPI.registerSpeechFilter(new ResourceLocation(MODID, "stutter"), PsycheService::filterSpeech);
            PsycheCommands.describe();
        });
    }

    private void registerCaps(RegisterCapabilitiesEvent e) {
        e.register(PsycheData.class);
    }

    private void attachCaps(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) e.addCapability(PsycheData.ID, new PsycheData.Provider());
    }

    private void onConfig(ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() == PsycheConfig.SPEC) PsycheConfig.bake();
    }

    private void onConfigReload(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == PsycheConfig.SPEC) PsycheConfig.bake();
    }
}
