package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cofre-forte: 27 espaços que só o dono (quem o pôs) abre. Os outros jogadores não o abrem nem o partem (nem a picareta avança:
 * {@link StorageFurnitureBlock#getDestroyProgress}; e o evento {@code PlayerBlockBreakEvents.BEFORE} em TMoveis recusa o partir); resiste
 * a explosões (resistência da obsidiana) e os funis e canos não lhe tiram nem põem nada. O dono parte-o e o conteúdo cai. As regras de
 * dono são as mesmas da caixa de correio ({@link StorageFurnitureBlock.Kind#ownerOnly}).
 */
public class SafeBlock extends StorageFurnitureBlock {
	public SafeBlock(Properties props) {
		super(Kind.SAFE, props);
	}

	public static boolean isOwner(BlockGetter level, BlockPos pos, Player player) {
		return StorageFurnitureBlock.isOwner(level, pos, player);
	}

	public static String ownerName(BlockGetter level, BlockPos pos) {
		return StorageFurnitureBlock.ownerName(level, pos);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		super.setPlacedBy(level, pos, state, by, stack);
		if (!level.isClientSide() && by instanceof Player p && level.getBlockEntity(pos) instanceof StorageFurnitureBlockEntity be) be.setOwner(p);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!isOwner(level, pos, player)) {
			player.sendOverlayMessage(Component.translatable("message.tmoveis.not_owner", ownerName(level, pos)));
			return InteractionResult.SUCCESS;
		}
		return super.useWithoutItem(state, level, pos, player, hit);
	}
}
