package com.nyonio.mekanism_advanced_configuration_card.event;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketHandler;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketSyncBagContents;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.inventory.container.MekanismTileContainer;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler that automatically moves items from Mekanism's upgradeOutputSlot
 * to the player's card slot bag. This intercepts the vanilla Mekanism upgrade removal
 * flow which puts items in the output slot, and redirects them to the bag instead.
 * 
 * Uses PlayerTickEvent instead of ServerTickEvent for more reliable registration
 * across both single-player and multiplayer.
 */
@Mod.EventBusSubscriber(modid = MekConfigCardUpgradesMod.MOD_ID, value = {Side.CLIENT, Side.SERVER})
public class UpgradeOutputSlotHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Only process on server side
        if (event.player.world.isRemote) return;
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        try {
            checkPlayerContainer(player);
        } catch (Exception e) {
            MekConfigCardUpgradesMod.LOGGER.error("[UpgradeOutputSlotHandler] Error checking player {}", player.getName(), e);
        }
    }

    private static void checkPlayerContainer(EntityPlayerMP player) {
        Container container = player.openContainer;
        if (container == null) return;
        if (!(container instanceof MekanismTileContainer)) return;

        if (!hasBagInInventory(player)) return;

        MekanismTileContainer mekanismContainer = (MekanismTileContainer) container;
        TileEntityContainerBlock tile = mekanismContainer.getTileEntity();
        if (tile == null) return;

        if (!(tile instanceof IUpgradeTile)) return;
        IUpgradeTile upgradeTile = (IUpgradeTile) tile;
        if (!upgradeTile.supportsUpgrades()) return;

        // Try to get the output slot from the container first, then from the component
        ItemStack outputStack = null;
        Slot outputContainerSlot = null;

        // Approach 1: Use the container's VirtualInventoryContainerSlot
        VirtualInventoryContainerSlot containerOutputSlot = mekanismContainer.getUpgradeOutputSlot();
        if (containerOutputSlot != null && containerOutputSlot.getHasStack()) {
            outputStack = containerOutputSlot.getStack();
            outputContainerSlot = containerOutputSlot;
        }

        // Approach 2: Fallback to component's UpgradeInventorySlot
        if (outputStack == null || outputStack.isEmpty()) {
            TileComponentUpgrade component = upgradeTile.getComponent();
            if (component != null) {
                mekanism.common.inventory.slot.UpgradeInventorySlot componentOutputSlot = component.getUpgradeOutputSlot();
                if (componentOutputSlot != null && !componentOutputSlot.isEmpty()) {
                    outputStack = componentOutputSlot.getStack();
                }
            }
        }

        if (outputStack == null || outputStack.isEmpty()) return;

        MekConfigCardUpgradesMod.LOGGER.info("[UpgradeOutputSlotHandler] Found {}x {} in upgradeOutputSlot, moving to bag",
                outputStack.getCount(), outputStack.getDisplayName());

        int remaining = outputStack.getCount();
        List<BagRef> modifiedBags = new ArrayList<>();

        // Try to insert into bag first
        for (int i = 0; i < player.inventory.mainInventory.size() && remaining > 0; i++) {
            ItemStack bagStack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(bagStack)) {
                remaining = insertIntoBag(bagStack, outputStack, remaining);
                modifiedBags.add(new BagRef(bagStack, i, false, false));
            }
        }

        // Try offhand bag
        if (remaining > 0) {
            ItemStack offhandStack = player.getHeldItemOffhand();
            if (ItemCardSlotBag.isBag(offhandStack)) {
                remaining = insertIntoBag(offhandStack, outputStack, remaining);
                modifiedBags.add(new BagRef(offhandStack, -1, false, true));
            }
        }

        // Try Baubles bag
        if (remaining > 0 && BaublesCompat.isBaublesLoaded()) {
            int baublesSlot = BaublesCompat.findFirstBagSlot(player);
            if (baublesSlot >= 0) {
                ItemStack bagStack = BaublesCompat.getStackInSlot(player, baublesSlot);
                if (ItemCardSlotBag.isBag(bagStack)) {
                    remaining = insertIntoBag(bagStack, outputStack, remaining);
                    modifiedBags.add(new BagRef(bagStack, baublesSlot, true, false));
                }
            }
        }

        // Try player inventory for any remaining
        if (remaining > 0) {
            ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(outputStack, remaining);
            if (player.inventory.addItemStackToInventory(toInsert)) {
                remaining = 0;
            } else {
                remaining = toInsert.getCount();
            }
        }

        // Drop any overflow
        if (remaining > 0) {
            ItemStack dropStack = outputStack.copy();
            dropStack.setCount(remaining);
            player.dropItem(dropStack, false);
        }

        // Clear the output slot
        if (outputContainerSlot != null) {
            outputContainerSlot.putStack(ItemStack.EMPTY);
        } else {
            // Fallback: clear via component
            TileComponentUpgrade component = upgradeTile.getComponent();
            if (component != null) {
                component.getUpgradeOutputSlot().setStackUnchecked(ItemStack.EMPTY);
            }
        }

        // Sync bag contents to client
        for (BagRef bagRef : modifiedBags) {
            syncBagToClient(player, bagRef);
        }

        player.inventoryContainer.detectAndSendChanges();
    }

    private static int insertIntoBag(ItemStack bagStack, ItemStack outputStack, int remaining) {
        ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack toInsert = ItemCardSlotBag.copyStackWithSize(outputStack, remaining);
            ItemStack result = handler.insertItem(slot, toInsert, false);
            if (result.isEmpty()) {
                remaining = 0;
            } else {
                remaining = result.getCount();
            }
        }
        ItemCardSlotBag.writeHandler(bagStack, handler);
        return remaining;
    }

    private static boolean hasBagInInventory(EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            if (ItemCardSlotBag.isBag(player.inventory.mainInventory.get(i))) return true;
        }
        if (ItemCardSlotBag.isBag(player.getHeldItemOffhand())) return true;
        if (BaublesCompat.isBaublesLoaded() && BaublesCompat.findFirstBagSlot(player) >= 0) return true;
        return false;
    }

    private static void syncBagToClient(EntityPlayerMP player, BagRef bagRef) {
        if (bagRef == null || bagRef.stack == null) return;
        NBTTagCompound tag = bagRef.stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        int source;
        if (bagRef.isOffhand) {
            source = PacketSyncBagContents.SOURCE_OFFHAND;
        } else if (bagRef.isBaubles) {
            source = PacketSyncBagContents.SOURCE_BAUBLES;
        } else {
            source = PacketSyncBagContents.SOURCE_MAIN;
        }
        PacketHandler.getNetwork().sendTo(new PacketSyncBagContents.SyncBagMessage(source, bagRef.slot, tag), player);
    }

    private static class BagRef {
        final ItemStack stack;
        final int slot;
        final boolean isBaubles;
        final boolean isOffhand;
        BagRef(ItemStack stack, int slot, boolean isBaubles, boolean isOffhand) {
            this.stack = stack;
            this.slot = slot;
            this.isBaubles = isBaubles;
            this.isOffhand = isOffhand;
        }
    }
}
