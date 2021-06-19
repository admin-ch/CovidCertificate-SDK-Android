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

import ch.admin.bag.covidcertificate.eval.chain.Base45Service
import ch.admin.bag.covidcertificate.eval.chain.DecompressionService
import ch.admin.bag.covidcertificate.eval.chain.PrefixIdentifierService
import ch.admin.bag.covidcertificate.eval.chain.RevokedHealthCertService
import ch.admin.bag.covidcertificate.eval.chain.TimestampService
import ch.admin.bag.covidcertificate.eval.chain.VerificationCoseService
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes.SIGNATURE_COSE_INVALID
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.RevokedCertificates
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier

internal object Eval {
	private val TAG = Eval::class.java.simpleName

	/**
	 * Checks whether the DCC has a valid signature.
	 *
	 * A signature is only valid if it is signed by a trusted key, but also only if other attributes are valid
	 * (e.g. the signature is not expired - which may be different from the legal national rules).
	 */
	fun checkSignature(dccHolder: DccHolder, signatures: Jwks): CheckSignatureState {

		/* Check that certificate type and signature timestamps are valid */

		val type = dccHolder.certType ?: return CheckSignatureState.INVALID(ErrorCodes.SIGNATURE_TYPE_INVALID)

		val timestampError = TimestampService.decode(dccHolder)
		if (timestampError != null) {
			return CheckSignatureState.INVALID(timestampError)
		}

		/* Repeat decode chain to get and verify COSE signature */

		val encoded = PrefixIdentifierService.decode(dccHolder.qrCodeData)
			?: return CheckSignatureState.INVALID(ErrorCodes.DECODE_PREFIX)
		val compressed = Base45Service.decode(encoded) ?: return CheckSignatureState.INVALID(ErrorCodes.DECODE_BASE_45)
		val cose = DecompressionService.decode(compressed) ?: return CheckSignatureState.INVALID(ErrorCodes.DECODE_Z_LIB)

		val valid = VerificationCoseService.decode(signatures.certs, cose, type)

		return if (valid) CheckSignatureState.SUCCESS else CheckSignatureState.INVALID(SIGNATURE_COSE_INVALID)
	}

	/**
	 * @param dccHolder Object which was returned from the decode function
	 * @return State for the revocation check
	 */
	fun checkRevocationStatus(dccHolder: DccHolder, revokedCertificates: RevokedCertificates): CheckRevocationState {
		val revokedCertificateService = RevokedHealthCertService(revokedCertificates)
		val containsRevokedCertificate = revokedCertificateService.isRevoked(dccHolder.euDGC)

		return if (containsRevokedCertificate) {
			CheckRevocationState.INVALID(ErrorCodes.REVOCATION_REVOKED)
		} else {
			CheckRevocationState.SUCCESS
		}
	}

	/**
	 * @param dccHolder Object which was returned from the decode function
	 * @return State for the Signaturecheck
	 */
	fun checkNationalRules(
		dccHolder: DccHolder,
		nationalRulesVerifier: NationalRulesVerifier,
		ruleSet: RuleSet
	): CheckNationalRulesState {
		return nationalRulesVerifier.verify(dccHolder.euDGC, ruleSet)
	}
}