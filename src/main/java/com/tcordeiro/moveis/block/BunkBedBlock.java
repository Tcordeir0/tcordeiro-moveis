package com.tcordeiro.moveis.block;

import java.util.Map;
import java.util.OptionalDouble;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Beliche: 4 blocos (2 de altura × 2 de comprimento). Por dentro são DUAS camas vanilla empilhadas ({@link AbstractBedBlock}: cada
 * andar, HALF, tem pés e cabeceira, PART): cada uma dorme um jogador como a cama normal (passa a noite, marca o renascer, rebenta no
 * Nether e no Fim). O desenho tem a cabeceira a norte = FACING (pés → cabeceira). Partir um bloco parte os quatro; só os pés de baixo
 * dão o item.
 */
public class BunkBedBlock extends AbstractBedBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/** Onde se dorme (o colchão de cada andar). */
	public static final double SLEEP_LOWER = 7 / 16.0, SLEEP_UPPER = 6 / 16.0;

	public final DyeColor color;

	private static final Map<Direction, VoxelShape> LOWER_HEAD = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 2, 0, 16, 7, 16),
			Block.box(0, 0, 0, 2, 16, 2), Block.box(14, 0, 0, 16, 16, 2)));
	private static final Map<Direction, VoxelShape> LOWER_FOOT = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 2, 0, 16, 7, 16),
			Block.box(0, 0, 14, 2, 16, 16), Block.box(14, 0, 14, 16, 16, 16)));
	private static final Map<Direction, VoxelShape> UPPER_HEAD = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 1, 0, 16, 6, 16),
			Block.box(0, 0, 0, 2, 14, 2), Block.box(14, 0, 0, 16, 14, 2)));
	private static final Map<Direction, VoxelShape> UPPER_FOOT = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 1, 0, 16, 6, 16),
			Block.box(0, 0, 14, 2, 14, 16), Block.box(14, 0, 14, 16, 14, 16)));

	public BunkBedBlock(DyeColor color, Properties props) {
		super(props);
		this.color = color;
		registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HALF);
	}

	@Override
	protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
		return EnvironmentAttributes.BED_RULE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		boolean head = state.getValue(PART) == BedPart.HEAD;
		Map<Direction, VoxelShape> m = state.getValue(HALF) == DoubleBlockHalf.LOWER ? (head ? LOWER_HEAD : LOWER_FOOT) : (head ? UPPER_HEAD : UPPER_FOOT);
		return m.get(state.getValue(FACING));
	}

	@Override
	public OptionalDouble getSleepHeight(BlockState state, Level level, BlockPos pos) {
		if (!state.is(this)) return OptionalDouble.empty();
		return OptionalDouble.of(state.getValue(HALF) == DoubleBlockHalf.LOWER ? SLEEP_LOWER : SLEEP_UPPER);
	}

	/** As 4 posições a partir de qualquer uma: [pés de baixo, cabeceira de baixo, pés de cima, cabeceira de cima]. */
	public static BlockPos[] parts(BlockPos pos, BlockState state) {
		Direction f = state.getValue(FACING);
		BlockPos foot = state.getValue(PART) == BedPart.HEAD ? pos.relative(f.getOpposite()) : pos;
		BlockPos footLow = state.getValue(HALF) == DoubleBlockHalf.LOWER ? foot : foot.below();
		return new BlockPos[] {footLow, footLow.relative(f), footLow.above(), footLow.relative(f).above()};
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction f = ctx.getHorizontalDirection();
		BlockPos pos = ctx.getClickedPos();
		Level level = ctx.getLevel();
		if (pos.getY() + 1 > level.getMaxY()) return null;
		for (BlockPos p : new BlockPos[] {pos.relative(f), pos.above(), pos.relative(f).above()})
			if (!level.getBlockState(p).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(p)) return null;
		return defaultBlockState().setValue(FACING, f).setValue(PART, BedPart.FOOT).setValue(HALF, DoubleBlockHalf.LOWER);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		if (level.isClientSide()) return;
		BlockPos[] p = parts(pos, state);
		level.setBlock(p[1], state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		level.setBlock(p[2], state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		level.setBlock(p[3], state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		level.updateNeighborsAt(pos, this);
		state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		DoubleBlockHalf half = state.getValue(HALF);
		if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
			boolean ok = nstate.is(this) && nstate.getValue(HALF) != half && nstate.getValue(PART) == state.getValue(PART)
					&& nstate.getValue(FACING) == state.getValue(FACING);
			return ok ? state : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, dir, npos, nstate, random);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.preventsBlockDrops()) {
			for (BlockPos p : parts(pos, state)) {
				if (p.equals(pos)) continue;
				BlockState s = level.getBlockState(p);
				if (s.is(this)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
					level.levelEvent(player, 2001, p, Block.getId(s));
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected InteractionResult destroyOnUse(BlockState state, Level level, BlockPos pos, Player player) {
		for (BlockPos p : parts(pos, state)) if (level.getBlockState(p).is(this)) level.removeBlock(p, false);
		Vec3 boom = Vec3.atCenterOf(pos);
		level.explode(null, level.damageSources().badRespawnPointExplosion(boom), null, boom, 5.0F, true, Level.ExplosionInteraction.BLOCK);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void destroyOnLeave(Level level, BlockPos pos) {}

	@Override
	protected long getSeed(BlockState state, BlockPos pos) {
		BlockPos p = parts(pos, state)[0];
		return Mth.getSeed(p.getX(), p.getY(), p.getZ());
	}
}
