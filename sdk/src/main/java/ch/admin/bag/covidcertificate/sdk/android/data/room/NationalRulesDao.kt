package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NationalRulesDao {

	@Query("SELECT * FROM nationalRules WHERE countryCode=:countryCode LIMIT 1")
	fun getNationalRulesForCountry(countryCode: String): NationalRulesEntity?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	fun insertOrReplace(nationalRules: NationalRulesEntity)

}