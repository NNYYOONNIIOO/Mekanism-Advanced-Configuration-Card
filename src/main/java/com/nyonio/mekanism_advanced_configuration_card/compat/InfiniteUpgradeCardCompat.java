package com.nyonio.mekanism_advanced_configuration_card.compat;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

public class InfiniteUpgradeCardCompat {
    
    private static boolean loaded = false;
    private static Item infiniteUpgrade = null;
    private static Item superInfiniteUpgrade = null;
    private static Item infiniteFactoryInstaller = null;
    private static final int MAX_BAUBLES_SLOTS = 64;
    
    public static void init() {
        loaded = Loader.isModLoaded("infinite_upgrade_card");
        MekConfigCardUpgradesMod.LOGGER.info("InfiniteUpgradeCardCompat init, loaded: {}", loaded);
    }
    
    public static void postInit() {
        if (loaded) {
            try {
                infiniteUpgrade = Item.getByNameOrId("infinite_upgrade_card:infinite_upgrade");
                superInfiniteUpgrade = Item.getByNameOrId("infinite_upgrade_card:super_infinite_upgrade");
                infiniteFactoryInstaller = Item.getByNameOrId("infinite_upgrade_card:infinite_factory_installer");
                MekConfigCardUpgradesMod.LOGGER.info("InfiniteUpgradeCardCompat postInit - infiniteUpgrade: {}, superInfiniteUpgrade: {}, infiniteFactoryInstaller: {}", infiniteUpgrade, superInfiniteUpgrade, infiniteFactoryInstaller);
            } catch (Exception e) {
                MekConfigCardUpgradesMod.LOGGER.error("InfiniteUpgradeCardCompat postInit error", e);
            }
        }
    }
    
    public static boolean isInfiniteUpgradeCardLoaded() {
        return loaded;
    }
    
    public static Item getInfiniteUpgradeItem() {
        return infiniteUpgrade;
    }
    
    public static Item getSuperInfiniteUpgradeItem() {
        return superInfiniteUpgrade;
    }
    
    public static Item getInfiniteFactoryInstallerItem() {
        return infiniteFactoryInstaller;
    }
    
    public static boolean hasInfiniteUpgrade(EntityPlayer player) {
        return hasInfiniteUpgrade(player, null);
    }
    
    public static boolean hasInfiniteUpgrade(EntityPlayer player, ItemStack configCard) {
        if (!loaded) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        
        if (infiniteUpgrade != null) {
            if (hasItemInPlayerStorage(player, infiniteUpgrade)) {
                return true;
            }
            
            if (hasInfiniteUpgradeInNetwork(player)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasSuperInfiniteUpgrade(EntityPlayer player) {
        return hasSuperInfiniteUpgrade(player, null);
    }
    
    public static boolean hasSuperInfiniteUpgrade(EntityPlayer player, ItemStack configCard) {
        if (!loaded) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        
        if (superInfiniteUpgrade != null) {
            if (hasItemInPlayerStorage(player, superInfiniteUpgrade)) {
                return true;
            }
            
            if (hasSuperInfiniteUpgradeInNetwork(player)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasInfiniteFactoryInstaller(EntityPlayer player) {
        return hasInfiniteFactoryInstaller(player, null);
    }
    
    public static boolean hasInfiniteFactoryInstaller(EntityPlayer player, ItemStack configCard) {
        if (!loaded) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        
        if (infiniteFactoryInstaller != null) {
            if (hasItemInPlayerStorage(player, infiniteFactoryInstaller)) {
                return true;
            }
            
            if (hasInfiniteFactoryInstallerInNetwork(player)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean hasItemInPlayerStorage(EntityPlayer player, Item item) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        
        ItemStack target = new ItemStack(item);
        if (ItemCardSlotBag.countInBags(player.inventory, target) > 0) {
            return true;
        }
        
        if (BaublesCompat.isBaublesLoaded()) {
            for (int slot = 0; slot < MAX_BAUBLES_SLOTS; slot++) {
                ItemStack stack = BaublesCompat.getStackInSlot(player, slot);
                if (!stack.isEmpty() && stack.getItem() == item) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static boolean hasInfiniteUpgradeInNetwork(EntityPlayer player) {
        if (!AE2Compat.isAE2Loaded()) {
            return false;
        }
        Object storage = AE2Compat.getStorageGridFromPlayer(player);
        if (storage == null) {
            return false;
        }
        return AE2Compat.countItemInNetwork(storage, infiniteUpgrade, 0) > 0;
    }
    
    private static boolean hasSuperInfiniteUpgradeInNetwork(EntityPlayer player) {
        if (!AE2Compat.isAE2Loaded()) {
            return false;
        }
        Object storage = AE2Compat.getStorageGridFromPlayer(player);
        if (storage == null) {
            return false;
        }
        return AE2Compat.countItemInNetwork(storage, superInfiniteUpgrade, 0) > 0;
    }
    
    private static boolean hasInfiniteFactoryInstallerInNetwork(EntityPlayer player) {
        if (!AE2Compat.isAE2Loaded()) {
            return false;
        }
        Object storage = AE2Compat.getStorageGridFromPlayer(player);
        if (storage == null) {
            return false;
        }
        return AE2Compat.countItemInNetwork(storage, infiniteFactoryInstaller, 0) > 0;
    }
}
