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

import ch.admin.bag.covidcertificate.eval.data.TrustListProvider
import ch.admin.bag.covidcertificate.eval.models.TrustList
import ch.admin.bag.covidcertificate.eval.net.RevocationService
import ch.admin.bag.covidcertificate.eval.net.RuleSetService
import ch.admin.bag.covidcertificate.eval.net.SignatureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class TrustListRepository(
	private val signatureService: SignatureService,
	private val revocationService: RevocationService,
	private val ruleSetService: RuleSetService,
	private val provider: TrustListProvider
) {

	companion object {
		private const val DEFAULT_VALIDITY_IN_DAYS = 2L
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
			async { refreshCertificateSignatures(forceRefresh) },
			async { refreshRevokedCertificates(forceRefresh) },
			async { refreshRuleSet(forceRefresh) }
		).awaitAll()
	}

	/**
	 * Get the trust list from the provider or null if at least one of the values is not set
	 */
	fun getTrustList(): TrustList? {
		val signatures = provider.certificateSignatures
		val revokedCertificates = provider.revokedCertificates
		val ruleSet = provider.ruleset

		return if (signatures != null && revokedCertificates != null && ruleSet != null) {
			TrustList(signatures, revokedCertificates, ruleSet)
		} else {
			null
		}
	}

	private suspend fun refreshCertificateSignatures(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val now = Instant.now().toEpochMilli()
		val shouldLoadSignatures = forceRefresh
				|| provider.certificateSignatures == null
				|| provider.certificateSignaturesValidUntil <= now

		if (shouldLoadSignatures) {
			val response = signatureService.getJwks()
			if (response.isSuccessful && response.body() != null) {
				provider.certificateSignatures = response.body()
				val newValidUntil = Instant.now().plus(DEFAULT_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli()
				provider.certificateSignaturesValidUntil = newValidUntil
			}
		}
	}

	private suspend fun refreshRevokedCertificates(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val now = Instant.now().toEpochMilli()
		val shouldLoadRevokedCertificates = forceRefresh
				|| provider.revokedCertificates == null
				|| provider.revokedCertificatesValidUntil <= now

		if (shouldLoadRevokedCertificates) {
			val response = revocationService.getRevokedCertificates()
			if (response.isSuccessful && response.body() != null) {
				provider.revokedCertificates = response.body()
				val newValidUntil = Instant.now().plus(DEFAULT_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli()
				provider.revokedCertificatesValidUntil = newValidUntil
			}
		}
	}

	private suspend fun refreshRuleSet(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
		val now = Instant.now().toEpochMilli()
		val shouldLoadRuleSet = forceRefresh || provider.ruleset == null || provider.rulesetValidUntil <= now

		if (shouldLoadRuleSet) {
			val response = ruleSetService.getRuleset()
			if (response.isSuccessful && response.body() != null) {
				provider.ruleset = response.body()
				val newValidUntil = Instant.now().plus(DEFAULT_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli()
				provider.rulesetValidUntil = newValidUntil
			}
		}
	}

}