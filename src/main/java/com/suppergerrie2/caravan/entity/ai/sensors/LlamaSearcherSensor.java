package com.suppergerrie2.caravan.entity.ai.sensors;

import com.google.common.collect.ImmutableSet;
import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LlamaSearcherSensor<T extends LivingEntity> extends Sensor<T> {

    @Override
    protected void doTick(@NotNull ServerLevel level, @NotNull T sensorOwner) {
        sensorOwner.getBrain().eraseMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get());

        // Find a llama nearby that is unclaimed by filtering for llamas that are tamed and not leashed/in a caravan
        sensorOwner.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).flatMap(memory -> memory.stream()
                .filter(entity -> entity instanceof Llama)
                .map(entity -> (Llama) entity)
                .filter(AbstractHorse::isTamed)
                .filter(llama -> !llama.inCaravan() && !llama.isLeashed())
                .findFirst()).ifPresent(llama -> sensorOwner.getBrain().setMemory(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get(), llama));
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(CaravanMod.NEARBY_UNCLAIMED_LLAMA.get(), MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }
}
