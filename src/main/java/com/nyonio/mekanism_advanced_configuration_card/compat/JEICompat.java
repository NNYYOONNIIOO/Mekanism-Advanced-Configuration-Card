package com.nyonio.mekanism_advanced_configuration_card.compat;

import com.nyonio.mekanism_advanced_configuration_card.event.GuiEventHandler;
import mekanism.client.gui.GuiMekanismTile;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IAdvancedGuiHandler;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.ArrayList;
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
    
    private static class UpgradeGuiHandler implements IAdvancedGuiHandler<GuiMekanismTile> {
        
        @Override
        public Class<GuiMekanismTile> getGuiContainerClass() {
            return GuiMekanismTile.class;
        }
        
        @Nullable
        @Override
        public List<Rectangle> getGuiExtraAreas(GuiMekanismTile guiContainer) {
            // Check if the slot bag window is currently open
            com.nyonio.mekanism_advanced_configuration_card.gui.GuiSlotBagWindow window = GuiEventHandler.getCurrentWindow();
            if (window == null) return null;

            List<Rectangle> areas = new ArrayList<>();
            areas.add(GuiEventHandler.getPanelArea(guiContainer.getGuiLeft(), guiContainer.getGuiTop(), GuiEventHandler.getGuiXSize(guiContainer), GuiEventHandler.getGuiYSize(guiContainer)));
            return areas;
        }
    }
}
