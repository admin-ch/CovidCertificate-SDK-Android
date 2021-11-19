package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RevokedCertificatesDao {

	@Query("SELECT * FROM revokedCertificates")
	fun getAllRevokedCertificates(): List<RevokedCertificateEntity>

	@Query("SELECT EXISTS (SELECT 1 FROM revokedCertificates WHERE certificate=:certificate)")
	fun containsCertificate(certificate: String): Boolean

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	fun insertOrReplace(certificates: List<RevokedCertificateEntity>)

}