package com.nyonio.mekanism_advanced_configuration_card.gui;

import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import invtweaks.api.container.ChestContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

@ChestContainer(rowSize = 9, showButtons = true)
public class ContainerCardSlotBag extends Container {
    private final ItemStack bagStack;
    private final ItemStackHandler handler;
    private final int bagSlotIndex;
    private final boolean bagIsBaubles;
    private final int configCardSlotIndex;
    private final EntityPlayer player;
    
    public ContainerCardSlotBag(InventoryPlayer playerInventory, ItemStack bagStack, int bagSlotIndex, boolean bagIsBaubles, int configCardSlotIndex) {
        this.bagStack = bagStack;
        this.bagSlotIndex = bagSlotIndex;
        this.bagIsBaubles = bagIsBaubles;
        this.configCardSlotIndex = configCardSlotIndex;
        this.player = playerInventory.player;
        this.handler = ItemCardSlotBag.readHandler(bagStack);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new SlotItemHandler(handler, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return ItemCardSlotBag.isSupportedBagItem(stack);
                    }
                    
                    @Override
                    public int getItemStackLimit(ItemStack stack) {
                        return com.nyonio.mekanism_advanced_configuration_card.ModConfig.getCardSlotBagStackLimit();
                    }
                });
            }
        }
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int playerSlotIndex = col + row * 9 + 9;
                addSlotToContainer(new Slot(playerInventory, playerSlotIndex, 8 + col * 18, 84 + row * 18) {
                    @Override
                    public boolean canTakeStack(EntityPlayer player) {
                        return !isLockedSlot(playerSlotIndex);
                    }
                });
            }
        }
        
        for (int col = 0; col < 9; col++) {
            int playerSlotIndex = col;
            addSlotToContainer(new Slot(playerInventory, playerSlotIndex, 8 + col * 18, 142) {
                @Override
                public boolean canTakeStack(EntityPlayer player) {
                    return !isLockedSlot(playerSlotIndex);
                }
            });
        }
    }
    
    private boolean isLockedSlot(int playerSlotIndex) {
        if (!bagIsBaubles && playerSlotIndex == bagSlotIndex) {
            return true;
        }
        if (configCardSlotIndex >= 0 && playerSlotIndex == configCardSlotIndex) {
            return true;
        }
        return false;
    }
    
    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            if (bagIsBaubles) {
                writeBaublesHandler(player, bagSlotIndex, handler);
            } else {
                if (bagSlotIndex >= 0 && bagSlotIndex < player.inventory.mainInventory.size()) {
                    ItemStack actualBag = player.inventory.mainInventory.get(bagSlotIndex);
                    if (!actualBag.isEmpty() && actualBag.getItem() == bagStack.getItem()) {
                        ItemCardSlotBag.writeHandler(actualBag, handler);
                    }
                }
            }
        }
    }
    
    private void writeBaublesHandler(EntityPlayer player, int slot, ItemStackHandler handler) {
        BaublesCompat.writeBagToBaublesSlot(player, slot, bagStack.getItem(), handler);
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (bagIsBaubles) {
            return true;
        }
        if (bagSlotIndex < 0 || bagSlotIndex >= player.inventory.mainInventory.size()) {
            return false;
        }
        ItemStack stackInSlot = player.inventory.mainInventory.get(bagSlotIndex);
        return !stackInSlot.isEmpty() && stackInSlot.getItem() == bagStack.getItem();
    }
    
    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickType, EntityPlayer player) {
        if (clickType == net.minecraft.inventory.ClickType.SWAP) {
            int hotbarSlot = dragType;
            if (isLockedSlot(hotbarSlot)) {
                return ItemStack.EMPTY;
            }
        }
        if (slotId >= 27 && slotId < 63) {
            int playerSlotIndex;
            if (slotId < 54) {
                playerSlotIndex = (slotId - 27) + 9;
            } else {
                playerSlotIndex = slotId - 54;
            }
            if (isLockedSlot(playerSlotIndex)) {
                return ItemStack.EMPTY;
            }
        }
        if (slotId >= 0 && slotId < 27 && clickType == net.minecraft.inventory.ClickType.PICKUP) {
            return handleBagSlotClick(slotId, dragType, player);
        }
        if (slotId >= 0 && slotId < 27 && clickType == net.minecraft.inventory.ClickType.QUICK_MOVE) {
            return handleBagShiftClick(slotId, player);
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }
    
    private ItemStack handleBagSlotClick(int slotId, int dragType, EntityPlayer player) {
        Slot slot = inventorySlots.get(slotId);
        if (slot == null) return ItemStack.EMPTY;
        
        ItemStack heldStack = player.inventory.getItemStack();
        ItemStack slotStack = slot.getStack();
        
        if (heldStack.isEmpty()) {
            if (slotStack.isEmpty()) return ItemStack.EMPTY;
            int takeCount = dragType == 0 ? slotStack.getCount() : (slotStack.getCount() + 1) / 2;
            ItemStack taken = copyStackWithSize(slotStack, takeCount);
            int newSlotCount = slotStack.getCount() - takeCount;
            if (newSlotCount <= 0) {
                handler.setStackInSlot(slotId, ItemStack.EMPTY);
            } else {
                handler.setStackInSlot(slotId, copyStackWithSize(slotStack, newSlotCount));
            }
            player.inventory.setItemStack(taken);
            syncSlot(player, slotId);
            return taken;
        } else {
            if (!ItemCardSlotBag.isSupportedBagItem(heldStack)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                int limit = com.nyonio.mekanism_advanced_configuration_card.ModConfig.getCardSlotBagStackLimit();
                int toPlace = Math.min(heldStack.getCount(), limit);
                handler.setStackInSlot(slotId, copyStackWithSize(heldStack, toPlace));
                heldStack.shrink(toPlace);
                if (heldStack.getCount() <= 0) {
                    player.inventory.setItemStack(ItemStack.EMPTY);
                }
                syncSlot(player, slotId);
                return copyStackWithSize(heldStack, toPlace);
            } else if (ItemStack.areItemsEqual(slotStack, heldStack) && ItemStack.areItemStackTagsEqual(slotStack, heldStack)) {
                int limit = com.nyonio.mekanism_advanced_configuration_card.ModConfig.getCardSlotBagStackLimit();
                int space = limit - slotStack.getCount();
                if (space <= 0) return ItemStack.EMPTY;
                int toAdd = Math.min(heldStack.getCount(), space);
                handler.setStackInSlot(slotId, copyStackWithSize(slotStack, slotStack.getCount() + toAdd));
                heldStack.shrink(toAdd);
                if (heldStack.getCount() <= 0) {
                    player.inventory.setItemStack(ItemStack.EMPTY);
                }
                syncSlot(player, slotId);
                return copyStackWithSize(slotStack, toAdd);
            } else {
                ItemStack swapHeld = heldStack.copy();
                ItemStack swapSlot = slotStack.copy();
                handler.setStackInSlot(slotId, swapHeld);
                player.inventory.setItemStack(swapSlot);
                syncSlot(player, slotId);
                return swapSlot;
            }
        }
    }
    
    private ItemStack handleBagShiftClick(int slotId, EntityPlayer player) {
        Slot slot = inventorySlots.get(slotId);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        
        ItemStack slotStack = slot.getStack();
        ItemStack copy = slotStack.copy();
        
        if (!mergeItemStack(slotStack, 27, inventorySlots.size(), true)) {
            return ItemStack.EMPTY;
        }
        
        if (slotStack.getCount() <= 0) {
            handler.setStackInSlot(slotId, ItemStack.EMPTY);
        } else {
            handler.setStackInSlot(slotId, copyStackWithSize(slotStack, slotStack.getCount()));
        }
        syncSlot(player, slotId);
        
        return copy;
    }
    
    private void syncSlot(EntityPlayer player, int slotId) {
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) player;
            mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(windowId, slotId, handler.getStackInSlot(slotId)));
            mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
            detectAndSendChanges();
        }
    }
    
    private static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = new ItemStack(stack.getItem(), size, stack.getMetadata());
        if (stack.getTagCompound() != null) {
            copy.setTagCompound(stack.getTagCompound().copy());
        }
        return copy;
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            
            if (index < 27) {
                if (!mergeItemStack(stack, 27, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                if (stack.getCount() <= 0) {
                    handler.setStackInSlot(index, ItemStack.EMPTY);
                } else {
                    handler.setStackInSlot(index, copyStackWithSize(stack, stack.getCount()));
                }
            } else {
                if (!ItemCardSlotBag.isSupportedBagItem(stack)) {
                    return ItemStack.EMPTY;
                }
                int remaining = stack.getCount();
                int limit = com.nyonio.mekanism_advanced_configuration_card.ModConfig.getCardSlotBagStackLimit();
                for (int i = 0; i < 27 && remaining > 0; i++) {
                    ItemStack bagSlot = handler.getStackInSlot(i);
                    if (bagSlot.isEmpty()) {
                        int toPlace = Math.min(remaining, limit);
                        handler.setStackInSlot(i, copyStackWithSize(stack, toPlace));
                        remaining -= toPlace;
                    } else if (ItemStack.areItemsEqual(bagSlot, stack) && ItemStack.areItemStackTagsEqual(bagSlot, stack)) {
                        int space = limit - bagSlot.getCount();
                        if (space > 0) {
                            int toAdd = Math.min(remaining, space);
                            handler.setStackInSlot(i, copyStackWithSize(bagSlot, bagSlot.getCount() + toAdd));
                            remaining -= toAdd;
                        }
                    }
                }
                if (remaining >= stack.getCount()) {
                    return ItemStack.EMPTY;
                }
                stack.shrink(stack.getCount() - remaining);
            }
            
            if (stack.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        
        return result;
    }
}
