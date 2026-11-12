package com.tcordeiro.moveis.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tcordeiro.moveis.anim.Anim;
import com.tcordeiro.moveis.anim.ClockMath;
import com.tcordeiro.moveis.anim.FurnitureParts;
import com.tcordeiro.moveis.anim.FurnitureParts.Op;
import com.tcordeiro.moveis.anim.FurnitureParts.Part;
import com.tcordeiro.moveis.anim.PianoMath;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.BedSide;
import com.tcordeiro.moveis.block.CeilingFanBlock;
import com.tcordeiro.moveis.block.LampBlock;
import com.tcordeiro.moveis.block.PianoBlock;
import com.tcordeiro.moveis.block.entity.Animated;
import com.tcordeiro.moveis.block.entity.ClockBlockEntity;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.block.entity.PianoBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Desenha as partes móveis dos móveis (como a tampa do baú): portas que rodam na dobradiça, gavetas que deslizam, tampas, cortinas a
 * correr, persianas, a bandeirinha do correio, as pás do ventilador, os ponteiros do relógio e as teclas do piano. Cada parte é o
 * seu modelo (pecas.py → template → filho com as texturas do bloco) com a transformação de {@link FurnitureParts}, interpolada com
 * o partialTick.
 * <p>
 * Desempenho: só corre perto ({@link #VIEW} blocos) e só quando há algo a mexer ({@link #shouldRender}: a animação da entidade diz se
 * as partes estão a mexer; parado, o modelo estático do bloco já tem tudo e nada é criado neste frame). 0.3.3: continua a desenhar
 * {@link com.tcordeiro.moveis.anim.Anim#GRACE} ticks depois do fim (o modelo do chunk só se refaz no fotograma seguinte). As pilhas de itens das partes
 * são criadas uma vez por bloco, parte e acabamento ({@link PartStacks}): as partes dos móveis envernizados, polidos ou gastos têm as
 * mesmas texturas do corpo (0.3.0).
 */
public class FurnitureRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, FurnitureRenderer.State> {
	public static final int VIEW = 32;
	static final int MAX = 24;

	public static class State extends BlockEntityRenderState {
		final ItemStackRenderState[] items = new ItemStackRenderState[MAX];
		final Part[] parts = new Part[MAX];
		final float[] value = new float[MAX], dx = new float[MAX], dy = new float[MAX];
		int n;
		float facingDeg;
	}

	public FurnitureRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return VIEW;
	}

	/** Há partes a desenhar neste momento? (senão, nem estado de desenho se cria) */
	static boolean active(BlockEntity be) {
		BlockState st = be.getBlockState();
		Block b = st.getBlock();
		// 0.3.3: a mexer vem da entidade (não do blockstate) e há uns ticks de graça no fim (o chunk refaz-se um fotograma depois)
		if (be instanceof Animated a && b instanceof AnimatedFurniture af) return af.drawsParts(st, a.anim());
		if (b instanceof CeilingFanBlock && be instanceof LampBlockEntity lamp) return st.getValue(LampBlock.LIT) || lamp.spin.drawing();
		if (be instanceof ClockBlockEntity) return true;
		if (be instanceof PianoBlockEntity piano) return piano.drawsKeys();
		return false;
	}

	@Override
	public boolean shouldRender(T be, Vec3 camera) {
		return active(be) && Vec3.atCenterOf(be.getBlockPos()).closerThan(camera, VIEW);
	}

	private static void add(State s, BlockEntity be, BlockState state, String model, Part part, float value, float dx, float dy) {
		if (s.n >= MAX) return;
		int i = s.n++;
		ItemStackRenderState r = s.items[i] == null ? (s.items[i] = new ItemStackRenderState()) : s.items[i];
		Minecraft.getInstance().getItemModelResolver().updateForTopItem(r, PartStacks.get(state, model), ItemDisplayContext.NONE, be.getLevel(), null, 0);
		s.parts[i] = part;
		s.value[i] = value;
		s.dx[i] = dx;
		s.dy[i] = dy;
	}

	@Override
	public void extractRenderState(T be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
		BlockState st = be.getBlockState();
		Block block = st.getBlock();
		s.n = 0;
		s.facingDeg = st.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? st.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() - 180f : 0f;
		if (be instanceof Animated a && block instanceof AnimatedFurniture af) {
			List<Part> parts = FurnitureParts.BY_KIND.get(af.animKind(st));
			if (parts == null) return;
			float p = Anim.ease(a.anim().progress(partialTick));
			for (Part part : parts) add(s, be, st, part.model(), part, p, 0, 0);
		} else if (be instanceof LampBlockEntity lamp) {
			float angle = lamp.spin.angle(partialTick);
			for (Part part : FurnitureParts.BY_KIND.get("ceiling_fan")) add(s, be, st, part.model(), part, angle, 0, 0);
		} else if (be instanceof ClockBlockEntity && be.getLevel() != null) {
			double t = be.getLevel().getOverworldClockTime() + partialTick;
			for (Part part : FurnitureParts.BY_KIND.get("wall_clock"))
				add(s, be, st, part.model(), part, part.op() == Op.CLOCK_HOUR ? ClockMath.hourAngle(t) : ClockMath.minuteAngle(t), 0, 0);
		} else if (be instanceof PianoBlockEntity piano && be.getLevel() != null) {
			boolean right = st.getValue(PianoBlock.SIDE) == BedSide.RIGHT;
			float now = be.getLevel().getGameTime() + partialTick;
			float[][] keys = FurnitureParts.PIANO_KEYS;
			for (int k = 0; k < keys.length; k++) {
				float x1 = keys[k][1];
				if ((x1 >= 16) != right) continue; // cada metade desenha as teclas que começam nela
				boolean black = PianoMath.black(k);
				float sink = PianoMath.sink(now - piano.pressedAt[k]) * (black ? FurnitureParts.BLACK_SINK : FurnitureParts.WHITE_SINK);
				add(s, be, st, black ? "piano_black_key" : "piano_white_key", null, 0, (x1 - (right ? 16 : 0)) / 16f, -sink / 16f);
			}
		}
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (s.n == 0) return;
		pose.pushPose();
		pose.translate(0.5f, 0, 0.5f);
		pose.rotateDegrees(Axis.YN, s.facingDeg); // como a rotação y do blockstate (norte = 0)
		pose.translate(-0.5f, 0, -0.5f);
		for (int i = 0; i < s.n; i++) {
			pose.pushPose();
			Part p = s.parts[i];
			if (p != null) transform(pose, p, s.value[i]);
			pose.translate(s.dx[i], s.dy[i], 0);
			pose.translate(0.5f, 0.5f, 0.5f); // o modelo de item (contexto NONE) é desenhado centrado
			s.items[i].submit(pose, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		pose.popPose();
	}

	/** A transformação de uma parte, no espaço do bloco (px/16), para o valor v (progresso 0–1, ou ângulo em graus). */
	static void transform(PoseStack pose, Part p, float v) {
		float px = p.px() / 16f, py = p.py() / 16f, pz = p.pz() / 16f;
		switch (p.op()) {
			case ROT -> pose.rotateAround(axis(p.axis()).rotationDegrees(p.amount() * v), px, py, pz);
			case SPIN -> pose.rotateAround(axis(p.axis()).rotationDegrees(v), px, py, pz);
			case CLOCK_HOUR, CLOCK_MINUTE -> pose.rotateAround(axis(p.axis()).rotationDegrees(v), px, py, pz);
			case MOVE -> pose.translate(px * v, py * v, pz * v);
			case SCALEX -> {
				pose.translate(px, 0, 0);
				pose.scale(Anim.lerp(1f, p.amount(), v), 1f, 1f);
				pose.translate(-px, 0, 0);
			}
		}
	}

	static Axis axis(char a) {
		return a == 'x' ? Axis.XP : a == 'z' ? Axis.ZP : Axis.YP;
	}

}
