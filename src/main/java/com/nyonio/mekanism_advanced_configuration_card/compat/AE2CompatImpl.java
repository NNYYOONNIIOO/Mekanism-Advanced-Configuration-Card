package com.nyonio.mekanism_advanced_configuration_card.compat;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.helpers.PlayerSource;
import appeng.tile.misc.TileSecurityStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

@Optional.InterfaceList({
    @Optional.Interface(iface = "appeng.api.networking.storage.IStorageGrid", modid = AE2Compat.MOD_ID),
    @Optional.Interface(iface = "appeng.api.networking.security.IActionSource", modid = AE2Compat.MOD_ID)
})
public class AE2CompatImpl {

    /**
     * Find the first wireless terminal in the player's inventory and return its IStorageGrid.
     * Scans main inventory, offhand, and Baubles slots.
     * Based on RandomComplement's MEHandler.getTerminalGuiObject() approach.
     */
    @Optional.Method(modid = AE2Compat.MOD_ID)
    public static Object getStorageGridFromPlayer(EntityPlayer player) {
        try {
            // Scan main inventory
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack item = player.inventory.getStackInSlot(i);
                if (!item.isEmpty() && item.getItem() instanceof IWirelessTermHandler) {
                    IWirelessTermHandler handler = (IWirelessTermHandler) item.getItem();
                    if (handler.canHandle(item)) {
                        Object grid = getStorageGridFromWirelessTerminal(item, player);
                        if (grid != null) return grid;
                    }
                }
            }

            // Scan Baubles
            if (BaublesCompat.isBaublesLoaded()) {
                for (int i = 0; i < BaublesCompat.getSlots(player); i++) {
                    ItemStack item = BaublesCompat.getStackInSlot(player, i);
                    if (item != null && !item.isEmpty() && item.getItem() instanceof IWirelessTermHandler) {
                        IWirelessTermHandler handler = (IWirelessTermHandler) item.getItem();
                        if (handler.canHandle(item)) {
                            Object grid = getStorageGridFromWirelessTerminal(item, player);
                            if (grid != null) return grid;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // AE2 not available or error accessing network
        }
        return null;
    }

    /**
     * Get IStorageGrid from a wireless terminal item by looking up its encryption key.
     */
    @Optional.Method(modid = AE2Compat.MOD_ID)
    private static Object getStorageGridFromWirelessTerminal(ItemStack wirelessTerminal, EntityPlayer player) {
        try {
            IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
            if (!registry.isWirelessTerminal(wirelessTerminal)) {
                return null;
            }
            IWirelessTermHandler handler = registry.getWirelessTerminalHandler(wirelessTerminal);
            String encKey = handler.getEncryptionKey(wirelessTerminal);
            if (encKey == null || encKey.isEmpty()) {
                return null;
            }
            long parsedKey = Long.parseLong(encKey);
            ILocatable locatable = AEApi.instance().registries().locatable().getLocatableBy(parsedKey);
            if (locatable instanceof IActionHost) {
                IGridNode node = ((IActionHost) locatable).getActionableNode();
                if (node != null) {
                    IGrid grid = node.getGrid();
                    if (grid != null) {
                        return grid.getCache(IStorageGrid.class);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Invalid encryption key
        } catch (Exception e) {
            // Error accessing network
        }
        return null;
    }

    /**
     * Create a PlayerSource for AE2 operations.
     * Uses the wireless terminal as the action host if available.
     */
    @Optional.Method(modid = AE2Compat.MOD_ID)
    public static Object createActionSourceFromPlayer(EntityPlayer player) {
        try {
            // Find the wireless terminal to use as the host in PlayerSource
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack item = player.inventory.getStackInSlot(i);
                if (!item.isEmpty() && item.getItem() instanceof IWirelessTermHandler) {
                    IWirelessTermHandler handler = (IWirelessTermHandler) item.getItem();
                    if (handler.canHandle(item)) {
                        // Find the IActionHost from the terminal's encryption key
                        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
                        if (registry.isWirelessTerminal(item)) {
                            IWirelessTermHandler wHandler = registry.getWirelessTerminalHandler(item);
                            String encKey = wHandler.getEncryptionKey(item);
                            if (encKey != null && !encKey.isEmpty()) {
                                long parsedKey = Long.parseLong(encKey);
                                ILocatable locatable = AEApi.instance().registries().locatable().getLocatableBy(parsedKey);
                                if (locatable instanceof IActionHost) {
                                    return new PlayerSource(player, (IActionHost) locatable);
                                }
                            }
                        }
                    }
                }
            }
            // Fallback: player-only source
            return new PlayerSource(player, null);
        } catch (Exception e) {
            return new PlayerSource(player, null);
        }
    }
    
    public static long countItemInNetwork(Object storageHandle, Item item, int metadata) {
        if (!(storageHandle instanceof IStorageGrid)) {
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
        if (!(storageHandle instanceof IStorageGrid)) {
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
        if (!(storageHandle instanceof IStorageGrid)) {
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
}
