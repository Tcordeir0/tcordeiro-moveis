package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Armário e gaveteiro: guardam 27, funis põem e tiram, o conteúdo cai ao partir, abrem e fecham. */
public class StorageGameTests {
	static Block cabinet() {
		return MoveisBlocks.wooden(Furniture.CABINET, "oak");
	}

	static Block drawer() {
		return MoveisBlocks.wooden(Furniture.DRAWER, "dark_oak");
	}

	@GameTest
	public void storesItems(GameTestHelper h) {
		for (Block b : new Block[] {cabinet(), drawer()}) {
			BlockPos p = b == cabinet() ? new BlockPos(1, 1, 1) : new BlockPos(3, 1, 1);
			h.setBlock(p, b);
			StorageFurnitureBlockEntity be = h.getBlockEntity(p, StorageFurnitureBlockEntity.class);
			h.assertValueEqual(be.getContainerSize(), 27, "27 espaços");
			be.setItem(26, new ItemStack(Items.APPLE, 5));
			h.assertValueEqual(h.getBlockEntity(p, StorageFurnitureBlockEntity.class).getItem(26).getCount(), 5, "guardou");
		}
		h.succeed();
	}

	@GameTest
	public void hopperPutsIn(GameTestHelper h) {
		BlockPos box = new BlockPos(1, 1, 1), hopper = box.above();
		h.setBlock(box, cabinet());
		h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		h.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, new ItemStack(Items.IRON_INGOT, 3));
		h.succeedWhen(() -> h.assertTrue(h.getBlockEntity(box, StorageFurnitureBlockEntity.class).countItem(Items.IRON_INGOT) == 3,
				"o funil por cima pôs os 3 lingotes"));
	}

	@GameTest
	public void hopperTakesOut(GameTestHelper h) {
		BlockPos box = new BlockPos(1, 2, 1), hopper = box.below();
		h.setBlock(box, drawer());
		h.getBlockEntity(box, StorageFurnitureBlockEntity.class).setItem(4, new ItemStack(Items.GOLD_INGOT, 2));
		h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
		h.succeedWhen(() -> h.assertTrue(h.getBlockEntity(hopper, HopperBlockEntity.class).countItem(Items.GOLD_INGOT) == 2,
				"o funil por baixo tirou os 2 lingotes"));
	}

	@GameTest
	public void contentsDropWhenBroken(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p.below(), Blocks.STONE);
		h.setBlock(p, cabinet());
		h.getBlockEntity(p, StorageFurnitureBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 7));
		h.getLevel().destroyBlock(h.absolutePos(p), true); // como um jogador a partir (com drops)
		h.assertItemEntityPresent(Items.DIAMOND, p, 2);
		h.assertItemEntityPresent(cabinet().asItem(), p, 2);
		h.succeed();
	}

	@GameTest
	public void opensWhileInUse(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, cabinet());
		ServerPlayer player = TestUtil.playerAt(h, p.north());
		player.openMenu(h.getBlockEntity(p, StorageFurnitureBlockEntity.class));
		h.assertBlockProperty(p, StorageFurnitureBlock.OPEN, true);
		player.closeContainer();
		h.assertBlockProperty(p, StorageFurnitureBlock.OPEN, false);
		h.succeed();
	}
}
