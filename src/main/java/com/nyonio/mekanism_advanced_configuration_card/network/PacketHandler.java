package com.nyonio.mekanism_advanced_configuration_card.network;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final String CHANNEL = "mekconfigcard";
    private static SimpleNetworkWrapper network;
    private static int currentId = 0;

    public static void init() {
        network = new SimpleNetworkWrapper(CHANNEL);
        
        network.registerMessage(PacketConfigCardAction.Handler.class, PacketConfigCardAction.class, currentId++, Side.SERVER);
        network.registerMessage(PacketRemoveUpgradeModded.class, PacketRemoveUpgradeModded.RemoveUpgradeModdedMessage.class, currentId++, Side.SERVER);
        network.registerMessage(PacketBatchUpgrade.class, PacketBatchUpgrade.BatchUpgradeMessage.class, currentId++, Side.SERVER);
        
        MekConfigCardUpgradesMod.proxy.registerClientPackets(network, currentId);
        currentId++;
        
        network.registerMessage(PacketRequestBagSync.class, PacketRequestBagSync.RequestMessage.class, currentId++, Side.SERVER);
        network.registerMessage(PacketInstallUpgradeFromBag.class, PacketInstallUpgradeFromBag.InstallMessage.class, currentId++, Side.SERVER);
    }

    public static SimpleNetworkWrapper getNetwork() {
        return network;
    }
}
