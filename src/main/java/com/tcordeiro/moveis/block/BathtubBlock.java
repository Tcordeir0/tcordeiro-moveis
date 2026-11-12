package com.tcordeiro.moveis.block;

import java.util.Map;

import com.tcordeiro.moveis.block.entity.WaterBlockEntity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Banheira: 2 blocos (pés → cabeceira = FACING; as torneiras na cabeceira). Guarda 3 baldes de água na cabeceira (balde, frasco ou
 * cano, pelas duas metades). Quem entra na água deixa de arder (como no caldeirão com água).
 */
public class BathtubBlock extends BaseEntityBlock implements WaterFurniture {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
	/** Cabeceira virada a norte (a parede de fora a z = 0); os pés, o contrário. */
	private static final Map<Direction, VoxelShape> HEAD = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 0, 0, 16, 2, 16),
			Block.box(0, 2, 0, 16, 10, 1.5), Block.box(0, 2, 0, 1.5, 10, 16), Block.box(14.5, 2, 0, 16, 10, 16)));
	private static final Map<Direction, VoxelShape> FOOT = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 0, 0, 16, 2, 16),
			Block.box(0, 2, 14.5, 16, 10, 16), Block.box(0, 2, 0, 1.5, 10, 16), Block.box(14.5, 2, 0, 16, 10, 16)));

	public BathtubBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT).setValue(WATER, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART, WATER);
	}

	@Override
	public long capacity() {
		return 3 * FluidConstants.BUCKET;
	}

	/** A outra metade. */
	public static BlockPos other(BlockPos pos, BlockState state) {
		Direction f = state.getValue(FACING);
		return state.getValue(PART) == BedPart.HEAD ? pos.relative(f.getOpposite()) : pos.relative(f);
	}

	@Override
	public BlockPos storagePos(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.HEAD ? pos : other(pos, state);
	}

	@Override
	public void setWater(Level level, BlockPos pos, BlockState state, boolean water) {
		for (BlockPos p : new BlockPos[] {pos, other(pos, state)}) {
			BlockState s = level.getBlockState(p);
			if (s.is(this) && s.getValue(WATER) != water) level.setBlock(p, s.setValue(WATER, water), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction f = ctx.getHorizontalDirection();
		BlockPos head = ctx.getClickedPos().relative(f);
		Level level = ctx.getLevel();
		if (!level.getBlockState(head).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(head)) return null;
		return defaultBlockState().setValue(FACING, f).setValue(PART, BedPart.FOOT);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		if (!level.isClientSide()) level.setBlock(other(pos, state), state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		Direction f = state.getValue(FACING);
		if (dir == (state.getValue(PART) == BedPart.HEAD ? f.getOpposite() : f)) {
			boolean ok = nstate.is(this) && nstate.getValue(PART) != state.getValue(PART) && nstate.getValue(FACING) == f;
			return ok ? state.setValue(WATER, nstate.getValue(WATER)) : Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(PART) == BedPart.HEAD) {
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
		return (state.getValue(PART) == BedPart.HEAD ? HEAD : FOOT).get(state.getValue(FACING));
	}

	/** Com água, apaga o fogo de quem lá está dentro. */
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
		if (state.getValue(WATER) && entity.getY() < pos.getY() + 0.7) effects.apply(InsideBlockEffectType.EXTINGUISH);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return WaterFurniture.use(stack, state, level, pos, player, hand);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		WaterBlockEntity be = WaterFurniture.storage(level, pos, state);
		if (be != null && !level.isClientSide()) player.sendOverlayMessage(WaterFurniture.status(be));
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.HEAD ? new WaterBlockEntity(pos, state) : null;
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
