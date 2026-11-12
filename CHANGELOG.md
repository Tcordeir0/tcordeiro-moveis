# Changelog

## 0.3.3 — 12/11/2026 — segurança, menos estados e correções do teste no jogo

- **Caixa de Correio protegida como o cofre.** Só o dono abre a caixa a sério e tira (os outros continuam a depositar); só o dono a
  quebra (a picareta dos outros não avança); explosões, o Wither e o dragão não a quebram (o correio não cai); funis, canos do
  Industrial e outros mods (Transfer API do Fabric, com e sem lado) só põem. O cofre ganhou as mesmas regras do lado da Transfer
  API e as tags `wither_immune`/`dragon_immune`.
- **Pincel de verniz e machado respeitam terrenos e proteções:** proteção do spawn, borda do mundo e modo aventura
  (`Protection.mayEdit`: `Level.mayInteract` + `can_place_on`, as mesmas regras do machado e do favo do jogo).
- **HUD (Receitário):** quem não é o dono já não vê o conteúdo do cofre nem da caixa de correio. A regra está no `canOpen` da
  entidade, que o `ProbePayload.mayInspect` do Receitário já consultava: o Receitário não precisou de mudar.
- **Menos estados de bloco: 127 882 → 108 970 (−18 912, −15 %).** A propriedade `moving` saiu do blockstate dos 111 móveis
  animados (armários, gaveteiros, bancadas, mesas de cabeceira, escrivaninhas, guarda-roupas, persianas, cortinas, cofre, lixeira,
  caixa de correio e ventilador): «a mexer» vem agora da animação da entidade, que o cliente entrega ao modelo do chunk
  (`getRenderData`). Os blockstates destes móveis passaram iguais (byte a byte) para `assets/tmoveis/anim_blockstates/`, lidos pelo
  cliente (`AnimModels`): os modelos e as texturas são os mesmos, o aspeto não muda. Os mundos antigos carregam sem perder blocos
  (o jogo ignora a propriedade que já não existe; os outros estados, incluindo o verniz, ficam).
- **As partes animadas já não somem 1 fotograma no fim da animação** (cortinas, persianas, cofre…): o renderer continua a
  desenhar 3 ticks depois do fim, enquanto o chunk se refaz. **Piano:** o teclado já não some entre teclas tocadas com mais de 6
  ticks de intervalo.
- **Ícone do Ventilador de Teto** do tamanho do Lustre na barra de atalhos e no inventário (também no chão e na moldura).
- GameTests novos: `SecurityGameTests` (caixa de correio, Transfer API e canos do Industrial, HUD do Receitário, pincel em modo
  aventura e fora da borda do mundo), `StateCountGameTests` (contagem de estados, mundos antigos com `moving`, cobertura dos
  `anim_blockstates`) e a graça do renderer (móveis e piano): 95 GameTests + 35 testes JUnit. Testes de mutação nas regras de
  segurança, na graça e no `getRenderData`.

## 0.3.2 — 12/11/2026 — guias revistos

- **Página do Livro-guia** com a receita (livro e qualquer tábua).
- **Parquet, espinha, assoalho, lambri e viga** mostram a receita da Serraria (de carvalho), com a Serraria desenhada por cima: as
  receitas do guia têm `station()` (`tmoveis:sawmill`), lida pelo Receitário 0.7.8.
- `lamps`: a luminária de chão também está na lista; `lamp_energy` sem conta («um só Fio de Cobre alimenta até 32 lâmpadas»); cama de
  casal «2 por 2».
- pt-BR: «Quebrar o móvel», «Ao quebrar», «se juntam», «dá para sentar», «clique com o botão direito», «girando», «em volta», «o
  dono abre e quebra»; pt-PT «Clica com o direito».
- Livro escrito: acaba numa página «E o resto?» que manda para o separador Guias do Receitário (65 páginas).
- `tools/check_written_book.py`: verifica um livro escrito (14 linhas, 100 páginas, sem «=» nem «×»), usado também na Forja e no
  Industrial.
- GameTest `guideBookFits`: cada receita do guia existe no servidor, dá o item mostrado e as da Serraria têm a estação.

