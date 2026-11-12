package com.tcordeiro.moveis.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Hora do mundo → ângulo dos ponteiros (0 ticks = 06:00; 1000 ticks = 1 h). */
class ClockMathTest {
	@Test
	void sixInTheMorningAtTickZero() {
		assertEquals(6.0, ClockMath.hours(0), 1e-9);
		assertEquals(180f, ClockMath.hourAngle(0), 1e-4, "6 h: ponteiro das horas para baixo");
		assertEquals(0f, ClockMath.minuteAngle(0), 1e-4);
	}

	@Test
	void noonAndMidnight() {
		assertEquals(0f, ClockMath.hourAngle(6000), 1e-4, "meio-dia: para cima");
		assertEquals(0f, ClockMath.minuteAngle(6000), 1e-4);
		assertEquals(0f, ClockMath.hourAngle(18000), 1e-4, "meia-noite: para cima");
	}

	@Test
	void halfPastSeven() {
		assertEquals(225f, ClockMath.hourAngle(1500), 1e-3, "7h30: entre o 7 e o 8");
		assertEquals(180f, ClockMath.minuteAngle(1500), 1e-3, "30 min: para baixo");
	}

	@Test
	void quarterPastThreeInTheAfternoon() {
		// 15h15 = 9h15 depois das 6h = 9250 ticks
		assertEquals(97.5f, ClockMath.hourAngle(9250), 1e-3);
		assertEquals(90f, ClockMath.minuteAngle(9250), 1e-3);
	}

	@Test
	void wrapsAroundDaysAndNegativeTime() {
		assertEquals(ClockMath.hourAngle(1500), ClockMath.hourAngle(1500 + 24000 * 7L), 1e-3);
		assertEquals(150f, ClockMath.hourAngle(-1000), 1e-3, "uma hora antes das 6: 5 h");
	}
}
