package com.nyonio.mekanism_advanced_configuration_card.proxy;

import com.nyonio.mekanism_advanced_configuration_card.client.PacketSyncBagContentsHandler;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketSyncBagContents;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void registerClientPackets(SimpleNetworkWrapper network, int startId) {
        network.registerMessage(
            PacketSyncBagContentsHandler.class,
            PacketSyncBagContents.SyncBagMessage.class,
            startId,
            Side.CLIENT
        );
    }
}
