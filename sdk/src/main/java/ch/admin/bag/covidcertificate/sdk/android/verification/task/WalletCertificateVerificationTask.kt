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
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier

/**
 * The verification task implementation specific for wallet applications. This task takes a [CertificateHolder] and returns a full
 * verification state that contains all information about the certificate validity.
 *
 * @param certificateHolder The verifier certificate holder (containing the certificate data) to verify
 * @param connectivityManager The Android connectivity service used to check if the device is offline or not
 * @param ignoreLocalTrustList True to ignore the local trust list during verification and force either an offline or network error
 */
internal class WalletCertificateVerificationTask(
	private val certificateHolder: CertificateHolder,
	connectivityManager: ConnectivityManager,
	ignoreLocalTrustList: Boolean = false
) : CertificateVerificationTask(connectivityManager, ignoreLocalTrustList) {

	override suspend fun verify(verifier: CertificateVerifier, trustList: TrustList): VerificationState {
		return verifier.verify(certificateHolder, trustList)
	}
}