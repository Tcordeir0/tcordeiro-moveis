package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.LampBlock;
import com.tcordeiro.moveis.block.WallLampBlock;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Lâmpadas: acendem com redstone; precisam de apoio; com o Industrial acendem com energia (fio de cobre e encostadas à bateria). */
public class LampGameTests {
	@GameTest
	public void redstoneLightsAllThree(GameTestHelper h) {
		BlockPos table = new BlockPos(1, 1, 1), chandelier = new BlockPos(3, 1, 1), wall = new BlockPos(5, 1, 1);
		h.setBlock(table.below(), Blocks.STONE);
		h.setBlock(table, MoveisBlocks.TABLE_LAMP);
		h.setBlock(chandelier.above(), Blocks.STONE);
		h.setBlock(chandelier, MoveisBlocks.CHANDELIER);
		h.setBlock(wall.south(), Blocks.STONE);
		h.setBlock(wall, MoveisBlocks.WALL_LAMP.defaultBlockState().setValue(WallLampBlock.FACING, Direction.NORTH));
		for (BlockPos p : new BlockPos[] {table, chandelier, wall}) h.assertBlockProperty(p, LampBlock.LIT, false);
		for (BlockPos p : new BlockPos[] {table, chandelier, wall}) h.setBlock(p.north(), Blocks.REDSTONE_BLOCK);
		for (BlockPos p : new BlockPos[] {table, chandelier, wall}) h.assertBlockProperty(p, LampBlock.LIT, true);
		h.assertValueEqual(h.getBlockState(chandelier).getLightEmission(), 15, "o lustre dá luz 15");
		for (BlockPos p : new BlockPos[] {table, chandelier, wall}) h.setBlock(p.north(), Blocks.AIR);
		h.runAfterDelay(2, () -> {
			for (BlockPos p : new BlockPos[] {table, chandelier, wall}) h.assertBlockProperty(p, LampBlock.LIT, false);
			h.succeed();
		});
	}

	@GameTest
	public void needSupport(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 2, 1);
		h.assertTrue(!MoveisBlocks.CHANDELIER.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(p)), "lustre sem teto");
		h.assertTrue(!MoveisBlocks.TABLE_LAMP.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(p)), "luminária no ar");
		h.assertTrue(!MoveisBlocks.WALL_LAMP.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(p)), "arandela sem parede");
		// o lustre cai quando se tira o teto
		h.setBlock(p.above(), Blocks.STONE);
		h.setBlock(p, MoveisBlocks.CHANDELIER);
		h.destroyBlock(p.above());
		h.assertBlockNotPresent(MoveisBlocks.CHANDELIER, p);
		h.succeed();
	}

	// ---------------------------------------------------------------- com o Industrial (por reflexão: corre também sem ele)

	static Block industrial(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("tindustrial", id));
	}

	/** Enche a bateria (energy().setStored(...), API pública do Industrial). */
	static void charge(GameTestHelper h, BlockPos battery) {
		try {
			Object be = h.getLevel().getBlockEntity(h.absolutePos(battery));
			Object buf = be.getClass().getMethod("energy").invoke(be);
			buf.getClass().getMethod("setStored", long.class).invoke(buf, 10_000L);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("bateria do Industrial sem energy().setStored", e);
		}
	}

	@GameTest(maxTicks = 200)
	public void copperWireLightsLamp(GameTestHelper h) {
		if (!TMoveis.INDUSTRIAL) {
			h.succeed(); // sem o Industrial (./gradlew runGametest -PnoIndustrial) não há energia a testar
			return;
		}
		// bateria → 3 fios de cobre no chão → luminária (em cima de pedra)
		for (int x = 0; x <= 5; x++) h.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
		BlockPos battery = new BlockPos(0, 1, 1), lamp = new BlockPos(4, 1, 1);
		h.setBlock(battery, industrial("battery"));
		h.setBlock(lamp, MoveisBlocks.TABLE_LAMP);
		h.assertTrue(h.getBlockEntity(lamp, LampBlockEntity.class).getClass().getSimpleName().equals("PoweredLampBlockEntity"),
				"com o Industrial a lâmpada é um consumidor de energia");
		for (int x = 1; x <= 3; x++) TestUtil.place(h, new BlockPos(x, 1, 1), industrial("copper_wire").defaultBlockState());
		charge(h, battery);
		h.succeedWhen(() -> {
			h.assertBlockProperty(lamp, LampBlock.LIT, true);
			h.assertTrue(h.getBlockEntity(lamp, LampBlockEntity.class).energized(), "acesa pela energia (não há redstone)");
		});
	}

	@GameTest(maxTicks = 200)
	public void lampAgainstBatteryAndTurnsOff(GameTestHelper h) {
		if (!TMoveis.INDUSTRIAL) {
			h.succeed();
			return;
		}
		BlockPos battery = new BlockPos(1, 1, 1), lamp = new BlockPos(1, 2, 1);
		h.setBlock(battery, industrial("battery"));
		h.setBlock(lamp, MoveisBlocks.TABLE_LAMP); // em cima da bateria
		charge(h, battery);
		h.startSequence()
				.thenWaitUntil(() -> h.assertBlockProperty(lamp, LampBlock.LIT, true))
				.thenExecute(() -> h.setBlock(battery, Blocks.STONE)) // sem energia: gasta o que guardou e apaga
				.thenWaitUntil(() -> h.assertBlockProperty(lamp, LampBlock.LIT, false))
				.thenSucceed();
	}
}
