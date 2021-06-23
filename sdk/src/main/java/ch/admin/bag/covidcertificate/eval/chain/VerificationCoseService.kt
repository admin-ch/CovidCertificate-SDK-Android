/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.models.Jwk

internal object VerificationCoseService {
	private val TAG = VerificationCoseService::class.java.simpleName

	fun decode(
		keys: List<Jwk>,
		input: ByteArray,
		type: CertType
	): Boolean {

		val signature: Sign1Message = try {
			(Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message)
		} catch (e: Throwable) {
			null
		} ?: return false

		for (k in keys) {
			val pk = k.getPublicKey() ?: continue

			try {
				val pubKey = OneKey(pk, null)
				if (signature.validate(pubKey)) {
					return true
				}
			} catch (e: Throwable) {
				// Key failed to verify the signature, try the next key
			}
		}

		return false
	}

}