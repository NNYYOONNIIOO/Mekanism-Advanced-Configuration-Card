package com.Nyonio.mekanism_advanced_configuration_card.network;

import com.Nyonio.mekanism_advanced_configuration_card.ConfigCardUpgradeHelper;
import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketConfigCardAction implements IMessage {
    private BlockPos pos;
    private EnumFacing side;
    private EnumHand hand;

    public PacketConfigCardAction() {
    }

    public PacketConfigCardAction(BlockPos pos, EnumFacing side, EnumHand hand) {
        this.pos = pos;
        this.side = side;
        this.hand = hand;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        side = EnumFacing.getFront(buf.readByte());
        hand = buf.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(side.getIndex());
        buf.writeBoolean(hand == EnumHand.MAIN_HAND);
    }

    public static class Handler implements IMessageHandler<PacketConfigCardAction, IMessage> {
        @Override
        public IMessage onMessage(PacketConfigCardAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                World world = player.world;
                if (!world.isBlockLoaded(message.pos)) {
                    return;
                }
                TileEntity tile = world.getTileEntity(message.pos);
                if (tile == null) {
                    return;
                }
                ItemStack stack = player.getHeldItem(message.hand);
                if (!(stack.getItem() instanceof mekanism.common.item.ItemConfigurationCard)) {
                    return;
                }
                if (!ConfigCardUpgradeHelper.canUseBatchMode(stack)) {
                    return;
                }
                String failure = ConfigCardUpgradeHelper.pasteCardToTarget(player, tile, message.side, stack, true);
                if (failure != null) {
                    player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + mekanism.common.Mekanism.LOG_TAG + " " + EnumColor.RED + failure));
                }
            });
            return null;
        }
    }
}
