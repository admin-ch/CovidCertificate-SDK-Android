/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-dcc-development/dgc-business-rules/tree/main/certlogic/certlogic-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.nationalrules.certlogic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

internal fun isFalsy(value: JsonNode): Boolean = when (value) {
	is BooleanNode -> value == BooleanNode.FALSE
	is NullNode -> true
	else -> false
}

internal fun isTruthy(value: JsonNode): Boolean = when (value) {
	is BooleanNode -> value == BooleanNode.TRUE
	is ArrayNode -> value.size() > 0
	is ObjectNode -> true
	is TextNode -> true
	else -> false
}


internal fun evaluateVar(args: JsonNode, data: JsonNode): JsonNode {
	if (data is NullNode) {
		return NullNode.instance
	}
	if (args !is TextNode) {
		throw RuntimeException("not of the form { \"var\": \"<path>\" }")
	}
	val path = args.asText()
	if (path == "") {  // "it"
		return data
	}
	return path.split(".").fold(data) { acc, fragment ->
		if (acc is NullNode) {
			acc
		} else {
			try {
				val index = Integer.parseInt(fragment, 10)
				if (acc is ArrayNode) acc[index] else null
			} catch (e: NumberFormatException) {
				if (acc is ObjectNode) acc[fragment] else null
			} ?: NullNode.instance
		}
	}
}


internal fun evaluateIf(guard: JsonNode, then: JsonNode, else_: JsonNode, data: JsonNode): JsonNode {
	val evalGuard = evaluate(guard, data)
	if (isTruthy(evalGuard)) {
		return evaluate(then, data)
	}
	if (isFalsy(evalGuard)) {
		return evaluate(else_, data)
	}
	throw RuntimeException("if-guard evaluates to something neither truthy, nor falsy: $evalGuard")
}


internal fun intCompare(operator: String, l: Int, r: Int): Boolean =
	when (operator) {
		"<" -> l < r
		">" -> l > r
		"<=" -> l <= r
		">=" -> l >= r
		else -> throw RuntimeException("unhandled binary comparison operator \"$operator\"")
	}

internal fun <T : Comparable<T>> compare(operator: String, args: List<T>): Boolean =
	when (args.size) {
		2 -> intCompare(operator, args[0].compareTo(args[1]), 0)
		3 -> intCompare(operator, args[0].compareTo(args[1]), 0) && intCompare(operator, args[1].compareTo(args[2]), 0)
		else -> throw RuntimeException("invalid number of operands to a \"$operator\" operation")
	}

internal fun evaluateBinOp(operator: String, args: ArrayNode, data: JsonNode): JsonNode {
	when (operator) {
		"and" -> if (args.size() < 2) throw RuntimeException("an \"and\" operation must have at least 2 operands")
		"<", ">", "<=", ">=" -> if (args.size() < 2 || args.size() > 3) throw RuntimeException("an operation with operator \"$operator\" must have 2 or 3 operands")
		else -> if (args.size() != 2) throw RuntimeException("an operation with operator \"$operator\" must have 2 operands")
	}
	val evalArgs = args.map { arg -> evaluate(arg, data) }
	return when (operator) {
		"===" -> BooleanNode.valueOf(evalArgs[0] == evalArgs[1])
		"in" -> {
			val r = evalArgs[1]
			if (r !is ArrayNode) {
				throw RuntimeException("right-hand side of an \"in\" operation must be an array")
			}
			BooleanNode.valueOf(r.contains(evalArgs[0]))
		}
		"+" -> {
			val l = evalArgs[0]
			val r = evalArgs[1]
			if (l !is IntNode || r !is IntNode) {
				throw RuntimeException("operands of a " + " operator must both be integers")
			}
			IntNode.valueOf(evalArgs[0].intValue() + evalArgs[1].intValue())
		}
		"and" -> args.fold(BooleanNode.TRUE as JsonNode) { acc, current ->
			if (isFalsy(acc)) acc else evaluate(current, data)
		}
		"<", ">", "<=", ">=" -> {
			BooleanNode.valueOf(
				when (evalArgs[0]) {
					is IntNode -> {
						if (evalArgs.any { it !is IntNode }) {
							throw RuntimeException("all operands must have the same type")
						}
						compare(operator, evalArgs.map { (it as IntNode).intValue() })
					}
					is JsonDateTime -> {
						if (evalArgs.any { it !is JsonDateTime }) {
							throw RuntimeException("all operands must have the same type")
						}
						compare(operator, evalArgs.map { (it as JsonDateTime).temporalValue() })
					}
					else -> throw RuntimeException("can't handle the following type for the operands to a \"$operator\" operation: ${evalArgs[0].javaClass}")
				}
			)
		}
		else -> throw RuntimeException("unhandled binary operator \"$operator\"")
	}
}


