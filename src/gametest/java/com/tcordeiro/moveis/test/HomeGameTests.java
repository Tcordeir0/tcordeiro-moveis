package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.anim.FurnitureParts;
import com.tcordeiro.moveis.block.BedSide;
import com.tcordeiro.moveis.block.BunkBedBlock;
import com.tcordeiro.moveis.block.CounterBlock;
import com.tcordeiro.moveis.block.DeskBlock;
import com.tcordeiro.moveis.block.FireplaceBlock;
import com.tcordeiro.moveis.block.PianoBlock;
import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.block.entity.DisplayBlockEntity;
import com.tcordeiro.moveis.block.entity.FireplaceBlockEntity;
import com.tcordeiro.moveis.block.entity.PianoBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.block.entity.WineRackBlockEntity;
import com.tcordeiro.moveis.entity.SeatEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/** O resto da casa: lixeira, bancada, escrivaninha, piano, floreira, beliche, lareira, mesas, adega, assentos. */
public class HomeGameTests {
	@GameTest(maxTicks = 60)
	public void trashCanDeletesFromHopperAndOnClose(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.TRASH_CAN);
		StorageFurnitureBlockEntity be = h.getBlockEntity(p, StorageFurnitureBlockEntity.class);
		ServerPlayer player = TestUtil.playerAt(h, p.north());
		player.openMenu(be);
		be.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 10));
		h.assertValueEqual(be.countItem(Items.ROTTEN_FLESH), 10, "aberta: ainda lá está (dá para tirar se foi engano)");
		player.closeContainer();
		h.assertTrue(be.isEmpty(), "⚠ ao fechar, sumiu");
		h.setBlock(p.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		h.getBlockEntity(p.above(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 3));
		h.succeedWhen(() -> {
			h.assertValueEqual(h.getBlockEntity(p.above(), HopperBlockEntity.class).countItem(Items.COBBLESTONE), 0, "o funil despejou tudo");
			h.assertTrue(be.isEmpty(), "e a lixeira apagou na hora");
		});
	}

	@GameTest
	public void counterMakesCornerAndHoldsNine(GameTestHelper h) {
		Block c = MoveisBlocks.wooden(Furniture.COUNTER, "acacia");
		BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(1, 1, 2);
		TestUtil.place(h, a, c.defaultBlockState().setValue(StorageFurnitureBlock.FACING, Direction.SOUTH));
		TestUtil.place(h, b, c.defaultBlockState().setValue(StorageFurnitureBlock.FACING, Direction.EAST));
		h.assertBlockProperty(a, CounterBlock.CORNER, true);
		h.assertBlockProperty(b, CounterBlock.CORNER, false);
		h.assertValueEqual(h.getBlockEntity(a, StorageFurnitureBlockEntity.class).getContainerSize(), 9, "9 espaços");
		h.destroyBlock(b);
		h.assertBlockProperty(a, CounterBlock.CORNER, false);
		h.succeed();
	}

	@GameTest
	public void desksJoinIntoOneTop(GameTestHelper h) {
		Block d = MoveisBlocks.wooden(Furniture.DESK, "oak");
		BlockState north = d.defaultBlockState();
		TestUtil.place(h, new BlockPos(2, 1, 1), north);
		TestUtil.place(h, new BlockPos(1, 1, 1), north); // a oeste = do lado da perna da primeira
		h.assertBlockProperty(new BlockPos(2, 1, 1), DeskBlock.JOINED, true);
		h.assertBlockProperty(new BlockPos(1, 1, 1), DeskBlock.JOINED, false);
		h.assertValueEqual(h.getBlockEntity(new BlockPos(1, 1, 1), StorageFurnitureBlockEntity.class).getContainerSize(), 9, "9 espaços");
		h.succeed();
	}

	/** O bloco da tecla (piano virado a norte: metade direita em {@code right}, a esquerda a oeste). */
	static BlockPos keyBlock(BlockPos right, int key) {
		float[] k = FurnitureParts.PIANO_KEYS[key];
		return (k[1] + k[2]) / 2 >= 16 ? right : right.west();
	}

	/** O ponto do mundo no meio da tecla, por cima (as pretas a meio do teclado, as brancas à frente). */
	static Vec3 keyHit(GameTestHelper h, BlockPos right, int key) {
		float[] k = FurnitureParts.PIANO_KEYS[key];
		float cx = (k[1] + k[2]) / 2;
		boolean black = k[0] > 0.5f;
		double mx = (cx >= 16 ? cx - 16 : cx) / 16.0;
		double y = (black ? FurnitureParts.BLACK_Y2 : FurnitureParts.WHITE_Y2) / 16.0, z = (black ? 6.5 : 3) / 16.0;
		BlockPos abs = h.absolutePos(keyBlock(right, key));
		return new Vec3(abs.getX() + mx, abs.getY() + y, abs.getZ() + z);
	}

	@GameTest
	public void pianoPlaysTheKeyClicked(GameTestHelper h) {
		BlockPos right = new BlockPos(2, 1, 2), left = right.west();
		h.setBlock(right, MoveisBlocks.PIANO.defaultBlockState().setValue(PianoBlock.FACING, Direction.NORTH).setValue(PianoBlock.SIDE, BedSide.RIGHT));
		h.setBlock(left, MoveisBlocks.PIANO.defaultBlockState().setValue(PianoBlock.FACING, Direction.NORTH).setValue(PianoBlock.SIDE, BedSide.LEFT));
		ServerPlayer player = TestUtil.playerAt(h, new BlockPos(2, 1, 0));
		int[] keys = {0, 4, 11, 16, 22};
		for (int key : keys) {
			BlockPos block = keyBlock(right, key);
			TestUtil.useEmptyAt(h, player, block, keyHit(h, right, key));
			PianoBlockEntity be = h.getBlockEntity(block, PianoBlockEntity.class);
			h.assertValueEqual(be.lastKey, key, "a tecla clicada");
			h.assertValueEqual(be.lastNote, key + 1, "a nota (Sol3 = 1)");
		}
		h.assertTrue(keyBlock(right, 0).equals(right) && keyBlock(right, 22).equals(left), "graves na metade direita (à esquerda de quem toca)");
		h.assertBlockProperty(right, PianoBlock.PLAYING, true);
		h.assertBlockProperty(left, PianoBlock.PLAYING, true);
		h.startSequence().thenIdle(PianoBlock.PLAY_TICKS + 2).thenExecute(() -> {
			h.assertBlockProperty(right, PianoBlock.PLAYING, false);
			h.assertBlockProperty(left, PianoBlock.PLAYING, false);
		}).thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void planterGrowsCropsAndHoldsFlowers(GameTestHelper h) {
		Block planter = MoveisBlocks.wooden(Furniture.PLANTER_BOX, "jungle");
		BlockPos box = new BlockPos(1, 1, 1), crop = box.above();
		h.setBlock(box, planter);
		h.setBlock(crop.north(), Blocks.GLOWSTONE); // luz para crescer, a qualquer hora
		h.assertTrue(Blocks.WHEAT.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(crop)), "o trigo pode ser plantado na floreira");
		h.assertTrue(Blocks.POPPY.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(crop)), "flores");
		h.assertTrue(Blocks.OAK_SAPLING.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(crop)), "mudas");
		h.assertTrue(Blocks.MELON_STEM.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(crop)), "caules (melancia, abóbora)");
		h.setBlock(box.east(), Blocks.STONE);
		h.assertTrue(!Blocks.WHEAT.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(box.east().above())), "na pedra não");
		h.setBlock(crop, Blocks.WHEAT);
		h.succeedWhen(() -> {
			h.randomTick(crop);
			h.assertTrue(h.getBlockState(crop).getValue(CropBlock.AGE) >= 3, "o trigo cresceu na floreira");
		});
	}

	@GameTest
	public void bunkBedSleepsTwoPlayers(GameTestHelper h) {
		BlockPos foot = new BlockPos(1, 1, 1), head = new BlockPos(1, 1, 2);
		h.setBlock(foot.below(), Blocks.STONE);
		h.setBlock(head.below(), Blocks.STONE);
		ServerPlayer a = TestUtil.playerAt(h, new BlockPos(2, 1, 2)), b = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		Block bunk = MoveisBlocks.BUNK_BEDS.get(DyeColor.GREEN);
		h.assertTrue(TestUtil.placeWithItem(h, b, new ItemStack(bunk), foot, 0f).consumesAction(), "posto");
		for (BlockPos p : new BlockPos[] {foot, head, foot.above(), head.above()}) h.assertTrue(h.getBlockState(p).is(bunk), "parte em " + p.toShortString());
		h.assertBlockProperty(head.above(), BunkBedBlock.HALF, DoubleBlockHalf.UPPER);
		h.assertBlockProperty(head.above(), BunkBedBlock.PART, BedPart.HEAD);
		BedGameTests.cmd(h, "time set midnight");
		h.runAfterDelay(2, () -> {
			h.useBlock(head, a);
			h.useBlock(head.above(), b);
			try {
				h.assertTrue(a.isSleeping(), "um dorme em baixo");
				h.assertTrue(b.isSleeping(), "o outro dorme em cima");
				h.assertTrue(h.getBlockState(head).getValue(BunkBedBlock.OCCUPIED) && h.getBlockState(head.above()).getValue(BunkBedBlock.OCCUPIED),
						"as duas camas ocupadas");
				h.assertTrue(b.getSleepingPos().get().equals(h.absolutePos(head.above())), "o de cima dorme na cabeceira de cima");
			} finally {
				a.stopSleeping();
				b.stopSleeping();
				BedGameTests.cmd(h, "time set day");
			}
			h.destroyBlock(foot.above());
			for (BlockPos p : new BlockPos[] {foot, head, foot.above(), head.above()}) h.assertBlockNotPresent(bunk, p);
			h.assertItemEntityCountIs(bunk.asItem(), foot, 4, 1);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 120)
	public void fireplaceLightsBurnsFirewoodAndGoesOut(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 2);
		h.setBlock(p, MoveisBlocks.FIREPLACE); // virada a norte: a boca do fogo para z = 1
		ServerPlayer player = TestUtil.survivor(h, new BlockPos(3, 1, 0));
		TestUtil.useItem(h, player, p, new ItemStack(Items.FLINT_AND_STEEL));
		h.assertBlockProperty(p, FireplaceBlock.LIT, false);
		TestUtil.useItem(h, player, p, new ItemStack(Items.STICK, 2));
		FireplaceBlockEntity be = h.getBlockEntity(p, FireplaceBlockEntity.class);
		h.assertValueEqual(be.fuel().getCount(), 2, "2 paus de lenha");
		TestUtil.useItem(h, player, p, new ItemStack(Items.FLINT_AND_STEEL));
		h.assertBlockProperty(p, FireplaceBlock.LIT, true);
		h.assertValueEqual(be.fuel().getCount(), 1, "acendeu e começou a gastar um");
		h.assertValueEqual(be.burn, FireplaceBlockEntity.STICK_TICKS, "um pau arde 20 s");
		h.assertValueEqual(h.getBlockState(p).getLightEmission(), 15, "luz 15");
		Pig pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(1.5, 1, 1.6)); // encostado à boca do fogo
		pig.setNoAi(true);
		float hp = pig.getHealth();
		h.startSequence()
				.thenIdle(21)
				.thenExecute(() -> {
					h.assertTrue(pig.getHealth() < hp, "⚠ queimou o porco encostado ao fogo");
					be.burn = 3; // para o teste não esperar 20 s
				})
				.thenIdle(4)
				.thenExecute(() -> {
					h.assertValueEqual(be.fuel().getCount(), 0, "gastou o segundo pau");
					h.assertBlockProperty(p, FireplaceBlock.LIT, true);
					be.burn = 2;
				})
				.thenIdle(4)
				.thenExecute(() -> h.assertBlockProperty(p, FireplaceBlock.LIT, false))
				.thenSucceed();
	}

	@GameTest
	public void tablesShowOneItem(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.wooden(Furniture.COFFEE_TABLE, "cherry"));
		ServerPlayer player = TestUtil.survivor(h, p.north());
		TestUtil.useItem(h, player, p, new ItemStack(Items.CAKE, 1));
		h.assertTrue(h.getBlockEntity(p, DisplayBlockEntity.class).item().is(Items.CAKE), "o bolo ficou em cima");
		h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "saiu da mão");
		TestUtil.useItem(h, player, p, ItemStack.EMPTY);
		h.assertTrue(h.getBlockEntity(p, DisplayBlockEntity.class).item().isEmpty(), "tirou");
		h.assertTrue(player.getInventory().countItem(Items.CAKE) == 1, "voltou para o inventário");
		h.succeed();
	}

	@GameTest
	public void wineRackTakesBottlesInTheClickedCell(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.wooden(Furniture.WINE_RACK, "dark_oak")); // virada a norte
		ServerPlayer player = TestUtil.survivor(h, p.north());
		BlockPos abs = h.absolutePos(p);
		Vec3 topLeft = new Vec3(abs.getX() + 13 / 16.0, abs.getY() + 13 / 16.0, abs.getZ()); // vista de frente: esquerda = x alto
		ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
		TestUtil.useItemAt(h, player, p, potion, topLeft);
		WineRackBlockEntity be = h.getBlockEntity(p, WineRackBlockEntity.class);
		h.assertTrue(be.getItem(0).is(Items.POTION), "a poção foi para a casa 0 (em cima à esquerda)");
		Vec3 bottomRight = new Vec3(abs.getX() + 3 / 16.0, abs.getY() + 3 / 16.0, abs.getZ());
		TestUtil.useItemAt(h, player, p, new ItemStack(Items.DIRT), bottomRight);
		h.assertTrue(be.getItem(8).isEmpty(), "terra não entra");
		h.assertTrue(!be.canPlaceItem(8, new ItemStack(Items.DIRT)) && be.canPlaceItem(8, new ItemStack(Items.GLASS_BOTTLE)), "funis: só garrafas");
		TestUtil.useEmptyAt(h, player, p, topLeft);
		h.assertTrue(be.getItem(0).isEmpty() && player.getInventory().countItem(Items.POTION) == 1, "a mão vazia tira a da casa");
		h.succeed();
	}

	@GameTest
	public void toiletAndBenchSeatPlayers(GameTestHelper h) {
		BlockPos t = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 1);
		h.setBlock(t, MoveisBlocks.TOILET);
		h.setBlock(b, MoveisBlocks.wooden(Furniture.GARDEN_BENCH, "mangrove"));
		for (BlockPos p : new BlockPos[] {t, b}) {
			ServerPlayer player = TestUtil.playerAt(h, p.north());
			h.useBlock(p, player);
			h.assertTrue(player.getVehicle() instanceof SeatEntity, "sentou em " + p.toShortString());
		}
		h.succeed();
	}
}
