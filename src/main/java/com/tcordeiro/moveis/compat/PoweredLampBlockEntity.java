package com.tcordeiro.moveis.compat;

import com.tcordeiro.industrial.energy.EnergyNode;
import com.tcordeiro.industrial.logic.EnergyBuffer;
import com.tcordeiro.moveis.block.entity.LampBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Lâmpada com o Industrial instalado: um consumidor ({@link EnergyNode.Role#CONSUMER}) da API pública dele. Os cabos e o Fio de
 * Cobre ligam-se sozinhos a qualquer entidade que seja {@link EnergyNode} (flange) e os painéis, baterias e geradores empurram
 * energia para os consumidores pelos cabos ou encostados — nada a mudar no Industrial. Gasta {@link #COST} J/t só quando não há
 * redstone a acendê-la. Guarda um pouco (2 s) para não piscar enquanto o painel espera entre envios.
 * <p>
 * Esta classe só é carregada quando o Industrial existe ({@code TMoveis.INDUSTRIAL}).
 */
public class PoweredLampBlockEntity extends LampBlockEntity implements EnergyNode {
	public static final long CAPACITY = 40, MAX_IN = 8;
	private final EnergyBuffer buffer = new EnergyBuffer(CAPACITY, MAX_IN, 0);
	private boolean energized;

	public PoweredLampBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	public static LampBlockEntity create(BlockPos pos, BlockState state) {
		return new PoweredLampBlockEntity(pos, state);
	}

	@Override
	public Role energyRole() {
		return Role.CONSUMER;
	}

	@Override
	public EnergyBuffer energy() {
		return buffer;
	}

	@Override
	public void onReceived(long amount) {
		setChanged();
	}

	@Override
	public boolean energized() {
		return energized;
	}

	@Override
	protected void tickEnergy(Level level, BlockPos pos, BlockState state) {
		buffer.newTick();
		// com redstone já está acesa: não gasta
		energized = !level.hasNeighborSignal(pos) && buffer.consume(COST);
		if (energized) setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		buffer.setStored(in.getLongOr("energy", 0));
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putLong("energy", buffer.stored());
	}
}
