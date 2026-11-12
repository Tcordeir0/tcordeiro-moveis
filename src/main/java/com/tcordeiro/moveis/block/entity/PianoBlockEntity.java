package com.tcordeiro.moveis.block.entity;

import java.util.Arrays;

import com.tcordeiro.moveis.anim.PianoMath;
import com.tcordeiro.moveis.registry.MoveisBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Quando foi tocada cada tecla (para a afundar no renderer) e a última nota (os testes leem-na). Não se grava: é só do momento. */
public class PianoBlockEntity extends BlockEntity {
	public static final int EVENT_KEY = 1;
	/** Tick do mundo em que cada tecla foi tocada (−∞ = nunca). */
	public final long[] pressedAt = new long[PianoMath.keyCount()];
	public int lastKey = -1, lastNote = -1;
	public long playUntil;
	/** Tick da última tecla (os dois lados; no cliente vem do blockEvent): a graça do renderer, {@link #drawsKeys}. */
	public long lastPressedAt = Long.MIN_VALUE / 2;

	public PianoBlockEntity(BlockPos pos, BlockState state) {
		super(MoveisBlocks.PIANO_BE, pos, state);
		Arrays.fill(pressedAt, Long.MIN_VALUE / 2);
	}

	public void pressed(int key, int note, long until) {
		lastKey = key;
		lastNote = note;
		playUntil = until;
	}

	/** Chega aos dois lados (blockEvent): o cliente marca a tecla a afundar. */
	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == EVENT_KEY && param >= 0 && param < pressedAt.length && level != null) {
			pressedAt[param] = level.getGameTime();
			lastPressedAt = level.getGameTime();
			return true;
		}
		return super.triggerEvent(id, param);
	}

	/** O renderer desenha as teclas agora? (PLAYING, ou a graça depois da última tecla: {@link com.tcordeiro.moveis.anim.PianoMath#drawsKeys}) */
	public boolean drawsKeys() {
		return level != null && com.tcordeiro.moveis.anim.PianoMath.drawsKeys(getBlockState().getValue(com.tcordeiro.moveis.block.PianoBlock.PLAYING),
				level.getGameTime() - lastPressedAt, com.tcordeiro.moveis.block.PianoBlock.PLAY_TICKS);
	}
}
