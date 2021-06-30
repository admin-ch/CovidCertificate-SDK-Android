package ch.admin.bag.covidcertificate.sdk.core.models.state

sealed class CheckRevocationState {
	object SUCCESS : CheckRevocationState()
	data class INVALID(val revocationErrorCode: String) : CheckRevocationState()
	object LOADING : CheckRevocationState()
	data class ERROR(val error: StateError) : CheckRevocationState()
}