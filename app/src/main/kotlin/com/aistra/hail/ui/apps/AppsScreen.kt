package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.components.ExpressiveLoadingIndicator
import com.aistra.hail.ui.components.TactilePillTab
import com.aistra.hail.ui.theme.AppShapes
import com.aistra.hail.ui.theme.HailMotionTokens
import com.aistra.hail.ui.theme.rememberReducedMotion
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    apps: List<ApplicationInfo>,
    isRefreshing: Boolean,
    checkedPackages: Set<String>,
    filterState: AppsFilterState,
    isExportingApk: Boolean,
    onRefresh: () -> Unit,
    onQuickFilterSelected: (AppsQuickFilter) -> Unit,
    onToggleChecked: (String) -> Unit,
    onActionDetails: (ApplicationInfo) -> Unit,
    onActionLaunch: (ApplicationInfo) -> Unit,
    onActionToggleFreeze: (ApplicationInfo, Boolean) -> Unit,
    onActionExportClipboard: (ApplicationInfo) -> Unit,
    onActionExtractApk: (ApplicationInfo) -> Unit,
    onActionUninstall: (ApplicationInfo) -> Unit,
    onActionReinstall: (ApplicationInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spanCount = integerResource(R.integer.apps_span).coerceAtLeast(1)
    val configuration = LocalConfiguration.current
    val useRail = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 600
    val bottomContentPadding = if (useRail) 24.dp else 104.dp
    val pullToRefreshState = rememberPullToRefreshState()

    val quickFilterItems = listOf(
        AppsQuickFilter.USER to stringResource(R.string.filter_user_apps),
        AppsQuickFilter.SYSTEM to stringResource(R.string.filter_system_apps),
        AppsQuickFilter.FROZEN to stringResource(R.string.filter_frozen_apps),
        AppsQuickFilter.UNFROZEN to stringResource(R.string.filter_unfrozen_apps),
    )
    val selectedFilterIndex = quickFilterItems
        .indexOfFirst { it.first == filterState.activeQuickFilter }
        .coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            quickFilterItems.forEachIndexed { index, (filter, label) ->
                TactilePillTab(
                    selected = index == selectedFilterIndex,
                    onClick = { onQuickFilterSelected(filter) },
                    label = label,
                    index = index,
                    selectedIndex = selectedFilterIndex,
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            indicator = {
                if (isRefreshing || pullToRefreshState.distanceFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                    ) {
                        ExpressiveLoadingIndicator(
                            progress = if (isRefreshing) {
                                null
                            } else {
                                pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
                            },
                            contentDescription = stringResource(R.string.app_name),
                        )
                    }
                }
            },
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spanCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomContentPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = apps,
                    key = { it.packageName },
                    contentType = { "app_item" },
                ) { info ->
                    val pkg = info.packageName
                    val isChecked = pkg in checkedPackages
                    AppItemCard(
                        info = info,
                        isChecked = isChecked,
                        onToggleChecked = remember(pkg, onToggleChecked) { { onToggleChecked(pkg) } },
                        onActionDetails = remember(pkg, onActionDetails) { { onActionDetails(info) } },
                        onActionLaunch = remember(pkg, onActionLaunch) { { onActionLaunch(info) } },
                        onActionToggleFreeze = remember(pkg, onActionToggleFreeze) { { frozen -> onActionToggleFreeze(info, frozen) } },
                        onActionExportClipboard = remember(pkg, onActionExportClipboard) { { onActionExportClipboard(info) } },
                        onActionExtractApk = remember(pkg, onActionExtractApk) { { onActionExtractApk(info) } },
                        onActionUninstall = remember(pkg, onActionUninstall) { { onActionUninstall(info) } },
                        onActionReinstall = remember(pkg, onActionReinstall) { { onActionReinstall(info) } },
                    )
                }
            }
        }
    }

    if (isExportingApk) {
        AlertDialog(
            onDismissRequest = {},
            shape = AppShapes.extraLarge,
            title = {
                Text(text = stringResource(R.string.action_extract_apk))
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpressiveLoadingIndicator(
                        contentDescription = stringResource(R.string.action_extract_apk),
                    )
                }
            },
            confirmButton = {},
        )
    }
}

private val AppItemDefaultShape = RoundedCornerShape(20.dp)
private val AppItemCheckedShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItemCard(
    info: ApplicationInfo,
    isChecked: Boolean,
    onToggleChecked: () -> Unit,
    onActionDetails: () -> Unit,
    onActionLaunch: () -> Unit,
    onActionToggleFreeze: (Boolean) -> Unit,
    onActionExportClipboard: () -> Unit,
    onActionExtractApk: () -> Unit,
    onActionUninstall: () -> Unit,
    onActionReinstall: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val pkg = info.packageName
    val isFrozen = remember(pkg, info.flags) { AppManager.isAppFrozen(pkg) }
    val isUninstalled = remember(pkg, info.flags) { HPackages.isAppUninstalled(pkg) }
    val rawLabel = remember(pkg, info.labelRes, info.nonLocalizedLabel) {
        info.loadLabel(pm).toString()
    }
    val displayLabel = if (!HailData.grayscaleIcon && isFrozen) "❄️$rawLabel" else rawLabel
    val isDimmed = HailData.grayscaleIcon && isFrozen

    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "AppCardScale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isChecked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        label = "AppCardContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isChecked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        label = "AppCardContent",
    )

    val iconSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val cachedIcon = remember(pkg, iconSizePx, HailData.synthesizeAdaptiveIcons) {
        AppIconCache.getCachedBitmap(pkg, HPackages.myUserId, iconSizePx)?.asImageBitmap()
    }
    val iconBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = cachedIcon,
        key1 = pkg,
        key2 = HailData.synthesizeAdaptiveIcons,
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    AppIconCache.getOrLoadBitmap(context, info, HPackages.myUserId, iconSizePx).asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) },
        )
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleChecked,
                onLongClick = { menuExpanded = true },
            ),
        shape = if (isChecked) AppItemCheckedShape else AppItemDefaultShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = iconBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        colorFilter = if (isDimmed) grayscaleFilter else null,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isDimmed) 0.55f else 1f),
            ) {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUninstalled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        contentColor
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pkg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isChecked) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = rawLabel,
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_details)) },
                        onClick = {
                            menuExpanded = false
                            onActionDetails()
                        },
                    )
                    if (!isUninstalled && pm.getLaunchIntentForPackage(pkg) != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_launch)) },
                            onClick = {
                                menuExpanded = false
                                onActionLaunch()
                            },
                        )
                    }
                    if (HailData.workingMode != HailData.MODE_DEFAULT) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isFrozen) R.string.action_unfreeze else R.string.action_freeze,
                                    ),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onActionToggleFreeze(!isFrozen)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export_clipboard)) },
                        onClick = {
                            menuExpanded = false
                            onActionExportClipboard()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_extract_apk)) },
                        onClick = {
                            menuExpanded = false
                            onActionExtractApk()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_uninstall)) },
                        onClick = {
                            menuExpanded = false
                            onActionUninstall()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_reinstall)) },
                        onClick = {
                            menuExpanded = false
                            onActionReinstall()
                        },
                    )
                }
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggleChecked() },
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
fun AppsConfirmDialog(
    title: String?,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppShapes.extraLarge,
        title = title?.let { { Text(text = it) } },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}
