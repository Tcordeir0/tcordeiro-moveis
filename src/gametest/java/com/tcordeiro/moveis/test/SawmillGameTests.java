package com.tcordeiro.moveis.test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tcordeiro.moveis.finish.FinishBlocks;
import com.tcordeiro.moveis.finish.FinishBlocks.Kind;
import com.tcordeiro.moveis.finish.sawmill.Sawing;
import com.tcordeiro.moveis.finish.sawmill.SawmillBlock;
import com.tcordeiro.moveis.finish.sawmill.SawmillMenu;
import com.tcordeiro.moveis.finish.sawmill.SawmillRecipe;
import com.tcordeiro.moveis.wood.WoodTypes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Serraria: tronco → 6 tábuas, tábuas → blocos novos/escadas/lajes/paus, o menu corta e a serra liga enquanto está aberta. */
public class SawmillGameTests {
	static List<ItemStack> results(GameTestHelper h, Item input) {
		return Sawing.recipesFor(h.getLevel(), new ItemStack(input)).stream().map(r -> r.value().resultItem()).toList();
	}

	static int count(List<ItemStack> rs, Item it) {
		return rs.stream().filter(s -> s.is(it)).mapToInt(ItemStack::getCount).findFirst().orElse(0);
	}

	@GameTest
	public void logGivesSixPlanks(GameTestHelper h) {
		h.assertValueEqual(count(results(h, Items.OAK_LOG), Items.OAK_PLANKS), 6, "tronco de carvalho → 6 tábuas");
		h.assertValueEqual(count(results(h, Items.STRIPPED_OAK_WOOD), Items.OAK_PLANKS), 6, "lenho descascado também");
		h.assertValueEqual(count(results(h, Items.CRIMSON_STEM), Items.CRIMSON_PLANKS), 6, "caule carmesim → 6");
		h.assertValueEqual(count(results(h, Items.BAMBOO_BLOCK), Items.BAMBOO_PLANKS), 3, "bloco de bambu → 3 (a bancada dá 2)");
		for (WoodTypes.Wood w : WoodTypes.ALL) {
			Item planks = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(w.planks()));
			boolean any = BuiltInRegistries.ITEM.stream().anyMatch(i -> count(results(h, i), planks) > 0);
			h.assertTrue(any, "há um tronco que dá " + w.planks());
		}
		h.succeed();
	}

	@GameTest
	public void planksGiveNewBlocksStairsSlabsSticks(GameTestHelper h) {
		List<ItemStack> rs = results(h, Items.SPRUCE_PLANKS);
		for (Kind k : Kind.values()) h.assertValueEqual(count(rs, FinishBlocks.NEW.get("spruce").get(k).asItem()), 1, "tábua → " + k);
		h.assertValueEqual(count(rs, Items.SPRUCE_STAIRS), 1, "escadas");
		h.assertValueEqual(count(rs, Items.SPRUCE_SLAB), 2, "2 lajes");
		h.assertValueEqual(count(rs, Items.STICK), 2, "2 paus");
		List<ItemStack> p = results(h, FinishBlocks.NEW.get("spruce").get(Kind.PARQUET).asItem());
		h.assertValueEqual(count(p, FinishBlocks.NEW_STAIRS.get("spruce").get(Kind.PARQUET).asItem()), 1, "parquet → escadas");
		h.assertValueEqual(count(p, FinishBlocks.NEW_SLABS.get("spruce").get(Kind.PARQUET).asItem()), 2, "parquet → 2 lajes");
		h.assertTrue(results(h, Items.STONE).isEmpty(), "pedra não se serra");
		Set<String> ids = h.getLevel().getServer().getRecipeManager().getRecipes().stream().filter(r -> r.value() instanceof SawmillRecipe)
				.map(r -> r.id().identifier().toString()).collect(Collectors.toSet());
		h.assertValueEqual(ids.size(), WoodTypes.ALL.size() * 15, "15 receitas da Serraria por madeira");
		h.succeed();
	}

	@GameTest
	public void menuCutsAndSawSpinsWhileOpen(GameTestHelper h) {
		BlockPos rel = new BlockPos(1, 1, 1);
		h.setBlock(rel, FinishBlocks.SAWMILL);
		ServerPlayer player = TestUtil.playerAt(h, new BlockPos(1, 1, 2));
		SawmillMenu menu = new SawmillMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(rel)));
		h.assertTrue(h.getBlockState(rel).getValue(SawmillBlock.ACTIVE), "ecrã aberto: a serra gira");
		menu.container.setItem(0, new ItemStack(Items.BIRCH_LOG, 2));
		List<RecipeHolder<SawmillRecipe>> rs = menu.getVisibleRecipes();
		int idx = -1;
		for (int i = 0; i < rs.size(); i++) if (rs.get(i).value().resultItem().is(Items.BIRCH_PLANKS)) idx = i;
		h.assertTrue(idx >= 0, "o tronco de bétula mostra as tábuas");
		menu.clickMenuButton(player, idx);
		ItemStack out = menu.getSlot(SawmillMenu.RESULT_SLOT).getItem();
		h.assertTrue(out.is(Items.BIRCH_PLANKS) && out.getCount() == 6, "sai 6 tábuas: " + out);
		menu.getSlot(SawmillMenu.RESULT_SLOT).onTake(player, out.copy());
		h.assertValueEqual(menu.container.getItem(0).getCount(), 1, "gastou 1 tronco");
		menu.removed(player);
		h.assertTrue(!h.getBlockState(rel).getValue(SawmillBlock.ACTIVE), "ecrã fechado: a serra para");
		h.succeed();
	}
}
