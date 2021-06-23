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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ch.admin.bag.covidcertificate.eval.TestDataGenerator
import ch.admin.bag.covidcertificate.eval.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.eval.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import ch.admin.bag.covidcertificate.eval.utils.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.eval.utils.TestType
import ch.admin.bag.covidcertificate.eval.utils.isTargetDiseaseCorrect
import ch.admin.bag.covidcertificate.eval.utils.validFromDate
import ch.admin.bag.covidcertificate.eval.utils.validUntilDate
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


@RunWith(AndroidJUnit4::class)
class NationalRulesVerifierTest {

	private lateinit var instrumentationContext: Context
	private lateinit var nationalRulesVerifier: NationalRulesVerifier
	private lateinit var nationalRuleSet: RuleSet

	@Before
	fun setup() {
		instrumentationContext = InstrumentationRegistry.getInstrumentation().context
		nationalRulesVerifier = NationalRulesVerifier(instrumentationContext)

		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		val fileContent = instrumentationContext.assets.open("nationalrules.json").bufferedReader().use { it.readText() }
		nationalRuleSet = moshi.adapter(RuleSet::class.java).fromJson(fileContent)!!
	}

	/// VACCINE TESTS

	@Test
	fun testVaccineDiseaseTargetedHasToBeSarsCoV2() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccineMustBeInWhitelist() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"1119349007",
			vaccinationDate,
		)
		val invalidCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529/INVALID",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"1119349007",
			vaccinationDate,
		)

		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.INVALID && result.nationalRulesError == NationalRulesError.NO_VALID_PRODUCT)
	}

	@Test
	fun test2of2VaccineIsValidToday() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccine1of1WithPreviousInfectionsIsValidToday() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)
		// Sanity check - this vaccine usually needs 2 shots
		val acp = AcceptedVaccineProvider.getInstance(instrumentationContext)
		assertEquals(acp.getVaccineDataFromList(cert.vaccinations!!.first())!!.total_dosis_number, 2)

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccine1of1IsValidAfter21Days() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val nowDate = LocalDate.now(clock).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001417",
			"EU/1/20/1525",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate.minusDays(21),
		)

		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001417",
			"EU/1/20/1525",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate.minusDays(20),
		)
		val expectedValidFrom = LocalDate.now(clock).plusDays(1).atStartOfDay()

		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.NOT_YET_VALID)

		result = result as CheckNationalRulesState.NOT_YET_VALID
		assertEquals(result.validityRange.validFrom!!, expectedValidFrom)
	}

	@Test
	fun testVaccineUntilDatesSuccess() {
		val validDateFrom = LocalDate.of(2021, 1, 3).atStartOfDay()
		val validDateUntil = LocalDate.of(2022, 1, 2).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			validDateFrom,
		)
		val vaccinationEntry = validCert.vaccinations!!.first()
		assertTrue(vaccinationEntry.validUntilDate(nationalRuleSet.valueSets.acceptanceCriteria) == validDateUntil)
		val validResultTodayBeforeUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-06-30T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayBeforeUntilDate is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-07-01T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualUntilDate is CheckNationalRulesState.SUCCESS)

		val invalidResultTodayAfterUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2022-01-03T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayAfterUntilDate is CheckNationalRulesState.NOT_VALID_ANYMORE)

		val invalidResultTodayBeforeFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-01-02T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayBeforeFromDate is CheckNationalRulesState.NOT_YET_VALID)
	}

	@Test
	fun testWeNeedAllShots() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val nowDate = LocalDate.now(clock).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate,
		)
		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateVaccineCert(
			1,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate,
		)
		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, clock)
		assertTrue(result is CheckNationalRulesState.INVALID)

		result = result as CheckNationalRulesState.INVALID
		assertEquals(result.nationalRulesError, NationalRulesError.NOT_FULLY_PROTECTED)
	}

	/// TEST TESTS

	@Test
	fun testTestDiseaseTargetedHasToBeSarsCoV2() {
		val duration = Duration.ofHours(-10)
		val validCert = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			duration
		)
		assertTrue(validCert.tests!!.first().isTargetDiseaseCorrect())
		val result = nationalRulesVerifier.verify(validCert, nationalRuleSet)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			"01123",
			duration
		)

		assertFalse(invalidCert.tests!!.first().isTargetDiseaseCorrect())
		val wrongResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet)
		assertTrue(wrongResult is CheckNationalRulesState.INVALID)


	}


	@Test
	fun testTypeHasToBePcrOrRat() {
		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)
		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)
		val invalidTest = TestDataGenerator.generateTestCert(
			"INVALID_TEST_TYPE",
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)

		val validRatResult = nationalRulesVerifier.verify(validRat, nationalRuleSet)

		assertTrue(validRatResult is CheckNationalRulesState.SUCCESS)

		val validPcrResult = nationalRulesVerifier.verify(validPcr, nationalRuleSet)
		assertTrue(validPcrResult is CheckNationalRulesState.SUCCESS)

		val invalidTestResult = nationalRulesVerifier.verify(invalidTest, nationalRuleSet)
		if (invalidTestResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidTestResult.nationalRulesError == NationalRulesError.WRONG_TEST_TYPE)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testTestHasToBeInWhitelist() {
		val invalidTest = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"abcdef",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)
		val invalidTestResult = nationalRulesVerifier.verify(invalidTest, nationalRuleSet)
		if (invalidTestResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidTestResult.nationalRulesError == NationalRulesError.NO_VALID_PRODUCT)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testPcrTestsAreAlwaysAccepted() {
		var validTest = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1097",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)
		var result = nationalRulesVerifier.verify(validTest, nationalRuleSet)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testPcrIsValidFor72h() {
		var validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-71)
		)
		var result = nationalRulesVerifier.verify(validPcr, nationalRuleSet)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		var invalidPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-72)
		)
		var invalid = nationalRulesVerifier.verify(invalidPcr, nationalRuleSet)
		if (invalid is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testRatIsValidFor24h() {
		var validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-47)
		)
		var result = nationalRulesVerifier.verify(validRat, nationalRuleSet)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		var invalidPcr = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-48)
		)
		var invalid = nationalRulesVerifier.verify(invalidPcr, nationalRuleSet)
		if (invalid is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testTestResultHasToBeNegative() {
		var validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			"positive",
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)
		var validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			"positive",
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10)
		)

		var invalidRat = nationalRulesVerifier.verify(validRat, nationalRuleSet)
		var invalidPcr = nationalRulesVerifier.verify(validPcr, nationalRuleSet)

		if (invalidRat is CheckNationalRulesState.INVALID &&
			invalidPcr is CheckNationalRulesState.INVALID
		) {
			assertTrue(invalidRat.nationalRulesError == NationalRulesError.POSITIVE_RESULT)
			assertTrue(invalidPcr.nationalRulesError == NationalRulesError.POSITIVE_RESULT)
		} else {
			assertFalse(true)
		}
	}

	/// RECOVERY TESTS

	@Test
	fun testRecoveryDiseaseTargetedHasToBeSarsCoV2() {
		var validRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)
		var invalidRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			"INVALID DISEASE"
		)

		var validRecoveryResult = nationalRulesVerifier.verify(validRecovery, nationalRuleSet)
		var invalidRecoveryResult = nationalRulesVerifier.verify(invalidRecovery, nationalRuleSet)
		assertTrue(validRecoveryResult is CheckNationalRulesState.SUCCESS)

		if (invalidRecoveryResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidRecoveryResult.nationalRulesError == NationalRulesError.WRONG_DISEASE_TARGET)
		}
	}

	@Test
	fun testRecoveryUntilDatesSuccess() {
		val firstTestResult = LocalDate.of(2021, 5, 8).atStartOfDay()
		val validDateFrom = LocalDate.of(2021, 5, 18).atStartOfDay()
		val validDateUntil = LocalDate.of(2021, 11, 3).atStartOfDay()
		val validCert = TestDataGenerator.generateRecoveryCertFromDate(
			validDateFrom = validDateFrom,
			validDateUntil = validDateUntil,
			firstTestResult = firstTestResult,
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		assertTrue(validCert.pastInfections!!.first().validFromDate(nationalRuleSet.valueSets.acceptanceCriteria) == validDateFrom)
		val todayBeforeUtil = Clock.fixed(Instant.parse("2021-11-02T12:00:00Z"), ZoneId.systemDefault())
		val validResultBefore = nationalRulesVerifier.verify(validCert, nationalRuleSet, todayBeforeUtil)
		assertTrue(validResultBefore is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualToUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-11-03T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualToUntilDate is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualToFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-05-18T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualToFromDate is CheckNationalRulesState.SUCCESS)

		val invalidResultTodayIsAfterUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-11-04T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayIsAfterUntilDate is CheckNationalRulesState.NOT_VALID_ANYMORE)

		val invalidResultTodayIsBeforeFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			Clock.fixed(Instant.parse("2021-05-17T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayIsBeforeFromDate is CheckNationalRulesState.NOT_YET_VALID)
	}

	@Test
	fun testCertificateIsValidFor180DaysAfter() {
		val validCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-179),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)
		val invalidCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-180),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val validResult = nationalRulesVerifier.verify(validCert, nationalRuleSet)
		val invalidResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet)

		assertTrue(validResult is CheckNationalRulesState.SUCCESS)
		assertTrue(invalidResult is CheckNationalRulesState.NOT_VALID_ANYMORE)
	}

	@Test
	fun testTestIsOnlyValid10DaysAfterTestResult() {
		val validCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-10),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)
		val invalidCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-9),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val validResult = nationalRulesVerifier.verify(validCert, nationalRuleSet)
		val invalidResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet)

		assertTrue(validResult is CheckNationalRulesState.SUCCESS)
		assertTrue(invalidResult is CheckNationalRulesState.NOT_YET_VALID)
	}
}