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

import java.lang.reflect.Field;
import java.util.Map;

public class PacketInstallUpgradeFromBag implements IMessageHandler<PacketInstallUpgradeFromBag.InstallMessage, IMessage> {
    
    public static final int MODE_MAX = 0;
    public static final int MODE_SINGLE = 1;
    
    private static Class<?> modConfigClass;
    private static boolean configLoaded = false;
    
    private static Field upgradesField;
    
    public static void initConfig() {
        if (configLoaded) return;
        try {
            modConfigClass = Class.forName("com.Nyonio.infiniteupgradecard.ModConfig");
            configLoaded = true;
        } catch (ClassNotFoundException e) {
        }
        
        try {
            upgradesField = TileComponentUpgrade.class.getDeclaredField("upgrades");
            upgradesField.setAccessible(true);
        } catch (Exception e) {
        }
    }
    
    private static int getConfigInt(String fieldName, int defaultValue) {
        if (modConfigClass == null) return defaultValue;
        try {
            Field field = modConfigClass.getField(fieldName);
            return field.getInt(null);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private static boolean getConfigBool(String fieldName, boolean defaultValue) {
        if (modConfigClass == null) return defaultValue;
        try {
            Field field = modConfigClass.getField(fieldName);
            return field.getBoolean(null);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    @Override
    public IMessage onMessage(InstallMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().player;
        if (player == null) return null;
        
        initConfig();
        
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
                handleInfiniteUpgrade(component, upgradeTile, message.mode);
            } else if (isSuperInfinite) {
                handleSuperInfiniteUpgrade(component, upgradeTile, message.mode);
            } else {
                if (!(item instanceof IUpgradeItem)) return;

                IUpgradeItem upgradeItem = (IUpgradeItem) item;
                Upgrade upgrade = upgradeItem.getUpgradeType(slotStack);
                if (upgrade == null) return;

                if (!component.canInstall(slotStack)) return;
                
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
                // Notify upgrade item of installation (e.g., wireless AE upgrades need to write encryption keys)
                upgradeItem.onInstalled(slotStack, upgradeTile, toInstall);
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
    
    private void handleInfiniteUpgrade(TileComponentUpgrade component, IUpgradeTile upgradeTile, int mode) {
        int speedCount = getConfigInt("infiniteUpgradeSpeed", 8);
        int energyCount = getConfigInt("infiniteUpgradeEnergy", 8);
        
        if (mode == MODE_MAX) {
            setUpgradeCountDirect(component, upgradeTile, Upgrade.SPEED, speedCount);
            setUpgradeCountDirect(component, upgradeTile, Upgrade.ENERGY, energyCount);
        } else {
            installUpgradeSingle(component, Upgrade.SPEED);
            installUpgradeSingle(component, Upgrade.ENERGY);
        }
    }
    
    private void handleSuperInfiniteUpgrade(TileComponentUpgrade component, IUpgradeTile upgradeTile, int mode) {
        int speedCount = getConfigInt("superInfiniteUpgradeSpeed", 17);
        int energyCount = getConfigInt("superInfiniteUpgradeEnergy", 32);
        int filterCount = getConfigInt("superInfiniteUpgradeFilter", -1);
        int gasCount = getConfigInt("superInfiniteUpgradeGas", -1);
        int mufflingCount = getConfigInt("superInfiniteUpgradeMuffling", -1);
        int stoneGenCount = getConfigInt("superInfiniteUpgradeStoneGenerator", -1);
        boolean anchorEnabled = getConfigBool("superInfiniteUpgradeAnchorEnabled", false);
        int anchorCount = getConfigInt("superInfiniteUpgradeAnchorCount", -1);
        
        if (mode == MODE_MAX) {
            setUpgradeCountDirect(component, upgradeTile, Upgrade.SPEED, speedCount);
            setUpgradeCountDirect(component, upgradeTile, Upgrade.ENERGY, energyCount);
            setUpgradeCountWithConfig(component, upgradeTile, Upgrade.FILTER, filterCount);
            setUpgradeCountWithConfig(component, upgradeTile, Upgrade.GAS, gasCount);
            setUpgradeCountWithConfig(component, upgradeTile, Upgrade.MUFFLING, mufflingCount);
            setUpgradeCountWithConfig(component, upgradeTile, Upgrade.STONE_GENERATOR, stoneGenCount);
            if (anchorEnabled) {
                setUpgradeCountWithConfig(component, upgradeTile, Upgrade.ANCHOR, anchorCount);
            }
        } else {
            installUpgradeSingle(component, Upgrade.SPEED);
            installUpgradeSingle(component, Upgrade.ENERGY);
            installUpgradeSingle(component, Upgrade.FILTER);
            installUpgradeSingle(component, Upgrade.GAS);
            installUpgradeSingle(component, Upgrade.MUFFLING);
            installUpgradeSingle(component, Upgrade.STONE_GENERATOR);
            if (anchorEnabled) {
                installUpgradeSingle(component, Upgrade.ANCHOR);
            }
        }
    }
    
    private void setUpgradeCountDirect(TileComponentUpgrade component, IUpgradeTile upgradeTile, Upgrade upgrade, int count) {
        if (!component.supports(upgrade)) return;
        int current = component.getUpgrades(upgrade);
        if (count <= current) return;
        
        try {
            if (upgradesField != null) {
                @SuppressWarnings("unchecked")
                Map<Upgrade, Integer> upgrades = (Map<Upgrade, Integer>) upgradesField.get(component);
                upgrades.put(upgrade, count);
                component.tileEntity.recalculateUpgradables(upgrade);
                component.tileEntity.markNoUpdateSync();
                return;
            }
        } catch (Exception e) {
        }
        
        component.addUpgrades(upgrade, count - current);
    }
    
    private void setUpgradeCountWithConfig(TileComponentUpgrade component, IUpgradeTile upgradeTile, Upgrade upgrade, int configValue) {
        if (!component.supports(upgrade)) return;
        int count;
        if (configValue == -1) {
            try {
                count = upgrade.getMaxInstalled();
            } catch (Exception e) {
                return;
            }
        } else {
            count = configValue;
        }
        if (count <= 0) return;
        setUpgradeCountDirect(component, upgradeTile, upgrade, count);
    }
    
    private void installUpgradeSingle(TileComponentUpgrade component, Upgrade upgrade) {
        if (!component.supports(upgrade)) return;
        int current = component.getUpgrades(upgrade);
        int max = upgrade.getMaxInstalled();
        if (current < max) {
            component.addUpgrades(upgrade, 1);
        }
    }
    
    private BagRef findBagRef(EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) return new BagRef(stack, i, false, false);
        }
        ItemStack offhandStack = player.getHeldItemOffhand();
        if (ItemCardSlotBag.isBag(offhandStack)) {
            return new BagRef(offhandStack, -1, false, true);
        }
        if (BaublesCompat.isBaublesLoaded()) {
            int slot = BaublesCompat.findFirstBagSlot(player);
            if (slot >= 0) {
                ItemStack stack = BaublesCompat.getStackInSlot(player, slot);
                if (!stack.isEmpty()) return new BagRef(stack, slot, true, false);
            }
        }
        return null;
    }
    
    private void syncBagToClient(EntityPlayerMP player, BagRef bagRef) {
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
