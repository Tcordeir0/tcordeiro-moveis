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
	static final Recipe OAK_COUNTER = new Recipe("tmoveis:oak_counter", "tmoveis:oak_counter", 2,
			new String[][] {{"minecraft:smooth_stone_slab", "minecraft:smooth_stone_slab", "minecraft:smooth_stone_slab"}, {"minecraft:oak_planks", "minecraft:stick", "minecraft:oak_planks"}, {"minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"}});
	static final Recipe SINK = new Recipe("tmoveis:sink", "tmoveis:sink", 1,
			new String[][] {{null, "minecraft:iron_ingot", null}, {"minecraft:smooth_stone_slab", null, "minecraft:smooth_stone_slab"}, {"#minecraft:planks", "#minecraft:planks", "#minecraft:planks"}});
	static final Recipe TRASH_CAN = new Recipe("tmoveis:trash_can", "tmoveis:trash_can", 1,
			new String[][] {{null, "minecraft:iron_nugget", null}, {"minecraft:iron_ingot", null, "minecraft:iron_ingot"}, {null, "minecraft:iron_ingot", null}});
	static final Recipe OAK_NIGHTSTAND = new Recipe("tmoveis:oak_nightstand", "tmoveis:oak_nightstand", 1,
			new String[][] {{"minecraft:oak_planks", "minecraft:oak_slab", "minecraft:oak_planks"}, {"minecraft:oak_planks", null, "minecraft:oak_planks"}, {null, null, null}});
	static final Recipe OAK_WARDROBE = new Recipe("tmoveis:oak_wardrobe", "tmoveis:oak_wardrobe", 1,
			new String[][] {{"tmoveis:oak_cabinet", "tmoveis:oak_cabinet", null}, {null, null, null}, {null, null, null}});
	static final Recipe WHITE_CURTAIN = new Recipe("tmoveis:white_curtain", "tmoveis:white_curtain", 2,
			new String[][] {{"minecraft:stick", null, null}, {"minecraft:white_wool", null, null}, {"minecraft:white_wool", null, null}});
	static final Recipe OAK_SHUTTERS = new Recipe("tmoveis:oak_shutters", "tmoveis:oak_shutters", 2,
			new String[][] {{"minecraft:oak_slab", null, "minecraft:oak_slab"}, {"minecraft:oak_slab", null, "minecraft:oak_slab"}, {null, null, null}});
	static final Recipe MIRROR = new Recipe("tmoveis:mirror", "tmoveis:mirror", 1,
			new String[][] {{"#minecraft:planks", "minecraft:glass_pane", "#minecraft:planks"}, {null, null, null}, {null, null, null}});
	static final Recipe WALL_CLOCK = new Recipe("tmoveis:wall_clock", "tmoveis:wall_clock", 1,
			new String[][] {{null, "#minecraft:planks", null}, {"#minecraft:planks", "minecraft:clock", "#minecraft:planks"}, {null, "#minecraft:planks", null}});
	static final Recipe WHITE_BUNK_BED = new Recipe("tmoveis:white_bunk_bed", "tmoveis:white_bunk_bed", 1,
			new String[][] {{"minecraft:ladder", null, null}, {"minecraft:white_bed", null, null}, {"minecraft:white_bed", null, null}});
	static final Recipe TOILET = new Recipe("tmoveis:toilet", "tmoveis:toilet", 1,
			new String[][] {{"minecraft:clay_ball", null, null}, {"minecraft:clay_ball", "minecraft:clay_ball", "minecraft:clay_ball"}, {null, "minecraft:clay_ball", null}});
	static final Recipe BATHTUB = new Recipe("tmoveis:bathtub", "tmoveis:bathtub", 1,
			new String[][] {{"minecraft:clay_ball", null, "minecraft:clay_ball"}, {"minecraft:clay_ball", "minecraft:clay_ball", "minecraft:clay_ball"}, {null, null, null}});
	static final Recipe FIREPLACE = new Recipe("tmoveis:fireplace", "tmoveis:fireplace", 1,
			new String[][] {{"minecraft:bricks", "minecraft:bricks", "minecraft:bricks"}, {"minecraft:bricks", "minecraft:campfire", "minecraft:bricks"}, {null, null, null}});
	static final Recipe CEILING_FAN = new Recipe("tmoveis:ceiling_fan", "tmoveis:ceiling_fan", 1,
			new String[][] {{null, "minecraft:iron_ingot", null}, {"#minecraft:planks", "minecraft:redstone", "#minecraft:planks"}, {null, null, null}});
	static final Recipe MAILBOX = new Recipe("tmoveis:mailbox", "tmoveis:mailbox", 1,
			new String[][] {{"minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"}, {null, "minecraft:stick", null}, {null, null, null}});
	static final Recipe OAK_DESK = new Recipe("tmoveis:oak_desk", "tmoveis:oak_desk", 1,
			new String[][] {{"minecraft:oak_slab", "minecraft:oak_slab", "minecraft:oak_slab"}, {"minecraft:stick", null, "minecraft:oak_planks"}, {"minecraft:stick", null, "minecraft:oak_planks"}});
	static final Recipe OAK_COFFEE_TABLE = new Recipe("tmoveis:oak_coffee_table", "tmoveis:oak_coffee_table", 1,
			new String[][] {{"minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"}, {"minecraft:stick", null, "minecraft:stick"}, {null, null, null}});
	static final Recipe OAK_SIDE_TABLE = new Recipe("tmoveis:oak_side_table", "tmoveis:oak_side_table", 1,
			new String[][] {{"minecraft:oak_slab", null, null}, {"minecraft:stick", null, null}, {"minecraft:oak_slab", null, null}});
	static final Recipe OAK_GARDEN_BENCH = new Recipe("tmoveis:oak_garden_bench", "tmoveis:oak_garden_bench", 1,
			new String[][] {{"minecraft:oak_slab", "minecraft:oak_slab", "minecraft:oak_slab"}, {"minecraft:iron_nugget", null, "minecraft:iron_nugget"}, {null, null, null}});
	static final Recipe OAK_PLANTER_BOX = new Recipe("tmoveis:oak_planter_box", "tmoveis:oak_planter_box", 1,
			new String[][] {{"minecraft:oak_planks", null, "minecraft:oak_planks"}, {"minecraft:oak_planks", "minecraft:dirt", "minecraft:oak_planks"}, {null, null, null}});
	static final Recipe OAK_WINE_RACK = new Recipe("tmoveis:oak_wine_rack", "tmoveis:oak_wine_rack", 1,
			new String[][] {{"minecraft:oak_planks", null, "minecraft:oak_planks"}, {null, "minecraft:oak_planks", null}, {"minecraft:oak_planks", null, "minecraft:oak_planks"}});
	static final Recipe FLOOR_LAMP = new Recipe("tmoveis:floor_lamp", "tmoveis:floor_lamp", 1,
			new String[][] {{"tmoveis:table_lamp", null, null}, {"minecraft:stick", null, null}, {"minecraft:copper_ingot", null, null}});
	static final Recipe PIANO = new Recipe("tmoveis:piano", "tmoveis:piano", 1,
			new String[][] {{"#minecraft:planks", "#minecraft:planks", "#minecraft:planks"}, {"#minecraft:planks", "minecraft:note_block", "#minecraft:planks"}, {null, null, null}});
	static final Recipe SAFE = new Recipe("tmoveis:safe", "tmoveis:safe", 1,
			new String[][] {{"minecraft:iron_ingot", "minecraft:tripwire_hook", "minecraft:iron_ingot"}, {"minecraft:iron_ingot", "minecraft:chest", "minecraft:iron_ingot"}, {"minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"}});
	static final Recipe WHITE_AWNING = new Recipe("tmoveis:white_awning", "tmoveis:white_awning", 2,
			new String[][] {{"minecraft:white_wool", "minecraft:white_wool", "minecraft:white_wool"}, {"minecraft:stick", null, "minecraft:stick"}, {null, null, null}});

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
			new Page("lamp_energy", null, List.of()),
			new Page("house", null, List.of()),
			new Page("counter", null, List.of(OAK_COUNTER)),
			new Page("sink", null, List.of(SINK)),
			new Page("trash_can", null, List.of(TRASH_CAN)),
			new Page("nightstand", null, List.of(OAK_NIGHTSTAND)),
			new Page("wardrobe", null, List.of(OAK_WARDROBE)),
			new Page("curtain", null, List.of(WHITE_CURTAIN)),
			new Page("shutters", null, List.of(OAK_SHUTTERS)),
			new Page("mirror", null, List.of(MIRROR)),
			new Page("wall_clock", null, List.of(WALL_CLOCK)),
			new Page("bunk_bed", null, List.of(WHITE_BUNK_BED)),
			new Page("toilet", null, List.of(TOILET)),
			new Page("bathtub", null, List.of(BATHTUB)),
			new Page("fireplace", null, List.of(FIREPLACE)),
			new Page("fireplace_use", null, List.of()),
			new Page("ceiling_fan", null, List.of(CEILING_FAN)),
			new Page("fan_climate", null, List.of()),
			new Page("mailbox", null, List.of(MAILBOX)),
			new Page("mail_use", null, List.of()),
			new Page("desk", null, List.of(OAK_DESK)),
			new Page("coffee_table", null, List.of(OAK_COFFEE_TABLE)),
			new Page("side_table", null, List.of(OAK_SIDE_TABLE)),
			new Page("garden_bench", null, List.of(OAK_GARDEN_BENCH)),
			new Page("planter_box", null, List.of(OAK_PLANTER_BOX)),
			new Page("wine_rack", null, List.of(OAK_WINE_RACK)),
			new Page("floor_lamp", null, List.of(FLOOR_LAMP)),
			new Page("piano", null, List.of(PIANO)),
			new Page("safe", null, List.of(SAFE)),
			new Page("awning", null, List.of(WHITE_AWNING)));
}
