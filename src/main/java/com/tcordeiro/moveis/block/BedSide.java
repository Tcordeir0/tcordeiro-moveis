package com.tcordeiro.moveis.block;

import net.minecraft.util.StringRepresentable;

/** Coluna da cama de casal: LEFT é onde se pôs o item; RIGHT fica à direita de quem olha para a cabeceira (FACING.getClockWise()). */
public enum BedSide implements StringRepresentable {
	LEFT, RIGHT;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	public BedSide other() {
		return this == LEFT ? RIGHT : LEFT;
	}
}
