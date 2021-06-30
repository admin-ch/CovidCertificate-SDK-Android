/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.trustlist

import com.squareup.moshi.JsonClass

/**
 * Unique Vaccination Certificate Identifier UVCI
 *
 * See https://ec.europa.eu/health/sites/default/files/ehealth/docs/vaccination-proof_interoperability-guidelines_en.pdf#page=11
 */
@JsonClass(generateAdapter = true)
data class RevokedCertificates(
	val revokedCerts: List<String> = emptyList(),
	val validDuration: Long
)