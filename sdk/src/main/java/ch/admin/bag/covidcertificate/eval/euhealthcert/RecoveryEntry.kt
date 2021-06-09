package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class RecoveryEntry(
	@Json(name = "tg") @get:JsonProperty("tg") val disease: String,
	@Json(name = "fr") @get:JsonProperty("fr") val dateFirstPositiveTest: String,
	@Json(name = "co") @get:JsonProperty("co") val countryOfTest: String,
	@Json(name = "is") @get:JsonProperty("is") val certificateIssuer: String,
	@Json(name = "df") @get:JsonProperty("df") val validFrom: String,
	@Json(name = "du") @get:JsonProperty("du") val validUntil: String,
	@Json(name = "ci") @get:JsonProperty("ci") val certificateIdentifier: String,
) : Serializable
