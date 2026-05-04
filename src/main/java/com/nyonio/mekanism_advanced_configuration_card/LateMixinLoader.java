package com.nyonio.mekanism_advanced_configuration_card;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

public class LateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.mekanism_advanced_configuration_card.json");
    }
}
