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
