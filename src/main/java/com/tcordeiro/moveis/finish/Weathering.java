package com.tcordeiro.moveis.finish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Desgaste da madeira crua DOS MÓVEIS (blocos novos e móveis) ao ar livre: com chuva a cair no bloco (céu por cima), um random tick
 * tem {@link FinishRules#CHANCE} de a passar ao estágio seguinte (gasta 1 → 2 → 3). Envernizada nunca; a madeira do jogo nunca
 * (não tem a propriedade, nem recebe random ticks; e {@link FinishState#with} recusa-a). Com o Clima, a estação pesa
 * ({@link FinishRules#seasonFactor}).
 * <p>
 * As classes dos blocos chamam {@link #ticks} em isRandomlyTicking e {@link #randomTick} em randomTick.
 */
public final class Weathering {
	private Weathering() {}

	/** Recebe random ticks? (só crua ou gasta 1–2: as envernizadas e as gasta 3 não pesam nos ticks do mundo) */
	public static boolean ticks(BlockState state) {
		return state.hasProperty(FinishState.FINISH) && FinishRules.canWeather(state.getValue(FinishState.FINISH));
	}

	public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		tryAge(level, pos, state, random.nextDouble());
	}

	/** Está a chover em cima deste bloco? (chuva no bioma, céu por cima: nada a tapar) */
	public static boolean exposed(Level level, BlockPos pos) {
		return level.isRainingAt(pos.above());
	}

	/** Um random tick com este sorteio ({@code roll} em [0,1)): gasta um estágio se as regras deixarem. Devolve se mudou. */
	public static boolean tryAge(ServerLevel level, BlockPos pos, BlockState state, double roll) {
		return tryAge(level, pos, state, roll, exposed(level, pos));
	}

	/** Igual, com a exposição à chuva já decidida (os testes forçam-na: o bioma do mundo de testes pode não ter chuva). */
	public static boolean tryAge(ServerLevel level, BlockPos pos, BlockState state, double roll, boolean exposedToRain) {
		Finish current = FinishState.of(state);
		if (current == null) return false;
		boolean modWood = state.hasProperty(FinishState.FINISH);
		if (!FinishRules.ages(current, modWood, exposedToRain, ClimaCompat.seasonId(level), roll)) return false;
		BlockState next = FinishState.with(state, FinishRules.nextWear(current));
		if (next == null) return false;
		level.setBlock(pos, next, Block.UPDATE_ALL);
		return true;
	}
}
