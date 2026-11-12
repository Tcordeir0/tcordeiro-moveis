package com.tcordeiro.moveis.block;

import net.minecraft.util.StringRepresentable;

/**
 * Forma do sofá, vista de quem está sentado. Reto: SINGLE (dois braços), LEFT (ponta esquerda: braço à esquerda), RIGHT (ponta
 * direita), MIDDLE (sem braços). Cantos (como as escadas): INNER_* (canto de dentro: encosto em dois lados), OUTER_* (canto de fora:
 * só um pilar de encosto no canto).
 */
public enum SofaShape implements StringRepresentable {
	SINGLE, LEFT, RIGHT, MIDDLE, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	public boolean corner() {
		return ordinal() >= INNER_LEFT.ordinal();
	}
}
