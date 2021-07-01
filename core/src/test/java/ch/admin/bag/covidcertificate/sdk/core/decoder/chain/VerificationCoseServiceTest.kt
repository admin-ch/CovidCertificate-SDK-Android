/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.HC1_A
import ch.admin.bag.covidcertificate.sdk.core.getHardcodedSigningKeys
import ch.admin.bag.covidcertificate.sdk.core.getInvalidSigningKeys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class VerificationCoseServiceTest {

	@Test
	fun decode_success() {
		val bagKeys = getHardcodedSigningKeys("dev")

		try {
			val encoded = PrefixIdentifierService.decode(HC1_A)
			val compressed = Base45Service.decode(encoded!!)
			val cose = DecompressionService.decode(compressed!!)
			val valid = VerificationCoseService.decode(bagKeys, cose!!)

			assertTrue(valid)
		} catch (t: Throwable) {
			fail(t)
		}
	}

	@Test
	fun decode_wrongFlavor() {
		val invalidKeys = getHardcodedSigningKeys("abn")

		try {
			val encoded = PrefixIdentifierService.decode(HC1_A)
			val compressed = Base45Service.decode(encoded!!)
			val cose = DecompressionService.decode(compressed!!)
			val valid = VerificationCoseService.decode(invalidKeys, cose!!)

			assertFalse(valid)
		} catch (t: Throwable) {
			fail(t)
		}
	}

	@Test
	fun decode_invalidSigningKey() {
		val invalidKeys = getInvalidSigningKeys()

		try {
			val encoded = PrefixIdentifierService.decode(HC1_A)
			val compressed = Base45Service.decode(encoded!!)
			val cose = DecompressionService.decode(compressed!!)
			val valid = VerificationCoseService.decode(invalidKeys, cose!!)

			assertFalse(valid)
		} catch (t: Throwable) {
			fail(t)
		}
	}

}