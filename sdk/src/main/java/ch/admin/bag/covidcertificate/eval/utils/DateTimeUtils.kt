/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


fun Instant.formatAsString(dateFormatter: DateTimeFormatter): String {
	return try {
		this.atZone(ZoneId.systemDefault()).format(dateFormatter)
	} catch (e: Throwable) {
		this.toString()
	}
}