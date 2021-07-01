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
	val JSON: Eudgc?,
	val CBOR: String?, // hex encoded
	val COSE: String?, // hex encoded
	val COMPRESSED: String?, // hex encoded
	val BASE45: String?, // base45 encoded compressed COSE
	val PREFIX: String, // base45 encoded compressed COSE with prefix HC:x
	val `2DCODE`: String?, // base64 encoded PNG
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
	val DESCRIPTION: String?,
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
	private val testDataDirectory = File(this::class.java.classLoader.getResource("dgc-testdata")!!.path)

	override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
		// Get all EU test case json files in the test data directory
		val testCaseJsonFiles = testDataDirectory.walk().mapNotNull { file ->
			if (file.path.endsWith("json")) {
				file.path
			} else {
				null
			}
		}.toSet()

		return testCaseJsonFiles.stream().map(Arguments::of)
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
			throw ArgumentConversionException("Could not parse json for $path", t)
		}
	}
}

class EuCompatTest {

	@ParameterizedTest
	@ArgumentsSource(EuTestDataProvider::class)
	fun testCanDecodePrefix(@ConvertWith(EuTestCaseConverter::class) testCase: EuTestCase) {
		val decodeState =  CertificateDecoder.decode(testCase.PREFIX)

		testCaseStep(testCase, "EXPECTEDUNPREFIX", testCase.EXPECTEDRESULTS.EXPECTEDUNPREFIX, decodeState, ErrorCodes.DECODE_PREFIX)
		testCaseStep(testCase, "EXPECTEDB45DECODE", testCase.EXPECTEDRESULTS.EXPECTEDB45DECODE, decodeState, ErrorCodes.DECODE_BASE_45)
		testCaseStep(testCase, "EXPECTEDCOMPRESSION", testCase.EXPECTEDRESULTS.EXPECTEDCOMPRESSION, decodeState, ErrorCodes.DECODE_Z_LIB)
		// Skip checking EXPECTEDVERIFY (since this is a noop in our decode)
		testCaseStep(testCase, "EXPECTEDDECODE", testCase.EXPECTEDRESULTS.EXPECTEDDECODE, decodeState, ErrorCodes.DECODE_CBOR)
	}

	private fun testCaseStep(
		testCase: EuTestCase,
		name: String,
		expected: Boolean?,
		decodeState: DecodeState,
		errorCodeIfExpectedToFail: String
	) {
		when (expected) {
			null -> {
				// Skip test case step if the expected flag was not set
			}
			true -> assertTrue(
				decodeState is DecodeState.SUCCESS
						|| (decodeState is DecodeState.ERROR && decodeState.error.code != errorCodeIfExpectedToFail),
				"${testCase.filePath} (${testCase.TESTCTX.DESCRIPTION}) failed test $name with decodeState=$decodeState"
			)
			false -> assertTrue(
				decodeState is DecodeState.ERROR
						&& decodeState.error.code == errorCodeIfExpectedToFail,
				"${testCase.filePath} (${testCase.TESTCTX.DESCRIPTION}) failed test $name with decodeState=$decodeState"
			)
		}
	}

}