package com.tcordeiro.moveis.client;

import java.util.HashMap;
import java.util.Map;

import com.tcordeiro.moveis.TMoveis;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * As partes móveis como itens com {@code item_model = tmoveis:part/<bloco>/<parte>} (os modelos que tools/gen_data.py gera a partir dos
 * templates do pecas.py, com as texturas do bloco). Criadas uma vez por bloco e parte (sem alocações por frame).
 */
final class PartStacks {
	private PartStacks() {}

	private static final Map<Block, Map<String, ItemStack>> CACHE = new HashMap<>();

	static ItemStack get(Block block, String part) {
		return CACHE.computeIfAbsent(block, b -> new HashMap<>()).computeIfAbsent(part, p -> {
			ItemStack st = new ItemStack(Items.STICK);
			st.set(DataComponents.ITEM_MODEL, TMoveis.id("part/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "/" + p));
			return st;
		});
	}
}
