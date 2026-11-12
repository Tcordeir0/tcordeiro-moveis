package com.tcordeiro.moveis.finish;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Pincel de verniz de um tom (64 demãos). O clique direito num bloco é tratado em {@link FinishInteractions} (antes do próprio bloco:
 * com o pincel na mão, uma cadeira enverniza-se em vez de se sentar).
 */
public class VarnishBrushItem extends Item {
	public static final int USES = 64;
	public final Tone tone;

	public VarnishBrushItem(Tone tone, Properties props) {
		super(props);
		this.tone = tone;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.tmoveis.brush").withStyle(ChatFormatting.GRAY));
	}
}
