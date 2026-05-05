package com.nyonio.mekanism_advanced_configuration_card.event;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.compat.BaublesCompat;
import com.nyonio.mekanism_advanced_configuration_card.compat.InfiniteUpgradeCardCompat;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketBatchUpgrade;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketHandler;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketInstallUpgradeFromBag;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketRemoveUpgradeModded;
import com.nyonio.mekanism_advanced_configuration_card.network.PacketRequestBagSync;
import mekanism.api.Coord4D;
import mekanism.client.gui.GuiUpgradeManagement;
import mekanism.common.base.IUpgradeTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemStackHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MekConfigCardUpgradesMod.MOD_ID, value = Side.CLIENT)
public class GuiEventHandler {
    
    private static final ResourceLocation CARD_SLOT_BAG_PANEL_TEXTURE = new ResourceLocation(
        MekConfigCardUpgradesMod.MOD_ID, "textures/gui/card_slot_bag_panel_upgrade.png");
    private static final ResourceLocation UPGRADE_BUTTON_TEXTURE = new ResourceLocation(
        MekConfigCardUpgradesMod.MOD_ID, "textures/gui/upgrade.png");
    
    private static final int PANEL_WIDTH = 72;
    private static final int PANEL_HEIGHT = 185;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 3;
    private static final int SLOTS_PER_COLUMN = 9;
    private static final int TITLE_HEIGHT = 12;
    private static final int SLOT_OFFSET_X = 7;
    private static final int SLOT_OFFSET_Y = TITLE_HEIGHT + 5;
    private static final int ICON_OFFSET_X = 1;
    private static final int ICON_OFFSET_Y = 1;
    private static final int BUTTON_SIZE = 12;
    private static final int BUTTON_Y = 4;
    private static final int BUTTON_GAP = 2;
    
    private static List<ItemStack> foundBags = new ArrayList<>();
    private static int hoveredSlot = -1;
    private static ItemStackHandler currentHandler = null;
    private static int cachedGuiLeft = 0;
    private static int cachedGuiTop = 0;
    
    private static int firstBagSlot = -1;
    private static boolean firstBagFromBaubles = false;
    
    private static long refreshTime = 0;
    private static int refreshRemaining = 0;
    private static final int[] REFRESH_DELAYS = {50, 200, 500};
    
    private static boolean wasMouseDown = false;
    private static boolean wasRightMouseDown = false;
    private static int hoveredButton = -1;
    
    private static Field selectedTypeField;
    private static Field tileEntityField;
    
    static {
        try {
            selectedTypeField = GuiUpgradeManagement.class.getDeclaredField("selectedType");
            selectedTypeField.setAccessible(true);
            tileEntityField = GuiUpgradeManagement.class.getDeclaredField("tileEntity");
            tileEntityField.setAccessible(true);
        } catch (Exception e) {
        }
    }
    
