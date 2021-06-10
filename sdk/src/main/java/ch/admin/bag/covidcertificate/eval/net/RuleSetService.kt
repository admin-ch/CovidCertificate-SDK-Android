/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.net

import ch.admin.bag.covidcertificate.eval.models.RuleSet
import retrofit2.Response
import retrofit2.http.GET

interface RuleSetService {

	@GET("verificationRules.json")
	suspend fun getRuleset() : Response<RuleSet>

}