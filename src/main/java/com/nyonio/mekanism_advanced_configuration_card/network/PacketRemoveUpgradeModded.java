package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.ModConfig;
import com.nyonio.mekanism_advanced_configuration_card.compat.AE2Compat;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemAdvancedConfigurationCard;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.common.Upgrade;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class PacketRemoveUpgradeModded implements IMessageHandler<PacketRemoveUpgradeModded.RemoveUpgradeModdedMessage, IMessage> {
    
    @Override
    public IMessage onMessage(RemoveUpgradeModdedMessage message, MessageContext context) {
        EntityPlayer player = context.getServerHandler().player;
        if (player == null) {
            return null;
        }
        
        player.getServer().addScheduledTask(() -> {
            TileEntity tileEntity = message.coord4D.getTileEntity(player.world);
            if (!canAccessTile(player, tileEntity)) {
                return;
            }
            
            if (tileEntity instanceof IUpgradeTile && tileEntity instanceof TileEntityBasicBlock) {
                IUpgradeTile upgradeTile = (IUpgradeTile) tileEntity;
                Upgrade upgrade = MekanismUtils.getByIndex(Upgrade.values(), message.upgradeType, null);
                if (upgrade == null) {
                    return;
                }
                
                int installedCount = upgradeTile.getComponent().getUpgrades(upgrade);
                if (installedCount <= 0) {
                    return;
                }
                
                int toRemove = message.removeAll ? installedCount : 1;
                ItemStack upgradeStack = upgrade.getStack();
                upgradeStack.setCount(toRemove);
                
                int remaining = upgradeStack.getCount();
                
                appeng.api.networking.storage.IStorageGrid ae2Storage = null;
                appeng.api.networking.security.IActionSource ae2Source = null;
                ItemStack boundConfigCard = findBoundConfigCard(player);
                if (boundConfigCard != null && AE2Compat.isAE2Loaded()) {
                    NBTTagCompound tag = boundConfigCard.getTagCompound();
                    if (AE2Compat.hasNetworkKey(tag)) {
                        String key = AE2Compat.getNetworkKey(tag);
                        ae2Storage = AE2Compat.getStorageGridFromKey(key);
                        if (ae2Storage != null) {
                            ae2Source = AE2Compat.createActionSource(player);
                        }
                    }
                }
                
                List<ModConfig.SourcePriority> priorities = ModConfig.getUpgradeReturnPriorityList();
                
                for (ModConfig.SourcePriority priority : priorities) {
                    if (remaining <= 0) break;
                    
                    switch (priority) {
                        case NETWORK:
                            if (ae2Storage != null && ae2Source != null && remaining > 0) {
                                ItemStack toInsert = upgradeStack.copy();
                                toInsert.setCount(remaining);
                                int inserted = AE2Compat.insertItemToNetwork(ae2Storage, toInsert, ae2Source);
                                remaining -= inserted;
                            }
                            break;
                        case CARD_SLOT_BAG:
                            remaining = insertToCardSlotBags(player, upgradeStack, remaining);
                            if (BaublesCompat.isBaublesLoaded()) {
                                remaining = BaublesCompat.giveToBaublesBags(player, upgradeStack, remaining);
                            }
                            break;
                        case PLAYER_INVENTORY:
                            if (remaining > 0) {
                                ItemStack toInsert = upgradeStack.copy();
                                toInsert.setCount(remaining);
                                if (player.inventory.addItemStackToInventory(toInsert)) {
                                    remaining = toInsert.getCount();
                                }
                            }
                            break;
                    }
                }
                
                if (remaining <= 0) {
                    upgradeTile.getComponent().removeUpgrade(upgrade, message.removeAll);
                }
                
                player.inventoryContainer.detectAndSendChanges();
            }
        });
        
        return null;
    }
    
    private boolean canAccessTile(EntityPlayer player, TileEntity tile) {
        return mekanism.common.PacketHandler.canAccessTile(player, tile);
    }
    
    private int insertToCardSlotBags(EntityPlayer player, ItemStack upgradeStack, int remaining) {
        for (int i = 0; i < player.inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack bagStack = player.inventory.mainInventory.get(i);
            if (!bagStack.isEmpty() && bagStack.getItem() instanceof ItemCardSlotBag) {
                ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
                
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    if (ItemCardSlotBag.isSupportedBagItem(upgradeStack)) {
                        ItemStack toInsert = upgradeStack.copy();
                        toInsert.setCount(remaining);
                        ItemStack result = handler.insertItem(slot, toInsert, false);
                        if (result.isEmpty()) {
                            remaining = 0;
                        } else {
                            remaining = result.getCount();
                        }
                    }
                }
                
                ItemCardSlotBag.writeHandler(bagStack, handler);
            }
        }
        return remaining;
    }
    
    private ItemStack findBoundConfigCard(EntityPlayer player) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (isBoundConfigCard(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getHeldItemOffhand();
        if (isBoundConfigCard(offHand)) {
            return offHand;
        }
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (isBoundConfigCard(stack)) {
                return stack;
            }
        }
        return null;
    }
    
    private boolean isBoundConfigCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ItemAdvancedConfigurationCard) {
            NBTTagCompound tag = stack.getTagCompound();
            return AE2Compat.hasNetworkKey(tag);
        }
        return false;
    }
    
    public static class RemoveUpgradeModdedMessage implements IMessage {
        public Coord4D coord4D;
        public int upgradeType;
        public boolean removeAll;
        
        public RemoveUpgradeModdedMessage() {
        }
        
        public RemoveUpgradeModdedMessage(Coord4D coord, int type, boolean remove) {
            coord4D = coord;
            upgradeType = type;
            removeAll = remove;
        }
        
        @Override
        public void toBytes(ByteBuf buf) {
            coord4D.write(buf);
            buf.writeInt(upgradeType);
            buf.writeBoolean(removeAll);
        }
        
        @Override
        public void fromBytes(ByteBuf buf) {
            coord4D = Coord4D.read(buf);
            upgradeType = buf.readInt();
            removeAll = buf.readBoolean();
        }
    }
}
