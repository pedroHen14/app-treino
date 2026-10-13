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
