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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tcordeiro.moveis.anim.FurnitureParts;
import com.tcordeiro.moveis.block.AnimatedFurniture;

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
		// madeira: 5 da 0.1.0 + 10 da 0.2.0; cores: sofá, cama de casal, cortina, toldo, beliche; 3 lâmpadas + 12 peças de uma variante (0.2.0)
		h.assertValueEqual(MoveisBlocks.all().size(), WoodTypes.ALL.size() * MoveisBlocks.Furniture.values().length + 16 * 5 + 3 + 12, "total de blocos");
		h.assertValueEqual(MoveisBlocks.Furniture.values().length, 15, "15 móveis de madeira");
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

	// ---------------------------------------------------------------- 0.2.0: os ficheiros do cliente batem certo com os estados

	static JsonObject json(String path) {
		var in = TMoveis.class.getResourceAsStream(path);
		if (in == null) return null;
		try (var r = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(r).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(path, e);
		}
	}

	static boolean modelExists(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		JsonObject m = json("/assets/tmoveis/models/" + path + ".json");
		if (m == null) return false;
		if (!m.has("parent")) return true;
		String parent = m.get("parent").getAsString();
		return !parent.startsWith("tmoveis:") || json("/assets/tmoveis/models/" + parent.substring(8) + ".json") != null;
	}

	static String value(BlockState st, String prop) {
		for (Property<?> p : st.getProperties()) if (p.getName().equals(prop)) return name(st, p);
		return null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static String name(BlockState st, Property p) {
		return p.getName(st.getValue(p));
	}

	/** Uma condição de multipart («when»): chaves simples (valores com |), AND, OR. */
	static boolean when(BlockState st, JsonObject w) {
		if (w.has("AND")) {
			for (JsonElement e : w.getAsJsonArray("AND")) if (!when(st, e.getAsJsonObject())) return false;
			return true;
		}
		if (w.has("OR")) {
			for (JsonElement e : w.getAsJsonArray("OR")) if (when(st, e.getAsJsonObject())) return true;
			return false;
		}
		for (var e : w.entrySet()) {
			String v = value(st, e.getKey());
			if (v == null || !java.util.Arrays.asList(e.getValue().getAsString().split("\\|")).contains(v)) return false;
		}
		return true;
	}

	static boolean variantMatches(BlockState st, String key) {
		if (key.isEmpty()) return true;
		for (String kv : key.split(",")) {
			String[] p = kv.split("=");
			if (!p[1].equals(value(st, p[0]))) return false;
		}
		return true;
	}

	static void models(JsonElement apply, java.util.List<String> out) {
		if (apply.isJsonArray()) for (JsonElement e : apply.getAsJsonArray()) out.add(e.getAsJsonObject().get("model").getAsString());
		else out.add(apply.getAsJsonObject().get("model").getAsString());
	}

	@GameTest
	public void everyStateHasAModelAndEveryMovingPartToo(GameTestHelper h) {
		int states = 0, parts = 0;
		for (Block b : MoveisBlocks.all()) {
			String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
			JsonObject bs = json("/assets/tmoveis/blockstates/" + path + ".json");
			h.assertTrue(bs != null, "blockstate de " + path);
			for (BlockState st : b.getStateDefinition().getPossibleStates()) {
				java.util.List<String> used = new java.util.ArrayList<>();
				if (bs.has("variants")) {
					for (var e : bs.getAsJsonObject("variants").entrySet()) if (variantMatches(st, e.getKey())) models(e.getValue(), used);
				} else {
					for (JsonElement part : bs.getAsJsonArray("multipart")) {
						JsonObject o = part.getAsJsonObject();
						if (!o.has("when") || when(st, o.getAsJsonObject("when"))) models(o.get("apply"), used);
					}
				}
				h.assertTrue(!used.isEmpty(), "sem modelo: " + st);
				for (String m : used) h.assertTrue(modelExists(m), "modelo em falta: " + m + " (" + st + ")");
				states++;
				// as partes móveis deste estado têm modelo de item (o renderer desenha-as)
				if (b instanceof AnimatedFurniture af) {
					var list = FurnitureParts.BY_KIND.get(af.animKind(st));
					h.assertTrue(list != null, "partes de " + af.animKind(st));
					for (var part : list) {
						JsonObject item = json("/assets/tmoveis/items/part/" + path + "/" + part.model() + ".json");
						h.assertTrue(item != null && modelExists(item.getAsJsonObject("model").get("model").getAsString()), "parte " + path + "/" + part.model());
						parts++;
					}
				}
			}
			JsonObject item = json("/assets/tmoveis/items/" + path + ".json");
			h.assertTrue(item != null, "item de " + path);
			JsonObject im = item.getAsJsonObject("model");
			if (im.has("models")) {
				for (JsonElement e : im.getAsJsonArray("models")) h.assertTrue(modelExists(e.getAsJsonObject().get("model").getAsString()), "item composto " + path);
			} else {
				h.assertTrue(modelExists(im.get("model").getAsString()), "modelo do item " + path);
			}
		}
		h.assertTrue(states > 1000 && parts > 100, "conferidos " + states + " estados e " + parts + " partes");
		h.succeed();
	}
}
