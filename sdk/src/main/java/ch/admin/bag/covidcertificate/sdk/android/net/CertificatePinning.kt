/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.net

import ch.admin.bag.covidcertificate.verifier.sdk.android.BuildConfig
import okhttp3.CertificatePinner

object CertificatePinning {

	private val CERTIFICATE_PINNER_DISABLED = CertificatePinner.DEFAULT
	private val CERTIFICATE_PINNER_LIVE = CertificatePinner.Builder()
		.add("www.cc-a.bit.admin.ch", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // root
		.add("www.cc-d.bit.admin.ch", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // root
		.add("www.cc.bit.admin.ch", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // root
		.add("covidcertificate-app-d.bit.admin.ch", "sha256/SkntvS+PgjC9VZKzE1c/4cFypF+pgBHMHt27Nq3j/OU=") // root
		.add("covidcertificate-app-a.bit.admin.ch", "sha256/SkntvS+PgjC9VZKzE1c/4cFypF+pgBHMHt27Nq3j/OU=") // root
		.add("covidcertificate-app.bit.admin.ch", "sha256/SkntvS+PgjC9VZKzE1c/4cFypF+pgBHMHt27Nq3j/OU=") // root
		.build()

	val pinner: CertificatePinner
		get() = if (enabled) CERTIFICATE_PINNER_LIVE else CERTIFICATE_PINNER_DISABLED

	/**
	 * Enabled/disable the certificate pinner.
	 *
	 * Per default certificate pinning is enabled on non-debug (i.e. release) builds.
	 *
	 * Setting this field only affects what is returned when getting the `pinner`.
	 * Hence you want to call this early in the app's lifecycle to make sure all Retrofit instances are initialised as intended.
	 * Also note that this pinner only applies to OkHttp, the network-security-config still applies.
	 */
	var enabled: Boolean = !BuildConfig.DEBUG

}