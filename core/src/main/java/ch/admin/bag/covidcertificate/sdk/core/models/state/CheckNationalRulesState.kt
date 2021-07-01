/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.state

import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesError
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.ValidityRange

sealed class CheckNationalRulesState {
	data class SUCCESS(val validityRange: ValidityRange) : CheckNationalRulesState()
	data class NOT_YET_VALID(val validityRange: ValidityRange, val ruleId: String? = null) : CheckNationalRulesState()
	data class NOT_VALID_ANYMORE(val validityRange: ValidityRange, val ruleId: String? = null) : CheckNationalRulesState()
	data class INVALID(val nationalRulesError: NationalRulesError, val ruleId: String? = null) : CheckNationalRulesState()
	object LOADING : CheckNationalRulesState()
	data class ERROR(val error: StateError) : CheckNationalRulesState()

	fun validityRange(): ValidityRange? = when (this) {
		is NOT_VALID_ANYMORE -> validityRange
		is NOT_YET_VALID -> validityRange
		is SUCCESS -> validityRange
		else -> null
	}
}