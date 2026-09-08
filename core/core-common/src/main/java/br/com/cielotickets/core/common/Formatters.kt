package br.com.cielotickets.core.common

import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE_PT_BR = Locale("pt", "BR")
private val brlFormat = NumberFormat.getCurrencyInstance(LOCALE_PT_BR)
private const val TIME_PATTERN = "HH:mm"

fun Long.centsToBrl(): String = brlFormat.format(this / 100.0)

fun String.toFriendlyDateTime(): String = runCatching {
    val dateTime = LocalDateTime.parse(this)
    val weekDay = dateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, LOCALE_PT_BR)
    val month = dateTime.month.getDisplayName(TextStyle.SHORT, LOCALE_PT_BR)
    val time = dateTime.format(DateTimeFormatter.ofPattern(TIME_PATTERN))
    "$weekDay, ${dateTime.dayOfMonth} de $month • $time"
}.getOrDefault(this)