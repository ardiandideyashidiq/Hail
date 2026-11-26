package com.aistra.hail.ui.home

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
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailApi.addTag
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import com.aistra.hail.work.HWork
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class HomeFragment : MainFragment(), MenuProvider {
    private var query by mutableStateOf("")
    private var multiselect by mutableStateOf(false)
    private var selectedPackages by mutableStateOf(emptySet<String>())
    private var selectedTagIndex by mutableIntStateOf(0)
    private var tagsSnapshot by mutableStateOf(HailData.tags.toList())
    private var refreshTick by mutableIntStateOf(0)
    private var isRefreshing by mutableStateOf(false)
    private var multiselectMenuItem: MenuItem? = null

    private val selectedList: List<AppInfo>
        get() = HailData.checkedList.filter { it.packageName in selectedPackages }

    private val currentTag: Pair<String, Int>?
        get() = tagsSnapshot.getOrNull(selectedTagIndex)

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
                    HomeScreen(
                        tags = tagsSnapshot,
                        selectedTagIndex = selectedTagIndex,
                        onSelectTag = { selectedTagIndex = it },
                        onTagLongClick = { index -> showTagDialog(tagPosition = index) },
                        query = query,
                        appsForTag = ::appsForTag,
                        searchResults = if (query.isEmpty()) emptyList() else searchApps(query),
                        refreshTick = refreshTick,
                        workingModeLabel = remember(refreshTick) { workingModeLabel() },
                        compactIcon = HailData.compactIcon,
                        homeFontSize = HailData.homeFontSize,
                        grayscaleIcon = HailData.grayscaleIcon,
                        synthesizeAdaptiveIcons = HailData.synthesizeAdaptiveIcons,
                        multiselect = multiselect,
                        selectedPackages = selectedPackages,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            refreshApps()
                            isRefreshing = false
                        },
                        onAppClick = ::onItemClick,
                        onAppLongClick = ::onItemLongClick,
                        onFreezeCurrent = {
                            setListFrozen(true, currentVisibleApps().filterNot { it.whitelisted })
                        },
                        onUnfreezeCurrent = {
                            setListFrozen(false, currentVisibleApps())
                        },
                        onFreezeNonWhitelisted = {
                            setListFrozen(true, HailData.checkedList.filterNot { it.whitelisted })
                        },
                        onBatchFreeze = {
                            setListFrozen(true, selectedList, false)
                            deselect()
                        },
                        onBatchUnfreeze = {
                            setListFrozen(false, selectedList, false)
                            deselect()
                        },
                        onBatchTag = { triStateTagDialog() },
                        onSelectAll = {
                            val current = currentVisibleApps().map { it.packageName }
                            selectedPackages = selectedPackages + current
                            updateBarTitle()
                        },
                        onCloseMultiselect = {
                            multiselect = false
                            multiselectMenuItem?.updateIcon()
                            deselect()
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity.fab.hide()
        refreshApps()
        updateBarTitle()
    }

    override fun onDestroyView() {
        multiselect = false
        selectedPackages = emptySet()
        multiselectMenuItem = null
        if (activity.fab.tag == true) {
            activity.fab.show()
        }
        super.onDestroyView()
    }

    private fun appsForTag(tagId: Int): List<AppInfo> =
        HailData.checkedList.filter { tagId in it.tagIdList }.sortedWith(NameComparator)

    private fun workingModeLabel(): String {
        val index = HailData.WORKING_MODE_VALUES.indexOf(HailData.workingMode)
        val entries = resources.getStringArray(R.array.working_mode_entries)
        return entries.getOrNull(index) ?: getString(R.string.mode_default)
    }

    private fun searchApps(searchQuery: String): List<AppInfo> =
        HailData.checkedList.filter {
            (HailData.nineKeySearch && NineKeySearch.search(
                searchQuery, it.packageName, it.name.toString(),
            )) || FuzzySearch.search(it.packageName, searchQuery) ||
                FuzzySearch.search(it.name.toString(), searchQuery) ||
                PinyinSearch.searchPinyinAll(it.name.toString(), searchQuery)
        }.sortedWith(NameComparator)

    private fun currentVisibleApps(): List<AppInfo> =
        if (query.isEmpty()) {
            currentTag?.second?.let(::appsForTag) ?: emptyList()
        } else {
            searchApps(query)
        }

    private fun refreshApps() {
        tagsSnapshot = HailData.tags.toList()
        if (selectedTagIndex >= tagsSnapshot.size) {
            selectedTagIndex = (tagsSnapshot.size - 1).coerceAtLeast(0)
        }
        refreshTick++
        app.setAutoFreezeService()
    }

    private fun updateBarTitle() {
        activity.supportActionBar?.title =
            if (multiselect) getString(R.string.msg_selected, selectedPackages.size.toString())
            else getString(R.string.app_name)
    }

    private fun onItemClick(info: AppInfo) {
        if (multiselect) {
            selectedPackages = if (info.packageName in selectedPackages) {
                selectedPackages - info.packageName
            } else {
                selectedPackages + info.packageName
            }
            updateBarTitle()
            return
        }
        if (info.applicationInfo == null) {
            Snackbar.make(requireView(), R.string.app_not_installed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_remove_home) { removeCheckedApp(info.packageName) }.show()
            return
        }
        launchApp(info.packageName)
    }

    private fun onItemLongClick(info: AppInfo) {
        if (info.applicationInfo == null && (!multiselect || info.packageName !in selectedPackages)) {
            exportToClipboard(listOf(info))
            return
        }
        if (info.packageName in selectedPackages) {
            onMultiSelect()
            return
        }
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        val action = getString(if (frozen) R.string.action_unfreeze else R.string.action_freeze)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            resources.getStringArray(R.array.home_action_entries).filter {
                (it != getString(R.string.action_freeze) || !frozen) &&
                    (it != getString(R.string.action_unfreeze) || frozen) &&
                    (it != getString(R.string.action_pin) || !info.pinned) &&
                    (it != getString(R.string.action_unpin) || info.pinned) &&
                    (it != getString(R.string.action_whitelist) || !info.whitelisted) &&
                    (it != getString(R.string.action_remove_whitelist) || info.whitelisted) &&
                    (it != getString(R.string.action_unfreeze_remove_home) || frozen)
            }.toTypedArray(),
        ) { _, which ->
            when (which) {
                0 -> launchApp(pkg)
                1 -> setListFrozen(!frozen, listOf(info))
                2 -> {
                    val values = resources.getIntArray(R.array.deferred_task_values)
                    val entries = arrayOfNulls<String>(values.size)
                    values.forEachIndexed { i, it ->
                        entries[i] = resources.getQuantityString(R.plurals.deferred_task_entry, it, it)
                    }
                    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_deferred_task)
                        .setItems(entries) { _, i ->
                            HWork.setDeferredFrozen(pkg, !frozen, values[i].toLong())
                            Snackbar.make(
                                requireView(),
                                resources.getQuantityString(
                                    R.plurals.msg_deferred_task, values[i], values[i], action, info.name,
                                ),
                                Snackbar.LENGTH_INDEFINITE,
                            ).setAction(R.string.action_undo) { HWork.cancelWork(pkg) }.show()
                        }.setNegativeButton(android.R.string.cancel, null).show()
                }

                3 -> {
                    info.pinned = !info.pinned
                    HailData.saveApps()
                    refreshApps()
                }

                4 -> {
                    info.whitelisted = !info.whitelisted
                    HailData.saveApps()
                    refreshApps()
                }

                5 -> tagDialog(info)

                6 -> if (tagsSnapshot.size > 1) {
                    MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_unfreeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            HShortcuts.addPinShortcut(
                                info,
                                pkg,
                                info.name,
                                HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg)
                                    .addTag(HailData.tags[index].first),
                            )
                        }.setPositiveButton(R.string.action_skip) { _, _ ->
                            HShortcuts.addPinShortcut(
                                info, pkg, info.name, HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg),
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()
                } else {
                    HShortcuts.addPinShortcut(
                        info, pkg, info.name, HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg),
                    )
                }

                7 -> exportToClipboard(listOf(info))
                8 -> removeCheckedApp(pkg)
                9 -> {
                    setListFrozen(false, listOf(info), false)
                    if (!AppManager.isAppFrozen(pkg)) removeCheckedApp(pkg)
                }
            }
        }.setNeutralButton(R.string.action_details) { _, _ ->
            HUI.startActivity(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(pkg),
            )
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun tagDialog(info: AppInfo) {
        val checkedItems = BooleanArray(HailData.tags.size) { index ->
            HailData.tags[index].second in info.tagIdList
        }
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setMultiChoiceItems(
            HailData.tags.map { it.first }.toTypedArray(), checkedItems,
        ) { _, index, isChecked ->
            checkedItems[index] = isChecked
        }.setPositiveButton(android.R.string.ok) { _, _ ->
            info.tagIdList.clear()
            checkedItems.forEachIndexed { index, checked ->
                if (checked) info.tagIdList.add(HailData.tags[index].second)
            }
            if (info.tagIdList.isEmpty()) {
                removeCheckedApp(info.packageName, false)
            }
            HailData.saveApps()
            refreshApps()
        }.setNeutralButton(R.string.action_tag_add) { _, _ ->
            showTagDialog(listOf(info))
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun deselect(update: Boolean = true) {
        selectedPackages = emptySet()
        if (!update) return
        refreshApps()
        updateBarTitle()
    }

    private fun onMultiSelect() {
        val currentSelected = selectedList
        MaterialAlertDialogBuilder(activity).setTitle(
            getString(R.string.msg_selected, currentSelected.size.toString()),
        ).setItems(
            intArrayOf(
                R.string.action_freeze,
                R.string.action_unfreeze,
                R.string.action_tag_set,
                R.string.action_export_clipboard,
                R.string.action_remove_home,
                R.string.action_unfreeze_remove_home,
            ).map { getString(it) }.toTypedArray(),
        ) { _, which ->
            when (which) {
                0 -> {
                    setListFrozen(true, currentSelected, false)
                    deselect()
                }

                1 -> {
                    setListFrozen(false, currentSelected, false)
                    deselect()
                }

                2 -> triStateTagDialog()

                3 -> {
                    exportToClipboard(currentSelected)
                    deselect()
                }

                4 -> {
                    currentSelected.forEach { removeCheckedApp(it.packageName, false) }
                    HailData.saveApps()
                    deselect()
                }

                5 -> {
                    setListFrozen(false, currentSelected, false)
                    currentSelected.forEach {
                        if (!AppManager.isAppFrozen(it.packageName)) removeCheckedApp(it.packageName, false)
                    }
                    HailData.saveApps()
                    deselect()
                }
            }
        }.setNegativeButton(R.string.action_deselect) { _, _ ->
            deselect()
        }.setNeutralButton(R.string.action_select_all) { _, _ ->
            selectedPackages = selectedPackages + currentVisibleApps().map { it.packageName }
            updateBarTitle()
            onMultiSelect()
        }.show()
    }

    private fun triStateTagDialog() {
        val currentSelected = selectedList
        val initialStates = Array(HailData.tags.size) { index ->
            val tagId = HailData.tags[index].second
            when (currentSelected.count { tagId in it.tagIdList }) {
                currentSelected.size -> ToggleableState.On
                0 -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
        val states = mutableStateListOf(*initialStates)
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent { AppTheme { TriStateTagList(initialStates, states) } }
            },
        ).setPositiveButton(android.R.string.ok) { _, _ ->
            currentSelected.forEach {
                states.forEachIndexed { index, state ->
                    val tagId = HailData.tags[index].second
                    when (state) {
                        ToggleableState.On -> {
                            if (tagId !in it.tagIdList) it.tagIdList.add(tagId)
                        }

                        ToggleableState.Off -> it.tagIdList.remove(tagId)
                        ToggleableState.Indeterminate -> {}
                    }
                }
                if (it.tagIdList.isEmpty()) removeCheckedApp(it.packageName, false)
            }
            HailData.saveApps()
            deselect()
        }.setNeutralButton(R.string.action_tag_add) { _, _ ->
            showTagDialog(currentSelected)
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    @Composable
    private fun TriStateTagList(initialStates: Array<ToggleableState>, states: MutableList<ToggleableState>) =
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            HailData.tags.forEachIndexed { index, tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            states[index] = if (initialStates[index] == ToggleableState.Indeterminate) {
                                when (states[index]) {
                                    ToggleableState.On -> ToggleableState.Off
                                    ToggleableState.Off -> ToggleableState.Indeterminate
                                    ToggleableState.Indeterminate -> ToggleableState.On
                                }
                            } else if (states[index] == ToggleableState.On) {
                                ToggleableState.Off
                            } else {
                                ToggleableState.On
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TriStateCheckbox(
                        state = states[index],
                        onClick = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary),
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = tag.first,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

    private fun launchApp(packageName: String) {
        if (AppManager.isAppFrozen(packageName) && AppManager.setAppFrozen(packageName, false)) {
            refreshApps()
        }
        if (HailData.workingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(packageName)
        }
        app.packageManager.getLaunchIntentForPackage(packageName)?.let {
            HShortcuts.addDynamicShortcut(packageName)
            startActivity(it)
        } ?: HUI.showToast(R.string.activity_not_found)
    }

    private fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo> = HailData.checkedList,
        updateList: Boolean = true,
    ) {
        if (HailData.workingMode == HailData.MODE_DEFAULT) {
            MaterialAlertDialogBuilder(activity).setMessage(R.string.msg_guide)
                .setPositiveButton(android.R.string.ok, null).show()
            return
        } else if (HailData.workingMode == HailData.MODE_SHIZUKU_HIDE) {
            runCatching { HShizuku.isRoot }.onSuccess {
                if (!it) {
                    MaterialAlertDialogBuilder(activity).setMessage(R.string.shizuku_hide_adb)
                        .setPositiveButton(android.R.string.ok, null).show()
                    return
                }
            }
        }
        val filtered = list.filter { AppManager.isAppFrozen(it.packageName) != frozen }
        when (val result = AppManager.setListFrozen(frozen, *filtered.toTypedArray())) {
            null -> HUI.showToast(R.string.permission_denied)
            else -> {
                if (updateList) refreshApps()
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, result,
                )
            }
        }
    }

    private fun showTagDialog(list: List<AppInfo>? = null, tagPosition: Int = selectedTagIndex) {
        val binding = DialogInputBinding.inflate(layoutInflater)
        binding.inputLayout.setHint(R.string.tag)
        val targetTag = HailData.tags.getOrNull(tagPosition)
        if (list == null) {
            binding.editText.setText(targetTag?.first ?: "")
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(if (list != null) R.string.action_tag_add else R.string.action_tag_set)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val tagName = binding.editText.text.toString()
                val tagId = tagName.hashCode()
                if (HailData.tags.any { it.first == tagName || it.second == tagId }) return@setPositiveButton
                if (list != null) {
                    HailData.tags.add(tagName to tagId)
                    HailData.saveTags()
                    refreshApps()
                    if (selectedPackages.isNotEmpty() && multiselect) {
                        triStateTagDialog()
                    } else if (list.isNotEmpty()) {
                        tagDialog(list.first())
                    }
                } else if (tagPosition in HailData.tags.indices) {
                    val defaultTab = tagPosition == 0
                    val oldTagId = HailData.tags[tagPosition].second
                    HailData.tags[tagPosition] = tagName to if (defaultTab) 0 else tagId
                    if (!defaultTab) {
                        HailData.checkedList.forEach {
                            val index = it.tagIdList.indexOf(oldTagId)
                            if (index != -1) it.tagIdList[index] = tagId
                        }
                        HailData.saveApps()
                    }
                    HailData.saveTags()
                    refreshApps()
                }
            }.apply {
                if (list != null || tagPosition == 0 || tagPosition !in HailData.tags.indices) return@apply
                setNeutralButton(R.string.action_tag_remove) { _, _ ->
                    val tagIdToRemove = HailData.tags[tagPosition].second
                    HailData.checkedList.toList().forEach {
                        if (it.tagIdList.remove(tagIdToRemove) && it.tagIdList.isEmpty()) {
                            removeCheckedApp(it.packageName, false)
                        }
                    }
                    HailData.tags.removeAt(tagPosition)
                    HailData.saveApps()
                    HailData.saveTags()
                    refreshApps()
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun exportToClipboard(list: List<AppInfo>) {
        if (list.isEmpty()) return
        HUI.copyText(
            if (list.size > 1) {
                JSONArray().run {
                    list.forEach { put(it.packageName) }
                    toString()
                }
            } else {
                list[0].packageName
            },
        )
        HUI.showToast(
            R.string.msg_exported, if (list.size > 1) list.size.toString() else list[0].name,
        )
    }

    private fun importFromClipboard() = runCatching {
        val str = HUI.pasteText() ?: throw IllegalArgumentException()
        val json = if (str.contains('[')) {
            JSONArray(str.substring(str.indexOf('[')..str.indexOf(']', str.indexOf('['))))
        } else {
            JSONArray().put(str)
        }
        var i = 0
        for (index in 0 until json.length()) {
            val pkg = json.getString(index)
            if (HPackages.getApplicationInfoOrNull(pkg) != null && !HailData.isChecked(pkg)) {
                HailData.addCheckedApp(pkg, currentTag?.second ?: 0, false)
                i++
            }
        }
        if (i > 0) {
            HailData.saveApps()
            refreshApps()
        }
        HUI.showToast(getString(R.string.msg_imported, i.toString()))
    }

    private suspend fun importFrozenApp() = withContext(Dispatchers.IO) {
        HPackages.getInstalledApplications().map { it.packageName }
            .filter { AppManager.isAppFrozen(it) && !HailData.isChecked(it) }
            .onEach { HailData.addCheckedApp(it, currentTag?.second ?: 0, false) }.size
    }

    private fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        HailData.removeCheckedApp(packageName, saveApps)
        if (saveApps) refreshApps()
    }

    private fun MenuItem.updateIcon() = icon?.setTint(
        MaterialColors.getColor(
            activity.findViewById(R.id.toolbar),
            if (multiselect) androidx.appcompat.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOnSurface,
        ),
    )

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_multiselect -> {
                multiselect = !multiselect
                item.updateIcon()
                if (multiselect) {
                    updateBarTitle()
                    HUI.showToast(R.string.tap_to_select)
                } else {
                    deselect()
                }
            }

            R.id.action_freeze_current ->
                setListFrozen(true, currentVisibleApps().filterNot { it.whitelisted })

            R.id.action_unfreeze_current ->
                setListFrozen(false, currentVisibleApps())

            R.id.action_freeze_all -> setListFrozen(true)
            R.id.action_unfreeze_all -> setListFrozen(false)
            R.id.action_freeze_non_whitelisted ->
                setListFrozen(true, HailData.checkedList.filterNot { it.whitelisted })

            R.id.action_import_clipboard -> importFromClipboard()
            R.id.action_import_frozen -> lifecycleScope.launch {
                val size = importFrozenApp()
                if (size > 0) {
                    HailData.saveApps()
                    refreshApps()
                }
                HUI.showToast(getString(R.string.msg_imported, size.toString()))
            }

            R.id.action_export_current -> exportToClipboard(currentVisibleApps())
            R.id.action_export_all -> exportToClipboard(HailData.checkedList)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        if (HailData.nineKeySearch) {
            val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            editText.inputType = InputType.TYPE_CLASS_PHONE
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (query != newText) {
                    query = newText
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })
        multiselectMenuItem = menu.findItem(R.id.action_multiselect).apply { updateIcon() }
    }
}
