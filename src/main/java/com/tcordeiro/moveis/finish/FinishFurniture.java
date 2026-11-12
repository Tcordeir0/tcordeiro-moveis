package com.tcordeiro.moveis.finish;

import java.util.List;

/**
 * Os móveis de madeira que levam verniz (propriedade {@link FinishState#FINISH}): sufixos dos ids &lt;madeira&gt;_&lt;sufixo&gt;.
 * GERADO por tools/gen_finishes.py (lista FURNITURE): não editar à mão. Um móvel novo entra lá e a classe do bloco acrescenta
 * FinishState.FINISH ao blockstate (mais o desgaste: isRandomlyTicking/randomTick → {@link Weathering}).
 */
public final class FinishFurniture {
	private FinishFurniture() {}

	public static final List<String> SUFFIXES = List.of("chair", "stool", "table", "cabinet", "drawer");
}
