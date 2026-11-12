package com.tcordeiro.moveis.block;

import java.util.Map;

import com.tcordeiro.moveis.anim.Local;
import com.tcordeiro.moveis.anim.PianoMath;
import com.tcordeiro.moveis.block.entity.PianoBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Piano: 2 blocos de largura (SIDE = metade esquerda/direita do piano, visto do banco; o teclado virado para FACING). Cada tecla toca
 * uma nota do bloco musical com o som «harpa» (o do piano do jogo): a posição da tecla é a altura da nota, graves à esquerda
 * ({@link PianoMath}). A tecla afunda (renderer); enquanto há teclas a mexer, PLAYING nas duas metades.
 */
public class PianoBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<BedSide> SIDE = EnumProperty.create("side", BedSide.class);
	public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
	/** Ticks que PLAYING fica depois da última tecla (a tecla demora 5 a voltar). */
	public static final int PLAY_TICKS = 6;
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 0, 8, 16, 23, 16), Block.box(0, 11, 1, 16, 15, 9)));

	public PianoBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIDE, BedSide.RIGHT).setValue(PLAYING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE, PLAYING);
	}

	/** A metade esquerda do piano (x 0–16 do desenho) está à esquerda de FACING; a direita (x 16–32, os graves), à direita. */
	public static BlockPos other(BlockPos pos, BlockState state) {
		Direction f = state.getValue(FACING);
		return state.getValue(SIDE) == BedSide.RIGHT ? pos.relative(f.getCounterClockWise()) : pos.relative(f.getClockWise());
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction f = ctx.getHorizontalDirection().getOpposite();
		BlockPos left = ctx.getClickedPos().relative(f.getCounterClockWise());
		if (!ctx.getLevel().getBlockState(left).canBeReplaced(ctx) || !ctx.getLevel().getWorldBorder().isWithinBounds(left)) return null;
		return defaultBlockState().setValue(FACING, f).setValue(SIDE, BedSide.RIGHT);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		if (!level.isClientSide()) level.setBlock(other(pos, state), state.setValue(SIDE, BedSide.LEFT), Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		Direction f = state.getValue(FACING);
		Direction toOther = state.getValue(SIDE) == BedSide.RIGHT ? f.getCounterClockWise() : f.getClockWise();
		if (dir == toOther && !(nstate.is(this) && nstate.getValue(SIDE) != state.getValue(SIDE) && nstate.getValue(FACING) == f))
			return Blocks.AIR.defaultBlockState();
		return state;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(SIDE) == BedSide.LEFT) {
			BlockPos o = other(pos, state);
			BlockState os = level.getBlockState(o);
			if (os.is(this)) {
				level.setBlock(o, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, o, Block.getId(os));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** A tecla neste ponto do mundo (−1 = fora do teclado). x do desenho: metade esquerda 0–16, direita 16–32. */
	public static int keyAt(BlockState state, BlockPos pos, Vec3 hit) {
		int q = WineRackBlock.quarter(state.getValue(FACING));
		double fx = hit.x - pos.getX(), fz = hit.z - pos.getZ();
		float x = (float) (Local.modelX(q, fx, fz) * 16 + (state.getValue(SIDE) == BedSide.RIGHT ? 16 : 0));
		float z = (float) (Local.modelZ(q, fx, fz) * 16);
		return PianoMath.keyAt(x, (float) ((hit.y - pos.getY()) * 16), z);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		int key = keyAt(state, pos, hit.getLocation());
		if (key < 0) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		play(level, pos, state, key, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Toca a tecla: som, partícula, vibração (como o bloco musical) e a tecla a afundar nas duas metades. */
	public static void play(Level level, BlockPos pos, BlockState state, int key, Player player) {
		int note = PianoMath.note(key);
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, 3.0f, PianoMath.pitch(note));
		if (level instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0, note / 24.0, 0, 0, 1);
		level.gameEvent(player, GameEvent.NOTE_BLOCK_PLAY, pos);
		for (BlockPos p : new BlockPos[] {pos, other(pos, state)}) {
			BlockState s = level.getBlockState(p);
			if (!s.is(state.getBlock())) continue;
			if (level.getBlockEntity(p) instanceof PianoBlockEntity be) be.pressed(key, note, level.getGameTime() + PLAY_TICKS);
			if (!s.getValue(PLAYING)) level.setBlock(p, s.setValue(PLAYING, true), Block.UPDATE_CLIENTS);
			level.blockEvent(p, s.getBlock(), PianoBlockEntity.EVENT_KEY, key);
			level.scheduleTick(p, s.getBlock(), PLAY_TICKS);
		}
	}

	/** Acabaram as teclas a mexer: volta o modelo estático. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(PLAYING) || !(level.getBlockEntity(pos) instanceof PianoBlockEntity be)) return;
		long left = be.playUntil - level.getGameTime();
		if (left > 0) level.scheduleTick(pos, this, (int) left);
		else level.setBlock(pos, state.setValue(PLAYING, false), Block.UPDATE_CLIENTS);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PianoBlockEntity(pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
