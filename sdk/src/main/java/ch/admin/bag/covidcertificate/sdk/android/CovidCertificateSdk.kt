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

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.coroutineScope
import ch.admin.bag.covidcertificate.sdk.android.data.CertificateSecureStorage
import ch.admin.bag.covidcertificate.sdk.android.data.MetadataStorage
import ch.admin.bag.covidcertificate.sdk.android.data.base64.AndroidUtilBase64
import ch.admin.bag.covidcertificate.sdk.android.metadata.ProductMetadataController
import ch.admin.bag.covidcertificate.sdk.android.models.VerifierCertificateHolder
import ch.admin.bag.covidcertificate.sdk.android.net.RetrofitFactory
import ch.admin.bag.covidcertificate.sdk.android.net.service.CertificateService
import ch.admin.bag.covidcertificate.sdk.android.net.service.MetadataService
import ch.admin.bag.covidcertificate.sdk.android.net.service.RevocationService
import ch.admin.bag.covidcertificate.sdk.android.net.service.RuleSetService
import ch.admin.bag.covidcertificate.sdk.android.repository.MetadataRepository
import ch.admin.bag.covidcertificate.sdk.android.repository.TrustListRepository
import ch.admin.bag.covidcertificate.sdk.android.verification.CertificateVerificationController
import ch.admin.bag.covidcertificate.sdk.android.verification.state.VerifierDecodeState
import ch.admin.bag.covidcertificate.sdk.android.verification.task.VerifierCertificateVerificationTask
import ch.admin.bag.covidcertificate.sdk.android.verification.task.WalletCertificateVerificationTask
import ch.admin.bag.covidcertificate.sdk.core.data.base64.Base64Impl
import ch.admin.bag.covidcertificate.sdk.core.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier
import ch.admin.bag.covidcertificate.verifier.sdk.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
	private lateinit var connectivityManager: ConnectivityManager
	private var isInitialized = false
	private var trustListRefreshTimer: Timer? = null
	private var sdkLifecycleObserver: SdkLifecycleObserver? = null

	fun init(context: Context) {
		// Replace the java.util.Base64 based provider in the core SDK with the android.util.Base64 provider because the Java one
		// was added in Android SDK level 26 and would lead to a ClassNotFoundException on earlier versions
		Base64Impl.setBase64Provider(AndroidUtilBase64())

		val retrofit = RetrofitFactory().create(context)
		val certificateService = retrofit.create(CertificateService::class.java)
		val revocationService = retrofit.create(RevocationService::class.java)
		val ruleSetService = retrofit.create(RuleSetService::class.java)

		val certificateStorage = CertificateSecureStorage.getInstance(context)
		val trustListRepository = TrustListRepository(certificateService, revocationService, ruleSetService, certificateStorage)

		val certificateVerifier = CertificateVerifier()
		certificateVerificationController = CertificateVerificationController(trustListRepository, certificateVerifier)

		val metadataService = retrofit.create(MetadataService::class.java)
		val metadataStorage = MetadataStorage.getInstance(context)
		val metadataRepository = MetadataRepository(metadataService, metadataStorage)
		productMetadataController = ProductMetadataController(metadataRepository)

		connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

	fun refreshTrustList(coroutineScope: CoroutineScope, onCompletionCallback: () -> Unit = {}, onErrorCallback: () -> Unit = {}) {
		requireInitialized()
		certificateVerificationController.refreshTrustList(coroutineScope, onCompletionCallback, onErrorCallback)
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

	object Verifier {
		fun decode(encodedData: String): VerifierDecodeState {
			requireInitialized()
			val decodeState = CertificateDecoder.decode(encodedData)
			return when (decodeState) {
				is DecodeState.SUCCESS -> {
					val certificateHolder = decodeState.certificateHolder
					val verifierCertificateHolder = VerifierCertificateHolder(certificateHolder)
					VerifierDecodeState.SUCCESS(verifierCertificateHolder)
				}
				is DecodeState.ERROR -> VerifierDecodeState.ERROR(decodeState.error)
			}
		}

		fun verify(
			certificateHolder: VerifierCertificateHolder,
			coroutineScope: CoroutineScope,
			ignoreLocalTrustList: Boolean = false
		): Flow<VerificationState> {
			requireInitialized()
			val task = VerifierCertificateVerificationTask(certificateHolder, connectivityManager, ignoreLocalTrustList)
			certificateVerificationController.enqueue(task, coroutineScope)
			return task.verificationStateFlow
		}
	}

	object Wallet {
		fun decode(encodedData: String): DecodeState {
			requireInitialized()
			return CertificateDecoder.decode(encodedData)
		}

		fun verify(
			certificateHolder: CertificateHolder,
			coroutineScope: CoroutineScope,
			ignoreLocalTrustList: Boolean = false
		): Flow<VerificationState> {
			requireInitialized()
			val task = WalletCertificateVerificationTask(certificateHolder, connectivityManager, ignoreLocalTrustList)
			certificateVerificationController.enqueue(task, coroutineScope)
			return task.verificationStateFlow
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