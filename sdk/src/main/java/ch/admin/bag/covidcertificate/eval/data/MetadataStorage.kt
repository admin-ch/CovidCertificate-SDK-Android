package ch.admin.bag.covidcertificate.eval.data

import android.content.Context
import ch.admin.bag.covidcertificate.eval.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.eval.models.ProductsMetadata
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import com.squareup.moshi.Moshi
import okio.IOException
import okio.buffer
import okio.source

internal class MetadataStorage private constructor(private val context: Context) {

	companion object : SingletonHolder<MetadataStorage, Context>(::MetadataStorage) {
		private const val FILE_PATH_PRODUCTS_METADATA = "products_metadata.json"
		private const val ASSET_PATH_FALLBACK_PRODUCTS_METADATA = "products_metadata.json"

		private val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		private val metadataAdapter = moshi.adapter(ProductsMetadata::class.java)
	}

	private val metadataFileStorage = FileStorage(FILE_PATH_PRODUCTS_METADATA)

	var productsMetadata: ProductsMetadata = loadProductsMetadata()
		set(value) {
			metadataFileStorage.write(context, metadataAdapter.toJson(productsMetadata))
			field = value
		}

	private fun loadProductsMetadata(): ProductsMetadata =
		metadataFileStorage.read(context)?.let { metadataAdapter.fromJson(it) }
			?: metadataAdapter.fromJson(context.assets.open(ASSET_PATH_FALLBACK_PRODUCTS_METADATA).source().buffer())
			?: throw IOException()
}