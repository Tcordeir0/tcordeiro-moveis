package com.tcordeiro.moveis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Luminária de mesa: fica em cima de um bloco (mesa, armário, qualquer bloco com o centro do topo firme). */
public class TableLampBlock extends LampBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 7, 11), Block.box(4, 7, 4, 12, 13, 12));

	public TableLampBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState st = placed(ctx, defaultBlockState());
		return st.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? st : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
