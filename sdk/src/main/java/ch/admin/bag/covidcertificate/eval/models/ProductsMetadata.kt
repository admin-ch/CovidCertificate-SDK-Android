/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.models

import ch.admin.bag.covidcertificate.eval.products.ValueSet
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ProductsMetadata(
	val test: TestProductsMetadata,
	val vaccine: VaccineProductsMetadata
)

@JsonClass(generateAdapter = true)
internal data class TestProductsMetadata(
	val type: ValueSet,
	val manf: ValueSet
)

@JsonClass(generateAdapter = true)
internal data class VaccineProductsMetadata(
	val mahManf: ValueSet,
	val medicinalProduct: ValueSet,
	val prophylaxis: ValueSet
)