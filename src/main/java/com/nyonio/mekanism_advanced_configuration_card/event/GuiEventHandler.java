package com.nyonio.mekanism_advanced_configuration_card.event;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.gui.GuiSlotBagWindow;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketHandler;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketRequestBagSync;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.GuiUpgradeWindow;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Mouse;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber(modid = MekConfigCardUpgradesMod.MOD_ID, value = Side.CLIENT)
public class GuiEventHandler {

    private static List<ItemStack> foundBags = new ArrayList<>();
    private static ItemStackHandler currentHandler = null;

    private static int firstBagSlot = -1;
    private static boolean firstBagFromBaubles = false;

    private static long refreshTime = 0;
    private static int refreshRemaining = 0;
    private static final int[] REFRESH_DELAYS = {50, 200, 500};

    // The current window instance
    private static GuiSlotBagWindow currentWindow = null;
    // Track whether the slot bag window was closed by user (to not auto-reopen until upgrade window reopens)
    private static boolean userClosedWindow = false;

    public static int getGuiXSize(GuiScreen gui) {
        if (gui instanceof GuiContainer) {
            try {
                java.lang.reflect.Field field = GuiContainer.class.getDeclaredField("xSize");
                field.setAccessible(true);
                return field.getInt(gui);
            } catch (Exception e) {
                return 190;
            }
        }
        return 190;
    }

    public static int getGuiYSize(GuiScreen gui) {
        return 166;
    }

