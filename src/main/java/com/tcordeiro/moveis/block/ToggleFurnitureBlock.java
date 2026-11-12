package com.tcordeiro.moveis.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tcordeiro.moveis.block.entity.AnimatedBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * Abre e fecha com um clique, com animação (cortinas a correr, persianas a rodar). Parada, tem modelo estático aberto e fechado; a
 * mexer, o renderer desenha as partes. As cortinas ({@code spreads}) abrem e fecham juntas com as vizinhas viradas para o mesmo lado
 * (na mesma fila e por cima/por baixo), até {@link #MAX_SPREAD}.
 */
public class ToggleFurnitureBlock extends BaseEntityBlock implements AnimatedFurniture {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final int MAX_SPREAD = 64;

	private final String kind;
	private final boolean spreads;
	private final SoundEvent openSound, closeSound;
	private final Map<Direction, VoxelShape> shapes;

	public ToggleFurnitureBlock(String kind, boolean spreads, SoundEvent openSound, SoundEvent closeSound, VoxelShape northShape, Properties props) {
		super(props);
		this.kind = kind;
		this.spreads = spreads;
		this.openSound = openSound;
		this.closeSound = closeSound;
		this.shapes = Shapes.rotateHorizontal(northShape);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(MOVING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, MOVING);
	}

	@Override
	public String animKind(BlockState state) {
		return kind;
	}

	@Override
	public boolean hasStaticOpenModel() {
		return true;
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
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(OPEN) ? Shapes.empty() : getShape(state, level, pos, ctx);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		toggle(level, pos, state, !state.getValue(OPEN));
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Abre ou fecha esta (e, nas cortinas, as vizinhas ligadas). Devolve quantas mudaram. */
	public int toggle(Level level, BlockPos pos, BlockState state, boolean open) {
		Set<BlockPos> done = new HashSet<>();
		ArrayDeque<BlockPos> todo = new ArrayDeque<>();
		todo.add(pos);
		Direction f = state.getValue(FACING);
		Direction[] around = {f.getClockWise(), f.getCounterClockWise(), Direction.UP, Direction.DOWN};
		int changed = 0;
		while (!todo.isEmpty() && changed < MAX_SPREAD) {
			BlockPos p = todo.poll();
			if (!done.add(p)) continue;
			BlockState s = level.getBlockState(p);
			// cortinas de qualquer cor juntam-se; o resto (persianas) só muda a que se clicou
			boolean same = s.is(this) || (spreads && s.getBlock() instanceof ToggleFurnitureBlock t && t.spreads);
			if (!same || s.getValue(FACING) != f) continue;
			AnimatedFurniture.setTarget(level, p, s, OPEN, open);
			changed++;
			if (!spreads) break;
			for (Direction d : around) todo.add(p.relative(d));
		}
		level.playSound(null, pos, open ? openSound : closeSound, SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.1f + 0.9f);
		return changed;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AnimatedBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, MoveisBlocks.ANIM_BE, AnimatedBlockEntity::tick);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return state.getValue(OPEN);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
