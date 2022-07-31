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

public class LoadCaravan extends Behavior<CaravanLeaderEntity> {

    // How many ticks to wait until moving another stack
    int moveTime = 20;

    public LoadCaravan() {
        super(ImmutableMap.of(CaravanMod.CARAVAN_SOURCE_TARGET.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity owner) {
        GlobalPos globalPos = owner.getBrain().getMemory(CaravanMod.CARAVAN_SOURCE_TARGET.get()).get();
        return owner.getCaravanHead().isPresent() &&
                globalPos.dimension().equals(level.dimension()) &&
                globalPos.pos().closerToCenterThan(owner.position(), 5) &&
                level.getBlockState(globalPos.pos()).is(CaravanMod.CARAVAN_SOURCE_BLOCK.get()) &&
                level.getBlockState(globalPos.pos()).getValue(CaravanTargetBlock.ENABLED) &&
                CaravanInventoryUtils.caravanHasSpace((Llama) level.getEntity(owner.getCaravanHead().get()));
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, CaravanLeaderEntity entity, long gameTime) {
        if (!entity.getBrain().hasMemoryValue(CaravanMod.CARAVAN_SOURCE_TARGET.get())) return false;

        GlobalPos globalPos = entity.getBrain().getMemory(CaravanMod.CARAVAN_SOURCE_TARGET.get()).get();
        return entity.getCaravanHead().isPresent() &&
                CaravanInventoryUtils.caravanHasSpace((Llama) level.getEntity(entity.getCaravanHead().get())) &&
                globalPos.dimension().equals(level.dimension()) &&
                level.getBlockState(globalPos.pos()).is(CaravanMod.CARAVAN_SOURCE_BLOCK.get()) &&
                level.getBlockState(globalPos.pos()).getValue(CaravanTargetBlock.ENABLED) &&
                globalPos.pos().closerToCenterThan(entity.position(), 10);
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity owner, long gameTime) {
        if (gameTime % moveTime != 0) {
            return;
        }

        GlobalPos globalPos = owner.getBrain().getMemory(CaravanMod.CARAVAN_SOURCE_TARGET.get()).get();
        BlockPos pos = globalPos.pos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CaravanTargetBlockEntity caravanTargetBlockEntity)) {
            return;
        }

        for (LazyOptional<IItemHandler> itemHandler : caravanTargetBlockEntity.getInventories()) {
            // Try to extract a stack from a handler, if one is extracted stop
            if(itemHandler.map(handler -> extractStack(handler, (Llama) level.getEntity(owner.getCaravanHead().get()))).orElse(false)) {
                return;
            }
        }
    }

    private boolean extractStack(IItemHandler handler, Llama head) {
        ItemStack stack = ItemStack.EMPTY;
        int slot = -1;

        // Find the biggest (relative to the max stack size) stack in the inventory
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack candidate = handler.extractItem(i, handler.getSlotLimit(i), true);
            if (!candidate.isEmpty()) {
                // This stack is completely full, so no stack can be bigger, so we can stop looking
                if (candidate.getMaxStackSize() == candidate.getCount()) {
                    stack = candidate;
                    slot = i;
                    break;
                }
                // Calculate the percentage filled of the stack and compare it to the chosen stack, if it's more than the current stack we found a new winner.
                else if (candidate.getCount() / (float) candidate.getMaxStackSize() > stack.getCount() / (float) stack.getMaxStackSize()) {
                    stack = candidate;
                    slot = i;
                }
            }
        }

        // All stacks were empty so there was nothing to extract
        if (stack.isEmpty()) {
            return false;
        }

        stack = handler.extractItem(slot, handler.getSlotLimit(slot), false);

        int count = stack.getCount();

        stack = CaravanInventoryUtils.insertStackIntoCaravan(head, stack);

        // If the stack is empty or contains less items than before then some items were moved, so items were successfully moved.
        boolean movedItems = stack.isEmpty() || stack.getCount() < count;

        if (!stack.isEmpty()) {
            ItemHandlerHelper.insertItem(handler, stack, false);
        }

        return movedItems;
    }

}
