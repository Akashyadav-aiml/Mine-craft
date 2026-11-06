package com.akash.upgradestation.upgrade;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * The value of the upgradestation:upgrades data component: upgrade id -> level.
 * Immutable. Data components must never be changed in place, so withLevel returns a new object.
 */
public record UpgradeData(Map<ResourceLocation, Integer> levels) {
    public static final UpgradeData EMPTY = new UpgradeData(Map.of());

    /** Codec: saves the component to disk (item NBT in the world save). */
    public static final Codec<UpgradeData> CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).xmap(UpgradeData::new, UpgradeData::levels);

    /** StreamCodec: sends the component over the network, so the client can show the tooltip. */
    public static final StreamCodec<ByteBuf, UpgradeData> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, Integer, Map<ResourceLocation, Integer>>map(
                    HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(UpgradeData::new, UpgradeData::levels);

    public UpgradeData {
        levels = Map.copyOf(levels);
    }

    public int levelOf(ResourceLocation id) {
        return levels.getOrDefault(id, 0);
    }

    public UpgradeData withLevel(ResourceLocation id, int level) {
        Map<ResourceLocation, Integer> copy = new HashMap<>(levels);
        copy.put(id, level);
        return new UpgradeData(copy);
    }
}
