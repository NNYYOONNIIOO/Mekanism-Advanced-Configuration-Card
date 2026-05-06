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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class PacketRemoveUpgradeModded implements IMessageHandler<PacketRemoveUpgradeModded.RemoveUpgradeModdedMessage, IMessage> {

    @Override
    public IMessage onMessage(RemoveUpgradeModdedMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().player;
        if (player == null) {
            return null;
        }

        player.getServer().addScheduledTask(() -> {
            TileEntity tileEntity = message.coord4D.getTileEntity(player.world);
            if (!canAccessTile(player, tileEntity)) {
                return;
            }

            if (!(tileEntity instanceof IUpgradeTile) || !(tileEntity instanceof TileEntityBasicBlock)) {
                return;
            }

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
            int remaining = toRemove;

            Object ae2Storage = null;
            Object ae2Source = null;
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
            List<BagRef> modifiedBags = new ArrayList<>();

            for (ModConfig.SourcePriority priority : priorities) {
                if (remaining <= 0) break;

                switch (priority) {
                    case NETWORK:
                        if (ae2Storage != null && ae2Source != null && remaining > 0) {
                            ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
                            int inserted = AE2Compat.insertItemToNetwork(ae2Storage, toInsert, ae2Source);
                            remaining -= inserted;
                        }
                        break;
                    case CARD_SLOT_BAG:
                        remaining = insertToCardSlotBags(player, upgradeStack, remaining, modifiedBags);
                        break;
                    case PLAYER_INVENTORY:
                        if (remaining > 0) {
                            ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
                            player.inventory.addItemStackToInventory(toInsert);
                            remaining = toInsert.getCount();
                        }
                        break;
                }
            }

            if (remaining > 0) {
                ItemStack dropStack = upgradeStack.copy();
                dropStack.setCount(remaining);
                player.dropItem(dropStack, false);
                remaining = 0;
            }

            int removed = toRemove - remaining;
            if (removed > 0) {
                for (int i = 0; i < removed; i++) {
                    upgradeTile.getComponent().removeUpgrade(upgrade, false);
                }
            }

            for (BagRef bagRef : modifiedBags) {
                syncBagToClient(player, bagRef);
            }

            player.inventoryContainer.detectAndSendChanges();
        });

        return null;
    }

    private boolean canAccessTile(EntityPlayer player, TileEntity tile) {
        return mekanism.common.PacketHandler.canAccessTile(player, tile);
    }

    private int insertToCardSlotBags(EntityPlayer player, ItemStack upgradeStack, int remaining, List<BagRef> modifiedBags) {
        for (int i = 0; i < player.inventory.mainInventory.size() && remaining > 0; i++) {
            int before = remaining;
            remaining = tryInsertToBag(player.inventory.mainInventory.get(i), upgradeStack, remaining);
            if (remaining < before) {
                modifiedBags.add(new BagRef(player.inventory.mainInventory.get(i), i, false));
            }
        }
        for (int i = 0; i < player.inventory.offHandInventory.size() && remaining > 0; i++) {
            remaining = tryInsertToBag(player.inventory.offHandInventory.get(i), upgradeStack, remaining);
        }
        if (remaining > 0 && BaublesCompat.isBaublesLoaded()) {
            int slot = BaublesCompat.findFirstBagSlot(player);
            if (slot >= 0) {
                ItemStack bagStack = BaublesCompat.getStackInSlot(player, slot);
                int before = remaining;
                remaining = tryInsertToBag(bagStack, upgradeStack, remaining);
                if (remaining < before) {
                    modifiedBags.add(new BagRef(bagStack, slot, true));
                }
            }
        }
        return remaining;
    }

    private int tryInsertToBag(ItemStack bagStack, ItemStack upgradeStack, int remaining) {
        if (!ItemCardSlotBag.isBag(bagStack)) return remaining;
        ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(upgradeStack, remaining);
            ItemStack result = handler.insertItem(slot, toInsert, false);
            remaining = result.isEmpty() ? 0 : result.getCount();
        }
        ItemCardSlotBag.writeHandler(bagStack, handler);
        return remaining;
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
