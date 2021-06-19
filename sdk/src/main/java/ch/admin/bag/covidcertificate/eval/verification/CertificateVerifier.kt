/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.verification

import ch.admin.bag.covidcertificate.eval.Eval
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.data.state.Error
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.RevokedCertificates
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import ch.admin.bag.covidcertificate.eval.models.TrustList
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

internal class CertificateVerifier(private val nationalRulesVerifier: NationalRulesVerifier) {

	suspend fun verify(dccHolder: DccHolder, trustList: TrustList): VerificationState = withContext(Dispatchers.Default) {
		// Execute all three checks in parallel...
		val checkSignatureStateDeferred = async { checkSignature(dccHolder, trustList.signatures) }
		val checkRevocationStateDeferred = async { checkRevocationStatus(dccHolder, trustList.revokedCertificates) }
		val checkNationalRulesStateDeferred = async { checkNationalRules(dccHolder, nationalRulesVerifier, trustList.ruleSet) }

		// ... but wait for all of them to finish
		val checkSignatureState = checkSignatureStateDeferred.await()
		val checkRevocationState = checkRevocationStateDeferred.await()
		val checkNationalRulesState = checkNationalRulesStateDeferred.await()

		if (checkSignatureState is CheckSignatureState.ERROR) {
			VerificationState.ERROR(checkSignatureState.error, checkNationalRulesState.validityRange())
		} else if (checkRevocationState is CheckRevocationState.ERROR) {
			VerificationState.ERROR(checkRevocationState.error, checkNationalRulesState.validityRange())
		} else if (checkNationalRulesState is CheckNationalRulesState.ERROR) {
			VerificationState.ERROR(checkNationalRulesState.error, null)
		} else if (
			checkSignatureState == CheckSignatureState.SUCCESS
			&& checkRevocationState == CheckRevocationState.SUCCESS
			&& checkNationalRulesState is CheckNationalRulesState.SUCCESS
		) {
			VerificationState.SUCCESS(checkNationalRulesState.validityRange)
		} else if (
			checkSignatureState is CheckSignatureState.INVALID
			|| checkRevocationState is CheckRevocationState.INVALID
			|| checkNationalRulesState is CheckNationalRulesState.INVALID
			|| checkNationalRulesState is CheckNationalRulesState.NOT_YET_VALID
			|| checkNationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE
		) {
			VerificationState.INVALID(
				checkSignatureState, checkRevocationState, checkNationalRulesState,
				checkNationalRulesState.validityRange()
			)
		} else {
			VerificationState.LOADING
		}
	}

	private suspend fun checkSignature(dccHolder: DccHolder, signatures: Jwks) = withContext(Dispatchers.Default) {
		try {
			Eval.checkSignature(dccHolder, signatures)
		} catch (e: Exception) {
			CheckSignatureState.ERROR(Error(ErrorCodes.SIGNATURE_UNKNOWN, e.message.toString(), dccHolder))
		}
	}

	private suspend fun checkRevocationStatus(
		dccHolder: DccHolder,
		revokedCertificates: RevokedCertificates
	) = withContext(Dispatchers.Default) {
		try {
			Eval.checkRevocationStatus(dccHolder, revokedCertificates)
		} catch (e: Exception) {
			CheckRevocationState.ERROR(Error(ErrorCodes.REVOCATION_UNKNOWN, e.message.toString(), dccHolder))
		}
	}

	private suspend fun checkNationalRules(
		dccHolder: DccHolder,
		nationalRulesVerifier: NationalRulesVerifier,
		ruleSet: RuleSet
	) = withContext(Dispatchers.Default) {
		try {
			Eval.checkNationalRules(dccHolder, nationalRulesVerifier, ruleSet)
		} catch (e: Exception) {
			CheckNationalRulesState.ERROR(Error(ErrorCodes.RULESET_UNKNOWN, e.message.toString(), dccHolder))
		}
	}

}