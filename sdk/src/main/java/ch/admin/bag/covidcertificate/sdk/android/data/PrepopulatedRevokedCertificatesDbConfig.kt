/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.data

import ch.admin.bag.covidcertificate.sdk.android.SdkEnvironment

object PrepopulatedRevokedCertificatesDbConfig {
	const val prepopulatedRevokedCertificatesDbPath = "revoked_certificates_db.db"
	fun getPrepopulatedSinceHeader(environment: SdkEnvironment) = when (environment) {
		SdkEnvironment.DEV, SdkEnvironment.ABN -> "0"
		SdkEnvironment.PROD -> "11667563"
	}
}