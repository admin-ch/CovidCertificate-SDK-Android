package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nationalRules")
data class NationalRulesEntity(
	@PrimaryKey val countryCode: String,
	val validUntil: Long,
	val rules: String,
	val lastModified: Long = System.currentTimeMillis(),
)