package com.tcordeiro.moveis.block;

import java.util.Map;

import com.tcordeiro.moveis.block.entity.FireplaceBlockEntity;
import com.tcordeiro.moveis.compat.ClimaBridge;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lareira: põe-se lenha (paus ou troncos; também por funil) e acende-se com a pederneira ou o isqueiro do Clima (que é uma
 * pederneira). Gasta a lenha (tronco 1 min, pau 20 s) e apaga sem ela; a pá apaga. Acesa dá luz 15, queima quem encosta à boca do
 * fogo (como a fogueira) e, com o mod Clima, aquece a sala ({@link ClimaBridge#zone}).
 */
public class FireplaceBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 0, 10, 16, 16, 16),
			Block.box(0, 0, 0, 3, 13, 10), Block.box(13, 0, 0, 16, 13, 10), Block.box(0, 13, 0, 16, 16, 10), Block.box(3, 0, 0, 13, 1, 10)));

	public FireplaceBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING));
	}

	public static boolean isFuel(ItemStack s) {
		return s.is(ItemTags.LOGS_THAT_BURN) || s.is(net.minecraft.world.item.Items.STICK);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FireplaceBlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (stack.getItem() instanceof FlintAndSteelItem) { // pederneira, e o isqueiro do Clima (é uma)
			if (state.getValue(LIT)) return InteractionResult.PASS;
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			if (!be.light()) {
				player.sendOverlayMessage(Component.translatable("message.tmoveis.fireplace_no_fuel"));
				return InteractionResult.FAIL;
			}
			level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, level.getRandom().nextFloat() * 0.4f + 0.8f);
			ClimaBridge.lighterOpened(stack, level, pos);
			stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (stack.is(ItemTags.SHOVELS) && state.getValue(LIT)) {
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			be.extinguish();
			level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1f, 1f);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (isFuel(stack)) {
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			int moved = be.addFuel(stack);
			if (moved == 0) return InteractionResult.FAIL;
			if (!player.getAbilities().instabuild) stack.shrink(moved);
			player.sendOverlayMessage(Component.translatable("message.tmoveis.fireplace_fuel", be.fuel().getCount()));
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 1f);
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FireplaceBlockEntity be)
			player.sendOverlayMessage(Component.translatable("message.tmoveis.fireplace_fuel", be.fuel().getCount()));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		Direction f = state.getValue(FACING);
		double x = pos.getX() + 0.5 + f.getStepX() * 0.15, z = pos.getZ() + 0.5 + f.getStepZ() * 0.15;
		if (random.nextInt(10) == 0)
			level.playLocalSound(x, pos.getY() + 0.3, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
		level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.2, z + (random.nextDouble() - 0.5) * 0.2, 0, 0.01, 0);
		if (random.nextInt(3) == 0)
			level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.7, z, 0, 0.03, 0);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FireplaceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, MoveisBlocks.FIREPLACE_BE, FireplaceBlockEntity::serverTick);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
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
