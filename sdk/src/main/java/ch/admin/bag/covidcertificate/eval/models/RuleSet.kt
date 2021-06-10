/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.models

import ch.admin.bag.covidcertificate.eval.data.moshi.RawJsonString
import ch.admin.bag.covidcertificate.eval.euhealthcert.RecoveryEntry
import ch.admin.bag.covidcertificate.eval.euhealthcert.TestEntry
import ch.admin.bag.covidcertificate.eval.euhealthcert.VaccinationEntry
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuleSet(
	val rules: List<Rule>,
	val valueSets: RuleValueSets,
	val validDuration: Long,
)

@JsonClass(generateAdapter = true)
data class Rule(
	val id: String,
	val businessDescription: String?,
	val description: String,
	val inputParameter: String,
	@RawJsonString val logic: String,
)

@JsonClass(generateAdapter = true)
data class RuleValueSets(
	@Json(name = "disease-agent-targeted")
	@get:JsonProperty("disease-agent-targeted")
	val diseaseAgents: List<String>?,
	@Json(name = "accepted-vaccines")
	@get:JsonProperty("accepted-vaccines")
	val acceptedVaccines: List<String>?,
	@Json(name = "two-dose-vaccines")
	@get:JsonProperty("two-dose-vaccines")
	val twoDoseVaccines: List<String>?,
	@Json(name = "one-dose-vaccines-with-offset")
	@get:JsonProperty("one-dose-vaccines-with-offset")
	val oneDoseVaccinesWithOffset: List<String>?,
	@Json(name = "covid-19-lab-test-type")
	@get:JsonProperty("covid-19-lab-test-type")
	val testTypes: List<String>?,
	@Json(name = "covid-19-lab-test-manufacturer-and-name")
	@get:JsonProperty("covid-19-lab-test-manufacturer-and-name")
	val testManufacturers: List<String>?,
	@Json(name = "acceptance-criteria")
	@get:JsonProperty("acceptance-criteria")
	val acceptanceCriteria: AcceptanceCriterias,
)

@JsonClass(generateAdapter = true)
data class AcceptanceCriterias(
	@Json(name = "single-vaccine-validity-offset")
	@get:JsonProperty("single-vaccine-validity-offset")
	val singleVaccineValidityOffset: Int,
	@Json(name = "vaccine-immunity")
	@get:JsonProperty("vaccine-immunity")
	val vaccineImmunity: Int,
	@Json(name = "rat-test-validity")
	@get:JsonProperty("rat-test-validity")
	val ratTestValidity: Int,
	@Json(name = "pcr-test-validity")
	@get:JsonProperty("pcr-test-validity")
	val pcrTestValidity: Int,
	@Json(name = "recovery-offset-valid-from")
	@get:JsonProperty("recovery-offset-valid-from")
	val recoveryOffsetValidFrom: Int,
	@Json(name = "recovery-offset-valid-until")
	@get:JsonProperty("recovery-offset-valid-until")
	val recoveryOffsetValidUntil: Int
)


data class CertLogicData(
	val payload: CertLogicPayload,
	val external: CertLogicExternalInfo,
)

data class CertLogicPayload(
	val r: List<RecoveryEntry>? = null,
	val t: List<TestEntry>? = null,
	val v: List<VaccinationEntry>? = null,
)

data class CertLogicExternalInfo(
	val valueSets: RuleValueSets,
	val validationClock: String, // ISO-8601 extended offset date-time format
	val validationClockAtStartOfDay: String, // ISO-8601 date format
)