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

/** Assento com frente (vaso sanitário, banco de jardim): a frente vira para quem o põe; quem se senta olha para lá. */
public class SimpleSeatBlock extends Block implements SeatBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private final Map<Direction, VoxelShape> shapes;
	private final double height;

	public SimpleSeatBlock(double height, VoxelShape northShape, Properties props) {
		super(props);
		this.height = height;
		this.shapes = Shapes.rotateHorizontal(northShape);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		com.tcordeiro.moveis.finish.FinishState.addIfWood(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapes.get(state.getValue(FACING));
	}

	@Override
	public double seatHeight(BlockState state) {
		return height;
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

	// 0.3.0: a madeira crua dos móveis gasta-se à chuva (só os de madeira têm «finish»: Weathering.ticks devolve false nos outros)
	@Override
	protected boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState state) {
		return com.tcordeiro.moveis.finish.Weathering.ticks(state);
	}

	@Override
	protected void randomTick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.server.level.ServerLevel level,
			net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
		com.tcordeiro.moveis.finish.Weathering.randomTick(state, level, pos, random);
	}
}
