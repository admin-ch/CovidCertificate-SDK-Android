/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.data

import android.content.Context
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.SdkEnvironment
import ch.admin.bag.covidcertificate.sdk.android.repository.CovidCertificateDatabaseRepository
import ch.admin.bag.covidcertificate.sdk.android.repository.TrustListRepository
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificatesStore
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant

internal class CertificateSecureStorage private constructor(private val context: Context) : TrustListStore {

	companion object : SingletonHolder<CertificateSecureStorage, Context>(::CertificateSecureStorage) {
		private const val PREFERENCES_NAME = "TrustListSecureStorage"
		private const val FILE_PATH_CERTIFICATE_SIGNATURES = "certificate_signatures.json"
		private const val FILE_PATH_RULESET = "national_ruleset_v2.json"

		private const val KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL = "KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL"
		private const val KEY_CERTIFICATE_SIGNATURES_SINCE_HEADER = "KEY_CERTIFICATE_SIGNATURES_SINCE_HEADER"
		private const val KEY_CERTIFICATE_SIGNATURES_UP_TO_HEADER = "KEY_CERTIFICATE_SIGNATURES_UP_TO_HEADER"
		private const val KEY_REVOKED_CERTIFICATES_VALID_UNTIL = "KEY_REVOKED_CERTIFICATES_VALID_UNTIL_V2"
		private const val KEY_REVOKED_CERTIFICATES_SINCE_HEADER = "KEY_REVOKED_CERTIFICATES_SINCE_HEADER_V2"
		private const val KEY_RULESET_VALID_UNTIL = "KEY_RULESET_VALID_UNTIL_V2"
		private const val KEY_FOREIGN_RULES_COUNTRY_CODES_VALID_UNTIL = "KEY_FOREIGN_RULES_COUNTRY_CODES_VALID_UNTIL"
		private const val KEY_FOREIGN_RULES_COUNTRY_CODES = "KEY_FOREIGN_RULES_COUNTRY_CODES"

		private val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		private val jwksAdapter = moshi.adapter(Jwks::class.java)
		private val rulesetAdapter = moshi.adapter(RuleSet::class.java)
	}

	private val certificateFileStorage = FileStorage(FILE_PATH_CERTIFICATE_SIGNATURES)
	private val ruleSetFileStorage = FileStorage(FILE_PATH_RULESET)

	private val preferences = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES_NAME)

	override var revokedCertificates: RevokedCertificatesStore = CovidCertificateDatabaseRepository.getInstance(context)
	override var nationalRules: NationalRulesStore = CovidCertificateDatabaseRepository.getInstance(context)

	override var certificateSignaturesValidUntil: Long
		get() = preferences.getLong(KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL, value).apply()
		}

	override var certificateSignatures: Jwks? = null
		get() {
			if (field == null) {
				field = certificateFileStorage.read(context)?.let { jwksAdapter.fromJson(it) }
			}
			return field
		}
		set(value) {
			certificateFileStorage.write(context, jwksAdapter.toJson(value))
			field = value
		}

	override var certificatesSinceHeader: String?
		get() = preferences.getString(KEY_CERTIFICATE_SIGNATURES_SINCE_HEADER, null)
		set(value) {
			preferences.edit().putString(KEY_CERTIFICATE_SIGNATURES_SINCE_HEADER, value).apply()
		}

	override var certificatesUpToHeader: Long
		get() = preferences.getLong(KEY_CERTIFICATE_SIGNATURES_UP_TO_HEADER, 0L)
		set(value) {
			preferences.edit().putLong(KEY_CERTIFICATE_SIGNATURES_UP_TO_HEADER, value).apply()
		}

	override var revokedCertificatesValidUntil: Long
		get() = preferences.getLong(KEY_REVOKED_CERTIFICATES_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_REVOKED_CERTIFICATES_VALID_UNTIL, value).apply()
		}

	override var revokedCertificatesSinceHeader: String?
		get() = preferences.getString(
			KEY_REVOKED_CERTIFICATES_SINCE_HEADER,
			revokedCertificates.getPrepopulatedSinceHeader(CovidCertificateSdk.getEnvironment() == SdkEnvironment.PROD  )
		)
		set(value) {
			preferences.edit().putString(KEY_REVOKED_CERTIFICATES_SINCE_HEADER, value).apply()
		}

	override var foreignRulesCountryCodesValidUntil: Long
		get() = preferences.getLong(KEY_FOREIGN_RULES_COUNTRY_CODES_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_FOREIGN_RULES_COUNTRY_CODES_VALID_UNTIL, value).apply()
		}

	override var foreignRulesCountryCodes: Set<String>
		get() = preferences.getStringSet(KEY_FOREIGN_RULES_COUNTRY_CODES, emptySet()) ?: emptySet()
		set(value) {
			preferences.edit().putStringSet(KEY_FOREIGN_RULES_COUNTRY_CODES, value).apply()
		}

	override fun areSignaturesValid(): Boolean {
		return certificateSignatures != null && Instant.now().toEpochMilli() < certificateSignaturesValidUntil
	}

	override fun areRevokedCertificatesValid(): Boolean {
		return Instant.now().toEpochMilli() < revokedCertificatesValidUntil
	}

	override fun areForeignRulesCountryCodesValid(): Boolean {
		return foreignRulesCountryCodes.isNotEmpty() && Instant.now().toEpochMilli() < foreignRulesCountryCodesValidUntil
	}

	override suspend fun areRuleSetsValid(countryCode: String): Boolean {
		val validUntil = nationalRules.getValidUntilForCountry(countryCode)
		val nationalRules = nationalRules.getRuleSetForCountry(countryCode)
		return nationalRules != null && validUntil != null && Instant.now().toEpochMilli() < validUntil
	}

	/**
	 * Migrate the rule set data from the shared preferences (CH only) to the database (multi country)
	 */
	fun migrateRuleSetFromPreferencesToDatabase() {
		if (preferences.contains(KEY_RULESET_VALID_UNTIL)) {
			val validUntil = preferences.getLong(KEY_RULESET_VALID_UNTIL, 0L)
			val ruleSet = ruleSetFileStorage.read(context)?.let { rulesetAdapter.fromJson(it) }

			if (validUntil > 0L && ruleSet != null) {
				// The national rules store uses a room database underneath, which should not be accessed on the main thread
				// GlobalScope is fine here as this happens during the SDK initialization and is fine as a fire-and-forget coroutine
				GlobalScope.launch(Dispatchers.IO) {
					nationalRules.addRuleSetForCountry(TrustListRepository.COUNTRY_CODE_SWITZERLAND, validUntil, ruleSet)
				}
			}

			preferences.edit().remove(KEY_RULESET_VALID_UNTIL).apply()
			ruleSetFileStorage.delete(context)
		}
	}
}