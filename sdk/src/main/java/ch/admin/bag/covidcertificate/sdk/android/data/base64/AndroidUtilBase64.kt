/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.data.base64

import android.util.Base64
import ch.admin.bag.covidcertificate.sdk.core.data.base64.Base64Provider

class AndroidUtilBase64 : Base64Provider {
	override fun decode(src: String): ByteArray = Base64.decode(src, Base64.NO_WRAP)

	override fun encode(src: ByteArray): String = Base64.encodeToString(src, Base64.NO_WRAP)
}