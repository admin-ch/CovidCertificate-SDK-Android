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

import ch.admin.bag.covidcertificate.eval.models.Jwk
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig


internal fun getHardcodedSigningKeys(flavor: String = BuildConfig.FLAVOR): List<Jwk> {
	val jwks = mutableListOf<Jwk>()
	when (flavor) {
		"dev" -> {
			jwks.add(Jwk.fromNE(CH_DEV_KID, CH_DEV_N, CH_DEV_E, use = ""))
		}
		"abn" -> {
			jwks.add(Jwk.fromNE(CH_ABN_KID, CH_ABN_N, CH_ABN_E, use = ""))
		}
		else -> {
			jwks.add(Jwk.fromNE(CH_PROD_KID, CH_PROD_N, CH_PROD_E, use = ""))
		}
	}
	return jwks
}

/* Switzerland's public keys */
private const val CH_DEV_KID = "mmrfzpMU6xc="
private const val CH_DEV_N = "AOLmTuP+7Z3md1w+TgIk8qADTqIUGQvg82eGAtAKC5xDvmdz3E4mpQrkSktcx37ozTyNBhhtPQ0VVV3b/rXCjVxQ7f50VNc5VgxhX+P+t5eUSI5FhQ9yRSqkfCJXCY62GMbLbmbNzGst0hkCfpGWnh+RhWTEbxNMGh6jMW38GpL43/KsgVwq2dVrCvlyX+4mGyUtnTtWuR53oMT7kQO2c/IpDu0Ec5kqJ4KjpZHoxGiJBY8e4Cxk1LDqwT2GubHWaopw8Jp47Soudhy1mqzF7PrdTDeHrSKexhO/82q4wTcZNRH4osJfkXXMCdrlcH64M8X79/03pGRfCFMpFdhnrt0="
private const val CH_DEV_E = "AQAB"

private const val CH_ABN_KID = "JLxre3vSwyg="
private const val CH_ABN_N = "ANG1XnHVRFARGgelLvFbV67VZzdBWvfoQHDtF3Iy4C1QwfPWOPobhjveGPd02ON8fXl0UVnDZXmnAUdDncw6QFDn3VG768NpzUm+ToYShvph27gWiJliqb4pmtAXitBondNSBvLvN0igTmm1N+FlJ+Zt+5j49GKJ6hTso58ghNcK52nhveZYdGQuVglAdgajSOGWUF8AwgguUk5Gt5dNmTQCBzKBy5oKgKlm110ua+NZbbpm0UWlRruO6UlEac8/8AmXqeh55oTbzhP0+ZTc5aJcYHJbSnO1WbXKGZyvSRZE+7ZOBkdh+JpwNZcQBzpCTmhJGcU+ja5ua/DrwNMm7jE="
private const val CH_ABN_E = "AQAB"

private const val CH_PROD_KID = "Ll3NP03zOxY="
private const val CH_PROD_N = "ALZP+dbLSV1OPEag9pYeCHbMRa45SX5kwqy693EDRF5KxKCNzhFfDZ6LRNUY1ZkK6i009OKMaVpXGzKJV7SQbbt6zoizcEL8lRG4/8UnOik/OE6exgaNT/5JLp2PlZmm+h1Alf6BmWJrHYlD/zp0z1+lsunXpQ4Z64ByA7Yu9/00rBu2ZdVepJu/iiJIwJFQhA5JFA+7n33eBvhgWdAfRdSjk9CHBUDbw5tM5UTlaBhZZj0vA1payx7iHTGwdvNbog43DfpDVLe61Mso+kxYF/VgoBAf+ZkATEWmlytc3g02jZJgtkuyFsYTELDAVycgHWw/QJ0DmXOl0YwWrju4M9M="
private const val CH_PROD_E = "AQAB"
