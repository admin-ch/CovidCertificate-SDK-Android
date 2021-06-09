package ch.admin.bag.covidcertificate.eval.nationalrules.certlogic

enum class TimeUnit(name: String) {
	DAY(name = "day"),
	HOUR(name = "hour")
}

/**
 * Returns `true` if enum T contains an entry with the specified name.
 */
inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
	return enumValues<T>().any { it.name == name }
}