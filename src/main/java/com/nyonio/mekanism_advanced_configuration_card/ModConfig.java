package com.nyonio.mekanism_advanced_configuration_card;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Config(modid = MekConfigCardUpgradesMod.MOD_ID)
public class ModConfig {
    
    public enum SourcePriority {
        NETWORK,
        CARD_SLOT_BAG,
        PLAYER_INVENTORY
    }
    
    @Config.Name("Upgrade Source Priority")
    @Config.Comment("Priority order for getting upgrades when pasting. Available: NETWORK, CARD_SLOT_BAG, PLAYER_INVENTORY. Higher priority sources are used first.")
    @Config.RequiresMcRestart
    public static String[] upgradeSourcePriority = {"CARD_SLOT_BAG", "NETWORK", "PLAYER_INVENTORY"};
    
    @Config.Name("Upgrade Return Priority")
    @Config.Comment("Priority order for returning upgrades when removing. Available: NETWORK, CARD_SLOT_BAG, PLAYER_INVENTORY. Higher priority sources receive upgrades first.")
    @Config.RequiresMcRestart
    public static String[] upgradeReturnPriority = {"CARD_SLOT_BAG", "NETWORK", "PLAYER_INVENTORY"};
    
    @Config.Name("Limit Saved Upgrade Count")
    @Config.Comment("When enabled, the upgrade count saved to config card is limited to the maximum installable amount (e.g., 8 speed upgrades). When disabled, allows saving and pasting more than the maximum.")
    public static boolean limitSavedUpgradeCount = true;
    
    @Config.Name("Max Network Item Count")
    @Config.Comment("Maximum number of items to consider from AE2 network when checking availability. Set to -1 for unlimited (Long.MAX_VALUE). Default is Integer.MAX_VALUE (2147483647).")
    public static String maxNetworkItemCount = "2147483647";
    
    @Config.Name("Card Slot Bag Stack Limit")
    @Config.Comment("Maximum stack size per slot in the card slot bag. Default is 8. Set to -1 for unlimited (Integer.MAX_VALUE).")
    public static int cardSlotBagStackLimit = 8;
    
    private static File configFile;
    
    public static void init(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        cleanInvalidConfigEntries();
    }
    
    private static void cleanInvalidConfigEntries() {
        if (configFile == null || !configFile.exists()) {
            return;
        }
        
        Configuration config = new Configuration(configFile);
        try {
            config.load();
            
            boolean changed = false;
            
            String[] sourcePriority = config.getStringList("Upgrade Source Priority", "general", 
                new String[]{"CARD_SLOT_BAG", "NETWORK", "PLAYER_INVENTORY"}, 
                "Priority order for getting upgrades when pasting. Available: NETWORK, CARD_SLOT_BAG, PLAYER_INVENTORY. Higher priority sources are used first.");
            List<String> validSourcePriority = filterValidPriorities(sourcePriority);
            if (validSourcePriority.size() != sourcePriority.length) {
                config.get("general", "Upgrade Source Priority", new String[]{"CARD_SLOT_BAG", "NETWORK", "PLAYER_INVENTORY"}).set(validSourcePriority.toArray(new String[0]));
                changed = true;
            }
            
            String[] returnPriority = config.getStringList("Upgrade Return Priority", "general", 
                new String[]{"CARD_SLOT_BAG", "NETWORK", "PLAYER_INVENTORY"}, 
                "Priority order for returning upgrades when removing. Available: NETWORK, CARD_SLOT_BAG, PLAYER_INVENTORY. Higher priority sources receive upgrades first.");
            List<String> validReturnPriority = filterValidPriorities(returnPriority);
            if (validReturnPriority.size() != returnPriority.length) {
                config.get("general", "Upgrade Return Priority", new String[]{"CARD_SLOT_BAG", "NETWORK", "PLAYER_INVENTORY"}).set(validReturnPriority.toArray(new String[0]));
                changed = true;
            }
            
            boolean limitSavedCount = config.getBoolean("Limit Saved Upgrade Count", "general", true,
                "When enabled, the upgrade count saved to config card is limited to the maximum installable amount (e.g., 8 speed upgrades). When disabled, allows saving and pasting more than the maximum.");
            
            String maxCount = config.getString("Max Network Item Count", "general", "2147483647",
                "Maximum number of items to consider from AE2 network when checking availability. Set to -1 for unlimited (Long.MAX_VALUE). Default is Integer.MAX_VALUE (2147483647).");
            if (!isValidMaxNetworkItemCount(maxCount)) {
                config.get("general", "Max Network Item Count", "2147483647").set("2147483647");
                changed = true;
            }
            
            int bagStackLimit = config.getInt("Card Slot Bag Stack Limit", "general", 8, -1, Integer.MAX_VALUE,
                "Maximum stack size per slot in the card slot bag. Default is 8. Set to -1 for unlimited (Integer.MAX_VALUE).");
            if (bagStackLimit < -1 || bagStackLimit == 0) {
                config.get("general", "Card Slot Bag Stack Limit", 8).set(8);
                changed = true;
            }
            
            if (changed) {
                config.save();
                ConfigManager.sync(MekConfigCardUpgradesMod.MOD_ID, Config.Type.INSTANCE);
            }
        } finally {
            config.save();
        }
    }
    
    private static List<String> filterValidPriorities(String[] priorities) {
        List<String> valid = new ArrayList<>();
        for (String s : priorities) {
            try {
                SourcePriority.valueOf(s.toUpperCase().trim());
                valid.add(s.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
            }
        }
        if (valid.isEmpty()) {
            valid.add("CARD_SLOT_BAG");
            valid.add("NETWORK");
            valid.add("PLAYER_INVENTORY");
        }
        return valid;
    }
    
    private static boolean isValidMaxNetworkItemCount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            long v = Long.parseLong(value.trim());
            return v == -1 || v > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static long getMaxNetworkItemCount() {
        try {
            long value = Long.parseLong(maxNetworkItemCount.trim());
            if (value == -1) {
                return Long.MAX_VALUE;
            }
            return value;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
    
    public static int getCardSlotBagStackLimit() {
        if (cardSlotBagStackLimit < 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, cardSlotBagStackLimit);
    }
    
    public static java.util.List<SourcePriority> getUpgradeSourcePriorityList() {
        java.util.List<SourcePriority> list = new java.util.ArrayList<>();
        for (String s : upgradeSourcePriority) {
            try {
                list.add(SourcePriority.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
            }
        }
        if (list.isEmpty()) {
            list.add(SourcePriority.CARD_SLOT_BAG);
            list.add(SourcePriority.NETWORK);
            list.add(SourcePriority.PLAYER_INVENTORY);
        }
        return list;
    }
    
    public static java.util.List<SourcePriority> getUpgradeReturnPriorityList() {
        java.util.List<SourcePriority> list = new java.util.ArrayList<>();
        for (String s : upgradeReturnPriority) {
            try {
                list.add(SourcePriority.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
            }
        }
        if (list.isEmpty()) {
            list.add(SourcePriority.CARD_SLOT_BAG);
            list.add(SourcePriority.NETWORK);
            list.add(SourcePriority.PLAYER_INVENTORY);
        }
        return list;
    }
    
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
