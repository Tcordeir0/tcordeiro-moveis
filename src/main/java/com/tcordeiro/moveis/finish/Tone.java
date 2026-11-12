package com.tcordeiro.moveis.finish;

/** Os 7 tons de verniz (um pincel por tom). A ordem e os ids batem com tools/gen_finishes.py (TONES). */
public enum Tone {
	NATURAL("natural"), HONEY("honey"), WALNUT("walnut"), MAHOGANY("mahogany"), EBONY("ebony"), WHITE_LACQUER("white_lacquer"),
	BLACK_LACQUER("black_lacquer");

	public final String id;

	Tone(String id) {
		this.id = id;
	}

	public static Tone byId(String id) {
		for (Tone t : values()) if (t.id.equals(id)) return t;
		return null;
	}
}
