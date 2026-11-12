package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.BathtubBlock;
import com.tcordeiro.moveis.block.WaterFurniture;
import com.tcordeiro.moveis.block.entity.WaterBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.Fluids;

/** Pia e banheira: balde e frasco à mão, FluidStorage (Transfer API) a pôr e tirar, lava recusada, a banheira apaga o fogo. */
public class WaterGameTests {
	static Storage<FluidVariant> storage(GameTestHelper h, BlockPos rel) {
		return FluidStorage.SIDED.find(h.getLevel(), h.absolutePos(rel), Direction.UP);
	}

	static long insert(Storage<FluidVariant> s, FluidVariant v, long amount) {
		try (Transaction t = Transaction.openOuter()) {
			long n = s.insert(v, amount, t);
			t.commit();
			return n;
		}
	}

	static long extract(Storage<FluidVariant> s, FluidVariant v, long amount) {
		try (Transaction t = Transaction.openOuter()) {
			long n = s.extract(v, amount, t);
			t.commit();
			return n;
		}
	}

	static long amount(GameTestHelper h, BlockPos rel) {
		return WaterFurniture.storage(h.getLevel(), h.absolutePos(rel), h.getBlockState(rel)).tank.amount;
	}

	@GameTest
	public void sinkFillsAndEmptiesWithBuckets(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.SINK);
		ServerPlayer player = TestUtil.survivor(h, p.north());
		TestUtil.useItem(h, player, p, new ItemStack(Items.WATER_BUCKET));
		h.assertValueEqual(amount(h, p), FluidConstants.BUCKET, "1 balde dentro");
		h.assertBlockProperty(p, WaterFurniture.WATER, true);
		h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "o balde ficou vazio");
		TestUtil.useItem(h, player, p, new ItemStack(Items.WATER_BUCKET));
		h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "cheia: o segundo balde não entra (nem se despeja)");
		TestUtil.useItem(h, player, p, new ItemStack(Items.BUCKET));
		h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "o balde vazio tirou a água");
		h.assertValueEqual(amount(h, p), 0L, "vazia");
		h.assertBlockProperty(p, WaterFurniture.WATER, false);
		h.succeed();
	}

	@GameTest
	public void sinkGivesWaterBottles(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.SINK);
		insert(storage(h, p), FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET);
		ServerPlayer player = TestUtil.survivor(h, p.north());
		TestUtil.useItem(h, player, p, new ItemStack(Items.GLASS_BOTTLE));
		h.assertTrue(WaterFurniture.isWaterBottle(player.getItemInHand(InteractionHand.MAIN_HAND)), "um frasco de água");
		h.assertValueEqual(amount(h, p), FluidConstants.BUCKET - FluidConstants.BOTTLE, "tirou 1/3 de balde");
		h.succeed();
	}

	@GameTest
	public void sinkThroughTransferApi(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.SINK);
		Storage<FluidVariant> s = storage(h, p);
		h.assertTrue(s != null, "a pia expõe FluidStorage.SIDED (os canos de fluidos ligam-se)");
		h.assertValueEqual(insert(s, FluidVariant.of(Fluids.WATER), 2 * FluidConstants.BUCKET), FluidConstants.BUCKET, "cabe 1 balde");
		h.assertBlockProperty(p, WaterFurniture.WATER, true);
		h.assertValueEqual(extract(s, FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET), FluidConstants.BUCKET, "tira 1 balde");
		h.assertBlockProperty(p, WaterFurniture.WATER, false);
		h.succeed();
	}

	@GameTest
	public void lavaIsRefused(GameTestHelper h) {
		BlockPos sink = new BlockPos(1, 1, 1), tub = new BlockPos(3, 1, 1);
		h.setBlock(sink, MoveisBlocks.SINK);
		h.setBlock(tub, MoveisBlocks.BATHTUB.defaultBlockState().setValue(BathtubBlock.PART, BedPart.HEAD));
		h.setBlock(tub.south(), MoveisBlocks.BATHTUB.defaultBlockState().setValue(BathtubBlock.PART, BedPart.FOOT));
		for (BlockPos p : new BlockPos[] {sink, tub}) {
			h.assertValueEqual(insert(storage(h, p), FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET), 0L, "a Transfer API recusa a lava");
			h.assertValueEqual(amount(h, p), 0L, "continua vazia");
		}
		ServerPlayer player = TestUtil.survivor(h, sink.north());
		TestUtil.useItem(h, player, sink, new ItemStack(Items.LAVA_BUCKET));
		h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.LAVA_BUCKET), "o balde de lava fica cheio");
		h.assertValueEqual(amount(h, sink), 0L, "sem lava na pia");
		for (BlockPos p : new BlockPos[] {sink.north(), sink.above(), sink}) h.assertTrue(!h.getBlockState(p).is(Blocks.LAVA), "nem despejada no mundo");
		h.succeed();
	}

	@GameTest
	public void bathtubHoldsThreeBucketsByBothHalves(GameTestHelper h) {
		BlockPos foot = new BlockPos(1, 1, 1), head = new BlockPos(1, 1, 2);
		for (BlockPos p : new BlockPos[] {foot, head}) h.setBlock(p.below(), Blocks.STONE);
		ServerPlayer player = TestUtil.survivor(h, new BlockPos(1, 1, 0));
		h.assertTrue(TestUtil.placeWithItem(h, player, new ItemStack(MoveisBlocks.BATHTUB), foot, 0f).consumesAction(), "posta");
		h.assertBlockProperty(head, BathtubBlock.PART, BedPart.HEAD);
		h.assertValueEqual(insert(storage(h, foot), FluidVariant.of(Fluids.WATER), 5 * FluidConstants.BUCKET), 3 * FluidConstants.BUCKET,
				"pelos pés: cabem 3 baldes");
		h.assertBlockProperty(foot, WaterFurniture.WATER, true);
		h.assertBlockProperty(head, WaterFurniture.WATER, true);
		h.assertValueEqual(extract(storage(h, head), FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET), FluidConstants.BUCKET, "pela cabeceira");
		TestUtil.useItem(h, player, foot, new ItemStack(Items.BUCKET));
		h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "o balde tira");
		h.assertValueEqual(h.getBlockEntity(head, WaterBlockEntity.class).tank.amount, FluidConstants.BUCKET, "fica 1 balde");
		h.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bathtubWaterPutsOutFire(GameTestHelper h) {
		BlockPos head = new BlockPos(1, 1, 2), foot = new BlockPos(1, 1, 1);
		h.setBlock(head, MoveisBlocks.BATHTUB.defaultBlockState().setValue(BathtubBlock.PART, BedPart.HEAD));
		h.setBlock(foot, MoveisBlocks.BATHTUB.defaultBlockState().setValue(BathtubBlock.PART, BedPart.FOOT));
		insert(storage(h, head), FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET);
		Pig pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, head);
		pig.igniteForSeconds(8);
		h.assertTrue(pig.isOnFire(), "a arder");
		h.succeedWhen(() -> h.assertTrue(!pig.isOnFire(), "a água da banheira apagou o fogo"));
	}
}
