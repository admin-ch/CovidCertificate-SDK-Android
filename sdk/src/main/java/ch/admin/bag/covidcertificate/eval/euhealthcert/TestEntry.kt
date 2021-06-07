package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.util.*

@JsonClass(generateAdapter = true)
data class TestEntry(
	@Json(name = "tg") val disease: String,
	@Json(name = "tt") val type: String,
	@Json(name = "nm") val naaTestName: String?,
	@Json(name = "ma") val ratTestNameAndManufacturer: String?,
	@Json(name = "sc") val timestampSample: Date,
	@Json(name = "dr") val timestampResult: Date?,
	@Json(name = "tr") val result: String,
	@Json(name = "tc") val testCenter: String,
	@Json(name = "co") val country: String,
	@Json(name = "is") val certificateIssuer: String,
	@Json(name = "ci") val certificateIdentifier: String,
) : Serializable
