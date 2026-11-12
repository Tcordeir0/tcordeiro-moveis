package com.tcordeiro.moveis.client;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.AnimatedFurniture;

import org.slf4j.Logger;

import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Modelos dos móveis animados sem MOVING no blockstate (0.3.3).
 * <p>Os blockstates destes móveis continuam a ser os de sempre, com a chave «moving» (parado = modelo inteiro; a mexer = só o corpo),
 * mas ficam em {@code assets/tmoveis/anim_blockstates/} (o jogo não os lê: o bloco já não tem essa propriedade). Aqui, para cada bloco
 * com um desses ficheiros, monta-se uma definição de estados FALSA (as propriedades do bloco + moving), o próprio jogo instancia o
 * ficheiro sobre ela ({@link BlockStateModelDispatcher#instantiate}) e cada estado real fica com DOIS modelos: o de moving=false e o de
 * moving=true — exatamente os mesmos de antes, por isso o aspeto não muda. Ao desenhar o chunk, escolhe-se pela entidade
 * ({@code getRenderData}: a animação está a mexer?).
 */
public final class AnimModels {
	private static final Logger LOG = LogUtils.getLogger();
	public static final FileToIdConverter LISTER = FileToIdConverter.json("anim_blockstates");

	private AnimModels() {}

	public static void init() {
		PreparableModelLoadingPlugin.register((state, executor) -> CompletableFuture.supplyAsync(() -> load(state.resourceManager()), executor),
				(defs, ctx) -> defs.forEach((block, def) -> ctx.registerBlockStateResolver(block, r -> resolve(block, def, r::setModel))));
	}

	/** Lê os ficheiros {@code anim_blockstates/<bloco>.json} do mod (o do pacote de recursos de cima, como o jogo). */
	static Map<Block, BlockStateModelDispatcher> load(net.minecraft.server.packs.resources.ResourceManager manager) {
		Map<Block, BlockStateModelDispatcher> out = new HashMap<>();
		LISTER.listMatchingResources(manager).forEach((file, res) -> {
			Identifier id = LISTER.fileToId(file);
			if (!id.getNamespace().equals(TMoveis.MOD_ID)) return;
			Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
			if (block == null) return;
			try (Reader reader = res.openAsReader()) {
				out.put(block, BlockStateModelDispatcher.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow());
			} catch (Exception e) {
				LOG.error("Móveis: não li {}", file, e);
			}
		});
		return out;
	}

	/** Os estados reais de {@code block} → o modelo que escolhe, ao desenhar, entre o de moving=false e o de moving=true. */
	static void resolve(Block block, BlockStateModelDispatcher def, java.util.function.BiConsumer<BlockState, BlockStateModel.UnbakedRoot> out) {
		StateDefinition.Builder<Block, BlockState> b = new StateDefinition.Builder<>(block);
		for (Property<?> p : block.getStateDefinition().getProperties()) b.add(p);
		b.add(AnimatedFurniture.MOVING);
		StateDefinition<Block, BlockState> fake = b.create(Block::defaultBlockState, BlockState::new);
		Map<BlockState, BlockStateModel.UnbakedRoot> models = def.instantiate(fake, () -> BuiltInRegistries.BLOCK.getKey(block).toString());
		for (BlockState real : block.getStateDefinition().getPossibleStates()) {
			BlockState still = fakeOf(fake, real, false), moving = fakeOf(fake, real, true);
			BlockStateModel.UnbakedRoot s = models.get(still), m = models.get(moving);
			if (s == null || m == null) {
				LOG.warn("Móveis: {} sem modelo para {} (moving={})", BuiltInRegistries.BLOCK.getKey(block), real, s == null ? "false" : "true");
				continue; // fica o modelo em falta, como o jogo faria
			}
			out.accept(real, new Unbaked(s, still, m, moving));
		}
	}

	/** O estado falso com os valores do real e moving = {@code moving}. */
	static BlockState fakeOf(StateDefinition<Block, BlockState> fake, BlockState real, boolean moving) {
		BlockState st = fake.any();
		for (Property<?> p : real.getProperties()) st = copy(real, st, p);
		return st.setValue(AnimatedFurniture.MOVING, moving);
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> p) {
		return to.setValue(p, from.getValue(p));
	}

	/** O par (parado, a mexer) por estado real. */
	record Unbaked(BlockStateModel.UnbakedRoot still, BlockState stillState, BlockStateModel.UnbakedRoot moving, BlockState movingState)
			implements BlockStateModel.UnbakedRoot {
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
			still.resolveDependencies(resolver);
			moving.resolveDependencies(resolver);
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			return new Switch(still.bake(stillState, baker), moving.bake(movingState, baker));
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return List.of(still.visualEqualityGroup(stillState), moving.visualEqualityGroup(movingState));
		}
	}

	/** A entidade deste bloco diz que as partes estão a mexer? (sem entidade ou sem resposta: parado) */
	static boolean moving(BlockAndTintGetter view, BlockPos pos) {
		return view != null && pos != null && view.getBlockEntityRenderData(pos) instanceof Boolean b && b;
	}

	/** O modelo cozido: fora do mundo (item, partículas) é o parado; no chunk, o que a entidade pedir. */
	record Switch(BlockStateModel still, BlockStateModel moving) implements BlockStateModel {
		BlockStateModel pick(BlockAndTintGetter view, BlockPos pos) {
			return AnimModels.moving(view, pos) ? moving : still;
		}

		@Override
		public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
			still.collectParts(random, output);
		}

		@Override
		public Material.Baked particleMaterial() {
			return still.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return still.materialFlags() | moving.materialFlags();
		}

		@Override
		public void emitQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random,
				Predicate<Direction> cullTest) {
			pick(view, pos).emitQuads(emitter, view, pos, state, random, cullTest);
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
			BlockStateModel m = pick(view, pos);
			Object k = m.createGeometryKey(view, pos, state, random);
			return k == null ? null : List.of(m == moving, k);
		}

		@Override
		public Material.Baked particleMaterial(BlockAndTintGetter view, BlockPos pos, BlockState state) {
			return still.particleMaterial(view, pos, state);
		}

		@Override
		public int materialFlags(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
			return pick(view, pos).materialFlags(view, pos, state, random);
		}
	}
}
