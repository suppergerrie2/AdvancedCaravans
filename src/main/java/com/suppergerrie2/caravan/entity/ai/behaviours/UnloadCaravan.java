package com.suppergerrie2.caravan.entity.ai.behaviours;

import com.google.common.collect.ImmutableMap;
import com.suppergerrie2.caravan.CaravanInventoryUtils;
import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.blocks.CaravanTargetBlock;
import com.suppergerrie2.caravan.blocks.CaravanTargetBlockEntity;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

public class UnloadCaravan extends Behavior<CaravanLeaderEntity> {

    // How many ticks to wait until moving another stack
    int moveTime = 20;

    public UnloadCaravan() {
        super(ImmutableMap.of(CaravanMod.CARAVAN_DEST_TARGET.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity owner) {
        GlobalPos globalPos = owner.getBrain().getMemory(CaravanMod.CARAVAN_DEST_TARGET.get()).get();
        return owner.getCaravanHead().isPresent() &&
                globalPos.dimension().equals(level.dimension()) &&
                globalPos.pos().closerToCenterThan(owner.position(), 6) &&
                level.getBlockState(globalPos.pos()).is(CaravanMod.CARAVAN_DEST_BLOCK.get()) &&
                level.getBlockState(globalPos.pos()).getValue(CaravanTargetBlock.ENABLED) &&
                CaravanInventoryUtils.caravanHasItems((Llama) ((ServerLevel) owner.level).getEntity(owner.getCaravanHead().get()));
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, CaravanLeaderEntity entity, long gameTime) {
        if (!entity.getBrain().hasMemoryValue(CaravanMod.CARAVAN_DEST_TARGET.get())) return false;

        GlobalPos globalPos = entity.getBrain().getMemory(CaravanMod.CARAVAN_DEST_TARGET.get()).get();
        return entity.getCaravanHead().isPresent() &&
                CaravanInventoryUtils.caravanHasItems((Llama) ((ServerLevel) entity.level).getEntity(entity.getCaravanHead().get())) &&
                globalPos.dimension().equals(level.dimension()) &&
                level.getBlockState(globalPos.pos()).is(CaravanMod.CARAVAN_DEST_BLOCK.get()) &&
                level.getBlockState(globalPos.pos()).getValue(CaravanTargetBlock.ENABLED) &&
                globalPos.pos().closerToCenterThan(entity.position(), 8);
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity owner, long gameTime) {
        if (gameTime % moveTime != 0) {
            return;
        }

        GlobalPos globalPos = owner.getBrain().getMemory(CaravanMod.CARAVAN_DEST_TARGET.get()).get();
        BlockPos pos = globalPos.pos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CaravanTargetBlockEntity caravanTargetBlockEntity)) {
            return;
        }

        for (LazyOptional<IItemHandler> itemHandler : caravanTargetBlockEntity.getInventories()) {
            // Try to insert a stack to a handler, if one is inserted stop
            if(itemHandler.map(handler -> insertStack(handler, (Llama) level.getEntity(owner.getCaravanHead().get()))).orElse(false)) {
                return;
            }
        }
    }

    private boolean insertStack(IItemHandler handler, Llama head) {
        // Get a stack from the caravan
        ItemStack stack = CaravanInventoryUtils.extractStackFromCaravan(head);

        if (stack.isEmpty()) {
            return false;
        }

        int oldCount = stack.getCount();

        // Insert it into the handler
        stack = ItemHandlerHelper.insertItem(handler, stack, false);

        boolean movedItems = stack.isEmpty() || stack.getCount() < oldCount;

        // If the stack didn't completely fit, put it back into the caravan
        if (!stack.isEmpty()) {
            CaravanInventoryUtils.insertStackIntoCaravan(head, stack);
        }

        return movedItems;
    }
}
