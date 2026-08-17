package com.nyonio.mekanism_advanced_configuration_card;

import com.nyonio.mekanism_advanced_configuration_card.compat.AE2Compat;
import com.nyonio.mekanism_advanced_configuration_card.compat.AEUpgradeCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.MoreMachineCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemAdvancedConfigurationCard;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IFactory;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.IUpgradeableTile;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.item.ItemConfigurationCard;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.SecurityUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.*;

public final class ConfigCardUpgradeHelper {
    public static final String UPGRADE_COPY_KEY = "mekanism_advanced_configuration_card";
    public static final String TIER_COPY_KEY = "mekanism_advanced_configuration_card_tier";
    public static final String PASTE_MODE_KEY = "mekanism_advanced_configuration_card_paste_mode";
    public static final String FUZZY_MODE_KEY = "mekanism_advanced_configuration_card_fuzzy_mode";
    public static final String FACTORY_RECIPE_TYPE_KEY = "mekanism_advanced_configuration_card_factory_recipe_type";
    public static final String FACTORY_TIER_KEY = "mekanism_advanced_configuration_card_factory_tier";

    public enum PasteMode {
        PRECISE("Precise", EnumColor.AQUA),
        FUZZY("Fuzzy", EnumColor.AQUA),
        CLEAR("Clear", EnumColor.RED);

        private final String displayName;
        private final EnumColor messageColor;

        PasteMode(String displayName, EnumColor messageColor) {
            this.displayName = displayName;
            this.messageColor = messageColor;
        }

        public String displayName() {
            return displayName;
        }

        public EnumColor messageColor() {
            return messageColor;
        }

        public PasteMode next() {
            switch (this) {
                case PRECISE: return FUZZY;
                case FUZZY: return CLEAR;
                case CLEAR: return PRECISE;
                default: return PRECISE;
            }
        }

        public static PasteMode byIndex(int index) {
            PasteMode[] values = values();
            if (index < 0 || index >= values.length) {
                return PRECISE;
            }
            return values[index];
        }
    }

    private ConfigCardUpgradeHelper() {
    }

    private static ItemStack getUpgradeStack(Upgrade upgrade, int count) {
        return ItemCardSlotBag.copyStackWithSize(upgrade.getStack(), count);
    }
    
    private static String getLocalizedTierName(BaseTier tier) {
        String tierKey = "tier." + tier.getSimpleName();
        String localized = mekanism.common.util.LangUtils.localize(tierKey);
        if (localized.equals(tierKey)) {
            return tier.getSimpleName();
        }
        return localized;
    }

    public static void init() {
    }

