/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.data

import android.content.Context
import ch.admin.bag.covidcertificate.sdk.android.models.ProductsMetadata
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
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

	private fun loadProductsMetadata(): ProductsMetadata {
		return try {
			// First try to read the products metadata from the file storage
			metadataFileStorage.read(context)?.let { metadataAdapter.fromJson(it) }
				?: throw IOException("Failed to read stored products metadata")
		} catch (e: Exception) {
			// If the stored products metadata could not be read (corrupt JSON or empty/non-existing file), fallback to the pre-packaged products metadata
			metadataAdapter.fromJson(context.assets.open(ASSET_PATH_FALLBACK_PRODUCTS_METADATA).source().buffer())
				?: throw IOException("Failed to read fallback products metadata")
		}
	}
}