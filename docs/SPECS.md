# Specs — Cielo Tickets

## Requisitos funcionais cobertos

| # | Requisito                                              | Onde                          |
|---|----------------------------------------------------------|--------------------------------|
| 1 | Visualizar eventos disponíveis                          | feature-home + core-network (mockapi.io) |
| 2 | Selecionar quantidade de ingressos                       | feature-ticket-selection       |
| 3 | Iniciar/concluir pagamento via Cielo                     | feature-payment + core-payment-cielo |
| 4 | Registrar resultado (aprovada/negada/cancelada)           | feature-payment (`PurchaseRepository` + `PurchaseAttempt`) |
| 5 | Exibir comprovante/resumo                                 | feature-receipt                |
| — | QR Code vinculado à compra concluída (opcional)            | feature-receipt (QrCodeGenerator) |
| — | Histórico de compras (extra, não pedido pelo case)          | feature-history                |

## Requisitos não-funcionais cobertos

| Requisito                                    | Onde |
|-----------------------------------------------|------|
| Tratamento explícito de erros de integração/pagamento | `DomainError` (core-common) + `try/catch` mapeado em `EventRepositoryImpl` e `ProcessPaymentUseCaseImpl` |
| Evitar duplicidade de cobrança em reenvio      | `idempotencyKey` + `PurchaseRepository` + trava de UI em `PaymentViewModel` + sobrevive a `process death` via `SavedStateHandle` |
| Código organizado e de fácil manutenção        | Clean Architecture (domain/data/presentation/di por feature) + módulos core/feature isolados + Hilt para DI + Repository/UseCase sempre atrás de interface |
| Testes automatizados para cenários críticos    | 8 arquivos de teste unitário (JUnit5 + MockK + Turbine) + 2 arquivos de teste instrumentado — ver `docs/ARCHITECTURE.md#testes-críticos-cobertos` |
| Uso de IA documentado                          | `docs/AI_USAGE.md` |

## Fora de escopo (assumido conforme o case permite)

- SDK real da Cielo Smart: não vinculado (não distribuído via Maven
  público); abstraído atrás de `CieloPaymentGateway` com implementação fake
  compatível com o emulador.
- Múltiplos ambientes (dev/prod) para `API_BASE_URL` (mockapi.io) e
  credenciais Cielo: hoje é `local.properties`/variável de ambiente manual,
  sem product flavor dedicado — ver `docs/ARCHITECTURE.md#deploy`.
