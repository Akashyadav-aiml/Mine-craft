package com.akash.upgradestation.registry;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.menu.UpgradeStationMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, UpgradeStation.MODID);

    // IMenuTypeExtension.create lets the client constructor read extra data (the BlockPos) from a buffer.
    public static final Supplier<MenuType<UpgradeStationMenu>> UPGRADE_STATION =
            MENUS.register("upgrade_station", () -> IMenuTypeExtension.create(UpgradeStationMenu::new));

    private ModMenus() {}
}
