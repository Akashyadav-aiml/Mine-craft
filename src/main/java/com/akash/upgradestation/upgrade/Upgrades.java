package com.akash.upgradestation.upgrade;

import com.akash.upgradestation.UpgradeStation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** All upgrades, in display order. A plain code registry: no JSON, no syncing needed. */
public final class Upgrades {
    public static final Upgrade SHARPENED = new Upgrade(UpgradeStation.id("sharpened"),
            Items.QUARTZ, 4, 60, 3, tool -> tool.is(ItemTags.SWORDS) || tool.is(ItemTags.AXES));
    public static final Upgrade REINFORCED = new Upgrade(UpgradeStation.id("reinforced"),
            Items.IRON_INGOT, 3, 80, 3, ItemStack::isDamageableItem);
    public static final Upgrade LIGHTWEIGHT = new Upgrade(UpgradeStation.id("lightweight"),
            Items.FEATHER, 8, 40, 2, tool -> tool.is(ItemTags.PICKAXES) || tool.is(ItemTags.SHOVELS)
                    || tool.is(ItemTags.AXES) || tool.is(ItemTags.HOES));
    public static final Upgrade LUCKY = new Upgrade(UpgradeStation.id("lucky"),
            Items.EMERALD, 1, 100, 1, ItemStack::isDamageableItem);

    public static final List<Upgrade> ALL = List.of(SHARPENED, REINFORCED, LIGHTWEIGHT, LUCKY);

    private static final Map<ResourceLocation, Upgrade> BY_ID =
            ALL.stream().collect(Collectors.toUnmodifiableMap(Upgrade::id, Function.identity()));

    private Upgrades() {}

    /** Returns null for unknown ids. Ids can come from packets or old saves, so callers must handle null. */
    @Nullable
    public static Upgrade get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** Position in {@link #ALL}, or -1 for null. Used as a small int for ContainerData sync. */
    public static int indexOf(@Nullable Upgrade upgrade) {
        // List.of(...).indexOf(null) throws, so null needs its own branch.
        return upgrade == null ? -1 : ALL.indexOf(upgrade);
    }

    /** Index from ContainerData back to an upgrade. Returns null for -1 or a bad value. */
    @Nullable
    public static Upgrade byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
    }

    public static boolean isUpgradeable(ItemStack stack) {
        for (Upgrade upgrade : ALL) {
            if (upgrade.appliesTo().test(stack)) return true;
        }
        return false;
    }

    public static boolean isMaterial(ItemStack stack) {
        for (Upgrade upgrade : ALL) {
            if (stack.is(upgrade.material())) return true;
        }
        return false;
    }
}
