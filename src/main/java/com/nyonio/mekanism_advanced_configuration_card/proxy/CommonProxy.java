package com.nyonio.mekanism_advanced_configuration_card.proxy;

import com.nyonio.mekanism_advanced_configuration_card.network.PacketSyncBagContents;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {
    public void registerClientPackets(SimpleNetworkWrapper network, int startId) {
        network.registerMessage(
            NoOpSyncBagHandler.class,
            PacketSyncBagContents.SyncBagMessage.class,
            startId,
            Side.CLIENT
        );
    }

    public static class NoOpSyncBagHandler implements IMessageHandler<PacketSyncBagContents.SyncBagMessage, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncBagContents.SyncBagMessage message, MessageContext context) {
            return null;
        }
    }
}
