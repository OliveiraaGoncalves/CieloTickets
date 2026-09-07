# Testes Unitários no Android: JUnit 5, MockK e Turbine (Guia Definitivo)

## 1. Visão Geral e Estratégia
No desenvolvimento Android moderno, a testabilidade é uma consequência natural da **Clean Architecture** e do uso de Injeção de Dependência. Como o Domínio e a Apresentação (ViewModels) são isolados de dependências nativas de UI e frameworks, podemos executar **testes unitários puros na JVM** (sem necessidade de emuladores ou dispositivos físicos) em frações de segundo.

Neste guia, cobriremos a stack padrão de mercado:
- **JUnit 5:** Framework de testes moderno com suporte a asserções avançadas, aninhamento e parâmetros.
- **MockK:** A biblioteca de mock mais idiomática e poderosa para Kotlin (suportando `val`, `object`, coroutines e `every`).
- **Turbine:** Biblioteca do Cash App ideal para testar `StateFlow` e `Flow` de forma síncrona e segura.

---

## 2. Configuração do Ambiente (`build.gradle.kts`)

No seu módulo de testes unitários (`test`), certifique-se de incluir as dependências essenciais:

```kotlin
dependencies {
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    
    // MockK
    testImplementation("io.mockk:mockk:1.13.10")
    
    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    
    // Turbine para Flows
    testImplementation("app.cash.turbine:turbine:1.1.0")
    
    // Truth ou JUnit assertions
    testImplementation("com.google.truth:truth:1.4.2")
}
```

---

## 3. Testando UseCases / Regras de Negócio

Vamos testar o `GetUserProfileUseCase` que criamos na nossa Clean Architecture. O objetivo é validar se o UseCase lida corretamente com IDs inválidos e se repassa o resultado do repositório com sucesso.

### Código de Teste com JUnit 5 e MockK:

```kotlin
package com.example.domain.usecase

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetUserProfileUseCaseTest {

    // Declaração do Mock do Repositório
    private lateinit var userRepository: UserRepository
    
    // Instância da classe sob teste (SUT - System Under Test)
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase

    @BeforeEach
    fun setUp() {
        // Inicializa o mock antes de cada teste
        userRepository = mockk()
        getUserProfileUseCase = GetUserProfileUseCase(userRepository)
    }

    @Test
    fun `quando userId for em branco, deve retornar failure com IllegalArgumentException`() = runTest {
        // Given (Dado um ID em branco)
        val blankId = ""

        // When (Executando a função)
        val result = getUserProfileUseCase(blankId)

        // Then (Verificando o resultado)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        
        // Garante que o repositório NUNCA foi chamado para poupar recursos
        coVerify(exactly = 0) { userRepository.getUserProfile(any()) }
    }

    @Test
    fun `quando userId for valido e repositorio retornar sucesso, deve retornar User`() = runTest {
        // Given
        val userId = "user_123"
        val mockUser = User(id = userId, name = "Raimundo", email = "test@email.com", isPremium = true)
        
        // Configurando o comportamento do Mock (coEvery para funções suspend)
        coEvery { userRepository.getUserProfile(userId) } returns Result.success(mockUser)

        // When
        val result = getUserProfileUseCase(userId)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(mockUser)
        
        // Verifica se o repositório foi chamado exatamente 1 vez com o ID correto
        coVerify(exactly = 1) { userRepository.getUserProfile(userId) }
    }
}
```

---

## 4. Testando ViewModels com Coroutines e Turbine (`StateFlow`)

Testar ViewModels exige o gerenciamento do despachador principal (*Main Dispatcher*), já que as Coroutines rodam assincronamente. Utilizamos `StandardTestDispatcher` e a biblioteca **Turbine** para testar as emissões sequenciais do `StateFlow`.

### Regra de Extensão para Coroutines em Testes:
Para evitar configurar o `Dispatchers.setMain` em todos os testes, criamos uma extensão ou regra JUnit.

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MainCoroutineExtension(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
```

### Implementação do Teste da ViewModel:

```kotlin
package com.example.presentation.login

import app.cash.turbine.test
import com.example.domain.usecase.LoginUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LoginViewModelTest {

    // Registra a extensão JUnit 5 para o Dispatcher Main
    @JvmField
    @RegisterExtension
    val coroutineExtension = MainCoroutineExtension()

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        loginUseCase = mockk()
        viewModel = LoginViewModel(loginUseCase)
    }

    @Test
    fun `quando digitar email e senha validos, o botao deve ser habilitado`() = runTest {
        // Usando o Turbine para coletar os estados emitidos pelo StateFlow
        viewModel.uiState.test {
            // Estado inicial padrão
            val initialState = awaitItem() as LoginUiState.Initial
            assertThat(initialState.isButtonEnabled).isFalse()

            // When: Mudando o email
            viewModel.onIntent(LoginUiIntent.EmailChanged("raimundo@email.com"))
            
            // Then: O estado atualiza, mas o botão continua false (falta senha)
            val stateAfterEmail = awaitItem() as LoginUiState.Initial
            assertThat(stateAfterEmail.email).isEqualTo("raimundo@email.com")
            assertThat(stateAfterEmail.isButtonEnabled).isFalse()

            // When: Mudando a senha
            viewModel.onIntent(LoginUiIntent.PasswordChanged("secure_pass_123"))

            // Then: Botão agora deve estar habilitado
            val stateAfterPass = awaitItem() as LoginUiState.Initial
            assertThat(stateAfterPass.pass).isEqualTo("secure_pass_123")
            assertThat(stateAfterPass.isButtonEnabled).isTrue()
        }
    }

    @Test
    fun `quando submeter login com sucesso, deve emitir estados Loading e Success`() = runTest {
        // Given
        coEvery { loginUseCase(any(), any()) } returns Result.success(Unit)

        viewModel.uiState.test {
            skipItems(1) // Ignora o estado inicial

            // Preenche dados para habilitar e submeter
            viewModel.onIntent(LoginUiIntent.EmailChanged("test@email.com"))
            skipItems(2) // Pula atualizações intermediárias de digitação

            viewModel.onIntent(LoginUiIntent.PasswordChanged("123456"))
            skipItems(1)

            // When: Clica em submeter
            viewModel.onIntent(LoginUiIntent.SubmitClicked)

            // Then: Deve emitir Loading
            assertThat(awaitItem()).isEqualTo(LoginUiState.Loading)

            // Then: Deve emitir Success após o sucesso do UseCase
            assertThat(awaitItem()).isEqualTo(LoginUiState.Success)
        }
    }
}
```

---

## 5. Melhores Práticas para Testes Unitários Mobile

1. **Apenas Testes Unitários:** Conforme diretrizes de arquitetura, evite testes instrumentados pesados (Espresso/UI) quando a lógica puder ser testada de forma unitária no ViewModel e nos UseCases.
2. **Nomes Descritivos:** Utilize nomes descritivos em português ou inglês no formato `quando [cenario], deve [resultado esperada]` para facilitar a leitura de relatórios de CI/CD.
3. **Isolamento Total:** Garanta que os testes não dependam de ordem de execução ou de recursos externos de rede/banco de dados reais. Sempre utilize **MockK** para isolar as dependências.
