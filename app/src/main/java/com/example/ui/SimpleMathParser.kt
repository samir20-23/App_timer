package com.example.ui

import java.math.RoundingMode
import java.text.DecimalFormat

object SimpleMathParser {
    fun eval(expression: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw RuntimeException("Unexpected trailing character: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val sStr = expression.substring(startPos, this.pos)
                    x = sStr.toDoubleOrNull() ?: throw RuntimeException("Invalid number: $sStr")
                } else {
                    throw RuntimeException("Unexpected symbol: " + (if (ch == -1) "EOF" else ch.toChar()))
                }

                return x
            }
        }.parse()
    }

    fun calculate(input: String): String {
        if (input.isBlank() || input == "0") return "0"
        return try {
            val clean = input
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace(" ", "")
            val result = eval(clean)

            if (result.isInfinite() || result.isNaN()) {
                "Error"
            } else {
                val df = DecimalFormat("#.##########")
                df.roundingMode = RoundingMode.HALF_UP
                df.format(result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }
}
