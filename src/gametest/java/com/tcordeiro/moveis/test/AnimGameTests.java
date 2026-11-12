package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.anim.Anim;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.MailboxBlock;
import com.tcordeiro.moveis.block.ToggleFurnitureBlock;
import com.tcordeiro.moveis.block.WardrobeBlock;
import com.tcordeiro.moveis.block.entity.AnimatedBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Animações: abrir e fechar mexe o estado (MOVING enquanto as partes andam, como a tampa do baú), a animação chega ao fim em
 * {@link Anim#TICKS} ticks e o modelo estático volta. Guarda-roupa (as duas metades), cortinas vizinhas juntas, persianas, bandeirinha.
 */
public class AnimGameTests {
	static boolean draws(GameTestHelper h, BlockPos p) {
		BlockState st = h.getBlockState(p);
		return ((AnimatedFurniture) st.getBlock()).drawsParts(st);
	}

	@GameTest
	public void cabinetDoorsOpenAndCloseSmoothly(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.wooden(Furniture.CABINET, "oak"));
		ServerPlayer player = TestUtil.playerAt(h, p.north());
		StorageFurnitureBlockEntity be = h.getBlockEntity(p, StorageFurnitureBlockEntity.class);
		h.assertTrue(!draws(h, p) && be.anim().settled(), "fechado e parado: só o modelo estático");
		player.openMenu(be);
		h.assertBlockProperty(p, AnimatedFurniture.OPEN, true);
		h.assertBlockProperty(p, AnimatedFurniture.MOVING, true);
		h.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					float pr = be.anim().progress(1f);
					h.assertTrue(pr > 0 && pr < 1, "a meio da animação (" + pr + ")");
					h.assertTrue(draws(h, p), "a mexer: o renderer desenha as portas");
				})
				.thenIdle(Anim.TICKS + 1)
				.thenExecute(() -> {
					h.assertValueEqual(be.anim().progress(1f), 1f, "aberto ao fim da animação");
					h.assertBlockProperty(p, AnimatedFurniture.MOVING, false);
					h.assertTrue(draws(h, p), "aberto (sem modelo estático aberto): o renderer mantém as portas abertas");
					player.closeContainer();
					h.assertBlockProperty(p, AnimatedFurniture.OPEN, false);
					h.assertBlockProperty(p, AnimatedFurniture.MOVING, true);
				})
				.thenIdle(Anim.TICKS + 2)
				.thenExecute(() -> {
					h.assertValueEqual(be.anim().progress(1f), 0f, "fechado");
					h.assertBlockProperty(p, AnimatedFurniture.MOVING, false);
					h.assertTrue(!draws(h, p), "parado e fechado: o renderer não corre");
				})
				.thenSucceed();
	}

	@GameTest
	public void wardrobeHasTwoHalvesAndOpensBoth(GameTestHelper h) {
		BlockPos low = new BlockPos(1, 1, 1), up = low.above();
		h.setBlock(low.below(), Blocks.STONE);
		ServerPlayer player = TestUtil.playerAt(h, low.north());
		Block w = MoveisBlocks.wooden(Furniture.WARDROBE, "spruce");
		h.assertTrue(TestUtil.placeWithItem(h, player, new ItemStack(w), low, 0f).consumesAction(), "posto");
		h.assertBlockProperty(low, WardrobeBlock.HALF, DoubleBlockHalf.LOWER);
		h.assertBlockProperty(up, WardrobeBlock.HALF, DoubleBlockHalf.UPPER);
		StorageFurnitureBlockEntity be = h.getBlockEntity(low, StorageFurnitureBlockEntity.class);
		h.assertValueEqual(be.getContainerSize(), 36, "36 espaços");
		h.useBlock(up, player); // clicar em cima abre o inventário de baixo
		h.assertTrue(player.containerMenu instanceof ChestMenu m && m.getContainer() == be && m.getRowCount() == 4, "ecrã de 4 linhas");
		h.assertBlockProperty(low, AnimatedFurniture.OPEN, true);
		h.assertBlockProperty(up, AnimatedFurniture.OPEN, true);
		h.assertBlockProperty(up, AnimatedFurniture.MOVING, true);
		h.startSequence().thenIdle(Anim.TICKS + 2).thenExecute(() -> {
			h.assertValueEqual(h.getBlockEntity(up, AnimatedBlockEntity.class).anim().progress(1f), 1f, "a metade de cima também abriu");
			player.closeContainer();
			h.destroyBlock(up);
			h.assertBlockNotPresent(w, low);
			h.assertItemEntityCountIs(w.asItem(), low, 3, 1);
		}).thenSucceed();
	}

	@GameTest
	public void neighbouringCurtainsMoveTogether(GameTestHelper h) {
		Block red = MoveisBlocks.CURTAINS.get(DyeColor.RED), blue = MoveisBlocks.CURTAINS.get(DyeColor.BLUE);
		BlockState north = red.defaultBlockState();
		for (int x = 0; x <= 2; x++) h.setBlock(new BlockPos(x, 1, 1), x == 2 ? blue.defaultBlockState() : north);
		h.setBlock(new BlockPos(1, 2, 1), north); // por cima (cortina alta)
		h.setBlock(new BlockPos(3, 1, 1), north.setValue(ToggleFurnitureBlock.FACING, Direction.EAST)); // virada para outro lado
		h.setBlock(new BlockPos(5, 1, 1), north); // longe (não é vizinha)
		ServerPlayer player = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		h.useBlock(new BlockPos(1, 1, 1), player);
		for (BlockPos p : new BlockPos[] {new BlockPos(0, 1, 1), new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(1, 2, 1)}) {
			h.assertBlockProperty(p, AnimatedFurniture.OPEN, true);
			h.assertBlockProperty(p, AnimatedFurniture.MOVING, true);
		}
		h.assertBlockProperty(new BlockPos(3, 1, 1), AnimatedFurniture.OPEN, false);
		h.assertBlockProperty(new BlockPos(5, 1, 1), AnimatedFurniture.OPEN, false);
		h.startSequence().thenIdle(Anim.TICKS + 2).thenExecute(() -> {
			h.assertBlockProperty(new BlockPos(0, 1, 1), AnimatedFurniture.MOVING, false);
			h.assertTrue(!draws(h, new BlockPos(0, 1, 1)), "aberta e parada: modelo estático aberto");
			h.useBlock(new BlockPos(0, 1, 1), player);
			h.assertBlockProperty(new BlockPos(2, 1, 1), AnimatedFurniture.OPEN, false);
		}).thenSucceed();
	}

	@GameTest
	public void shuttersToggleOnlyThemselves(GameTestHelper h) {
		Block s = MoveisBlocks.wooden(Furniture.SHUTTERS, "birch");
		h.setBlock(new BlockPos(1, 1, 1), s);
		h.setBlock(new BlockPos(2, 1, 1), s);
		ServerPlayer player = TestUtil.playerAt(h, new BlockPos(1, 1, 0));
		h.useBlock(new BlockPos(1, 1, 1), player);
		h.assertBlockProperty(new BlockPos(1, 1, 1), AnimatedFurniture.OPEN, true);
		h.assertBlockProperty(new BlockPos(1, 1, 1), AnimatedFurniture.MOVING, true);
		h.assertBlockProperty(new BlockPos(2, 1, 1), AnimatedFurniture.OPEN, false);
		h.startSequence().thenIdle(Anim.TICKS + 2).thenExecute(() -> h.assertBlockProperty(new BlockPos(1, 1, 1), AnimatedFurniture.MOVING, false))
				.thenSucceed();
	}

	@GameTest
	public void mailboxFlagRisesWithMail(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.MAILBOX);
		StorageFurnitureBlockEntity be = h.getBlockEntity(p, StorageFurnitureBlockEntity.class);
		be.setItem(0, new ItemStack(net.minecraft.world.item.Items.PAPER));
		be.setChanged();
		h.assertBlockProperty(p, MailboxBlock.HAS_MAIL, true);
		h.assertBlockProperty(p, AnimatedFurniture.MOVING, true);
		h.startSequence().thenIdle(Anim.TICKS + 2).thenExecute(() -> {
			h.assertBlockProperty(p, AnimatedFurniture.MOVING, false);
			h.assertTrue(!draws(h, p), "bandeirinha em cima e parada: modelo estático");
			be.clearContent();
			be.setChanged();
			h.assertBlockProperty(p, MailboxBlock.HAS_MAIL, false);
			h.assertBlockProperty(p, AnimatedFurniture.MOVING, true);
		}).thenSucceed();
	}
}
