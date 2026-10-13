Muscle Notes é o caderno de quem treina para construir músculo. A pessoa já tem a ficha; o app registra cada série e mostra a carga subindo. A identidade junta três ideias: **o ferro** (peso, contraste, tipo pesado), **o caderno** (pautas, números escritos à mão em fonte mono) e **o próximo recorde** (o ponto brasa que fica sempre um pouco acima).

## A marca

O símbolo é um **M desenhado como um gráfico de carga**: o segundo pico é mais alto que o primeiro, porque a carga de hoje passa a da semana anterior. O **ponto brasa** continua a subida: é o próximo recorde. Atrás, as **pautas** do caderno.

- Use `muscle-notes-lockup-iron.svg` sobre fundos claros e `muscle-notes-lockup-chalk.svg` sobre fundos escuros. Em espaços estreitos, troque pela marca sozinha (`muscle-notes-mark-*`) ou pelo logotipo só texto (`muscle-notes-wordmark-*`).
- O M é sempre `ink` (ferro ou giz) e o ponto é sempre `brasa`. Sobre fundo brasa, o M fica ferro e o ponto fica giz (`app-icon-brasa.svg`). Nunca pinte o M de brasa.
- Área de respiro: o diâmetro do ponto em volta de tudo. Tamanho mínimo: 24dp para a marca, 120dp de largura para a assinatura.
- No logotipo, MUSCLE vai em Archivo Black largo e NOTES em IBM Plex Mono espaçada, cada um apoiado numa pauta. Não recomponha o logotipo com texto vivo: use os arquivos em curvas.
- Não gire, não contorne, não aplique sombra ou gradiente, não troque a ordem MUSCLE/NOTES.

## Voz e conteúdo

- Fale como parceiro de treino, curto e direto, em português, com "você". Comemore com números, não com adjetivos: "Supino reto: 82,5 kg × 8. Novo recorde." em vez de "Incrível, você arrasou!".
- O fechamento padrão do treino é **"Tá pago."**, em `display`. Embaixo, o resumo: "6 exercícios · 21 séries · 1 recorde".
- Use o vocabulário da academia sem explicar: série, reps, carga, descanso, falha, PR, volume, upper/lower.
- Carga sempre em kg, com vírgula decimal e uma casa: `80,0 kg`, `82,5 kg`. Séries no formato `80,0 × 8`. Faixa de reps com meia-risca: `8–10`.
- Caixa normal de frase em títulos e botões ("Concluir série", "Iniciar treino"). Caixa alta só no estilo `label` e no logotipo.
- Sem emoji na interface, nem no "Tá pago".
- Mensagens destrutivas dizem o que se perde: "Descartar treino? As 4 séries não concluídas serão apagadas."

## Fundamentos visuais

**Cor.** Ferro (`surface` escuro) é o tema padrão, porque o app é usado na academia, entre séries, muitas vezes com pouca luz. Giz é o tema claro e segue a configuração do sistema.
- `brasa` é rara de propósito: é a cor do progresso. Use em, no máximo, um elemento por tela (o botão principal ou o selo PR), além da linha de carga do gráfico. Texto em brasa usa sempre `brasa-text`.
- Sobre preenchimento brasa, texto e ícone em `on-brasa`. Nunca branco.
- Nos gráficos de progressão, carga máxima é `brasa` e volume é `aco`. Grade e eixos em `rule`, rótulos em `ink-muted`.
- `perigo` só para excluir, descartar e erro de validação, e sempre com ícone e palavra, porque o tom fica perto da brasa.

**Tipografia.** Três vozes, cada uma com um trabalho:
- Archivo pesada (`display`, `title`, `heading`) para nomes de treino e exercícios.
- Archivo regular (`body`, `body-strong`, `caption`) para o resto do texto.
- IBM Plex Mono (`load-xl`, `load`, `label`) para **todo número de treino**: cargas, reps, tempo de descanso, datas curtas. A fonte mono alinha as colunas do caderno, então cargas de séries diferentes ficam uma embaixo da outra.

**O caderno.** A lista de séries é desenhada como uma página pautada: cada série é uma linha com divisória `rule` de 1dp, altura mínima de 48dp (`space-3` em cima e embaixo) e colunas fixas (Série · Carga · Reps · ✓). A série em andamento ganha fundo `surface-raised`; séries feitas mostram o check em `ink`; o recorde ganha o selo PR em `brasa` com texto `on-brasa`.

**Espaço e forma.** Grade de 4dp (`space-1` a `space-8`). Margem lateral de 16dp (`space-4`). Cantos quase retos, como o M: `radius-sm` em campos e chips, `radius-md` em cards e botões. `radius-pill` só no timer de descanso flutuante.

**Profundidade.** Sem sombras. Separe superfícies por tom (`surface` → `surface-raised`) e por pauta (`rule`).

**Foco e toque.** Anel de foco sólido de 2dp em `brasa-text`, com contraste acima de 3:1 nos dois temas. Alvos de toque com no mínimo 48dp, porque a pessoa está com a mão cansada e às vezes suja de magnésio.

**Movimento.** Rápido e firme: 150–200ms, sem quique. A única celebração animada é o ponto brasa "subindo" no selo de novo recorde.

## Iconografia

Material Symbols **Sharp**, peso 500, 24dp, para combinar com os cantos retos do M. Ícones em `ink` ou `ink-muted`; em brasa só quando acompanham a ação principal. Não use emoji como ícone.

## No Android

- Mapeie os tokens com os mesmos nomes (`surface`, `ink`, `brasa`, `brasa_text`, `on_brasa` e os demais): Giz em `res/values/colors.xml` e Ferro em `res/values-night/colors.xml`. Para Ferro ser o padrão, inicie o app com `AppCompatDelegate.MODE_NIGHT_YES` e deixe a troca para Giz numa preferência.
- Fontes: Archivo (variável) e IBM Plex Mono via Google Fonts para Android (downloadable fonts) ou empacotadas em `res/font`. Ative `fontFeatureSettings="tnum"` nos números.
- Ícone adaptável: o primeiro plano é `adaptive-icon-foreground.svg` (importe pelo Vector Asset do Android Studio), o fundo é a cor `#141412`, e o ícone monocromático (para ícones temáticos do Android 13+) usa `muscle-notes-mark-iron.svg`.
- Play Store: `play-store-icon-512.png` (ícone 512 × 512, sem cantos: a loja aplica a máscara) e `play-feature-graphic.png` (imagem de destaque 1024 × 500).
