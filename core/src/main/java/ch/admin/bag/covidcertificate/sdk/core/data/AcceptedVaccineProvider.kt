/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.data

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.products.Vaccine

interface AcceptedVaccineProvider {

	fun getVaccineName(vaccinationEntry: VaccinationEntry): String
	fun getProphylaxis(vaccinationEntry: VaccinationEntry): String
	fun getAuthHolder(vaccinationEntry: VaccinationEntry): String
	fun getVaccineDataFromList(vaccinationEntry: VaccinationEntry): Vaccine?

}