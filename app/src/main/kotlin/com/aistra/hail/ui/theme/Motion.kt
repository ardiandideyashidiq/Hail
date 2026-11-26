package com.aistra.hail.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Material 3 Expressive spring physics tokens.
 *
 * Spatial springs (dampingRatio = 0.9f) are used for position, size, and shape morphs.
 * Effects springs (dampingRatio = 1.0f, no overshoot) are used for color, alpha, and elevation.
 */
object HailMotionTokens {
    const val SpatialDampingRatio = 0.9f
    const val EffectsDampingRatio = 1.0f

    const val FastSpatialStiffness = 1400f
    const val DefaultSpatialStiffness = 700f
    const val SlowSpatialStiffness = 300f

    const val FastEffectsStiffness = 3800f
    const val DefaultEffectsStiffness = 1600f
    const val SlowEffectsStiffness = 800f

    val FastSpatial: SpringSpec<Float> = spring(
        dampingRatio = SpatialDampingRatio,
        stiffness = FastSpatialStiffness,
    )

    val DefaultSpatial: SpringSpec<Float> = spring(
        dampingRatio = SpatialDampingRatio,
        stiffness = DefaultSpatialStiffness,
    )

    val SlowSpatial: SpringSpec<Float> = spring(
        dampingRatio = SpatialDampingRatio,
        stiffness = SlowSpatialStiffness,
    )

    val FastEffects: SpringSpec<Float> = spring(
        dampingRatio = EffectsDampingRatio,
        stiffness = FastEffectsStiffness,
    )

    val DefaultEffects: SpringSpec<Float> = spring(
        dampingRatio = EffectsDampingRatio,
        stiffness = DefaultEffectsStiffness,
    )

    val SlowEffects: SpringSpec<Float> = spring(
        dampingRatio = EffectsDampingRatio,
        stiffness = SlowEffectsStiffness,
    )

    fun <T> fastSpatial(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = SpatialDampingRatio,
        stiffness = FastSpatialStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> defaultSpatial(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = SpatialDampingRatio,
        stiffness = DefaultSpatialStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> slowSpatial(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = SpatialDampingRatio,
        stiffness = SlowSpatialStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> fastEffects(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EffectsDampingRatio,
        stiffness = FastEffectsStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> defaultEffects(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EffectsDampingRatio,
        stiffness = DefaultEffectsStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> slowEffects(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EffectsDampingRatio,
        stiffness = SlowEffectsStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> fastSpatialOrSnap(
        reducedMotion: Boolean,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else fastSpatial(visibilityThreshold)

    fun <T> defaultSpatialOrSnap(
        reducedMotion: Boolean,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else defaultSpatial(visibilityThreshold)

    fun <T> slowSpatialOrSnap(
        reducedMotion: Boolean,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else slowSpatial(visibilityThreshold)

    fun <T> fastEffectsOrSnap(
        reducedMotion: Boolean,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else fastEffects(visibilityThreshold)

    fun <T> defaultEffectsOrSnap(
        reducedMotion: Boolean,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else defaultEffects(visibilityThreshold)

    fun <T> slowEffectsOrSnap(
        reducedMotion: Boolean,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else slowEffects(visibilityThreshold)
}

/**
 * Returns `snap()` when [reducedMotion] is true, otherwise returns this [FiniteAnimationSpec].
 */
fun <T> FiniteAnimationSpec<T>.orSnap(reducedMotion: Boolean): FiniteAnimationSpec<T> =
    if (reducedMotion) snap() else this

/**
 * Observes the system animator duration scale and returns `true` when animations are disabled
 * (`ANIMATOR_DURATION_SCALE == 0f`).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    var reducedMotion by remember(context) {
        mutableStateOf(isReducedMotionEnabled(context))
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = isReducedMotionEnabled(context)
            }
        }
        runCatching {
            resolver.registerContentObserver(uri, false, observer)
        }
        onDispose {
            runCatching {
                resolver.unregisterContentObserver(observer)
            }
        }
    }

    return reducedMotion
}

private fun isReducedMotionEnabled(context: Context): Boolean =
    runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)
