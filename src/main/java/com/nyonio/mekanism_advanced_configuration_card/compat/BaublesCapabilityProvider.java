package com.nyonio.mekanism_advanced_configuration_card.compat;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BaublesCapabilityProvider {

    public static void attachCapability(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.getItem() instanceof ItemCardSlotBag) {
            event.addCapability(
                new net.minecraft.util.ResourceLocation(MekConfigCardUpgradesMod.MOD_ID, "bauble"),
                new Provider()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static class Provider implements ICapabilityProvider {

        private final IBauble bauble = new IBauble() {
            @Override
            public BaubleType getBaubleType(ItemStack itemstack) {
                return BaubleType.TRINKET;
            }
        };

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE;
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE) {
                return (T) bauble;
            }
            return null;
        }
    }
}
