package com.nyonio.mekanism_advanced_configuration_card.compat;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import mekanism.common.base.IUpgradeableTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.block.state.IBlockState;
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
    private static boolean adapterRegistrationAttempted;
    
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

    public static boolean isUpgradeable(TileEntity tile) {
        return tile instanceof IUpgradeableTile || findTileUpgradeAdapter(tile) != null;
    }

    /**
     * A card copied from a normal Mekanism machine has no MoreMachine tier
     * field. When the target is still the normal machine implementation, the
     * first BASIC tier conversion must nevertheless be inferred from the
     * registered MoreMachine adapter.
     */
    public static boolean canConvertToMoreMachine(Class<? extends TileEntity> storedType, TileEntity targetTile) {
        if (!moreMachineLoaded || storedType == null || targetTile == null || isTierMachine(targetTile)) {
            return false;
        }
        String storedFamily = getMachineFamily(storedType);
        String targetFamily = getMachineFamily(targetTile.getClass());
        if (storedFamily == null || !storedFamily.equals(targetFamily)) {
            return false;
        }
        return getUpgradeData(targetTile, BaseTier.BASIC) != null && getUpgradeResult(targetTile, BaseTier.BASIC) != null;
    }

    public static IUpgradeData getUpgradeData(TileEntity tile, BaseTier tier) {
        Object adapter = findTileUpgradeAdapter(tile);
        Object data = invokeAdapter(adapter, "getUpgradeData", tier);
        if (data instanceof IUpgradeData) {
            return (IUpgradeData) data;
        }
        if (tile instanceof IUpgradeableTile) {
            return ((IUpgradeableTile) tile).getUpgradeData(tier);
        }
        return null;
    }

    public static IBlockState getUpgradeResult(TileEntity tile, BaseTier tier) {
        Object adapter = findTileUpgradeAdapter(tile);
        Object result = invokeAdapter(adapter, "getUpgradeResult", tier);
        if (result instanceof IBlockState) {
            return (IBlockState) result;
        }
        if (tile instanceof IUpgradeableTile) {
            return ((IUpgradeableTile) tile).getUpgradeResult(tier);
        }
        return null;
    }

    /**
     * Calls MoreMachine's optional replacement helper without linking this
     * mod to MoreMachine at class-load time.
     */
    public static boolean replaceTileForUpgrade(TileEntity sourceTile, IBlockState targetState, IUpgradeData upgradeData) {
        if (!moreMachineLoaded || sourceTile == null || targetState == null || upgradeData == null) {
            return false;
        }
        try {
            Class<?> helper = Class.forName("mekceumoremachine.common.util.MEKCeuMoreMachineUpgradeUtils");
            Method replace = helper.getMethod("replaceTileForUpgrade", TileEntity.class, IBlockState.class, IUpgradeData.class);
            return Boolean.TRUE.equals(replace.invoke(null, sourceTile, targetState, upgradeData));
        } catch (Exception e) {
            MekConfigCardUpgradesMod.LOGGER.error("Error replacing tile for MoreMachine upgrade", e);
            return false;
        }
    }

    private static Object findTileUpgradeAdapter(TileEntity tile) {
        if (!moreMachineLoaded || tile == null) {
            return null;
        }
        try {
            Class<?> registry = Class.forName("mekanism.common.upgrade.TileUpgradeRegistry");
            Method find = registry.getMethod("find", TileEntity.class);
            Object adapter = find.invoke(null, tile);
            if (adapter == null && !adapterRegistrationAttempted) {
                adapterRegistrationAttempted = true;
                try {
                    Class<?> adapters = Class.forName("mekceumoremachine.common.upgrade.MoreMachineTileUpgradeAdapters");
                    adapters.getMethod("register").invoke(null);
                } catch (Exception e) {
                    MekConfigCardUpgradesMod.LOGGER.warn("Unable to register MoreMachine tile upgrade adapters", e);
                }
                adapter = find.invoke(null, tile);
            }
            return adapter;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object invokeAdapter(Object adapter, String methodName, BaseTier tier) {
        if (adapter == null) {
            return null;
        }
        try {
            Method method;
            try {
                method = adapter.getClass().getMethod(methodName, BaseTier.class);
            } catch (NoSuchMethodException e) {
                method = adapter.getClass().getDeclaredMethod(methodName, BaseTier.class);
                method.setAccessible(true);
            }
            return method.invoke(adapter, tier);
        } catch (Exception e) {
            return null;
        }
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
