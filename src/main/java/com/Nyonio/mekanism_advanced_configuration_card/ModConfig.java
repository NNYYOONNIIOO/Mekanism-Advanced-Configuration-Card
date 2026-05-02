package com.Nyonio.mekanism_advanced_configuration_card;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = MekConfigCardUpgradesMod.MOD_ID)
public class ModConfig {
    
    @Config.Name("Prioritize Card Slot Bag for Upgrades")
    @Config.Comment("When returning upgrades, prioritize inserting them into card slot bags before regular inventory.")
    public static boolean prioritizeCardSlotBag = true;
    
    @Config.Name("Limit Saved Upgrade Count")
    @Config.Comment("When enabled, the upgrade count saved to config card is limited to the maximum installable amount (e.g., 8 speed upgrades). When disabled, allows saving and pasting more than the maximum.")
    public static boolean limitSavedUpgradeCount = true;
    
    @Mod.EventBusSubscriber(modid = MekConfigCardUpgradesMod.MOD_ID)
    private static class ConfigHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(MekConfigCardUpgradesMod.MOD_ID)) {
                ConfigManager.sync(MekConfigCardUpgradesMod.MOD_ID, net.minecraftforge.common.config.Config.Type.INSTANCE);
            }
        }
    }
}
