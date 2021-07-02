/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder

import ch.admin.bag.covidcertificate.sdk.core.HC1_A
import ch.admin.bag.covidcertificate.sdk.core.LT1_A
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CertificateDecoderTest {

	@Test
	fun testDecode() {
		val decodeState = CertificateDecoder.decode(HC1_A)
		assertTrue(decodeState is DecodeState.SUCCESS)
	}

	@Test
	fun testDecodeLight() {
		val decodeState = CertificateDecoder.decode(LT1_A)
		assertTrue(decodeState is DecodeState.SUCCESS)
	}
}