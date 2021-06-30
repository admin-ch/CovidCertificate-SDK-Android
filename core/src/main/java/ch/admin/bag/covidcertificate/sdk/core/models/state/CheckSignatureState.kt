package ch.admin.bag.covidcertificate.sdk.core.models.state

sealed class CheckSignatureState {
	object SUCCESS : CheckSignatureState()
	data class INVALID(val signatureErrorCode: String) : CheckSignatureState()
	object LOADING : CheckSignatureState()
	data class ERROR(val error: StateError) : CheckSignatureState()
}