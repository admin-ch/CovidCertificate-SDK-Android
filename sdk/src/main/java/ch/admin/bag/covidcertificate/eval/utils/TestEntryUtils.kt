/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.utils

import ch.admin.bag.covidcertificate.eval.euhealthcert.TestEntry
import ch.admin.bag.covidcertificate.eval.models.AcceptanceCriterias
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


fun TestEntry.isNegative(): Boolean {
	return this.result == AcceptanceCriteriasConstants.NEGATIVE_CODE
}

fun TestEntry.isTargetDiseaseCorrect(): Boolean {
	return this.disease == AcceptanceCriteriasConstants.TARGET_DISEASE
}

fun TestEntry.getFormattedSampleDate(dateTimeFormatter: DateTimeFormatter): String? {
	return try {
		return this.timestampSample.toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
	} catch (e: Exception) {
		null
	}
}

fun TestEntry.getFormattedResultDate(dateTimeFormatter: DateTimeFormatter): String? {
	if (this.timestampResult == null) {
		return null
	}
	return try {
		this.timestampResult.toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
	} catch (e: Exception) {
		null
	}
}

fun TestEntry.getTestCenter(): String? {
	if (!this.testCenter.isNullOrBlank()) {
		return this.testCenter
	}
	return null
}

fun TestEntry.getTestCountry(showEnglishVersionForLabels: Boolean): String {
	return try {
		val loc = Locale("", this.country)
		var countryString = loc.displayCountry
		if (showEnglishVersionForLabels) {
			countryString = "$countryString / ${loc.getDisplayCountry(Locale.ENGLISH)}"
		}
		return countryString
	} catch (e: Exception) {
		this.country
	}
}

fun TestEntry.getIssuer(): String {
	return this.certificateIssuer
}

fun TestEntry.getCertificateIdentifier(): String {
	return this.certificateIdentifier
}

fun TestEntry.validFromDate(): LocalDateTime? {
	return try {
		this.timestampSample.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
	} catch (e: Exception) {
		return null
	}
}

fun TestEntry.validUntilDate(acceptanceCriterias: AcceptanceCriterias): LocalDateTime? {
	val startDate = this.validFromDate() ?: return null
	return when (type) {
		TestType.PCR.code -> startDate.plusHours(acceptanceCriterias.pcrTestValidity.toLong())
		TestType.RAT.code -> startDate.plusHours(acceptanceCriterias.ratTestValidity.toLong())
		else -> null
	}
}

