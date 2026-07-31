package com.nyonio.mekanism_advanced_configuration_card.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Loader;

public class AEUpgradeCompat {

    private static boolean aeUpgradeLoaded = false;

    public static void init() {
        aeUpgradeLoaded = Loader.isModLoaded("mekceuaeupgrade");
    }

    public static boolean isAEUpgradeLoaded() {
        return aeUpgradeLoaded;
    }

    public static void saveWirelessKeys(TileEntity tile, NBTTagCompound data) {
        if (!aeUpgradeLoaded) return;
        try {
            AEUpgradeCompatDirect.saveWirelessKeys(tile, data);
        } catch (Exception | NoClassDefFoundError e) {
            // Mod not loaded or incompatible
        }
    }

    public static void applyWirelessKeys(TileEntity tile, NBTTagCompound data) {
        if (!aeUpgradeLoaded) return;
        try {
            AEUpgradeCompatDirect.applyWirelessKeys(tile, data);
        } catch (Exception | NoClassDefFoundError e) {
            // Mod not loaded or incompatible
        }
    }
}
