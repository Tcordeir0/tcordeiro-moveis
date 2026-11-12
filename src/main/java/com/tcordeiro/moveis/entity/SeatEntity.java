package com.tcordeiro.moveis.entity;

import com.tcordeiro.moveis.block.SeatBlock;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Assento invisível (como nos mods de móveis): o jogador «monta» esta entidade, posta à altura do assento da cadeira, banco ou
 * sofá. Sai com Shift (o desmontar normal do jogo). Desaparece sozinha quando fica vazia ou quando o bloco deixa de ser um assento
 * (partido, explodido, empurrado). Tem de poder ser gravada (o jogo só deixa montar entidades graváveis); se ficar gravada vazia,
 * apaga-se no primeiro tick ao carregar o mundo.
 * <p>
 * 0.3.1: quem está sentado tem o corpo virado para a frente do móvel (o yaw desta entidade) e a cabeça só roda até
 * {@link #TURN_LIMIT} para cada lado, como no barco. Corre nos dois lados: no cliente (onPassengerTurned, quando o rato mexe) e
 * em cada tick (positionRider).
 */
public class SeatEntity extends Entity {
	/** Quanto a cabeça de quem está sentado pode rodar para cada lado da frente do móvel, em graus. */
	public static final float TURN_LIMIT = 90f;

	public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Um assento no bloco {@code pos}, com o jogador a {@code height} (em blocos) acima do chão do bloco. */
	public static SeatEntity create(Level level, BlockPos pos, double height, float yaw) {
		SeatEntity seat = new SeatEntity(MoveisBlocks.SEAT, level);
		seat.setPos(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5);
		seat.setYRot(yaw);
		return seat;
	}

	/** Há alguém sentado neste bloco? */
	public static boolean occupied(Level level, BlockPos pos) {
		return !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos), s -> !s.getPassengers().isEmpty()).isEmpty();
	}

	/** Tira toda a gente do bloco e apaga os assentos (o bloco foi partido). */
	public static void removeAt(Level level, BlockPos pos) {
		for (SeatEntity s : level.getEntitiesOfClass(SeatEntity.class, new AABB(pos))) {
			s.ejectPassengers();
			s.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		if (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof SeatBlock)) {
			ejectPassengers();
			discard();
		}
	}

	/** O jogador fica com o rabo à altura da entidade (a posição dela é o assento). */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return Vec3.ZERO;
	}

	/** O yaw {@code yaw} limitado a ±{@link #TURN_LIMIT} em volta de {@code seatYaw} (mantém as voltas inteiras do yaw original). */
	public static float clampYaw(float seatYaw, float yaw) {
		float d = Mth.wrapDegrees(yaw - seatYaw);
		return yaw + Mth.clamp(d, -TURN_LIMIT, TURN_LIMIT) - d;
	}

	/** Corpo para a frente do móvel; cabeça (e câmara) dentro do limite. */
	protected void clampRotation(Entity passenger) {
		passenger.setYBodyRot(getYRot());
		float y = clampYaw(getYRot(), passenger.getYRot());
		passenger.yRotO += y - passenger.getYRot();
		passenger.setYRot(y);
		passenger.setYHeadRot(y);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		super.positionRider(passenger, moveFunction);
		clampRotation(passenger);
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		clampRotation(passenger);
	}

	/** Ao levantar: ao lado do móvel, num sítio seguro; se não houver, em cima dele. */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		BlockPos pos = blockPosition();
		Direction front = Direction.fromYRot(getYRot());
		for (Direction d : new Direction[] {front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
			Vec3 v = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), pos.relative(d), true);
			if (v != null) return v;
		}
		return new Vec3(getX(), pos.getY() + 1.0, getZ());
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}
}
