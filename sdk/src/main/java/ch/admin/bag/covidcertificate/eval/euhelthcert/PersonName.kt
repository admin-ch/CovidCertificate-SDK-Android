package ch.admin.bag.covidcertificate.eval.euhelthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

/*
@JsonPropertyOrder({
    "fn" family name,
    "fnt" The family name(s) of the person transliterated,
    "gn" The given name(s) of the person addressed in the certificate,
    "gnt"The given name(s) of the person transliterated
})*/
@JsonClass(generateAdapter = true)
data class PersonName(
	@Json(name = "fn") val fn: String?,
	@Json(name = "fnt") val fnt: String,
	@Json(name = "gn") val gn: String,
	@Json(name = "gnt") val gnt: String,
) : Serializable