# Decisões Arquiteturais — Cielo Tickets

## Visão geral de módulos

```
app                        (orquestração, @HiltAndroidApp, NavHost — conhece tudo)
├── core:core-common        (AppResult, DomainError, UseCase base, Dispatchers)
├── core:core-local-storage  (Room: PurchaseAttempt = idempotência; Event = catálogo local)
├── core:core-payment-cielo  (integração Deeplink real com a Cielo Smart)
├── core:core-designsystem   (tema Compose, componentes de loading/erro)
├── feature:feature-home             (listar eventos)
├── feature:feature-ticket-selection (escolher quantidade)
├── feature:feature-payment          (cobrança + anti-duplicidade)
└── feature:feature-receipt          (comprovante + QR code — domain/data/di
                                       completos, reconstrói o comprovante a
                                       partir só da idempotencyKey)
```

Não existe módulo de rede: o app não tem backend (nem precisa, ver
"Trade-offs assumidos") — a única fonte de dados é o Room, tanto para o
catálogo de eventos quanto para a trilha de idempotência de pagamento.

Regra dura, verificável pelo grafo de dependências do Gradle:
**`core:*` nunca depende de `feature:*`. `feature:*` pode depender de `core:*`
e, quando compartilha modelo de domínio (ex. `Event`), de outra `feature:*`
específica — mas nunca do `app`.** Isso mantém o paralelismo real de build:
o Gradle pode compilar todos os módulos `core` em paralelo, e as `feature`
em paralelo entre si assim que seus `core` terminam.

## `build-logic`: plugins de convenção

Todo o boilerplate de `android { compileSdk/minSdk/compileOptions }`,
Compose e Hilt que se repetia em cada `build.gradle.kts` foi extraído para
`build-logic/convention` (composite build incluído via
`pluginManagement.includeBuild("build-logic")` no `settings.gradle.kts` raiz).
Plugins registrados (aplicados via `alias(libs.plugins.cielotickets.*)`):

- `cielotickets.android.library` / `cielotickets.android.application`:
  `compileSdk`/`minSdk`/`compileOptions`/`jvmTarget` comuns.
- `cielotickets.android.compose`: liga Compose (plugin do compilador +
  `buildFeatures.compose` + BOM/ui/material3/tooling) em qualquer módulo já
  configurado por um dos dois acima.
- `cielotickets.android.hilt`: KSP + plugin do Hilt + dependência
  `hilt-android`/`hilt-compiler`.
- `cielotickets.android.feature`: agrega library + compose + hilt +
  `hilt-navigation-compose` + `navigation-compose` (pro `SavedStateHandle`
  ler os argumentos de rota — ver seção de navegação abaixo) +
  `core-common`/`core-designsystem` — o que toda `feature:*` com
  `@HiltViewModel` via Compose precisa. As 4 features usam (`feature-receipt`
  passou a usar também depois de ganhar `ReceiptViewModel`).
- `cielotickets.android.test.junit5`: `testOptions.unitTests.useJUnitPlatform()`
  + JUnit5/MockK/Turbine, só nos módulos com testes reais
  (`feature-home`, `feature-payment`).

Dependências entre módulos usam os accessors tipados do Gradle
(`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` no `settings.gradle.kts`
raiz) — `projects.core.coreLocalStorage` em vez de
`project(":core:core-local-storage")` — para pegar erro de digitação em
tempo de compilação do script, não só quando o Gradle tenta resolver o
módulo.

**Pegadinha do Gradle 9 encontrada aqui:** um módulo que aplica um plugin
vindo de um *included build* (caso de todo `cielotickets.*`) perde, *nesse
mesmo arquivo*, só a parte **tipada** do accessor do catálogo — `libs.core.ktx`
dá "Unresolved reference". O método genérico `libs.findLibrary("nome-no-toml")`
continua funcionando normalmente no mesmo objeto `libs`, sem precisar
resolver o catálogo de novo na mão. Nos módulos que ainda precisam de uma
lib direta (não coberta por nenhum plugin de convenção — `app`,
`core-common`, `core-local-storage`, `core-payment-cielo`, e as 4
`feature:*` por causa de `kotlinx-serialization-json` das rotas tipadas), a
saída é só trocar `libs.foo.bar` por `libs.findLibrary("foo-bar").get()`.

