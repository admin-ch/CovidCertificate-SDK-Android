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
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The base task class to verify a certificate.
 *
 * @param connectivityManager The Android connectivity service used to check if the device is offline or not
 * @param ignoreLocalTrustList True to ignore the local trust list during verification and force either an offline or network error
 */
internal abstract class CertificateVerificationTask(
	private val connectivityManager: ConnectivityManager,
	private val ignoreLocalTrustList: Boolean = false
) {

	private val mutableVerificationStateFlow = MutableStateFlow<VerificationState>(VerificationState.LOADING)
	val verificationStateFlow = mutableVerificationStateFlow.asStateFlow()

	/**
	 * Execute this verification task with the specified verifier and trust list
	 */
	suspend fun execute(verifier: CertificateVerifier, trustList: TrustList?) {
		mutableVerificationStateFlow.emit(VerificationState.LOADING)
		if (trustList != null && !ignoreLocalTrustList) {
			val verificationState = verify(verifier, trustList)
			mutableVerificationStateFlow.emit(verificationState)
		} else {
			val hasNetwork = NetworkUtil.isNetworkAvailable(connectivityManager)
			if (hasNetwork) {
				mutableVerificationStateFlow.emit(VerificationState.ERROR(StateError(ErrorCodes.GENERAL_NETWORK_FAILURE), null))
			} else {
				mutableVerificationStateFlow.emit(VerificationState.ERROR(StateError(ErrorCodes.GENERAL_OFFLINE), null))
			}
		}
	}

	/**
	 * Execute the actual verification and return the verification state, so that the individual subclasses can do additional
	 * processing on the verification state, such as stripping some sensitive information.
	 */
	protected abstract suspend fun verify(verifier: CertificateVerifier, trustList: TrustList): VerificationState

}
