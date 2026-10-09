# App de treino

[![Android CI](https://github.com/pedroHen14/app-treino/actions/workflows/android.yml/badge.svg)](https://github.com/pedroHen14/app-treino/actions/workflows/android.yml)

App Android nativo para logbook de treinos de musculação: modelos de treino, agenda semanal, execução com timer de descanso, histórico, progressão de carga e recordes pessoais.

## Stack

- Kotlin com XML Views (sem Jetpack Compose)
- Single Activity + Navigation Component, com barra inferior: Hoje, Treinos e Histórico
- MVVM com ViewModel + StateFlow
- Hilt (injeção de dependência)
- Room (persistência), com processamento de anotações via KSP
- Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`)
- `minSdk` 26

## Arquitetura

Clean Architecture organizada por feature, em `app/src/main/java/com/pedro/treinoapp/`:

```
core/
  database/   AppDatabase e TypeConverters
  di/         módulos Hilt compartilhados
  ui/         componentes de UI compartilhados
feature/
  exercise/   catálogo de exercícios
  workout/    modelos de treino e agenda semanal
  session/    execução do treino
  history/    histórico e progressão
  today/      tela Hoje, montada a partir de cards
```

Cada feature é dividida em `data`, `domain` e `presentation`.

A tela Hoje é composta por cards independentes (um Fragment por card), registrados em `feature/today/presentation/TodayCards.kt`. Uma nova feature entra na tela adicionando o seu card a essa lista.

### Convenções de dados

- Carga sempre em gramas (`Int`). A conversão para kg acontece só na apresentação.
- Datas gravadas como epoch day (`Long`), e instantes como epoch millis.
- O schema do Room é exportado em `app/schemas/`.

## Como rodar

Requisitos: JDK 21 e Android SDK 36.

```bash
./gradlew assembleDebug        # gera o APK de debug
./gradlew testDebugUnitTest    # roda os testes unitários
```

O GitHub Actions roda build e testes a cada push e em cada pull request.

## Documentação

- [Contexto do projeto e modelo de dados](docs/contexto-app-treino.md)
- [Backlog do MVP](docs/backlog.md)
