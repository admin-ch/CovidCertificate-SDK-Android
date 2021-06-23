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

import android.net.ConnectivityManager
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes
import ch.admin.bag.covidcertificate.eval.data.state.Error
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.models.TrustList
import ch.admin.bag.covidcertificate.eval.utils.NetworkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A task to verify a specifc DCC holder.
 *
 * @param dccHolder The DCC holder (containing the EuDgc data) to verify
 * @param connectivityManager The Android connectivity service used to check if the device is offline or not
 * @param ignoreLocalTrustList True to ignore the local trust list during verification and force either an offline or network error
 */
class CertificateVerificationTask(
	val dccHolder: DccHolder,
	val connectivityManager: ConnectivityManager,
	val ignoreLocalTrustList: Boolean = false
) {

	private val mutableVerificationStateFlow = MutableStateFlow<VerificationState>(VerificationState.LOADING)
	val verificationStateFlow = mutableVerificationStateFlow.asStateFlow()

	/**
	 * Execute this verification task with the specified verifier and trust list
	 */
	internal suspend fun execute(verifier: CertificateVerifier, trustList: TrustList?) {
		if (trustList != null && !ignoreLocalTrustList) {
			val state = verifier.verify(dccHolder, trustList)
			mutableVerificationStateFlow.emit(state)
		} else {
			val hasNetwork = NetworkUtil.isNetworkAvailable(connectivityManager)
			if (hasNetwork) {
				mutableVerificationStateFlow.emit(
					VerificationState.ERROR(
						Error(ErrorCodes.GENERAL_NETWORK_FAILURE, dccHolder = dccHolder), null
					)
				)
			} else {
				mutableVerificationStateFlow.emit(
					VerificationState.ERROR(
						Error(ErrorCodes.GENERAL_OFFLINE, dccHolder = dccHolder), null
					)
				)
			}
		}
	}

}
