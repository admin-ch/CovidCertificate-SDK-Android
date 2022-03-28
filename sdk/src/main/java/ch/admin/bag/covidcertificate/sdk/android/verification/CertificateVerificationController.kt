/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.verification

import android.util.Log
import ch.admin.bag.covidcertificate.sdk.android.exceptions.ServerTimeOffsetException
import ch.admin.bag.covidcertificate.sdk.android.repository.TrustListRepository
import ch.admin.bag.covidcertificate.sdk.android.verification.task.CertificateVerificationTask
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.net.UnknownHostException
import java.util.*

internal class CertificateVerificationController internal constructor(
	private val trustListRepository: TrustListRepository,
	private val verifier: CertificateVerifier
) {

	companion object {
		private val TAG = CertificateVerificationController::class.java.simpleName
	}

	private val taskQueue: Queue<CertificateVerificationTask> = LinkedList()
	private var taskProcessingJob: Job? = null

	private val trustListLoadingJobs = mutableMapOf<String, Job>()

	/**
	 * Trigger a refresh of the trust list unless there is already a refresh running
	 */
	fun refreshTrustList(coroutineScope: CoroutineScope, onCompletionCallback: () -> Unit = {}, onErrorCallback: (String) -> Unit = {}) {
		// The publicly exposed method to force refresh the trust list should always refresh the CH trust list
		val countryCode = TrustListRepository.COUNTRY_CODE_SWITZERLAND
		val job = trustListLoadingJobs[countryCode]
		if (job == null || job.isCompleted) {
			trustListLoadingJobs[countryCode] = coroutineScope.launch {
				try {
					trustListRepository.refreshTrustList(forceRefresh = true)
					trustListLoadingJobs.remove(countryCode)
					onCompletionCallback.invoke()
				} catch (e: Exception) {
					Log.e(TAG, "Manually refreshing trust list failed", e)

					// Loading trust list failed, map the exception to an error code and invoke the error callback
					when (e) {
						is UnknownHostException -> onErrorCallback.invoke(ErrorCodes.GENERAL_OFFLINE)
						is ServerTimeOffsetException -> onErrorCallback.invoke(ErrorCodes.TIME_INCONSISTENCY)
						is HttpException -> onErrorCallback.invoke("${ErrorCodes.GENERAL_NETWORK_FAILURE}-${e.code()}")
						else -> onErrorCallback.invoke(ErrorCodes.GENERAL_NETWORK_FAILURE)
					}
				}
			}
		}
	}

	/**
	 * Add a certificate verification task to the queue with the provided coroutine scope. This will either immediately execute the
	 * task if the queue is empty or queue the task for later execution. The trust list is refreshed if necessary every time before
	 * a task is executed.
	 */
	fun enqueue(task: CertificateVerificationTask, coroutineScope: CoroutineScope) {
		val job = taskProcessingJob
		if (job == null || job.isCompleted) {
			taskProcessingJob = coroutineScope.launch {
				processTask(task)
			}
		} else {
			taskQueue.add(task)
		}
	}

	private suspend fun processTask(task: CertificateVerificationTask): Unit = withContext(Dispatchers.IO) {
		val countryCode = task.countryCode ?: TrustListRepository.COUNTRY_CODE_SWITZERLAND

		// Launch a new trust list loading job for the given country code if it is not yet running
		val trustListLoadingJob = trustListLoadingJobs[countryCode]
		if (trustListLoadingJob == null || trustListLoadingJob.isCompleted) {
			trustListLoadingJobs[countryCode] = launch {
				try {
					trustListRepository.refreshTrustList(countryCode, forceRefresh = false)
					trustListLoadingJobs.remove(countryCode)
				} catch (e: Exception) {
					// Loading trust list failed, keep using last stored version
					Log.e(TAG, "Refreshing trust list for [$countryCode] as part of certificate verification task failed", e)
				}
			}
		}

		// Suspend until the existing or new job is completed
		trustListLoadingJobs[countryCode]?.join()

		// Get the current trust list. If this returns null, it means that some or all of the content is either missing or outdated.
		// In that case, the task will fail with a corresponding error code
		val trustList = trustListRepository.getTrustList(countryCode)
		task.execute(verifier, trustList)

		// When the current task is finished, check the next task in the queue for execution
		val nextTask = taskQueue.poll()
		taskProcessingJob = nextTask?.let {
			launch {
				processTask(it)
			}
		}
	}

}