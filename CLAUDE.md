# App de treino — instruções para o Claude Code

## Sobre o projeto
App Android nativo para logbook de treinos de musculação: modelos de treino, agenda semanal, execução com timer de descanso, histórico, progressão de carga e recordes pessoais. Uso próprio e portfólio.
Contexto completo (escopo, PBIs, modelo de dados): `docs/contexto-app-treino.md`.

## Stack
- Kotlin, XML Views (sem Jetpack Compose)
- Single Activity + Navigation Component, barra inferior: Hoje, Treinos, Histórico
- Clean Architecture por feature: `feature/<nome>/{data,domain,presentation}` + `core/`
- MVVM com ViewModel + StateFlow
- Hilt (DI), Room (persistência), KSP (não usar kapt)
- Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`)
- MPAndroidChart (gráficos), DataStore (preferências, ex.: tempo padrão do timer)
- Retrofit + coroutines apenas a partir do PBI 7 (API wger)
- minSdk 26

## Convenções
- Código em inglês (classes, funções, variáveis); textos de UI em português via `strings.xml`.
- Carga sempre em gramas (`Int`) no banco e no domínio; conversão para kg só na apresentação.
- Datas como epoch day (`Long`) via TypeConverter.
- Use cases pequenos, um por ação (ex.: `StartWorkoutSessionUseCase`).
- Toda regra de negócio de um PBI deve ter teste unitário no domínio.

## Fluxo de trabalho
1. Antes de implementar, leia a issue com `gh issue view <número>` e siga os critérios de aceite e regras de negócio.
2. Trabalhe em branch `feature/pbi-<número>-<descricao-curta>`.
3. Commits pequenos, no formato `tipo: descrição (#número)` (ex.: `feat: cria entidade de exercício (#2)`).
4. Abra PR com `gh pr create`, descrição com `Closes #<número>` e o checklist dos critérios de aceite.
5. Se algo da issue estiver ambíguo ou contradizer outra regra, pergunte antes de decidir.
6. Não adicione bibliotecas fora da stack sem perguntar.

## Identidade visual (Muscle Notes) — leia antes de mexer em qualquer tela
- Regras completas: `docs/design/README.md`. Valores: `docs/design/tokens.json` (fonte da verdade).
- Cores em `res/values/colors.xml` (tema Giz, claro) e `res/values-night/colors.xml` (tema Ferro, escuro e padrão). Se mudar uma cor, mude em `tokens.json` e nos dois `colors.xml` juntos.
- Nunca use cor hex direto em layout ou código: use `@color/surface`, `@color/ink`, `@color/brasa` etc. (ou os atributos do tema mapeados para eles).
- `brasa` só para progresso: botão principal da tela, selo PR, linha de carga no gráfico. No máximo um elemento brasa por tela. Texto laranja usa `brasa_text`; texto sobre preenchimento brasa usa `on_brasa` (nunca branco).
- Gráficos: carga máxima em `brasa`, volume em `aco`, grade em `rule`, rótulos em `ink_muted`.
- Tipografia: Archivo (títulos e texto) e IBM Plex Mono para todo número de treino (carga, reps, timer), com `fontFeatureSettings="tnum"` (estilos `TextAppearance.MuscleNotes.*`).
- Lista de séries = linha de caderno: divisória `rule` de 1dp, altura mínima 48dp, colunas fixas Série · Carga · Reps · ✓.
- Espaçamento e cantos em `res/values/dimens_design.xml` (grade de 4dp; `radius_sm` 4dp, `radius_md` 10dp). Sem sombras: separe por tom (`surface` → `surface_raised`).
- Ícones: Material Symbols Sharp, peso 500, 24dp.
- Textos em português, com "você". Carga em kg com vírgula e uma casa (`82,5 kg`), séries como `80,0 × 8`. Sem emoji.
