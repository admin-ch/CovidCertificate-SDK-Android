package ch.admin.bag.covidcertificate.sdk.core.models.state

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.DccHolder

sealed class DecodeState {
	data class SUCCESS(val dccHolder: DccHolder) : DecodeState()
	data class ERROR(val error: StateError) : DecodeState()
}