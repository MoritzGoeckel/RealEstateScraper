package library

import structures.Currency

fun String.remove(str: String): String{
    return this.replace(str, "", true)
}

fun String.toDoubleOrNaN(): Double{
    return this.toDoubleOrNull() ?: Double.NaN
}

fun String.parseGermanDouble(): Double{
    return this
        .remove(".")
        .replace(",", ".")
        .trim()
        .toDoubleOrNaN()
}

fun String.toCurrency(): Currency{
    return when(this) {
        "€" -> Currency.EUR
        "$" -> Currency.USD
        else -> Currency.Other
    }
}