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

import ch.admin.bag.covidcertificate.eval.data.EvalErrorCodes
import ch.admin.bag.covidcertificate.eval.data.state.Error
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.models.TrustList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CertificateVerificationTask(val dccHolder: DccHolder) {

	private val mutableVerificationStateFlow = MutableStateFlow<VerificationState>(VerificationState.LOADING)
	val verificationStateFlow = mutableVerificationStateFlow.asStateFlow()

	/**
	 * Execute this verification task with the specified verifier and trust list
	 */
	internal suspend fun execute(verifier: CertificateVerifier, trustList: TrustList?) {
		if (trustList != null) {
			val state = verifier.verify(dccHolder, trustList)
			mutableVerificationStateFlow.emit(state)
		} else {
			mutableVerificationStateFlow.emit(
				VerificationState.ERROR(
					Error(
						EvalErrorCodes.TRUST_LIST_MISSING,
						dccHolder = dccHolder
					), null
				)
			)
		}
	}

}
