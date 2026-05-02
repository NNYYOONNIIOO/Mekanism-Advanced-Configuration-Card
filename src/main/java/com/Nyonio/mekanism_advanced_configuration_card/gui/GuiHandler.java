package com.Nyonio.mekanism_advanced_configuration_card.gui;

import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {
    public static final int CARD_SLOT_BAG = 0;
    
    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == CARD_SLOT_BAG) {
            BagInfo bagInfo = findBag(player);
            if (bagInfo != null) {
                return new ContainerCardSlotBag(player.inventory, bagInfo.stack, bagInfo.slotIndex);
            }
        }
        return null;
    }
    
    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == CARD_SLOT_BAG) {
            BagInfo bagInfo = findBag(player);
            if (bagInfo != null) {
                return new GuiCardSlotBag(player.inventory, bagInfo.stack, bagInfo.slotIndex);
            }
        }
        return null;
    }
    
    private BagInfo findBag(EntityPlayer player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                return new BagInfo(stack, i);
            }
        }
        return null;
    }
    
    private static class BagInfo {
        final ItemStack stack;
        final int slotIndex;
        
        BagInfo(ItemStack stack, int slotIndex) {
            this.stack = stack;
            this.slotIndex = slotIndex;
        }
    }
}
