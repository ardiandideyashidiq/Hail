package com.aistra.hail.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aistra.hail.ui.theme.HailMotionTokens
import com.aistra.hail.ui.theme.orSnap
import com.aistra.hail.ui.theme.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Contained 48dp Material 3 Expressive shape-morphing loading indicator.
 *
 * Morphs smoothly through lobed polygon phases while rotating when [progress] is null,
 * or tracks [progress] (0f..1f) deterministically for pull-to-refresh. Falls back to a
 * static emblem when system reduced motion is enabled.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    contentDescription: String? = null,
) {
    val reducedMotion = rememberReducedMotion()
    val phase: Float
    val rotation: Float

    if (reducedMotion) {
        phase = 0.5f
        rotation = 0f
    } else if (progress != null) {
        val clamped = progress.coerceIn(0f, 1f)
        phase = clamped
        rotation = clamped * 180f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "ExpressiveLoading")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "MorphPhase",
        )
        val animatedRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "Rotation",
        )
        phase = animatedPhase
        rotation = animatedRotation
    }

    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Surface(
        modifier = semanticsModifier.size(48.dp),
        shape = CircleShape,
        color = containerColor,
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = rotation },
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseRadius = size.minDimension * 0.36f
                // Morph between a 4-lobed clover and a 7-scalloped burst
                val lobes = 4f + 3f * phase
                val amplitude = size.minDimension * (0.11f - 0.03f * phase)
                val steps = 72
                val path = Path()
                for (i in 0..steps) {
                    val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                    val r = baseRadius + amplitude * cos(lobes * theta)
                    val x = cx + r * cos(theta)
                    val y = cy + r * sin(theta)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, color = indicatorColor)
            }
        }
    }
}

/**
 * Item model for [ExpressiveConnectedButtonGroup].
 */
data class ExpressiveButtonGroupItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

/**
 * Material 3 Expressive connected button group for tag/filter selection.
 *
 * Outer corners remain pill-rounded (20dp) while inner corners morph between 4dp (unselected)
 * and 20dp (selected/pressed) using [HailMotionTokens.fastSpatial]. Minimum height is 48dp.
 */
@Composable
fun <T> ExpressiveConnectedButtonGroup(
    items: List<ExpressiveButtonGroupItem<T>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    if (items.isEmpty()) return
    val reducedMotion = rememberReducedMotion()
    val outerCorner = 20.dp
    val innerUnselectedCorner = 4.dp
    val innerSelectedCorner = 20.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val targetInnerCorner = if (isSelected || isPressed) {
                innerSelectedCorner
            } else {
                innerUnselectedCorner
            }
            val innerCorner by animateDpAsState(
                targetValue = targetInnerCorner,
                animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                label = "ButtonGroupInnerCorner",
            )

            val shape = when {
                items.size == 1 -> RoundedCornerShape(outerCorner)
                index == 0 -> RoundedCornerShape(
                    topStart = outerCorner,
                    bottomStart = outerCorner,
                    topEnd = innerCorner,
                    bottomEnd = innerCorner,
                )
                index == items.lastIndex -> RoundedCornerShape(
                    topStart = innerCorner,
                    bottomStart = innerCorner,
                    topEnd = outerCorner,
                    bottomEnd = outerCorner,
                )
                else -> RoundedCornerShape(innerCorner)
            }

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) selectedContainerColor else containerColor,
                animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                label = "ButtonGroupBg",
            )
            val fgColor by animateColorAsState(
                targetValue = if (isSelected) selectedContentColor else contentColor,
                animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                label = "ButtonGroupFg",
            )

            Surface(
                selected = isSelected,
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        selected = isSelected
                        role = Role.Tab
                    },
                shape = shape,
                color = bgColor,
                contentColor = fgColor,
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                interactionSource = interactionSource,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Action item model for [ExpressiveFabMenu].
 */
data class ExpressiveFabMenuItem(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
    val onClick: () -> Unit,
)

/**
 * Material 3 Expressive FAB menu.
 *
 * The 56dp FAB morphs its corner radius from 16dp to 28dp (circle) into a close button when
 * [expanded] is true, revealing 2-6 labeled action items with spring physics.
 */
@Composable
fun ExpressiveFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<ExpressiveFabMenuItem>,
    fabContentDescription: String,
    closeContentDescription: String,
    modifier: Modifier = Modifier,
    fabIcon: ImageVector = Icons.Rounded.Add,
    closeIcon: ImageVector = Icons.Rounded.Close,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    itemContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    itemContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val reducedMotion = rememberReducedMotion()
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 28.dp else 16.dp,
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "FabCornerMorph",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded && items.isNotEmpty(),
            enter = fadeIn(HailMotionTokens.fastEffectsOrSnap(reducedMotion)) +
                expandVertically(
                    animationSpec = HailMotionTokens.defaultSpatialOrSnap(reducedMotion),
                    expandFrom = Alignment.Bottom,
                ) +
                scaleIn(
                    animationSpec = HailMotionTokens.defaultSpatialOrSnap(reducedMotion),
                    initialScale = 0.85f,
                    transformOrigin = TransformOrigin(1f, 1f),
                ),
            exit = fadeOut(HailMotionTokens.fastEffectsOrSnap(reducedMotion)) +
                shrinkVertically(
                    animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                    shrinkTowards = Alignment.Bottom,
                ) +
                scaleOut(
                    animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                    targetScale = 0.85f,
                    transformOrigin = TransformOrigin(1f, 1f),
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    Surface(
                        onClick = {
                            onExpandedChange(false)
                            item.onClick()
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = item.contentDescription
                                role = Role.Button
                            },
                        shape = CircleShape,
                        color = itemContainerColor,
                        contentColor = itemContentColor,
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Surface(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = if (expanded) closeContentDescription else fabContentDescription
                    role = Role.Button
                },
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(HailMotionTokens.fastEffectsOrSnap(reducedMotion)) +
                            scaleIn(
                                animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                                initialScale = 0.8f,
                            ))
                            .togetherWith(
                                fadeOut(HailMotionTokens.fastEffectsOrSnap(reducedMotion)) +
                                    scaleOut(
                                        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                                        targetScale = 0.8f,
                                    ),
                            )
                    },
                    label = "FabIconMorph",
                ) { isExpanded ->
                    Icon(
                        imageVector = if (isExpanded) closeIcon else fabIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Vibrant Material 3 Expressive floating toolbar for multi-select batch actions.
 *
 * Enters and exits with [HailMotionTokens.defaultSpatial] / [HailMotionTokens.fastSpatial]
 * and [HailMotionTokens.fastEffects], snapping immediately when reduced motion is enabled.
 */
@Composable
fun ExpressiveFloatingToolbar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val reducedMotion = rememberReducedMotion()

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = HailMotionTokens.defaultSpatialOrSnap<IntOffset>(reducedMotion),
            initialOffsetY = { it },
        ) + fadeIn(
            animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        ),
        exit = slideOutVertically(
            animationSpec = HailMotionTokens.fastSpatialOrSnap<IntOffset>(reducedMotion),
            targetOffsetY = { it },
        ) + fadeOut(
            animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        ),
    ) {
        Surface(
            modifier = Modifier.defaultMinSize(minHeight = 64.dp),
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 6.dp,
            tonalElevation = 3.dp,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Row(
                    modifier = Modifier.padding(contentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * Accessible action button with a minimum 48dp touch target for use inside [ExpressiveFloatingToolbar].
 */
@Composable
fun ExpressiveFloatingToolbarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
    showLabel: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        shape = CircleShape,
        color = containerColor,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showLabel) 14.dp else 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
