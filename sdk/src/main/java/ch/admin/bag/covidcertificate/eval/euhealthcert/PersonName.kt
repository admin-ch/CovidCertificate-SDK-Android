package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class PersonName(
	@Json(name = "fn") val familyName: String?,
	@Json(name = "fnt") val standardizedFamilyName: String,
	@Json(name = "gn") val givenName: String?,
	@Json(name = "gnt") val standardizedGivenName: String?,
) : Serializable