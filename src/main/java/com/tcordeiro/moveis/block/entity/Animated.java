package com.tcordeiro.moveis.block.entity;

import com.tcordeiro.moveis.anim.Anim;

/** Entidade com uma animação de abrir/fechar (lida pelo renderer). */
public interface Animated {
	Anim anim();
}
