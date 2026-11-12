package com.tcordeiro.moveis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Bancada da cozinha: tampo de pedra e armário de 9 espaços. Liga-se às vizinhas: em L (uma bancada à frente, virada de lado, como o
 * canto de dentro das escadas e dos sofás) passa a ser o canto, com uma porta só.
 */
public class CounterBlock extends StorageFurnitureBlock {
	public static final BooleanProperty CORNER = BooleanProperty.create("corner");

	public CounterBlock(Properties props) {
		super(Kind.COUNTER, props);
		registerDefaultState(defaultBlockState().setValue(CORNER, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CORNER);
	}

	@Override
	public String animKind(BlockState state) {
		return state.getValue(CORNER) ? "counter_corner" : "counter";
	}

	/** Canto: há uma bancada à frente virada para o lado (perpendicular a esta). */
	public static boolean corner(BlockState state, BlockGetter level, BlockPos pos) {
		Direction f = state.getValue(FACING);
		BlockState front = level.getBlockState(pos.relative(f));
		return front.getBlock() instanceof CounterBlock && front.getValue(FACING).getAxis() != f.getAxis();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState st = super.getStateForPlacement(ctx);
		return st.setValue(CORNER, corner(st, ctx.getLevel(), ctx.getClickedPos()));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		return dir.getAxis().isHorizontal() ? state.setValue(CORNER, corner(state, level, pos)) : state;
	}
}
