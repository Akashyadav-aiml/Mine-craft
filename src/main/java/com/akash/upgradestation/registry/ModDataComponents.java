package com.akash.upgradestation.registry;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.upgrade.UpgradeData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, UpgradeStation.MODID);

    /** upgradestation:upgrades. persistent = saved with the item, networkSynchronized = sent to clients. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UpgradeData>> UPGRADES =
            DATA_COMPONENTS.registerComponentType("upgrades", builder -> builder
                    .persistent(UpgradeData.CODEC)
                    .networkSynchronized(UpgradeData.STREAM_CODEC));

    private ModDataComponents() {}
}
