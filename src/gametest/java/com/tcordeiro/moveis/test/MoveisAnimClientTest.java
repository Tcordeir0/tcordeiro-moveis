package com.tcordeiro.moveis.test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.BathtubBlock;
import com.tcordeiro.moveis.block.ChairBlock;
import com.tcordeiro.moveis.block.FireplaceBlock;
import com.tcordeiro.moveis.block.PianoBlock;
import com.tcordeiro.moveis.block.SimpleSeatBlock;
import com.tcordeiro.moveis.block.SinkBlock;
import com.tcordeiro.moveis.block.SofaBlock;
import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.block.ToggleFurnitureBlock;
import com.tcordeiro.moveis.block.WallDecorBlock;
import com.tcordeiro.moveis.block.WaterFurniture;
import com.tcordeiro.moveis.block.entity.FireplaceBlockEntity;
import com.tcordeiro.moveis.block.entity.MailboxBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.finish.Finish;
import com.tcordeiro.moveis.finish.FinishBlocks;
import com.tcordeiro.moveis.finish.FinishState;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Móveis no jogo. 0.3.2 (12/11): vídeos das animações, verniz nas partes animadas, sentar, água, lareira, madeira, ícones.
 * 0.3.3 (13/11): as mesmas câmaras das animações e do verniz (para comparar o render antes/depois de tirar MOVING do blockstate), o
 * piano com teclas espaçadas (o teclado não pode sumir entre teclas), o ícone do ventilador na barra e a caixa de correio protegida
 * (um estranho deposita mas não abre nem vê o conteúdo no HUD, um funil não tira nada, não se quebra, explosão não a parte).
 * Sem som, pt_br, sem chat.
 */
public class MoveisAnimClientTest implements FabricClientGameTest {
	static final int Y = -60;
	private final List<String> notes = new ArrayList<>();

	static ServerPlayer player(MinecraftServer s) {
		return s.getPlayerList().getPlayers().get(0);
	}

	static void set(ServerLevel l, BlockPos pos, BlockState st) {
		l.setBlock(pos, st, 3);
	}

	static BlockPos p(int x, int y, int z) {
		return new BlockPos(x, Y + y, z);
	}

	static Block w(Furniture f, String wood) {
		return MoveisBlocks.wooden(f, wood);
	}

	static BlockState south(Block b) {
		BlockState s = b.defaultBlockState();
		for (var prop : s.getProperties())
			if (prop.getName().equals("facing") && prop.getValueClass() == Direction.class) {
				@SuppressWarnings("unchecked")
				var dp = (net.minecraft.world.level.block.state.properties.Property<Direction>) prop;
				return s.setValue(dp, Direction.SOUTH);
			}
		return s;
	}

	/** Um jogador de teste (ServerPlayer real com ligação falsa): o FakePlayer recusa sentar e dormir. */
	static ServerPlayer fake(ServerLevel l, String name, double x, double y, double z) {
		UUID id = UUID.nameUUIDFromBytes(name.getBytes());
		ServerPlayer f = l.getServer().getPlayerList().getPlayer(id);
		if (f == null) {
			GameProfile prof = new GameProfile(id, name);
			var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(prof, false);
			f = new ServerPlayer(l.getServer(), l, cookie.gameProfile(), cookie.clientInformation());
			var conn = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
			new io.netty.channel.embedded.EmbeddedChannel(conn);
			l.getServer().getPlayerList().placeNewPlayer(conn, f, cookie);
			f.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		}
		f.teleportTo(x, y, z);
		return f;
	}

	/** A Ana fica debaixo do chão, perto do móvel (abre-o sem aparecer na foto). */
	static ServerPlayer ana(MinecraftServer s, BlockPos near) {
		return ana(s, near, "Ana");
	}

	static ServerPlayer ana(MinecraftServer s, BlockPos near, String name) {
		return fake(s.overworld(), name, near.getX() + 0.5, Y - 3, near.getZ() + 0.5);
	}

	static final List<String> FAKES = new ArrayList<>(List.of("Ana"));

