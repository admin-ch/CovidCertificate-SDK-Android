/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.data.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import okio.Buffer


@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class DataString

/**
 * A Moshi json adapter that parses a JSON object as a string.
 */
class DataAsStringAdapter {

	@ToJson
	fun toJson(writer: JsonWriter, @DataString string: String) {
		writer.value(Buffer().writeUtf8(string))
	}

	@FromJson
	@DataString
	fun fromJson(reader: JsonReader, delegate: JsonAdapter<Any>): String {
		val data = reader.readJsonValue()
		return delegate.toJson(data)
	}

}