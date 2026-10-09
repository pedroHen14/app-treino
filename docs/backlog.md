# Backlog do MVP — App de treino

Cada seção abaixo vira uma issue no GitHub (template "PBI"), na ordem de entrega.

---

## PBI 1 — Estrutura base do projeto

### História de usuário
Como desenvolvedor, quero o projeto configurado com a arquitetura e as bibliotecas definidas, para que as funcionalidades sejam construídas sobre uma base consistente.

### Critérios de aceite
- [ ] Projeto criado com Kotlin, XML Views, Single Activity e Navigation Component, com barra inferior de três abas: Hoje, Treinos e Histórico.
- [ ] Hilt configurado para injeção de dependência e Room configurado com banco inicial e TypeConverters de data.
- [ ] Gradle com version catalog (`libs.versions.toml`) e KSP.
- [ ] Estrutura de pacotes por feature (`feature/workout`, `core/...`), com as camadas `data`, `domain` e `presentation`.
- [ ] README inicial e GitHub Actions rodando build e testes a cada push.

### Regras de negócio
- `minSdk` 26.
- A tela Hoje deve ser montada a partir de cards independentes, para permitir incluir novas features (como a dieta) sem refatoração.

---

## PBI 2 — Catálogo de exercícios

### História de usuário
Como usuário, quero ter uma lista de exercícios disponível e poder cadastrar os meus, para montar meus treinos sem digitar tudo do zero.

### Critérios de aceite
- [ ] Na primeira execução, o app carrega um catálogo inicial a partir de um arquivo JSON embutido.
- [ ] Listagem com busca por nome e filtro por grupo muscular.
- [ ] É possível criar, editar e arquivar exercícios personalizados (nome e grupo muscular).

### Regras de negócio
- Nome do exercício é obrigatório e único, sem diferenciar maiúsculas e minúsculas.
- Exercício já usado em alguma sessão não pode ser excluído, apenas arquivado. Exercício arquivado não aparece na seleção, mas continua no histórico.

---

## PBI 3 — Modelos de treino e agenda semanal

### História de usuário
Como usuário, quero cadastrar meus treinos e definir em que dia da semana faço cada um, para não precisar montar o treino toda vez.

### Critérios de aceite
- [ ] É possível criar, editar, duplicar e excluir modelos de treino, como "Upper" e "Lower".
- [ ] Cada modelo tem uma lista ordenada de exercícios com séries-alvo e repetições-alvo. A ordem pode ser alterada arrastando os itens.
- [ ] Tela de agenda semanal em que cada dia recebe um modelo ou fica marcado como descanso.

### Regras de negócio
- Modelo precisa de nome e de pelo menos um exercício.
- Repetições-alvo aceitam valor fixo (10) ou faixa (8–12).
- Um modelo pode estar em vários dias da semana.
- Excluir um modelo usado na agenda transforma esses dias em descanso, após confirmação do usuário.
- Editar um modelo não altera sessões já registradas.

---

## PBI 4 — Execução do treino do dia com timer de descanso

### História de usuário
Como usuário, quero iniciar o treino do dia com os exercícios e cargas já preenchidos e controlar meu descanso entre séries, para registrar o treino rápido durante a academia.

### Critérios de aceite
- [ ] A tela Hoje mostra o treino previsto na agenda, ou "Descanso", com um botão para iniciar.
- [ ] A sessão é criada com os exercícios do modelo. Cada série vem pré-preenchida com a carga e as repetições da última execução daquele exercício; sem execução anterior, aparecem apenas os valores-alvo.
- [ ] Durante a sessão é possível ajustar carga e repetições, marcar a série como concluída, adicionar ou remover séries, e adicionar, remover ou trocar exercícios.
- [ ] Ao concluir uma série, o timer de descanso inicia automaticamente, com tempo padrão configurável e opções de pausar, pular e somar ou subtrair 15 segundos. Ao terminar, o celular vibra.
- [ ] É possível iniciar um treino avulso, fora da agenda, a partir de qualquer modelo ou de uma sessão vazia.
- [ ] Ao iniciar um modelo diferente do agendado para o dia, se houver dia anterior na semana corrente com esse modelo e sem sessão registrada, o app pergunta se o treino é uma reposição. A resposta fica vinculada à sessão.
- [ ] Ao finalizar, a sessão é salva no histórico.

### Regras de negócio
- Só pode haver uma sessão em andamento por vez. Se o app for fechado, a sessão é retomada ao reabrir.
- A sessão grava o que foi executado, independente do modelo de origem.
- Séries não concluídas são descartadas ao finalizar, com aviso ao usuário.
- Carga em kg, com incremento mínimo de 0,5 kg.
- O timer funciona com o app aberto. Notificação com o app em segundo plano fica fora do MVP.
- A reposição só vale para dias da mesma semana (segunda a domingo). Treino perdido de semanas anteriores não pode ser reposto.
- Se houver mais de um dia pendente com o mesmo modelo, o app sugere o mais antigo.
- Se o dia escolhido já tinha outro treino agendado, aquele treino continua pendente. O app não reorganiza a agenda sozinho.

---

## PBI 5 — Histórico, progressão e recordes pessoais

### História de usuário
Como usuário, quero ver meus treinos anteriores e a evolução de cada exercício, para acompanhar meu progresso.

### Critérios de aceite
- [ ] Lista de sessões por data, com o nome do modelo, a duração e a quantidade de exercícios. Ao abrir uma sessão, aparece o detalhe completo.
- [ ] Tela de progressão por exercício com o histórico de execuções e um gráfico (MPAndroidChart) de carga máxima ou volume ao longo do tempo, com opção de alternar entre os dois.
- [ ] Ao finalizar uma série que bate um recorde pessoal, o app exibe um destaque na própria sessão. Os recordes ficam visíveis na tela de progressão.
- [ ] É possível editar ou excluir uma sessão já finalizada.

### Regras de negócio
- Volume de uma série = carga × repetições. Volume do exercício na sessão = soma das séries.
- Recorde pessoal é avaliado por exercício e pode ser de dois tipos: maior carga já registrada ou mais repetições com uma mesma carga.
- Editar ou excluir uma sessão recalcula os recordes.

---

## PBI 6 — Resumo semanal na tela Hoje

### História de usuário
Como usuário, quero ver como está minha semana de treinos, para saber se estou cumprindo o planejado.

### Critérios de aceite
- [ ] Card na tela Hoje com os treinos realizados contra os planejados na semana corrente, mostrando os dias com status de feito, reposto, pendente, perdido ou descanso.
- [ ] Também exibe o volume total da semana e os recordes batidos no período.

### Regras de negócio
- A semana vai de segunda a domingo.
- Dia agendado já passado e sem sessão aparece como perdido.
- Um dia reposto conta como cumprido no total de realizados contra planejados.
- Treino avulso sem vínculo de reposição conta apenas como sessão extra.

---

## PBI 7 — Enriquecimento do catálogo via API (wger)

### História de usuário
Como usuário, quero importar exercícios de uma base pública, com descrição e grupo muscular, para ter um catálogo mais completo.

### Critérios de aceite
- [ ] Integração com a API pública do wger usando Retrofit e coroutines.
- [ ] Tela para buscar exercícios no wger e importar para o catálogo local.
- [ ] Estados de carregamento, erro e falta de conexão tratados na tela.

### Regras de negócio
- Exercícios importados ficam salvos localmente e funcionam offline.
- Se já existir exercício com o mesmo nome, a importação é bloqueada e o app sugere o exercício existente.
- A importação dá prioridade à tradução em português, quando a API tiver; se não tiver, usa o nome em inglês e permite editar.
