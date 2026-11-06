package com.akash.upgradestation.network;

import com.akash.upgradestation.UpgradeStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: "at station {@code pos}, start (or cancel) upgrade {@code upgradeId}".
 * It carries only intent. Cost, result and timing are decided by the server from its own state.
 */
public record SelectUpgradePayload(BlockPos pos, ResourceLocation upgradeId, boolean start)
        implements CustomPacketPayload {

    public static final Type<SelectUpgradePayload> TYPE = new Type<>(UpgradeStation.id("select_upgrade"));

    public static final StreamCodec<ByteBuf, SelectUpgradePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SelectUpgradePayload::pos,
            ResourceLocation.STREAM_CODEC, SelectUpgradePayload::upgradeId,
            ByteBufCodecs.BOOL, SelectUpgradePayload::start,
            SelectUpgradePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
