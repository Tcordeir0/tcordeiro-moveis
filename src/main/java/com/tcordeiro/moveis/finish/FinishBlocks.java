package com.tcordeiro.moveis.finish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.finish.sawmill.Sawing;
import com.tcordeiro.moveis.finish.sawmill.SawmillBlock;
import com.tcordeiro.moveis.finish.sawmill.SawmillBlockEntity;
import com.tcordeiro.moveis.wood.WoodTypes;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registo da madeira e verniz (0.3.0), à parte de {@link com.tcordeiro.moveis.registry.MoveisBlocks}:
 * <ul>
 * <li>tábuas, escadas e lajes do jogo envernizadas: um bloco por madeira × forma × acabamento (7 tons × polido ou não);</li>
 * <li>blocos novos (parquet, espinha de peixe, assoalho com escadas e lajes; lambri; viga) com a propriedade «finish»;</li>
 * <li>pincéis e vernizes (um por tom) e a Serraria.</li>
 * </ul>
 * Os ids e modelos batem com tools/gen_finishes.py.
 */
public final class FinishBlocks {
	private FinishBlocks() {}

	/** Uma versão envernizada de uma tábua/escada/laje do jogo. */
	public record Varnished(Block vanilla, Finish finish) {}

	/** Os blocos novos: id e prefixo das escadas/lajes (null = sem escadas nem lajes). */
	public enum Kind {
		PARQUET("parquet", "parquet"), HERRINGBONE("herringbone", "herringbone"), FLOORBOARDS("floorboards", "floorboard"),
		PANELING("paneling", null), BEAM("beam", null);

		public final String id, stairsId;

		Kind(String id, String stairsId) {
			this.id = id;
			this.stairsId = stairsId;
		}
	}

	/** Tábua/escada/laje do jogo → acabamento → bloco envernizado. */
	public static final Map<Block, Map<Finish, Block>> BY_VANILLA = new HashMap<>();
	public static final Map<Block, Varnished> VARNISHED = new HashMap<>();
	/** madeira → tipo → bloco (e as escadas e lajes dos que as têm). */
	public static final Map<String, Map<Kind, Block>> NEW = new LinkedHashMap<>(), NEW_STAIRS = new LinkedHashMap<>(),
			NEW_SLABS = new LinkedHashMap<>();
	public static final Map<Tone, Item> BRUSHES = new EnumMap<>(Tone.class), VARNISHES = new EnumMap<>(Tone.class);
	/** Todos os blocos com a propriedade finish deste registo (os móveis têm-na à parte). */
	public static final List<Block> WITH_FINISH = new ArrayList<>();
	private static final List<Block> ALL = new ArrayList<>();

	public static final Block SAWMILL;
	public static final BlockEntityType<SawmillBlockEntity> SAWMILL_BE;

	static {
		for (WoodTypes.Wood w : WoodTypes.ALL) {
			Block planks = vanilla(w.planks()), stairs = vanilla(w.id() + "_stairs"), slab = vanilla(w.slab());
			for (Finish f : Finish.values()) {
				if (!f.varnished()) continue;
				String s = "_" + f.getSerializedName();
				Block p = block(w.id() + "_planks" + s, Block::new, BlockBehaviour.Properties.ofLegacyCopy(planks), w.flammable());
				Block st = block(w.id() + "_stairs" + s, pp -> new StairBlock(p.defaultBlockState(), pp), BlockBehaviour.Properties.ofLegacyCopy(stairs),
						w.flammable());
				Block sl = block(w.id() + "_slab" + s, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(slab), w.flammable());
				varnished(planks, p, f);
				varnished(stairs, st, f);
				varnished(slab, sl, f);
			}
			Map<Kind, Block> nb = new EnumMap<>(Kind.class), ns = new EnumMap<>(Kind.class), nl = new EnumMap<>(Kind.class);
			for (Kind k : Kind.values()) {
				Block b = block(w.id() + "_" + k.id, k == Kind.BEAM ? FinishPillarBlock::new : FinishBlock::new,
						BlockBehaviour.Properties.ofLegacyCopy(planks), w.flammable());
				nb.put(k, b);
				WITH_FINISH.add(b);
				if (k.stairsId == null) continue;
				Block st = block(w.id() + "_" + k.stairsId + "_stairs", pp -> new FinishStairBlock(b.defaultBlockState(), pp),
						BlockBehaviour.Properties.ofLegacyCopy(stairs), w.flammable());
				Block sl = block(w.id() + "_" + k.stairsId + "_slab", FinishSlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(slab), w.flammable());
				ns.put(k, st);
				nl.put(k, sl);
				WITH_FINISH.add(st);
				WITH_FINISH.add(sl);
			}
			NEW.put(w.id(), nb);
			NEW_STAIRS.put(w.id(), ns);
			NEW_SLABS.put(w.id(), nl);
		}
		for (Tone t : Tone.values()) {
			VARNISHES.put(t, item("varnish_" + t.id, Item::new, new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
			BRUSHES.put(t, item("varnish_brush_" + t.id, p -> new VarnishBrushItem(t, p), new Item.Properties().durability(VarnishBrushItem.USES)));
		}
		SAWMILL = block("sawmill", SawmillBlock::new, BlockBehaviour.Properties.ofLegacyCopy(vanilla("stonecutter")).noOcclusion(), false);
		SAWMILL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("sawmill"),
				new BlockEntityType<>(SawmillBlockEntity::new, Set.of(SAWMILL)));
	}

	private static Block vanilla(String id) {
		Block b = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
		if (b == null || b == net.minecraft.world.level.block.Blocks.AIR) throw new IllegalStateException("sem minecraft:" + id);
		return b;
	}

	private static void varnished(Block vanilla, Block block, Finish f) {
		BY_VANILLA.computeIfAbsent(vanilla, k -> new EnumMap<>(Finish.class)).put(f, block);
		VARNISHED.put(block, new Varnished(vanilla, f));
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props, boolean flammable) {
		Identifier id = TMoveis.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		if (flammable) FlammableBlockRegistry.getDefaultInstance().add(block, 5, 20); // como as tábuas
		ALL.add(block);
		return block;
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TMoveis.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	/** Todos os blocos deste registo (690), pela ordem do registo. */
	public static List<Block> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static void init() {
		Sawing.init();
		FinishInteractions.init();
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TMoveis.id("wood"));
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tab, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.tmoveis.wood"))
				.icon(() -> new ItemStack(BRUSHES.get(Tone.HONEY)))
				.displayItems((params, out) -> {
					out.accept(SAWMILL);
					for (Tone t : Tone.values()) out.accept(VARNISHES.get(t));
					for (Tone t : Tone.values()) out.accept(BRUSHES.get(t));
					for (Block b : ALL) if (b != SAWMILL && !VARNISHED.containsKey(b)) out.accept(b);
					for (Block b : ALL) if (VARNISHED.containsKey(b)) out.accept(b);
				}).build());
	}
}
