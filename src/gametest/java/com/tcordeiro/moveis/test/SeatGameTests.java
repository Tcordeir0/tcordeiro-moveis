package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.ChairBlock;
import com.tcordeiro.moveis.block.SimpleSeatBlock;
import com.tcordeiro.moveis.block.SofaBlock;
import com.tcordeiro.moveis.block.SofaShape;
import com.tcordeiro.moveis.entity.SeatEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

	// 0.3.1: o jogador vira-se para a frente do móvel ao sentar, e a cabeça só roda ±90° (o corpo fica para a frente)

	static void assertYaw(GameTestHelper h, float expected, float actual, String what) {
		h.assertTrue(Mth.degreesDifferenceAbs(expected, actual) < 0.01f, what + ": esperado " + expected + "°, está " + actual + "°");
	}

	/** Senta um jogador que olha para {@code lookYaw} e confirma que fica virado para {@code seatYaw} (cabeça, corpo e assento). */
	static ServerPlayer sitFacing(GameTestHelper h, BlockState state, float lookYaw, float seatYaw) {
		h.setBlock(POS.below(), Blocks.STONE);
		h.setBlock(POS, state);
		ServerPlayer p = TestUtil.playerAt(h, POS.east());
		p.setYRot(lookYaw);
		p.setYHeadRot(lookYaw);
		p.setYBodyRot(lookYaw);
		h.useBlock(POS, p);
		h.assertTrue(p.getVehicle() instanceof SeatEntity, "sentou-se em " + state.getBlock().getName().getString());
		assertYaw(h, seatYaw, p.getVehicle().getYRot(), "assento");
		assertYaw(h, seatYaw, p.getYRot(), "o jogador olha para a frente do móvel");
		assertYaw(h, seatYaw, p.getYHeadRot(), "cabeça");
		assertYaw(h, seatYaw, p.yBodyRot, "corpo");
		return p;
	}

	@GameTest
	public void sittingTurnsPlayerToChairFront(GameTestHelper h) {
		// cadeira virada a oeste (90°); o jogador estava a olhar para leste (270°), de costas para onde a cadeira olha
		BlockState chair = MoveisBlocks.wooden(Furniture.CHAIR, "oak").defaultBlockState().setValue(ChairBlock.FACING, Direction.WEST);
		sitFacing(h, chair, 270f, 90f);
		h.succeed();
	}

	@GameTest
	public void sittingTurnsPlayerToSofaAndBenchFront(GameTestHelper h) {
		BlockState sofa = MoveisBlocks.SOFAS.get(DyeColor.BLUE).defaultBlockState().setValue(SofaBlock.FACING, Direction.SOUTH);
		ServerPlayer p = sitFacing(h, sofa, 90f, 0f);
		p.stopRiding();
		h.runAfterDelay(2, () -> {
			BlockState toilet = MoveisBlocks.TOILET.defaultBlockState().setValue(SimpleSeatBlock.FACING, Direction.NORTH);
			sitFacing(h, toilet, 0f, 180f);
			h.succeed();
		});
	}

	@GameTest
	public void sofaCornersFaceTheDiagonal(GameTestHelper h) {
		// virado a norte (180°): canto direito (de dentro ou de fora) = nordeste (225°), canto esquerdo = noroeste (135°)
		BlockState n = MoveisBlocks.SOFAS.get(DyeColor.RED).defaultBlockState().setValue(SofaBlock.FACING, Direction.NORTH);
		assertYaw(h, 225f, SofaBlock.seatYaw(n.setValue(SofaBlock.SHAPE, SofaShape.INNER_RIGHT)), "canto de dentro direito");
		assertYaw(h, 225f, SofaBlock.seatYaw(n.setValue(SofaBlock.SHAPE, SofaShape.OUTER_RIGHT)), "canto de fora direito");
		assertYaw(h, 135f, SofaBlock.seatYaw(n.setValue(SofaBlock.SHAPE, SofaShape.INNER_LEFT)), "canto de dentro esquerdo");
		assertYaw(h, 135f, SofaBlock.seatYaw(n.setValue(SofaBlock.SHAPE, SofaShape.OUTER_LEFT)), "canto de fora esquerdo");
		assertYaw(h, 180f, SofaBlock.seatYaw(n.setValue(SofaBlock.SHAPE, SofaShape.MIDDLE)), "reto");
		// e sentado mesmo num canto de dentro virado a leste: 270° + 45° = 315°
		sitFacing(h, n.setValue(SofaBlock.FACING, Direction.EAST).setValue(SofaBlock.SHAPE, SofaShape.INNER_RIGHT), 0f, 315f);
		h.succeed();
	}

	@GameTest
	public void seatedHeadTurnIsLimited(GameTestHelper h) {
		BlockState chair = MoveisBlocks.wooden(Furniture.CHAIR, "spruce").defaultBlockState().setValue(ChairBlock.FACING, Direction.WEST);
		ServerPlayer p = sitFacing(h, chair, 90f, 90f);
		SeatEntity seat = (SeatEntity) p.getVehicle();
		// o rato rodou 150° para a direita: fica nos 90° (cabeça a 180°), o corpo continua para a frente
		p.setYRot(240f);
		seat.onPassengerTurned(p);
		assertYaw(h, 180f, p.getYRot(), "+150° fica em +90°");
		assertYaw(h, 180f, p.getYHeadRot(), "cabeça em +90°");
		assertYaw(h, 90f, p.yBodyRot, "corpo para a frente");
		// para a esquerda, também no limite
		p.setYRot(90f - 170f);
		seat.onPassengerTurned(p);
		assertYaw(h, 0f, p.getYRot(), "-170° fica em -90°");
		// dentro do limite não mexe
		p.setYRot(90f + 60f);
		seat.onPassengerTurned(p);
		assertYaw(h, 150f, p.getYRot(), "+60° fica");
		// e no tick do jogo (positionRider), sem ninguém chamar onPassengerTurned
		p.setYRot(90f + 179f);
		p.setYBodyRot(0f);
		h.runAfterDelay(2, () -> {
			h.assertTrue(p.getVehicle() == seat, "continua sentado");
			assertYaw(h, 180f, p.getYRot(), "no tick: +179° fica em +90°");
			assertYaw(h, 90f, p.yBodyRot, "no tick: corpo para a frente");
			h.succeed();
		});
	}
}
