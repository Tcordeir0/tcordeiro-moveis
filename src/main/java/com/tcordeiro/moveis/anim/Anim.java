package com.tcordeiro.moveis.anim;

/**
 * Abrir/fechar suave, como a tampa do baú: o progresso anda {@link #TICKS} ticks até ao alvo e desenha-se interpolado com o
 * partialTick (sem saltos entre ticks). Só lógica (sem classes do jogo): corre no servidor (para saber quando acabou) e no cliente
 * (para desenhar). Sem alocações.
 */
public final class Anim {
	/** Ticks a abrir e a fechar (5 = 1/4 s). */
	public static final int TICKS = 5;

	private int prev, cur;
	private boolean target;

	public Anim(boolean open) {
		target = open;
		prev = cur = open ? TICKS : 0;
	}

	/** Um tick para o alvo (aberta ou fechada). */
	public void tick(boolean open) {
		target = open;
		prev = cur;
		if (open && cur < TICKS) cur++;
		else if (!open && cur > 0) cur--;
	}

	/** 0 = fechada, 1 = aberta; interpolado entre o tick anterior e o atual. */
	public float progress(float partialTick) {
		return (prev + (cur - prev) * partialTick) / TICKS;
	}

	/** Parada no alvo (já não há nada a desenhar a mexer). */
	public boolean settled() {
		return prev == cur && cur == (target ? TICKS : 0);
	}

	public boolean target() {
		return target;
	}

	/** Como a tampa do baú: arranca depressa e trava no fim (1 − (1 − t)³). */
	public static float ease(float t) {
		float u = 1 - Math.max(0, Math.min(1, t));
		return 1 - u * u * u;
	}

	/** Interpolação linear (a mesma do jogo). */
	public static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}
}
