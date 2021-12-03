/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.verification.task

import android.net.ConnectivityManager
import ch.admin.bag.covidcertificate.sdk.android.models.VerifierCertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier

/**
 * The verification task implementation specific for verifier applications. This task takes a [VerifierCertificateHolder] and
 * returns a reduced verification state that does not contain information that could indicate what type of certificate was verified.
 *
 * @param certificateHolder The verifier certificate holder (containing the certificate data) to verify
 * @param connectivityManager The Android connectivity service used to check if the device is offline or not
 */
internal class VerifierCertificateVerificationTask(
	private val certificateHolder: VerifierCertificateHolder,
	private val verificationModes: Set<String>,
	connectivityManager: ConnectivityManager
) : CertificateVerificationTask(connectivityManager) {

	override suspend fun verify(verifier: CertificateVerifier, trustList: TrustList): VerificationState {
		val verificationState = verifier.verify(certificateHolder.internalCertificateHolder, trustList, verificationModes)
		return when (verificationState) {
			is VerificationState.LOADING -> verificationState
			is VerificationState.SUCCESS -> verificationState
			is VerificationState.INVALID -> {
				// Strip out all information that identifies the national rule that failed for verifier apps
				when (val nationalRulesState = verificationState.nationalRulesState) {
					is CheckNationalRulesState.SUCCESS -> {
						val strippedNationalRulesState = nationalRulesState.copy(validityRange = null)
						verificationState.copy(nationalRulesState = strippedNationalRulesState)
					}
					is CheckNationalRulesState.NOT_YET_VALID -> {
						val strippedNationalRulesState = nationalRulesState.copy(validityRange = null, ruleId = null)
						verificationState.copy(nationalRulesState = strippedNationalRulesState)
					}
					is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
						val strippedNationalRulesState = nationalRulesState.copy(validityRange = null, ruleId = null)
						verificationState.copy(nationalRulesState = strippedNationalRulesState)
					}
					is CheckNationalRulesState.INVALID -> {
						val strippedNationalRulesState = nationalRulesState.copy(nationalRulesError = null, ruleId = null)
						verificationState.copy(nationalRulesState = strippedNationalRulesState)
					}
					else -> verificationState
				}
			}
			is VerificationState.ERROR -> verificationState
		}
	}
}