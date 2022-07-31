package com.suppergerrie2.caravan.blocks;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Function4;
import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CaravanTargetBlockEntity extends BlockEntity {


     UUID caravanLeaderId;

    private final BlockPos.MutableBlockPos lastPos = new BlockPos.MutableBlockPos();
    private final int range = 4;
    private final HashSet<LazyOptional<IItemHandler>> handlersInRange = new HashSet<>();
    private TargetType targetType;

    public CaravanTargetBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, TargetType.UNKNOWN);
    }

    public CaravanTargetBlockEntity(BlockPos pos, BlockState blockState, TargetType targetType) {
        super(CaravanMod.CARAVAN_TARGET_BLOCK_ENTITY.get(), pos, blockState);
        caravanLeaderId = new UUID(0, 0);
        this.targetType = targetType;

        // Initialize as far away as possible, then call incrementSearchPos to find the initial position
        lastPos.setWithOffset(pos, -range, 0, -range);
        incrementSearchPos(pos);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof CaravanTargetBlockEntity targetBlockEntity)
            targetBlockEntity.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Every 10 ticks a new block is scanned for a handler
        if (level.getGameTime() % 10 == 0) {
            ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, lastPos.getX() + 0.5, lastPos.getY() + 0.5, lastPos.getZ() + 0.5, 1, 0, 0, 0, 0);
            BlockEntity blockEntity = level.getBlockEntity(lastPos);
            if (blockEntity != null)
                saveHandler(blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY));

            incrementSearchPos(pos);
        }

        if (level.getGameTime() % 20 == 0) {
            updateState(level, pos, state);
        }
    }

    public void updateState(Level level, BlockPos pos, BlockState state) {
        // Redstone can force the block to be enabled, allowing more complex logic to be used for loading the caravan
        boolean enabled = level.hasNeighborSignal(pos) || targetType.shouldBeEnabled(level, pos, state, this);

        if(state.getValue(CaravanTargetBlock.ENABLED) != enabled) {
            level.setBlock(pos, state.setValue(CaravanTargetBlock.ENABLED, enabled), 3);
        }
    }

    private static boolean handlerHasSpace(LazyOptional<IItemHandler> itemHandler) {
        return itemHandler.map(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getStackInSlot(i).isEmpty()) {
                    return true;
                }
            }

            return false;
        }).orElse(false);
    }

    private static boolean handlerHasItems(LazyOptional<IItemHandler> itemHandler) {
        return itemHandler.map(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return true;
                }
            }

            return false;
        }).orElse(false);
    }

    void saveHandler(LazyOptional<IItemHandler> itemHandler) {
        if (!itemHandler.isPresent()) return;

        handlersInRange.add(itemHandler);
        itemHandler.addListener(handlersInRange::remove);
    }

    void incrementSearchPos(BlockPos blockEntityPos) {
        BlockPos maxPos = blockEntityPos.offset(range, 0, range);
        BlockPos minPos = blockEntityPos.offset(-range, 0, -range);

        do {
            if (lastPos.getX() < maxPos.getX()) {
                lastPos.move(1, 0, 0);
            } else if (lastPos.getZ() < maxPos.getZ()) {
                lastPos.move(-range * 2, 0, 1);
            } else {
                lastPos.set(minPos);
            }
        } while (lastPos.distSqr(blockEntityPos) > range * range);
    }

    public Set<LazyOptional<IItemHandler>> getInventories() {
        return ImmutableSet.copyOf(handlersInRange);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putUUID("CaravanLeaderId", caravanLeaderId);
        tag.putString("TargetType", targetType.name());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        caravanLeaderId = tag.getUUID("CaravanLeaderId");

        try {
            targetType = TargetType.valueOf(tag.getString("TargetType"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            targetType = TargetType.UNKNOWN;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putUUID("CaravanLeaderId", caravanLeaderId);
        tag.putString("TargetType", targetType.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        caravanLeaderId = tag.getUUID("CaravanLeaderId");

        try {
            targetType = TargetType.valueOf(tag.getString("TargetType"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            targetType = TargetType.UNKNOWN;
        }
    }

    enum TargetType {
        UNKNOWN((level, pos, state, blockEntity) -> false),
        // Source requires items in the inventories to turn on
        SOURCE((level, pos, state, blockEntity) -> blockEntity.handlersInRange.stream().anyMatch(CaravanTargetBlockEntity::handlerHasItems)),
        // Dest requires the inventories to have space for items to be put in
        DEST((level, pos, state, blockEntity) -> blockEntity.handlersInRange.stream().anyMatch(CaravanTargetBlockEntity::handlerHasSpace));

        private final Function4<Level, BlockPos, BlockState, CaravanTargetBlockEntity, Boolean> shouldBeEnabled;

        TargetType(Function4<Level, BlockPos, BlockState, CaravanTargetBlockEntity, Boolean> shouldBeEnabled) {
            this.shouldBeEnabled = shouldBeEnabled;
        }

        public boolean shouldBeEnabled(Level level, BlockPos pos, BlockState state, CaravanTargetBlockEntity blockEntity) {
            return shouldBeEnabled.apply(level, pos, state, blockEntity);
        }
    }
}
