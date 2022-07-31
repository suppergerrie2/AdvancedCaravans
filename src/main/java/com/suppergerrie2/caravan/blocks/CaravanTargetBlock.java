package com.suppergerrie2.caravan.blocks;

import com.mojang.logging.LogUtils;
import com.suppergerrie2.caravan.CaravanBlockAssignmentManager;
import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.UUID;

public class CaravanTargetBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Amount of "layers" in the diagonal parts of the block
    public static final int SHAPE_RESOLUTION = 6;

    // Cache for the shapes as computing them is quite expensive
    private final HashMap<BlockState, VoxelShape> cachedShapes = new HashMap<>();

    // Callbacks for managing the target blocks. These are called when the block is placed or removed or when it requests a block.
    private final CaravanBlockAssignmentManager.CaravanBlockAddTarget onPlaced;
    private final CaravanBlockAssignmentManager.CaravanBlockRemoveTarget onRemoved;
    private final CaravanBlockAssignmentManager.CaravanBlockGetTarget onGet;

    private static final Logger LOGGER = LogUtils.getLogger();

    public CaravanTargetBlock(CaravanBlockAssignmentManager.CaravanBlockAddTarget onPlaced, CaravanBlockAssignmentManager.CaravanBlockRemoveTarget onRemoved, CaravanBlockAssignmentManager.CaravanBlockGetTarget onGet) {
        super(Properties.of(Material.WOOD));
        this.onPlaced = onPlaced;
        this.onRemoved = onRemoved;
        this.onGet = onGet;

        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(ENABLED, false).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, HALF);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        // Only the bottom half needs a block entity, the top half is purely for collision purposes
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return null;
        }

        return new CaravanTargetBlockEntity(pos, state, this == CaravanMod.CARAVAN_SOURCE_BLOCK.get() ? CaravanTargetBlockEntity.TargetType.SOURCE : CaravanTargetBlockEntity.TargetType.DEST);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Top half has to be manually set
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);

        // Lower half has to register the target to the target manager
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {

            // TODO: Is it needed to get the data from the stack of can it be accessed from the block entity?
            CompoundTag entityTag = stack.getOrCreateTagElement("BlockEntityTag");

            if (!entityTag.contains("CaravanLeaderId")) {
                level.destroyBlock(pos, true);
                return;
            }

            UUID uuid = entityTag.getUUID("CaravanLeaderId");
            if (onGet.getTarget(((ServerLevel) level), uuid).isPresent()) {
                level.destroyBlock(pos, true);
                return;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof CaravanTargetBlockEntity caravanTargetBlockEntity) {
                onPlaced.addTarget((ServerLevel) level, GlobalPos.of(level.dimension(), pos), caravanTargetBlockEntity.caravanLeaderId);
            } else {
                LOGGER.warn("CaravanDestBlockEntity not found at {}, was {}", pos, blockEntity);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        // Check if the bottom half changed state and react accordingly
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && pos.below().equals(fromPos)) {
            BlockState bottom = level.getBlockState(fromPos);
            if (bottom.is(this) && state.getValue(ENABLED) != bottom.getValue(ENABLED)) {
                level.setBlock(pos, state.setValue(ENABLED, bottom.getValue(ENABLED)), 3);
            }
        }

        // Redstone may give an update in which case the BE needs to be updated
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof CaravanTargetBlockEntity caravanDestBlockEntity) {
                caravanDestBlockEntity.updateState(level, pos, state);
            } else {
                LOGGER.warn("CaravanDestBlockEntity not found at {}, was {}", pos, blockEntity);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        // Remove the target from the target manager
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof CaravanTargetBlockEntity caravanDestBlockEntity) {
                onRemoved.removeTarget(((ServerLevel) level), GlobalPos.of(level.dimension(), pos), caravanDestBlockEntity.caravanLeaderId);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
        // Original code from DoorBlock::updateShape, modified to work for the TargetBlock, makes sure the both halves get removed when one half is destroyed
        DoubleBlockHalf doubleblockhalf = state.getValue(HALF);
        if ((direction.getAxis() == Direction.Axis.Y) &&
                (doubleblockhalf == DoubleBlockHalf.LOWER == (direction == Direction.UP))) {
            return neighborState.is(this) && neighborState.getValue(HALF) != doubleblockhalf ?
                    super.updateShape(state, direction, neighborState, level, currentPos, neighborPos) :
                    Blocks.AIR.defaultBlockState();
        } else {
            return doubleblockhalf == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, currentPos) ?
                    Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }
    }

    @Override
    public void playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        // Destroy the bottom half without dropping the item when in creative mode to prevent drops in creative mode
        if (!level.isClientSide && player.isCreative()) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                BlockState lowerState = level.getBlockState(pos.below());
                if (lowerState.is(this) && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, pos, Block.getId(lowerState));
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack result = new ItemStack(this);

        // Data is in the bottom block
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) pos = pos.below();

        if (level.getBlockEntity(pos) instanceof CaravanTargetBlockEntity caravanTargetBlockEntity) {
            result.getOrCreateTagElement("BlockEntityTag").putUUID("CaravanLeaderId", caravanTargetBlockEntity.caravanLeaderId);
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return cachedShapes.computeIfAbsent(state, s -> {
            VoxelShape newShape = Shapes.box(7 / 16.0, 0, 7 / 16.0, 9 / 16.0, 1, 9 / 16.0);

            if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                double angle = Math.toRadians(67);
                double slope = 1 / Math.tan(angle);

                double stepSize = (14 / 16.0) / SHAPE_RESOLUTION;

                for (int i = 0; i < SHAPE_RESOLUTION; i++) {
                    double startX = (i * stepSize + 2 / 16.0) * slope;
                    double endX = ((i + 1) * stepSize + 2 / 16.0) * slope + 2 / 16.0;

                    newShape = Shapes.or(newShape,
                            Shapes.box(startX, i * stepSize, startX,
                                    endX, i * stepSize + stepSize, endX));
                    newShape = Shapes.or(newShape,
                            Shapes.box(1.0 - endX, i * stepSize, startX,
                                    1.0 - startX, i * stepSize + stepSize, endX));
                    newShape = Shapes.or(newShape,
                            Shapes.box(1.0 - endX, i * stepSize, 1.0 - endX,
                                    1.0 - startX, i * stepSize + stepSize, 1.0 - startX));
                    newShape = Shapes.or(newShape,
                            Shapes.box(startX, i * stepSize, 1.0 - endX,
                                    endX, i * stepSize + stepSize, 1.0 - startX));

                }
            }

            return newShape;
        });
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return blockEntityType == CaravanMod.CARAVAN_TARGET_BLOCK_ENTITY.get() ? CaravanTargetBlockEntity::tick : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos blockpos = pos.below();
        BlockState blockstate = level.getBlockState(blockpos);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? blockstate.isFaceSturdy(level, blockpos, Direction.UP) : blockstate.is(this);
    }
}
