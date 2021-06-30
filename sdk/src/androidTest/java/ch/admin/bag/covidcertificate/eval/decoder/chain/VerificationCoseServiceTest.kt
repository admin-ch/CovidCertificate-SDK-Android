/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.decoder.chain

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.admin.bag.covidcertificate.eval.HC1_A
import ch.admin.bag.covidcertificate.eval.getInvalidSigningKeys
import ch.admin.bag.covidcertificate.eval.utils.getHardcodedSigningKeys
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class VerificationCoseServiceTest {

	@Test
	fun decode_success() {
		val bagKeys = getHardcodedSigningKeys("dev")

		val encoded = PrefixIdentifierService.decode(HC1_A)
		val compressed = Base45Service.decode(encoded!!)
		val cose = DecompressionService.decode(compressed!!)
		val valid = VerificationCoseService.decode(bagKeys, cose!!)

		assertTrue(valid)
	}

	@Test
	fun decode_wrongFlavor() {
		val invalidKeys = getHardcodedSigningKeys("abn")

		val encoded = PrefixIdentifierService.decode(HC1_A)
		val compressed = Base45Service.decode(encoded!!)
		val cose = DecompressionService.decode(compressed!!)
		val valid = VerificationCoseService.decode(invalidKeys, cose!!)

		assertFalse(valid)
	}

	@Test
	fun decode_invalidSigningKey() {
		val invalidKeys = getInvalidSigningKeys()

		val encoded = PrefixIdentifierService.decode(HC1_A)
		val compressed = Base45Service.decode(encoded!!)
		val cose = DecompressionService.decode(compressed!!)
		val valid = VerificationCoseService.decode(invalidKeys, cose!!)

		assertFalse(valid)
	}

}