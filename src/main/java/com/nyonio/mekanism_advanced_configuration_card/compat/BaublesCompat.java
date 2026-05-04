package com.nyonio.mekanism_advanced_configuration_card.compat;

import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class BaublesCompat {
    private static boolean baublesLoaded = false;
    
    public static void init() {
        baublesLoaded = Loader.isModLoaded("baubles");
    }
    
    public static boolean isBaublesLoaded() {
        return baublesLoaded;
    }
    
    public static int countInBaublesBags(EntityPlayer player, ItemStack stack) {
        if (!baublesLoaded) {
            return 0;
        }
        try {
            return BaublesCompatDirect.countInBaublesBags(player, stack);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static boolean consumeFromBaublesBags(EntityPlayer player, ItemStack stack, int amount) {
        if (!baublesLoaded) {
            return false;
        }
        try {
            return BaublesCompatDirect.consumeFromBaublesBags(player, stack, amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static int giveToBaublesBags(EntityPlayer player, ItemStack stack, int remaining) {
        if (!baublesLoaded || remaining <= 0) {
            return remaining;
        }
        try {
            return BaublesCompatDirect.giveToBaublesBags(player, stack, remaining);
        } catch (Exception e) {
            return remaining;
        }
    }
    
    public static List<ItemStack> getBaublesBagItems(EntityPlayer player) {
        if (!baublesLoaded) {
            return new ArrayList<>();
        }
        try {
            return BaublesCompatDirect.getBaublesBagItems(player);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public static int findFirstBagSlot(EntityPlayer player) {
        if (!baublesLoaded) {
            return -1;
        }
        try {
            return BaublesCompatDirect.findFirstBagSlot(player);
        } catch (Exception e) {
            return -1;
        }
    }
    
    public static List<ItemStack> getBaublesBags(EntityPlayer player) {
        if (!baublesLoaded) {
            return new ArrayList<>();
        }
        try {
            return BaublesCompatDirect.getBaublesBags(player);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public static boolean hasBaublesBag(EntityPlayer player) {
        if (!baublesLoaded) {
            return false;
        }
        try {
            return BaublesCompatDirect.hasBaublesBag(player);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean hasBagAtSlot(EntityPlayer player, int slot) {
        if (!baublesLoaded) {
            return false;
        }
        try {
            return BaublesCompatDirect.hasBagAtSlot(player, slot);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void writeBagToBaublesSlot(EntityPlayer player, int slot, net.minecraft.item.Item bagItem, ItemStackHandler handler) {
        if (!baublesLoaded) {
            return;
        }
        try {
            BaublesCompatDirect.writeBagToSlot(player, slot, bagItem, handler);
        } catch (Exception e) {
        }
    }
    
    public static ItemStack getStackInSlot(EntityPlayer player, int slot) {
        if (!baublesLoaded) {
            return ItemStack.EMPTY;
        }
        try {
            return BaublesCompatDirect.getStackInSlot(player, slot);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    public static void markSlotChanged(EntityPlayer player, int slot) {
        if (!baublesLoaded) {
            return;
        }
        try {
            BaublesCompatDirect.markSlotChanged(player, slot);
        } catch (Exception e) {
        }
    }
}
