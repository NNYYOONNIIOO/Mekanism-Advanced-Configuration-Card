package com.nyonio.mekanism_advanced_configuration_card.compat;

import mekanism.common.Upgrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

public class AE2Compat {
    public static final String MOD_ID = "appliedenergistics2";
    
    private static boolean ae2Loaded = false;
    
    public static void init() {
        ae2Loaded = Loader.isModLoaded(MOD_ID);
    }
    
    public static boolean isAE2Loaded() {
        return ae2Loaded;
    }

    /**
     * Get the AE2 storage grid from the player's wireless terminal.
     * Scans the player's inventory (including Baubles) for a wireless terminal
     * and uses its encryption key to connect to the AE2 network.
     * 
     * @param player The player whose wireless terminal to use
     * @return Object representing IStorageGrid, or null if not available
     */
    public static Object getStorageGridFromPlayer(EntityPlayer player) {
        if (!ae2Loaded || player == null) {
            return null;
        }
        return AE2CompatImpl.getStorageGridFromPlayer(player);
    }
    
    /**
     * Get the AE2 action source from the player's wireless terminal.
     * Returns a PlayerSource with the wireless terminal as the host.
     * 
     * @param player The player whose wireless terminal to use
     * @return Object representing IActionSource, or null if not available
     */
    public static Object createActionSourceFromPlayer(EntityPlayer player) {
        if (!ae2Loaded || player == null) {
            return null;
        }
        return AE2CompatImpl.createActionSourceFromPlayer(player);
    }
    
    public static long countItemInNetwork(Object storageHandle, Item item, int metadata) {
        if (!ae2Loaded || storageHandle == null || item == null) {
            return 0;
        }
        return AE2CompatImpl.countItemInNetwork(storageHandle, item, metadata);
    }
    
    public static ItemStack extractItemFromNetwork(Object storageHandle, Item item, int metadata, int count, Object sourceHandle) {
        if (!ae2Loaded || storageHandle == null || item == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        return AE2CompatImpl.extractItemFromNetwork(storageHandle, item, metadata, count, sourceHandle);
    }
    
    public static int insertItemToNetwork(Object storageHandle, ItemStack stack, Object sourceHandle) {
        if (!ae2Loaded || storageHandle == null || stack.isEmpty()) {
            return 0;
        }
        return AE2CompatImpl.insertItemToNetwork(storageHandle, stack, sourceHandle);
    }
    
    public static ItemStack extractUpgradeFromNetwork(Object storageHandle, Upgrade upgrade, int count, Object sourceHandle) {
        if (!ae2Loaded || storageHandle == null || upgrade == null) {
            return ItemStack.EMPTY;
        }
        ItemStack upgradeStack = upgrade.getStack();
        return extractItemFromNetwork(storageHandle, upgradeStack.getItem(), upgradeStack.getMetadata(), count, sourceHandle);
    }
}