    public static NBTTagCompound appendUpgradeData(IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades()) {
            return data;
        }
        NBTTagCompound upgradeData = new NBTTagCompound();
        Map<Upgrade, Integer> installedUpgrades = getInstalledUpgrades(tile.getComponent());
        Upgrade.saveMap(installedUpgrades, upgradeData);
        data.setTag(UPGRADE_COPY_KEY, upgradeData);
        // Save AE wireless encryption keys
        if (tile instanceof TileEntity) {
            AEUpgradeCompat.saveWirelessKeys((TileEntity) tile, data);
        }
        return data;
    }

    public static NBTTagCompound appendTierData(TileEntity tile, NBTTagCompound data) {
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.isTierMachine(tile)) {
            MoreMachineCompat.saveTierData(tile, data);
        }
        return data;
    }
    
    public static NBTTagCompound appendFactoryData(TileEntity tile, NBTTagCompound data) {
        
        boolean isFactory = TileEntityFactory.class.isAssignableFrom(tile.getClass());
        
        if (isFactory) {
            try {
                TileEntityFactory factory = (TileEntityFactory) tile;
                int recipeType = factory.getRecipeType().ordinal();
                int tier = factory.tier.ordinal();
                data.setInteger(FACTORY_RECIPE_TYPE_KEY, recipeType);
                data.setInteger(FACTORY_TIER_KEY, tier);
                
                
            } catch (Exception e) {
                
            }
        } else {
            
        }
        return data;
    }
    
    public static boolean hasFactoryData(NBTTagCompound data) {
        return data.hasKey(FACTORY_RECIPE_TYPE_KEY) && data.hasKey(FACTORY_TIER_KEY);
    }
    
    public static int getStoredFactoryRecipeType(NBTTagCompound data) {
        return data.getInteger(FACTORY_RECIPE_TYPE_KEY);
    }
    
    public static int getStoredFactoryTier(NBTTagCompound data) {
        return data.getInteger(FACTORY_TIER_KEY);
    }
    
    public static boolean isFactoryCompatible(TileEntity sourceTile, TileEntity targetTile, NBTTagCompound data) {
        if (!(targetTile instanceof TileEntityFactory)) {
            return false;
        }
        if (!hasFactoryData(data)) {
            return sourceTile.getClass().isInstance(targetTile);
        }
        TileEntityFactory targetFactory = (TileEntityFactory) targetTile;
        int storedRecipeType = getStoredFactoryRecipeType(data);
        return targetFactory.getRecipeType().ordinal() == storedRecipeType;
    }

    public static boolean hasUpgradeData(NBTTagCompound data) {
        return data.hasKey(UPGRADE_COPY_KEY);
    }

    public static boolean hasTierData(NBTTagCompound data) {
        return data.hasKey(TIER_COPY_KEY);
    }

    public static int getStoredTier(NBTTagCompound data) {
        return data.getInteger(TIER_COPY_KEY);
    }

    public static boolean hasCardData(ItemStack stack) {
        NBTTagCompound data = ItemDataUtils.getCompound(stack, "data");
        return !data.hasNoTags() && (getStoredTileType(data) != null || hasUpgradeData(data));
    }

    public static boolean canUseBatchMode(ItemStack stack) {
        return isClearMode(stack) || hasCardData(stack);
    }

    public static PasteMode getPasteMode(ItemStack stack) {
        NBTTagCompound dataMap = ItemDataUtils.getDataMap(stack);
        if (dataMap != null && dataMap.hasKey(PASTE_MODE_KEY)) {
            return PasteMode.byIndex(dataMap.getInteger(PASTE_MODE_KEY));
        }
        return ItemDataUtils.getBoolean(stack, FUZZY_MODE_KEY) ? PasteMode.FUZZY : PasteMode.PRECISE;
    }

    public static boolean isFuzzyMode(ItemStack stack) {
        return getPasteMode(stack) == PasteMode.FUZZY;
    }

    public static boolean isClearMode(ItemStack stack) {
        return getPasteMode(stack) == PasteMode.CLEAR;
    }

    public static PasteMode togglePasteMode(ItemStack stack) {
        PasteMode nextMode = getPasteMode(stack).next();
        ItemDataUtils.setInt(stack, PASTE_MODE_KEY, nextMode.ordinal());
        return nextMode;
    }

    public static NBTTagCompound getConfigurationOnlyData(NBTTagCompound data) {
        NBTTagCompound copy = data.copy();
        copy.removeTag(UPGRADE_COPY_KEY);
        copy.removeTag(TIER_COPY_KEY);
        copy.removeTag(FACTORY_RECIPE_TYPE_KEY);
        copy.removeTag(FACTORY_TIER_KEY);
        return copy;
    }

    public static Class<? extends TileEntity> getStoredTileType(NBTTagCompound data) {
        if (data.hasKey("tileClass")) {
            String className = data.getString("tileClass");
            try {
                return (Class<? extends TileEntity>) Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        if (!data.hasKey("dataType")) {
            return null;
        }
        String className = data.getString("dataType");
        try {
            return (Class<? extends TileEntity>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    private static String getLocalizedDataType(NBTTagCompound data, String dataType) {
        if (dataType == null || dataType.isEmpty()) {
            return "";
        }
        return dataType;
    }

    public static String pasteCardToTarget(EntityPlayer player, TileEntity tile, EnumFacing side, ItemStack stack, boolean sendSuccessMessage) {
        
        
        if (!SecurityUtils.canAccess(player, tile)) {
            return "Paste failed: cannot access target";
        }
        if (isClearMode(stack)) {
            return clearUpgradesFromTarget(player, tile, sendSuccessMessage, stack);
        }
        NBTTagCompound data = ItemDataUtils.getCompound(stack, "data");
        
        
        
        if (data.hasNoTags()) {
            return "Paste failed: no data saved on config card";
        }
        if (isFuzzyMode(stack)) {
            if (!hasUpgradeData(data)) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.no_upgrade_data");
            }
            if (!(tile instanceof IUpgradeTile)) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.target_no_upgrade_support");
            }
            IUpgradeTile upgradeTile = (IUpgradeTile) tile;
            if (!upgradeTile.supportsUpgrades()) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.target_no_upgrade_support");
            }
            String failure = validateFuzzyPaste(player, upgradeTile, data, stack);
            if (failure != null) {
                return failure;
            }
            applyFuzzyStoredUpgrades(player, upgradeTile, data, stack);
            if (sendSuccessMessage) {
                player.sendMessage(new TextComponentString(EnumColor.DARK_GREEN + new TextComponentTranslation("message.mekanism_advanced_configuration_card.configuration_set").getFormattedText()));
            }
            return null;
        }
        if (!CapabilityUtils.hasCapability(tile, Capabilities.CONFIG_CARD_CAPABILITY, side)) {
            if (!(tile instanceof ISideConfiguration) && !(tile instanceof IUpgradeTile)) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.no_config_support");
            }
        }
        Class<? extends TileEntity> storedType = getStoredTileType(data);
        if (storedType == null) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.invalid_config_data");
        }
        
        boolean hasSpecialConfigData = data.hasKey("hasSpecialConfigData") && data.getBoolean("hasSpecialConfigData");
        IConfigCardAccess.ISpecialConfigData configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
        
        if (hasSpecialConfigData && configCardAccess == null) {
            if (!MoreMachineCompat.isMoreMachineLoaded() || !MoreMachineCompat.isTierMachine(tile)) {
                if (!(tile instanceof ISideConfiguration) && !(tile instanceof IUpgradeTile)) {
                    return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.no_config_support");
                }
            }
        }
        
        boolean sourceIsFactory = TileEntityFactory.class.isAssignableFrom(storedType);
        boolean factoryCardCanUpgradeTarget = (hasFactoryData(data) || sourceIsFactory)
              && !(tile instanceof TileEntityFactory)
              && (tile instanceof IUpgradeableTile || MoreMachineCompat.canConvertToMoreMachine(tile));
        if (!isConfigurationCompatible(tile, storedType, data)
              && !MoreMachineCompat.canConvertToMoreMachine(storedType, tile)
              && !factoryCardCanUpgradeTarget) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.type_mismatch");
        }
        IUpgradeTile upgradeTile = null;
        if (tile instanceof IUpgradeTile) {
            IUpgradeTile ut = (IUpgradeTile) tile;
            if (ut.supportsUpgrades()) {
                upgradeTile = ut;
            }
        }
        if (hasUpgradeData(data)) {
            if (upgradeTile == null) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.target_no_upgrade_support");
            }
            String failure = validatePaste(player, upgradeTile, data, stack);
            if (failure != null) {
                return failure;
            }
        }
        TileEntityFactory upgradedFactory = null;
        boolean convertedToMoreMachine = false;
        if (hasFactoryData(data) && tile instanceof TileEntityFactory) {
            TileEntityFactory targetFactory = (TileEntityFactory) tile;
            int storedTierOrdinal = getStoredFactoryTier(data);
            int targetTierOrdinal = targetFactory.tier.ordinal();
            
            if (storedTierOrdinal > targetTierOrdinal) {
                String failure = validateAndConsumeTierInstallers(player, tile, data, stack);
                if (failure != null) {
                    return failure;
                }
                upgradedFactory = performFactoryUpgrade(player, targetFactory, storedTierOrdinal);
                if (upgradedFactory != null) {
                    tile = upgradedFactory;
                    configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
                    if (tile instanceof IUpgradeTile) {
                        IUpgradeTile ut = (IUpgradeTile) tile;
                        if (ut.supportsUpgrades()) {
                            upgradeTile = ut;
                        } else {
                            upgradeTile = null;
                        }
                    }
                }
            }
        }
        // A card copied from a normal Mekanism machine does not contain a
        // MoreMachine tier field. If the target is the matching normal
        // machine, use the registered BASIC adapter to convert it first.
        if (MoreMachineCompat.canConvertToMoreMachine(storedType, tile)
              && !hasFactoryData(data) && !MoreMachineCompat.hasTierData(data)) {
            String failure = validateAndConsumeTierInstallersForUpgradeableMachine(player, tile, BaseTier.BASIC.ordinal(), stack);
            if (failure != null) {
                return failure;
            }
            performUpgradeableMachineUpgrade(player, tile, BaseTier.BASIC.ordinal(), true);
            TileEntity updatedTile = tile.getWorld() == null ? null : tile.getWorld().getTileEntity(tile.getPos());
            if (updatedTile == null || !MoreMachineCompat.isTierMachine(updatedTile)) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_upgrade");
            }
            tile = updatedTile;
            convertedToMoreMachine = true;
            configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
            if (tile instanceof IUpgradeTile) {
                IUpgradeTile ut = (IUpgradeTile) tile;
                upgradeTile = ut.supportsUpgrades() ? ut : null;
            } else {
                upgradeTile = null;
            }
        }
        // Handle Factory -> IUpgradeableTile machine upgrade
        if (hasFactoryData(data) && !(tile instanceof TileEntityFactory)
              && (tile instanceof IUpgradeableTile || MoreMachineCompat.isUpgradeable(tile))
              && !MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = getStoredFactoryTier(data);
            boolean convertsToMoreMachine = MoreMachineCompat.canConvertToMoreMachine(tile);
            int requestedTierOrdinal = convertsToMoreMachine ? BaseTier.BASIC.ordinal() : storedTierOrdinal;
            String failure = validateAndConsumeTierInstallersForUpgradeableMachine(player, tile, requestedTierOrdinal, stack);
            if (failure != null) {
                return failure;
            }
            // A normal machine may need to become either a MoreMachine tier
            // block or a normal Mekanism factory. The upgrade helper selects
            // the correct adapter from the target block state.
            performUpgradeableMachineUpgrade(player, tile, requestedTierOrdinal, true);
            TileEntity updatedTile = tile.getWorld() == null ? null : tile.getWorld().getTileEntity(tile.getPos());
            if (updatedTile == null
                  || (convertsToMoreMachine
                  ? !MoreMachineCompat.isTierMachine(updatedTile)
                  : !(updatedTile instanceof TileEntityFactory))) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_upgrade");
            }
            int updatedTierOrdinal = updatedTile instanceof TileEntityFactory
                  ? ((TileEntityFactory) updatedTile).tier.ordinal()
                  : MoreMachineCompat.getTierOrdinal(updatedTile);
            if (!convertsToMoreMachine && updatedTierOrdinal < requestedTierOrdinal) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_upgrade");
            }
            tile = updatedTile;
            configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
            if (tile instanceof IUpgradeTile) {
                IUpgradeTile ut = (IUpgradeTile) tile;
                upgradeTile = ut.supportsUpgrades() ? ut : null;
            } else {
                upgradeTile = null;
            }
        }
        // Handle MoreMachine -> IUpgradeableTile machine upgrade
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && !(tile instanceof TileEntityFactory) && MoreMachineCompat.isUpgradeable(tile) && !MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = MoreMachineCompat.getStoredTier(data);
            String failure = validateAndConsumeTierInstallersForUpgradeableMachine(player, tile, storedTierOrdinal, stack);
            if (failure != null) {
                return failure;
            }
            performUpgradeableMachineUpgrade(player, tile, storedTierOrdinal, true);
            TileEntity updatedTile = tile.getWorld() == null ? null : tile.getWorld().getTileEntity(tile.getPos());
            if (updatedTile != null) {
                tile = updatedTile;
                configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
                if (tile instanceof IUpgradeTile) {
                    IUpgradeTile ut = (IUpgradeTile) tile;
                    upgradeTile = ut.supportsUpgrades() ? ut : null;
                } else {
                    upgradeTile = null;
                }
            }
        }
        // Handle MoreMachine tier upgrade
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = MoreMachineCompat.getStoredTier(data);
            int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
            if (currentTierOrdinal >= 0 && storedTierOrdinal > currentTierOrdinal) {
                String failure = validateAndConsumeMoreMachineTierInstallers(player, tile, storedTierOrdinal, stack);
                if (failure != null) {
                    return failure;
                }
                performMoreMachineTierUpgrade(player, tile, storedTierOrdinal);
            }
        }
        // Handle Factory -> MoreMachine tier conversion
        if (MoreMachineCompat.isMoreMachineLoaded() && hasFactoryData(data)
              && !(tile instanceof TileEntityFactory) && MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = getStoredFactoryTier(data);
            int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
            if (currentTierOrdinal >= 0 && storedTierOrdinal > currentTierOrdinal) {
                String failure = validateAndConsumeMoreMachineTierInstallers(player, tile, storedTierOrdinal, stack);
                if (failure != null) {
                    return failure;
                }
                performMoreMachineTierUpgrade(player, tile, storedTierOrdinal);
                TileEntity updatedMoreMachineTile = tile.getWorld() == null ? null : tile.getWorld().getTileEntity(tile.getPos());
                if (updatedMoreMachineTile == null
                      || MoreMachineCompat.getTierOrdinal(updatedMoreMachineTile) < storedTierOrdinal) {
                    return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_upgrade");
                }
                tile = updatedMoreMachineTile;
                configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
                if (tile instanceof IUpgradeTile) {
                    IUpgradeTile ut = (IUpgradeTile) tile;
                    upgradeTile = ut.supportsUpgrades() ? ut : null;
                } else {
                    upgradeTile = null;
                }
            }
        }
        // Handle MoreMachine -> Factory tier conversion
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && !hasFactoryData(data) && tile instanceof TileEntityFactory) {
            TileEntityFactory targetFactory = (TileEntityFactory) tile;
            int storedTierOrdinal = MoreMachineCompat.getStoredTier(data);
            int targetTierOrdinal = targetFactory.tier.ordinal();
            if (storedTierOrdinal > targetTierOrdinal) {
                String failure = validateAndConsumeTierInstallersForFactory(player, targetFactory, storedTierOrdinal, stack);
                if (failure != null) {
                    return failure;
                }
                upgradedFactory = performFactoryUpgrade(player, targetFactory, storedTierOrdinal);
                if (upgradedFactory != null) {
                    tile = upgradedFactory;
                    configCardAccess = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
                    if (tile instanceof IUpgradeTile) {
                        IUpgradeTile ut = (IUpgradeTile) tile;
                        if (ut.supportsUpgrades()) {
                            upgradeTile = ut;
                        } else {
                            upgradeTile = null;
                        }
                    }
                }
            }
        }
        if (configCardAccess != null) {
            configCardAccess.setConfigurationData(getConfigurationOnlyData(data));
        }
        ItemAdvancedConfigurationCard.applyBaseData(tile, data, player);
        if (hasUpgradeData(data) && upgradeTile != null) {
            applyStoredUpgrades(player, upgradeTile, data, stack);
        }
        if (sendSuccessMessage) {
            String dataType = data.getString("dataType");
            String localizedDataType = getLocalizedDataType(data, dataType);
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.DARK_GREEN + mekanism.common.util.LangUtils.localize("tooltip.configurationCard.set").replaceAll("%s", EnumColor.INDIGO + localizedDataType + EnumColor.DARK_GREEN)));
        }
        return null;
    }
    
    private static TileEntityFactory performFactoryUpgrade(EntityPlayer player, TileEntityFactory factory, int targetTierOrdinal) {
        int currentTierOrdinal = factory.tier.ordinal();
        if (currentTierOrdinal >= targetTierOrdinal) {
            return null;
        }
        
        TileEntityFactory currentFactory = factory;
        while (currentFactory.tier.ordinal() < targetTierOrdinal) {
            BaseTier nextTier = BaseTier.values()[currentFactory.tier.ordinal() + 1];
            
            if (currentFactory instanceof IUpgradeableTile) {
                IUpgradeableTile upgradeable = (IUpgradeableTile) currentFactory;
                mekanism.common.upgrade.IUpgradeData upgradeData = upgradeable.getUpgradeData(nextTier);
                IBlockState upgradeResult = upgradeable.getUpgradeResult(nextTier);
                boolean success;
                if (upgradeResult != null && upgradeData != null) {
                    success = UpgradeUtils.replaceTileForUpgrade(currentFactory, upgradeResult, upgradeData);
                } else if (upgradeData != null) {
                    success = upgradeable.parseUpgradeData(upgradeData);
                } else {
                    break;
                }
                if (!success) break;
            } else {
                break;
            }
            
            TileEntity newTile = factory.getWorld().getTileEntity(factory.getPos());
            if (newTile instanceof TileEntityFactory) {
                currentFactory = (TileEntityFactory) newTile;
            } else {
                break;
            }
        }
        return currentFactory;
    }
    
    private static String validateAndConsumeTierInstallersForUpgradeableMachine(EntityPlayer player, TileEntity tile, int targetTierOrdinal, ItemStack configCard) {
        if (player.isCreative()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasInfiniteFactoryInstaller(player, configCard)) {
            return null;
        }
        if (!MoreMachineCompat.isUpgradeable(tile)) {
            return null;
        }
        
        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(player);
            }
        }
        
        boolean isUltimateTarget = targetTierOrdinal >= BaseTier.ULTIMATE.ordinal();
        Item compositeTierInstaller = null;
        int compositeCountInInventory = 0;
        int compositeCountInBags = 0;
        long compositeCountInNetwork = 0;
        
        if (isUltimateTarget && MoreMachineCompat.isMoreMachineLoaded()) {
            compositeTierInstaller = MoreMachineCompat.getCompositeTierInstallerItem();
            if (compositeTierInstaller != null) {
                compositeCountInInventory = countInInventory(player.inventory, compositeTierInstaller);
                compositeCountInBags = ItemCardSlotBag.countInBags(player.inventory, new ItemStack(compositeTierInstaller));
                if (ae2Storage != null) {
                    compositeCountInNetwork = AE2Compat.countItemInNetwork(ae2Storage, compositeTierInstaller, 0);
                }
            }
        }
        
        java.util.List<String> missingTiers = new java.util.ArrayList<>();
        int tempTierOrdinal = -1;
        while (tempTierOrdinal < targetTierOrdinal) {
            BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
            
            ItemStack tierInstallerStack = getTierInstallerStack(requiredTier);
            if (tierInstallerStack.isEmpty()) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_find_tier_installer") + ": " + getLocalizedTierName(requiredTier);
            }
            int countInInventory = countInInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, tierInstallerStack);
            long countInNetwork = 0;
            if (ae2Storage != null) {
                countInNetwork = AE2Compat.countItemInNetwork(ae2Storage, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            }
            long totalAvailable = countInInventory + countInBags + countInNetwork;
            
            if (totalAvailable < 1) {
                missingTiers.add(getLocalizedTierName(requiredTier));
            }
            tempTierOrdinal++;
        }
        
        boolean canUseComposite = isUltimateTarget && (compositeCountInInventory + compositeCountInBags + compositeCountInNetwork) >= 1;
        
        if (!missingTiers.isEmpty() && !canUseComposite) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_tier_installers") + ": " + String.join(", ", missingTiers);
        }
        
        if (canUseComposite && !missingTiers.isEmpty()) {
            if (compositeCountInInventory > 0) {
                removeFromInventory(player.inventory, compositeTierInstaller, 1);
            } else if (compositeCountInBags > 0) {
                ItemCardSlotBag.consumeFromBags(player.inventory, new ItemStack(compositeTierInstaller), 1);
            } else if (ae2Storage != null && ae2Source != null) {
                AE2Compat.extractItemFromNetwork(ae2Storage, compositeTierInstaller, 0, 1, ae2Source);
            }
            player.inventoryContainer.detectAndSendChanges();
            return null;
        }
        
        tempTierOrdinal = -1;
        while (tempTierOrdinal < targetTierOrdinal) {
            BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
            
            ItemStack tierInstallerStack = getTierInstallerStack(requiredTier);
            int countInInventory = countInInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, tierInstallerStack);
            
            boolean consumed = false;
            if (countInInventory > 0) {
                removeFromInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata(), 1);
                consumed = true;
            } else if (countInBags > 0) {
                ItemCardSlotBag.consumeFromBags(player.inventory, tierInstallerStack, 1);
                consumed = true;
            } else if (ae2Storage != null && ae2Source != null) {
                ItemStack extracted = AE2Compat.extractItemFromNetwork(ae2Storage, tierInstallerStack.getItem(), tierInstallerStack.getMetadata(), 1, ae2Source);
                if (!extracted.isEmpty()) {
                    consumed = true;
                }
            }
            tempTierOrdinal++;
        }
        
        player.inventoryContainer.detectAndSendChanges();
        return null;
    }
    
    private static void performUpgradeableMachineUpgrade(EntityPlayer player, TileEntity tile, int targetTierOrdinal) {
        performUpgradeableMachineUpgrade(player, tile, targetTierOrdinal, false);
    }

    /**
     * Performs an upgrade through MoreMachine's adapter when a normal
     * Mekanism machine is being converted to a tiered MoreMachine block.
     * Normal Mekanism machines also implement IUpgradeableTile, so checking
     * only for that interface used to return before the adapter was invoked.
     */
    private static void performUpgradeableMachineUpgrade(EntityPlayer player, TileEntity tile, int targetTierOrdinal, boolean forceMoreMachine) {
        if ((forceMoreMachine || !(tile instanceof IUpgradeableTile))
              && !(tile instanceof TileEntityFactory)
              && MoreMachineCompat.isUpgradeable(tile)) {
            World world = tile.getWorld();
            BlockPos pos = tile.getPos();
            if (world == null || pos == null) {
                return;
            }
            int currentTierOrdinal = -1;
            while (currentTierOrdinal < targetTierOrdinal && currentTierOrdinal < BaseTier.ULTIMATE.ordinal()) {
                BaseTier nextTier = BaseTier.values()[currentTierOrdinal + 1];
                mekanism.common.upgrade.IUpgradeData upgradeData = MoreMachineCompat.getUpgradeData(tile, nextTier);
                IBlockState upgradeResult = MoreMachineCompat.getUpgradeResult(tile, nextTier);
                if (upgradeData == null) {
                    break;
                }
                boolean success;
                if (upgradeResult != null) {
                    success = MoreMachineCompat.replaceTileForUpgrade(tile, upgradeResult, upgradeData);
                } else if (tile instanceof IUpgradeableTile) {
                    success = ((IUpgradeableTile) tile).parseUpgradeData(upgradeData);
                } else {
                    break;
                }
                if (!success) {
                    break;
                }
                TileEntity updatedTile = world.getTileEntity(pos);
                if (updatedTile == null) {
                    break;
                }
                tile = updatedTile;
                if (updatedTile instanceof TileEntityFactory) {
                    currentTierOrdinal = ((TileEntityFactory) updatedTile).tier.ordinal();
                } else if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.isTierMachine(updatedTile)) {
                    int updatedTierOrdinal = MoreMachineCompat.getTierOrdinal(updatedTile);
                    if (updatedTierOrdinal <= currentTierOrdinal) {
                        break;
                    }
                    currentTierOrdinal = updatedTierOrdinal;
                } else {
                    break;
                }
            }
            return;
        }
        if (!(tile instanceof IUpgradeableTile)) {
            return;
        }
        IUpgradeableTile upgradeable = (IUpgradeableTile) tile;
        if (!(tile instanceof TileEntityFactory) && !MoreMachineCompat.isTierMachine(tile)) {
            return;
        }
        
        World world = tile.getWorld();
        BlockPos pos = tile.getPos();
        
        int currentTierOrdinal = -1;
        if (tile instanceof TileEntityFactory) {
            currentTierOrdinal = ((TileEntityFactory) tile).tier.ordinal();
        } else if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.isTierMachine(tile)) {
            currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
        }
        
        while (currentTierOrdinal < targetTierOrdinal && currentTierOrdinal < BaseTier.ULTIMATE.ordinal()) {
            BaseTier nextTier = BaseTier.values()[currentTierOrdinal + 1];
            
            mekanism.common.upgrade.IUpgradeData upgradeData = upgradeable.getUpgradeData(nextTier);
            IBlockState upgradeResult = upgradeable.getUpgradeResult(nextTier);
            boolean success;
            if (upgradeResult != null && upgradeData != null) {
                success = UpgradeUtils.replaceTileForUpgrade(tile, upgradeResult, upgradeData);
            } else if (upgradeData != null) {
                success = upgradeable.parseUpgradeData(upgradeData);
            } else {
                break;
            }
            
            if (!success) break;
            
            TileEntity updatedTile = world.getTileEntity(pos);
            
            if (updatedTile instanceof TileEntityFactory) {
                TileEntityFactory factory = (TileEntityFactory) updatedTile;
                currentTierOrdinal = factory.tier.ordinal();
                upgradeable = factory;
            } else if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.isTierMachine(updatedTile)) {
                currentTierOrdinal = MoreMachineCompat.getTierOrdinal(updatedTile);
                if (updatedTile instanceof IUpgradeableTile) {
                    upgradeable = (IUpgradeableTile) updatedTile;
                } else {
                    break;
                }
            } else if (updatedTile instanceof IUpgradeableTile) {
                upgradeable = (IUpgradeableTile) updatedTile;
                currentTierOrdinal++;
            } else {
                break;
            }
        }
    }
    
    private static String validateAndConsumeTierInstallers(EntityPlayer player, TileEntity tile, NBTTagCompound data, ItemStack configCard) {
        if (!(tile instanceof TileEntityFactory) || !hasFactoryData(data)) {
            return null;
        }
        int storedTierOrdinal = getStoredFactoryTier(data);
        return validateAndConsumeTierInstallersForFactory(player, (TileEntityFactory) tile, storedTierOrdinal, configCard);
    }
    
    private static String validateAndConsumeTierInstallersForFactory(EntityPlayer player, TileEntityFactory targetFactory, int targetTierOrdinal, ItemStack configCard) {
        if (player.isCreative()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasInfiniteFactoryInstaller(player, configCard)) {
            return null;
        }
        int currentTierOrdinal = targetFactory.tier.ordinal();
        
        if (targetTierOrdinal <= currentTierOrdinal) {
            return null;
        }
        
        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(player);
            }
        }
        
        boolean isUltimateTarget = targetTierOrdinal >= BaseTier.ULTIMATE.ordinal();
        Item compositeTierInstaller = null;
        int compositeCountInInventory = 0;
        int compositeCountInBags = 0;
        long compositeCountInNetwork = 0;
        
        if (isUltimateTarget && MoreMachineCompat.isMoreMachineLoaded()) {
            compositeTierInstaller = MoreMachineCompat.getCompositeTierInstallerItem();
            if (compositeTierInstaller != null) {
                compositeCountInInventory = countInInventory(player.inventory, compositeTierInstaller);
                compositeCountInBags = ItemCardSlotBag.countInBags(player.inventory, new ItemStack(compositeTierInstaller));
                if (ae2Storage != null) {
                    compositeCountInNetwork = AE2Compat.countItemInNetwork(ae2Storage, compositeTierInstaller, 0);
                }
            }
        }
        
        java.util.List<String> missingTiers = new java.util.ArrayList<>();
        int tempTierOrdinal = currentTierOrdinal;
        while (tempTierOrdinal < targetTierOrdinal) {
            BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
            
            ItemStack tierInstallerStack = getTierInstallerStack(requiredTier);
            if (tierInstallerStack.isEmpty()) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_find_tier_installer") + ": " + getLocalizedTierName(requiredTier);
            }
            int countInInventory = countInInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, tierInstallerStack);
            long countInNetwork = 0;
            if (ae2Storage != null) {
                countInNetwork = AE2Compat.countItemInNetwork(ae2Storage, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            }
            long totalAvailable = countInInventory + countInBags + countInNetwork;
            
            if (totalAvailable < 1) {
                missingTiers.add(getLocalizedTierName(requiredTier));
            }
            tempTierOrdinal++;
        }
        
        boolean canUseComposite = isUltimateTarget && (compositeCountInInventory + compositeCountInBags + compositeCountInNetwork) >= 1;
        
        if (!missingTiers.isEmpty() && !canUseComposite) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_tier_installers") + ": " + String.join(", ", missingTiers);
        }
        
        if (canUseComposite && !missingTiers.isEmpty()) {
            if (compositeCountInInventory > 0) {
                removeFromInventory(player.inventory, compositeTierInstaller, 1);
            } else if (compositeCountInBags > 0) {
                ItemCardSlotBag.consumeFromBags(player.inventory, new ItemStack(compositeTierInstaller), 1);
            } else if (ae2Storage != null && ae2Source != null) {
                AE2Compat.extractItemFromNetwork(ae2Storage, compositeTierInstaller, 0, 1, ae2Source);
            }
            player.inventoryContainer.detectAndSendChanges();
            return null;
        }
        
        tempTierOrdinal = currentTierOrdinal;
        while (tempTierOrdinal < targetTierOrdinal) {
            BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
            
            ItemStack tierInstallerStack = getTierInstallerStack(requiredTier);
            int countInInventory = countInInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            int countInBags = ItemCardSlotBag.countInBags(player.inventory, tierInstallerStack);
            
            if (countInInventory > 0) {
                removeFromInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata(), 1);
            } else if (countInBags > 0) {
                ItemCardSlotBag.consumeFromBags(player.inventory, tierInstallerStack, 1);
            } else if (ae2Storage != null && ae2Source != null) {
                AE2Compat.extractItemFromNetwork(ae2Storage, tierInstallerStack.getItem(), tierInstallerStack.getMetadata(), 1, ae2Source);
            }
            tempTierOrdinal++;
        }
        
        player.inventoryContainer.detectAndSendChanges();
        return null;
    }
    
    private static ItemStack getTierInstallerStack(BaseTier tier) {
        Item tierInstallerItem = mekanism.common.MekanismItems.TierInstaller;
        if (tierInstallerItem == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(tierInstallerItem, 1, tier.ordinal());
    }
    
    private static String validateAndConsumeMoreMachineTierInstallers(EntityPlayer player, TileEntity tile, int targetTierOrdinal, ItemStack configCard) {
        if (!MoreMachineCompat.isMoreMachineLoaded() || !MoreMachineCompat.isTierMachine(tile)) {
            return null;
        }
        if (player.isCreative()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasInfiniteFactoryInstaller(player, configCard)) {
            return null;
        }
        int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
        if (currentTierOrdinal < 0 || targetTierOrdinal <= currentTierOrdinal) {
            return null;
        }
        
        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(player);
            }
        }
        
        Item compositeTierInstaller = MoreMachineCompat.getCompositeTierInstallerItem();
        Item tierInstaller = MoreMachineCompat.getTierInstallerItem();
        
        int compositeCountInInventory = 0;
        int compositeCountInBags = 0;
        long compositeCountInNetwork = 0;
        if (compositeTierInstaller != null) {
            compositeCountInInventory = countInInventory(player.inventory, compositeTierInstaller);
            compositeCountInBags = ItemCardSlotBag.countInBags(player.inventory, new ItemStack(compositeTierInstaller));
            if (ae2Storage != null) {
                compositeCountInNetwork = AE2Compat.countItemInNetwork(ae2Storage, compositeTierInstaller, 0);
            }
        }
        long totalCompositeAvailable = compositeCountInInventory + compositeCountInBags + compositeCountInNetwork;
        
        if (totalCompositeAvailable >= 1) {
            if (compositeCountInInventory > 0) {
                removeFromInventory(player.inventory, compositeTierInstaller, 1);
            } else if (compositeCountInBags > 0) {
                ItemCardSlotBag.consumeFromBags(player.inventory, new ItemStack(compositeTierInstaller), 1);
            } else if (ae2Storage != null && ae2Source != null) {
                AE2Compat.extractItemFromNetwork(ae2Storage, compositeTierInstaller, 0, 1, ae2Source);
            }
            player.inventoryContainer.detectAndSendChanges();
            return null;
        }
        
        if (tierInstaller != null) {
            java.util.List<String> missingTiers = new java.util.ArrayList<>();
            int tempTierOrdinal = currentTierOrdinal;
            boolean hasAllInstallers = true;
            
            while (tempTierOrdinal < targetTierOrdinal) {
                BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
                int countInInventory = countInInventory(player.inventory, tierInstaller, requiredTier.ordinal());
                int countInBags = ItemCardSlotBag.countInBags(player.inventory, new ItemStack(tierInstaller, 1, requiredTier.ordinal()));
                long countInNetwork = 0;
                if (ae2Storage != null) {
                    countInNetwork = AE2Compat.countItemInNetwork(ae2Storage, tierInstaller, requiredTier.ordinal());
                }
                long totalAvailable = countInInventory + countInBags + countInNetwork;
                
                if (totalAvailable < 1) {
                    missingTiers.add(getLocalizedTierName(requiredTier));
                    hasAllInstallers = false;
                }
                tempTierOrdinal++;
            }
            
            if (hasAllInstallers) {
                tempTierOrdinal = currentTierOrdinal;
                while (tempTierOrdinal < targetTierOrdinal) {
                    BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
                    int countInInventory = countInInventory(player.inventory, tierInstaller, requiredTier.ordinal());
                    int countInBags = ItemCardSlotBag.countInBags(player.inventory, new ItemStack(tierInstaller, 1, requiredTier.ordinal()));
                    
                    if (countInInventory > 0) {
                        removeFromInventory(player.inventory, tierInstaller, requiredTier.ordinal(), 1);
                    } else if (countInBags > 0) {
                        ItemCardSlotBag.consumeFromBags(player.inventory, new ItemStack(tierInstaller, 1, requiredTier.ordinal()), 1);
                    } else if (ae2Storage != null && ae2Source != null) {
                        AE2Compat.extractItemFromNetwork(ae2Storage, tierInstaller, requiredTier.ordinal(), 1, ae2Source);
                    }
                    tempTierOrdinal++;
                }
                player.inventoryContainer.detectAndSendChanges();
                return null;
            }
            
            if (!missingTiers.isEmpty()) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_tier_installers") + ": " + String.join(", ", missingTiers);
            }
        }
        
        return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_tier_installers");
    }
    
    private static void performMoreMachineTierUpgrade(EntityPlayer player, TileEntity tile, int targetTierOrdinal) {
        if (!MoreMachineCompat.isMoreMachineLoaded() || !MoreMachineCompat.isTierMachine(tile)) {
            return;
        }
        // If the tile implements IUpgradeableTile, use the standard upgrade mechanism
        // which uses getUpgradeData/getUpgradeResult and replaceTileForUpgrade
        if (tile instanceof IUpgradeableTile) {
            performUpgradeableMachineUpgrade(player, tile, targetTierOrdinal);
            return;
        }
        // Fallback: use reflection to call upgrade(BaseTier) for tiles that don't implement IUpgradeableTile
        World world = tile.getWorld();
        BlockPos pos = tile.getPos();
        BaseTier lastTier = null;
        int loopCount = 0;
        int maxLoops = 10;
        while (loopCount < maxLoops) {
            loopCount++;
            TileEntity currentTile = world.getTileEntity(pos);
            if (currentTile == null || !MoreMachineCompat.isTierMachine(currentTile)) {
                break;
            }
            int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(currentTile);
            if (currentTierOrdinal < 0 || currentTierOrdinal >= targetTierOrdinal) {
                break;
            }
            if (currentTierOrdinal >= BaseTier.ULTIMATE.ordinal()) {
                break;
            }
            BaseTier currentBaseTier = BaseTier.values()[currentTierOrdinal];
            if (currentBaseTier == lastTier) {
                break;
            }
            BaseTier nextTier = BaseTier.values()[currentTierOrdinal + 1];
            
            try {
                java.lang.reflect.Method upgradeMethod = currentTile.getClass().getMethod("upgrade", BaseTier.class);
                upgradeMethod.invoke(currentTile, nextTier);
            } catch (Exception e) {
                break;
            }
            
            lastTier = currentBaseTier;
        }
    }

    private static boolean isConfigurationCompatible(TileEntity tile, Class<? extends TileEntity> storedType, NBTTagCompound data) {
        if (storedType.isInstance(tile)) {
            return true;
        }
        
        boolean sourceIsFactory = TileEntityFactory.class.isAssignableFrom(storedType);
        boolean targetIsFactory = tile instanceof TileEntityFactory;

        if (MoreMachineCompat.isEquivalentMachineType(storedType, tile)) {
            return true;
        }
        
        if (sourceIsFactory && targetIsFactory && hasFactoryData(data)) {
            return true;
        } else if (hasFactoryData(data) && targetIsFactory) {
            return true;
        }
        if (hasTierData(data) && tile instanceof IUpgradeableTile) {
            return true;
        }
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && MoreMachineCompat.isTierMachine(tile)) {
            return true;
        }
        
        if (sourceIsFactory && hasFactoryData(data) && !targetIsFactory) {
            if (tile instanceof IUpgradeableTile) {
                return true;
            }
        }
        
        if (MoreMachineCompat.isMoreMachineLoaded()) {
            if (sourceIsFactory && MoreMachineCompat.isTierMachine(tile) && hasFactoryData(data)) {
                return true;
            }
            if (MoreMachineCompat.hasTierData(data) && targetIsFactory) {
                return true;
            }
            if (MoreMachineCompat.hasTierData(data) && !targetIsFactory && tile instanceof IUpgradeableTile && !MoreMachineCompat.isTierMachine(tile)) {
                return true;
            }
        }
        
        if (!sourceIsFactory && !targetIsFactory) {
            if (tile instanceof ISideConfiguration || tile instanceof IUpgradeTile) {
                return true;
            }
        }
        
        boolean hasSpecialConfigData = data.hasKey("hasSpecialConfigData") && data.getBoolean("hasSpecialConfigData");
        if (!hasSpecialConfigData) {
            if (tile instanceof ISideConfiguration || tile instanceof IUpgradeTile) {
                return true;
            }
        }
        
        return false;
    }

    public static String clearUpgradesFromTarget(EntityPlayer player, TileEntity tile, boolean sendSuccessMessage, ItemStack configCard) {
        if (!(tile instanceof IUpgradeTile)) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.target_no_upgrade_support");
        }
        IUpgradeTile upgradeTile = (IUpgradeTile) tile;
        if (!upgradeTile.supportsUpgrades()) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.target_no_upgrade_support");
        }
        TileComponentUpgrade component = upgradeTile.getComponent();
        if (!hasInstalledUpgrades(component)) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.no_upgrades_to_remove");
        }
        String failure = validateClear(player, upgradeTile, configCard);
        if (failure != null) {
            return failure;
        }
        applyClear(player, upgradeTile, configCard);
        if (sendSuccessMessage) {
            player.sendMessage(new TextComponentString(EnumColor.DARK_GREEN + new TextComponentTranslation("message.mekanism_advanced_configuration_card.configuration_set").getFormattedText()));
        }
        return null;
    }

    public static String validatePaste(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data, ItemStack configCard) {
        if (!tile.supportsUpgrades()) {
            return null;
        }
        boolean hasSuperInfinite = InfiniteUpgradeCardCompat.hasSuperInfiniteUpgrade(player, configCard);
        boolean hasInfinite = InfiniteUpgradeCardCompat.hasInfiniteUpgrade(player, configCard);
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        InventorySimulation inventory = new InventorySimulation(player.inventory, configCard);
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            int actualTarget = target;
            if (ModConfig.limitSavedUpgradeCount) {
                int maxInstallable = upgrade.getMaxInstalled();
                actualTarget = Math.min(target, maxInstallable);
            }
            
            if (actualTarget > 0 && !component.supports(upgrade)) {
                continue;
            }
            if (!player.isCreative()) {
                if (hasSuperInfinite) continue;
                if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) continue;
                if (actualTarget > current && !inventory.remove(upgrade.getStack(), actualTarget - current)) {
                    return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_upgrade") + ": " + upgrade.getName();
                }
                if (current > actualTarget && !inventory.insert(getUpgradeStack(upgrade, current - actualTarget))) {
                    return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.inventory_full");
                }
            }
        }
        return null;
    }

    public static String validateFuzzyPaste(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data, ItemStack configCard) {
        if (!tile.supportsUpgrades()) {
            return null;
        }
        boolean hasSuperInfinite = InfiniteUpgradeCardCompat.hasSuperInfiniteUpgrade(player, configCard);
        boolean hasInfinite = InfiniteUpgradeCardCompat.hasInfiniteUpgrade(player, configCard);
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        InventorySimulation inventory = new InventorySimulation(player.inventory, configCard);
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            if (!player.isCreative()) {
                if (hasSuperInfinite) continue;
                if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) continue;
                if (target > current && !inventory.remove(upgrade.getStack(), target - current)) {
                    return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_upgrade") + ": " + upgrade.getName();
                }
                if (current > target && !inventory.insert(getUpgradeStack(upgrade, current - target))) {
                    return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.inventory_full");
                }
            }
        }
        return null;
    }

    public static String validateClear(EntityPlayer player, IUpgradeTile tile, ItemStack configCard) {
        if (!tile.supportsUpgrades()) {
            return null;
        }
        if (player.isCreative()) {
            return null;
        }
        TileComponentUpgrade component = tile.getComponent();
        InventorySimulation inventory = new InventorySimulation(player.inventory, configCard);
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            int current = component.getUpgrades(upgrade);
            if (current > 0 && !inventory.insert(getUpgradeStack(upgrade, current))) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.inventory_full");
            }
        }
        return null;
    }

    public static void consumeUpgradeItems(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data, ItemStack configCard) {
        if (!tile.supportsUpgrades() || player.isCreative()) {
            return;
        }
        boolean hasSuperInfinite = InfiniteUpgradeCardCompat.hasSuperInfiniteUpgrade(player, configCard);
        boolean hasInfinite = InfiniteUpgradeCardCompat.hasInfiniteUpgrade(player, configCard);
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        
        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(player);
            }
        }
        
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) {
                continue;
            }
            if (hasSuperInfinite) continue;
            if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) continue;
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            int actualTarget = target;
            if (ModConfig.limitSavedUpgradeCount) {
                int maxInstallable = upgrade.getMaxInstalled();
                actualTarget = Math.min(target, maxInstallable);
            }
            
            if (actualTarget > current) {
                int needed = actualTarget - current;
                ItemStack upgradeStack = upgrade.getStack();
                
                long fromNetwork = 0;
                if (ae2Storage != null) {
                    fromNetwork = AE2Compat.countItemInNetwork(ae2Storage, upgradeStack.getItem(), upgradeStack.getMetadata());
                }
                
                int fromInventory = countInInventory(player.inventory, upgradeStack.getItem(), upgradeStack.getMetadata());
                int fromBags = ItemCardSlotBag.countInBags(player.inventory, upgradeStack);
                long totalAvailable = fromNetwork + fromInventory + fromBags;
                
                if (totalAvailable < needed) {
                    continue;
                }
                
                int remaining = needed;
                
                if (fromNetwork > 0 && remaining > 0) {
                    int toExtractFromNetwork = (int) Math.min(fromNetwork, remaining);
                    ItemStack extracted = AE2Compat.extractUpgradeFromNetwork(ae2Storage, upgrade, toExtractFromNetwork, ae2Source);
                    if (!extracted.isEmpty()) {
                        remaining -= extracted.getCount();
                    }
                }
                
                if (remaining > 0) {
                    int fromInventoryActual = Math.min(fromInventory, remaining);
                    if (fromInventoryActual > 0) {
                        removeFromInventory(player.inventory, upgradeStack.getItem(), upgradeStack.getMetadata(), fromInventoryActual);
                        remaining -= fromInventoryActual;
                    }
                }
                
                if (remaining > 0) {
                    boolean success = ItemCardSlotBag.consumeFromBags(player.inventory, upgradeStack, remaining);
                    if (success) {
                        remaining = 0;
                    }
                }
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        
    }

    public static void applyStoredUpgrades(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data, ItemStack configCard) {
        if (!tile.supportsUpgrades()) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        boolean hasSuperInfinite = InfiniteUpgradeCardCompat.hasSuperInfiniteUpgrade(player, configCard);
        boolean hasInfinite = InfiniteUpgradeCardCompat.hasInfiniteUpgrade(player, configCard);

        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(player);
            }
        }

        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) {
                continue;
            }
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            int actualTarget = target;
            if (ModConfig.limitSavedUpgradeCount) {
                int maxInstallable = upgrade.getMaxInstalled();
                actualTarget = Math.min(target, maxInstallable);
            }
            int current = component.getUpgrades(upgrade);

            if (current > actualTarget) {
                int toRemove = current - actualTarget;
                // Use setUpgrades to directly set count, bypassing output slot
                component.setUpgrades(upgrade, actualTarget);
                if (!player.isCreative()) {
                    ItemStack returnStack = upgrade.getStack(toRemove);
                    if (!returnStack.isEmpty()) {
                        ItemStack remainder = giveToInventory(player.inventory, returnStack, configCard);
                        if (!remainder.isEmpty()) {
                            player.dropItem(remainder, false);
                        }
                    }
                }
            }
        }

        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) {
                continue;
            }
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            int actualTarget = target;
            if (ModConfig.limitSavedUpgradeCount) {
                int maxInstallable = upgrade.getMaxInstalled();
                actualTarget = Math.min(target, maxInstallable);
            }
            int current = component.getUpgrades(upgrade);

            if (actualTarget > current) {
                int needed = actualTarget - current;

                if (player.isCreative() || hasSuperInfinite) {
                    component.addUpgrades(upgrade, needed);
                    continue;
                }

                if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) {
                    component.addUpgrades(upgrade, needed);
                    continue;
                }

                ItemStack upgradeStack = upgrade.getStack();
                int consumed = consumeUpgradeFromSources(player, upgrade, needed, ae2Storage, ae2Source);

                if (consumed > 0) {
                    if (ModConfig.limitSavedUpgradeCount) {
                        component.addUpgrades(upgrade, consumed);
                    } else {
                        setUpgradeCountDirect(component, upgrade, current + consumed);
                    }
                }
            }
        }
        // Restore AE wireless encryption keys
        if (tile instanceof TileEntity) {
            AEUpgradeCompat.applyWirelessKeys((TileEntity) tile, data);
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    private static int consumeUpgradeFromSources(EntityPlayer player, Upgrade upgrade, int needed, Object ae2Storage, Object ae2Source) {
        List<ModConfig.SourcePriority> priorities = ModConfig.getUpgradeSourcePriorityList();
        int remaining = needed;
        ItemStack upgradeStack = upgrade.getStack();

        for (ModConfig.SourcePriority priority : priorities) {
            if (remaining <= 0) break;

            switch (priority) {
                case NETWORK:
                    if (ae2Storage != null && ae2Source != null && remaining > 0) {
                        long available = AE2Compat.countItemInNetwork(ae2Storage, upgradeStack.getItem(), upgradeStack.getMetadata());
                        if (available > 0) {
                            int toExtract = (int) Math.min(available, remaining);
                            ItemStack extracted = AE2Compat.extractUpgradeFromNetwork(ae2Storage, upgrade, toExtract, ae2Source);
                            if (!extracted.isEmpty()) {
                                remaining -= extracted.getCount();
                            }
                        }
                    }
                    break;
                case CARD_SLOT_BAG:
                    if (remaining > 0) {
                        int fromBags = ItemCardSlotBag.countInBags(player.inventory, upgradeStack);
                        if (fromBags > 0) {
                            int toConsume = Math.min(fromBags, remaining);
                            boolean success = ItemCardSlotBag.consumeFromBags(player.inventory, upgradeStack, toConsume);
                            if (success) {
                                remaining -= toConsume;
                            }
                        }
                    }
                    break;
                case PLAYER_INVENTORY:
                    if (remaining > 0) {
                        int fromInventory = countInInventory(player.inventory, upgradeStack.getItem(), upgradeStack.getMetadata());
                        if (fromInventory > 0) {
                            int toConsume = Math.min(fromInventory, remaining);
                            removeFromInventory(player.inventory, upgradeStack.getItem(), upgradeStack.getMetadata(), toConsume);
                            remaining -= toConsume;
                        }
                    }
                    break;
            }
        }

        return needed - remaining;
    }
    
    private static void setUpgradeCountDirect(TileComponentUpgrade component, Upgrade upgrade, int count) {
        try {
            java.lang.reflect.Field upgradesField = TileComponentUpgrade.class.getDeclaredField("upgrades");
            upgradesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Upgrade, Integer> upgrades = (java.util.Map<Upgrade, Integer>) upgradesField.get(component);
            upgrades.put(upgrade, count);
            component.tileEntity.recalculateUpgradables(upgrade);
            component.tileEntity.markNoUpdateSync();
        } catch (Exception e) {
            component.addUpgrades(upgrade, count - component.getUpgrades(upgrade));
        }
    }


    public static void applyFuzzyStoredUpgrades(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data, ItemStack configCard) {
        if (!tile.supportsUpgrades()) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        boolean hasSuperInfinite = InfiniteUpgradeCardCompat.hasSuperInfiniteUpgrade(player, configCard);
        boolean hasInfinite = InfiniteUpgradeCardCompat.hasInfiniteUpgrade(player, configCard);

        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(player);
            }
        }

        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            
            if (current > target) {
                int toRemove = current - target;
                // Use setUpgrades to directly set count, bypassing output slot
                component.setUpgrades(upgrade, target);
                if (!player.isCreative()) {
                    ItemStack returnStack = upgrade.getStack(toRemove);
                    if (!returnStack.isEmpty()) {
                        ItemStack remainder = giveToInventory(player.inventory, returnStack, configCard);
                        if (!remainder.isEmpty()) {
                            player.dropItem(remainder, false);
                        }
                    }
                }
            }
        }
        
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            if (target > current) {
                int needed = target - current;

                if (player.isCreative() || hasSuperInfinite) {
                    component.addUpgrades(upgrade, needed);
                    continue;
                }

                if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) {
                    component.addUpgrades(upgrade, needed);
                    continue;
                }

                ItemStack upgradeStack = upgrade.getStack();
                int consumed = consumeUpgradeFromSources(player, upgrade, needed, ae2Storage, ae2Source);

                if (consumed > 0) {
                    component.addUpgrades(upgrade, consumed);
                }
            }
        }
        // Restore AE wireless encryption keys
        if (tile instanceof TileEntity) {
            AEUpgradeCompat.applyWirelessKeys((TileEntity) tile, data);
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    public static void applyClear(EntityPlayer player, IUpgradeTile tile, ItemStack configCard) {
        if (!tile.supportsUpgrades()) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();

        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            int current = component.getUpgrades(upgrade);
            if (current > 0) {
                // Use setUpgrades to directly set count, bypassing output slot
                component.setUpgrades(upgrade, 0);
                if (!player.isCreative()) {
                    ItemStack returnStack = upgrade.getStack(current);
                    if (!returnStack.isEmpty()) {
                        ItemStack remainder = giveToInventory(player.inventory, returnStack, configCard);
                        if (!remainder.isEmpty()) {
                            player.dropItem(remainder, false);
                        }
                    }
                }
            }
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    public static boolean fillSupportedUpgrades(EntityPlayer player, IUpgradeTile tile) {
        if (!tile.supportsUpgrades()) {
            return false;
        }
        TileComponentUpgrade component = tile.getComponent();
        boolean changed = false;
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int needed = upgrade.getMaxInstalled() - current;
            if (needed <= 0) {
                continue;
            }
            int toAdd = needed;
            if (!player.isCreative()) {
                toAdd = Math.min(needed, countInInventory(player.inventory, upgrade.getStack().getItem()));
                if (toAdd <= 0) {
                    continue;
                }
                removeFromInventory(player.inventory, upgrade.getStack().getItem(), toAdd);
            }
            int added = component.addUpgrades(upgrade, toAdd);
            if (!player.isCreative() && added < toAdd) {
                ItemStack refund = giveToInventory(player.inventory, getUpgradeStack(upgrade, toAdd - added), ItemStack.EMPTY);
                if (!refund.isEmpty()) {
                    player.dropItem(refund, false);
                }
            }
            if (added > 0) {
                changed = true;
            }
        }
        if (changed) {
            player.inventoryContainer.detectAndSendChanges();
        }
        return changed;
    }

    private static int countInInventory(InventoryPlayer inventory, Item item) {
        int total = 0;
        for (ItemStack stack : inventory.mainInventory) {
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offHandInventory) {
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
    
    private static int countInInventory(InventoryPlayer inventory, Item item, int metadata) {
        int total = 0;
        for (ItemStack stack : inventory.mainInventory) {
            if (stack.getItem() == item && stack.getMetadata() == metadata) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offHandInventory) {
            if (stack.getItem() == item && stack.getMetadata() == metadata) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static Map<Upgrade, Integer> getStoredUpgrades(NBTTagCompound data) {
        
        if (!hasUpgradeData(data)) {
            return Collections.emptyMap();
        }
        NBTTagCompound upgradeData = data.getCompoundTag(UPGRADE_COPY_KEY);
        
        
        Map<Upgrade, Integer> result = Upgrade.buildMap(upgradeData);
        
        return result;
    }

    private static Map<Upgrade, Integer> getInstalledUpgrades(TileComponentUpgrade component) {
        Map<Upgrade, Integer> installed = new LinkedHashMap<>();
        for (Upgrade upgrade : component.getInstalledTypes()) {
            installed.put(upgrade, component.getUpgrades(upgrade));
        }
        return installed;
    }

    private static boolean hasInstalledUpgrades(TileComponentUpgrade component) {
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
            if (component.getUpgrades(upgrade) > 0) {
                return true;
            }
        }
        return false;
    }

    private static void removeFromInventory(InventoryPlayer inventory, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.mainInventory.get(i);
            if (stack.getItem() == item) {
                int toShrink = Math.min(stack.getCount(), remaining);
                stack.shrink(toShrink);
                remaining -= toShrink;
            }
        }
        for (int i = 0; i < inventory.offHandInventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.offHandInventory.get(i);
            if (stack.getItem() == item) {
                int toShrink = Math.min(stack.getCount(), remaining);
                stack.shrink(toShrink);
                remaining -= toShrink;
            }
        }
    }
    
    private static void removeFromInventory(InventoryPlayer inventory, Item item, int metadata, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.mainInventory.get(i);
            if (stack.getItem() == item && stack.getMetadata() == metadata) {
                int toShrink = Math.min(stack.getCount(), remaining);
                stack.shrink(toShrink);
                remaining -= toShrink;
            }
        }
        for (int i = 0; i < inventory.offHandInventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.offHandInventory.get(i);
            if (stack.getItem() == item && stack.getMetadata() == metadata) {
                int toShrink = Math.min(stack.getCount(), remaining);
                stack.shrink(toShrink);
                remaining -= toShrink;
            }
        }
    }

    private static ItemStack giveToInventory(InventoryPlayer inventory, ItemStack stack, ItemStack configCard) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        int remaining = stack.getCount();
        java.util.List<ModConfig.SourcePriority> priorities = ModConfig.getUpgradeReturnPriorityList();
        
        Object ae2Storage = null;
        Object ae2Source = null;
        if (AE2Compat.isAE2Loaded()) {
            ae2Storage = AE2Compat.getStorageGridFromPlayer(inventory.player);
            if (ae2Storage != null) {
                ae2Source = AE2Compat.createActionSourceFromPlayer(inventory.player);
            }
        }
        
        for (ModConfig.SourcePriority priority : priorities) {
            if (remaining <= 0) break;
            
            switch (priority) {
                case NETWORK:
                    if (ae2Storage != null && remaining > 0) {
                        ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(stack, remaining);
                        int inserted = AE2Compat.insertItemToNetwork(ae2Storage, toInsert, ae2Source);
                        remaining -= inserted;
                    }
                    break;
                case CARD_SLOT_BAG:
                    remaining = giveToCardSlotBags(inventory, stack, remaining);
                    break;
                case PLAYER_INVENTORY:
                    remaining = giveToPlayerInventory(inventory, stack, remaining);
                    break;
            }
        }
        
        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }
        
        return ItemCardSlotBag.copyStackWithSize(stack, remaining);
    }
    
    private static int giveToCardSlotBags(InventoryPlayer inventory, ItemStack stack, int remaining) {
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack bagStack = inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(bagStack)) {
                net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(stack, remaining);
                    ItemStack result = handler.insertItem(slot, toInsert, false);
                    if (result.isEmpty()) {
                        remaining = 0;
                    } else {
                        remaining = result.getCount();
                    }
                }
                ItemCardSlotBag.writeHandler(bagStack, handler);
            }
        }
        for (int i = 0; i < inventory.offHandInventory.size() && remaining > 0; i++) {
            ItemStack bagStack = inventory.offHandInventory.get(i);
            if (ItemCardSlotBag.isBag(bagStack)) {
                net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(stack, remaining);
                    ItemStack result = handler.insertItem(slot, toInsert, false);
                    if (result.isEmpty()) {
                        remaining = 0;
                    } else {
                        remaining = result.getCount();
                    }
                }
                ItemCardSlotBag.writeHandler(bagStack, handler);
            }
        }
        if (remaining > 0 && BaublesCompat.isBaublesLoaded()) {
            remaining = BaublesCompat.giveToBaublesBags(inventory.player, stack, remaining);
        }
        return remaining;
    }
    
    private static int giveToPlayerInventory(InventoryPlayer inventory, ItemStack stack, int remaining) {
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack existing = inventory.mainInventory.get(i);
            if (!existing.isEmpty() && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                int space = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining);
                    inventory.mainInventory.set(i, ItemCardSlotBag.copyStackWithSize(existing, existing.getCount() + toAdd));
                    remaining -= toAdd;
                }
            }
        }
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            if (inventory.mainInventory.get(i).isEmpty()) {
                int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                inventory.mainInventory.set(i, ItemCardSlotBag.copyStackWithSize(stack, toAdd));
                remaining -= toAdd;
            }
        }
        return remaining;
    }

    private static final class InventorySimulation {
        private final List<ItemStack> slots = new ArrayList<>();
        private final java.util.Set<Integer> bagSlotIndices = new java.util.HashSet<>();
        private final Object ae2Storage;
        private final Object ae2Source;
        private final EntityPlayer player;

        private InventorySimulation(InventoryPlayer inventory, ItemStack configCard) {
            this.player = inventory.player;
            
            Object tempStorage = null;
            Object tempSource = null;
            if (AE2Compat.isAE2Loaded()) {
                tempStorage = AE2Compat.getStorageGridFromPlayer(inventory.player);
                if (tempStorage != null) {
                    tempSource = AE2Compat.createActionSourceFromPlayer(inventory.player);
                }
            }
            this.ae2Storage = tempStorage;
            this.ae2Source = tempSource;
            
            for (ItemStack stack : inventory.mainInventory) {
                if (ItemCardSlotBag.isBag(stack)) {
                    net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(stack);
                    for (int i = 0; i < handler.getSlots(); i++) {
                        int idx = slots.size();
                        bagSlotIndices.add(idx);
                        ItemStack inSlot = handler.getStackInSlot(i);
                        slots.add(inSlot.isEmpty() ? ItemStack.EMPTY : inSlot.copy());
                    }
                } else {
                    slots.add(stack.copy());
                }
            }
            for (ItemStack stack : inventory.offHandInventory) {
                if (ItemCardSlotBag.isBag(stack)) {
                    net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(stack);
                    for (int i = 0; i < handler.getSlots(); i++) {
                        int idx = slots.size();
                        bagSlotIndices.add(idx);
                        ItemStack inSlot = handler.getStackInSlot(i);
                        slots.add(inSlot.isEmpty() ? ItemStack.EMPTY : inSlot.copy());
                    }
                } else {
                    slots.add(stack.copy());
                }
            }
            if (BaublesCompat.isBaublesLoaded()) {
                try {
                    List<ItemStack> baublesItems = BaublesCompat.getBaublesBagItems(inventory.player);
                    for (ItemStack inSlot : baublesItems) {
                        int idx = slots.size();
                        bagSlotIndices.add(idx);
                        slots.add(inSlot.isEmpty() ? ItemStack.EMPTY : inSlot.copy());
                    }
                } catch (Exception e) {
                }
            }
        }

        private boolean remove(ItemStack stack, int amount) {
            long available = 0;
            for (ItemStack slot : slots) {
                if (!slot.isEmpty() && slot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                    available += slot.getCount();
                }
            }
            long fromNetwork = 0;
            if (ae2Storage != null) {
                fromNetwork = AE2Compat.countItemInNetwork(ae2Storage, stack.getItem(), stack.getMetadata());
                available += fromNetwork;
            }
            if (available < amount) {
                return false;
            }
            int remaining = amount;
            for (ItemStack slot : slots) {
                if (remaining <= 0) {
                    break;
                }
                if (!slot.isEmpty() && slot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                    int toShrink = Math.min(slot.getCount(), remaining);
                    slot.shrink(toShrink);
                    remaining -= toShrink;
                }
            }
            return true;
        }

        private boolean insert(ItemStack stack) {
            int remaining = stack.getCount();
            
            if (ae2Storage != null && ae2Source != null) {
                ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(stack, remaining);
                int inserted = AE2Compat.insertItemToNetwork(ae2Storage, toInsert, ae2Source);
                remaining -= inserted;
            }
            
            for (int i = 0; i < slots.size(); i++) {
                if (remaining <= 0) {
                    return true;
                }
                ItemStack slot = slots.get(i);
                if (!slot.isEmpty() && slot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                    int limit = bagSlotIndices.contains(i) ? ModConfig.getCardSlotBagStackLimit() : Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());
                    int space = limit - slot.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remaining);
                        slots.set(i, ItemCardSlotBag.copyStackWithSize(slot, slot.getCount() + toAdd));
                        remaining -= toAdd;
                    }
                }
            }
            for (int i = 0; i < slots.size(); i++) {
                if (remaining <= 0) {
                    return true;
                }
                ItemStack slot = slots.get(i);
                if (slot.isEmpty()) {
                    int limit = bagSlotIndices.contains(i) ? ModConfig.getCardSlotBagStackLimit() : stack.getMaxStackSize();
                    int toAdd = Math.min(limit, remaining);
                    slots.set(i, ItemCardSlotBag.copyStackWithSize(stack, toAdd));
                    remaining -= toAdd;
                }
            }
            return remaining <= 0;
        }
    }
}
