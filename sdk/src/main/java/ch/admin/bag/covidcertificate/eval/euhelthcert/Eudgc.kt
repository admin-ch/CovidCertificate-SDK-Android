package ch.admin.bag.covidcertificate.eval.euhelthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class Eudgc (
	@Json(name = "ver") val ver: String,
	@Json(name = "nam") val nam: PersonName,
	@Json(name = "dob") val dob: String,
	@Json(name = "v") val v: List<VaccinationEntry>?,
	@Json(name = "t") val t: List<TestEntry>?,
	@Json(name = "r") val r: List<RecoveryEntry>?,
): Serializable
