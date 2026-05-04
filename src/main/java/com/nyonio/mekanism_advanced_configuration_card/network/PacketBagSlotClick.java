package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
        
        MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] Received: slotIndex={}, action={}, maxTake={}", 
            message.slotIndex, message.action, message.maxTake);
        
        player.getServer().addScheduledTask(() -> {
            BagRef bagRef = findBagRef(player);
            if (bagRef == null || bagRef.stack == null) {
                MekConfigCardUpgradesMod.LOGGER.warn("[BagSlotClick] No bag found!");
                return;
            }
            
            ItemStack bagStack = bagRef.stack;
            ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
            if (message.slotIndex < 0 || message.slotIndex >= handler.getSlots()) {
                MekConfigCardUpgradesMod.LOGGER.warn("[BagSlotClick] Invalid slot index: {}", message.slotIndex);
                return;
            }
            
            ItemStack slotStack = handler.getStackInSlot(message.slotIndex);
            ItemStack heldStack = player.inventory.getItemStack();
            
            MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] Before: slotStack={} x{}, heldStack={} x{}", 
                slotStack.getItem().getRegistryName(), slotStack.getCount(),
                heldStack.isEmpty() ? "empty" : heldStack.getItem().getRegistryName(), 
                heldStack.isEmpty() ? 0 : heldStack.getCount());
            
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
                    MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] TAKE: took {} x{}", taken.getItem().getRegistryName(), taken.getCount());
                    break;
                }
                case ACTION_PUT: {
                    if (heldStack.isEmpty()) break;
                    if (!ItemCardSlotBag.isSupportedBagItem(heldStack)) break;
                    if (slotStack.isEmpty()) {
                        ItemStack toInsert = heldStack.copy();
                        handler.setStackInSlot(message.slotIndex, toInsert);
                        player.inventory.setItemStack(ItemStack.EMPTY);
                        MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] PUT: placed {} x{}", toInsert.getItem().getRegistryName(), toInsert.getCount());
                    } else if (ItemStack.areItemsEqual(slotStack, heldStack) && ItemStack.areItemStackTagsEqual(slotStack, heldStack)) {
                        int space = com.nyonio.mekanism_advanced_configuration_card.ModConfig.getCardSlotBagStackLimit() - slotStack.getCount();
                        int toAdd = Math.min(heldStack.getCount(), space);
                        slotStack.setCount(slotStack.getCount() + toAdd);
                        if (toAdd >= heldStack.getCount()) {
                            player.inventory.setItemStack(ItemStack.EMPTY);
                        } else {
                            heldStack.setCount(heldStack.getCount() - toAdd);
                        }
                        MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] PUT: added {} to existing stack", toAdd);
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
                    MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] SWAP: exchanged held with slot");
                    break;
                }
            }
            
            ItemStack newSlotStack = handler.getStackInSlot(message.slotIndex);
            ItemStack newHeldStack = player.inventory.getItemStack();
            MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] After: slotStack={} x{}, heldStack={} x{}", 
                newSlotStack.isEmpty() ? "empty" : newSlotStack.getItem().getRegistryName(), 
                newSlotStack.isEmpty() ? 0 : newSlotStack.getCount(),
                newHeldStack.isEmpty() ? "empty" : newHeldStack.getItem().getRegistryName(), 
                newHeldStack.isEmpty() ? 0 : newHeldStack.getCount());
            
            syncBagToClient(player, bagRef);
            
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
                MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] Found bag in main inventory slot {}", i);
                return new BagRef(stack, i, false);
            }
        }
        if (BaublesCompat.isBaublesLoaded()) {
            int slot = BaublesCompat.findFirstBagSlot(player);
            if (slot >= 0) {
                ItemStack stack = BaublesCompat.getStackInSlot(player, slot);
                if (!stack.isEmpty()) {
                    MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] Found bag in baubles slot {}", slot);
                    return new BagRef(stack, slot, true);
                }
            }
        }
        return null;
    }
    
    private void syncBagToClient(EntityPlayerMP player, BagRef bagRef) {
        if (bagRef == null || bagRef.stack == null) return;
        NBTTagCompound tag = bagRef.stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        int source = bagRef.isBaubles ? PacketSyncBagContents.SOURCE_BAUBLES : PacketSyncBagContents.SOURCE_MAIN;
        MekConfigCardUpgradesMod.LOGGER.info("[BagSlotClick] Sending sync to client: source={}, slot={}", source, bagRef.slot);
        PacketHandler.getNetwork().sendTo(new PacketSyncBagContents.SyncBagMessage(source, bagRef.slot, tag), player);
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
