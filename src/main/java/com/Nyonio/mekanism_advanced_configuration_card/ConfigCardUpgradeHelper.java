package com.Nyonio.mekanism_advanced_configuration_card;

import com.Nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.Nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.Nyonio.mekanism_advanced_configuration_card.compat.MoreMachineCompat;
import com.Nyonio.mekanism_advanced_configuration_card.item.ItemAdvancedConfigurationCard;
import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IFactory;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.item.ItemConfigurationCard;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.SecurityUtils;
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
import net.minecraftforge.common.MinecraftForge;

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
        ItemStack stack = upgrade.getStack().copy();
        stack.setCount(count);
        return stack;
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

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.register(new ConfigCardUpgradeHelper());
    }

    public static NBTTagCompound appendUpgradeData(IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades()) {
            return data;
        }
        NBTTagCompound upgradeData = new NBTTagCompound();
        Map<Upgrade, Integer> installedUpgrades = getInstalledUpgrades(tile.getComponent());
        
        for (Map.Entry<Upgrade, Integer> entry : installedUpgrades.entrySet()) {
            
        }
        Upgrade.saveMap(installedUpgrades, upgradeData);
        data.setTag(UPGRADE_COPY_KEY, upgradeData);
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
    
    public static int getRequiredTierInstallers(TileEntity targetTile, NBTTagCompound data) {
        if (!(targetTile instanceof TileEntityFactory) || !hasFactoryData(data)) {
            return 0;
        }
        TileEntityFactory targetFactory = (TileEntityFactory) targetTile;
        int storedTierOrdinal = getStoredFactoryTier(data);
        int targetTierOrdinal = targetFactory.tier.ordinal();
        if (storedTierOrdinal <= targetTierOrdinal) {
            return 0;
        }
        int required = 0;
        for (int i = targetTierOrdinal; i < storedTierOrdinal; i++) {
            FactoryTier tier = FactoryTier.values()[i];
            if (tier == FactoryTier.ELITE || tier == FactoryTier.ULTIMATE) {
                required += 1;
            } else if (tier == FactoryTier.BASIC || tier == FactoryTier.ADVANCED) {
                required += 1;
            }
        }
        return required;
    }
    
    public static BaseTier getRequiredTierInstallerType(TileEntity targetTile, NBTTagCompound data) {
        if (!(targetTile instanceof TileEntityFactory) || !hasFactoryData(data)) {
            return null;
        }
        TileEntityFactory targetFactory = (TileEntityFactory) targetTile;
        int storedTierOrdinal = getStoredFactoryTier(data);
        int targetTierOrdinal = targetFactory.tier.ordinal();
        if (storedTierOrdinal <= targetTierOrdinal) {
            return null;
        }
        return BaseTier.values()[targetTierOrdinal + 1];
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

    public static String getModeSwitchMessage(PasteMode mode) {
        return "Config card switched to " + mode.displayName() + " mode";
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

    public static String getStoredDataName(NBTTagCompound data) {
        return data.hasKey("dataType") ? data.getString("dataType") : null;
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
            return clearUpgradesFromTarget(player, tile, sendSuccessMessage);
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
            String failure = validateFuzzyPaste(player, upgradeTile, data);
            if (failure != null) {
                return failure;
            }
            applyFuzzyStoredUpgrades(player, upgradeTile, data);
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
        
        if (!isConfigurationCompatible(tile, storedType, data)) {
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
            String failure = validatePaste(player, upgradeTile, data);
            if (failure != null) {
                return failure;
            }
        }
        TileEntityFactory upgradedFactory = null;
        if (hasFactoryData(data) && tile instanceof TileEntityFactory) {
            TileEntityFactory targetFactory = (TileEntityFactory) tile;
            int storedTierOrdinal = getStoredFactoryTier(data);
            int targetTierOrdinal = targetFactory.tier.ordinal();
            
            if (storedTierOrdinal > targetTierOrdinal) {
                String failure = validateAndConsumeTierInstallers(player, tile, data);
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
        // Handle Factory -> ITierUpgradeable machine upgrade
        if (hasFactoryData(data) && !(tile instanceof TileEntityFactory) && tile instanceof ITierUpgradeable && !MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = getStoredFactoryTier(data);
            String failure = validateAndConsumeTierInstallersForUpgradeableMachine(player, tile, storedTierOrdinal);
            if (failure != null) {
                return failure;
            }
            performUpgradeableMachineUpgrade(player, tile, storedTierOrdinal);
        }
        // Handle MoreMachine -> ITierUpgradeable machine upgrade
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && !(tile instanceof TileEntityFactory) && tile instanceof ITierUpgradeable && !MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = MoreMachineCompat.getStoredTier(data);
            String failure = validateAndConsumeTierInstallersForUpgradeableMachine(player, tile, storedTierOrdinal);
            if (failure != null) {
                return failure;
            }
            performUpgradeableMachineUpgrade(player, tile, storedTierOrdinal);
        }
        // Handle MoreMachine tier upgrade
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = MoreMachineCompat.getStoredTier(data);
            int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
            if (storedTierOrdinal > currentTierOrdinal) {
                String failure = validateAndConsumeMoreMachineTierInstallers(player, tile, storedTierOrdinal);
                if (failure != null) {
                    return failure;
                }
                performMoreMachineTierUpgrade(player, tile, storedTierOrdinal);
            }
        }
        // Handle Factory -> MoreMachine tier conversion
        if (MoreMachineCompat.isMoreMachineLoaded() && hasFactoryData(data) && !(tile instanceof TileEntityFactory) && MoreMachineCompat.isTierMachine(tile)) {
            int storedTierOrdinal = getStoredFactoryTier(data);
            int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
            if (storedTierOrdinal > currentTierOrdinal) {
                String failure = validateAndConsumeMoreMachineTierInstallers(player, tile, storedTierOrdinal);
                if (failure != null) {
                    return failure;
                }
                performMoreMachineTierUpgrade(player, tile, storedTierOrdinal);
            }
        }
        // Handle MoreMachine -> Factory tier conversion
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && !hasFactoryData(data) && tile instanceof TileEntityFactory) {
            TileEntityFactory targetFactory = (TileEntityFactory) tile;
            int storedTierOrdinal = MoreMachineCompat.getStoredTier(data);
            int targetTierOrdinal = targetFactory.tier.ordinal();
            if (storedTierOrdinal > targetTierOrdinal) {
                String failure = validateAndConsumeTierInstallersForFactory(player, targetFactory, storedTierOrdinal);
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
        ItemAdvancedConfigurationCard.applyBaseData(tile, data);
        if (hasUpgradeData(data) && upgradeTile != null) {
            consumeUpgradeItems(player, upgradeTile, data);
            applyStoredUpgrades(player, upgradeTile, data);
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
            
            boolean success = currentFactory.upgrade(nextTier);
            
            if (!success) {
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
    
    private static String validateAndConsumeTierInstallersForUpgradeableMachine(EntityPlayer player, TileEntity tile, int targetTierOrdinal) {
        if (player.isCreative()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasInfiniteFactoryInstaller(player)) {
            return null;
        }
        if (!(tile instanceof ITierUpgradeable)) {
            return null;
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
            int totalAvailable = countInInventory + countInBags;
            
            if (totalAvailable < 1) {
                missingTiers.add(getLocalizedTierName(requiredTier));
            }
            tempTierOrdinal++;
        }
        
        if (!missingTiers.isEmpty()) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_tier_installers") + ": " + String.join(", ", missingTiers);
        }
        
        tempTierOrdinal = -1;
        while (tempTierOrdinal < targetTierOrdinal) {
            BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
            
            ItemStack tierInstallerStack = getTierInstallerStack(requiredTier);
            int countInInventory = countInInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            
            if (countInInventory > 0) {
                removeFromInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata(), 1);
            } else {
                ItemCardSlotBag.consumeFromBags(player.inventory, tierInstallerStack, 1);
            }
            tempTierOrdinal++;
        }
        
        player.inventoryContainer.detectAndSendChanges();
        return null;
    }
    
    private static void performUpgradeableMachineUpgrade(EntityPlayer player, TileEntity tile, int targetTierOrdinal) {
        if (!(tile instanceof ITierUpgradeable)) {
            return;
        }
        ITierUpgradeable upgradeable = (ITierUpgradeable) tile;
        
        boolean success = upgradeable.upgrade(BaseTier.BASIC);
        
        if (!success) {
            return;
        }
        
        World world = tile.getWorld();
        BlockPos pos = tile.getPos();
        TileEntity newTile = world.getTileEntity(pos);
        
        if (newTile instanceof TileEntityFactory) {
            TileEntityFactory factory = (TileEntityFactory) newTile;
            
            while (factory.tier.ordinal() < targetTierOrdinal) {
                BaseTier nextTier = BaseTier.values()[factory.tier.ordinal() + 1];
                
                boolean factorySuccess = factory.upgrade(nextTier);
                
                if (!factorySuccess) {
                    break;
                }
                
                TileEntity updatedTile = world.getTileEntity(pos);
                if (updatedTile instanceof TileEntityFactory) {
                    factory = (TileEntityFactory) updatedTile;
                } else {
                    newTile = updatedTile;
                    break;
                }
            }
        } else if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.isTierMachine(newTile)) {
            performMoreMachineTierUpgrade(player, newTile, targetTierOrdinal);
        }
    }
    
    private static String validateAndConsumeTierInstallers(EntityPlayer player, TileEntity tile, NBTTagCompound data) {
        if (!(tile instanceof TileEntityFactory) || !hasFactoryData(data)) {
            return null;
        }
        int storedTierOrdinal = getStoredFactoryTier(data);
        return validateAndConsumeTierInstallersForFactory(player, (TileEntityFactory) tile, storedTierOrdinal);
    }
    
    private static String validateAndConsumeTierInstallersForFactory(EntityPlayer player, TileEntityFactory targetFactory, int targetTierOrdinal) {
        if (player.isCreative()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasInfiniteFactoryInstaller(player)) {
            return null;
        }
        int currentTierOrdinal = targetFactory.tier.ordinal();
        
        if (targetTierOrdinal <= currentTierOrdinal) {
            return null;
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
            int totalAvailable = countInInventory + countInBags;
            
            if (totalAvailable < 1) {
                missingTiers.add(getLocalizedTierName(requiredTier));
            }
            tempTierOrdinal++;
        }
        if (!missingTiers.isEmpty()) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.missing_tier_installers") + ": " + String.join(", ", missingTiers);
        }
        
        tempTierOrdinal = currentTierOrdinal;
        while (tempTierOrdinal < targetTierOrdinal) {
            BaseTier requiredTier = BaseTier.values()[tempTierOrdinal + 1];
            
            ItemStack tierInstallerStack = getTierInstallerStack(requiredTier);
            int countInInventory = countInInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata());
            
            if (countInInventory > 0) {
                removeFromInventory(player.inventory, tierInstallerStack.getItem(), tierInstallerStack.getMetadata(), 1);
            } else {
                ItemCardSlotBag.consumeFromBags(player.inventory, tierInstallerStack, 1);
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
    
    private static String validateAndConsumeMoreMachineTierInstallers(EntityPlayer player, TileEntity tile, int targetTierOrdinal) {
        if (!MoreMachineCompat.isMoreMachineLoaded() || !MoreMachineCompat.isTierMachine(tile)) {
            return null;
        }
        if (player.isCreative()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasInfiniteFactoryInstaller(player)) {
            return null;
        }
        if (!MoreMachineCompat.canInstall(tile)) {
            return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.cannot_upgrade");
        }
        int currentTierOrdinal = MoreMachineCompat.getTierOrdinal(tile);
        if (targetTierOrdinal <= currentTierOrdinal) {
            return null;
        }
        
        Item compositeTierInstaller = MoreMachineCompat.getCompositeTierInstallerItem();
        Item tierInstaller = MoreMachineCompat.getTierInstallerItem();
        
        int compositeCountInInventory = 0;
        int compositeCountInBags = 0;
        if (compositeTierInstaller != null) {
            compositeCountInInventory = countInInventory(player.inventory, compositeTierInstaller);
            compositeCountInBags = ItemCardSlotBag.countInBags(player.inventory, new ItemStack(compositeTierInstaller));
        }
        int totalCompositeAvailable = compositeCountInInventory + compositeCountInBags;
        
        if (totalCompositeAvailable >= 1) {
            if (compositeCountInInventory > 0) {
                removeFromInventory(player.inventory, compositeTierInstaller, 1);
            } else {
                ItemCardSlotBag.consumeFromBags(player.inventory, new ItemStack(compositeTierInstaller), 1);
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
                int totalAvailable = countInInventory + countInBags;
                
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
                    
                    if (countInInventory > 0) {
                        removeFromInventory(player.inventory, tierInstaller, requiredTier.ordinal(), 1);
                    } else {
                        ItemCardSlotBag.consumeFromBags(player.inventory, new ItemStack(tierInstaller, 1, requiredTier.ordinal()), 1);
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
            if (!MoreMachineCompat.canInstall(currentTile)) {
                break;
            }
            BaseTier nextTier = BaseTier.values()[currentTierOrdinal + 1];
            boolean success = MoreMachineCompat.upgradeToTier(currentTile, nextTier.ordinal());
            if (!success) {
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
        
        if (sourceIsFactory && targetIsFactory && hasFactoryData(data)) {
            return true;
        } else if (hasFactoryData(data) && targetIsFactory) {
            return true;
        }
        if (hasTierData(data) && tile instanceof ITierUpgradeable) {
            return true;
        }
        if (MoreMachineCompat.isMoreMachineLoaded() && MoreMachineCompat.hasTierData(data) && MoreMachineCompat.isTierMachine(tile)) {
            return true;
        }
        
        if (sourceIsFactory && hasFactoryData(data) && !targetIsFactory) {
            if (tile instanceof ITierUpgradeable) {
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
            if (MoreMachineCompat.hasTierData(data) && !targetIsFactory && tile instanceof ITierUpgradeable && !MoreMachineCompat.isTierMachine(tile)) {
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

    public static String clearUpgradesFromTarget(EntityPlayer player, TileEntity tile, boolean sendSuccessMessage) {
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
        String failure = validateClear(player, upgradeTile);
        if (failure != null) {
            return failure;
        }
        applyClear(player, upgradeTile);
        if (sendSuccessMessage) {
            player.sendMessage(new TextComponentString(EnumColor.DARK_GREEN + new TextComponentTranslation("message.mekanism_advanced_configuration_card.configuration_set").getFormattedText()));
        }
        return null;
    }

    public static String validatePaste(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasAnyInfiniteUpgrade(player)) {
            return null;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        InventorySimulation inventory = new InventorySimulation(player.inventory);
        for (Upgrade upgrade : Upgrade.values()) {
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

    public static String validateFuzzyPaste(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades()) {
            return null;
        }
        if (InfiniteUpgradeCardCompat.hasAnyInfiniteUpgrade(player)) {
            return null;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        InventorySimulation inventory = new InventorySimulation(player.inventory);
        for (Upgrade upgrade : Upgrade.values()) {
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            if (!player.isCreative()) {
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

    public static String validateClear(EntityPlayer player, IUpgradeTile tile) {
        if (!tile.supportsUpgrades()) {
            return null;
        }
        if (player.isCreative()) {
            return null;
        }
        TileComponentUpgrade component = tile.getComponent();
        InventorySimulation inventory = new InventorySimulation(player.inventory);
        for (Upgrade upgrade : Upgrade.values()) {
            int current = component.getUpgrades(upgrade);
            if (current > 0 && !inventory.insert(getUpgradeStack(upgrade, current))) {
                return mekanism.common.util.LangUtils.localize("message.mekanism_advanced_configuration_card.inventory_full");
            }
        }
        return null;
    }

    public static void consumeUpgradeItems(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades() || player.isCreative()) {
            return;
        }
        if (InfiniteUpgradeCardCompat.hasAnyInfiniteUpgrade(player)) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        
        for (Upgrade upgrade : Upgrade.values()) {
            if (!component.supports(upgrade)) {
                continue;
            }
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
                int fromInventory = countInInventory(player.inventory, upgradeStack.getItem(), upgradeStack.getMetadata());
                int fromBags = ItemCardSlotBag.countInBags(player.inventory, upgradeStack);
                int totalAvailable = fromInventory + fromBags;
                
                if (totalAvailable < needed) {
                    continue;
                }
                int fromInventoryActual = Math.min(fromInventory, needed);
                if (fromInventoryActual > 0) {
                    removeFromInventory(player.inventory, upgradeStack.getItem(), upgradeStack.getMetadata(), fromInventoryActual);
                    
                }
                int remaining = needed - fromInventoryActual;
                if (remaining > 0) {
                    boolean success = ItemCardSlotBag.consumeFromBags(player.inventory, upgradeStack, remaining);
                    
                }
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        
    }

    public static void applyStoredUpgrades(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades()) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        
        
        for (Upgrade upgrade : Upgrade.values()) {
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
            if (current != actualTarget) {
                
            }
            int removed = 0;
            int initialCurrent = current;
            while (current > actualTarget) {
                component.removeUpgrade(upgrade, false);
                removed++;
                if (!player.isCreative()) {
                    ItemStack extracted = upgrade.getStack().copy();
                    extracted.setCount(1);
                    ItemStack remainder = giveToInventory(player.inventory, extracted);
                    if (!remainder.isEmpty()) {
                        player.dropItem(remainder, false);
                    }
                }
                int newCurrent = component.getUpgrades(upgrade);
                if (newCurrent == current) {
                    
                    break;
                }
                current = newCurrent;
            }
            if (removed > 0) {
                
            }
        }
        for (Upgrade upgrade : Upgrade.values()) {
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
                if (ModConfig.limitSavedUpgradeCount) {
                    component.addUpgrades(upgrade, actualTarget - current);
                } else {
                    setUpgradeCountDirect(component, upgrade, actualTarget);
                }
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        
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

    public static void applyFuzzyStoredUpgrades(EntityPlayer player, IUpgradeTile tile, NBTTagCompound data) {
        if (!tile.supportsUpgrades()) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();
        Map<Upgrade, Integer> desired = getStoredUpgrades(data);
        for (Upgrade upgrade : Upgrade.values()) {
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            while (current > target) {
                component.removeUpgrade(upgrade, false);
                if (!player.isCreative()) {
                    ItemStack extracted = upgrade.getStack().copy();
                    extracted.setCount(1);
                    ItemStack remainder = giveToInventory(player.inventory, extracted);
                    if (!remainder.isEmpty()) {
                        player.dropItem(remainder, false);
                    }
                }
                current--;
            }
        }
        for (Upgrade upgrade : Upgrade.values()) {
            if (!component.supports(upgrade)) {
                continue;
            }
            int current = component.getUpgrades(upgrade);
            int target = desired.containsKey(upgrade) ? desired.get(upgrade) : 0;
            if (target > current) {
                if (!player.isCreative()) {
                    int needed = target - current;
                    int fromInventory = countInInventory(player.inventory, upgrade.getStack().getItem());
                    int fromBags = ItemCardSlotBag.countInBags(player.inventory, upgrade.getStack());
                    int totalAvailable = fromInventory + fromBags;
                    if (totalAvailable < needed) {
                        continue;
                    }
                    int fromInventoryActual = Math.min(fromInventory, needed);
                    if (fromInventoryActual > 0) {
                        removeFromInventory(player.inventory, upgrade.getStack().getItem(), fromInventoryActual);
                    }
                    int remaining = needed - fromInventoryActual;
                    if (remaining > 0) {
                        ItemCardSlotBag.consumeFromBags(player.inventory, upgrade.getStack(), remaining);
                    }
                }
                component.addUpgrades(upgrade, target - current);
            }
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    public static void applyClear(EntityPlayer player, IUpgradeTile tile) {
        if (!tile.supportsUpgrades()) {
            return;
        }
        TileComponentUpgrade component = tile.getComponent();
        for (Upgrade upgrade : Upgrade.values()) {
            int current = component.getUpgrades(upgrade);
            while (current > 0) {
                component.removeUpgrade(upgrade, false);
                if (!player.isCreative()) {
                    ItemStack extracted = upgrade.getStack().copy();
                    extracted.setCount(1);
                    ItemStack remainder = giveToInventory(player.inventory, extracted);
                    if (!remainder.isEmpty()) {
                        player.dropItem(remainder, false);
                    }
                }
                current--;
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
        for (Upgrade upgrade : Upgrade.values()) {
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
                ItemStack refund = giveToInventory(player.inventory, getUpgradeStack(upgrade, toAdd - added));
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
        Map<Upgrade, Integer> installed = new EnumMap<>(Upgrade.class);
        for (Upgrade upgrade : component.getInstalledTypes()) {
            installed.put(upgrade, component.getUpgrades(upgrade));
        }
        return installed;
    }

    private static boolean hasInstalledUpgrades(TileComponentUpgrade component) {
        for (Upgrade upgrade : Upgrade.values()) {
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

    private static ItemStack giveToInventory(InventoryPlayer inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        int remaining = stack.getCount();
        
        // First, try to insert into card slot bags (if enabled in config)
        if (ModConfig.prioritizeCardSlotBag) {
            for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
                ItemStack bagStack = inventory.mainInventory.get(i);
                if (ItemCardSlotBag.isBag(bagStack) && ItemCardSlotBag.isSupportedBagItem(stack)) {
                    net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
                    for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                        ItemStack existing = handler.getStackInSlot(slot);
                        if (!existing.isEmpty() && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                            int space = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                            if (space > 0) {
                                int toAdd = Math.min(space, remaining);
                                existing.grow(toAdd);
                                remaining -= toAdd;
                            }
                        }
                    }
                    for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                        ItemStack existing = handler.getStackInSlot(slot);
                        if (existing.isEmpty()) {
                            int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                            handler.setStackInSlot(slot, new ItemStack(stack.getItem(), toAdd, stack.getMetadata(), stack.getTagCompound()));
                            remaining -= toAdd;
                        }
                    }
                    ItemCardSlotBag.writeHandler(bagStack, handler);
                }
            }
            for (int i = 0; i < inventory.offHandInventory.size() && remaining > 0; i++) {
                ItemStack bagStack = inventory.offHandInventory.get(i);
                if (ItemCardSlotBag.isBag(bagStack) && ItemCardSlotBag.isSupportedBagItem(stack)) {
                    net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
                    for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                        ItemStack existing = handler.getStackInSlot(slot);
                        if (!existing.isEmpty() && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                            int space = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                            if (space > 0) {
                                int toAdd = Math.min(space, remaining);
                                existing.grow(toAdd);
                                remaining -= toAdd;
                            }
                        }
                    }
                    for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                        ItemStack existing = handler.getStackInSlot(slot);
                        if (existing.isEmpty()) {
                            int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                            handler.setStackInSlot(slot, new ItemStack(stack.getItem(), toAdd, stack.getMetadata(), stack.getTagCompound()));
                            remaining -= toAdd;
                        }
                    }
                    ItemCardSlotBag.writeHandler(bagStack, handler);
                }
            }
            // Also try baubles bags first
            if (remaining > 0 && BaublesCompat.isBaublesLoaded()) {
                remaining = BaublesCompat.giveToBaublesBags(inventory.player, stack, remaining);
            }
        }
        
        // Then, try to insert into regular inventory
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack existing = inventory.mainInventory.get(i);
            if (!existing.isEmpty() && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                int space = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining);
                    existing.grow(toAdd);
                    remaining -= toAdd;
                }
            }
        }
        for (int i = 0; i < inventory.mainInventory.size() && remaining > 0; i++) {
            if (inventory.mainInventory.get(i).isEmpty()) {
                int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                inventory.mainInventory.set(i, new ItemStack(stack.getItem(), toAdd, stack.getMetadata(), stack.getTagCompound()));
                remaining -= toAdd;
            }
        }
        for (int i = 0; i < inventory.offHandInventory.size() && remaining > 0; i++) {
            ItemStack existing = inventory.offHandInventory.get(i);
            if (!existing.isEmpty() && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                int space = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining);
                    existing.grow(toAdd);
                    remaining -= toAdd;
                }
            }
        }
        for (int i = 0; i < inventory.offHandInventory.size() && remaining > 0; i++) {
            if (inventory.offHandInventory.get(i).isEmpty()) {
                int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                inventory.offHandInventory.set(i, new ItemStack(stack.getItem(), toAdd, stack.getMetadata(), stack.getTagCompound()));
                remaining -= toAdd;
            }
        }
        
        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }
        
        return new ItemStack(stack.getItem(), remaining, stack.getMetadata(), stack.getTagCompound());
    }

    private static final class InventorySimulation {
        private final List<ItemStack> slots = new ArrayList<>();

        private InventorySimulation(InventoryPlayer inventory) {
            for (ItemStack stack : inventory.mainInventory) {
                if (ItemCardSlotBag.isBag(stack)) {
                    net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(stack);
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack inSlot = handler.getStackInSlot(i);
                        if (!inSlot.isEmpty()) {
                            slots.add(inSlot.copy());
                        }
                    }
                } else {
                    slots.add(stack.copy());
                }
            }
            for (ItemStack stack : inventory.offHandInventory) {
                if (ItemCardSlotBag.isBag(stack)) {
                    net.minecraftforge.items.ItemStackHandler handler = ItemCardSlotBag.readHandler(stack);
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack inSlot = handler.getStackInSlot(i);
                        if (!inSlot.isEmpty()) {
                            slots.add(inSlot.copy());
                        }
                    }
                } else {
                    slots.add(stack.copy());
                }
            }
            if (BaublesCompat.isBaublesLoaded()) {
                try {
                    List<ItemStack> baublesItems = BaublesCompat.getBaublesBagItems(inventory.player);
                    for (ItemStack inSlot : baublesItems) {
                        slots.add(inSlot.copy());
                    }
                } catch (Exception e) {
                }
            }
        }

        private boolean remove(ItemStack stack, int amount) {
            int available = 0;
            for (ItemStack slot : slots) {
                if (!slot.isEmpty() && slot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                    available += slot.getCount();
                }
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
            for (ItemStack slot : slots) {
                if (remaining <= 0) {
                    return true;
                }
                if (!slot.isEmpty() && slot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                    int space = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize()) - slot.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remaining);
                        slot.grow(toAdd);
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
                    int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                    slots.set(i, new ItemStack(stack.getItem(), toAdd, stack.getMetadata(), stack.getTagCompound()));
                    remaining -= toAdd;
                }
            }
            return remaining <= 0;
        }
    }
}
