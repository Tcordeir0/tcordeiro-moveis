package com.tcordeiro.moveis.block.entity;

import java.util.List;

import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** O inventário do armário/gaveteiro: 27 espaços, como o barril (o mesmo ecrã de 3 linhas, compatível com funis e comparadores). */
public class StorageFurnitureBlockEntity extends RandomizableContainerBlockEntity {
	public static final int SIZE = 27;
	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, true);
			setOpen(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, false);
			setOpen(state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == StorageFurnitureBlockEntity.this;
		}
	};

	public StorageFurnitureBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.STORAGE_BE, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!trySaveLootTable(output)) ContainerHelper.saveAllItems(output, items);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		if (!tryLoadLootTable(input)) ContainerHelper.loadAllItems(input, items);
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator())
			openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator())
			openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return openers.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
	}

	public void recheckOpen() {
		if (!remove) openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
	}

	private void setOpen(BlockState state, boolean open) {
		if (level != null && state.hasProperty(StorageFurnitureBlock.OPEN)) level.setBlockAndUpdate(getBlockPos(), state.setValue(StorageFurnitureBlock.OPEN, open));
	}

	/** Armário: portas de madeira; gaveteiro: o som do barril (madeira a deslizar). */
	private void playSound(BlockState state, boolean open) {
		if (level == null) return;
		boolean cabinet = state.getBlock() instanceof StorageFurnitureBlock b && b.kind == StorageFurnitureBlock.Kind.CABINET;
		SoundEvent ev = cabinet ? (open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE) : (open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE);
		Vec3i d = state.getValue(StorageFurnitureBlock.FACING).getUnitVec3i();
		level.playSound(null, worldPosition.getX() + 0.5 + d.getX() / 2.0, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5 + d.getZ() / 2.0, ev,
				SoundSource.BLOCKS, 0.5f, level.getRandom().nextFloat() * 0.1f + 0.9f);
	}
}
