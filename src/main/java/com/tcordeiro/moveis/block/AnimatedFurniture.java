package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.anim.Anim;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Móvel com partes que se mexem (portas, gavetas, tampas, cortinas, persianas, bandeirinha). O bloco tem a propriedade
 * {@link #MOVING}: enquanto é true o modelo estático é só o corpo e o renderer desenha as partes a mexer (lerp por tick); parado,
 * o modelo estático tem tudo e o renderer não corre. O servidor liga MOVING quando o alvo muda e desliga-o quando a animação chega
 * ao fim ({@link #tickAnim}), por isso o estado do bloco diz sempre se há algo a desenhar.
 */
public interface AnimatedFurniture {
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

	/** O renderer tem de desenhar as partes neste estado? */
	default boolean drawsParts(BlockState state) {
		return state.getValue(MOVING) || (animTarget(state) && !hasStaticOpenModel());
	}

	/** Muda uma propriedade-alvo (OPEN, HAS_MAIL…) e marca MOVING (servidor). */
	static void setTarget(Level level, BlockPos pos, BlockState state, BooleanProperty prop, boolean value) {
		if (!state.hasProperty(prop) || state.getValue(prop) == value) return;
		level.setBlock(pos, state.setValue(prop, value).setValue(MOVING, true), Block.UPDATE_CLIENTS);
	}

	/** Um tick da animação (nos dois lados); no servidor, desliga MOVING quando chega ao fim. */
	static void tickAnim(Level level, BlockPos pos, BlockState state, Anim anim) {
		if (!(state.getBlock() instanceof AnimatedFurniture af)) return;
		anim.tick(af.animTarget(state));
		if (!level.isClientSide() && state.getValue(MOVING) && anim.settled())
			level.setBlock(pos, state.setValue(MOVING, false), Block.UPDATE_CLIENTS);
	}

	/** O alvo inicial de uma entidade nova (para a animação começar parada onde o bloco está). */
	static boolean initialTarget(BlockState state) {
		return state.getBlock() instanceof AnimatedFurniture af && af.animTarget(state);
	}
}
