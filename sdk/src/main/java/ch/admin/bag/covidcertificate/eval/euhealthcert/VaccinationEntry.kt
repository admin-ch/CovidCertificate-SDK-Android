package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class VaccinationEntry(
	@Json(name = "tg") @get:JsonProperty("tg") val disease: String,
	@Json(name = "vp") @get:JsonProperty("vp") val vaccine: String,
	@Json(name = "mp") @get:JsonProperty("mp") val medicinialProduct: String,
	@Json(name = "ma") @get:JsonProperty("ma") val marketingAuthorizationHolder: String,
	@Json(name = "dn") @get:JsonProperty("dn") val doseNumber: Int,
	@Json(name = "sd") @get:JsonProperty("sd") val totalDoses: Int,
	@Json(name = "dt") @get:JsonProperty("dt") val vaccinationDate: String,
	@Json(name = "co") @get:JsonProperty("co") val country: String,
	@Json(name = "is") @get:JsonProperty("is") val certificateIssuer: String,
	@Json(name = "ci") @get:JsonProperty("ci") val certificateIdentifier: String,
) : Serializable
