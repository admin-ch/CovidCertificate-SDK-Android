package ch.admin.bag.covidcertificate.eval.nationalrules.certlogic

enum class TimeUnit(val identifier: String) {
	DAY(identifier = "day"),
	HOUR(identifier = "hour");

	companion object {
		fun fromName(identifier: String): TimeUnit {
			return values().find {
				it.identifier == identifier
			} ?: throw IllegalArgumentException("No TimeUnit enum constant with identifier $identifier")
		}
	}
}

/**
 * Returns `true` if enum T contains an entry with the specified name.
 */
inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
	return enumValues<T>().any { it.name == name }
}