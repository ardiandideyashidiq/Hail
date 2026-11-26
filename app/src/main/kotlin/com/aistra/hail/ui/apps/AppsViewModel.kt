package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppsQuickFilter {
    USER,
    SYSTEM,
    FROZEN,
    UNFROZEN,
}

data class AppsFilterState(
    val userApps: Boolean = HailData.filterUserApps,
    val systemApps: Boolean = HailData.filterSystemApps,
    val frozenApps: Boolean = HailData.filterFrozenApps,
    val unfrozenApps: Boolean = HailData.filterUnfrozenApps,
    val sortBy: String = HailData.sortBy ?: HailData.SORT_NAME,
) {
    val activeQuickFilter: AppsQuickFilter
        get() = when {
            frozenApps && !unfrozenApps -> AppsQuickFilter.FROZEN
            !frozenApps && unfrozenApps -> AppsQuickFilter.UNFROZEN
            systemApps -> AppsQuickFilter.SYSTEM
            else -> AppsQuickFilter.USER
        }
}

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<ApplicationInfo>>(emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _displayApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    val displayApps: StateFlow<List<ApplicationInfo>> = _displayApps.asStateFlow()

    private val _checkedPackages = MutableStateFlow(readCheckedPackages())
    val checkedPackages: StateFlow<Set<String>> = _checkedPackages.asStateFlow()

    private val _filterState = MutableStateFlow(readFilterState())
    val filterState: StateFlow<AppsFilterState> = _filterState.asStateFlow()

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null
    private var filterJob: Job? = null

    init {
        updateAppList()
    }

    private fun readCheckedPackages(): Set<String> =
        HailData.checkedList.mapTo(HashSet()) { it.packageName }

    private fun readFilterState(): AppsFilterState = AppsFilterState(
        userApps = HailData.filterUserApps,
        systemApps = HailData.filterSystemApps,
        frozenApps = HailData.filterFrozenApps,
        unfrozenApps = HailData.filterUnfrozenApps,
        sortBy = HailData.sortBy ?: HailData.SORT_NAME,
    )

    fun syncCheckedPackages() {
        _checkedPackages.value = readCheckedPackages()
    }

    fun toggleChecked(packageName: String) {
        if (HailData.isChecked(packageName)) {
            HailData.removeCheckedApp(packageName)
        } else {
            HailData.addCheckedApp(packageName)
        }
        syncCheckedPackages()
    }

    /**
     * Delaying changes to the refreshing state prevents the progress indicator from flickering.
     */
    private fun postRefreshState(state: Boolean, delayTime: Long = 200L) {
        if (!state) {
            refreshStateJob?.cancel()
            _isRefreshing.value = false
        } else if (refreshStateJob == null || refreshStateJob!!.isCompleted) {
            refreshStateJob = viewModelScope.launch {
                delay(delayTime)
                _isRefreshing.value = true
            }
        }
    }

    fun postQuery(text: String, delayTime: Long = 300L) {
        refreshJob?.cancel()
        if (delayTime == 0L) {
            _query.value = text
            updateDisplayAppList()
        } else {
            refreshJob = viewModelScope.launch {
                delay(delayTime)
                _query.value = text
                updateDisplayAppList()
            }
        }
    }

    /**
     * Refreshes all installed applications and updates the filtered display list.
     */
    fun updateAppList() {
        viewModelScope.launch {
            postRefreshState(true)
            val installed = withContext(Dispatchers.IO) {
                HPackages.getInstalledApplications()
            }
            _apps.value = installed
            syncCheckedPackages()
            _displayApps.value = filterList(installed, _query.value)
            postRefreshState(false)
        }
    }

    /**
     * Filters and sorts the loaded applications into [displayApps].
     */
    fun updateDisplayAppList() {
        val currentApps = _apps.value
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            postRefreshState(true)
            _displayApps.value = filterList(currentApps, _query.value)
            postRefreshState(false)
        }
    }

    fun changeAppsSort(sort: String) {
        HailData.changeAppsSort(sort)
        _filterState.value = readFilterState()
        updateDisplayAppList()
    }

    fun applyQuickFilter(preset: AppsQuickFilter) {
        when (preset) {
            AppsQuickFilter.USER -> {
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, false)
                HailData.changeAppsFilter(HailData.FILTER_FROZEN_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, true)
            }
            AppsQuickFilter.SYSTEM -> {
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, false)
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_FROZEN_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, true)
            }
            AppsQuickFilter.FROZEN -> {
                HailData.changeAppsFilter(HailData.FILTER_FROZEN_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, false)
            }
            AppsQuickFilter.UNFROZEN -> {
                HailData.changeAppsFilter(HailData.FILTER_FROZEN_APPS, false)
                HailData.changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, true)
            }
        }
        _filterState.value = readFilterState()
        updateDisplayAppList()
    }

    fun changeAppsFilter(filter: String) {
        when (filter) {
            HailData.FILTER_USER_APPS -> {
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, false)
            }
            HailData.FILTER_SYSTEM_APPS -> {
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, false)
            }
            HailData.FILTER_FROZEN_APPS -> {
                HailData.changeAppsFilter(HailData.FILTER_FROZEN_APPS, !HailData.filterFrozenApps)
            }
            HailData.FILTER_UNFROZEN_APPS -> {
                HailData.changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, !HailData.filterUnfrozenApps)
            }
        }
        _filterState.value = readFilterState()
        updateDisplayAppList()
    }

    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    private val ApplicationInfo.isAppFrozen: Boolean
        get() = AppManager.isAppFrozen(packageName)

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?,
    ): List<ApplicationInfo> {
        val pm = getApplication<HailApp>().packageManager
        return withContext(Dispatchers.Default) {
            appList.filter {
                ((HailData.filterUserApps && !it.isSystemApp)
                    || (HailData.filterSystemApps && it.isSystemApp))
                    && ((HailData.filterFrozenApps && it.isAppFrozen)
                    || (HailData.filterUnfrozenApps && !it.isAppFrozen))
                    && ((HailData.nineKeySearch
                    && NineKeySearch.search(query, it.packageName, it.loadLabel(pm).toString()))
                    || FuzzySearch.search(it.packageName, query)
                    || FuzzySearch.search(it.loadLabel(pm).toString(), query)
                    || PinyinSearch.searchPinyinAll(it.loadLabel(pm).toString(), query))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)
                            ?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }
    }
}
