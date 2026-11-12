package com.tcordeiro.moveis.finish.sawmill;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.tcordeiro.moveis.TMoveis;

import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * O tipo de receita da Serraria, o serializador (sincronizado com os clientes pela API do Fabric, para o ecrã mostrar as opções) e o
 * tipo de menu.
 */
public final class Sawing {
	private Sawing() {}

	public static final RecipeType<SawmillRecipe> TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE, TMoveis.id("sawing"),
			new RecipeType<SawmillRecipe>() {
				@Override
				public String toString() {
					return "tmoveis:sawing";
				}
			});
	public static final MenuType<SawmillMenu> MENU = Registry.register(BuiltInRegistries.MENU, TMoveis.id("sawmill"),
			new MenuType<>(SawmillMenu::new, FeatureFlags.VANILLA_SET));

	static {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, TMoveis.id("sawing"), SawmillRecipe.SERIALIZER);
		RecipeSynchronization.synchronizeRecipeSerializer(SawmillRecipe.SERIALIZER);
	}

	public static void init() {}

	/** As receitas para este item, pela ordem do id (a mesma no servidor e no cliente: o botão escolhido é o índice). */
	public static List<RecipeHolder<SawmillRecipe>> recipesFor(Level level, ItemStack stack) {
		if (stack.isEmpty()) return List.of();
		SingleRecipeInput in = new SingleRecipeInput(stack);
		Stream<RecipeHolder<SawmillRecipe>> s = level instanceof ServerLevel sl ? sl.recipeAccess().getAllMatches(TYPE, in, level)
				: level.recipeAccess().getSynchronizedRecipes().getAllMatches(TYPE, in, level);
		return s.sorted(Comparator.comparing(h -> h.id().identifier().toString())).toList();
	}
}
