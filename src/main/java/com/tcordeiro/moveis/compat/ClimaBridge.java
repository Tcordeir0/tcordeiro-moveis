package com.tcordeiro.moveis.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ligação ao mod Clima (tclima), só por reflexão: os Móveis nunca dependem dele (como o Industrial faz). Usa a API pública
 * {@code com.tcordeiro.clima.ClimaApi}: {@code zone(Level, BlockPos, double, int, int)} (a lareira aquece, o ventilador refresca) e
 * {@code seasonId(Level)} (o ventilador só refresca no verão). Sem o Clima, nada disto faz nada.
 */
public final class ClimaBridge {
	private ClimaBridge() {}

	private static final MethodHandle ZONE, SEASON, LIGHTER_OPENED;

	static {
		MethodHandle zone = null, season = null, opened = null;
		try {
			Class<?> api = Class.forName("com.tcordeiro.clima.ClimaApi");
			MethodHandles.Lookup l = MethodHandles.publicLookup();
			zone = l.findStatic(api, "zone", MethodType.methodType(void.class, Level.class, BlockPos.class, double.class, int.class, int.class));
			season = l.findStatic(api, "seasonId", MethodType.methodType(String.class, Level.class));
		} catch (ReflectiveOperationException | LinkageError e) {
			zone = season = null;
		}
		try { // o isqueiro abre a tampa e faz o som dele
			Class<?> lighter = Class.forName("com.tcordeiro.clima.torch.Lighter");
			opened = MethodHandles.publicLookup().findStatic(lighter, "opened", MethodType.methodType(void.class, ItemStack.class, Level.class, BlockPos.class));
		} catch (ReflectiveOperationException | LinkageError e) {
			opened = null;
		}
		ZONE = zone;
		SEASON = season;
		LIGHTER_OPENED = opened;
	}

	public static boolean present() {
		return ZONE != null;
	}

	/** Zona de temperatura do Clima (servidor). Devolve se foi enviada. */
	public static boolean zone(Level level, BlockPos pos, double target, int radius, int ttl) {
		if (ZONE == null || level.isClientSide()) return false;
		try {
			ZONE.invoke(level, pos, target, radius, ttl);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/** «spring», «summer», «autumn», «winter» (ou "" sem o Clima). */
	public static String seasonId(Level level) {
		if (SEASON == null) return "";
		try {
			return (String) SEASON.invoke(level);
		} catch (Throwable t) {
			return "";
		}
	}

	public static void lighterOpened(ItemStack stack, Level level, BlockPos pos) {
		if (LIGHTER_OPENED == null) return;
		try {
			LIGHTER_OPENED.invoke(stack, level, pos);
		} catch (Throwable ignored) {
		}
	}
}
