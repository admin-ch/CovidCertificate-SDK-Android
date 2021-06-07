/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.verification

import ch.admin.bag.covidcertificate.eval.repository.TrustListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class CertificateVerificationController internal constructor(
	private val trustListRepository: TrustListRepository,
	private val verifier: CertificateVerifier
) {

	private val taskQueue: Queue<CertificateVerificationTask> = LinkedList()
	private var trustListLoadingJob: Job? = null
	private var taskProcessingJob: Job? = null

	/**
	 * Trigger a refresh of the trust list unless there is already a refresh running
	 */
	fun refreshTrustList(coroutineScope: CoroutineScope) {
		val job = trustListLoadingJob
		if (job == null || job.isCompleted) {
			trustListLoadingJob = coroutineScope.launch {
				trustListRepository.refreshTrustList(forceRefresh = true)
				trustListLoadingJob = null
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
				processTask(task, coroutineScope)
			}
		} else {
			taskQueue.add(task)
		}
	}

	private suspend fun processTask(task: CertificateVerificationTask, coroutineScope: CoroutineScope) {
		// Launch a new trust list loading job if it is not yet running, then suspend until the existing or new job is completed
		if (trustListLoadingJob == null || trustListLoadingJob?.isCompleted == true) {
			trustListLoadingJob = coroutineScope.launch {
				trustListRepository.refreshTrustList(forceRefresh = false)
				trustListLoadingJob = null
			}
		}
		trustListLoadingJob?.join()

		// Get the trust list from the repository. If this returns null, it means any of the backend requests failed
		val trustList = trustListRepository.getTrustList()
		if (trustList == null) {
			taskQueue.add(task)
		} else {
			task.execute(verifier, trustList)
		}

		// When the current task is finished, check the next task in the queue for execution
		val nextTask = taskQueue.poll()
		taskProcessingJob = nextTask?.let {
			coroutineScope.launch {
				processTask(it, coroutineScope)
			}
		}
	}

}