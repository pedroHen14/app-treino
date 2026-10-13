# Publicação na Play Store

App: **Muscle Notes** (`com.pedro.musclenotes`).

## 1. Assinatura de release

A chave de upload fica em `key-generate-apk` na raiz do projeto (ignorada pelo git).
As senhas ficam em `keystore.properties`, também fora do git:

1. Copie `keystore.properties.example` para `keystore.properties`.
2. Preencha `storePassword`, `keyAlias` e `keyPassword`. Para conferir o alias:
   `keytool -list -keystore key-generate-apk`.

Sem `keystore.properties` o build de release continua funcionando, mas gera artefato sem assinatura
(é o que acontece no CI).

Guarde a chave e as senhas fora da máquina (gerenciador de senhas). Com o Play App Signing,
perder a chave de upload tem conserto (pedido de redefinição no Play Console), mas leva dias.

## 2. Versão

Calculada pelo git em `app/build.gradle.kts`:

- `versionCode` = número de commits do `HEAD` (`git rev-list --count HEAD`). Só cresce se o release
  sair sempre do `master`, sem reescrever histórico.
- `versionName` = tag `vX.Y.Z` (`git describe --tags`). Fora de uma tag fica `X.Y.Z-N-g<hash>`;
  sem nenhuma tag, `0.0.0-dev`.

Para gerar um release:

```bash
git switch master && git pull
git tag v1.0.0
git push origin v1.0.0
```

Depois rode a configuração **bundleRelease** no Android Studio (ou `./gradlew :app:bundleRelease`).
O arquivo sai em `app/build/outputs/bundle/release/app-release.aab`.

## 3. Primeiro envio (manual)

O primeiro `.aab` precisa ser enviado pelo Play Console: é nele que você ativa o Play App Signing e
o Google passa a reconhecer `key-generate-apk` como chave de upload. A API de publicação só funciona
depois que o app existe no Console.

## 4. Automação depois do primeiro envio (avaliação)

Os dois caminhos usam uma **conta de serviço** do Google Cloud com acesso ao app no Play Console
(Usuários e permissões → convidar o e-mail da conta de serviço com permissão de lançamento).

| | Gradle Play Publisher (GPP) | GitHub Actions por tag |
|---|---|---|
| O que é | Plugin Gradle (`com.github.triplet.play`) com tarefas como `publishReleaseBundle` | Workflow que roda em `push` de tag `v*`, gera o `.aab` assinado e envia |
| Envio | Da sua máquina ou do CI | Só do CI (por uma action de upload ou pelo próprio GPP) |
| Segredos | JSON da conta de serviço local | Keystore, senhas e JSON da conta de serviço em *GitHub Secrets* |
| Extras | Notas de versão, textos e imagens da loja versionados no repo | Release sempre a partir de uma tag, reproduzível |
| Custo | Uma dependência nova no build | Workflow novo; nenhuma dependência no app |

**Recomendação:** GitHub Actions disparado por tag, faixa `internal` primeiro. Combina com o fluxo
de versão acima (a tag já define o `versionName`) e mantém a chave fora da máquina de desenvolvimento.
O GPP pode ser o passo de upload dentro do workflow se você quiser também versionar os textos da loja;
como é uma biblioteca fora da stack, só entra com aprovação.

O que o workflow vai precisar:

- `actions/checkout` com `fetch-depth: 0` (senão o `versionCode` vira 1 e o `describe` não acha a tag).
- Secrets: keystore em base64, as três senhas/alias e o JSON da conta de serviço.
- Gerar `keystore.properties` e `key-generate-apk` no runner antes do `bundleRelease`.

## 5. Política de privacidade e Segurança dos dados

Texto em [`politica-de-privacidade.md`](politica-de-privacidade.md), publicado pelo GitHub Pages
(branch `master`, pasta `/docs`) em:

<https://pedrohen14.github.io/app-treino/politica-de-privacidade>

Cole essa URL no Play Console em **Conteúdo do app → Política de privacidade**. Se o app passar a
coletar algo (conta, sincronização, analytics, crash reporting), atualize a política e o formulário
abaixo antes do release.

Respostas para **Segurança dos dados**, de acordo com o que o app faz hoje:

- **O app coleta ou compartilha dados do usuário?** Não. Treinos e preferências ficam só no aparelho
  (Room e DataStore), sem servidor próprio.
- **Busca no wger:** o termo digitado vai direto do aparelho para `wger.de`, por ação da pessoa, e o
  desenvolvedor nunca o recebe. O Google isenta de "compartilhamento" a transferência iniciada pelo
  usuário que ele espera que aconteça. Se quiser ser conservador, declare "Atividade no app → Histórico
  de pesquisa no app" como coletado, processado de forma efêmera, opcional, para funcionalidade do app.
- **Criptografia em trânsito:** sim (HTTPS).
- **Exclusão:** a pessoa apaga os dados limpando o armazenamento do app ou desinstalando.

## 6. Capturas de tela da loja

Ficam em [`play-store/screenshots`](play-store/screenshots), em 1080 × 2160 (a Play Store não aceita
lado maior que o dobro do menor, então 1080 × 2400 é recusado).

Os dados vêm de uma seed fictícia que só existe no build debug (`app/src/debug`): quatro modelos,
agenda semanal e doze semanas de histórico com progressão de carga. Para refazer no emulador:

```bash
./gradlew :app:installDebug
adb shell wm size 1080x2160
adb shell am broadcast -n com.pedro.musclenotes/com.pedro.treinoapp.debug.DemoSeedReceiver
adb shell am force-stop com.pedro.musclenotes
```

A seed apaga modelos, agenda e sessões (o catálogo de exercícios fica). Com `--ez active true` ela
também deixa o treino do dia em andamento, para capturar a tela de execução com o timer.
Ao terminar, `adb shell wm size reset`.
