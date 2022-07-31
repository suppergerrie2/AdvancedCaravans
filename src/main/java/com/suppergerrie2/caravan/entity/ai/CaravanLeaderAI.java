package com.suppergerrie2.caravan.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import com.suppergerrie2.caravan.entity.ai.behaviours.LookAndFollowTradingPlayerSink;
import com.suppergerrie2.caravan.entity.ai.behaviours.*;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public class CaravanLeaderAI {

    public static Brain<CaravanLeaderEntity> makeBrain(CaravanLeaderEntity entity, Brain<CaravanLeaderEntity> brain) {
        brain.setSchedule(CaravanMod.CARAVAN_SCHEDULE.get());

        initCoreActivity(brain);
        initIdleActivity(brain);
        initRestActivity(brain);
        initLeadActivity(brain);

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        brain.updateActivityFromSchedule(entity.level.getDayTime(), entity.level.getGameTime());

        return brain;
    }

    private static void initCoreActivity(Brain<CaravanLeaderEntity> brain) {

        brain.addActivity(Activity.CORE, ImmutableList.of(
                Pair.of(0, new Swim(0.8F)),
                Pair.of(0, new InteractWithDoor()),
                Pair.of(0, new LookAtTargetSink(45, 90)),
                Pair.of(0, new WakeUp()),
                Pair.of(0, new ReactToBell()),
                Pair.of(0, new SetRaidStatus()),
                Pair.of(1, new MoveToTargetSink()),
                Pair.of(3, new LookAndFollowTradingPlayerSink(0.5f)),
                Pair.of(5, new GoToWantedItem<>(0.5f, false, 4))
        ));
    }

    private static void initIdleActivity(Brain<CaravanLeaderEntity> brain) {
        float speedModifier = 0.5f;
        brain.addActivity(Activity.IDLE, ImmutableList.of(
                Pair.of(2, new RunOne<>(
                        ImmutableList.of(
                                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 2),
                                Pair.of(new InteractWith<>(EntityType.VILLAGER, 8, AgeableMob::canBreed, AgeableMob::canBreed, MemoryModuleType.BREED_TARGET, speedModifier, 2), 1),
                                Pair.of(InteractWith.of(EntityType.LLAMA, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 1),
                                Pair.of(new VillageBoundRandomStroll(speedModifier), 1),
                                Pair.of(new SetWalkTargetFromLookTarget(speedModifier, 2), 1),
                                Pair.of(new JumpOnBed(speedModifier), 1),
                                Pair.of(new DoNothing(30, 60), 1)))),
                Pair.of(3, new SetLookAndInteract(EntityType.PLAYER, 4)),
                getFullLookBehavior(),
                Pair.of(99, new UpdateActivityFromSchedule())));
    }

    private static void initRestActivity(Brain<CaravanLeaderEntity> brain) {
        float speedModifier = 0.5f;
        brain.addActivity(Activity.REST, ImmutableList.of(
                Pair.of(3, new SleepInPlace(100)),
                Pair.of(5, new RunOne<>(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT), ImmutableList.of(
                        Pair.of(new SetClosestHomeAsWalkTarget(speedModifier), 1),
                        Pair.of(new InsideBrownianWalk(speedModifier), 4),
                        Pair.of(new DoNothing(20, 40), 2)))),
                getMinimalLookBehavior(),
                Pair.of(99, new UpdateActivityFromSchedule())));
    }

    static void initLeadActivity(Brain<CaravanLeaderEntity> brain) {
        float speedModifier = 0.5f;
        brain.addActivity(CaravanMod.LEAD_CARAVAN.get(), ImmutableList.of(
                Pair.of(1, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(
                        Pair.of(new AcquireCaravan(), 9),
                        Pair.of(new GoToPotentialCaravan(speedModifier), 10),
                        Pair.of(new LoadCaravan(), 11),
                        Pair.of(new UnloadCaravan(), 12),
                        Pair.of(new GoToTarget(0.6f, CaravanMod.CARAVAN_SOURCE_TARGET.get(), true), 21),
                        Pair.of(new GoToTarget(0.6f, CaravanMod.CARAVAN_DEST_TARGET.get(), false), 22)
                ))),
                Pair.of(99, new UpdateActivityFromSchedule())
        ));
    }

    private static Pair<Integer, Behavior<LivingEntity>> getFullLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
                Pair.of(new SetEntityLookTarget(EntityType.LLAMA, 8.0F), 8),
                Pair.of(new SetEntityLookTarget(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(new SetEntityLookTarget(EntityType.PLAYER, 8.0F), 2),
                Pair.of(new SetEntityLookTarget(MobCategory.CREATURE, 8.0F), 1),
                Pair.of(new SetEntityLookTarget(MobCategory.WATER_CREATURE, 8.0F), 1),
                Pair.of(new SetEntityLookTarget(MobCategory.AXOLOTLS, 8.0F), 1),
                Pair.of(new SetEntityLookTarget(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                Pair.of(new SetEntityLookTarget(MobCategory.WATER_AMBIENT, 8.0F), 1),
                Pair.of(new SetEntityLookTarget(MobCategory.MONSTER, 8.0F), 1),
                Pair.of(new DoNothing(30, 60), 2))));
    }

    private static Pair<Integer, Behavior<LivingEntity>> getMinimalLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
                Pair.of(new SetEntityLookTarget(EntityType.LLAMA, 8.0F), 8),
                Pair.of(new SetEntityLookTarget(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(new SetEntityLookTarget(EntityType.PLAYER, 8.0F), 2),
                Pair.of(new DoNothing(30, 60), 8))));
    }
}
