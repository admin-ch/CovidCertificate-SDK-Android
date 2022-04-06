package ch.admin.bag.covidcertificate.sdk.android.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForeignRulesCountryCodes(
	val countries: List<String>,
	val validDuration: Long,
)
