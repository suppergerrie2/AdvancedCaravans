package com.suppergerrie2.caravan.entity.ai.sensors;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

public class CaravanTargetSensor<T extends LivingEntity> extends Sensor<T> {

    private final MemoryModuleType<GlobalPos> memoryTarget;
    private final BiFunction<ServerLevel, UUID, Optional<GlobalPos>> getTarget;

    public CaravanTargetSensor(MemoryModuleType<GlobalPos> memoryTarget, BiFunction<ServerLevel, UUID, Optional<GlobalPos>> getTarget) {
        this.memoryTarget = memoryTarget;
        this.getTarget = getTarget;
    }

    @Override
    protected void doTick(@NotNull ServerLevel level, @NotNull T entity) {
        getTarget.apply(level, entity.getUUID())
                .ifPresentOrElse(
                        pos -> entity.getBrain().setMemory(memoryTarget, pos),
                        () -> entity.getBrain().eraseMemory(memoryTarget));
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(memoryTarget);
    }
}
