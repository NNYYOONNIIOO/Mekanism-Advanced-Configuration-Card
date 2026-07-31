package com.nyonio.mekanism_advanced_configuration_card.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketSyncBagContents {
    
    public static final int SOURCE_MAIN = 0;
    public static final int SOURCE_BAUBLES = 1;
    public static final int SOURCE_OFFHAND = 2;
    
    public static class SyncBagMessage implements IMessage {
        public int source;
        public int slotIndex;
        public NBTTagCompound tagCompound;
        
        public SyncBagMessage() {
        }
        
        public SyncBagMessage(int source, int slotIndex, NBTTagCompound tag) {
            this.source = source;
            this.slotIndex = slotIndex;
            this.tagCompound = tag;
        }
        
        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(source);
            buf.writeInt(slotIndex);
            ByteBufUtils.writeTag(buf, tagCompound);
        }
        
        @Override
        public void fromBytes(ByteBuf buf) {
            source = buf.readInt();
            slotIndex = buf.readInt();
            tagCompound = ByteBufUtils.readTag(buf);
        }
    }
}
