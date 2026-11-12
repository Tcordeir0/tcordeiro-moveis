package com.tcordeiro.moveis.finish;

import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.Level;

/** O mod Clima (opcional): a estação atual, por reflexão (com.tcordeiro.clima.ClimaApi.seasonId). Sem Clima: null. */
final class ClimaCompat {
	private ClimaCompat() {}

	private static final Method SEASON = find();

	private static Method find() {
		if (!FabricLoader.getInstance().isModLoaded("tclima")) return null;
		try {
			return Class.forName("com.tcordeiro.clima.ClimaApi").getMethod("seasonId", Level.class);
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	static String seasonId(Level level) {
		if (SEASON == null) return null;
		try {
			return (String) SEASON.invoke(null, level);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
