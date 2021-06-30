/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.InflaterInputStream

internal object DecompressionService {

	/**
	 * Limit the byte array size after decompression to 5 MB.
	 *
	 * Reasoning:
	 * 1. QR codes can hold at most < 4500 alphanumeric chars (https://www.qrcode.com/en/about/version.html)
	 *    Sidenote: The EHN spec recommends a compression level of Q, which limits it to at most < 2500 alphanumeric chars
	 * 	  (https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf#page=7)
	 *    This is a lower bound (since any DCC should be encodable in both Aztec and QR codes).
	 * 2. As an additional upper bound: base45 encodes 2 bytes into 3 chars (https://datatracker.ietf.org/doc/html/draft-faltstrom-base45-04#section-4)
	 * 3.  zlib's maximum compression factor is roughly 1000:1 (http://www.zlib.net/zlib_tech.html)
	 */
	private const val MAX_DECOMPRESSED_SIZE = 5 * 1024 * 1024

	private const val ERROR_TOO_MANY_BYTES_READ = -1L

	fun decode(input: ByteArray): ByteArray? {
		// Spec: https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf#page=7
		// "the CWT SHALL be compressed using ZLIB"
		// => data that is not compressed is invalid

		return try {
			val inflaterStream = InflaterInputStream(input.inputStream())
			val outputStream = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
			val decodedBytes = inflaterStream.copyTo(outputStream)

			if (decodedBytes == ERROR_TOO_MANY_BYTES_READ) {
				null
			} else {
				outputStream.toByteArray()
			}
			// Closing ByteArray streams has no effect anyway
		} catch (e: Throwable) {
			null
		}
	}

	/**
	 * Adapted from kotlin-stdblib's kotlin.io.IOStreams.kt
	 */
	private fun InflaterInputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
		var bytesCopied: Long = 0
		val buffer = ByteArray(bufferSize)
		var bytes = read(buffer)
		while (bytes >= 0) {
			out.write(buffer, 0, bytes)
			bytesCopied += bytes
			bytes = read(buffer)
			// begin patch
			if (bytesCopied > MAX_DECOMPRESSED_SIZE) {
				return ERROR_TOO_MANY_BYTES_READ
			}
			// end patch
		}
		return bytesCopied
	}
}
