package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.extensions.exportFileName
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class AppsFragment : MainFragment(), MenuProvider {

    private val model: AppsViewModel by viewModels()

    private var exportApkPkg: String? = null
    private var isExportingApk by mutableStateOf(false)
    private var pendingSystemFilterConfirm by mutableStateOf(false)
    private var pendingUninstallApp by mutableStateOf<Pair<String, String>?>(null)

    private val exportApk =
        registerForActivityResult(CreateDocument("application/vnd.android.package-archive")) { uri ->
            val pkg = exportApkPkg
            exportApkPkg = null
            if (pkg == null || uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val applicationInfo = HPackages.getApplicationInfoOrNull(pkg) ?: return@launch
                isExportingApk = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        FileInputStream(applicationInfo.sourceDir).use { source ->
                            activity.contentResolver.openOutputStream(uri, "rwt").use { target ->
                                if (target == null) return@withContext
                                HFiles.copy(source, target)
                            }
                        }
                    }
                }.onSuccess {
                    HUI.showToast(R.string.msg_extract_apk, uri.toString())
                }.onFailure {
                    HUI.showToast(R.string.operation_failed, it.localizedMessage ?: "Unknown", true)
                }
                isExportingApk = false
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val apps by model.displayApps.collectAsState()
                    val isRefreshing by model.isRefreshing.collectAsState()
                    val checkedPackages by model.checkedPackages.collectAsState()
                    val filterState by model.filterState.collectAsState()

                    AppsScreen(
                        apps = apps,
                        isRefreshing = isRefreshing,
                        checkedPackages = checkedPackages,
                        filterState = filterState,
                        isExportingApk = isExportingApk,
                        onRefresh = { model.updateAppList() },
                        onQuickFilterSelected = { preset ->
                            if (preset == AppsQuickFilter.SYSTEM && !HailData.filterSystemApps) {
                                pendingSystemFilterConfirm = true
                            } else {
                                model.applyQuickFilter(preset)
                                activity.invalidateMenu()
                            }
                        },
                        onToggleChecked = { pkg -> model.toggleChecked(pkg) },
                        onActionDetails = { info ->
                            HUI.startActivity(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                HPackages.packageUri(info.packageName),
                            )
                        },
                        onActionLaunch = { info -> launchApp(info) },
                        onActionToggleFreeze = { info, frozen ->
                            toggleFreezeApp(info, frozen)
                        },
                        onActionExportClipboard = { info ->
                            HUI.copyText(info.packageName)
                            HUI.showToast(R.string.msg_text_copied, info.packageName)
                        },
                        onActionExtractApk = { info -> extractApk(info.packageName) },
                        onActionUninstall = { info ->
                            uninstallApp(info.loadLabel(app.packageManager), info.packageName)
                        },
                        onActionReinstall = { info ->
                            val name = info.loadLabel(app.packageManager)
                            if (AppManager.reinstallApp(info.packageName)) {
                                model.updateAppList()
                            } else {
                                HUI.showToast(R.string.operation_failed, name)
                            }
                        },
                    )

                    if (pendingSystemFilterConfirm) {
                        AppsConfirmDialog(
                            title = null,
                            message = stringResource(R.string.freeze_system_app),
                            confirmText = stringResource(R.string.action_continue),
                            onConfirm = {
                                pendingSystemFilterConfirm = false
                                model.applyQuickFilter(AppsQuickFilter.SYSTEM)
                                activity.invalidateMenu()
                            },
                            onDismiss = { pendingSystemFilterConfirm = false },
                        )
                    }

                    pendingUninstallApp?.let { (name, pkg) ->
                        AppsConfirmDialog(
                            title = name,
                            message = stringResource(R.string.msg_uninstall),
                            confirmText = stringResource(android.R.string.ok),
                            onConfirm = {
                                pendingUninstallApp = null
                                if (AppManager.uninstallApp(pkg)) model.updateAppList()
                            },
                            onDismiss = { pendingUninstallApp = null },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.syncCheckedPackages()
    }

    private fun launchApp(info: ApplicationInfo) {
        val pkg = info.packageName
        if (AppManager.isAppFrozen(pkg)) {
            AppManager.setAppFrozen(pkg, false)
        }
        val intent = app.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
        } else {
            HUI.showToast(R.string.operation_failed, info.loadLabel(app.packageManager))
        }
    }

    private fun toggleFreezeApp(info: ApplicationInfo, frozen: Boolean) {
        val pkg = info.packageName
        if (AppManager.setAppFrozen(pkg, frozen)) {
            model.updateAppList()
        } else {
            HUI.showToast(R.string.operation_failed, info.loadLabel(app.packageManager))
        }
    }

    private fun extractApk(pkg: String) {
        exportApkPkg = pkg
        exportApk.launch(HPackages.getUnhiddenPackageInfoOrNull(pkg)?.exportFileName ?: pkg)
    }

    private fun uninstallApp(name: CharSequence, pkg: String) {
        when {
            HPackages.isAppUninstalled(pkg) -> HUI.showToast(R.string.app_not_installed)

            pkg == app.packageName -> {
                when {
                    HPolicy.isDeviceOwnerActive -> activity.ownerRemoveDialog()
                    HPolicy.isProfileOwner -> HPolicy.removeProfileOwner()
                    HPolicy.isAdminActive -> HPolicy.removeActiveAdmin()
                    else -> pendingUninstallApp = name.toString() to pkg
                }
            }

            HailData.workingMode == HailData.MODE_DEFAULT -> AppManager.uninstallApp(pkg)
            else -> pendingUninstallApp = name.toString() to pkg
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_apps, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        if (HailData.nineKeySearch) {
            val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            editText.inputType = InputType.TYPE_CLASS_PHONE
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                model.postQuery(newText, if (newText.isEmpty()) 0L else 300L)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                model.postQuery(query, 0L)
                return true
            }
        })
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(
            when (HailData.sortBy) {
                HailData.SORT_INSTALL -> R.id.sort_by_install
                HailData.SORT_UPDATE -> R.id.sort_by_update
                else -> R.id.sort_by_name
            },
        ).isChecked = true
        menu.findItem(
            if (HailData.filterSystemApps) R.id.filter_system_apps else R.id.filter_user_apps,
        ).isChecked = true
        menu.findItem(R.id.filter_frozen_apps).isChecked = HailData.filterFrozenApps
        menu.findItem(R.id.filter_unfrozen_apps).isChecked = HailData.filterUnfrozenApps
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_by_name -> changeAppsSort(HailData.SORT_NAME, item)
            R.id.sort_by_install -> changeAppsSort(HailData.SORT_INSTALL, item)
            R.id.sort_by_update -> changeAppsSort(HailData.SORT_UPDATE, item)
            R.id.filter_user_apps -> changeAppsFilter(HailData.FILTER_USER_APPS, item)
            R.id.filter_system_apps -> pendingSystemFilterConfirm = true
            R.id.filter_frozen_apps -> changeAppsFilter(HailData.FILTER_FROZEN_APPS, item)
            R.id.filter_unfrozen_apps -> changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, item)
            else -> return false
        }
        return true
    }

    private fun changeAppsSort(sort: String, item: MenuItem) {
        item.isChecked = true
        model.changeAppsSort(sort)
    }

    private fun changeAppsFilter(filter: String, item: MenuItem) {
        when (item.itemId) {
            R.id.filter_user_apps -> {
                item.isChecked = true
                model.changeAppsFilter(filter)
            }

            R.id.filter_system_apps -> {
                item.isChecked = true
                model.changeAppsFilter(filter)
            }

            else -> {
                item.isChecked = !item.isChecked
                model.changeAppsFilter(filter)
            }
        }
    }
}
