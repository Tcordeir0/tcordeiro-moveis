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
import com.tcordeiro.moveis.block.BathtubBlock;
import com.tcordeiro.moveis.block.BunkBedBlock;
import com.tcordeiro.moveis.block.CeilingFanBlock;
import com.tcordeiro.moveis.block.ChairBlock;
import com.tcordeiro.moveis.block.CounterBlock;
import com.tcordeiro.moveis.block.DeskBlock;
import com.tcordeiro.moveis.block.DisplayTableBlock;
import com.tcordeiro.moveis.block.FacingBlock;
import com.tcordeiro.moveis.block.FireplaceBlock;
import com.tcordeiro.moveis.block.FloorLampBlock;
import com.tcordeiro.moveis.block.MailboxBlock;
import com.tcordeiro.moveis.block.PianoBlock;
import com.tcordeiro.moveis.block.PlanterBoxBlock;
import com.tcordeiro.moveis.block.SafeBlock;
import com.tcordeiro.moveis.block.SimpleSeatBlock;
import com.tcordeiro.moveis.block.SinkBlock;
import com.tcordeiro.moveis.block.ToggleFurnitureBlock;
import com.tcordeiro.moveis.block.WallClockBlock;
import com.tcordeiro.moveis.block.WallDecorBlock;
import com.tcordeiro.moveis.block.WaterFurniture;
import com.tcordeiro.moveis.block.WardrobeBlock;
import com.tcordeiro.moveis.block.WineRackBlock;
import com.tcordeiro.moveis.block.entity.AnimatedBlockEntity;
import com.tcordeiro.moveis.block.entity.ClockBlockEntity;
import com.tcordeiro.moveis.block.entity.DisplayBlockEntity;
import com.tcordeiro.moveis.block.entity.FireplaceBlockEntity;
import com.tcordeiro.moveis.block.entity.MailboxBlockEntity;
import com.tcordeiro.moveis.block.entity.PianoBlockEntity;
import com.tcordeiro.moveis.block.entity.WaterBlockEntity;
import com.tcordeiro.moveis.block.entity.WineRackBlockEntity;
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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
		CHAIR("chair"), STOOL("stool"), TABLE("table"), CABINET("cabinet"), DRAWER("drawer"),
		// 0.2.0
		COUNTER("counter"), NIGHTSTAND("nightstand"), WARDROBE("wardrobe"), DESK("desk"), COFFEE_TABLE("coffee_table"),
		SIDE_TABLE("side_table"), GARDEN_BENCH("garden_bench"), PLANTER_BOX("planter_box"), SHUTTERS("shutters"), WINE_RACK("wine_rack");

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

	public static final Map<DyeColor, Block> CURTAINS = new EnumMap<>(DyeColor.class);
	public static final Map<DyeColor, Block> AWNINGS = new EnumMap<>(DyeColor.class);
	public static final Map<DyeColor, Block> BUNK_BEDS = new EnumMap<>(DyeColor.class);

	public static final Block CHANDELIER, WALL_LAMP, TABLE_LAMP;
	public static final Block SINK, TRASH_CAN, MIRROR, WALL_CLOCK, TOILET, BATHTUB, FIREPLACE, CEILING_FAN, MAILBOX, FLOOR_LAMP, PIANO, SAFE;
	public static final BlockEntityType<StorageFurnitureBlockEntity> STORAGE_BE;
	public static final BlockEntityType<LampBlockEntity> LAMP_BE, FAN_BE;
	public static final BlockEntityType<AnimatedBlockEntity> ANIM_BE;
	public static final BlockEntityType<MailboxBlockEntity> MAILBOX_BE;
	public static final BlockEntityType<ClockBlockEntity> CLOCK_BE;
	public static final BlockEntityType<WaterBlockEntity> WATER_BE;
	public static final BlockEntityType<FireplaceBlockEntity> FIREPLACE_BE;
	public static final BlockEntityType<DisplayBlockEntity> DISPLAY_BE;
	public static final BlockEntityType<WineRackBlockEntity> WINE_BE;
	public static final BlockEntityType<PianoBlockEntity> PIANO_BE;
	public static final EntityType<SeatEntity> SEAT;

	static {
		for (Furniture f : Furniture.values()) WOODEN.put(f, new LinkedHashMap<>());
		for (WoodTypes.Wood w : WoodTypes.ALL) {
			Block planks = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(w.planks()));
			for (Furniture f : Furniture.values()) {
				BlockBehaviour.Properties p = BlockBehaviour.Properties.ofLegacyCopy(planks).noOcclusion();
				Function<BlockBehaviour.Properties, Block> make = switch (f) {
					case CHAIR -> ChairBlock::new;
					case STOOL -> StoolBlock::new;
					case TABLE -> TableBlock::new;
					case CABINET -> pp -> new StorageFurnitureBlock(StorageFurnitureBlock.Kind.CABINET, pp);
					case DRAWER -> pp -> new StorageFurnitureBlock(StorageFurnitureBlock.Kind.DRAWER, pp);
					case COUNTER -> CounterBlock::new;
					case NIGHTSTAND -> pp -> new StorageFurnitureBlock(StorageFurnitureBlock.Kind.NIGHTSTAND, pp);
					case WARDROBE -> WardrobeBlock::new;
					case DESK -> DeskBlock::new;
					case COFFEE_TABLE -> pp -> new DisplayTableBlock(8f, Block.box(0, 0, 2, 16, 8, 14), pp);
					case SIDE_TABLE -> pp -> new DisplayTableBlock(13f, Block.box(2, 0, 2, 14, 13, 14), pp);
					case GARDEN_BENCH -> pp -> new SimpleSeatBlock(0.5, Shapes.or(Block.box(0, 0, 2, 16, 8, 14), Block.box(0, 8, 13, 16, 15, 14.5)), pp);
					case PLANTER_BOX -> PlanterBoxBlock::new;
					case SHUTTERS -> pp -> new ToggleFurnitureBlock("shutters", false, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_TRAPDOOR_CLOSE,
							Block.box(0, 0, 15, 16, 16, 16), pp);
					case WINE_RACK -> WineRackBlock::new;
				};
				// 0.3.0: os de madeira levam verniz (propriedade «finish»; lista única em tools/gen_finishes.py → FinishFurniture)
				boolean finish = com.tcordeiro.moveis.finish.FinishFurniture.SUFFIXES.contains(f.suffix);
				Block b = block(w.id() + "_" + f.suffix, finish ? pp -> com.tcordeiro.moveis.finish.FinishState.wood(() -> make.apply(pp)) : make, p, 64);
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

		// ---------------------------------------------------------------- 0.2.0: 16 cores
		for (DyeColor c : DyeColor.values()) {
			Block curtain = block(c.getSerializedName() + "_curtain", p -> new ToggleFurnitureBlock("curtain", true, SoundEvents.WOOL_PLACE,
					SoundEvents.WOOL_HIT, Block.box(0, 0, 14, 16, 16, 16), p), wool(c), 64);
			CURTAINS.put(c, curtain);
			FlammableBlockRegistry.getDefaultInstance().add(curtain, 30, 60);
		}
		for (DyeColor c : DyeColor.values()) {
			Block awning = block(c.getSerializedName() + "_awning", p -> new WallDecorBlock(Block.box(0, 7, 0, 16, 14, 16), p), wool(c).noCollision(), 64);
			AWNINGS.put(c, awning);
			FlammableBlockRegistry.getDefaultInstance().add(awning, 30, 60);
		}
		for (DyeColor c : DyeColor.values())
			BUNK_BEDS.put(c, block(c.getSerializedName() + "_bunk_bed", p -> new BunkBedBlock(c, p), BlockBehaviour.Properties.of()
					.mapColor(c.getMapColor()).sound(SoundType.WOOD).strength(0.2f).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED), 1));

		// ---------------------------------------------------------------- 0.2.0: uma variante
		SINK = block("sink", SinkBlock::new, stone(2f), 64);
		TRASH_CAN = block("trash_can", p -> new StorageFurnitureBlock(StorageFurnitureBlock.Kind.TRASH, p), metal(2f), 64);
		MIRROR = block("mirror", p -> new WallDecorBlock(Block.box(2, 1, 14.5, 14, 15, 16), p),
				BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS).noOcclusion(), 64);
		WALL_CLOCK = block("wall_clock", p -> new WallClockBlock(Block.box(3, 3, 13.6, 13, 13, 16), p),
				BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD).noOcclusion(), 64);
		TOILET = block("toilet", p -> new SimpleSeatBlock(9.5 / 16, Shapes.or(Block.box(3, 0, 1, 13, 9.5, 12), Block.box(3, 6, 12, 13, 16, 16)), p),
				stone(1.5f), 64);
		BATHTUB = block("bathtub", BathtubBlock::new, stone(1.5f), 64);
		FIREPLACE = block("fireplace", FireplaceBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2f, 6f)
				.sound(SoundType.STONE).noOcclusion().lightLevel(s -> s.getValue(FireplaceBlock.LIT) ? 15 : 0), 64);
		CEILING_FAN = block("ceiling_fan", CeilingFanBlock::new, lamp(12), 64);
		MAILBOX = block("mailbox", MailboxBlock::new, metal(1.5f), 64);
		FLOOR_LAMP = block("floor_lamp", FloorLampBlock::new, lamp(14), 64);
		PIANO = block("piano", PianoBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f).sound(SoundType.WOOD)
				.noOcclusion().ignitedByLava(), 64);
		SAFE = block("safe", SafeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f, 1200f).sound(SoundType.METAL)
				.noOcclusion().pushReaction(PushReaction.IMMOVEABLE), 64);

		Set<Block> storage = new HashSet<>();
		for (Furniture f : new Furniture[] {Furniture.CABINET, Furniture.DRAWER, Furniture.COUNTER, Furniture.NIGHTSTAND, Furniture.DESK, Furniture.WARDROBE})
			storage.addAll(WOODEN.get(f).values());
		storage.add(SAFE);
		storage.add(TRASH_CAN);
		STORAGE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("storage_furniture"),
				new BlockEntityType<>(StorageFurnitureBlockEntity::new, Set.copyOf(storage)));
		LAMP_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("lamp"),
				new BlockEntityType<>(LampBlockEntity::create, Set.of(CHANDELIER, WALL_LAMP, TABLE_LAMP, FLOOR_LAMP)));
		FAN_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("ceiling_fan"),
				new BlockEntityType<>((pos, st) -> LampBlockEntity.create(MoveisBlocks.FAN_BE, pos, st), Set.of(CEILING_FAN)));
		Set<Block> anim = new HashSet<>(WOODEN.get(Furniture.WARDROBE).values());
		anim.addAll(WOODEN.get(Furniture.SHUTTERS).values());
		anim.addAll(CURTAINS.values());
		ANIM_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("animated"), new BlockEntityType<>(AnimatedBlockEntity::new, Set.copyOf(anim)));
		MAILBOX_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("mailbox"), new BlockEntityType<>(MailboxBlockEntity::new, Set.of(MAILBOX)));
		CLOCK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("wall_clock"), new BlockEntityType<>(ClockBlockEntity::new, Set.of(WALL_CLOCK)));
		WATER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("water"), new BlockEntityType<>(WaterBlockEntity::new, Set.of(SINK, BATHTUB)));
		FIREPLACE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("fireplace"),
				new BlockEntityType<>(FireplaceBlockEntity::new, Set.of(FIREPLACE)));
		Set<Block> display = new HashSet<>(WOODEN.get(Furniture.COFFEE_TABLE).values());
		display.addAll(WOODEN.get(Furniture.SIDE_TABLE).values());
		DISPLAY_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("display_table"),
				new BlockEntityType<>(DisplayBlockEntity::new, Set.copyOf(display)));
		WINE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("wine_rack"),
				new BlockEntityType<>(WineRackBlockEntity::new, Set.copyOf(WOODEN.get(Furniture.WINE_RACK).values())));
		PIANO_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TMoveis.id("piano"), new BlockEntityType<>(PianoBlockEntity::new, Set.of(PIANO)));

		// pia e banheira: a água pela Transfer API (canos de fluidos de qualquer mod); a banheira pelas duas metades
		FluidStorage.SIDED.registerForBlocks((level, pos, state, be, dir) -> {
			WaterBlockEntity w = WaterFurniture.storage(level, pos, state);
			return w == null ? null : w.tank;
		}, SINK, BATHTUB);

		ResourceKey<EntityType<?>> seatKey = ResourceKey.create(Registries.ENTITY_TYPE, TMoveis.id("seat"));
		SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, seatKey, EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
				.sized(0.001f, 0.001f).noSummon().noLootTable().clientTrackingRange(10).updateInterval(20).build(seatKey));
	}

	private static BlockBehaviour.Properties wool(DyeColor c) {
		return BlockBehaviour.Properties.of().mapColor(c.getMapColor()).strength(0.8f).sound(SoundType.WOOL).ignitedByLava().noOcclusion();
	}

	private static BlockBehaviour.Properties stone(float strength) {
		return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(strength).sound(SoundType.STONE).noOcclusion();
	}

	private static BlockBehaviour.Properties metal(float strength) {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(strength).sound(SoundType.METAL).noOcclusion();
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
					for (Block b : new Block[] {SINK, TRASH_CAN, MIRROR, WALL_CLOCK, TOILET, BATHTUB, FIREPLACE, CEILING_FAN, MAILBOX, FLOOR_LAMP, PIANO, SAFE})
						out.accept(b);
					for (Block b : CURTAINS.values()) out.accept(b);
					for (Block b : AWNINGS.values()) out.accept(b);
					for (Block b : BUNK_BEDS.values()) out.accept(b);
				}).build());
	}
}
