package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.anim.Anim;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Entidade só para a animação (metade de cima do guarda-roupa, cortinas, persianas): não guarda nada. */
public class AnimatedBlockEntity extends BlockEntity implements Animated {
	private final Anim anim;

	public AnimatedBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.ANIM_BE, pos, state);
		anim = new Anim(AnimatedFurniture.initialTarget(state));
	}

	@Override
	public Anim anim() {
		return anim;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AnimatedBlockEntity be) {
		AnimatedFurniture.tickAnim(level, pos, state, be.anim);
	}
}
