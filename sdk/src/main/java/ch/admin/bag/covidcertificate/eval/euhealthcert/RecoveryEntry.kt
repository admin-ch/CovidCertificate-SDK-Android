package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class RecoveryEntry(
	@Json(name = "tg") val disease: String,
	@Json(name = "fr") val dateFirstPositiveTest: String,
	@Json(name = "co") val countryOfTest: String,
	@Json(name = "is") val certificateIssuer: String,
	@Json(name = "df") val validFrom: String,
	@Json(name = "du") val validUntil: String,
	@Json(name = "ci") val certificateIdentifier: String,
) : Serializable
