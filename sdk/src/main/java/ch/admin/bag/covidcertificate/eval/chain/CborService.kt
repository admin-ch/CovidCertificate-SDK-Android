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
package ch.admin.bag.covidcertificate.eval.chain

import ch.admin.bag.covidcertificate.eval.data.Eudgc
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.upokecenter.cbor.CBORObject
import java.time.Instant


internal object CborService {

	private val keyEuDgcV1 = CBORObject.FromObject(1)

	// Takes qrCodeData to directly construct a Bagdgc AND keep the field in the DCC a val
	fun decode(input: ByteArray, qrCodeData: String): Bagdgc? {
		try {
			val map = CBORObject.DecodeFromBytes(input)

			val expirationTime: Instant? = map[CwtHeaderKeys.EXPIRATION.AsCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
			val issuedAt: Instant? = map[CwtHeaderKeys.ISSUED_AT.AsCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
			val issuer: String? = map[CwtHeaderKeys.ISSUER.AsCBOR()]?.AsString()

			map[CwtHeaderKeys.HCERT.AsCBOR()]?.let { hcert ->
				hcert[keyEuDgcV1]?.let {
					val eudgc = CBORMapper().readValue(getContents(it), Eudgc::class.java)

					return Bagdgc(eudgc, qrCodeData, expirationTime, issuedAt, issuer)
				}
			}
			return null
		} catch (e: Throwable) {
			return null
		}
	}

	private fun getContents(it: CBORObject) = try {
		it.GetByteString()
	} catch (e: Throwable) {
		it.EncodeToBytes()
	}

}