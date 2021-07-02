/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.verification

import android.net.ConnectivityManager
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A task to verify a specifc DCC holder.
 *
 * @param certificateHolder The certificate holder (containing the certificate data) to verify
 * @param connectivityManager The Android connectivity service used to check if the device is offline or not
 * @param ignoreLocalTrustList True to ignore the local trust list during verification and force either an offline or network error
 */
class CertificateVerificationTask(
	val certificateHolder: CertificateHolder,
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
			val state = verifier.verify(certificateHolder, trustList)
			mutableVerificationStateFlow.emit(state)
		} else {
			val hasNetwork = NetworkUtil.isNetworkAvailable(connectivityManager)
			if (hasNetwork) {
				mutableVerificationStateFlow.emit(
					VerificationState.ERROR(
						StateError(ErrorCodes.GENERAL_NETWORK_FAILURE, certificateHolder = certificateHolder), null
					)
				)
			} else {
				mutableVerificationStateFlow.emit(
					VerificationState.ERROR(
						StateError(ErrorCodes.GENERAL_OFFLINE, certificateHolder = certificateHolder), null
					)
				)
			}
		}
	}

}
