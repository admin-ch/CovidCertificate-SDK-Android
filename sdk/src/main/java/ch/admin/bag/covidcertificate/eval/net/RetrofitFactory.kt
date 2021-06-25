/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.net

import android.content.Context
import ch.admin.bag.covidcertificate.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.eval.data.Config
import ch.admin.bag.covidcertificate.eval.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

internal class RetrofitFactory {

	companion object {
		private const val CACHE_SIZE = 5 * 1024 * 1024 // 5 MB
	}

	fun create(context: Context): Retrofit {
		val rootCa = CovidCertificateSdk.getRootCa(context)
		val expectedCommonName = CovidCertificateSdk.getExpectedCommonName()
		val okHttpBuilder = OkHttpClient.Builder()
			.certificatePinner(CertificatePinning.pinner)
			.addInterceptor(JwsInterceptor(rootCa, expectedCommonName))
			.addInterceptor(ApiKeyInterceptor())
			.addInterceptor(UserAgentInterceptor(Config.userAgent))

		val cache = Cache(context.cacheDir, CACHE_SIZE.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		return Retrofit.Builder()
			.baseUrl(BuildConfig.BASE_URL_TRUST_LIST)
			.client(okHttpBuilder.build())
			.addConverterFactory(ScalarsConverterFactory.create())
			.addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(RawJsonStringAdapter()).build()))
			.build()
	}

}