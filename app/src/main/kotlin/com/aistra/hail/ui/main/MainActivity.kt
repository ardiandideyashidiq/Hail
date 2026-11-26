package com.aistra.hail.ui.main

import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.extensions.*
import com.aistra.hail.ui.components.BottomFadeGradient
import com.aistra.hail.ui.components.ExpressiveNavRail
import com.aistra.hail.ui.components.FloatingPillNavBar
import com.aistra.hail.ui.components.PixelPlayNavItem
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var appbar: AppBarLayout
    private val currentDestinationId = mutableIntStateOf(R.id.nav_home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = initView()
        if (!HailData.biometricLogin || BiometricManager.from(this)
                .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS
        ) return
        binding.root.isVisible = false
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    HUI.showToast(errString)
                    finishAndRemoveTask()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    binding.root.isVisible = true
                }
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.action_biometric))
            .setSubtitle(getString(R.string.msg_biometric)).setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun initView() = ActivityMainBinding.inflate(layoutInflater).apply {
        setContentView(root)
        setSupportActionBar(appBarMain.toolbar)
        appBarMain.appBarLayout.outlineProvider = null
        appBarMain.toolbar.children.filterIsInstance<TextView>().forEach {
            it.setTypeface(it.typeface, Typeface.BOLD)
        }
        fab = appBarMain.fab
        appbar = appBarMain.appBarLayout

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this@MainActivity)
        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home, R.id.nav_apps, R.id.nav_settings, R.id.nav_about
        ).build()
        setupActionBarWithNavController(navController, appBarConfiguration)

        val navItems = listOf(
            PixelPlayNavItem(
                id = R.id.nav_home,
                label = getString(R.string.title_home),
                iconRes = R.drawable.ic_round_frozen,
            ),
            PixelPlayNavItem(
                id = R.id.nav_apps,
                label = getString(R.string.title_apps),
                iconRes = R.drawable.ic_baseline_android,
            ),
            PixelPlayNavItem(
                id = R.id.nav_settings,
                label = getString(R.string.title_settings),
                iconRes = R.drawable.ic_outline_settings,
                selectedIconRes = R.drawable.ic_baseline_settings,
            ),
            PixelPlayNavItem(
                id = R.id.nav_about,
                label = getString(R.string.title_about),
                iconRes = R.drawable.ic_outline_info,
                selectedIconRes = R.drawable.ic_baseline_info,
            ),
        )

        val isRtl = isRtl
        val isLandscape = isLandscape
        val useRail = isLandscape || resources.configuration.screenWidthDp >= 600

        if (useRail && !isLandscape) {
            ConstraintSet().apply {
                clone(root)
                clear(R.id.nav_compose_view, ConstraintSet.END)
                constrainWidth(R.id.nav_compose_view, ConstraintSet.WRAP_CONTENT)
                constrainHeight(R.id.nav_compose_view, ConstraintSet.MATCH_CONSTRAINT)
                connect(R.id.nav_compose_view, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(R.id.nav_compose_view, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(R.id.nav_compose_view, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                constrainWidth(R.id.app_bar_main, ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(R.id.app_bar_main, ConstraintSet.MATCH_CONSTRAINT)
                connect(R.id.app_bar_main, ConstraintSet.START, R.id.nav_compose_view, ConstraintSet.END)
                connect(R.id.app_bar_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                applyTo(root)
            }
        }

        navComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val onNavigate: (Int) -> Unit = { itemId ->
                        if (navController.currentDestination?.id != itemId) {
                            navController.navigate(
                                itemId,
                                null,
                                NavOptions.Builder()
                                    .setLaunchSingleTop(true)
                                    .setRestoreState(true)
                                    .setEnterAnim(android.R.anim.fade_in)
                                    .setExitAnim(android.R.anim.fade_out)
                                    .setPopEnterAnim(android.R.anim.fade_in)
                                    .setPopExitAnim(android.R.anim.fade_out)
                                    .setPopUpTo(
                                        navController.graph.findStartDestination().id,
                                        inclusive = false,
                                        saveState = true,
                                    )
                                    .build(),
                            )
                        }
                    }

                    if (useRail) {
                        ExpressiveNavRail(
                            items = navItems,
                            selectedItemId = currentDestinationId.intValue,
                            onItemSelected = onNavigate,
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            BottomFadeGradient()
                            FloatingPillNavBar(
                                items = navItems,
                                selectedItemId = currentDestinationId.intValue,
                                onItemSelected = onNavigate,
                            )
                        }
                    }
                }
            }
        }

        appBarMain.appBarLayout.applyDefaultInsetter {
            paddingRelative(isRtl, start = !useRail, end = true, top = true)
        }
        fab.applyDefaultInsetter { marginRelative(isRtl, end = true, bottom = useRail) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { MenuCompat.setGroupDividerEnabled(it, true) }
        return super.onCreateOptionsMenu(menu)
    }

    fun ownerRemoveDialog() {
        MaterialAlertDialogBuilder(this).setTitle(R.string.title_remove_owner).setMessage(R.string.msg_remove_owner)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                HPolicy.setOrganizationName()
                HPolicy.removeDeviceOwner()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    /* override fun onStop() {
        super.onStop()
        if (HailData.biometricLogin) finishAndRemoveTask()
    } */

    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        currentDestinationId.intValue = destination.id
        fab.tag = destination.id == R.id.nav_home
        if (fab.tag == true) fab.show() else fab.hide()
    }
}
