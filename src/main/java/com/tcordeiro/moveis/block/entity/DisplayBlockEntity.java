package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** O item em cima da mesa (um só), enviado aos jogadores para o renderer o desenhar. */
public class DisplayBlockEntity extends BlockEntity {
	private ItemStack item = ItemStack.EMPTY;

	public DisplayBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.DISPLAY_BE, pos, state);
	}

	public ItemStack item() {
		return item;
	}

	public void setItem(ItemStack stack) {
		item = stack;
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (!item.isEmpty()) out.store("item", ItemStack.CODEC, item);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		item = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !item.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, item);
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
