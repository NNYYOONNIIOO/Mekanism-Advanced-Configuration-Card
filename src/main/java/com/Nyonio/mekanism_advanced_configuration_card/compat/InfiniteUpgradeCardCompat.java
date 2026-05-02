package com.Nyonio.mekanism_advanced_configuration_card.compat;

import com.Nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

public class InfiniteUpgradeCardCompat {
    
    private static boolean loaded = false;
    private static Item infiniteUpgrade = null;
    private static Item superInfiniteUpgrade = null;
    private static Item infiniteFactoryInstaller = null;
    
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
        if (!loaded) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        
        if (infiniteUpgrade != null) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == infiniteUpgrade) {
                    MekConfigCardUpgradesMod.LOGGER.info("Found infinite_upgrade in inventory at slot {}", i);
                    return true;
                }
            }
            
            ItemStack infiniteStack = new ItemStack(infiniteUpgrade);
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, infiniteStack);
            if (countInBags > 0) {
                MekConfigCardUpgradesMod.LOGGER.info("Found infinite_upgrade in card slot bag");
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasSuperInfiniteUpgrade(EntityPlayer player) {
        if (!loaded) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        
        if (superInfiniteUpgrade != null) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == superInfiniteUpgrade) {
                    MekConfigCardUpgradesMod.LOGGER.info("Found super_infinite_upgrade in inventory at slot {}", i);
                    return true;
                }
            }
            
            ItemStack superInfiniteStack = new ItemStack(superInfiniteUpgrade);
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, superInfiniteStack);
            if (countInBags > 0) {
                MekConfigCardUpgradesMod.LOGGER.info("Found super_infinite_upgrade in card slot bag");
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasAnyInfiniteUpgrade(EntityPlayer player) {
        boolean result = hasInfiniteUpgrade(player) || hasSuperInfiniteUpgrade(player);
        MekConfigCardUpgradesMod.LOGGER.info("hasAnyInfiniteUpgrade result: {}", result);
        return result;
    }
    
    public static boolean hasInfiniteFactoryInstaller(EntityPlayer player) {
        if (!loaded) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        
        if (infiniteFactoryInstaller != null) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == infiniteFactoryInstaller) {
                    MekConfigCardUpgradesMod.LOGGER.info("Found infinite_factory_installer in inventory at slot {}", i);
                    return true;
                }
            }
            
            ItemStack infiniteFactoryInstallerStack = new ItemStack(infiniteFactoryInstaller);
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, infiniteFactoryInstallerStack);
            if (countInBags > 0) {
                MekConfigCardUpgradesMod.LOGGER.info("Found infinite_factory_installer in card slot bag");
                return true;
            }
        }
        
        return false;
    }
}
