/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.trustlist

import ch.admin.bag.covidcertificate.sdk.core.extensions.fromBase64
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.utils.CryptoUtil
import com.squareup.moshi.JsonClass
import java.security.PublicKey

@JsonClass(generateAdapter = true)
data class Jwks(
	val certs: List<Jwk>
)

/**
 * Note that some fields are base64 encoded. E.g. kid, x, y, n, e.
 */
@JsonClass(generateAdapter = true)
data class Jwk(
	val keyId: String,
	val alg: String,
	val use: String, // For which type of health cert the signing key is valid. One of: r,v,t.
	val x5a: X5A? = null,
	val x5aS256: String? = null,
	val crv: String? = null,
	val x: String? = null,
	val y: String? = null,
	val n: String? = null,
	val e: String? = null,
) {

	companion object {
		private const val ALG_RSA_256 = "RS256"
		private const val ALG_ES_256 = "ES256"

		fun fromNE(kid: String, n: String, e: String, use: String) = Jwk(
			keyId = kid,
			alg = ALG_RSA_256,
			use = use,
			n = n,
			e = e,
		)

		fun fromXY(kid: String, x: String, y: String, use: String) = Jwk(
			keyId = kid,
			alg = ALG_ES_256,
			use = use,
			x = x,
			y = y,
		)
	}

	fun getKid(): ByteArray = keyId.fromBase64()

	fun getPublicKey(): PublicKey? {
		try {
			return when (alg) {
				ALG_ES_256 -> CryptoUtil.ecPublicKeyFromCoordinate(x!!.fromBase64(), y!!.fromBase64())
				ALG_RSA_256 -> CryptoUtil.rsaPublicKeyFromModulusExponent(n!!.fromBase64(), e!!.fromBase64())
				else -> null
			}
		} catch (e: Exception) {
			// Can throw e.g. if the (x, y) pair is not a point on the curve
			e.printStackTrace()
		}
		return null
	}

	fun isAllowedToSign(certType: CertType): Boolean {
		return getKeyUsageTypes().contains(certType)
				|| use.isEmpty() // if key.use is empty, we assume the key is allowed to be used for all operations
	}

	fun getKeyUsageTypes(): List<CertType> {
		val certTypes = mutableListOf<CertType>()

		if (use.contains(CertType.VACCINATION.use)) {
			certTypes.add(CertType.VACCINATION)
		}
		if (use.contains(CertType.RECOVERY.use)) {
			certTypes.add(CertType.RECOVERY)
		}
		if (use.contains(CertType.TEST.use)) {
			certTypes.add(CertType.TEST)
		}

		return certTypes
	}
}

@JsonClass(generateAdapter = true)
data class X5A(
	val issuer: String,
	val serial: Long,
	var subject: String
)
