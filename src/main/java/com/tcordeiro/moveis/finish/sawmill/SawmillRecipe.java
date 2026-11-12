package com.tcordeiro.moveis.finish.sawmill;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.tcordeiro.moveis.finish.FinishBlocks;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

/** Receita da Serraria (tipo tmoveis:sawing): 1 item → resultado, como o cortador de pedras. JSON: ingredient + result. */
public class SawmillRecipe extends SingleItemRecipe {
	public static final MapCodec<SawmillRecipe> MAP_CODEC = simpleMapCodec(SawmillRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, SawmillRecipe> STREAM_CODEC = simpleStreamCodec(SawmillRecipe::new);
	public static final RecipeSerializer<SawmillRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public SawmillRecipe(CommonInfo info, Ingredient ingredient, ItemStackTemplate result) {
		super(info, ingredient, result);
	}

	@Override
	public RecipeType<SawmillRecipe> getType() {
		return Sawing.TYPE;
	}

	@Override
	public RecipeSerializer<SawmillRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public String group() {
		return "";
	}

	/** O que sai (para o ecrã e os testes). */
	public ItemStack resultItem() {
		return result().create();
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(new StonecutterRecipeDisplay(input().display(), new SlotDisplay.ItemStackSlotDisplay(result()),
				new SlotDisplay.ItemSlotDisplay(FinishBlocks.SAWMILL.asItem())));
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.STONECUTTER;
	}
}
