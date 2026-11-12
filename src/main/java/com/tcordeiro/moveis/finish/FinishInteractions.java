package com.tcordeiro.moveis.finish;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Clique direito com o pincel ou com o machado, ANTES do bloco (UseBlockCallback): senão uma cadeira sentava o jogador e um armário
 * abria-se. Pincel: crua/gasta → envernizada → polida (mesmo tom) ou troca de tom. Machado: polida → envernizada → crua; gasta → um
 * estágio a menos (como a cera e a oxidação do cobre).
 */
public final class FinishInteractions {
	private FinishInteractions() {}

	public static void init() {
		UseBlockCallback.EVENT.register(FinishInteractions::use);
	}

	private static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator()) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (stack.getItem() instanceof VarnishBrushItem brush) return brush(level, pos, state, brush.tone, player, hand, stack);
		if (stack.is(ItemTags.AXES)) return axe(level, pos, state, player, hand, stack);
		return InteractionResult.PASS;
	}

	/** O pincel do tom {@code tone} neste bloco. PASS = não leva verniz; FAIL = já está polido deste tom (nada a fazer). */
	public static InteractionResult brush(Level level, BlockPos pos, BlockState state, Tone tone, Player player, InteractionHand hand,
			ItemStack stack) {
		Finish current = FinishState.of(state);
		if (current == null) return InteractionResult.PASS;
		BlockState next = FinishState.with(state, FinishRules.brush(current, tone));
		if (next == null) return InteractionResult.FAIL;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		level.setBlock(pos, next, Block.UPDATE_ALL_IMMEDIATE);
		level.levelEvent(null, LevelEvent.PARTICLES_WAX_ON, pos, 0);
		level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0f, 1.0f);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next));
		if (player != null && stack != null) stack.hurtAndBreak(1, player, hand);
		return InteractionResult.SUCCESS;
	}

	/** O machado neste bloco: raspa um estágio. PASS se não há nada a raspar (o machado faz o que faria). */
	public static InteractionResult axe(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
		Finish current = FinishState.of(state);
		if (current == null) return InteractionResult.PASS;
		BlockState next = FinishState.with(state, FinishRules.axe(current));
		if (next == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		level.setBlock(pos, next, Block.UPDATE_ALL_IMMEDIATE);
		boolean wax = current.varnished();
		level.levelEvent(null, wax ? LevelEvent.PARTICLES_WAX_OFF : LevelEvent.PARTICLES_SCRAPE, pos, 0);
		level.playSound(null, pos, (wax ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE).value(), SoundSource.BLOCKS, 1.0f, 1.0f);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next));
		if (player != null && stack != null) stack.hurtAndBreak(1, player, hand);
		return InteractionResult.SUCCESS;
	}
}
