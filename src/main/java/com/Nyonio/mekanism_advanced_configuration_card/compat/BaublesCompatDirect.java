package com.Nyonio.mekanism_advanced_configuration_card.compat;

import baubles.api.cap.BaublesCapabilities;
import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
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
                        ItemStack existing = itemHandler.getStackInSlot(slot);
                        if (!existing.isEmpty() && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                            int space = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                            if (space > 0) {
                                int toAdd = Math.min(space, remaining);
                                existing.grow(toAdd);
                                remaining -= toAdd;
                            }
                        }
                    }
                    for (int slot = 0; slot < itemHandler.getSlots() && remaining > 0; slot++) {
                        ItemStack existing = itemHandler.getStackInSlot(slot);
                        if (existing.isEmpty()) {
                            int toAdd = Math.min(stack.getMaxStackSize(), remaining);
                            itemHandler.setStackInSlot(slot, new ItemStack(stack.getItem(), toAdd, stack.getMetadata(), stack.getTagCompound()));
                            remaining -= toAdd;
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
}
