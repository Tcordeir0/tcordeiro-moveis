package com.tcordeiro.moveis.anim;

/**
 * Teclado do piano: de um clique (coordenadas do modelo virado a norte, px, na largura de 32 do piano) para a tecla, e da tecla para a
 * nota do bloco musical (0–24, F#3–F#5) e para o «pitch» do som. As teclas vêm de pecas.py ({@link FurnitureParts#PIANO_KEYS}): graves
 * à esquerda de quem toca (x alto).
 */
public final class PianoMath {
	private PianoMath() {}

	public static int keyCount() {
		return FurnitureParts.PIANO_KEYS.length;
	}

	public static boolean black(int key) {
		return FurnitureParts.PIANO_KEYS[key][0] > 0.5f;
	}

	/** Tecla clicada (−1 se o clique não foi no teclado). As pretas ganham onde se sobrepõem às brancas. */
	public static int keyAt(float x, float y, float z) {
		float[][] k = FurnitureParts.PIANO_KEYS;
		if (y >= FurnitureParts.BLACK_Y1 - 0.2f && z >= FurnitureParts.BLACK_Z1 - 0.2f && z <= FurnitureParts.BLACK_Z2 + 0.2f)
			for (int i = 0; i < k.length; i++)
				if (k[i][0] > 0.5f && x >= k[i][1] - 0.1f && x <= k[i][2] + 0.1f) return i;
		if (y < FurnitureParts.WHITE_Y1 - 0.5f || z > FurnitureParts.WHITE_Z2 + 0.5f) return -1;
		for (int i = 0; i < k.length; i++)
			if (k[i][0] < 0.5f && x >= k[i][1] - 0.15f && x <= k[i][2] + 0.15f) return i;
		return -1;
	}

	/** Nota do bloco musical: a tecla 0 é Sol3 (nota 1); sobe meio-tom por tecla. */
	public static int note(int key) {
		return key + 1;
	}

	/** Pitch do som, como o bloco musical: 2^((nota − 12) / 12). */
	public static float pitch(int note) {
		return (float) Math.pow(2, (note - 12) / 12.0);
	}

	/** Quanto a tecla está afundada (0–1) {@code t} ticks depois de tocada: desce logo, fica 2 ticks e sobe em 3. */
	public static float sink(float t) {
		if (t < 0 || t >= 5) return 0;
		if (t < 2) return 1;
		return 1 - (t - 2) / 3f;
	}

	/**
	 * O renderer desenha as teclas? A tocar ({@code playing}, o estado PLAYING), ou até {@code playTicks} + {@link Anim#GRACE} ticks depois
	 * da última tecla (0.3.3: quando PLAYING desliga, o modelo estático com as teclas só aparece no fotograma seguinte — sem esta graça
	 * o teclado sumia entre teclas tocadas com mais de {@code playTicks} de intervalo).
	 */
	public static boolean drawsKeys(boolean playing, long sinceLastKey, int playTicks) {
		return playing || (sinceLastKey >= 0 && sinceLastKey <= playTicks + Anim.GRACE);
	}
}
