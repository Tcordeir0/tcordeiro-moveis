package com.tcordeiro.moveis.block;

import java.util.Map;
import java.util.OptionalDouble;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cama de casal: 2 de largura × 2 de comprimento. Por dentro são DUAS camas vanilla lado a lado (cada coluna LEFT/RIGHT tem pés e
 * cabeceira, PART), coladas: por isso dorme-se como numa cama normal (passa a noite, marca o ponto de renascer, rebenta no Nether e
 * no Fim — tudo pelas regras do jogo, {@link AbstractBedBlock}) e cabem duas pessoas, uma em cada lado. Partir qualquer bloco parte
 * os quatro; só a cabeceira esquerda dá o item.
 */
public class DoubleBedBlock extends AbstractBedBlock {
	public static final EnumProperty<BedSide> SIDE = EnumProperty.create("side", BedSide.class);
	/** Altura onde se dorme (a do colchão, como as camas vanilla), mesmo com a cabeceira mais alta. */
	public static final double SLEEP_HEIGHT = 9 / 16.0;

	public final DyeColor color;

	/** Desenhadas com a cabeceira em z alto (a sul) e entregues a rotateHorizontal como «norte»: ver getShape. */
	private static final VoxelShape MATTRESS = Block.box(0, 3, 0, 16, 9, 16);
	private static final Map<Direction, VoxelShape> FOOT = Shapes.rotateHorizontal(Shapes.or(MATTRESS, Block.box(0, 0, 0, 16, 3, 3)));
	private static final Map<Direction, VoxelShape> HEAD = Shapes.rotateHorizontal(Shapes.or(MATTRESS, Block.box(0, 0, 14, 16, 16, 16)));

	public DoubleBedBlock(DyeColor color, Properties props) {
		super(props);
		this.color = color;
		registerDefaultState(defaultBlockState().setValue(SIDE, BedSide.LEFT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SIDE);
	}

	@Override
	protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
		return EnvironmentAttributes.BED_RULE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		// as formas são desenhadas viradas a NORTE em rotateHorizontal: FACING sul = cabeceira a sul ⇒ usar a direção oposta
		Direction f = state.getValue(FACING);
		return (state.getValue(PART) == BedPart.HEAD ? HEAD : FOOT).get(f.getOpposite());
	}

	@Override
	public OptionalDouble getSleepHeight(BlockState state, Level level, BlockPos pos) {
		return state.is(this) ? OptionalDouble.of(SLEEP_HEIGHT) : OptionalDouble.empty();
	}

	// ---------------------------------------------------------------- as 4 partes

	/** Para que lado está a outra coluna. */
	public static Direction sideDirection(BlockState state) {
		Direction f = state.getValue(FACING);
		return state.getValue(SIDE) == BedSide.LEFT ? f.getClockWise() : f.getCounterClockWise();
	}

	/** As posições das 4 partes, a partir de qualquer uma: [pés esq., cabeceira esq., pés dir., cabeceira dir.]. */
	public static BlockPos[] parts(BlockPos pos, BlockState state) {
		Direction f = state.getValue(FACING);
		BlockPos foot = state.getValue(PART) == BedPart.HEAD ? pos.relative(f.getOpposite()) : pos;
		BlockPos footLeft = state.getValue(SIDE) == BedSide.LEFT ? foot : foot.relative(f.getCounterClockWise());
		BlockPos footRight = footLeft.relative(f.getClockWise());
		return new BlockPos[] {footLeft, footLeft.relative(f), footRight, footRight.relative(f)};
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction f = ctx.getHorizontalDirection();
		BlockPos pos = ctx.getClickedPos();
		Level level = ctx.getLevel();
		BlockPos right = pos.relative(f.getClockWise());
		for (BlockPos p : new BlockPos[] {pos.relative(f), right, right.relative(f)})
			if (!level.getBlockState(p).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(p)) return null;
		return defaultBlockState().setValue(FACING, f).setValue(PART, BedPart.FOOT).setValue(SIDE, BedSide.LEFT);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
		// não chama o da cama vanilla (que só põe uma cabeceira): põe as outras 3 partes, por esta ordem para nenhuma se desfazer
		if (level.isClientSide()) return;
		BlockPos[] p = parts(pos, state);
		level.setBlock(p[1], state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		level.setBlock(p[2], state.setValue(SIDE, BedSide.RIGHT), Block.UPDATE_ALL);
		level.setBlock(p[3], state.setValue(SIDE, BedSide.RIGHT).setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		level.updateNeighborsAt(pos, this);
		state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		if (dir == sideDirection(state)) {
			// a outra coluna tem de ser esta cama, a mesma parte, o outro lado
			boolean ok = nstate.is(this) && nstate.getValue(PART) == state.getValue(PART) && nstate.getValue(SIDE) == state.getValue(SIDE).other()
					&& nstate.getValue(FACING) == state.getValue(FACING);
			return ok ? state : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, dir, npos, nstate, random);
	}

	/** Criativo: parte as 4 sem dar o item (a cama vanilla faz o mesmo com a cabeceira). */
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

	/** No Nether e no Fim: rebenta como as camas vanilla (tira as 4 partes primeiro, para não dar o item). */
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
		return net.minecraft.util.Mth.getSeed(p.getX(), pos.getY(), p.getZ());
	}
}
