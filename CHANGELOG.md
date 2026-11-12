# Changelog

## 0.2.0 — 12/11/2026

A casa toda: cozinha, quarto, banheiro, sala e jardim, com animações suaves.

- **Animações** (como a tampa do baú, 5 ticks a abrir e 5 a fechar, interpoladas com o partialTick): portas do armário, da bancada,
  do guarda-roupa e do cofre rodam na dobradiça; gavetas do gaveteiro, da mesa de cabeceira e da escrivaninha deslizam; tampa da
  lixeira; cortinas a correr; persianas a rodar; bandeirinha da caixa de correio; pás do ventilador (velocidade = sinal de
  redstone, máxima com energia; travam e param alinhadas); ponteiros do relógio com a hora do mundo; teclas do piano a afundar.
  Os modelos «_open» deixaram de existir. O renderer só corre a menos de 32 blocos e só enquanto algo se mexe (propriedade
  `moving` do bloco); parado, é o modelo estático.
- **Cozinha**: Bancada (13 madeiras, 9 espaços, canto em L), Pia (água por balde, frasco ou cano de fluidos — FluidStorage da
  Fabric, 1 balde, recusa lava), Lixeira (⚠ apaga ao fechar ou ao receber de um funil).
- **Quarto**: Mesa de Cabeceira (9), Guarda-Roupa (2 de altura, 36 espaços — o ecrã de 4 linhas), Cortina (16 cores, as vizinhas
  abrem juntas), Espelho, Relógio de Parede, Beliche (16 cores; duas camas a sério).
- **Banheiro**: Vaso Sanitário (sentar), Banheira (2 blocos, 3 baldes, apaga o fogo de quem entra).
- **Sala e rua**: Lareira (lenha, pederneira/isqueiro do Clima, pá apaga, ⚠ queima quem encosta, aquece a sala com o Clima),
  Ventilador de Teto (redstone ou energia do Industrial; refresca no verão com o Clima), Caixa de Correio (dono, depósito para os
  outros, bandeirinha, aviso ao entrar), Escrivaninha (3 gavetas, 9, junta-se à vizinha), Mesa de Centro e Mesa Lateral (mostram um
  item), Banco de Jardim, Floreira (plantas e plantações crescem, sempre regada), Persianas, Adega (9 garrafas/poções, visíveis),
  Luminária de Chão (2 de altura), Piano (23 teclas, Sol3–Fá5), Cofre-Forte (só o dono abre e parte; resiste a explosões e a
  funis), Toldo (16 cores).
- 190 blocos novos (290 no total). Guia com 28 páginas novas. 64 GameTests + 21 testes JUnit.

## 0.1.0 — 12/11/2026

Primeira versão.

- Cadeira, Banco, Mesa, Armário e Gaveteiro nas 13 madeiras do jogo (65 blocos), com texturas vanilla.
- Sentar em cadeiras, bancos e sofás (assento invisível; um por bloco; some ao quebrar ou ao levantar).
- Mesas que se juntam (pernas só nos cantos de fora).
- Armário e Gaveteiro com 27 espaços, animação e som de abrir, funis, comparadores, conteúdo cai ao quebrar.
- Sofá em 16 cores, com pontas, meio e cantos (de dentro e de fora).
- Cama de Casal 2×2 em 16 cores: dormir, ponto de renascer, duas pessoas, explosão no Nether/Fim.
- Lustre, Arandela e Luminária de Mesa: redstone; energia do Industrial (1 J/t) se ele estiver instalado.
- Livro-guia (livro + tábuas) e páginas no separador «Guias» do Receitário.
- Modelos provisórios em cubos, organizados em templates para trocar pelos do Blender.
