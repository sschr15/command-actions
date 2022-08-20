package sschr15.mods.commandactions.impl.processing

import sschr15.mods.commandactions.impl.processing.TokenType.*

sealed interface InternalValue

data class Token(val type: TokenType, val value: String, val extraData: String? = null) : InternalValue {
    override fun toString() = value
}

enum class TokenType {
    /**
     * Any string of characters that doesn't fall into any other category.
     */
    WORD,

    /**
     * Any string of numerical digits, possibly with a decimal point and/or a preceding negative sign.
     * If preceded or followed by a non-whitespace non-separator character, it no longer counts as a number.
     */
    NUMBER,

    /**
     * Any whitespace character, excluding newlines.
     */
    WHITESPACE,

    /**
     * Any newline character.
     */
    NEWLINE,

    /**
     * Separating characters which are not whitespace.
     */
    SEPARATOR,

    /**
     * Any line which is not a directive, but which starts with a hash character.
     */
    COMMENT,

    /**
     * Any line which is a directive.
     */
    DIRECTIVE,

    /**
     * The end of the input.
     */
    EOF
}

fun tokenize(s: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var currentToken = ""
    var currentType = WORD
    var idx = 0
    while (idx in s.indices) {
        when (val char = s[idx++]) {
            '\n' -> {
                if (currentToken.isNotEmpty()) {
                    tokens.add(Token(currentType, currentToken))
                    currentToken = ""
                }
                tokens.add(Token(NEWLINE, "\n"))
                currentType = WORD
            }
            ' ', '\t' -> {
                if (currentToken.isNotEmpty()) {
                    tokens.add(Token(currentType, currentToken))
                    currentToken = ""
                }
                tokens.add(Token(WHITESPACE, buildString {
                    append(char)
                    while (idx in indices && this[idx] in listOf(' ', '\t')) {
                        append(this[idx++])
                    }
                }))
                currentType = WORD
            }
            '#' -> {
                if (currentToken.isNotEmpty()) {
                    currentToken += char
                } else {
                    val firstPart = buildString {
                        while (idx in s.indices && s[idx] !in listOf(' ', '\t', '\n')) {
                            append(s[idx++])
                        }
                    }
                    val secondPart = if (s[idx] == '\n') "" else buildString {
                        append(s[idx++])
                        while (idx in s.indices && (s[idx] != '\n' || s[idx - 1] == '\\')) {
                            append(s[idx++])
                        }
                    }
                    when (firstPart) {
                        "if", "elif", "else", "endif", "define", "undef" -> {
                            tokens.add(Token(DIRECTIVE, secondPart.trim(), firstPart))
                        }
                        else -> {
                            tokens.add(Token(COMMENT, "$firstPart$secondPart"))
                        }
                    }
                }
            }
            in ";:,=()[]{}<>+*/\\%^&|!`~'\"?" -> {
                if (currentToken.isNotEmpty()) {
                    tokens.add(Token(currentType, currentToken))
                    currentToken = ""
                }
                tokens.add(Token(SEPARATOR, char.toString()))
                currentType = WORD
            }
            in '0'..'9' -> {
                if (currentToken.isNotEmpty() && currentType != NUMBER && currentToken != "-") {
                    currentToken += char
                } else if (currentToken == "-" && tokens.last().type !in listOf(WHITESPACE, NEWLINE, SEPARATOR)) {
                    // The dash has to count as a separator if it's not preceded by whitespace or another separator.
                    tokens.add(Token(SEPARATOR, "-"))
                    currentToken = char.toString()
                    currentType = NUMBER
                } else {
                    currentToken += char
                    currentType = NUMBER
                }
            }
            '-' -> { // Special case: is it a separator or a negative number?
                if (currentToken.isNotEmpty()) { // Negative numbers only start with a dash.
                    tokens.add(Token(currentType, currentToken))
                }
                currentToken = char.toString()
                currentType = SEPARATOR
            }
            '.' -> { // Special case: is it a separator or a decimal number?
                if (currentType != NUMBER) {
                    tokens.add(Token(currentType, currentToken))
                    tokens.add(Token(SEPARATOR, "."))
                    currentToken = ""
                } else if (currentToken.isEmpty()) {
                    tokens.add(Token(SEPARATOR, "."))
                } else {
                    currentToken += char
                }
            }
            else -> {
                when (currentType) {
                    SEPARATOR -> {
                        // If the current token is a separator, it is added and the current token is reset to the current character.
                        if (currentToken.isNotEmpty()) {
                            tokens.add(Token(currentType, currentToken))
                        }
                        currentToken = ""
                        currentType = WORD
                    }
                    NUMBER -> {
                        // The token no longer qualifies as a number, so its type is overridden to WORD. The current token is added to.
                        currentType = WORD
                    }
                    else -> {} // nothing extra to do
                }
                currentToken += char
            }
        }
    }

    if (currentToken.isNotEmpty()) {
        tokens.add(Token(currentType, currentToken))
    }
    tokens.add(Token(EOF, ""))
    return tokens
}
