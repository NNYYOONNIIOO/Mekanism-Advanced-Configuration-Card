package com.nyonio.mekanism_advanced_configuration_card.item;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.ModConfig;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.MoreMachineCompat;
import com.nyonio.mekanism_advanced_configuration_card.gui.GuiHandler;
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
        for (Upgrade upgrade : Upgrade.getRegisteredUpgrades()) {
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
        ItemStackHandler handler = new ItemStackHandler(BAG_SIZE) {
            @Override
            public int getSlotLimit(int slot) {
                return ModConfig.getCardSlotBagStackLimit();
            }
            
            @Override
            protected void validateSlotIndex(int slot) {
                if (slot < 0 || slot >= getSlots()) {
                    throw new IndexOutOfBoundsException("Slot " + slot + " is out of range");
                }
            }
            
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                validateSlotIndex(slot);
                ItemStack existing = this.stacks.get(slot);
                int limit = getSlotLimit(slot);
                if (!existing.isEmpty()) {
                    if (!ItemStack.areItemsEqual(existing, stack) || !ItemStack.areItemStackTagsEqual(existing, stack)) {
                        return stack;
                    }
                    limit -= existing.getCount();
                    if (limit <= 0) {
                        return stack;
                    }
                }
                boolean reachedLimit = stack.getCount() > limit;
                int toInsert = reachedLimit ? limit : stack.getCount();
                if (!simulate) {
                    if (existing.isEmpty()) {
                        this.stacks.set(slot, ItemCardSlotBag.copyStackWithSize(stack, toInsert));
                    } else {
                        this.stacks.set(slot, ItemCardSlotBag.copyStackWithSize(existing, existing.getCount() + toInsert));
                    }
                    onContentsChanged(slot);
                }
                return reachedLimit ? ItemCardSlotBag.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
            }
            
            @Override
            public void deserializeNBT(NBTTagCompound nbt) {
                setSize(nbt.hasKey("Size") ? nbt.getInteger("Size") : BAG_SIZE);
                net.minecraft.nbt.NBTTagList tagList = nbt.getTagList("Items", 10);
                for (int i = 0; i < tagList.tagCount(); i++) {
                    NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
                    int slot = itemTags.getInteger("Slot");
                    if (slot >= 0 && slot < stacks.size()) {
                        ItemStack stack = readLargeStack(itemTags);
                        if (!stack.isEmpty()) {
                            stacks.set(slot, stack);
                        }
                    }
                }
                onLoad();
            }
            
            private ItemStack readLargeStack(NBTTagCompound nbt) {
                if (!nbt.hasKey("id")) {
                    return ItemStack.EMPTY;
                }
                String id = nbt.getString("id");
                ResourceLocation location = new ResourceLocation(id);
                Item item = Item.REGISTRY.getObject(location);
                if (item == null) {
                    return ItemStack.EMPTY;
                }
                int count = nbt.hasKey("Count", 3) ? nbt.getInteger("Count") : nbt.getByte("Count");
                int damage = nbt.hasKey("Damage", 3) ? nbt.getInteger("Damage") : nbt.getShort("Damage");
                ItemStack stack = new ItemStack(item, count, damage);
                if (nbt.hasKey("tag")) {
                    stack.setTagCompound(nbt.getCompoundTag("tag"));
                }
                return stack;
            }
            
            @Override
            public NBTTagCompound serializeNBT() {
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setInteger("Size", getSlots());
                net.minecraft.nbt.NBTTagList tagList = new net.minecraft.nbt.NBTTagList();
                for (int i = 0; i < stacks.size(); i++) {
                    if (!stacks.get(i).isEmpty()) {
                        NBTTagCompound itemTags = new NBTTagCompound();
                        itemTags.setInteger("Slot", i);
                        ItemStack stack = stacks.get(i);
                        itemTags.setString("id", stack.getItem().getRegistryName().toString());
                        itemTags.setInteger("Count", stack.getCount());
                        itemTags.setInteger("Damage", stack.getMetadata());
                        if (stack.getTagCompound() != null) {
                            itemTags.setTag("tag", stack.getTagCompound().copy());
                        }
                        tagList.appendTag(itemTags);
                    }
                }
                nbt.setTag("Items", tagList);
                return nbt;
            }
        };
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

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = new ItemStack(stack.getItem(), size, stack.getMetadata());
        if (stack.getTagCompound() != null) {
            copy.setTagCompound(stack.getTagCompound().copy());
        }
        return copy;
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
            int bagSlot = findBagSlotInInventory(player, stack);
            if (bagSlot >= 0) {
                player.openGui(MekConfigCardUpgradesMod.instance, GuiHandler.CARD_SLOT_BAG, world, -1, GuiHandler.SOURCE_MAIN, bagSlot);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
    private int findBagSlotInInventory(EntityPlayer player, ItemStack bagStack) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            if (player.inventory.mainInventory.get(i) == bagStack) {
                return i;
            }
        }
        return -1;
    }
}
