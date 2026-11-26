package com.aistra.hail.ui.home

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.ui.components.ExpressiveButtonGroupItem
import com.aistra.hail.ui.components.ExpressiveConnectedButtonGroup
import com.aistra.hail.ui.components.ExpressiveFabMenu
import com.aistra.hail.ui.components.ExpressiveFabMenuItem
import com.aistra.hail.ui.components.ExpressiveFloatingToolbar
import com.aistra.hail.ui.components.ExpressiveFloatingToolbarAction
import com.aistra.hail.ui.components.ExpressiveLoadingIndicator
import com.aistra.hail.ui.theme.HailMotionTokens
import com.aistra.hail.ui.theme.rememberReducedMotion
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages.myUserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tags: List<Pair<String, Int>>,
    selectedTagIndex: Int,
    onSelectTag: (Int) -> Unit,
    onTagLongClick: (Int) -> Unit,
    query: String,
    appsForTag: (Int) -> List<AppInfo>,
    searchResults: List<AppInfo>,
    refreshTick: Int,
    compactIcon: Boolean,
    homeFontSize: Float,
    grayscaleIcon: Boolean,
    synthesizeAdaptiveIcons: Boolean,
    multiselect: Boolean,
    selectedPackages: Set<String>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFreezeCurrent: () -> Unit,
    onUnfreezeCurrent: () -> Unit,
    onFreezeNonWhitelisted: () -> Unit,
    onBatchFreeze: () -> Unit,
    onBatchUnfreeze: () -> Unit,
    onBatchTag: () -> Unit,
    onSelectAll: () -> Unit,
    onCloseMultiselect: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val coroutineScope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bottomInsetModifier = if (isLandscape) {
        Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    } else {
        Modifier
    }

    val safePageCount = tags.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = selectedTagIndex.coerceIn(0, safePageCount - 1),
        pageCount = { safePageCount },
    )

    LaunchedEffect(selectedTagIndex, tags.size) {
        val target = selectedTagIndex.coerceIn(0, safePageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != selectedTagIndex && page < tags.size) {
                    onSelectTag(page)
                }
            }
    }

    var fabExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(multiselect) {
        if (multiselect) fabExpanded = false
    }

    val spanCount = integerResource(
        if (compactIcon) R.integer.home_span_compact else R.integer.home_span,
    )
    val pullRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (query.isEmpty() && tags.size > 1) {
                TagSelectorBar(
                    tags = tags,
                    selectedIndex = selectedTagIndex.coerceIn(0, tags.lastIndex),
                    onSelect = { index ->
                        onSelectTag(index)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    onTagLongClick = onTagLongClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                indicator = {
                    PullToRefreshDefaults.IndicatorBox(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = Color.Transparent,
                        elevation = 0.dp,
                    ) {
                        ExpressiveLoadingIndicator(
                            progress = if (isRefreshing) {
                                null
                            } else {
                                pullRefreshState.distanceFraction.coerceIn(0f, 1f)
                            },
                        )
                    }
                },
            ) {
                if (query.isNotEmpty()) {
                    AppGridContent(
                        apps = searchResults,
                        spanCount = spanCount,
                        refreshTick = refreshTick,
                        homeFontSize = homeFontSize,
                        grayscaleIcon = grayscaleIcon,
                        synthesizeAdaptiveIcons = synthesizeAdaptiveIcons,
                        selectedPackages = selectedPackages,
                        reducedMotion = reducedMotion,
                        isLandscape = isLandscape,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { index -> tags.getOrNull(index)?.second ?: index },
                    ) { page ->
                        val tagId = tags.getOrNull(page)?.second ?: 0
                        val pageApps = remember(tagId, refreshTick) { appsForTag(tagId) }
                        AppGridContent(
                            apps = pageApps,
                            spanCount = spanCount,
                            refreshTick = refreshTick,
                            homeFontSize = homeFontSize,
                            grayscaleIcon = grayscaleIcon,
                            synthesizeAdaptiveIcons = synthesizeAdaptiveIcons,
                            selectedPackages = selectedPackages,
                            reducedMotion = reducedMotion,
                            isLandscape = isLandscape,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !multiselect,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(bottomInsetModifier)
                .padding(16.dp),
            enter = fadeIn(HailMotionTokens.fastEffectsOrSnap(reducedMotion)) +
                scaleIn(
                    animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                    initialScale = 0.85f,
                ),
            exit = fadeOut(HailMotionTokens.fastEffectsOrSnap(reducedMotion)) +
                scaleOut(
                    animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                    targetScale = 0.85f,
                ),
        ) {
            ExpressiveFabMenu(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                items = listOf(
                    ExpressiveFabMenuItem(
                        label = stringResource(R.string.action_freeze_current),
                        icon = Icons.Rounded.AcUnit,
                        onClick = onFreezeCurrent,
                    ),
                    ExpressiveFabMenuItem(
                        label = stringResource(R.string.action_unfreeze_current),
                        icon = Icons.Rounded.WbSunny,
                        onClick = onUnfreezeCurrent,
                    ),
                    ExpressiveFabMenuItem(
                        label = stringResource(R.string.action_freeze_non_whitelisted),
                        icon = Icons.Rounded.Shield,
                        onClick = onFreezeNonWhitelisted,
                    ),
                ),
                fabContentDescription = stringResource(R.string.action_freeze_current),
                closeContentDescription = stringResource(android.R.string.cancel),
                fabIcon = Icons.Rounded.AcUnit,
            )
        }

        ExpressiveFloatingToolbar(
            visible = multiselect,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(bottomInsetModifier)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            ExpressiveFloatingToolbarAction(
                icon = Icons.Rounded.AcUnit,
                label = stringResource(R.string.action_freeze),
                onClick = onBatchFreeze,
                enabled = selectedPackages.isNotEmpty(),
            )
            ExpressiveFloatingToolbarAction(
                icon = Icons.Rounded.WbSunny,
                label = stringResource(R.string.action_unfreeze),
                onClick = onBatchUnfreeze,
                enabled = selectedPackages.isNotEmpty(),
            )
            ExpressiveFloatingToolbarAction(
                icon = Icons.AutoMirrored.Rounded.Label,
                label = stringResource(R.string.action_tag_set),
                onClick = onBatchTag,
                enabled = selectedPackages.isNotEmpty(),
            )
            ExpressiveFloatingToolbarAction(
                icon = Icons.Rounded.SelectAll,
                label = stringResource(R.string.action_select_all),
                onClick = onSelectAll,
            )
            ExpressiveFloatingToolbarAction(
                icon = Icons.Rounded.Close,
                label = stringResource(R.string.action_deselect),
                onClick = onCloseMultiselect,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagSelectorBar(
    tags: List<Pair<String, Int>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onTagLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()

    if (tags.size in 2..4) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExpressiveConnectedButtonGroup(
                items = tags.map { ExpressiveButtonGroupItem(value = it.second, label = it.first) },
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onTagLongClick(selectedIndex) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.action_tag_set),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tags.forEachIndexed { index, (label, _) ->
                val isSelected = index == selectedIndex
                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 12.dp,
                    animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
                    label = "ScrollableTagCorner",
                )
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                    label = "ScrollableTagBg",
                )
                val fgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
                    label = "ScrollableTagFg",
                )

                Surface(
                    shape = RoundedCornerShape(cornerRadius),
                    color = bgColor,
                    contentColor = fgColor,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .combinedClickable(
                            onClick = { onSelect(index) },
                            onLongClick = { onTagLongClick(index) },
                        )
                        .semantics {
                            selected = isSelected
                            role = Role.Tab
                        },
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
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
    }
}

@Composable
private fun AppGridContent(
    apps: List<AppInfo>,
    spanCount: Int,
    refreshTick: Int,
    homeFontSize: Float,
    grayscaleIcon: Boolean,
    synthesizeAdaptiveIcons: Boolean,
    selectedPackages: Set<String>,
    reducedMotion: Boolean,
    isLandscape: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.nothing_here),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val bottomPadding = if (isLandscape) 112.dp else 96.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            items = apps,
            key = { it.packageName },
        ) { info ->
            AppCard(
                info = info,
                isSelected = info.packageName in selectedPackages,
                refreshTick = refreshTick,
                homeFontSize = homeFontSize,
                grayscaleIcon = grayscaleIcon,
                synthesizeAdaptiveIcons = synthesizeAdaptiveIcons,
                reducedMotion = reducedMotion,
                onClick = { onAppClick(info) },
                onLongClick = { onAppLongClick(info) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(
    info: AppInfo,
    isSelected: Boolean,
    refreshTick: Int,
    homeFontSize: Float,
    grayscaleIcon: Boolean,
    synthesizeAdaptiveIcons: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val appState = remember(info.packageName, refreshTick) { info.state }
    val isFrozen = appState == AppInfo.State.FROZEN
    val isNotFound = appState == AppInfo.State.NOT_FOUND

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 28.dp else 16.dp,
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "AppCardCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = HailMotionTokens.fastSpatialOrSnap(reducedMotion),
        label = "AppCardScale",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isFrozen -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = HailMotionTokens.fastEffectsOrSnap(reducedMotion),
        label = "AppCardContainer",
    )

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isNotFound -> MaterialTheme.colorScheme.error
        grayscaleIcon && isFrozen -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val iconSize = dimensionResource(R.dimen.app_icon_size)
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = shape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                AppIconImage(
                    info = info,
                    grayscale = grayscaleIcon && isFrozen,
                    synthesizeAdaptiveIcons = synthesizeAdaptiveIcons,
                    iconSize = iconSize,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.padding_small)),
                )

                if (isFrozen && !grayscaleIcon) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AcUnit,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

                if (info.whitelisted) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(22.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = info.name.toString(),
                color = textColor,
                fontSize = homeFontSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val grayscaleColorFilter: ColorFilter by lazy {
    ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) },
    )
}

@Composable
private fun AppIconImage(
    info: AppInfo,
    grayscale: Boolean,
    synthesizeAdaptiveIcons: Boolean,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
    val appInfo = info.applicationInfo

    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = info.packageName,
        key2 = appInfo != null,
        key3 = synthesizeAdaptiveIcons,
    ) {
        value = withContext(Dispatchers.IO) {
            val loaded: Bitmap = if (appInfo != null) {
                AppIconCache.getOrLoadBitmap(context, appInfo, myUserId, iconSizePx)
            } else {
                context.packageManager.defaultActivityIcon.toBitmap(iconSizePx, iconSizePx)
            }
            loaded.asImageBitmap()
        }
    }

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap,
            contentDescription = null,
            modifier = modifier,
            colorFilter = if (grayscale) grayscaleColorFilter else null,
        )
    } else {
        Box(modifier = modifier)
    }
}
