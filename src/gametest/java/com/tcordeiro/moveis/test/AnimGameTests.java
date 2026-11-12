package com.tcordeiro.moveis.test;

import net.minecraft.world.level.block.entity.BlockEntity;
import com.tcordeiro.moveis.block.CeilingFanBlock;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.block.entity.Animated;
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
 * Animações: abrir e fechar mexe as partes (a mexer: a entidade diz ao modelo do chunk, 0.3.3), a animação chega ao fim em
 * {@link Anim#TICKS} ticks e o modelo estático volta. Guarda-roupa (as duas metades), cortinas vizinhas juntas, persianas, bandeirinha.
 */
public class AnimGameTests {
	/** Espera até a animação acabar E passar a graça do renderer. */
	static final int SETTLE = Anim.TICKS + Anim.GRACE + 3;

	static boolean draws(GameTestHelper h, BlockPos p) {
		BlockState st = h.getBlockState(p);
		return ((AnimatedFurniture) st.getBlock()).drawsParts(st, ((Animated) h.getBlockEntity(p, BlockEntity.class)).anim());
	}

	/** As partes estão a mexer? (0.3.3: vem da entidade, já não é a propriedade MOVING do blockstate) */
	static boolean moving(GameTestHelper h, BlockPos p) {
		BlockState st = h.getBlockState(p);
		BlockEntity be = h.getBlockEntity(p, BlockEntity.class);
		if (be instanceof LampBlockEntity lamp) return CeilingFanBlock.moving(lamp);
		return ((AnimatedFurniture) st.getBlock()).moving(st, ((Animated) be).anim());
	}

	static void assertMoving(GameTestHelper h, BlockPos p, boolean want) {
		h.assertValueEqual(moving(h, p), want, "a mexer em " + p);
		h.assertValueEqual(h.getBlockEntity(p, BlockEntity.class).getRenderData(), (Object) want, "o modelo do chunk recebe o mesmo (getRenderData)");
	}

	@GameTest(maxTicks = 100)
	public void cabinetDoorsOpenAndCloseSmoothly(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.wooden(Furniture.CABINET, "oak"));
		ServerPlayer player = TestUtil.playerAt(h, p.north());
		StorageFurnitureBlockEntity be = h.getBlockEntity(p, StorageFurnitureBlockEntity.class);
		h.assertTrue(!draws(h, p) && be.anim().settled(), "fechado e parado: só o modelo estático");
		player.openMenu(be);
		h.assertBlockProperty(p, AnimatedFurniture.OPEN, true);
		assertMoving(h, p, true);
		h.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					float pr = be.anim().progress(1f);
					h.assertTrue(pr > 0 && pr < 1, "a meio da animação (" + pr + ")");
					h.assertTrue(draws(h, p), "a mexer: o renderer desenha as portas");
				})
				.thenIdle(SETTLE)
				.thenExecute(() -> {
					h.assertValueEqual(be.anim().progress(1f), 1f, "aberto ao fim da animação");
					assertMoving(h, p, false);
					h.assertTrue(draws(h, p), "aberto (sem modelo estático aberto): o renderer mantém as portas abertas");
					player.closeContainer();
					h.assertBlockProperty(p, AnimatedFurniture.OPEN, false);
					assertMoving(h, p, true);
				})
				.thenIdle(SETTLE)
				.thenExecute(() -> {
					h.assertValueEqual(be.anim().progress(1f), 0f, "fechado");
					assertMoving(h, p, false);
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
		assertMoving(h, up, true);
		h.startSequence().thenIdle(SETTLE).thenExecute(() -> {
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
			assertMoving(h, p, true);
		}
		h.assertBlockProperty(new BlockPos(3, 1, 1), AnimatedFurniture.OPEN, false);
		h.assertBlockProperty(new BlockPos(5, 1, 1), AnimatedFurniture.OPEN, false);
		h.startSequence().thenIdle(SETTLE).thenExecute(() -> {
			assertMoving(h, new BlockPos(0, 1, 1), false);
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
		assertMoving(h, new BlockPos(1, 1, 1), true);
		h.assertBlockProperty(new BlockPos(2, 1, 1), AnimatedFurniture.OPEN, false);
		h.startSequence().thenIdle(SETTLE).thenExecute(() -> assertMoving(h, new BlockPos(1, 1, 1), false))
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
		assertMoving(h, p, true);
		h.startSequence().thenIdle(SETTLE).thenExecute(() -> {
			assertMoving(h, p, false);
			h.assertTrue(!draws(h, p), "bandeirinha em cima e parada: modelo estático");
			be.clearContent();
			be.setChanged();
			h.assertBlockProperty(p, MailboxBlock.HAS_MAIL, false);
			assertMoving(h, p, true);
		}).thenSucceed();
	}

	/**
	 * 0.3.3: no fim da animação o modelo do chunk volta logo ao inteiro (getRenderData = false), mas o renderer ainda desenha as partes
	 * {@link Anim#GRACE} ticks: o chunk só se refaz no fotograma seguinte e sem isto as partes sumiam 1 fotograma (cortinas, persianas,
	 * cofre).
	 */
	@GameTest(maxTicks = 60)
	public void partsKeepDrawingForAGraceAfterTheEnd(GameTestHelper h) {
		BlockPos[] ps = {new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1)};
		h.setBlock(ps[0], MoveisBlocks.CURTAINS.get(DyeColor.RED));
		h.setBlock(ps[1], MoveisBlocks.wooden(Furniture.SHUTTERS, "oak"));
		h.setBlock(ps[2], MoveisBlocks.SAFE);
		ServerPlayer player = TestUtil.playerAt(h, new BlockPos(3, 1, 0));
		h.useBlock(ps[0], player);
		h.useBlock(ps[1], player);
		player.openMenu(h.getBlockEntity(ps[2], StorageFurnitureBlockEntity.class));
		int[] endTick = new int[1];
		h.startSequence()
				.thenWaitUntil(() -> {
					for (BlockPos p : ps) h.assertTrue(!moving(h, p), "ainda a mexer");
				})
				.thenExecute(() -> {
					for (BlockPos p : ps) h.assertTrue(draws(h, p), "acabou de parar: o renderer ainda desenha (graça) em " + p);
				})
				.thenIdle(Anim.GRACE + 1)
				.thenExecute(() -> {
					h.assertTrue(!draws(h, ps[0]) && !draws(h, ps[1]), "passada a graça, cortina e persiana abertas: só o modelo estático");
					h.assertTrue(draws(h, ps[2]), "o cofre aberto não tem modelo estático aberto: o renderer continua");
					player.closeContainer();
				})
				.thenWaitUntil(() -> h.assertTrue(!moving(h, ps[2]), "o cofre ainda a fechar"))
				.thenExecute(() -> h.assertTrue(draws(h, ps[2]), "o cofre acabou de fechar: graça"))
				.thenIdle(Anim.GRACE + 1)
				.thenExecute(() -> h.assertTrue(!draws(h, ps[2]), "fechado e parado: o renderer não corre"))
				.thenSucceed();
	}

	/** Piano (0.3.3): depois de PLAYING desligar, as teclas continuam desenhadas uns ticks (o teclado não some entre teclas). */
	@GameTest(maxTicks = 60)
	public void pianoKeysKeepDrawingForAGrace(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p.below(), Blocks.STONE);
		h.setBlock(p.below().east(), Blocks.STONE);
		ServerPlayer player = TestUtil.playerAt(h, p.north());
		h.assertTrue(TestUtil.placeWithItem(h, player, new ItemStack(MoveisBlocks.PIANO), p, 0f).consumesAction(), "piano posto");
		BlockState st = h.getBlockState(p);
		com.tcordeiro.moveis.block.PianoBlock.play(h.getLevel(), h.absolutePos(p), st, 3, player);
		com.tcordeiro.moveis.block.entity.PianoBlockEntity be = h.getBlockEntity(p, com.tcordeiro.moveis.block.entity.PianoBlockEntity.class);
		h.assertTrue(be.drawsKeys(), "a tocar: o renderer desenha as teclas");
		h.startSequence()
				.thenWaitUntil(() -> h.assertBlockProperty(p, com.tcordeiro.moveis.block.PianoBlock.PLAYING, false))
				.thenExecute(() -> h.assertTrue(be.drawsKeys(), "PLAYING acabou de desligar: graça, o teclado não some"))
				.thenIdle(Anim.GRACE + 2)
				.thenExecute(() -> h.assertTrue(!be.drawsKeys(), "passada a graça: só o modelo estático"))
				.thenSucceed();
	}
}
