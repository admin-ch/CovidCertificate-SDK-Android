/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.chain

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.admin.bag.covidcertificate.eval.EvalErrorCodes
import ch.admin.bag.covidcertificate.eval.data.Eudgc
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TimestampServiceTest {

	// RECALL: The service returns null if there was NO error

	@Test
	fun future_expiration() {
		val dcc = Bagdgc(Eudgc(), "", expirationTime = Instant.now().plusSeconds(60))
		assertNull(TimestampService.decode(dcc))
	}

	@Test
	fun past_expiration() {
		val dcc = Bagdgc(Eudgc(), "", expirationTime = Instant.now().minusSeconds(60))
		assertEquals(TimestampService.decode(dcc), EvalErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED)
	}

	@Test
	fun no_expiration() {
		val dcc = Bagdgc(Eudgc(), "", expirationTime = null)
		assertNull(TimestampService.decode(dcc))
	}

	@Test
	fun future_issuedAt() {
		val dcc = Bagdgc(Eudgc(), "", issuedAt = Instant.now().plusSeconds(60))
		assertEquals(TimestampService.decode(dcc), EvalErrorCodes.SIGNATURE_TIMESTAMP_NOT_YET_VALID)
	}

	@Test
	fun past_issuedAt() {
		val dcc = Bagdgc(Eudgc(), "", issuedAt = Instant.now().minusSeconds(60))
		assertNull(TimestampService.decode(dcc))
	}

	@Test
	fun no_issuedAt() {
		val dcc = Bagdgc(Eudgc(), "", issuedAt = null)
		assertNull(TimestampService.decode(dcc))
	}

	@Test
	fun combined_invalid() {
		val dcc = Bagdgc(
			Eudgc(), "",
			expirationTime = Instant.now().minusSeconds(120),
			issuedAt = Instant.now().plusSeconds(60)
		)
		// expiration is more important than future issuedAt
		assertEquals(TimestampService.decode(dcc), EvalErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED)
	}

	@Test
	fun combined_valid() {
		val dcc = Bagdgc(
			Eudgc(), "",
			expirationTime = Instant.now().plusSeconds(120),
			issuedAt = Instant.now().minusSeconds(60)
		)
		assertNull(TimestampService.decode(dcc))
	}
}