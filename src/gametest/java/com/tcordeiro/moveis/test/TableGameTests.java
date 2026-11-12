package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.TableBlock;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A mesa liga-se às vizinhas e só tem pernas nos cantos de fora. */
public class TableGameTests {
	static Block table(String wood) {
		return MoveisBlocks.wooden(Furniture.TABLE, wood);
	}

	static void put(GameTestHelper h, BlockPos p, String wood) {
		TestUtil.place(h, p, table(wood).defaultBlockState());
	}

	static void sides(GameTestHelper h, BlockPos p, boolean n, boolean e, boolean s, boolean w, int legs) {
		BlockState st = h.getBlockState(p);
		String at = " em " + p.toShortString();
		h.assertValueEqual(st.getValue(TableBlock.NORTH), n, "norte" + at);
		h.assertValueEqual(st.getValue(TableBlock.EAST), e, "este" + at);
		h.assertValueEqual(st.getValue(TableBlock.SOUTH), s, "sul" + at);
		h.assertValueEqual(st.getValue(TableBlock.WEST), w, "oeste" + at);
		h.assertValueEqual(TableBlock.legCount(st), legs, "pernas" + at);
	}

	@GameTest
	public void singleTableHasFourLegs(GameTestHelper h) {
		put(h, new BlockPos(1, 1, 1), "oak");
		sides(h, new BlockPos(1, 1, 1), false, false, false, false, 4);
		h.succeed();
	}

	@GameTest
	public void rowJoinsAndKeepsEndLegs(GameTestHelper h) {
		BlockPos a = new BlockPos(1, 1, 1), b = a.east(), c = b.east();
		put(h, a, "oak");
		put(h, b, "spruce"); // madeiras diferentes também se juntam
		put(h, c, "oak");
		sides(h, a, false, true, false, false, 2);
		sides(h, b, false, true, false, true, 0);
		sides(h, c, false, false, false, true, 2);
		// tirar a do meio separa as pontas
		h.destroyBlock(b);
		sides(h, a, false, false, false, false, 4);
		h.succeed();
	}

	@GameTest
	public void lShapeCornerHasOneLeg(GameTestHelper h) {
		BlockPos corner = new BlockPos(1, 1, 1), east = corner.east(), south = corner.south();
		put(h, corner, "cherry");
		put(h, east, "cherry");
		put(h, south, "cherry");
		sides(h, corner, false, true, true, false, 1);
		BlockState st = h.getBlockState(corner);
		h.assertTrue(TableBlock.hasLeg(st, Direction.NORTH, Direction.WEST), "o canto de fora (noroeste) tem perna");
		h.assertTrue(!TableBlock.hasLeg(st, Direction.SOUTH, Direction.EAST), "o canto de dentro não tem perna");
		sides(h, east, false, false, false, true, 2);
		sides(h, south, true, false, false, false, 2);
		h.succeed();
	}
}
