package com.tcordeiro.moveis.finish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** As regras do verniz e do desgaste (Java puro). */
class FinishRulesTest {
	@Test
	void brushVarnishesRawAndWorn() {
		assertEquals(Finish.HONEY, FinishRules.brush(Finish.RAW, Tone.HONEY));
		for (int s = 1; s <= 3; s++) assertEquals(Finish.EBONY, FinishRules.brush(Finish.worn(s), Tone.EBONY), "gasta " + s + " + pincel = cor reposta");
	}

	@Test
	void sameToneAgainPolishes() {
		for (Tone t : Tone.values()) {
			Finish v = FinishRules.brush(Finish.RAW, t);
			Finish p = FinishRules.brush(v, t);
			assertTrue(p.polished, t + " polido");
			assertEquals(t, p.tone);
			assertNull(FinishRules.brush(p, t), "polido do mesmo tom: nada a fazer");
		}
	}

	@Test
	void otherToneRepaints() {
		assertEquals(Finish.WALNUT, FinishRules.brush(Finish.HONEY_POLISHED, Tone.WALNUT));
		assertEquals(Finish.WHITE_LACQUER, FinishRules.brush(Finish.BLACK_LACQUER, Tone.WHITE_LACQUER));
	}

	@Test
	void axeRemovesOneStage() {
		assertEquals(Finish.MAHOGANY, FinishRules.axe(Finish.MAHOGANY_POLISHED));
		assertEquals(Finish.RAW, FinishRules.axe(Finish.MAHOGANY));
		assertNull(FinishRules.axe(Finish.RAW));
		assertEquals(Finish.WORN_2, FinishRules.axe(Finish.WORN_3));
		assertEquals(Finish.WORN_1, FinishRules.axe(Finish.WORN_2));
		assertEquals(Finish.RAW, FinishRules.axe(Finish.WORN_1));
	}

	@Test
	void wearStages() {
		assertEquals(Finish.WORN_1, FinishRules.nextWear(Finish.RAW));
		assertEquals(Finish.WORN_2, FinishRules.nextWear(Finish.WORN_1));
		assertEquals(Finish.WORN_3, FinishRules.nextWear(Finish.WORN_2));
		assertNull(FinishRules.nextWear(Finish.WORN_3), "o último estágio não passa daí");
	}

	@Test
	void varnishedNeverWeathers() {
		for (Finish f : Finish.values()) {
			if (!f.varnished()) continue;
			assertNull(FinishRules.nextWear(f), f + " não se gasta");
			assertFalse(FinishRules.canWeather(f));
			assertFalse(FinishRules.ages(f, true, true, null, 0.0), f + " não se gasta nem com o sorteio mais baixo");
		}
	}

	@Test
	void vanillaWoodNeverWeathers() {
		for (Finish f : Finish.values()) assertFalse(FinishRules.ages(f, false, true, "autumn", 0.0), "madeira do jogo: " + f);
	}

	@Test
	void onlyWithRain() {
		assertFalse(FinishRules.ages(Finish.RAW, true, false, null, 0.0), "sem chuva (ou tapada) não se gasta");
		assertTrue(FinishRules.ages(Finish.RAW, true, true, null, 0.0));
		assertFalse(FinishRules.ages(Finish.RAW, true, true, null, FinishRules.CHANCE), "o sorteio tem de ficar abaixo da probabilidade");
	}

	@Test
	void probabilityAndSeasons() {
		assertTrue(FinishRules.CHANCE > 0 && FinishRules.CHANCE < 0.1, "probabilidade baixa");
		assertEquals(1.0, FinishRules.seasonFactor(null));
		assertEquals(1.25, FinishRules.seasonFactor("autumn"));
		assertEquals(0.75, FinishRules.seasonFactor("summer"));
		double roll = FinishRules.CHANCE * 1.1; // passa no outono (×1,25), não passa sem Clima nem no verão
		assertTrue(FinishRules.ages(Finish.RAW, true, true, "autumn", roll));
		assertFalse(FinishRules.ages(Finish.RAW, true, true, null, roll));
		assertFalse(FinishRules.ages(Finish.RAW, true, true, "summer", roll));
	}

	@Test
	void averageDaysPerStageIsAboutNine() {
		double d = FinishRules.averageDaysPerStage(FinishRules.CHANCE, 3, FinishRules.VANILLA_RAIN_FRACTION);
		assertTrue(d > 8 && d < 10, "≈ 9 dias de jogo por estágio, deu " + d);
	}

	@Test
	void toneAndFinishMapping() {
		assertEquals(7, Tone.values().length);
		assertEquals(4 + 14, Finish.values().length);
		assertEquals(Finish.RAW, Finish.values()[0], "crua é a de omissão (primeira)");
		for (Tone t : Tone.values()) {
			assertEquals(t, Tone.byId(t.id));
			Finish v = Finish.of(t, false), p = Finish.of(t, true);
			assertEquals(t.id, v.getSerializedName());
			assertEquals(t.id + "_polished", p.getSerializedName());
			assertEquals(v, Finish.byId(v.getSerializedName()));
		}
		for (int s = 1; s <= 3; s++) assertEquals("worn_" + s, Finish.worn(s).getSerializedName());
	}
}
