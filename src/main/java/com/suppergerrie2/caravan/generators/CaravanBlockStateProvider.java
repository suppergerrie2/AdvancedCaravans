package com.suppergerrie2.caravan.generators;

import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.blocks.CaravanTargetBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.loaders.ObjModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class CaravanBlockStateProvider extends BlockStateProvider {
    public CaravanBlockStateProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, CaravanMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        generateModelForTarget(CaravanMod.CARAVAN_SOURCE_BLOCK.get(), "flag_source");
        generateModelForTarget(CaravanMod.CARAVAN_DEST_BLOCK.get(), "flag_dest");
    }

    void generateModelForTarget(Block b, String flag) {
        ResourceLocation blockName = ForgeRegistries.BLOCKS.getKey(b);

        ModelFile enabledTopModel = models().getBuilder(blockName.getPath() + "_enabled")
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/caravan_target_enabled.obj"))
                .flipV(true)
                .end()
            .texture("flag", modLoc("block/%s".formatted(flag)))
            .texture("top", mcLoc("block/stripped_oak_log"))
            .texture("body", mcLoc("block/oak_log"));
        ModelFile disabledTopModel = models().getBuilder(blockName.getPath() + "_disabled")
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/caravan_target_disabled.obj"))
                .flipV(true)
                .end()
                .texture("flag", modLoc("block/%s".formatted(flag)))
                .texture("top", mcLoc("block/stripped_oak_log"))
                .texture("body", mcLoc("block/oak_log"));
        ModelFile bottomModel = models().getBuilder(blockName.getPath() + "_bottom")
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/caravan_target_bottom.obj"))
                .flipV(true)
                .end()
                .texture("flag", modLoc("block/%s".formatted(flag)))
                .texture("body", mcLoc("block/oak_log"));
        horizontalBlock(b, blockState -> {
            if(blockState.getValue(CaravanTargetBlock.HALF) == DoubleBlockHalf.UPPER) {
                if (blockState.getValue(CaravanTargetBlock.ENABLED)) {
                    return enabledTopModel;
                } else {
                    return disabledTopModel;
                }
            } else {
                return bottomModel;
            }
        });
    }

}
