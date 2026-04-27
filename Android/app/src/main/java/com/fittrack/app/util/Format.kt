package com.fittrack.app.util

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "60.00" → "60", "62.50" → "62.5". */
fun formatWeight(weight: String): String {
    val d = weight.toDoubleOrNull() ?: return weight
    return formatWeight(d)
}

fun formatWeight(weight: Double): String =
    if (weight == weight.toLong().toDouble()) weight.toLong().toString()
    else weight.toString()

/** Same as [formatWeight] but with a " kg" suffix for display rows. */
fun formatWeightKg(weight: String): String = "${formatWeight(weight)} kg"

/** dp → px for use with View.setPadding/etc. */
fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/** "2026-04-22" → "Apr 22, 2026". Falls back to the input on parse failure. */
fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DATE_FORMATTER)
} catch (_: Exception) {
    iso
}
