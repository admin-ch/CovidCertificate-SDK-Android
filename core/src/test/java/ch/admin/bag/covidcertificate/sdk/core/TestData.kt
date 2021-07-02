/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core

import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwk

// Note that this String (taken directly from a QR code) has some \ that escape $
// This HC is signed by the hardcoded BAG DEV key
internal const val HC1_A = "HC1:NCFJ60EG0/3WUWGSLKH47GO0KNJ9DSWQIIWT9CK+500XKY-CE59-G80:84F3ZKG%QU2F30GK JEY50.FK6ZK7:EDOLOPCF8F746KG7+59.Q6+A80:6JM8SX8RM8.A8TL6IA7-Q6.Q6JM8WJCT3EYM8XJC +DXJCCWENF6OF63W5\$Q69L6%JC+QE$.32%E6VCHQEU\$DE44NXOBJE719\$QE0/D+8D-ED.24-G8$:8.JCBECB1A-:8$96646AL60A60S6Q\$D.UDRYA 96NF6L/5QW6307KQEPD09WEQDD+Q6TW6FA7C466KCN9E%961A6DL6FA7D46JPCT3E5JDJA76L68463W5/A6..DX%DZJC3/DH$9- NTVDWKEI3DK2D4XOXVD1/DLPCG/DU2D4ZA2T9GY8MPCG/DY-CAY81C9XY8O/EZKEZ96446256V50G7AZQ4CUBCD9-FV-.6+OJROVHIBEI3KMU/TLRYPM0FA9DCTID.GQ\$NYE3NPBP90/9IQH24YL7WMO0CNV1 SDB1AHX7:O26872.NV/LC+VJ75L%NGF7PT134ERGJ.I0 /49BB6JA7WKY:AL19PB120CUQ37XL1P9505-YEFJHVETB3CB-KE8EN9BPQIMPRTEW*DU+X2STCJ6O6S4XXVJ\$UQNJW6IIO0X20D4S3AWSTHTA5FF7I/J9:8ALF/VP 4K1+8QGI:N0H 91QBHPJLSMNSJC BFZC5YSD.9-9E5R8-.IXUB-OG1RRQR7JEH/5T852EA3T7P6 VPFADBFUN0ZD93MQY07/4OH1FKHL9P95LIG841 BM7EXDR/PLCUUE88+-IX:Q"
internal const val LT1_A = "LT1:6BFY90R10RDWT 9O60GO0000W50JB06H08CK%QC/70YM8N34GB8FN04BC6S5WY01BC9HH597MTKGVC*JC1A6/Q63W5KF6746TPCBEC7ZKW.CU2DNXO VD5\$C JC3/DMP8\$ILZEDZ CW.C9WE.Y9AY8+S9VIAI3D8WEVM8:S9C+9\$PC5\$CUZCY$5Y$527BK/CV3VEAFC48\$CS/M8WBD543I 2QRK\$G6RXQT-T74F\$SCMWJ+*VADUJR1T46 /Q+38HH61HVL-U78GRAKUIOIVTWXG5%JL%Q1SPOF9"

internal fun getInvalidSigningKeys(): List<Jwk>{
	val n = "4uZO4_7tneZ3XD5OAiTyoANOohQZC-DzZ4YC0AoLnEO-Z3PcTialCuRKS1zHfujNPI0GGG09DRVVXdv-tcKNXFDt_nRU1zlWDGFf4_63l5RIjkWFD3JFKqR8IlcJjrYYxstuZs3May3SGQJ-kZaeH5GFZMRvE0waHqMxbfwakvjf8qyBXCrZ1WsK-XJf7iYbJS2dO1a5HnegxPuRA7Zz8ikO7QRzmSongqOlkejEaIkFjx7gLGTUsOrBPYa5sdZqinDwmnjtKi52HLWarMXs-t1MN4etIp7GE7_zarjBNxk1Efiiwl-RdcwJ2uVwfrgzxfv3_TekZF8IUykV2Geu3Q"
	val e = "AQAB"

	return listOf(
		Jwk.fromNE("", n, e, use = "")
	)
}

internal fun getCertificateLightTestKey() = Jwk.fromXY(LIGHT_TEST_KID, LIGHT_TEST_X, LIGHT_TEST_Y, use = "")

internal fun getHardcodedSigningKeys(flavor: String): List<Jwk> {
	val jwks = mutableListOf<Jwk>()
	when (flavor) {
		"dev" -> {
			jwks.add(Jwk.fromNE(CH_DEV_KID, CH_DEV_N, CH_DEV_E, use = ""))

			jwks.add(Jwk.fromXY(LI_DEV_ABN_VACCINATION_KID, LI_DEV_ABN_VACCINATION_X, LI_DEV_ABN_VACCINATION_Y, use = "v"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_TEST_KID, LI_DEV_ABN_TEST_X, LI_DEV_ABN_TEST_Y, use = "t"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_RECOVERY_KID, LI_DEV_ABN_RECOVERY_X, LI_DEV_ABN_RECOVERY_Y, use = "r"))
		}
		"abn" -> {
			jwks.add(Jwk.fromNE(CH_ABN_KID, CH_ABN_N, CH_ABN_E, use = ""))

			jwks.add(Jwk.fromXY(LI_DEV_ABN_VACCINATION_KID, LI_DEV_ABN_VACCINATION_X, LI_DEV_ABN_VACCINATION_Y, use = "v"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_TEST_KID, LI_DEV_ABN_TEST_X, LI_DEV_ABN_TEST_Y, use = "t"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_RECOVERY_KID, LI_DEV_ABN_RECOVERY_X, LI_DEV_ABN_RECOVERY_Y, use = "r"))
		}
		else -> {
			jwks.add(Jwk.fromNE(CH_PROD_KID, CH_PROD_N, CH_PROD_E, use = ""))
		}
	}
	return jwks
}

/* Certificate Light Test Key */
private const val LIGHT_TEST_KID = "AAABAQICAwM="
private const val LIGHT_TEST_X = "ceBrQgj3RwWzoxkv8/vApqkB7yJGfpBC9TjeIiXUR0U="
private const val LIGHT_TEST_Y = "g9ufnhfjFLVIiQYeQWmQATN/CMiVbfAgFp/08+Qqv2s="

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

/* Liechtenstein's public keys */
private const val LI_DEV_ABN_VACCINATION_KID = "pXjP4Y6sns4="
private const val LI_DEV_ABN_VACCINATION_X = "iO9c7u35s7GF1I6gTyy7W3l4WkEil7N6s/Zbs613fvo="
private const val LI_DEV_ABN_VACCINATION_Y = "ITx2eL6yzmysHC2jVab+YVoxiwKyZ9X3vAn56zyxCTU="

private const val LI_DEV_ABN_TEST_KID = "7/MOPvQI+WY="
private const val LI_DEV_ABN_TEST_X = "IjUSzW6EsS4U+yWuH9asbOHSH+KUeAVHcQ0xCIMOY5E="
private const val LI_DEV_ABN_TEST_Y = "37bBS8tAw4WoQQrehHpj/bjMbuDL4piC/loUgMaA8zY="

private const val LI_DEV_ABN_RECOVERY_KID = "dAacIEGMNcE="
private const val LI_DEV_ABN_RECOVERY_X = "dRONZMFNTpyg8cRP8uVmscHjdfKotSCTIfnZQb9NWuA="
private const val LI_DEV_ABN_RECOVERY_Y = "T8mYbbPQoU9PFssKpqiENWd7sZl4EMwH9hUVkr/bcyE="