package com.suppergerrie2.caravan.generators;

import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.blocks.CaravanTargetBlock;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class CaravanBlockLoot extends BlockLoot {

    @Override
    protected void addTables() {
        generateTargetLootTable(CaravanMod.CARAVAN_SOURCE_BLOCK.get());
        generateTargetLootTable(CaravanMod.CARAVAN_DEST_BLOCK.get());
    }

    private void generateTargetLootTable(Block block) {

        LootTable.Builder tableBuilder = LootTable.lootTable().withPool(
                applyExplosionCondition(block,
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(block)
                                        .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy("CaravanLeaderId", "BlockEntityTag.CaravanLeaderId"))
                                        .when(LootItemBlockStatePropertyCondition
                                                .hasBlockStateProperties(block)
                                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                                        .hasProperty(CaravanTargetBlock.HALF, DoubleBlockHalf.LOWER)
                                                )
                                        )
                                )
                )
        );

        add(block, tableBuilder);
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return CaravanMod.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
    }
}
