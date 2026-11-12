package com.tcordeiro.moveis;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/** Móveis do Tcordeiro: cadeiras, bancos, mesas, armários, gaveteiros, sofás, camas de casal e lâmpadas. */
public class TMoveis implements ModInitializer {
	public static final String MOD_ID = "tmoveis";
	/** O Industrial está instalado? (as lâmpadas passam a aceitar energia dos cabos; sem ele, só redstone) */
	public static final boolean INDUSTRIAL = FabricLoader.getInstance().isModLoaded("tindustrial");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		MoveisBlocks.init();
	}
}
