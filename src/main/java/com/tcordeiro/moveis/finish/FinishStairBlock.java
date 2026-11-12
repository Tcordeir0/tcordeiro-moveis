package com.tcordeiro.moveis.finish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Escadas dos blocos novos, com acabamento e desgaste. */
public class FinishStairBlock extends StairBlock {
	public FinishStairBlock(BlockState base, Properties props) {
		super(base, props);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FinishState.FINISH);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return Weathering.ticks(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Weathering.randomTick(state, level, pos, random);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return FinishItems.stack(state);
	}
}
