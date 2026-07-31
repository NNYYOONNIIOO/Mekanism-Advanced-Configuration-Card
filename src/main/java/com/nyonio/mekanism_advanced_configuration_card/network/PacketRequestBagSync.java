package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestBagSync implements IMessageHandler<PacketRequestBagSync.RequestMessage, IMessage> {
    
    @Override
    public IMessage onMessage(RequestMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().player;
        if (player == null) return null;
        
        player.getServer().addScheduledTask(() -> {
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack stack = player.inventory.mainInventory.get(i);
                if (ItemCardSlotBag.isBag(stack)) {
                    syncBagToClient(player, stack, i, false, false);
                    return;
                }
            }
            
            ItemStack offhandStack = player.getHeldItemOffhand();
            if (ItemCardSlotBag.isBag(offhandStack)) {
                syncBagToClient(player, offhandStack, -1, false, true);
                return;
            }
            
            if (BaublesCompat.isBaublesLoaded()) {
                int slot = BaublesCompat.findFirstBagSlot(player);
                if (slot >= 0) {
                    ItemStack stack = BaublesCompat.getStackInSlot(player, slot);
                    if (ItemCardSlotBag.isBag(stack)) {
                        syncBagToClient(player, stack, slot, true, false);
                    }
                }
            }
        });
        
        return null;
    }
    
    private void syncBagToClient(EntityPlayerMP player, ItemStack bagStack, int slot, boolean isBaubles, boolean isOffhand) {
        NBTTagCompound tag = bagStack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        int source;
        if (isOffhand) {
            source = PacketSyncBagContents.SOURCE_OFFHAND;
        } else if (isBaubles) {
            source = PacketSyncBagContents.SOURCE_BAUBLES;
        } else {
            source = PacketSyncBagContents.SOURCE_MAIN;
        }
        PacketHandler.getNetwork().sendTo(new PacketSyncBagContents.SyncBagMessage(source, slot, tag), player);
    }
    
    public static class RequestMessage implements IMessage {
        public RequestMessage() {
        }
        
        @Override
        public void toBytes(ByteBuf buf) {
        }
        
        @Override
        public void fromBytes(ByteBuf buf) {
        }
    }
}
