package com.tcordeiro.moveis.client;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class TMoveisClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// o assento é invisível: só serve para o jogador ficar sentado
		EntityRendererRegistry.register(MoveisBlocks.SEAT, NoopRenderer::new);
	}
}
