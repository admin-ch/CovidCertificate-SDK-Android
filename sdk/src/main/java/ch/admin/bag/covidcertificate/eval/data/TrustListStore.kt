/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.data

import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.RevokedCertificates
import ch.admin.bag.covidcertificate.eval.models.RuleSet

interface TrustListStore {

	var certificateSignaturesValidUntil: Long
	var certificateSignatures: Jwks?
	var certificatesSinceHeader: String?

	var revokedCertificatesValidUntil: Long
	var revokedCertificates: RevokedCertificates?

	var rulesetValidUntil: Long
	var ruleset: RuleSet?

	fun areSignaturesValid(): Boolean
	fun areRevokedCertificatesValid(): Boolean
	fun areRuleSetsValid(): Boolean

}