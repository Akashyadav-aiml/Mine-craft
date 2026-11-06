package com.akash.upgradestation.registry;

import com.akash.upgradestation.UpgradeStation;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UpgradeStation.MODID);

    public static final DeferredItem<BlockItem> UPGRADE_STATION =
            ITEMS.registerSimpleBlockItem("upgrade_station", ModBlocks.UPGRADE_STATION);

    private ModItems() {}
}
