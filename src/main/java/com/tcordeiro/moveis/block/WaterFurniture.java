package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.block.entity.WaterBlockEntity;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;

/**
 * Pia e banheira: guardam água (só água; a lava é recusada) num {@code SingleFluidStorage} exposto à Transfer API da Fabric
 * ({@code FluidStorage.SIDED}, registado em MoveisBlocks): os canos de fluidos do Industrial (ou de qualquer mod) enchem e esvaziam.
 * À mão: balde de água enche, balde vazio ou frasco de vidro tira. WATER = há água (a camada aparece).
 */
public interface WaterFurniture {
	BooleanProperty WATER = BooleanProperty.create("water");

	/** Capacidade (gotas da Fabric; 1 balde = 81 000). */
	long capacity();

	/** Onde está a entidade com a água (a banheira guarda na cabeceira). */
	BlockPos storagePos(BlockPos pos, BlockState state);

	/** Muda a camada de água (nas duas metades da banheira). */
	void setWater(Level level, BlockPos pos, BlockState state, boolean water);

	static WaterBlockEntity storage(BlockGetter level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof WaterFurniture w)) return null;
		return level.getBlockEntity(w.storagePos(pos, state)) instanceof WaterBlockEntity be ? be : null;
	}

	/** Clique com balde ou frasco; outras coisas seguem para o comportamento normal. */
	static InteractionResult use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
		WaterBlockEntity be = storage(level, pos, state);
		if (be == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (stack.is(Items.GLASS_BOTTLE)) { // frasco: tira 1/3 de balde em água (frasco de água)
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			try (Transaction t = Transaction.openOuter()) {
				if (be.tank.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BOTTLE, t) != FluidConstants.BOTTLE) return InteractionResult.FAIL;
				t.commit();
			}
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (FluidStorage.ITEM.find(stack, ContainerItemContext.forPlayerInteraction(player, hand)) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		// balde de água → enche; balde vazio → tira 1 balde; balde de lava (recusada) → não despeja no mundo
		return FluidStorageUtil.interactWithFluidStorage(be.tank, player, hand) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}

	static Component status(WaterBlockEntity be) {
		return Component.translatable("message.tmoveis.water", String.format("%.1f", be.tank.amount / (double) FluidConstants.BUCKET),
				be.tank.getCapacity() / FluidConstants.BUCKET);
	}

	static boolean isWaterBottle(ItemStack s) {
		PotionContents c = s.get(DataComponents.POTION_CONTENTS);
		return s.is(Items.POTION) && c != null && c.is(Potions.WATER);
	}
}
