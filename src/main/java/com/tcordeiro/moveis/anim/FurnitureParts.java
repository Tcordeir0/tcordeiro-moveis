package com.tcordeiro.moveis.anim;

import java.util.List;
import java.util.Map;

/**
 * As partes móveis dos móveis animados e onde rodam/deslizam (px; 16 = 1 bloco; modelo virado a norte). Vêm de
 * tools/blender/pecas.py (ANIM, PIANO_KEYS, WINE_CELLS): GERADO por tools/gen_data.py, não editar à mão.
 * Cada parte é desenhada pelo renderer com o modelo {@code tmoveis:part/<bloco>/<parte>} (o mesmo template do bloco).
 */
public final class FurnitureParts {
	private FurnitureParts() {}

	/** ROT: roda {@code amount} graus (aberta) à volta do eixo no pivô; MOVE: desliza (px, py, pz); SCALEX: encolhe em x para px até
	 *  {@code amount}; SPIN: gira sem parar; CLOCK_*: ponteiros do relógio. */
	public enum Op { ROT, MOVE, SCALEX, SPIN, CLOCK_HOUR, CLOCK_MINUTE }

	public record Part(String model, Op op, char axis, float px, float py, float pz, float amount) {}

	/** Tipo de peça → partes (pela ordem de pecas.ANIM). */
	public static final Map<String, List<Part>> BY_KIND = Map.ofEntries(
			Map.entry("cabinet", List.of(new Part("cabinet_door_left", Op.ROT, 'y', 1.5f, 0.0f, 0.5f, 100.0f), new Part("cabinet_door_right", Op.ROT, 'y', 14.5f, 0.0f, 0.5f, -100.0f))),
			Map.entry("ceiling_fan", List.of(new Part("ceiling_fan_blades", Op.SPIN, 'y', 8.0f, 0.0f, 8.0f, 1.0f))),
			Map.entry("counter", List.of(new Part("counter_door_left", Op.ROT, 'y', 1.0f, 0.0f, 0.5f, 100.0f), new Part("counter_door_right", Op.ROT, 'y', 15.0f, 0.0f, 0.5f, -100.0f))),
			Map.entry("counter_corner", List.of(new Part("counter_corner_door", Op.ROT, 'y', 1.0f, 0.0f, 0.5f, 100.0f))),
			Map.entry("curtain", List.of(new Part("curtain_left", Op.SCALEX, 'x', 0.0f, 0.0f, 0.0f, 0.375f), new Part("curtain_right", Op.SCALEX, 'x', 16.0f, 0.0f, 0.0f, 0.375f))),
			Map.entry("desk", List.of(new Part("desk_drawer", Op.MOVE, 'x', 0.0f, 0.0f, -6.0f, 1.0f))),
			Map.entry("drawer", List.of(new Part("drawer_drawer", Op.MOVE, 'x', 0.0f, 0.0f, -6.0f, 1.0f))),
			Map.entry("mailbox", List.of(new Part("mailbox_flag", Op.ROT, 'x', 12.25f, 12.5f, 9.5f, 90.0f))),
			Map.entry("nightstand", List.of(new Part("nightstand_drawer", Op.MOVE, 'x', 0.0f, 0.0f, -6.0f, 1.0f))),
			Map.entry("safe", List.of(new Part("safe_door", Op.ROT, 'y', 2.0f, 0.0f, 0.5f, 100.0f))),
			Map.entry("shutters", List.of(new Part("shutters_left", Op.ROT, 'y', 0.5f, 0.0f, 15.5f, 90.0f), new Part("shutters_right", Op.ROT, 'y', 15.5f, 0.0f, 15.5f, -90.0f))),
			Map.entry("trash_can", List.of(new Part("trash_can_lid", Op.ROT, 'x', 8.0f, 12.0f, 12.5f, 100.0f))),
			Map.entry("wall_clock", List.of(new Part("wall_clock_hour", Op.CLOCK_HOUR, 'z', 8.0f, 8.0f, 0.0f, 1.0f), new Part("wall_clock_minute", Op.CLOCK_MINUTE, 'z', 8.0f, 8.0f, 0.0f, 1.0f))),
			Map.entry("wardrobe_bottom", List.of(new Part("wardrobe_bottom_door_left", Op.ROT, 'y', 1.5f, 0.0f, 0.5f, 100.0f), new Part("wardrobe_bottom_door_right", Op.ROT, 'y', 14.5f, 0.0f, 0.5f, -100.0f))),
			Map.entry("wardrobe_top", List.of(new Part("wardrobe_top_door_left", Op.ROT, 'y', 1.5f, 0.0f, 0.5f, 100.0f), new Part("wardrobe_top_door_right", Op.ROT, 'y', 14.5f, 0.0f, 0.5f, -100.0f))));

	/** Teclas do piano pela ordem cromática (Sol3 = tecla 0 = nota 1 do bloco musical): {preta?, x1, x2} na largura de 32 px. */
	public static final float[][] PIANO_KEYS = {
			{0, 28.95714285714286f, 30.9f},
			{1, 28.257142857142856f, 29.45714285714286f},
			{0, 26.814285714285717f, 28.757142857142856f},
			{1, 26.114285714285714f, 27.314285714285717f},
			{0, 24.67142857142857f, 26.614285714285714f},
			{0, 22.528571428571432f, 24.471428571428568f},
			{1, 21.82857142857143f, 23.028571428571432f},
			{0, 20.385714285714286f, 22.32857142857143f},
			{1, 19.685714285714283f, 20.885714285714286f},
			{0, 18.242857142857144f, 20.185714285714283f},
			{0, 16.1f, 18.04285714285714f},
			{1, 15.4f, 16.6f},
			{0, 13.957142857142857f, 15.9f},
			{1, 13.257142857142858f, 14.457142857142857f},
			{0, 11.814285714285715f, 13.757142857142858f},
			{1, 11.114285714285716f, 12.314285714285715f},
			{0, 9.671428571428573f, 11.614285714285716f},
			{0, 7.52857142857143f, 9.471428571428573f},
			{1, 6.828571428571431f, 8.02857142857143f},
			{0, 5.385714285714284f, 7.328571428571431f},
			{1, 4.685714285714285f, 5.885714285714284f},
			{0, 3.2428571428571424f, 5.185714285714285f},
			{0, 1.1f, 3.0428571428571423f}};
	public static final float WHITE_Y1 = 12.0f, WHITE_Y2 = 13.5f, BLACK_Y1 = 13.5f, BLACK_Y2 = 14.3f;
	public static final float WHITE_Z1 = 2.0f, WHITE_Z2 = 8.0f, BLACK_Z1 = 4.5f, BLACK_Z2 = 8.0f;
	public static final float WHITE_SINK = 0.7f, BLACK_SINK = 0.5f;

	/** Deslocamento (px) da garrafa da casa 0 para cada uma das 9 casas da adega (0 = em cima à esquerda, vista de frente). */
	public static final float[][] WINE_CELLS = {{9.700000000000001f, 9.700000000000001f}, {4.8f, 9.700000000000001f}, {0.0f, 9.700000000000001f}, {9.700000000000001f, 4.8f}, {4.8f, 4.8f}, {0.0f, 4.8f}, {9.700000000000001f, 0.0f}, {4.8f, 0.0f}, {0.0f, 0.0f}};
}
