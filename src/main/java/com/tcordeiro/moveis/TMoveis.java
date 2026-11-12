package com.tcordeiro.moveis;

import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/** Móveis do Tcordeiro: cadeiras, mesas, armários, sofás, camas, lâmpadas e a casa toda (cozinha, quarto, banheiro, sala, jardim). */
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
		com.tcordeiro.moveis.finish.FinishBlocks.init(); // 0.3.0: madeira e verniz, Serraria
		// cofre-forte e caixa de correio (0.3.3): só o dono os parte (os outros nem com a picareta)
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
			if (!StorageFurnitureBlock.ownerOnly(state) || StorageFurnitureBlock.isOwner(level, pos, player)) return true;
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.tmoveis.not_owner",
					StorageFurnitureBlock.ownerName(level, pos)));
			return false;
		});
		// funis, canos do Industrial e outros mods (Transfer API): as regras de lado valem mesmo sem lado (0.3.3)
		com.tcordeiro.moveis.block.entity.FurnitureItemStorage.init();
		// caixa de correio: o dono sabe ao entrar no mundo que tem correio à espera
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				com.tcordeiro.moveis.mail.MailData.greet(handler.getPlayer()));
	}
}
