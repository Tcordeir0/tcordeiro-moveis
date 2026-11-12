package com.tcordeiro.moveis.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.tcordeiro.moveis.TMoveis;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Que caixas de correio têm correio, por dono (para o aviso quando o dono entra no mundo). Gravado no mundo. */
public final class MailData extends SavedData {
	private final Map<UUID, List<GlobalPos>> boxes = new HashMap<>();

	public static final Codec<MailData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC.listOf()).xmap(m -> {
		MailData d = new MailData();
		m.forEach((k, v) -> d.boxes.put(k, new ArrayList<>(v)));
		return d;
	}, d -> d.boxes);
	public static final SavedDataType<MailData> TYPE = new SavedDataType<>(TMoveis.id("mail"), MailData::new, CODEC, null);

	public static MailData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public void add(UUID owner, GlobalPos pos) {
		List<GlobalPos> l = boxes.computeIfAbsent(owner, k -> new ArrayList<>());
		if (!l.contains(pos)) {
			l.add(pos);
			setDirty();
		}
	}

	public void remove(UUID owner, GlobalPos pos) {
		List<GlobalPos> l = boxes.get(owner);
		if (l != null && l.remove(pos)) {
			if (l.isEmpty()) boxes.remove(owner);
			setDirty();
		}
	}

	public List<GlobalPos> boxes(UUID owner) {
		return boxes.getOrDefault(owner, List.of());
	}

	/** Ao entrar no mundo: «tem correio à espera». */
	public static void greet(ServerPlayer player) {
		List<GlobalPos> l = get(player.level().getServer()).boxes(player.getUUID());
		if (!l.isEmpty())
			player.sendSystemMessage(Component.translatable("message.tmoveis.mail_waiting", l.size(), l.get(0).pos().toShortString()));
	}
}
