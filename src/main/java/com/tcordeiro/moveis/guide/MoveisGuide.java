package com.tcordeiro.moveis.guide;

import java.util.List;

/**
 * Páginas do guia dos Móveis, lidas pelo separador «Guias» do Receitário por reflexão ({@code PAGES}; cada página com
 * {@code key()}, {@code image()} e {@code recipes()}; cada receita com {@code result()}, {@code count()} e {@code cell(linha, coluna)}).
 * Os textos estão nas traduções: guide.tmoveis.&lt;página&gt;.title / .text.
 * <p>
 * GERADO por tools/gen_data.py: não editar à mão.
 */
public final class MoveisGuide {
	private MoveisGuide() {}

	/** Receita numa página: id, resultado, quantidade e 3 linhas de 3 casas (id de item, «#tag» ou null). */
	public record Recipe(String id, String result, int count, String[][] grid) {
		public String cell(int row, int col) {
			return row >= 0 && row < grid.length && col >= 0 && col < grid[row].length ? grid[row][col] : null;
		}
	}

	/** Página: chave de tradução, imagem opcional (textures/gui/guide/&lt;img&gt;.png) e receitas. */
	public record Page(String key, String image, List<Recipe> recipes) {}

	static final Recipe OAK_CHAIR = new Recipe("tmoveis:oak_chair", "tmoveis:oak_chair", 2,
			new String[][] {{"minecraft:oak_planks", null, null}, {"minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"}, {"minecraft:stick", null, "minecraft:stick"}});
	static final Recipe OAK_STOOL = new Recipe("tmoveis:oak_stool", "tmoveis:oak_stool", 2,
			new String[][] {{"minecraft:oak_slab", "minecraft:oak_slab", "minecraft:oak_slab"}, {"minecraft:stick", null, "minecraft:stick"}, {null, null, null}});
	static final Recipe OAK_TABLE = new Recipe("tmoveis:oak_table", "tmoveis:oak_table", 1,
			new String[][] {{"minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"}, {"minecraft:stick", null, "minecraft:stick"}, {"minecraft:stick", null, "minecraft:stick"}});
	static final Recipe OAK_CABINET = new Recipe("tmoveis:oak_cabinet", "tmoveis:oak_cabinet", 1,
			new String[][] {{"minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"}, {"minecraft:oak_planks", "minecraft:stick", "minecraft:oak_planks"}, {"minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"}});
	static final Recipe OAK_DRAWER = new Recipe("tmoveis:oak_drawer", "tmoveis:oak_drawer", 1,
			new String[][] {{"minecraft:oak_planks", "minecraft:oak_slab", "minecraft:oak_planks"}, {"minecraft:oak_planks", "minecraft:stick", "minecraft:oak_planks"}, {"minecraft:oak_planks", "minecraft:oak_slab", "minecraft:oak_planks"}});
	static final Recipe WHITE_SOFA = new Recipe("tmoveis:white_sofa", "tmoveis:white_sofa", 2,
			new String[][] {{"minecraft:white_wool", null, "minecraft:white_wool"}, {"minecraft:white_wool", "minecraft:white_wool", "minecraft:white_wool"}, {"#minecraft:wooden_slabs", "#minecraft:wooden_slabs", "#minecraft:wooden_slabs"}});
	static final Recipe WHITE_DOUBLE_BED = new Recipe("tmoveis:white_double_bed", "tmoveis:white_double_bed", 1,
			new String[][] {{"minecraft:white_bed", "minecraft:white_bed", null}, {null, null, null}, {null, null, null}});
	static final Recipe CHANDELIER = new Recipe("tmoveis:chandelier", "tmoveis:chandelier", 1,
			new String[][] {{null, "minecraft:copper_ingot", null}, {"minecraft:glass", "minecraft:redstone", "minecraft:glass"}, {"minecraft:glass", null, "minecraft:glass"}});
	static final Recipe WALL_LAMP = new Recipe("tmoveis:wall_lamp", "tmoveis:wall_lamp", 2,
			new String[][] {{"minecraft:copper_ingot", "minecraft:glass", null}, {"minecraft:copper_ingot", "minecraft:redstone", null}, {null, null, null}});
	static final Recipe TABLE_LAMP = new Recipe("tmoveis:table_lamp", "tmoveis:table_lamp", 1,
			new String[][] {{"minecraft:glass", null, null}, {"minecraft:redstone", null, null}, {"minecraft:copper_ingot", null, null}});

	public static final List<Page> PAGES = List.of(
			new Page("cover", null, List.of()),
			new Page("sitting", null, List.of()),
			new Page("chair", null, List.of(OAK_CHAIR)),
			new Page("stool", null, List.of(OAK_STOOL)),
			new Page("table", null, List.of(OAK_TABLE)),
			new Page("cabinet", null, List.of(OAK_CABINET)),
			new Page("drawer", null, List.of(OAK_DRAWER)),
			new Page("storage_use", null, List.of()),
			new Page("sofa", null, List.of(WHITE_SOFA)),
			new Page("sofa_shapes", null, List.of()),
			new Page("double_bed", null, List.of(WHITE_DOUBLE_BED)),
			new Page("double_bed_use", null, List.of()),
			new Page("lamps", null, List.of()),
			new Page("chandelier", null, List.of(CHANDELIER)),
			new Page("wall_lamp", null, List.of(WALL_LAMP)),
			new Page("table_lamp", null, List.of(TABLE_LAMP)),
			new Page("lamp_energy", null, List.of()));
}
