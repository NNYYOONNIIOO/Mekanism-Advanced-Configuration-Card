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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class PacketBatchUpgrade implements IMessageHandler<PacketBatchUpgrade.BatchUpgradeMessage, IMessage> {

    public static final int ACTION_UPGRADE = 0;
    public static final int ACTION_UNLOAD = 1;
    private static final int MAX_BAUBLES_SLOTS = 64;

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

            List<BagRef> bagRefs = findBagRefs(player);
            List<BagRef> modifiedBags = new ArrayList<>();

            if (message.action == ACTION_UPGRADE) {
                doUpgrade(player, upgradeTile, bagRefs, modifiedBags);
            } else if (message.action == ACTION_UNLOAD) {
                doUnload(player, upgradeTile, bagRefs, modifiedBags);
            } else {
                return;
            }

            for (BagRef bagRef : modifiedBags) {
                syncBagToClient(player, bagRef);
                if (bagRef.isBaubles && BaublesCompat.isBaublesLoaded()) {
                    BaublesCompat.markSlotChanged(player, bagRef.slot);
                }
            }
            player.inventoryContainer.detectAndSendChanges();
        });

        return null;
    }

    private void doUpgrade(EntityPlayer player, IUpgradeTile upgradeTile, List<BagRef> bagRefs, List<BagRef> modifiedBags) {
        TileComponentUpgrade component = upgradeTile.getComponent();
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
                continue;
            }

            if (hasInfinite && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY)) {
                component.addUpgrades(upgrade, needed);
                continue;
            }

            ItemStack upgradeStack = upgrade.getStack();
            int consumed = consumeUpgradeFromBags(bagRefs, modifiedBags, upgradeStack, needed);
            if (consumed > 0) {
                component.addUpgrades(upgrade, consumed);
            }
        }
    }

    private void doUnload(EntityPlayer player, IUpgradeTile upgradeTile, List<BagRef> bagRefs, List<BagRef> modifiedBags) {
        TileComponentUpgrade component = upgradeTile.getComponent();

        for (Upgrade upgrade : Upgrade.values()) {
            int current = component.getUpgrades(upgrade);
            if (current <= 0) continue;

            ItemStack upgradeStack = upgrade.getStack();
            int remaining = insertUpgradeToBags(bagRefs, modifiedBags, upgradeStack, current);

            if (remaining > 0) {
                ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
                if (player.inventory.addItemStackToInventory(toInsert)) {
                    remaining = 0;
                } else {
                    remaining = toInsert.getCount();
                }
            }

            if (remaining > 0) {
                ItemStack dropStack = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
                player.dropItem(dropStack, false);
                remaining = 0;
            }

            if (remaining < current) {
                int removed = current - remaining;
                for (int i = 0; i < removed; i++) {
                    component.removeUpgrade(upgrade, false);
                }
            }
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

    private int consumeUpgradeFromBags(List<BagRef> bagRefs, List<BagRef> modifiedBags, ItemStack stack, int amount) {
        int remaining = amount;
        for (BagRef bagRef : bagRefs) {
            if (remaining <= 0) break;
            ItemStackHandler handler = ItemCardSlotBag.readHandler(bagRef.stack);
            int consumed = consumeFromHandler(handler, stack, remaining);
            if (consumed > 0) {
                remaining -= consumed;
                ItemCardSlotBag.writeHandler(bagRef.stack, handler);
                addModifiedBag(modifiedBags, bagRef);
            }
        }
        return amount - remaining;
    }

    private int consumeFromHandler(ItemStackHandler handler, ItemStack stack, int amount) {
        int consumed = 0;
        int remaining = amount;
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack)) {
                int toShrink = Math.min(inSlot.getCount(), remaining);
                ItemStack extracted = handler.extractItem(i, toShrink, false);
                if (!extracted.isEmpty()) {
                    int extractedCount = extracted.getCount();
                    consumed += extractedCount;
                    remaining -= extractedCount;
                }
            }
        }
        return consumed;
    }

    private int insertUpgradeToBags(List<BagRef> bagRefs, List<BagRef> modifiedBags, ItemStack stack, int amount) {
        int remaining = amount;
        for (BagRef bagRef : bagRefs) {
            if (remaining <= 0) break;
            ItemStackHandler handler = ItemCardSlotBag.readHandler(bagRef.stack);
            int before = remaining;
            remaining = insertToHandler(handler, stack, remaining);
            if (remaining < before) {
                ItemCardSlotBag.writeHandler(bagRef.stack, handler);
                addModifiedBag(modifiedBags, bagRef);
            }
        }
        return remaining;
    }

    private int insertToHandler(ItemStackHandler handler, ItemStack stack, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(stack, remaining);
            ItemStack result = handler.insertItem(slot, toInsert, false);
            if (result.isEmpty()) {
                remaining = 0;
            } else {
                remaining = result.getCount();
            }
        }
        return remaining;
    }

    private List<BagRef> findBagRefs(EntityPlayer player) {
        List<BagRef> bagRefs = new ArrayList<>();
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                bagRefs.add(new BagRef(stack, i, false, true));
            }
        }
        for (int i = 0; i < player.inventory.offHandInventory.size(); i++) {
            ItemStack stack = player.inventory.offHandInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                bagRefs.add(new BagRef(stack, i, false, false));
            }
        }
        if (BaublesCompat.isBaublesLoaded()) {
            for (int slot = 0; slot < MAX_BAUBLES_SLOTS; slot++) {
                if (BaublesCompat.hasBagAtSlot(player, slot)) {
                    ItemStack stack = BaublesCompat.getStackInSlot(player, slot);
                    if (ItemCardSlotBag.isBag(stack)) {
                        bagRefs.add(new BagRef(stack, slot, true, true));
                    }
                }
            }
        }
        return bagRefs;
    }

    private void addModifiedBag(List<BagRef> modifiedBags, BagRef bagRef) {
        if (bagRef == null || !bagRef.syncPacket) return;
        for (BagRef modifiedBag : modifiedBags) {
            if (modifiedBag.stack == bagRef.stack && modifiedBag.slot == bagRef.slot && modifiedBag.isBaubles == bagRef.isBaubles) {
                return;
            }
        }
        modifiedBags.add(bagRef);
    }
    
    private void syncBagToClient(EntityPlayer player, BagRef bagRef) {
        if (bagRef == null || bagRef.stack == null || !bagRef.syncPacket) return;
        if (!(player instanceof net.minecraft.entity.player.EntityPlayerMP)) return;
        net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) player;
        NBTTagCompound tag = bagRef.stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        int source = bagRef.isBaubles ? PacketSyncBagContents.SOURCE_BAUBLES : PacketSyncBagContents.SOURCE_MAIN;
        PacketHandler.getNetwork().sendTo(new PacketSyncBagContents.SyncBagMessage(source, bagRef.slot, tag), mp);
    }

    private static class BagRef {
        final ItemStack stack;
        final int slot;
        final boolean isBaubles;
        final boolean syncPacket;
        BagRef(ItemStack stack, int slot, boolean isBaubles, boolean syncPacket) {
            this.stack = stack;
            this.slot = slot;
            this.isBaubles = isBaubles;
            this.syncPacket = syncPacket;
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
