package library

fun <T> Collection<T>.firstOrDefault(default: () -> T): T {
    return firstOrNull() ?: default.invoke();
}