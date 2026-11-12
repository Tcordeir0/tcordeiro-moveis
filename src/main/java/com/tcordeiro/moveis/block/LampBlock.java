package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Lâmpada elétrica (lustre, arandela, luminária de mesa). Acende com redstone (sinal em qualquer lado, como a Lâmpada de Redstone);
 * com o Industrial instalado também acende com energia dos cabos/Fio de Cobre ou de um painel/bateria encostado (gasta
 * {@link LampBlockEntity#COST} J/t só enquanto não há redstone). Sem o Industrial a entidade fica parada (sem tick).
 */
public abstract class LampBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	protected LampBlock(Properties props) {
		super(props);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	/** O estado a pôr (com LIT já pela redstone); as subclasses acrescentam a orientação. */
	protected BlockState placed(BlockPlaceContext ctx, BlockState st) {
		return st.setValue(LIT, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation o, boolean moved) {
		if (!level.isClientSide()) refresh(level, pos, state);
	}

	/** Acesa = redstone, ou energia (Industrial) no último tick. */
	public static void refresh(Level level, BlockPos pos, BlockState state) {
		boolean energy = level.getBlockEntity(pos) instanceof LampBlockEntity be && be.energized();
		boolean lit = level.hasNeighborSignal(pos) || energy;
		if (lit != state.getValue(LIT)) {
			level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
			level.playSound(null, pos, lit ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 0.4f, 1.3f);
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return LampBlockEntity.create(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !TMoveis.INDUSTRIAL) return null;
		return createTickerHelper(type, MoveisBlocks.LAMP_BE, LampBlockEntity::serverTick);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {}
}
