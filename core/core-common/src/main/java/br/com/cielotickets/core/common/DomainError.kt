package br.com.cielotickets.core.common

sealed class DomainError {
    data class Network(val cause: Throwable) : DomainError()
    data class Timeout(val cause: Throwable) : DomainError()
    data class PaymentDenied(val reasonCode: String?) : DomainError()
    data class PaymentCancelled(val byUser: Boolean) : DomainError()
    data class DuplicateTransaction(val idempotencyKey: String) : DomainError()
    data class Serialization(val cause: Throwable) : DomainError()
    data class Unknown(val cause: Throwable? = null, val message: String? = null) : DomainError()
}