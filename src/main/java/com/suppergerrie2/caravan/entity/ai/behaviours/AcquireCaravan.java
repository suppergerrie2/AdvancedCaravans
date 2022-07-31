package com.suppergerrie2.caravan.entity.ai.behaviours;

import com.google.common.collect.ImmutableMap;
import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.horse.Llama;
import org.jetbrains.annotations.NotNull;

public class AcquireCaravan extends Behavior<CaravanLeaderEntity> {
    public AcquireCaravan() {
        super(ImmutableMap.of(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, CaravanLeaderEntity owner) {
        Llama llama = owner.getBrain().getMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get()).get();

        // Make sure the llama is not leashed, it can be claimed by a different leader in the last 20 ticks
        if(llama.isLeashed()) return false;

        BlockPos position = llama.blockPosition();
        return position.closerToCenterThan(owner.position(), 2.0D) && owner.getCaravanHead().isEmpty() /* || pOwner.assignProfessionWhenSpawned()*/;
    }

    @Override
    protected void start(@NotNull ServerLevel level, CaravanLeaderEntity entity, long gameTime) {
        Llama llama = entity.getBrain().getMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get()).get();
        llama.setLeashedTo(entity, true);
        entity.getBrain().eraseMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get());
        entity.setCaravanHead(llama);
    }
}
