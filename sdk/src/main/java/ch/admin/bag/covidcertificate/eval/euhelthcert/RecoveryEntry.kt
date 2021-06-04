package ch.admin.bag.covidcertificate.eval.euhelthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

/*
*   "tg",disease-agent-targeted
        "fr", ISO 8601 Date of First Positive Test Result
        "co",Country of Test
        "is", Certificate Issuer
        "df",ISO 8601 Date: Certificate Valid From
        "du",Certificate Valid Until
        "ci"Unique Certificate Identifier, UVCI
        * */

@JsonClass(generateAdapter = true)
data class RecoveryEntry(
	@Json(name = "tg") val tg: String,
	@Json(name = "fr") val fr: String,
	@Json(name = "co") val co: String,
	@Json(name = "is") val `is`: String,
	@Json(name = "df") val df: String,
	@Json(name = "du") val du: String,
	@Json(name = "ci") val ci: String,
) : Serializable
