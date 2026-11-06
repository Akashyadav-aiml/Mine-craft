package com.akash.upgradestation.block;

import com.akash.upgradestation.upgrade.Upgrades;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The station's 3 slots and their rules. The block entity owns the real one (server). The client menu makes
 * its own empty copy, so slot rules are the same on both sides and the client never predicts a wrong move.
 */
public class StationInventory extends ItemStackHandler {
    public static final int INPUT = 0;
    public static final int MATERIAL = 1;
    public static final int OUTPUT = 2;
    public static final int SIZE = 3;

    private final Runnable onChanged;

    public StationInventory(Runnable onChanged) {
        super(SIZE);
        this.onChanged = onChanged;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> Upgrades.isUpgradeable(stack);
            case MATERIAL -> Upgrades.isMaterial(stack);
            default -> false; // OUTPUT: only the station puts items there (setStackInSlot skips this check)
        };
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == INPUT ? 1 : super.getSlotLimit(slot);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }
}
