package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Entity

@Entity(tableName = "metadata", primaryKeys = ["nextSince", "validDuration", "lastDownload"])
data class MetadataEntity(
	val nextSince: String,
	val validDuration: Int,
	val lastDownload: Int,
)

