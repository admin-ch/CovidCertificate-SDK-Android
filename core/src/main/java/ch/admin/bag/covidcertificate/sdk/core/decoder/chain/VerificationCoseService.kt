/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwk
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

internal object VerificationCoseService {

	init {
		Security.removeProvider("BC")
		Security.addProvider(BouncyCastleProvider())
	}

	fun decode(keys: List<Jwk>, input: ByteArray): Boolean {
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