package com.nyonio.mekanism_advanced_configuration_card.compat;

import net.minecraftforge.fml.common.Loader;

public class InvTweaksCompat {
    private static boolean invTweaksLoaded = false;
    
    public static void init() {
        invTweaksLoaded = Loader.isModLoaded("inventorytweaks");
    }
}
