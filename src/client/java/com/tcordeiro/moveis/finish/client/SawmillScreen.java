package com.tcordeiro.moveis.finish.client;

import java.util.List;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.tcordeiro.moveis.finish.sawmill.SawmillMenu;
import com.tcordeiro.moveis.finish.sawmill.SawmillRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeHolder;

/** O ecrã da Serraria: o do cortador de pedras (mesmas texturas e geometria: 4 colunas × 3 linhas, barra de rolar). */
public class SawmillScreen extends AbstractContainerScreen<SawmillMenu> {
	private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/stonecutter/scroller");
	private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
	private static final Identifier RECIPE_SELECTED = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
	private static final Identifier RECIPE_HIGHLIGHTED = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
	private static final Identifier RECIPE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
	private static final Identifier BG = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
	private static final int COLS = 4, ROWS = 3, X0 = 52, Y0 = 14;
	private float scrollOffs;
	private boolean scrolling;
	private int startIndex;
	private boolean displayRecipes;

	public SawmillScreen(SawmillMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		menu.registerUpdateListener(this::containerChanged);
		this.titleLabelY--;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		int xo = leftPos, yo = topPos;
		g.blit(RenderPipelines.GUI_TEXTURED, BG, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
		int sy = (int) (41.0F * scrollOffs);
		int sx0 = xo + 119, sy0 = yo + 15;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, scrollBarActive() ? SCROLLER : SCROLLER_DISABLED, sx0, sy0 + sy, 12, 15);
		if (mouseX >= sx0 && mouseY >= sy0 && mouseX < sx0 + 12 && mouseY < sy0 + 54)
			g.requestCursor(scrollBarActive() ? (scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND) : CursorTypes.NOT_ALLOWED);
		int x = leftPos + X0, y = topPos + Y0, end = startIndex + COLS * ROWS;
		buttons(g, mouseX, mouseY, x, y, end);
		icons(g, x, y, end);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		if (!displayRecipes) return;
		List<RecipeHolder<SawmillRecipe>> rs = menu.getVisibleRecipes();
		for (int i = startIndex; i < startIndex + COLS * ROWS && i < rs.size(); i++) {
			int p = i - startIndex, ix = leftPos + X0 + p % COLS * 16, iy = topPos + Y0 + p / COLS * 18 + 2;
			if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 18)
				g.setTooltipForNextFrame(font, rs.get(i).value().resultItem(), mouseX, mouseY);
		}
	}

	private void buttons(GuiGraphicsExtractor g, int xm, int ym, int x, int y, int end) {
		for (int i = startIndex; i < end && i < menu.getNumberOfVisibleRecipes(); i++) {
			int p = i - startIndex, px = x + p % COLS * 16, py = y + p / COLS * 18 + 2;
			Identifier sprite = i == menu.getSelectedRecipeIndex() ? RECIPE_SELECTED
					: (xm >= px && ym >= py && xm < px + 16 && ym < py + 18) ? RECIPE_HIGHLIGHTED : RECIPE;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, px, py - 1, 16, 18);
			if (xm >= px && ym >= py - 1 && xm < px + 16 && ym < py + 17) g.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private void icons(GuiGraphicsExtractor g, int x, int y, int end) {
		List<RecipeHolder<SawmillRecipe>> rs = menu.getVisibleRecipes();
		for (int i = startIndex; i < end && i < rs.size(); i++) {
			int p = i - startIndex;
			g.item(rs.get(i).value().resultItem(), x + p % COLS * 16, y + p / COLS * 18 + 2);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (displayRecipes) {
			int xo = leftPos + X0, yo = topPos + Y0;
			for (int i = startIndex; i < startIndex + COLS * ROWS; i++) {
				int p = i - startIndex;
				double xx = event.x() - (xo + p % COLS * 16), yy = event.y() - (yo + p / COLS * 18);
				if (xx >= 0 && yy >= 0 && xx < 16 && yy < 18 && menu.clickMenuButton(minecraft.player, i)) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
					return true;
				}
			}
			xo = leftPos + 119;
			yo = topPos + 9;
			if (event.x() >= xo && event.x() < xo + 12 && event.y() >= yo && event.y() < yo + 54) scrolling = true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (scrolling && scrollBarActive()) {
			int y1 = topPos + 14, y2 = y1 + 54;
			scrollOffs = Mth.clamp(((float) event.y() - y1 - 7.5F) / (y2 - y1 - 15.0F), 0.0F, 1.0F);
			startIndex = (int) (scrollOffs * offscreenRows() + 0.5) * COLS;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		scrolling = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (super.mouseScrolled(x, y, scrollX, scrollY)) return true;
		if (scrollBarActive()) {
			int rows = offscreenRows();
			scrollOffs = Mth.clamp(scrollOffs - (float) scrollY / rows, 0.0F, 1.0F);
			startIndex = (int) (scrollOffs * rows + 0.5) * COLS;
		}
		return true;
	}

	private boolean scrollBarActive() {
		return displayRecipes && menu.getNumberOfVisibleRecipes() > COLS * ROWS;
	}

	private int offscreenRows() {
		return (menu.getNumberOfVisibleRecipes() + COLS - 1) / COLS - ROWS;
	}

	private void containerChanged() {
		displayRecipes = menu.hasInputItem();
		scrollOffs = 0.0F;
		startIndex = 0;
	}
}
