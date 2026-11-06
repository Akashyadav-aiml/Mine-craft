package com.akash.upgradestation.block;

import static com.akash.upgradestation.block.StationInventory.INPUT;
import static com.akash.upgradestation.block.StationInventory.MATERIAL;
import static com.akash.upgradestation.block.StationInventory.OUTPUT;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.menu.UpgradeStationMenu;
import com.akash.upgradestation.registry.ModBlockEntities;
import com.akash.upgradestation.registry.ModDataComponents;
import com.akash.upgradestation.upgrade.Upgrade;
import com.akash.upgradestation.upgrade.UpgradeData;
import com.akash.upgradestation.upgrade.Upgrades;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Owns the station's real state: the items, the running job and its progress. Server is the authority. */
public class UpgradeStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_ACTIVE_INDEX = 2;
    public static final int DATA_COUNT = 3;

    private final StationInventory items = new StationInventory(this::setChanged);
    @Nullable
    private Upgrade activeUpgrade;
    private int progress;
    private int maxProgress;

    /**
     * The menu syncs these ints to the client whenever they change. They travel as shorts, so every value
     * must stay below 32767 (process times are a few hundred ticks at most).
     */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> maxProgress;
                case DATA_ACTIVE_INDEX -> Upgrades.indexOf(activeUpgrade);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Never called on the server copy: only the client's SimpleContainerData receives synced values.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public UpgradeStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPGRADE_STATION.get(), pos, state);
    }

    public StationInventory getItems() {
        return items;
    }

    public ContainerData getData() {
        return data;
    }

    @Nullable
    public Upgrade getActiveUpgrade() {
        return activeUpgrade;
    }

    public boolean isProcessing() {
        return activeUpgrade != null;
    }

    /** Checks the upgrade against this station's real inventory. */
    public Upgrade.Status check(Upgrade upgrade) {
        return upgrade.check(items.getStackInSlot(INPUT), items.getStackInSlot(MATERIAL), items.getStackInSlot(OUTPUT));
    }

    /** Called by the packet handler after all validation passed. */
    public void startJob(Upgrade upgrade) {
        activeUpgrade = upgrade;
        progress = 0;
        maxProgress = upgrade.processTicks();
        setLit(true);
        setChanged();
    }

    /** Stops the current job without changing any items (cancel, invalid inputs, or after finishing). */
    public void stopJob() {
        activeUpgrade = null;
        progress = 0;
        maxProgress = 0;
        setLit(false);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UpgradeStationBlockEntity station) {
        station.tick();
    }

    private void tick() {
        if (activeUpgrade == null) return;
        // The player can take the tool or materials out mid-job, so re-check every tick.
        if (check(activeUpgrade) != Upgrade.Status.READY) {
            stopJob();
            return;
        }
        progress++;
        if (progress >= maxProgress) {
            finishJob(activeUpgrade);
        }
        setChanged();
    }

    private void finishJob(Upgrade upgrade) {
        ItemStack result = items.getStackInSlot(INPUT).copy();
        UpgradeData upgrades = result.getOrDefault(ModDataComponents.UPGRADES, UpgradeData.EMPTY);
        result.set(ModDataComponents.UPGRADES, upgrades.withLevel(upgrade.id(), upgrades.levelOf(upgrade.id()) + 1));

        items.extractItem(MATERIAL, upgrade.materialCount(), false);
        items.setStackInSlot(INPUT, ItemStack.EMPTY);
        items.setStackInSlot(OUTPUT, result);
        stopJob();
    }

    private void setLit(boolean lit) {
        BlockState state = getBlockState();
        if (level != null && state.getValue(UpgradeStationBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(UpgradeStationBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    items.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        if (activeUpgrade != null) {
            tag.putString("ActiveUpgrade", activeUpgrade.id().toString());
        }
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        ResourceLocation id = tag.contains("ActiveUpgrade") ? ResourceLocation.tryParse(tag.getString("ActiveUpgrade")) : null;
        // An id we no longer know (e.g. an upgrade removed in an update) resets to idle instead of crashing.
        activeUpgrade = id == null ? null : Upgrades.get(id);
        progress = activeUpgrade == null ? 0 : tag.getInt("Progress");
        maxProgress = activeUpgrade == null ? 0 : tag.getInt("MaxProgress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + UpgradeStation.MODID + ".upgrade_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new UpgradeStationMenu(containerId, playerInventory, this);
    }
}
