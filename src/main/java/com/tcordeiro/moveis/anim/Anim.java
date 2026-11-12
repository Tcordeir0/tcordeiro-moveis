package com.tcordeiro.moveis.anim;

/**
 * Abrir/fechar suave, como a tampa do baú: o progresso anda {@link #TICKS} ticks até ao alvo e desenha-se interpolado com o
 * partialTick (sem saltos entre ticks). Só lógica (sem classes do jogo): corre no servidor (para saber quando acabou) e no cliente
 * (para desenhar). Sem alocações.
 */
public final class Anim {
	/** Ticks a abrir e a fechar (5 = 1/4 s). */
	public static final int TICKS = 5;
	/**
	 * Ticks de graça (0.3.3): depois de parar, o renderer ainda desenha as partes este tempo. O modelo estático do chunk só se refaz no
	 * fotograma seguinte; sem a graça, as partes sumiam durante 1 fotograma no fim de cada animação.
	 */
	public static final int GRACE = 3;

	private int prev, cur;
	private boolean target;
	/** Ticks desde que parou (GRACE + 1 = parada há muito; começa assim: uma entidade nova não desenha nada). */
	private int still = GRACE + 1;

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
		still = settled() ? Math.min(GRACE + 1, still + 1) : 0;
	}

	/**
	 * A mexer para o alvo {@code open}? (o alvo vem do estado do bloco: logo que muda, mesmo antes do tick, já conta como a mexer).
	 * É o que escolhe o modelo do chunk (só o corpo a mexer, tudo parado) — 0.3.3, em vez da propriedade MOVING do blockstate.
	 */
	public boolean moving(boolean open) {
		return !(prev == cur && cur == (open ? TICKS : 0));
	}

	/** O renderer tem de desenhar as partes? A mexer, ou parada há {@link #GRACE} ticks ou menos. */
	public boolean drawing(boolean open) {
		return moving(open) || still <= GRACE;
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
