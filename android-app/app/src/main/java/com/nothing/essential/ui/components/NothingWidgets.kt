package com.nothing.essential.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.ui.theme.*

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CardBorder,
    backgroundColor: Color = CardBackground,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp))
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
            )
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun TopBarHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    hasRedDot: Boolean = false,
    isActionPrimary: Boolean = true,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    letterSpacing = 1.2.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp
                )
            )
        }

        if (actionText != null && onActionClick != null) {
            PillButton(
                text = actionText,
                onClick = onActionClick,
                icon = actionIcon,
                hasRedDot = hasRedDot,
                isSolidWhite = isActionPrimary
            )
        }
    }
}

@Composable
fun CapsulePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val bg = if (selected) TextPrimary else PillBackground
    val fg = if (selected) AmoledBlack else TextSecondary
    val border = if (selected) null else BorderStroke(1.dp, PillBorder)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(20.dp)) else Modifier)
            .background(bg)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                color = fg,
                letterSpacing = 0.6.sp
            )
        )
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hasRedDot: Boolean = false,
    isSolidWhite: Boolean = false,
    isDarkOutline: Boolean = false,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    val bg = when {
        !enabled -> PillBackground
        isSolidWhite -> TextPrimary
        isDarkOutline -> Color(0xFF0A0A0A)
        else -> Color(0xFF1E1E1E)
    }

    val fg = when {
        !enabled -> TextTertiary
        isSolidWhite -> AmoledBlack
        else -> TextPrimary
    }

    val border = if (isSolidWhite) null else BorderStroke(1.dp, Color(0xFF333333))

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(20.dp)) else Modifier)
            .background(bg)
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (hasRedDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = fg,
                letterSpacing = 0.8.sp
            )
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .border(BorderStroke(1.dp, Color(0xFF262626)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextTertiary,
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PillButton(
            text = buttonText,
            onClick = onButtonClick,
            isDarkOutline = true
        )
    }
}

@Composable
fun MaskedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isSecret: Boolean = true
) {
    var isVisible by remember { mutableStateOf(!isSecret) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141414))
                .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    visualTransformation = if (isSecret && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(NothingRed),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isSecret) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Visibility",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { isVisible = !isVisible }
                )
            }
        }
    }
}
