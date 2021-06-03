/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

import android.content.Context
import ch.admin.bag.covidcertificate.eval.EvalErrorCodes.SIGNATURE_COSE_INVALID
import ch.admin.bag.covidcertificate.eval.chain.*
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.eval.utils.getHardcodedSigningKeys
import com.squareup.moshi.Moshi


object Eval {
	private val TAG = Eval::class.java.simpleName

	private val moshi by lazy { Moshi.Builder().build() }

	private val signingKeys = getHardcodedSigningKeys()

	/**
	 * Decodes the string from a QR code into a DCC.
	 *
	 * Does not do any validity checks. Simply checks whether the data is decodable.
	 *
	 * @param qrCodeData content of the scanned qr code, of the format "HC1:base45(...)"
	 */
	fun decode(qrCodeData: String): DecodeState {

		val encoded = PrefixIdentifierService.decode(qrCodeData) ?: return DecodeState.ERROR(Error(EvalErrorCodes.DECODE_PREFIX))

		val compressed = BagBase45Service.decode(encoded) ?: return DecodeState.ERROR(Error(EvalErrorCodes.DECODE_BASE_45))

		val cose = DecompressionService.decode(compressed) ?: return DecodeState.ERROR(Error(EvalErrorCodes.DECODE_Z_LIB))

		val cbor = NoopVerificationCoseService.decode(cose) ?: return DecodeState.ERROR(Error(EvalErrorCodes.DECODE_COSE))

		val bagdgc = CborService.decode(cbor, qrCodeData) ?: return DecodeState.ERROR(Error(EvalErrorCodes.DECODE_CBOR))

		bagdgc.certType = CertTypeService.decode(bagdgc.dgc)

		return DecodeState.SUCCESS(bagdgc)
	}

	/**
	 * Checks whether the DCC has a valid signature.
	 *
	 * A signature is only valid if it is signed by a trusted key, but also only if other attributes are valid
	 * (e.g. the signature is not expired - which may be different from the legal national rules).
	 */
	suspend fun checkSignature(bagdgc: Bagdgc, context: Context): CheckSignatureState {

		/* Check that certificate type and signature timestamps are valid */

		val type = bagdgc.certType ?: return CheckSignatureState.INVALID(EvalErrorCodes.SIGNATURE_TYPE_INVALID)

		val timestampError = TimestampService.decode(bagdgc)
		if (timestampError != null) {
			return CheckSignatureState.INVALID(timestampError)
		}

		/* Repeat decode chain to get and verify COSE signature */

		val encoded = PrefixIdentifierService.decode(bagdgc.qrCodeData)
			?: return CheckSignatureState.INVALID(EvalErrorCodes.DECODE_PREFIX)
		val compressed = BagBase45Service.decode(encoded) ?: return CheckSignatureState.INVALID(EvalErrorCodes.DECODE_BASE_45)
		val cose = DecompressionService.decode(compressed) ?: return CheckSignatureState.INVALID(EvalErrorCodes.DECODE_Z_LIB)

		val valid = VerificationCoseService.decode(signingKeys, cose, type)

		return if (valid) CheckSignatureState.SUCCESS else CheckSignatureState.INVALID(SIGNATURE_COSE_INVALID)
	}

	/**
	 * @param bagdgc Object which was returned from the decode function
	 * @return State for the revocation check
	 */
	suspend fun checkRevocationStatus(bagdgc: Bagdgc, context: Context): CheckRevocationState {
		return CheckRevocationState.SUCCESS
	}

	/**
	 * @param bagdgc Object which was returned from the decode function
	 * @return State for the Signaturecheck
	 */
	suspend fun checkNationalRules(bagdgc: Bagdgc, context: Context): CheckNationalRulesState {
		return if (!bagdgc.dgc.v.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyVaccine(bagdgc.dgc.v[0])
		} else if (!bagdgc.dgc.t.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyTest(bagdgc.dgc.t[0])
		} else if (!bagdgc.dgc.r.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyRecovery(bagdgc.dgc.r[0])
		} else {
			throw Exception("NO VALID DATA")
		}
	}
}