/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.android.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RevokedCertificatesDao {

	@Query("SELECT * FROM revocations")
	fun getAllRevokedCertificates(): List<RevokedCertificateEntity>

	@Query("SELECT EXISTS (SELECT 1 FROM revocations WHERE uvci=:uvci)")
	fun containsCertificate(uvci: String): Boolean

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	fun insertOrReplace(certificates: List<RevokedCertificateEntity>)

}