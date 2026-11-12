package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.block.entity.ClockBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Relógio de parede: o bloco é o mostrador; os ponteiros desenha-os o renderer com a hora do mundo (ClockMath). */
public class WallClockBlock extends WallDecorBlock implements EntityBlock {
	public WallClockBlock(VoxelShape northShape, Properties props) {
		super(northShape, props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ClockBlockEntity(pos, state);
	}
}
