package br.com.cielotickets.core.paymentcielo

import javax.inject.Inject

/**
 * Client-Id e Access-Token obtidos no Portal do Desenvolvedor Cielo
 * (https://desenvolvedores.cielo.com.br/api-portal/myapps) ao criar uma
 * credencial para a API Local / Cielo Smart.
 *
 * NUNCA commitar valores reais — vêm de `BuildConfig`, alimentado a partir
 * de `local.properties` (gitignorado) ou variável de ambiente/CI, nunca
 * hardcoded no fonte (ver `core-payment-cielo/build.gradle.kts`).
 */
data class CieloCredentials(
    val clientId: String,
    val accessToken: String
)

interface CieloCredentialsProvider {
    fun get(): CieloCredentials
}

/** Lê as credenciais do `BuildConfig` — placeholder óbvio se nada for configurado. */
class DevCieloCredentialsProvider @Inject constructor() : CieloCredentialsProvider {
    override fun get() = CieloCredentials(
        clientId = BuildConfig.CIELO_CLIENT_ID,
        accessToken = BuildConfig.CIELO_ACCESS_TOKEN
    )
}