## Por que Clean Architecture + MVVM por feature

Cada `feature:*` segue `domain/ → data/ → presentation/ → di/`:
- `domain`: modelos e regras de negócio puras (sem Android, sem Room).
- `data`: implementação concreta (Room DAO, mapeamento Entity → domínio).
- `presentation`: ViewModel (`@HiltViewModel`, estado imutável via `StateFlow`) + Composables.
- `di`: `@Module @InstallIn(SingletonComponent::class)` só quando a feature
  precisa de `@Binds`/`@Provides` (ex. bindar uma interface de domínio a
  sua implementação) — `UseCase`s e `ViewModel`s não precisam de módulo
  próprio, ganham `@Inject constructor` direto e o Hilt resolve sozinho.

Isso torna o `domain` 100% testável em JVM puro e isola qualquer troca de
fornecedor (ex. trocar a fonte local por uma API real no futuro) na camada
`data`.

## Anti-duplicidade de cobrança (requisito não-funcional crítico)

Ponto mais sensível do case. Estratégia adotada em `feature-payment`:

1. Cada **pedido** (não cada request HTTP) recebe uma `idempotencyKey`
   (UUID) gerada uma única vez pela `PaymentViewModel`, sobrevivendo a
   qualquer reenvio dentro daquela tela (retry de rede, duplo toque).
2. Antes de chamar o gateway, `ProcessPaymentUseCase` grava no Room
   (`PurchaseAttemptDao`) uma linha `PENDING` com essa chave.
3. Se a mesma chave já tiver uma tentativa `APPROVED` (já cobrou) ou
   `PENDING` (pode estar em andamento) registrada, o caso de uso **não
   chama a Cielo de novo** — retorna o resultado já persistido. `DENIED`/
   `CANCELLED`/`ERROR` não chegaram a cobrar nada, então "Tentar novamente"
   dispara uma cobrança de verdade de novo com a mesma chave — só o
   resultado final é que sobrescreve a linha.
4. A `PaymentViewModel` bloqueia novos cliques enquanto `Processing`,
   cobrindo o caso mais comum de duplicidade (duplo toque de UI).

## Tratamento diferenciado por tipo de falha (Negada vs. Cancelada/Timeout)

O case pede tratamento **diferente** pra cada um dos 3 desfechos possíveis
de um pagamento, não um "deu erro" genérico:

- **Aprovada**: `PaymentUiState.Success` → navega pro comprovante com QR Code.
- **Negada** (`DomainError.PaymentDenied`): `PaymentUiState.Failed` — fica
  na própria tela de pagamento, mensagem específica de negada, botão
  "Tentar novamente" (mesma `idempotencyKey`, sem perder o carrinho).
- **Cancelada/Timeout** (`DomainError.PaymentCancelled`, `DomainError.Timeout`,
  e o botão manual "Cancelar" durante `Processing`): `PaymentUiState.Cancelled`
  — a `PaymentScreen` reage com um `LaunchedEffect` que aciona
  `onCancelled()`, e o `CieloNavHost` faz `popBackStack()` de volta pra
  `TicketSelectionRoute`. Como essa tela nunca sai do back stack (só ganha
  outra por cima), seu `TicketSelectionViewModel` sobrevive com o estado
  intacto — inclusive a quantidade escolhida. Isso hoje é garantia de
  lifecycle do próprio Android (o ViewModel só é criado uma vez por
  `NavBackStackEntry`, e seu `init` só roda naquela primeira vez), não mais
  uma checagem manual de "é o mesmo evento?" — ver seção de navegação
  tipada abaixo.

Diferença de UX proposital: "negada" mantém o cliente na tela de pagamento
(ele pode querer tentar outro cartão ali mesmo); "cancelada/timeout" tira o
cliente da tela de pagamento e devolve pro resumo do pedido — reflete que
nesses dois casos a intenção mais provável é revisar o pedido antes de
tentar de novo, não bater no mesmo botão imediatamente.

## Integração Cielo Smart (via Deeplink)

