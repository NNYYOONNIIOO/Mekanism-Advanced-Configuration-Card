package com.Nyonio.mekanism_advanced_configuration_card.gui;

import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerCardSlotBag extends Container {
    private final ItemStack bagStack;
    private final ItemStackHandler handler;
    private final int bagSlotIndex;
    private final EntityPlayer player;
    
    public ContainerCardSlotBag(InventoryPlayer playerInventory, ItemStack bagStack, int bagSlotIndex) {
        this.bagStack = bagStack;
        this.bagSlotIndex = bagSlotIndex;
        this.player = playerInventory.player;
        this.handler = ItemCardSlotBag.readHandler(bagStack);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new SlotItemHandler(handler, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return ItemCardSlotBag.isSupportedBagItem(stack);
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
                        return playerSlotIndex != bagSlotIndex;
                    }
                });
            }
        }
        
        for (int col = 0; col < 9; col++) {
            int playerSlotIndex = col;
            addSlotToContainer(new Slot(playerInventory, playerSlotIndex, 8 + col * 18, 142) {
                @Override
                public boolean canTakeStack(EntityPlayer player) {
                    return playerSlotIndex != bagSlotIndex;
                }
            });
        }
    }
    
    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            ItemCardSlotBag.writeHandler(bagStack, handler);
        }
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (bagSlotIndex < 0 || bagSlotIndex >= player.inventory.mainInventory.size()) {
            return false;
        }
        ItemStack stackInSlot = player.inventory.mainInventory.get(bagSlotIndex);
        if (stackInSlot.isEmpty() || stackInSlot.getItem() != bagStack.getItem()) {
            return false;
        }
        if (stackInSlot != bagStack) {
            return false;
        }
        return true;
    }
    
    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickType, EntityPlayer player) {
        if (clickType == net.minecraft.inventory.ClickType.SWAP) {
            int hotbarSlot = dragType;
            if (hotbarSlot == bagSlotIndex) {
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
            if (playerSlotIndex == bagSlotIndex) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }
    
    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return super.canMergeSlot(stack, slotIn);
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
            } else {
                if (!mergeItemStack(stack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
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
