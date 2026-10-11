# Contexto do projeto — App de treino (Android/Kotlin)

## Objetivo
App Android nativo (Kotlin, XML Views) para logbook de treinos de musculação, com histórico e evolução de carga. Uso próprio e portfólio.

## Decisões de escopo
- MVP focado só em treino. Dieta fica para depois (tela Hoje montada com cards para receber a dieta sem refatoração).
- Rotina que o app precisa suportar: upper/lower 2x por semana, descanso na quarta.
- Backlog completo: `docs/backlog.md` (também cadastrado como issues no GitHub).

## Modelo de dados (Room)
- exercise: id, name (único), muscle_group, is_custom, is_archived, wger_id (único, opcional)
- workout_template: id, name (único), created_at, updated_at
- template_exercise: id, template_id (FK cascade), exercise_id (FK restrict), position, target_sets, target_reps_min, target_reps_max
- weekly_schedule: day_of_week (PK), template_id (FK set null = descanso)
- workout_session: id, template_id (FK set null), template_name (cópia do nome), date (epoch day), started_at, finished_at (nulo = em andamento), makeup_for_date (dia reposto, epoch day), notes
- session_exercise: id, session_id (FK cascade), exercise_id (FK restrict), position, target_reps_min, target_reps_max (alvo copiado do modelo, nulo em exercício avulso)
- workout_set: id, session_exercise_id (FK cascade), position, weight_grams (Int, nulo = não preenchida), reps (nulo = não preenchida), is_completed, completed_at

Decisões:
- O que é planejado (exercise, workout_template, template_exercise, weekly_schedule) fica separado do que é executado (workout_session, session_exercise, workout_set). Editar um modelo nunca altera o histórico.
- Carga em gramas (Int) para comparação exata de recordes.
- Recordes pessoais e resumo semanal são calculados por consulta, sem tabela própria.
- Tempo padrão do timer fica no DataStore.
- Índices em todas as FKs e em workout_session.date.

## Wireframes
Canvas no Claude com 7 telas: Hoje, Sessão de treino, Treinos e agenda, Editar modelo, Histórico, Progressão do exercício, Catálogo de exercícios.
https://claude.ai/artifact/24dzU3haVFUiyFuuuxhCVG
