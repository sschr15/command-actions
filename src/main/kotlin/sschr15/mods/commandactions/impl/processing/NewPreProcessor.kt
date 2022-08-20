package sschr15.mods.commandactions.impl.processing

import sschr15.mods.commandactions.impl.processing.TokenType.*
import java.util.*

fun preprocess(tokens: List<Token>, customMacros: Map<String, String>): List<Token> {
    val currentMacros = customMacros.toMutableMap()
    // Add true / false numerical conversions
    currentMacros["true"] = "1"
    currentMacros["false"] = "0"

    val newTokens = mutableListOf<Token>()
    val ifStack = Stack<Boolean?>()
    for (t in tokens) {
        if (ifStack.isNotEmpty() && ifStack.peek() != true && t.type != DIRECTIVE) {
            continue
        }
        when (t.type) {
            WORD -> {
                val list = mutableListOf(t)
                while (list.any { it.value in currentMacros && it.extraData != "macro" }) {
                    val idx = list.indexOfFirst { it.value in currentMacros }
                    val token = list[idx]
                    val macro = tokenize(currentMacros[token.value]!!).map { if (token.value == it.value) it.copy(extraData = "macro") else it } // prevent infinite loop
                    list.removeAt(idx)
                    list.addAll(idx, macro)
                }
                newTokens.addAll(list)
            }
            NUMBER, SEPARATOR -> newTokens.add(t) // Never modify numbers or separators
            WHITESPACE -> newTokens.add(Token(WHITESPACE, " ")) // Replace with single space
            NEWLINE -> if (newTokens.lastOrNull()?.type != NEWLINE) {
                newTokens.add(Token(NEWLINE, "\n")) // Replace multiple consecutive newlines with single newline
            }
            COMMENT -> {} // Remove comments
            DIRECTIVE -> parseDirective(currentMacros, ifStack, t)
            EOF -> {} // Do nothing
        }
    }
    return newTokens
}

private fun parseIf(ifStack: Stack<Boolean?>, tokens: List<Token>) {
    val condition = tokens
        .filter { it.type != WHITESPACE }
    val conditionStr = condition.joinToString("") { it.value }
    val left: List<Token>
    val right: List<Token>
    val shouldFlip = if ("==" in conditionStr) {
        conditionStr.split("==").map(::tokenize).let { left = it[0]; right = it[1] }
        false
    } else if ("!=" in conditionStr) {
        conditionStr.split("!=").map(::tokenize).let { left = it[0]; right = it[1] }
        true
    } else if (condition.size == 1 && condition[0].type == NUMBER) {
        ifStack.push(condition[0].value.toInt() != 0)
        return
    } else if (condition.isNotEmpty() && condition.all { it.type == NUMBER || (it.value.length == 1 && it.value[0] in "+-*/") }) {
        var collector = 0.0
        var operator = '+'
        for (t in condition) {
            if (t.type == NUMBER) {
                when (operator) {
                    '+' -> collector += t.value.toDouble()
                    '-' -> collector -= t.value.toDouble()
                    '*' -> collector *= t.value.toDouble()
                    '/' -> collector /= t.value.toDouble()
                }
            } else {
                operator = t.value[0]
            }
        }
        ifStack.push(collector != 0.0)
        return
    } else {
        ifStack.push(false)
        return
    }

    val result = left.size == right.size && left.zip(right).all { (l, r) -> l == r }
    ifStack.push(if (shouldFlip) !result else result)
}

private fun parseDirective(currentMacros: MutableMap<String, String>, ifStack: Stack<Boolean?>, t: Token) {
    when (t.extraData) {
        "if" -> {
            if (ifStack.isNotEmpty() && ifStack.peek() != true) return
            parseIf(ifStack, tokenize(t.value))
        }
        "elif" -> {
            if (ifStack.isEmpty()) {
                currentMacros["_error"] = "elif without if"
                return
            }
            val prev = ifStack.pop()
            if (prev == false) {
                parseIf(ifStack, tokenize(t.value))
            } else {
                ifStack.push(null) // Indicate that future `if` directives should be ignored
            }
        }
        "else" -> {
            if (ifStack.isEmpty()) {
                currentMacros["_error"] = "else without if"
                return
            }
            val prev = ifStack.pop()
            if (prev == false) {
                ifStack.push(true)
            } else {
                ifStack.push(null) // Indicate that future `if` directives should be ignored
            }
        }
        "endif" -> {
            if (ifStack.isEmpty()) {
                currentMacros["_error"] = "endif without if"
                return
            }
            ifStack.pop()
        }
        "define" -> {
            val tokens = tokenize(t.value)
            if (tokens.size < 4 || tokens[0].type != WORD || tokens[1].type != WHITESPACE) {
                currentMacros["_error"] = "Invalid macro definition: ${t.value}"
                return
            }
            currentMacros[tokens[0].value] = tokens.drop(2).joinToString("") { it.value }
        }
        "undef" -> {
            val tokens = tokenize(t.value)
            if (tokens.size != 2 || tokens[0].type != WORD) {
                currentMacros["_error"] = "Invalid macro definition: ${t.value}"
                return
            }
            currentMacros.remove(tokens[0].value)
        }
        null -> {
            currentMacros["_error"] = "Non-directive attempted to be parsed as directive: ${t.value}"
        }
        else -> {
            currentMacros["_error"] = "Invalid directive: ${t.value}"
        }
    }
}
