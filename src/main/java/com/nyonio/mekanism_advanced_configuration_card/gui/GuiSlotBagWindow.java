package com.nyonio.mekanism_advanced_configuration_card.gui;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.event.GuiEventHandler;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketBatchUpgrade;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketHandler;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketInstallUpgradeFromBag;
import mekanism.api.Coord4D;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.base.IUpgradeItem;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.Upgrade;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Map;

import java.awt.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class GuiSlotBagWindow extends GuiWindow {

    private static final DecimalFormat COUNT_FORMAT = new DecimalFormat("#.#");

    static {
        COUNT_FORMAT.setRoundingMode(RoundingMode.FLOOR);
    }

    // Register custom WindowType for position persistence
    public static final WindowType SLOT_BAG_WINDOW_TYPE = WindowType.register(
            new ResourceLocation(MekConfigCardUpgradesMod.MOD_ID, "slot_bag"),
            "slot_bag",
            true
    );

    // Layout constants - 3 rows x 9 columns horizontal layout
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;
    private static final int SLOT_ROWS = 3;
    private static final int PADDING = 8;
    private static final int TITLE_BAR_HEIGHT = 18;

    // Window dimensions
    public static final int WINDOW_WIDTH = PADDING + SLOTS_PER_ROW * SLOT_SIZE + PADDING;  // 8 + 162 + 8 = 178
    public static final int WINDOW_HEIGHT = TITLE_BAR_HEIGHT + SLOT_ROWS * SLOT_SIZE + PADDING; // 18 + 54 + 8 = 80

    // Offsets
    private static final int SLOT_OFFSET_X = PADDING;
    private static final int SLOT_OFFSET_Y = TITLE_BAR_HEIGHT;

    // Button constants for install/unload (texture is 18x18)
    private static final int UPGRADE_BTN_SIZE = 18;
    // Unload on the right, Install on the left (swapped as per user request)
    private static final int UNLOAD_BTN_X = WINDOW_WIDTH - PADDING - UPGRADE_BTN_SIZE;
    private static final int INSTALL_BTN_X = UNLOAD_BTN_X - UPGRADE_BTN_SIZE - 2;
    private static final int UPGRADE_BTN_Y = 0;

    // Textures
    private static final ResourceLocation SLOT_BAG_PANEL = new ResourceLocation(
            MekConfigCardUpgradesMod.MOD_ID, "textures/gui/card_slot_bag_panel_upgrade.png");
    private static final ResourceLocation UPGRADE_BUTTONS = new ResourceLocation(
            MekConfigCardUpgradesMod.MOD_ID, "textures/gui/upgrade.png");
    private static final ResourceLocation AMPLIFIER_OFF = new ResourceLocation(
            "mekanism", "gui/amplifier_off.png");

    // Data
    private final TileEntityContainerBlock tile;
    private ItemStackHandler handler;
    private final ItemStack bagStack;

    public GuiSlotBagWindow(IGuiWrapper gui, int x, int y, TileEntityContainerBlock tile, ItemStackHandler handler, ItemStack bagStack) {
        super(gui, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, SLOT_BAG_WINDOW_TYPE);
        interactionStrategy = InteractionStrategy.ALL;
        this.tile = tile;
        this.handler = handler;
        this.bagStack = bagStack;
    }

    public GuiSlotBagWindow(IGuiWrapper gui, int x, int y, TileEntityContainerBlock tile, ItemStackHandler handler, ItemStack bagStack, SelectedWindowData windowData) {
        super(gui, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, windowData);
        interactionStrategy = InteractionStrategy.ALL;
        this.tile = tile;
        this.handler = handler;
        this.bagStack = bagStack;
    }

    public void updateHandler(ItemStackHandler newHandler) {
        this.handler = newHandler;
    }

    @Override
    public void renderBackgroundOverlay(int mouseX, int mouseY) {
        super.renderBackgroundOverlay(mouseX, mouseY);

        // Render the slot bag panel overlay on top of the window background
        MekanismRenderer.bindTexture(SLOT_BAG_PANEL);
        int panelX = relativeX + SLOT_OFFSET_X;
        int panelY = relativeY + SLOT_OFFSET_Y;
        int panelWidth = SLOTS_PER_ROW * SLOT_SIZE;
        int panelHeight = SLOT_ROWS * SLOT_SIZE;
        GuiUtils.blit(panelX, panelY, 0, 0, panelWidth, panelHeight, 256, 256);

        // Render install/unload buttons at the same layer as the outer frame
        renderUpgradeButtons(mouseX, mouseY);
    }

    @Override
    public void renderForeground(int mouseX, int mouseY) {
        super.renderForeground(mouseX, mouseY);

        Minecraft mc = Minecraft.getMinecraft();

        // 1. Draw centered title "卡槽包" in the title bar
        String titleText = net.minecraft.util.text.translation.I18n.translateToLocal(
                "mekanism_advanced_configuration_card.card_slot_bag.title");
        drawTitleText(new TextComponentString(titleText), 5);

        // 2. Render items in slots with z-offset
        if (handler != null) {
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 50);

            for (int row = 0; row < SLOT_ROWS; row++) {
                for (int col = 0; col < SLOTS_PER_ROW; col++) {
                    int slotIndex = row * SLOTS_PER_ROW + col;
                    if (slotIndex >= ItemCardSlotBag.BAG_SIZE) continue;

                    ItemStack stack = handler.getStackInSlot(slotIndex);
                    if (!stack.isEmpty()) {
                        int itemX = relativeX + SLOT_OFFSET_X + col * SLOT_SIZE + 1;
                        int itemY = relativeY + SLOT_OFFSET_Y + row * SLOT_SIZE + 1;

                        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, itemX, itemY);
                        String countText = getCountText(stack.getCount());
                        if (countText != null) {
                            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, itemX, itemY, countText);
                        } else {
                            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, itemX, itemY, null);
                        }

                        // Render conflict overlay for upgrades that can't be installed
                        if (isUpgradeBlocked(stack)) {
                            GlStateManager.pushMatrix();
                            GlStateManager.disableDepth();
                            GlStateManager.disableLighting();
                            GlStateManager.enableBlend();
                            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                            Minecraft.getMinecraft().getTextureManager().bindTexture(AMPLIFIER_OFF);
                            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                            // amplifier_off.png is 18x18, center on 16x16 item icon
                            int overlayX = itemX - 1;
                            int overlayY = itemY - 1;
                            net.minecraft.client.gui.GuiScreen.drawModalRectWithCustomSizedTexture(
                                    overlayX, overlayY, 0, 0, 18, 18, 18, 18);
                            GlStateManager.disableBlend();
                            GlStateManager.enableLighting();
                            GlStateManager.enableDepth();
                            GlStateManager.popMatrix();
                        }
                    }
                }
            }

            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
        }

        // 3. Render hover highlights on slots
        int hoveredSlot = getSlotAtPosition(mouseX, mouseY);
        if (hoveredSlot >= 0) {
            int slotRow = hoveredSlot / SLOTS_PER_ROW;
            int slotCol = hoveredSlot % SLOTS_PER_ROW;
            int slotX = relativeX + SLOT_OFFSET_X + slotCol * SLOT_SIZE;
            int slotY = relativeY + SLOT_OFFSET_Y + slotRow * SLOT_SIZE;
            GuiUtils.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
        }

        // 4. Render tooltip for hovered slot (adjust for GL translation)
        if (hoveredSlot >= 0 && handler != null) {
            ItemStack stack = handler.getStackInSlot(hoveredSlot);
            if (!stack.isEmpty()) {
                ITooltipFlag flag = mc.gameSettings.advancedItemTooltips
                        ? ITooltipFlag.TooltipFlags.ADVANCED
                        : ITooltipFlag.TooltipFlags.NORMAL;
                List<String> tooltipLines = stack.getTooltip(mc.player, flag);
                // mouseX/mouseY are screen coords, but GL is translated by (guiLeft, guiTop)
                // displayTooltips adds to current translation, so subtract gui offset
                displayTooltips(tooltipLines, mouseX - getGuiLeft(), mouseY - getGuiTop());
            }
        }

        // 5. Render tooltip for upgrade buttons
        boolean installHovered = isMouseOverInstallBtn(mouseX, mouseY);
        boolean unloadHovered = isMouseOverUnloadBtn(mouseX, mouseY);
        if (installHovered) {
            String tooltip = net.minecraft.util.text.translation.I18n.translateToLocal(
                    "mekanism_advanced_configuration_card.button.upgrade_all");
            List<String> lines = new ArrayList<>();
            lines.add(tooltip);
            displayTooltips(lines, mouseX - getGuiLeft(), mouseY - getGuiTop());
        } else if (unloadHovered) {
            String tooltip = net.minecraft.util.text.translation.I18n.translateToLocal(
                    "mekanism_advanced_configuration_card.button.unload_all");
            List<String> lines = new ArrayList<>();
            lines.add(tooltip);
            displayTooltips(lines, mouseX - getGuiLeft(), mouseY - getGuiTop());
        }
    }

    private void renderUpgradeButtons(int mouseX, int mouseY) {
        MekanismRenderer.bindTexture(UPGRADE_BUTTONS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Install button: render the 18x18 texture normally
        int installX = relativeX + INSTALL_BTN_X;
        int installY = relativeY + UPGRADE_BTN_Y;
        GuiUtils.blit(installX, installY, 0, 0, UPGRADE_BTN_SIZE, UPGRADE_BTN_SIZE, UPGRADE_BTN_SIZE, UPGRADE_BTN_SIZE);

        // Unload button: render the 18x18 texture vertically flipped (swap minV and maxV)
        int unloadX = relativeX + UNLOAD_BTN_X;
        int unloadY = relativeY + UPGRADE_BTN_Y;
        GuiUtils.innerBlit(unloadX, unloadX + UPGRADE_BTN_SIZE, unloadY, unloadY + UPGRADE_BTN_SIZE, 0,
                0.0f, 1.0f, 1.0f, 0.0f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check install button
        if (isMouseOverInstallBtn(mouseX, mouseY)) {
            handleBatchUpgrade(PacketBatchUpgrade.ACTION_UPGRADE);
            return true;
        }

        // Check unload button
        if (isMouseOverUnloadBtn(mouseX, mouseY)) {
            handleBatchUpgrade(PacketBatchUpgrade.ACTION_UNLOAD);
            return true;
        }

        // Check slot clicks
        int clickedSlot = getSlotAtPosition(mouseX, mouseY);
        if (clickedSlot >= 0) {
            if (button == 0) {
                handleInstallUpgrade(clickedSlot, PacketInstallUpgradeFromBag.MODE_MAX);
            } else if (button == 1) {
                handleInstallUpgrade(clickedSlot, PacketInstallUpgradeFromBag.MODE_SINGLE);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getSlotAtPosition(double mouseX, double mouseY) {
        int relX = (int) mouseX - x;
        int relY = (int) mouseY - y;
        int gridX = relX - SLOT_OFFSET_X;
        int gridY = relY - SLOT_OFFSET_Y;
        if (gridX < 0 || gridY < 0) return -1;
        int col = gridX / SLOT_SIZE;
        int row = gridY / SLOT_SIZE;
        if (col >= SLOTS_PER_ROW || row >= SLOT_ROWS) return -1;
        int slot = row * SLOTS_PER_ROW + col;
        if (slot >= ItemCardSlotBag.BAG_SIZE) return -1;
        return slot;
    }

    private boolean isMouseOverInstallBtn(double mouseX, double mouseY) {
        int relX = (int) mouseX - x;
        int relY = (int) mouseY - y;
        return relX >= INSTALL_BTN_X && relX < INSTALL_BTN_X + UPGRADE_BTN_SIZE
                && relY >= UPGRADE_BTN_Y && relY < UPGRADE_BTN_Y + UPGRADE_BTN_SIZE;
    }

    private boolean isMouseOverUnloadBtn(double mouseX, double mouseY) {
        int relX = (int) mouseX - x;
        int relY = (int) mouseY - y;
        return relX >= UNLOAD_BTN_X && relX < UNLOAD_BTN_X + UPGRADE_BTN_SIZE
                && relY >= UPGRADE_BTN_Y && relY < UPGRADE_BTN_Y + UPGRADE_BTN_SIZE;
    }

    private void handleInstallUpgrade(int clickedSlot, int mode) {
        if (clickedSlot < 0 || handler == null) return;

        ItemStack slotStack = handler.getStackInSlot(clickedSlot);
        if (slotStack.isEmpty()) return;

        Item item = slotStack.getItem();
        boolean isInfinite = InfiniteUpgradeCardCompat.isInfiniteUpgradeCardLoaded() &&
                item == InfiniteUpgradeCardCompat.getInfiniteUpgradeItem();
        boolean isSuperInfinite = InfiniteUpgradeCardCompat.isInfiniteUpgradeCardLoaded() &&
                item == InfiniteUpgradeCardCompat.getSuperInfiniteUpgradeItem();
        boolean isUpgradeItem = item instanceof IUpgradeItem;

        if (!isInfinite && !isSuperInfinite && !isUpgradeItem) return;

        if (tile != null) {
            PacketHandler.getNetwork().sendToServer(
                    new PacketInstallUpgradeFromBag.InstallMessage(Coord4D.get(tile), clickedSlot, mode)
            );
            GuiEventHandler.scheduleRefresh();
        }
    }

    private void handleBatchUpgrade(int action) {
        if (tile != null) {
            MekConfigCardUpgradesMod.LOGGER.info("[SlotBagWindow] handleBatchUpgrade: action={}",
                    action == PacketBatchUpgrade.ACTION_UPGRADE ? "UPGRADE" : "UNLOAD");
            PacketHandler.getNetwork().sendToServer(
                    new PacketBatchUpgrade.BatchUpgradeMessage(Coord4D.get(tile), action)
            );
            GuiEventHandler.scheduleRefresh();
        }
    }

    private static String getCountText(long count) {
        if (count <= 1) {
            return null;
        } else if (count < 10_000) {
            return Long.toString(count);
        } else if (count < 10_000_000) {
            return COUNT_FORMAT.format(count / 1_000D) + "K";
        } else if (count < 10_000_000_000L) {
            return COUNT_FORMAT.format(count / 1_000_000D) + "M";
        } else if (count < 10_000_000_000_000L) {
            return COUNT_FORMAT.format(count / 1_000_000_000D) + "B";
        }
        return ">10T";
    }

    private boolean isUpgradeBlocked(ItemStack stack) {
        if (tile == null || !(tile instanceof IUpgradeTile)) return false;
        if (!(stack.getItem() instanceof IUpgradeItem)) return false;

        IUpgradeItem upgradeItem = (IUpgradeItem) stack.getItem();
        Upgrade upgrade = upgradeItem.getUpgradeType(stack);
        if (upgrade == null) return false;

        TileComponentUpgrade component = ((IUpgradeTile) tile).getComponent();

        // Upgrade not supported by this machine
        if (!component.supports(upgrade)) return true;

        // Upgrade already at max capacity
        if (component.getUpgrades(upgrade) >= upgrade.getMaxInstalled()) return true;

        // Upgrade conflicts with any installed upgrade
        for (Map.Entry<Upgrade, Integer> entry : component.getInstalledUpgrades().entrySet()) {
            if (entry.getValue() > 0 && !upgrade.isCompatibleWith(entry.getKey())) {
                return true;
            }
        }

        // Item-level check (e.g., wireless upgrade needs encryption key)
        if (!upgradeItem.canInstallUpgrade(stack, (IUpgradeTile) tile)) return true;

        return false;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    public ItemStack getBagStack() {
        return bagStack;
    }
}
