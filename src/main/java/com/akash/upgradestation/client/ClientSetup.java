package com.akash.upgradestation.client;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.client.screen.UpgradeStationScreen;
import com.akash.upgradestation.registry.ModDataComponents;
import com.akash.upgradestation.registry.ModMenus;
import com.akash.upgradestation.upgrade.Upgrade;
import com.akash.upgradestation.upgrade.UpgradeData;
import com.akash.upgradestation.upgrade.Upgrades;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client-only entry point. NeoForge never loads this class on a dedicated server. */
@Mod(value = UpgradeStation.MODID, dist = Dist.CLIENT)
public class ClientSetup {
    public ClientSetup(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerScreens);
        NeoForge.EVENT_BUS.addListener(ClientSetup::addUpgradeTooltip);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.UPGRADE_STATION.get(), UpgradeStationScreen::new);
    }

    /** Shows "Sharpened II" etc. under the item name, read from the synced data component. */
    private static void addUpgradeTooltip(ItemTooltipEvent event) {
        UpgradeData data = event.getItemStack().get(ModDataComponents.UPGRADES);
        if (data == null) return;
        List<Component> tooltip = event.getToolTip();
        int line = Math.min(1, tooltip.size()); // right below the item name
        for (Upgrade upgrade : Upgrades.ALL) {
            int level = data.levelOf(upgrade.id());
            if (level > 0) {
                tooltip.add(line++, Component.translatable("tooltip." + UpgradeStation.MODID + ".upgrade",
                        upgrade.displayName(), Component.translatable("enchantment.level." + level))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
