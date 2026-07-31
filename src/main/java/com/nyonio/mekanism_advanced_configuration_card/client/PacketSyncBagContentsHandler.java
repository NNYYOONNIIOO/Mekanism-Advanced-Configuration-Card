package com.nyonio.mekanism_advanced_configuration_card.client;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.event.GuiEventHandler;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketSyncBagContents;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PacketSyncBagContentsHandler implements IMessageHandler<PacketSyncBagContents.SyncBagMessage, IMessage> {
    
    @Override
    public IMessage onMessage(PacketSyncBagContents.SyncBagMessage message, MessageContext context) {
        MekConfigCardUpgradesMod.LOGGER.info("[SyncBag] Received sync packet: source={}, slotIndex={}, hasTag={}", 
            message.source, message.slotIndex, message.tagCompound != null);
        
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) {
                MekConfigCardUpgradesMod.LOGGER.warn("[SyncBag] Player is null!");
                return;
            }
            
            ItemStack bagStack = null;
            if (message.source == PacketSyncBagContents.SOURCE_BAUBLES && BaublesCompat.isBaublesLoaded()) {
                bagStack = BaublesCompat.getStackInSlot(player, message.slotIndex);
                MekConfigCardUpgradesMod.LOGGER.info("[SyncBag] Baubles slot {} bag: {}", message.slotIndex, bagStack);
            } else if (message.source == PacketSyncBagContents.SOURCE_OFFHAND) {
                bagStack = player.getHeldItemOffhand();
                MekConfigCardUpgradesMod.LOGGER.info("[SyncBag] Offhand bag: {}", bagStack);
            } else {
                if (message.slotIndex >= 0 && message.slotIndex < player.inventory.mainInventory.size()) {
                    bagStack = player.inventory.mainInventory.get(message.slotIndex);
                }
                MekConfigCardUpgradesMod.LOGGER.info("[SyncBag] Main inventory slot {} bag: {}", message.slotIndex, bagStack);
            }
            
            if (ItemCardSlotBag.isBag(bagStack)) {
                NBTTagCompound oldTag = bagStack.getTagCompound();
                bagStack.setTagCompound(message.tagCompound);
                MekConfigCardUpgradesMod.LOGGER.info("[SyncBag] Updated bag NBT. Old tag: {}, New tag: {}", 
                    oldTag != null ? oldTag.toString().substring(0, Math.min(100, oldTag.toString().length())) + "..." : "null",
                    message.tagCompound != null ? message.tagCompound.toString().substring(0, Math.min(100, message.tagCompound.toString().length())) + "..." : "null");
                GuiEventHandler.scheduleRefresh();
            } else {
                MekConfigCardUpgradesMod.LOGGER.warn("[SyncBag] Target stack is not a bag or is null!");
            }
        });
        
        return null;
    }
}
