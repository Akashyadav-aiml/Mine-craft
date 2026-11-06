package com.akash.upgradestation.upgrade;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.registry.ModDataComponents;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One upgrade the station can apply. Upgrades are defined in code (see {@link Upgrades}), so the client and
 * the server already have the same list. Packets only ever carry the id.
 */
public record Upgrade(ResourceLocation id, Item material, int materialCount, int processTicks, int maxLevel,
                      Predicate<ItemStack> appliesTo) {

    /** Why an upgrade can or cannot start. The server uses it to decide, the client to explain. */
    public enum Status {
        READY, NO_TOOL, WRONG_TOOL, MAX_LEVEL, NOT_ENOUGH_MATERIAL, OUTPUT_FULL;

        public MutableComponent message() {
            return Component.translatable("status." + UpgradeStation.MODID + "." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Checks the three station slots. Same code on both sides, but only the server's answer is trusted. */
    public Status check(ItemStack tool, ItemStack material, ItemStack output) {
        if (tool.isEmpty()) return Status.NO_TOOL;
        if (!appliesTo.test(tool)) return Status.WRONG_TOOL;
        if (levelOn(tool) >= maxLevel) return Status.MAX_LEVEL;
        if (!material.is(this.material) || material.getCount() < materialCount) return Status.NOT_ENOUGH_MATERIAL;
        if (!output.isEmpty()) return Status.OUTPUT_FULL;
        return Status.READY;
    }

    public int levelOn(ItemStack tool) {
        return tool.getOrDefault(ModDataComponents.UPGRADES, UpgradeData.EMPTY).levelOf(id);
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey());
    }

    public MutableComponent description() {
        return Component.translatable(translationKey() + ".desc");
    }

    private String translationKey() {
        return "upgrade." + id.getNamespace() + "." + id.getPath();
    }
}
