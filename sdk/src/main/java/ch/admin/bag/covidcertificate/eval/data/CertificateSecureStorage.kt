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
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.RevokedList
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import com.squareup.moshi.Moshi
import java.io.IOException
import java.security.GeneralSecurityException

class CertificateSecureStorage private constructor(context: Context) : TrustListProvider {

	companion object : SingletonHolder<CertificateSecureStorage, Context>(::CertificateSecureStorage) {
		private const val PREFERENCES = "CertificateSecureStorage"

		private const val KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL = "KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL"
		private const val KEY_CERTIFICATE_SIGNATURES = "KEY_CERTIFICATE_SIGNATURES"

		private const val KEY_REVOKED_CERTIFICATES_VALID_UNTIL = "KEY_REVOKED_CERTIFICATES_VALID_UNTIL"
		private const val KEY_REVOKED_CERTIFICATES = "KEY_REVOKED_CERTIFICATES"

		private const val KEY_RULESET_VALID_UNTIL = "KEY_RULESET_VALID_UNTIL"
		private const val KEY_RULESET = "KEY_RULESET"

		private val moshi = Moshi.Builder().build()
		private val jwksAdapter = moshi.adapter(Jwks::class.java)
		private val revokedCertificatesAdapter = moshi.adapter(RevokedList::class.java)
		private val rulesetAdapter = moshi.adapter(RuleSet::class.java)
	}

	private val preferences = initializeSharedPreferences(context)

	@Synchronized
	private fun initializeSharedPreferences(context: Context): SharedPreferences {
		return try {
			createEncryptedSharedPreferences(context)
		} catch (e: GeneralSecurityException) {
			throw RuntimeException(e)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}

	/**
	 * Create or obtain an encrypted SharedPreferences instance. Note that this method is synchronized because the AndroidX
	 * Security
	 * library is not thread-safe.
	 * @see [https://developer.android.com/topic/security/data](https://developer.android.com/topic/security/data)
	 */
	@Synchronized
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
		val masterKeys = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
		return EncryptedSharedPreferences
			.create(
				PREFERENCES, masterKeys, context, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
				EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
			)
	}

	override var certificateSignaturesValidUntil: Long
		get() = preferences.getLong(KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_CERTIFICATE_SIGNATURES_VALID_UNTIL, value).apply()
		}

	override var certificateSignatures: Jwks?
		get() = preferences.getString(KEY_CERTIFICATE_SIGNATURES, null)?.let { jwksAdapter.fromJson(it) }
		set(value) {
			preferences.edit().putString(KEY_CERTIFICATE_SIGNATURES, jwksAdapter.toJson(value)).apply()
		}

	override var revokedCertificatesValidUntil: Long
		get() = preferences.getLong(KEY_REVOKED_CERTIFICATES_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_REVOKED_CERTIFICATES_VALID_UNTIL, value).apply()
		}

	override var revokedCertificates: RevokedList?
		get() = preferences.getString(KEY_REVOKED_CERTIFICATES, null)?.let { revokedCertificatesAdapter.fromJson(it) }
		set(value) {
			preferences.edit().putString(KEY_REVOKED_CERTIFICATES, revokedCertificatesAdapter.toJson(value)).apply()
		}

	override var rulesetValidUntil: Long
		get() = preferences.getLong(KEY_RULESET_VALID_UNTIL, 0L)
		set(value) {
			preferences.edit().putLong(KEY_RULESET_VALID_UNTIL, value).apply()
		}

	override var ruleset: RuleSet?
		get() = preferences.getString(KEY_RULESET, null)?.let { rulesetAdapter.fromJson(it) }
		set(value) {
			preferences.edit().putString(KEY_RULESET, rulesetAdapter.toJson(value)).apply()
		}
}