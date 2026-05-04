package com.nyonio.mekanism_advanced_configuration_card.gui;

import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {
    public static final int CARD_SLOT_BAG = 0;
    public static final int SOURCE_MAIN = 0;
    public static final int SOURCE_BAUBLES = 1;
    
    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == CARD_SLOT_BAG) {
            int configCardSlot = x;
            int bagSource = y;
            int bagSlot = z;
            
            BagInfo bagInfo = findBag(player, bagSource, bagSlot);
            if (bagInfo != null) {
                return new ContainerCardSlotBag(player.inventory, bagInfo.stack, bagInfo.slotIndex, bagInfo.isBaubles, configCardSlot);
            }
        }
        return null;
    }
    
    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == CARD_SLOT_BAG) {
            int configCardSlot = x;
            int bagSource = y;
            int bagSlot = z;
            
            BagInfo bagInfo = findBag(player, bagSource, bagSlot);
            if (bagInfo != null) {
                return new GuiCardSlotBag(player.inventory, bagInfo.stack, bagInfo.slotIndex, bagInfo.isBaubles, configCardSlot);
            }
        }
        return null;
    }
    
    public static BagInfo findFirstBag(EntityPlayer player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                return new BagInfo(stack, i, false);
            }
        }
        if (BaublesCompat.isBaublesLoaded()) {
            int baublesSlot = BaublesCompat.findFirstBagSlot(player);
            if (baublesSlot >= 0) {
                ItemStack stack = getBaublesStack(player, baublesSlot);
                if (stack != null && !stack.isEmpty()) {
                    return new BagInfo(stack, baublesSlot, true);
                }
            }
        }
        return null;
    }
    
    private BagInfo findBag(EntityPlayer player, int bagSource, int bagSlot) {
        if (bagSource == SOURCE_BAUBLES && BaublesCompat.isBaublesLoaded()) {
            ItemStack stack = getBaublesStack(player, bagSlot);
            if (stack != null && !stack.isEmpty()) {
                return new BagInfo(stack, bagSlot, true);
            }
        } else {
            if (bagSlot >= 0 && bagSlot < player.inventory.mainInventory.size()) {
                ItemStack stack = player.inventory.mainInventory.get(bagSlot);
                if (ItemCardSlotBag.isBag(stack)) {
                    return new BagInfo(stack, bagSlot, false);
                }
            }
        }
        return null;
    }
    
    private static ItemStack getBaublesStack(EntityPlayer player, int slot) {
        return BaublesCompat.getStackInSlot(player, slot);
    }
    
    public static class BagInfo {
        public final ItemStack stack;
        public final int slotIndex;
        public final boolean isBaubles;
        
        BagInfo(ItemStack stack, int slotIndex, boolean isBaubles) {
            this.stack = stack;
            this.slotIndex = slotIndex;
            this.isBaubles = isBaubles;
        }
    }
}
