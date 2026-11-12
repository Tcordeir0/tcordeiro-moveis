package com.tcordeiro.moveis.block;

import java.util.Map;

import com.tcordeiro.moveis.entity.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cadeira: vira para onde o jogador olha ao pô-la (quem se senta fica a olhar para esse lado; o encosto fica atrás). Senta-se com
 * clique direito. FACING = para onde se olha sentado.
 */
public class ChairBlock extends Block implements SeatBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Virada a norte: assento e encosto do lado sul (a forma não depende do modelo JSON). */
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
			Block.box(2, 0, 2, 14, 10, 14),
			Block.box(2, 10, 12, 14, 24, 14)));

	public ChairBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, com.tcordeiro.moveis.finish.FinishState.FINISH);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public double seatHeight(BlockState state) {
		return 0.5;
	}

	@Override
	public float seatYaw(BlockState state, Player player) {
		return state.getValue(FACING).toYRot();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return SeatBlock.sit(this, state, level, pos, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		SeatEntity.removeAt(level, pos);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
