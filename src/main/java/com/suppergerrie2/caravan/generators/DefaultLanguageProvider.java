package com.suppergerrie2.caravan.generators;

import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class DefaultLanguageProvider extends LanguageProvider {

    public DefaultLanguageProvider(DataGenerator gen) {
        super(gen, CaravanMod.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(CaravanMod.CARAVAN_SOURCE_BLOCK.get(), "Caravan Source Block");

        add(CaravanMod.CARAVAN_DEST_BLOCK.get(), "Caravan Destination Block");

        add(CaravanMod.DEBUG_ITEM.get(), "Debug Item. Why do you have this!?");

        add(CaravanMod.CARAVAN_LEADER.get(), "Caravan Leader");
    }
}
