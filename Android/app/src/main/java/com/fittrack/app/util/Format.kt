package com.fittrack.app.util

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
