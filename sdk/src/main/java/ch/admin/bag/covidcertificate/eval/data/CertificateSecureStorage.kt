/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.data

import android.content.Context
import ch.admin.bag.covidcertificate.eval.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.eval.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.eval.models.trustlist.RevokedCertificates
import ch.admin.bag.covidcertificate.eval.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.eval.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import com.squareup.moshi.Moshi
import java.time.Instant

internal class CertificateSecureStorage private constructor(private val context: Context) : TrustListStore {

	companion object : SingletonHolder<CertificateSecureStorage, Context>(::CertificateSecureStorage) {
		private const val PREFERENCES_NAME = "TrustListSecureStorage"
		private const val FILE_PATH_CERTIFICATE_SIGNATURES = "certificate_signatures.json"
		private const val FILE_PATH_REVOKED_CERTIFICATES = "revoked_certificates.json"
		private const val FILE_PATH_RULESET = "national_ruleset.json"

		private const val KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL = "KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL"
		private const val KEY_CERTIFICATE_SIGNATURES_SINCE_HEADER = "KEY_CERTIFICATE_SIGNATURES_SINCE_HEADER"
		private const val KEY_REVOKED_CERTIFICATES_VALID_UNTIL = "KEY_REVOKED_CERTIFICATES_VALID_UNTIL"
		private const val KEY_RULESET_VALID_UNTIL = "KEY_RULESET_VALID_UNTIL"

		private val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		private val jwksAdapter = moshi.adapter(Jwks::class.java)
		private val revokedCertificatesAdapter = moshi.adapter(RevokedCertificates::class.java)
		private val rulesetAdapter = moshi.adapter(RuleSet::class.java)
	}

	private val certificateFileStorage = FileStorage(FILE_PATH_CERTIFICATE_SIGNATURES)
	private val revocationFileStorage = FileStorage(FILE_PATH_REVOKED_CERTIFICATES)
	private val ruleSetFileStorage = FileStorage(FILE_PATH_RULESET)

	private val preferences = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES_NAME)

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

	override var revokedCertificatesValidUntil: Long
		get() = preferences.getLong(KEY_REVOKED_CERTIFICATES_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_REVOKED_CERTIFICATES_VALID_UNTIL, value).apply()
		}

	override var revokedCertificates: RevokedCertificates? = null
		get() {
			if (field == null) {
				field = revocationFileStorage.read(context)?.let { revokedCertificatesAdapter.fromJson(it) }
			}
			return field
		}
		set(value) {
			revocationFileStorage.write(context, revokedCertificatesAdapter.toJson(value))
			field = value
		}

	override var rulesetValidUntil: Long
		get() = preferences.getLong(KEY_RULESET_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_RULESET_VALID_UNTIL, value).apply()
		}

	override var ruleset: RuleSet? = null
		get() {
			if (field == null) {
				field = ruleSetFileStorage.read(context)?.let { rulesetAdapter.fromJson(it) }
			}
			return field
		}
		set(value) {
			ruleSetFileStorage.write(context, rulesetAdapter.toJson(value))
			field = value
		}

	override fun areSignaturesValid(): Boolean {
		return certificateSignatures != null && Instant.now().toEpochMilli() < certificateSignaturesValidUntil
	}

	override fun areRevokedCertificatesValid(): Boolean {
		return revokedCertificates != null && Instant.now().toEpochMilli() < revokedCertificatesValidUntil
	}

	override fun areRuleSetsValid(): Boolean {
		return ruleset != null && Instant.now().toEpochMilli() < rulesetValidUntil
	}
}