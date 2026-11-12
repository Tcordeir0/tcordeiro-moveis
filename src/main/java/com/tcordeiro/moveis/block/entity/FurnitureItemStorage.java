package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;

/**
 * O que a Transfer API do Fabric (funis de outros mods, canos do Industrial, redes do Storage) vê nos móveis de guardar. Sem isto vale o
 * recurso da Fabric, que com lado {@code null} ignora as regras de lado — e o cofre e a caixa de correio ficavam vazáveis. Aqui as
 * regras de {@link StorageFurnitureBlockEntity#canTakeItemThroughFace} valem sempre: no cofre, na caixa de correio e na lixeira, sem
 * lado usa-se o de cima (as regras deles não dependem do lado).
 */
public final class FurnitureItemStorage {
	private FurnitureItemStorage() {}

	public static void init() {
		ItemStorage.SIDED.registerForBlockEntities((be, side) -> {
			StorageFurnitureBlockEntity s = (StorageFurnitureBlockEntity) be;
			boolean strict = !StorageFurnitureBlockEntity.takesOut(s.kind());
			return ContainerStorage.of(s, side == null && strict ? Direction.UP : side);
		}, MoveisBlocks.STORAGE_BE, MoveisBlocks.MAILBOX_BE);
	}
}
