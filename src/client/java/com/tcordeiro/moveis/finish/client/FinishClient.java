package com.tcordeiro.moveis.finish.client;

import com.tcordeiro.moveis.finish.FinishBlocks;
import com.tcordeiro.moveis.finish.sawmill.Sawing;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Cliente da madeira e verniz: o ecrã da Serraria e a serra a girar. */
public final class FinishClient {
	private FinishClient() {}

	public static void init() {
		MenuScreens.register(Sawing.MENU, SawmillScreen::new);
		BlockEntityRenderers.register(FinishBlocks.SAWMILL_BE, SawmillRenderer::new);
	}
}
