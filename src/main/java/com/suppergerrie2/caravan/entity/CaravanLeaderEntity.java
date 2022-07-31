package com.suppergerrie2.caravan.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.ai.CaravanLeaderAI;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public class CaravanLeaderEntity extends AbstractVillager {


    private static final byte ACQUIRED_CARAVAN_EVENT = 96;
    private static final byte LOST_CARAVAN_EVENT = 97;
    private static final String CARAVAN_HEAD_NBT_KEY = "CaravanHeadLlama";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<Optional<UUID>> DATA_CARAVAN_HEAD_LLAMA = SynchedEntityData.defineId(CaravanLeaderEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // TODO: Find out which ones are actually needed since this is a copy from the villager
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            CaravanMod.NEARBY_UNCLAIMED_LLAMA.get(),
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.INTERACTION_TARGET,
            MemoryModuleType.BREED_TARGET,
            MemoryModuleType.PATH,
            MemoryModuleType.DOORS_TO_CLOSE,
            MemoryModuleType.NEAREST_BED,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.NEAREST_HOSTILE,
            MemoryModuleType.HIDING_PLACE,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.LAST_SLEPT,
            MemoryModuleType.LAST_WOKEN);
    private static final ImmutableList<SensorType<? extends Sensor<? super CaravanLeaderEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.NEAREST_PLAYERS,
            SensorType.NEAREST_ITEMS,
            SensorType.NEAREST_BED,
            SensorType.HURT_BY,
            SensorType.VILLAGER_HOSTILES,
            CaravanMod.FIND_UNCLAIMED_LLAMA.get(),
            CaravanMod.SOURCE_SENSOR.get(),
            CaravanMod.DEST_SENSOR.get()
    );

    public CaravanLeaderEntity(EntityType<CaravanLeaderEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected @NotNull Brain<CaravanLeaderEntity> makeBrain(@NotNull Dynamic<?> dynamic) {
        return CaravanLeaderAI.makeBrain(this, this.brainProvider().makeBrain(dynamic));
    }

    @Override
    protected @NotNull Brain.Provider<CaravanLeaderEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_CARAVAN_HEAD_LLAMA, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        getEntityData().get(DATA_CARAVAN_HEAD_LLAMA).ifPresent(uuid -> compound.putUUID(CARAVAN_HEAD_NBT_KEY, uuid));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.hasUUID(CARAVAN_HEAD_NBT_KEY)) {
            getEntityData().set(DATA_CARAVAN_HEAD_LLAMA, Optional.of(compound.getUUID(CARAVAN_HEAD_NBT_KEY)));
        }
    }

    @Override
    protected void customServerAiStep() {
        this.getBrain().tick((ServerLevel) level, this);

        // Remove the caravan head if it is no longer in the world
        getCaravanHead().ifPresent(uuid -> {
            if (((ServerLevel) level).getEntity(uuid) == null) {
                removeCaravanHead();
            }
        });

        getCaravanHead().map(((ServerLevel) level)::getEntity).map(e -> (Llama) e).ifPresent(llama -> {
            // Only check after 100 ticks since the leash isn't loaded immediately on world load :)
            if (tickCount > 100 && (llama.isRemoved() || !llama.isLeashed())) {
                removeCaravanHead();
            }
        });

        super.customServerAiStep();
    }

    @Override
    public @NotNull Brain<CaravanLeaderEntity> getBrain() {
        //noinspection unchecked
        return (Brain<CaravanLeaderEntity>) super.getBrain();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        // Llamas are life, no time for offspring
        return null;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case ACQUIRED_CARAVAN_EVENT -> addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
            case LOST_CARAVAN_EVENT -> addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER);
            default -> super.handleEntityEvent(id);
        }
    }

    public Optional<UUID> getCaravanHead() {
        return getEntityData().get(CaravanLeaderEntity.DATA_CARAVAN_HEAD_LLAMA);
    }

    public void setCaravanHead(Llama head) {
        getEntityData().set(CaravanLeaderEntity.DATA_CARAVAN_HEAD_LLAMA, Optional.of(head.getUUID()));
        level.broadcastEntityEvent(this, ACQUIRED_CARAVAN_EVENT);
        this.playSound(SoundEvents.VILLAGER_YES);
    }

    public void removeCaravanHead() {
        getEntityData().set(CaravanLeaderEntity.DATA_CARAVAN_HEAD_LLAMA, Optional.empty());
        level.broadcastEntityEvent(this, LOST_CARAVAN_EVENT);
        this.playSound(SoundEvents.VILLAGER_NO);
    }

    protected void addParticlesAroundSelf(@NotNull ParticleOptions particleOption) {
        for (int i = 0; i < 5; i++) {
            double xSpeed = this.random.nextGaussian() * 0.02D;
            double ySpeed = this.random.nextGaussian() * 0.02D;
            double zSpeed = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(particleOption, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), xSpeed, ySpeed, zSpeed);
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {

        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }

            if (!this.getOffers().isEmpty() && !this.level.isClientSide) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            } else {
                playSound(SoundEvents.VILLAGER_NO);
            }

            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    /**
     * Create an ItemStack of the given item with the UUID of this entity in the BlockEntityTag.CaravanLeadId tag.
     *
     * @param item The item type
     * @param count Count of the item stack
     * @return The item stack
     */
    ItemStack createStackWithOwnerId(ItemLike item, int count) {
        ItemStack stack = new ItemStack(item, count);

        CompoundTag blockEntityTag = stack.getOrCreateTagElement("BlockEntityTag");
        blockEntityTag.putUUID("CaravanLeaderId", this.getUUID());

        return stack;
    }

    @Override
    protected void updateTrades() {
        if(offers == null) {
            this.offers = new MerchantOffers();
        }

        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, random.nextInt(5, 13)), createStackWithOwnerId(CaravanMod.CARAVAN_SOURCE_BLOCK.get(), 1), 1, 0, 0));
        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, random.nextInt(5, 13)), createStackWithOwnerId(CaravanMod.CARAVAN_DEST_BLOCK.get(), 1), 1, 0, 0));
    }

    @Override
    protected void rewardTradeXp(@NotNull MerchantOffer offer) {
        if (!isClientSide() && offer.shouldRewardExp()) {
            this.level.addFreshEntity(new ExperienceOrb(this.level, this.getX(), this.getY() + 0.5D, this.getZ(), random.nextInt(1, 3)));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Don't despawn
        return false;
    }
}
