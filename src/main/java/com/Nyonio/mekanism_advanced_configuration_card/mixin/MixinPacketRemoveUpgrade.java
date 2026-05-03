package com.Nyonio.mekanism_advanced_configuration_card.mixin;

import com.Nyonio.mekanism_advanced_configuration_card.ModConfig;
import com.Nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import mekanism.common.Upgrade;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.network.PacketRemoveUpgrade;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PacketRemoveUpgrade.class)
public class MixinPacketRemoveUpgrade {
    
    @Overwrite(remap = false)
    public IMessage onMessage(PacketRemoveUpgrade.RemoveUpgradeMessage message, MessageContext context) {
        EntityPlayer player = mekanism.common.PacketHandler.getPlayer(context);
        if (player == null) {
            return null;
        }
        mekanism.common.PacketHandler.handlePacket(() -> {
            TileEntity tileEntity = message.coord4D.getTileEntity(player.world);
            if (!mekanism.common.PacketHandler.canAccessTile(player, tileEntity)) {
                return;
            }
            if (tileEntity instanceof IUpgradeTile && tileEntity instanceof TileEntityBasicBlock) {
                IUpgradeTile upgradeTile = (IUpgradeTile) tileEntity;
                Upgrade upgrade = MekanismUtils.getByIndex(Upgrade.values(), message.upgradeType, null);
                if (upgrade == null) {
                    return;
                }
                if (upgradeTile.getComponent().getUpgrades(upgrade) > 0) {
                    ItemStack upgradeStack = upgrade.getStack();
                    upgradeStack.setCount(message.removeAll == 1 ? upgradeTile.getComponent().getUpgrades(upgrade) : 1);
                    
                    boolean inserted = false;
                    
                    if (ModConfig.prioritizeCardSlotBag) {
                        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                            ItemStack bagStack = player.inventory.mainInventory.get(i);
                            if (!bagStack.isEmpty() && bagStack.getItem() instanceof ItemCardSlotBag) {
                                ItemStackHandler handler = ItemCardSlotBag.readHandler(bagStack);
                                int remaining = upgradeStack.getCount();
                                
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
                                
                                if (remaining < upgradeStack.getCount()) {
                                    ItemCardSlotBag.writeHandler(bagStack, handler);
                                    if (remaining <= 0) {
                                        upgradeTile.getComponent().removeUpgrade(upgrade, message.removeAll == 1);
                                        inserted = true;
                                        return;
                                    } else {
                                        upgradeStack.setCount(remaining);
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!inserted) {
                        if (player.inventory.addItemStackToInventory(upgradeStack)) {
                            upgradeTile.getComponent().removeUpgrade(upgrade, message.removeAll == 1);
                        }
                    }
                }
            }
        }, player);
        return null;
    }
}
