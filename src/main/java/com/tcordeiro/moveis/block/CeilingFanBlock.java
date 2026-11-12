package com.tcordeiro.moveis.block;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;
import com.tcordeiro.moveis.compat.ClimaBridge;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ventilador de teto (com luz): gira com redstone — a velocidade é a força do sinal ({@link #POWER}) — ou, com o Industrial, com
 * energia (velocidade máxima, como as lâmpadas: 1 J/t). Ao desligar trava e para com as pás no sítio do modelo (MOVING enquanto
 * trava). Com o mod Clima, a girar no verão refresca a sala ({@link ClimaBridge}).
 */
public class CeilingFanBlock extends LampBlock {
	public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
	/** Refresco do Clima: alvo, raio e duração de cada renovação (a cada segundo). */
	public static final double COOL_C = 18;
	public static final int COOL_RADIUS = 5, COOL_TTL = 40;
	private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 8, 0, 16, 12, 16), Block.box(7, 12, 7, 9, 16, 9));

	public CeilingFanBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(POWER, 0).setValue(AnimatedFurniture.MOVING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POWER, AnimatedFurniture.MOVING);
	}

	@Override
	protected BlockState powered(Level level, BlockPos pos, BlockState state, int signal, boolean energy) {
		int power = energy ? 15 : signal;
		boolean was = state.getValue(LIT);
		BlockState s = state.setValue(POWER, power).setValue(LIT, power > 0);
		if (was && power == 0) s = s.setValue(AnimatedFurniture.MOVING, true); // trava a girar
		return s;
	}

	/** Velocidade pedida (0–1). */
	public static float speed(BlockState state) {
		return state.getValue(POWER) / 15f;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState st = placed(ctx, defaultBlockState());
		return st.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? st : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos,
			BlockState nstate, RandomSource random) {
		return dir == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return LampBlockEntity.create(MoveisBlocks.FAN_BE, pos, state);
	}

	/** Cliente: as pás. Servidor: energia (Industrial), fim da travagem e o refresco (Clima). */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, MoveisBlocks.FAN_BE, level.isClientSide() ? CeilingFanBlock::clientTick : CeilingFanBlock::serverTick);
	}

	static void clientTick(Level level, BlockPos pos, BlockState state, LampBlockEntity be) {
		be.spin.tick(speed(state));
	}

	static void serverTick(Level level, BlockPos pos, BlockState state, LampBlockEntity be) {
		if (TMoveis.INDUSTRIAL) LampBlockEntity.serverTick(level, pos, state, be);
		BlockState now = level.getBlockState(pos);
		if (!now.is(state.getBlock())) return;
		be.spin.tick(speed(now));
		if (now.getValue(AnimatedFurniture.MOVING) && be.spin.stopped()) level.setBlock(pos, now.setValue(AnimatedFurniture.MOVING, false), Block.UPDATE_CLIENTS);
		if (now.getValue(LIT) && level.getGameTime() % 20 == 0 && "summer".equals(ClimaBridge.seasonId(level)))
			ClimaBridge.zone(level, pos, COOL_C, COOL_RADIUS, COOL_TTL);
	}
}
