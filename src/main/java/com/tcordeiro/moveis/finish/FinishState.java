package com.tcordeiro.moveis.finish;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Ler e trocar o acabamento de um bloco, seja ele qual for: os blocos dos Móveis (blocos novos e móveis) guardam-no na propriedade
 * {@link #FINISH}; as tábuas, escadas e lajes do jogo não podem ter propriedades novas, por isso cada versão envernizada é um bloco
 * próprio ({@link FinishBlocks#VARNISHED}). A troca mantém todos os outros estados (escadas: facing/half/shape; lajes: type;
 * waterlogged; móveis: facing, open, ligações…).
 */
public final class FinishState {
	private FinishState() {}

	public static final EnumProperty<Finish> FINISH = EnumProperty.create("finish", Finish.class);

	/** O acabamento deste bloco, ou null se não leva verniz (portas, cercas, pedra…). */
	public static Finish of(BlockState state) {
		if (state.hasProperty(FINISH)) return state.getValue(FINISH);
		FinishBlocks.Varnished v = FinishBlocks.VARNISHED.get(state.getBlock());
		if (v != null) return v.finish();
		return FinishBlocks.BY_VANILLA.containsKey(state.getBlock()) ? Finish.RAW : null;
	}

	/** O mesmo bloco com outro acabamento (mesma madeira, mesma forma, mesmos estados), ou null se não pode ter esse acabamento. */
	public static BlockState with(BlockState state, Finish finish) {
		if (finish == null) return null;
		if (state.hasProperty(FINISH)) return state.setValue(FINISH, finish);
		Block vanilla = vanillaOf(state.getBlock());
		if (vanilla == null) return null;
		if (finish.wear > 0) return null; // a madeira do jogo nunca fica gasta
		Block target = finish == Finish.RAW ? vanilla : FinishBlocks.BY_VANILLA.get(vanilla).get(finish);
		return target == null ? null : copyProperties(state, target.defaultBlockState());
	}

	/** A tábua/escada/laje do jogo por trás deste bloco (ele próprio, ou a base de uma versão envernizada); null se não é uma delas. */
	public static Block vanillaOf(Block block) {
		if (FinishBlocks.BY_VANILLA.containsKey(block)) return block;
		FinishBlocks.Varnished v = FinishBlocks.VARNISHED.get(block);
		return v == null ? null : v.vanilla();
	}

	/** Copia para {@code to} todas as propriedades de {@code from} que ele também tenha. */
	public static BlockState copyProperties(BlockState from, BlockState to) {
		for (Property<?> p : from.getProperties()) if (to.hasProperty(p)) to = copy(from, to, p);
		return to;
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> p) {
		return to.setValue(p, from.getValue(p));
	}
}
