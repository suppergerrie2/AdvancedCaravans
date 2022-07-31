package com.suppergerrie2.caravan.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import com.suppergerrie2.caravan.entity.ai.behaviours.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

// TODO: Need more AI outside of the caravan activity
public class CaravanLeaderAI {

    public static Brain<CaravanLeaderEntity> makeBrain(CaravanLeaderEntity entity, Brain<CaravanLeaderEntity> brain) {
        initCoreActivity(brain, entity);
//        initIdleActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(CaravanMod.LEAD_CARAVAN.get());
        brain.useDefaultActivity();
        //        brain.updateActivityFromSchedule(entity.level.getDayTime(), entity.level.getGameTime());
        return brain;
    }

    private static void initCoreActivity(Brain<CaravanLeaderEntity> brain, CaravanLeaderEntity entity) {
//        VillagerProfession villagerprofession = entity.getVillagerData().getProfession();
//        if (entity.isBaby()) {
//            brain.setSchedule(Schedule.VILLAGER_BABY);
//            brain.addActivity(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F));
//        } else {
//            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
//            brain.addActivityWithConditions(Activity.WORK, VillagerGoalPackages.getWorkPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
//        }

        brain.setSchedule(CaravanMod.CARAVAN_SCHEDULE.get());

        brain.addActivity(Activity.CORE, getCorePackage(0.5F));
//        brain.addActivityWithConditions(Activity.MEET, VillagerGoalPackages.getMeetPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
//        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(villagerprofession, 0.5F));
        brain.addActivity(Activity.IDLE, getIdleTasks(entity, 0.5f));
        brain.addActivity(CaravanMod.LEAD_CARAVAN.get(), getLeadTasks(entity, 0.5f));
//        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(villagerprofession, 0.5F));


//        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new AnimalPanic(2.0F), new LookAtTargetSink(45, 90), new MoveToTargetSink(), new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)));
    }

    static ImmutableList<Pair<Integer, ? extends Behavior<? super CaravanLeaderEntity>>> getIdleTasks(CaravanLeaderEntity entity, float speedModifier) {
        return ImmutableList.of(
                Pair.of(2, new RunOne<>(
                        ImmutableList.of(
                                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 2),
                                Pair.of(new InteractWith<>(EntityType.VILLAGER, 8, AgeableMob::canBreed, AgeableMob::canBreed, MemoryModuleType.BREED_TARGET, speedModifier, 2), 1),
                                Pair.of(InteractWith.of(EntityType.LLAMA, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 1),
                                Pair.of(new VillageBoundRandomStroll(speedModifier), 1),
                                Pair.of(new SetWalkTargetFromLookTarget(speedModifier, 2), 1),
                                Pair.of(new JumpOnBed(speedModifier), 1),
                                Pair.of(new DoNothing(30, 60), 1)))),
//                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, new SetLookAndInteract(EntityType.PLAYER, 4)),
//                Pair.of(3, new ShowTradesToPlayer(400, 1600)),
//                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(
//                        Pair.of(new TradeWithVillager(), 1)))),
//                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.BREED_TARGET), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(
//                        Pair.of(new VillagerMakeLove(), 1)))),
                getFullLookBehavior(),
                Pair.of(99, new UpdateActivityFromSchedule()));
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

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super CaravanLeaderEntity>>> getCorePackage(float pSpeedModifier) {
        return ImmutableList.of(
                Pair.of(0, new Swim(0.8F)),
                Pair.of(0, new InteractWithDoor()),
                Pair.of(0, new LookAtTargetSink(45, 90)),
//                Pair.of(0, new VillagerPanicTrigger()), TODO: This only works for villagers, not for caravans
                Pair.of(0, new WakeUp()),
                Pair.of(0, new ReactToBell()),
                Pair.of(0, new SetRaidStatus()),
//                Pair.of(0, new ValidateNearbyPoi(pProfession.heldJobSite(), MemoryModuleType.JOB_SITE)),
//                Pair.of(0, new ValidateNearbyPoi(pProfession.acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(1, new MoveToTargetSink()),
//                Pair.of(2, new PoiCompetitorScan(pProfession)),
//                Pair.of(3, new LookAndFollowTradingPlayerSink(pSpeedModifier)),
                Pair.of(5, new GoToWantedItem<>(pSpeedModifier, false, 4))
//                Pair.of(6, new AcquirePoi(pProfession.acquirableJobSite(), MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty())),
//                Pair.of(7, new GoToPotentialJobSite(pSpeedModifier)),
//                Pair.of(8, new YieldJobSite(pSpeedModifier)),
//                Pair.of(10, new AcquirePoi((p_217499_) -> p_217499_.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte) 14))),
//                Pair.of(10, new AcquirePoi((p_217497_) -> p_217497_.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte) 14)))
//                Pair.of(10, new AssignProfessionFromJobSite()),
//                Pair.of(10, new ResetProfession())
        );
    }

    static ImmutableList<Pair<Integer, ? extends Behavior<? super CaravanLeaderEntity>>> getLeadTasks(CaravanLeaderEntity entity, float speedModifier) {
        return ImmutableList.of(
                Pair.of(1, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(
                        Pair.of(new AcquireCaravan(), 9),
                        Pair.of(new GoToPotentialCaravan(speedModifier), 10),
                        Pair.of(new LoadCaravan(), 11),
                        Pair.of(new UnloadCaravan(), 12),
                        Pair.of(new GoToTarget(0.6f, CaravanMod.CARAVAN_SOURCE_TARGET.get(), true), 21),
                        Pair.of(new GoToTarget(0.6f, CaravanMod.CARAVAN_DEST_TARGET.get(), false), 22)
                ))),
                Pair.of(99, new UpdateActivityFromSchedule())
        );
    }

    public static void updateActivity(Villager entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
    }
}
