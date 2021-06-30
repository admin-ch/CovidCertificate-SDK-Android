/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.net.interceptor

import ch.admin.bag.covidcertificate.eval.data.Config
import okhttp3.Interceptor
import okhttp3.Response

internal class ApiKeyInterceptor : Interceptor {

	companion object {
		private const val HEADER_AUTHORIZATION = "Authorization"
		private const val APP_TOKEN_PREFIX = "Bearer "
	}

	override fun intercept(chain: Interceptor.Chain): Response {
		val newRequest = chain.request()
			.newBuilder()
			.addHeader(HEADER_AUTHORIZATION, APP_TOKEN_PREFIX + Config.appToken)
			.build()

		return chain.proceed(newRequest)
	}

}