O SDK nativo da Cielo Lio foi **descontinuado** (recebe apenas patches) e a
própria Cielo recomenda oficialmente a **integração via Deeplink** para a
nova geração de terminais Cielo Smart — é o modelo demonstrado no repositório
[LIO-SDK-Sample-Integracao-Local](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local)
e usado como referência aqui. Vantagem prática para este case: funciona
direto contra o **emulador Cielo Smart** exigido no enunciado, sem precisar
publicar nenhum `.aar` num repositório Maven local.

Fluxo implementado em `core-payment-cielo/deeplink/`:

1. `CieloDeeplinkCodec` monta o JSON de checkout (`CieloCheckoutRequestPayload`)
   com `reference = idempotencyKey` do nosso pedido, Base64-encoda e monta a
   URI `lio://payment?request=...&urlCallback=order://response`.
2. `CieloDeeplinkPaymentGateway.charge()` dispara essa URI via
   `Intent(ACTION_VIEW)` — isso abre a Cielo Smart/emulador em outro app — e
   **suspende** aguardando o resultado.
3. A Cielo Smart devolve o controle chamando de volta `order://response?response=BASE64`.
   Isso é recebido por `CieloResponseActivity` (declarada no
   `AndroidManifest.xml` do próprio módulo `core-payment-cielo`, que faz merge
   automático no manifest do `app` — um dos poucos casos em que um `core`
   contribui algo "visível" para o app final, mas sem depender de nenhuma
   `feature`).
4. `CieloResponseActivity` decodifica o JSON e publica o resultado em
   `CieloDeeplinkResultBus` (um `SharedFlow` singleton de processo) — essa
   ponte é necessária porque o callback chega numa Activity nova, desacoplada
   da tela que iniciou o pagamento.
5. `CieloDeeplinkPaymentGateway` retoma a coroutine suspensa, correlacionando
   pelo campo `reference` (nossa `idempotencyKey`) e com timeout de 5 minutos,
   e mapeia o JSON de volta para `CieloChargeResult` — o resto do app nunca
   viu formato de JSON da Cielo, só `feature-payment` conhece `CieloChargeResult`.

**Forma de pagamento escolhida na maquininha, não no app**: `paymentCode`
em `CieloCheckoutRequestPayload` é opcional e vai `null` (omitido do JSON
por causa de `explicitNulls = false`). Sem esse campo, a própria Cielo
Smart pergunta ao cliente a forma de pagamento (crédito à vista, parcelado,
débito — o que estiver habilitado nas credenciais do lojista) na tela da
maquininha, em vez de uma tela nossa. Evita, de propósito, o risco de
mandar um código fixo (ex. `CREDITO_AVISTA`) que o lojista não tenha
habilitado — nesse caso a Cielo Smart recusaria a transação.

