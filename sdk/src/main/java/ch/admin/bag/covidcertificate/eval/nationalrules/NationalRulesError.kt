/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.nationalrules

import ch.admin.bag.covidcertificate.eval.data.ErrorCodes

enum class NationalRulesError(val message: String, val errorCode: String) {
	NO_VALID_DATE("Not a valid Date format", ErrorCodes.NO_VALID_DATE),
	NO_VALID_PRODUCT("Product is not registered", ErrorCodes.NO_VALID_PRODUCT),
	WRONG_DISEASE_TARGET("Only SarsCov2 is a valid disease target", ErrorCodes.WRONG_DISEASE_TARGET),
	WRONG_TEST_TYPE("Test type invalid", ErrorCodes.WRONG_TEST_TYPE),
	POSITIVE_RESULT("Test result was positive", ErrorCodes.POSITIVE_RESULT),
	NOT_FULLY_PROTECTED("Missing vaccine shots, only partially protected", ErrorCodes.NOT_FULLY_PROTECTED),
	TOO_MANY_VACCINE_ENTRIES("Certificate contains more than one vaccine entries", ErrorCodes.TOO_MANY_VACCINE_ENTRIES),
	TOO_MANY_TEST_ENTRIES("Certificate contains more than one test entries", ErrorCodes.TOO_MANY_TEST_ENTRIES),
	TOO_MANY_RECOVERY_ENTRIES("Certificate contains more than one recovery entries", ErrorCodes.TOO_MANY_RECOVERY_ENTRIES),
	UNKNOWN_RULE_FAILED("An unknown rule failed to verify", ErrorCodes.UNKNOWN_RULE_FAILED)
}