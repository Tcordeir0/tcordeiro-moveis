package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.anim.Spin;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.CeilingFanBlock;
import com.tcordeiro.moveis.block.FloorLampBlock;
import com.tcordeiro.moveis.block.LampBlock;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Ventilador e luminária de chão: redstone (o ventilador gira mais devagar com sinal fraco) e energia do Industrial (se houver). */
public class PowerGameTests {
	@GameTest(maxTicks = 100)
	public void fanSpinsWithRedstoneAndBrakes(GameTestHelper h) {
		BlockPos fan = new BlockPos(1, 2, 1);
		h.setBlock(fan.above(), Blocks.STONE);
		h.setBlock(fan, MoveisBlocks.CEILING_FAN);
		h.assertBlockProperty(fan, LampBlock.LIT, false);
		h.setBlock(fan.north(), Blocks.REDSTONE_BLOCK);
		h.assertBlockProperty(fan, CeilingFanBlock.POWER, 15);
		h.assertBlockProperty(fan, LampBlock.LIT, true);
		LampBlockEntity be = h.getBlockEntity(fan, LampBlockEntity.class);
		h.startSequence()
				.thenIdle(30)
				.thenExecute(() -> {
					h.assertTrue(be.spin.speed() > 0.9f, "a girar depressa (sinal 15)");
					h.setBlock(fan.north(), Blocks.AIR);
					h.assertBlockProperty(fan, LampBlock.LIT, false);
					AnimGameTests.assertMoving(h, fan, true);
				})
				.thenIdle(Spin.BRAKE_TICKS + 3)
				.thenExecute(() -> {
					AnimGameTests.assertMoving(h, fan, false);
					h.assertTrue(be.spin.stopped(), "parou");
				})
				.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void weakSignalSpinsSlower(GameTestHelper h) {
		// barril quase vazio → comparador (sinal 1) → ventilador
		BlockPos barrel = new BlockPos(0, 2, 1), comp = new BlockPos(1, 2, 1), fan = new BlockPos(2, 2, 1);
		for (BlockPos p : new BlockPos[] {barrel.below(), comp.below(), fan.above()}) h.setBlock(p, Blocks.STONE);
		h.setBlock(barrel, Blocks.BARREL);
		h.setBlock(fan, MoveisBlocks.CEILING_FAN);
		h.setBlock(comp, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST)); // lê a oeste, sai para este
		BarrelBlockEntity b = h.getBlockEntity(barrel, BarrelBlockEntity.class);
		b.setItem(0, new ItemStack(Items.DIRT, 1));
		b.setChanged(); // o comparador vê a mudança
		h.succeedWhen(() -> {
			h.assertBlockProperty(fan, LampBlock.LIT, true);
			int power = h.getBlockState(fan).getValue(CeilingFanBlock.POWER);
			h.assertTrue(power > 0 && power < 15, "sinal fraco (" + power + ")");
			h.assertTrue(CeilingFanBlock.speed(h.getBlockState(fan)) < 0.5f, "velocidade menor");
		});
	}

	@GameTest
	public void floorLampIsTwoTallAndLightsWithRedstone(GameTestHelper h) {
		BlockPos low = new BlockPos(1, 1, 1), up = low.above();
		h.setBlock(low.below(), Blocks.STONE);
		ServerPlayer p = TestUtil.playerAt(h, low.north());
		h.assertTrue(TestUtil.placeWithItem(h, p, new ItemStack(MoveisBlocks.FLOOR_LAMP), low, 0f).consumesAction(), "posta");
		h.assertBlockProperty(up, FloorLampBlock.HALF, DoubleBlockHalf.UPPER);
		h.setBlock(up.north(), Blocks.REDSTONE_BLOCK); // sinal na metade de cima
		h.assertBlockProperty(low, LampBlock.LIT, true);
		h.assertBlockProperty(up, LampBlock.LIT, true);
		h.assertValueEqual(h.getBlockState(up).getLightEmission(), 14, "luz 14");
		h.setBlock(up.north(), Blocks.AIR);
		h.runAfterDelay(2, () -> {
			h.assertBlockProperty(up, LampBlock.LIT, false);
			h.destroyBlock(up);
			h.assertBlockNotPresent(MoveisBlocks.FLOOR_LAMP, low);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void fanRunsOnIndustrialEnergy(GameTestHelper h) {
		if (!TMoveis.INDUSTRIAL) {
			h.succeed(); // ./gradlew runGametest -PnoIndustrial: sem energia a testar
			return;
		}
		BlockPos battery = new BlockPos(1, 2, 1), fan = battery.below();
		h.setBlock(battery, LampGameTests.industrial("battery"));
		h.setBlock(fan, MoveisBlocks.CEILING_FAN); // pendurado na bateria
		h.assertTrue(h.getBlockEntity(fan, LampBlockEntity.class).getClass().getSimpleName().equals("PoweredLampBlockEntity"), "o ventilador é um consumidor");
		LampGameTests.charge(h, battery);
		h.succeedWhen(() -> {
			h.assertBlockProperty(fan, LampBlock.LIT, true);
			h.assertBlockProperty(fan, CeilingFanBlock.POWER, 15);
			h.assertTrue(h.getBlockEntity(fan, LampBlockEntity.class).energized(), "pela energia (sem redstone)");
		});
	}

	@GameTest(maxTicks = 200)
	public void floorLampOnIndustrialEnergy(GameTestHelper h) {
		if (!TMoveis.INDUSTRIAL) {
			h.succeed();
			return;
		}
		BlockPos battery = new BlockPos(1, 1, 1), lamp = new BlockPos(2, 1, 1);
		h.setBlock(battery, LampGameTests.industrial("battery"));
		h.setBlock(lamp.below(), Blocks.STONE);
		h.setBlock(lamp, MoveisBlocks.FLOOR_LAMP.defaultBlockState());
		h.setBlock(lamp.above(), MoveisBlocks.FLOOR_LAMP.defaultBlockState().setValue(FloorLampBlock.HALF, DoubleBlockHalf.UPPER));
		LampGameTests.charge(h, battery);
		h.succeedWhen(() -> {
			h.assertBlockProperty(lamp, LampBlock.LIT, true);
			h.assertBlockProperty(lamp.above(), LampBlock.LIT, true);
		});
	}
}
