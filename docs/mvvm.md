# Padrão MVVM (Model-View-ViewModel) no Android Moderno: Guia Definitivo

## 1. Visão Geral e Conceito
O **MVVM** é o padrão de arquitetura oficial recomendado pelo Google para aplicações Android. Ele separa a interface do usuário (UI) da lógica de apresentação e dos dados de negócio.

- **Model:** Camada de domínio/dados responsável pelas regras de negócio, repositórios e fontes de dados.
- **View:** A UI declarativa (**Jetpack Compose**) ou Activity/Fragment imperativa responsável por desenhar a tela e coletar eventos do usuário.
- **ViewModel:** Mantém o estado da UI, sobrevive a mudanças de configuração (como rotação de tela) e processa eventos da View comunicando-se com os UseCases.

---

## 2. Fluxo de Dados Unidirecional (UDF - Unidirectional Data Flow)
No Android moderno com Jetpack Compose e Coroutines, o MVVM opera sob o princípio de UDF:
1. O usuário interage com a **View** (clique em botão).
2. A **View** dispara um evento/ação para o **ViewModel** (ex: `viewModel.onAction(UiAction.Submit)`).
3. O **ViewModel** processa a lógica de negócio e atualiza um `StateFlow` imutável.
4. A **View** observa o estado de forma reativa e se recompõe automaticamente.

```
 [ UI (Compose) ] --(Event / Intent)--> [ ViewModel ]
       ^                                      |
       |--------------(StateFlow)-------------|
```

---

## 3. Exemplo Completo com Jetpack Compose e Kotlin

### 1. Definindo o Estado da UI (`UiState`) e Eventos (`UiIntent`)
Utilizar sealed interfaces garante que o estado da tela seja totalmente previsível e seguro em tempo de compilação.

```kotlin
package com.example.presentation.login

// Estados possíveis da tela de Login
sealed interface LoginUiState {
    data class Initial(
        val email: String = "",
        val pass: String = "",
        val isButtonEnabled: Boolean = false
    ) : LoginUiState

    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

// Ações do usuário enviadas à ViewModel
sealed interface LoginUiIntent {
    data class EmailChanged(val email: String) : LoginUiState // ou similar
    data class PasswordChanged(val pass: String) : LoginUiState
    object SubmitClicked : LoginUiIntent
}
```

---

### 2. Implementando o ViewModel

```kotlin
package com.example.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onIntent(intent: LoginUiIntent) {
        when (intent) {
            is LoginUiIntent.EmailChanged -> {
                _uiState.update { current ->
                    if (current is LoginUiState.Initial) {
                        current.copy(
                            email = intent.email,
                            isButtonEnabled = intent.email.isNotBlank() && current.pass.isNotBlank()
                        )
                    } else current
                }
            }
            is LoginUiIntent.PasswordChanged -> {
                _uiState.update { current ->
                    if (current is LoginUiState.Initial) {
                        current.copy(
                            pass = intent.pass,
                            isButtonEnabled = current.email.isNotBlank() && intent.pass.isNotBlank()
                        )
                    } else current
                }
            }
            is LoginUiIntent.SubmitClicked -> {
                performLogin()
            }
        }
    }

    private fun performLogin() {
        val currentState = _uiState.value
        if (currentState !is LoginUiState.Initial) return

        viewModelScope.launch {
            _uiState.update { LoginUiState.Loading }

            val result = loginUseCase(currentState.email, currentState.pass)

            result.fold(
                onSuccess = {
                    _uiState.update { LoginUiState.Success }
                },
                onFailure = { error ->
                    _uiState.update { LoginUiState.Error(error.localizedMessage ?: "Erro ao logar") }
                }
            )
        }
    }
}
```

---

### 3. Consumindo o ViewModel na View (Jetpack Compose)

```kotlin
package com.example.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: LoginViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) {
            when (val state = uiState) {
                is LoginUiState.Initial -> {
                    LoginFormContent(
                        email = state.email,
                        pass = state.pass,
                        isButtonEnabled = state.isButtonEnabled,
                        onEmailChange = { viewModel.onIntent(LoginUiIntent.EmailChanged(it)) },
                        onPassChange = { viewModel.onIntent(LoginUiIntent.PasswordChanged(it)) },
                        onSubmit = { viewModel.onIntent(LoginUiIntent.SubmitClicked) }
                    )
                }
                is LoginUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                }
                is LoginUiState.Success -> {
                    Text("Login efetuado com sucesso!", style = MaterialTheme.typography.headlineMedium)
                }
                is LoginUiState.Error -> {
                    Column(modifier = Modifier.align(androidx.compose.ui.Alignment.Center)) {
                        Text("Erro: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.onIntent(LoginUiIntent.EmailChanged("")) }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginFormContent(
    email: String,
    pass: String,
    isButtonEnabled: Boolean,
    onEmailChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = onPassChange,
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = isButtonEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
    }
}
```
