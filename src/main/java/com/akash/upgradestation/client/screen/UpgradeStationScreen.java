package com.akash.upgradestation.client.screen;

import com.akash.upgradestation.UpgradeStation;
import com.akash.upgradestation.client.DevExploitTest;
import com.akash.upgradestation.menu.UpgradeStationMenu;
import com.akash.upgradestation.network.SelectUpgradePayload;
import com.akash.upgradestation.upgrade.Upgrade;
import com.akash.upgradestation.upgrade.Upgrades;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client GUI. It only draws synced state and sends intent (a payload). Every decision is made again on
 * the server, so the checks here exist only to give the player a good experience.
 */
public class UpgradeStationScreen extends AbstractContainerScreen<UpgradeStationMenu> {
    private static final ResourceLocation TEXTURE = UpgradeStation.id("textures/gui/upgrade_station.png");
    private static final int PROGRESS_X = 120;
    private static final int PROGRESS_Y = 38;
    private static final int PROGRESS_WIDTH = 28;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int PROGRESS_U = 176; // the filled bar sits right of the background in the texture
    private static final int SELECTED_OUTLINE = 0xFFFFD24A;

    private static final Component START = Component.translatable("gui.upgradestation.start");
    private static final Component CANCEL = Component.translatable("gui.upgradestation.cancel");
    private static final Component SELECT_FIRST =
            Component.translatable("gui.upgradestation.select_first").withStyle(ChatFormatting.RED);

    private final List<Button> upgradeButtons = new ArrayList<>();
    private Button startButton;
    @Nullable
    private Upgrade selected;

    public UpgradeStationScreen(UpgradeStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        upgradeButtons.clear();
        for (int i = 0; i < Upgrades.ALL.size(); i++) {
            Upgrade upgrade = Upgrades.ALL.get(i);
            upgradeButtons.add(addRenderableWidget(Button.builder(upgrade.displayName(), button -> selected = upgrade)
                    .bounds(leftPos + 8, topPos + 18 + i * 18, 84, 16)
                    .build()));
        }
        startButton = addRenderableWidget(Button.builder(START, button -> onStartPressed())
                .bounds(leftPos + 98, topPos + 70, 70, 18)
                .build());
    }

    private void onStartPressed() {
        Upgrade active = menu.getActiveUpgrade();
        if (active != null) {
            PacketDistributor.sendToServer(new SelectUpgradePayload(menu.getBlockPos(), active.id(), false));
        } else if (selected != null) {
            PacketDistributor.sendToServer(new SelectUpgradePayload(menu.getBlockPos(), selected.id(), true));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Upgrade active = menu.getActiveUpgrade();
        if (active != null) {
            selected = active; // while working, always show the upgrade the station is running
        }
        startButton.setMessage(active != null ? CANCEL : START);
        startButton.active = active != null || (selected != null && menu.check(selected) == Upgrade.Status.READY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int width = menu.getProgressScaled(PROGRESS_WIDTH);
        if (width > 0) {
            graphics.blit(TEXTURE, leftPos + PROGRESS_X, topPos + PROGRESS_Y, PROGRESS_U, 0, width, PROGRESS_HEIGHT);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (selected != null) {
            Button button = upgradeButtons.get(Upgrades.indexOf(selected));
            graphics.renderOutline(button.getX() - 1, button.getY() - 1, button.getWidth() + 2, button.getHeight() + 2,
                    SELECTED_OUTLINE);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY); // item tooltips over slots
        for (int i = 0; i < upgradeButtons.size(); i++) {
            if (upgradeButtons.get(i).isHovered()) {
                graphics.renderComponentTooltip(font, upgradeTooltip(Upgrades.ALL.get(i)), mouseX, mouseY);
            }
        }
        if (startButton.isHovered() && !startButton.active) {
            graphics.renderTooltip(font, selected == null ? SELECT_FIRST
                    : menu.check(selected).message().withStyle(ChatFormatting.RED), mouseX, mouseY);
        }
    }

    private List<Component> upgradeTooltip(Upgrade upgrade) {
        List<Component> lines = new ArrayList<>();
        lines.add(upgrade.displayName());
        lines.add(upgrade.description().withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.upgradestation.cost", upgrade.materialCount(),
                upgrade.material().getDescription()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.upgradestation.max_level", upgrade.maxLevel())
                .withStyle(ChatFormatting.GRAY));
        Upgrade.Status status = menu.check(upgrade);
        lines.add(status.message().withStyle(status == Upgrade.Status.READY ? ChatFormatting.GREEN : ChatFormatting.RED));
        return lines;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!FMLEnvironment.production && keyCode == GLFW.GLFW_KEY_F9) {
            DevExploitTest.sendForgedPackets(menu.getBlockPos());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
