package com.nyonio.mekanism_advanced_configuration_card;

import com.nyonio.mekanism_advanced_configuration_card.compat.AE2Compat;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCapabilityProvider;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompatHandler;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.InvTweaksCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.MoreMachineCompat;
import com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemAdvancedConfigurationCard;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = MekConfigCardUpgradesMod.MOD_ID, name = MekConfigCardUpgradesMod.NAME, version = MekConfigCardUpgradesMod.VERSION, dependencies = "required-after:mekanism;")
public class MekConfigCardUpgradesMod {
    public static final String MOD_ID = "mekanism_advanced_configuration_card";
    public static final String NAME = "Mekanism Advanced Configuration Card";
    public static final String VERSION = "1.0.3";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static MekConfigCardUpgradesMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event);
        BaublesCompat.init();
        MoreMachineCompat.init();
        InfiniteUpgradeCardCompat.init();
        InvTweaksCompat.init();
        AE2Compat.init();
        PacketHandler.init();
        ConfigCardUpgradeHelper.init();
        LOGGER.info("General Machinery Advanced Configuration Card loaded");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        BaublesCompatHandler.register(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        InfiniteUpgradeCardCompat.postInit();
    }

    @GameRegistry.ObjectHolder(MOD_ID)
    public static class Items {
        public static final Item CARD_SLOT_BAG = new ItemCardSlotBag();
        public static final Item ADVANCED_CONFIGURATION_CARD = new ItemAdvancedConfigurationCard();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            IForgeRegistry<Item> registry = event.getRegistry();
            registry.register(Items.CARD_SLOT_BAG.setRegistryName(MOD_ID, "card_slot_bag").setUnlocalizedName(MOD_ID + ".card_slot_bag"));
            registry.register(Items.ADVANCED_CONFIGURATION_CARD.setRegistryName(MOD_ID, "advanced_configuration_card").setUnlocalizedName(MOD_ID + ".advanced_configuration_card"));
        }

        @SubscribeEvent
        public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
            if (BaublesCompat.isBaublesLoaded()) {
                BaublesCapabilityProvider.attachCapability(event);
            }
        }
    }
}
