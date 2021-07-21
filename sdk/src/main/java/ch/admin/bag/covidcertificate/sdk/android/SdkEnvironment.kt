/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android

enum class SdkEnvironment(
	internal val trustListBaseUrl: String,
	internal val leafCertificateCommonName: String
) {
	DEV("https://www.cc-d.bit.admin.ch/trust/", "CH01-AppContentCertificate-ref"),
	ABN("https://www.cc-a.bit.admin.ch/trust/", "CH01-AppContentCertificate-abn"),
	PROD("https://www.cc.bit.admin.ch/trust/", "CH01-AppContentCertificate");
}