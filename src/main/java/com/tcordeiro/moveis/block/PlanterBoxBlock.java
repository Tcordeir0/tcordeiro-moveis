package com.tcordeiro.moveis.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Floreira: flores, mudas e sementes de plantações crescem em cima dela (está nas tags supports_crops, supports_vegetation,
 * supports_stem_crops e grows_crops, como a terra arada). Tem a humidade da terra arada sempre no máximo ({@link #MOISTURE} = 7), por
 * isso as plantações crescem ao ritmo da terra regada. As árvores não a trocam por terra (cannot_replace_below_tree_trunk).
 */
public class PlanterBoxBlock extends Block {
	public static final IntegerProperty MOISTURE = FarmlandBlock.MOISTURE;

	public PlanterBoxBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(MOISTURE, FarmlandBlock.MAX_MOISTURE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MOISTURE);
		com.tcordeiro.moveis.finish.FinishState.addIfWood(builder);
	}

	// 0.3.0: a madeira crua dos móveis gasta-se à chuva (só os de madeira têm «finish»: Weathering.ticks devolve false nos outros)
	@Override
	protected boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState state) {
		return com.tcordeiro.moveis.finish.Weathering.ticks(state);
	}

	@Override
	protected void randomTick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.server.level.ServerLevel level,
			net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
		com.tcordeiro.moveis.finish.Weathering.randomTick(state, level, pos, random);
	}
}
