# Móveis do Tcordeiro (`tmoveis`)

Mod Fabric para Minecraft Java **26.3** (Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, Java 25).

- **Cadeira** e **Banco** — clique direito para sentar, Shift para levantar. Cadeira vira para onde você olha ao pôr.
- **Mesa** — junta-se às vizinhas; as pernas só aparecem nos cantos de fora.
- **Armário** e **Gaveteiro** — 27 espaços, abrem as portas/gaveta, funis e comparadores, o conteúdo cai ao quebrar.
- **Sofá** (16 cores) — emenda com os vizinhos (pontas, meio, canto de dentro e de fora) e dá para sentar.
- **Cama de Casal** (16 cores, 2×2) — dorme como a cama vanilla, cabem duas pessoas, explode no Nether/Fim.
- **Lustre**, **Arandela**, **Luminária de Mesa** — acendem com redstone; com o mod Industrial também com energia (1 J/t).

**0.2.0 — a casa toda** (portas, gavetas, tampas, cortinas e persianas com animação suave):

- Cozinha: **Bancada** (canto em L, 9 espaços), **Pia** (balde, frasco ou cano de fluidos; só água), **Lixeira** (⚠ apaga).
- Quarto: **Mesa de Cabeceira**, **Guarda-Roupa** (36), **Cortina** (16 cores, abrem juntas), **Espelho**, **Relógio de Parede**,
  **Beliche** (16 cores, 2 camas).
- Banheiro: **Vaso Sanitário**, **Banheira** (3 baldes, apaga o fogo).
- Sala/rua: **Lareira**, **Ventilador de Teto**, **Caixa de Correio**, **Escrivaninha**, **Mesa de Centro**, **Mesa Lateral**,
  **Banco de Jardim**, **Floreira**, **Persianas**, **Adega**, **Luminária de Chão**, **Piano**, **Cofre-Forte**, **Toldo** (16 cores).
- Com o mod **Clima** (opcional, por reflexão): a lareira aquece e o ventilador refresca no verão (`ClimaApi.zone`).

Os móveis de madeira existem em **todas as 13 madeiras** do jogo (lidas do jar 26.3). Guia: livro (livro + tábuas) e separador
«Guias» do Receitário.

## Desenvolvimento

```sh
export JAVA_HOME="$HOME/Library/Application Support/horizon-launcher/minecraft/runtime/java-runtime-epsilon/mac-os-arm64/java-runtime-epsilon/jre.bundle/Contents/Home"
python3 tools/gen_data.py          # modelos, blockstates, receitas, loot, tags, traduções, guia, WoodTypes.java
./gradlew build                    # jar em build/libs/
./gradlew runGametest              # testes de servidor (sem janela), com o Industrial
./gradlew runGametest -PnoIndustrial
python3 tools/check_book_layout.py <26.3.jar> src/main/java/com/tcordeiro/moveis/guide/MoveisGuide.java src/main/resources/assets/tmoveis/lang/pt_br.json guide.tmoveis
```

### Modelos (para substituir pelos do Blender)

Cada peça é um modelo-pai genérico em `src/main/resources/assets/tmoveis/models/block/template/<peça>.json`; o gerador cria os
filhos por madeira/cor só com `parent` + `textures` e **nunca reescreve um template que já exista**. As formas de colisão estão no
código (não dependem do JSON). Orientação: tudo virado a norte, exceto a cama (virada a sul, como as camas vanilla).

| Template | Variáveis |
|---|---|
| `chair`, `stool`, `table_top`, `table_leg` (perna no canto noroeste; o blockstate roda-a) | `#planks`, `#log`, `#particle` |
| `cabinet`, `cabinet_open`, `drawer`, `drawer_open` | `#planks`, `#log`, `#particle` |
| `sofa_single`, `sofa_left`, `sofa_right`, `sofa_middle`, `sofa_corner_inner`, `sofa_corner_outer` | `#wool`, `#planks`, `#particle` |
| `double_bed_head_left`, `double_bed_head_right`, `double_bed_foot_left`, `double_bed_foot_right` | `#wool`, `#planks`, `#log`, `#pillow`, `#particle` |
| `chandelier`, `wall_lamp`, `table_lamp` (o filho `_lit` muda só `#light`) | `#metal`, `#glass`, `#light`, `#particle` |

### Animações (0.2.0)

As partes que se mexem estão separadas no `tools/blender/pecas.py` (`<peça>_body` + `<peça>_<parte>`, e `ANIM` com o pivô e o
movimento). O gerador escreve um modelo por parte (`models/block/part/<bloco>/<template>.json` + `items/part/...`) e
`anim/FurnitureParts.java` com os pivôs; o `client/FurnitureRenderer` desenha as partes com essa transformação, só enquanto a
propriedade `moving` do bloco é true (ou com a porta aberta). Lógica pura (JUnit): `anim/Anim`, `Spin`, `ClockMath`, `PianoMath`.

```sh
python3 tools/blender/export_templates.py   # templates a partir do pecas.py
python3 tools/gen_data.py                   # filhos, partes, blockstates, receitas (confere colisões), guia, FurnitureParts.java
./gradlew test                               # JUnit
```

### Energia (Industrial, opcional)

Sem dependência obrigatória: com o `tindustrial` instalado a entidade da lâmpada passa a ser `compat.PoweredLampBlockEntity`, que
implementa a interface pública `EnergyNode` do Industrial (papel `CONSUMER`). Os cabos/Fio de Cobre ligam-se e alimentam-na sem
nenhuma mudança no Industrial.