## 0.3.1 — 12/11/2026 — correções do teste no jogo

- **Sentar virado para a frente do móvel.** Antes o jogador ficava virado para onde estava a olhar ao clicar: na cadeira de costas
  para a mesa, com as pernas a sair pelo encosto; no sofá com as pernas a sair pelo braço. Agora, ao sentar, o jogador é rodado
  para a frente do móvel (o servidor manda a rotação ao cliente) e, enquanto está sentado, o corpo fica para a frente e a cabeça
  só roda até 90° para cada lado (como no barco). Vale para cadeira, banco, sofá, banco de jardim e vaso sanitário.
- **Cantos do sofá:** quem se senta num canto (de dentro ou de fora) olha na diagonal, entre os dois lados abertos.
- **Ícones do Lustre e da Arandela** maiores e centrados na barra de atalhos e no inventário (também no chão e na moldura).
- 83 GameTests (4 novos: rotação ao sentar na cadeira, sofá e vaso sanitário; cantos do sofá; limite de ±90°) + 32 testes JUnit.

## 0.3.0 — 12/11/2026 — madeira e verniz

- **Verniz em 7 tons** (Natural, Mel, Nogueira, Mogno, Ébano, Laca Branca, Laca Preta): um Pincel de Verniz por tom (`tmoveis:varnish_brush_<tom>`, 64 demãos). Clique direito em tábuas, escadas e lajes de qualquer madeira, nos blocos novos e nos móveis: a madeira continua a mesma (o carvalho continua carvalho) e mantém os estados (lado, metade, forma, tipo da laje, água).
- **Polido:** o mesmo pincel outra vez dá a versão polida (mais brilho). Um pincel de outro tom troca a cor.
- **Tirar o verniz:** machado, como a cera do cobre (polida → envernizada → crua), com partículas e som.
- **Desgaste:** só a madeira crua DOS MÓVEIS (blocos novos e móveis), à chuva e com o céu por cima, passa a Desgaste Leve → Médio → Forte (cerca de 9 dias de jogo por estágio). A madeira do jogo nunca muda; a envernizada nunca se desgasta. Com o mod Clima, o outono desgasta mais e o verão menos. O machado raspa um estágio; o pincel numa madeira gasta repõe a cor e a protege.
- **Blocos novos** em todas as 13 madeiras: Parquet, Espinha de Peixe e Assoalho (com escadas e lajes), Lambri e Viga (pilar com eixo).
- **Serraria** (`tmoveis:sawmill`): o cortador de pedras da madeira. Tronco → 6 tábuas (a bancada dá 4); tábuas → blocos novos 1:1, escadas, 2 lajes, 2 paus; parquet/espinha/assoalho → escadas ou 2 lajes. Receitas próprias (`tmoveis:sawing`). A serra gira enquanto a tela está aberta.
- Verniz: frasco + favo de mel + a cor do tom (corantes, carvão vegetal, garrafa de mel). Pincel: lã + verniz + pau (devolve o frasco).
- Os 15 móveis de madeira (os 5 da 0.1.0 e os 10 da 0.2.0: bancada, mesa de cabeceira, guarda-roupa, escrivaninha, mesas de
  centro e lateral, banco de jardim, floreira, persianas, adega) ganharam a propriedade `finish` (crua, 3 desgastes, 7 tons,
  7 polidos); partidos guardam o acabamento e mostram-no no nome. As duas metades do guarda-roupa têm sempre o mesmo acabamento.
  As peças de metal, lã ou porcelana que partilham as classes (lixeira, cofre, caixa de correio, cortina, vaso sanitário…) não.
- **Partes animadas com acabamento:** as portas, gavetas e persianas que o renderer desenha a abrir mostram o mesmo verniz,
  polido ou desgaste do corpo do móvel (o item da parte escolhe o modelo pelo `finish`, como os itens dos blocos).
- Novo separador criativo «Madeira e Verniz»; 16 páginas novas no guia (63 no livro).
- 980 blocos no total, 17 022 modelos, 1 860 texturas. 79 GameTests + 32 testes JUnit.

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
