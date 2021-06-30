/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.metadata

import android.util.Log
import ch.admin.bag.covidcertificate.eval.repository.MetadataRepository
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerificationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ProductMetadataController internal constructor(
	private val metadataRepository: MetadataRepository
) {

	companion object {
		private val TAG = CertificateVerificationController::class.java.simpleName
	}

	private var metadataLoadingJob: Job? = null

	/**
	 * Trigger a refresh of the product metadata unless there is already a refresh running
	 */
	fun refreshProductsMetadata(
		coroutineScope: CoroutineScope,
		onCompletionCallback: () -> Unit = {},
		onErrorCallback: () -> Unit = {}
	) {
		val job = metadataLoadingJob
		if (job == null || job.isCompleted) {
			metadataLoadingJob = coroutineScope.launch {
				try {
					metadataRepository.refreshMetadata()
					metadataLoadingJob = null
					onCompletionCallback.invoke()
				} catch (e: Exception) {
					// Loading product metadata failed, keep using last stored version
					Log.e(TAG, "Manually refreshing product metadata failed", e)
					onErrorCallback.invoke()
				}
			}
		}
	}

}