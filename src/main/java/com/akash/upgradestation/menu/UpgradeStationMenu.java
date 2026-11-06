package com.akash.upgradestation.menu;

import static com.akash.upgradestation.block.StationInventory.INPUT;
import static com.akash.upgradestation.block.StationInventory.MATERIAL;
import static com.akash.upgradestation.block.StationInventory.OUTPUT;

import com.akash.upgradestation.block.StationInventory;
import com.akash.upgradestation.block.UpgradeStationBlockEntity;
import com.akash.upgradestation.registry.ModBlocks;
import com.akash.upgradestation.registry.ModMenus;
import com.akash.upgradestation.upgrade.Upgrade;
import com.akash.upgradestation.upgrade.Upgrades;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Server-side logic of the GUI: which slots exist and how items move between them. A copy also exists on the
 * client; vanilla keeps the two in sync (slot contents and the ContainerData ints).
 */
public class UpgradeStationMenu extends AbstractContainerMenu {
    // Slot index ranges inside this menu.
    private static final int STATION_END = StationInventory.SIZE;      // 0..2  station
    private static final int INVENTORY_END = STATION_END + 27;         // 3..29 main inventory
    private static final int HOTBAR_END = INVENTORY_END + 9;           // 30..38 hotbar

    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client constructor: called through the MenuType with the BlockPos the server wrote in openMenu. */
    public UpgradeStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos(), new StationInventory(() -> {}), idleClientData());
    }

    /** Until the first sync arrives the client must show "idle" (-1), not upgrade index 0. */
    private static ContainerData idleClientData() {
        SimpleContainerData data = new SimpleContainerData(UpgradeStationBlockEntity.DATA_COUNT);
        data.set(UpgradeStationBlockEntity.DATA_ACTIVE_INDEX, -1);
        return data;
    }

    /** Server constructor: uses the block entity's real inventory and data. */
    public UpgradeStationMenu(int containerId, Inventory playerInventory, UpgradeStationBlockEntity station) {
        this(containerId, playerInventory, station.getBlockPos(), station.getItems(), station.getData());
    }

    private UpgradeStationMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler items,
                               ContainerData data) {
        super(ModMenus.UPGRADE_STATION.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;

        addSlot(new SlotItemHandler(items, INPUT, 98, 20));
        addSlot(new SlotItemHandler(items, MATERIAL, 98, 48));
        addSlot(new SlotItemHandler(items, OUTPUT, 152, 34));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < STATION_END) {
            // Station -> player inventory (hotbar first, like vanilla).
            moved = moveItemStackTo(stack, STATION_END, HOTBAR_END, true);
        } else {
            // Player -> station. mayPlace() uses StationInventory rules, so tools go to INPUT and
            // materials to MATERIAL; OUTPUT accepts nothing. Otherwise swap inventory <-> hotbar.
            moved = moveItemStackTo(stack, INPUT, OUTPUT, false);
            if (!moved) {
                moved = index < INVENTORY_END
                        ? moveItemStackTo(stack, INVENTORY_END, HOTBAR_END, false)
                        : moveItemStackTo(stack, STATION_END, INVENTORY_END, false);
            }
        }
        // Returning EMPTY tells vanilla to stop. Returning a stack makes it call us again (endless loop risk).
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        // Closes the GUI when the block is gone or the player walks away.
        return stillValid(access, player, ModBlocks.UPGRADE_STATION.get());
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public boolean isProcessing() {
        return data.get(UpgradeStationBlockEntity.DATA_ACTIVE_INDEX) >= 0;
    }

    @Nullable
    public Upgrade getActiveUpgrade() {
        return Upgrades.byIndex(data.get(UpgradeStationBlockEntity.DATA_ACTIVE_INDEX));
    }

    /** Progress bar width in pixels, from the synced ints. */
    public int getProgressScaled(int width) {
        int max = data.get(UpgradeStationBlockEntity.DATA_MAX_PROGRESS);
        return max <= 0 ? 0 : data.get(UpgradeStationBlockEntity.DATA_PROGRESS) * width / max;
    }

    /** Client-side check for the Start button (UX only). Uses the synced slot contents. */
    public Upgrade.Status check(Upgrade upgrade) {
        return upgrade.check(slots.get(INPUT).getItem(), slots.get(MATERIAL).getItem(), slots.get(OUTPUT).getItem());
    }
}
