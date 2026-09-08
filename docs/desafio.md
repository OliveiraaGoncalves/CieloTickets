# TicketFlow Android - App de Venda de Ingressos com Integração Cielo Smart

Solução desenvolvida como parte do desafio técnico para **Desenvolvedor(a) Mobile Android**, estruturada com foco em arquitetura limpa, alta testabilidade, resiliência em pagamentos e integração com o ecossistema Cielo Lio / Smart.

---

## 🏗️ 1. Decisões Arquiteturais e Stack Tecnológica

O projeto foi construído seguindo rigorosamente os padrões de engenharia de software mobile de nível sênior:

* **Clean Architecture:** Separação estrita de responsabilidades em camadas (`Domain`, `Data`, `Presentation`) para garantir independência de frameworks e alta testabilidade.
* **MVVM + UDF (Unidirectional Data Flow):** Gerenciamento de estado reativo e previsível utilizando **Jetpack Compose** e `StateFlow` com `collectAsStateWithLifecycle()`.
* **Linguagem:** 100% **Kotlin**, com uso extensivo de Coroutines e Flows assíncronos.
* **Injeção de Dependência:** Hilt para gerenciamento limpo do ciclo de vida das instâncias.
* **Testabilidade:** Testes unitários puros na JVM utilizando **JUnit 5**, **MockK** e **Turbine** para fluxos reativos.
* **Consumo de API REST:** catálogo de eventos via [mockapi.io](https://mockapi.io) com Retrofit/OkHttp, cache offline-first em Room — a rede é a fonte de verdade, o cache local só cobre indisponibilidade.

---

## 💳 2. Integração com a Cielo Smart (Lio)

A integração com o ecossistema de pagamentos da Cielo foi projetada considerando cenários de falhas de rede, resiliência e experiência do usuário:

* **Abstração via Interface:** O SDK da Cielo é isolado por trás de um contrato (`PaymentGateway`), permitindo mockar facilmente o comportamento de pagamento em testes unitários e no ambiente de desenvolvimento sem depender fisicamente da maquininha.
* **Prevenção de Dupla Cobrança (Idempotência / UI Lock):** Para atender aos requisitos não-funcionais, a camada de UI implementa bloqueio imediato do botão de pagamento e exibe um indicador de carregamento (`Loading State`) logo no primeiro toque. Isso impede cliques múltiplos e reenvios acidentados de transações.
* **Tratamento de Estados:** Tratamento explícito para os três retornos possíveis da adquirente:
  1. **Aprovada:** Conclusão da venda e geração do QR Code do ingresso vinculado.
  2. **Negada:** Mensagem amigável ao operador, permitindo tentar novamente sem perder o carrinho.
  3. **Cancelada/Timeout:** Retorno seguro para o resumo da compra mantendo o estado anterior.

---

## 🧪 3. Plano Estruturado de Testes

O projeto conta com uma suíte de testes robusta cobrindo cenários críticos de negócio e integração:

### A. Matriz de Casos de Teste Funcionais
| ID | Cenário / Funcionalidade | Passos de Execução | Resultado Esperado | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| **CT-01** | Listagem de Eventos | Abrir a tela inicial do app. | Exibe eventos (via mockapi.io, com cache local offline-first) com título, data e preço corretos. | Unitário / UI |
| **CT-02** | Seleção de Ingressos | Selecionar quantidade (ex: 2 unidades). | Recalcula o valor total de forma precisa no ViewModel. | Unitário |
| **CT-03** | Pagamento Aprovado | Acionar pagamento via Cielo (Simulador Sucesso). | Transação concluída, salvamento e exibição do comprovante/QR Code. | Integração |
| **CT-04** | Pagamento Negado | Simular cartão negado no emulador Cielo. | Exibe alerta de erro sem quebrar o fluxo; permite nova tentativa. | Tratamento de Erro |
| **CT-05** | Prevenção de Duplicidade | Pressionar o botão de pagamento rapidamente várias vezes. | Primeiro clique já troca a tela pro estado `Processing` (spinner + botão "Cancelar"), removendo o botão de pagar — segundo clique é ignorado no ViewModel mesmo que a UI ainda reagisse. | Não-Funcional |

### B. Testes Automatizados (JUnit 5 + MockK + Turbine)
* **UseCases (Domínio):** Validação de regras de negócio puras e tratamento de parâmetros inválidos.
* **ViewModels (Apresentação):** Auditoria de emissões de `StateFlow` sequenciais (ex: `Initial` -> `Loading` -> `Success` / `Error`).

---

## 🤖 4. Uso de Inteligência Artificial no Desenvolvimento

A IA foi utilizada de forma estratégica como copiloto de desenvolvimento (*AI-Assisted Engineering*):
* **Geração de Boilerplate e Estrutura:** Aceleração na criação de arquiteturas limpas, mapeadores DTO-Domain e esqueletos de classes com sealed interfaces.
* **Refinamento de Casos de Borda:** Consultas para assegurar tratamento de concorrência em botões de pagamento e melhores práticas para testes com Turbine e Coroutines.
* **Prompt Principal Utilizado:** *"Atue como um Engenheiro Mobile Sênior especialista em Android e estruture Clean Architecture, MVVM com StateFlow e tratamento robusto de concorrência para transações de pagamento."*

---

## 🚀 5. Instruções de Execução

1. **Pré-requisitos:**
   * Android Studio (Jellyfish ou superior recomendado).
   * JDK 17 configurado.
   * [Emulador Cielo Smart instalado](https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo) para testes de pagamento.
2. **Passos para rodar:**
   * Clone o repositório para sua máquina local.
   * Abra o projeto no Android Studio.
   * Sincronize o Gradle (`Sync Project with Gradle Files`).
   * Conecte um dispositivo físico Cielo ou execute o emulador configurado.
   * Execute a aplicação (`Shift + F10`).

---

## 📋 6. Trade-offs e O Que Faria com Mais Tempo

* **Trade-off (Persistência):** Utilizou-se Room como cache offline-first do catálogo (fonte de verdade é o mockapi.io) e como única fonte de verdade da trilha de idempotência de pagamento, em vez de depender exclusivamente de memória volátil.
* **Testes instrumentados (E2E) com Compose UI Test:** implementados em `app/src/androidTest` — cobrem a jornada completa (Home → Seleção → Pagamento → Comprovante) via `PaymentGateway` fake controlável por teste, sem depender do emulador físico da Cielo Smart. Ver `docs/ARCHITECTURE.md#instrumented-tests`.
* **Sobrevivência a `process death`:** rotas tipadas (Navigation-Compose 2.8) + `SavedStateHandle` — nenhum dado de domínio fica em `remember` no `NavHost`, e a `idempotencyKey` do pagamento sobrevive à recriação do processo (fecha uma janela real de cobrança duplicada). Ver `docs/ARCHITECTURE.md#process-death`.
* **CI ativo:** `.github/workflows/ci.yml` roda build+lint+testes unitários e instrumentados a cada push/PR pra `main` no repositório publicado.
* **Escopo extra:** histórico de compras (`feature-history`) — não pedido pelo case original, adicionado por completude seguindo o mesmo padrão arquitetural das demais features.
* **Com mais tempo:** rodaria a suíte instrumentada em mais de uma configuração de tela/densidade via Firebase Test Lab ou similar, e faria o upgrade de toolchain (AGP/Kotlin/Hilt/Compose BOM/Room) deliberadamente adiado hoje (ver `gradle/libs.versions.toml`).
