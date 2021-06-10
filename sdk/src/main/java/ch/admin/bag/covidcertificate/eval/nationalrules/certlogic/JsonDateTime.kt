package ch.admin.bag.covidcertificate.eval.nationalrules.certlogic

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ValueNode
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class JsonDateTime protected constructor(dateTime: OffsetDateTime) : ValueNode(), Comparable<JsonDateTime?> {
	private val _value: OffsetDateTime
	fun temporalValue(): OffsetDateTime {
		return _value
	}

	fun plusTime(amount: Long, unit: TimeUnit?): JsonDateTime {
		return when (unit?.name) {
			TimeUnit.DAY.name -> JsonDateTime(_value.plusDays(amount))
			TimeUnit.HOUR.name -> JsonDateTime(_value.plusHours(amount))
			else -> throw RuntimeException(String.format("time unit \"%s\" not handled", unit))
		}
	}

	override fun compareTo(other: JsonDateTime?): Int {
		return _value.compareTo(other?._value)
	}

	override fun hashCode(): Int {
		return _value.hashCode()
	}

	override fun equals(o: Any?): Boolean {
		return o is JsonDateTime && this.compareTo(o) == 0
	}

	override fun asToken(): JsonToken? {
		return null
	}

	@Throws(IOException::class, JsonProcessingException::class)
	override fun serialize(jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
		// (unimplemented)
	}

	override fun getNodeType(): JsonNodeType {
		return JsonNodeType.OBJECT
	}

	override fun asText(): String {
		return formatter.format(_value)
	}

	companion object {
		private const val serialVersionUID = 1L
		private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
		fun fromIso8601(dateTimeString: String?): JsonDateTime {
			return try {
				JsonDateTime(OffsetDateTime.parse(dateTimeString, formatter))
			} catch (e: DateTimeParseException) {
				try {
					JsonDateTime(LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime())
				} catch (e: DateTimeParseException) {
					throw e
				}
			}
		}
	}

	init {
		_value = dateTime
	}
}
