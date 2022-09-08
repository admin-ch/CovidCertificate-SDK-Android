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

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
	entities = [MetadataEntity::class, RevokedCertificateEntity::class, NationalRulesEntity::class],
	version = 3,//TODO get gradle.properties field as BuildConfig
)
internal abstract class CovidCertificateDatabase : RoomDatabase() {
	abstract fun metadata(): MetadataDao
	abstract fun revokedCertificatesDao(): RevokedCertificatesDao
	abstract fun nationalRulesDao(): NationalRulesDao
}

