package com.nyonio.mekanism_advanced_configuration_card.compat;

import com.nyonio.mekanism_advanced_configuration_card.MekConfigCardUpgradesMod;
import com.nyonio.mekanism_advanced_configuration_card.item.ItemCardSlotBag;
import mekanism.client.gui.GuiUpgradeManagement;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JEIPlugin
public class JEICompat implements IModPlugin {
    
    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(new UpgradeGuiHandler());
    }
    
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    }
    
    private static class UpgradeGuiHandler implements IAdvancedGuiHandler<GuiUpgradeManagement> {
        
        @Override
        public Class<GuiUpgradeManagement> getGuiContainerClass() {
            return GuiUpgradeManagement.class;
        }
        
        @Nullable
        @Override
        public List<Rectangle> getGuiExtraAreas(GuiUpgradeManagement guiContainer) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) return null;
            
            boolean hasBag = false;
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack stack = player.inventory.mainInventory.get(i);
                if (ItemCardSlotBag.isBag(stack)) {
                    hasBag = true;
                    break;
                }
            }
            
            if (!hasBag && BaublesCompat.isBaublesLoaded()) {
                hasBag = BaublesCompat.hasBaublesBag(player);
            }
            
            if (!hasBag) return null;
            
            int guiLeft = guiContainer.getGuiLeft();
            int guiTop = guiContainer.getGuiTop();
            int guiXSize = guiContainer.getXSize();
            
            int panelX = guiLeft + guiXSize + 4;
            int panelY = guiTop - (185 - 166) / 2 - 2;
            
            List<Rectangle> areas = new ArrayList<>();
            areas.add(new Rectangle(panelX, panelY, 72, 185));
            return areas;
        }
    }
}
