# Specs — Cielo Tickets

## Requisitos funcionais cobertos

| # | Requisito                                              | Onde                          |
|---|----------------------------------------------------------|--------------------------------|
| 1 | Visualizar eventos disponíveis                          | feature-home                   |
| 2 | Selecionar quantidade de ingressos                       | feature-ticket-selection       |
| 3 | Iniciar/concluir pagamento via Cielo                     | feature-payment + core-payment-cielo |
| 4 | Registrar resultado (aprovada/negada/cancelada)           | feature-payment (PurchaseAttempt) |
| 5 | Exibir comprovante/resumo                                 | feature-receipt                |
| — | QR Code vinculado à compra concluída (opcional)            | feature-receipt (QrCodeGenerator) |

## Requisitos não-funcionais cobertos

| Requisito                                    | Onde |
|-----------------------------------------------|------|
| Tratamento explícito de erros de integração/pagamento | `DomainError` (core-common) + `try/catch` mapeado em `EventRepositoryImpl` e `ProcessPaymentUseCase` |
| Evitar duplicidade de cobrança em reenvio      | `idempotencyKey` + `PurchaseAttemptDao` + trava de UI em `PaymentViewModel` |
| Código organizado e de fácil manutenção        | Clean Architecture + módulos core/feature isolados + Hilt para DI |
| Testes automatizados para cenários críticos    | `HomeViewModelTest`, `ProcessPaymentUseCaseTest` (JUnit5 + MockK + Turbine) |
| Uso de IA documentado                          | `docs/AI_USAGE.md` |

## Fora de escopo (assumido conforme o case permite)

- Backend de apoio: não construído — o catálogo de eventos é local
  (`EventDao` + `SeedEvents`, populado uma única vez no Room), conforme
  pedido em `docs/desafio.md` (CT-01).
- SDK real da Cielo Smart: não vinculado (não distribuído via Maven
  público); abstraído atrás de `CieloPaymentGateway` com implementação fake
  compatível com o emulador.
