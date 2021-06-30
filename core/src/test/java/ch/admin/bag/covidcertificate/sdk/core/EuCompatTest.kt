/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core

import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.Eudgc
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ArgumentConversionException
import org.junit.jupiter.params.converter.ArgumentConverter
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.*
import java.util.stream.Stream

// See https://github.com/eu-digital-green-certificates/dgc-testdata

@JsonClass(generateAdapter = true)
data class EuTestCase(
	val JSON: Eudgc,
	val CBOR: String, // hex encoded
	val COSE: String, // hex encoded
	val COMPRESSED: String, // hex encoded
	val BASE45: String, // base45 encoded compressed COSE
	val PREFIX: String, // base45 encoded compressed COSE with prefix HC:x
	val `2DCODE`: String, // base64 encoded PNG
	val TESTCTX: EuTestContext,
	val EXPECTEDRESULTS: EuTestExpectedResult,
	val filePath: String? = null,
)

@JsonClass(generateAdapter = true)
data class EuTestContext(
	val VERSION: Int,
	val SCHEMA: String,
	val CERTIFICATE: String, // base64
	val VALIDATIONCLOCK: String, // ISO-8601 timestamp
	val DESCRIPTION: String,
	val `GATEWAY-ENV`: List<String>?,
)

@JsonClass(generateAdapter = true)
data class EuTestExpectedResult(
	val EXPECTEDVALIDOBJECT: Boolean?,
	val EXPECTEDSCHEMAVALIDATION: Boolean?,
	val EXPECTEDENCODE: Boolean?,
	val EXPECTEDDECODE: Boolean?,
	val EXPECTEDVERIFY: Boolean?,
	val EXPECTEDCOMPRESSION: Boolean?,
	val EXPECTEDKEYUSAGE: Boolean?,
	val EXPECTEDUNPREFIX: Boolean?,
	val EXPECTEDVALIDJSON: Boolean?,
	val EXPECTEDB45DECODE: Boolean?,
	val EXPECTEDPICTUREDECODE: Boolean?,
	val EXPECTEDEXPIRATIONCHECK: Boolean?,
)

class EuTestDataProvider : ArgumentsProvider {
	private val moshi = Moshi.Builder().add(Date::class.java, Rfc3339DateJsonAdapter()).build()
	private val adapter = moshi.adapter(EuTestCase::class.java)
	private val testDataDirectory = this::class.java.classLoader.getResource("dgc-testdata")!!

	override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
		// Traverse the assets directory to find all input file paths
		val accumulatedPaths = mutableListOf<String>()
		findAllJsons(File(testDataDirectory.path), accumulatedPaths)

		// Read all files into data classes
//		val testCases = mutableListOf<Pair<String, EuTestCase>>()
//		accumulatedPaths.forEach { path ->
//			try {
//				val string = File(path).readText()
//
//				adapter.fromJson(string)?.let { testCase ->
//					testCases.add(Pair(path, testCase))
//				}
//			} catch (e: Exception) {
//				e.printStackTrace()
//			}
//		}

		return accumulatedPaths.stream().map(Arguments::of)
	}

	// Helper to traverse the assets directory
	private fun findAllJsons(directory: File, accumulatedPaths: MutableList<String>) {
		directory.walk().forEach { file ->
			val path = file.path

			if (directory.path != path) {
				if (path.endsWith("json")) {
					accumulatedPaths.add(path)
				} else {
					findAllJsons(file, accumulatedPaths)
				}
			}
		}
	}
}

class EuTestCaseConverter : ArgumentConverter {
	private val moshi = Moshi.Builder().add(Date::class.java, Rfc3339DateJsonAdapter()).build()
	private val adapter = moshi.adapter(EuTestCase::class.java)

	override fun convert(source: Any?, context: ParameterContext?): Any {
		val path = source as String
		try {
			val content = File(path).readText()
			val testCase = adapter.fromJson(content) ?: throw ArgumentConversionException("Could not parse json for $path")
			return testCase.copy(filePath = path)
		} catch (t: Throwable) {
			throw ArgumentConversionException("Could not parse json for $path")
		}
	}
}

class EuCompatTest {

	@ParameterizedTest
	@ArgumentsSource(EuTestDataProvider::class)
	fun testCanDecodePrefix(@ConvertWith(EuTestCaseConverter::class) testCase: EuTestCase) {
		val decodeState =  CertificateDecoder.decode(testCase.PREFIX)

		if (testCase.EXPECTEDRESULTS.EXPECTEDUNPREFIX == false) {
			assertTrue(
				decodeState is DecodeState.ERROR && decodeState.error.code == ErrorCodes.DECODE_PREFIX,
				"${testCase.filePath} (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDUNPREFIX with decodeState=$decodeState"
			)
		}

		if (testCase.EXPECTEDRESULTS.EXPECTEDB45DECODE == false) {
			assertTrue(
				decodeState is DecodeState.ERROR && decodeState.error.code == ErrorCodes.DECODE_BASE_45,
				"${testCase.filePath} (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDB45DECODE with decodeState=$decodeState"
			)
		}

		if (testCase.EXPECTEDRESULTS.EXPECTEDCOMPRESSION == false) {
			assertTrue(
				decodeState is DecodeState.ERROR && decodeState.error.code == ErrorCodes.DECODE_Z_LIB,
				"${testCase.filePath} (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDCOMPRESSION with decodeState=$decodeState"
			)
		}

		// Skip checking EXPECTEDVERIFY (since this is a noop in our decode)

		if (testCase.EXPECTEDRESULTS.EXPECTEDDECODE == false) {
			assertTrue(
				decodeState is DecodeState.ERROR && decodeState.error.code == ErrorCodes.DECODE_CBOR,
				"${testCase.filePath} (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDDECODE with decodeState=$decodeState"
			)
		}
	}

}