/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.extensions

import android.util.Base64

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP).trim()
fun ByteArray.toBase64NoPadding(): String = Base64.encodeToString(this, Base64.NO_PADDING).trim()
fun String.toBase64NoPadding(): String = Base64.encodeToString(this.toByteArray(), Base64.NO_PADDING).trim()
fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
fun String.fromBase64NoPadding(): ByteArray = Base64.decode(this, Base64.NO_PADDING)

// NB: toString() does not work!
fun String.fromBase64ToString(): String = String(bytes = this.fromBase64())
fun String.fromBase64NoPaddingToString(): String = String(bytes = this.fromBase64NoPadding())