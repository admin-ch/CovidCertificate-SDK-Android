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

import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.models.healthcert.eu.Eudgc

internal object CertTypeService {

	fun decode(dcc: Eudgc): CertType? {
		// Certificate must not have two types => if it has more then it is invalid
		var type: CertType? = null
		var numContainedContent = 0

		dcc.tests?.size?.also { numTests ->
			if (numTests > 0) {
				numContainedContent += numTests
				type = CertType.TEST
			}
		}

		dcc.pastInfections?.size?.also { numRecoveries ->
			if (numRecoveries > 0) {
				numContainedContent += numRecoveries
				type = CertType.RECOVERY
			}
		}

		dcc.vaccinations?.size?.also { numVaccinations ->
			if (numVaccinations > 0) {
				numContainedContent += numVaccinations
				type = CertType.VACCINATION
			}
		}

		return if (numContainedContent == 1) type else null
	}

}