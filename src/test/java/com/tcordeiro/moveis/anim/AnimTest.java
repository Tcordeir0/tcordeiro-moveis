package com.tcordeiro.moveis.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Interpolação da animação de abrir/fechar (lerp por tick com o partialTick). */
class AnimTest {
	@Test
	void opensInFiveTicksAndInterpolates() {
		Anim a = new Anim(false);
		assertEquals(0f, a.progress(0.5f), 1e-6);
		assertTrue(a.settled());
		a.tick(true);
		assertFalse(a.settled());
		assertEquals(0f, a.progress(0f), 1e-6, "no início do tick ainda está fechada");
		assertEquals(0.1f, a.progress(0.5f), 1e-6, "a meio do primeiro tick: metade de 1/5");
		assertEquals(0.2f, a.progress(1f), 1e-6);
		for (int i = 0; i < Anim.TICKS - 1; i++) a.tick(true);
		assertEquals(1f, a.progress(1f), 1e-6, "aberta ao fim de 5 ticks");
		assertFalse(a.settled(), "ainda a acabar (o tick anterior não estava no fim)");
		a.tick(true);
		assertTrue(a.settled());
		assertEquals(1f, a.progress(0.3f), 1e-6);
	}

	@Test
	void closesBackFromHalfway() {
		Anim a = new Anim(false);
		a.tick(true);
		a.tick(true); // 2/5
		a.tick(false); // volta: 1/5
		assertEquals(0.4f, a.progress(0f), 1e-6);
		assertEquals(0.2f, a.progress(1f), 1e-6);
		a.tick(false);
		a.tick(false);
		assertTrue(a.settled());
		assertEquals(0f, a.progress(0.7f), 1e-6);
	}

	@Test
	void progressIsMonotonicWhileOpening() {
		Anim a = new Anim(false);
		float last = -1;
		for (int t = 0; t < Anim.TICKS; t++) {
			a.tick(true);
			for (float pt = 0; pt <= 1f; pt += 0.25f) {
				float p = a.progress(pt);
				assertTrue(p >= last - 1e-6, "não anda para trás");
				last = p;
			}
		}
	}

	@Test
	void easeLikeTheChestLid() {
		assertEquals(0f, Anim.ease(0f), 1e-6);
		assertEquals(1f, Anim.ease(1f), 1e-6);
		assertEquals(1 - 0.125f, Anim.ease(0.5f), 1e-6);
		assertEquals(1f, Anim.ease(7f), 1e-6, "limitado a 0–1");
	}

	@Test
	void startsWhereTheBlockIs() {
		Anim open = new Anim(true);
		assertTrue(open.settled());
		assertEquals(1f, open.progress(0.5f), 1e-6);
	}
}
