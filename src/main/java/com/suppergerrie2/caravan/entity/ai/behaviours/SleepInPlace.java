package com.suppergerrie2.caravan.entity.ai.behaviours;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.pathfinder.Node;
import org.jetbrains.annotations.NotNull;

public class SleepInPlace extends Behavior<LivingEntity> {

    long lastWokeTime = 0;
    final long minimumAwakeTime;
    BlockPos sleepPos = BlockPos.ZERO;

    public SleepInPlace(long minimumAwakeTime) {
        super(ImmutableMap.of(MemoryModuleType.LAST_WOKEN, MemoryStatus.REGISTERED));
        this.minimumAwakeTime = minimumAwakeTime;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, LivingEntity owner) {
        return !owner.isPassenger() && level.getGameTime() - lastWokeTime > minimumAwakeTime;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, LivingEntity entity, long gameTime) {
        return entity.getBrain().isActive(Activity.REST) && entity.blockPosition().closerThan(sleepPos, 2) && entity.isSleeping();
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull LivingEntity entity, long gameTime) {
        if (gameTime > lastWokeTime + minimumAwakeTime) {
            InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(level, entity, null, null);
            entity.startSleeping(entity.blockPosition());
            sleepPos = entity.blockPosition();
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, LivingEntity entity, long gameTime) {
        if (entity.isSleeping()) {
            entity.stopSleeping();
        }
        lastWokeTime = gameTime;
        sleepPos = BlockPos.ZERO;
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }
}
