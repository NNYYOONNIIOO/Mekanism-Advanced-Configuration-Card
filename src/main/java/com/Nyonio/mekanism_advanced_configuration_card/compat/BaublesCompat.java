package com.Nyonio.mekanism_advanced_configuration_card.compat;

import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class BaublesCompat {
    private static boolean baublesLoaded = false;
    
    public static void init() {
        try {
            Class.forName("baubles.api.BaublesApi");
            baublesLoaded = true;
        } catch (ClassNotFoundException e) {
            baublesLoaded = false;
        }
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
}
