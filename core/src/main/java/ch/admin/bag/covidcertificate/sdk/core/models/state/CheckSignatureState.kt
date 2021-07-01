/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.state

sealed class CheckSignatureState {
	object SUCCESS : CheckSignatureState()
	data class INVALID(val signatureErrorCode: String) : CheckSignatureState()
	object LOADING : CheckSignatureState()
	data class ERROR(val error: StateError) : CheckSignatureState()
}