package com.suppergerrie2.caravan.entity.ai.behaviours;

import com.google.common.collect.ImmutableMap;
import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jetbrains.annotations.NotNull;

public class GoToPotentialCaravan extends Behavior<CaravanLeaderEntity> {

    private static final int TICKS_UNTIL_TIMEOUT = 1200;
    final float speedModifier;

    public GoToPotentialCaravan(float speedModifier) {
        super(ImmutableMap.of(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get(), MemoryStatus.VALUE_PRESENT), TICKS_UNTIL_TIMEOUT);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, CaravanLeaderEntity owner) {
        return owner.getCaravanHead().isEmpty();
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, CaravanLeaderEntity entity, long gameTime) {
        return entity.getBrain().hasMemoryValue(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get()) &&
                entity.getBrain().getMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get()).get().distanceTo(entity) > 1.5;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity owner, long gameTime) {
        BehaviorUtils.setWalkAndLookTargetMemories(owner, owner.getBrain().getMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get()).get().blockPosition(), this.speedModifier, 1);
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull CaravanLeaderEntity entity, long gameTime) {
        entity.getBrain().eraseMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get());
    }
}
