/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.repository

import ch.admin.bag.covidcertificate.sdk.android.data.TrustListStore
import ch.admin.bag.covidcertificate.sdk.android.net.service.CertificateService
import ch.admin.bag.covidcertificate.sdk.android.net.service.RevocationService
import ch.admin.bag.covidcertificate.sdk.android.net.service.RuleSetService
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.ActiveModes
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class TrustListRepository(
	private val certificateService: CertificateService,
	private val revocationService: RevocationService,
	private val ruleSetService: RuleSetService,
	private val store: TrustListStore,
	private val provider: () -> TimeShiftDetectionConfig
) {

	companion object {
		private const val HEADER_UP_TO = "up-to"
		private const val HEADER_UP_TO_DATE = "up-to-date"
		private const val HEADER_NEXT_SINCE = "X-Next-Since"
		private const val HEADER_DATE = "Date"
		private const val HEADER_AGE = "Age"
	}

	val activeModes = MutableStateFlow(getCurrentActiveModes())

	/**
	 * Refresh the trust list if necessary. This will check for the presence and validity of the certificate signatures,
	 * revoked certificates and rule set and load them from the backend if necessary. Set the [forceRefresh] flag to always load
	 * the data from the server.
	 *
	 * @param forceRefresh False to only load data from the server if it is missing or outdated, true to always load from the server
	 */
	suspend fun refreshTrustList(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		listOf(
			launch { refreshCertificateSignatures(forceRefresh) },
			launch { refreshRevokedCertificates(forceRefresh) },
			launch { refreshRuleSet(forceRefresh) }
		).joinAll()
		activeModes.value = getCurrentActiveModes()
	}

	private fun getCurrentActiveModes(): List<ActiveModes> {
		return store.ruleset?.modeRules?.activeModes ?: listOf(ActiveModes("THREE_G", "3G"))
	}

	/**
	 * Get the trust list from the provider or null if at least one of the values is not set
	 */
	fun getTrustList(): TrustList? {
		return try {
			if (store.areSignaturesValid() && store.areRevokedCertificatesValid() && store.areRuleSetsValid()) {
				val signatures = store.certificateSignatures!!
				val revokedCertificates = store.revokedCertificates
				val ruleSet = store.ruleset!!
				TrustList(signatures, revokedCertificates, ruleSet)
			} else {
				null
			}
		} catch (e: Exception) {
			null
		}
	}

	private suspend fun refreshCertificateSignatures(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadSignatures = forceRefresh || !store.areSignaturesValid()
		if (shouldLoadSignatures) {
			// Load the active certificate key IDs
			val activeCertificatesResponse = certificateService.getActiveSignerCertificateKeyIds()
			val activeCertificatesBody = activeCertificatesResponse.body()
			if (activeCertificatesResponse.isSuccessful && activeCertificatesBody != null) {
				val allCertificates = store.certificateSignatures?.certs?.toMutableList() ?: mutableListOf()
				var since = store.certificatesSinceHeader
				val upTo = activeCertificatesResponse.body()?.upTo?.toLong()
					?: activeCertificatesResponse.headers()[HEADER_UP_TO]?.toLongOrNull()
					?: 0

				// Get the signer certificates as long as there are entries in the response list
				var count = 0
				var certificatesResponse =
					certificateService.getSignerCertificates(getCacheControlParameter(forceRefresh), upTo, since)
				while (certificatesResponse.isSuccessful && certificatesResponse.body()?.certs?.isNullOrEmpty() == false) {
					allCertificates.addAll(certificatesResponse.body()?.certs ?: emptyList())

					// Check if the request returns an up to date header, the next since has changed or we reached a loop count threshold
					val isUpToDate = certificatesResponse.headers()[HEADER_UP_TO_DATE].toBoolean()
					since = certificatesResponse.headers()[HEADER_NEXT_SINCE]

					if (isUpToDate || count >= 20) break

					count++
					certificatesResponse =
						certificateService.getSignerCertificates(getCacheControlParameter(forceRefresh), upTo, since)
				}

				// Filter only active certificates and store them
				val activeCertificateKeyIds = activeCertificatesBody.activeKeyIds
				val activeCertificates = allCertificates.filter { activeCertificateKeyIds.contains(it.keyId) }

				// Only replace the stored certificates if the list sizes match
				store.certificatesSinceHeader = since
				store.certificatesUpToHeader = upTo
				store.certificateSignatures = Jwks(activeCertificates)

				val validDuration = activeCertificatesBody.validDuration
				val newValidUntil = Instant.now().plus(validDuration, ChronoUnit.MILLIS).toEpochMilli()
				store.certificateSignaturesValidUntil = newValidUntil
			}
		}
	}

	private suspend fun refreshRevokedCertificates(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadRevokedCertificates = forceRefresh || !store.areRevokedCertificatesValid()
		if (shouldLoadRevokedCertificates) {
			var since = store.revokedCertificatesSinceHeader

			// Get the revocation list as long as the request is successful
			var revocationListResponse = revocationService.getRevokedCertificates(getCacheControlParameter(forceRefresh), since)
			while (revocationListResponse.isSuccessful) {

				val validDuration = revocationListResponse.body()?.validDuration

				// Check if the request returns an up to date header, the next since has changed or we reached a loop count threshold
				val isUpToDate = revocationListResponse.headers()[HEADER_UP_TO_DATE].toBoolean()
				since = revocationListResponse.headers()[HEADER_NEXT_SINCE]

				detectTimeshift(revocationListResponse)

				// Add all revoked certificates from the response to the list and save the validity duration of this response
				store.revokedCertificates.addCertificates(revocationListResponse.body()?.revokedCerts.orEmpty())
				store.revokedCertificatesSinceHeader = since

				if (validDuration != null) {
					val newValidUntil = Instant.now().plus(validDuration, ChronoUnit.MILLIS).toEpochMilli()
					store.revokedCertificatesValidUntil = newValidUntil
				}

				if (isUpToDate) break

				revocationListResponse = revocationService.getRevokedCertificates(getCacheControlParameter(forceRefresh), since)
			}
		}
	}

	private suspend fun refreshRuleSet(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadRuleSet = forceRefresh || !store.areRuleSetsValid()
		if (shouldLoadRuleSet) {
			val response = ruleSetService.getRuleset(getCacheControlParameter(forceRefresh))
			val body = response.body()
			if (response.isSuccessful && body != null) {
				store.ruleset = body
				val newValidUntil = Instant.now().plus(body.validDuration, ChronoUnit.MILLIS).toEpochMilli()
				store.rulesetValidUntil = newValidUntil
			}
		}
	}

	private fun detectTimeshift(response: Response<out Any>) {
		val timeShiftDetectionConfig = provider()
		if (timeShiftDetectionConfig.enabled) {
			val serverTime = response.headers().getInstant(HEADER_DATE)
			val ageString: String? = response.headers()[HEADER_AGE]
			if (serverTime == null) return
			val age: Long = if (ageString != null) 1000 * ageString.toLong() else 0
			val liveServerTime = serverTime.toEpochMilli() + age
			val systemTime = System.currentTimeMillis()
			if (Math.abs(systemTime - liveServerTime) > timeShiftDetectionConfig.allowedServerTimeDiff) {
				throw ServerTimeOffsetException()
			}
		}
	}

	private fun getCacheControlParameter(forceRefresh: Boolean): String? = if (forceRefresh) "no-cache" else null
}

data class TimeShiftDetectionConfig(var enabled: Boolean, var allowedServerTimeDiff: Long = DEFAULT_ALLOWED_SERVER_TIME_DIFF) {
	companion object {
		const val DEFAULT_ALLOWED_SERVER_TIME_DIFF = 2 * 60 * 60 * 1000L; //2h
	}
}

class ServerTimeOffsetException() : Exception()
