package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.TestDataGenerator
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.DccHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.PersonName
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.Eudgc
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.DccLight
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class CertTypeServiceTest {

	@Test
	fun testDccHolderWithoutCertificate() {
		assertThrows<IllegalArgumentException> {
			DccHolder("", null, null, null, null, null)
		}
	}

	@Test
	fun testDccHolderWithBothCertificates() {
		assertThrows<IllegalArgumentException> {
			val person = PersonName("FamilyName", "StandardizedFamilyName", "GivenName", "StandardizedGivenName")
			DccHolder(
				"",
				Eudgc("1.0.0", person, "1990-12-31", emptyList(), emptyList(), emptyList()),
				DccLight("1.0.0", person, "1990-12-31"),
				null,
				null,
				null
			)
		}
	}

	@Test
	fun testDccHolderIsCertificateLight() {
		val person = PersonName("FamilyName", "StandardizedFamilyName", "GivenName", "StandardizedGivenName")
		val dccHolder = DccHolder("", null, DccLight("1.0.0", person, "1990-12-31"), null, null, null)
		assertTrue(dccHolder.isLightCertificate(), "DccHolder should contain a certificate light")

		val certificateType = CertTypeService.decode(dccHolder)
		assertEquals(CertType.LIGHT, certificateType)
	}

	@Test
	fun testDccHolderIsVaccination() {
		val euDgc = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			LocalDate.now().minusDays(10).atStartOfDay(),
		)

		val dccHolder = DccHolder("", euDgc, null, null, null, null)
		assertTrue(dccHolder.isFullCertificate(), "DccHolder should contain a full certificate")

		val certificateType = CertTypeService.decode(dccHolder)
		assertEquals(CertType.VACCINATION, certificateType)
	}

	@Test
	fun testDccHolderIsTest() {
		val euDgc = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-1)
		)

		val dccHolder = DccHolder("", euDgc, null, null, null, null)
		assertTrue(dccHolder.isFullCertificate(), "DccHolder should contain a full certificate")

		val certificateType = CertTypeService.decode(dccHolder)
		assertEquals(CertType.TEST, certificateType)
	}

	@Test
	fun testDccHolderIsRecovery() {
		val euDgc = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val dccHolder = DccHolder("", euDgc, null, null, null, null)
		assertTrue(dccHolder.isFullCertificate(), "DccHolder should contain a full certificate")

		val certificateType = CertTypeService.decode(dccHolder)
		assertEquals(CertType.RECOVERY, certificateType)
	}

	@Test
	fun testDccHolderIsOnlyOneType() {
		val testEuDgc = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-1)
		)

		val recoveryEuDgc = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val euDgc = testEuDgc.copy(pastInfections = recoveryEuDgc.pastInfections)

		val dccHolder = DccHolder("", euDgc, null, null, null, null)
		assertTrue(dccHolder.isFullCertificate(), "DccHolder should contain a full certificate")

		val certificateType = CertTypeService.decode(dccHolder)
		assertNull(certificateType, "Certificate can only contain one of the three data sets (v, t or r)")
	}

}