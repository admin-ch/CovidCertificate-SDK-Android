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
 * Adapted from https://github.com/ehn-dcc-development/dgc-business-rules/tree/main/certlogic/certlogic-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic

enum class TimeUnit(val identifier: String) {
	DAY(identifier = "day"),
	HOUR(identifier = "hour");

	companion object {
		fun fromName(identifier: String): TimeUnit {
			return values().find {
				it.identifier == identifier
			} ?: throw IllegalArgumentException("No TimeUnit enum constant with identifier $identifier")
		}
	}
}

/**
 * Returns `true` if enum T contains an entry with the specified name.
 */
inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
	return enumValues<T>().any { it.name == name }
}