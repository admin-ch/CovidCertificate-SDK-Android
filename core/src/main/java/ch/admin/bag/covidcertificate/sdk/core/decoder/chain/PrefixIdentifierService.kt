/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

internal object PrefixIdentifierService {

	private const val PREFIX_FULL = "HC1:"
	private const val PREFIX_LIGHT = "LT1:"

	fun decode(input: String): String? = when {
		// Spec: https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf#page=7
		// "the base45 encoded data [...] SHALL be prefixed by the Context  Identifier string "HC1:""
		// => when the prefix is missing, the data is invalid
		input.startsWith(PREFIX_FULL) -> input.drop(PREFIX_FULL.length)
		input.startsWith(PREFIX_LIGHT) -> input.drop(PREFIX_LIGHT.length)
		else -> null
	}

}