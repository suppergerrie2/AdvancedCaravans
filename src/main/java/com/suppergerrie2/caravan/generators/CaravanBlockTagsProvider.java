package com.suppergerrie2.caravan.generators;

import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class CaravanBlockTagsProvider extends BlockTagsProvider {
    public CaravanBlockTagsProvider(DataGenerator generator, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(generator, modId, existingFileHelper);
    }

    @Override
    protected void addTags() {
        this.tag(BlockTags.MINEABLE_WITH_AXE).add(CaravanMod.CARAVAN_SOURCE_BLOCK.get()).add(CaravanMod.CARAVAN_DEST_BLOCK.get()).replace(false);
    }
}
