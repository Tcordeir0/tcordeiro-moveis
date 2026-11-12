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
		return powered(ctx.getLevel(), ctx.getClickedPos(), st, ctx.getLevel().getBestNeighborSignal(ctx.getClickedPos()), false);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation o, boolean moved) {
		if (!level.isClientSide()) refresh(level, pos, state);
	}

	/** Acesa = redstone, ou energia (Industrial) no último tick. */
	public static void refresh(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof LampBlock lamp)) return;
		boolean energy = level.getBlockEntity(pos) instanceof LampBlockEntity be && be.energized();
		BlockState now = lamp.powered(level, pos, state, lamp.signal(level, pos, state), energy);
		if (now != state) {
			level.setBlock(pos, now, Block.UPDATE_CLIENTS);
			boolean lit = now.getValue(LIT);
			if (lit != state.getValue(LIT))
				level.playSound(null, pos, lit ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 0.4f, 1.3f);
		}
	}

	/** O sinal de redstone que acende esta lâmpada (0–15). */
	protected int signal(Level level, BlockPos pos, BlockState state) {
		return level.getBestNeighborSignal(pos);
	}

	/** O estado com este sinal/energia (o ventilador também guarda a velocidade). */
	protected BlockState powered(Level level, BlockPos pos, BlockState state, int signal, boolean energy) {
		return state.setValue(LIT, signal > 0 || energy);
	}

	/** Há redstone a acender (a energia só se gasta sem ela). */
	public boolean hasRedstone(Level level, BlockPos pos, BlockState state) {
		return signal(level, pos, state) > 0;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return LampBlockEntity.create(MoveisBlocks.LAMP_BE, pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !TMoveis.INDUSTRIAL) return null;
		return createTickerHelper(type, MoveisBlocks.LAMP_BE, LampBlockEntity::serverTick);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {}
}
