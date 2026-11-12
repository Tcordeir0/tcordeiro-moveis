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
		h.assertValueEqual(com.tcordeiro.moveis.guide.MoveisGuide.PAGES.size() + 2, content.pages().size(),
				"livro = páginas do guia + madeiras + «o resto no separador Guias»");
		h.assertTrue(content.pages().get(content.pages().size() - 1).raw().getString().contains("separador Guias"), "a última página manda para o separador");
		// 0.3.2: cada receita do separador Guias existe no servidor e dá o item mostrado; as da Serraria são do tipo tmoveis:sawing
		var ctx = SlotDisplayContext.fromLevel(h.getLevel());
		for (var p : com.tcordeiro.moveis.guide.MoveisGuide.PAGES)
			for (var r : p.recipes()) {
				var rh = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.parse(r.id())));
				h.assertTrue(rh.isPresent(), "receita do guia existe: " + r.id());
				var shown = rh.get().value().display().get(0).result().resolveForFirstStack(ctx);
				h.assertValueEqual(BuiltInRegistries.ITEM.getKey(shown.getItem()).toString(), r.result(), "resultado de " + r.id());
				boolean saw = BuiltInRegistries.RECIPE_TYPE.getKey(rh.get().value().getType()).toString().equals("tmoveis:sawing");
				h.assertValueEqual(r.station() != null, saw, "estação da Serraria em " + r.id());
				if (saw) h.assertValueEqual(r.station(), "tmoveis:sawmill", "estação de " + r.id());
			}
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

	private static final java.util.Map<String, Boolean> MODEL_OK = new java.util.concurrent.ConcurrentHashMap<>();

	static boolean modelExists(String id) {
		return MODEL_OK.computeIfAbsent(id, DataGameTests::modelExists0);
	}

	private static boolean modelExists0(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		JsonObject m = json("/assets/tmoveis/models/" + path + ".json");
		if (m == null) return false;
		if (!m.has("parent")) return true;
		String parent = m.get("parent").getAsString();
		return !parent.startsWith("tmoveis:") || json("/assets/tmoveis/models/" + parent.substring(8) + ".json") != null;
	}

	static String value(BlockState st, String prop) {
		if (prop.equals("moving")) return moving; // 0.3.3: só nos anim_blockstates (vem da entidade)
		for (Property<?> p : st.getProperties()) if (p.getName().equals(prop)) return name(st, p);
		return null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static String name(BlockState st, Property p) {
		return p.getName(st.getValue(p));
	}

	/** Uma condição de multipart («when»): chaves simples (valores com |), AND, OR. */
	/** O valor de «moving» a conferir nos anim_blockstates (null = o bloco não a usa). */
	static String moving;

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

	/** Todos os modelos de uma definição de item (simples, composto, select por acabamento…). */
	static void itemModels(JsonElement e, java.util.List<String> out) {
		if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) itemModels(x, out);
		else if (e.isJsonObject()) for (var kv : e.getAsJsonObject().entrySet()) {
			if (kv.getKey().equals("model") && kv.getValue().isJsonPrimitive()) out.add(kv.getValue().getAsString());
			else itemModels(kv.getValue(), out);
		}
	}

	@GameTest
	public void everyStateHasAModelAndEveryMovingPartToo(GameTestHelper h) {
		int states = 0, parts = 0;
		for (Block b : MoveisBlocks.all()) {
			String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
			JsonObject anim = json("/assets/tmoveis/anim_blockstates/" + path + ".json");
			JsonObject bs = anim != null ? anim : json("/assets/tmoveis/blockstates/" + path + ".json");
			h.assertTrue(bs != null, "blockstate de " + path);
			for (BlockState st : b.getStateDefinition().getPossibleStates()) for (String mv : anim != null ? new String[] {"false", "true"} : new String[] {null}) {
				moving = mv;
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
						h.assertTrue(item != null, "item da parte " + path + "/" + part.model());
						java.util.List<String> pm = new java.util.ArrayList<>();
						itemModels(item, pm);
						h.assertTrue(!pm.isEmpty(), "parte sem modelo " + path + "/" + part.model());
						for (String m : pm) h.assertTrue(modelExists(m), "modelo da parte em falta: " + m);
						parts++;
					}
				}
			}
			JsonObject item = json("/assets/tmoveis/items/" + path + ".json");
			h.assertTrue(item != null, "item de " + path);
			java.util.List<String> im = new java.util.ArrayList<>();
			itemModels(item, im);
			h.assertTrue(!im.isEmpty(), "item sem modelo " + path);
			for (String m : im) h.assertTrue(modelExists(m), "modelo do item " + path + ": " + m);
		}
		h.assertTrue(states > 1000 && parts > 100, "conferidos " + states + " estados e " + parts + " partes");
		h.succeed();
	}
}
