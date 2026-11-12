package com.tcordeiro.moveis.client;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.tcordeiro.moveis.finish.Finish;
import com.tcordeiro.moveis.finish.FinishParts;
import com.tcordeiro.moveis.finish.FinishState;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * As partes móveis como itens ({@link FinishParts#stack}: {@code item_model = tmoveis:part/<bloco>/<parte>} e o acabamento do
 * móvel no componente block_state). Criadas uma vez por bloco, parte e acabamento (sem alocações por frame).
 */
final class PartStacks {
	private PartStacks() {}

	private static final Map<Block, Map<String, Map<Finish, ItemStack>>> CACHE = new HashMap<>();
	private static final Map<Block, Map<String, ItemStack>> PLAIN = new HashMap<>();

	static ItemStack get(BlockState state, String part) {
		Block block = state.getBlock();
		if (!state.hasProperty(FinishState.FINISH))
			return PLAIN.computeIfAbsent(block, b -> new HashMap<>()).computeIfAbsent(part, p -> FinishParts.stack(block, p, null));
		Finish f = state.getValue(FinishState.FINISH);
		return CACHE.computeIfAbsent(block, b -> new HashMap<>()).computeIfAbsent(part, p -> new EnumMap<>(Finish.class))
				.computeIfAbsent(f, ff -> FinishParts.stack(block, part, ff));
	}
}
