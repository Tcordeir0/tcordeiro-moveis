package com.tcordeiro.moveis.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mesa: liga-se às mesas vizinhas (de qualquer madeira) nos 4 lados. O tampo é sempre inteiro; uma perna só aparece num canto
 * cujos dois lados não têm mesa (mesa sozinha: 4 pernas; fila: pernas só nas pontas; L: o canto de dentro fica sem perna).
 */
public class TableBlock extends Block {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST, SOUTH = BlockStateProperties.SOUTH,
			WEST = BlockStateProperties.WEST;
	public static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH,
			Direction.WEST, WEST);

	private static final VoxelShape TOP = Block.box(0, 14, 0, 16, 16, 16);
	private static final VoxelShape LEG_NW = Block.box(1, 0, 1, 3, 14, 3), LEG_NE = Block.box(13, 0, 1, 15, 14, 3),
			LEG_SE = Block.box(13, 0, 13, 15, 14, 15), LEG_SW = Block.box(1, 0, 13, 3, 14, 15);
	private static final VoxelShape[] SHAPES = new VoxelShape[16];

	static {
		for (int m = 0; m < 16; m++) {
			boolean n = (m & 1) != 0, e = (m & 2) != 0, s = (m & 4) != 0, w = (m & 8) != 0;
			VoxelShape sh = TOP;
			if (!n && !w) sh = Shapes.or(sh, LEG_NW);
			if (!n && !e) sh = Shapes.or(sh, LEG_NE);
			if (!s && !e) sh = Shapes.or(sh, LEG_SE);
			if (!s && !w) sh = Shapes.or(sh, LEG_SW);
			SHAPES[m] = sh;
		}
	}

	public TableBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, com.tcordeiro.moveis.finish.FinishState.FINISH);
	}

	/** Tem perna neste canto? (os dois lados do canto sem mesa) */
	public static boolean hasLeg(BlockState state, Direction a, Direction b) {
		return !state.getValue(SIDES.get(a)) && !state.getValue(SIDES.get(b));
	}

	public static int legCount(BlockState state) {
		int n = 0;
		for (Direction d : Direction.Plane.HORIZONTAL) if (hasLeg(state, d, d.getClockWise())) n++;
		return n;
	}

	private BlockState connect(LevelReader level, BlockPos pos, BlockState state) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			state = state.setValue(SIDES.get(d), level.getBlockState(pos.relative(d)).getBlock() instanceof TableBlock);
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return connect(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		if (!dir.getAxis().isHorizontal()) return state;
		return state.setValue(SIDES.get(dir), nstate.getBlock() instanceof TableBlock);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		int m = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0) | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
		return SHAPES[m];
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rot) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(SIDES.get(rot.rotate(d)), state.getValue(SIDES.get(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(SIDES.get(mirror.mirror(d)), state.getValue(SIDES.get(d)));
		return out;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	// verniz e desgaste (0.3.0): ver com.tcordeiro.moveis.finish
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return com.tcordeiro.moveis.finish.Weathering.ticks(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		com.tcordeiro.moveis.finish.Weathering.randomTick(state, level, pos, random);
	}
}
