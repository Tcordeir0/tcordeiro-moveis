package com.tcordeiro.moveis.test;

import java.util.List;

import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.block.TableBlock;
import com.tcordeiro.moveis.finish.Finish;
import com.tcordeiro.moveis.finish.FinishBlocks;
import com.tcordeiro.moveis.finish.FinishBlocks.Kind;
import com.tcordeiro.moveis.finish.FinishFurniture;
import com.tcordeiro.moveis.finish.FinishInteractions;
import com.tcordeiro.moveis.finish.FinishState;
import com.tcordeiro.moveis.finish.Tone;
import com.tcordeiro.moveis.finish.Weathering;
import com.tcordeiro.moveis.registry.MoveisBlocks;
import com.tcordeiro.moveis.registry.MoveisBlocks.Furniture;
import com.tcordeiro.moveis.wood.WoodTypes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Verniz: pincel, polimento, machado, desgaste (só a madeira crua dos Móveis, à chuva), móveis com «finish», loot e dados. */
public class FinishGameTests {
	static ItemStack brush(Tone t) {
		return new ItemStack(FinishBlocks.BRUSHES.get(t));
	}

	/** Pincel neste bloco (posição relativa), como um jogador. */
	static InteractionResult brushAt(GameTestHelper h, Player p, BlockPos rel, Tone t, ItemStack stack) {
		BlockPos abs = h.absolutePos(rel);
		p.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return FinishInteractions.brush(h.getLevel(), abs, h.getLevel().getBlockState(abs), t, p, InteractionHand.MAIN_HAND, stack);
	}

	static InteractionResult axeAt(GameTestHelper h, Player p, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		ItemStack axe = new ItemStack(Items.IRON_AXE);
		p.setItemInHand(InteractionHand.MAIN_HAND, axe);
		return FinishInteractions.axe(h.getLevel(), abs, h.getLevel().getBlockState(abs), p, InteractionHand.MAIN_HAND, axe);
	}

	/** Todas as propriedades de {@code a} (menos finish) iguais em {@code b}. */
	static void assertSameState(GameTestHelper h, BlockState a, BlockState b, String what) {
		for (Property<?> p : a.getProperties()) {
			if (p == FinishState.FINISH) continue;
			h.assertTrue(b.hasProperty(p) && b.getValue(p).equals(a.getValue(p)), what + ": manteve " + p.getName() + " (" + a + " → " + b + ")");
		}
	}

