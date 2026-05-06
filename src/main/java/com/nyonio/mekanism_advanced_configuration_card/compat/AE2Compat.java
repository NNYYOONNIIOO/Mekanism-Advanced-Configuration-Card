package com.nyonio.mekanism_advanced_configuration_card.compat;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.ILocatable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.helpers.PlayerSource;
import mekanism.common.Upgrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;

public class AE2Compat {
    public static final String MOD_ID = "appliedenergistics2";
    public static final String AE2_NETWORK_KEY = "ae2_network_key";
    
    private static boolean ae2Loaded = false;
    
    public static void init() {
        ae2Loaded = Loader.isModLoaded(MOD_ID);
    }
    
    public static boolean isAE2Loaded() {
        return ae2Loaded;
    }
    
    public static boolean hasNetworkKey(NBTTagCompound tag) {
        return tag != null && tag.hasKey(AE2_NETWORK_KEY) && !tag.getString(AE2_NETWORK_KEY).isEmpty();
    }
    
    public static String getNetworkKey(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(AE2_NETWORK_KEY)) {
            return "";
        }
        return tag.getString(AE2_NETWORK_KEY);
    }
    
    public static Object getStorageGridFromKey(String encryptionKey) {
        if (!ae2Loaded || encryptionKey == null || encryptionKey.isEmpty()) {
            return null;
        }
        try {
            long encKey = Long.parseLong(encryptionKey);
            ILocatable obj = AEApi.instance().registries().locatable().getLocatableBy(encKey);
            if (obj instanceof IActionHost) {
                IGridNode node = ((IActionHost) obj).getActionableNode();
                if (node != null) {
                    IGrid grid = node.getGrid();
                    if (grid != null) {
                        return grid.getCache(IStorageGrid.class);
                    }
                }
            }
        } catch (NumberFormatException e) {
        } catch (Exception e) {
        }
        return null;
    }
    
    public static Object createActionSource(EntityPlayer player) {
        if (!ae2Loaded || player == null) {
            return null;
        }
        return new PlayerSource(player, null);
    }
    
    public static long countItemInNetwork(Object storageHandle, Item item, int metadata) {
        if (!ae2Loaded || !(storageHandle instanceof IStorageGrid) || item == null) {
            return 0;
        }
        try {
            IStorageGrid storage = (IStorageGrid) storageHandle;
            IStorageChannel<IAEItemStack> channel = AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IItemStorageChannel.class);
            if (channel == null) {
                return 0;
            }
            IItemList<IAEItemStack> items = storage.getInventory(channel).getStorageList();
            for (IAEItemStack aeStack : items) {
                ItemStack stack = aeStack.getDefinition();
                if (stack.getItem() == item && (metadata == -1 || stack.getMetadata() == metadata)) {
                    long size = aeStack.getStackSize();
                    if (size <= 0) {
                        return 0;
                    }
                    long maxCount = com.nyonio.mekanism_advanced_configuration_card.ModConfig.getMaxNetworkItemCount();
                    return Math.min(size, maxCount);
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }
    
    public static ItemStack extractItemFromNetwork(Object storageHandle, Item item, int metadata, int count, Object sourceHandle) {
        if (!ae2Loaded || !(storageHandle instanceof IStorageGrid) || item == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        try {
            IStorageGrid storage = (IStorageGrid) storageHandle;
            IActionSource source = sourceHandle instanceof IActionSource ? (IActionSource) sourceHandle : null;
            IStorageChannel<IAEItemStack> channel = AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IItemStorageChannel.class);
            if (channel == null) {
                return ItemStack.EMPTY;
            }
            IItemList<IAEItemStack> items = storage.getInventory(channel).getStorageList();
            for (IAEItemStack aeStack : items) {
                ItemStack stack = aeStack.getDefinition();
                if (stack.getItem() == item && (metadata == -1 || stack.getMetadata() == metadata)) {
                    IAEItemStack request = aeStack.copy();
                    request.setStackSize(Math.min(count, aeStack.getStackSize()));
                    IAEItemStack extracted = storage.getInventory(channel).extractItems(request, Actionable.MODULATE, source);
                    if (extracted != null && extracted.getStackSize() > 0) {
                        return extracted.createItemStack();
                    }
                }
            }
        } catch (Exception e) {
        }
        return ItemStack.EMPTY;
    }
    
    public static int insertItemToNetwork(Object storageHandle, ItemStack stack, Object sourceHandle) {
        if (!ae2Loaded || !(storageHandle instanceof IStorageGrid) || stack.isEmpty()) {
            return 0;
        }
        try {
            IStorageGrid storage = (IStorageGrid) storageHandle;
            IActionSource source = sourceHandle instanceof IActionSource ? (IActionSource) sourceHandle : null;
            IStorageChannel<IAEItemStack> channel = AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IItemStorageChannel.class);
            if (channel == null) {
                return 0;
            }
            IAEItemStack aeStack = channel.createStack(stack);
            IAEItemStack notInserted = storage.getInventory(channel).injectItems(aeStack, Actionable.MODULATE, source);
            if (notInserted == null || notInserted.getStackSize() == 0) {
                return stack.getCount();
            }
            return stack.getCount() - (int) notInserted.getStackSize();
        } catch (Exception e) {
        }
        return 0;
    }
    
    public static ItemStack extractUpgradeFromNetwork(Object storageHandle, Upgrade upgrade, int count, Object sourceHandle) {
        if (!ae2Loaded || !(storageHandle instanceof IStorageGrid) || upgrade == null) {
            return ItemStack.EMPTY;
        }
        ItemStack upgradeStack = upgrade.getStack();
        return extractItemFromNetwork(storageHandle, upgradeStack.getItem(), upgradeStack.getMetadata(), count, sourceHandle);
    }
}
