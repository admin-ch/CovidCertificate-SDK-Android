package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MetadataDao {

	@Query("SELECT * FROM metadata")
	fun getMetadata(): MetadataEntity?

}