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

	static ServerPlayer playerAt(GameTestHelper h, BlockPos rel) {
		ServerPlayer p = h.makeMockServerPlayerInLevel();
		Vec3 v = Vec3.atBottomCenterOf(h.absolutePos(rel));
		p.setPos(v.x, v.y, v.z);
		return p;
	}
}
