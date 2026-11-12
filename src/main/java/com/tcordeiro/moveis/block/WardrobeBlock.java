package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.block.entity.AnimatedBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Guarda-roupa: 2 blocos de altura, 36 espaços (o ecrã de 4 linhas). O inventário está na metade de baixo; a de cima só desenha a sua
 * metade das portas (as mesmas dobradiças). Partir uma metade parte as duas; só a de baixo dá o item.
 */
public class WardrobeBlock extends StorageFurnitureBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	public WardrobeBlock(Properties props) {
		super(Kind.WARDROBE, props);
		registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HALF);
	}

	@Override
	public String animKind(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? "wardrobe_bottom" : "wardrobe_top";
	}

	static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockPos pos = ctx.getClickedPos();
		Level level = ctx.getLevel();
		if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(ctx)) return null;
		return super.getStateForPlacement(ctx).setValue(HALF, DoubleBlockHalf.LOWER);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		DoubleBlockHalf half = state.getValue(HALF);
		if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
			boolean ok = nstate.is(this) && nstate.getValue(HALF) != half && nstate.getValue(FACING) == state.getValue(FACING);
			if (!ok) return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	/** Criativo: partir a metade de cima não deixa a de baixo dar o item (como as portas). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos low = pos.below();
			BlockState ls = level.getBlockState(low);
			if (ls.is(this)) {
				level.setBlock(low, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, low, Block.getId(ls));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(lower(pos, state)) instanceof StorageFurnitureBlockEntity be) player.openMenu(be);
		return InteractionResult.SUCCESS;
	}

	/** As duas metades abrem e fecham juntas. */
	@Override
	public void onOpenChanged(Level level, BlockPos pos, BlockState state, boolean open) {
		super.onOpenChanged(level, pos, state, open);
		BlockPos up = pos.above();
		BlockState us = level.getBlockState(up);
		if (us.is(this)) AnimatedFurniture.setTarget(level, up, us, OPEN, open);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new StorageFurnitureBlockEntity(pos, state) : new AnimatedBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.getTicker(level, state, type)
				: createTickerHelper(type, MoveisBlocks.ANIM_BE, AnimatedBlockEntity::tick);
	}

	@Override
	protected long getSeed(BlockState state, BlockPos pos) {
		return net.minecraft.util.Mth.getSeed(pos.getX(), lower(pos, state).getY(), pos.getZ());
	}
}
