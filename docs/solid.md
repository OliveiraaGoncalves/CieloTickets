# Princípios SOLID no Desenvolvimento Android com Kotlin: Guia Definitivo

## 1. Visão Geral
Os princípios **SOLID** formam a base da orientação a objetos limpa e sustentável. No ecossistema Android moderno (Kotlin, Jetpack Compose, KMP), aplicar o SOLID evita o temido acoplamento estrito com o Android SDK e facilita drasticamente a criação de **testes unitários**.

---

## 2. S - Single Responsibility Principle (Princípio da Responsabilidade Única)
> *"Uma classe deve ter apenas um motivo para mudar."*

### Contexto Android:
Evite a criação de Activities, Fragments ou ViewModels "God Objects" que gerenciam UI, salvam dados em banco, fazem chamadas de rede e calculam regras de negócio simultaneamente.

### Exemplo em Kotlin (Errado vs Certo):

```kotlin
// ERRADO: ViewModel fazendo requisição de rede, tratando banco e calculando imposto
class BadCheckoutViewModel : ViewModel() {
    fun processCheckout(cart: Cart) {
        // Regra de negócio misturada com cálculo de imposto e chamada Retrofit
        val tax = cart.items.sumOf { it.price } * 0.15
        val total = cart.items.sumOf { it.price } + tax
        // Salva banco e chama API...
    }
}
```

```kotlin
// CERTO: Responsabilidades segregadas
class CalculateCheckoutUseCase {
    operator fun invoke(cart: Cart): CheckoutResult {
        val tax = cart.items.sumOf { it.price } * 0.15
        val total = cart.items.sumOf { it.price } + tax
        return CheckoutResult(total, tax)
    }
}

class CheckoutViewModel(
    private val calculateCheckoutUseCase: CalculateCheckoutUseCase,
    private val processPaymentUseCase: ProcessPaymentUseCase
) : ViewModel() {
    // ViewModel focado estritamente em gerenciar estado e coordenar UseCases
}
```

---

## 3. O - Open/Closed Principle (Princípio Aberto/Fechado)
> *"Entidades devem estar abertas para extensão, mas fechadas para modificação."*

### Contexto Android:
Adicionar novos tipos de pagamento ou fontes de dados sem precisar reescrever as classes existentes, utilizando polimorfismo e interfaces.

### Exemplo em Kotlin:

```kotlin
interface PaymentGateway {
    suspend fun charge(amount: Double): Boolean
}

class CreditCardGateway : PaymentGateway {
    override suspend fun charge(amount: Double): Boolean = true
}

class PixGateway : PaymentGateway {
    override suspend fun charge(amount: Double): Boolean = true
}

// Fechado para modificação quando um novo método de pagamento surgir
class ProcessPaymentUseCase(
    private val paymentGateway: PaymentGateway
) {
    suspend operator fun invoke(amount: Double) = paymentGateway.charge(amount)
}
```

---

## 4. L - Liskov Substitution Principle (Princípio da Substituição de Liskov)
> *"Objetos em um programa devem ser substituíveis por instâncias de seus subtipos sem alterar a correção do programa."*

### Contexto Android:
Evitar contratos quebrados em repositórios ou fontes de dados. Se uma interface promete retornar uma lista, a implementação nunca deve retornar `null` ou lançar exceções inesperadas que quebrem o contrato.

### Exemplo em Kotlin:

```kotlin
interface DataSource {
    suspend fun getData(): List<String>
}

// Implementação válida que cumpre rigorosamente o contrato
class LocalDataSource : DataSource {
    override suspend fun getData(): List<String> = listOf("Item 1", "Item 2")
}

// Implementação que viola LSP lançando UnsupportedOperationException inesperada quebra o consumidor
class ReadOnlyRemoteDataSource : DataSource {
    override suspend fun getData(): List<String> {
        // Violação se o chamador espera escrita ou comportamento consistente
        return emptyList() 
    }
}
```

---

## 5. I - Interface Segregation Principle (Princípio da Segregação de Interfaces)
> *"Muitas interfaces específicas são melhores do que uma única interface genérica."*

### Contexto Android:
Evitar interfaces monstro com 20 métodos onde um ViewModel ou UseCase precisa implementar ou injetar coisas que não utiliza.

### Exemplo em Kotlin:

```kotlin
// ERRADO: Interface única gigante
interface UserRepository {
    suspend fun getUserProfile()
    suspend fun updateAvatar()
    suspend fun deleteAccount()
    suspend fun syncOfflineCache()
    suspend fun clearBiometrics()
}

// CERTO: Interfaces segregadas
interface UserReadRepository {
    suspend fun getUserProfile(): User
}

interface UserWriteRepository {
    suspend fun updateAvatar(url: String)
    suspend fun deleteAccount()
}
```

---

## 6. D - Dependency Inversion Principle (Princípio da Inversão de Dependência)
> *"Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de abstrações."*

### Contexto Android:
O pilar fundamental da Clean Architecture. ViewModels e UseCases dependem de **interfaces**, permitindo o uso de Injeção de Dependência (Hilt / Koin) e testes unitários com mocks perfeitos.

### Exemplo em Kotlin:

```kotlin
// Abstração (Interface)
interface AnalyticsTracker {
    void trackEvent(String eventName, Map<String, Any> params);
}

// Implementação concreta de baixo nível (Firebase)
class FirebaseAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(eventName: String, params: Map<String, Any>) {
        // Firebase specific call
    }
}

// Módulo de alto nível depende da abstração, nunca do Firebase direto
class LoginViewModel(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    fun onLoginClicked() {
        analyticsTracker.trackEvent("login_clicked", emptyMap())
    }
}
```
