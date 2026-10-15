package com.devloper.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tvHistory: TextView
    private lateinit var tvResult: TextView

    private var currentExpression: String = ""
    private var lastResult: String = ""
    private var isNewCalculation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvHistory = findViewById(R.id.textViewHisroty)
        tvResult = findViewById(R.id.textViewResul)

        setupButtons()
    }

    private fun setupButtons() {
        val numberButtons = mapOf(
            R.id.btnZero to "0",
            R.id.btnOne to "1",
            R.id.btnTwo to "2",
            R.id.btnThree to "3",
            R.id.btnFour to "4",
            R.id.btnFive to "5",
            R.id.btnSix to "6",
            R.id.btnSeven to "7",
            R.id.btnEight to "8",
            R.id.btnNine to "9"
        )

        for ((id, value) in numberButtons) {
            findViewById<Button>(id)?.setOnClickListener {
                onDigitPressed(value)
            }
        }

        val operatorButtons = mapOf(
            R.id.btnPlus to "+",
            R.id.btnMinus to "-",
            R.id.btnMulti to "*",
            R.id.btnDivide to "/"
        )

        for ((id, op) in operatorButtons) {
            findViewById<Button>(id)?.setOnClickListener {
                onOperatorPressed(op)
            }
        }

        findViewById<Button>(R.id.btnDot)?.setOnClickListener {
            onDotPressed()
        }

        findViewById<Button>(R.id.btnAC)?.setOnClickListener {
            onClearPressed()
        }

        findViewById<Button>(R.id.btnDel)?.setOnClickListener {
            onDeletePressed()
        }

        findViewById<Button>(R.id.btnEqual)?.setOnClickListener {
            onEqualPressed()
        }
    }

    private fun onDigitPressed(digit: String) {
        if (isNewCalculation) {
            currentExpression = ""
            isNewCalculation = false
        }
        currentExpression += digit
        updateDisplay()
    }

    private fun onOperatorPressed(operator: String) {
        if (isNewCalculation) {
            if (lastResult.isNotEmpty() && lastResult != "Error") {
                currentExpression = lastResult
            }
            isNewCalculation = false
        }

        if (currentExpression.isEmpty()) {
            if (operator == "-") {
                currentExpression = "-"
                updateDisplay()
            }
            return
        }

        val lastChar = currentExpression.last()
        if (lastChar in listOf('+', '-', '*', '/')) {
            currentExpression = currentExpression.dropLast(1) + operator
        } else {
            currentExpression += operator
        }
        updateDisplay()
    }

    private fun onDotPressed() {
        if (isNewCalculation) {
            currentExpression = "0"
            isNewCalculation = false
        }

        val lastNumberSegment = currentExpression.split('+', '-', '*', '/').lastOrNull() ?: ""
        if (!lastNumberSegment.contains(".")) {
            if (lastNumberSegment.isEmpty()) {
                currentExpression += "0."
            } else {
                currentExpression += "."
            }
            updateDisplay()
        }
    }

    private fun onClearPressed() {
        currentExpression = ""
        lastResult = ""
        isNewCalculation = false
        tvHistory.text = ""
        tvResult.text = "0"
    }

    private fun onDeletePressed() {
        if (isNewCalculation) {
            onClearPressed()
            return
        }

        if (currentExpression.isNotEmpty()) {
            currentExpression = currentExpression.dropLast(1)
            updateDisplay()
        }
    }

    private fun onEqualPressed() {
        if (currentExpression.isEmpty()) return

        val result = evaluateExpression(currentExpression)
        tvHistory.text = currentExpression
        tvResult.text = result
        lastResult = result
        isNewCalculation = true
    }

    private fun updateDisplay() {
        tvResult.text = if (currentExpression.isEmpty()) "0" else currentExpression
    }

    private fun evaluateExpression(expr: String): String {
        return try {
            val tokens = tokenize(expr)
            if (tokens.isEmpty()) return ""
            val result = parseAndCompute(tokens) ?: return "Error"

            if (result.isNaN() || result.isInfinite()) {
                return "Error"
            }

            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                val df = DecimalFormat("#.########")
                df.format(result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                tokens.add(sb.toString())
            } else if (c in listOf('+', '-', '*', '/')) {
                if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    val sb = StringBuilder("-")
                    i++
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    if (sb.length > 1) {
                        tokens.add(sb.toString())
                    } else {
                        tokens.add("-")
                    }
                } else {
                    tokens.add(c.toString())
                    i++
                }
            } else {
                i++
            }
        }
        return tokens
    }

    private fun parseAndCompute(tokens: List<String>): Double? {
        if (tokens.isEmpty()) return null

        val values = mutableListOf<Double>()
        val ops = mutableListOf<String>()

        var idx = 0
        val firstNum = tokens[idx++].toDoubleOrNull() ?: return null
        values.add(firstNum)

        while (idx < tokens.size) {
            val op = tokens[idx++]
            if (idx >= tokens.size) break
            val nextNum = tokens[idx++].toDoubleOrNull() ?: return null

            if (op == "*" || op == "/") {
                val prevNum = values.removeAt(values.size - 1)
                if (op == "/") {
                    if (nextNum == 0.0) return null
                    values.add(prevNum / nextNum)
                } else {
                    values.add(prevNum * nextNum)
                }
            } else {
                ops.add(op)
                values.add(nextNum)
            }
        }

        var result = values[0]
        for (i in ops.indices) {
            val op = ops[i]
            val num = values[i + 1]
            if (op == "+") {
                result += num
            } else if (op == "-") {
                result -= num
            }
        }
        return result
    }
}