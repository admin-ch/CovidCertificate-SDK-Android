/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.utils

import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.*

internal object CryptoUtil {

	private var ecParameterSpec: ECParameterSpec? = null
		get() {
			if (field == null) {
				val ecParameters = ECGenParameterSpec("prime256v1")
				field = try {
					val algorithmParameters = AlgorithmParameters.getInstance("EC")
					algorithmParameters.init(ecParameters)

					algorithmParameters.getParameterSpec(ECParameterSpec::class.java)
				} catch (e: Exception) {
					// This is a work-around for getting the ECParameterSpec since EC is only available in AlgorithmParameters on API 26+
					val kpg = KeyPairGenerator.getInstance("EC")
					kpg.initialize(ecParameters, SecureRandom())
					val keyPair = kpg.generateKeyPair()
					(keyPair.public as ECPublicKey).params
				}
			}
			return field
		}

	/**
	 * Creates a {@link java.security.PublicKey} from a coordinate point (x, y).
	 * Assumes curve P-256.
	 */
	fun ecPublicKeyFromCoordinate(x: ByteArray, y: ByteArray): PublicKey {
		// x, y are unsigned (recall that they are just field elements)
		val x = BigInteger(1, x)
		val y = BigInteger(1, y)

		val ecPoint = ECPoint(x, y)
		val ecKeySpec = ECPublicKeySpec(ecPoint, ecParameterSpec)

		val keyFactory = KeyFactory.getInstance("EC")
		return keyFactory.generatePublic(ecKeySpec)
	}

	/**
	 * Creates a {@link java.security.PublicKey} from an RSA modulus n exponent e
	 */
	fun rsaPublicKeyFromModulusExponent(n: ByteArray, e: ByteArray): PublicKey {
		val rsaPublicKeySpec = RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))
		val keyFactory = KeyFactory.getInstance("RSA")
		return keyFactory.generatePublic(rsaPublicKeySpec)
	}
}