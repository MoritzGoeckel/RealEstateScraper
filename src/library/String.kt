package library

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