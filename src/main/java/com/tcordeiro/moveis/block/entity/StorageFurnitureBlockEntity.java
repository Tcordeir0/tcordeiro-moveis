package com.tcordeiro.moveis.block.entity;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.tcordeiro.moveis.anim.Anim;
import com.tcordeiro.moveis.block.AnimatedFurniture;
import com.tcordeiro.moveis.block.StorageFurnitureBlock;
import com.tcordeiro.moveis.block.StorageFurnitureBlock.Kind;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O inventário dos móveis de guardar: 9, 27 ou 36 espaços (os ecrãs de 1, 3 e 4 linhas do baú, com funis e comparadores). Guarda a
 * animação das portas/gavetas ({@link Anim}) e, no cofre e na caixa de correio, o dono.
 */
public class StorageFurnitureBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, Animated {
	private NonNullList<ItemStack> items;
	private final Anim anim;
	private final int[] slots;
	/** Dono (cofre, caixa de correio); null = sem dono. */
	public UUID owner;
	public String ownerName = "";

	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, true);
			if (state.getBlock() instanceof StorageFurnitureBlock b) b.onOpenChanged(level, pos, state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, false);
			if (state.getBlock() instanceof StorageFurnitureBlock b) b.onOpenChanged(level, pos, state, false);
			if (kind() == Kind.TRASH) clearContent(); // ⚠ a lixeira apaga tudo ao fechar
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == StorageFurnitureBlockEntity.this;
		}
	};

	public StorageFurnitureBlockEntity(BlockPos pos, BlockState state) {
		this(MoveisBlocks.STORAGE_BE, pos, state);
	}

	protected StorageFurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		items = NonNullList.withSize(kindOf(state).size(), ItemStack.EMPTY);
		slots = IntStream.range(0, items.size()).toArray();
		anim = new Anim(AnimatedFurniture.initialTarget(state));
	}

	static Kind kindOf(BlockState state) {
		return state.getBlock() instanceof StorageFurnitureBlock b ? b.kind : Kind.CABINET;
	}

	public Kind kind() {
		return kindOf(getBlockState());
	}

	@Override
	public Anim anim() {
		return anim;
	}

	/** 0.3.3: o modelo do chunk (cliente) pergunta à entidade se as partes estão a mexer (já não há MOVING no blockstate). */
	@Override
	public Object getRenderData() {
		return getBlockState().getBlock() instanceof AnimatedFurniture af && af.moving(getBlockState(), anim);
	}

	public boolean isOwner(Player player) {
		return owner == null || owner.equals(player.getUUID());
	}

	/** Cofre e caixa de correio (0.3.3): só o dono tira, ninguém mais os parte, funis/canos/explosões não os esvaziam. */
	public boolean ownerOnly() {
		return kind().ownerOnly();
	}

	/**
	 * Quem pode abrir o inventário A SÉRIO (ver e tirar). No cofre e na caixa de correio, só o dono (0.3.3); a fechadura do jogo
	 * ({@code lock}) continua a valer para todos. É também o que o HUD do Receitário consulta antes de mostrar o conteúdo
	 * ({@code ProbePayload.mayInspect} → {@code canOpen}): quem não é o dono não vê o que lá está.
	 */
	@Override
	public boolean canOpen(Player player) {
		return (!ownerOnly() || isOwner(player)) && super.canOpen(player);
	}

	/** Só a fechadura do jogo (sem a regra do dono): a caixa de correio deixa os outros depositar. */
	protected boolean lockAllows(Player player) {
		return super.canOpen(player);
	}

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getName().getString();
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!trySaveLootTable(output)) ContainerHelper.saveAllItems(output, items);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
			output.putString("owner_name", ownerName);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		if (!tryLoadLootTable(input)) ContainerHelper.loadAllItems(input, items);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("owner_name", "");
	}

	@Override
	public int getContainerSize() {
		return slots.length;
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
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	public static MenuType<ChestMenu> menuType(int rows) {
		return switch (rows) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			case 6 -> MenuType.GENERIC_9x6;
			default -> MenuType.GENERIC_9x3;
		};
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		int rows = kind().rows;
		return new ChestMenu(menuType(rows), containerId, inventory, this, rows);
	}

	/** A lixeira apaga o que um funil lá põe quando ninguém a tem aberta (⚠ some para sempre). */
	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && !level.isClientSide() && kind() == Kind.TRASH && openers.getOpenerCount() == 0 && !isEmpty()) clearContent();
	}

	// ---------------------------------------------------------------- funis

	@Override
	public int[] getSlotsForFace(Direction side) {
		return kind() == Kind.SAFE ? new int[0] : slots;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return kind() != Kind.SAFE;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return takesOut(kind());
	}

	/** Funis, canos do Industrial e outros mods podem tirar deste tipo? (o cofre, a caixa de correio e a lixeira não) */
	public static boolean takesOut(Kind k) {
		return k != Kind.SAFE && k != Kind.MAILBOX && k != Kind.TRASH;
	}

	// ---------------------------------------------------------------- abrir / fechar

	@Override
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator())
			openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator())
			openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return openers.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
	}

	public int openerCount() {
		return openers.getOpenerCount();
	}

	public void recheckOpen() {
		if (!remove) openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageFurnitureBlockEntity be) {
		AnimatedFurniture.tickAnim(level, pos, state, be.anim);
	}

	private void playSound(BlockState state, boolean open) {
		if (level == null || !state.hasProperty(StorageFurnitureBlock.FACING)) return;
		Vec3i d = state.getValue(StorageFurnitureBlock.FACING).getUnitVec3i();
		level.playSound(null, worldPosition.getX() + 0.5 + d.getX() / 2.0, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5 + d.getZ() / 2.0,
				kind().sound(open), SoundSource.BLOCKS, 0.5f, level.getRandom().nextFloat() * 0.1f + 0.9f);
	}
}
