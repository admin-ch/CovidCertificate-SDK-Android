/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

object EncryptedSharedPreferencesUtil {

	private val TAG = EncryptedSharedPreferencesUtil::class.java.simpleName

	@Synchronized
	@kotlin.jvm.Throws(CorruptedEncryptedSharedPreferencesException::class)
	fun initializeSharedPreferences(context: Context, preferencesName: String): SharedPreferences {
		return try {
			createEncryptedSharedPreferences(context, preferencesName)
		} catch (e: Exception) {
			e.printStackTrace()
			throw CorruptedEncryptedSharedPreferencesException(preferencesName, "Failed to create preferences $preferencesName", e)
		}
	}

	/**
	 * Create or obtain an encrypted SharedPreferences instance.
	 * Note that this method is synchronized because the AndroidX Security library is not thread-safe.
	 * @see [https://developer.android.com/topic/security/data](https://developer.android.com/topic/security/data)
	 */
	@Synchronized
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun createEncryptedSharedPreferences(context: Context, preferencesName: String): SharedPreferences {
		val masterKeys = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
		return EncryptedSharedPreferences.create(
			preferencesName,
			masterKeys,
			context,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
		)
	}

	/**
	 * Try to recreate the shared preferences. This will cause any previous data to be lost.
	 */
	fun tryToDeleteSharedPreferencesFile(context: Context, preferencesName: String) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			// Try to delete the shared preferences file via the API
			context.deleteSharedPreferences(preferencesName)
		} else {
			// Try to manually delete the shared preferences file
			val sharedPreferencesFile = File(context.applicationInfo.dataDir + "/shared_prefs/" + preferencesName + ".xml")
			if (sharedPreferencesFile.exists()) {
				if (!sharedPreferencesFile.delete()) {
					Log.e(TAG, "Failed to delete $sharedPreferencesFile")
				}
			}
		}
	}
}

/**
 * Thrown when the EncryptedSharedPreferences experience an error that we cannot recover from
 * (other than deleting and re-creating them).
 */
class CorruptedEncryptedSharedPreferencesException(
	val preferencesName: String, message: String, cause: Throwable
) : Exception(message, cause)