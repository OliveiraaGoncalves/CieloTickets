package br.com.cielotickets.core.common

import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val brlFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

/** Centavos → "R$ 150,00". Puro java.text/java.time — sem framework Android. */
fun Long.centsToBrl(): String = brlFormat.format(this / 100.0)

/** "2026-10-10T20:00:00" → "sáb, 10 de out • 20:00". Cai no valor bruto se o parse falhar. */
fun String.toFriendlyDateTime(): String = runCatching {
    val dateTime = LocalDateTime.parse(this)
    val weekDay = dateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
    val month = dateTime.month.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    "$weekDay, ${dateTime.dayOfMonth} de $month • $time"
}.getOrDefault(this)
