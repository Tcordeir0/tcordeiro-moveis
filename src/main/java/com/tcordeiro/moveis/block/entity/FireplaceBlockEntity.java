package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.block.FireplaceBlock;
import com.tcordeiro.moveis.compat.ClimaBridge;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** A lenha da lareira (1 espaço, também por funil) e quanto falta a arder. */
public class FireplaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	/** Quanto arde (ticks): tronco 1 minuto, pau 20 segundos. */
	public static final int LOG_TICKS = 1200, STICK_TICKS = 400;
	/** Calor do Clima: alvo, raio e duração de cada renovação (a cada segundo). */
	public static final double WARM_C = 22;
	public static final int WARM_RADIUS = 6, WARM_TTL = 40;
	private static final int[] SLOTS = {0};

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	/** Ticks que faltam à lenha que está a arder. */
	public int burn;
	public int burns, zonesSent;

	public FireplaceBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.FIREPLACE_BE, pos, state);
	}

	public ItemStack fuel() {
		return items.get(0);
	}

	public static int burnTicks(ItemStack s) {
		return s.is(ItemTags.LOGS_THAT_BURN) ? LOG_TICKS : s.is(net.minecraft.world.item.Items.STICK) ? STICK_TICKS : 0;
	}

	/** Põe lenha da pilha (até encher o espaço); devolve quantas entraram. */
	public int addFuel(ItemStack stack) {
		if (!FireplaceBlock.isFuel(stack)) return 0;
		ItemStack in = fuel();
		if (in.isEmpty()) {
			int n = Math.min(stack.getCount(), stack.getMaxStackSize());
			items.set(0, stack.copyWithCount(n));
			setChanged();
			return n;
		}
		if (!ItemStack.isSameItemSameComponents(in, stack)) return 0;
		int n = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
		in.grow(n);
		setChanged();
		return n;
	}

	/** Acende (se houver lenha): começa a arder um pedaço. */
	public boolean light() {
		if (burn <= 0 && !consumeOne()) return false;
		if (level != null) level.setBlock(worldPosition, getBlockState().setValue(FireplaceBlock.LIT, true), Block.UPDATE_ALL);
		return true;
	}

	public void extinguish() {
		if (level != null) level.setBlock(worldPosition, getBlockState().setValue(FireplaceBlock.LIT, false), Block.UPDATE_ALL);
	}

	private boolean consumeOne() {
		ItemStack f = fuel();
		int t = burnTicks(f);
		if (t <= 0) return false;
		f.shrink(1);
		burn = t;
		burns++;
		setChanged();
		return true;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FireplaceBlockEntity be) {
		if (!state.getValue(FireplaceBlock.LIT)) return;
		if (--be.burn <= 0 && !be.consumeOne()) { // acabou a lenha
			be.burn = 0;
			be.extinguish();
			return;
		}
		long t = level.getGameTime();
		if (t % 10 == 0) be.burnTouching((ServerLevel) level, pos, state);
		if (t % 20 == 0 && ClimaBridge.zone(level, pos, WARM_C, WARM_RADIUS, WARM_TTL)) be.zonesSent++;
	}

	/** ⚠ Queima quem encosta à boca do fogo (um palmo à frente da lareira), como a fogueira. */
	void burnTouching(ServerLevel level, BlockPos pos, BlockState state) {
		Direction f = state.getValue(FireplaceBlock.FACING);
		AABB box = new AABB(pos).deflate(0.2, 0.15, 0.2).move(f.getStepX() * 0.55, 0, f.getStepZ() * 0.55);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> !e.fireImmune()))
			e.hurtServer(level, level.damageSources().campfire(), 1f);
	}

	// ---------------------------------------------------------------- inventário (só lenha; funis põem, não tiram)

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return FireplaceBlock.isFuel(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return FireplaceBlock.isFuel(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return null;
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, items);
		out.putInt("burn", burn);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(in, items);
		burn = in.getIntOr("burn", 0);
	}
}
