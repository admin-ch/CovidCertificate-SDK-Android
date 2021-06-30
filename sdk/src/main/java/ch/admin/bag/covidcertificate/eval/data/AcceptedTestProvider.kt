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
import ch.admin.bag.covidcertificate.eval.models.healthcert.eu.TestEntry
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import ch.admin.bag.covidcertificate.eval.utils.TestType

class AcceptedTestProvider private constructor(context: Context) {

	companion object : SingletonHolder<AcceptedTestProvider, Context>(::AcceptedTestProvider)

	private val metadataStorage = MetadataStorage.getInstance(context)

	fun getTestType(testEntry: TestEntry): String {
		metadataStorage.productsMetadata.test.type
		return metadataStorage.productsMetadata.test.type.valueSetValues[testEntry.type]?.display ?: testEntry.type
	}

	fun testIsPCRorRAT(testEntry: TestEntry): Boolean {
		return testEntry.type == TestType.PCR.code || testEntry.type == TestType.RAT.code
	}

	fun testIsAcceptedInEuAndCH(testEntry: TestEntry): Boolean {
		if (testEntry.type == TestType.PCR.code) {
			return true
		} else if (testEntry.type == TestType.RAT.code) {
			return metadataStorage.productsMetadata.test.manf.valueSetValues[testEntry.ratTestNameAndManufacturer]?.let { return true }
				?: return false
		}
		return false
	}

	fun getTestName(testEntry: TestEntry): String? {
		if (testEntry.type == TestType.PCR.code) {
			return testEntry.naaTestName ?: "PCR"
		} else if (testEntry.type == TestType.RAT.code) {
			return testEntry.naaTestName
		}
		return null
	}

	fun getManufacturesIfExists(testEntry: TestEntry): String? {
		var ma = metadataStorage.productsMetadata.test.manf.valueSetValues[testEntry.ratTestNameAndManufacturer]?.display
		testEntry.naaTestName?.let { nm ->
			ma = ma?.replace(nm, "")?.trim()?.removeSuffix(",")
		}

		return if (ma.isNullOrEmpty()) {
			null
		} else {
			ma
		}
	}
}