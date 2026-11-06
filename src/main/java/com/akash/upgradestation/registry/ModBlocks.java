package com.akash.upgradestation.registry;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.block.UpgradeStationBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UpgradeStation.MODID);

    public static final DeferredBlock<UpgradeStationBlock> UPGRADE_STATION = BLOCKS.register("upgrade_station",
            () -> new UpgradeStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(UpgradeStationBlock.LIT) ? 13 : 0)));

    private ModBlocks() {}
}
