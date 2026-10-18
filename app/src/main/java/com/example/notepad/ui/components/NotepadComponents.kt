package com.example.notepad.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

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
    color: Color = Color.Red,
    cornerRadius: Float = 16f,
    strokeWidth: Float = 4f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_dashed_border")
    
    // Animate alpha for the "breathing" effect
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Animate phase for the "marching ants" dashed effect
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f, // Match dash length to create seamless movement
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
                intervals = floatArrayOf(10f.dp.toPx(), 10f.dp.toPx()), 
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
        targetValue = -12f,
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
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                modifier = Modifier.size(16.dp)
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
 * Task Card demonstrating Imminent Deadline UI Alerts.
 */
@Composable
fun DeadlineAlertCard(
    title: String,
    description: String,
    tag: String,
    tagColor: Color,
    isDueTomorrow: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isDueTomorrow) 12.dp else 0.dp) // Provide space for badge if needed
                .then(
                    if (isDueTomorrow) {
                        Modifier.breathingDashedBorder(
                            color = MaterialTheme.colorScheme.error,
                            cornerRadius = 12f,
                            strokeWidth = 2f
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ColorTag(text = tag, tagColor = tagColor)
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isDueTomorrow) {
            BouncingBadge(
                text = "Due Tomorrow!",
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Normal Item
            DeadlineAlertCard(
                title = "Write Documentation",
                description = "Complete the API reference for the new endpoints.",
                tag = "Development",
                tagColor = Color(0xFF2196F3), // Blue
                isDueTomorrow = false
            )

            // Due Tomorrow Item
            DeadlineAlertCard(
                title = "Submit Taxes",
                description = "Review and submit all forms before the deadline.",
                tag = "Urgent",
                tagColor = Color(0xFFFFEB3B), // Yellow (Light background, should have dark text)
                isDueTomorrow = true
            )
            
            // Due Tomorrow Item with dark tag
            DeadlineAlertCard(
                title = "Design Review",
                description = "Prepare slides for the design review meeting.",
                tag = "Design",
                tagColor = Color(0xFF673AB7), // Purple (Dark background, should have light text)
                isDueTomorrow = true
            )
        }
    }
}