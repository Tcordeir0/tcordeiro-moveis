package com.tcordeiro.moveis.test;

import java.util.HashSet;
import java.util.Set;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.wood.WoodTypes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.block.Block;

/** Dados: todas as madeiras do jogo têm móveis, cada bloco tem item, receita e loot; o livro-guia cabe no limite de 100 páginas. */
public class DataGameTests {
	@GameTest
	public void everyVanillaWoodHasFurniture(GameTestHelper h) {
		Set<String> planks = new HashSet<>();
		BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.PLANKS).forEach(it -> planks.add(BuiltInRegistries.ITEM.getKey(it.value()).getPath()));
		Set<String> ours = new HashSet<>();
		for (WoodTypes.Wood w : WoodTypes.ALL) ours.add(w.planks());
		h.assertValueEqual(ours, planks, "as madeiras dos móveis são as da tag minecraft:planks");
		for (MoveisBlocks.Furniture f : MoveisBlocks.Furniture.values())
			h.assertValueEqual(MoveisBlocks.WOODEN.get(f).size(), WoodTypes.ALL.size(), "uma " + f + " por madeira");
		h.assertValueEqual(MoveisBlocks.all().size(), WoodTypes.ALL.size() * 5 + 16 + 16 + 3, "total de blocos");
		h.succeed();
	}

	@GameTest
	public void everyBlockHasItemRecipeAndLoot(GameTestHelper h) {
		var server = h.getLevel().getServer();
		var recipes = server.getRecipeManager();
		for (Block b : MoveisBlocks.all()) {
			var id = BuiltInRegistries.BLOCK.getKey(b);
			Item item = b.asItem();
			h.assertTrue(item != net.minecraft.world.item.Items.AIR, "item de " + id);
			h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent(), "receita de " + id);
			h.assertTrue(b.getLootTable().isPresent()
					&& server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE).flatMap(l -> l.get(b.getLootTable().get())).isPresent(),
					"loot de " + id);
		}
		h.succeed();
	}

	@GameTest
	public void guideBookFits(GameTestHelper h) {
		var server = h.getLevel().getServer();
		var holder = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, TMoveis.id("guide")));
		h.assertTrue(holder.isPresent(), "receita do livro-guia");
		ItemStack book = holder.get().value().display().get(0).result().resolveForFirstStack(SlotDisplayContext.fromLevel(h.getLevel()));
		var content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
		h.assertTrue(content != null && !content.pages().isEmpty() && content.pages().size() <= 100, "livro com 1–100 páginas");
		h.assertValueEqual(com.tcordeiro.moveis.guide.MoveisGuide.PAGES.size() + 1, content.pages().size(), "livro = páginas do guia + madeiras");
		h.succeed();
	}
}
