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

import ch.admin.bag.covidcertificate.eval.euhealthcert.VaccinationEntry
import ch.admin.bag.covidcertificate.eval.products.Vaccine
import ch.admin.bag.covidcertificate.eval.utils.AcceptanceCriterias.SINGLE_VACCINE_VALIDITY_OFFSET_IN_DAYS
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun VaccinationEntry.doseNumber(): Int = this.doseNumber

fun VaccinationEntry.totalDoses(): Int = this.totalDoses

fun VaccinationEntry.hadPastInfection(vaccine: Vaccine): Boolean {
	//if the total Doses of the vaccine is bigger then the total doses in the certificate, the patient had a past infection
	return vaccine.total_dosis_number > this.totalDoses()
}

fun VaccinationEntry.getNumberOverTotalDose(): String {
	return " ${this.doseNumber()}/${this.totalDoses()}"
}

fun VaccinationEntry.isTargetDiseaseCorrect(): Boolean {
	return this.disease == AcceptanceCriterias.TARGET_DISEASE
}

fun VaccinationEntry.getFormattedVaccinationDate(dateFormatter: DateTimeFormatter): String? {
	if (this.vaccinationDate.isNullOrEmpty()) {
		return null
	}
	return try {
		LocalDate.parse(this.vaccinationDate, DateTimeFormatter.ISO_DATE).format(dateFormatter)
	} catch (e: java.lang.Exception) {
		this.vaccinationDate
	}
}

fun VaccinationEntry.validFromDate(vaccine: Vaccine): LocalDateTime? {
	val vaccineDate = this.vaccineDate() ?: return null
	val totalNumberOfDosis = vaccine.total_dosis_number
	// if this is a vaccine, which only needs one shot AND we had no previous infections, the vaccine is valid 15 days after the date of vaccination
	return if (!this.hadPastInfection(vaccine) && totalNumberOfDosis == 1) {
		return vaccineDate.plusDays(SINGLE_VACCINE_VALIDITY_OFFSET_IN_DAYS)
	} else {
		// In any other case the vaccine is valid from the date of vaccination
		vaccineDate
	}
}

/// Vaccines are valid for 179 days
fun VaccinationEntry.validUntilDate(): LocalDateTime? {
	val vaccinationImmunityEndDate = this.vaccineDate() ?: return null
	return vaccinationImmunityEndDate.plusDays(AcceptanceCriterias.VACCINE_IMMUNITY_DURATION_IN_DAYS)
}

fun VaccinationEntry.vaccineDate(): LocalDateTime? {
	if (this.vaccinationDate.isNullOrEmpty()) {
		return null
	}
	val date: LocalDate?
	try {
		date = LocalDate.parse(this.vaccinationDate, DateTimeFormatter.ISO_DATE)
	} catch (e: Exception) {
		return null
	}
	return date.atStartOfDay()
}

fun VaccinationEntry.getVaccinationCountry(showEnglishVersionForLabels: Boolean): String {
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

fun VaccinationEntry.getIssuer(): String {
	return this.certificateIssuer
}

fun VaccinationEntry.getCertificateIdentifier(): String {
	return this.certificateIdentifier
}
