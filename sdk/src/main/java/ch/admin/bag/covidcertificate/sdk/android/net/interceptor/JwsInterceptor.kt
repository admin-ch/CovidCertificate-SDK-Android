/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.net.interceptor

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.security.cert.X509Certificate

/**
 * Interceptor to verify a JSON Web Signature and return a response containing the JWS body as plain JSON.
 */
class JwsInterceptor(
	rootCA: X509Certificate,
	expectedCommonName: String,
) : Interceptor {

	companion object {
		// application/json media type for the payload of a JWS
		private val APPLICATION_JSON = "application/json".toMediaType()
	}

	private val keyResolver: JwsKeyResolver = JwsKeyResolver(rootCA, expectedCommonName)

	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val response: Response = chain.proceed(request)

		val requestAcceptsJws = request.header("Accept")?.contains("+jws") ?: false
		if (!response.isSuccessful || !requestAcceptsJws) {
			return response
		}

		val jws = response.body?.string() ?: throw IOException("Body has no signature")
		val body: String
		try {
			val claimsJws: Jws<Claims> = Jwts.parserBuilder()
				.setSigningKeyResolver(keyResolver)
				.build()
				.parseClaimsJws(jws)
			// now that the JWS is verified, we can safely assume that the body can be trusted, so serialize it to JSON again
			body = Moshi.Builder().build()
				.adapter<Claims>(Types.newParameterizedType(Map::class.java, String::class.java, Object::class.java))
				.toJson(claimsJws.body)
		} catch (e: Throwable) {
			e.printStackTrace()
			throw IOException("Failed to parse/verify JWS")
		}

		// SAFE ZONE
		// from here on body contains a JSON-string of the payload of the JWS, whose signature we verified.
		// the backend guarantees us that the body is JSON

		return response.newBuilder()
			.body(body.toResponseBody(APPLICATION_JSON))
			.build()
	}
}