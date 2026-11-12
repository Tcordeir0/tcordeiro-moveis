package com.tcordeiro.moveis.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Teclado do piano: posição da tecla → nota (graves à esquerda de quem toca = x alto) e pitch do bloco musical. */
class PianoMathTest {
	static float centerX(int key) {
		return (FurnitureParts.PIANO_KEYS[key][1] + FurnitureParts.PIANO_KEYS[key][2]) / 2;
	}

	@Test
	void twentyThreeKeysInChromaticOrder() {
		assertEquals(23, PianoMath.keyCount());
		int blacks = 0;
		for (int k = 0; k < PianoMath.keyCount(); k++) if (PianoMath.black(k)) blacks++;
		assertEquals(9, blacks, "14 brancas e 9 pretas");
		assertTrue(!PianoMath.black(0) && PianoMath.black(1) && !PianoMath.black(2), "Sol, Sol#, Lá…");
		for (int k = 1; k < PianoMath.keyCount(); k++) assertTrue(centerX(k) < centerX(k - 1), "cada tecla mais aguda fica mais à direita (x menor)");
	}

	@Test
	void clickingAKeyGivesThatKey() {
		for (int k = 0; k < PianoMath.keyCount(); k++) {
			float y = PianoMath.black(k) ? FurnitureParts.BLACK_Y2 : FurnitureParts.WHITE_Y2;
			float z = PianoMath.black(k) ? 6f : 3f; // as pretas só vão até meio do teclado
			assertEquals(k, PianoMath.keyAt(centerX(k), y, z), "tecla " + k);
		}
	}

	@Test
	void frontOfTheKeyboardUnderABlackKeyIsWhite() {
		float x = centerX(1); // Sol#3, entre Sol e Lá
		assertEquals(1, PianoMath.keyAt(x, FurnitureParts.BLACK_Y2, 6f), "em cima, atrás: a preta");
		int k = PianoMath.keyAt(x, FurnitureParts.WHITE_Y2, 3f);
		assertTrue(k == 0 || k == 2, "à frente (onde as pretas não chegam): uma das brancas ao lado, deu " + k);
		assertEquals(-1, PianoMath.keyAt(16, 20, 12), "na caixa do piano não há tecla");
	}

	@Test
	void notesAndPitchLikeTheNoteBlock() {
		assertEquals(1, PianoMath.note(0), "a tecla mais grave é Sol3 = nota 1");
		assertEquals(23, PianoMath.note(22));
		assertEquals(1f, PianoMath.pitch(12), 1e-6, "nota 12 (Fá#4) = pitch 1");
		assertEquals(0.5f, PianoMath.pitch(0), 1e-6);
		assertEquals(2f, PianoMath.pitch(24), 1e-6);
		for (int k = 0; k < PianoMath.keyCount(); k++) {
			float p = PianoMath.pitch(PianoMath.note(k));
			assertTrue(p >= 0.5f && p <= 2f, "dentro do que o jogo toca");
		}
	}

	@Test
	void keySinksAndComesBack() {
		assertEquals(1f, PianoMath.sink(0), 1e-6);
		assertEquals(1f, PianoMath.sink(1.9f), 1e-6);
		assertEquals(0.5f, PianoMath.sink(3.5f), 1e-6);
		assertEquals(0f, PianoMath.sink(5), 1e-6);
		assertEquals(0f, PianoMath.sink(-1), 1e-6);
	}

	/** 0.3.3: o teclado continua desenhado GRACE ticks depois de PLAYING desligar. */
	@Test
	void keysKeepDrawingForAGrace() {
		org.junit.jupiter.api.Assertions.assertTrue(PianoMath.drawsKeys(true, 100, 6));
		org.junit.jupiter.api.Assertions.assertTrue(PianoMath.drawsKeys(false, 6 + Anim.GRACE, 6));
		org.junit.jupiter.api.Assertions.assertFalse(PianoMath.drawsKeys(false, 6 + Anim.GRACE + 1, 6));
		org.junit.jupiter.api.Assertions.assertFalse(PianoMath.drawsKeys(false, Long.MAX_VALUE / 2, 6), "nunca tocado");
	}
}
