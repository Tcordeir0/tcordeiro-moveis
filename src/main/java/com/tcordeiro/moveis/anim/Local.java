package com.tcordeiro.moveis.anim;

/**
 * De onde se clicou no bloco (fração 0–1 em x e z do mundo) para as coordenadas do modelo virado a norte, conforme para onde o
 * móvel está virado (como a rotação y dos blockstates: norte 0°, este 90°, sul 180°, oeste 270°). {@code quarter}: 0 norte, 1 este,
 * 2 sul, 3 oeste.
 */
public final class Local {
	private Local() {}

	public static double modelX(int quarter, double wx, double wz) {
		return switch (quarter & 3) {
			case 0 -> wx;
			case 1 -> wz;
			case 2 -> 1 - wx;
			default -> 1 - wz;
		};
	}

	public static double modelZ(int quarter, double wx, double wz) {
		return switch (quarter & 3) {
			case 0 -> wz;
			case 1 -> 1 - wx;
			case 2 -> 1 - wz;
			default -> wx;
		};
	}
}
