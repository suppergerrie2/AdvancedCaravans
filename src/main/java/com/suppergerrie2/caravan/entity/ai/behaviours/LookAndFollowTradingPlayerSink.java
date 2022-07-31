package com.suppergerrie2.caravan.entity.ai.behaviours;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

// Complete copy from vanilla, but vanilla's version is overly strict on the type
public class LookAndFollowTradingPlayerSink extends Behavior<AbstractVillager> {
    private final float speedModifier;

    public LookAndFollowTradingPlayerSink(float pSpeedModifier) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.speedModifier = pSpeedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, AbstractVillager owner) {
        Player player = owner.getTradingPlayer();
        return owner.isAlive() && player != null && !owner.isInWater() && !owner.hurtMarked && owner.distanceToSqr(player) <= 16.0D && player.containerMenu != null;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull AbstractVillager entity, long gameTime) {
        return this.checkExtraStartConditions(level, entity);
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull AbstractVillager entity, long gameTime) {
        this.followPlayer(entity);
    }

    @Override
    protected void stop(@NotNull ServerLevel level, AbstractVillager entity, long gameTime) {
        Brain<?> brain = entity.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull AbstractVillager owner, long gameTime) {
        this.followPlayer(owner);
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    private void followPlayer(AbstractVillager pOwner) {
        Brain<?> brain = pOwner.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(pOwner.getTradingPlayer(), false), this.speedModifier, 2));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(pOwner.getTradingPlayer(), true));
    }
}
