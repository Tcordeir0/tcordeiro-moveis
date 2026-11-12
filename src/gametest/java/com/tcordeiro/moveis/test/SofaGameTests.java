package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.SofaBlock;
import com.tcordeiro.moveis.block.SofaShape;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

/** O sofá muda de forma com os vizinhos: sozinho, em fila (pontas e meio) e em L (canto de dentro e de fora). */
public class SofaGameTests {
	static BlockState sofa(DyeColor c, Direction look) {
		return MoveisBlocks.SOFAS.get(c).defaultBlockState().setValue(SofaBlock.FACING, look);
	}

	static void shape(GameTestHelper h, BlockPos p, SofaShape s) {
		h.assertValueEqual(h.getBlockState(p).getValue(SofaBlock.SHAPE), s, "forma em " + p.toShortString());
	}

	@GameTest
	public void aloneIsSingle(GameTestHelper h) {
		TestUtil.place(h, new BlockPos(1, 1, 1), sofa(DyeColor.BLUE, Direction.NORTH));
		shape(h, new BlockPos(1, 1, 1), SofaShape.SINGLE);
		h.succeed();
	}

	@GameTest
	public void rowHasArmsOnlyAtTheEnds(GameTestHelper h) {
		// virados a norte: a esquerda de quem se senta é oeste
		BlockPos w = new BlockPos(1, 1, 1), m = w.east(), e = m.east();
		TestUtil.place(h, w, sofa(DyeColor.BLUE, Direction.NORTH));
		TestUtil.place(h, m, sofa(DyeColor.RED, Direction.NORTH)); // cores diferentes juntam-se
		TestUtil.place(h, e, sofa(DyeColor.BLUE, Direction.NORTH));
		shape(h, w, SofaShape.LEFT);
		shape(h, m, SofaShape.MIDDLE);
		shape(h, e, SofaShape.RIGHT);
		h.destroyBlock(m);
		shape(h, w, SofaShape.SINGLE);
		h.succeed();
	}

	@GameTest
	public void innerCorner(GameTestHelper h) {
		// canto com paredes a sul e a este: o sofá do canto olha para norte, à frente dele um sofá a olhar para oeste
		BlockPos corner = new BlockPos(2, 1, 2), front = corner.north(), side = corner.west();
		TestUtil.place(h, front, sofa(DyeColor.GREEN, Direction.WEST));
		TestUtil.place(h, side, sofa(DyeColor.GREEN, Direction.NORTH));
		TestUtil.place(h, corner, sofa(DyeColor.GREEN, Direction.NORTH));
		shape(h, corner, SofaShape.INNER_LEFT);
		// o modelo do canto (encosto atrás e à esquerda) roda para oeste: encostos a sul e a este
		h.assertValueEqual(SofaBlock.modelFacing(h.getBlockState(corner)), Direction.WEST, "rotação do canto de dentro");
		h.succeed();
	}

	@GameTest
	public void outerCorner(GameTestHelper h) {
		BlockPos corner = new BlockPos(2, 1, 1), behind = corner.south();
		TestUtil.place(h, behind, sofa(DyeColor.YELLOW, Direction.EAST));
		TestUtil.place(h, corner, sofa(DyeColor.YELLOW, Direction.NORTH));
		shape(h, corner, SofaShape.OUTER_RIGHT);
		h.assertValueEqual(SofaBlock.modelFacing(h.getBlockState(corner)), Direction.NORTH, "rotação do canto de fora");
		h.succeed();
	}
}
