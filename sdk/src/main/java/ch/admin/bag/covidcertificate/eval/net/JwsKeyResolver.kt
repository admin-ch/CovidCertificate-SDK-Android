package ch.admin.bag.covidcertificate.eval.net

import android.util.Base64
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.SigningKeyResolverAdapter
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.security.Key
import io.jsonwebtoken.security.SignatureException
import java.security.cert.CertificateFactory

import java.security.cert.X509Certificate

 class JwsKeyResolver(private val rootCA: X509Certificate) : SigningKeyResolverAdapter() {

    override fun resolveSigningKey(jwsHeader: JwsHeader<*>, claims: Claims) : Key
     {
         val encodedCertificates : List<String> = jwsHeader["x5c"] as List<String>? ?: throw SignatureException("JWS is missing the required certificate chain")
        var certificates : MutableList<X509Certificate> = mutableListOf()
        val certificateFactory = CertificateFactory.getInstance("X.509")
        for ( cert in encodedCertificates) {
            val certBytes = Base64.decode(cert, Base64.DEFAULT)
            val byteInputStream = ByteArrayInputStream(certBytes)
            // catch exception and rethrow
            try {
                val x509 = certificateFactory.generateCertificate(byteInputStream) as X509Certificate
                if (certificates.isNotEmpty()) {
                    val certificateToVerify = certificates.last()
                    try {
                        certificateToVerify.verify(x509.publicKey)
                    } catch (e: Exception) {
                        throw SignatureException("Certificate chain cannot be verified")
                    }
                }
                certificates.add(x509)
            } catch (e: Exception) {
                throw SignatureException("x5c is not a x509 certificate")
            }
        }

        val checkWithRoot = certificates.last()
         try {
             checkWithRoot.verify(rootCA.publicKey)
         } catch (e: Exception) {
             throw SignatureException("Certificate chain cannot be verified")
         }

         return certificates[0].publicKey
     }
 }