package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.block.WaterFurniture;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A água da pia (1 balde) ou da banheira (3 baldes). Só aceita água. */
public class WaterBlockEntity extends BlockEntity {
	public final SingleFluidStorage tank = new SingleFluidStorage() {
		@Override
		protected long getCapacity(FluidVariant variant) {
			return getBlockState().getBlock() instanceof WaterFurniture w ? w.capacity() : FluidConstants.BUCKET;
		}

		/** Só água: a lava (e o resto) é recusada pela Transfer API (insert devolve 0). */
		@Override
		protected boolean canInsert(FluidVariant variant) {
			return variant.isOf(Fluids.WATER);
		}

		@Override
		protected void onFinalCommit() {
			changed();
		}
	};

	public WaterBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.WATER_BE, pos, state);
	}

	public void changed() {
		setChanged();
		if (level != null && !level.isClientSide() && getBlockState().getBlock() instanceof WaterFurniture w)
			w.setWater(level, worldPosition, getBlockState(), tank.amount > 0);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (tank.amount > 0) tank.writeValue(out.child("tank"));
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		tank.variant = FluidVariant.blank();
		tank.amount = 0;
		in.child("tank").ifPresent(tank::readValue);
		if (tank.amount <= 0) tank.variant = FluidVariant.blank();
	}
}
