package com.tcordeiro.moveis.finish;

/**
 * As regras do verniz e do desgaste, sem nada do Minecraft (testadas em JUnit): o que o pincel, o machado e a chuva fazem a cada
 * acabamento, e com que probabilidade a madeira crua se gasta.
 */
public final class FinishRules {
	private FinishRules() {}

	/**
	 * Probabilidade de um random tick, com chuva e céu por cima, gastar um estágio. Com randomTickSpeed 3 e a chuva do jogo (~16% do
	 * tempo) dá ~9 dias de jogo por estágio — ver {@link #averageDaysPerStage}.
	 */
	public static final double CHANCE = 0.04;
	/** Fração do tempo com chuva no tempo do jogo: chuva 12000–24000 ticks, céu limpo 12000–180000 (médias 18000 e 96000). */
	public static final double VANILLA_RAIN_FRACTION = 18000.0 / (18000.0 + 96000.0);

	/**
	 * Pincel do tom {@code tone} num bloco com {@code current}: crua ou gasta → envernizada; envernizada do mesmo tom → polida;
	 * envernizada (ou polida) de outro tom → o tom novo. Devolve null quando não muda nada (polida do mesmo tom).
	 */
	public static Finish brush(Finish current, Tone tone) {
		if (current.tone == tone) return current.polished ? null : Finish.of(tone, true);
		return Finish.of(tone, false);
	}

	/** Machado: polida → envernizada → crua; gasta → um estágio a menos. Null = nada a raspar (crua). */
	public static Finish axe(Finish current) {
		if (current.varnished()) return current.polished ? Finish.of(current.tone, false) : Finish.RAW;
		if (current.wear > 0) return Finish.worn(current.wear - 1);
		return null;
	}

	/** O próximo estágio de desgaste (null = não se gasta: envernizada, ou já no último estágio). */
	public static Finish nextWear(Finish current) {
		if (current.varnished() || current.wear >= 3) return null;
		return Finish.worn(current.wear + 1);
	}

	/** Este acabamento ainda se pode gastar? (decide se o bloco recebe random ticks) */
	public static boolean canWeather(Finish current) {
		return nextWear(current) != null;
	}

	/**
	 * Multiplicador da estação do mod Clima (id de ClimaApi.seasonId; null = sem Clima): o outono húmido gasta mais, o verão seca a
	 * madeira depressa e gasta menos.
	 */
	public static double seasonFactor(String seasonId) {
		if (seasonId == null) return 1.0;
		return switch (seasonId) {
			case "autumn" -> 1.25;
			case "summer" -> 0.75;
			default -> 1.0;
		};
	}

	/**
	 * Gasta neste random tick? {@code modWood}: o bloco é madeira dos Móveis (a do jogo NUNCA se gasta: as casas que já existem não
	 * mudam sozinhas). {@code roll} uniforme em [0,1).
	 */
	public static boolean ages(Finish current, boolean modWood, boolean exposedToRain, String seasonId, double roll) {
		if (!modWood) return false;
		if (!exposedToRain) return false;
		if (nextWear(current) == null) return false; // envernizada (protegida) ou já no fim
		return roll < CHANCE * seasonFactor(seasonId);
	}

	/**
	 * Média de dias de jogo por estágio: random ticks por bloco e por dia (24000 × speed / 4096), só os que calham com chuva contam.
	 */
	public static double averageDaysPerStage(double chance, int randomTickSpeed, double rainFraction) {
		double ticksPerDay = 24000.0 * randomTickSpeed / 4096.0;
		return 1.0 / (ticksPerDay * rainFraction * chance);
	}
}
