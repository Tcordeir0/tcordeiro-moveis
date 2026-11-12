package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.MailboxBlock;
import com.tcordeiro.moveis.mail.MailData;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O inventário da caixa de correio (9 espaços). O dono abre-o a sério; os outros recebem um espaço de depósito vazio
 * ({@link DepositMenu}): o que lá deixarem entra na caixa ao fechar (e o que não couber volta para eles). Assim nunca veem nem
 * tiram o correio de outra pessoa.
 */
public class MailboxBlockEntity extends StorageFurnitureBlockEntity {
	public MailboxBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.MAILBOX_BE, pos, state);
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		if (isOwner(inventory.player)) return super.createMenu(containerId, inventory);
		return new DepositMenu(containerId, inventory, this);
	}

	/** Põe na caixa (de outro jogador ou de um funil); devolve o que não coube. */
	public ItemStack deposit(ItemStack stack) {
		for (int i = 0; i < getContainerSize() && !stack.isEmpty(); i++) {
			ItemStack in = getItem(i);
			if (in.isEmpty()) {
				setItem(i, stack.copyAndClear());
			} else if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) {
				int n = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
				in.grow(n);
				stack.shrink(n);
			}
		}
		setChanged();
		return stack;
	}

	/** Correio = a caixa não está vazia: levanta/baixa a bandeirinha, avisa o dono e guarda no registo (para o aviso ao entrar). */
	@Override
	public void setChanged() {
		super.setChanged();
		if (!(level instanceof ServerLevel sl) || !(getBlockState().getBlock() instanceof MailboxBlock)) return;
		boolean mail = !isEmpty();
		BlockState st = getBlockState();
		if (st.getValue(MailboxBlock.HAS_MAIL) == mail) return;
		AnimatedFurniture.setTarget(sl, worldPosition, st, MailboxBlock.HAS_MAIL, mail);
		if (owner == null) return;
		MailData data = MailData.get(sl.getServer());
		GlobalPos gp = GlobalPos.of(sl.dimension(), worldPosition);
		if (mail) {
			data.add(owner, gp);
			ServerPlayer p = sl.getServer().getPlayerList().getPlayer(owner);
			if (p != null) p.sendSystemMessage(Component.translatable("message.tmoveis.mail_arrived", worldPosition.toShortString()));
		} else {
			data.remove(owner, gp);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel sl && owner != null) MailData.get(sl.getServer()).remove(owner, GlobalPos.of(sl.dimension(), pos));
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MailboxBlockEntity be) {
		StorageFurnitureBlockEntity.tick(level, pos, state, be);
	}

	/** O ecrã de quem não é o dono: 9 espaços vazios, só dele; ao fechar entram na caixa. */
	public static class DepositMenu extends ChestMenu {
		private final MailboxBlockEntity box;
		private final Container deposit;

		public DepositMenu(int containerId, Inventory inventory, MailboxBlockEntity box) {
			this(containerId, inventory, box, new SimpleContainer(9));
		}

		private DepositMenu(int containerId, Inventory inventory, MailboxBlockEntity box, Container deposit) {
			super(MenuType.GENERIC_9x1, containerId, inventory, deposit, 1);
			this.box = box;
			this.deposit = deposit;
		}

		public MailboxBlockEntity box() {
			return box;
		}

		@Override
		public boolean stillValid(Player player) {
			return Container.stillValidBlockEntity(box, player);
		}

		@Override
		public void removed(Player player) {
			super.removed(player);
			if (player.level().isClientSide()) return;
			for (int i = 0; i < deposit.getContainerSize(); i++) {
				ItemStack st = deposit.removeItemNoUpdate(i);
				if (st.isEmpty()) continue;
				ItemStack left = box.isRemoved() ? st : box.deposit(st);
				if (!left.isEmpty()) player.getInventory().placeItemBackInInventory(left, net.minecraft.util.Prediction.SERVER_ONLY);
			}
		}
	}
}
