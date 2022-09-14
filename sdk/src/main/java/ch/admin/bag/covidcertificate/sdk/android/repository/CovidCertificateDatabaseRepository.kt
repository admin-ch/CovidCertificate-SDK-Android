package ch.admin.bag.covidcertificate.sdk.android.repository

import android.content.Context
import androidx.room.Room
import ch.admin.bag.covidcertificate.sdk.android.data.NationalRulesStore
import ch.admin.bag.covidcertificate.sdk.android.data.room.CovidCertificateDatabase
import ch.admin.bag.covidcertificate.sdk.android.data.room.NationalRulesEntity
import ch.admin.bag.covidcertificate.sdk.android.data.room.RevokedCertificateEntity
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificatesStore
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import com.squareup.moshi.Moshi

internal class CovidCertificateDatabaseRepository private constructor(context: Context) : RevokedCertificatesStore,
	NationalRulesStore {

	companion object : SingletonHolder<CovidCertificateDatabaseRepository, Context>(::CovidCertificateDatabaseRepository) {
		private const val ASSET_DB_PATH = "covid_certificate_database.sqlite"
		private const val LEGACY_ASSET_DB_PATH = "revoked-certificates-db"
	}

	private val database by lazy {
		val path = context.getDatabasePath(LEGACY_ASSET_DB_PATH)
		if (path.exists()) {
			context.deleteDatabase(LEGACY_ASSET_DB_PATH)
		}
		// Legacy database file name because the DB initially only contained revoked certificates
		Room.databaseBuilder(context, CovidCertificateDatabase::class.java, "certificates-db").fallbackToDestructiveMigration()
			.createFromAsset(ASSET_DB_PATH).build()
	}

	private val metadataDao by lazy { database.metadata() }
	private val revokedCertificatesDao by lazy { database.revokedCertificatesDao() }
	private val nationalRulesDao by lazy { database.nationalRulesDao() }

	private val ruleSetAdapter by lazy {
		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		moshi.adapter(RuleSet::class.java)
	}

	override fun addCertificates(certificates: List<String>) =
		revokedCertificatesDao.insertOrReplace(certificates.map { RevokedCertificateEntity(it) })

	override fun getPrepopulatedSinceHeader(isProd: Boolean): String {
		return if (isProd) metadataDao.getMetadata()?.nextSince ?: "0" else "0"
	}

	override fun containsCertificate(certificate: String) = revokedCertificatesDao.containsCertificate(certificate)

	override suspend fun getValidUntilForCountry(countryCode: String): Long? {
		return nationalRulesDao.getNationalRulesForCountry(countryCode)?.validUntil
	}

	override suspend fun getRuleSetForCountry(countryCode: String): RuleSet? {
		val rulesString = nationalRulesDao.getNationalRulesForCountry(countryCode)?.rules
		return rulesString?.let { ruleSetAdapter.fromJson(it) }
	}

	override suspend fun addRuleSetForCountry(countryCode: String, validUntil: Long, ruleSet: RuleSet) {
		val ruleSetString = ruleSetAdapter.toJson(ruleSet)
		val entity = NationalRulesEntity(countryCode, validUntil, ruleSetString)
		nationalRulesDao.insertOrReplace(entity)
	}
}