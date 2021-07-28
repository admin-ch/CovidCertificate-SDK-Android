/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.net.service

import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.ActiveSignerCertificates
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

internal interface CertificateService {

	@Headers("Accept: application/json+jws")
	@GET("v2/keys/list")
	suspend fun getActiveSignerCertificateKeyIds(): Response<ActiveSignerCertificates>

	@Headers("Accept: application/json+jws")
	@GET("v2/keys/updates?certFormat=ANDROID")
	suspend fun getSignerCertificates(@Query("upTo") upTo: Long, @Query("since") since: String? = null): Response<Jwks>
}