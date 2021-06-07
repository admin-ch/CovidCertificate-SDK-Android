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
import ch.admin.bag.covidcertificate.eval.models.Jwk
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
	private val store: TrustListStore
) {

	companion object {
		private const val DEFAULT_VALIDITY_IN_DAYS = 2L
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
		val signatures = store.certificateSignatures
		val revokedCertificates = store.revokedCertificates
		val ruleSet = store.ruleset

		return if (signatures != null && revokedCertificates != null && ruleSet != null) {
			TrustList(signatures, revokedCertificates, ruleSet)
		} else {
			null
		}
	}

	private suspend fun refreshCertificateSignatures(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val now = Instant.now().toEpochMilli()
		val shouldLoadSignatures = forceRefresh
				|| store.certificateSignatures == null
				|| store.certificateSignaturesValidUntil <= now

		if (shouldLoadSignatures) {
			// Load the active certificate key IDs
			val activeCertificatesResponse = certificateService.getActiveSignerCertificateKeyIds()
			if (activeCertificatesResponse.isSuccessful && activeCertificatesResponse.body() != null) {
				val activeCertificateKeyIds = activeCertificatesResponse.body()?.activeKeyIds ?: emptyList()
				var since: Long? = null
				val allCertificates = mutableListOf<Jwk>()

				// Get the signer certificates as long as there are entries in the response list
				var certificatesResponse = certificateService.getSignerCertificates(since)
				while (certificatesResponse.isSuccessful && certificatesResponse.body()?.certs?.isNullOrEmpty() == false) {
					allCertificates.addAll(certificatesResponse.body()?.certs ?: emptyList())
					since = certificatesResponse.headers()[HEADER_NEXT_SINCE]?.toLong()
					certificatesResponse = certificateService.getSignerCertificates(since)
				}

				// Filter out non-active certificates and store them
				val activeCertificates = allCertificates.filter { activeCertificateKeyIds.contains(it.keyId) }
				store.certificateSignatures = Jwks(activeCertificates)
				val newValidUntil = Instant.now().plus(DEFAULT_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli()
				store.certificateSignaturesValidUntil = newValidUntil
			}
		}
	}

	private suspend fun refreshRevokedCertificates(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val now = Instant.now().toEpochMilli()
		val shouldLoadRevokedCertificates = forceRefresh
				|| store.revokedCertificates == null
				|| store.revokedCertificatesValidUntil <= now

		if (shouldLoadRevokedCertificates) {
			val response = revocationService.getRevokedCertificates()
			if (response.isSuccessful && response.body() != null) {
				store.revokedCertificates = response.body()
				val newValidUntil = Instant.now().plus(DEFAULT_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli()
				store.revokedCertificatesValidUntil = newValidUntil
			}
		}
	}

	private suspend fun refreshRuleSet(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val now = Instant.now().toEpochMilli()
		val shouldLoadRuleSet = forceRefresh || store.ruleset == null || store.rulesetValidUntil <= now

		if (shouldLoadRuleSet) {
			val response = ruleSetService.getRuleset()
			if (response.isSuccessful && response.body() != null) {
				store.ruleset = response.body()
				val newValidUntil = Instant.now().plus(DEFAULT_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli()
				store.rulesetValidUntil = newValidUntil
			}
		}
	}

}