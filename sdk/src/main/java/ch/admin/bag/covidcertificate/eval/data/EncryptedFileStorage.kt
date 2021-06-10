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
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.ByteArrayOutputStream
import java.io.File

internal class EncryptedFileStorage(private val path: String) {

	private val TAG = EncryptedFileStorage::class.java.simpleName

	fun write(context: Context, content: String) {
		val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

		val file = File(context.filesDir, path)
		if (file.exists()) {
			// Yeah, interesting design choice by Google, see also https://stackoverflow.com/q/63127401
			file.delete()
		}

		val encryptedFile = EncryptedFile.Builder(
			file,
			context,
			masterKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		val encryptedOutputStream = encryptedFile.openFileOutput()
		try {
			encryptedOutputStream.write(content.encodeToByteArray())
		} catch (e: Exception) {
			Log.e(TAG, e.toString())
		} finally {
			encryptedOutputStream.close()
		}
	}

	fun read(context: Context): String? {
		val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

		val file = File(context.filesDir, path)
		if (!file.exists()) return null

		val encryptedFile: EncryptedFile = EncryptedFile.Builder(
			file,
			context,
			masterKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		val encryptedInputStream = encryptedFile.openFileInput()
		val byteArrayOutputStream = ByteArrayOutputStream()
		var nextByte: Int = encryptedInputStream.read()
		while (nextByte != -1) {
			byteArrayOutputStream.write(nextByte)
			nextByte = encryptedInputStream.read()
		}
		val bytes: ByteArray = byteArrayOutputStream.toByteArray()
		return bytes.decodeToString()
	}

}