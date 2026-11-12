package com.tcordeiro.moveis.block;

import java.util.EnumMap;
import java.util.Map;

import com.tcordeiro.moveis.entity.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sofá (lã de 16 cores, estrutura de madeira): a frente vira para o jogador que o põe (o encosto fica do lado de lá). Emenda com os
 * sofás vizinhos (de qualquer cor): em fila perde os braços do lado emendado; em L faz canto (de dentro ou de fora), com a mesma
 * lógica das escadas. FACING = para onde se olha sentado.
 */
public class SofaBlock extends Block implements SeatBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<SofaShape> SHAPE = EnumProperty.create("shape", SofaShape.class);

	public final DyeColor color;

	/** Formas viradas a norte (encosto a sul = z alto; esquerda de quem se senta = oeste). */
	private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 8, 16), BACK = Block.box(0, 8, 12, 16, 15, 16),
			ARM_L = Block.box(0, 8, 0, 3, 11, 12), ARM_R = Block.box(13, 8, 0, 16, 11, 12), BACK_L = Block.box(0, 8, 0, 4, 15, 12),
			POST = Block.box(0, 8, 12, 4, 15, 16);
	private static final Map<SofaShape, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(SofaShape.class);

	static {
		SHAPES.put(SofaShape.SINGLE, Shapes.rotateHorizontal(Shapes.or(BASE, BACK, ARM_L, ARM_R)));
		SHAPES.put(SofaShape.LEFT, Shapes.rotateHorizontal(Shapes.or(BASE, BACK, ARM_L)));
		SHAPES.put(SofaShape.RIGHT, Shapes.rotateHorizontal(Shapes.or(BASE, BACK, ARM_R)));
		SHAPES.put(SofaShape.MIDDLE, Shapes.rotateHorizontal(Shapes.or(BASE, BACK)));
		// cantos: o modelo/forma base tem o encosto atrás e à esquerda (de dentro) ou o pilar atrás-esquerda (de fora); as variantes
		// «left» são a mesma forma rodada 90° para a esquerda (ver modelFacing)
		Map<Direction, VoxelShape> inner = Shapes.rotateHorizontal(Shapes.or(BASE, BACK, BACK_L));
		Map<Direction, VoxelShape> outer = Shapes.rotateHorizontal(Shapes.or(BASE, POST));
		SHAPES.put(SofaShape.INNER_LEFT, inner);
		SHAPES.put(SofaShape.INNER_RIGHT, inner);
		SHAPES.put(SofaShape.OUTER_LEFT, outer);
		SHAPES.put(SofaShape.OUTER_RIGHT, outer);
	}

	public SofaBlock(DyeColor color, Properties props) {
		super(props);
		this.color = color;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, SofaShape.SINGLE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE);
	}

	/**
	 * Para que lado está virado o modelo de canto desta forma: os cantos «left» usam o modelo de canto rodado 90° para a esquerda (o mesmo
	 * truque das escadas). Também serve à forma da colisão.
	 */
	public static Direction modelFacing(BlockState state) {
		Direction f = state.getValue(FACING);
		SofaShape s = state.getValue(SHAPE);
		return s == SofaShape.INNER_LEFT || s == SofaShape.OUTER_LEFT ? f.getCounterClockWise() : f;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(SHAPE)).get(modelFacing(state));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState st = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
		return st.setValue(SHAPE, shapeFor(st, ctx.getLevel(), ctx.getClickedPos()));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		return dir.getAxis().isHorizontal() ? state.setValue(SHAPE, shapeFor(state, level, pos)) : state;
	}

	private static boolean isSofa(BlockState s) {
		return s.getBlock() instanceof SofaBlock;
	}

	/**
	 * A forma pelos vizinhos. Cantos: como as escadas, com «costas» = o lado do encosto (oposto a FACING). Um sofá atrás, virado de
	 * lado, faz canto de fora; um sofá à frente, virado de lado, faz canto de dentro. Senão é reto e perde o braço de cada lado onde
	 * houver outro sofá.
	 */
	public static SofaShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
		Direction look = state.getValue(FACING), back = look.getOpposite();
		BlockState behind = level.getBlockState(pos.relative(back));
		if (isSofa(behind)) {
			Direction bBack = behind.getValue(FACING).getOpposite();
			if (bBack.getAxis() != back.getAxis() && canTakeShape(state, level, pos, bBack.getOpposite()))
				return bBack == back.getCounterClockWise() ? SofaShape.OUTER_LEFT : SofaShape.OUTER_RIGHT;
		}
		BlockState front = level.getBlockState(pos.relative(look));
		if (isSofa(front)) {
			Direction fBack = front.getValue(FACING).getOpposite();
			if (fBack.getAxis() != back.getAxis() && canTakeShape(state, level, pos, fBack))
				return fBack == back.getCounterClockWise() ? SofaShape.INNER_LEFT : SofaShape.INNER_RIGHT;
		}
		boolean left = isSofa(level.getBlockState(pos.relative(look.getCounterClockWise())));
		boolean right = isSofa(level.getBlockState(pos.relative(look.getClockWise())));
		if (left && right) return SofaShape.MIDDLE;
		if (left) return SofaShape.RIGHT; // emendado à esquerda: é a ponta direita (braço à direita)
		if (right) return SofaShape.LEFT;
		return SofaShape.SINGLE;
	}

	/** Como nas escadas: não faz canto se o vizinho desse lado for um sofá virado para o mesmo lado (seria uma fila). */
	private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		BlockState n = level.getBlockState(pos.relative(dir));
		return !isSofa(n) || n.getValue(FACING) != state.getValue(FACING);
	}

	@Override
	public double seatHeight(BlockState state) {
		return 0.5;
	}

	@Override
	public float seatYaw(BlockState state, Player player) {
		return seatYaw(state);
	}

	/**
	 * Para onde se olha sentado. Reto: FACING. Cantos: na diagonal, entre os dois lados abertos. O modelo de canto (virado para
	 * {@link #modelFacing}) tem o encosto atrás e à esquerda (de dentro) ou o pilar atrás-esquerda (de fora), por isso a diagonal
	 * livre é sempre a frente-direita dele: modelFacing + 45°. Dá o mesmo para os dois cantos de dentro e os dois de fora.
	 */
	public static float seatYaw(BlockState state) {
		if (!state.getValue(SHAPE).corner()) return state.getValue(FACING).toYRot();
		return Mth.wrapDegrees(modelFacing(state).toYRot() + 45f);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return SeatBlock.sit(this, state, level, pos, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		SeatEntity.removeAt(level, pos);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
