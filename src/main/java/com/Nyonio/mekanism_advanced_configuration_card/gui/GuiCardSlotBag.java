package com.Nyonio.mekanism_advanced_configuration_card.gui;

import com.Nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.io.IOException;

public class GuiCardSlotBag extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MekConfigCardUpgradesMod.MOD_ID, "textures/gui/card_slot_bag_panel.png");
    
    public GuiCardSlotBag(InventoryPlayer playerInventory, ItemStack bagStack, int bagSlotIndex) {
        super(new ContainerCardSlotBag(playerInventory, bagStack, bagSlotIndex));
        xSize = 176;
        ySize = 166;
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.translateToLocal("item." + MekConfigCardUpgradesMod.MOD_ID + ".card_slot_bag.name"), 8, 6, 4210752);
        fontRenderer.drawString(I18n.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.player.closeScreen();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
