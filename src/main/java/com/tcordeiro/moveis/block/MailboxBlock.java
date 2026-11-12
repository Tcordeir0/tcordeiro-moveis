package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.block.entity.MailboxBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Caixa de correio: tem dono (quem a põe). Qualquer jogador deixa itens (num espaço à parte, que entra na caixa ao fechar: não vê nem
 * tira o correio que lá está); só o dono abre a caixa a sério e tira. A bandeirinha levanta quando há correio (HAS_MAIL, animada) e
 * o dono recebe um aviso (na hora, se estiver no mundo; senão, ao entrar). Os funis põem mas não tiram.
 */
public class MailboxBlock extends StorageFurnitureBlock {
	public static final BooleanProperty HAS_MAIL = BooleanProperty.create("has_mail");

	public MailboxBlock(Properties props) {
		super(Kind.MAILBOX, props);
		registerDefaultState(defaultBlockState().setValue(HAS_MAIL, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HAS_MAIL);
	}

	@Override
	public boolean animTarget(BlockState state) {
		return state.getValue(HAS_MAIL);
	}

	/** A bandeirinha tem modelo estático em cima e em baixo: só se desenha a mexer. */
	@Override
	public boolean hasStaticOpenModel() {
		return true;
	}

	/** Abrir o ecrã não mexe nada (só a bandeirinha, com o correio). */
	@Override
	public void onOpenChanged(Level level, BlockPos pos, BlockState state, boolean open) {}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		super.setPlacedBy(level, pos, state, by, stack);
		if (!level.isClientSide() && by instanceof Player p && level.getBlockEntity(pos) instanceof MailboxBlockEntity be) be.setOwner(p);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MailboxBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, MoveisBlocks.MAILBOX_BE, MailboxBlockEntity::tick);
	}
}
