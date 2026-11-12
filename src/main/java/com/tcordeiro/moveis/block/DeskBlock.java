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
 * Escrivaninha: 3 gavetas, 9 espaços (a de cima desliza). Liga-se à vizinha: com outra escrivaninha virada para o mesmo lado à sua
 * esquerda (o lado da perna), a perna some e os tampos fazem um só (JOINED).
 */
public class DeskBlock extends StorageFurnitureBlock {
	public static final BooleanProperty JOINED = BooleanProperty.create("joined");

	public DeskBlock(Properties props) {
		super(Kind.DESK, props);
		registerDefaultState(defaultBlockState().setValue(JOINED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(JOINED);
	}

	/** O lado da perna (x baixo no modelo virado a norte = oeste = à esquerda de FACING). */
	public static Direction legSide(BlockState state) {
		return state.getValue(FACING).getCounterClockWise();
	}

	public static boolean joined(BlockState state, BlockGetter level, BlockPos pos) {
		BlockState n = level.getBlockState(pos.relative(legSide(state)));
		return n.getBlock() instanceof DeskBlock && n.getValue(FACING) == state.getValue(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState st = super.getStateForPlacement(ctx);
		return st.setValue(JOINED, joined(st, ctx.getLevel(), ctx.getClickedPos()));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		return dir.getAxis().isHorizontal() ? state.setValue(JOINED, joined(state, level, pos)) : state;
	}
}
