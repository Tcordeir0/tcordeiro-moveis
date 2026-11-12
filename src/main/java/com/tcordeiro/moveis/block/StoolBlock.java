package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.entity.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Banco (sem encosto, redondo para todos os lados): senta-se com clique direito, fica-se virado para onde se estava a olhar. */
public class StoolBlock extends Block implements SeatBlock {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

	public StoolBlock(Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public double seatHeight(BlockState state) {
		return 0.5;
	}

	@Override
	public float seatYaw(BlockState state, Player player) {
		return player.getYRot();
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
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
