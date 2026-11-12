package com.tcordeiro.moveis.anim;

/**
 * Ponteiros do relógio de parede a partir da hora do mundo (ticks; 0 = 06:00, 1000 ticks = 1 hora, como o relógio do jogo).
 * Ângulos em graus, no sentido dos ponteiros, a partir das 12.
 */
public final class ClockMath {
	private ClockMath() {}

	/** Horas do dia (0–24, com fração) para este tempo do mundo. */
	public static double hours(double ticks) {
		return ((ticks / 1000.0 + 6) % 24 + 24) % 24;
	}

	public static float hourAngle(double ticks) {
		return (float) (hours(ticks) % 12 * 30);
	}

	public static float minuteAngle(double ticks) {
		return (float) (hours(ticks) % 1 * 360);
	}
}
