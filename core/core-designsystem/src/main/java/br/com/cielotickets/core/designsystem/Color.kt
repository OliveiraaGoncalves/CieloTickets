package br.com.cielotickets.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta inspirada na identidade visual azul da Cielo (não temos acesso ao
 * brand book oficial neste ambiente — troque pelos hex exatos se tiver o
 * kit de marca à mão). Escala numérica: quanto maior o número, mais escuro.
 */
object CieloColors {
    val Blue900 = Color(0xFF001C3A)
    val Blue800 = Color(0xFF00325C)
    val Blue700 = Color(0xFF00497F)
    val Blue600 = Color(0xFF0057B8) // âncora da marca
    val Blue500 = Color(0xFF2E7BD6)
    val Blue300 = Color(0xFF9DC7F5)
    val Blue100 = Color(0xFFD6E6FA)
    val Blue50 = Color(0xFFF0F6FD)

    val Amber700 = Color(0xFF4A2800)
    val Amber600 = Color(0xFF6B3C00)
    val Amber500 = Color(0xFFFF8A00) // accent / CTA secundário
    val Amber300 = Color(0xFFFFB877)
    val Amber100 = Color(0xFFFFE3C2)

    val Green700 = Color(0xFF00390F)
    val Green500 = Color(0xFF1E8E3E) // aprovado
    val Green300 = Color(0xFF8BD99C)

    val Red700 = Color(0xFF7E0000)
    val Red500 = Color(0xFFBA1A1A) // negado / erro
    val Red300 = Color(0xFFFFB4AB)

    val Neutral900 = Color(0xFF10131A)
    val Neutral800 = Color(0xFF1A1E27)
    val Neutral700 = Color(0xFF262B36)
    val Neutral600 = Color(0xFF333947)
    val Neutral500 = Color(0xFF6B7280)
    val Neutral300 = Color(0xFFC7CCD6)
    val Neutral200 = Color(0xFFDEE2E8)
    val Neutral100 = Color(0xFFEDF0F4)
    val Neutral50 = Color(0xFFF7F8FA)
}
