package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.common.Upgrade;
import mekanism.common.base.IUpgradeItem;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;

public class PacketInstallUpgradeFromBag implements IMessageHandler<PacketInstallUpgradeFromBag.InstallMessage, IMessage> {
    
    public static final int MODE_MAX = 0;
    public static final int MODE_SINGLE = 1;
    
    @Override
    public IMessage onMessage(InstallMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().player;
        if (player == null) return null;
        
        player.getServer().addScheduledTask(() -> {
            TileEntity tileEntity = message.coord4D.getTileEntity(player.world);
            if (tileEntity == null) return;
            if (!mekanism.common.PacketHandler.canAccessTile(player, tileEntity)) return;
            if (!(tileEntity instanceof IUpgradeTile)) return;
            
            IUpgradeTile upgradeTile = (IUpgradeTile) tileEntity;
            if (!upgradeTile.supportsUpgrades()) return;
            
            BagRef bagRef = findBagRef(player);
            if (bagRef == null || bagRef.stack == null) return;
            
            ItemStackHandler handler = ItemCardSlotBag.readHandler(bagRef.stack);
            if (message.slotIndex < 0 || message.slotIndex >= handler.getSlots()) return;
            
            ItemStack slotStack = handler.getStackInSlot(message.slotIndex);
            if (slotStack.isEmpty()) return;
            
            Item item = slotStack.getItem();
            TileComponentUpgrade component = upgradeTile.getComponent();
            
            Item infiniteUpgrade = InfiniteUpgradeCardCompat.getInfiniteUpgradeItem();
            Item superInfiniteUpgrade = InfiniteUpgradeCardCompat.getSuperInfiniteUpgradeItem();
            
            boolean isInfinite = infiniteUpgrade != null && item == infiniteUpgrade;
            boolean isSuperInfinite = superInfiniteUpgrade != null && item == superInfiniteUpgrade;
            
            if (isInfinite) {
                handleInfiniteUpgrade(component, message.mode);
            } else if (isSuperInfinite) {
                handleSuperInfiniteUpgrade(component, message.mode);
            } else {
                if (!(item instanceof IUpgradeItem)) return;
                
                IUpgradeItem upgradeItem = (IUpgradeItem) item;
                Upgrade upgrade = upgradeItem.getUpgradeType(slotStack);
                if (upgrade == null) return;
                
                if (!component.supports(upgrade)) return;
                
                int current = component.getUpgrades(upgrade);
                int max = upgrade.getMaxInstalled();
                int needed = max - current;
                if (needed <= 0) return;
                
                int available = slotStack.getCount();
                int toInstall;
                if (message.mode == MODE_SINGLE) {
                    toInstall = 1;
                } else {
                    toInstall = Math.min(needed, available);
                }
                
                slotStack.shrink(toInstall);
                if (slotStack.isEmpty()) {
                    handler.setStackInSlot(message.slotIndex, ItemStack.EMPTY);
                }
                component.addUpgrades(upgrade, toInstall);
                ItemCardSlotBag.writeHandler(bagRef.stack, handler);
            }
            
            syncBagToClient(player, bagRef);
            
            if (bagRef.isBaubles && BaublesCompat.isBaublesLoaded()) {
                BaublesCompat.markSlotChanged(player, bagRef.slot);
            }
            player.inventoryContainer.detectAndSendChanges();
        });
        
        return null;
    }
    
    private void handleInfiniteUpgrade(TileComponentUpgrade component, int mode) {
        if (mode == MODE_MAX) {
            for (Upgrade upgrade : new Upgrade[]{Upgrade.SPEED, Upgrade.ENERGY}) {
                if (!component.supports(upgrade)) continue;
                int current = component.getUpgrades(upgrade);
                int max = upgrade.getMaxInstalled();
                int needed = max - current;
                if (needed > 0) {
                    component.addUpgrades(upgrade, needed);
                }
            }
        } else {
            for (Upgrade upgrade : new Upgrade[]{Upgrade.SPEED, Upgrade.ENERGY}) {
                if (!component.supports(upgrade)) continue;
                int current = component.getUpgrades(upgrade);
                int max = upgrade.getMaxInstalled();
                if (current < max) {
                    component.addUpgrades(upgrade, 1);
                }
            }
        }
    }
    
    private void handleSuperInfiniteUpgrade(TileComponentUpgrade component, int mode) {
        if (mode == MODE_MAX) {
            for (Upgrade upgrade : Upgrade.values()) {
                if (upgrade == Upgrade.ANCHOR) continue;
                if (!component.supports(upgrade)) continue;
                int current = component.getUpgrades(upgrade);
                int max = upgrade.getMaxInstalled();
                int needed = max - current;
                if (needed > 0) {
                    component.addUpgrades(upgrade, needed);
                }
            }
        } else {
            for (Upgrade upgrade : Upgrade.values()) {
                if (upgrade == Upgrade.ANCHOR) continue;
                if (!component.supports(upgrade)) continue;
                int current = component.getUpgrades(upgrade);
                int max = upgrade.getMaxInstalled();
                if (current < max) {
                    component.addUpgrades(upgrade, 1);
                }
            }
        }
    }
    
    private BagRef findBagRef(EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) return new BagRef(stack, i, false);
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
    
    private void syncBagToClient(EntityPlayerMP player, BagRef bagRef) {
        if (bagRef == null || bagRef.stack == null) return;
        NBTTagCompound tag = bagRef.stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        int source = bagRef.isBaubles ? PacketSyncBagContents.SOURCE_BAUBLES : PacketSyncBagContents.SOURCE_MAIN;
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
    
    public static class InstallMessage implements IMessage {
        public Coord4D coord4D;
        public int slotIndex;
        public int mode;
        
        public InstallMessage() {
        }
        
        public InstallMessage(Coord4D coord, int slot, int mode) {
            this.coord4D = coord;
            this.slotIndex = slot;
            this.mode = mode;
        }
        
        @Override
        public void toBytes(ByteBuf buf) {
            coord4D.write(buf);
            buf.writeInt(slotIndex);
            buf.writeInt(mode);
        }
        
        @Override
        public void fromBytes(ByteBuf buf) {
            coord4D = Coord4D.read(buf);
            slotIndex = buf.readInt();
            mode = buf.readInt();
        }
    }
}
