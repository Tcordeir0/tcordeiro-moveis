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

/** Lustre: pendurado no teto (precisa de um bloco por cima, como a lanterna pendurada). */
public class ChandelierBlock extends LampBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 8, 1, 15, 14, 15), Block.box(7, 14, 7, 9, 16, 9));

	public ChandelierBlock(Properties props) {
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
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		return dir == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
