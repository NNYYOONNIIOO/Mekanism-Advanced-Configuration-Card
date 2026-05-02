package com.Nyonio.mekanism_advanced_configuration_card.compat;

import baubles.api.BaubleTypeEx;
import baubles.api.registries.ItemData;
import baubles.api.registries.TypeData;
import baubles.lib.util.ItemQuery;
import com.Nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class BaublesCompatHandler {
    
    public static void register(FMLInitializationEvent event) {
        try {
            BaubleTypeEx bodyType = TypeData.Preset.BODY;
            
            Item cardSlotBagItem = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation(MekConfigCardUpgradesMod.MOD_ID, "card_slot_bag"));
            if (cardSlotBagItem == null) {
                return;
            }
            
            ItemQuery itemQuery = ItemQuery.of(cardSlotBagItem);
            ItemData.registerBauble(itemQuery, bodyType);
            
        } catch (NoClassDefFoundError | Exception e) {
        }
    }
}
