package ch.admin.bag.covidcertificate.eval.euhelthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

/*
* @JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
   "tg", disease or agent targeted
        "vp", vaccine or prophylaxis
        "mp",vaccine medicinal product
        "ma",Marketing Authorization Holder - if no MAH present, then manufacturer
        "dn",Dose Number
        "sd", Total Series of Doses
        "dt",Date of Vaccination
        "co",Country of Vaccination
        "is",Certificate Issuer
        "ci"Unique Certificate Identifier: UVCI
})*/
@JsonClass(generateAdapter = true)
data class VaccinationEntry(
	@Json(name = "tg") val tg: String,
	@Json(name = "vp") val vp: String,
	@Json(name = "mp") val mp: String,
	@Json(name = "ma") val ma: String,
	@Json(name = "dn") val dn: Int,
	@Json(name = "sd") val sd: Int,
	@Json(name = "dt") val dt: String,
	@Json(name = "co") val co: String,
	@Json(name = "is") val `is`: String,
	@Json(name = "ci") val ci: String,
) : Serializable
