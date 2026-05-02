package com.Nyonio.mekanism_advanced_configuration_card.client;

import com.Nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MekConfigCardUpgradesMod.MOD_ID)
public class ModelRegistrationHandler {
    
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(MekConfigCardUpgradesMod.Items.CARD_SLOT_BAG);
        registerModel(MekConfigCardUpgradesMod.Items.ADVANCED_CONFIGURATION_CARD);
    }
    
    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, 
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
