package com.ashmeet.hyperlauncher.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class MarkdownColors(
    val textColor: Color,
    val primaryColor: Color,
    val codeBgColor: Color,
    val codeTextColor: Color,
    val blockquoteColor: Color,
    val dividerColor: Color,
)

@Composable
fun rememberMarkdownColors(): MarkdownColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        MarkdownColors(
            textColor = colorScheme.onSurface,
            primaryColor = colorScheme.primary,
            codeBgColor = colorScheme.surfaceVariant,
            codeTextColor = colorScheme.onSurfaceVariant,
            blockquoteColor = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            dividerColor = colorScheme.outlineVariant
        )
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    baseFontSize: TextUnit = 13.sp,
    baseLineHeight: TextUnit = 18.sp,
    colors: MarkdownColors = rememberMarkdownColors()
) {
    val finalColors = if (color != Color.Unspecified) colors.copy(textColor = color) else colors
    val annotatedString = remember(markdown, finalColors, baseFontSize) {
        parseMarkdownToAnnotatedString(markdown, finalColors, baseFontSize)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        fontSize = baseFontSize,
        lineHeight = baseLineHeight,
        fontFamily = FontFamily.SansSerif
    )
}

fun parseMarkdownToAnnotatedString(
    markdown: String,
    colors: MarkdownColors,
    baseFontSize: TextUnit = 13.sp
): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var inCodeBlock = false
        var isFirstLine = true

        for (rawLine in lines) {
            if (rawLine.trimStart().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                if (!inCodeBlock) {
                    append("\n")
                }
                isFirstLine = false
                continue
            }

            if (inCodeBlock) {
                if (length > 0) append("\n")
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = colors.codeBgColor,
                        color = colors.codeTextColor,
                        fontSize = (baseFontSize.value - 1).coerceAtLeast(10f).sp
                    )
                )
                append("  $rawLine  ")
                pop()
                isFirstLine = false
                continue
            }


            val trimmedLine = rawLine.trim()
            if (trimmedLine.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$"))) {
                if (!isFirstLine) append("\n\n")
                pushStyle(SpanStyle(color = colors.dividerColor, fontWeight = FontWeight.Bold))
                append("──────────────────────────────")
                pop()
                isFirstLine = false
                continue
            }

            var headingLevel = 0
            var headingText = ""
            if (trimmedLine.startsWith("#")) {
                val match = Regex("^(#{1,6})\\s+(.*)$").find(trimmedLine)
                if (match != null) {
                    headingLevel = match.groupValues[1].length
                    headingText = match.groupValues[2]
                }
            }

            if (headingLevel > 0) {
                if (!isFirstLine) append("\n\n")
                val fontSize = when (headingLevel) {
                    1 -> (baseFontSize.value + 5).sp
                    2 -> (baseFontSize.value + 3.5).sp
                    3 -> (baseFontSize.value + 2).sp
                    4 -> (baseFontSize.value + 1).sp
                    else -> baseFontSize
                }
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = colors.primaryColor
                    )
                )
                append(parseInlineMarkdown(headingText, colors))
                pop()
                isFirstLine = false
                continue
            }

            if (trimmedLine.startsWith(">")) {
                if (!isFirstLine) append("\n")
                val quoteText = trimmedLine.removePrefix(">").trim()
                pushStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = colors.blockquoteColor
                    )
                )
                append("│ ")
                append(parseInlineMarkdown(quoteText, colors))
                pop()
                isFirstLine = false
                continue
            }

            val bulletMatch = Regex("^([-*+])\\s+(.*)$").find(trimmedLine)
            if (bulletMatch != null) {
                if (!isFirstLine) append("\n")
                val content = bulletMatch.groupValues[2]
                pushStyle(SpanStyle(color = colors.primaryColor, fontWeight = FontWeight.Bold))
                append("• ")
                pop()
                append(parseInlineMarkdown(content, colors))
                isFirstLine = false
                continue
            }

            val numListMatch = Regex("^(\\d+\\.)\\s+(.*)$").find(trimmedLine)
            if (numListMatch != null) {
                if (!isFirstLine) append("\n")
                val prefix = numListMatch.groupValues[1]
                val content = numListMatch.groupValues[2]
                pushStyle(SpanStyle(color = colors.primaryColor, fontWeight = FontWeight.Bold))
                append("$prefix ")
                pop()
                append(parseInlineMarkdown(content, colors))
                isFirstLine = false
                continue
            }

            if (trimmedLine.isEmpty()) {
                append("\n")
            } else {
                if (!isFirstLine) append("\n")
                append(parseInlineMarkdown(rawLine, colors))
            }
            isFirstLine = false
        }
    }
}

