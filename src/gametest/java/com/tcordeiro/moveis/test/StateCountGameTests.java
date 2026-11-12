package com.tcordeiro.moveis.test;

import java.util.Map;
import java.util.TreeMap;

import com.tcordeiro.moveis.TMoveis;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

/** Medição: quantos estados de bloco os Móveis registam (cada estado pesa no arranque e na memória). */
public class StateCountGameTests {
	@GameTest
	public void stateCount(GameTestHelper h) {
		int total = 0;
		Map<String, int[]> byClass = new TreeMap<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			if (!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(TMoveis.MOD_ID)) continue;
			int n = b.getStateDefinition().getPossibleStates().size();
			total += n;
			int[] a = byClass.computeIfAbsent(b.getClass().getSimpleName() + " " + b.getStateDefinition().getProperties().stream().map(p -> p.getName()).toList(), k -> new int[2]);
			a[0]++;
			a[1] += n;
		}
		StringBuilder sb = new StringBuilder("[ESTADOS] total=" + total + "\n");
		byClass.forEach((k, v) -> sb.append("[ESTADOS] ").append(v[1]).append(" = ").append(v[0]).append(" x ").append(k).append('\n'));
		System.out.println(sb);
		h.succeed();
	}
}
