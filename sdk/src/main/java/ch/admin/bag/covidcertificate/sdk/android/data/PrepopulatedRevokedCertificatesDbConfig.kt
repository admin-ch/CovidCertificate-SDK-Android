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

import ch.admin.bag.covidcertificate.verifier.sdk.android.BuildConfig

object PrepopulatedRevokedCertificatesDbConfig {
	const val prepopulatedRevokedCertificatesDbPath = "revoked_certificates_db.db"
	val prepopulatedSinceHeader = if (BuildConfig.FLAVOR == "prod") "11667563" else "0"
}