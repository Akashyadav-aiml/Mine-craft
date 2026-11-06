package com.akash.upgradestation;

import com.akash.upgradestation.network.SelectUpgradePayload;
import com.akash.upgradestation.network.ServerPayloadHandler;
import com.akash.upgradestation.registry.ModBlockEntities;
import com.akash.upgradestation.registry.ModBlocks;
import com.akash.upgradestation.registry.ModDataComponents;
import com.akash.upgradestation.registry.ModItems;
import com.akash.upgradestation.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Mod entry point. Runs on both client and server, so it must not touch client-only classes. */
@Mod(UpgradeStation.MODID)
public class UpgradeStation {
    public static final String MODID = "upgradestation";

    public UpgradeStation(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        modBus.addListener(UpgradeStation::registerPayloads);
        modBus.addListener(UpgradeStation::addToCreativeTab);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // "1" is our protocol version. Client and server must have the same one to connect.
        // NETWORK: the handler runs on the network thread, so it must hop to the main thread itself
        // (context.enqueueWork) before touching the world. This makes the thread switch explicit.
        event.registrar("1")
                .executesOn(HandlerThread.NETWORK)
                .playToServer(SelectUpgradePayload.TYPE, SelectUpgradePayload.STREAM_CODEC, ServerPayloadHandler::handle);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.UPGRADE_STATION);
        }
    }
}
