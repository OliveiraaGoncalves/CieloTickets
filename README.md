# Cielo Tickets — Case Técnico Android

App de venda de ingressos para eventos locais, com integração de pagamento
via ecossistema **Cielo Smart / Cielo Lio**, construído em **Kotlin +
Jetpack Compose**, arquitetura **Clean Architecture + MVVM**, modularizado
em módulos `core:*` e `feature:*` (Gradle multi-módulo, paralelizável), com
a configuração de build repetitiva (Android/Compose/Hilt/testes)
centralizada em plugins de convenção próprios em `build-logic/` (detalhe em
`docs/ARCHITECTURE.md`).

Documentação completa exigida pelo case está em `docs/`:
- [`docs/SPECS.md`](docs/SPECS.md) — mapa requisito → implementação.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — decisões arquiteturais e trade-offs.
- [`docs/AI_USAGE.md`](docs/AI_USAGE.md) — como e onde a IA foi usada.

## Instruções de execução

1. Android Studio Koala+ (AGP 8.6, Kotlin 2.0, JDK 17).
2. Baixe e instale o [emulador Cielo Smart](https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo).
3. Abra o projeto na raiz (`CieloTickets/`) — o Gradle sincroniza os 9 módulos.
4. Rode a configuração `app` no emulador/dispositivo.
5. Testes: `./gradlew testDebugUnitTest` (roda os testes de todos os módulos).

> `./gradlew assembleDebug testDebugUnitTest` foi executado e passa de
> ponta a ponta neste ambiente (compilação de todos os módulos, geração dos
> componentes Hilt, testes JUnit5). O único passo não verificável aqui é a
> execução real num emulador/dispositivo — não há um disponível neste
> ambiente de geração.

## Decisões arquiteturais (resumo — detalhe em docs/ARCHITECTURE.md)

- **Modularização core/feature**: `core:*` nunca depende de `feature:*`;
  `feature:*` depende de `core:*` (e às vezes de outra `feature:*` quando
  compartilha modelo de domínio, como `Event`). Só o `app` conhece todas as
  features — é quem monta o `NavHost` e declara `@HiltAndroidApp`.
- **DI**: Hilt, com um `@Module` por `core`/`feature` que precisa de
  `@Binds`/`@Provides`; classes concretas (`UseCase`s, `ViewModel`s) usam
  `@Inject constructor` direto, sem módulo próprio.
- **Eventos**: catálogo 100% local, sem backend — `Room` guarda o catálogo
  de eventos e é populado uma única vez com dados fixos (`SeedEvents`) na
  criação do banco.
- **Persistência local**: Room, usado tanto para a trilha de idempotência
  de pagamento quanto para o catálogo de eventos (as duas únicas fontes de
  dado do app — não há mais camada de rede).
- **Pagamento**: toda a integração Cielo passa pela interface
  `CieloPaymentGateway`, com uma implementação fake (`FakeCieloPaymentGateway`)
  disponível para desenvolvimento sem o emulador — troque o alvo do
  `@Binds` em `PaymentGatewayModule` para usá-la.

## Bibliotecas externas e justificativas

| Lib | Por quê |
|---|---|
| Hilt | DI padrão recomendado pelo Google para Android, integra nativamente com `ViewModel`/`Compose` via `hiltViewModel()` |
| kotlinx.serialization | usado pelo `core-payment-cielo` para o contrato JSON do Deeplink Cielo Smart, sem reflection do Gson |
| Room | única fonte de dado do app: tentativas de compra (anti-duplicidade) e catálogo local de eventos |
| ZXing | geração do QR Code do ingresso (requisito opcional) sem dependência de serviço externo |
| JUnit 5 + MockK + Turbine | testes de coroutines/Flow em Kotlin idiomático, com JUnit Jupiter |

## Como foi feita a integração com a Cielo Smart

**Integração via Deeplink** (recomendada pela própria Cielo — o SDK nativo
está descontinuado), seguindo o padrão do
[sample oficial](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local):
o app monta um JSON de checkout, abre `lio://payment?...` via `Intent`, e
recebe o retorno em `order://response` numa `Activity` dedicada
(`CieloResponseActivity`, em `core-payment-cielo`). Essa ponte
Activity-assíncrona é traduzida para uma função `suspend` única
(`CieloPaymentGateway.charge()`) via um `SharedFlow` de correlação — assim
`feature-payment` nunca lida com Intents ou Activities, só com
`CieloChargeResult`. Detalhe completo em `docs/ARCHITECTURE.md`.

**Antes de rodar contra o emulador**, preencha `Client-Id`/`Access-Token`
(gerados no [Portal do Desenvolvedor Cielo](https://desenvolvedores.cielo.com.br/api-portal/myapps))
como `CIELO_CLIENT_ID`/`CIELO_ACCESS_TOKEN` no `local.properties` (gitignorado).

## Build types e deploy

`debug` (sufixo `.dev`, sem minificação) e `release` (ProGuard/R8 +
`isShrinkResources` + assinatura via `keystore.properties`, gitignorado —
template em `keystore.properties.example`). Detalhe completo, incluindo
por que `kotlinx.serialization` precisa de regra manual de ProGuard, em
`docs/ARCHITECTURE.md#deploy`.

## Trade-offs considerados

Ver `docs/ARCHITECTURE.md#trade-offs-assumidos`.

## O que faria com mais tempo

1. Fluxo de cancelamento (`lio://payment-reversal`) para a tela de comprovante.
2. Testes instrumentados (Compose UI tests) para os fluxos de seleção e pagamento.
3. `SavedStateHandle`/rotas tipadas no NavHost para sobreviver a process death no meio do pagamento (crítico aqui: o app perde foco enquanto a Cielo Smart está em primeiro plano) — hoje `selectedEvent`/`currentOrder`/`currentReceipt` sobrevivem a recomposição via `remember`, mas não a um `Activity` recriado do zero pelo sistema.
4. Tela de histórico de compras lendo `PurchaseAttemptDao.history()` (já existe no DAO, falta a UI).
5. CI (Azure Pipelines ou GitHub Actions) rodando lint + testes por módulo em paralelo, aproveitando a modularização.
