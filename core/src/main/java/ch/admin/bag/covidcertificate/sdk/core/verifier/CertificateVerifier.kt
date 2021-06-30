/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.verifier

import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.Base45Service
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.DecompressionService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.PrefixIdentifierService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.RevokedHealthCertService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.TimestampService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.VerificationCoseService
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.DccHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificates
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.ValidityRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

class CertificateVerifier(private val nationalRulesVerifier: NationalRulesVerifier) {

	/**
	 * Verify the validity of a certificate. This checks the certificate signature, its revocation status as well as the conformity
	 * with national rules.
	 *
	 * @param dccHolder The object returned from the decoder
	 * @param trustList The current applicable trust list, containing active public keys for signing, revoked certificate identifiers and the national rule set
	 * @return Outcome state of the verification
	 */
	suspend fun verify(dccHolder: DccHolder, trustList: TrustList): VerificationState = withContext(Dispatchers.Default) {
		// Execute all three checks in parallel...
		val checkSignatureStateDeferred = async { checkSignature(dccHolder, trustList.signatures) }
		val checkRevocationStateDeferred = async { checkRevocationStatus(dccHolder, trustList.revokedCertificates) }
		val checkNationalRulesStateDeferred = async { checkNationalRules(dccHolder, nationalRulesVerifier, trustList.ruleSet) }

		// ... but wait for all of them to finish
		val checkSignatureState = checkSignatureStateDeferred.await()
		val checkRevocationState = checkRevocationStateDeferred.await()
		val checkNationalRulesState = checkNationalRulesStateDeferred.await()

		if (checkSignatureState is CheckSignatureState.ERROR) {
			VerificationState.ERROR(checkSignatureState.error, checkNationalRulesState.validityRange())
		} else if (checkRevocationState is CheckRevocationState.ERROR) {
			VerificationState.ERROR(checkRevocationState.error, checkNationalRulesState.validityRange())
		} else if (checkNationalRulesState is CheckNationalRulesState.ERROR) {
			VerificationState.ERROR(checkNationalRulesState.error, null)
		} else if (
			checkSignatureState == CheckSignatureState.SUCCESS
			&& checkRevocationState == CheckRevocationState.SUCCESS
			&& checkNationalRulesState is CheckNationalRulesState.SUCCESS
		) {
			VerificationState.SUCCESS(checkNationalRulesState.validityRange)
		} else if (
			checkSignatureState is CheckSignatureState.INVALID
			|| checkRevocationState is CheckRevocationState.INVALID
			|| checkNationalRulesState is CheckNationalRulesState.INVALID
			|| checkNationalRulesState is CheckNationalRulesState.NOT_YET_VALID
			|| checkNationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE
		) {
			VerificationState.INVALID(
				checkSignatureState, checkRevocationState, checkNationalRulesState,
				checkNationalRulesState.validityRange()
			)
		} else {
			VerificationState.LOADING
		}
	}

	/**
	 * Checks whether a certificate has a valid signature.
	 *
	 * A signature is only valid if it is signed by a trusted key, but also only if other attributes are valid
	 * (e.g. the signature is not expired - which may be different from the legal national rules).
	 *
	 * @param dccHolder The object returned from the decoder
	 * @param signatures A list of active public keys used for signing certificates
	 * @return Outcome state of the signature check
	 */
	private suspend fun checkSignature(dccHolder: DccHolder, signatures: Jwks) = withContext(Dispatchers.Default) {
		try {
			// Check that certificate type and signature timestamps are valid
			val type = dccHolder.certType ?: return@withContext CheckSignatureState.INVALID(ErrorCodes.SIGNATURE_TYPE_INVALID)

			val timestampError = TimestampService.decode(dccHolder)
			if (timestampError != null) {
				return@withContext CheckSignatureState.INVALID(timestampError)
			}

			// Repeat decode chain to get and verify COSE signature
			val encoded = PrefixIdentifierService.decode(dccHolder.qrCodeData)
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.DECODE_PREFIX)
			val compressed = Base45Service.decode(encoded)
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.DECODE_BASE_45)
			val cose = DecompressionService.decode(compressed)
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.DECODE_Z_LIB)

			val valid = VerificationCoseService.decode(signatures.certs, cose)
			if (valid) {
				CheckSignatureState.SUCCESS
			} else {
				CheckSignatureState.INVALID(ErrorCodes.SIGNATURE_COSE_INVALID)
			}
		} catch (e: Exception) {
			CheckSignatureState.ERROR(StateError(ErrorCodes.SIGNATURE_UNKNOWN, e.message.toString(), dccHolder))
		}
	}

	/**
	 * Checks whether a certificate has been revoked
	 *
	 * @param dccHolder The object returned from the decoder
	 * @param revokedCertificates The list of revoked certificate identifiers from the trust list
	 * @return Outcome state of the revocation check
	 */
	private suspend fun checkRevocationStatus(
		dccHolder: DccHolder,
		revokedCertificates: RevokedCertificates
	) = withContext(Dispatchers.Default) {
		// Revocation is not possible for light certificates, so this check always returns SUCCESS for them
		if (dccHolder.isLightCertificate()) return@withContext CheckRevocationState.SUCCESS

		try {
			val revokedCertificateService = RevokedHealthCertService(revokedCertificates)
			val containsRevokedCertificate = revokedCertificateService.isRevoked(dccHolder.euDGC!!)

			if (containsRevokedCertificate) {
				CheckRevocationState.INVALID(ErrorCodes.REVOCATION_REVOKED)
			} else {
				CheckRevocationState.SUCCESS
			}
		} catch (e: Exception) {
			CheckRevocationState.ERROR(StateError(ErrorCodes.REVOCATION_UNKNOWN, e.message.toString(), dccHolder))
		}
	}

	/**
	 * Checks whether a certificate conforms to a set of national rules
	 *
	 * @param dccHolder The object returned from the decoder
	 * @param nationalRulesVerifier The verifier responsible to verify a certificate against a national rule set
	 * @param ruleSet The national rule set from the trust list
	 * @return Outcome state of the national rules check
	 */
	private suspend fun checkNationalRules(
		dccHolder: DccHolder,
		nationalRulesVerifier: NationalRulesVerifier,
		ruleSet: RuleSet
	) = withContext(Dispatchers.Default) {
		try {
			if (dccHolder.isLightCertificate()) {
				// If the DccHolder contains a light certificate, the national rules don't have to be verified
				// In that case, the validity range is from the issuedAt date until the expiration date from the CWT headers
				val issued = dccHolder.issuedAt?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
				val expiration = dccHolder.expirationTime?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
				CheckNationalRulesState.SUCCESS(ValidityRange(issued, expiration))
			} else {
				nationalRulesVerifier.verify(dccHolder.euDGC!!, ruleSet)
			}
		} catch (e: Exception) {
			CheckNationalRulesState.ERROR(StateError(ErrorCodes.RULESET_UNKNOWN, e.message.toString(), dccHolder))
		}
	}

}