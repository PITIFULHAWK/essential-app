package com.nothing.essential.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothing.essential.ui.theme.DotInactive
import com.nothing.essential.ui.theme.TextPrimary

// 5x7 bitmaps for digits and symbols
private val DIGIT_MATRICES = mapOf(
    '0' to arrayOf(
        "01110",
        "10001",
        "10011",
        "10101",
        "11001",
        "10001",
        "01110"
    ),
    '1' to arrayOf(
        "00100",
        "01100",
        "00100",
        "00100",
        "00100",
        "00100",
        "01110"
    ),
    '2' to arrayOf(
        "01110",
        "10001",
        "00001",
        "00010",
        "00100",
        "01000",
        "11111"
    ),
    '3' to arrayOf(
        "01110",
        "10001",
        "00001",
        "00110",
        "00001",
        "10001",
        "01110"
    ),
    '4' to arrayOf(
        "00010",
        "00110",
        "01010",
        "10010",
        "11111",
        "00010",
        "00010"
    ),
    '5' to arrayOf(
        "11111",
        "10000",
        "11110",
        "00001",
        "00001",
        "10001",
        "01110"
    ),
    '6' to arrayOf(
        "01110",
        "10000",
        "11110",
        "10001",
        "10001",
        "10001",
        "01110"
    ),
    '7' to arrayOf(
        "11111",
        "00001",
        "00010",
        "00100",
        "01000",
        "01000",
        "01000"
    ),
    '8' to arrayOf(
        "01110",
        "10001",
        "10001",
        "01110",
        "10001",
        "10001",
        "01110"
    ),
    '9' to arrayOf(
        "01110",
        "10001",
        "10001",
        "01111",
        "00001",
        "00001",
        "01110"
    ),
    ':' to arrayOf(
        "00000",
        "00100",
        "00100",
        "00000",
        "00100",
        "00100",
        "00000"
    ),
    '/' to arrayOf(
        "00001",
        "00010",
        "00010",
        "00100",
        "00100",
        "01000",
        "10000"
    ),
    '-' to arrayOf(
        "00000",
        "00000",
        "00000",
        "11111",
        "00000",
        "00000",
        "00000"
    ),
    ' ' to arrayOf(
        "00000",
        "00000",
        "00000",
        "00000",
        "00000",
        "00000",
        "00000"
    )
)

@Composable
fun DotMatrixChar(
    char: Char,
    modifier: Modifier = Modifier,
    activeColor: Color = TextPrimary,
    inactiveColor: Color = DotInactive,
    dotSize: Dp = 3.dp,
    dotSpacing: Dp = 1.5.dp
) {
    val matrix = DIGIT_MATRICES[char] ?: DIGIT_MATRICES[' ']!!

    Canvas(
        modifier = modifier.size(
            width = (dotSize * 5) + (dotSpacing * 4),
            height = (dotSize * 7) + (dotSpacing * 6)
        )
    ) {
        val r = dotSize.toPx() / 2f
        val step = (dotSize + dotSpacing).toPx()

        for (row in 0 until 7) {
            val line = matrix[row]
            for (col in 0 until 5) {
                val isActive = line.getOrNull(col) == '1'
                val cx = col * step + r
                val cy = row * step + r
                drawCircle(
                    color = if (isActive) activeColor else inactiveColor,
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    activeColor: Color = TextPrimary,
    inactiveColor: Color = DotInactive,
    dotSize: Dp = 3.5.dp,
    charSpacing: Dp = 6.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(charSpacing)) {
        for (c in text) {
            DotMatrixChar(
                char = c,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                dotSize = dotSize
            )
        }
    }
}
