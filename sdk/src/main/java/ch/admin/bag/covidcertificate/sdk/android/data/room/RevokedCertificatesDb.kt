package ch.admin.bag.covidcertificate.sdk.android.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificatesStore

class RevokedCertificatesDb private constructor(context: Context) : RevokedCertificatesStore {

	companion object : SingletonHolder<RevokedCertificatesDb, Context>(::RevokedCertificatesDb)

	private val revokedCertificatesDao by lazy {
		Room.databaseBuilder(context, RevokedCertificatesDatabase::class.java, "revoked-certificates-db")
			.build().revokedCertificatesDao()
	}

	override fun addCertificates(certificates: List<String>) =
		revokedCertificatesDao.insertOrReplace(certificates.map { RevokedCertificateEntity(it) })

	override fun containsCertificate(certificate: String) = revokedCertificatesDao.containsCertificate(certificate)

}


@Database(entities = [RevokedCertificateEntity::class], version = 1)
abstract class RevokedCertificatesDatabase : RoomDatabase() {
	abstract fun revokedCertificatesDao(): RevokedCertificatesDao
}

