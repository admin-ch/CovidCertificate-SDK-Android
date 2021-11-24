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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.admin.bag.covidcertificate.sdk.android.data.PrepopulatedRevokedCertificatesDbConfig
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificatesStore

class RevokedCertificatesDb private constructor(context: Context) : RevokedCertificatesStore {

	companion object : SingletonHolder<RevokedCertificatesDb, Context>(::RevokedCertificatesDb)

	private val revokedCertificatesDao by lazy {
		Room.databaseBuilder(context, RevokedCertificatesDatabase::class.java, "revoked-certificates-db")
			.createFromAsset(PrepopulatedRevokedCertificatesDbConfig.prepopulatedRevokedCertificatesDbPath)
			.build()
			.revokedCertificatesDao()
	}

	override fun addCertificates(certificates: List<String>) =
		revokedCertificatesDao.insertOrReplace(certificates.map { RevokedCertificateEntity(it) })

	override fun containsCertificate(certificate: String) = revokedCertificatesDao.containsCertificate(certificate)

}


@Database(entities = [RevokedCertificateEntity::class], version = 1)
abstract class RevokedCertificatesDatabase : RoomDatabase() {
	abstract fun revokedCertificatesDao(): RevokedCertificatesDao
}

