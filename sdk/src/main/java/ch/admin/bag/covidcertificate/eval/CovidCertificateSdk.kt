/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.coroutineScope
import ch.admin.bag.covidcertificate.eval.data.CertificateSecureStorage
import ch.admin.bag.covidcertificate.eval.data.Config
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.eval.net.CertificatePinning
import ch.admin.bag.covidcertificate.eval.net.CertificateService
import ch.admin.bag.covidcertificate.eval.net.RevocationService
import ch.admin.bag.covidcertificate.eval.net.RuleSetService
import ch.admin.bag.covidcertificate.eval.net.UserAgentInterceptor
import ch.admin.bag.covidcertificate.eval.repository.TrustListRepository
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerificationController
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

object CovidCertificateSdk {

	private val TRUST_LIST_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1L)

	private lateinit var certificateVerificationController: CertificateVerificationController
	private var isInitialized = false
	private var trustListRefreshTimer: Timer? = null

	fun init(context: Context) {
		val retrofit = createRetrofit(context)
		val certificateService = retrofit.create(CertificateService::class.java)
		val revocationService = retrofit.create(RevocationService::class.java)
		val ruleSetService = retrofit.create(RuleSetService::class.java)

		val certificateStorage = CertificateSecureStorage.getInstance(context)
		val trustListRepository = TrustListRepository(certificateService, revocationService, ruleSetService, certificateStorage)
		val nationalRulesVerifier = NationalRulesVerifier(context)
		val certificateVerifier = CertificateVerifier(nationalRulesVerifier)
		certificateVerificationController = CertificateVerificationController(trustListRepository, certificateVerifier)

		isInitialized = true
	}

	fun registerWithLifecycle(lifecycle: Lifecycle) {
		requireInitialized()

		lifecycle.addObserver(object : LifecycleObserver {
			@OnLifecycleEvent(Lifecycle.Event.ON_START)
			fun onStart() {
				trustListRefreshTimer?.cancel()
				trustListRefreshTimer = timer(initialDelay = TRUST_LIST_REFRESH_INTERVAL, period = TRUST_LIST_REFRESH_INTERVAL) {
					certificateVerificationController.refreshTrustList(lifecycle.coroutineScope)
				}
			}

			@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
			fun onStop() {
				trustListRefreshTimer?.cancel()
			}
		})
	}

	fun getCertificateVerificationController(): CertificateVerificationController {
		requireInitialized()
		return certificateVerificationController
	}

	private fun requireInitialized() {
		if (!isInitialized) {
			throw IllegalStateException("CovidCertificateSdk must be initialized by calling init(context)")
		}
	}

	private fun createRetrofit(context: Context): Retrofit {
		val okHttpBuilder = OkHttpClient.Builder()
			.certificatePinner(CertificatePinning.pinner)
			.addInterceptor(UserAgentInterceptor(Config.userAgent))

		val cacheSize = 5 * 1024 * 1024 // 5 MB
		val cache = Cache(context.cacheDir, cacheSize.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		return Retrofit.Builder()
			.baseUrl(BuildConfig.BASE_URL_TRUST_LIST)
			.client(okHttpBuilder.build())
			.addConverterFactory(MoshiConverterFactory.create())
			.build()
	}

}