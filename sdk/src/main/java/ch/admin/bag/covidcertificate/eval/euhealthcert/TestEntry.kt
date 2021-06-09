package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.util.*

@JsonClass(generateAdapter = true)
data class TestEntry(
	@Json(name = "tg") @get:JsonProperty("tg") val disease: String,
	@Json(name = "tt") @get:JsonProperty("tt") val type: String,
	@Json(name = "nm") @get:JsonProperty("nm") val naaTestName: String?,
	@Json(name = "ma") @get:JsonProperty("ma") val ratTestNameAndManufacturer: String?,
	@Json(name = "sc") @get:JsonProperty("sc") val timestampSample: Date,
	@Json(name = "dr") @get:JsonProperty("dr") val timestampResult: Date?,
	@Json(name = "tr") @get:JsonProperty("tr") val result: String,
	@Json(name = "tc") @get:JsonProperty("tc") val testCenter: String,
	@Json(name = "co") @get:JsonProperty("co") val country: String,
	@Json(name = "is") @get:JsonProperty("is") val certificateIssuer: String,
	@Json(name = "ci") @get:JsonProperty("ci") val certificateIdentifier: String,
) : Serializable
