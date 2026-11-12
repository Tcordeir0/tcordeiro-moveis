package com.tcordeiro.moveis.finish.sawmill;

import com.tcordeiro.moveis.finish.FinishBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Só existe para o BlockEntityRenderer desenhar a serra a girar (sem dados). */
public class SawmillBlockEntity extends BlockEntity {
	public SawmillBlockEntity(BlockPos pos, BlockState state) {
		super(FinishBlocks.SAWMILL_BE, pos, state);
	}
}
