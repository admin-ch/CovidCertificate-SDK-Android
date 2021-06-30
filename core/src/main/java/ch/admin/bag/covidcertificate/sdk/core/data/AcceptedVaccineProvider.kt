package ch.admin.bag.covidcertificate.sdk.core.data

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.products.Vaccine

interface AcceptedVaccineProvider {

	fun getVaccineName(vaccinationEntry: VaccinationEntry): String
	fun getProphylaxis(vaccinationEntry: VaccinationEntry): String
	fun getAuthHolder(vaccinationEntry: VaccinationEntry): String
	fun getVaccineDataFromList(vaccinationEntry: VaccinationEntry): Vaccine?

}