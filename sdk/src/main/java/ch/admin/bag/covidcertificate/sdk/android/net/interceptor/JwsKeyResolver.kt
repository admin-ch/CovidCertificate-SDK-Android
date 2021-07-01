package ch.admin.bag.covidcertificate.sdk.android.net.interceptor

import android.util.Base64
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwsHeader.X509_CERT_CHAIN
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.jsonwebtoken.security.SignatureException
import java.io.ByteArrayInputStream
import java.security.Key
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

internal class JwsKeyResolver(
	private val rootCA: X509Certificate,
	private val expectedCommonName: String,
) : SigningKeyResolverAdapter() {

	/**
	 * Parses and verifies the certificate chain in the JwsHeader.
	 * Upon success, returns the public key with which the JWS is signed.
	 */
	override fun resolveSigningKey(jwsHeader: JwsHeader<*>, claims: Claims): Key {
		val encodedCertificates: List<String> = jwsHeader[X509_CERT_CHAIN] as? List<String>?
			?: throw SignatureException("JWS is missing the required certificate chain")
		val certificates: MutableList<X509Certificate> = mutableListOf()
		val certificateFactory = CertificateFactory.getInstance("X.509")

		// Check the certificate chain: each cert is certified by the next cert in the chain (i.e. the root CA comes last)
		for (cert in encodedCertificates) {
			val certBytes = Base64.decode(cert, Base64.DEFAULT)
			val byteInputStream = ByteArrayInputStream(certBytes)

			// catch exceptions and rethrow
			try {
				val currentCert = certificateFactory.generateCertificate(byteInputStream) as X509Certificate
				if (certificates.isNotEmpty()) {
					val previousCert = certificates.last()
					try {
						// Is the previous cert signed by the current cert?
						previousCert.verify(currentCert.publicKey)
					} catch (e: Exception) {
						throw SignatureException("Certificate chain cannot be verified")
					}
				}
				certificates.add(currentCert)
			} catch (e: Exception) {
				throw SignatureException("x5c is not a x509 certificate")
			}
		}

		if (certificates.isEmpty()) {
			throw SignatureException("Empty parsed certificate chain")
		}

		// Is the end of the chain signed by the given root CA?
		val checkWithRoot = certificates.last()
		try {
			checkWithRoot.verify(rootCA.publicKey)
		} catch (e: Exception) {
			throw SignatureException("Certificate chain cannot be verified")
		}

		val signingCertificate = certificates.first()

		// Extract Common Name
		val subjectName = signingCertificate.subjectX500Principal.name
		val startIndex = maxOf(0, subjectName.indexOf("CN="))
		var endIndex = subjectName.indexOf(",", startIndex)
		if (endIndex < startIndex) {
			endIndex = subjectName.length
		}

		// Check that signing cert belongs to the correct environment
		val commonName = subjectName.substring(startIndex + 3, endIndex)
		if (commonName != expectedCommonName) {
			throw SignatureException("Wrong CN! Got $commonName but expected $expectedCommonName")
		}

		return signingCertificate.publicKey
	}
}