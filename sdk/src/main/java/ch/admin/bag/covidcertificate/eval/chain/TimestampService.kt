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

import ch.admin.bag.covidcertificate.eval.EvalErrorCodes
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import java.time.Instant


internal object TimestampService {
	private val TAG = TimestampService::class.java.simpleName

	fun decode(
		dcc: Bagdgc,
		now: Instant = Instant.now()
	): String? {
		dcc.expirationTime?.also { et ->
			if (et.isBefore(now)) {
				return EvalErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED
			}
		}

		dcc.issuedAt?.also { ia ->
			if (ia.isAfter(now)) {
				return EvalErrorCodes.SIGNATURE_TIMESTAMP_NOT_YET_VALID
			}
		}

		return null
	}

}