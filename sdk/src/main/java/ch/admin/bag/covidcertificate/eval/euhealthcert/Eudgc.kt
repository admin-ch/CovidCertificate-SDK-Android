package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class Eudgc(
	@Json(name = "ver") val version: String,
	@Json(name = "nam") val person: PersonName,
	@Json(name = "dob") val dateOfBirth: String,
	@Json(name = "v") val vaccinations: List<VaccinationEntry>?,
	@Json(name = "t") val tests: List<TestEntry>?,
	@Json(name = "r") val pastInfections: List<RecoveryEntry>?,
) : Serializable
