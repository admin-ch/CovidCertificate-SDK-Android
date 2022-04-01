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
import android.util.Log
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class FileStorage(private val path: String) {

	companion object {
		private val TAG = FileStorage::class.java.simpleName
	}

	private val rwl = ReentrantReadWriteLock()

	fun write(context: Context, content: String) {
		val file = File(context.filesDir, path)

		rwl.writeLock().lock()
		try {
			file.writeText(content)
			rwl.writeLock().unlock()
		} catch (e: Exception) {
			Log.e(TAG, e.message, e)
			rwl.writeLock().unlock()

			// If we failed to write to the encrypted file, delete it to recreate it the next time
			file.delete()
		}
	}

	fun read(context: Context): String? {
		val file = File(context.filesDir, path)
		if (!file.exists()) return null

		rwl.readLock().lock()
		return try {
			file.readText()
		} catch (e: Exception) {
			Log.e(TAG, e.message, e)

			// If we failed to read from the encrypted file, delete it to recreate it on the next write
			file.delete()
			null
		} finally {
			rwl.readLock().unlock()
		}
	}

	fun delete(context: Context) {
		val file = File(context.filesDir, path)

		rwl.writeLock().lock()

		try {
			file.delete()
		} catch (e: Exception) {
			Log.e(TAG, e.message, e)
		} finally {
			rwl.writeLock().unlock()
		}
	}

}