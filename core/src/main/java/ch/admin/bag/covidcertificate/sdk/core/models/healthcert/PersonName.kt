/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.healthcert

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class PersonName(
	@Json(name = "fn") val familyName: String?,
	@Json(name = "fnt") val standardizedFamilyName: String,
	@Json(name = "gn") val givenName: String?,
	@Json(name = "gnt") val standardizedGivenName: String?,
) : Serializable