package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.entity.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Um móvel onde se senta (cadeira, banco, sofá): clique direito com a mão vazia (ou um item que não se use no bloco). */
public interface SeatBlock {
	/** Altura do assento, em blocos (o jogador fica sentado aqui). */
	double seatHeight(BlockState state);

	/** Para onde o jogador fica virado ao sentar (a frente do móvel; o corpo fica assim e a cabeça roda até ±90°). */
	float seatYaw(BlockState state, Player player);

	/** Senta o jogador. Agachado não senta (deixa pôr blocos em cima, como nas mesas de trabalho). */
	static InteractionResult sit(SeatBlock block, BlockState state, Level level, BlockPos pos, Player player) {
		if (player.isShiftKeyDown() || player.isPassenger() || player.isSpectator()) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (SeatEntity.occupied(level, pos)) {
			player.sendOverlayMessage(Component.translatable("message.tmoveis.occupied"));
			return InteractionResult.SUCCESS;
		}
		float yaw = block.seatYaw(state, player);
		SeatEntity seat = SeatEntity.create(level, pos, block.seatHeight(state), yaw);
		level.addFreshEntity(seat);
		if (!player.startRiding(seat)) {
			seat.discard();
			return InteractionResult.SUCCESS;
		}
		// 0.3.1: o jogador vira-se para a frente do móvel (antes ficava virado para onde olhava: de costas para a mesa, com as pernas a
		// sair pelo encosto). No ServerPlayer, forceSetRotation também manda o pacote de rotação ao cliente (é quem manda na câmara).
		player.forceSetRotation(yaw, false, 0f, true);
		player.setYBodyRot(yaw);
		return InteractionResult.SUCCESS;
	}
}
