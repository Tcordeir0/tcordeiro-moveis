package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.BedSide;
import com.tcordeiro.moveis.block.DoubleBedBlock;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Cama de casal: põe as 4 partes, parte-se toda, dorme-se (e marca o renascer), rebenta no Nether. */
public class BedGameTests {
	static Block bed() {
		return MoveisBlocks.DOUBLE_BEDS.get(DyeColor.LIGHT_BLUE);
	}

	/** Põe a cama em (1,1,1) virada a sul (cabeceira a +z); a coluna direita fica a oeste (x=0). */
	static void placeBed(GameTestHelper h, ServerPlayer p) {
		for (int x = 0; x <= 1; x++) for (int z = 1; z <= 2; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		InteractionResult r = TestUtil.placeWithItem(h, p, new ItemStack(bed()), new BlockPos(1, 1, 1), 0f);
		h.assertTrue(r.consumesAction(), "a cama foi posta");
	}

	static final BlockPos FOOT_L = new BlockPos(1, 1, 1), HEAD_L = new BlockPos(1, 1, 2), FOOT_R = new BlockPos(0, 1, 1), HEAD_R = new BlockPos(0, 1, 2);

	@GameTest
	public void placesFourParts(GameTestHelper h) {
		ServerPlayer p = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		placeBed(h, p);
		Object[][] want = {{FOOT_L, BedPart.FOOT, BedSide.LEFT}, {HEAD_L, BedPart.HEAD, BedSide.LEFT}, {FOOT_R, BedPart.FOOT, BedSide.RIGHT},
				{HEAD_R, BedPart.HEAD, BedSide.RIGHT}};
		for (Object[] w : want) {
			BlockState st = h.getBlockState((BlockPos) w[0]);
			h.assertTrue(st.is(bed()), "cama em " + ((BlockPos) w[0]).toShortString());
			h.assertValueEqual(st.getValue(DoubleBedBlock.PART), w[1], "parte");
			h.assertValueEqual(st.getValue(DoubleBedBlock.SIDE), w[2], "lado");
			h.assertValueEqual(st.getValue(DoubleBedBlock.FACING), Direction.SOUTH, "virada a sul");
		}
		h.succeed();
	}

	@GameTest
	public void needsRoomForFourBlocks(GameTestHelper h) {
		h.setBlock(HEAD_R, Blocks.STONE); // um bloco no sítio da cabeceira direita
		ServerPlayer p = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		TestUtil.placeWithItem(h, p, new ItemStack(bed()), FOOT_L, 0f);
		h.assertTrue(!h.getBlockState(FOOT_L).is(bed()), "sem espaço não se põe");
		h.succeed();
	}

	@GameTest
	public void breakingOnePartBreaksAllAndDropsOne(GameTestHelper h) {
		ServerPlayer p = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		placeBed(h, p);
		h.destroyBlock(FOOT_R);
		for (BlockPos b : new BlockPos[] {FOOT_L, HEAD_L, FOOT_R, HEAD_R}) h.assertBlockNotPresent(bed(), b);
		h.assertItemEntityCountIs(bed().asItem(), FOOT_L, 4, 1);
		h.succeed();
	}

	@GameTest
	public void twoPlayersSleepAndSetSpawn(GameTestHelper h) {
		ServerPlayer a = TestUtil.playerAt(h, new BlockPos(2, 1, 1)), b = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		placeBed(h, b);
		cmd(h, "time set midnight");
		h.runAfterDelay(2, () -> { // a hora muda no tick seguinte
			h.useBlock(HEAD_L, a);
			h.useBlock(HEAD_R, b);
			try {
				if (!a.isSleeping()) { // diagnóstico: porque é que não dormiu?
					BlockState st = h.getBlockState(HEAD_L);
					DoubleBedBlock blk = (DoubleBedBlock) bed();
					var r = a.startSleepInBed(blk, st, blk.getBedRule(h.getLevel(), h.absolutePos(HEAD_L)), h.absolutePos(HEAD_L));
					h.fail("não dormiu: " + r.left().map(pr -> pr.message() == null ? "sem mensagem" : pr.message().getString()).orElse("dormiu agora")
							+ " escuro=" + h.getLevel().isDarkOutside());
				}
				h.assertTrue(b.isSleeping(), "o segundo dorme no lado direito");
				h.assertTrue(h.getBlockState(HEAD_L).getValue(DoubleBedBlock.OCCUPIED), "lado esquerdo ocupado");
				h.assertTrue(h.getBlockState(HEAD_R).getValue(DoubleBedBlock.OCCUPIED), "lado direito ocupado");
				var spawn = a.getRespawnConfig();
				h.assertTrue(spawn != null && spawn.respawnData().pos().equals(h.absolutePos(HEAD_L)), "o renascer ficou na cama");
				h.assertTrue(a.getSleepingPos().isPresent() && a.getSleepingPos().get().equals(h.absolutePos(HEAD_L)), "dorme na cabeceira");
				h.assertTrue(Math.abs(a.getY() - (h.absolutePos(HEAD_L).getY() + DoubleBedBlock.SLEEP_HEIGHT + 0.125)) < 0.01, "deitado à altura do colchão (+0,125 como nas camas vanilla)");
			} finally {
				a.stopSleeping();
				b.stopSleeping();
				cmd(h, "time set day");
			}
			h.assertTrue(!h.getBlockState(HEAD_L).getValue(DoubleBedBlock.OCCUPIED), "ao levantar, a cama fica livre");
			h.succeed();
		});
	}

	@GameTest(dimension = "minecraft:the_nether")
	public void explodesInTheNether(GameTestHelper h) {
		ServerPlayer p = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		placeBed(h, p);
		h.useBlock(HEAD_L, p);
		for (BlockPos b : new BlockPos[] {FOOT_L, HEAD_L, FOOT_R, HEAD_R}) h.assertBlockNotPresent(bed(), b);
		h.assertTrue(!p.isSleeping(), "no Nether não se dorme");
		h.assertItemEntityNotPresent(bed().asItem(), FOOT_L, 6);
		h.succeed();
	}

	static void cmd(GameTestHelper h, String c) {
		var server = h.getLevel().getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
	}
}