internal fun evaluateNot(operandExpr: JsonNode, data: JsonNode): JsonNode {
	val operand = evaluate(operandExpr, data)
	if (isFalsy(operand)) {
		return BooleanNode.TRUE
	}
	if (isTruthy(operand)) {
		return BooleanNode.FALSE
	}
	throw RuntimeException("operand of ! evaluates to something neither truthy, nor falsy: $operand")
}


private fun isTimeUnit(unit: JsonNode): Boolean {
	if (unit !is TextNode) return false
	return try {
		val timeUnit = enumContains<TimeUnit>(unit.textValue())
		true
	} catch (iae: IllegalArgumentException) {
		false
	}
}

internal fun evaluatePlusTime(dateOperand: JsonNode, amount: JsonNode, unit: JsonNode, data: JsonNode): JsonDateTime {
	var numericAmountNode = amount
	if (amount is ObjectNode) {
		numericAmountNode = evaluate(amount, data)
	}

	val longAmount = if (numericAmountNode.isNumber) {
		numericAmountNode.longValue()
	} else {
		throw RuntimeException("\"amount\" argument (#2) of \"plusTime\" must be a number")
	}

	if (!isTimeUnit(unit)) {
		throw RuntimeException("\"unit\" argument (#3) of \"plusTime\" must be a string 'day' or 'hour'")
	}
	val timeUnit = TimeUnit.fromName(unit.textValue())
	val dateTimeStr = evaluate(dateOperand, data)
	if (dateTimeStr !is TextNode) {
		throw RuntimeException("date argument of \"plusTime\" must be a string")
	}
	return JsonDateTime.fromIso8601(dateTimeStr.asText()).plusTime(longAmount, timeUnit)
}


internal fun evaluateReduce(operand: JsonNode, lambda: JsonNode, initial: JsonNode, data: JsonNode): JsonNode {
	val evalOperand = evaluate(operand, data)
	val evalInitial = { evaluate(initial, data) }
	if (evalOperand == NullNode.instance) {
		return evalInitial()
	}
	if (evalOperand !is ArrayNode) {
		throw RuntimeException("operand of reduce evaluated to a non-null non-array")
	}
	return evalOperand.fold(evalInitial()) { accumulator, current ->
		evaluate(
			lambda,
			JsonNodeFactory.instance.objectNode().apply {
				set<ObjectNode>("accumulator", accumulator)
				set<ObjectNode>("current", current)
			}
		)
	}
}


fun evaluate(expr: JsonNode, data: JsonNode): JsonNode = when (expr) {
	is TextNode -> expr
	is IntNode -> expr
	is BooleanNode -> expr
	is NullNode -> expr
	is ArrayNode -> JsonNodeFactory.instance.arrayNode().addAll(expr.map { evaluate(it, data) })
	is ObjectNode -> {
		if (expr.size() != 1) {
			throw RuntimeException("unrecognised expression object encountered")
		}
		val (operator, args) = expr.fields().next()
		if (operator == "var") {
			evaluateVar(args, data)
		} else {
			if (!(args is ArrayNode && args.size() > 0)) {
				throw RuntimeException("operation not of the form { \"<operator>\": [ <args...> ] }")
			}
			when (operator) {
				"if" -> evaluateIf(args[0], args[1], args[2], data)
				"===", "and", ">", "<", ">=", "<=", "in", "+" -> evaluateBinOp(operator, args, data)
				"!" -> evaluateNot(args[0], data)
				"plusTime" -> evaluatePlusTime(args[0], args[1], args[2], data)
				"reduce" -> evaluateReduce(args[0], args[1], args[2], data)
				else -> throw RuntimeException("unrecognised operator: \"$operator\"")
			}
		}
	}
	else -> throw RuntimeException("invalid CertLogic expression: $expr")
}

