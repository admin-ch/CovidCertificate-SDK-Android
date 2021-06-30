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
import ch.admin.bag.covidcertificate.eval.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.models.healthcert.eu.Eudgc
import ch.admin.bag.covidcertificate.eval.models.trustlist.AcceptanceCriterias
import ch.admin.bag.covidcertificate.eval.models.trustlist.CertLogicData
import ch.admin.bag.covidcertificate.eval.models.trustlist.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.eval.models.trustlist.CertLogicPayload
import ch.admin.bag.covidcertificate.eval.models.trustlist.Rule
import ch.admin.bag.covidcertificate.eval.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.eval.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.eval.nationalrules.certlogic.isTruthy
import ch.admin.bag.covidcertificate.eval.extensions.validFromDate
import ch.admin.bag.covidcertificate.eval.extensions.validUntilDate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class NationalRulesVerifier(context: Context) {

	private val acceptedVaccineProvider = AcceptedVaccineProvider.getInstance(context)

	fun verify(euDgc: Eudgc, ruleSet: RuleSet, clock: Clock = Clock.systemDefaultZone()): CheckNationalRulesState {
		val payload = CertLogicPayload(euDgc.pastInfections, euDgc.tests, euDgc.vaccinations)
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(ruleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)

		for (rule in ruleSet.rules) {
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val isSuccessful = isTruthy(evaluate(ruleLogic, data))

			if (!isSuccessful) {
				return getErrorStateForRule(rule, euDgc, ruleSetData.external.valueSets.acceptanceCriteria)
			}
		}

		val validityRange = getValidityRange(euDgc, ruleSetData.external.valueSets.acceptanceCriteria)
		return if (validityRange != null) {
			CheckNationalRulesState.SUCCESS(validityRange)
		} else {
			CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}
	}

	private fun getErrorStateForRule(rule: Rule, euDgc: Eudgc, acceptanceCriterias: AcceptanceCriterias): CheckNationalRulesState {
		return when (rule.id) {
			"GR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET, rule.id)
			"VR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_VACCINE_ENTRIES, rule.id)
			"VR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NOT_FULLY_PROTECTED, rule.id)
			"VR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"VR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0004" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0005" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0006" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_TEST_ENTRIES, rule.id)
			"TR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.POSITIVE_RESULT, rule.id)
			"TR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_TEST_TYPE, rule.id)
			"TR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"TR-CH-0004" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0005" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0006" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0007" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_RECOVERY_ENTRIES, rule.id)
			"RR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0002" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0003" -> getValidityRange(euDgc, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			else -> CheckNationalRulesState.INVALID(NationalRulesError.UNKNOWN_RULE_FAILED, rule.id)
		}
	}

	private fun getValidityRange(euDgc: Eudgc, acceptanceCriterias: AcceptanceCriterias): ValidityRange? {
		return when {
			!euDgc.vaccinations.isNullOrEmpty() -> {
				val vaccination = euDgc.vaccinations.first()
				val usedVaccine = acceptedVaccineProvider.getVaccineDataFromList(vaccination)
				usedVaccine?.let {
					ValidityRange(vaccination.validFromDate(it, acceptanceCriterias),
						vaccination.validUntilDate(acceptanceCriterias))
				}
			}
			!euDgc.tests.isNullOrEmpty() -> {
				val test = euDgc.tests.first()
				ValidityRange(test.validFromDate(), test.validUntilDate(acceptanceCriterias))
			}
			!euDgc.pastInfections.isNullOrEmpty() -> {
				val recovery = euDgc.pastInfections.first()
				ValidityRange(recovery.validFromDate(acceptanceCriterias), recovery.validUntilDate(acceptanceCriterias))
			}
			else -> null
		}
	}
}

