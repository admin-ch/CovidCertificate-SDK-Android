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
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.Eudgc
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.DccHolder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.upokecenter.cbor.CBORObject
import java.time.Instant
import java.util.*

internal object CborService {

	private val keyEuDgcV1 = CBORObject.FromObject(1)

	// Takes qrCodeData to directly construct a Bagdgc AND keep the field in the DCC a val
	fun decode(input: ByteArray, qrCodeData: String): DccHolder? {

		val adapter: JsonAdapter<Eudgc> =
			Moshi.Builder().add(Date::class.java, Rfc3339DateJsonAdapter()).build().adapter(Eudgc::class.java)

		try {
			val map = CBORObject.DecodeFromBytes(input)

			val expirationTime: Instant? = map[CwtHeaderKeys.EXPIRATION.asCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
			val issuedAt: Instant? = map[CwtHeaderKeys.ISSUED_AT.asCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
			val issuer: String? = map[CwtHeaderKeys.ISSUER.asCBOR()]?.AsString()

			map[CwtHeaderKeys.HCERT.asCBOR()]?.let { hcert ->
				hcert[keyEuDgcV1]?.let {
					val eudgc = adapter.fromJson(it.ToJSONString()) ?: return null
					return DccHolder(qrCodeData, eudgc, null, expirationTime, issuedAt, issuer)
				}
			}
			return null
		} catch (e: Throwable) {
			return null
		}
	}

}