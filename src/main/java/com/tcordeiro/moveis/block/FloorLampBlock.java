package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Luminária de chão: 2 blocos de altura (a lâmpada é a de cima). A metade de baixo é a lâmpada a sério (entidade, energia do
 * Industrial); o sinal de redstone conta nas duas metades e as duas acendem juntas.
 */
public class FloorLampBlock extends LampBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	private static final VoxelShape LOWER = Shapes.or(Block.box(4, 0, 4, 12, 1.5, 12), Block.box(7.5, 1.5, 7.5, 8.5, 16, 8.5));
	private static final VoxelShape UPPER = Shapes.or(Block.box(7.5, 0, 7.5, 8.5, 6, 8.5), Block.box(3, 6, 3, 13, 14, 13));

	public FloorLampBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HALF);
	}

	static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	@Override
	protected int signal(Level level, BlockPos pos, BlockState state) {
		BlockPos low = lower(pos, state);
		return Math.max(level.getBestNeighborSignal(low), level.getBestNeighborSignal(low.above()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation o, boolean moved) {
		if (level.isClientSide()) return;
		BlockPos low = lower(pos, state);
		BlockState ls = level.getBlockState(low);
		if (ls.is(this)) LampBlock.refresh(level, low, ls);
	}

	/** As duas metades acendem juntas. */
	@Override
	protected BlockState powered(Level level, BlockPos pos, BlockState state, int signal, boolean energy) {
		BlockState s = super.powered(level, pos, state, signal, energy);
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER && s.getValue(LIT) != state.getValue(LIT)) {
			BlockState up = level.getBlockState(pos.above());
			if (up.is(this)) level.setBlock(pos.above(), up.setValue(LIT, s.getValue(LIT)), Block.UPDATE_CLIENTS);
		}
		return s;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockPos pos = ctx.getClickedPos();
		Level level = ctx.getLevel();
		if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(ctx)) return null;
		int sig = Math.max(level.getBestNeighborSignal(pos), level.getBestNeighborSignal(pos.above()));
		return defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER).setValue(LIT, sig > 0);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		DoubleBlockHalf half = state.getValue(HALF);
		if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)
				&& !(nstate.is(this) && nstate.getValue(HALF) != half)) return Blocks.AIR.defaultBlockState();
		if (half == DoubleBlockHalf.LOWER && dir == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
		return state;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return level.getBlockState(pos.below()).is(this);
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos low = pos.below();
			BlockState ls = level.getBlockState(low);
			if (ls.is(this)) {
				level.setBlock(low, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, low, Block.getId(ls));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LampBlockEntity.create(MoveisBlocks.LAMP_BE, pos, state) : null;
	}
}
