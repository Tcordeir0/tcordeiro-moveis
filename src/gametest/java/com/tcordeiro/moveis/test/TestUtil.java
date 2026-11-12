package com.tcordeiro.moveis.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class TestUtil {
	private TestUtil() {}

	/** Põe o bloco e deixa-o ver os vizinhos (como se tivesse sido posto à mão: updateShape para os 6 lados). */
	static void place(GameTestHelper h, BlockPos rel, BlockState state) {
		h.setBlock(rel, state);
		BlockPos abs = h.absolutePos(rel);
		BlockState now = Block.updateFromNeighbourShapes(state, h.getLevel(), abs);
		if (now != state) h.setBlock(rel, now);
	}

	/** Põe com o item, como um jogador virado para {@code yaw} a clicar no topo do bloco de baixo. */
	static InteractionResult placeWithItem(GameTestHelper h, ServerPlayer player, ItemStack stack, BlockPos rel, float yaw) {
		BlockPos abs = h.absolutePos(rel);
		player.setYRot(yaw);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs.below()).add(0, 0.5, 0), Direction.UP, abs.below(), false);
		return ((BlockItem) stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
	}

	/** Um jogador em modo sobrevivência (os baldes e as pilhas gastam-se como no jogo normal). */
	static ServerPlayer survivor(GameTestHelper h, BlockPos rel) {
		ServerPlayer p = playerAt(h, rel);
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		return p;
	}

	/** Usa o item na mão sobre o bloco, como o clique direito do jogo (bloco primeiro, depois o item). */
	static InteractionResult useItem(GameTestHelper h, ServerPlayer p, BlockPos rel, ItemStack stack) {
		return useItemAt(h, p, rel, stack, Vec3.atCenterOf(h.absolutePos(rel)));
	}

	static InteractionResult useItemAt(GameTestHelper h, ServerPlayer p, BlockPos rel, ItemStack stack, Vec3 hitAbs) {
		p.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos abs = h.absolutePos(rel);
		BlockHitResult hit = new BlockHitResult(hitAbs, Direction.UP, abs, false);
		return p.gameMode.useItemOn(p, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
	}

	/** Clique de mão vazia num ponto exato do bloco (piano, adega). */
	static InteractionResult useEmptyAt(GameTestHelper h, ServerPlayer p, BlockPos rel, Vec3 hitAbs) {
		return useItemAt(h, p, rel, ItemStack.EMPTY, hitAbs);
	}

	static ServerPlayer playerAt(GameTestHelper h, BlockPos rel) {
		ServerPlayer p = h.makeMockServerPlayerInLevel();
		Vec3 v = Vec3.atBottomCenterOf(h.absolutePos(rel));
		p.setPos(v.x, v.y, v.z);
		return p;
	}
}
