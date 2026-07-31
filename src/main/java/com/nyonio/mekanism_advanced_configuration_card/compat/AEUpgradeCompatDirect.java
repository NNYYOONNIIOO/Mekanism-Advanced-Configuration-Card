package com.nyonio.mekanism_advanced_configuration_card.compat;

import mekceuaeupgrade.common.host.AEUpgradeNode;
import mekceuaeupgrade.common.host.IAEUpgradeHost;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

class AEUpgradeCompatDirect {

    private static final String AE_WIRELESS_KEYS_TAG = "ae_wireless_keys";
    private static final String WIRELESS_CRAFTING_KEY = "wireless_crafting";
    private static final String WIRELESS_AUTO_PROCESSING_KEY = "wireless_auto_processing";
    private static final String WIRELESS_OUTPUT_KEY = "wireless_output";

    static void saveWirelessKeys(TileEntity tile, NBTTagCompound data) {
        if (!(tile instanceof IAEUpgradeHost)) return;
        IAEUpgradeHost host = (IAEUpgradeHost) tile;
        AEUpgradeNode node = host.getAEUpgradeNode();
        if (node == null) return;

        NBTTagCompound keys = new NBTTagCompound();

        String craftingKey = node.getWirelessCraftingKey();
        if (craftingKey != null && !craftingKey.isEmpty()) {
            keys.setString(WIRELESS_CRAFTING_KEY, craftingKey);
        }

        String autoProcessingKey = node.getWirelessAutoProcessingKey();
        if (autoProcessingKey != null && !autoProcessingKey.isEmpty()) {
            keys.setString(WIRELESS_AUTO_PROCESSING_KEY, autoProcessingKey);
        }

        String outputKey = node.getWirelessOutputKey();
        if (outputKey != null && !outputKey.isEmpty()) {
            keys.setString(WIRELESS_OUTPUT_KEY, outputKey);
        }

        if (!keys.hasNoTags()) {
            data.setTag(AE_WIRELESS_KEYS_TAG, keys);
        }
    }

    static void applyWirelessKeys(TileEntity tile, NBTTagCompound data) {
        if (!(tile instanceof IAEUpgradeHost)) return;
        if (!data.hasKey(AE_WIRELESS_KEYS_TAG)) return;

        IAEUpgradeHost host = (IAEUpgradeHost) tile;
        AEUpgradeNode node = host.getAEUpgradeNode();
        if (node == null) return;

        NBTTagCompound keys = data.getCompoundTag(AE_WIRELESS_KEYS_TAG);

        if (keys.hasKey(WIRELESS_CRAFTING_KEY)) {
            node.setWirelessCraftingKey(keys.getString(WIRELESS_CRAFTING_KEY));
        }

        if (keys.hasKey(WIRELESS_AUTO_PROCESSING_KEY)) {
            node.setWirelessAutoProcessingKey(keys.getString(WIRELESS_AUTO_PROCESSING_KEY));
        }

        if (keys.hasKey(WIRELESS_OUTPUT_KEY)) {
            node.setWirelessOutputKey(keys.getString(WIRELESS_OUTPUT_KEY));
        }
    }
}
