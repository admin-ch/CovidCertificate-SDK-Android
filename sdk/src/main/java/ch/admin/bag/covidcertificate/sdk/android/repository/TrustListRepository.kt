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
import ch.admin.bag.covidcertificate.sdk.android.exceptions.ServerTimeOffsetException
import ch.admin.bag.covidcertificate.sdk.android.net.service.CertificateService
import ch.admin.bag.covidcertificate.sdk.android.net.service.RevocationService
import ch.admin.bag.covidcertificate.sdk.android.net.service.RuleSetService
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.ActiveModes
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

internal class TrustListRepository(
	private val certificateService: CertificateService,
	private val revocationService: RevocationService,
	private val ruleSetService: RuleSetService,
	private val store: TrustListStore,
	private val provider: () -> TimeShiftDetectionConfig
) {

	companion object {
		const val COUNTRY_CODE_SWITZERLAND = "CH"
		private const val HEADER_UP_TO = "up-to"
		private const val HEADER_UP_TO_DATE = "up-to-date"
		private const val HEADER_NEXT_SINCE = "X-Next-Since"
		private const val HEADER_DATE = "Date"
		private const val HEADER_AGE = "Age"
	}

	val walletActiveModes = MutableStateFlow<List<ActiveModes>>(emptyList())
	val verifierActiveModes = MutableStateFlow<List<ActiveModes>>(emptyList())

	init {
		// The national rules store uses a room database underneath, which should not be accessed on the main thread
		// GlobalScope is fine here as this happens during the SDK initialization and is fine as a fire-and-forget coroutine
		GlobalScope.launch(Dispatchers.IO) {
			val switzerlandRuleSet = store.nationalRules.getRuleSetForCountry(COUNTRY_CODE_SWITZERLAND)
			updateActiveModes(switzerlandRuleSet)
		}
	}

	/**
	 * Refresh the trust list if necessary. This will check for the presence and validity of the certificate signatures,
	 * revoked certificates and rule set and load them from the backend if necessary. Set the [forceRefresh] flag to always load
	 * the data from the server.
	 *
	 * @param forceRefresh False to only load data from the server if it is missing or outdated, true to always load from the server
	 * @throws HttpException If [forceRefresh] is true and any of the requests returns a non-2xx HTTP response
	 */
	suspend fun refreshTrustList(countryCode: String = COUNTRY_CODE_SWITZERLAND, forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		listOf(
			launch { refreshCertificateSignatures(forceRefresh) },
			launch { refreshRevokedCertificates(forceRefresh) },
			launch { refreshRuleSet(countryCode, forceRefresh) }
		).joinAll()

		val switzerlandRuleSet = store.nationalRules.getRuleSetForCountry(COUNTRY_CODE_SWITZERLAND)
		updateActiveModes(switzerlandRuleSet)
	}

	/**
	 * Get the list of country codes available for foreign rules check
	 *
	 * @param forceRefresh False to only load data from the server if it is missing or outdated, true to always load from the server
	 */
	suspend fun getForeignRulesCountryCodes(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadCountryCodes = forceRefresh || !store.areForeignRulesCountryCodesValid()
		if (shouldLoadCountryCodes) {
			val response = ruleSetService.getForeignRulesCountryCodes(getCacheControlParameter(forceRefresh))
			val body = response.body()
			return@withContext if (response.isSuccessful && body != null) {
				val countryCodes = body.countries.toSet()
				store.foreignRulesCountryCodes = countryCodes
				store.foreignRulesCountryCodesValidUntil = Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli() // TODO How long?
				countryCodes
			} else {
				throw HttpException(response)
			}
		} else {
			return@withContext store.foreignRulesCountryCodes
		}
	}

	/**
	 * Get the trust list from the provider or null if at least one of the values is not set
	 * @param countryCode An optional ISO-3166 alpha-2 country code to verify against a specific country's national rules. Defaults to "CH"
	 */
	suspend fun getTrustList(countryCode: String = COUNTRY_CODE_SWITZERLAND): TrustList? {
		return try {
			if (store.areSignaturesValid() && store.areRevokedCertificatesValid() && store.areRuleSetsValid(countryCode)) {
				val signatures = requireNotNull(store.certificateSignatures)
				val revokedCertificates = store.revokedCertificates
				val ruleSet = requireNotNull(store.nationalRules.getRuleSetForCountry(countryCode))
				TrustList(signatures, revokedCertificates, ruleSet)
			} else {
				null
			}
		} catch (e: Exception) {
			null
		}
	}

	private fun updateActiveModes(ruleSet: RuleSet?) {
		walletActiveModes.value = ruleSet?.modeRules?.walletActiveModes
			?: ruleSet?.modeRules?.activeModes
					?: listOf(ActiveModes("THREE_G", "3G"))

		verifierActiveModes.value = ruleSet?.modeRules?.verifierActiveModes
			?: listOf(ActiveModes("THREE_G", "3G"))
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
				if (forceRefresh && !certificatesResponse.isSuccessful) {
					throw HttpException(certificatesResponse)
				}

				while (certificatesResponse.isSuccessful && certificatesResponse.body()?.certs?.isNullOrEmpty() == false) {
					allCertificates.addAll(certificatesResponse.body()?.certs ?: emptyList())

					// Check if the request returns an up to date header, the next since has changed or we reached a loop count threshold
					val isUpToDate = certificatesResponse.headers()[HEADER_UP_TO_DATE].toBoolean()
					since = certificatesResponse.headers()[HEADER_NEXT_SINCE]

					if (isUpToDate || count >= 20) break

					count++
					certificatesResponse =
						certificateService.getSignerCertificates(getCacheControlParameter(forceRefresh), upTo, since)
					if (forceRefresh && !certificatesResponse.isSuccessful) {
						throw HttpException(certificatesResponse)
					}
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
			} else if (forceRefresh && !activeCertificatesResponse.isSuccessful) {
				throw HttpException(activeCertificatesResponse)
			}
		}
	}

	private suspend fun refreshRevokedCertificates(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadRevokedCertificates = forceRefresh || !store.areRevokedCertificatesValid()
		if (shouldLoadRevokedCertificates) {
			var since = store.revokedCertificatesSinceHeader

			// Get the revocation list as long as the request is successful
			var revocationListResponse = revocationService.getRevokedCertificates(getCacheControlParameter(forceRefresh), since)
			detectTimeshift(revocationListResponse)

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

			// If this is a force refresh and the response was not successful (and no timeshift was detected), throw an HttpException
			if (forceRefresh && !revocationListResponse.isSuccessful) {
				throw HttpException(revocationListResponse)
			}
		}
	}

	private suspend fun refreshRuleSet(countryCode: String, forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		if (countryCode == COUNTRY_CODE_SWITZERLAND) {
			refreshRuleSetForCountry(countryCode, forceRefresh) {
				ruleSetService.getRuleset(getCacheControlParameter(forceRefresh = true))
			}
		} else {
			refreshRuleSetForCountry(countryCode, forceRefresh) {
				ruleSetService.getForeignRules(getCacheControlParameter(forceRefresh = true), countryCode)
			}
		}
	}

	private suspend fun refreshRuleSetForCountry(
		countryCode: String,
		forceRefresh: Boolean,
		serviceCall: suspend () -> Response<RuleSet>
	) {
		val shouldLoadRuleSet = forceRefresh || !store.areRuleSetsValid(countryCode)
		if (shouldLoadRuleSet) {
			val response = serviceCall.invoke()
			val body = response.body()
			if (response.isSuccessful && body != null) {
				val newValidUntil = Instant.now().plus(body.validDuration, ChronoUnit.MILLIS).toEpochMilli()
				store.nationalRules.addRuleSetForCountry(countryCode, newValidUntil, body)
			} else if (forceRefresh && !response.isSuccessful) {
				throw HttpException(response)
			}
		}
	}

	private fun detectTimeshift(response: Response<*>) {
		val timeShiftDetectionConfig = provider()
		if (timeShiftDetectionConfig.enabled) {
			val serverTime = response.headers().getInstant(HEADER_DATE)
			val ageString: String? = response.headers()[HEADER_AGE]
			if (serverTime == null) return
			val age: Long = if (ageString != null) 1000 * ageString.toLong() else 0
			val liveServerTime = serverTime.toEpochMilli() + age
			val systemTime = System.currentTimeMillis()
			if (abs(systemTime - liveServerTime) > timeShiftDetectionConfig.allowedServerTimeDiff) {
				throw ServerTimeOffsetException()
			}
		}
	}

	private fun getCacheControlParameter(forceRefresh: Boolean): String? = if (forceRefresh) "no-cache" else null
}

data class TimeShiftDetectionConfig(var enabled: Boolean, var allowedServerTimeDiff: Long = DEFAULT_ALLOWED_SERVER_TIME_DIFF) {
	companion object {
		const val DEFAULT_ALLOWED_SERVER_TIME_DIFF = 2 * 60 * 60 * 1000L //2h
	}
}
