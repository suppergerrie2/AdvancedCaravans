package com.suppergerrie2.caravan.entity.ai.behaviours;

import com.google.common.collect.ImmutableMap;
import com.suppergerrie2.caravan.CaravanInventoryUtils;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.horse.Llama;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class GoToTarget extends Behavior<CaravanLeaderEntity> {

    private static final int TICKS_UNTIL_TIMEOUT = 1200;
    final float speedModifier;
    private final MemoryModuleType<GlobalPos> targetMemory;
    private final boolean requiresEmptyInventory;

    public GoToTarget(float speedModifier, MemoryModuleType<GlobalPos> targetMemory, boolean requiresEmptyInventory) {
        super(ImmutableMap.of(targetMemory, MemoryStatus.VALUE_PRESENT), TICKS_UNTIL_TIMEOUT);
        this.speedModifier = speedModifier;
        this.targetMemory = targetMemory;
        this.requiresEmptyInventory = requiresEmptyInventory;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, CaravanLeaderEntity owner) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") GlobalPos targetPos = owner.getBrain().getMemory(targetMemory).get();

        Optional<UUID> caravanHead = owner.getCaravanHead();
        // Requires a caravan
        if (caravanHead.isPresent()) {
            Llama llama = (Llama) level.getEntity(caravanHead.get());
            // Either the inventory should be empty or there should be some items in it depending on the requiresEmptyInventory flag
            if (requiresEmptyInventory && !CaravanInventoryUtils.caravanInventoryIsEmpty(llama)) {
                return false;
            } else if (!requiresEmptyInventory && CaravanInventoryUtils.caravanInventoryIsEmpty(llama)) {
                return false;
            }
        } else {
            return false;
        }

        return targetPos.dimension().equals(level.dimension()) &&
                !targetPos.pos().closerToCenterThan(owner.position(), 5);
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, CaravanLeaderEntity entity, long gameTime) {
        if(!entity.getBrain().hasMemoryValue(targetMemory)) return false;

        @SuppressWarnings("OptionalGetWithoutIsPresent") GlobalPos targetPos = entity.getBrain().getMemory(targetMemory).get();
        return !targetPos.pos().closerToCenterThan(entity.position(), 5) ;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity owner, long gameTime) {
        //noinspection OptionalGetWithoutIsPresent
        BehaviorUtils.setWalkAndLookTargetMemories(owner, owner.getBrain().getMemory(targetMemory).get().pos(), this.speedModifier, 1);
    }
}
