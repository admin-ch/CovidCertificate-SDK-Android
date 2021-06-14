/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.utils

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtil {

	fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
		val activeNetwork = connectivityManager.activeNetwork ?: return false
		val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
		return when {
			networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
			networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
			networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
			else -> false
		}
	}

}