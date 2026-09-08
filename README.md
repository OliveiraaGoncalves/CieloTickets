# Cielo Tickets — Case Técnico Android

[![CI](https://github.com/OliveiraaGoncalves/CieloTickets/actions/workflows/ci.yml/badge.svg)](https://github.com/OliveiraaGoncalves/CieloTickets/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM-4285F4?logo=jetpackcompose)
![Hilt](https://img.shields.io/badge/DI-Hilt-34A853)
![Tests](https://img.shields.io/badge/tests-JUnit5%20%2B%20MockK%20%2B%20Turbine-orange)

App de venda de ingressos com integração de pagamento real via **Cielo
Smart / Cielo Lio** (Deeplink) e catálogo de eventos consumido de uma API
REST ([mockapi.io](https://mockapi.io)). Kotlin + Jetpack Compose, Clean
Architecture + MVVM, 11 módulos Gradle (`core:*`/`feature:*`) com
plugins de convenção próprios em `build-logic/`.

Este README cobre o essencial. Detalhe técnico completo em `docs/`:

| Documento | Conteúdo |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | decisões arquiteturais, trade-offs, pegadinhas de build resolvidas |
| [`docs/SPECS.md`](docs/SPECS.md) | mapa requisito → implementação |
| [`docs/AI_USAGE.md`](docs/AI_USAGE.md) | onde e como a IA foi usada |
| [`docs/desafio.md`](docs/desafio.md) | enunciado do case e matriz de casos de teste (CT-01 a CT-05) |

---

## Rodando o projeto

Validação rápida (sem abrir Android Studio, sem emulador):

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

É exatamente a bateria que o CI (`.github/workflows/ci.yml`) roda a cada
push/PR pra `main` — o badge no topo reflete o resultado da última rodada.

Pra rodar o app de verdade:

1. Android Studio Koala+, JDK 17.
2. Clone e sincronize o Gradle (11 módulos).
3. Rode a configuração `app` — funciona sem nenhuma credencial configurada
   (`API_BASE_URL`/`CIELO_CLIENT_ID`/`CIELO_ACCESS_TOKEN` caem num
   placeholder óbvio se `local.properties` não existir, ver
   `docs/ARCHITECTURE.md#deploy`), mas o fluxo de pagamento real só
   funciona com o [emulador Cielo Smart](https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo)
   instalado e credenciais válidas do [Portal do Desenvolvedor Cielo](https://desenvolvedores.cielo.com.br/api-portal/myapps).
   Sem isso, troque o `@Binds` em `PaymentGatewayModule` pra
   `FakeCieloPaymentGateway` e desenvolva/teste sem a maquininha.

---

## 1. Arquitetura e decisões de design

**Fluxo de dependências** (verificável pelo grafo do Gradle, não é só
convenção): `core:*` nunca depende de `feature:*`; `feature:*` depende de
`core:*` e, quando compartilha modelo de domínio, de outra `feature:*`
específica; só o `app` conhece todas as features. O `domain` de cada
feature é 100% Kotlin puro — sem `import android.*`, sem Room, sem
Retrofit — só `data/` conhece framework.

**UDF**: cada tela expõe um único `StateFlow<UiState>` (ou um sealed
state próprio pra fluxos transacionais, como `PaymentUiState`), consumido
via `collectAsStateWithLifecycle()`. O ViewModel nunca expõe eventos
imperativos pra UI — toda transição de tela nasce de uma mudança de
estado observada (ex.: `PaymentScreen` navega ao ver `PaymentUiState.
Success`/`Cancelled`, não por um callback direto do ViewModel).

**Modularização por feature, não por camada**: cada `feature:*` segue
`domain/ → data/ → presentation/ → di/` internamente — a alternativa
(um módulo `:domain`, um `:data`, um `:presentation` pro app inteiro)
foi descartada porque não isola nada de verdade (mudar uma regra de
negócio de pagamento ainda recompilaria potencialmente tudo) e não deixa
claro o limite de cada funcionalidade. `Repository`/`UseCase` são sempre
`interface` + `Impl`, nunca classe concreta injetada direto — ver
`docs/ARCHITECTURE.md` pra um caso real onde essa regra foi corrigida
depois de encontrada violada (`PurchaseRepository`).

Módulos compartilhados entre features (`EventModel`, `PurchaseOrderModel`)
moram em `core-common` — a alternativa de uma feature depender da outra só
pra enxergar um modelo foi descartada de propósito (acoplaria
`feature-payment` a `feature-home`, por exemplo).

## 2. Tratamento de erros e resiliência

Nenhum `try/catch` solto espalhado pelo código: toda fronteira
domain/data devolve `AppResult<T>` (`Success`/`Failure` seladas), nunca
lança exception pra camada de cima. `safeApiCall` (`core-network`) é o
único lugar que faz `IOException`/`HttpException`/`SocketTimeoutException`
→ `DomainError`; qualquer repositório novo reaproveita, não repete o
`catch`. `DomainError` é um `sealed class` fechado (`Network`, `Timeout`,
`PaymentDenied`, `PaymentCancelled`, `DuplicateTransaction`,
`Serialization`, `Unknown`) — o compilador força tratar cada caso onde
importa (ex.: `PaymentViewModel.stateFor()`), não dá pra esquecer um tipo
de erro novo silenciosamente.

**Anti-duplicidade de cobrança** (requisito não-funcional mais sensível do
case): cada **pedido** recebe uma `idempotencyKey` (UUID) gerada uma única
vez, persistida no próprio `SavedStateHandle` — sobrevive a reenvio de UI
*e* a `process death` no meio do pagamento (a janela mais provável deste
app, já que o fluxo depende de sair pra Cielo Smart e voltar).
`ProcessPaymentUseCase` grava uma tentativa `PENDING` antes de chamar o
gateway; se a mesma chave já tiver `APPROVED`/`PENDING` registrada, a
cobrança **não** é disparada de novo — só `DENIED`/`CANCELLED`/`ERROR`
permitem retry de verdade. Além disso, a UI trava o botão de pagar
enquanto `Processing` (cobre o caso mais comum: duplo toque). Detalhe
completo, incluindo o teste que garante isso, em
`docs/ARCHITECTURE.md#anti-duplicidade-de-cobrança-requisito-não-funcional-crítico`.

## 3. Estratégia de testes

Pirâmide inclinada pra unitário de propósito — o `domain` é puro Kotlin,
então a maior parte da lógica crítica (regra de idempotência, mapeamento
de erro, cálculo de total) é testável em JVM sem emulador:

- **29 testes unitários** (JUnit 5 + MockK + Turbine) em 8 arquivos:
  UseCases (`ProcessPaymentUseCaseTest` — o mais importante do case,
  garante que reenvio com a mesma chave não chama a Cielo de novo) e
  ViewModels (auditoria de emissão sequencial de `StateFlow`, via
  Turbine) de todas as 5 features + mapeamento de rede
  (`EventMapperTest`) + repositório com fallback offline-first
  (`EventRepositoryImplTest`).
- **7 testes instrumentados** (JUnit4 + Compose UI Test, `app/src/
  androidTest`) rodando a jornada completa Home → Seleção → Pagamento →
  Comprovante contra `MainActivity`/`CieloNavHost` reais, com
  `PaymentGateway` fake controlável por teste — pega bug de navegação/back
  stack que teste unitário não alcança.

Rodar só os unitários (rápido, sem emulador):

```bash
./gradlew testDebugUnitTest
```

Rodar os instrumentados (precisa de emulador/dispositivo conectado):

```bash
./gradlew :app:connectedDebugAndroidTest
```

Lista completa e o que cada teste prova: `docs/ARCHITECTURE.md#testes-críticos-cobertos`.

## 4. Engenharia de IA no desenvolvimento

IA (Claude) usada como copiloto em pontos específicos, não como gerador
de código não revisado: tradução do fluxo Activity/Intent da Cielo Smart
pra uma função `suspend` única, exploração de trade-offs de idempotência
(cliente vs. header HTTP vs. combinado), scaffolding de módulo/feature
seguindo o padrão já estabelecido, e — nas sessões mais recentes —
diagnóstico de dois bugs reais de tooling do Gradle (acessores tipados
quebrados por pacote não-nomeado no `build-logic`, e `HttpLoggingInterceptor`
faltando em build release) achados via reprodução isolada e comparação
com repositórios de referência, não achados por "achismo". Toda decisão
arquitetural, revisão de correção e escolha final ficou a cargo do
critério técnico do desenvolvedor — detalhe completo, com os pontos onde
a IA discordou de si mesma e teve que reproduzir/comparar antes de propor
fix, em `docs/AI_USAGE.md`.

## 5. Trade-offs e o que faria com mais tempo

Trade-offs conscientes já assumidos (Room com `fallbackToDestructiveMigration`,
sem product flavor pra `API_BASE_URL`/credencial Cielo por ambiente,
`feature-history` como escopo extra não pedido pelo case): detalhe completo
em `docs/ARCHITECTURE.md#trade-offs-assumidos`.

Com mais tempo:

1. **Fluxo de cancelamento server-side** (`lio://payment-reversal`): hoje
   `cancelWaiting()` só cancela a coroutine local e marca `CANCELLED` no
   nosso banco — não avisa a Cielo Smart pra reverter a transação do lado
   dela, se ela já tiver processado algo antes do cancelamento chegar.
2. **Migrations versionadas do Room** em vez de
   `fallbackToDestructiveMigration()` — aceitável pro escopo do case, não
   pra produção.
3. **Processo formal de segredo por ambiente** (dev/produção) pra
   `API_BASE_URL`/credenciais Cielo, hoje resolvido via `local.properties`/
   variável de ambiente manual.
4. **Suíte instrumentada em mais de uma configuração de tela/densidade**
   via Firebase Test Lab ou similar.
5. **Upgrade de toolchain** (AGP/Kotlin/Hilt/Compose BOM/Room) —
   deliberadamente adiado hoje porque esse grupo é acoplado e subir
   qualquer um força um bump de AGP pra série 9.x (testado
   empiricamente); ver comentário no topo de `gradle/libs.versions.toml`.