	static void use(ServerLevel l, ServerPlayer pl, BlockPos pos) {
		l.getBlockState(pos).useWithoutItem(l, pl, new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
	}

	static void useItem(ServerLevel l, ServerPlayer pl, BlockPos pos, ItemStack stack) {
		pl.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		pl.gameMode.useItemOn(pl, l, stack, net.minecraft.world.InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	static void openWith(MinecraftServer s, BlockPos pos) {
		openWith(s, pos, "Ana");
	}

	/** Cada jogador só tem um ecrã aberto: para abrir vários móveis ao mesmo tempo, um jogador de teste por móvel. */
	static void openWith(MinecraftServer s, BlockPos pos, String name) {
		ServerLevel l = s.overworld();
		if (!FAKES.contains(name)) FAKES.add(name);
		if (l.getBlockEntity(pos) instanceof StorageFurnitureBlockEntity be) ana(s, pos, name).openMenu(be);
	}

	static void close(MinecraftServer s) {
		for (String n : FAKES) if (s.getPlayerList().getPlayer(UUID.nameUUIDFromBytes(n.getBytes())) instanceof ServerPlayer a) a.closeContainer();
	}

	private void cam(TestSingleplayerContext sp, ClientGameTestContext ctx, double x, double eye, double z, double tx, double ty, double tz, int wait) {
		// como o «/tp … facing» com o desconto dos olhos (1,62), mas só o jogador (o @a levava também os jogadores de teste)
		double dx = tx - x, dy = ty - eye, dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		sp.getServer().runOnServer(s -> player(s).teleportTo(s.overworld(), x, eye - 1.62, z, java.util.Set.of(), yaw, pitch, false));
		ctx.waitTicks(wait);
	}

	private void shot(ClientGameTestContext ctx, String name) {
		ctx.takeScreenshot("moveis033_" + name);
	}

	/** n fotogramas, 1 por tick (20 fps = velocidade real); {@code each} corre no servidor antes de cada fotograma (pode ser null). */
	private int film(TestSingleplayerContext sp, ClientGameTestContext ctx, String dir, int start, int n, Consumer<Integer> each) {
		int f = start;
		StringBuilder ticks = new StringBuilder();
		for (int i = 0; i < n; i++) {
			if (each != null) {
				int ii = i;
				each.accept(ii);
			}
			// o servidor anda ao ritmo real enquanto se tiram as fotos: guarda-se o tick de cada fotograma (para o vídeo sair à velocidade real)
			long t = sp.getServer().computeOnServer(s -> s.overworld().getGameTime());
			ticks.append(String.format("f%04d %d%n", f, t));
			ctx.takeScreenshot(TestScreenshotOptions.of(String.format("f%04d", f++)).disableCounterPrefix().withDestinationDir(Path.of("video", dir)));
			ctx.waitTicks(1);
		}
		try {
			Path p = Path.of("video", dir, "ticks.txt");
			java.nio.file.Files.createDirectories(p.getParent());
			java.nio.file.Files.writeString(p, ticks, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	/** Abre (Ana), espera, fecha, espera: um vídeo. */
	private void filmOpenClose(TestSingleplayerContext sp, ClientGameTestContext ctx, String dir, BlockPos pos) {
		int f = film(sp, ctx, dir, 0, 10, null);
		sp.getServer().runOnServer(s -> openWith(s, pos));
		f = film(sp, ctx, dir, f, 30, null);
		sp.getServer().runOnServer(MoveisAnimClientTest::close);
		film(sp, ctx, dir, f, 25, null);
	}

	private void toggleFilm(TestSingleplayerContext sp, ClientGameTestContext ctx, String dir, BlockPos pos) {
		int f = film(sp, ctx, dir, 0, 10, null);
		sp.getServer().runOnServer(s -> use(s.overworld(), ana(s, pos), pos));
		f = film(sp, ctx, dir, f, 30, null);
		sp.getServer().runOnServer(s -> use(s.overworld(), ana(s, pos), pos));
		film(sp, ctx, dir, f, 25, null);
	}

	private void hud(ClientGameTestContext ctx, boolean show) {
		ctx.runOnClient(mc -> {
			if (mc.gui.hud.isHidden() == show) mc.gui.hud.toggle();
		});
	}

	private void log(String s) {
		System.out.println("[MoveisAnimClientTest] " + s);
		notes.add(s);
	}

	static void build(MinecraftServer s) {
		ServerLevel l = s.overworld();
		// chão de abeto e parede de tijolo claro atrás (z = -1) da fila das animações (x 0..42)
		for (int x = -3; x <= 44; x++) {
			for (int z = -1; z <= 6; z++) set(l, new BlockPos(x, Y - 1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
			for (int y = 0; y <= 3; y++) set(l, p(x, y, -1), Blocks.SMOOTH_SANDSTONE.defaultBlockState());
		}
		// portas e gavetas (carvalho), virados para sul (para a câmara)
		set(l, p(0, 0, 0), south(w(Furniture.CABINET, "oak")));
		BlockState wr = south(w(Furniture.WARDROBE, "oak"));
		set(l, p(3, 0, 0), wr);
		w(Furniture.WARDROBE, "oak").setPlacedBy(l, p(3, 0, 0), wr, null, new ItemStack(w(Furniture.WARDROBE, "oak")));
		set(l, p(6, 0, 0), south(w(Furniture.DRAWER, "oak")));
		set(l, p(9, 0, 0), south(w(Furniture.NIGHTSTAND, "oak")));
		set(l, p(12, 0, 0), south(w(Furniture.DESK, "oak")));
		set(l, p(15, 0, 0), south(MoveisBlocks.SAFE));
		set(l, p(18, 0, 0), south(MoveisBlocks.TRASH_CAN));
		// cortinas 2×2 (vermelhas) e persianas numa janela de vidro
		for (int x = 20; x <= 27; x++) for (int y = 1; y <= 2; y++) set(l, p(x, y, -1), Blocks.GLASS.defaultBlockState());
		for (int x = 21; x <= 22; x++) for (int y = 1; y <= 2; y++) set(l, p(x, y, 0), south(MoveisBlocks.CURTAINS.get(DyeColor.RED)));
		set(l, p(25, 1, 0), south(w(Furniture.SHUTTERS, "oak")));
		set(l, p(26, 1, 0), south(w(Furniture.SHUTTERS, "oak")));
		// ventilador no teto (pedra por cima), comparador + barril a oeste (sinal fraco); o bloco de redstone a este é o forte
		set(l, p(29, 3, 0), Blocks.STONE.defaultBlockState());
		for (int x = 27; x <= 28; x++) set(l, p(x, 1, 0), Blocks.STONE.defaultBlockState());
		set(l, p(27, 2, 0), Blocks.BARREL.defaultBlockState());
		if (l.getBlockEntity(p(27, 2, 0)) instanceof BarrelBlockEntity b) {
			for (int i = 0; i < 13; i++) b.setItem(i, new ItemStack(Items.DIRT, 64));
			b.setChanged();
		}
		set(l, p(29, 2, 0), MoveisBlocks.CEILING_FAN.defaultBlockState());
		// relógio de parede, caixa de correio, piano
		set(l, p(33, 1, 0), MoveisBlocks.WALL_CLOCK.defaultBlockState().setValue(WallDecorBlock.FACING, Direction.SOUTH));
		set(l, p(36, 0, 0), south(MoveisBlocks.MAILBOX));
		BlockState piano = MoveisBlocks.PIANO.defaultBlockState().setValue(PianoBlock.FACING, Direction.SOUTH);
		set(l, p(40, 0, 0), piano);
		MoveisBlocks.PIANO.setPlacedBy(l, p(40, 0, 0), piano, null, new ItemStack(MoveisBlocks.PIANO));

		// verniz nas partes animadas: armários e gaveteiros crus, mel, ébano polido, mogno, laca branca (z = 12, parede em z = 11)
		Finish[] fin = {Finish.RAW, Finish.HONEY, Finish.EBONY_POLISHED, Finish.MAHOGANY, Finish.WHITE_LACQUER_POLISHED};
		for (int x = -1; x <= 11; x++) {
			for (int z = 11; z <= 16; z++) set(l, new BlockPos(x, Y - 1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
			for (int y = 0; y <= 2; y++) set(l, p(x, y, 11), Blocks.SMOOTH_SANDSTONE.defaultBlockState());
		}
		for (int i = 0; i < fin.length; i++) {
			set(l, p(i * 2, 0, 12), FinishState.with(south(w(Furniture.CABINET, "oak")), fin[i]));
			set(l, p(i * 2, 1, 12), FinishState.with(south(w(Furniture.DRAWER, "oak")), fin[i]));
		}

		// sentar (x 50..70, z 20): cadeira, sofá reto (3), sofá de canto (L), vaso sanitário — virados para sul
		for (int x = 48; x <= 72; x++) for (int z = 16; z <= 28; z++) set(l, new BlockPos(x, Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
		set(l, p(50, 0, 20), w(Furniture.CHAIR, "oak").defaultBlockState().setValue(ChairBlock.FACING, Direction.SOUTH));
		List<BlockPos> sofas = new ArrayList<>();
		for (int x = 54; x <= 56; x++) {
			set(l, p(x, 0, 20), MoveisBlocks.SOFAS.get(DyeColor.BLUE).defaultBlockState().setValue(SofaBlock.FACING, Direction.SOUTH));
			sofas.add(p(x, 0, 20));
		}
		// canto: a fila a olhar para sul (60..62, z 20) e a perna a olhar para oeste (63, z 21..22): o canto é (63, 20)
		for (int x = 60; x <= 63; x++) {
			set(l, p(x, 0, 20), MoveisBlocks.SOFAS.get(DyeColor.GREEN).defaultBlockState().setValue(SofaBlock.FACING, Direction.SOUTH));
			sofas.add(p(x, 0, 20));
		}
		for (int z = 21; z <= 22; z++) {
			set(l, p(63, 0, z), MoveisBlocks.SOFAS.get(DyeColor.GREEN).defaultBlockState().setValue(SofaBlock.FACING, Direction.WEST));
			sofas.add(p(63, 0, z));
		}
		for (BlockPos q : sofas) {
			BlockState st = l.getBlockState(q), now = Block.updateFromNeighbourShapes(st, l, q);
			if (now != st) l.setBlock(q, now, 3);
		}
		set(l, p(68, 0, 20), MoveisBlocks.TOILET.defaultBlockState().setValue(SimpleSeatBlock.FACING, Direction.SOUTH));

		// água e fogo (x 80..90, z 0, parede em z = -1)
		for (int x = 78; x <= 92; x++) {
			for (int z = -1; z <= 6; z++) set(l, new BlockPos(x, Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
			for (int y = 0; y <= 3; y++) set(l, p(x, y, -1), Blocks.QUARTZ_BLOCK.defaultBlockState());
		}
		set(l, p(80, 0, 0), MoveisBlocks.SINK.defaultBlockState().setValue(SinkBlock.FACING, Direction.SOUTH));
		BlockState tub = MoveisBlocks.BATHTUB.defaultBlockState().setValue(BathtubBlock.FACING, Direction.EAST);
		set(l, p(83, 0, 0), tub);
		MoveisBlocks.BATHTUB.setPlacedBy(l, p(83, 0, 0), tub, null, new ItemStack(MoveisBlocks.BATHTUB));
		set(l, p(88, 0, 0), MoveisBlocks.FIREPLACE.defaultBlockState().setValue(FireplaceBlock.FACING, Direction.SOUTH));
		for (BlockPos q : new BlockPos[] {p(80, 0, 0), p(83, 0, 0)}) {
			WaterFurniture.storage(l, q, l.getBlockState(q)); // cria a entidade
			var st = WaterFurniture.storage(l, q, l.getBlockState(q));
			if (st != null) try (Transaction t = Transaction.openOuter()) {
				st.tank.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 8, t);
				t.commit();
			}
		}

		// madeira (x 100.., z 0..): linhas = parquet, espinha, assoalho, lambri, viga, tábuas de carvalho envernizadas; colunas = acabamentos
		buildWood(l);
		var pl = player(s);
		pl.getAbilities().flying = true;
		pl.onUpdateAbilities();
	}

	static final Finish[] WOOD_FINISHES = Finish.values(); // RAW, WORN_1..3, 7 tons × (envernizado, polido)

	static void buildWood(ServerLevel l) {
		int x0 = 100;
		FinishBlocks.Kind[] kinds = FinishBlocks.Kind.values();
		for (int x = x0 - 2; x <= x0 + WOOD_FINISHES.length * 2 + 2; x++)
			for (int z = -2; z <= kinds.length * 3 + 6; z++) set(l, new BlockPos(x, Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
		for (int c = 0; c < WOOD_FINISHES.length; c++) {
			Finish f = WOOD_FINISHES[c];
			for (int r = 0; r < kinds.length; r++) {
				Block b = FinishBlocks.NEW.get("oak").get(kinds[r]);
				for (int dx = 0; dx < 2; dx++)
					for (int dz = 0; dz < 2; dz++) set(l, new BlockPos(x0 + c * 2 + dx, Y - 1, r * 3 + dz), FinishState.with(b.defaultBlockState(), f));
			}
			// tábuas de carvalho do jogo: crua ou envernizada (não há desgaste nas tábuas do jogo: as colunas de desgaste ficam cruas)
			Block planks = f.varnished() ? FinishBlocks.BY_VANILLA.get(Blocks.OAK_PLANKS).get(f) : Blocks.OAK_PLANKS;
			for (int dx = 0; dx < 2; dx++)
				for (int dz = 0; dz < 2; dz++) set(l, new BlockPos(x0 + c * 2 + dx, Y - 1, kinds.length * 3 + dz), planks.defaultBlockState());
		}
		// uma sala: chão de espinha mel, parede de lambri nogueira, vigas de ébano polido no teto
		int rx = x0, rz = kinds.length * 3 + 10;
		for (int x = rx - 1; x <= rx + 9; x++)
			for (int z = rz - 1; z <= rz + 8; z++) set(l, new BlockPos(x, Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
		for (int x = rx; x <= rx + 8; x++)
			for (int z = rz; z <= rz + 7; z++)
				set(l, new BlockPos(x, Y - 1, z), FinishState.with(FinishBlocks.NEW.get("oak").get(FinishBlocks.Kind.HERRINGBONE).defaultBlockState(), Finish.HONEY_POLISHED));
		for (int x = rx; x <= rx + 8; x++)
			for (int y = 0; y <= 3; y++)
				set(l, p(x, y, rz), FinishState.with(FinishBlocks.NEW.get("oak").get(FinishBlocks.Kind.PANELING).defaultBlockState(), Finish.WALNUT));
		for (int z = rz + 1; z <= rz + 7; z++)
			for (int y = 0; y <= 3; y++)
				set(l, p(rx, y, z), FinishState.with(FinishBlocks.NEW.get("oak").get(FinishBlocks.Kind.PARQUET).defaultBlockState(), Finish.WORN_3));
		for (int x = rx; x <= rx + 8; x += 2)
			for (int z = rz + 1; z <= rz + 7; z++) {
				BlockState beam = FinishState.with(FinishBlocks.NEW.get("oak").get(FinishBlocks.Kind.BEAM).defaultBlockState(), Finish.EBONY_POLISHED);
				if (beam.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS))
					beam = beam.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z);
				set(l, p(x, 4, z), beam);
			}
		for (int y = 0; y <= 3; y++) {
			BlockState beam = FinishState.with(FinishBlocks.NEW.get("oak").get(FinishBlocks.Kind.BEAM).defaultBlockState(), Finish.EBONY_POLISHED);
			set(l, p(rx + 8, y, rz + 1), beam);
		}
		for (int x = rx + 3; x <= rx + 6; x++)
			for (int z = rz + 3; z <= rz + 5; z++)
				set(l, new BlockPos(x, Y - 1, z), FinishState.with(FinishBlocks.NEW.get("oak").get(FinishBlocks.Kind.FLOORBOARDS).defaultBlockState(), Finish.MAHOGANY));
	}


	static final BlockPos MAIL = p(36, 0, 0);

	static int mailCount(MinecraftServer s, net.minecraft.world.item.Item it) {
		return s.overworld().getBlockEntity(MAIL) instanceof MailboxBlockEntity be ? be.countItem(it) : -1;
	}

	/** A caixa de correio do «Dono» vista por um estranho (o jogador da câmara), um funil por baixo e, no fim, o dono. */
	private void mailbox(TestSingleplayerContext sp, ClientGameTestContext ctx) {
		sp.getServer().runOnServer(s -> {
			ServerLevel l = s.overworld();
			if (!FAKES.contains("Dono")) FAKES.add("Dono");
			ServerPlayer dono = fake(l, "Dono", MAIL.getX() + 0.5, Y - 3, MAIL.getZ() + 2.5);
			// funil debaixo da caixa (no chão), a despejar num baú a este
			set(l, MAIL.below(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.EAST));
			set(l, MAIL.below().east(), Blocks.CHEST.defaultBlockState());
			if (l.getBlockEntity(MAIL) instanceof MailboxBlockEntity be) {
				be.clearContent();
				be.setOwner(dono);
				be.deposit(new ItemStack(Items.DIAMOND, 5));
				be.deposit(new ItemStack(Items.GOLD_INGOT, 12));
			}
			player(s).setGameMode(net.minecraft.world.level.GameType.CREATIVE);
			player(s).getAbilities().flying = true;
			player(s).onUpdateAbilities();
		});
		hud(ctx, true);
		ctx.waitTicks(5);
		// o estranho olha para a caixa: o HUD do Receitário não mostra o conteúdo
		cam(sp, ctx, MAIL.getX() + 0.5, Y + 1.7, 2.3, MAIL.getX() + 0.5, Y + 0.75, 0.5, 30);
		shot(ctx, "20a_correio_estranho_hud_sem_conteudo");
		// abre: só o espaço de depósito (vazio), não a caixa
		sp.getServer().runOnServer(s -> {
			if (s.overworld().getBlockEntity(MAIL) instanceof MailboxBlockEntity be) player(s).openMenu(be);
		});
		ctx.waitTicks(10);
		String menu = sp.getServer().computeOnServer(s -> player(s).containerMenu.getClass().getSimpleName());
		log("estranho abre a caixa de correio: " + menu);
		if (!menu.equals("DepositMenu")) log("PROBLEMA: o estranho abriu " + menu);
		shot(ctx, "20b_correio_estranho_so_deposita");
		sp.getServer().runOnServer(s -> player(s).containerMenu.getSlot(0).set(new ItemStack(Items.PAPER, 3)));
		ctx.waitTicks(5);
		shot(ctx, "20c_correio_estranho_poe_carta");
		sp.getServer().runOnServer(s -> player(s).closeContainer());
		ctx.waitTicks(5);
		log("depois do depósito: papel=" + sp.getServer().computeOnServer(s -> mailCount(s, Items.PAPER)) + " diamantes="
				+ sp.getServer().computeOnServer(s -> mailCount(s, Items.DIAMOND)));
		sp.getServer().runOnServer(s -> {
			if (s.overworld().getBlockEntity(MAIL) instanceof MailboxBlockEntity be) player(s).openMenu(be);
		});
		ctx.waitTicks(10);
		shot(ctx, "20d_correio_estranho_reabre_nao_ve_o_correio");
		sp.getServer().runOnServer(s -> player(s).closeContainer());
		ctx.waitTicks(5);
		// partir: em sobrevivência, a picareta não avança e o servidor recusa
		String brk = sp.getServer().computeOnServer(s -> {
			ServerPlayer pl = player(s);
			pl.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			float prog = s.overworld().getBlockState(MAIL).getDestroyProgress(pl, s.overworld(), MAIL);
			boolean broke = pl.gameMode.destroyBlock(MAIL);
			pl.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
			pl.getAbilities().flying = true;
			pl.onUpdateAbilities();
			return "progresso=" + prog + " partiu=" + broke + " bloco=" + s.overworld().getBlockState(MAIL).getBlock();
		});
		log("estranho tenta partir: " + brk);
		if (brk.contains("partiu=true")) log("PROBLEMA: o estranho partiu a caixa");
		// o funil: 40 ticks por baixo da caixa, a olhar para o baú onde despejaria
		cam(sp, ctx, MAIL.getX() + 1.5, Y + 1.2, 2.2, MAIL.getX() + 1.0, Y - 0.8, 0.5, 10);
		film(sp, ctx, "correio_funil_nao_tira", 0, 40, null);
		String fun = sp.getServer().computeOnServer(s -> {
			int chest = 0;
			if (s.overworld().getBlockEntity(MAIL.below().east()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c)
				for (int i = 0; i < c.getContainerSize(); i++) chest += c.getItem(i).getCount();
			int hop = 0;
			if (s.overworld().getBlockEntity(MAIL.below()) instanceof net.minecraft.world.level.block.entity.HopperBlockEntity h)
				for (int i = 0; i < h.getContainerSize(); i++) hop += h.getItem(i).getCount();
			return "caixa diamantes=" + mailCount(s, Items.DIAMOND) + " ouro=" + mailCount(s, Items.GOLD_INGOT) + " funil=" + hop + " baú=" + chest;
		});
		log("funil depois de 40 ticks: " + fun);
		if (!fun.endsWith("funil=0 baú=0")) log("PROBLEMA: o funil tirou da caixa de correio");
		shot(ctx, "20e_funil_e_bau_vazios");
		// o dono (passa a ser o jogador da câmara): o HUD mostra o conteúdo e a caixa abre a sério
		sp.getServer().runOnServer(s -> {
			if (s.overworld().getBlockEntity(MAIL) instanceof MailboxBlockEntity be) be.setOwner(player(s));
		});
		cam(sp, ctx, MAIL.getX() + 0.5, Y + 1.7, 2.3, MAIL.getX() + 0.5, Y + 0.75, 0.5, 30);
		shot(ctx, "20f_correio_dono_hud_ve_conteudo");
		sp.getServer().runOnServer(s -> {
			if (s.overworld().getBlockEntity(MAIL) instanceof MailboxBlockEntity be) player(s).openMenu(be);
		});
		ctx.waitTicks(10);
		shot(ctx, "20g_correio_dono_abre_a_caixa");
		sp.getServer().runOnServer(s -> player(s).closeContainer());
		ctx.waitTicks(5);
		// de volta ao Dono (para a explosão no fim)
		sp.getServer().runOnServer(s -> {
			ServerPlayer dono = fake(s.overworld(), "Dono", MAIL.getX() + 0.5, Y - 3, MAIL.getZ() + 2.5);
			if (s.overworld().getBlockEntity(MAIL) instanceof MailboxBlockEntity be) be.setOwner(dono);
		});
		hud(ctx, false);
	}

	/** Explosão de TNT ao lado da caixa de correio: a caixa e o correio ficam. */
	private void mailboxExplosion(TestSingleplayerContext sp, ClientGameTestContext ctx) {
		hud(ctx, false);
		cam(sp, ctx, MAIL.getX() + 0.5, Y + 3.5, 7.5, MAIL.getX() + 0.5, Y + 0.5, 0.5, 15);
		int f = film(sp, ctx, "correio_explosao", 0, 10, null);
		sp.getServer().runOnServer(s -> s.overworld().explode(null, MAIL.getX() + 1.5, Y + 0.5, MAIL.getZ() + 1.5, 4f,
				net.minecraft.world.level.Level.ExplosionInteraction.TNT));
		film(sp, ctx, "correio_explosao", f, 40, null);
		String after = sp.getServer().computeOnServer(s -> "bloco=" + s.overworld().getBlockState(MAIL).getBlock() + " diamantes="
				+ mailCount(s, Items.DIAMOND) + " ouro=" + mailCount(s, Items.GOLD_INGOT) + " papel=" + mailCount(s, Items.PAPER));
		log("depois da explosão: " + after);
		if (!after.contains("Caixa") && !after.contains("mailbox")) log("PROBLEMA: a explosão levou a caixa de correio");
		shot(ctx, "21_correio_depois_da_explosao");
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0));
		ctx.getInput().resizeWindow(1280, 720);
		ctx.runOnClient(mc -> {
			mc.options.languageCode = "pt_br";
			mc.getLanguageManager().setSelected("pt_br");
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		ctx.runOnClient(mc -> mc.reloadResourcePacks());
		ctx.waitTicks(120);
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			for (String c : new String[] {"time set 6000", "gamerule advance_time false", "gamerule spawn_mobs false", "gamerule advance_weather false",
					"weather clear", "gamerule random_tick_speed 0", "gamerule send_command_feedback false", "gamerule players_sleeping_percentage 101",
					"gamemode spectator @a"})
				sp.getServer().runCommand(c);
			sp.getServer().runOnServer(MoveisAnimClientTest::build);
			ctx.waitTicks(40);
			hud(ctx, false);

			// ---------------------------------------------------------------- 1) animações
			cam(sp, ctx, 9.5, Y + 1.6, 6.5, 9.5, Y + 0.8, 0.5, 20);
			sp.getConnection().waitForChunksRender();
			shot(ctx, "01_fila_animados_visao_geral");
			String[][] storage = {{"armario", "0", "0"}, {"guarda_roupa", "3", "0"}, {"gaveteiro", "6", "0"}, {"mesa_de_cabeceira", "9", "0"},
					{"escrivaninha", "12", "0"}, {"cofre_forte", "15", "0"}, {"lixeira", "18", "0"}};
			for (String[] st : storage) {
				int x = Integer.parseInt(st[1]);
				BlockPos pos = p(x, 0, 0);
				boolean tall = st[0].equals("guarda_roupa");
				boolean low = st[0].equals("lixeira") || st[0].equals("mesa_de_cabeceira");
				// de frente e um pouco de lado (as portas abrem para a câmara)
				cam(sp, ctx, x + 1.9, Y + (tall ? 1.6 : low ? 1.25 : 1.4), 2.4, x + 0.5, Y + (tall ? 1.0 : 0.5), 0.5, 10);
				filmOpenClose(sp, ctx, "anim_" + st[0], pos);
				// a foto aberta
				sp.getServer().runOnServer(s -> openWith(s, pos));
				ctx.waitTicks(10);
				shot(ctx, "02_" + st[0] + "_aberto");
				boolean open = sp.getServer().computeOnServer(s -> s.overworld().getBlockState(pos).getValue(AnimatedFurniture.OPEN));
				if (!open) log("PROBLEMA: " + st[0] + " não abriu (OPEN=false)");
				sp.getServer().runOnServer(MoveisAnimClientTest::close);
				ctx.waitTicks(10);
			}
			// cortinas (as 4 juntas) e persianas
			cam(sp, ctx, 21.99, Y + 2.2, 3.6, 21.99, Y + 1.9, 0.5, 10);
			toggleFilm(sp, ctx, "anim_cortinas", p(21, 1, 0));
			sp.getServer().runOnServer(s -> use(s.overworld(), ana(s, p(21, 1, 0)), p(21, 1, 0)));
			ctx.waitTicks(10);
			shot(ctx, "03_cortinas_abertas");
			int openCurtains = sp.getServer().computeOnServer(s -> {
				int n = 0;
				for (int x = 21; x <= 22; x++)
					for (int y = 1; y <= 2; y++) if (s.overworld().getBlockState(p(x, y, 0)).getValue(AnimatedFurniture.OPEN)) n++;
				return n;
			});
			log("cortinas abertas juntas: " + openCurtains + " de 4");
			if (openCurtains != 4) log("PROBLEMA: só " + openCurtains + " cortinas abriram");
			sp.getServer().runOnServer(s -> use(s.overworld(), ana(s, p(21, 1, 0)), p(21, 1, 0)));
			ctx.waitTicks(10);
			cam(sp, ctx, 26.0, Y + 1.9, 3.2, 26.0, Y + 1.5, 0.5, 10);
			toggleFilm(sp, ctx, "anim_persianas", p(25, 1, 0));
			sp.getServer().runOnServer(s -> use(s.overworld(), ana(s, p(25, 1, 0)), p(25, 1, 0)));
			ctx.waitTicks(10);
			shot(ctx, "04_persianas_uma_aberta");
			sp.getServer().runOnServer(s -> use(s.overworld(), ana(s, p(25, 1, 0)), p(25, 1, 0)));

			// ventilador: forte (redstone a este), fraco (só o comparador), parado
			cam(sp, ctx, 30.6, Y + 1.25, 1.6, 29.5, Y + 2.55, 0.5, 10);
			sp.getServer().runOnServer(s -> set(s.overworld(), p(30, 2, 0), Blocks.REDSTONE_BLOCK.defaultBlockState()));
			ctx.waitTicks(30);
			int pw = sp.getServer().computeOnServer(s -> s.overworld().getBlockState(p(29, 2, 0)).getValue(com.tcordeiro.moveis.block.CeilingFanBlock.POWER));
			log("ventilador forte: power=" + pw);
			int f = film(sp, ctx, "anim_ventilador", 0, 60, null);
			sp.getServer().runOnServer(s -> {
				set(s.overworld(), p(30, 2, 0), Blocks.AIR.defaultBlockState());
				set(s.overworld(), p(28, 2, 0), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
				if (s.overworld().getBlockEntity(p(27, 2, 0)) instanceof BarrelBlockEntity b) b.setChanged(); // o comparador lê o barril
			});
			ctx.waitTicks(10);
			pw = sp.getServer().computeOnServer(s -> s.overworld().getBlockState(p(29, 2, 0)).getValue(com.tcordeiro.moveis.block.CeilingFanBlock.POWER));
			log("ventilador fraco: power=" + pw);
			if (pw <= 0 || pw >= 15) log("PROBLEMA: sinal fraco do ventilador = " + pw);
			f = film(sp, ctx, "anim_ventilador", f, 80, null);
			shot(ctx, "05_ventilador_sinal_fraco");
			sp.getServer().runOnServer(s -> set(s.overworld(), p(28, 2, 0), Blocks.AIR.defaultBlockState()));
			ctx.waitTicks(2);
			film(sp, ctx, "anim_ventilador", f, 80, null);
			shot(ctx, "05b_ventilador_parado");

			// relógio: a hora avança 20 ticks por fotograma (2 h em 5 s; à velocidade real os ponteiros mal se mexem)
			cam(sp, ctx, 33.5, Y + 1.5, 2.0, 33.5, Y + 1.5, 0.5, 10);
			shot(ctx, "06a_relogio_meio_dia");
			film(sp, ctx, "anim_relogio_acelerado", 0, 100, i -> sp.getServer().runCommand("time add 20"));
			shot(ctx, "06b_relogio_14h");
			sp.getServer().runCommand("time set 6000");

			// caixa de correio: chega carta, a bandeirinha sobe; tira-se, desce
			cam(sp, ctx, 37.6, Y + 1.5, 2.0, 36.5, Y + 0.8, 0.5, 10);
			f = film(sp, ctx, "anim_caixa_correio", 0, 10, null);
			sp.getServer().runOnServer(s -> {
				if (s.overworld().getBlockEntity(p(36, 0, 0)) instanceof StorageFurnitureBlockEntity be) {
					be.setItem(0, new ItemStack(Items.PAPER));
					be.setChanged();
				}
			});
			f = film(sp, ctx, "anim_caixa_correio", f, 30, null);
			shot(ctx, "07_caixa_correio_bandeira_em_cima");
			sp.getServer().runOnServer(s -> {
				if (s.overworld().getBlockEntity(p(36, 0, 0)) instanceof StorageFurnitureBlockEntity be) {
					be.clearContent();
					be.setChanged();
				}
			});
			film(sp, ctx, "anim_caixa_correio", f, 25, null);

			// piano: uma escala tocada (uma tecla a cada 3 ticks), visto de cima
			cam(sp, ctx, 41.0, Y + 2.3, 1.9, 41.0, Y + 0.85, 0.6, 10);
			shot(ctx, "08a_piano_teclado");
			int[] keys = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 22, 1, 3, 6, 8, 10};
			// uma tecla a cada 4 ticks do servidor (pelo relógio do mundo, não pelos fotogramas)
			long t0 = sp.getServer().computeOnServer(s -> s.overworld().getGameTime());
			int[] next = {0};
			film(sp, ctx, "anim_piano", 0, 90, i -> sp.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				while (next[0] < keys.length && l.getGameTime() - t0 >= next[0] * 4L) {
					PianoBlock.play(l, p(40, 0, 0), l.getBlockState(p(40, 0, 0)), keys[next[0]], ana(s, p(40, 0, 0)));
					next[0]++;
				}
			}));
			// 0.3.3: teclas espaçadas (10 ticks > PLAY_TICKS): o teclado não pode sumir entre uma e outra
			long t1 = sp.getServer().computeOnServer(s -> s.overworld().getGameTime());
			int[] next2 = {0};
			int[] spaced = {0, 4, 7, 12, 7, 4, 0, 12};
			film(sp, ctx, "anim_piano_teclas_espacadas", 0, 100, i -> sp.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				while (next2[0] < spaced.length && l.getGameTime() - t1 >= 5 + next2[0] * 10L) {
					PianoBlock.play(l, p(40, 0, 0), l.getBlockState(p(40, 0, 0)), spaced[next2[0]], ana(s, p(40, 0, 0)));
					next2[0]++;
				}
			}));
			sp.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				for (int k : new int[] {4, 7, 11}) PianoBlock.play(l, p(40, 0, 0), l.getBlockState(p(40, 0, 0)), k, ana(s, p(40, 0, 0)));
			});
			ctx.waitTicks(1);
			shot(ctx, "08b_piano_tres_teclas_afundadas");

			// ---------------------------------------------------------------- 2) verniz nas partes animadas
			cam(sp, ctx, 4.5, Y + 1.8, 17.0, 4.5, Y + 1.0, 12.5, 15);
			sp.getConnection().waitForChunksRender();
			shot(ctx, "09a_verniz_fechados_cru_mel_ebano_mogno_laca");
			sp.getServer().runOnServer(s -> {
				for (int i = 0; i < 5; i++) {
					openWith(s, p(i * 2, 0, 12), "Ana" + i + "a");
					openWith(s, p(i * 2, 1, 12), "Ana" + i + "b");
				}
			});
			ctx.waitTicks(15);
			shot(ctx, "09b_verniz_abertos");
			cam(sp, ctx, 3.3, Y + 1.5, 14.4, 2.5, Y + 0.9, 12.5, 10);
			shot(ctx, "09c_verniz_mel_de_perto");
			cam(sp, ctx, 5.3, Y + 1.5, 14.4, 4.5, Y + 0.9, 12.5, 10);
			shot(ctx, "09d_verniz_ebano_polido_de_perto");
			cam(sp, ctx, 9.3, Y + 1.5, 14.4, 8.5, Y + 0.9, 12.5, 10);
			shot(ctx, "09e_verniz_laca_branca_polida_de_perto");
			sp.getServer().runOnServer(MoveisAnimClientTest::close);
			cam(sp, ctx, 4.8, Y + 1.5, 15.0, 4.5, Y + 0.9, 12.5, 10);
			// vídeo: só o mel e o ébano a abrir e fechar
			int g = film(sp, ctx, "verniz_abrir_fechar", 0, 10, null);
			sp.getServer().runOnServer(s -> {
				for (int i = 1; i <= 2; i++) {
					openWith(s, p(i * 2, 0, 12), "Ana" + i + "a");
					openWith(s, p(i * 2, 1, 12), "Ana" + i + "b");
				}
			});
			g = film(sp, ctx, "verniz_abrir_fechar", g, 30, null);
			sp.getServer().runOnServer(MoveisAnimClientTest::close);
			film(sp, ctx, "verniz_abrir_fechar", g, 25, null);

			// ---------------------------------------------------------------- 3) caixa de correio protegida (0.3.3)
			mailbox(sp, ctx);

			// ---------------------------------------------------------------- 6) ícones na barra (lustre, arandela e, 0.3.3, ventilador)
			hud(ctx, true);
			sp.getServer().runOnServer(s -> {
				var inv = player(s).getInventory();
				Block[] items = {MoveisBlocks.CHANDELIER, MoveisBlocks.WALL_LAMP, MoveisBlocks.TABLE_LAMP, MoveisBlocks.FLOOR_LAMP, MoveisBlocks.CEILING_FAN,
						w(Furniture.CHAIR, "oak"), w(Furniture.CABINET, "oak"), MoveisBlocks.SOFAS.get(DyeColor.RED), MoveisBlocks.PIANO};
				for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(items[i]));
				inv.setSelectedSlot(4); // o ventilador (o nome aparece por cima da barra)
			});
			cam(sp, ctx, 9.5, Y + 1.6, 6.5, 9.5, Y + 0.8, 0.5, 15);
			for (int[] size : new int[][] {{1280, 720}, {854, 480}}) {
				ctx.getInput().resizeWindow(size[0], size[1]);
				ctx.waitTicks(10);
				shot(ctx, "16_barra_lustre_arandela_ventilador_" + size[0] + "x" + size[1]);
			}
			ctx.getInput().resizeWindow(1280, 720);
			ctx.runOnClient(mc -> {
				mc.options.guiScale().set(4);
				mc.resizeGui();
			});
			ctx.waitTicks(5);
			shot(ctx, "16c_barra_escala4");
			ctx.runOnClient(mc -> {
				mc.options.guiScale().set(0);
				mc.resizeGui();
			});
			ctx.waitTicks(5);
			// a explosão por último (estraga a fila): a caixa de correio fica, com o correio
			mailboxExplosion(sp, ctx);
			sp.getServer().runOnServer(s -> {
				for (String n : FAKES)
					if (s.getPlayerList().getPlayer(UUID.nameUUIDFromBytes(n.getBytes())) instanceof ServerPlayer a) {
						a.closeContainer();
						s.overworld().removePlayerImmediately(a, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
					}
			});
			ctx.waitTicks(5);
		}
		System.out.println("[MoveisAnimClientTest] RESUMO: " + notes);
	}
}
