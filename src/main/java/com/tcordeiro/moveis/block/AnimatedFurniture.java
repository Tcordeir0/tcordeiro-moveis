package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.anim.Anim;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Móvel com partes que se mexem (portas, gavetas, tampas, cortinas, persianas, bandeirinha). Enquanto as partes andam, o modelo
 * estático do chunk é só o corpo e o renderer desenha as partes (lerp por tick); parado, o modelo estático tem tudo e o renderer não
 * corre.
 * <p>0.3.3: «a mexer» já não é uma propriedade do blockstate (MOVING duplicava os estados de cada móvel destes): vem da animação da
 * entidade ({@link Anim#moving}), nos dois lados. No cliente, a entidade entrega-a ao modelo do chunk ({@code getRenderData}) e, quando
 * a animação acaba, pede para refazer o chunk ({@link #tickAnim}). Os blockstates destes móveis continuam com a chave «moving» (em
 * {@code assets/tmoveis/anim_blockstates/}, lidos pelo cliente — {@code client.AnimModels}): os modelos escolhidos são os mesmos.
 */
public interface AnimatedFurniture {
	/**
	 * Só para os ficheiros {@code anim_blockstates} (o cliente monta com ela uma definição de estados falsa para os ler): os blocos NÃO
	 * a têm. Os mundos antigos com «moving» no blockstate carregam na mesma (o jogo ignora propriedades que o bloco já não tem).
	 */
	BooleanProperty MOVING = BooleanProperty.create("moving");
	BooleanProperty OPEN = BlockStateProperties.OPEN;

	/** O tipo de peça (pecas.ANIM / FurnitureParts.BY_KIND) que este estado desenha. */
	String animKind(BlockState state);

	/** O alvo da animação (aberta = 1). Por omissão, OPEN. */
	default boolean animTarget(BlockState state) {
		return state.getValue(OPEN);
	}

	/** O estado tem um modelo estático para «aberta» (cortinas, persianas): aberta e parada, o renderer não desenha. */
	default boolean hasStaticOpenModel() {
		return false;
	}

	/** As partes estão a mexer (o modelo do chunk é só o corpo)? */
	default boolean moving(BlockState state, Anim anim) {
		return anim.moving(animTarget(state));
	}

	/**
	 * O renderer tem de desenhar as partes neste estado? A mexer (e {@link Anim#GRACE} ticks depois, enquanto o chunk se refaz), ou aberta
	 * sem modelo estático de aberta.
	 */
	default boolean drawsParts(BlockState state, Anim anim) {
		return anim.drawing(animTarget(state)) || (animTarget(state) && !hasStaticOpenModel());
	}

	/** Muda uma propriedade-alvo (OPEN, HAS_MAIL…) (servidor); a animação começa sozinha nos dois lados. */
	static void setTarget(Level level, BlockPos pos, BlockState state, BooleanProperty prop, boolean value) {
		if (!state.hasProperty(prop) || state.getValue(prop) == value) return;
		level.setBlock(pos, state.setValue(prop, value), Block.UPDATE_CLIENTS);
	}

	/** Um tick da animação (nos dois lados); no cliente, quando acaba, refaz o chunk (o modelo estático volta a ter tudo). */
	static void tickAnim(Level level, BlockPos pos, BlockState state, Anim anim) {
		if (!(state.getBlock() instanceof AnimatedFurniture af)) return;
		boolean target = af.animTarget(state), was = anim.moving(target);
		anim.tick(target);
		if (level.isClientSide() && was && !anim.moving(target)) remesh(level, pos, state);
	}

	/**
	 * Cliente: o modelo do chunk deste bloco mudou (a escolha vem da entidade, não do estado). UPDATE_IMMEDIATE: a secção refaz-se já
	 * (como quando o próprio jogador mexe num bloco), e a graça do renderer ({@link Anim#GRACE}) cobre o resto.
	 */
	static void remesh(Level level, BlockPos pos, BlockState state) {
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
	}

	/** O alvo inicial de uma entidade nova (para a animação começar parada onde o bloco está). */
	static boolean initialTarget(BlockState state) {
		return state.getBlock() instanceof AnimatedFurniture af && af.animTarget(state);
	}
}
