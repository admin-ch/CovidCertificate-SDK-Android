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
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.products.AcceptedVaccine
import ch.admin.bag.covidcertificate.sdk.core.models.products.Vaccine
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.IOException

class AcceptedVaccineProvider private constructor(context: Context) :
	ch.admin.bag.covidcertificate.sdk.core.data.AcceptedVaccineProvider {

	companion object : SingletonHolder<AcceptedVaccineProvider, Context>(::AcceptedVaccineProvider) {
		// for national rules validation
		private const val ACCEPTED_VACCINE_FILE_NAME = "acceptedCHVaccine.json"
	}

	private val acceptedVaccine: AcceptedVaccine
	private val metadataStorage = MetadataStorage.getInstance(context)

	init {
		val acceptedVaccineAdapter: JsonAdapter<AcceptedVaccine> = Moshi.Builder().build().adapter(AcceptedVaccine::class.java)
		acceptedVaccine = acceptedVaccineAdapter.fromJson(context.assets.open(ACCEPTED_VACCINE_FILE_NAME).source().buffer())
			?: throw IOException()
	}

	override fun getVaccineName(vaccinationEntry: VaccinationEntry): String {
		return metadataStorage.productsMetadata.vaccine.medicinalProduct.valueSetValues[vaccinationEntry.medicinialProduct]?.display
			?: vaccinationEntry.medicinialProduct
	}

	override fun getProphylaxis(vaccinationEntry: VaccinationEntry): String {
		return metadataStorage.productsMetadata.vaccine.prophylaxis.valueSetValues[vaccinationEntry.vaccine]?.display
			?: vaccinationEntry.vaccine
	}

	override fun getAuthHolder(vaccinationEntry: VaccinationEntry): String {
		return metadataStorage.productsMetadata.vaccine.mahManf.valueSetValues[vaccinationEntry.marketingAuthorizationHolder]?.display
			?: vaccinationEntry.marketingAuthorizationHolder
	}

	override fun getVaccineDataFromList(vaccinationEntry: VaccinationEntry): Vaccine? {
		return acceptedVaccine.entries.firstOrNull { entry -> entry.code == vaccinationEntry.medicinialProduct }
	}

}