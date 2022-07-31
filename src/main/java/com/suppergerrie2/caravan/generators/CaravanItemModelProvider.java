package com.suppergerrie2.caravan.generators;

import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class CaravanItemModelProvider extends ItemModelProvider {
    public CaravanItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, CaravanMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(CaravanMod.CARAVAN_SOURCE_ITEM.get());
        basicItem(CaravanMod.CARAVAN_DEST_ITEM.get());
        basicItem(CaravanMod.DEBUG_ITEM.get());
    }
}
