package com.tcordeiro.moveis.test;

import java.lang.reflect.Method;

import com.tcordeiro.moveis.block.entity.MailboxBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.finish.Finish;
import com.tcordeiro.moveis.finish.FinishState;
import com.tcordeiro.moveis.finish.Tone;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;
import com.tcordeiro.moveis.wood.WoodTypes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;

/**
 * Segurança (0.3.3). C4: a caixa de correio protegida como o cofre (só o dono a parte; explosões, funis, canos e a Transfer API não
 * a esvaziam). M4: o pincel e o machado respeitam terrenos e proteções. M5: o HUD (Receitário) não mostra o conteúdo do cofre nem do
 * correio a quem não é o dono.
 */
public class SecurityGameTests {
	static MailboxBlockEntity mailbox(GameTestHelper h, BlockPos p, ServerPlayer owner) {
		h.setBlock(p, MoveisBlocks.MAILBOX);
		MailboxBlockEntity box = h.getBlockEntity(p, MailboxBlockEntity.class);
		box.setOwner(owner);
		box.deposit(new ItemStack(Items.DIAMOND, 4));
		return box;
	}

	// ---------------------------------------------------------------- C4

	@GameTest
	public void mailboxCannotBeBrokenByOthers(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p.below(), Blocks.STONE);
		ServerPlayer owner = TestUtil.survivor(h, p.north()), other = TestUtil.survivor(h, p.west());
		mailbox(h, p, owner);
		h.assertTrue(h.getBlockState(p).getDestroyProgress(other, h.getLevel(), h.absolutePos(p)) == 0f, "a picareta do outro não avança");
		h.assertTrue(!other.gameMode.destroyBlock(h.absolutePos(p)), "o outro não parte (PlayerBlockBreakEvents.BEFORE)");
		h.assertBlockPresent(MoveisBlocks.MAILBOX, p);
		h.assertValueEqual(h.getBlockEntity(p, MailboxBlockEntity.class).countItem(Items.DIAMOND), 4, "o correio ficou lá");
		h.assertTrue(h.getBlockState(p).getDestroyProgress(owner, h.getLevel(), h.absolutePos(p)) > 0f, "o dono consegue partir");
		h.assertTrue(owner.gameMode.destroyBlock(h.absolutePos(p)), "o dono parte");
		h.assertBlockNotPresent(MoveisBlocks.MAILBOX, p);
		h.assertItemEntityPresent(Items.DIAMOND, p, 2);
		h.succeed();
	}

	@GameTest
	public void mailboxSurvivesExplosions(GameTestHelper h) {
		BlockPos p = new BlockPos(2, 2, 2);
		ServerPlayer owner = TestUtil.survivor(h, new BlockPos(0, 1, 0));
		mailbox(h, p, owner);
		var c = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(p));
		h.getLevel().explode(null, c.x + 1, c.y, c.z, 4f, Level.ExplosionInteraction.TNT);
		h.assertBlockPresent(MoveisBlocks.MAILBOX, p);
		h.assertValueEqual(h.getBlockEntity(p, MailboxBlockEntity.class).countItem(Items.DIAMOND), 4, "a explosão não tirou o correio");
		h.assertTrue(h.getBlockState(p).is(BlockTags.WITHER_IMMUNE) && h.getBlockState(p).is(BlockTags.DRAGON_IMMUNE), "nem o Wither nem o dragão");
		h.setBlock(p.east(), MoveisBlocks.SAFE);
		h.assertTrue(h.getBlockState(p.east()).is(BlockTags.WITHER_IMMUNE), "o cofre também");
		h.succeed();
	}

	/** Funis de outros mods, canos do Industrial, redes do Storage: a Transfer API (com e sem lado) só põe na caixa e não toca no cofre. */
	@GameTest
	public void transferApiCannotEmptyMailboxOrSafe(GameTestHelper h) {
		BlockPos mp = new BlockPos(1, 1, 1), sp = new BlockPos(3, 1, 1);
		ServerPlayer owner = TestUtil.survivor(h, new BlockPos(0, 1, 0));
		mailbox(h, mp, owner);
		OwnerGameTests.placeSafe(h, sp, owner);
		Direction[] sides = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		for (Direction d : sides) {
			Storage<ItemVariant> mail = ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(mp), d);
			Storage<ItemVariant> safe = ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(sp), d);
			h.assertTrue(mail != null && safe != null, "há armazenamento (" + d + ")");
			try (Transaction t = Transaction.openOuter()) {
				for (var view : mail) if (!view.isResourceBlank())
					h.assertValueEqual(view.extract(view.getResource(), 64, t), 0L, "nada sai da caixa de correio (" + d + ")");
				h.assertValueEqual(mail.extract(ItemVariant.of(Items.DIAMOND), 64, t), 0L, "nem diamantes da caixa (" + d + ")");
				h.assertValueEqual(safe.extract(ItemVariant.of(Items.EMERALD), 64, t), 0L, "nada sai do cofre (" + d + ")");
				h.assertValueEqual(safe.insert(ItemVariant.of(Items.DIRT), 1, t), 0L, "nada entra no cofre (" + d + ")");
				h.assertValueEqual(mail.insert(ItemVariant.of(Items.PAPER), 1, t), 1L, "a caixa recebe correio (" + d + ")");
			}
		}
		// os canos do Industrial (se estiver instalado) pedem o inventário por PipeStorage.find
		try {
			Class<?> ps = Class.forName("com.tcordeiro.industrial.pipe.PipeStorage");
			Method find = null;
			for (Method m : ps.getMethods()) if (m.getName().equals("find") && m.getParameterCount() == 4) find = m;
			if (find != null) for (Direction d : Direction.values()) {
				@SuppressWarnings("unchecked")
				Storage<ItemVariant> s = (Storage<ItemVariant>) find.invoke(null, h.getLevel(), h.absolutePos(mp), d, null);
				try (Transaction t = Transaction.openOuter()) {
					h.assertValueEqual(s == null ? 0L : s.extract(ItemVariant.of(Items.DIAMOND), 64, t), 0L, "um cano do Industrial não tira (" + d + ")");
				}
			}
		} catch (ClassNotFoundException e) {
			// sem o Industrial: só a Transfer API
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		h.assertValueEqual(h.getBlockEntity(mp, MailboxBlockEntity.class).countItem(Items.DIAMOND), 4, "o correio está todo lá");
		h.assertValueEqual(h.getBlockEntity(sp, StorageFurnitureBlockEntity.class).countItem(Items.EMERALD), 5, "o cofre está todo lá");
		h.succeed();
	}

	// ---------------------------------------------------------------- M5

	@GameTest
	public void onlyOwnerCanOpenSafeAndMailbox(GameTestHelper h) {
		BlockPos mp = new BlockPos(1, 1, 1), sp = new BlockPos(3, 1, 1), cp = new BlockPos(5, 1, 1);
		ServerPlayer owner = TestUtil.survivor(h, new BlockPos(2, 1, 0)), other = TestUtil.survivor(h, new BlockPos(2, 1, 2));
		MailboxBlockEntity box = mailbox(h, mp, owner);
		OwnerGameTests.placeSafe(h, sp, owner);
		StorageFurnitureBlockEntity safe = h.getBlockEntity(sp, StorageFurnitureBlockEntity.class);
		h.setBlock(cp, MoveisBlocks.WOODEN.get(Furniture.CABINET).get(WoodTypes.ALL.get(0).id()));
		StorageFurnitureBlockEntity cabinet = h.getBlockEntity(cp, StorageFurnitureBlockEntity.class);
		h.assertTrue(box.canOpen(owner) && safe.canOpen(owner), "o dono abre os dois");
		h.assertTrue(!box.canOpen(other), "o outro não abre a caixa de correio a sério (canOpen)");
		h.assertTrue(!safe.canOpen(other), "o outro não abre o cofre (canOpen)");
		h.assertTrue(cabinet.canOpen(other), "um armário qualquer continua aberto a todos");
		// a caixa de correio continua a aceitar depósitos dos outros
		other.openMenu(box);
		h.assertTrue(other.containerMenu instanceof MailboxBlockEntity.DepositMenu, "o outro ainda deposita");
		other.closeContainer();
		h.succeed();
	}

	/** O HUD do Receitário (se estiver no jogo dos testes): o resumo do conteúdo só vai para o dono. */
	@GameTest
	public void hudHidesContentsFromOthers(GameTestHelper h) {
		Method probeFor;
		try {
			probeFor = Class.forName("com.tcordeiro.receitario.net.ProbePayload").getMethod("probeFor", ServerPlayer.class, BlockPos.class);
		} catch (ReflectiveOperationException e) {
			h.succeed(); // sem o Receitário nos testes: a regra está em canOpen (onlyOwnerCanOpenSafeAndMailbox)
			return;
		}
		BlockPos mp = new BlockPos(1, 1, 1), sp = new BlockPos(3, 1, 1);
		ServerPlayer owner = TestUtil.survivor(h, new BlockPos(2, 1, 0)), other = TestUtil.survivor(h, new BlockPos(2, 1, 2));
		mailbox(h, mp, owner);
		OwnerGameTests.placeSafe(h, sp, owner);
		for (BlockPos p : new BlockPos[] {mp, sp}) {
			int ownerUsed = used(probeFor, owner, h.absolutePos(p)), otherUsed = used(probeFor, other, h.absolutePos(p));
			h.assertTrue(ownerUsed > 0, "o dono vê o conteúdo no HUD (" + p + "): " + ownerUsed);
			h.assertValueEqual(otherUsed, -1, "o outro não vê o conteúdo no HUD (" + p + ")");
		}
		h.succeed();
	}

	static int used(Method probeFor, ServerPlayer p, BlockPos abs) {
		try {
			p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(abs));
			Object reply = probeFor.invoke(null, p, abs);
			if (reply == null) return Integer.MIN_VALUE;
			return (int) reply.getClass().getMethod("used").invoke(reply);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// ---------------------------------------------------------------- M4

	static Block chair() {
		return MoveisBlocks.WOODEN.get(Furniture.CHAIR).get(WoodTypes.ALL.get(0).id());
	}

	/** Modo aventura (como num mapa ou num terreno protegido): o pincel e o machado não mexem, nem pelo clique normal. */
	@GameTest
	public void brushRespectsAdventureMode(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p.below(), Blocks.STONE);
		h.setBlock(p, chair());
		ServerPlayer pl = TestUtil.survivor(h, p.north(2));
		pl.setGameMode(GameType.ADVENTURE);
		h.assertTrue(FinishGameTests.brushAt(h, pl, p, Tone.WALNUT, FinishGameTests.brush(Tone.WALNUT)) == InteractionResult.FAIL, "o pincel recusa");
		TestUtil.useItem(h, pl, p, FinishGameTests.brush(Tone.WALNUT));
		h.assertValueEqual(FinishState.of(h.getBlockState(p)), Finish.RAW, "nem pelo clique do jogo");
		// o mesmo jogador em sobrevivência consegue (o teste não é vácuo)
		pl.setGameMode(GameType.SURVIVAL);
		h.assertTrue(FinishGameTests.brushAt(h, pl, p, Tone.WALNUT, FinishGameTests.brush(Tone.WALNUT)).consumesAction(), "em sobrevivência pinta");
		h.assertValueEqual(FinishState.of(h.getBlockState(p)), Finish.WALNUT, "envernizada");
		pl.setGameMode(GameType.ADVENTURE);
		FinishGameTests.axeAt(h, pl, p);
		h.assertValueEqual(FinishState.of(h.getBlockState(p)), Finish.WALNUT, "o machado também não raspa em aventura");
		h.succeed();
	}

	/** Fora da borda do mundo (Level.mayInteract: a mesma regra da proteção do spawn e dos mods de terrenos). No End: a borda é só dele. */
	@GameTest(dimension = "minecraft:the_end")
	public void brushRespectsMayInteract(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p.below(), Blocks.STONE);
		h.setBlock(p, chair());
		ServerPlayer pl = TestUtil.survivor(h, p.north(2));
		WorldBorder border = h.getLevel().getWorldBorder();
		double cx = border.getCenterX(), cz = border.getCenterZ(), size = border.getSize();
		BlockPos abs = h.absolutePos(p);
		try {
			border.setCenter(abs.getX() + 5000, abs.getZ() + 5000);
			border.setSize(16);
			h.assertTrue(!h.getLevel().mayInteract(pl, abs), "o bloco está fora da borda");
			h.assertTrue(FinishGameTests.brushAt(h, pl, p, Tone.HONEY, FinishGameTests.brush(Tone.HONEY)) == InteractionResult.FAIL, "o pincel recusa");
			TestUtil.useItem(h, pl, p, FinishGameTests.brush(Tone.HONEY));
			h.assertValueEqual(FinishState.of(h.getBlockState(p)), Finish.RAW, "nem pelo clique do jogo");
		} finally {
			border.setCenter(cx, cz);
			border.setSize(size);
		}
		h.assertTrue(FinishGameTests.brushAt(h, pl, p, Tone.HONEY, FinishGameTests.brush(Tone.HONEY)).consumesAction(), "dentro da borda pinta");
		h.succeed();
	}
}
