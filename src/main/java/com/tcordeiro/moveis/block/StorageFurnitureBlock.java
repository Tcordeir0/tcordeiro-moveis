package com.tcordeiro.moveis.block;

import java.util.Map;

import com.tcordeiro.moveis.block.entity.StorageFurnitureBlockEntity;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Móveis com inventário (armário, gaveteiro, bancada, mesa de cabeceira, escrivaninha, guarda-roupa, cofre, lixeira, caixa de correio).
 * A frente vira para o jogador. OPEN enquanto alguém tem o inventário aberto (como o barril); as portas/gavetas/tampa abrem e fecham
 * com animação ({@link AnimatedFurniture}). Funis põem e tiram (o cofre não; a caixa de correio e a lixeira só recebem); o conteúdo
 * cai ao partir.
 */
public class StorageFurnitureBlock extends BaseEntityBlock implements AnimatedFurniture {
	/** Linhas do ecrã (9 espaços cada), o tipo de peça animada (pecas.ANIM) e o som. */
	public enum Kind {
		CABINET(3, "cabinet", Sound.DOOR), DRAWER(3, "drawer", Sound.BARREL), COUNTER(1, "counter", Sound.DOOR),
		NIGHTSTAND(1, "nightstand", Sound.BARREL), DESK(1, "desk", Sound.BARREL), WARDROBE(4, "wardrobe_bottom", Sound.DOOR),
		SAFE(3, "safe", Sound.IRON), TRASH(1, "trash_can", Sound.LID), MAILBOX(1, "mailbox", Sound.LID);

		public final int rows;
		public final String anim;
		final Sound sound;

		Kind(int rows, String anim, Sound sound) {
			this.rows = rows;
			this.anim = anim;
			this.sound = sound;
		}

		public int size() {
			return rows * 9;
		}

		/** Com dono: só ele tira e parte (cofre e caixa de correio, 0.3.3). */
		public boolean ownerOnly() {
			return this == SAFE || this == MAILBOX;
		}

		public SoundEvent sound(boolean open) {
			return switch (sound) {
				case DOOR -> open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE;
				case BARREL -> open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE;
				case IRON -> open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE;
				case LID -> open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE;
			};
		}
	}

	enum Sound { DOOR, BARREL, IRON, LID }

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Formas viradas a norte (as que não são o bloco inteiro). */
	private static final Map<Kind, Map<Direction, VoxelShape>> SHAPES = Map.of(
			Kind.NIGHTSTAND, Shapes.rotateHorizontal(Block.box(0.5, 0, 0.5, 15.5, 13, 15.5)),
			Kind.DESK, Shapes.rotateHorizontal(Shapes.or(Block.box(0, 14, 0, 16, 16, 16), Block.box(10, 0, 1, 15.5, 14, 15), Block.box(0.5, 0, 1, 2.5, 14, 15))),
			Kind.SAFE, Shapes.rotateHorizontal(Block.box(0.5, 0, 0.5, 15.5, 16, 15.5)),
			Kind.TRASH, Shapes.rotateHorizontal(Block.box(3.5, 0, 3.5, 12.5, 13, 12.5)),
			Kind.MAILBOX, Shapes.rotateHorizontal(Shapes.or(Block.box(7, 0, 7, 9, 10, 9), Block.box(4, 10, 3, 12, 16, 13))));

	public final Kind kind;

	public StorageFurnitureBlock(Kind kind, Properties props) {
		super(props);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN); // 0.3.3: sem MOVING (vem da entidade)
		com.tcordeiro.moveis.finish.FinishState.addIfWood(builder);
	}

	@Override
	public String animKind(BlockState state) {
		return kind.anim;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		Map<Direction, VoxelShape> m = SHAPES.get(kind);
		return m == null ? Shapes.block() : m.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageFurnitureBlockEntity be) player.openMenu(be);
		return InteractionResult.SUCCESS;
	}

	/** O inventário abriu (primeiro jogador) ou fechou (último): mexe as portas. O guarda-roupa mexe as duas metades. */
	public void onOpenChanged(Level level, BlockPos pos, BlockState state, boolean open) {
		AnimatedFurniture.setTarget(level, pos, state, OPEN, open);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof StorageFurnitureBlockEntity be) be.recheckOpen();
	}

	// ---------------------------------------------------------------- dono (cofre e caixa de correio, 0.3.3)

	/** O jogador é o dono (ou o móvel não tem dono)? */
	public static boolean isOwner(BlockGetter level, BlockPos pos, Player player) {
		return !(level.getBlockEntity(pos) instanceof StorageFurnitureBlockEntity be) || be.isOwner(player);
	}

	public static String ownerName(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof StorageFurnitureBlockEntity be ? be.ownerName : "";
	}

	/** Este estado só pode ser partido pelo dono? (o evento PlayerBlockBreakEvents.BEFORE em TMoveis usa isto) */
	public static boolean ownerOnly(BlockState state) {
		return state.getBlock() instanceof StorageFurnitureBlock b && b.kind.ownerOnly();
	}

	/** Quem não é o dono não consegue partir o cofre nem a caixa de correio (a rachadura não avança). */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return kind.ownerOnly() && !isOwner(level, pos, player) ? 0f : super.getDestroyProgress(state, player, level, pos);
	}

	/** Explosões nunca partem o cofre nem a caixa de correio (além da resistência alta, por garantia): o conteúdo não cai. */
	@Override
	protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.world.level.Explosion explosion,
			java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, BlockPos> drops) {
		if (!kind.ownerOnly()) super.onExplosionHit(state, level, pos, explosion, drops);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageFurnitureBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, MoveisBlocks.STORAGE_BE, StorageFurnitureBlockEntity::tick);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction dir) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// verniz e desgaste (0.3.0): ver com.tcordeiro.moveis.finish
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return com.tcordeiro.moveis.finish.Weathering.ticks(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		com.tcordeiro.moveis.finish.Weathering.randomTick(state, level, pos, random);
	}
}
