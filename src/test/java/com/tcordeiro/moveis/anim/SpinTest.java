package com.tcordeiro.moveis.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pás do ventilador: aceleram, a velocidade segue o sinal e param alinhadas com o modelo (múltiplo de 90°). */
class SpinTest {
	@Test
	void acceleratesToTheRequestedSpeed() {
		Spin s = new Spin();
		s.tick(1f);
		assertEquals(Spin.ACCEL, s.speed(), 1e-6);
		for (int i = 0; i < 100; i++) s.tick(1f);
		assertEquals(1f, s.speed(), 1e-6);
		for (int i = 0; i < 100; i++) s.tick(0.4f); // sinal 6 de 15
		assertEquals(0.4f, s.speed(), 1e-6, "mais fraco o sinal, mais devagar");
	}

	@Test
	void stopsOnAMultipleOf90WithoutGoingBack() {
		Spin s = new Spin();
		for (int i = 0; i < 37; i++) s.tick(1f);
		float last = s.angle(1f);
		float total = 0;
		for (int i = 0; i < Spin.BRAKE_TICKS + 2; i++) {
			s.tick(0f);
			float a = s.angle(1f);
			float d = a - last;
			if (d < -180) d += 360; // deu a volta
			assertTrue(d >= -1e-4, "a travar não roda para trás");
			total += d;
			last = a;
		}
		assertTrue(s.stopped());
		float a = s.angle(1f);
		assertEquals(0f, a % 90f, 1e-3, "para onde o modelo estático tem as pás");
		assertTrue(total > 0, "ainda andou a travar");
	}

	@Test
	void interpolatesBetweenTicks() {
		Spin s = new Spin();
		for (int i = 0; i < 40; i++) s.tick(1f);
		float a0 = s.angle(0f), a1 = s.angle(1f), mid = s.angle(0.5f);
		assertEquals((a0 + a1) / 2, mid, 1e-4);
	}

	@Test
	void stoppedFanStaysStill() {
		Spin s = new Spin();
		s.tick(0f);
		assertTrue(s.stopped());
		assertEquals(0f, s.angle(0.5f), 1e-6);
	}
}
