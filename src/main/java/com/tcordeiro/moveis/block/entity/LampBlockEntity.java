package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.TMoveis;
import com.tcordeiro.moveis.block.LampBlock;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Entidade das lâmpadas. Sozinha não faz nada (só redstone); com o Industrial o jogo cria a subclasse
 * {@code compat.PoweredLampBlockEntity}, que é um consumidor de energia da rede dele.
 */
public class LampBlockEntity extends BlockEntity {
	/** Consumo com energia: 1 J/t (o Fio de Cobre leva 32 J/t: dá para 32 lâmpadas num fio, menos a perda). */
	public static final long COST = 1;

	public LampBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.LAMP_BE, pos, state);
	}

	/** Com o Industrial: a versão que aceita energia (a classe só é carregada se ele existir). */
	public static LampBlockEntity create(BlockPos pos, BlockState state) {
		return TMoveis.INDUSTRIAL ? com.tcordeiro.moveis.compat.PoweredLampBlockEntity.create(pos, state) : new LampBlockEntity(pos, state);
	}

	/** Recebeu energia suficiente no último tick? */
	public boolean energized() {
		return false;
	}

	/** Um tick com energia (só na subclasse do Industrial). */
	protected void tickEnergy(Level level, BlockPos pos, BlockState state) {}

	public static void serverTick(Level level, BlockPos pos, BlockState state, LampBlockEntity be) {
		be.tickEnergy(level, pos, state);
		LampBlock.refresh(level, pos, state);
	}
}
