package com.tcordeiro.moveis.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Clique no mundo → coordenadas do modelo virado a norte, para as 4 rotações dos blockstates. */
class LocalTest {
	@Test
	void frontCenterIsAlwaysModelFront() {
		// a frente do modelo é z = 0 (norte); virado para cada lado, a frente fica nesse lado do bloco
		double[][] front = {{0.5, 0}, {1, 0.5}, {0.5, 1}, {0, 0.5}}; // norte, este, sul, oeste
		for (int q = 0; q < 4; q++) {
			assertEquals(0.5, Local.modelX(q, front[q][0], front[q][1]), 1e-9, "x, quarto " + q);
			assertEquals(0, Local.modelZ(q, front[q][0], front[q][1]), 1e-9, "z, quarto " + q);
		}
	}

	@Test
	void modelWestSideRotatesClockwise() {
		// o lado x = 0 do modelo (oeste a norte) roda no sentido dos ponteiros como os blockstates: norte, este → sul…
		assertEquals(0, Local.modelX(0, 0, 0.5), 1e-9);
		assertEquals(0, Local.modelX(1, 0.5, 0), 1e-9, "virado a este, o x = 0 do modelo está a norte");
		assertEquals(0, Local.modelX(2, 1, 0.5), 1e-9);
		assertEquals(0, Local.modelX(3, 0.5, 1), 1e-9);
	}
}
