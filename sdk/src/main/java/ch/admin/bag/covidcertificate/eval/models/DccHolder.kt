/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.models

import ch.admin.bag.covidcertificate.eval.models.healthcert.eu.Eudgc
import ch.admin.bag.covidcertificate.eval.models.healthcert.light.DccLight
import java.io.Serializable
import java.time.Instant
import java.util.*

data class DccHolder internal constructor(
	val qrCodeData: String,
	val euDGC: Eudgc? = null,
	val dccLight: DccLight? = null,
	val expirationTime: Instant? = null,
	val issuedAt: Instant? = null,
	val issuer: String? = null,
) : Serializable {

	var certType: CertType? = null
		internal set

	fun isFullCertificate() = euDGC != null

	fun isLightCertificate() = dccLight != null

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as DccHolder

		if (euDGC != other.euDGC) return false
		if (dccLight != other.dccLight) return false
		if (qrCodeData != other.qrCodeData) return false

		return true
	}

	override fun hashCode(): Int {
		var result = qrCodeData.hashCode()
		result = 31 * result + (euDGC?.hashCode() ?: 0)
		result = 31 * result + (dccLight?.hashCode() ?: 0)
		return result
	}

}
