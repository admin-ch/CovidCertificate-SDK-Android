package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "revokedCertificates")
data class RevokedCertificateEntity(
	@PrimaryKey val certificate: String
)