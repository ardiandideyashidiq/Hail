package com.aistra.hail.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppShapes
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.ui.theme.HailMotionTokens
import com.aistra.hail.ui.theme.rememberReducedMotion
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import java.text.SimpleDateFormat

class AboutFragment : MainFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AboutScreen(HPackages.getUnhiddenPackageInfoOrNull(app.packageName)!!.firstInstallTime)
                }
            }
        }

    @Preview(showBackground = true)
    @Composable
    fun PreviewAboutScreen() = AppTheme { AboutScreen(System.currentTimeMillis()) }

    @Composable
    private fun AboutScreen(installTime: Long) {
        var openLicenseDialog by remember { mutableStateOf(false) }
        var openDonateDialog by remember { mutableStateOf(false) }
        var openWeChatQrDialog by remember { mutableStateOf(false) }
        val reducedMotion = rememberReducedMotion()
        val heroInteractionSource = remember { MutableInteractionSource() }
        val isHeroPressed by heroInteractionSource.collectIsPressedAsState()
        val heroScale by animateFloatAsState(
            targetValue = if (isHeroPressed && !reducedMotion) 0.97f else 1f,
            animationSpec = if (reducedMotion) snap() else HailMotionTokens.FastSpatial,
            label = "HeroCardPressScale",
        )

        if (openLicenseDialog) LicenseDialog { openLicenseDialog = false }
        if (openDonateDialog) {
            DonateDialog(
                onDismiss = { openDonateDialog = false },
                onWeChatSelected = { openWeChatQrDialog = true },
            )
        }
        if (openWeChatQrDialog) WeChatQrDialog { openWeChatQrDialog = false }

        Column(
            modifier = Modifier.verticalScroll(state = rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            Card(
                onClick = { HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.header_height))
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                    .graphicsLayer {
                        scaleX = heroScale
                        scaleY = heroScale
                    },
                shape = AppShapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                interactionSource = heroInteractionSource,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.padding_extra_small), Alignment.CenterVertically
                    ), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).background(Color.White, CircleShape),
                        contentScale = ContentScale.None
                    )
                    Text(
                        text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.app_slogan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            AboutActionCard {
                ClickableItem(
                    icon = Icons.Outlined.Update,
                    title = R.string.label_version,
                    desc = HailData.VERSION,
                    reducedMotion = reducedMotion,
                ) { HUI.openLink(HailData.URL_RELEASES) }
                ClickableItem(
                    icon = Icons.Outlined.InstallMobile,
                    title = R.string.label_time,
                    desc = SimpleDateFormat.getDateInstance().format(installTime),
                    reducedMotion = reducedMotion,
                ) { HUI.showToast("\uD83E\uDD76\uD83D\uDCA8\uD83D\uDC09") }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            AboutActionCard {
                ClickableItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = R.string.action_telegram,
                    reducedMotion = reducedMotion,
                ) { HUI.openLink(HailData.URL_TELEGRAM) }
                ClickableItem(
                    icon = Icons.Outlined.Group,
                    title = R.string.action_qq,
                    reducedMotion = reducedMotion,
                ) { HUI.openLink(HailData.URL_QQ) }
                ClickableItem(
                    icon = Icons.Outlined.LocalMall,
                    title = R.string.action_fdroid,
                    reducedMotion = reducedMotion,
                ) { HUI.openLink(HailData.URL_FDROID) }
                ClickableItem(
                    icon = Icons.Outlined.Coffee,
                    title = R.string.action_donate,
                    reducedMotion = reducedMotion,
                ) { openDonateDialog = true }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            AboutActionCard {
                ClickableItem(
                    icon = Icons.Outlined.Code,
                    title = R.string.action_github,
                    reducedMotion = reducedMotion,
                ) { HUI.openLink(HailData.URL_GITHUB) }
                ClickableItem(
                    icon = Icons.Outlined.Translate,
                    title = R.string.action_translate,
                    reducedMotion = reducedMotion,
                ) { HUI.openLink(HailData.URL_TRANSLATE) }
                ClickableItem(
                    icon = Icons.Outlined.Description,
                    title = R.string.action_licenses,
                    reducedMotion = reducedMotion,
                ) { openLicenseDialog = true }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        }
    }

    @Composable
    private fun AboutActionCard(content: @Composable ColumnScope.() -> Unit) = Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        content = content,
    )

    @Composable
    private fun ClickableItem(
        icon: ImageVector,
        @StringRes title: Int,
        desc: String? = null,
        reducedMotion: Boolean = rememberReducedMotion(),
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed && !reducedMotion) 0.98f else 1f,
            animationSpec = if (reducedMotion) snap() else HailMotionTokens.FastSpatial,
            label = "AboutItemPressScale",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(if (desc == null) R.dimen.padding_medium else R.dimen.padding_large)
                ),
            )
            Column {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (desc != null) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun LicenseDialog(onDismiss: () -> Unit) = AlertDialog(
        shape = AppShapes.extraLarge,
        title = { Text(text = stringResource(R.string.action_licenses)) },
        text = {
            SelectionContainer {
                Text(
                    text = buildAnnotatedString {
                        val lines = resources.openRawResource(R.raw.licenses).bufferedReader().readLines()
                        lines.forEach {
                            if (it.isNotBlank()) withLink(
                                LinkAnnotation.Url(
                                    it.substringAfter(": "),
                                    TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                                )
                            ) {
                                append(it.substringBefore(": "))
                            }
                            if (it != lines.last()) append("\n\n")
                        }
                    }, modifier = Modifier.verticalScroll(state = rememberScrollState())
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.ok)) } })

    @Composable
    private fun DonateDialog(
        onDismiss: () -> Unit,
        onWeChatSelected: () -> Unit,
    ) {
        val entries = stringArrayResource(R.array.donate_payment_entries)
        AlertDialog(
            shape = AppShapes.extraLarge,
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.title_donate)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    entries.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.medium)
                                .clickable(role = Role.RadioButton) {
                                    onDismiss()
                                    when (index) {
                                        0 -> if (HUI.openLink(HailData.URL_ALIPAY_API).not()) {
                                            HUI.openLink(HailData.URL_ALIPAY)
                                        }
                                        1 -> onWeChatSelected()
                                        2 -> HUI.openLink(HailData.URL_KOFI)
                                        3 -> HUI.openLink(HailData.URL_LIBERAPAY)
                                        4 -> HUI.openLink(HailData.URL_PAYPAL)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == 0, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
            },
        )
    }

    @Composable
    private fun WeChatQrDialog(onDismiss: () -> Unit) = AlertDialog(
        shape = AppShapes.extraLarge,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.title_donate)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.qr_wechat),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = dimensionResource(R.dimen.padding_large))
                        .clip(AppShapes.medium),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                app.packageManager.getLaunchIntentForPackage("com.tencent.mm")?.let {
                    it.putExtra("LauncherUI.From.Scaner.Shortcut", true)
                    startActivity(it)
                } ?: HUI.showToast(R.string.app_not_installed)
            }) {
                Text(text = stringResource(R.string.donate_wechat_scan))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        },
    )
}