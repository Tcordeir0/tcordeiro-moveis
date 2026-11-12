package com.tcordeiro.moveis.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.CeilingFanBlock;
import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.finish.Finish;
import com.tcordeiro.moveis.finish.FinishState;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 0.3.3 (A4): sem MOVING no blockstate. Conta os estados (cada um pesa no arranque e na memória), confirma que os mundos antigos (com
 * «moving» gravado) carregam sem perder blocos nem os outros estados, e que os blockstates dos móveis animados (anim_blockstates, lidos
 * pelo cliente) cobrem todos os estados reais, parados e a mexer.
 */
public class StateCountGameTests {
	/** 0.3.2: 127 882 estados (medido com este teste antes da mudança). */
	public static final int BEFORE = 127_882;

	@GameTest
	public void stateCount(GameTestHelper h) {
		int total = 0;
		Map<String, int[]> byClass = new TreeMap<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			if (!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(TMoveis.MOD_ID)) continue;
			int n = b.getStateDefinition().getPossibleStates().size();
			total += n;
			h.assertTrue(b.getStateDefinition().getProperty("moving") == null, "sem MOVING no blockstate: " + b);
			int[] a = byClass.computeIfAbsent(b.getClass().getSimpleName() + " " + b.getStateDefinition().getProperties().stream().map(Property::getName).toList(),
					k -> new int[2]);
			a[0]++;
			a[1] += n;
		}
		StringBuilder sb = new StringBuilder("[ESTADOS] total=" + total + " (0.3.2: " + BEFORE + ")\n");
		byClass.forEach((k, v) -> sb.append("[ESTADOS] ").append(v[1]).append(" = ").append(v[0]).append(" x ").append(k).append('\n'));
		System.out.println(sb);
		h.assertTrue(total <= BEFORE - 18_000, "menos estados que a 0.3.2 (" + total + ")");
		h.succeed();
	}

	/** Um mundo da 0.3.2: o estado gravado tem «moving» — carrega o mesmo bloco, com todos os outros estados. */
	@GameTest
	public void oldWorldsWithMovingStillLoad(GameTestHelper h) {
		Block cab = MoveisBlocks.wooden(Furniture.CABINET, "oak");
		// a paleta dos chunks (BlockState.CODEC) e as estruturas/NBT (NbtUtils.readBlockState)
		JsonObject props = new JsonObject();
		props.addProperty("facing", "east");
		props.addProperty("open", "true");
		props.addProperty("moving", "true");
		props.addProperty("finish", "walnut_polished");
		JsonObject json = new JsonObject();
		json.addProperty("id", "tmoveis:oak_cabinet");
		json.add("properties", props);
		BlockState fromChunk = BlockState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		CompoundTag tag = (CompoundTag) new Dynamic<>(JsonOps.INSTANCE, (JsonElement) json).convert(NbtOps.INSTANCE).getValue();
		BlockState fromNbt = NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), tag);
		for (BlockState st : new BlockState[] {fromChunk, fromNbt}) {
			h.assertTrue(st.is(cab), "o mesmo bloco: " + st);
			h.assertValueEqual(st.getValue(StorageFurnitureBlock.FACING), Direction.EAST, "virado a este");
			h.assertValueEqual(st.getValue(AnimatedFurniture.OPEN), true, "aberto");
			h.assertValueEqual(FinishState.of(st), Finish.WALNUT_POLISHED, "o verniz");
		}
		// o ventilador e a caixa de correio também
		JsonObject f = new JsonObject();
		f.addProperty("id", "tmoveis:ceiling_fan");
		JsonObject fp = new JsonObject();
		fp.addProperty("lit", "true");
		fp.addProperty("power", "7");
		fp.addProperty("moving", "true");
		f.add("properties", fp);
		BlockState fan = BlockState.CODEC.parse(JsonOps.INSTANCE, f).getOrThrow();
		h.assertTrue(fan.is(MoveisBlocks.CEILING_FAN) && fan.getValue(CeilingFanBlock.POWER) == 7, "ventilador com a força do sinal: " + fan);
		h.succeed();
	}

	/**
	 * Os blockstates dos móveis animados (anim_blockstates): só os animados os têm (e não têm o blockstate normal); as chaves são as
	 * propriedades do bloco + moving; nas variantes, cada estado real tem exatamente um modelo parado e um a mexer.
	 */
	@GameTest
	public void animBlockstatesCoverEveryState(GameTestHelper h) {
		int animated = 0;
		for (Block b : BuiltInRegistries.BLOCK) {
			var id = BuiltInRegistries.BLOCK.getKey(b);
			if (!id.getNamespace().equals(TMoveis.MOD_ID)) continue;
			boolean anim = b instanceof AnimatedFurniture || b instanceof CeilingFanBlock;
			JsonObject animBs = read("/assets/tmoveis/anim_blockstates/" + id.getPath() + ".json"), bs = read("/assets/tmoveis/blockstates/" + id.getPath() + ".json");
			h.assertTrue(anim == (animBs != null), "anim_blockstates só nos animados: " + id);
			h.assertTrue(anim == (bs == null), "o blockstate normal só nos outros: " + id);
			if (!anim) continue;
			animated++;
			Set<String> names = new HashSet<>();
			for (Property<?> p : b.getStateDefinition().getProperties()) names.add(p.getName());
			names.add("moving");
			List<String> keys = new ArrayList<>();
			collectKeys(animBs, keys);
			for (String k : keys) h.assertTrue(names.contains(k), id + ": chave desconhecida «" + k + "»");
			if (animBs.has("variants")) {
				var variants = animBs.getAsJsonObject("variants").keySet();
				for (BlockState st : b.getStateDefinition().getPossibleStates())
					for (String mv : new String[] {"false", "true"}) {
						int n = 0;
						for (String v : variants) if (matches(v, st, mv)) n++;
						h.assertValueEqual(n, 1, id + " " + st + " moving=" + mv + ": um modelo");
					}
			}
		}
		h.assertValueEqual(animated, 111, "111 móveis animados");
		h.succeed();
	}

	static JsonObject read(String path) {
		try (InputStream in = TMoveis.class.getResourceAsStream(path)) {
			return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Nomes de propriedades usados no ficheiro (chaves das variantes e condições do multipart). */
	static void collectKeys(JsonElement e, List<String> out) {
		if (e.isJsonObject()) {
			JsonObject o = e.getAsJsonObject();
			if (o.has("variants")) {
				for (String v : o.getAsJsonObject("variants").keySet())
					for (String kv : v.split(",")) if (!kv.isEmpty()) out.add(kv.split("=")[0]);
				return;
			}
			for (var en : o.entrySet()) {
				if (en.getKey().equals("apply")) continue;
				if (!en.getKey().equals("when") && !en.getKey().equals("multipart") && !en.getKey().equals("AND") && !en.getKey().equals("OR"))
					out.add(en.getKey());
				collectKeys(en.getValue(), out);
			}
		} else if (e.isJsonArray()) {
			for (JsonElement x : e.getAsJsonArray()) collectKeys(x, out);
		}
	}

	static boolean matches(String variant, BlockState st, String moving) {
		for (String kv : variant.split(",")) {
			if (kv.isEmpty()) continue;
			String[] p = kv.split("=");
			String now = p[0].equals("moving") ? moving : value(st, st.getBlock().getStateDefinition().getProperty(p[0]));
			if (!p[1].equals(now)) return false;
		}
		return true;
	}

	private static <T extends Comparable<T>> String value(BlockState st, Property<T> p) {
		return p.getName(st.getValue(p));
	}
}
