/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.decoder

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.admin.bag.covidcertificate.eval.HC1_A
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CertificateDecoderTest {

	@Test
	fun testDecode() {
		val decodeState = CertificateDecoder.decode(HC1_A)
		assertTrue(decodeState is DecodeState.SUCCESS)
	}
}