Configuração necessária antes de rodar contra o emulador:
- `Client-Id` e `Access-Token` gerados no
  [Portal do Desenvolvedor Cielo](https://desenvolvedores.cielo.com.br/api-portal/myapps),
  configurados como `CIELO_CLIENT_ID`/`CIELO_ACCESS_TOKEN` no `local.properties`
  (gitignorado) — viram `BuildConfig.CIELO_CLIENT_ID`/`CIELO_ACCESS_TOKEN` em
  `core-payment-cielo`, lidos por `DevCieloCredentialsProvider`. Sem essas
  chaves, cai num placeholder óbvio (`SEU_CLIENT_ID_AQUI`) — o projeto
  continua compilando, só não autentica de verdade contra a Cielo. Ver
  `#deploy` abaixo.
- `meta-data android:name="cs_integration_type" android:value="uri"` no
  `AndroidManifest.xml` do `app` — obrigatório para publicação em terminais
  Cielo Smart (já incluído).

`ProcessPaymentUseCaseTest` mocka `CieloPaymentGateway` diretamente com
MockK — não faz sentido, nem é possível, abrir outro app/Activity num teste
JVM puro. `FakeCieloPaymentGateway` continua existindo à parte, como opção
para rodar o app manualmente sem o emulador instalado: basta trocar o alvo
do `@Binds bindPaymentGateway` em `PaymentGatewayModule` para apontar para
ela (com Hilt essa troca é em tempo de compilação, diferente do binding
dinâmico que o Koin permitia).


## Navegação tipada e sobrevivência a `process death` {#process-death}

Motivação: o fluxo inteiro deste app depende de sair pro app da Cielo Smart
(via Deeplink) e voltar — exatamente a janela em que o Android mais mata
processos em background por memória. Até uma versão anterior deste projeto,
`selectedEvent`/`currentOrder`/`currentReceipt` viviam em
`remember { mutableStateOf(...) }` no `CieloNavHost`, o que se perde por
completo quando o processo morre: o usuário voltava pra Home do zero, e —
pior — a `idempotencyKey` da cobrança em andamento também se perdia, abrindo
uma janela real de cobrança duplicada (um "tentar de novo" pós-restauração
geraria uma chave nova, e o curto-circuito de idempotência nunca
encontraria a tentativa antiga).

**Solução**: rotas tipadas via Navigation-Compose 2.8 (`@Serializable`, um
arquivo por feature em `navigation/*Route.kt` — cada rota mora no módulo da
própria feature, nunca no `:app`, porque a regra de dependência é
`feature:* → core:*` e nunca o contrário). Cada tela recebe só um ID
primitivo (`eventId`, `quantity`, `idempotencyKey`) em vez de um objeto de
domínio inteiro, e o próprio ViewModel recarrega o que precisa via
`SavedStateHandle` + um `UseCase` (`GetEventByIdUseCase`, `GetReceiptUseCase`):

- `TicketSelectionViewModel`/`PaymentViewModel` reconstroem o
  `Event`/`PurchaseOrder` a partir do `eventId`.
- `ReceiptViewModel` reconstrói o `PurchaseReceipt` inteiro a partir só da
  `idempotencyKey`, combinando `PurchaseAttemptDao.findByKey` +
  `EventDao.getById` — por isso `feature-receipt` ganhou uma camada
  `domain/data/di` completa (antes era só 2 arquivos de `presentation`,
  sem repositório nenhum).
- **Ponto crítico**: a `idempotencyKey` gerada pela `PaymentViewModel` é
  persistida no próprio `SavedStateHandle` (`savedStateHandle["idempotency_key"] = ...`),
  não só num campo em memória. Se o processo morrer com uma cobrança
  `PENDING`, o ViewModel recriado continua vendo a MESMA chave — é isso que
  fecha a janela de dupla cobrança descrita acima. Coberto por teste
  unitário dedicado em `PaymentViewModelTest` (duas instâncias do ViewModel
  compartilhando o mesmo `SavedStateHandle`, confirmando que a chave usada
  na cobrança é idêntica nas duas).

**Detalhe de implementação**: os ViewModels leem os campos da rota via
`savedStateHandle.get<String>("eventId")` direto, **não** via
`savedStateHandle.toRoute<Route>()`. O decoder de `toRoute()` passa por
`android.os.Bundle` por baixo dos panos (confirmado empiricamente pelo
stack trace), que não é mockado em teste unitário puro — só funciona em
instrumentado ou com Robolectric. `get()` simples lê do mapa interno do
próprio `SavedStateHandle` e funciona idêntico nos dois ambientes, sem
precisar de Robolectric só para isso. Trade-off consciente:
a chave string (`"eventId"`) tem que casar com o nome do campo na `Route`
— um rename futuro do campo não é pego pelo compilador nessa ponta
específica, mas os testes instrumentados (que exercitam a navegação real de
ponta a ponta) pegariam a quebra na hora.

## Testes críticos cobertos

- `HomeViewModelTest`: sucesso ao carregar eventos reflete no `StateFlow`;
  catálogo vazio vira `Success(emptyList())`, não `Error` (são branches
  diferentes — só falha de repositório é erro).
- `TicketSelectionViewModelTest`/`PaymentViewModelTest`/`ReceiptViewModelTest`:
  carregam o domínio a partir do `eventId`/`idempotencyKey` da rota (mock de
  `GetEventByIdUseCase`/`GetReceiptUseCase`), cobrindo sucesso e "não
  encontrado".
- `ProcessPaymentUseCaseTest`:
  - reenvio com a mesma `idempotencyKey` **não** dispara nova chamada ao
    gateway (o teste que mais importa para este case);
  - pagamento aprovado grava `status = APPROVED` com o `transactionId`.

## Testes instrumentados (`app/src/androidTest`) {#instrumented-tests}

Rodam de verdade num dispositivo/emulador (`./gradlew :app:connectedDebugAndroidTest`),
via `MainActivity` + `CieloNavHost` reais — diferente dos testes unitários,
aqui a gente exercita navegação, back stack e composição de tela juntos, que
foi exatamente onde os bugs mais difíceis deste projeto apareceram (ver
seção de trade-offs/histórico). Usam **JUnit4** (não JUnit5): o
`ComposeTestRule`/`AndroidJUnitRunner` do ecossistema androidx.test são
construídos sobre JUnit4 — forçar JUnit5 aqui seria nadar contra a maré do
ecossistema sem ganho real, então os dois convivem no projeto por design
(unitário = JUnit5, instrumentado = JUnit4).

**DI de teste** (`app/src/androidTest/.../di/`): `HiltTestRunner` troca a
`Application` por `HiltTestApplication`, o que ativa os módulos
`@TestInstallIn`:
- `TestPaymentGatewayModule` — substitui a integração real (abre a Cielo
  Smart via Deeplink) por `ControllableFakePaymentGateway`, que cada `@Test`
  reconfigura (`nextResult`/`responseDelayMs`) antes de tocar em "Pagar".
  Sem isso, o teste dependeria de um emulador Cielo Smart instalado no
  dispositivo de CI e de interação manual no app dele.
- `TestLocalStorageModule` — Room em memória, populado de forma síncrona
  (a versão de produção popula via coroutine fire-and-forget no `onCreate`
  — ver `LocalStorageModule` —, o que é uma corrida aceitável em produção
  mas indesejável num teste determinístico).

**Cobertura**: `HomeScreenInstrumentedTest` (CT-01) e
`PurchaseFlowInstrumentedTest` (CT-02 a CT-05 + duas regressões achadas via
teste manual nesta sessão: pilha de navegação duplicando "receipt" ao voltar
depois de aprovado, e cancelar durante "Processando..." precisa voltar pro
resumo em vez de travar). Ver docstring de cada classe para o que cada teste
prova e por quê.

## CI (`.github/workflows/ci.yml`) {#ci}

Dois jobs, sequenciais (`instrumented` só roda se `verify` passar, pra não
gastar tempo de emulador num build já quebrado):

- **`verify`**: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`
  em `ubuntu-latest` — feedback rápido (sem emulador), roda em todo
  push/PR pra `main`.
- **`instrumented`**: `./gradlew :app:connectedDebugAndroidTest` via
  [`reactivecircus/android-emulator-runner`](https://github.com/ReactiveCircus/android-emulator-runner),
  que sobe um emulador Android de verdade (API 34) no próprio runner
  hospedado do GitHub (usa KVM, sem custo de infra própria).

Fica pronto pra funcionar assim que o repositório for publicado num remoto
— não requer nenhum segredo/credencial (os testes usam
`ControllableFakePaymentGateway`/Room em memória, nunca a Cielo Smart real
ou credenciais de produção).

## Lint como gate de verdade {#lint}

`app/build.gradle.kts` configura `lint { checkDependencies = true, ... }`:
`checkDependencies` faz o `:app` analisar todos os módulos `core`/`feature`
juntos num único relatório (lint por módulo isolado perde problema
cross-module, tipo recurso duplicado). `abortOnError = true` +
`warningsAsErrors = true` — lint quebra o build de verdade, não é só um
relatório pra ignorar.

Achados corrigidos na limpeza inicial: parâmetro `Modifier` fora de ordem em
`FullScreenLoading` (`core-designsystem`), duas strings com contagem
(`"%1$d disponíveis"`) convertidas pra `<plurals>` de verdade (evita
"1 disponíveis" no singular), dependência `datastore-preferences` não usada
removida. O check `Typos` foi desligado (`disable += "Typos"`) porque usa
dicionário em inglês e o app é 100% pt-BR — só gera falso positivo (ex:
"momento" acusado como erro de "memento").

**`app/lint-baseline.xml`** (comitado) captura os achados de
`AndroidGradlePluginVersion`/`GradleDependency`/`NewerVersionAvailable` sobre
o grupo de dependências deliberadamente preso numa versão mais antiga — ver
o comentário no topo de `gradle/libs.versions.toml` pra detalhes de por quê
(resumo: `agp`/`kotlin`/`ksp`/`hilt`/`composeBom`/`room`/toda a família
`androidx.activity`/`lifecycle`/`navigation` formam um grupo acoplado; subir
qualquer um force um bump de AGP pra série 9.x, testado empiricamente).
Achado novo fora do baseline quebra o build de propósito. Pra atualizar o
baseline depois de um upgrade real desse grupo: `./gradlew :app:updateLintBaseline`.

## Deploy: build types, ProGuard/R8 e assinatura {#deploy}

Dois build types (não product flavors — não há ambiente/backend diferente
pra flavorizar, já que o app não faz chamada de rede nenhuma):

- **`debug`**: `applicationIdSuffix = ".dev"` + `versionNameSuffix = "-dev"`
  + nome do app "Cielo Tickets Dev" (`app/src/debug/res/values/strings.xml`
  sobrescreve `app_name`) — dá pra instalar debug e release no mesmo
  aparelho ao mesmo tempo, e fica óbvio visualmente qual é qual.
- **`release`**: `isMinifyEnabled = true` + `isShrinkResources = true` +
  `proguardFiles(...)` (regras em `app/proguard-rules.pro`) + assinado com
  keystore de verdade.

**Assinatura**: `keystore.properties` (gitignorado, template em
`keystore.properties.example`) guarda `storeFile`/`storePassword`/
`keyAlias`/`keyPassword`. Sem esse arquivo, `assembleRelease` ainda
funciona **localmente** (cai pra assinatura de debug, com um `logger.warn`
avisando) — mas esse artefato não deve ser publicado. Pra gerar um keystore
de verdade:
```
keytool -genkeypair -v -keystore release.keystore -alias cielotickets \
  -keyalg RSA -keysize 2048 -validity 10000
```

**ProGuard/R8**: Hilt, Room e o compilador do Compose já publicam suas
próprias `consumer-rules.pro` — não precisam de regra manual. O ponto que
precisa de atenção manual é `kotlinx.serialization`: os modelos do
checkout/callback da Cielo Smart (`CieloDeeplinkModels.kt`) são
serializados via reflection no serializer gerado. Sem as regras em
`app/proguard-rules.pro`, o R8 poderia remover/renomear esses campos **sem
erro de compilação** — o app instalaria normalmente, mas o payload que a
Cielo Smart espera viria com nomes errados, e o pagamento real quebraria
silenciosamente. É exatamente o tipo de bug que só aparece em produção,
nunca em debug (onde `isMinifyEnabled = false`).

**Permissão de INTERNET removida do manifest**: sobrava do antigo
`core-network` (ver histórico) — o app não faz nenhuma chamada de rede hoje
(eventos são locais, pagamento é via Deeplink/Intent). Pedir uma permissão
que não é usada é ruído na revisão de loja e no consentimento do usuário.

## Trade-offs assumidos

- Sem backend próprio (o case libera essa opção, e pede eventos locais —
  ver `docs/desafio.md`, CT-01): o catálogo de eventos é 100% local,
  populado uma única vez no Room via `SeedEvents` na criação do banco. Em
  produção, `EventRepositoryImpl` seria o único ponto a trocar para buscar
  de uma API real, sem tocar em `domain`/`presentation`.
- Room com `fallbackToDestructiveMigration()`: aceitável para o escopo do
  desafio; produção exigiria migrations versionadas.
- Sem product flavors para trocar credencial Cielo dev/produção — hoje é
  `local.properties`/variável de ambiente manual (ver `#deploy`). Aceitável
  para o escopo do case (não há múltiplos ambientes de backend pra
  flavorizar), mas um app publicado precisaria de um processo mais formal
  de gestão de segredo por ambiente.
