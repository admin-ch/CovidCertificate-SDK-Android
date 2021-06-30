package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PrefixIdentifierServiceTest {

	@Test
	fun testHc1Prefix() {
		val expected = "Lorem Ipsum"
		val input = "HC1:$expected"
		val output = PrefixIdentifierService.decode(input)
		assertEquals(expected, output, "The prefix HC1: was not correctly identified")
	}

	@Test
	fun testHc1PrefixLowercase() {
		val input = "hc1:Lorem Ipsum"
		val output = PrefixIdentifierService.decode(input)
		assertNull(output, "The lowercase prefix hc1: must not be accepted")
	}

	@Test
	fun testLt1Prefix() {
		val expected = "Lorem Ipsum"
		val input = "LT1:$expected"
		val output = PrefixIdentifierService.decode(input)
		assertEquals(expected, output, "The prefix LT1: was not correctly identified")
	}

	@Test
	fun testLt1PrefixLowercase() {
		val input = "lt1:Lorem Ipsum"
		val output = PrefixIdentifierService.decode(input)
		assertNull(output, "The lowercase prefix lt1: must not be accepted")
	}

	@Test
	fun testUnknownPrefix() {
		val input = "DE1:Lorem Ipsum"
		val output = PrefixIdentifierService.decode(input)
		assertNull(output, "An unknown prefix must not be accepted")
	}

}