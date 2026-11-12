package com.tcordeiro.moveis.finish.sawmill;

import java.util.List;

import com.tcordeiro.moveis.finish.FinishBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O menu da Serraria — o do cortador de pedras (StonecutterMenu) com as receitas tmoveis:sawing. As receitas para a entrada vêm de
 * {@link Sawing#recipesFor} (servidor: RecipeManager; cliente: as sincronizadas), pela mesma ordem nos dois lados.
 */
public class SawmillMenu extends AbstractContainerMenu {
	public static final int INPUT_SLOT = 0, RESULT_SLOT = 1;
	private static final int INV_START = 2, INV_END = 29, USE_START = 29, USE_END = 38;

	private final ContainerLevelAccess access;
	private final DataSlot selected = DataSlot.standalone();
	private final Level level;
	private List<RecipeHolder<SawmillRecipe>> recipes = List.of();
	private ItemStack input = ItemStack.EMPTY;
	private long lastSoundTime;
	final Slot inputSlot;
	final Slot resultSlot;
	private Runnable slotUpdateListener = () -> {};
	public final Container container = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			SawmillMenu.this.slotsChanged(this);
			SawmillMenu.this.slotUpdateListener.run();
		}
	};
	private final ResultContainer resultContainer = new ResultContainer();

	public SawmillMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public SawmillMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(Sawing.MENU, id);
		this.access = access;
		this.level = inventory.player.level();
		this.inputSlot = addSlot(new Slot(container, 0, 20, 33));
		this.resultSlot = addSlot(new Slot(resultContainer, 1, 143, 33) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack carried) {
				carried.onCraftedBy(player, carried.getCount());
				resultContainer.awardUsedRecipes(player, List.of(inputSlot.getItem()));
				ItemStack remaining = inputSlot.remove(1);
				if (!remaining.isEmpty()) setupResultSlot(selected.get());
				access.execute((lvl, pos) -> {
					long t = lvl.getGameTime();
					if (lastSoundTime != t) {
						lvl.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 0.8f);
						lastSoundTime = t;
					}
				});
				super.onTake(player, carried);
			}
		});
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlot(selected);
		access.execute((lvl, pos) -> setActive(lvl, pos, true));
	}

	public int getSelectedRecipeIndex() {
		return selected.get();
	}

	public List<RecipeHolder<SawmillRecipe>> getVisibleRecipes() {
		return recipes;
	}

	public int getNumberOfVisibleRecipes() {
		return recipes.size();
	}

	public boolean hasInputItem() {
		return inputSlot.hasItem() && !recipes.isEmpty();
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, FinishBlocks.SAWMILL);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (selected.get() == button) return false;
		if (button >= 0 && button < recipes.size()) {
			selected.set(button);
			setupResultSlot(button);
		}
		return true;
	}

	@Override
	public void slotsChanged(Container changed) {
		ItemStack now = inputSlot.getItem();
		if (!now.is(input.getItem())) {
			input = now.copy();
			selected.set(-1);
			resultSlot.set(ItemStack.EMPTY);
			recipes = Sawing.recipesFor(level, now);
		}
	}

	private void setupResultSlot(int index) {
		if (!recipes.isEmpty() && index >= 0 && index < recipes.size()) {
			RecipeHolder<SawmillRecipe> r = recipes.get(index);
			resultContainer.setRecipeUsed(r);
			resultSlot.set(r.value().assemble(new SingleRecipeInput(container.getItem(0))));
		} else {
			resultSlot.set(ItemStack.EMPTY);
			resultContainer.setRecipeUsed(null);
		}
		broadcastChanges();
	}

	public void registerUpdateListener(Runnable listener) {
		this.slotUpdateListener = listener;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != resultContainer && super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (index == RESULT_SLOT) {
				stack.getItem().onCraftedBy(stack, player);
				if (!moveItemStackTo(stack, INV_START, USE_END, true)) return ItemStack.EMPTY;
				slot.onQuickCraft(stack, clicked);
			} else if (index == INPUT_SLOT) {
				if (!moveItemStackTo(stack, INV_START, USE_END, false)) return ItemStack.EMPTY;
			} else if (!Sawing.recipesFor(level, stack).isEmpty()) {
				if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
			} else if (index >= INV_START && index < INV_END) {
				if (!moveItemStackTo(stack, USE_START, USE_END, false)) return ItemStack.EMPTY;
			} else if (index >= USE_START && index < USE_END && !moveItemStackTo(stack, INV_START, INV_END, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
			slot.setChanged();
			if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
			slot.onTake(player, stack);
			if (index == RESULT_SLOT) player.drop(stack, false, Prediction.PREDICTED);
			broadcastChanges();
		}
		return clicked;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		resultContainer.removeItemNoUpdate(1);
		access.execute((lvl, pos) -> {
			clearContainer(player, container);
			boolean others = lvl instanceof ServerLevel sl && sl.players().stream()
					.anyMatch(p -> p != player && p.containerMenu instanceof SawmillMenu m && m.access.evaluate((l, q) -> q.equals(pos)).orElse(false));
			if (!others) setActive(lvl, pos, false);
		});
	}

	/** Liga/desliga a serra (o bloco guarda ACTIVE; o renderer gira-a). */
	private static void setActive(Level lvl, BlockPos pos, boolean on) {
		BlockState s = lvl.getBlockState(pos);
		if (s.getBlock() instanceof SawmillBlock && s.getValue(SawmillBlock.ACTIVE) != on)
			lvl.setBlock(pos, s.setValue(SawmillBlock.ACTIVE, on), Block.UPDATE_ALL);
	}
}
