package com.example.notepad.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

data class PresetColorItem(val name: String, val color: Color, val colorLong: Long)

val PRESET_COLORS = listOf(
    PresetColorItem("經典藍", Color(0xFF2196F3), 0xFF2196F3L),
    PresetColorItem("森林綠", Color(0xFF4CAF50), 0xFF4CAF50L),
    PresetColorItem("琥珀橙", Color(0xFFFF9800), 0xFFFF9800L),
    PresetColorItem("珊瑚紅", Color(0xFFF44336), 0xFFF44336L),
    PresetColorItem("典雅紫", Color(0xFF9C27B0), 0xFF9C27B0L),
    PresetColorItem("蒂芬妮", Color(0xFF009688), 0xFF009688L),
    PresetColorItem("明亮黃", Color(0xFFFFEB3B), 0xFFFFEB3BL),
    PresetColorItem("質感灰", Color(0xFF607D8B), 0xFF607D8BL)
)

/**
 * Color picker row displaying 8 classic preset color dots.
 */
@Composable
fun ColorPickerRow(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(PRESET_COLORS) { item ->
            val isSelected = item.colorLong == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.color)
                    .clickable { onColorSelected(item.colorLong) }
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val iconColor = if (item.color.luminance() > 0.5f) Color.Black else Color.White
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Color tag component calculates readable text contrast automatically based on background luminance.
 */
@Composable
fun ColorTag(
    text: String,
    tagColor: Color,
    modifier: Modifier = Modifier
) {
    // Calculate contrast text color: use black for light backgrounds and white for dark backgrounds
    val textColor = if (tagColor.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier
            .background(color = tagColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Custom Modifier for Breathing Dashed Border.
 */
fun Modifier.breathingDashedBorder(
    color: Color = Color(0xFFFF5722),
    cornerRadius: Float = 16f,
    strokeWidth: Float = 3f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_dashed_border")
    
    // Animate alpha for the "breathing" effect
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Animate phase for the "marching ants" dashed effect
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    this.drawBehind {
        val stroke = Stroke(
            width = strokeWidth.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(8f.dp.toPx(), 8f.dp.toPx()), 
                phase = phase.dp.toPx()
            )
        )
        drawRoundRect(
            color = color.copy(alpha = alpha),
            style = stroke,
            cornerRadius = CornerRadius(cornerRadius.dp.toPx(), cornerRadius.dp.toPx())
        )
    }
}

/**
 * Bouncing Badge Component
 */
@Composable
fun BouncingBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing_badge")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Surface(
        modifier = modifier.offset { 
            IntOffset(x = 0, y = bounceOffset.dp.roundToPx()) 
        },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Task Card demonstrating Imminent Deadline UI Alerts with interactive features.
 */
@Composable
fun DeadlineAlertCard(
    title: String,
    description: String,
    tag: String,
    tagColor: Color,
    isDueTomorrow: Boolean,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    dueDateText: String? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val showImminentAlert = isDueTomorrow && !isCompleted

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (showImminentAlert) 10.dp else 0.dp)
                .then(
                    if (showImminentAlert) {
                        Modifier.breathingDashedBorder(
                            color = Color(0xFFFF3D00),
                            cornerRadius = 14f,
                            strokeWidth = 2.5f
                        )
                    } else Modifier
                )
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onCheckedChange != null) {
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = onCheckedChange
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ColorTag(text = tag, tagColor = tagColor)
                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "刪除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!dueDateText.isNullOrEmpty()) {
                    Text(
                        text = dueDateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showImminentAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (showImminentAlert) {
            BouncingBadge(
                text = "⚠️ 明日到期！",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeadlineAlertCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DeadlineAlertCard(
                title = "寫期末專案報告",
                description = "完成 Android Compose 與 Room 資料庫架構章節",
                tag = "課業",
                tagColor = Color(0xFF2196F3),
                isDueTomorrow = false,
                dueDateText = "📅 2026/10/25 到期"
            )

            DeadlineAlertCard(
                title = "繳交伺服器帳單",
                description = "務必在明日午夜前完成信用卡繳費",
                tag = "緊急",
                tagColor = Color(0xFFFF9800),
                isDueTomorrow = true,
                dueDateText = "📅 明日到期"
            )
        }
    }
}