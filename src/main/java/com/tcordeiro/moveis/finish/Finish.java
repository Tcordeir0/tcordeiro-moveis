package com.tcordeiro.moveis.finish;

import net.minecraft.util.StringRepresentable;

/**
 * Acabamento da madeira: crua, gasta (1–3, só a madeira crua dos Móveis ao ar livre), envernizada num dos 7 tons, ou polida.
 * É o valor da propriedade «finish» dos blocos novos e dos móveis; a ordem é a de tools/gen_finishes.py (FINISHES) e a primeira
 * (RAW) é a de omissão do blockstate.
 */
public enum Finish implements StringRepresentable {
	RAW(null, false, 0), WORN_1(null, false, 1), WORN_2(null, false, 2), WORN_3(null, false, 3),
	NATURAL(Tone.NATURAL, false, 0), NATURAL_POLISHED(Tone.NATURAL, true, 0),
	HONEY(Tone.HONEY, false, 0), HONEY_POLISHED(Tone.HONEY, true, 0),
	WALNUT(Tone.WALNUT, false, 0), WALNUT_POLISHED(Tone.WALNUT, true, 0),
	MAHOGANY(Tone.MAHOGANY, false, 0), MAHOGANY_POLISHED(Tone.MAHOGANY, true, 0),
	EBONY(Tone.EBONY, false, 0), EBONY_POLISHED(Tone.EBONY, true, 0),
	WHITE_LACQUER(Tone.WHITE_LACQUER, false, 0), WHITE_LACQUER_POLISHED(Tone.WHITE_LACQUER, true, 0),
	BLACK_LACQUER(Tone.BLACK_LACQUER, false, 0), BLACK_LACQUER_POLISHED(Tone.BLACK_LACQUER, true, 0);

	/** Tom do verniz (null = crua ou gasta). */
	public final Tone tone;
	public final boolean polished;
	/** Estágio de desgaste: 0 (crua ou envernizada) a 3. */
	public final int wear;
	private final String id;

	Finish(Tone tone, boolean polished, int wear) {
		this.tone = tone;
		this.polished = polished;
		this.wear = wear;
		this.id = name().toLowerCase(java.util.Locale.ROOT);
	}

	public boolean varnished() {
		return tone != null;
	}

	public static Finish of(Tone tone, boolean polished) {
		for (Finish f : values()) if (f.tone == tone && f.polished == polished) return f;
		throw new IllegalArgumentException(tone + " " + polished);
	}

	public static Finish worn(int stage) {
		return switch (stage) {
			case 0 -> RAW;
			case 1 -> WORN_1;
			case 2 -> WORN_2;
			case 3 -> WORN_3;
			default -> throw new IllegalArgumentException("desgaste " + stage);
		};
	}

	public static Finish byId(String id) {
		for (Finish f : values()) if (f.id.equals(id)) return f;
		return null;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
