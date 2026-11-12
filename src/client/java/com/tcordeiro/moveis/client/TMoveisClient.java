package com.tcordeiro.moveis.client;

import java.util.List;
import java.util.Set;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class TMoveisClient implements ClientModInitializer {
	/** A cor da água (a textura water_still é cinzenta): o azul da água normal. */
	public static final int WATER = 0xFF3F76E4;

	@Override
	public void onInitializeClient() {
		// o assento é invisível: só serve para o jogador ficar sentado
		EntityRendererRegistry.register(MoveisBlocks.SEAT, NoopRenderer::new);
		com.tcordeiro.moveis.finish.client.FinishClient.init(); // 0.3.0: Serraria (ecrã e serra a girar)
		// 0.3.3: o modelo do chunk dos móveis animados escolhe «parado»/«a mexer» pela entidade (sem MOVING no blockstate)
		AnimModels.init();
		// partes móveis (portas, gavetas, cortinas, pás, ponteiros, teclas…)
		BlockEntityRenderers.register(MoveisBlocks.STORAGE_BE, FurnitureRenderer::new);
		BlockEntityRenderers.register(MoveisBlocks.ANIM_BE, FurnitureRenderer::new);
		BlockEntityRenderers.register(MoveisBlocks.MAILBOX_BE, FurnitureRenderer::new);
		BlockEntityRenderers.register(MoveisBlocks.FAN_BE, FurnitureRenderer::new);
		BlockEntityRenderers.register(MoveisBlocks.CLOCK_BE, FurnitureRenderer::new);
		BlockEntityRenderers.register(MoveisBlocks.PIANO_BE, FurnitureRenderer::new);
		// o que está pousado (mesas, adega)
		BlockEntityRenderers.register(MoveisBlocks.DISPLAY_BE, ShelfRenderer::new);
		BlockEntityRenderers.register(MoveisBlocks.WINE_BE, ShelfRenderer::new);
		// a água da pia e da banheira (tintindex 0 nos templates)
		BlockColorRegistry.register(List.of(new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return WATER;
			}

			@Override
			public Set<Property<?>> relevantProperties() {
				return Set.of();
			}
		}), MoveisBlocks.SINK, MoveisBlocks.BATHTUB);
		// avisos (⚠ só onde há perigo real) e regras de dono
		ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
			String k = null;
			if (is(stack, MoveisBlocks.TRASH_CAN)) k = "trash_can";
			else if (is(stack, MoveisBlocks.FIREPLACE)) k = "fireplace";
			else if (is(stack, MoveisBlocks.SAFE)) k = "safe";
			else if (is(stack, MoveisBlocks.MAILBOX)) k = "mailbox";
			if (k != null) lines.add(Component.translatable("tooltip.tmoveis." + k).withStyle(k.equals("safe") || k.equals("mailbox")
					? ChatFormatting.GRAY : ChatFormatting.GOLD));
		});
	}

	private static boolean is(net.minecraft.world.item.ItemStack stack, Block b) {
		return stack.is(b.asItem());
	}
}