fun parseInlineMarkdown(
    input: String,
    colors: MarkdownColors
): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        val length = input.length

        while (index < length) {
            if (input[index] == '[') {
                val closeBracket = input.indexOf(']', index + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && input[closeBracket + 1] == '(') {
                    val closeParen = input.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = input.substring(index + 1, closeBracket).trim()
                        if (linkText.isNotEmpty()) {
                            pushStyle(
                                SpanStyle(
                                    color = colors.primaryColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            append(parseInlineMarkdown(linkText, colors))
                            pop()
                        }
                        index = closeParen + 1
                        continue
                    }
                }
            }

            if (input[index] == '`') {
                val end = input.indexOf('`', index + 1)
                if (end != -1 && end > index) {
                    val codeText = input.substring(index + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = colors.codeBgColor,
                            color = colors.codeTextColor,
                            fontSize = 12.sp
                        )
                    )
                    append(" $codeText ")
                    pop()
                    index = end + 1
                    continue
                }
            }

            // Remove () from markdowns
            if (input[index] == '(' || input[index] == ')') {
                index++
                continue
            }

            // Check bold + italic: ***text*** or ___text___
            if ((input.startsWith("***", index) || input.startsWith("___", index)) && index + 3 < length) {
                val delimiter = input.substring(index, index + 3)
                val end = input.indexOf(delimiter, index + 3)
                if (end != -1 && end > index + 3) {
                    val content = input.substring(index + 3, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(parseInlineMarkdown(content, colors))
                    pop()
                    index = end + 3
                    continue
                }
            }

            // Check bold: **text** or __text__
            if ((input.startsWith("**", index) || input.startsWith("__", index)) && index + 2 < length) {
                val delimiter = input.substring(index, index + 2)
                val end = input.indexOf(delimiter, index + 2)
                if (end != -1 && end > index + 2) {
                    val content = input.substring(index + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(parseInlineMarkdown(content, colors))
                    pop()
                    index = end + 2
                    continue
                }
            }

            // Check italic: *text* or _text_
            if ((input[index] == '*' || input[index] == '_') && index + 1 < length) {
                val delimiter = input[index]
                val isUnderscore = delimiter == '_'
                val isWordCharBefore = index > 0 && input[index - 1].isLetterOrDigit()

                if (!isUnderscore || !isWordCharBefore) {
                    val end = input.indexOf(delimiter, index + 1)
                    if (end != -1 && end > index + 1) {
                        val isWordCharAfter = end < length - 1 && input[end + 1].isLetterOrDigit()
                        if (!isUnderscore || !isWordCharAfter) {
                            val content = input.substring(index + 1, end)
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                            append(parseInlineMarkdown(content, colors))
                            pop()
                            index = end + 1
                            continue
                        }
                    }
                }
            }

            // Check strikethrough: ~~text~~
            if (input.startsWith("~~", index) && index + 2 < length) {
                val end = input.indexOf("~~", index + 2)
                if (end != -1 && end > index + 2) {
                    val content = input.substring(index + 2, end)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(parseInlineMarkdown(content, colors))
                    pop()
                    index = end + 2
                    continue
                }
            }

            // Normal character
            append(input[index])
            index++
        }
    }
}
