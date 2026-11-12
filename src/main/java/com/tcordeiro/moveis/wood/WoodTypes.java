package com.tcordeiro.moveis.wood;

import java.util.List;

/**
 * As madeiras dos móveis: TODAS as do Minecraft 26.3, pela ordem da tag minecraft:planks (lidas do jar do jogo por
 * tools/gen_data.py — GERADO, não editar à mão; para acrescentar madeiras ou «acabamentos» (verniz), mudar o gerador).
 * Cada uma: id, tábuas, laje, tronco descascado (textura das pernas) e se arde (as do Nether não).
 */
public final class WoodTypes {
	private WoodTypes() {}

	public record Wood(String id, String planks, String slab, String log, boolean flammable) {}

	public static final List<Wood> ALL = List.of(
			new Wood("oak", "oak_planks", "oak_slab", "stripped_oak_log", true),
			new Wood("spruce", "spruce_planks", "spruce_slab", "stripped_spruce_log", true),
			new Wood("birch", "birch_planks", "birch_slab", "stripped_birch_log", true),
			new Wood("jungle", "jungle_planks", "jungle_slab", "stripped_jungle_log", true),
			new Wood("acacia", "acacia_planks", "acacia_slab", "stripped_acacia_log", true),
			new Wood("dark_oak", "dark_oak_planks", "dark_oak_slab", "stripped_dark_oak_log", true),
			new Wood("pale_oak", "pale_oak_planks", "pale_oak_slab", "stripped_pale_oak_log", true),
			new Wood("crimson", "crimson_planks", "crimson_slab", "stripped_crimson_stem", false),
			new Wood("warped", "warped_planks", "warped_slab", "stripped_warped_stem", false),
			new Wood("mangrove", "mangrove_planks", "mangrove_slab", "stripped_mangrove_log", true),
			new Wood("bamboo", "bamboo_planks", "bamboo_slab", "stripped_bamboo_block", true),
			new Wood("cherry", "cherry_planks", "cherry_slab", "stripped_cherry_log", true),
			new Wood("poplar", "poplar_planks", "poplar_slab", "stripped_poplar_log", true));

	public static Wood byId(String id) {
		for (Wood w : ALL) if (w.id().equals(id)) return w;
		return null;
	}
}
