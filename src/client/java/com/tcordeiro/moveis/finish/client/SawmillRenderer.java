package com.tcordeiro.moveis.finish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.finish.sawmill.SawmillBlock;
import com.tcordeiro.moveis.finish.sawmill.SawmillBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A serra da Serraria (modelo tmoveis:sawmill_blade, desenhado como item): parada, ou a girar enquanto alguém tem o ecrã aberto
 * (ACTIVE). Roda à volta do eixo (x = 8, y = 16 px), no plano da lâmina.
 */
public class SawmillRenderer implements BlockEntityRenderer<SawmillBlockEntity, SawmillRenderer.State> {
	static final float AX = 8f / 16, AY = 16f / 16;
	/** Graus por tick a girar (uma volta em 0,75 s). */
	static final float SPEED = 24f;
	private static ItemStack blade;

	private static ItemStack bladeStack() {
		if (blade == null) {
			blade = new ItemStack(Items.STICK);
			blade.set(DataComponents.ITEM_MODEL, TMoveis.id("sawmill_blade"));
		}
		return blade;
	}

	public static class State extends BlockEntityRenderState {
		public final ItemStackRenderState blade = new ItemStackRenderState();
		public float angle, facingDeg;
	}

	public SawmillRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SawmillBlockEntity be, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, crumbling);
		BlockState bs = be.getBlockState();
		Direction d = bs.hasProperty(SawmillBlock.FACING) ? bs.getValue(SawmillBlock.FACING) : Direction.NORTH;
		state.facingDeg = d.toYRot() - 180f; // north = 0, como no blockstate
		boolean on = bs.hasProperty(SawmillBlock.ACTIVE) && bs.getValue(SawmillBlock.ACTIVE) && be.getLevel() != null;
		state.angle = on ? ((be.getLevel().getGameTime() % 360) + partialTick) * SPEED % 360f : 0f;
		Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.blade, bladeStack(), ItemDisplayContext.NONE, be.getLevel(), null, 0);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5f, 0, 0.5f);
		pose.rotateDegrees(Axis.YN, state.facingDeg);
		pose.translate(-0.5f, 0, -0.5f);
		pose.translate(AX, AY, 0);
		pose.rotateDegrees(Axis.ZP, state.angle);
		pose.translate(-AX, -AY, 0);
		pose.translate(0.5f, 0.5f, 0.5f); // o modelo de item com contexto NONE é desenhado centrado
		state.blade.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}
}
