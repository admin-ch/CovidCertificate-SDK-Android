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
import ch.admin.bag.covidcertificate.eval.data.MetadataStorage
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.eval.metadata.ProductMetadataController
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.eval.net.service.CertificateService
import ch.admin.bag.covidcertificate.eval.net.service.MetadataService
import ch.admin.bag.covidcertificate.eval.net.RetrofitFactory
import ch.admin.bag.covidcertificate.eval.net.service.RevocationService
import ch.admin.bag.covidcertificate.eval.net.service.RuleSetService
import ch.admin.bag.covidcertificate.eval.repository.MetadataRepository
import ch.admin.bag.covidcertificate.eval.repository.TrustListRepository
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerificationController
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

object CovidCertificateSdk {

	private const val ROOT_CA_TYPE = "X.509"
	private const val ASSET_PATH_ROOT_CA = "swiss_governmentrootcaii.der"

	private val TRUST_LIST_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1L)

	private lateinit var certificateVerificationController: CertificateVerificationController
	private lateinit var productMetadataController: ProductMetadataController
	private var isInitialized = false
	private var trustListRefreshTimer: Timer? = null
	private var sdkLifecycleObserver: SdkLifecycleObserver? = null

	fun init(context: Context) {
		val retrofit = RetrofitFactory().create(context)
		val certificateService = retrofit.create(CertificateService::class.java)
		val revocationService = retrofit.create(RevocationService::class.java)
		val ruleSetService = retrofit.create(RuleSetService::class.java)

		val certificateStorage = CertificateSecureStorage.getInstance(context)
		val trustListRepository = TrustListRepository(certificateService, revocationService, ruleSetService, certificateStorage)

		val nationalRulesVerifier = NationalRulesVerifier(context)
		val certificateVerifier = CertificateVerifier(nationalRulesVerifier)
		certificateVerificationController = CertificateVerificationController(trustListRepository, certificateVerifier)

		val metadataService = retrofit.create(MetadataService::class.java)
		val metadataStorage = MetadataStorage.getInstance(context)
		val metadataRepository = MetadataRepository(metadataService, metadataStorage)
		productMetadataController = ProductMetadataController(metadataRepository)

		isInitialized = true
	}

	fun registerWithLifecycle(lifecycle: Lifecycle) {
		requireInitialized()

		sdkLifecycleObserver = SdkLifecycleObserver(lifecycle)
		sdkLifecycleObserver?.register(lifecycle)
	}

	fun unregisterWithLifecycle(lifecycle: Lifecycle) {
		requireInitialized()

		sdkLifecycleObserver?.unregister(lifecycle)
		sdkLifecycleObserver = null
	}

	fun decode(qrCodeData: String): DecodeState {
		return CertificateDecoder.decode(qrCodeData)
	}

	fun getCertificateVerificationController(): CertificateVerificationController {
		requireInitialized()
		return certificateVerificationController
	}

	fun getRootCa(context: Context): X509Certificate {
		val certificateFactory = CertificateFactory.getInstance(ROOT_CA_TYPE)
		val inputStream = context.assets.open(ASSET_PATH_ROOT_CA)
		return certificateFactory.generateCertificate(inputStream) as X509Certificate
	}

	fun getExpectedCommonName() = BuildConfig.LEAF_CERT_CN

	private fun requireInitialized() {
		if (!isInitialized) {
			throw IllegalStateException("CovidCertificateSdk must be initialized by calling init(context)")
		}
	}

	internal class SdkLifecycleObserver(private val lifecycle: Lifecycle) : LifecycleObserver {
		fun register(lifecycle: Lifecycle) {
			lifecycle.addObserver(this)
		}

		fun unregister(lifecycle: Lifecycle) {
			lifecycle.removeObserver(this)
		}

		@OnLifecycleEvent(Lifecycle.Event.ON_START)
		fun onStart() {
			// Schedule a timer on app start to periodicaly refresh the trust list while the app is in the foreground
			trustListRefreshTimer?.cancel()
			trustListRefreshTimer = timer(initialDelay = TRUST_LIST_REFRESH_INTERVAL, period = TRUST_LIST_REFRESH_INTERVAL) {
				certificateVerificationController.refreshTrustList(lifecycle.coroutineScope)
			}

			// Refresh the products metadata once on app start
			productMetadataController.refreshProductsMetadata(lifecycle.coroutineScope)
		}

		@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
		fun onStop() {
			trustListRefreshTimer?.cancel()
		}
	}

}