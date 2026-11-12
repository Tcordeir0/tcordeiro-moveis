package com.tcordeiro.moveis.finish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O item de um bloco com acabamento: guarda-o no componente block_state (posto de volta tem o mesmo acabamento) e mostra-o no nome,
 * «Parquet de Carvalho (Mel)» — igual ao que o loot (tools/gen_finishes.py) põe ao partir, para os dois empilharem.
 */
public final class FinishItems {
	private FinishItems() {}

	public static ItemStack stack(Block block, Finish finish) {
		ItemStack stack = new ItemStack(block);
		if (finish == null || finish == Finish.RAW) return stack;
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(FinishState.FINISH, finish));
		stack.set(DataComponents.ITEM_NAME, Component.translatable("finish.tmoveis.named", Component.translatable(block.getDescriptionId()),
				Component.translatable("finish.tmoveis." + finish.getSerializedName())));
		return stack;
	}

	public static ItemStack stack(BlockState state) {
		return stack(state.getBlock(), state.hasProperty(FinishState.FINISH) ? state.getValue(FinishState.FINISH) : null);
	}
}
