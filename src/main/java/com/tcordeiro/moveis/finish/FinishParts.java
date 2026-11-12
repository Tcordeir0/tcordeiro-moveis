package com.tcordeiro.moveis.finish;

import com.tcordeiro.moveis.TMoveis;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/**
 * A pilha de item com que o BlockEntityRenderer desenha uma parte móvel (porta, gaveta, persiana…): {@code item_model =
 * tmoveis:part/<bloco>/<parte>} e, se o móvel tem acabamento, o componente block_state {@code finish}. A definição desse item
 * (tools/gen_finishes.py) é um {@code minecraft:select} pela propriedade «finish»: escolhe o modelo da parte com as texturas do
 * verniz, do polido ou do desgaste — as mesmas do corpo do móvel. Fica no código comum para os GameTests a poderem conferir.
 */
public final class FinishParts {
	private FinishParts() {}

	public static ItemStack stack(Block block, String part, Finish finish) {
		ItemStack st = new ItemStack(Items.STICK);
		st.set(DataComponents.ITEM_MODEL, TMoveis.id("part/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "/" + part));
		if (finish != null && finish != Finish.RAW)
			st.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(FinishState.FINISH, finish));
		return st;
	}
}
