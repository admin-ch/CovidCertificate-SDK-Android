package ch.admin.bag.covidcertificate.sdk.core.models.state

sealed class TrustListState {
	object SUCCESS : TrustListState()
	data class ERROR(val error: StateError) : TrustListState()
}