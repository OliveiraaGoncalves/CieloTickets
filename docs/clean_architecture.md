# Clean Architecture no Android: Guia Definitivo e Base de Conhecimento

## 1. Introdução e Visão Geral
A **Clean Architecture** (proposta por Robert C. Martin / Uncle Bob) aplicada ao desenvolvimento Android visa criar sistemas que sejam:
- Independentes de frameworks (Android SDK, bibliotecas de UI, bancos de dados).
- Altamente testáveis (lógica de negócio testada sem emuladores ou contextos de UI).
- Independentes de UI (a interface pode mudar de Jetpack Compose para Views sem tocar na lógica).
- Independentes de Banco de Dados ou APIs externas.

### O Princípio da Dependência (Dependency Inversion)
O código fonte só pode apontar para dentro. As camadas externas conhecem as camadas internas, mas **as camadas internas nunca sabem nada sobre o que está fora**.

```
   [ UI / Framework (Android) ]
         v (conhece)
   [ Presentation (ViewModel) ]
         v (conhece)
   [ Domain (UseCases / Models) ] <--- Core (Regra de Negócio Pura)
         ^ (implementa interfaces de)
   [ Data (Repositories / Network / DB) ]
```

---

## 2. Divisão das Camadas

### A. Camada de Domínio (`Domain`)
É o coração da aplicação. **Não contém nenhuma dependência do Android SDK** (nem mesmo `android.util.Log` ou `Context`). Deve ser pura Kotlin.
- **Entities:** Objetos de negócio puros (data classes com regras de validação ou estado intrínseco).
- **UseCases / Interactors:** Representam ações/casos de uso específicos da aplicação (ex: `AuthenticateUserUseCase`, `CalculateCartTotalUseCase`). Cada UseCase deve ter uma única responsabilidade (SRP).
- **Repositories (Interfaces):** Contratos abstratos definindo o que a camada de dados precisa fornecer, invertendo a dependência.

### B. Camada de Dados (`Data`)
Responsável por buscar, persistir e sincronizar os dados de fontes externas (APIs, Banco de Dados, Cache).
- **Models / DTOs:** Estruturas de dados mapeadas da API (`ApiResponse`) ou do Banco (`Entity` do Room).
- **Data Sources:** Implementações concretas (`RemoteDataSource`, `LocalDataSource`).
- **Repositories (Implementações):** Classes que implementam as interfaces do Domínio, decidindo a estratégia de dados (ex: *Offline-First* combinando Room e Ktor).

### C. Camada de Apresentação (`Presentation`)
Responsável por exibir os dados na tela e capturar as intenções do usuário.
- **ViewModels:** Mantêm o estado da UI e executam os UseCases do domínio.
- **UI (Jetpack Compose / Activities / Fragments):** Camada puramente visual que consome o estado de forma reativa (StateFlow / Compose State).

---

## 3. Exemplo Prático com Kotlin (Caso de Uso: Obter Perfil do Usuário)

### 1. Domain Layer (`domain/model` e `domain/usecase`)

```kotlin
package com.example.domain.model

// Entidade de Negócio Pura
data class User(
    val id: String,
    val name: String,
    val email: String,
    val isPremium: Boolean
) {
    fun getFormattedGreeting(): = "Olá, $name! Seja bem-vindo."
}
```

```kotlin
package com.example.domain.repository

import com.example.domain.model.User

// Contrato (Interface) definido no Domínio
interface UserRepository {
    suspend fun getUserProfile(userId: String): Result<User>
}
```

```kotlin
package com.example.domain.usecase

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

// Caso de Uso Isolado (Single Responsibility)
class GetUserProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de usuário inválido"))
        }
        return userRepository.getUserProfile(userId)
    }
}
```

---

### 2. Data Layer (`data/dto` e `data/repository`)

```kotlin
package com.example.domain.data.dto

import com.example.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val fullName: String,
    val mail: String,
    val subscriptionStatus: String
) {
    // Mapper de DTO para Domain Entity
    fun toDomain(): User = User(
        id = id,
        name = fullName,
        email = mail,
        isPremium = subscriptionStatus == "PREMIUM"
    )
}
```

```kotlin
package com.example.domain.data.repository

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import com.example.domain.data.dto.UserDto

// Implementação concreta da interface do Domínio
class UserRepositoryImpl(
    private val apiService: UserApiService,
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            // Estratégia simples: tenta rede, fallback para cache local ou vice-versa
            val response = apiService.fetchUser(userId)
            val user = response.toDomain()
            
            // Salva localmente para offline-first
            userDao.insertUser(UserEntity.fromDomain(user))
            
            Result.success(user)
        } catch (e: Exception) {
            // Fallback para cache local em caso de falha de rede
            val localUser = userDao.getUser(userId)
            if (localUser != null) {
                Result.success(localUser.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }
}
```

---

### 3. Presentation Layer (`presentation/viewmodel`)

```kotlin
package com.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.User
import com.example.domain.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UserUiState {
    object Idle : UserUiState
    object Loading : UserUiState
    data class Success(val user: User, val greeting: String) : UserUiState
    data class Error(val message: String) : UserUiState
}

class UserViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { UserUiState.Loading }
            
            val result = getUserProfileUseCase(userId)
            
            result.fold(
                onSuccess = { user ->
                    _uiState.update { 
                        UserUiState.Success(
                            user = user, 
                            greeting = user.getFormattedGreeting()
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        UserUiState.Error(error.localizedMessage ?: "Erro desconhecido") 
                    }
                }
            )
        }
    }
}
```
