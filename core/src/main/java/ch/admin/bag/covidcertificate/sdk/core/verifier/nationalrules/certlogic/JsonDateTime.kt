/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-dcc-development/dgc-business-rules/tree/main/certlogic/certlogic-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ValueNode
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class JsonDateTime protected constructor(dateTime: OffsetDateTime) : ValueNode(), Comparable<JsonDateTime?> {
	private val _value: OffsetDateTime
	fun temporalValue(): OffsetDateTime {
		return _value
	}

	fun plusTime(amount: Long, unit: TimeUnit?): JsonDateTime {
		return when (unit?.name) {
			TimeUnit.DAY.name -> JsonDateTime(_value.plusDays(amount).atZoneSimilarLocal(ZoneId.systemDefault()).toOffsetDateTime())
			TimeUnit.HOUR.name -> JsonDateTime(_value.plusHours(amount).atZoneSimilarLocal(ZoneId.systemDefault()).toOffsetDateTime())
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
