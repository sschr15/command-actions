package sschr15.mods.commandactions

import java.util.*

fun processIfs(fileContent: String): String {
    val ifs = fileContent.split("\n").filter { it.startsWith("#if ") }
    if (ifs.isEmpty()) return fileContent

    return buildString {
        val ifStack = Stack<Boolean>()
        for (line in fileContent.split("\n")) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#if ") && (ifStack.isEmpty() || ifStack.peek())) {
                val condition = trimmed.substring(4).trim()
                if (condition.isEmpty()) {
                    // Unknown conditions should be treated falsy
                    ifStack.push(false)
                } else {
                    ifStack.push(evaluateCondition(condition))
                }
            } else if (trimmed.startsWith("#else")) {
                if (ifStack.isEmpty()) {
                    appendLine("# An #else was found without an #if")
                    continue
                }
                val result = ifStack.pop()
                ifStack.push(!result)
            } else if (trimmed.startsWith("#endif")) {
                if (ifStack.isEmpty()) {
                    appendLine("# A #endif was found without an #if")
                    continue
                }
                ifStack.pop()
            } else if (ifStack.isEmpty() || ifStack.peek()) {
                appendLine(trimmed)
            }
        }
    }
}

fun evaluateCondition(condition: String): Boolean {
    var working = condition.trim()
    if (working == "true") return true
    if (working == "false") return false

    if (working.startsWith("!")) {
        return !evaluateCondition(working.substring(1))
    }
    if (working.contains(Regex("""\(.*\)""""))) {
        val stack = Stack<String>()
        working.split('(').forEach {
            stack.push(it)
            it.split(')').forEach { s ->
                val value = stack.pop() + s
                val evaluated = evaluateCondition(value)
                stack.push(stack.pop() + evaluated)
            }
        }
        working = stack.pop()
    }
    if (working.contains(Regex("""&&|\|\|"""))) {
        val parts = working.split("&&").map { it.split("||") }
        return parts.all { it.any { s -> evaluateCondition(s) } }
    }

    val delimiters = listOf("==", "!=", ">", "<", ">=", "<=")
    for (delimiter in delimiters) {
        while (working.contains(delimiter)) {
            val (left, right) = working.split(delimiter, limit = 2)
            val leftNum = left.trim().toDoubleOrNull()
            val rightNum = right.trim().toDoubleOrNull()
            val canCompareNumbers = leftNum != null && rightNum != null
            val result = when (delimiter) {
                "==" -> left.trim() == right.trim()
                "!=" -> left.trim() != right.trim()
                ">" -> canCompareNumbers && leftNum!! > rightNum!!
                "<" -> canCompareNumbers && leftNum!! < rightNum!!
                ">=" -> canCompareNumbers && leftNum!! >= rightNum!!
                "<=" -> canCompareNumbers && leftNum!! <= rightNum!!
                else -> false // Unknown conditions should be treated falsy
            }

            working = working.replace("$left$delimiter$right", "$result")
        }
    }
    return working.toBooleanStrictOrNull() ?: false
}
