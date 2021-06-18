/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.data

object EvalErrorCodes {
	/* Errors during decoding */
	const val DECODE_PREFIX = "D|PRX"
	const val DECODE_BASE_45 = "D|B45"
	const val DECODE_Z_LIB = "D|ZLB"
	const val DECODE_COSE = "D|CSE"
	const val DECODE_CBOR = "D|CBR"

	/* Errors during signature verification */
	const val SIGNATURE_NETWORK = "S|NWN"
	const val SIGNATURE_TIMESTAMP_NOT_YET_VALID = "S|NYV"
	const val SIGNATURE_TIMESTAMP_EXPIRED = "S|EXP"
	const val SIGNATURE_TYPE_INVALID = "S|TIV"
	const val SIGNATURE_COSE_INVALID = "S|CSI"
	const val SIGNATURE_UNKNOWN = "S|UNK"

	/* Errors during revocation verification */
	const val REVOCATION_NETWORK = "R|NWN"
	const val REVOCATION_UNKNOWN = "R|UNK"
	const val REVOCATION_REVOKED = "R|REV"

	/* Errors during national rules verification */
	const val NO_VALID_DATE = "N|NVD"
	const val NO_VALID_PRODUCT = "N|NVP"
	const val WRONG_DISEASE_TARGET = "N|WDT"
	const val WRONG_TEST_TYPE = "N|WTT"
	const val POSITIVE_RESULT = "N|PR"
	const val NOT_FULLY_PROTECTED = "N|NFP"
	const val RULESET_UNKNOWN = "N|UNK"
	const val TOO_MANY_VACCINE_ENTRIES = "N|TMV"
	const val TOO_MANY_TEST_ENTRIES = "N|TMT"
	const val TOO_MANY_RECOVERY_ENTRIES = "N|TMR"
	const val UNKNOWN_RULE_FAILED = "N|UNK"

	/* General errors */
	const val GENERAL_NETWORK_FAILURE = "G|NWF"
	const val GENERAL_OFFLINE = "G|OFF"

}