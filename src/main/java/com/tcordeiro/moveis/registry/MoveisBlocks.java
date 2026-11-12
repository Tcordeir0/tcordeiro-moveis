package com.tcordeiro.moveis.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.ChairBlock;
import com.tcordeiro.moveis.block.ChandelierBlock;
import com.tcordeiro.moveis.block.DoubleBedBlock;
import com.tcordeiro.moveis.block.LampBlock;
import com.tcordeiro.moveis.block.SofaBlock;
import com.tcordeiro.moveis.block.StoolBlock;
import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.block.TableBlock;
import com.tcordeiro.moveis.block.TableLampBlock;
import com.tcordeiro.moveis.block.WallLampBlock;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.entity.SeatEntity;
import com.tcordeiro.moveis.wood.WoodTypes;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Registo de tudo. Os móveis de madeira saem de {@link WoodTypes#ALL} (todas as madeiras do jogo, lista gerada por
 * tools/gen_data.py a partir do jar 26.3): um bloco por madeira e por tipo ({@link Furniture}). Sofás e camas: um por cor.
 */
public final class MoveisBlocks {
	private MoveisBlocks() {}

	/** Os móveis de madeira (id = &lt;madeira&gt;_&lt;sufixo&gt;). */
	public enum Furniture {
		CHAIR("chair"), STOOL("stool"), TABLE("table"), CABINET("cabinet"), DRAWER("drawer");

		public final String suffix;

		Furniture(String suffix) {
			this.suffix = suffix;
		}
	}

	/** Tipo → madeira → bloco (pela ordem de {@link WoodTypes#ALL}). */
	public static final Map<Furniture, Map<String, Block>> WOODEN = new EnumMap<>(Furniture.class);
	public static final Map<DyeColor, Block> SOFAS = new EnumMap<>(DyeColor.class);
	public static final Map<DyeColor, Block> DOUBLE_BEDS = new EnumMap<>(DyeColor.class);
	/** Tudo o que tem item, pela ordem do separador criativo. */
	private static final List<Block> ALL = new ArrayList<>();

	public static final Block CHANDELIER, WALL_LAMP, TABLE_LAMP;
	public static final BlockEntityType<StorageFurnitureBlockEntity> STORAGE_BE;
	public static final BlockEntityType<LampBlockEntity> LAMP_BE;
	public static final EntityType<SeatEntity> SEAT;

	static {
		for (Furniture f : Furniture.values()) WOODEN.put(f, new LinkedHashMap<>());
		for (WoodTypes.Wood w : WoodTypes.ALL) {
			Block planks = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(w.planks()));
			for (Furniture f : Furniture.values()) {
				BlockBehaviour.Properties p = BlockBehaviour.Properties.ofLegacyCopy(planks).noOcclusion();
				Block b = block(w.id() + "_" + f.suffix, switch (f) {
					case CHAIR -> ChairBlock::new;
					case STOOL -> StoolBlock::new;
					case TABLE -> TableBlock::new;
					case CABINET -> pp -> new StorageFurnitureBlock(StorageFurnitureBlock.Kind.CABINET, pp);
					case DRAWER -> pp -> new StorageFurnitureBlock(StorageFurnitureBlock.Kind.DRAWER, pp);
				}, p, 64);
				WOODEN.get(f).put(w.id(), b);
				if (w.flammable()) FlammableBlockRegistry.getDefaultInstance().add(b, 5, 20); // como as tábuas
			}
		}
		for (DyeColor c : DyeColor.values()) {
			Block sofa = block(c.getSerializedName() + "_sofa", p -> new SofaBlock(c, p), BlockBehaviour.Properties.of().mapColor(c.getMapColor())
					.strength(0.8f).sound(SoundType.WOOL).ignitedByLava().noOcclusion(), 64);
			SOFAS.put(c, sofa);
			FlammableBlockRegistry.getDefaultInstance().add(sofa, 30, 60); // como a lã
		}
		for (DyeColor c : DyeColor.values()) {
			Block bed = block(c.getSerializedName() + "_double_bed", p -> new DoubleBedBlock(c, p), BlockBehaviour.Properties.of()
					.mapColor(c.getMapColor()).sound(SoundType.WOOD).strength(0.2f).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED), 1);
			DOUBLE_BEDS.put(c, bed);
		}
		CHANDELIER = block("chandelier", ChandelierBlock::new, lamp(15), 64);
		WALL_LAMP = block("wall_lamp", WallLampBlock::new, lamp(14), 64);
		TABLE_LAMP = block("table_lamp", TableLampBlock::new, lamp(13), 64);

		Set<Block> storage = new HashSet<>(WOODEN.get(Furniture.CABINET).values());
		storage.addAll(WOODEN.get(Furniture.DRAWER).values());
		STORAGE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("storage_furniture"),
				new BlockEntityType<>(StorageFurnitureBlockEntity::new, Set.copyOf(storage)));
		LAMP_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("lamp"),
				new BlockEntityType<>(LampBlockEntity::create, Set.of(CHANDELIER, WALL_LAMP, TABLE_LAMP)));

		ResourceKey<EntityType<?>> seatKey = ResourceKey.create(Registries.ENTITY_TYPE, TMoveis.id("seat"));
		SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, seatKey, EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
				.sized(0.001f, 0.001f).noSummon().noLootTable().clientTrackingRange(10).updateInterval(20).build(seatKey));
	}

	private static BlockBehaviour.Properties lamp(int light) {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.COPPER).noOcclusion()
				.pushReaction(PushReaction.POPPED).lightLevel(s -> s.getValue(LampBlock.LIT) ? light : 0);
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props, int stack) {
		Identifier id = TMoveis.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().stacksTo(stack)));
		ALL.add(block);
		return block;
	}

	public static List<Block> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static Block wooden(Furniture f, String wood) {
		return WOODEN.get(f).get(wood);
	}

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TMoveis.id("furniture"));
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tab, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.tmoveis.furniture"))
				.icon(() -> new ItemStack(wooden(Furniture.CHAIR, "oak")))
				.displayItems((params, out) -> {
					for (Furniture f : Furniture.values()) for (Block b : WOODEN.get(f).values()) out.accept(b);
					for (Block b : SOFAS.values()) out.accept(b);
					for (Block b : DOUBLE_BEDS.values()) out.accept(b);
					out.accept(CHANDELIER);
					out.accept(WALL_LAMP);
					out.accept(TABLE_LAMP);
				}).build());
	}
}
