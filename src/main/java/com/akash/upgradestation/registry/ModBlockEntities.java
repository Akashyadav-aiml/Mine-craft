package com.akash.upgradestation.registry;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.block.UpgradeStationBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UpgradeStation.MODID);

    public static final Supplier<BlockEntityType<UpgradeStationBlockEntity>> UPGRADE_STATION =
            BLOCK_ENTITIES.register("upgrade_station", () -> BlockEntityType.Builder
                    .of(UpgradeStationBlockEntity::new, ModBlocks.UPGRADE_STATION.get())
                    .build(null));

    private ModBlockEntities() {}
}
