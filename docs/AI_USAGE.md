# Uso de IA no desenvolvimento — Cielo Tickets

O case pede explicitamente para documentar "o como" a IA foi usada,
especialmente no sistema de reserva/pagamento de ingresso.

## Onde a IA (Claude) foi usada

1. **Integração Cielo Smart via Deeplink**: a partir do sample oficial da
   Cielo ([LIO-SDK-Sample-Integracao-Local](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local)),
   a IA ajudou a traduzir um fluxo baseado em `Intent`/callback entre Activities
   (inerentemente assíncrono e "cross-app") para uma função `suspend` única
   (`CieloPaymentGateway.charge()`) que o resto do app já esperava — via um
   `SharedFlow` singleton (`CieloDeeplinkResultBus`) fazendo a ponte entre a
   `ResponseActivity` (que recebe o callback) e a coroutine suspensa que
   originou o pagamento, correlacionando pelo campo `reference` (nossa
   `idempotencyKey`). Essa tradução de padrão foi decidida em conjunto, não
   copiada do sample — o sample da Cielo é Activity-based, nosso `domain`
   precisa continuar agnóstico de Android.

1. **Desenho do módulo de anti-duplicidade** (`ProcessPaymentUseCase` +
   `PurchaseAttemptDao`): a IA foi usada para explorar o trade-off entre
   (a) deduplicar só no cliente via chave local, (b) deduplicar só via
   header HTTP, e (c) as duas camadas combinadas — chegando à abordagem
   combinada descrita em `docs/ARCHITECTURE.md`, priorizando o caso mais
   comum em campo (duplo toque / perda de conexão no meio do pagamento).
2. **Estrutura de módulos Gradle** (core/feature, version catalog,
   `settings.gradle.kts`): gerada com apoio de IA para garantir a regra
   "core nunca conhece feature" desde a raiz do projeto, evitando ciclos de
   dependência que quebrariam o paralelismo do Gradle.
3. **Scaffolding de cada feature** (domain/data/presentation/di): a IA
   gerou o esqueleto repetitivo (mapeamento Entity→domínio, `@HiltViewModel`,
   módulos Hilt com `@Binds`/`@Provides` só onde havia interface para
   bindar) seguindo o mesmo padrão em todas as features, para reduzir
   divergência de estilo entre elas. A migração inicial de Koin para Hilt e
   de um `EventApi` (Retrofit, sem backend real por trás) para um catálogo
   de eventos local via Room seguiu o mesmo processo: a IA aplicou o
   padrão `@Inject constructor` nas classes concretas e `@Module` só onde
   uma interface de domínio precisava de binding, revisado módulo a módulo
   até o build compilar e os testes passarem.
4. **Testes de exemplo**: a IA sugeriu os testes considerados mais críticos
   pelo enunciado — fluxo feliz e de erro do Home, não-duplicidade do
   pagamento, recálculo de total na seleção de ingressos (CT-02) e a
   auditoria de sequência de `StateFlow` do pagamento (Idle→Processing→
   Success/Failed, incluindo o clique duplo do CT-05 e o cancelamento
   manual) — como ponto de partida pra suíte, hoje com JUnit5+MockK+Turbine
   nos 4 módulos que têm ViewModel/UseCase com lógica real.
5. **Centralização de build** (`build-logic/convention`): a IA identificou
   o boilerplate repetido de `android {}`/Compose/Hilt/testes em cada
   `build.gradle.kts` e propôs plugins de convenção — descoberta e corrigida
   no processo uma limitação real do Gradle 9 (accessor tipado do catálogo
   quebra quando o módulo também aplica um plugin de um *included build*),
   documentada em `docs/ARCHITECTURE.md`.
6. **Bugs encontrados via teste manual, não só revisão de código**: ao
   simular os cenários de negado/cancelado no emulador, apareceram dois
   bugs reais que a leitura do código sozinha não teria pego — (a) o
   curto-circuito de idempotência tratava `DENIED`/`CANCELLED` como
   "já resolvido" e never re-tentava a cobrança no "Tentar novamente"; (b)
   um payload de erro/cancelamento da Cielo Smart era decodificado como
   `Malformed` (silenciosamente descartado) em vez de `Error`, porque
   `CieloOrderResponse` só tem campos opcionais e engolia o parse sem
   lançar exceção. Os dois viraram teste de regressão.
7. **Deploy (build types + ProGuard)**: a IA sinalizou um risco específico
   deste projeto — os modelos `@Serializable` do checkout Cielo Smart
   dependem de reflection no serializer gerado, e minificação sem regra
   manual de ProGuard poderia quebrar o contrato JSON só em build de
   release, nunca em debug. As regras ficaram documentadas com o porquê,
   não só copiadas do guia oficial do `kotlinx.serialization`.

## Restrições aplicadas ao uso da IA

- Nenhuma chamada real à Cielo Smart foi assumida sem visibilidade da doc
  oficial — por isso a integração ficou atrás de uma interface com
  implementação fake, em vez de "inventar" o contrato do SDK.
- Toda sugestão de código gerada foi revisada manualmente quanto a:
  aderência à regra core/feature, ausência de vazamento de Android
  framework na camada `domain`, e cobertura do requisito de anti-duplicidade
  antes de aceitar o trecho.
- Documentação de arquitetura (`ARCHITECTURE.md`) e este arquivo foram
  escritos para refletir decisões reais tomadas durante a sessão, não como
  texto genérico pós-fato.

## O que faria diferente com mais tempo

Ver seção "O que faria com mais tempo" no `README.md` — a IA foi usada
para priorizar essa lista pelo impacto no requisito de code review do case
(decisões técnicas e trade-offs, que é justamente o que será avaliado).
