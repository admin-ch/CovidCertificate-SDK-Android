/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes.DECODE_BASE_45
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes.DECODE_CBOR
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes.DECODE_PREFIX
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes.DECODE_Z_LIB
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.eval.models.healthcert.eu.Eudgc
import com.google.gson.Gson
import com.squareup.moshi.JsonClass
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

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

// Still works as long as testInstrumentationRunner is set: https://stackoverflow.com/a/46553713
//@RunWith(AndroidJUnit4::class)
@RunWith(Parameterized::class)
class EuCompatTest(
	@Suppress("unused")
	private val testInputFile: String,
	private val testCase: EuTestCase,
) {

	lateinit var instrumentationContext: Context

	@Before
	fun setup() {
		instrumentationContext = InstrumentationRegistry.getInstrumentation().context
	}

	companion object {
		private val TAG = EuCompatTest::class.java.canonicalName

		@Parameters
		@JvmStatic
		@Suppress("unused")
		fun parseAndLoadTestData(): Collection<Array<Any>> {
			val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
			val gson = Gson()

			// Traverse the assets directory to find all input file paths
			val accumulatedPaths = mutableListOf<String>()
			findAllJsons(instrumentationContext.assets, "dgc-testdata", accumulatedPaths)

			// Read all files into data classes
			val testCases = mutableListOf<Pair<String, EuTestCase>>()
			accumulatedPaths.forEach { path ->
				try {
					val string = instrumentationContext.assets.open(path).bufferedReader().readText()

					gson.fromJson(string, EuTestCase::class.java)?.let { testCase ->
						testCases.add(Pair(path, testCase))
					}
				} catch (e: Exception) {
					Log.w(TAG, "Failed to parse $path")
				}
			}

			return testCases.map { arrayOf(it.first, it.second) }
		}

		// Helper to traverse the assets directory
		private fun findAllJsons(assets: AssetManager, basePath: String, accumulatedPaths: MutableList<String>) {
			assets.list(basePath)?.forEach { item ->
				val itemPath = "$basePath/$item"

				if (item.endsWith("json")) {
					accumulatedPaths.add(itemPath)
				} else {
					findAllJsons(assets, itemPath, accumulatedPaths)
				}
			}
		}
	}

	@Test
	fun testCanDecodePrefix() {
		val decodeState =  CertificateDecoder.decode(testCase.PREFIX)

		if (testCase.EXPECTEDRESULTS.EXPECTEDUNPREFIX == false) {
			assertTrue(
				"$testInputFile (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDUNPREFIX with decodeState=$decodeState",
				decodeState is DecodeState.ERROR && decodeState.error.code == DECODE_PREFIX
			)
		}

		if (testCase.EXPECTEDRESULTS.EXPECTEDB45DECODE == false) {
			assertTrue(
				"$testInputFile (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDB45DECODE with decodeState=$decodeState",
				decodeState is DecodeState.ERROR && decodeState.error.code == DECODE_BASE_45
			)
		}

		if (testCase.EXPECTEDRESULTS.EXPECTEDCOMPRESSION == false) {
			assertTrue(
				"$testInputFile (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDCOMPRESSION with decodeState=$decodeState",
				decodeState is DecodeState.ERROR && decodeState.error.code == DECODE_Z_LIB
			)
		}

		// Skip checking EXPECTEDVERIFY (since this is a noop in our decode)

		if (testCase.EXPECTEDRESULTS.EXPECTEDDECODE == false) {
			assertTrue(
				"$testInputFile (${testCase.TESTCTX.DESCRIPTION}) failed test EXPECTEDDECODE with decodeState=$decodeState",
				decodeState is DecodeState.ERROR && decodeState.error.code == DECODE_CBOR
			)
		}
	}

}