    /**
     * Get the area occupied by the slot bag window for JEI compat.
     */
    public static java.awt.Rectangle getPanelArea(int guiLeft, int guiTop, int guiXSize, int guiYSize) {
        if (currentWindow != null) {
            return currentWindow.getBounds();
        }
        // Fallback: calculate default position
        int panelX = guiLeft + guiXSize + 4;
        int panelY = guiTop;
        return new java.awt.Rectangle(panelX, panelY, GuiSlotBagWindow.WINDOW_WIDTH, GuiSlotBagWindow.WINDOW_HEIGHT);
    }

    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiMekanismTile)) return;

        MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] GUI init - clearing state");
        foundBags.clear();
        currentHandler = null;
        firstBagSlot = -1;
        firstBagFromBaubles = false;
        currentWindow = null;
        userClosedWindow = false;

        PacketHandler.getNetwork().sendToServer(new PacketRequestBagSync.RequestMessage());

        findFirstBag();
        findBags();
        // Don't create window here - wait for upgrade window sync in onDrawBackground
    }

    @SubscribeEvent
    public static void onDrawBackground(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.getGui() instanceof GuiMekanismTile)) return;

        GuiMekanismTile gui = (GuiMekanismTile) event.getGui();

        // Handle refresh timing
        if (refreshTime > 0 && System.currentTimeMillis() >= refreshTime) {
            refreshRemaining--;
            if (refreshRemaining <= 0) {
                refreshTime = 0;
            } else {
                refreshTime = System.currentTimeMillis() + REFRESH_DELAYS[Math.min(REFRESH_DELAYS.length - 1, REFRESH_DELAYS.length - refreshRemaining)];
            }
            updateHandler();
        }

        // Sync with upgrade window visibility
        syncWithUpgradeWindow(gui);
    }

    /**
     * Sync slot bag window visibility with the upgrade management window.
     * - Show slot bag window when upgrade window is open
     * - Hide slot bag window when upgrade window is closed
     * - If user manually closed slot bag window, don't reopen until upgrade window reopens
     */
    private static void syncWithUpgradeWindow(GuiMekanismTile gui) {
        if (!(gui instanceof GuiMekanism)) return;

        GuiMekanism mekanismGui = (GuiMekanism) gui;
        Collection<GuiWindow> windows = mekanismGui.getWindows();
        boolean hasUpgradeWindow = windows.stream().anyMatch(w -> w instanceof GuiUpgradeWindow);

        // Check if our window is still valid (may have been removed by close button)
        boolean hasOurWindow = currentWindow != null && windows.contains(currentWindow);

        if (hasUpgradeWindow) {
            // Upgrade window is open
            if (userClosedWindow) {
                // User manually closed slot bag window, don't auto-reopen
                return;
            }

            if (!hasOurWindow && !foundBags.isEmpty() && currentHandler != null) {
                // Need to create the slot bag window
                createSlotBagWindow(gui);
            }
        } else {
            // No upgrade window open - close our window if it exists
            if (hasOurWindow) {
                mekanismGui.removeWindow(currentWindow);
                currentWindow = null;
            }
            userClosedWindow = false;
        }
    }

    private static void createSlotBagWindow(GuiMekanismTile gui) {
        try {
            TileEntityContainerBlock tile = gui.getTileEntity();
            int guiXSize = getGuiXSize(gui);

            // Default position: right side of the GUI
            int windowX = guiXSize + 4;
            int windowY = 0;

            IGuiWrapper guiWrapper = (IGuiWrapper) gui;
            SelectedWindowData windowData = new SelectedWindowData(GuiSlotBagWindow.SLOT_BAG_WINDOW_TYPE);

            currentWindow = new GuiSlotBagWindow(guiWrapper, windowX, windowY, tile, currentHandler, foundBags.get(0), windowData);

            // Set close listener to track when user manually closes the window
            currentWindow.setTabListeners(
                    window -> {
                        // User closed the slot bag window
                        MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Slot bag window closed by user");
                        currentWindow = null;
                        userClosedWindow = true;
                    },
                    window -> {
                        // Window reattached (after resize)
                    }
            );

            guiWrapper.addWindow(currentWindow);
            MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Created GuiSlotBagWindow synced with upgrade window");
        } catch (Exception e) {
            MekConfigCardUpgradesMod.LOGGER.error("[GuiEvent] Failed to create window", e);
        }
    }

    /**
     * Prevent moving/operating the card slot bag when the window is open.
     * Intercepts mouse clicks on the bag item slot in the player inventory.
     */
    @SubscribeEvent
    public static void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (currentWindow == null) return;
        if (!(event.getGui() instanceof GuiContainer)) return;

        GuiContainer guiContainer = (GuiContainer) event.getGui();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        // Get mouse position scaled to GUI coordinates
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getX() * guiContainer.width / mc.displayWidth;
        int mouseY = guiContainer.height - Mouse.getY() * guiContainer.height / mc.displayHeight - 1;

        // Check if any slot under the mouse contains a bag item
        for (Slot slot : guiContainer.inventorySlots.inventorySlots) {
            if (slot != null && slot.getHasStack()) {
                if (ItemCardSlotBag.isBag(slot.getStack())) {
                    // Check if mouse is over this slot using manual calculation
                    int slotX = guiContainer.getGuiLeft() + slot.xPos;
                    int slotY = guiContainer.getGuiTop() + slot.yPos;
                    if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        // Block the click
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    private static void findFirstBag() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                firstBagSlot = i;
                firstBagFromBaubles = false;
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Found first bag in main inventory slot {}", i);
                return;
            }
        }

        ItemStack offhandStack = player.getHeldItemOffhand();
        if (ItemCardSlotBag.isBag(offhandStack)) {
            firstBagSlot = -1;
            firstBagFromBaubles = false;
            MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Found first bag in offhand");
            return;
        }

        if (BaublesCompat.isBaublesLoaded()) {
            int baublesSlot = BaublesCompat.findFirstBagSlot(player);
            if (baublesSlot >= 0) {
                firstBagSlot = baublesSlot;
                firstBagFromBaubles = true;
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Found first bag in baubles slot {}", baublesSlot);
            }
        }
    }

    public static void scheduleRefresh() {
        refreshRemaining = REFRESH_DELAYS.length;
        refreshTime = System.currentTimeMillis() + REFRESH_DELAYS[0];
    }

    private static void findBags() {
        foundBags.clear();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                foundBags.add(stack);
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] findBags: found bag in main inventory slot {}", i);
                break;
            }
        }

        if (foundBags.isEmpty()) {
            ItemStack offhandStack = player.getHeldItemOffhand();
            if (ItemCardSlotBag.isBag(offhandStack)) {
                foundBags.add(offhandStack);
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] findBags: found bag in offhand");
            }
        }

        if (foundBags.isEmpty() && BaublesCompat.isBaublesLoaded()) {
            List<ItemStack> baublesBags = BaublesCompat.getBaublesBags(player);
            if (!baublesBags.isEmpty()) {
                foundBags.add(baublesBags.get(0));
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] findBags: found bag in baubles");
            }
        }

        if (!foundBags.isEmpty()) updateHandler();
    }

    private static void updateHandler() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) { currentHandler = null; return; }

        ItemStack bagStack = null;
        String bagSource = "none";
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (ItemCardSlotBag.isBag(stack)) {
                bagStack = stack;
                bagSource = "main:" + i;
                break;
            }
        }

        if (bagStack == null) {
            ItemStack offhandStack = player.getHeldItemOffhand();
            if (ItemCardSlotBag.isBag(offhandStack)) {
                bagStack = offhandStack;
                bagSource = "offhand";
            }
        }

        if (bagStack == null && BaublesCompat.isBaublesLoaded()) {
            List<ItemStack> baublesBags = BaublesCompat.getBaublesBags(player);
            if (!baublesBags.isEmpty()) {
                bagStack = baublesBags.get(0);
                bagSource = "baubles";
            }
        }

        if (bagStack != null) {
            NBTTagCompound tag = bagStack.getTagCompound();
            MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] updateHandler: bagSource={}, hasTag={}, tagSize={}",
                bagSource,
                tag != null,
                tag != null ? tag.toString().length() : 0);
        } else {
            MekConfigCardUpgradesMod.LOGGER.warn("[GuiEvent] updateHandler: no bag found!");
        }

        currentHandler = bagStack != null ? ItemCardSlotBag.readHandler(bagStack) : null;

        // Update the window's handler reference
        if (currentWindow != null) {
            currentWindow.updateHandler(currentHandler);
        }

        if (currentHandler != null) {
            int totalItems = 0;
            for (int i = 0; i < currentHandler.getSlots(); i++) {
                ItemStack s = currentHandler.getStackInSlot(i);
                if (!s.isEmpty()) totalItems++;
            }
            MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] updateHandler: handler loaded with {} non-empty slots", totalItems);
        }
    }

    /**
     * Get the current window instance (for JEI compat).
     */
    public static GuiSlotBagWindow getCurrentWindow() {
        return currentWindow;
    }

    /**
     * Check if the slot bag window is currently open.
     */
    public static boolean isWindowOpen() {
        return currentWindow != null;
    }
}
