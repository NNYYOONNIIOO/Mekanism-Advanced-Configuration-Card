package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.common.Upgrade;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;

public class PacketBatchUpgrade implements IMessageHandler<PacketBatchUpgrade.BatchUpgradeMessage, IMessage> {

    public static final int ACTION_UPGRADE = 0;
    public static final int ACTION_UNLOAD = 1;

    @Override
    public IMessage onMessage(BatchUpgradeMessage message, MessageContext context) {
        EntityPlayer player = context.getServerHandler().player;
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

            if (message.action == ACTION_UPGRADE) {
                doUpgrade(player, upgradeTile, bagRef.stack);
            } else if (message.action == ACTION_UNLOAD) {
                doUnload(player, upgradeTile, bagRef.stack);
            }

            if (bagRef.isBaubles && BaublesCompat.isBaublesLoaded()) {
                BaublesCompat.markSlotChanged(player, bagRef.slot);
            }
            player.inventoryContainer.detectAndSendChanges();
        });

        return null;
    }

    private void doUpgrade(EntityPlayer player, IUpgradeTile upgradeTile, ItemStack bagStack) {
        TileComponentUpgrade component = upgradeTile.getComponent();
        ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
        boolean changed = false;
        boolean hasSuperInfinite = InfiniteUpgradeCardCompat.hasSuperInfiniteUpgrade(player);
        boolean hasInfinite = InfiniteUpgradeCardCompat.hasInfiniteUpgrade(player);

        for (Upgrade upgrade : Upgrade.values()) {
            if (upgrade == Upgrade.ANCHOR) continue;
            if (!component.supports(upgrade)) continue;
            int current = component.getUpgrades(upgrade);
            int max = upgrade.getMaxInstalled();
            int needed = max - current;
            if (needed <= 0) continue;

            if (player.isCreative() || hasSuperInfinite) {
                component.addUpgrades(upgrade, needed);
                changed = true;
                continue;
            }

            if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) {
                component.addUpgrades(upgrade, needed);
                changed = true;
                continue;
            }

            ItemStack upgradeStack = upgrade.getStack();
            int available = countInHandler(handler, upgradeStack);
            if (available <= 0) continue;

            int toInstall = Math.min(needed, available);
            consumeFromHandler(handler, upgradeStack, toInstall);
            component.addUpgrades(upgrade, toInstall);
            changed = true;
        }

        if (changed) {
            ItemCardSlotBag.writeHandler(bagStack, handler);
        }
    }

    private void doUnload(EntityPlayer player, IUpgradeTile upgradeTile, ItemStack bagStack) {
        TileComponentUpgrade component = upgradeTile.getComponent();
        ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
        boolean changed = false;

        for (Upgrade upgrade : Upgrade.values()) {
            int current = component.getUpgrades(upgrade);
            if (current <= 0) continue;

            ItemStack upgradeStack = upgrade.getStack();
            int remaining = current;

            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
                ItemStack result = handler.insertItem(slot, toInsert, false);
                if (result.isEmpty()) {
                    remaining = 0;
                } else {
                    remaining = result.getCount();
                }
            }

            if (remaining > 0) {
                ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
                int notInserted = player.inventory.addItemStackToInventory(toInsert) ? 0 : toInsert.getCount();
                remaining = notInserted;
            }

            if (remaining < current) {
                int removed = current - remaining;
                for (int i = 0; i < removed; i++) {
                    component.removeUpgrade(upgrade, false);
                }
                changed = true;

                if (remaining > 0) {
                    ItemStack dropStack = upgrade.getStack().copy();
                    dropStack.setCount(remaining);
                    player.dropItem(dropStack, false);
                }
            }
        }

        if (changed) {
            ItemCardSlotBag.writeHandler(bagStack, handler);
        }
    }

    private int countInHandler(ItemStackHandler handler, ItemStack stack) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack)) {
                total += inSlot.getCount();
            }
        }
        return total;
    }

    private void consumeFromHandler(ItemStackHandler handler, ItemStack stack, int amount) {
        int remaining = amount;
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack)) {
                int toShrink = Math.min(inSlot.getCount(), remaining);
                inSlot.shrink(toShrink);
                remaining -= toShrink;
                if (inSlot.isEmpty()) {
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private BagRef findBagRef(EntityPlayer player) {
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

    public static class BatchUpgradeMessage implements IMessage {
        public Coord4D coord4D;
        public int action;

        public BatchUpgradeMessage() {
        }

        public BatchUpgradeMessage(Coord4D coord, int action) {
            this.coord4D = coord;
            this.action = action;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            coord4D.write(buf);
            buf.writeInt(action);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            coord4D = Coord4D.read(buf);
            action = buf.readInt();
        }
    }
}
