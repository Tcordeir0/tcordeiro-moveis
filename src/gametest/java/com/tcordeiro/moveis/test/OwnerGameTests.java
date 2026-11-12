package com.tcordeiro.moveis.test;

import com.tcordeiro.moveis.block.MailboxBlock;
import com.tcordeiro.moveis.block.entity.MailboxBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.mail.MailData;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Regras de dono. Caixa de correio: qualquer um põe, só o dono tira (nem um funil tira). Cofre-forte: outro jogador não abre nem
 * parte, explosões não o partem, os funis não lhe tiram nada; o dono parte e o conteúdo cai.
 */
public class OwnerGameTests {
	@GameTest
	public void mailboxAnyonePutsOnlyOwnerTakes(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p, MoveisBlocks.MAILBOX);
		ServerPlayer owner = TestUtil.survivor(h, p.north()), other = TestUtil.survivor(h, p.west());
		MailboxBlockEntity box = h.getBlockEntity(p, MailboxBlockEntity.class);
		box.setOwner(owner);
		// o outro jogador abre: recebe um depósito vazio, não a caixa
		other.openMenu(box);
		h.assertTrue(other.containerMenu instanceof MailboxBlockEntity.DepositMenu, "quem não é o dono só deposita");
		ChestMenu dep = (ChestMenu) other.containerMenu;
		h.assertTrue(dep.getContainer() != box, "não mexe na caixa a sério");
		dep.getSlot(0).set(new ItemStack(Items.DIAMOND, 3));
		other.closeContainer();
		h.assertValueEqual(box.countItem(Items.DIAMOND), 3, "ao fechar, o correio entrou na caixa");
		h.assertBlockProperty(p, MailboxBlock.HAS_MAIL, true);
		h.assertTrue(MailData.get(h.getLevel().getServer()).boxes(owner.getUUID()).contains(GlobalPos.of(h.getLevel().dimension(), h.absolutePos(p))),
				"registado para o aviso ao dono");
		// volta a abrir: não vê nem tira o que lá está
		other.openMenu(box);
		h.assertTrue(other.containerMenu.getSlot(0).getItem().isEmpty(), "o outro não vê o correio");
		other.closeContainer();
		h.assertValueEqual(box.countItem(Items.DIAMOND), 3, "o outro não tirou nada");
		// o dono abre a caixa a sério e tira
		owner.openMenu(box);
		h.assertTrue(owner.containerMenu instanceof ChestMenu m && m.getContainer() == box && !(m instanceof MailboxBlockEntity.DepositMenu), "o dono abre a caixa");
		h.assertTrue(owner.containerMenu.getSlot(0).mayPickup(owner) && owner.containerMenu.getSlot(0).getItem().is(Items.DIAMOND), "e pode tirar");
		owner.closeContainer();
		h.succeed();
	}

	@GameTest(maxTicks = 60)
	public void hopperFeedsMailboxButCannotEmptyIt(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 2, 1);
		h.setBlock(p, MoveisBlocks.MAILBOX);
		h.setBlock(p.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		h.getBlockEntity(p.above(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.PAPER, 2));
		h.setBlock(p.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
		h.startSequence()
				.thenWaitUntil(() -> h.assertValueEqual(h.getBlockEntity(p, StorageFurnitureBlockEntity.class).countItem(Items.PAPER), 2, "o funil pôs"))
				.thenIdle(20)
				.thenExecute(() -> {
					h.assertValueEqual(h.getBlockEntity(p, StorageFurnitureBlockEntity.class).countItem(Items.PAPER), 2, "e o de baixo não tirou");
					h.assertValueEqual(h.getBlockEntity(p.below(), HopperBlockEntity.class).countItem(Items.PAPER), 0, "funil de baixo vazio");
				})
				.thenSucceed();
	}

	static void placeSafe(GameTestHelper h, BlockPos p, ServerPlayer owner) {
		h.setBlock(p, MoveisBlocks.SAFE);
		h.getBlockEntity(p, StorageFurnitureBlockEntity.class).setOwner(owner);
		h.getBlockEntity(p, StorageFurnitureBlockEntity.class).setItem(0, new ItemStack(Items.EMERALD, 5));
	}

	@GameTest
	public void safeOnlyOwnerOpens(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		ServerPlayer owner = TestUtil.survivor(h, p.north()), other = TestUtil.survivor(h, p.west());
		placeSafe(h, p, owner);
		h.useBlock(p, other);
		h.assertTrue(other.containerMenu == other.inventoryMenu, "o outro jogador não abre o cofre");
		h.useBlock(p, owner);
		h.assertTrue(owner.containerMenu instanceof ChestMenu m && m.getContainer() == h.getBlockEntity(p, StorageFurnitureBlockEntity.class),
				"o dono abre");
		owner.closeContainer();
		h.succeed();
	}

	@GameTest
	public void safeCannotBeBrokenByOthers(GameTestHelper h) {
		BlockPos p = new BlockPos(1, 1, 1);
		h.setBlock(p.below(), Blocks.STONE);
		ServerPlayer owner = TestUtil.survivor(h, p.north()), other = TestUtil.survivor(h, p.west());
		placeSafe(h, p, owner);
		h.assertTrue(h.getBlockState(p).getDestroyProgress(other, h.getLevel(), h.absolutePos(p)) == 0f, "a picareta do outro não avança");
		h.assertTrue(!other.gameMode.destroyBlock(h.absolutePos(p)), "o outro não parte (PlayerBlockBreakEvents.BEFORE)");
		h.assertBlockPresent(MoveisBlocks.SAFE, p);
		h.assertTrue(h.getBlockState(p).getDestroyProgress(owner, h.getLevel(), h.absolutePos(p)) > 0f, "o dono consegue partir");
		h.assertTrue(owner.gameMode.destroyBlock(h.absolutePos(p)), "o dono parte");
		h.assertBlockNotPresent(MoveisBlocks.SAFE, p);
		h.assertItemEntityPresent(Items.EMERALD, p, 2);
		h.succeed();
	}

	@GameTest
	public void safeSurvivesExplosionsAndHoppers(GameTestHelper h) {
		BlockPos p = new BlockPos(2, 2, 2);
		ServerPlayer owner = TestUtil.survivor(h, new BlockPos(0, 1, 0));
		placeSafe(h, p, owner);
		h.setBlock(p.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
		var c = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(p));
		h.getLevel().explode(null, c.x + 1, c.y, c.z, 4f, Level.ExplosionInteraction.TNT);
		h.assertBlockPresent(MoveisBlocks.SAFE, p);
		StorageFurnitureBlockEntity be = h.getBlockEntity(p, StorageFurnitureBlockEntity.class);
		for (Direction d : Direction.values()) {
			h.assertValueEqual(be.getSlotsForFace(d).length, 0, "os funis não veem os espaços do cofre (" + d + ")");
			h.assertTrue(!be.canTakeItemThroughFace(0, be.getItem(0), d) && !be.canPlaceItemThroughFace(0, new ItemStack(Items.DIRT), d),
					"nem tiram nem põem");
		}
		h.startSequence().thenIdle(20).thenExecute(() -> {
			h.assertValueEqual(h.getBlockEntity(p, StorageFurnitureBlockEntity.class).countItem(Items.EMERALD), 5, "nem a explosão nem um funil tiraram");
		}).thenSucceed();
	}
}
