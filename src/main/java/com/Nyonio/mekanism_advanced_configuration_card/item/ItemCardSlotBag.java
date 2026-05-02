package com.Nyonio.mekanism_advanced_configuration_card.item;

import com.Nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.Nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.Nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.Nyonio.mekanism_advanced_configuration_card.compat.MoreMachineCompat;
import com.Nyonio.mekanism_advanced_configuration_card.gui.GuiHandler;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public class ItemCardSlotBag extends Item {
    private static final String BAG_ITEMS_KEY = "CardSlotBagItems";
    public static final int BAG_SIZE = 27;

    public ItemCardSlotBag() {
        setMaxStackSize(1);
    }

    public static boolean isBag(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == MekConfigCardUpgradesMod.Items.CARD_SLOT_BAG;
    }

    public static boolean isSupportedBagItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        for (Upgrade upgrade : Upgrade.values()) {
            if (upgrade.getStack().getItem() == item) {
                return true;
            }
        }
        ResourceLocation registryName = item.getRegistryName();
        if (registryName != null) {
            String namespace = registryName.getResourceDomain();
            if (namespace.equals("mekanism") || namespace.equals("mekanismgenerators") || namespace.equals("mekanismtools") || namespace.equals("mekanismmultiblockmachine")) {
                String path = registryName.getResourcePath();
                if (path.contains("upgrade") || path.contains("Upgrade") || path.equals("tierinstaller")) {
                    return true;
                }
            }
            if (MoreMachineCompat.isMoreMachineLoaded() && namespace.equals(MoreMachineCompat.MOD_ID)) {
                String path = registryName.getResourcePath();
                if (path.equals("compositetierinstaller") || path.contains("tierinstaller")) {
                    return true;
                }
            }
        }
        if (InfiniteUpgradeCardCompat.isInfiniteUpgradeCardLoaded()) {
            if (item == InfiniteUpgradeCardCompat.getInfiniteUpgradeItem() || item == InfiniteUpgradeCardCompat.getSuperInfiniteUpgradeItem() || item == InfiniteUpgradeCardCompat.getInfiniteFactoryInstallerItem()) {
                return true;
            }
        }
        return false;
    }

    public static ItemStackHandler readHandler(ItemStack bagStack) {
        ItemStackHandler handler = new ItemStackHandler(BAG_SIZE);
        NBTTagCompound tag = bagStack.getTagCompound();
        if (tag != null && tag.hasKey(BAG_ITEMS_KEY)) {
            handler.deserializeNBT(tag.getCompoundTag(BAG_ITEMS_KEY));
        }
        return handler;
    }

    public static void writeHandler(ItemStack bagStack, ItemStackHandler handler) {
        NBTTagCompound tag = bagStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            bagStack.setTagCompound(tag);
        }
        tag.setTag(BAG_ITEMS_KEY, handler.serializeNBT());
    }

    public static boolean hasInBags(InventoryPlayer inventory, ItemStack stack, int amount) {
        return countInBags(inventory, stack) >= amount;
    }

    public static int countInBags(InventoryPlayer inventory, ItemStack stack) {
        int total = 0;
        for (ItemStack bag : inventory.mainInventory) {
            if (isBag(bag)) {
                ItemStackHandler handler = readHandler(bag);
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack inSlot = handler.getStackInSlot(i);
                    if (!inSlot.isEmpty() && inSlot.isItemEqual(stack)) {
                        total += inSlot.getCount();
                    }
                }
            }
        }
        for (ItemStack bag : inventory.offHandInventory) {
            if (isBag(bag)) {
                ItemStackHandler handler = readHandler(bag);
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack inSlot = handler.getStackInSlot(i);
                    if (!inSlot.isEmpty() && inSlot.isItemEqual(stack)) {
                        total += inSlot.getCount();
                    }
                }
            }
        }
        if (BaublesCompat.isBaublesLoaded()) {
            total += BaublesCompat.countInBaublesBags(inventory.player, stack);
        }
        return total;
    }

    public static boolean consumeFromBags(InventoryPlayer inventory, ItemStack stack, int amount) {
        int total = countInBags(inventory, stack);
        if (total < amount) {
            return false;
        }
        int remaining = amount;
        for (int bagIndex = 0; bagIndex < inventory.mainInventory.size() && remaining > 0; bagIndex++) {
            ItemStack bag = inventory.mainInventory.get(bagIndex);
            if (!isBag(bag)) {
                continue;
            }
            ItemStackHandler handler = readHandler(bag);
            boolean changed = false;
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (!inSlot.isEmpty() && inSlot.isItemEqual(stack)) {
                    int toExtract = Math.min(remaining, inSlot.getCount());
                    handler.extractItem(slot, toExtract, false);
                    remaining -= toExtract;
                    changed = true;
                }
            }
            if (changed) {
                writeHandler(bag, handler);
            }
        }
        for (int bagIndex = 0; bagIndex < inventory.offHandInventory.size() && remaining > 0; bagIndex++) {
            ItemStack bag = inventory.offHandInventory.get(bagIndex);
            if (!isBag(bag)) {
                continue;
            }
            ItemStackHandler handler = readHandler(bag);
            boolean changed = false;
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (!inSlot.isEmpty() && inSlot.isItemEqual(stack)) {
                    int toExtract = Math.min(remaining, inSlot.getCount());
                    handler.extractItem(slot, toExtract, false);
                    remaining -= toExtract;
                    changed = true;
                }
            }
            if (changed) {
                writeHandler(bag, handler);
            }
        }
        if (remaining > 0 && BaublesCompat.isBaublesLoaded()) {
            BaublesCompat.consumeFromBaublesBags(inventory.player, stack, remaining);
        }
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            player.openGui(MekConfigCardUpgradesMod.instance, GuiHandler.CARD_SLOT_BAG, world, 0, 0, 0);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
