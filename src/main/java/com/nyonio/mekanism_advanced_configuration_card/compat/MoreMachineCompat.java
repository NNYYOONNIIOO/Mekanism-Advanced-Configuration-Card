package com.nyonio.mekanism_advanced_configuration_card.compat;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Locale;

public class MoreMachineCompat {
    public static final String MOD_ID = "mekceumoremachine";
    public static final String TIER_MACHINE_KEY = "mekanism_advanced_configuration_card_more_machine_tier";
    
    private static boolean moreMachineLoaded = false;
    private static Class<?> needRepeatTierUpgradeClass;
    private static Class<?> tierMachineClass;
    
    public static void init() {
        moreMachineLoaded = Loader.isModLoaded(MOD_ID);
        if (moreMachineLoaded) {
            try {
                needRepeatTierUpgradeClass = Class.forName("mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade");
            } catch (ClassNotFoundException e) {
            }
            try {
                tierMachineClass = Class.forName("mekceumoremachine.common.tile.interfaces.ITierMachine");
            } catch (ClassNotFoundException e) {
            }
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
        if (needRepeatTierUpgradeClass != null && needRepeatTierUpgradeClass.isInstance(tile)) {
            return true;
        }
        if (tierMachineClass != null && tierMachineClass.isInstance(tile)) {
            return true;
        }
        String className = tile.getClass().getName();
        return className.startsWith("mekceumoremachine.");
    }

    /**
     * MoreMachine uses a different TileEntity class for the tiered version of
     * a Mekanism machine. Configuration cards should still be transferable
     * between those two implementations when they represent the same machine.
     */
    public static boolean isEquivalentMachineType(Class<? extends TileEntity> storedType, TileEntity targetTile) {
        if (storedType == null || targetTile == null) {
            return false;
        }
        boolean storedIsMoreMachine = storedType.getName().startsWith("mekceumoremachine.");
        boolean targetIsMoreMachine = targetTile.getClass().getName().startsWith("mekceumoremachine.");
        if (storedIsMoreMachine == targetIsMoreMachine) {
            return false;
        }
        String storedFamily = getMachineFamily(storedType);
        String targetFamily = getMachineFamily(targetTile.getClass());
        return storedFamily != null && storedFamily.equals(targetFamily);
    }

    private static String getMachineFamily(Class<?> tileClass) {
        String name = tileClass.getSimpleName();
        if (name.startsWith("TileEntity")) {
            name = name.substring("TileEntity".length());
        }
        boolean changed;
        do {
            changed = false;
            String[] tierPrefixes = {"Basic", "Advanced", "Elite", "Ultimate", "Tier"};
            for (String prefix : tierPrefixes) {
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed && !name.isEmpty());
        if (name.isEmpty() || name.endsWith("Factory")) {
            return null;
        }
        return name.toLowerCase(Locale.ROOT);
    }
    
    public static int getTierOrdinal(TileEntity tile) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return -1;
        }
        try {
            if (needRepeatTierUpgradeClass != null && needRepeatTierUpgradeClass.isInstance(tile)) {
                Method getNowTierMethod = tile.getClass().getMethod("getNowTier");
                Object tier = getNowTierMethod.invoke(tile);
                if (tier != null) {
                    Method getBaseTierMethod = tier.getClass().getMethod("getBaseTier");
                    BaseTier baseTier = (BaseTier) getBaseTierMethod.invoke(tier);
                    if (baseTier != null) {
                        return baseTier.ordinal();
                    }
                }
            }
            if (tierMachineClass != null && tierMachineClass.isInstance(tile)) {
                Method getTierMethod = tile.getClass().getMethod("getTier");
                Object tier = getTierMethod.invoke(tile);
                if (tier != null) {
                    Method getBaseTierMethod = tier.getClass().getMethod("getBaseTier");
                    BaseTier baseTier = (BaseTier) getBaseTierMethod.invoke(tier);
                    if (baseTier != null) {
                        return baseTier.ordinal();
                    }
                }
            }
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            if (tag.hasKey("tier")) {
                int tierOrdinal = tag.getInteger("tier");
                if (tierOrdinal >= 0 && tierOrdinal < BaseTier.values().length) {
                    return tierOrdinal;
                }
            }
        } catch (Exception e) {
            MekConfigCardUpgradesMod.LOGGER.error("Error getting tier ordinal", e);
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
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(MOD_ID, "compositetierinstaller"));
    }
    
    public static Item getTierInstallerItem() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism", "tierinstaller"));
    }
    
    public static boolean upgradeToTier(TileEntity tile, int targetTierOrdinal) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return false;
        }
        try {
            int currentTierOrdinal = getTierOrdinal(tile);
            if (currentTierOrdinal >= targetTierOrdinal) {
                return true;
            }
            BaseTier targetTier = BaseTier.values()[targetTierOrdinal];
            Method upgradeMethod = tile.getClass().getMethod("upgrade", BaseTier.class);
            Object result = upgradeMethod.invoke(tile, targetTier);
            return result == null || Boolean.TRUE.equals(result);
        } catch (Exception e) {
            MekConfigCardUpgradesMod.LOGGER.error("Error upgrading tier", e);
        }
        return false;
    }
}
