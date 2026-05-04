package com.nyonio.mekanism_advanced_configuration_card.compat;

import baubles.api.cap.BaublesCapabilities;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

class BaublesCompatDirect {
    
    static int countInBaublesBags(EntityPlayer player, ItemStack stack) {
        int total = 0;
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (ItemCardSlotBag.isBag(bagStack)) {
                    ItemStackHandler itemHandler = ItemCardSlotBag.readHandler(bagStack);
                    for (int j = 0; j < itemHandler.getSlots(); j++) {
                        ItemStack inSlot = itemHandler.getStackInSlot(j);
                        if (!inSlot.isEmpty() && inSlot.isItemEqual(stack)) {
                            total += inSlot.getCount();
                        }
                    }
                }
            }
        }
        return total;
    }
    
    static boolean consumeFromBaublesBags(EntityPlayer player, ItemStack stack, int amount) {
        int remaining = amount;
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots && remaining > 0; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (!ItemCardSlotBag.isBag(bagStack)) {
                    continue;
                }
                ItemStackHandler itemHandler = ItemCardSlotBag.readHandler(bagStack);
                boolean changed = false;
                for (int j = 0; j < itemHandler.getSlots() && remaining > 0; j++) {
                    ItemStack inSlot = itemHandler.getStackInSlot(j);
                    if (!inSlot.isEmpty() && inSlot.isItemEqual(stack)) {
                        int toExtract = Math.min(remaining, inSlot.getCount());
                        itemHandler.extractItem(j, toExtract, false);
                        remaining -= toExtract;
                        changed = true;
                    }
                }
                if (changed) {
                    ItemCardSlotBag.writeHandler(bagStack, itemHandler);
                }
            }
        }
        return remaining <= 0;
    }
    
    static int giveToBaublesBags(EntityPlayer player, ItemStack stack, int remaining) {
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots && remaining > 0; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (ItemCardSlotBag.isBag(bagStack) && ItemCardSlotBag.isSupportedBagItem(stack)) {
                    ItemStackHandler itemHandler = ItemCardSlotBag.readHandler(bagStack);
                    for (int slot = 0; slot < itemHandler.getSlots() && remaining > 0; slot++) {
                        ItemStack toInsert = stack.copy();
                        toInsert.setCount(remaining);
                        ItemStack result = itemHandler.insertItem(slot, toInsert, false);
                        if (result.isEmpty()) {
                            remaining = 0;
                        } else {
                            remaining = result.getCount();
                        }
                    }
                    ItemCardSlotBag.writeHandler(bagStack, itemHandler);
                }
            }
        }
        return remaining;
    }
    
    static List<ItemStack> getBaublesBagItems(EntityPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (ItemCardSlotBag.isBag(bagStack)) {
                    ItemStackHandler itemHandler = ItemCardSlotBag.readHandler(bagStack);
                    for (int j = 0; j < itemHandler.getSlots(); j++) {
                        ItemStack inSlot = itemHandler.getStackInSlot(j);
                        if (!inSlot.isEmpty()) {
                            items.add(inSlot);
                        }
                    }
                }
            }
        }
        return items;
    }
    
    static int findFirstBagSlot(EntityPlayer player) {
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (ItemCardSlotBag.isBag(bagStack)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    static List<ItemStack> getBaublesBags(EntityPlayer player) {
        List<ItemStack> bags = new ArrayList<>();
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (ItemCardSlotBag.isBag(bagStack)) {
                    bags.add(bagStack);
                }
            }
        }
        return bags;
    }
    
    static boolean hasBaublesBag(EntityPlayer player) {
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack bagStack = handler.getStackInSlot(i);
                if (ItemCardSlotBag.isBag(bagStack)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    static boolean hasBagAtSlot(EntityPlayer player, int slot) {
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null && slot >= 0 && slot < handler.getSlots()) {
            ItemStack bagStack = handler.getStackInSlot(slot);
            return ItemCardSlotBag.isBag(bagStack);
        }
        return false;
    }
    
    static void writeBagToSlot(EntityPlayer player, int slot, net.minecraft.item.Item bagItem, ItemStackHandler handler) {
        baubles.api.cap.IBaublesItemHandler baublesHandler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (baublesHandler != null) {
            ItemStack bagStack = baublesHandler.getStackInSlot(slot);
            if (!bagStack.isEmpty() && bagStack.getItem() == bagItem) {
                ItemCardSlotBag.writeHandler(bagStack, handler);
            }
        }
    }
    
    static ItemStack getStackInSlot(EntityPlayer player, int slot) {
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null && slot >= 0 && slot < handler.getSlots()) {
            return handler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }
    
    static void markSlotChanged(EntityPlayer player, int slot) {
        baubles.api.cap.IBaublesItemHandler handler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (handler != null && slot >= 0 && slot < handler.getSlots()) {
            handler.setChanged(slot, true);
        }
    }
}