	@GameTest
	public void brushVarnishesPlanksStairsSlabsKeepingState(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		BlockPos planks = new BlockPos(1, 1, 1), stairs = new BlockPos(3, 1, 1), slab = new BlockPos(5, 1, 1), parquet = new BlockPos(7, 1, 1);
		h.setBlock(planks, Blocks.SPRUCE_PLANKS);
		h.setBlock(stairs, Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP)
				.setValue(StairBlock.WATERLOGGED, true));
		h.setBlock(slab, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
		Block parquetStairs = FinishBlocks.NEW_STAIRS.get("birch").get(Kind.PARQUET);
		h.setBlock(parquet, parquetStairs.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP));
		for (BlockPos rel : List.of(planks, stairs, slab, parquet)) {
			BlockState before = h.getBlockState(rel);
			h.assertValueEqual(brushAt(h, p, rel, Tone.MAHOGANY, brush(Tone.MAHOGANY)), InteractionResult.SUCCESS, "pincel em " + before);
			BlockState after = h.getBlockState(rel);
			h.assertValueEqual(FinishState.of(after), Finish.MAHOGANY, "ficou mogno: " + after);
			assertSameState(h, before, after, before.getBlock().toString());
			Block vanilla = FinishState.vanillaOf(after.getBlock());
			if (vanilla != null) h.assertValueEqual(vanilla, before.getBlock(), "a madeira continua a mesma");
			else h.assertValueEqual(after.getBlock(), before.getBlock(), "o bloco novo continua o mesmo (só muda a propriedade)");
		}
		h.assertValueEqual(h.getBlockState(planks).getBlock(), FinishBlocks.BY_VANILLA.get(Blocks.SPRUCE_PLANKS).get(Finish.MAHOGANY), "tábuas de abeto mogno");
		h.succeed();
	}

	@GameTest
	public void secondBrushPolishes(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 1);
		h.setBlock(a, Blocks.ACACIA_PLANKS);
		h.setBlock(b, FinishBlocks.NEW.get("oak").get(Kind.HERRINGBONE));
		for (BlockPos rel : List.of(a, b)) {
			ItemStack br = brush(Tone.HONEY);
			brushAt(h, p, rel, Tone.HONEY, br);
			h.assertValueEqual(FinishState.of(h.getBlockState(rel)), Finish.HONEY, "1.ª demão");
			brushAt(h, p, rel, Tone.HONEY, br);
			h.assertValueEqual(FinishState.of(h.getBlockState(rel)), Finish.HONEY_POLISHED, "2.ª demão = polido");
			h.assertValueEqual(brushAt(h, p, rel, Tone.HONEY, br), InteractionResult.FAIL, "polido do mesmo tom: nada a fazer");
			h.assertValueEqual(br.getDamageValue(), 2, "só gastou as 2 demãos que mudaram o bloco");
			brushAt(h, p, rel, Tone.EBONY, brush(Tone.EBONY));
			h.assertValueEqual(FinishState.of(h.getBlockState(rel)), Finish.EBONY, "outro tom troca a cor, sem polimento");
		}
		h.succeed();
	}

	@GameTest
	public void axeRemovesOneStage(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 1), c = new BlockPos(5, 1, 1);
		Block polished = FinishBlocks.BY_VANILLA.get(Blocks.DARK_OAK_STAIRS).get(Finish.WALNUT_POLISHED);
		h.setBlock(a, polished.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
		axeAt(h, p, a);
		h.assertValueEqual(h.getBlockState(a).getBlock(), FinishBlocks.BY_VANILLA.get(Blocks.DARK_OAK_STAIRS).get(Finish.WALNUT), "polida → envernizada");
		axeAt(h, p, a);
		h.assertValueEqual(h.getBlockState(a).getBlock(), Blocks.DARK_OAK_STAIRS, "envernizada → crua (a do jogo)");
		h.assertValueEqual(h.getBlockState(a).getValue(StairBlock.FACING), Direction.SOUTH, "manteve o lado");
		h.assertValueEqual(axeAt(h, p, a), InteractionResult.PASS, "crua: nada a raspar");
		Block beam = FinishBlocks.NEW.get("jungle").get(Kind.BEAM);
		h.setBlock(b, beam.defaultBlockState().setValue(FinishState.FINISH, Finish.WORN_3));
		axeAt(h, p, b);
		h.assertValueEqual(h.getBlockState(b).getValue(FinishState.FINISH), Finish.WORN_2, "gasta 3 → gasta 2");
		h.setBlock(c, FinishBlocks.NEW.get("jungle").get(Kind.PANELING).defaultBlockState().setValue(FinishState.FINISH, Finish.NATURAL_POLISHED));
		axeAt(h, p, c);
		axeAt(h, p, c);
		h.assertValueEqual(h.getBlockState(c).getValue(FinishState.FINISH), Finish.RAW, "polida → envernizada → crua");
		h.succeed();
	}

	/** Um random tick forçado (sorteio 0) com chuva a cair no bloco. */
	static boolean ageInRain(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return Weathering.tryAge(h.getLevel(), abs, h.getLevel().getBlockState(abs), 0.0, true);
	}

	/** Um random tick forçado com a exposição real (chuva do mundo, céu por cima, bioma). */
	static boolean ageReal(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return Weathering.tryAge(h.getLevel(), abs, h.getLevel().getBlockState(abs), 0.0);
	}

	@GameTest
	public void weatheringOnlyModRawWoodOutdoorsInRain(GameTestHelper h) {
		BlockPos open = new BlockPos(1, 1, 1), covered = new BlockPos(3, 1, 1), chair = new BlockPos(5, 1, 1);
		Block floor = FinishBlocks.NEW.get("oak").get(Kind.FLOORBOARDS);
		h.setBlock(open, floor);
		h.setBlock(covered, floor);
		h.setBlock(covered.above(), Blocks.STONE);
		h.setBlock(chair, MoveisBlocks.wooden(Furniture.CHAIR, "spruce"));
		h.assertTrue(h.getBlockState(open).isRandomlyTicking(), "a madeira crua dos Móveis recebe random ticks");
		ServerLevel level = h.getLevel();
		float old = level.getRainLevel(1.0f);
		level.setRainLevel(0f);
		h.assertFalse(Weathering.exposed(level, h.absolutePos(open)), "sem chuva não está exposta");
		h.assertFalse(ageReal(h, open), "sem chuva não se gasta");
		level.setRainLevel(1f);
		h.assertFalse(Weathering.exposed(level, h.absolutePos(covered)), "tapada não apanha chuva");
		h.assertFalse(ageReal(h, covered), "tapada não se gasta");
		level.setRainLevel(old);
		h.assertTrue(ageInRain(h, open), "à chuva gasta");
		h.assertTrue(ageInRain(h, chair), "o móvel cru também se gasta");
		h.assertTrue(ageInRain(h, open) && ageInRain(h, open), "gasta 2 e 3");
		h.assertFalse(ageInRain(h, open), "3 é o último estágio");
		BlockPos abs = h.absolutePos(open);
		h.assertFalse(Weathering.tryAge(level, abs, level.getBlockState(abs), 0.999, true), "sorteio alto: não");
		h.assertValueEqual(h.getBlockState(open).getValue(FinishState.FINISH), Finish.WORN_3, "gasta 3");
		h.assertFalse(h.getBlockState(open).isRandomlyTicking(), "gasta 3 já não recebe ticks");
		h.assertValueEqual(h.getBlockState(covered).getValue(FinishState.FINISH), Finish.RAW, "a tapada ficou crua");
		h.assertValueEqual(h.getBlockState(chair).getValue(FinishState.FINISH), Finish.WORN_1, "cadeira gasta 1");
		h.succeed();
	}

	@GameTest
	public void vanillaWoodNeverWeathers(GameTestHelper h) {
		List<Block> vanilla = List.of(Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.CHERRY_PLANKS, Blocks.BAMBOO_PLANKS);
		for (int i = 0; i < vanilla.size(); i++) {
			BlockPos rel = new BlockPos(1 + 2 * i, 1, 1);
			h.setBlock(rel, vanilla.get(i));
			for (int k = 0; k < 10; k++) h.assertFalse(ageInRain(h, rel), "a madeira do jogo nunca se gasta: " + vanilla.get(i));
			h.assertValueEqual(h.getBlockState(rel).getBlock(), vanilla.get(i), "continua a do jogo");
			h.assertFalse(h.getBlockState(rel).isRandomlyTicking(), "nem recebe random ticks");
		}
		h.succeed();
	}

	@GameTest
	public void varnishedNeverWeathers(GameTestHelper h) {
		BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 1), c = new BlockPos(5, 1, 1);
		h.setBlock(a, FinishBlocks.NEW.get("cherry").get(Kind.PARQUET).defaultBlockState().setValue(FinishState.FINISH, Finish.HONEY));
		h.setBlock(b, FinishBlocks.BY_VANILLA.get(Blocks.OAK_PLANKS).get(Finish.WHITE_LACQUER_POLISHED));
		h.setBlock(c, MoveisBlocks.wooden(Furniture.TABLE, "oak").defaultBlockState().setValue(FinishState.FINISH, Finish.EBONY));
		for (BlockPos rel : List.of(a, b, c)) {
			BlockState before = h.getBlockState(rel);
			h.assertFalse(before.isRandomlyTicking(), "envernizada não recebe random ticks");
			for (int k = 0; k < 10; k++) h.assertFalse(ageInRain(h, rel), "envernizada não se gasta: " + before);
			h.assertValueEqual(h.getBlockState(rel), before, "igual");
		}
		h.succeed();
	}

	@GameTest
	public void furnitureFinishKeepsState(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		BlockPos chair = new BlockPos(1, 1, 1), cab = new BlockPos(3, 1, 1), t1 = new BlockPos(5, 1, 1), t2 = new BlockPos(6, 1, 1);
		for (String suffix : FinishFurniture.SUFFIXES)
			for (WoodTypes.Wood w : WoodTypes.ALL) {
				Block b = BuiltInRegistries.BLOCK.getValue(com.tcordeiro.moveis.TMoveis.id(w.id() + "_" + suffix));
				h.assertTrue(b.defaultBlockState().hasProperty(FinishState.FINISH), "móvel com finish: " + b);
				h.assertValueEqual(b.defaultBlockState().getValue(FinishState.FINISH), Finish.RAW, "cru por omissão");
			}
		h.setBlock(chair, MoveisBlocks.wooden(Furniture.CHAIR, "oak").defaultBlockState().setValue(com.tcordeiro.moveis.block.ChairBlock.FACING, Direction.EAST));
		h.setBlock(cab, MoveisBlocks.wooden(Furniture.CABINET, "birch").defaultBlockState().setValue(StorageFurnitureBlock.FACING, Direction.WEST));
		TestUtil.place(h, t1, MoveisBlocks.wooden(Furniture.TABLE, "acacia").defaultBlockState());
		TestUtil.place(h, t2, MoveisBlocks.wooden(Furniture.TABLE, "acacia").defaultBlockState());
		TestUtil.place(h, t1, h.getBlockState(t1));
		for (BlockPos rel : List.of(chair, cab, t1)) {
			BlockState before = h.getBlockState(rel);
			brushAt(h, p, rel, Tone.WALNUT, brush(Tone.WALNUT));
			BlockState after = h.getBlockState(rel);
			h.assertValueEqual(after.getBlock(), before.getBlock(), "mesmo móvel");
			h.assertValueEqual(after.getValue(FinishState.FINISH), Finish.WALNUT, "nogueira");
			assertSameState(h, before, after, before.getBlock().toString());
			brushAt(h, p, rel, Tone.WALNUT, brush(Tone.WALNUT));
			h.assertValueEqual(h.getBlockState(rel).getValue(FinishState.FINISH), Finish.WALNUT_POLISHED, "polido");
			axeAt(h, p, rel);
			axeAt(h, p, rel);
			h.assertValueEqual(h.getBlockState(rel).getValue(FinishState.FINISH), Finish.RAW, "o machado tira tudo");
		}
		h.assertTrue(h.getBlockState(t1).getValue(TableBlock.EAST), "a mesa continua ligada à vizinha");
		h.succeed();
	}

	@GameTest
	public void brushUsesDurability(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		ItemStack br = brush(Tone.NATURAL);
		h.assertValueEqual(br.getMaxDamage(), 64, "64 demãos");
		BlockPos rel = new BlockPos(1, 1, 1);
		h.setBlock(rel, Blocks.MANGROVE_PLANKS);
		brushAt(h, p, rel, Tone.NATURAL, br);
		h.assertValueEqual(br.getDamageValue(), 1, "gastou 1");
		h.setBlock(rel, Blocks.STONE);
		h.assertValueEqual(brushAt(h, p, rel, Tone.NATURAL, br), InteractionResult.PASS, "pedra não leva verniz");
		h.assertValueEqual(br.getDamageValue(), 1, "e não gasta o pincel");
		h.setBlock(rel, Blocks.OAK_DOOR);
		h.assertValueEqual(brushAt(h, p, rel, Tone.NATURAL, br), InteractionResult.PASS, "portas não");
		h.setBlock(rel, Blocks.OAK_FENCE);
		h.assertValueEqual(brushAt(h, p, rel, Tone.NATURAL, br), InteractionResult.PASS, "cercas não");
		h.setBlock(rel, Blocks.OAK_TRAPDOOR);
		h.assertValueEqual(brushAt(h, p, rel, Tone.NATURAL, br), InteractionResult.PASS, "alçapões não");
		br.setDamageValue(63);
		h.setBlock(rel, Blocks.MANGROVE_PLANKS);
		brushAt(h, p, rel, Tone.NATURAL, br);
		h.assertTrue(br.isEmpty(), "a última demão parte o pincel");
		h.succeed();
	}

	@GameTest
	public void dropsKeepTheFinish(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		BlockPos rel = new BlockPos(1, 1, 1), abs = h.absolutePos(rel);
		Block parquet = FinishBlocks.NEW.get("oak").get(Kind.PARQUET);
		BlockState honey = parquet.defaultBlockState().setValue(FinishState.FINISH, Finish.HONEY);
		List<ItemStack> drops = Block.getDrops(honey, level, abs, null);
		h.assertValueEqual(drops.size(), 1, "1 item");
		ItemStack d = drops.get(0);
		h.assertValueEqual(d.get(DataComponents.BLOCK_STATE).get(FinishState.FINISH), Finish.HONEY, "o item guarda o mel");
		h.assertValueEqual(d.getItemName(), honeyParquet().getItemName(), "nome «… (Mel)», igual ao do criativo");
		ItemStack raw = Block.getDrops(parquet.defaultBlockState(), level, abs, null).get(0);
		h.assertTrue(!raw.has(DataComponents.BLOCK_STATE), "cru: item simples (empilha com os novos)");
		h.assertTrue(ItemStack.isSameItemSameComponents(raw, new ItemStack(parquet)), "igual a um parquet novo");
		var player = TestUtil.playerAt(h, new BlockPos(1, 2, 3));
		h.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
		TestUtil.placeWithItem(h, player, d.copy(), new BlockPos(3, 2, 1), 0);
		h.assertValueEqual(h.getBlockState(new BlockPos(3, 2, 1)).getValue(FinishState.FINISH), Finish.HONEY, "posto de volta: mel");
		Block chair = MoveisBlocks.wooden(Furniture.CHAIR, "oak");
		ItemStack c = Block.getDrops(chair.defaultBlockState().setValue(FinishState.FINISH, Finish.EBONY_POLISHED), level, abs, null).get(0);
		h.assertValueEqual(c.get(DataComponents.BLOCK_STATE).get(FinishState.FINISH), Finish.EBONY_POLISHED, "a cadeira guarda o ébano polido");
		h.succeed();
	}

	private static ItemStack honeyParquet() {
		return com.tcordeiro.moveis.finish.FinishItems.stack(FinishBlocks.NEW.get("oak").get(Kind.PARQUET), Finish.HONEY);
	}

	@GameTest
	public void everyFinishBlockHasItemLootAndName(GameTestHelper h) {
		var server = h.getLevel().getServer();
		var lang = net.minecraft.locale.Language.getInstance();
		int n = 0;
		for (Block b : FinishBlocks.all()) {
			var id = BuiltInRegistries.BLOCK.getKey(b);
			h.assertTrue(b.asItem() != Items.AIR, "item de " + id);
			h.assertTrue(b.getLootTable().isPresent()
					&& server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE).flatMap(l -> l.get(b.getLootTable().get())).isPresent(),
					"loot de " + id);
			h.assertTrue(lang.has(b.getDescriptionId()), "nome de " + id);
			n++;
		}
		int woods = WoodTypes.ALL.size();
		h.assertValueEqual(n, woods * 3 * 14 + woods * 11 + 1, "tábuas/escadas/lajes × 14 + 11 blocos novos por madeira + Serraria");
		for (Finish f : Finish.values()) h.assertTrue(lang.has("finish.tmoveis." + f.getSerializedName()), "nome do acabamento " + f);
		for (Tone t : Tone.values()) {
			h.assertTrue(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE,
					com.tcordeiro.moveis.TMoveis.id("varnish_brush_" + t.id))).isPresent(), "receita do pincel " + t);
			h.assertTrue(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE,
					com.tcordeiro.moveis.TMoveis.id("varnish_" + t.id))).isPresent(), "receita do verniz " + t);
		}
		h.assertTrue(Component.translatable("container.tmoveis.sawmill").getString().length() > 0, "nome da Serraria");
		h.succeed();
	}
}
