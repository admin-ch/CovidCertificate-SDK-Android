/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.repository

import ch.admin.bag.covidcertificate.eval.data.TrustListStore
import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.TrustList
import ch.admin.bag.covidcertificate.eval.net.CertificateService
import ch.admin.bag.covidcertificate.eval.net.RevocationService
import ch.admin.bag.covidcertificate.eval.net.RuleSetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class TrustListRepository(
	private val certificateService: CertificateService,
	private val revocationService: RevocationService,
	private val ruleSetService: RuleSetService,
	private val store: TrustListStore,
) {

	companion object {
		private const val HEADER_UP_TO_DATE = "up-to-date"
		private const val HEADER_NEXT_SINCE = "X-Next-Since"
	}

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
	}

	/**
	 * Get the trust list from the provider or null if at least one of the values is not set
	 */
	fun getTrustList(): TrustList? {
		return if (store.areSignaturesValid() && store.areRevokedCertificatesValid() && store.areRuleSetsValid()) {
			val signatures = store.certificateSignatures!!
			val revokedCertificates = store.revokedCertificates!!
			val ruleSet = store.ruleset!!
			TrustList(signatures, revokedCertificates, ruleSet)
		} else {
			null
		}
	}

	private suspend fun refreshCertificateSignatures(forceRefresh: Boolean, isRecursive: Boolean = false): Unit =
		withContext(Dispatchers.IO) {
			val shouldLoadSignatures = forceRefresh || !store.areSignaturesValid()
			if (shouldLoadSignatures) {
				// Load the active certificate key IDs
				val activeCertificatesResponse = certificateService.getActiveSignerCertificateKeyIds()
				val activeCertificatesBody = activeCertificatesResponse.body()
				if (activeCertificatesResponse.isSuccessful && activeCertificatesBody != null) {
					val allCertificates = store.certificateSignatures?.certs?.toMutableList() ?: mutableListOf()
					var since = store.certificatesSinceHeader

					// Get the signer certificates as long as there are entries in the response list
					var count = 0
					var certificatesResponse = certificateService.getSignerCertificates(since)
					while (certificatesResponse.isSuccessful && certificatesResponse.body()?.certs?.isNullOrEmpty() == false) {
						allCertificates.addAll(certificatesResponse.body()?.certs ?: emptyList())

						// Check if the request returns an up to date header, the next since has changed or we reached a loop count threshold
						val isUpToDate = certificatesResponse.headers()[HEADER_UP_TO_DATE].toBoolean()
						since = certificatesResponse.headers()[HEADER_NEXT_SINCE]
						if (isUpToDate || count >= 20) break

						count++
						certificatesResponse = certificateService.getSignerCertificates(since)
					}

					// Filter only active certificates and store them
					val activeCertificateKeyIds = activeCertificatesBody.activeKeyIds
					val activeCertificates = allCertificates.filter { activeCertificateKeyIds.contains(it.keyId) }

					if (allCertificates.size != activeCertificateKeyIds.size) {
						if (!isRecursive) {
							// If the list sizes don't match, try once to refresh everything again
							refreshCertificateSignatures(forceRefresh = true, isRecursive = true)
						} else {
							// If the list sizes still don't match after a full refresh, abort
							return@withContext
						}
					} else {
						// Only replace the stored certificates if the list sizes match
						store.certificatesSinceHeader = since
						store.certificateSignatures = Jwks(activeCertificates)

						val validDuration = activeCertificatesBody.validDuration
						val newValidUntil = Instant.now().plus(validDuration, ChronoUnit.MILLIS).toEpochMilli()
						store.certificateSignaturesValidUntil = newValidUntil
					}
				}
			}
		}

	private suspend fun refreshRevokedCertificates(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadRevokedCertificates = forceRefresh || !store.areRevokedCertificatesValid()
		if (shouldLoadRevokedCertificates) {
			val response = revocationService.getRevokedCertificates()
			val body = response.body()
			if (response.isSuccessful && body != null) {
				store.revokedCertificates = body
				val newValidUntil = Instant.now().plus(body.validDuration, ChronoUnit.MILLIS).toEpochMilli()
				store.revokedCertificatesValidUntil = newValidUntil
			}
		}
	}

	private suspend fun refreshRuleSet(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val shouldLoadRuleSet = forceRefresh || !store.areRuleSetsValid()
		if (shouldLoadRuleSet) {
			val response = ruleSetService.getRuleset()
			val body = response.body()
			if (response.isSuccessful && body != null) {
				store.ruleset = body
				val newValidUntil = Instant.now().plus(body.validDuration, ChronoUnit.MILLIS).toEpochMilli()
				store.rulesetValidUntil = newValidUntil
			}
		}
	}

}