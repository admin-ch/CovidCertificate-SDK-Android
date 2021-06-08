package ch.admin.bag.covidcertificate.eval.data

import ch.admin.bag.covidcertificate.eval.net.UserAgentInterceptor

object Config {
	var userAgent: UserAgentInterceptor.UserAgentGenerator = UserAgentInterceptor.UserAgentGenerator { "covid-cert-sdk" }
}