/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

import android.content.Context
import ch.admin.bag.covidcertificate.eval.chain.*
import ch.admin.bag.covidcertificate.eval.data.EvalErrorCodes
import ch.admin.bag.covidcertificate.eval.data.EvalErrorCodes.SIGNATURE_COSE_INVALID
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.eval.utils.getHardcodedSigningKeys
import com.squareup.moshi.Moshi


object Eval {
	private val TAG = Eval::class.java.simpleName

	private val moshi by lazy { Moshi.Builder().build() }

	private val signingKeys = getHardcodedSigningKeys()

	/**
	 * Checks whether the DCC has a valid signature.
	 *
	 * A signature is only valid if it is signed by a trusted key, but also only if other attributes are valid
	 * (e.g. the signature is not expired - which may be different from the legal national rules).
	 */
	suspend fun checkSignature(dccHolder: DccHolder, context: Context): CheckSignatureState {

		/* Check that certificate type and signature timestamps are valid */

		val type = dccHolder.certType ?: return CheckSignatureState.INVALID(EvalErrorCodes.SIGNATURE_TYPE_INVALID)

		val timestampError = TimestampService.decode(dccHolder)
		if (timestampError != null) {
			return CheckSignatureState.INVALID(timestampError)
		}

		/* Repeat decode chain to get and verify COSE signature */

		val encoded = PrefixIdentifierService.decode(dccHolder.qrCodeData)
			?: return CheckSignatureState.INVALID(EvalErrorCodes.DECODE_PREFIX)
		val compressed = Base45Service.decode(encoded) ?: return CheckSignatureState.INVALID(EvalErrorCodes.DECODE_BASE_45)
		val cose = DecompressionService.decode(compressed) ?: return CheckSignatureState.INVALID(EvalErrorCodes.DECODE_Z_LIB)

		val valid = VerificationCoseService.decode(signingKeys, cose, type)

		return if (valid) CheckSignatureState.SUCCESS else CheckSignatureState.INVALID(SIGNATURE_COSE_INVALID)
	}

	/**
	 * @param dccHolder Object which was returned from the decode function
	 * @return State for the revocation check
	 */
	suspend fun checkRevocationStatus(dccHolder: DccHolder, context: Context): CheckRevocationState {
		return CheckRevocationState.SUCCESS
	}

	/**
	 * @param dccHolder Object which was returned from the decode function
	 * @return State for the Signaturecheck
	 */
	suspend fun checkNationalRules(dccHolder: DccHolder, context: Context): CheckNationalRulesState {
		return if (!dccHolder.euDGC.vaccinations.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyVaccine(dccHolder.euDGC.vaccinations.first())
		} else if (!dccHolder.euDGC.tests.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyTest(dccHolder.euDGC.tests.first())
		} else if (!dccHolder.euDGC.pastInfections.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyRecovery(dccHolder.euDGC.pastInfections.first())
		} else {
			throw Exception("NO VALID DATA")
		}
	}
}