    @SubscribeEvent
    public static void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof GuiUpgradeManagement)) return;
        
        GuiButton button = event.getButton();
        if (button.id == 1) {
            event.setCanceled(true);
            
            GuiUpgradeManagement gui = (GuiUpgradeManagement) event.getGui();
            
            try {
                Object selectedType = selectedTypeField.get(gui);
                if (selectedType instanceof mekanism.common.Upgrade) {
                    mekanism.common.Upgrade upgrade = (mekanism.common.Upgrade) selectedType;
                    IUpgradeTile tile = (IUpgradeTile) tileEntityField.get(gui);
                    if (tile != null) {
                        TileEntity te = (TileEntity) tile;
                        boolean removeAll = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
                        PacketHandler.getNetwork().sendToServer(
                            new PacketRemoveUpgradeModded.RemoveUpgradeModdedMessage(
                                Coord4D.get(te),
                                upgrade.ordinal(),
                                removeAll
                            )
                        );
                        scheduleRefresh();
                    }
                }
            } catch (Exception e) {
            }
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null || !(mc.currentScreen instanceof GuiUpgradeManagement)) return;
        if (foundBags.isEmpty()) return;
        
        boolean isNowDown = Mouse.isButtonDown(0);
        boolean isNowRightDown = Mouse.isButtonDown(1);
        
        if (!wasMouseDown && isNowDown) {
            int mouseX = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
            int mouseY = mc.currentScreen.height - Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;
            
            int guiXSize = 190;
            try {
                Field xSizeField = GuiScreen.class.getDeclaredField("xSize");
                xSizeField.setAccessible(true);
                guiXSize = xSizeField.getInt(mc.currentScreen);
            } catch (Exception e) {}
            
            int guiLeft = (mc.currentScreen.width - guiXSize) / 2;
            int guiTop = (mc.currentScreen.height - 166) / 2;
            int panelX = guiLeft + guiXSize + 4;
            int panelY = guiTop - (PANEL_HEIGHT - 166) / 2 - 2;
            
            int relX = mouseX - panelX;
            int relY = mouseY - panelY;
            
            int unloadBtnX = PANEL_WIDTH - SLOT_OFFSET_X - BUTTON_SIZE;
            int upgradeBtnX = unloadBtnX - BUTTON_GAP - BUTTON_SIZE;
            
            if (relX >= upgradeBtnX && relX < upgradeBtnX + BUTTON_SIZE && relY >= BUTTON_Y && relY < BUTTON_Y + BUTTON_SIZE) {
                handleBatchUpgrade(PacketBatchUpgrade.ACTION_UPGRADE);
            } else if (relX >= unloadBtnX && relX < unloadBtnX + BUTTON_SIZE && relY >= BUTTON_Y && relY < BUTTON_Y + BUTTON_SIZE) {
                handleBatchUpgrade(PacketBatchUpgrade.ACTION_UNLOAD);
            } else if (relX >= 0 && relX < PANEL_WIDTH && relY >= 0 && relY < PANEL_HEIGHT) {
                int clickedSlot = getSlotAtPosition(relX, relY);
                if (clickedSlot >= 0) {
                    handleLeftClick(clickedSlot);
                }
            }
        }
        
        if (!wasRightMouseDown && isNowRightDown) {
            int mouseX = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
            int mouseY = mc.currentScreen.height - Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;
            
            int guiXSize = 190;
            try {
                Field xSizeField = GuiScreen.class.getDeclaredField("xSize");
                xSizeField.setAccessible(true);
                guiXSize = xSizeField.getInt(mc.currentScreen);
            } catch (Exception e) {}
            
            int guiLeft = (mc.currentScreen.width - guiXSize) / 2;
            int guiTop = (mc.currentScreen.height - 166) / 2;
            int panelX = guiLeft + guiXSize + 4;
            int panelY = guiTop - (PANEL_HEIGHT - 166) / 2 - 2;
            
            int relX = mouseX - panelX;
            int relY = mouseY - panelY;
            
            if (relX >= 0 && relX < PANEL_WIDTH && relY >= 0 && relY < PANEL_HEIGHT) {
                int clickedSlot = getSlotAtPosition(relX, relY);
                if (clickedSlot >= 0) {
                    handleRightClick(clickedSlot);
                }
            }
        }
        
        wasMouseDown = isNowDown;
        wasRightMouseDown = isNowRightDown;
    }
    
    private static int getSlotAtPosition(int relX, int relY) {
        for (int row = 0; row < SLOTS_PER_COLUMN; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotIndex = col * SLOTS_PER_COLUMN + row;
                if (slotIndex >= ItemCardSlotBag.BAG_SIZE) continue;
                
                int slotX = SLOT_OFFSET_X + col * SLOT_SIZE;
                int slotY = SLOT_OFFSET_Y + row * SLOT_SIZE;
                
                if (relX >= slotX && relX < slotX + SLOT_SIZE && relY >= slotY && relY < slotY + SLOT_SIZE) {
                    return slotIndex;
                }
            }
        }
        return -1;
    }
    
    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiUpgradeManagement)) return;
        
        MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] GUI init - clearing state");
        foundBags.clear();
        currentHandler = null;
        firstBagSlot = -1;
        firstBagFromBaubles = false;
        wasMouseDown = false;
        wasRightMouseDown = false;
        
        PacketHandler.getNetwork().sendToServer(new PacketRequestBagSync.RequestMessage());
        
        findFirstBag();
        findBags();
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
        
        if (BaublesCompat.isBaublesLoaded()) {
            int baublesSlot = BaublesCompat.findFirstBagSlot(player);
            if (baublesSlot >= 0) {
                firstBagSlot = baublesSlot;
                firstBagFromBaubles = true;
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Found first bag in baubles slot {}", baublesSlot);
            }
        }
    }
    
    private static boolean isFirstBagStillThere() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || firstBagSlot < 0) return true;
        
        if (firstBagFromBaubles) {
            if (!BaublesCompat.isBaublesLoaded()) return false;
            return BaublesCompat.hasBagAtSlot(player, firstBagSlot);
        } else {
            if (firstBagSlot >= player.inventory.mainInventory.size()) return false;
            ItemStack stack = player.inventory.mainInventory.get(firstBagSlot);
            return ItemCardSlotBag.isBag(stack);
        }
    }
    
    @SubscribeEvent
    public static void onDrawBackground(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.getGui() instanceof GuiUpgradeManagement)) return;
        
        if (!isFirstBagStillThere()) {
            Minecraft.getMinecraft().player.closeScreen();
            return;
        }
        
        if (foundBags.isEmpty()) findBags();
        if (foundBags.isEmpty()) return;
        
        if (refreshTime > 0 && System.currentTimeMillis() >= refreshTime) {
            refreshRemaining--;
            if (refreshRemaining <= 0) {
                refreshTime = 0;
            } else {
                refreshTime = System.currentTimeMillis() + REFRESH_DELAYS[Math.min(REFRESH_DELAYS.length - 1, REFRESH_DELAYS.length - refreshRemaining)];
            }
            MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] Refresh triggered, remaining={}", refreshRemaining);
            updateHandler();
        }
        
        GuiScreen gui = event.getGui();
        
        int guiXSize = 190;
        try {
            Field xSizeField = GuiScreen.class.getDeclaredField("xSize");
            xSizeField.setAccessible(true);
            guiXSize = xSizeField.getInt(gui);
        } catch (Exception e) {}
        
        cachedGuiLeft = (gui.width - guiXSize) / 2;
        cachedGuiTop = (gui.height - 166) / 2;
        
        int panelX = cachedGuiLeft + guiXSize + 4;
        int panelY = cachedGuiTop - (PANEL_HEIGHT - 166) / 2 - 2;
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(CARD_SLOT_BAG_PANEL_TEXTURE);
        gui.drawTexturedModalRect(panelX, panelY, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        
        String title = net.minecraft.util.text.translation.I18n.translateToLocal("mekanism_advanced_configuration_card.card_slot_bag.title");
        Minecraft.getMinecraft().fontRenderer.drawString(title, panelX + SLOT_OFFSET_X, panelY + 5, 0x404040);
        
        int unloadBtnX = panelX + PANEL_WIDTH - SLOT_OFFSET_X - BUTTON_SIZE;
        int upgradeBtnX = unloadBtnX - BUTTON_GAP - BUTTON_SIZE;
        int btnY = panelY + BUTTON_Y;
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(UPGRADE_BUTTON_TEXTURE);
        
        if (hoveredButton == 0) {
            GlStateManager.color(1.0F, 1.0F, 0.6F, 1.0F);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(upgradeBtnX, btnY, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        
        if (hoveredButton == 1) {
            GlStateManager.color(1.0F, 1.0F, 0.6F, 1.0F);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(unloadBtnX, btnY + BUTTON_SIZE, 0);
        GlStateManager.scale(1, -1, 1);
        GlStateManager.disableCull();
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        if (currentHandler != null) {
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, -50);
            
            for (int row = 0; row < SLOTS_PER_COLUMN; row++) {
                for (int col = 0; col < SLOTS_PER_ROW; col++) {
                    int slotIndex = col * SLOTS_PER_COLUMN + row;
                    if (slotIndex >= ItemCardSlotBag.BAG_SIZE) continue;
                    
                    ItemStack stack = currentHandler.getStackInSlot(slotIndex);
                    if (!stack.isEmpty()) {
                        int slotX = panelX + SLOT_OFFSET_X + col * SLOT_SIZE + ICON_OFFSET_X;
                        int slotY = panelY + SLOT_OFFSET_Y + row * SLOT_SIZE + ICON_OFFSET_Y;
                        
                        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, slotX, slotY);
                        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer, stack, slotX, slotY, null);
                    }
                }
            }
            
            GlStateManager.popMatrix();
            
            RenderHelper.disableStandardItemLighting();
        }
        
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        int relMouseX = mouseX - cachedGuiLeft;
        int relMouseY = mouseY - cachedGuiTop;
        
        int relPanelX = guiXSize + 4;
        int relPanelYOffset = -(PANEL_HEIGHT - 166) / 2 - 2;
        
        hoveredSlot = -1;
        hoveredButton = -1;
        
        int relUnloadBtnX = relPanelX + PANEL_WIDTH - SLOT_OFFSET_X - BUTTON_SIZE;
        int relUpgradeBtnX = relUnloadBtnX - BUTTON_GAP - BUTTON_SIZE;
        
        if (relMouseX >= relUpgradeBtnX && relMouseX < relUpgradeBtnX + BUTTON_SIZE && relMouseY >= relPanelYOffset + BUTTON_Y && relMouseY < relPanelYOffset + BUTTON_Y + BUTTON_SIZE) {
            hoveredButton = 0;
        } else if (relMouseX >= relUnloadBtnX && relMouseX < relUnloadBtnX + BUTTON_SIZE && relMouseY >= relPanelYOffset + BUTTON_Y && relMouseY < relPanelYOffset + BUTTON_Y + BUTTON_SIZE) {
            hoveredButton = 1;
        }
        
        for (int row = 0; row < SLOTS_PER_COLUMN; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotIndex = col * SLOTS_PER_COLUMN + row;
                if (slotIndex >= ItemCardSlotBag.BAG_SIZE) continue;
                
                int slotX = relPanelX + SLOT_OFFSET_X + col * SLOT_SIZE;
                int slotY = relPanelYOffset + SLOT_OFFSET_Y + row * SLOT_SIZE;
                
                if (relMouseX >= slotX && relMouseX < slotX + SLOT_SIZE && relMouseY >= slotY && relMouseY < slotY + SLOT_SIZE) {
                    hoveredSlot = slotIndex;
                    
                    int absSlotX = cachedGuiLeft + slotX;
                    int absSlotY = cachedGuiTop + slotY;
                    
                    GuiScreen.drawRect(absSlotX, absSlotY, absSlotX + SLOT_SIZE, absSlotY + SLOT_SIZE, 0x80FFFFFF);
                    
                    break;
                }
            }
            if (hoveredSlot >= 0) break;
        }
    }
    
    @SubscribeEvent
    public static void onDrawForeground(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiUpgradeManagement)) return;
        if (foundBags.isEmpty()) return;
        
        GuiScreen gui = event.getGui();
        
        if (hoveredButton >= 0) {
            String tooltipKey = hoveredButton == 0 
                ? "mekanism_advanced_configuration_card.button.upgrade_all" 
                : "mekanism_advanced_configuration_card.button.unload_all";
            String tooltip = net.minecraft.util.text.translation.I18n.translateToLocal(tooltipKey);
            List<String> lines = new ArrayList<>();
            lines.add(tooltip);
            gui.drawHoveringText(lines, event.getMouseX(), event.getMouseY());
            return;
        }
        
        if (hoveredSlot < 0 || currentHandler == null) return;
        
        ItemStack stack = currentHandler.getStackInSlot(hoveredSlot);
        if (!stack.isEmpty()) {
            net.minecraft.client.util.ITooltipFlag flag = Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL;
            gui.drawHoveringText(stack.getTooltip(Minecraft.getMinecraft().player, flag), event.getMouseX(), event.getMouseY());
        }
    }
    
    private static void handleLeftClick(int clickedSlot) {
        handleInstallUpgrade(clickedSlot, PacketInstallUpgradeFromBag.MODE_MAX);
    }
    
    private static void handleRightClick(int clickedSlot) {
        handleInstallUpgrade(clickedSlot, PacketInstallUpgradeFromBag.MODE_SINGLE);
    }
    
    private static void handleInstallUpgrade(int clickedSlot, int mode) {
        if (clickedSlot < 0 || currentHandler == null) return;
        
        ItemStack slotStack = currentHandler.getStackInSlot(clickedSlot);
        if (slotStack.isEmpty()) return;
        
        Item item = slotStack.getItem();
        boolean isInfinite = InfiniteUpgradeCardCompat.isInfiniteUpgradeCardLoaded() && 
            item == InfiniteUpgradeCardCompat.getInfiniteUpgradeItem();
        boolean isSuperInfinite = InfiniteUpgradeCardCompat.isInfiniteUpgradeCardLoaded() && 
            item == InfiniteUpgradeCardCompat.getSuperInfiniteUpgradeItem();
        boolean isUpgradeItem = item instanceof mekanism.common.base.IUpgradeItem;
        
        if (!isInfinite && !isSuperInfinite && !isUpgradeItem) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiUpgradeManagement)) return;
        
        GuiUpgradeManagement gui = (GuiUpgradeManagement) mc.currentScreen;
        
        try {
            IUpgradeTile tile = (IUpgradeTile) tileEntityField.get(gui);
            if (tile != null) {
                TileEntity te = (TileEntity) tile;
                PacketHandler.getNetwork().sendToServer(
                    new PacketInstallUpgradeFromBag.InstallMessage(Coord4D.get(te), clickedSlot, mode)
                );
                scheduleRefresh();
            }
        } catch (Exception e) {
        }
    }
    
    private static void handleBatchUpgrade(int action) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiUpgradeManagement)) return;
        
        GuiUpgradeManagement gui = (GuiUpgradeManagement) mc.currentScreen;
        
        try {
            IUpgradeTile tile = (IUpgradeTile) tileEntityField.get(gui);
            if (tile != null) {
                TileEntity te = (TileEntity) tile;
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] handleBatchUpgrade: action={}", action == PacketBatchUpgrade.ACTION_UPGRADE ? "UPGRADE" : "UNLOAD");
                PacketHandler.getNetwork().sendToServer(
                    new PacketBatchUpgrade.BatchUpgradeMessage(Coord4D.get(te), action)
                );
                scheduleRefresh();
            }
        } catch (Exception e) {
            MekConfigCardUpgradesMod.LOGGER.error("[GuiEvent] handleBatchUpgrade error", e);
        }
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
        
        if (foundBags.isEmpty() && BaublesCompat.isBaublesLoaded()) {
            List<ItemStack> baublesBags = BaublesCompat.getBaublesBags(player);
            if (!baublesBags.isEmpty()) {
                foundBags.add(baublesBags.get(0));
                MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] findBags: found bag in baubles");
            }
        }
        
        if (!foundBags.isEmpty()) updateHandler();
    }
    
    public static void scheduleRefresh() {
        refreshRemaining = REFRESH_DELAYS.length;
        refreshTime = System.currentTimeMillis() + REFRESH_DELAYS[0];
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
        
        if (currentHandler != null) {
            int totalItems = 0;
            for (int i = 0; i < currentHandler.getSlots(); i++) {
                ItemStack s = currentHandler.getStackInSlot(i);
                if (!s.isEmpty()) totalItems++;
            }
            MekConfigCardUpgradesMod.LOGGER.info("[GuiEvent] updateHandler: handler loaded with {} non-empty slots", totalItems);
        }
    }
}
