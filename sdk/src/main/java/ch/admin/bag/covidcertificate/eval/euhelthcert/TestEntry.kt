package ch.admin.bag.covidcertificate.eval.euhelthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

/*
* required": [
        "tg",disease-agent-targeted
        "tt", test-type
       	"nm" NAA Test Name
       	"ma" RAT Test name and manufacturer
        "sc", Date/Time of Sample Collection
        "dr", Date/Time of Test Result
        "tr", Test Result
        "tc",Testing Centre
        "co",Country of Test
        "is",Certificate Issuer
        "ci" Unique Certificate Identifier, UVCI
      ],*/

@JsonClass(generateAdapter = true)
data class TestEntry(
	@Json(name = "tg") val tg: String,
	@Json(name = "tt") val tt: String,
	@Json(name = "nm") val nm: String?,
	@Json(name = "ma") val ma: String?,
	@Json(name = "sc") val sc: Date,
	@Json(name = "dr") val dr: Date?,
	@Json(name = "tr") val tr: String,
	@Json(name = "tc") val tc: String,
	@Json(name = "co") val co: String,
	@Json(name = "is") val `is`: String,
	@Json(name = "ci") val ci: String,
) : Serializable
