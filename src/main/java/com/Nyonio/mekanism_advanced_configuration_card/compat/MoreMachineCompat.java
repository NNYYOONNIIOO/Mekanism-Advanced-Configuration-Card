package com.Nyonio.mekanism_advanced_configuration_card.compat;

import mekanism.common.tier.BaseTier;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class MoreMachineCompat {
    public static final String MOD_ID = "mekceumoremachine";
    public static final String TIER_MACHINE_KEY = "mekanism_advanced_configuration_card_more_machine_tier";
    
    private static boolean moreMachineLoaded = false;
    private static Item compositeTierInstallerItem = null;
    private static Item tierInstallerItem = null;
    private static Class<?> tierMachineInterface = null;
    private static Class<?> needRepeatTierInterface = null;
    private static Class<?> tierUpgradeableInterface = null;
    private static Class<?> machineTierClass = null;
    
    public static void init() {
        try {
            Class.forName("mekceumoremachine.common.MEKCeuMoreMachine");
            moreMachineLoaded = true;
            tierMachineInterface = Class.forName("mekceumoremachine.common.tile.interfaces.ITierMachine");
            needRepeatTierInterface = Class.forName("mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade");
            tierUpgradeableInterface = Class.forName("mekanism.common.base.ITierUpgradeable");
            machineTierClass = Class.forName("mekceumoremachine.common.tier.MachineTier");
        } catch (ClassNotFoundException e) {
            moreMachineLoaded = false;
        }
    }
    
    public static boolean isMoreMachineLoaded() {
        return moreMachineLoaded;
    }
    
    public static boolean isTierMachine(TileEntity tile) {
        if (!moreMachineLoaded || tile == null) {
            return false;
        }
        if (tile instanceof TileEntityFactory) {
            return false;
        }
        if (tierMachineInterface != null && tierMachineInterface.isInstance(tile)) {
            return true;
        }
        if (needRepeatTierInterface != null && needRepeatTierInterface.isInstance(tile)) {
            return true;
        }
        return false;
    }
    
    public static int getTierOrdinal(TileEntity tile) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return -1;
        }
        try {
            if (tierMachineInterface != null && tierMachineInterface.isInstance(tile)) {
                java.lang.reflect.Method getTierMethod = tierMachineInterface.getMethod("getTier");
                Object tier = getTierMethod.invoke(tile);
                if (tier != null && machineTierClass != null) {
                    java.lang.reflect.Method getBaseTierMethod = machineTierClass.getMethod("getBaseTier");
                    Object baseTier = getBaseTierMethod.invoke(tier);
                    if (baseTier instanceof BaseTier) {
                        return ((BaseTier) baseTier).ordinal();
                    }
                }
            }
            if (needRepeatTierInterface != null && needRepeatTierInterface.isInstance(tile)) {
                java.lang.reflect.Method getNowTierMethod = needRepeatTierInterface.getMethod("getNowTier");
                Object tier = getNowTierMethod.invoke(tile);
                if (tier != null && machineTierClass != null) {
                    java.lang.reflect.Method getBaseTierMethod = machineTierClass.getMethod("getBaseTier");
                    Object baseTier = getBaseTierMethod.invoke(tier);
                    if (baseTier instanceof BaseTier) {
                        return ((BaseTier) baseTier).ordinal();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static NBTTagCompound saveTierData(TileEntity tile, NBTTagCompound data) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return data;
        }
        int tierOrdinal = getTierOrdinal(tile);
        if (tierOrdinal >= 0) {
            data.setInteger(TIER_MACHINE_KEY, tierOrdinal);
        }
        return data;
    }
    
    public static boolean hasTierData(NBTTagCompound data) {
        return data.hasKey(TIER_MACHINE_KEY);
    }
    
    public static int getStoredTier(NBTTagCompound data) {
        return data.getInteger(TIER_MACHINE_KEY);
    }
    
    public static Item getCompositeTierInstallerItem() {
        if (!moreMachineLoaded) {
            return null;
        }
        if (compositeTierInstallerItem == null) {
            try {
                compositeTierInstallerItem = ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.util.ResourceLocation(MOD_ID, "compositetierinstaller")
                );
            } catch (Exception e) {
            }
        }
        return compositeTierInstallerItem;
    }
    
    public static Item getTierInstallerItem() {
        if (tierInstallerItem == null) {
            try {
                tierInstallerItem = ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.util.ResourceLocation("mekanism", "tierinstaller")
                );
            } catch (Exception e) {
            }
        }
        return tierInstallerItem;
    }
    
    public static Item getTierInstallerItemForTier(BaseTier tier) {
        Item baseItem = getTierInstallerItem();
        if (baseItem == null) {
            return null;
        }
        return baseItem;
    }
    
    public static boolean canInstall(TileEntity tile) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return false;
        }
        if (tierUpgradeableInterface == null) {
            return false;
        }
        if (!tierUpgradeableInterface.isInstance(tile)) {
            return false;
        }
        try {
            for (java.lang.reflect.Method m : tierUpgradeableInterface.getMethods()) {
                if (m.getName().equals("CanInstalled")) {
                    Object result = m.invoke(tile);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    }
                }
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }
    
    public static boolean upgradeToTier(TileEntity tile, int nextTierOrdinal) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return false;
        }
        int currentOrdinal = getTierOrdinal(tile);
        if (currentOrdinal < 0) {
            return false;
        }
        BaseTier nextBaseTier = BaseTier.values()[nextTierOrdinal];
        try {
            java.lang.reflect.Method upgradeMethod = tile.getClass().getMethod("upgrade", BaseTier.class);
            Object result = upgradeMethod.invoke(tile, nextBaseTier);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean isFactoryToMoreMachineConversion(TileEntity sourceTile, TileEntity targetTile) {
        if (!moreMachineLoaded) {
            return false;
        }
        boolean sourceIsFactory = sourceTile instanceof TileEntityFactory;
        boolean targetIsTierMachine = isTierMachine(targetTile);
        return sourceIsFactory && targetIsTierMachine;
    }
    
    public static boolean isMoreMachineToFactoryConversion(TileEntity sourceTile, TileEntity targetTile) {
        if (!moreMachineLoaded) {
            return false;
        }
        boolean sourceIsTierMachine = isTierMachine(sourceTile);
        boolean targetIsFactory = targetTile instanceof TileEntityFactory;
        return sourceIsTierMachine && targetIsFactory;
    }
}
