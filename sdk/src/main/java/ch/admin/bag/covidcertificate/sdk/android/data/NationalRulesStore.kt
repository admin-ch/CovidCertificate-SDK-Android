package ch.admin.bag.covidcertificate.sdk.android.data

import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet

internal interface NationalRulesStore {
	suspend fun getValidUntilForCountry(countryCode: String): Long?
	suspend fun getRuleSetForCountry(countryCode: String): RuleSet?
	suspend fun addRuleSetForCountry(countryCode: String, validUntil: Long, ruleSet: RuleSet)
}