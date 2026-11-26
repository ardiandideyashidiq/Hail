package com.aistra.hail.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.ui.theme.HailMotionTokens
import com.aistra.hail.ui.theme.rememberReducedMotion

/**
 * Navigation destination item for [FloatingPillNavBar] and [ExpressiveNavRail].
 */
data class PixelPlayNavItem(
    val id: Int,
    val label: String,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val selectedIconRes: Int = iconRes,
)

/**
 * PixelPlayer-inspired floating pill bottom navigation bar.
 *
 * Uses [RoundedCornerShape] (32.dp), `surfaceContainerHigh` with a subtle tonal border,
 * `navigationBarsPadding()` + 16.dp horizontal/bottom margins, `>= 48.dp` touch targets,
 * always-visible concise labels, and [HailMotionTokens.FastSpatial] spring indicator
 * animations with [rememberReducedMotion] `snap()` fallback.
 */
@Composable
fun FloatingPillNavBar(
    items: List<PixelPlayNavItem>,
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = item.id == selectedItemId
                    val interactionSource = remember(item.id) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.94f
                            isSelected -> 1.04f
                            else -> 1f
                        },
                        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                        label = "FloatingNavScale",
                    )
                    val nudgeY by animateDpAsState(
                        targetValue = if (isSelected) (-1).dp else 0.dp,
                        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                        label = "FloatingNavNudgeY",
                    )
                    val indicatorColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                        label = "FloatingNavIndicator",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                        label = "FloatingNavContent",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = nudgeY.toPx()
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(indicatorColor)
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onItemSelected(item.id) },
                            )
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isSelected) item.selectedIconRes else item.iconRes,
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matching PixelPlayer-inspired floating pill navigation rail for landscape, medium,
 * and expanded screens.
 */
@Composable
fun ExpressiveNavRail(
    items: List<PixelPlayNavItem>,
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEach { item ->
                    val isSelected = item.id == selectedItemId
                    val interactionSource = remember(item.id) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.94f
                            isSelected -> 1.04f
                            else -> 1f
                        },
                        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                        label = "RailNavScale",
                    )
                    val nudgeX by animateDpAsState(
                        targetValue = if (isSelected) 2.dp else 0.dp,
                        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                        label = "RailNavNudgeX",
                    )
                    val indicatorColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                        label = "RailNavIndicator",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                        label = "RailNavContent",
                    )

                    Box(
                        modifier = Modifier
                            .widthIn(min = 64.dp)
                            .heightIn(min = 56.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = nudgeX.toPx()
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .background(indicatorColor)
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onItemSelected(item.id) },
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isSelected) item.selectedIconRes else item.iconRes,
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smooth vertical gradient from [Color.Transparent] to [surfaceColor] that sits behind
 * [FloatingPillNavBar] so scrolling content fades cleanly without intercepting touches.
 */
@Composable
fun BottomFadeGradient(
    modifier: Modifier = Modifier,
    height: Dp = 112.dp,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        surfaceColor.copy(alpha = 0.85f),
                        surfaceColor,
                    ),
                ),
            ),
    )
}

/**
 * PixelPlayer `StatsOverviewCard`-style hero bento card displaying Frozen vs Active counts,
 * Working Mode badge, and two quick pill action buttons.
 */
@Composable
fun BentoStatusCard(
    frozenCount: Int,
    activeCount: Int,
    workingModeLabel: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    frozenLabel: String = stringResource(R.string.filter_frozen_apps),
    activeLabel: String = stringResource(R.string.filter_unfrozen_apps),
    primaryActionIcon: ImageVector? = null,
    secondaryActionIcon: ImageVector? = null,
) {
    val reducedMotion = rememberReducedMotion()
    val primaryInteraction = remember { MutableInteractionSource() }
    val secondaryInteraction = remember { MutableInteractionSource() }
    val primaryPressed by primaryInteraction.collectIsPressedAsState()
    val secondaryPressed by secondaryInteraction.collectIsPressedAsState()

    val primaryScale by animateFloatAsState(
        targetValue = if (primaryPressed) 0.96f else 1f,
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "BentoPrimaryScale",
    )
    val secondaryScale by animateFloatAsState(
        targetValue = if (secondaryPressed) 0.96f else 1f,
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "BentoSecondaryScale",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = workingModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Text(
                    text = "${frozenCount + activeCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoStatTile(
                    count = frozenCount,
                    label = frozenLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                BentoStatTile(
                    count = activeCount,
                    label = activeLabel,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onPrimaryAction,
                    interactionSource = primaryInteraction,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .graphicsLayer {
                            scaleX = primaryScale
                            scaleY = primaryScale
                        },
                ) {
                    if (primaryActionIcon != null) {
                        Icon(
                            imageVector = primaryActionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = primaryActionLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilledTonalButton(
                    onClick = onSecondaryAction,
                    interactionSource = secondaryInteraction,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .graphicsLayer {
                            scaleX = secondaryScale
                            scaleY = secondaryScale
                        },
                ) {
                    if (secondaryActionIcon != null) {
                        Icon(
                            imageVector = secondaryActionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = secondaryActionLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoStatTile(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * PixelPlayer `TabAnimation`-style tactile pill tab with [HailMotionTokens.FastSpatial]
 * spring scale and neighbor nudge (`offsetX`), falling back to `snap()` when reduced
 * motion is enabled.
 */
@Composable
fun TactilePillTab(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    index: Int = 0,
    selectedIndex: Int = 0,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "TactilePillTabScale",
    )
    val nudgeOffsetX by animateDpAsState(
        targetValue = when {
            index == selectedIndex - 1 -> (-3).dp
            index == selectedIndex + 1 -> 3.dp
            else -> 0.dp
        },
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "TactilePillTabNudgeX",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        label = "TactilePillTabContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        label = "TactilePillTabContent",
    )

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .offset { IntOffset(nudgeOffsetX.roundToPx(), 0) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            },
        ),
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
