package ch.admin.bag.covidcertificate.sdk.core.verifier

import ch.admin.bag.covidcertificate.sdk.core.HC1_A
import ch.admin.bag.covidcertificate.sdk.core.LT1_A
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.sdk.core.getCertificateLightTestKey
import ch.admin.bag.covidcertificate.sdk.core.getHardcodedSigningKeys
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.DccHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.products.AcceptedVaccine
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.AcceptanceCriterias
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwk
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificates
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleValueSets
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesVerifier
import com.squareup.moshi.Moshi
import com.sun.net.httpserver.Authenticator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class CertificateVerifierTest {

	private lateinit var acceptedVaccineProvider: AcceptedVaccineProvider
	private lateinit var nationalRulesVerifier: NationalRulesVerifier
	private lateinit var certificateVerifier: CertificateVerifier

	@BeforeEach
	fun setup() {
		val acceptedVaccineContent = this::class.java.classLoader.getResource("acceptedCHVaccine.json")!!.readText()
		val acceptedVaccines = Moshi.Builder().build().adapter(AcceptedVaccine::class.java).fromJson(acceptedVaccineContent)!!

		acceptedVaccineProvider = object : AcceptedVaccineProvider {
			override fun getVaccineName(vaccinationEntry: VaccinationEntry) =
				acceptedVaccines.entries.first { it.code == vaccinationEntry.medicinialProduct }.name

			override fun getProphylaxis(vaccinationEntry: VaccinationEntry) =
				acceptedVaccines.entries.first { it.code == vaccinationEntry.medicinialProduct }.prophylaxis

			override fun getAuthHolder(vaccinationEntry: VaccinationEntry) =
				acceptedVaccines.entries.first { it.code == vaccinationEntry.medicinialProduct }.auth_holder

			override fun getVaccineDataFromList(vaccinationEntry: VaccinationEntry) =
				acceptedVaccines.entries.firstOrNull { it.code == vaccinationEntry.medicinialProduct }
		}

		nationalRulesVerifier = NationalRulesVerifier(acceptedVaccineProvider)
		certificateVerifier = CertificateVerifier(nationalRulesVerifier)
	}

	@Test
	fun testFullCertificateInvalidSignature() {
		val dccHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList()

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)
			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testFullCertificateWrongSignature() {
		val dccHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList(getHardcodedSigningKeys("abn"))

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)
			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testFullCertificateRevocation() {
		val dccHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList(
			signingKeys = getHardcodedSigningKeys("dev"),
			revokedKeyIds = listOf("01:CH:42A272C9E1CAA43D934142C9")
		)

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.SUCCESS)

			assertTrue(invalidState.revocationState is CheckRevocationState.INVALID)
			assertEquals(
				ErrorCodes.REVOCATION_REVOKED,
				(invalidState.revocationState as CheckRevocationState.INVALID).revocationErrorCode
			)

			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testFullCertificateValid() {
		val dccHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList(getHardcodedSigningKeys("dev"))

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.SUCCESS)
		}
	}

	@Test
	fun testCertificateLightInvalidSignature() {
		val dccHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList()

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)
			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testCertificateLightWrongSignature() {
		val dccHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList(getHardcodedSigningKeys("dev"))

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)
			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testCertificateLightValid() {
		val dccHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList(listOf(getCertificateLightTestKey()))

		runBlocking {
			val verificationState = certificateVerifier.verify(dccHolder, trustList)
			assertTrue(verificationState is VerificationState.SUCCESS)

			val successState = verificationState as VerificationState.SUCCESS
			val expectedValidFrom = LocalDateTime.ofInstant(dccHolder.issuedAt!!, ZoneId.systemDefault())
			assertEquals(expectedValidFrom, successState.validityRange.validFrom)

			val expectedValidUntil = LocalDateTime.ofInstant(dccHolder.expirationTime!!, ZoneId.systemDefault())
			assertEquals(expectedValidUntil, successState.validityRange.validUntil)
		}
	}

	private fun decodeCertificate(qrCodeData: String): DccHolder {
		val decodeState = CertificateDecoder.decode(qrCodeData)
		assertTrue(decodeState is DecodeState.SUCCESS)

		return (decodeState as DecodeState.SUCCESS).dccHolder
	}

	private fun createTrustList(
		signingKeys: List<Jwk> = emptyList(),
		revokedKeyIds: List<String> = emptyList(),
		ruleSet: RuleSet? = null
	): TrustList {
		return TrustList(
			Jwks(signingKeys),
			RevokedCertificates(revokedKeyIds, Long.MAX_VALUE),
			ruleSet ?: RuleSet(
				emptyList(),
				RuleValueSets(null, null, null, null, null, null, AcceptanceCriterias(0, 0, 0, 0, 0, 0)),
				0L
			)
		)
	}

}