# CovidCertificate-SDK-Android

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/admin-ch/CovidCertificate-SDK-iOS/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/ch.admin.bag.covidcertificate/sdk-android.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22ch.admin.bag.covidcertificate%22%20AND%20a:%22sdk-android%22)

## Introduction

This is the implementation of
the [Electronic Health Certificates (HCERT)](https://github.com/ehn-digital-green-development/hcert-spec)
and [Swiss Certificate Light](https://www.bag.admin.ch/bag/en/home/krankheiten/ausbrueche-epidemien-pandemien/aktuelle-ausbrueche-epidemien/novel-cov/covid-zertifikat.html#-518758716)
specification used to verify the validity of COVID
Certificates [in Switzerland](https://github.com/admin-ch/CovidCertificate-App-Android).

It is partially based on [these](https://github.com/ehn-digital-green-development/hcert-kotlin)
[two](https://github.com/DIGGSweden/dgc-java) implementations.

## Contribution Guide

This project is truly open-source and we welcome any feedback on the code regarding both the implementation and security aspects.

Bugs or potential problems should be reported using Github issues. We welcome all pull requests that improve the quality of the
source code.

## Repositories

* Android App: [CovidCertificate-App-Android](https://github.com/admin-ch/CovidCertificate-App-Android)
* iOS App: [CovidCertificate-App-iOS](https://github.com/admin-ch/CovidCertificate-App-iOS)
* iOS SDK: [CovidCertificate-SDK-iOS](https://github.com/admin-ch/CovidCertificate-SDK-iOS)
* Verifier Service: [CovidCertificate-App-Verifier-Service](https://github.com/admin-ch/CovidCertificate-App-Verifier-Service)
* For all others, see the [Github organisation](https://github.com/admin-ch/)

## Installation

For a change log, check out the [releases](https://github.com/admin-ch/CovidCertificate-SDK-Android/releases) page.

The latest release is available on [Maven Central](https://search.maven.org/artifact/ch.admin.bag.covidcertificate/sdk-android/).
```groovy
implementation 'ch.admin.bag.covidcertificate:sdk-android:1.2.0'
```

## How It Works

The SDK provides the functionality of decoding a QR code into an electronic health certificate and verifying the validity of the
decoded certificate. It also takes care of loading and storing the latest trust list information that is required for verification.
The trust list is a data model that contains a list of trusted public signing keys, a list of revoked certificate identifiers and
the currently active national rules. This trust list is loaded from a backend service. Refer to the "Verifier Service" listed in
the [Repositories](#repositories) section for more information.

### Decoding

Decoding a QR code into a COVID certificate uses the following steps. For more information, refer to
the [EHN specification](https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf).

1. Check the prefix of the data. Only `HC1:` (EU Dcc Certificate) and `LT1:` (CH Certificate Light) are valid prefixes
2. Base45 decode the data <sup> [[1]](https://datatracker.ietf.org/doc/draft-faltstrom-base45/) </sup>
3. ZLIB decompress the data
4. COSE decode the data <sup> [[2]](https://github.com/cose-wg/COSE-JAVA) </sup>
5. CBOR decode the data and parse it into a `CertificateHolder` containing either a `DccCert` or a `ChLightCert`

### Verification

The verification process consists of three parts that need to be successful in order for a certificate to be considered valid.

1. The certificate signature is verified against a list of trusted public keys from issueing countries
2. The UVCI (unique vaccination certificate identifier) is compared to a list of revoked certificates to ensure the certificate has
   not been revoked
3. The certificate details are checked based on the Swiss national rules for certificate validity. (Is the number of vaccination
   doses sufficient, is the test recent enough, how long ago was the recovery?)

## Usage

Once the SDK is added as a dependency, it needs to be initialized with an app token, a user agent and the application context. This
is preferably within your main Android Application class. Please get in touch with the [BAG](mailto:Covid-Zertifikat@bag.admin.ch)
to get a token assigned.

```kotlin
Config.appToken = "YOUR-APP-TOKEN"
Config.userAgent = UserAgentInterceptor.UserAgentGenerator { "YOUR-USER-AGENT" }

CovidCertificateSdk.init(applicationContext, SdkEnvironment.PROD)
```

After initialization, the SDK can also be registered with the application lifecycle to automatically update the trust list once
every hour when the app is in the foreground. This is useful for verifier applications that tend to be opened for a long period of
time.

```kotlin
// In your main activity
fun onCreate() {
	CovidCertificateSdk.registerWithLifecycle(lifecycle)
}

fun onDestroy() {
	CovidCertificateSdk.unregisterWithLifecycle(lifecycle)
}
```

If, for some reason, you suspect the locally stored trust list to be outdated, you can manually force a refresh by calling

```kotlin
CovidCertificateSdk.refreshTrustList(lifecycleScope, onCompletionCallback, onErrorCallback)
```

CovidCertificateSDK offers a Verifier and Wallet namespace for decoding and verification. Methods in the Wallet namespace must only
be used by the official COVID Certificate App.

### Decoding

Decoding takes the QR code data as a string and returns a decoding state object containing the decoded certificate in the success
case or error information in the failure case.

```kotlin
val decodeState = CovidCertificateSdk.Verifier.decode(qrCodeData)
when (decodeState) {
	is VerifierDecodeState.SUCCESS -> {
		// decodeState.certificateHolder contains the decoded certificate, use this for verification
	}
	is VerifierDecodeStat.ERROR -> {
		// decodeState.error contains information about the nature of the error, e.g. wrong prefix, failed to base45 decode, etc.
	}
}
```

### Verification

Verifying a certificate takes three parameters:

* The certificate holder that contains the certificate to be verified (required)
* The coroutine scope within which to run the verification process (required)
* A boolean flag whether to ignore the local trust list or not. This is useful to force a network error instead of using the locally
  stored trust list, which might be outdated. (optional)

```kotlin
// In your ViewModel
val verificationStateFlow = CovidCertificateSdk.Verifier.verify(certificateHolder, viewModelScope)

viewModelScope.launch {
	verificationStateFlow.collect { verificationState ->
		when (verificationState) {
			is VerificationState.LOADING -> {
				// The verification process is still ongoing
			}
			is VerificationState.SUCCESS -> {
				// The certificate is valid. The state contains a flag if this is a Swiss light certificate or not
			}
			is VerificationState.INVALID -> {
				// The certificate is not valid. The state contains multiple additional fields indicating why the certificate is invalid.
				// There are separate states for signature validity, revocation status and national rules conformity.
			}
			is VerificationState.ERROR -> {
				// An unexpected error occurred. The state contains error information
			}
		}
	}
}
```

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file for details.

## References

[[1](https://github.com/ehn-digital-green-development/hcert-spec)] Health Certificate Specification