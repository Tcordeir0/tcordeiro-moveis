package com.tcordeiro.moveis;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

/**
 * «Pode mexer aqui?» (0.3.3): as mesmas regras que o jogo usa antes de um item mudar um bloco (o machado a descascar, o favo de mel a
 * encerar: {@code ItemStack.useOn}) e o que o Receitário consulta antes de mostrar um bloco ({@code Level.mayInteract}). Os mods de
 * terrenos e proteção ligam-se a estes métodos.
 * <ul>
 * <li>{@link Level#mayInteract}: proteção do spawn (servidor) e a borda do mundo;</li>
 * <li>modo aventura / sem permissão de construir: só nos blocos que o item permite ({@code can_place_on}), como no jogo.</li>
 * </ul>
 */
public final class Protection {
	private Protection() {}

	/** Este jogador pode mudar o bloco em {@code pos} com {@code stack}? Sem jogador (dispensador, testes) sim. */
	public static boolean mayEdit(Player player, Level level, BlockPos pos, ItemStack stack) {
		if (player == null) return true;
		if (player.isSpectator()) return false;
		if (!level.mayInteract(player, pos)) return false;
		return player.getAbilities().mayBuild || (stack != null && stack.canPlaceOnBlockInAdventureMode(new BlockInWorld(level, pos, false)));
	}
}
