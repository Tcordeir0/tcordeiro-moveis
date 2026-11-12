package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.anim.Local;
import com.tcordeiro.moveis.block.entity.WineRackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Adega: 9 casas (3×3), cada uma com uma garrafa ou poção (só essas; também por funil). Clique numa casa com o frasco põe-no lá;
 * com a mão vazia tira o dessa casa. As garrafas aparecem nas casas (renderer).
 */
public class WineRackBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public WineRackBlock(Properties props) {
		super(props);
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

	/** Quarto de volta do bloco (0 norte, 1 este, 2 sul, 3 oeste), para {@link Local}. */
	public static int quarter(Direction f) {
		return switch (f) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	/** A casa onde se clicou: 0 = em cima à esquerda (vista de frente), por linhas; as divisórias estão em 5,55 e 10,45 px. */
	public static int cellAt(BlockState state, BlockPos pos, Vec3 hit) {
		int q = quarter(state.getValue(FACING));
		double x = Local.modelX(q, hit.x - pos.getX(), hit.z - pos.getZ()) * 16, y = (hit.y - pos.getY()) * 16;
		int col = x >= 10.45 ? 0 : x >= 5.55 ? 1 : 2;
		int row = y >= 10.45 ? 0 : y >= 5.55 ? 1 : 2;
		return row * 3 + col;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!WineRackBlockEntity.isBottle(stack) || !(level.getBlockEntity(pos) instanceof WineRackBlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
		int cell = cellAt(state, pos, hit.getLocation());
		if (!be.getItem(cell).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		be.setItem(cell, stack.copyWithCount(1));
		stack.consume(1, player);
		level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof WineRackBlockEntity be)) return InteractionResult.PASS;
		int cell = cellAt(state, pos, hit.getLocation());
		if (be.getItem(cell).isEmpty()) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		ItemStack it = be.removeItemNoUpdate(cell);
		be.setChanged();
		if (!player.getInventory().add(it)) player.drop(it, false, net.minecraft.util.Prediction.SERVER_ONLY);
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WineRackBlockEntity(pos, state);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction dir) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
