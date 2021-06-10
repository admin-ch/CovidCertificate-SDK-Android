/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.nationalrules

import android.content.Context
import ch.admin.bag.covidcertificate.eval.data.AcceptedTestProvider
import ch.admin.bag.covidcertificate.eval.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.euhealthcert.Eudgc
import ch.admin.bag.covidcertificate.eval.euhealthcert.RecoveryEntry
import ch.admin.bag.covidcertificate.eval.euhealthcert.TestEntry
import ch.admin.bag.covidcertificate.eval.euhealthcert.VaccinationEntry
import ch.admin.bag.covidcertificate.eval.models.CertLogicData
import ch.admin.bag.covidcertificate.eval.models.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.eval.models.CertLogicPayload
import ch.admin.bag.covidcertificate.eval.models.Rule
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import ch.admin.bag.covidcertificate.eval.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.eval.nationalrules.certlogic.isTruthy
import ch.admin.bag.covidcertificate.eval.products.Vaccine
import ch.admin.bag.covidcertificate.eval.utils.doseNumber
import ch.admin.bag.covidcertificate.eval.utils.isNegative
import ch.admin.bag.covidcertificate.eval.utils.isTargetDiseaseCorrect
import ch.admin.bag.covidcertificate.eval.utils.totalDoses
import ch.admin.bag.covidcertificate.eval.utils.validFromDate
import ch.admin.bag.covidcertificate.eval.utils.validUntilDate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NationalRulesVerifier(context: Context) {

	private val acceptedVaccineProvider = AcceptedVaccineProvider.getInstance(context)
	private val acceptedTestProvider = AcceptedTestProvider.getInstance(context)

	fun verify(euDgc: Eudgc, ruleSet: RuleSet): CheckNationalRulesState {
		val payload = CertLogicPayload(euDgc.pastInfections, euDgc.tests, euDgc.vaccinations)
		val validationClock = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(ruleSet.valueSets, validationClock)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)

		for (rule in ruleSet.rules) {
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val isSuccessful = isTruthy(evaluate(ruleLogic, data))

			if (!isSuccessful) {
				return getErrorStateForRule(rule, euDgc)
			}
		}

		val validityRange = getValidityRange(euDgc)
		return if (validityRange != null) {
			CheckNationalRulesState.SUCCESS(validityRange)
		} else {
			CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}
	}

	private fun getErrorStateForRule(rule: Rule, euDgc: Eudgc): CheckNationalRulesState {
		return when (rule.id) {
			"GR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET, rule.id)
			"VR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_VACCINE_ENTRIES, rule.id)
			"VR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NOT_FULLY_PROTECTED, rule.id)
			"VR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"VR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0004" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0005" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0006" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_TEST_ENTRIES, rule.id)
			"TR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.POSITIVE_RESULT, rule.id)
			"TR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_TEST_TYPE, rule.id)
			"TR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"TR-CH-0004" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0005" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0006" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0007" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_RECOVERY_ENTRIES, rule.id)
			"RR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0002" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0003" -> getValidityRange(euDgc)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			else -> CheckNationalRulesState.INVALID(NationalRulesError.UNKNOWN_RULE_FAILED, rule.id)
		}
	}

	private fun getValidityRange(euDgc: Eudgc) : ValidityRange? {
		return when {
			!euDgc.vaccinations.isNullOrEmpty() -> {
				val vaccination = euDgc.vaccinations.first()
				val usedVaccine = acceptedVaccineProvider.getVaccineDataFromList(vaccination)
				usedVaccine?.let {
					ValidityRange(vaccination.validFromDate(it), vaccination.validUntilDate())
				}
			}
			!euDgc.tests.isNullOrEmpty() -> {
				val test = euDgc.tests.first()
				ValidityRange(test.validFromDate(), test.validUntilDate())
			}
			!euDgc.pastInfections.isNullOrEmpty() -> {
				val recovery = euDgc.pastInfections.first()
				ValidityRange(recovery.validFromDate(), recovery.validUntilDate())
			}
			else -> null
		}
	}

	@Deprecated("This method still uses hardcoded rules")
	fun verifyVaccine(
		vaccinationEntry: VaccinationEntry,
		clock: Clock = Clock.systemDefaultZone(),
	): CheckNationalRulesState {

		// tg must be sars-cov2
		// GR-CH-0001
		if (!vaccinationEntry.isTargetDiseaseCorrect()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET)
		}

		// dosis number must be greater or equal to total number of dosis
		// VR-CH-0001
		if (vaccinationEntry.doseNumber() < vaccinationEntry.totalDoses()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NOT_FULLY_PROTECTED)
		}

		// check if vaccine is in accepted product. We only check the mp now, since the same product held by different license holder should work the same -- right?
		// VR-CH-0002
		val foundEntry: Vaccine = acceptedVaccineProvider.getVaccineDataFromList(vaccinationEntry)
			?: return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT)

		val today = LocalDate.now(clock).atStartOfDay()
		val validFromDate = vaccinationEntry.validFromDate(foundEntry) // if (hadPastInfection) dt + 15 else dt
		val validUntilDate = vaccinationEntry.validUntilDate() // dt + 179

		// VR-CH-0003
		if (validFromDate == null || validUntilDate == null) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}

		// VR-CH-0004 & VR-CH-0005
		if (validFromDate.isAfter(today)) {
			return CheckNationalRulesState.NOT_YET_VALID(ValidityRange(validFromDate, validUntilDate))
		}

		// VR-CH-0006
		if (validUntilDate.isBefore(today)) {
			return CheckNationalRulesState.NOT_VALID_ANYMORE(ValidityRange(validFromDate, validUntilDate))
		}
		return CheckNationalRulesState.SUCCESS(ValidityRange(validFromDate, validUntilDate))
	}

	@Deprecated("This method still uses hardcoded rules")
	fun verifyTest(
		testEntry: TestEntry,
		clock: Clock = Clock.systemDefaultZone(),
	): CheckNationalRulesState {
		// tg must be sars-cov2
		// GR-CH-0001
		if (!testEntry.isTargetDiseaseCorrect()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET)
		}

		// TR-CH-0001
		if (!testEntry.isNegative()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.POSITIVE_RESULT)
		}

		// test type must be RAT or PCR
		// TR-CH-0002
		if (!acceptedTestProvider.testIsPCRorRAT(testEntry)) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_TEST_TYPE)
		}

		// TR-CH-0003
		if (!acceptedTestProvider.testIsAcceptedInEuAndCH(testEntry)) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT)
		}

		val today = LocalDateTime.now(clock)
		val validFromDate = testEntry.validFromDate() // sc
		val validUntilDate = testEntry.validUntilDate() // sc + 72h (PCR) / 24h (RAT)

		// TR-CH-0004
		if (validFromDate == null || validUntilDate == null) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}

		// TR-CH-0005
		if (validFromDate.isAfter(today)) {
			return CheckNationalRulesState.NOT_YET_VALID(ValidityRange(validFromDate, validUntilDate))
		}

		// TR-CH-0006 & TR-CH-0007
		if (validUntilDate.isBefore(today)) {
			return CheckNationalRulesState.NOT_VALID_ANYMORE(ValidityRange(validFromDate, validUntilDate))
		}
		return CheckNationalRulesState.SUCCESS(ValidityRange(validFromDate, validUntilDate))
	}

	@Deprecated("This method still uses hardcoded rules")
	fun verifyRecovery(
		recoveryEntry: RecoveryEntry,
		clock: Clock = Clock.systemDefaultZone(),
	): CheckNationalRulesState {

		// tg must be sars-cov2
		// GR-CH-0001
		if (!recoveryEntry.isTargetDiseaseCorrect()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET)
		}

		val today = LocalDate.now(clock).atStartOfDay()
		val validFromDate = recoveryEntry.validFromDate() // fr + 10
		val validUntilDate = recoveryEntry.validUntilDate() // fr + 179

		// RR-CH-0001
		if (validFromDate == null || validUntilDate == null) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}

		// RR-CH-0002
		if (validFromDate.isAfter(today)) {
			return CheckNationalRulesState.NOT_YET_VALID(ValidityRange(validFromDate, validUntilDate))
		}

		// RR-CH-0003
		if (validUntilDate.isBefore(today)) {
			return CheckNationalRulesState.NOT_VALID_ANYMORE(ValidityRange(validFromDate, validUntilDate))
		}
		return CheckNationalRulesState.SUCCESS(ValidityRange(validFromDate, validUntilDate))
	}
}

