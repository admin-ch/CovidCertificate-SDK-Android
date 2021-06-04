package ch.admin.bag.covidcertificate.eval.euhealthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class VaccinationEntry(
	@Json(name = "tg") val disease: String,
	@Json(name = "vp") val vaccine: String,
	@Json(name = "mp") val medicinialProduct: String,
	@Json(name = "ma") val marketingAuthorizationHolder: String,
	@Json(name = "dn") val doseNumber: Int,
	@Json(name = "sd") val totalDoses: Int,
	@Json(name = "dt") val vaccinationDate: String,
	@Json(name = "co") val country: String,
	@Json(name = "is") val certificateIssuer: String,
	@Json(name = "ci") val certificateIdentifier: String,
) : Serializable
