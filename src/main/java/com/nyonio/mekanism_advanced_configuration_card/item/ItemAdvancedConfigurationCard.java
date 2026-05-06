package com.nyonio.mekanism_advanced_configuration_card.item;

import com.nyonio.mekanism_advanced_configuration_card.ConfigCardUpgradeHelper;
import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.AE2Compat;
import com.nyonio.mekanism_advanced_configuration_card.compat.MoreMachineCompat;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.common.Mekanism;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAdvancedConfigurationCard extends Item {
    
    public ItemAdvancedConfigurationCard() {
        setMaxStackSize(1);
    }
    
    public String getEncryptionKey(ItemStack item) {
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null && tag.hasKey(AE2Compat.AE2_NETWORK_KEY)) {
            return tag.getString(AE2Compat.AE2_NETWORK_KEY);
        }
        return "";
    }
    
    public void setEncryptionKey(ItemStack item, String encKey, String name) {
        NBTTagCompound tag = item.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            item.setTagCompound(tag);
        }
        if (encKey != null && !encKey.isEmpty()) {
            tag.setString(AE2Compat.AE2_NETWORK_KEY, encKey);
        } else {
            tag.removeTag(AE2Compat.AE2_NETWORK_KEY);
        }
    }
    
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        
        TileEntity tile = world.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);
        
        if (player.isSneaking()) {
            if (tile == null) {
                return EnumActionResult.PASS;
            }
            if (!SecurityUtils.canAccess(player, tile)) {
                SecurityUtils.displayNoAccess(player);
                return EnumActionResult.FAIL;
            }
            if (!canSaveConfiguration(tile, side)) {
                return EnumActionResult.PASS;
            }
            return saveConfiguration(player, tile, side, stack);
        } else {
            if (tile == null || !canSaveConfiguration(tile, side)) {
                return EnumActionResult.PASS;
            }
            if (!SecurityUtils.canAccess(player, tile)) {
                SecurityUtils.displayNoAccess(player);
                return EnumActionResult.FAIL;
            }
            return pasteConfiguration(player, tile, side, stack);
        }
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        
        if (!world.isRemote) {
            if (player.isSneaking()) {
                net.minecraft.util.math.RayTraceResult rayTrace = player.rayTrace(5.0D, 1.0F);
                if (rayTrace != null && rayTrace.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                    TileEntity tile = world.getTileEntity(rayTrace.getBlockPos());
                    if (tile != null && canSaveConfiguration(tile, rayTrace.sideHit)) {
                        return new ActionResult<>(EnumActionResult.PASS, stack);
                    }
                }
                clearCardData(stack);
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.DARK_GREEN + mekanism.common.util.LangUtils.localize("message." + MekConfigCardUpgradesMod.MOD_ID + ".card_cleared")));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            } else {
                int configCardSlot = findConfigCardSlot(player, stack);
                com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler.BagInfo bagInfo = com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler.findFirstBag(player);
                if (bagInfo != null) {
                    int bagSource = bagInfo.isBaubles ? com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler.SOURCE_BAUBLES : com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler.SOURCE_MAIN;
                    player.openGui(MekConfigCardUpgradesMod.instance, com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler.CARD_SLOT_BAG, world, configCardSlot, bagSource, bagInfo.slotIndex);
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                }
            }
        }
        
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
    
    private int findConfigCardSlot(EntityPlayer player, ItemStack configCard) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            if (player.inventory.mainInventory.get(i) == configCard) {
                return i;
            }
        }
        return -1;
    }
    
    private boolean canSaveConfiguration(TileEntity tile, EnumFacing side) {
        if (CapabilityUtils.hasCapability(tile, Capabilities.CONFIG_CARD_CAPABILITY, side)) {
            return true;
        }
        if (tile instanceof ISideConfiguration) {
            return true;
        }
        if (tile instanceof IUpgradeTile) {
            return true;
        }
        return false;
    }
    
    private void clearCardData(ItemStack stack) {
        NBTTagCompound dataMap = ItemDataUtils.getDataMap(stack);
        dataMap.removeTag("data");
        setEncoded(stack, false);
    }
    
    private EnumActionResult saveConfiguration(EntityPlayer player, TileEntity tile, EnumFacing side, ItemStack stack) {
        if (!CapabilityUtils.hasCapability(tile, Capabilities.CONFIG_CARD_CAPABILITY, side)) {
            if (!(tile instanceof ISideConfiguration) && !(tile instanceof IUpgradeTile)) {
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + mekanism.common.util.LangUtils.localize("message." + MekConfigCardUpgradesMod.MOD_ID + ".no_config_support")));
                return EnumActionResult.FAIL;
            }
        }
        
        NBTTagCompound data = new NBTTagCompound();
        
        IConfigCardAccess.ISpecialConfigData configData = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
        boolean hasSpecialConfigData = configData != null;
        
        if (hasSpecialConfigData) {
            NBTTagCompound configNbt = configData.getConfigurationData(new NBTTagCompound());
            if (configNbt != null) {
                data = configNbt;
            }
        }
        
        String dataType = getNameFromTile(tile, side);
        data.setString("dataType", dataType);
        data.setString("tileClass", tile.getClass().getName());
        data.setBoolean("hasSpecialConfigData", hasSpecialConfigData);
        
        saveBaseData(tile, data);
        
        if (tile instanceof IUpgradeTile) {
            IUpgradeTile upgradeTile = (IUpgradeTile) tile;
            if (upgradeTile.supportsUpgrades()) {
                ConfigCardUpgradeHelper.appendUpgradeData(upgradeTile, data);
            }
        }
        
        ConfigCardUpgradeHelper.appendFactoryData(tile, data);
        
        ConfigCardUpgradeHelper.appendTierData(tile, data);
        
        setData(stack, data);
        setEncoded(stack, true);
        
        sendConfigMessage(player, dataType, data, true);
        return EnumActionResult.SUCCESS;
    }
    
    private void sendConfigMessage(EntityPlayer player, String dataType, NBTTagCompound data, boolean isSave) {
        String localizedDataType = getLocalizedDataType(data, dataType);
        
        if (isSave) {
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.GREY + mekanism.common.util.LangUtils.localize("tooltip.configurationCard.got").replaceAll("%s", EnumColor.INDIGO + localizedDataType + EnumColor.GREY)));
        } else {
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.DARK_GREEN + mekanism.common.util.LangUtils.localize("tooltip.configurationCard.set").replaceAll("%s", EnumColor.INDIGO + localizedDataType + EnumColor.DARK_GREEN)));
        }
    }
    
    private String getLocalizedDataType(NBTTagCompound data, String dataType) {
        if (dataType == null || dataType.isEmpty()) {
            return "";
        }
        return dataType;
    }
    
    private String getNameFromTile(TileEntity tile, EnumFacing side) {
        if (CapabilityUtils.hasCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side)) {
            IConfigCardAccess.ISpecialConfigData special = CapabilityUtils.getCapability(tile, Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY, side);
            return special.getDataType();
        }
        if (tile instanceof mekanism.common.tile.prefab.TileEntityContainerBlock) {
            return ((mekanism.common.tile.prefab.TileEntityContainerBlock) tile).getName();
        }
        return tile.getClass().getSimpleName();
    }
    
    private void saveBaseData(TileEntity tile, NBTTagCompound data) {
        if (tile instanceof IRedstoneControl) {
            IRedstoneControl control = (IRedstoneControl) tile;
            data.setInteger("controlType", control.getControlType().ordinal());
        }
        if (tile instanceof ISideConfiguration) {
            ISideConfiguration configuration = (ISideConfiguration) tile;
            configuration.getConfig().write(data);
            configuration.getEjector().write(data);
        }
    }
    
    public static void applyBaseData(TileEntity tile, NBTTagCompound data) {
        if (tile instanceof IRedstoneControl) {
            IRedstoneControl control = (IRedstoneControl) tile;
            int controlType = data.getInteger("controlType");
            IRedstoneControl.RedstoneControl[] values = IRedstoneControl.RedstoneControl.values();
            if (controlType >= 0 && controlType < values.length) {
                control.setControlType(values[controlType]);
            }
        }
        if (tile instanceof ISideConfiguration) {
            ISideConfiguration configuration = (ISideConfiguration) tile;
            configuration.getConfig().read(data);
            configuration.getEjector().read(data);
        }
    }
    
    private EnumActionResult pasteConfiguration(EntityPlayer player, TileEntity tile, EnumFacing side, ItemStack stack) {
        NBTTagCompound data = getData(stack);
        if (data == null || data.hasNoTags()) {
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + new TextComponentTranslation("message." + MekConfigCardUpgradesMod.MOD_ID + ".paste_failed").getFormattedText()));
            return EnumActionResult.FAIL;
        }
        
        String failure = ConfigCardUpgradeHelper.pasteCardToTarget(player, tile, side, stack, true);
        if (failure != null) {
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + failure));
            return EnumActionResult.FAIL;
        }
        
        return EnumActionResult.SUCCESS;
    }
    
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        
        if (isEncoded(stack)) {
            tooltip.add(EnumColor.AQUA + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.encoded").getFormattedText());
            
            NBTTagCompound data = getData(stack);
            if (data != null) {
                if (data.hasKey("dataType")) {
                    String typeName = data.getString("dataType");
                    tooltip.add(EnumColor.WHITE + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.type").getFormattedText() + ": " + EnumColor.INDIGO + typeName);
                }
                
                if (ConfigCardUpgradeHelper.hasUpgradeData(data)) {
                    tooltip.add(EnumColor.WHITE + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.contains_upgrade").getFormattedText());
                }
            }
        } else {
            tooltip.add(EnumColor.GREY + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.blank").getFormattedText());
        }
        
        tooltip.add("");
        if (AE2Compat.isAE2Loaded() && AE2Compat.hasNetworkKey(stack.getTagCompound())) {
            tooltip.add(EnumColor.GREY + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.network_linked").getFormattedText());
        } else {
            tooltip.add(EnumColor.GREY + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.network_unlinked").getFormattedText());
        }
        
        tooltip.add("");
        tooltip.add(EnumColor.AQUA + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.sneak_right_click_save").getFormattedText() + " " + EnumColor.GREY + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.clear_notice").getFormattedText());
        tooltip.add(EnumColor.AQUA + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.right_click_paste").getFormattedText());
        tooltip.add("");
        tooltip.add(EnumColor.YELLOW + new TextComponentTranslation("tooltip." + MekConfigCardUpgradesMod.MOD_ID + ".advanced_configuration_card.factory_tier_notice").getFormattedText());
    }
    
    public static void setData(ItemStack stack, NBTTagCompound data) {
        ItemDataUtils.setCompound(stack, "data", data);
    }
    
    public static NBTTagCompound getData(ItemStack stack) {
        return ItemDataUtils.getCompound(stack, "data");
    }
    
    public static void setEncoded(ItemStack stack, boolean encoded) {
        ItemDataUtils.setBoolean(stack, "encoded", encoded);
    }
    
    public static boolean isEncoded(ItemStack stack) {
        return ItemDataUtils.getBoolean(stack, "encoded");
    }
}
