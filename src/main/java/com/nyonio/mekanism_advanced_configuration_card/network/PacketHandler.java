package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    private static SimpleNetworkWrapper network;

    public static void init() {
        network = new SimpleNetworkWrapper(MekConfigCardUpgradesMod.MOD_ID);
        int id = 0;
        network.registerMessage(PacketConfigCardAction.Handler.class, PacketConfigCardAction.class, id++, Side.SERVER);
        network.registerMessage(PacketRemoveUpgradeModded.class, PacketRemoveUpgradeModded.RemoveUpgradeModdedMessage.class, id++, Side.SERVER);
        network.registerMessage(PacketBagSlotClick.class, PacketBagSlotClick.BagSlotClickMessage.class, id++, Side.SERVER);
        network.registerMessage(PacketBatchUpgrade.class, PacketBatchUpgrade.BatchUpgradeMessage.class, id++, Side.SERVER);
        network.registerMessage(PacketSyncBagContents.class, PacketSyncBagContents.SyncBagMessage.class, id++, Side.CLIENT);
        network.registerMessage(PacketRequestBagSync.class, PacketRequestBagSync.RequestMessage.class, id++, Side.SERVER);
    }

    public static SimpleNetworkWrapper getNetwork() {
        return network;
    }
}
