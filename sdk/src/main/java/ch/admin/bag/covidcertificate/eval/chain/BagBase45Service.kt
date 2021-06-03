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
package ch.admin.bag.covidcertificate.eval.chain

import ch.admin.bag.covidcertificate.eval.utils.Base45

internal object BagBase45Service {

    fun decode(input: String): ByteArray? {
        // Spec: https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf#page=7
        // "The Alphanumeric Mode [...] MUST be used in conjunction with Base45"
        // => data that is not compressed is invalid
        return try {
            Base45.getDecoder().decode(input)
        } catch (e: Throwable) {
            null
        }
    }

}