/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.repository

import ch.admin.bag.covidcertificate.sdk.android.data.MetadataStorage
import ch.admin.bag.covidcertificate.sdk.android.net.service.MetadataService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MetadataRepository(private val metadataService: MetadataService, private val store: MetadataStorage) {
	suspend fun refreshMetadata() = withContext(Dispatchers.IO) {
		val metadataResponse = metadataService.getMetadata()
		val metadataBody = metadataResponse.body()
		if (metadataResponse.isSuccessful && metadataBody != null) {
			store.productsMetadata = metadataBody
		}
	}
}