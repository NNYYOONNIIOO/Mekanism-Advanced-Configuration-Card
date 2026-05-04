package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;

public class PacketBagSlotClick implements IMessageHandler<PacketBagSlotClick.BagSlotClickMessage, IMessage> {
    
    public static final int ACTION_TAKE = 0;
    public static final int ACTION_PUT = 1;
    public static final int ACTION_SWAP = 2;
    
    @Override
    public IMessage onMessage(BagSlotClickMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().player;
        if (player == null) return null;
        
        player.getServer().addScheduledTask(() -> {
            BagRef bagRef = findBagRef(player);
            if (bagRef == null || bagRef.stack == null) return;
            
            ItemStack bagStack = bagRef.stack;
            ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
            if (message.slotIndex < 0 || message.slotIndex >= handler.getSlots()) return;
            
            ItemStack slotStack = handler.getStackInSlot(message.slotIndex);
            ItemStack heldStack = player.inventory.getItemStack();
            
            switch (message.action) {
                case ACTION_TAKE: {
                    if (slotStack.isEmpty()) break;
                    int takeCount = slotStack.getCount();
                    if (message.maxTake > 0) {
                        takeCount = Math.min(takeCount, message.maxTake);
                    }
                    ItemStack taken = slotStack.copy();
                    taken.setCount(takeCount);
                    if (slotStack.getCount() <= takeCount) {
                        handler.setStackInSlot(message.slotIndex, ItemStack.EMPTY);
                    } else {
                        slotStack.setCount(slotStack.getCount() - takeCount);
                    }
                    player.inventory.setItemStack(taken);
                    ItemCardSlotBag.writeHandler(bagStack, handler);
                    break;
                }
                case ACTION_PUT: {
                    if (heldStack.isEmpty()) break;
                    if (!ItemCardSlotBag.isSupportedBagItem(heldStack)) break;
                    if (slotStack.isEmpty()) {
                        ItemStack toInsert = heldStack.copy();
                        handler.setStackInSlot(message.slotIndex, toInsert);
                        player.inventory.setItemStack(ItemStack.EMPTY);
                    } else if (ItemStack.areItemsEqual(slotStack, heldStack) && ItemStack.areItemStackTagsEqual(slotStack, heldStack)) {
                        int space = com.nyonio.mekanism_advanced_configuration_card.ModConfig.getCardSlotBagStackLimit() - slotStack.getCount();
                        int toAdd = Math.min(heldStack.getCount(), space);
                        slotStack.setCount(slotStack.getCount() + toAdd);
                        if (toAdd >= heldStack.getCount()) {
                            player.inventory.setItemStack(ItemStack.EMPTY);
                        } else {
                            heldStack.setCount(heldStack.getCount() - toAdd);
                        }
                    }
                    ItemCardSlotBag.writeHandler(bagStack, handler);
                    break;
                }
                case ACTION_SWAP: {
                    if (heldStack.isEmpty() || slotStack.isEmpty()) break;
                    if (!ItemCardSlotBag.isSupportedBagItem(heldStack)) break;
                    handler.setStackInSlot(message.slotIndex, heldStack.copy());
                    player.inventory.setItemStack(slotStack.copy());
                    ItemCardSlotBag.writeHandler(bagStack, handler);
                    break;
                }
            }
            
            if (bagRef.isBaubles && BaublesCompat.isBaublesLoaded()) {
                BaublesCompat.markSlotChanged(player, bagRef.slot);
            }
            player.inventoryContainer.detectAndSendChanges();
            player.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
        });
        
        return null;
    }
    
    private BagRef findBagRef(EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                return new BagRef(stack, i, false);
            }
        }
        if (BaublesCompat.isBaublesLoaded()) {
            int slot = BaublesCompat.findFirstBagSlot(player);
            if (slot >= 0) {
                ItemStack stack = BaublesCompat.getStackInSlot(player, slot);
                if (!stack.isEmpty()) return new BagRef(stack, slot, true);
            }
        }
        return null;
    }
    
    private static class BagRef {
        final ItemStack stack;
        final int slot;
        final boolean isBaubles;
        BagRef(ItemStack stack, int slot, boolean isBaubles) {
            this.stack = stack;
            this.slot = slot;
            this.isBaubles = isBaubles;
        }
    }
    
    public static class BagSlotClickMessage implements IMessage {
        public int slotIndex;
        public int action;
        public int maxTake;
        
        public BagSlotClickMessage() {
        }
        
        public BagSlotClickMessage(int slot, int act, int max) {
            slotIndex = slot;
            action = act;
            maxTake = max;
        }
        
        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(slotIndex);
            buf.writeInt(action);
            buf.writeInt(maxTake);
        }
        
        @Override
        public void fromBytes(ByteBuf buf) {
            slotIndex = buf.readInt();
            action = buf.readInt();
            maxTake = buf.readInt();
        }
    }
}
