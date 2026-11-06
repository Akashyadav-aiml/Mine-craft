package com.akash.upgradestation.network;

import com.akash.upgradestation.block.UpgradeStationBlockEntity;
import com.akash.upgradestation.menu.UpgradeStationMenu;
import com.akash.upgradestation.upgrade.Upgrade;
import com.akash.upgradestation.upgrade.Upgrades;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

/** Handles {@link SelectUpgradePayload} on the server. Never trusts anything the client sent. */
public final class ServerPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private ServerPayloadHandler() {}

    public static void handle(SelectUpgradePayload payload, IPayloadContext context) {
        // We are on the network thread (see UpgradeStation#registerPayloads). World access is only safe on
        // the main server thread, so all work goes through enqueueWork.
        context.enqueueWork(() -> {
            // Check 1: only a real server player can send this.
            if (!(context.player() instanceof ServerPlayer player)) return;
            String rejection = process(player, payload);
            if (rejection != null) {
                LOGGER.debug("Rejected {} from {}: {}", payload, player.getGameProfile().getName(), rejection);
            }
        });
    }

    /** Runs every check, then acts. Returns why the packet was rejected, or null when it was accepted. */
    @Nullable
    private static String process(ServerPlayer player, SelectUpgradePayload payload) {
        BlockPos pos = payload.pos();

        // Check 2: the player really has this station's menu open. Blocks most forged packets by itself.
        if (!(player.containerMenu instanceof UpgradeStationMenu menu) || !menu.getBlockPos().equals(pos)) {
            return "no open station menu at " + pos;
        }
        // Check 3: never let a packet load (or generate) a chunk.
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)) return "chunk not loaded";
        // Check 4: the block entity is really an upgrade station.
        if (!(level.getBlockEntity(pos) instanceof UpgradeStationBlockEntity station)) {
            return "no upgrade station at " + pos;
        }
        // Check 5: the player is close enough to use it.
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return "too far away";
        // Check 6: the upgrade id exists.
        Upgrade upgrade = Upgrades.get(payload.upgradeId());
        if (upgrade == null) return "unknown upgrade " + payload.upgradeId();

        if (!payload.start()) {
            if (station.getActiveUpgrade() != upgrade) return "cancel: " + upgrade.id() + " is not running";
            station.stopJob();
            return null;
        }
        // Check 7: recipe check on the station's real inventory, never on values from the packet.
        Upgrade.Status status = station.check(upgrade);
        if (status != Upgrade.Status.READY) return "cannot start: " + status;
        // Check 8: no restarting or stacking jobs by spamming the packet.
        if (station.isProcessing()) return "already processing";

        station.startJob(upgrade);
        return null;
    }
}
