package com.nyonio.mekanism_advanced_configuration_card.compat;

import mekanism.api.ITierOptionalUpgradeable;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.ITier;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class MoreMachineCompat {
    public static final String MOD_ID = "mekceumoremachine";
    public static final String TIER_MACHINE_KEY = "mekanism_advanced_configuration_card_more_machine_tier";
    
    private static boolean moreMachineLoaded = false;
    
    public static void init() {
        moreMachineLoaded = Loader.isModLoaded(MOD_ID);
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
        String className = tile.getClass().getName();
        return className.startsWith("mekceumoremachine.");
    }
    
    public static int getTierOrdinal(TileEntity tile) {
        if (!moreMachineLoaded || !isTierMachine(tile)) {
            return -1;
        }
        return getTierOrdinalInternal(tile);
    }
    
    @Optional.Method(modid = MOD_ID)
    private static int getTierOrdinalInternal(TileEntity tile) {
        if (tile instanceof ITierMachineAccessor) {
            ITier tier = ((ITierMachineAccessor) tile).getTier();
            if (tier != null) {
                return tier.getBaseTier().ordinal();
            }
        }
        ITier tier = getTierFromTile(tile);
        if (tier != null) {
            return tier.getBaseTier().ordinal();
        }
        return -1;
    }
    
    private static ITier getTierFromTile(TileEntity tile) {
        try {
            String className = tile.getClass().getName();
            if (className.startsWith("mekceumoremachine.")) {
                NBTTagCompound tag = new NBTTagCompound();
                tile.writeToNBT(tag);
                if (tag.hasKey("tier")) {
                    int tierOrdinal = tag.getInteger("tier");
                    if (tierOrdinal >= 0 && tierOrdinal < BaseTier.values().length) {
                        return new SimpleTier(BaseTier.values()[tierOrdinal]);
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
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
        return upgradeToTierInternal(tile, targetTierOrdinal);
    }
    
    @Optional.Method(modid = MOD_ID)
    private static boolean upgradeToTierInternal(TileEntity tile, int targetTierOrdinal) {
        if (tile instanceof ITierMachineAccessor) {
            ITier currentTier = ((ITierMachineAccessor) tile).getTier();
            if (currentTier != null) {
                int currentOrdinal = currentTier.getBaseTier().ordinal();
                if (currentOrdinal >= targetTierOrdinal) {
                    return true;
                }
            }
        }
        if (tile instanceof ITierOptionalUpgradeable) {
            @SuppressWarnings("unchecked")
            ITierOptionalUpgradeable<BaseTier> upgradeable = (ITierOptionalUpgradeable<BaseTier>) tile;
            BaseTier targetTier = BaseTier.values()[targetTierOrdinal];
            return upgradeable.upgrade(targetTier);
        }
        return false;
    }
    
    @Optional.Interface(modid = MOD_ID, iface = "mekceumoremachine.common.tile.interfaces.ITierMachine")
    public interface ITierMachineAccessor extends ITierUpgradeable {
        ITier getTier();
    }
    
    private static class SimpleTier implements ITier {
        private final BaseTier baseTier;
        
        SimpleTier(BaseTier baseTier) {
            this.baseTier = baseTier;
        }
        
        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }
    }
}
