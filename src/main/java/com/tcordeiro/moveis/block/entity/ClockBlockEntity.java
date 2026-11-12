package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Só existe para o renderer dos ponteiros (não guarda nada, não tem tick). */
public class ClockBlockEntity extends BlockEntity {
	public ClockBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.CLOCK_BE, pos, state);
	}
}
