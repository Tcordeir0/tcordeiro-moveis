package com.tcordeiro.moveis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tcordeiro.moveis.anim.FurnitureParts;
import com.tcordeiro.moveis.block.DisplayTableBlock;
import com.tcordeiro.moveis.block.entity.DisplayBlockEntity;
import com.tcordeiro.moveis.block.entity.WineRackBlockEntity;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** O que está pousado: o item em cima da mesa de centro/lateral e as garrafas nas casas da adega. Só corre perto e só se houver algo. */
public class ShelfRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, ShelfRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final ItemStackRenderState[] items = new ItemStackRenderState[9];
		final float[] dx = new float[9], dy = new float[9];
		int n;
		boolean onTable;
		float top, facingDeg;
	}

	public ShelfRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return FurnitureRenderer.VIEW;
	}

	@Override
	public boolean shouldRender(T be, Vec3 camera) {
		boolean any = be instanceof DisplayBlockEntity d ? !d.item().isEmpty() : be instanceof WineRackBlockEntity w && !w.isEmpty();
		return any && Vec3.atCenterOf(be.getBlockPos()).closerThan(camera, FurnitureRenderer.VIEW);
	}

	private static ItemStackRenderState slot(State s, int i) {
		return s.items[i] == null ? (s.items[i] = new ItemStackRenderState()) : s.items[i];
	}

	@Override
	public void extractRenderState(T be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
		BlockState st = be.getBlockState();
		s.n = 0;
		s.facingDeg = st.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? st.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() - 180f : 0f;
		var resolver = Minecraft.getInstance().getItemModelResolver();
		if (be instanceof DisplayBlockEntity d) {
			s.onTable = true;
			s.top = st.getBlock() instanceof DisplayTableBlock t ? t.topPx / 16f : 0.5f;
			resolver.updateForTopItem(slot(s, 0), d.item(), ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong());
			s.n = 1;
		} else if (be instanceof WineRackBlockEntity w) {
			s.onTable = false;
			for (int i = 0; i < WineRackBlockEntity.SIZE; i++) {
				if (w.getItem(i).isEmpty()) continue;
				int k = s.n++;
				resolver.updateForTopItem(slot(s, k), PartStacks.get(st.getBlock(), "wine_rack_bottle"), ItemDisplayContext.NONE, be.getLevel(), null, 0);
				s.dx[k] = FurnitureParts.WINE_CELLS[i][0] / 16f;
				s.dy[k] = FurnitureParts.WINE_CELLS[i][1] / 16f;
			}
		}
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (s.n == 0) return;
		pose.pushPose();
		pose.translate(0.5f, 0, 0.5f);
		pose.rotateDegrees(Axis.YN, s.facingDeg);
		if (s.onTable) { // deitado no tampo, como num expositor
			pose.translate(0, s.top + 0.02f, 0);
			pose.rotateDegrees(Axis.XP, 90);
			pose.scale(0.45f, 0.45f, 0.45f);
			s.items[0].submit(pose, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		} else {
			pose.translate(-0.5f, 0, -0.5f);
			for (int i = 0; i < s.n; i++) {
				pose.pushPose();
				pose.translate(s.dx[i] + 0.5f, s.dy[i] + 0.5f, 0.5f);
				s.items[i].submit(pose, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				pose.popPose();
			}
		}
		pose.popPose();
	}
}
