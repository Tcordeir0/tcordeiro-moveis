package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** As 9 casas da adega: uma garrafa/poção em cada (também por funil), enviadas aos jogadores para o renderer. Não tem ecrã. */
public class WineRackBlockEntity extends BaseContainerBlockEntity {
	public static final int SIZE = 9;
	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	public WineRackBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.WINE_BE, pos, state);
	}

	/** Só frascos, poções e garrafas (mel, experiência, a ominosa). */
	public static boolean isBottle(ItemStack s) {
		return s.is(Items.GLASS_BOTTLE) || s.is(Items.POTION) || s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION) || s.is(Items.HONEY_BOTTLE)
				|| s.is(Items.EXPERIENCE_BOTTLE) || s.is(Items.OMINOUS_BOTTLE) || s.has(DataComponents.POTION_CONTENTS);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return isBottle(stack) && getItem(slot).isEmpty();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
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
		return null; // sem ecrã: põe-se e tira-se com cliques nas casas
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, items);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(in, items);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
