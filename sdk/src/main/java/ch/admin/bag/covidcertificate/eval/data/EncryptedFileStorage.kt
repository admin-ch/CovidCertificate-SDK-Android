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
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class EncryptedFileStorage(private val path: String) {

	private val TAG = EncryptedFileStorage::class.java.simpleName

	private val rwl = ReentrantReadWriteLock()

	fun write(context: Context, content: String) {
		val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

		val file = File(context.filesDir, path)
		if (file.exists()) {
			file.delete()
		}

		val encryptedFile = EncryptedFile.Builder(
			file,
			context,
			masterKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		rwl.writeLock().lock()
		try {
			val encryptedOutputStream = encryptedFile.openFileOutput()
			encryptedOutputStream.use {
				it.write(content.encodeToByteArray())
				it.flush()
			}
			rwl.writeLock().unlock()
		} catch (e: Exception) {
			Log.e(TAG, e.message, e)
			rwl.writeLock().unlock()

			// If we failed to write to the encrypted file, delete it to recreate it the next time
			file.delete()
		}
	}

	fun read(context: Context): String? {
		val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

		val file = File(context.filesDir, path)
		if (!file.exists()) return null

		val encryptedFile = EncryptedFile.Builder(
			file,
			context,
			masterKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		rwl.readLock().lock()
		try {
			val encryptedInputStream = encryptedFile.openFileInput()
			val byteArrayOutputStream = ByteArrayOutputStream()
			var nextByte: Int = encryptedInputStream.read()
			while (nextByte != -1) {
				byteArrayOutputStream.write(nextByte)
				nextByte = encryptedInputStream.read()
			}
			val bytes: ByteArray = byteArrayOutputStream.toByteArray()
			return bytes.decodeToString()
		} catch (e: Exception) {
			Log.e(TAG, e.message, e)

			// If we failed to read from the encrypted file, delete it to recreate it on the next write
			file.delete()
			return null
		} finally {
			rwl.readLock().unlock()
		}
	}

}