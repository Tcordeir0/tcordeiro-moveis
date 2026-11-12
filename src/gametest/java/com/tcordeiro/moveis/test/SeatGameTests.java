package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.ChairBlock;
import com.tcordeiro.moveis.entity.SeatEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Sentar e levantar: cadeira, banco e sofá; um de cada vez; o assento desaparece quando se levanta ou o bloco parte. */
public class SeatGameTests {
	static final BlockPos POS = new BlockPos(1, 1, 1);

	static void sitOn(GameTestHelper h, Block block) {
		h.setBlock(POS.below(), Blocks.STONE);
		h.setBlock(POS, block);
		ServerPlayer p = TestUtil.playerAt(h, POS.east());
		h.useBlock(POS, p);
		h.assertTrue(p.getVehicle() instanceof SeatEntity, "sentou-se em " + block.getName().getString());
		SeatEntity seat = (SeatEntity) p.getVehicle();
		h.assertTrue(seat.blockPosition().equals(h.absolutePos(POS)), "o assento está no bloco");
		h.assertTrue(Math.abs(seat.getY() - (h.absolutePos(POS).getY() + 0.5)) < 1e-6, "assento a meia altura");
		p.stopRiding();
		h.assertTrue(!p.isPassenger(), "levantou-se");
		h.runAfterDelay(2, () -> {
			h.assertTrue(h.getEntities(MoveisBlocks.SEAT).isEmpty(), "assento vazio desapareceu");
			h.succeed();
		});
	}

	@GameTest
	public void sitOnChair(GameTestHelper h) {
		sitOn(h, MoveisBlocks.wooden(Furniture.CHAIR, "oak"));
	}

	@GameTest
	public void sitOnStool(GameTestHelper h) {
		sitOn(h, MoveisBlocks.wooden(Furniture.STOOL, "poplar"));
	}

	@GameTest
	public void sitOnSofa(GameTestHelper h) {
		sitOn(h, MoveisBlocks.SOFAS.get(DyeColor.RED));
	}

	@GameTest
	public void secondPlayerCannotSit(GameTestHelper h) {
		h.setBlock(POS, MoveisBlocks.wooden(Furniture.CHAIR, "spruce"));
		ServerPlayer a = TestUtil.playerAt(h, POS.east()), b = TestUtil.playerAt(h, POS.west());
		h.useBlock(POS, a);
		h.useBlock(POS, b);
		h.assertTrue(a.isPassenger(), "o primeiro sentou-se");
		h.assertTrue(!b.isPassenger(), "o segundo não se senta no mesmo lugar");
		h.assertValueEqual(h.getEntities(MoveisBlocks.SEAT).size(), 1, "um só assento");
		h.succeed();
	}

	@GameTest
	public void sneakingDoesNotSit(GameTestHelper h) {
		h.setBlock(POS, MoveisBlocks.wooden(Furniture.CHAIR, "oak"));
		ServerPlayer p = TestUtil.playerAt(h, POS.east());
		p.setShiftKeyDown(true);
		h.useBlock(POS, p);
		h.assertTrue(!p.isPassenger(), "agachado não se senta");
		h.succeed();
	}

	@GameTest
	public void seatRemovedWhenBroken(GameTestHelper h) {
		h.setBlock(POS, MoveisBlocks.wooden(Furniture.CHAIR, "cherry"));
		ServerPlayer p = TestUtil.playerAt(h, POS.east());
		h.useBlock(POS, p);
		h.assertTrue(p.isPassenger(), "sentou-se");
		h.destroyBlock(POS);
		h.runAfterDelay(1, () -> {
			h.assertTrue(!p.isPassenger(), "partir a cadeira levanta o jogador");
			h.assertTrue(h.getEntities(MoveisBlocks.SEAT).isEmpty(), "o assento foi apagado");
			h.succeed();
		});
	}

	@GameTest
	public void chairFacesWherePlayerLooks(GameTestHelper h) {
		h.setBlock(POS.below(), Blocks.STONE);
		Block chair = MoveisBlocks.wooden(Furniture.CHAIR, "birch");
		ServerPlayer p = TestUtil.playerAt(h, POS.east(2));
		TestUtil.placeWithItem(h, p, new ItemStack(chair), POS, 90f); // 90° = a olhar para oeste
		h.assertBlockProperty(POS, ChairBlock.FACING, Direction.WEST);
		h.useBlock(POS, p);
		h.assertValueEqual(Direction.fromYRot(p.getVehicle().getYRot()), Direction.WEST, "sentado, fica virado para onde a cadeira olha");
		h.succeed();
	}
}
