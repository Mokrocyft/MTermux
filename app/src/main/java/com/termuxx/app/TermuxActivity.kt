package com.termuxx.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.ActionMode
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.TextAttribute
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.termuxx.R
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Material 3 Expressive Subdued Dark AMOLED Palette
private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
private const val COLOR_SHEET_BG = 0xFF0B0E14.toInt()
private const val COLOR_CARD_BG = 0xFF11161F.toInt()
private const val COLOR_SURFACE = 0xFF0C0F14.toInt()
private const val COLOR_SURFACE_PRESSED = 0xFF161C26.toInt()
private const val COLOR_BORDER = 0xFF191E28.toInt()
private const val COLOR_TEXT = 0xFFB8C0CC.toInt()
private const val COLOR_TEXT_BRIGHT = 0xFFE2E8F0.toInt()
private const val COLOR_TEXT_DIM = 0xFF6E7785.toInt()
private const val COLOR_ROOT = 0xFFD97B73.toInt()
private const val COLOR_ROOT_CONTAINER = 0xFF2B1212.toInt()

// Native Selection Handle Palette
private const val COLOR_HANDLE_STEM = 0xFFC4C6CC.toInt()
private const val COLOR_HANDLE_RING = 0xFF9EA0A8.toInt()
private const val COLOR_HANDLE_FILL = 0xFFE8EAEF.toInt()

private const val PREFS_NAME = "termuxx_prefs"
private const val KEY_EXTRA_KEYS_SPEC = "extra_keys_spec_v2"
private const val KEY_UI_THEME_INDEX = "ui_theme_index_v1"
private const val KEY_FONT_SCALE = "font_scale_v1"

// Precise Cursor & Animation Preference Keys
private const val KEY_CURSOR_SHAPE = "cursor_shape_v2"
private const val KEY_CURSOR_BLINK_STYLE = "cursor_blink_style_v2" // 0: VS Code Smooth, 1: Phase, 2: Expand, 3: Blink, 4: Solid
private const val KEY_CURSOR_ALPHA_PCT = "cursor_alpha_pct_v2" // 15..100 (default 85)
private const val KEY_CURSOR_RADIUS_TENTHS = "cursor_radius_tenths_v2" // 0..60 -> 0.0..6.0 dp (default 30 -> 3.0 dp)
private const val KEY_CURSOR_FADE_MS = "cursor_fade_ms_v2" // 200..1200 ms half-cycle (default 500 ms)
private const val KEY_CURSOR_SMOOTH_MOVE = "cursor_smooth_move_v1"
private const val KEY_CURSOR_GLIDE_MS = "cursor_glide_ms_v2" // 30..220 ms (default 80 ms)

// Tactile Haptics Preference Keys
private const val KEY_HAPTIC_ENABLED = "haptic_enabled_v1"
private const val KEY_HAPTIC_AMPLITUDE = "haptic_amplitude_v1" // 1..255 (default 85)
private const val KEY_HAPTIC_DURATION_MS = "haptic_duration_ms_v1" // 1..40 ms (default 7 ms)

// Snapdragon 8 Elite Gen 5 Oryon Performance Mode
private const val KEY_ORYON_PERF_MODE = "oryon_perf_mode_v1" // 0: Smart Boost, 1: Prime Max, 2: Eco

private const val DEFAULT_UI_THEME_INDEX = 0
private const val DEFAULT_CURSOR_SHAPE = 0 // 0: Block, 1: Underline, 2: Bar, 3: Outline Block
private const val DEFAULT_CURSOR_BLINK_STYLE = 0 // VS Code Smooth
private const val DEFAULT_CURSOR_ALPHA_PCT = 85
private const val DEFAULT_CURSOR_RADIUS_TENTHS = 30 // 3.0 dp
private const val DEFAULT_CURSOR_FADE_MS = 500
private const val DEFAULT_CURSOR_SMOOTH_MOVE = true
private const val DEFAULT_CURSOR_GLIDE_MS = 80

private const val DEFAULT_HAPTIC_ENABLED = true
private const val DEFAULT_HAPTIC_AMPLITUDE = 85
private const val DEFAULT_HAPTIC_DURATION_MS = 7
private const val DEFAULT_ORYON_PERF_MODE = 0

private const val REQ_POST_NOTIFICATIONS = 1001

private const val DEFAULT_EXTRA_KEYS_SPEC =
    "ESC|TAB|CTRL|ALT|-|/|▲|▼|◀|▶"

private const val EXTENDED_EXTRA_KEYS_SPEC =
    "ESC|/|\\|-|HOME|▲|END|PGUP\nTAB|CTRL|ALT|_|◀|▼|▶|PGDN"

private const val VIM_TMUX_EXTRA_KEYS_SPEC =
    "ESC|:|CTRL|ALT|\"|%|h|j|k|l"

private const val ITEM_COPY = 1
private const val ITEM_PASTE = 2
private const val ITEM_SELECT_ALL = 3
private const val ITEM_SHARE = 4

private data class UiThemePreset(
    val name: String,
    val accent: Int,
    val container: Int
)

private val UI_THEMES = arrayOf(
    UiThemePreset("Синий", 0xFF6E94CC.toInt(), 0xFF111926.toInt()),
    UiThemePreset("Изумрудный", 0xFF569E94.toInt(), 0xFF0E1F1D.toInt()),
    UiThemePreset("Фиолетовый", 0xFF8E76B8.toInt(), 0xFF191424.toInt()),
    UiThemePreset("Янтарный", 0xFFC2965B.toInt(), 0xFF241B10.toInt()),
    UiThemePreset("Графитовый", 0xFF8C96A6.toInt(), 0xFF16191F.toInt())
)

private data class DpkgPackageInfo(
    val name: String,
    val version: String,
    val sizeKb: Long,
    val description: String
)

class TermuxActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootLayout: LinearLayout
    private lateinit var headerBar: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var extraKeysRoot: LinearLayout
    internal lateinit var terminalSurface: TerminalSurface
    private val modifierButtons = ArrayList<TextView>(4)

    internal var uiThemeIndex = DEFAULT_UI_THEME_INDEX
    internal val uiAccent: Int
        get() = UI_THEMES[uiThemeIndex.coerceIn(0, UI_THEMES.lastIndex)].accent
    internal val uiAccentContainer: Int
        get() = UI_THEMES[uiThemeIndex.coerceIn(0, UI_THEMES.lastIndex)].container

    internal var hapticEnabled = DEFAULT_HAPTIC_ENABLED
    internal var hapticAmplitude = DEFAULT_HAPTIC_AMPLITUDE
    internal var hapticDurationMs = DEFAULT_HAPTIC_DURATION_MS
    internal var oryonPerfMode = DEFAULT_ORYON_PERF_MODE

    // File manager clipboard state (copy / cut)
    private var fileClipSource: File? = null
    private var fileClipIsMove: Boolean = false

    private var ctrlLocked = false
    private var altLocked = false

    private val backCallback = OnBackInvokedCallback {
        if (terminalSurface.isSelectingText()) {
            terminalSurface.stopTextSelection()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        uiThemeIndex = prefs.getInt(KEY_UI_THEME_INDEX, DEFAULT_UI_THEME_INDEX)
            .coerceIn(0, UI_THEMES.lastIndex)
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, DEFAULT_HAPTIC_ENABLED)
        hapticAmplitude = prefs.getInt(KEY_HAPTIC_AMPLITUDE, DEFAULT_HAPTIC_AMPLITUDE).coerceIn(1, 255)
        hapticDurationMs = prefs.getInt(KEY_HAPTIC_DURATION_MS, DEFAULT_HAPTIC_DURATION_MS).coerceIn(1, 40)
        oryonPerfMode = prefs.getInt(KEY_ORYON_PERF_MODE, DEFAULT_ORYON_PERF_MODE).coerceIn(0, 2)

        window.apply {
            attributes = attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            setDecorFitsSystemWindows(false)
        }

        requestHighRefreshRate()
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            backCallback
        )

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
        }

        // Top Bar — pure #000000 transparent/seamless background with no border line
        headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tabScroll.addView(tabStrip)
        headerBar.addView(
            tabScroll,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        headerBar.addView(makeHeaderButton("+") { createSession(false) })
        headerBar.addView(
            makeHeaderButton(
                label = "#",
                onClick = { createSession(true) },
                onLongClick = { terminalSurface.showTranscriptCopyDialog() }
            )
        )
        headerBar.addView(makeHeaderButton("▤") { showFileAndPackageManagerDialog() })
        headerBar.addView(makeHeaderButton("⚙") { showCategorizedSettingsDialog() })
        headerBar.addView(makeHeaderButton("✕") { closeCurrentSession() })

        rootLayout.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34))
        )

        // Terminal Viewport
        val viewport = FrameLayout(this).apply {
            setBackgroundColor(COLOR_AMOLED_BG)
        }
        terminalSurface = TerminalSurface(this)
        viewport.addView(
            terminalSurface,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout.addView(
            viewport,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        // Bottom Extra Keys Bar (no border line, uniform tight spacing)
        extraKeysRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
            setPadding(dp(2), dp(1), dp(2), dp(2))
        }
        rebuildExtraKeysBar()
        rootLayout.addView(
            extraKeysRoot,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(rootLayout)

        rootLayout.setOnApplyWindowInsetsListener { v, insets ->
            val sysBars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsets.Type.ime())
            v.setPadding(
                sysBars.left,
                sysBars.top,
                sysBars.right,
                max(sysBars.bottom, ime.bottom)
            )
            insets
        }
        rootLayout.post { rootLayout.requestApplyInsets() }

        val initCursorShape = prefs.getInt(KEY_CURSOR_SHAPE, DEFAULT_CURSOR_SHAPE)
        nativeSetDefaultCursorShape(min(2, initCursorShape))
        nativeSetPerformanceMode(oryonPerfMode)
        rebuildTabs()
        nativeRegisterSurface(terminalSurface)
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)

        rootLayout.postDelayed({
            ensureNotificationsAndKeepAlive()
        }, 350L)
    }

    /**
     * Custom Tactile Haptic Engine (configurable on/off, amplitude 1..255, duration 1..40 ms).
     */
    internal fun triggerHaptic(scale: Float = 1.0f) {
        if (!hapticEnabled) return
        try {
            val vm = getSystemService(VibratorManager::class.java)
            val vib: Vibrator? = vm?.defaultVibrator ?: getSystemService(Vibrator::class.java)
            if (vib != null && vib.hasVibrator()) {
                val dur = max(1L, (hapticDurationMs * scale).roundToInt().toLong())
                val amp = (hapticAmplitude * scale).roundToInt().coerceIn(1, 255)
                vib.vibrate(VibrationEffect.createOneShot(dur, amp))
            }
        } catch (_: Throwable) {
        }
    }

    private fun ensureNotificationsAndKeepAlive() {
        try {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_POST_NOTIFICATIONS
                )
            } else {
                startForegroundKeepAlive()
            }
        } catch (_: Throwable) {
            startForegroundKeepAlive()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            startForegroundKeepAlive()
            terminalSurface.postInvalidateOnAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        nativeRegisterSurface(terminalSurface)
        terminalSurface.onActivityResume()
        terminalSurface.requestFocus()
    }

    override fun onPause() {
        terminalSurface.onActivityPause()
        super.onPause()
    }

    override fun onDestroy() {
        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCallback)
        nativeRegisterSurface(null)
        if (isFinishing) {
            nativeDestroyAll()
            try {
                stopService(Intent(this, KeepAliveService::class.java))
            } catch (_: Throwable) {
            }
        }
        super.onDestroy()
    }

    private fun parseKeyToken(rawToken: String): Pair<String, String> {
        val trimmed = rawToken.trim()
        val sepIdx = trimmed.indexOf("::")
        if (sepIdx > 0 && sepIdx + 2 < trimmed.length) {
            val disp = trimmed.substring(0, sepIdx).trim()
            val act = trimmed.substring(sepIdx + 2).trim()
            return disp to act
        }
        val disp = when (trimmed.uppercase()) {
            "UP" -> "▲"
            "DOWN" -> "▼"
            "LEFT" -> "◀"
            "RIGHT" -> "▶"
            "SPACE" -> "SPC"
            else -> trimmed
        }
        return disp to disp
    }

    internal fun rebuildExtraKeysBar() {
        extraKeysRoot.removeAllViews()
        modifierButtons.clear()

        val spec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)?.trim().orEmpty()
        if (spec.isEmpty()) {
            extraKeysRoot.visibility = View.GONE
            return
        }
        extraKeysRoot.visibility = View.VISIBLE

        for (rawLine in spec.split("\n")) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(COLOR_AMOLED_BG)
                setPadding(0, 0, 0, 0)
            }

            val delimiter = if (line.contains("|")) "|" else ","
            var added = 0
            for (rawToken in line.split(delimiter)) {
                val token = rawToken.trim()
                if (token.isEmpty()) continue
                val (displayLabel, actionSpec) = parseKeyToken(token)
                val btn = createExtraKeyButton(displayLabel, actionSpec)
                if (actionSpec == "CTRL" || actionSpec == "ALT") {
                    modifierButtons.add(btn)
                }
                rowLayout.addView(
                    btn,
                    LinearLayout.LayoutParams(0, dp(31), 1f).apply {
                        setMargins(dp(1), dp(1), dp(1), dp(1))
                    }
                )
                added++
            }
            if (added > 0) {
                extraKeysRoot.addView(rowLayout)
            }
        }
        syncModifierButtons(nativeGetModifierMask())
    }

    internal fun dispatchExtraKeyWithHoldSupport(actionSpec: String) {
        terminalSurface.stopTextSelection()
        if (ctrlLocked && (nativeGetModifierMask() and 1) == 0) {
            nativeDispatchCustomKey("CTRL")
        }
        if (altLocked && (nativeGetModifierMask() and 2) == 0) {
            nativeDispatchCustomKey("ALT")
        }
        var mask = nativeDispatchCustomKey(actionSpec)
        if (ctrlLocked && (mask and 1) == 0) {
            mask = nativeDispatchCustomKey("CTRL")
        }
        if (altLocked && (mask and 2) == 0) {
            mask = nativeDispatchCustomKey("ALT")
        }
        syncModifierButtons(mask)
        terminalSurface.resetScrollAndBlink()
    }

    internal fun rearmLockedModifiersIfNeeded() {
        var mask = nativeGetModifierMask()
        if (ctrlLocked && (mask and 1) == 0) {
            mask = nativeDispatchCustomKey("CTRL")
        }
        if (altLocked && (mask and 2) == 0) {
            mask = nativeDispatchCustomKey("ALT")
        }
        syncModifierButtons(mask)
    }

    private fun createExtraKeyButton(displayLabel: String, actionSpec: String): TextView {
        val isModifier = (actionSpec == "CTRL" || actionSpec == "ALT")
        return TextView(this).apply {
            text = displayLabel
            tag = actionSpec
            gravity = Gravity.CENTER
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_TEXT)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(5))

            var holdTriggered = false
            val repeatRunnable = object : Runnable {
                override fun run() {
                    if (!isPressed) return
                    holdTriggered = true
                    if (isModifier) {
                        triggerHaptic(1.35f)
                        if (actionSpec == "CTRL") {
                            ctrlLocked = !ctrlLocked
                            if (ctrlLocked && (nativeGetModifierMask() and 1) == 0) {
                                nativeDispatchCustomKey("CTRL")
                            } else if (!ctrlLocked && (nativeGetModifierMask() and 1) != 0) {
                                nativeDispatchCustomKey("CTRL")
                            }
                        } else if (actionSpec == "ALT") {
                            altLocked = !altLocked
                            if (altLocked && (nativeGetModifierMask() and 2) == 0) {
                                nativeDispatchCustomKey("ALT")
                            } else if (!altLocked && (nativeGetModifierMask() and 2) != 0) {
                                nativeDispatchCustomKey("ALT")
                            }
                        }
                        syncModifierButtons(nativeGetModifierMask())
                    } else {
                        triggerHaptic(0.55f)
                        dispatchExtraKeyWithHoldSupport(actionSpec)
                        postDelayed(this, 50L)
                    }
                }
            }

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        holdTriggered = false
                        triggerHaptic(1.0f)
                        if (!isModifier) {
                            v.background = pillBg(COLOR_SURFACE_PRESSED, uiAccent, dp(5))
                            (v as TextView).setTextColor(uiAccent)
                            dispatchExtraKeyWithHoldSupport(actionSpec)
                            v.postDelayed(repeatRunnable, 350L)
                        } else {
                            v.postDelayed(repeatRunnable, 320L)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.removeCallbacks(repeatRunnable)
                        val wasPressed = v.isPressed
                        v.isPressed = false
                        if (isModifier) {
                            if (wasPressed && !holdTriggered && event.actionMasked == MotionEvent.ACTION_UP) {
                                if (actionSpec == "CTRL" && ctrlLocked) {
                                    ctrlLocked = false
                                    if ((nativeGetModifierMask() and 1) != 0) {
                                        nativeDispatchCustomKey("CTRL")
                                    }
                                } else if (actionSpec == "ALT" && altLocked) {
                                    altLocked = false
                                    if ((nativeGetModifierMask() and 2) != 0) {
                                        nativeDispatchCustomKey("ALT")
                                    }
                                } else {
                                    nativeDispatchCustomKey(actionSpec)
                                }
                            }
                            syncModifierButtons(nativeGetModifierMask())
                        } else {
                            v.background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(5))
                            (v as TextView).setTextColor(COLOR_TEXT)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    // ========================================================================
    // Material 3 Expressive Dialog Shell (Unified Background, Zero Banding)
    // ========================================================================
    private fun createExpressiveDialog(maxWidthDp: Int = 420): Pair<Dialog, LinearLayout> {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(
                min(resources.displayMetrics.widthPixels - dp(24), dp(maxWidthDp)),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(14))
            background = GradientDrawable().apply {
                setColor(COLOR_SHEET_BG)
                cornerRadius = dpF(26f)
                setStroke(1, COLOR_BORDER)
            }
        }
        dlg.setContentView(sheet)
        return dlg to sheet
    }

    private fun makeExpressiveChip(
        title: String,
        selected: Boolean,
        accentColor: Int = uiAccent,
        containerColor: Int = uiAccentContainer,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = title
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setTextColor(if (selected) accentColor else COLOR_TEXT)
            background = pillBg(
                if (selected) containerColor else COLOR_CARD_BG,
                if (selected) accentColor else COLOR_BORDER,
                dp(10)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dp(6), dp(4))
            }
            setOnClickListener {
                triggerHaptic(0.8f)
                onClick()
            }
        }
    }

    private fun makeHorizontalChipRow(): Pair<HorizontalScrollView, LinearLayout> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(row)
        }
        return hsv to row
    }

    private fun makeSectionLabel(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(10), 0, dp(4))
        }
    }

    private fun makeSeekBarRow(
        labelPrefix: String,
        initialProgress: Int,
        maxProgress: Int,
        formatValue: (Int) -> String,
        onChanged: (Int) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(4))

            val topRow = LinearLayout(this@TermuxActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(this@TermuxActivity).apply {
                text = labelPrefix
                setTextColor(COLOR_TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valTv = TextView(this@TermuxActivity).apply {
                text = formatValue(initialProgress)
                setTextColor(uiAccent)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            }
            topRow.addView(titleTv)
            topRow.addView(valTv)
            addView(topRow)

            val sb = SeekBar(this@TermuxActivity).apply {
                max = maxProgress
                progress = initialProgress.coerceIn(0, maxProgress)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
                        valTv.text = formatValue(p)
                        if (fromUser) onChanged(p)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        triggerHaptic(0.7f)
                    }
                })
            }
            addView(sb)
        }
    }

    // ========================================================================
    // Categorized Material 3 Expressive Settings Hub
    // ========================================================================
    private fun showCategorizedSettingsDialog() {
        val (dlg, sheet) = createExpressiveDialog(420)

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        val backBtn = TextView(this).apply {
            text = "←"
            setTextColor(uiAccent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(8), dp(2), dp(12), dp(2))
            visibility = View.GONE
        }
        val titleTv = TextView(this).apply {
            text = "Настройки"
            setTextColor(COLOR_TEXT_BRIGHT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(10), dp(4), dp(6), dp(4))
            setOnClickListener {
                triggerHaptic(0.8f)
                dlg.dismiss()
            }
        }
        headerRow.addView(backBtn)
        headerRow.addView(titleTv)
        headerRow.addView(closeBtn)
        sheet.addView(headerRow)

        val bodyScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        bodyScroll.addView(bodyContainer)
        sheet.addView(bodyScroll)

        fun makeCategoryCard(
            title: String,
            subtitle: String,
            badge: String,
            onClick: () -> Unit
        ): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }

                val badgeView = TextView(this@TermuxActivity).apply {
                    text = badge
                    gravity = Gravity.CENTER
                    setTextColor(uiAccent)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    background = pillBg(uiAccentContainer, uiAccent, dp(12))
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                        marginEnd = dp(12)
                    }
                }
                addView(badgeView)

                val textCol = LinearLayout(this@TermuxActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                textCol.addView(TextView(this@TermuxActivity).apply {
                    text = title
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    typeface = Typeface.DEFAULT_BOLD
                })
                textCol.addView(TextView(this@TermuxActivity).apply {
                    text = subtitle
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, dp(2), 0, 0)
                })
                addView(textCol)

                addView(TextView(this@TermuxActivity).apply {
                    text = "›"
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(dp(8), 0, 0, 0)
                })

                setOnClickListener {
                    triggerHaptic(0.9f)
                    onClick()
                }
            }
        }

        lateinit var showHomeCategories: () -> Unit

        // Category 1: Курсор и анимации (VS Code)
        fun showCursorCategory() {
            titleTv.text = "Курсор и анимации"
            backBtn.visibility = View.VISIBLE
            backBtn.setOnClickListener {
                triggerHaptic(0.8f)
                showHomeCategories()
            }
            bodyContainer.removeAllViews()

            fun rebuildCursorCategoryUI() {
                bodyContainer.removeAllViews()

                bodyContainer.addView(makeSectionLabel("Форма курсора"))
                val (shapeScroll, shapeRow) = makeHorizontalChipRow()
                val shapes = listOf("Блок" to 0, "Линия" to 1, "Луч" to 2, "Контур" to 3)
                for ((lbl, s) in shapes) {
                    shapeRow.addView(makeExpressiveChip(lbl, terminalSurface.cursorShapePref == s) {
                        terminalSurface.cursorShapePref = s
                        prefs.edit().putInt(KEY_CURSOR_SHAPE, s).apply()
                        nativeSetDefaultCursorShape(min(2, s))
                        terminalSurface.resetScrollAndBlink()
                        rebuildCursorCategoryUI()
                    })
                }
                bodyContainer.addView(shapeScroll)

                bodyContainer.addView(makeSectionLabel("Анимация мигания (VS Code)"))
                val (blinkScroll, blinkRow) = makeHorizontalChipRow()
                val styles = listOf(
                    "Smooth" to 0,
                    "Phase" to 1,
                    "Expand" to 2,
                    "Blink" to 3,
                    "Solid" to 4
                )
                for ((lbl, st) in styles) {
                    blinkRow.addView(makeExpressiveChip(lbl, terminalSurface.cursorBlinkStyle == st) {
                        terminalSurface.cursorBlinkStyle = st
                        prefs.edit().putInt(KEY_CURSOR_BLINK_STYLE, st).apply()
                        terminalSurface.resetScrollAndBlink()
                        rebuildCursorCategoryUI()
                    })
                }
                bodyContainer.addView(blinkScroll)

                bodyContainer.addView(makeSectionLabel("Движение каретки (VS Code Smooth Caret)"))
                val (moveScroll, moveRow) = makeHorizontalChipRow()
                moveRow.addView(makeExpressiveChip("Плавное скольжение", terminalSurface.cursorSmoothMove) {
                    val next = !terminalSurface.cursorSmoothMove
                    terminalSurface.cursorSmoothMove = next
                    prefs.edit().putBoolean(KEY_CURSOR_SMOOTH_MOVE, next).apply()
                    rebuildCursorCategoryUI()
                })
                moveRow.addView(makeExpressiveChip("Мгновенное", !terminalSurface.cursorSmoothMove) {
                    terminalSurface.cursorSmoothMove = false
                    prefs.edit().putBoolean(KEY_CURSOR_SMOOTH_MOVE, false).apply()
                    rebuildCursorCategoryUI()
                })
                bodyContainer.addView(moveScroll)

                // Sliders for exact tuning
                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Непрозрачность курсора",
                        initialProgress = terminalSurface.cursorAlphaPct - 15,
                        maxProgress = 85,
                        formatValue = { "${it + 15}%" },
                        onChanged = { p ->
                            val pct = (p + 15).coerceIn(15, 100)
                            terminalSurface.cursorAlphaPct = pct
                            prefs.edit().putInt(KEY_CURSOR_ALPHA_PCT, pct).apply()
                            terminalSurface.postInvalidateOnAnimation()
                        }
                    )
                )

                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Радиус скругления углов",
                        initialProgress = terminalSurface.cursorRadiusTenths,
                        maxProgress = 60,
                        formatValue = { "${it / 10}.${it % 10} dp" },
                        onChanged = { p ->
                            terminalSurface.cursorRadiusTenths = p
                            prefs.edit().putInt(KEY_CURSOR_RADIUS_TENTHS, p).apply()
                            terminalSurface.postInvalidateOnAnimation()
                        }
                    )
                )

                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Скорость затухания (полупериод)",
                        initialProgress = (terminalSurface.cursorFadeMs - 200) / 10,
                        maxProgress = 100,
                        formatValue = { "${200 + it * 10} мс" },
                        onChanged = { p ->
                            val ms = 200 + p * 10
                            terminalSurface.cursorFadeMs = ms
                            prefs.edit().putInt(KEY_CURSOR_FADE_MS, ms).apply()
                            terminalSurface.resetScrollAndBlink()
                        }
                    )
                )

                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Длительность скольжения каретки",
                        initialProgress = (terminalSurface.cursorGlideMs - 30) / 5,
                        maxProgress = 38,
                        formatValue = { "${30 + it * 5} мс" },
                        onChanged = { p ->
                            val ms = 30 + p * 5
                            terminalSurface.cursorGlideMs = ms
                            prefs.edit().putInt(KEY_CURSOR_GLIDE_MS, ms).apply()
                        }
                    )
                )
            }

            rebuildCursorCategoryUI()
        }

        // Category 2: Нижняя клавиатура и бинды
        fun showKeyboardCategory() {
            titleTv.text = "Нижняя панель клавиш"
            backBtn.visibility = View.VISIBLE
            backBtn.setOnClickListener {
                triggerHaptic(0.8f)
                showHomeCategories()
            }

            val currentSpec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)
                ?: DEFAULT_EXTRA_KEYS_SPEC
            val lines = currentSpec.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            val row1Tokens = ArrayList<String>()
            val row2Tokens = ArrayList<String>()
            if (lines.isNotEmpty()) {
                val d1 = if (lines[0].contains("|")) "|" else ","
                row1Tokens.addAll(lines[0].split(d1).map { it.trim() }.filter { it.isNotEmpty() })
            }
            if (lines.size > 1) {
                val d2 = if (lines[1].contains("|")) "|" else ","
                row2Tokens.addAll(lines[1].split(d2).map { it.trim() }.filter { it.isNotEmpty() })
            }
            var activeEditRow = 0

            fun persistRows() {
                val builtSpec = buildString {
                    if (row1Tokens.isNotEmpty()) append(row1Tokens.joinToString("|"))
                    if (row2Tokens.isNotEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append(row2Tokens.joinToString("|"))
                    }
                }
                prefs.edit().putString(KEY_EXTRA_KEYS_SPEC, builtSpec).apply()
                rebuildExtraKeysBar()
                terminalSurface.post { terminalSurface.syncSize() }
            }

            fun rebuildKeyboardUI() {
                bodyContainer.removeAllViews()

                bodyContainer.addView(makeSectionLabel("Готовые раскладки"))
                val (presetScroll, presetRow) = makeHorizontalChipRow()
                fun loadPreset(specStr: String) {
                    row1Tokens.clear()
                    row2Tokens.clear()
                    val ls = specStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    if (ls.isNotEmpty()) row1Tokens.addAll(ls[0].split("|"))
                    if (ls.size > 1) row2Tokens.addAll(ls[1].split("|"))
                    persistRows()
                    rebuildKeyboardUI()
                }
                presetRow.addView(makeExpressiveChip("1 ряд", row1Tokens.isNotEmpty() && row2Tokens.isEmpty()) {
                    loadPreset(DEFAULT_EXTRA_KEYS_SPEC)
                })
                presetRow.addView(makeExpressiveChip("2 ряда", row2Tokens.isNotEmpty()) {
                    loadPreset(EXTENDED_EXTRA_KEYS_SPEC)
                })
                presetRow.addView(makeExpressiveChip("Vim / Tmux", false) {
                    loadPreset(VIM_TMUX_EXTRA_KEYS_SPEC)
                })
                presetRow.addView(makeExpressiveChip("Скрыть", row1Tokens.isEmpty() && row2Tokens.isEmpty()) {
                    loadPreset("")
                })
                bodyContainer.addView(presetScroll)

                bodyContainer.addView(makeSectionLabel("Редактируемый ряд"))
                val (rowSelScroll, rowSelBar) = makeHorizontalChipRow()
                val targetList = if (activeEditRow == 0) row1Tokens else row2Tokens

                rowSelBar.addView(makeExpressiveChip("Ряд 1 (${row1Tokens.size})", activeEditRow == 0) {
                    activeEditRow = 0
                    rebuildKeyboardUI()
                })
                rowSelBar.addView(makeExpressiveChip("Ряд 2 (${row2Tokens.size})", activeEditRow == 1) {
                    activeEditRow = 1
                    rebuildKeyboardUI()
                })
                rowSelBar.addView(makeExpressiveChip("+ Своя клавиша", false) {
                    val (bindDlg, bindSheet) = createExpressiveDialog(360)
                    bindSheet.addView(TextView(this).apply {
                        text = "Привязка клавиши или команды"
                        setTextColor(COLOR_TEXT_BRIGHT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(0, 0, 0, dp(8))
                    })
                    val lblEdit = EditText(this).apply {
                        hint = "Надпись на кнопке (напр. C-c или top)"
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(10))
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        setSingleLine(true)
                    }
                    val actEdit = EditText(this).apply {
                        hint = "Действие (CTRL+C, ALT+B, htop\\n)"
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(10))
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        setSingleLine(true)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(8)
                        }
                    }
                    bindSheet.addView(lblEdit)
                    bindSheet.addView(actEdit)

                    val btnBar = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(12), 0, 0)
                    }
                    btnBar.addView(makeExpressiveChip("Отмена", false) { bindDlg.dismiss() })
                    btnBar.addView(makeExpressiveChip("Добавить", true) {
                        val l = lblEdit.text.toString().trim().replace("|", "")
                        val a = actEdit.text.toString().trim().replace("|", "")
                        if (l.isNotEmpty() || a.isNotEmpty()) {
                            val tok = when {
                                l.isNotEmpty() && a.isNotEmpty() && l != a -> "$l::$a"
                                a.isNotEmpty() -> a
                                else -> l
                            }
                            targetList.add(tok)
                            persistRows()
                            rebuildKeyboardUI()
                        }
                        bindDlg.dismiss()
                    })
                    bindSheet.addView(btnBar)
                    bindDlg.show()
                })
                rowSelBar.addView(makeExpressiveChip("Очистить", false) {
                    targetList.clear()
                    persistRows()
                    rebuildKeyboardUI()
                })
                bodyContainer.addView(rowSelScroll)

                bodyContainer.addView(makeSectionLabel("Кнопки выбранного ряда (нажми для удаления)"))
                val (curScroll, curRow) = makeHorizontalChipRow()
                curRow.setPadding(dp(6), dp(6), dp(6), dp(6))
                curRow.background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(12))
                if (targetList.isEmpty()) {
                    curRow.addView(TextView(this).apply {
                        text = "Ряд пуст"
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                    })
                } else {
                    for (idx in targetList.indices) {
                        val (disp, _) = parseKeyToken(targetList[idx])
                        curRow.addView(makeExpressiveChip("$disp ✕", true) {
                            targetList.removeAt(idx)
                            persistRows()
                            rebuildKeyboardUI()
                        })
                    }
                }
                bodyContainer.addView(curScroll)

                bodyContainer.addView(makeSectionLabel("Быстрое добавление клавиш"))
                val (p1Scroll, p1Row) = makeHorizontalChipRow()
                val (p2Scroll, p2Row) = makeHorizontalChipRow()
                val keys1 = listOf(
                    "ESC", "TAB", "CTRL", "ALT", "ENTER", "BKSP", "DEL", "SPC",
                    "▲", "▼", "◀", "▶", "HOME", "END", "PGUP", "PGDN"
                )
                val keys2 = listOf(
                    "-", "/", "|", "\\", "_", "~", ":", ";", "\"", "'",
                    "C-c::CTRL+C", "C-z::CTRL+Z", "C-d::CTRL+D", "C-l::CTRL+L",
                    "F1", "F2", "F3", "F4", "F5", "F10", "F12"
                )
                for (k in keys1) {
                    val (d, _) = parseKeyToken(k)
                    p1Row.addView(makeExpressiveChip("+ $d", false) {
                        targetList.add(k)
                        persistRows()
                        rebuildKeyboardUI()
                    })
                }
                for (k in keys2) {
                    val (d, _) = parseKeyToken(k)
                    p2Row.addView(makeExpressiveChip("+ $d", false) {
                        targetList.add(k)
                        persistRows()
                        rebuildKeyboardUI()
                    })
                }
                bodyContainer.addView(p1Scroll)
                bodyContainer.addView(p2Scroll)
            }

            rebuildKeyboardUI()
        }

        // Category 3: Тактильный отклик (Вибрация)
        fun showHapticsCategory() {
            titleTv.text = "Тактильный отклик"
            backBtn.visibility = View.VISIBLE
            backBtn.setOnClickListener {
                triggerHaptic(0.8f)
                showHomeCategories()
            }

            fun rebuildHapticUI() {
                bodyContainer.removeAllViews()
                bodyContainer.addView(makeSectionLabel("Состояние вибрации"))
                val (enScroll, enRow) = makeHorizontalChipRow()
                enRow.addView(makeExpressiveChip("Включена", hapticEnabled) {
                    hapticEnabled = true
                    prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, true).apply()
                    triggerHaptic(1.0f)
                    rebuildHapticUI()
                })
                enRow.addView(makeExpressiveChip("Отключена", !hapticEnabled) {
                    hapticEnabled = false
                    prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, false).apply()
                    rebuildHapticUI()
                })
                bodyContainer.addView(enScroll)

                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Мощность импульса",
                        initialProgress = hapticAmplitude - 1,
                        maxProgress = 254,
                        formatValue = { "${((it + 1) * 100f / 255f).roundToInt()}% (${it + 1})" },
                        onChanged = { p ->
                            hapticAmplitude = (p + 1).coerceIn(1, 255)
                            prefs.edit().putInt(KEY_HAPTIC_AMPLITUDE, hapticAmplitude).apply()
                        }
                    )
                )

                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Длительность / задержка импульса",
                        initialProgress = hapticDurationMs - 1,
                        maxProgress = 39,
                        formatValue = { "${it + 1} мс" },
                        onChanged = { p ->
                            hapticDurationMs = (p + 1).coerceIn(1, 40)
                            prefs.edit().putInt(KEY_HAPTIC_DURATION_MS, hapticDurationMs).apply()
                        }
                    )
                )

                bodyContainer.addView(makeSectionLabel("Быстрые профили вибрации"))
                val (profScroll, profRow) = makeHorizontalChipRow()
                profRow.addView(makeExpressiveChip("Мягкий (6 мс / 30%)", hapticDurationMs == 6 && hapticAmplitude in 70..85) {
                    hapticEnabled = true
                    hapticDurationMs = 6
                    hapticAmplitude = 76
                    prefs.edit()
                        .putBoolean(KEY_HAPTIC_ENABLED, true)
                        .putInt(KEY_HAPTIC_DURATION_MS, 6)
                        .putInt(KEY_HAPTIC_AMPLITUDE, 76)
                        .apply()
                    triggerHaptic(1.0f)
                    rebuildHapticUI()
                })
                profRow.addView(makeExpressiveChip("Упругий (8 мс / 55%)", hapticDurationMs == 8 && hapticAmplitude in 130..150) {
                    hapticEnabled = true
                    hapticDurationMs = 8
                    hapticAmplitude = 140
                    prefs.edit()
                        .putBoolean(KEY_HAPTIC_ENABLED, true)
                        .putInt(KEY_HAPTIC_DURATION_MS, 8)
                        .putInt(KEY_HAPTIC_AMPLITUDE, 140)
                        .apply()
                    triggerHaptic(1.0f)
                    rebuildHapticUI()
                })
                profRow.addView(makeExpressiveChip("Мощный (14 мс / 90%)", hapticDurationMs == 14 && hapticAmplitude >= 220) {
                    hapticEnabled = true
                    hapticDurationMs = 14
                    hapticAmplitude = 230
                    prefs.edit()
                        .putBoolean(KEY_HAPTIC_ENABLED, true)
                        .putInt(KEY_HAPTIC_DURATION_MS, 14)
                        .putInt(KEY_HAPTIC_AMPLITUDE, 230)
                        .apply()
                    triggerHaptic(1.0f)
                    rebuildHapticUI()
                })
                bodyContainer.addView(profScroll)
            }

            rebuildHapticUI()
        }

        // Category 4: Оформление и цвет интерфейса
        fun showThemeCategory() {
            titleTv.text = "Оформление и тема"
            backBtn.visibility = View.VISIBLE
            backBtn.setOnClickListener {
                triggerHaptic(0.8f)
                showHomeCategories()
            }

            fun rebuildThemeUI() {
                bodyContainer.removeAllViews()
                bodyContainer.addView(makeSectionLabel("Цветовая палитра интерфейса"))
                val (thScroll, thRow) = makeHorizontalChipRow()
                for (i in UI_THEMES.indices) {
                    val preset = UI_THEMES[i]
                    val isSel = (uiThemeIndex == i)
                    thRow.addView(
                        makeExpressiveChip(
                            title = preset.name,
                            selected = isSel,
                            accentColor = preset.accent,
                            containerColor = preset.container
                        ) {
                            uiThemeIndex = i
                            prefs.edit().putInt(KEY_UI_THEME_INDEX, i).apply()
                            backBtn.setTextColor(uiAccent)
                            rebuildTabs()
                            rebuildExtraKeysBar()
                            terminalSurface.postInvalidateOnAnimation()
                            rebuildThemeUI()
                        }
                    )
                }
                bodyContainer.addView(thScroll)

                val curScalePct = (terminalSurface.fontScale * 100f).roundToInt().coerceIn(50, 220)
                bodyContainer.addView(
                    makeSeekBarRow(
                        labelPrefix = "Масштаб текста терминала",
                        initialProgress = (curScalePct - 50) / 5,
                        maxProgress = 34,
                        formatValue = { "${50 + it * 5}%" },
                        onChanged = { p ->
                            val newScale = (50 + p * 5) / 100f
                            terminalSurface.setCustomFontScale(newScale)
                            prefs.edit().putFloat(KEY_FONT_SCALE, newScale).apply()
                        }
                    )
                )
            }

            rebuildThemeUI()
        }

        // Category 5: Процессор и система (Snapdragon 8 Elite Gen 5 + proot/chroot)
        fun showPerformanceCategory() {
            titleTv.text = "Процессор и система"
            backBtn.visibility = View.VISIBLE
            backBtn.setOnClickListener {
                triggerHaptic(0.8f)
                showHomeCategories()
            }

            fun rebuildPerfUI() {
                bodyContainer.removeAllViews()
                bodyContainer.addView(makeSectionLabel("Планировщик Qualcomm Oryon (Snapdragon 8 Elite Gen 5)"))
                val (pScroll, pRow) = makeHorizontalChipRow()
                val modes = listOf(
                    "Oryon Smart Boost" to 0,
                    "Prime Max (Все 8 ядер)" to 1,
                    "Oryon Eco (Энергосбережение)" to 2
                )
                for ((lbl, m) in modes) {
                    pRow.addView(makeExpressiveChip(lbl, oryonPerfMode == m) {
                        oryonPerfMode = m
                        prefs.edit().putInt(KEY_ORYON_PERF_MODE, m).apply()
                        nativeSetPerformanceMode(m)
                        rebuildPerfUI()
                    })
                }
                bodyContainer.addView(pScroll)

                val descText = when (oryonPerfMode) {
                    1 -> "Prime Max: все 8 ядер Oryon (включая 2 Prime-ядра 4.32+ ГГц) разблокированы, приоритет процесса повышен (-15), пакетное чтение PTY до 384 КБ за цикл. Рекомендуется для proot-distro / chroot и компиляции."
                    2 -> "Oryon Eco: фоновые потоки привязаны к энергоэффективному кластеру ядер (CPU 0..3) с минимальным энергопотреблением."
                    else -> "Smart Boost: 0% нагрузки в покое (poll -1) и мгновенный автоматический разгон на все 8 ядер Oryon при интенсивном потоке данных."
                }
                bodyContainer.addView(TextView(this).apply {
                    text = descText
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(dp(4), dp(6), dp(4), dp(8))
                })

                bodyContainer.addView(makeSectionLabel("Окружение Linux-дистрибутивов (proot-distro / chroot)"))
                val infoCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(14))
                }
                infoCard.addView(TextView(this).apply {
                    text = "• VFS Redirector: совместим с proot / chroot (авто-очистка Bionic LD_PRELOAD для гостевых glibc/musl бинарников)\n• PROOT_TMP_DIR: /data/data/com.termuxx/file/usr/tmp\n• SELinux & umask(0022): настроены для распаковки rootfs"
                    setTextColor(COLOR_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                })
                bodyContainer.addView(infoCard)

                bodyContainer.addView(makeSectionLabel("Быстрое развёртывание дистрибутива в терминале"))
                val (dScroll, dRow) = makeHorizontalChipRow()
                dRow.addView(makeExpressiveChip("Установить proot-distro", false) {
                    dlg.dismiss()
                    nativeSendText("pkg install -y proot-distro\n", false)
                })
                dRow.addView(makeExpressiveChip("Ubuntu (proot)", false) {
                    dlg.dismiss()
                    nativeSendText("pkg install -y proot-distro && proot-distro install ubuntu && proot-distro login ubuntu\n", false)
                })
                dRow.addView(makeExpressiveChip("Alpine (proot)", false) {
                    dlg.dismiss()
                    nativeSendText("pkg install -y proot-distro && proot-distro install alpine && proot-distro login alpine\n", false)
                })
                dRow.addView(makeExpressiveChip("Arch Linux (proot)", false) {
                    dlg.dismiss()
                    nativeSendText("pkg install -y proot-distro && proot-distro install archlinux && proot-distro login archlinux\n", false)
                })
                bodyContainer.addView(dScroll)
            }

            rebuildPerfUI()
        }

        showHomeCategories = {
            titleTv.text = "Настройки"
            backBtn.visibility = View.GONE
            bodyContainer.removeAllViews()

            val styleNames = arrayOf("VS Code Smooth", "VS Code Phase", "VS Code Expand", "Blink", "Solid")
            val curStyleName = styleNames[terminalSurface.cursorBlinkStyle.coerceIn(0, 4)]
            bodyContainer.addView(
                makeCategoryCard(
                    title = "Курсор и анимации",
                    subtitle = "$curStyleName · ${terminalSurface.cursorAlphaPct}% · ${terminalSurface.cursorRadiusTenths / 10f}dp",
                    badge = "▊"
                ) { showCursorCategory() }
            )

            bodyContainer.addView(
                makeCategoryCard(
                    title = "Клавиатура и бинды",
                    subtitle = "Настройка рядов, спецклавиш и пользовательских команд",
                    badge = "⌨"
                ) { showKeyboardCategory() }
            )

            bodyContainer.addView(
                makeCategoryCard(
                    title = "Тактильный отклик",
                    subtitle = if (hapticEnabled) "Вкл · ${hapticDurationMs} мс · ${(hapticAmplitude * 100f / 255f).roundToInt()}%" else "Отключён",
                    badge = "≈"
                ) { showHapticsCategory() }
            )

            bodyContainer.addView(
                makeCategoryCard(
                    title = "Оформление и тема",
                    subtitle = "Палитра: ${UI_THEMES[uiThemeIndex].name} · Масштаб шрифта",
                    badge = "◈"
                ) { showThemeCategory() }
            )

            val perfNames = arrayOf("Oryon Smart Boost", "Oryon Prime Max", "Oryon Eco")
            bodyContainer.addView(
                makeCategoryCard(
                    title = "Процессор и система",
                    subtitle = "${perfNames[oryonPerfMode.coerceIn(0, 2)]} · proot / chroot",
                    badge = "⚡"
                ) { showPerformanceCategory() }
            )
        }

        showHomeCategories()
        dlg.show()
    }

    // ========================================================================
    // Material 3 Expressive File Manager & Package Manager
    // ========================================================================
    private fun formatSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes Б"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format("%.1f КБ", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format("%.1f МБ", mb)
        val gb = mb / 1024.0
        return String.format("%.2f ГБ", gb)
    }

    private fun computeItemSizeBounded(f: File, maxDepth: Int = 3): Long {
        return try {
            if (!f.exists()) return 0L
            if (f.isFile || File(f.parentFile, f.name).let { it.canonicalPath != it.absolutePath }) {
                f.length()
            } else if (maxDepth <= 0) {
                0L
            } else {
                var sum = 0L
                val children = f.listFiles()
                if (children != null) {
                    for (c in children) {
                        sum += if (c.isFile) c.length() else computeItemSizeBounded(c, maxDepth - 1)
                    }
                }
                sum
            }
        } catch (_: Throwable) {
            0L
        }
    }

    private fun parseInstalledDpkgPackages(): List<DpkgPackageInfo> {
        val statusFile = File("/data/data/com.termuxx/file/usr/var/lib/dpkg/status")
        if (!statusFile.exists() || !statusFile.canRead()) return emptyList()
        val out = ArrayList<DpkgPackageInfo>()
        try {
            val content = statusFile.readText()
            for (block in content.split("\n\n")) {
                var pkg = ""
                var ver = ""
                var status = ""
                var sizeKb = 0L
                var desc = ""
                for (line in block.lines()) {
                    when {
                        line.startsWith("Package: ") -> pkg = line.substring(9).trim()
                        line.startsWith("Version: ") -> ver = line.substring(9).trim()
                        line.startsWith("Status: ") -> status = line.substring(8).trim()
                        line.startsWith("Installed-Size: ") -> {
                            sizeKb = line.substring(16).trim().toLongOrNull() ?: 0L
                        }
                        line.startsWith("Description: ") -> desc = line.substring(13).trim()
                    }
                }
                if (pkg.isNotEmpty() && status.contains("installed")) {
                    out.add(DpkgPackageInfo(pkg, ver, sizeKb, desc))
                }
            }
        } catch (_: Throwable) {
        }
        return out.sortedBy { it.name }
    }

    private fun showFileAndPackageManagerDialog() {
        val (dlg, sheet) = createExpressiveDialog(440)

        var activeTab = 0 // 0 = Files, 1 = Packages
        var currentDir = File("/data/data/com.termuxx/file/home").let {
            if (it.exists()) it else File(applicationInfo.dataDir)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        val tabFilesBtn = makeExpressiveChip("Файлы", true) {}
        val tabPkgsBtn = makeExpressiveChip("Пакеты", false) {}
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(10), dp(4), dp(6), dp(4))
            setOnClickListener {
                triggerHaptic(0.8f)
                dlg.dismiss()
            }
        }
        topBar.addView(tabFilesBtn)
        topBar.addView(tabPkgsBtn)
        topBar.addView(spacer)
        topBar.addView(closeBtn)
        sheet.addView(topBar)

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        sheet.addView(contentContainer)

        lateinit var renderCurrentTab: () -> Unit

        tabFilesBtn.setOnClickListener {
            triggerHaptic(0.8f)
            activeTab = 0
            renderCurrentTab()
        }
        tabPkgsBtn.setOnClickListener {
            triggerHaptic(0.8f)
            activeTab = 1
            renderCurrentTab()
        }

        fun showTextEditorDialog(file: File, onSaved: () -> Unit) {
            val (edDlg, edSheet) = createExpressiveDialog(440)
            edSheet.addView(TextView(this).apply {
                text = file.name
                setTextColor(COLOR_TEXT_BRIGHT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(8))
            })
            val initialText = try {
                if (file.length() <= 256 * 1024) file.readText() else "Файл слишком велик для текстового редактора (> 256 КБ)"
            } catch (e: Throwable) {
                ""
            }
            val edit = EditText(this).apply {
                setText(initialText)
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.TOP or Gravity.START
                minLines = 10
                maxLines = 18
                background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(12))
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }
            edSheet.addView(edit)

            val bar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(10), 0, 0)
            }
            bar.addView(makeExpressiveChip("Отмена", false) { edDlg.dismiss() })
            bar.addView(makeExpressiveChip("Сохранить", true) {
                try {
                    file.writeText(edit.text.toString())
                    Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                    onSaved()
                } catch (e: Throwable) {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                edDlg.dismiss()
            })
            edSheet.addView(bar)
            edDlg.show()
        }

        fun showFileActionsDialog(target: File) {
            val itemSize = computeItemSizeBounded(target, 4)
            val options = ArrayList<Pair<String, () -> Unit>>()
            if (target.isFile) {
                options.add("Редактировать текст" to {
                    showTextEditorDialog(target) { renderCurrentTab() }
                })
            } else {
                options.add("Открыть папку в терминале (cd)" to {
                    dlg.dismiss()
                    nativeSendText("cd \"${target.absolutePath}\"\n", false)
                })
            }
            options.add("Переименовать" to {
                val (renDlg, renSheet) = createExpressiveDialog(360)
                renSheet.addView(TextView(this).apply {
                    text = "Переименовать"
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, dp(8))
                })
                val input = EditText(this).apply {
                    setText(target.name)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(10))
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    setSingleLine(true)
                }
                renSheet.addView(input)
                val bar = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(10), 0, 0)
                }
                bar.addView(makeExpressiveChip("Отмена", false) { renDlg.dismiss() })
                bar.addView(makeExpressiveChip("Готово", true) {
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty() && newName != target.name) {
                        target.renameTo(File(target.parentFile, newName))
                        renderCurrentTab()
                    }
                    renDlg.dismiss()
                })
                renSheet.addView(bar)
                renDlg.show()
            })
            options.add("Копировать" to {
                fileClipSource = target
                fileClipIsMove = false
                Toast.makeText(this, "Скопировано: ${target.name}", Toast.LENGTH_SHORT).show()
                renderCurrentTab()
            })
            options.add("Вырезать (Переместить)" to {
                fileClipSource = target
                fileClipIsMove = true
                Toast.makeText(this, "Вырезано: ${target.name}", Toast.LENGTH_SHORT).show()
                renderCurrentTab()
            })
            if (target.isFile) {
                options.add("Сделать исполняемым (chmod 700)" to {
                    target.setExecutable(true, true)
                    target.setReadable(true, true)
                    target.setWritable(true, true)
                    Toast.makeText(this, "Права 700 установлены", Toast.LENGTH_SHORT).show()
                    renderCurrentTab()
                })
            }
            options.add("Скопировать путь" to {
                copyToClipboard(target.absolutePath, true)
            })
            options.add("Удалить (${formatSize(itemSize)})" to {
                try {
                    target.deleteRecursively()
                    renderCurrentTab()
                } catch (e: Throwable) {
                    Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            })

            val labels = options.map { it.first }.toTypedArray()
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("${target.name} (${formatSize(itemSize)})")
                .setItems(labels) { _, which ->
                    options[which].second.invoke()
                }
                .show()
        }

        fun renderFilesTab() {
            contentContainer.removeAllViews()

            // Quick bookmarks
            val (bmScroll, bmRow) = makeHorizontalChipRow()
            val bookmarks = listOf(
                "Домашняя (~)" to File("/data/data/com.termuxx/file/home"),
                "Usr (\$PREFIX)" to File("/data/data/com.termuxx/file/usr"),
                "Данные приложения" to File(applicationInfo.dataDir),
                "Дистрибутивы" to File("/data/data/com.termuxx/file/usr/var/lib/proot-distro/installed-rootfs"),
                "SDCard" to Environment.getExternalStorageDirectory()
            )
            for ((lbl, dir) in bookmarks) {
                val isCur = (currentDir.absolutePath == dir.absolutePath)
                bmRow.addView(makeExpressiveChip(lbl, isCur) {
                    if (!dir.exists()) dir.mkdirs()
                    if (dir.exists() && dir.canRead()) {
                        currentDir = dir
                        renderCurrentTab()
                    } else {
                        Toast.makeText(this, "Нет доступа к ${dir.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            contentContainer.addView(bmScroll)

            // Current path & storage stats card
            val dirSize = computeItemSizeBounded(currentDir, 2)
            val freeBytes = try {
                StatFs(currentDir.absolutePath).availableBytes
            } catch (_: Throwable) {
                0L
            }
            contentContainer.addView(TextView(this).apply {
                text = "${currentDir.absolutePath}\nЗанято в папке: ${formatSize(dirSize)} · Свободно: ${formatSize(freeBytes)}"
                setTextColor(COLOR_TEXT_DIM)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setPadding(dp(2), dp(4), dp(2), dp(6))
            })

            // Action toolbar: Up, + Folder, + File, Paste, Open in Terminal
            val (actScroll, actRow) = makeHorizontalChipRow()
            actRow.addView(makeExpressiveChip("↑ Вверх", false) {
                val parent = currentDir.parentFile
                if (parent != null && parent.canRead()) {
                    currentDir = parent
                    renderCurrentTab()
                }
            })
            actRow.addView(makeExpressiveChip("+ Папка", false) {
                val (cDlg, cSheet) = createExpressiveDialog(340)
                cSheet.addView(TextView(this).apply {
                    text = "Новая папка"
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, dp(8))
                })
                val inp = EditText(this).apply {
                    hint = "Имя папки"
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(10))
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    setSingleLine(true)
                }
                cSheet.addView(inp)
                val bar = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(10), 0, 0)
                }
                bar.addView(makeExpressiveChip("Отмена", false) { cDlg.dismiss() })
                bar.addView(makeExpressiveChip("Создать", true) {
                    val name = inp.text.toString().trim()
                    if (name.isNotEmpty()) {
                        File(currentDir, name).mkdirs()
                        renderCurrentTab()
                    }
                    cDlg.dismiss()
                })
                cSheet.addView(bar)
                cDlg.show()
            })
            actRow.addView(makeExpressiveChip("+ Файл", false) {
                val (cDlg, cSheet) = createExpressiveDialog(340)
                cSheet.addView(TextView(this).apply {
                    text = "Новый файл"
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, dp(8))
                })
                val inp = EditText(this).apply {
                    hint = "Имя файла (напр. script.sh)"
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(10))
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    setSingleLine(true)
                }
                cSheet.addView(inp)
                val bar = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(10), 0, 0)
                }
                bar.addView(makeExpressiveChip("Отмена", false) { cDlg.dismiss() })
                bar.addView(makeExpressiveChip("Создать", true) {
                    val name = inp.text.toString().trim()
                    if (name.isNotEmpty()) {
                        val f = File(currentDir, name)
                        try {
                            f.createNewFile()
                            renderCurrentTab()
                            showTextEditorDialog(f) { renderCurrentTab() }
                        } catch (_: Throwable) {
                        }
                    }
                    cDlg.dismiss()
                })
                cSheet.addView(bar)
                cDlg.show()
            })

            val clipFile = fileClipSource
            if (clipFile != null && clipFile.exists()) {
                val actionLabel = if (fileClipIsMove) "Вставить (Перем. ${clipFile.name})" else "Вставить (${clipFile.name})"
                actRow.addView(makeExpressiveChip(actionLabel, true) {
                    try {
                        val dst = File(currentDir, clipFile.name)
                        if (fileClipIsMove) {
                            if (!clipFile.renameTo(dst)) {
                                clipFile.copyRecursively(dst, overwrite = true)
                                clipFile.deleteRecursively()
                            }
                            fileClipSource = null
                        } else {
                            clipFile.copyRecursively(dst, overwrite = true)
                        }
                        renderCurrentTab()
                    } catch (e: Throwable) {
                        Toast.makeText(this, "Ошибка вставки: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            actRow.addView(makeExpressiveChip("Открыть в терминале", false) {
                dlg.dismiss()
                nativeSendText("cd \"${currentDir.absolutePath}\"\n", false)
            })
            contentContainer.addView(actScroll)

            // File list
            val listScroll = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(320)
                ).apply {
                    topMargin = dp(6)
                }
            }
            val listCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            listScroll.addView(listCol)
            contentContainer.addView(listScroll)

            val entries = (currentDir.listFiles()?.toList() ?: emptyList())
                .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))

            if (entries.isEmpty()) {
                listCol.addView(TextView(this).apply {
                    text = "Папка пуста"
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(dp(8), dp(16), dp(8), dp(16))
                })
            } else {
                for (item in entries) {
                    val isDir = item.isDirectory
                    val sz = if (isDir) computeItemSizeBounded(item, 1) else item.length()
                    val rowCard = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(8), dp(10), dp(8))
                        background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(12))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(5)
                        }

                        addView(TextView(this@TermuxActivity).apply {
                            text = if (isDir) "▸" else "·"
                            setTextColor(if (isDir) uiAccent else COLOR_TEXT_DIM)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setPadding(0, 0, dp(10), 0)
                        })

                        val midCol = LinearLayout(this@TermuxActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        midCol.addView(TextView(this@TermuxActivity).apply {
                            text = item.name
                            setSingleLine(true)
                            setTextColor(if (isDir) COLOR_TEXT_BRIGHT else COLOR_TEXT)
                            typeface = Typeface.MONOSPACE
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        })
                        val perms = buildString {
                            append(if (isDir) "d" else "-")
                            append(if (item.canRead()) "r" else "-")
                            append(if (item.canWrite()) "w" else "-")
                            append(if (item.canExecute()) "x" else "-")
                        }
                        midCol.addView(TextView(this@TermuxActivity).apply {
                            text = "$perms · ${formatSize(sz)}"
                            setTextColor(COLOR_TEXT_DIM)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                        })
                        addView(midCol)

                        val moreBtn = TextView(this@TermuxActivity).apply {
                            text = "⋯"
                            setTextColor(COLOR_TEXT)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setPadding(dp(10), dp(4), dp(8), dp(4))
                            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(8))
                            setOnClickListener {
                                triggerHaptic(0.8f)
                                showFileActionsDialog(item)
                            }
                        }
                        addView(moreBtn)

                        setOnClickListener {
                            triggerHaptic(0.8f)
                            if (isDir) {
                                if (item.canRead()) {
                                    currentDir = item
                                    renderCurrentTab()
                                } else {
                                    Toast.makeText(this@TermuxActivity, "Нет прав чтения", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showFileActionsDialog(item)
                            }
                        }
                    }
                    listCol.addView(rowCard)
                }
            }
        }

        fun renderPackagesTab() {
            contentContainer.removeAllViews()
            val installed = parseInstalledDpkgPackages()
            val totalKb = installed.sumOf { it.sizeKb }

            contentContainer.addView(TextView(this).apply {
                text = "Установлено пакетов: ${installed.size} · Объём: ${formatSize(totalKb * 1024L)}"
                setTextColor(COLOR_TEXT_DIM)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(dp(2), dp(2), dp(2), dp(6))
            })

            // Global package actions: Update & Upgrade All, Clean Cache
            val (actScroll, actRow) = makeHorizontalChipRow()
            actRow.addView(makeExpressiveChip("Обновить все (apt upgrade)", true) {
                dlg.dismiss()
                nativeSendText("apt update && apt upgrade -y\n", false)
            })
            actRow.addView(makeExpressiveChip("Проверить обновления", false) {
                dlg.dismiss()
                nativeSendText("apt update && apt list --upgradable\n", false)
            })
            actRow.addView(makeExpressiveChip("Очистить кэш", false) {
                dlg.dismiss()
                nativeSendText("apt clean && apt autoremove -y\n", false)
            })
            contentContainer.addView(actScroll)

            // Quick install bar
            val installBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(4))
            }
            val pkgInput = EditText(this).apply {
                hint = "Имя пакета для установки (напр. git, python)"
                setHintTextColor(COLOR_TEXT_DIM)
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(10))
                setPadding(dp(10), dp(7), dp(10), dp(7))
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(6)
                }
            }
            installBar.addView(pkgInput)
            installBar.addView(makeExpressiveChip("+ Установить", true) {
                val pkgName = pkgInput.text.toString().trim()
                if (pkgName.isNotEmpty()) {
                    dlg.dismiss()
                    nativeSendText("apt install -y $pkgName\n", false)
                }
            })
            contentContainer.addView(installBar)

            // One-tap catalog of popular packages
            val (catScroll, catRow) = makeHorizontalChipRow()
            val popular = listOf("proot-distro", "tsu", "git", "python", "clang", "cmake", "neovim", "tmux", "htop", "fastfetch", "cmatrix", "curl", "openssh")
            val installedSet = installed.map { it.name }.toHashSet()
            for (p in popular) {
                if (!installedSet.contains(p)) {
                    catRow.addView(makeExpressiveChip("+ $p", false) {
                        dlg.dismiss()
                        nativeSendText("apt install -y $p\n", false)
                    })
                }
            }
            contentContainer.addView(catScroll)

            // Installed packages list with Remove button
            val pkgScroll = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(270)
                ).apply {
                    topMargin = dp(6)
                }
            }
            val pkgCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            pkgScroll.addView(pkgCol)
            contentContainer.addView(pkgScroll)

            for (pkg in installed) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(8), dp(10), dp(8))
                    background = pillBg(COLOR_CARD_BG, COLOR_BORDER, dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(5)
                    }

                    val infoCol = LinearLayout(this@TermuxActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    infoCol.addView(TextView(this@TermuxActivity).apply {
                        text = "${pkg.name} (${pkg.version})"
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    })
                    infoCol.addView(TextView(this@TermuxActivity).apply {
                        text = "${formatSize(pkg.sizeKb * 1024L)} · ${pkg.description}"
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                    })
                    addView(infoCol)

                    addView(makeExpressiveChip("Удалить", false, COLOR_ROOT, COLOR_ROOT_CONTAINER) {
                        dlg.dismiss()
                        nativeSendText("apt remove -y ${pkg.name}\n", false)
                    })
                }
                pkgCol.addView(card)
            }
        }

        renderCurrentTab = {
            val isFiles = (activeTab == 0)
            tabFilesBtn.setTextColor(if (isFiles) uiAccent else COLOR_TEXT)
            tabFilesBtn.background = pillBg(
                if (isFiles) uiAccentContainer else COLOR_CARD_BG,
                if (isFiles) uiAccent else COLOR_BORDER,
                dp(10)
            )
            tabPkgsBtn.setTextColor(if (!isFiles) uiAccent else COLOR_TEXT)
            tabPkgsBtn.background = pillBg(
                if (!isFiles) uiAccentContainer else COLOR_CARD_BG,
                if (!isFiles) uiAccent else COLOR_BORDER,
                dp(10)
            )
            if (isFiles) renderFilesTab() else renderPackagesTab()
        }

        renderCurrentTab()
        dlg.show()
    }

    internal fun syncModifierButtons(mask: Int) {
        val ctrl = ctrlLocked || (mask and 1) != 0
        val alt = altLocked || (mask and 2) != 0
        for (btn in modifierButtons) {
            val tag = btn.tag as? String ?: continue
            val isLocked = (tag == "CTRL" && ctrlLocked) || (tag == "ALT" && altLocked)
            val isActive = (tag == "CTRL" && ctrl) || (tag == "ALT" && alt)
            when {
                isLocked -> {
                    btn.setTextColor(COLOR_AMOLED_BG)
                    btn.background = pillBg(uiAccent, uiAccent, dp(5))
                }
                isActive -> {
                    btn.setTextColor(uiAccent)
                    btn.background = pillBg(uiAccentContainer, uiAccent, dp(5))
                }
                else -> {
                    btn.setTextColor(COLOR_TEXT)
                    btn.background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(5))
                }
            }
        }
    }

    internal fun createSession(asRoot: Boolean) {
        if (nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir) != 2) return
        terminalSurface.stopTextSelection()
        val rows = max(5, terminalSurface.rows)
        val cols = max(10, terminalSurface.cols)
        nativeNewSession(
            asRoot,
            rows,
            cols,
            terminalSurface.cellWidth.roundToInt(),
            terminalSurface.cellHeight.roundToInt()
        )
        rebuildTabs()
        terminalSurface.resetScrollAndBlink()
        terminalSurface.invalidateAllRows()
    }

    internal fun closeCurrentSession() {
        val active = nativeGetActiveIndex()
        if (active < 0) {
            finish()
            return
        }
        terminalSurface.stopTextSelection()
        val rem = nativeCloseSession(active)
        if (rem <= 0) {
            finish()
        } else {
            rebuildTabs()
            terminalSurface.invalidateAllRows()
        }
    }

    private fun showTabContextMenu(tabIndex: Int) {
        if (nativeGetSessionCount() > tabIndex && nativeGetActiveIndex() != tabIndex) {
            nativeSwitchSession(tabIndex)
            terminalSurface.syncSize()
            rebuildTabs()
            terminalSurface.invalidateAllRows()
        }
        val items = arrayOf(
            "Вставить из буфера",
            "История вывода",
            "Очистить экран (Ctrl+L)",
            "Закрыть сессию"
        )
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Сессия ${tabIndex + 1}")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> pasteFromClipboard()
                    1 -> terminalSurface.showTranscriptCopyDialog()
                    2 -> dispatchExtraKeyWithHoldSupport("CTRL+L")
                    3 -> closeCurrentSession()
                }
            }
            .show()
    }

    internal fun rebuildTabs() {
        tabStrip.removeAllViews()
        val rawCount = nativeGetSessionCount()
        val count = max(1, rawCount)
        val active = if (rawCount > 0) nativeGetActiveIndex() else 0
        for (i in 0 until count) {
            val idx = i
            val isRoot = if (rawCount > 0) nativeIsSessionRoot(i) else false
            val isSelected = i == active

            val tab = TextView(this).apply {
                text = (if (isRoot) "# " else "$ ") + (i + 1)
                setSingleLine(true)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(dp(10), dp(3), dp(10), dp(3))
                val accent = if (isRoot) COLOR_ROOT else uiAccent
                val activeFill = if (isRoot) COLOR_ROOT_CONTAINER else uiAccentContainer
                setTextColor(if (isSelected) accent else COLOR_TEXT_DIM)
                background = pillBg(
                    if (isSelected) activeFill else COLOR_SURFACE,
                    if (isSelected) accent else COLOR_BORDER,
                    dp(6)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(4), 0)
                }
                setOnClickListener {
                    if (nativeGetSessionCount() > idx) {
                        triggerHaptic(0.8f)
                        terminalSurface.stopTextSelection()
                        if (nativeGetActiveIndex() == idx) {
                            terminalSurface.resetScrollAndBlink()
                        } else {
                            nativeSwitchSession(idx)
                            terminalSurface.syncSize()
                            rebuildTabs()
                            terminalSurface.invalidateAllRows()
                        }
                    }
                }
                setOnLongClickListener {
                    if (nativeGetSessionCount() > idx) {
                        triggerHaptic(1.3f)
                        showTabContextMenu(idx)
                    }
                    true
                }
            }
            tabStrip.addView(tab)
        }
    }

    internal fun copyToClipboard(text: String?, showToast: Boolean) {
        if (text.isNullOrEmpty()) return
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Termux", text))
        if (showToast) {
            Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun pasteFromClipboard() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)
        if (!text.isNullOrEmpty()) {
            nativeSendText(text.toString(), true)
            rearmLockedModifiersIfNeeded()
            terminalSurface.resetScrollAndBlink()
        }
    }

    internal fun hasClipboardText(): Boolean {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = cm.primaryClip ?: return false
        return clip.itemCount > 0 && !clip.getItemAt(0).coerceToText(this).isNullOrEmpty()
    }

    private fun makeHeaderButton(label: String, onClick: () -> Unit): TextView =
        makeHeaderButton(label, onClick, null)

    private fun makeHeaderButton(
        label: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)?
    ): TextView {
        return TextView(this).apply {
            text = label
            setSingleLine(true)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(COLOR_TEXT)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(26)).apply {
                setMargins(dp(3), 0, 0, 0)
            }
            setOnClickListener {
                triggerHaptic(0.85f)
                onClick()
            }
            if (onLongClick != null) {
                setOnLongClickListener {
                    triggerHaptic(1.3f)
                    onLongClick()
                    true
                }
            }
        }
    }

    private fun pillBg(fill: Int, stroke: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx.toFloat()
            setStroke(1, stroke)
        }
    }

    internal fun dp(v: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }

    internal fun dpF(v: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v,
            resources.displayMetrics
        )
    }

    private fun requestHighRefreshRate() {
        val d = display ?: return
        val best = d.supportedModes.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = best.modeId
        }
    }

    private fun startForegroundKeepAlive() {
        try {
            startForegroundService(Intent(this, KeepAliveService::class.java))
        } catch (_: Throwable) {
        }
    }

    // ========================================================================
    // Terminal Surface (VS Code Cursor Animations + Gboard Pre-Delete & Paste)
    // ========================================================================
    inner class TerminalSurface(context: Context) : View(context) {

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val graphicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val wavePath = Path()
        private val cursorRectF = RectF()

        private val baseTypeface: Typeface
        private val boldTypeface: Typeface
        private val italicTypeface: Typeface
        private val boldItalicTypeface: Typeface

        internal var cellWidth = 18f
        internal var cellHeight = 36f
        private var cellBaseline = 28f
        internal var fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.45f, 3.0f)

        internal var rows = 24
        internal var cols = 80
        internal var scrollOffset = 0
        private var scrollAccum = 0f

        private var activityResumed = true
        internal var cursorShapePref = prefs.getInt(KEY_CURSOR_SHAPE, DEFAULT_CURSOR_SHAPE)
        internal var cursorBlinkStyle = prefs.getInt(KEY_CURSOR_BLINK_STYLE, DEFAULT_CURSOR_BLINK_STYLE)
        internal var cursorAlphaPct = prefs.getInt(KEY_CURSOR_ALPHA_PCT, DEFAULT_CURSOR_ALPHA_PCT).coerceIn(15, 100)
        internal var cursorRadiusTenths = prefs.getInt(KEY_CURSOR_RADIUS_TENTHS, DEFAULT_CURSOR_RADIUS_TENTHS).coerceIn(0, 60)
        internal var cursorFadeMs = prefs.getInt(KEY_CURSOR_FADE_MS, DEFAULT_CURSOR_FADE_MS).coerceIn(200, 1200)
        internal var cursorSmoothMove = prefs.getBoolean(KEY_CURSOR_SMOOTH_MOVE, DEFAULT_CURSOR_SMOOTH_MOVE)
        internal var cursorGlideMs = prefs.getInt(KEY_CURSOR_GLIDE_MS, DEFAULT_CURSOR_GLIDE_MS).coerceIn(30, 220)

        private var cursorBlinkOn = true
        private var lastInputTimeMs = SystemClock.uptimeMillis()

        // VS Code Smooth Caret Interpolation State
        private var animCursorX = 0f
        private var animCursorY = 0f
        private var cursorPosInitialized = false
        private var lastCursorFrameNs = 0L

        // Selection Handle Auto-Scroll State (never cancelled by ACTION_MOVE jitter!)
        private var isDraggingSelectionHandle = false
        private var isAutoScrollRunning = false
        private var lastDragTouchX = 0f
        private var lastDragTouchY = 0f

        private val bgRunsBuf = IntArray(512 * 3)
        private val glyphRunsBuf = IntArray(1024 * 4)
        private val charBuf = CharArray(2)
        private val selGeomBuf = FloatArray(14)

        private var rowNodes: Array<RenderNode?>? = null
        private var rowNodesValid: BooleanArray? = null
        private var cachedScrollOffset = Int.MIN_VALUE

        private var actionMode: ActionMode? = null
        private val handleRadiusPx = dpF(6.5f)
        private val handleInnerRadiusPx = dpF(4.7f)
        private val handleStemWidthPx = dpF(2.2f)

        private val gestureDetector: GestureDetector
        private val scaleDetector: ScaleGestureDetector

        private val stepBlinkRunnable = object : Runnable {
            override fun run() {
                if (!activityResumed || cursorBlinkStyle != 3) return
                if (scrollOffset == 0 && !nativeIsSelectingText()) {
                    cursorBlinkOn = !cursorBlinkOn
                    postInvalidateOnAnimation()
                }
                postDelayed(this, cursorFadeMs.toLong())
            }
        }

        private val selectionAutoScrollRunnable = object : Runnable {
            override fun run() {
                if (!isDraggingSelectionHandle || !nativeIsSelectingText()) {
                    isAutoScrollRunning = false
                    return
                }
                val edgeZone = cellHeight * 2.2f
                var delta = 0
                if (lastDragTouchY < edgeZone) {
                    delta = if (lastDragTouchY < cellHeight * 0.4f) 2 else 1
                } else if (lastDragTouchY > height - edgeZone) {
                    delta = if (lastDragTouchY > height - cellHeight * 0.4f) -2 else -1
                }
                if (delta != 0) {
                    val maxSb = nativeGetActiveScrollback()
                    val newOffset = (scrollOffset + delta).coerceIn(0, maxSb)
                    scrollOffset = newOffset
                    nativeOnSelectionTouchPx(
                        MotionEvent.ACTION_MOVE,
                        scrollOffset,
                        lastDragTouchX,
                        lastDragTouchY.coerceIn(0f, max(1f, height.toFloat() - 1f)),
                        cellWidth,
                        cellHeight,
                        width.toFloat(),
                        height.toFloat(),
                        handleRadiusPx
                    )
                    invalidateAllRows()
                    postDelayed(this, 36L)
                } else {
                    isAutoScrollRunning = false
                }
            }
        }

        init {
            isFocusable = true
            isFocusableInTouchMode = true
            defaultFocusHighlightEnabled = false
            setBackgroundColor(COLOR_AMOLED_BG)

            baseTypeface = try {
                Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
            } catch (_: Throwable) {
                Typeface.MONOSPACE
            }
            boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)
            italicTypeface = Typeface.create(baseTypeface, Typeface.ITALIC)
            boldItalicTypeface = Typeface.create(baseTypeface, Typeface.BOLD_ITALIC)

            textPaint.typeface = baseTypeface
            textPaint.fontFeatureSettings = "'liga' 0, 'calt' 0"
            graphicPaint.style = Paint.Style.FILL

            updateMetrics(false)
            scheduleBlinkLoop()

            gestureDetector = GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        if (nativeIsSelectingText()) {
                            stopTextSelection()
                            return true
                        }
                        requestFocus()
                        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showSoftInput(this@TerminalSurface, 0)

                        val modes = nativeGetModesMask()
                        if ((modes and 8) != 0) {
                            val col = (e.x / cellWidth).toInt() + 1
                            val row = (e.y / cellHeight).toInt() + 1
                            nativeSendMouse(0, col, row, true)
                            nativeSendMouse(0, col, row, false)
                        }
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        if (scaleDetector.isInProgress) return
                        triggerHaptic(1.35f)
                        val col = (e.x / cellWidth).toInt().coerceIn(0, max(0, cols - 1))
                        val row = (e.y / cellHeight).toInt().coerceIn(0, max(0, rows - 1))
                        nativeStartTextSelection(scrollOffset, col, row)
                        showColorOsFloatingActionMode()
                        invalidateAllRows()
                    }

                    override fun onScroll(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float
                    ): Boolean {
                        if (scaleDetector.isInProgress) return false
                        scrollAccum += distanceY
                        val deltaRows = (scrollAccum / cellHeight).toInt()
                        if (deltaRows != 0) {
                            scrollAccum -= deltaRows * cellHeight
                            val maxSb = nativeGetActiveScrollback()
                            scrollOffset = (scrollOffset - deltaRows).coerceIn(0, maxSb)
                            actionMode?.invalidateContentRect()
                            invalidateAllRows()
                        }
                        return true
                    }
                }
            )

            scaleDetector = ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                        stopTextSelection()
                        return true
                    }

                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val prevScale = fontScale
                        fontScale = (fontScale * detector.scaleFactor).coerceIn(0.45f, 3.0f)
                        if (abs(fontScale - prevScale) > 0.004f) {
                            updateMetrics(false)
                            cursorPosInitialized = false
                            syncSize()
                        }
                        return true
                    }

                    override fun onScaleEnd(detector: ScaleGestureDetector) {
                        prefs.edit().putFloat(KEY_FONT_SCALE, fontScale).apply()
                        updateMetrics(true)
                        cursorPosInitialized = false
                        syncSize()
                    }
                }
            )
        }

        internal fun setCustomFontScale(newScale: Float) {
            fontScale = newScale.coerceIn(0.45f, 3.0f)
            updateMetrics(true)
            cursorPosInitialized = false
            syncSize()
        }

        private fun scheduleBlinkLoop() {
            removeCallbacks(stepBlinkRunnable)
            cursorBlinkOn = true
            lastInputTimeMs = SystemClock.uptimeMillis()
            if (activityResumed && cursorBlinkStyle == 3) {
                postDelayed(stepBlinkRunnable, cursorFadeMs.toLong())
            }
            postInvalidateOnAnimation()
        }

        internal fun onActivityResume() {
            activityResumed = true
            scheduleBlinkLoop()
        }

        internal fun onActivityPause() {
            activityResumed = false
            removeCallbacks(stepBlinkRunnable)
            removeCallbacks(selectionAutoScrollRunnable)
            isDraggingSelectionHandle = false
            isAutoScrollRunning = false
            stopTextSelection()
        }

        internal fun showTranscriptCopyDialog() {
            val fullText = nativeGetTranscriptText()
            val tv = TextView(context).apply {
                text = fullText.ifEmpty { "Буфер пуст." }
                setTextIsSelectable(true)
                typeface = Typeface.MONOSPACE
                setTextColor(COLOR_TEXT_BRIGHT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            val scroll = ScrollView(context).apply {
                addView(tv)
            }
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("История сессии")
                .setView(scroll)
                .setPositiveButton("Копировать всё") { _, _ ->
                    copyToClipboard(fullText, true)
                }
                .setNeutralButton("Вставить") { _, _ ->
                    pasteFromClipboard()
                }
                .setNegativeButton("Закрыть", null)
                .show()
        }

        private fun showColorOsFloatingActionMode() {
            actionMode?.finish()
            val callback = object : ActionMode.Callback2() {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    val show = MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
                    menu.add(Menu.NONE, ITEM_COPY, Menu.NONE, "Копировать").setShowAsAction(show)
                    menu.add(Menu.NONE, ITEM_PASTE, Menu.NONE, "Вставить")
                        .setEnabled(hasClipboardText())
                        .setShowAsAction(show)
                    menu.add(Menu.NONE, ITEM_SELECT_ALL, Menu.NONE, "Выбрать всё").setShowAsAction(show)
                    menu.add(Menu.NONE, ITEM_SHARE, Menu.NONE, "Поделиться")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    menu.findItem(ITEM_PASTE)?.isEnabled = hasClipboardText()
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    when (item.itemId) {
                        ITEM_COPY -> {
                            copyToClipboard(nativeGetSelectedText(), true)
                            stopTextSelection()
                        }
                        ITEM_PASTE -> {
                            stopTextSelection()
                            pasteFromClipboard()
                        }
                        ITEM_SELECT_ALL -> {
                            nativeSelectAll(scrollOffset)
                            invalidateAllRows()
                            actionMode?.invalidateContentRect()
                        }
                        ITEM_SHARE -> {
                            val selected = nativeGetSelectedText()
                            stopTextSelection()
                            if (selected.isNotEmpty()) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, selected)
                                }
                                startActivity(Intent.createChooser(sendIntent, "Поделиться"))
                            }
                        }
                    }
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode) {}

                override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                    if (nativeGetSelectionGeometryPx(
                            scrollOffset,
                            cellWidth,
                            cellHeight,
                            width.toFloat(),
                            height.toFloat(),
                            handleRadiusPx,
                            selGeomBuf
                        )
                    ) {
                        outRect.set(
                            selGeomBuf[10].roundToInt(),
                            selGeomBuf[11].roundToInt(),
                            selGeomBuf[12].roundToInt(),
                            selGeomBuf[13].roundToInt()
                        )
                    } else {
                        outRect.set(0, 0, width, height)
                    }
                }
            }
            actionMode = startActionMode(callback, ActionMode.TYPE_FLOATING)
        }

        internal fun stopTextSelection(): Boolean {
            removeCallbacks(selectionAutoScrollRunnable)
            isDraggingSelectionHandle = false
            isAutoScrollRunning = false
            actionMode?.finish()
            actionMode = null
            if (!nativeIsSelectingText()) return false
            val stopped = nativeStopTextSelection()
            invalidateAllRows()
            return stopped
        }

        internal fun isSelectingText(): Boolean = nativeIsSelectingText()

        private fun updateMetrics(forceRebuildCache: Boolean) {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                13.5f * fontScale,
                resources.displayMetrics
            )
            textPaint.textSize = px
            textPaint.typeface = baseTypeface
            val fm = textPaint.fontMetrics
            cellWidth = max(6f, textPaint.measureText("M"))
            cellHeight = max(12f, ceil((fm.descent - fm.ascent + fm.leading).toDouble()).toFloat())
            cellBaseline = -fm.ascent
            if (forceRebuildCache) {
                rowNodes = null
                rowNodesValid = null
            }
        }

        internal fun resetScrollAndBlink() {
            scrollOffset = 0
            scheduleBlinkLoop()
            invalidateAllRows()
        }

        internal fun invalidateAllRows() {
            rowNodesValid?.fill(false)
            postInvalidateOnAnimation()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            syncSize()
        }

        internal fun syncSize() {
            val w = width
            val h = height
            if (w <= 0 || h <= 0) return
            val newCols = max(10, (w / cellWidth).toInt())
            val newRows = max(4, (h / cellHeight).toInt())
            val changed = (newCols != cols || newRows != rows)
            cols = newCols
            rows = newRows
            if (changed || rowNodes == null || rowNodes?.size != rows) {
                rowNodes = arrayOfNulls(rows)
                rowNodesValid = BooleanArray(rows)
            } else {
                rowNodesValid?.fill(false)
            }
            nativeResizeActive(rows, cols, cellWidth.roundToInt(), cellHeight.roundToInt())
            postInvalidateOnAnimation()
        }

        override fun onDraw(canvas: Canvas) {
            bgPaint.color = COLOR_AMOLED_BG
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val bootState = nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
            if (bootState != 2) {
                textPaint.color = uiAccent
                textPaint.typeface = baseTypeface
                canvas.drawText(
                    "Инициализация...",
                    dp(16).toFloat(),
                    height / 2f,
                    textPaint
                )
                postInvalidateDelayed(40L)
                return
            }

            if (nativeGetSessionCount() == 0) {
                nativeNewSession(
                    false,
                    max(5, rows),
                    max(10, cols),
                    cellWidth.roundToInt(),
                    cellHeight.roundToInt()
                )
                post { rebuildTabs() }
                rowNodesValid?.fill(false)
            }

            val oscClip = nativeConsumeOsc52Clipboard()
            if (!oscClip.isNullOrEmpty()) {
                copyToClipboard(oscClip, false)
            }

            val engineDirty = nativeConsumeFrameDirty()
            if (engineDirty) {
                lastInputTimeMs = SystemClock.uptimeMillis()
            }
            val nodes = rowNodes ?: arrayOfNulls<RenderNode>(rows).also { rowNodes = it }
            val valid = rowNodesValid ?: BooleanArray(rows).also { rowNodesValid = it }

            if (scrollOffset != cachedScrollOffset) {
                valid.fill(false)
                cachedScrollOffset = scrollOffset
            } else if (engineDirty) {
                for (r in 0 until min(rows, valid.size)) {
                    if (nativeIsRowDirty(r)) {
                        valid[r] = false
                    }
                }
                nativeClearDirtyFlags()
            }

            if (canvas.isHardwareAccelerated) {
                val rowHeightPx = ceil(cellHeight.toDouble()).toInt()
                for (r in 0 until rows) {
                    val top = r * cellHeight
                    var node = nodes[r]
                    if (node == null) {
                        node = RenderNode("row_$r")
                        nodes[r] = node
                        valid[r] = false
                    }
                    if (!valid[r] || !node.hasDisplayList()) {
                        node.setPosition(0, 0, width, rowHeightPx)
                        val rc = node.beginRecording()
                        drawSingleRow(rc, r, 0f)
                        node.endRecording()
                        valid[r] = true
                    }
                    canvas.save()
                    canvas.translate(0f, top)
                    canvas.drawRenderNode(node)
                    canvas.restore()
                }
            } else {
                for (r in 0 until rows) {
                    drawSingleRow(canvas, r, r * cellHeight)
                }
            }

            val selecting = nativeIsSelectingText()

            // Minimalist right-edge scrollbar indicator when viewing scrollback history
            if (scrollOffset > 0 && height > 0) {
                val maxSb = nativeGetActiveScrollback()
                if (maxSb > 0) {
                    val totalRows = maxSb + rows
                    val thumbH = max(dpF(20f), height.toFloat() * (rows.toFloat() / totalRows.toFloat()))
                    val fractionFromTop = 1f - (scrollOffset.toFloat() / maxSb.toFloat()).coerceIn(0f, 1f)
                    val thumbTop = fractionFromTop * (height - thumbH)
                    bgPaint.style = Paint.Style.FILL
                    bgPaint.color = (0x55 shl 24) or (uiAccent and 0x00FFFFFF)
                    cursorRectF.set(width - dpF(2.5f), thumbTop, width - dpF(0.5f), thumbTop + thumbH)
                    canvas.drawRoundRect(cursorRectF, dpF(1f), dpF(1f), bgPaint)
                }
            }

            // Draw Cursor (True VSYNC VS Code Smooth / Phase / Expand / Blink / Solid)
            if (scrollOffset == 0 && !selecting) {
                val cursorState = nativeGetCursorState()
                if (cursorState >= 0) {
                    val rawShape = (cursorState ushr 29) and 3
                    val shape = if (cursorShapePref == 3 && rawShape == 0) 3 else if (rawShape in 1..2) rawShape else cursorShapePref
                    val cRow = (cursorState ushr 16) and 0x1FFF
                    val cCol = cursorState and 0xFFFF
                    if (cRow in 0 until rows && cCol in 0 until cols) {
                        val targetX = cCol * cellWidth
                        val targetY = cRow * cellHeight
                        val nowNs = System.nanoTime()

                        var stillMoving = false
                        if (!cursorPosInitialized || !cursorSmoothMove) {
                            animCursorX = targetX
                            animCursorY = targetY
                            cursorPosInitialized = true
                        } else {
                            val distCells = hypot(
                                (targetX - animCursorX) / max(1f, cellWidth),
                                (targetY - animCursorY) / max(1f, cellHeight)
                            )
                            if (distCells > 18f) {
                                animCursorX = targetX
                                animCursorY = targetY
                            } else {
                                val dtSec = if (lastCursorFrameNs > 0L) {
                                    ((nowNs - lastCursorFrameNs) * 1e-9f).coerceIn(0.001f, 0.05f)
                                } else {
                                    0.008f
                                }
                                val tauSec = max(0.015f, cursorGlideMs / 1000f * 0.45f)
                                val factor = (1f - exp(-dtSec / tauSec)).coerceIn(0.12f, 1f)
                                animCursorX += (targetX - animCursorX) * factor
                                animCursorY += (targetY - animCursorY) * factor
                                if (abs(targetX - animCursorX) > 0.3f || abs(targetY - animCursorY) > 0.3f) {
                                    stillMoving = true
                                } else {
                                    animCursorX = targetX
                                    animCursorY = targetY
                                }
                            }
                        }
                        lastCursorFrameNs = nowNs

                        val baseMaxAlpha = (cursorAlphaPct.coerceIn(15, 100) / 100f)
                        val elapsedMs = SystemClock.uptimeMillis() - lastInputTimeMs
                        val isIdleSleep = (elapsedMs > 10_000L) // VS Code #22900 10s idle freeze for 0% CPU

                        var alphaMul = 1f
                        var scaleYMul = 1f
                        var keepVSyncAnimating = stillMoving

                        if (!isIdleSleep) {
                            val holdSolidMs = 300L
                            when (cursorBlinkStyle) {
                                0 -> {
                                    // VS Code Smooth: symmetric alternate ease-in-out with 18% plateau at 1.0 and 0.0
                                    keepVSyncAnimating = true
                                    if (elapsedMs >= holdSolidMs) {
                                        val halfMs = max(200L, cursorFadeMs.toLong())
                                        val elapsedBlink = elapsedMs - holdSolidMs
                                        val halfIdx = elapsedBlink / halfMs
                                        val rawU = (elapsedBlink % halfMs).toFloat() / halfMs.toFloat()
                                        val u = if (halfIdx % 2L == 0L) rawU else (1f - rawU)
                                        alphaMul = when {
                                            u <= 0.18f -> 1f
                                            u >= 0.82f -> 0f
                                            else -> {
                                                val t = (u - 0.18f) / 0.64f
                                                0.5f * (1f + cos(Math.PI.toFloat() * t))
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // VS Code Phase: pure continuous cosine breathing wave
                                    keepVSyncAnimating = true
                                    if (elapsedMs >= holdSolidMs) {
                                        val fullMs = max(400f, cursorFadeMs * 2f)
                                        val phase = ((elapsedMs - holdSolidMs).toFloat() / fullMs) * (2f * Math.PI.toFloat())
                                        alphaMul = 0.5f + 0.5f * cos(phase)
                                    }
                                }
                                2 -> {
                                    // VS Code Expand: vertical scaleY collapse/expand towards center
                                    keepVSyncAnimating = true
                                    if (elapsedMs >= holdSolidMs) {
                                        val halfMs = max(200L, cursorFadeMs.toLong())
                                        val elapsedBlink = elapsedMs - holdSolidMs
                                        val halfIdx = elapsedBlink / halfMs
                                        val rawU = (elapsedBlink % halfMs).toFloat() / halfMs.toFloat()
                                        val u = if (halfIdx % 2L == 0L) rawU else (1f - rawU)
                                        scaleYMul = when {
                                            u <= 0.15f -> 1f
                                            u >= 0.85f -> 0.05f
                                            else -> {
                                                val t = (u - 0.15f) / 0.70f
                                                0.05f + 0.95f * (0.5f * (1f + cos(Math.PI.toFloat() * t)))
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    // Classic step Blink
                                    alphaMul = if (cursorBlinkOn) 1f else 0f
                                }
                                else -> {
                                    // Solid
                                    alphaMul = 1f
                                }
                            }
                        }

                        val finalAlpha = baseMaxAlpha * alphaMul
                        if (finalAlpha > 0.01f && scaleYMul > 0.02f) {
                            val aInt = (finalAlpha * 255f).roundToInt().coerceIn(0, 255)
                            bgPaint.color = (aInt shl 24) or (uiAccent and 0x00FFFFFF)
                            val cx = animCursorX
                            val cy = animCursorY
                            val cornerPx = dpF(cursorRadiusTenths / 10f)
                            val centerY = cy + cellHeight * 0.5f
                            val halfH = (cellHeight * 0.5f - dpF(0.6f)) * scaleYMul

                            when (shape) {
                                1 -> { // Underline
                                    bgPaint.style = Paint.Style.FILL
                                    val h = dpF(2.4f)
                                    val r = min(cornerPx, h * 0.5f)
                                    cursorRectF.set(cx, cy + cellHeight - h, cx + cellWidth, cy + cellHeight)
                                    canvas.drawRoundRect(cursorRectF, r, r, bgPaint)
                                }
                                2 -> { // Beam / Bar
                                    bgPaint.style = Paint.Style.FILL
                                    val w = dpF(2.2f)
                                    val r = min(cornerPx, w * 0.5f)
                                    cursorRectF.set(cx, centerY - halfH, cx + w, centerY + halfH)
                                    canvas.drawRoundRect(cursorRectF, r, r, bgPaint)
                                }
                                3 -> { // Outline Block
                                    bgPaint.style = Paint.Style.STROKE
                                    bgPaint.strokeWidth = dpF(1.6f)
                                    cursorRectF.set(cx + dpF(0.8f), centerY - halfH, cx + cellWidth - dpF(0.8f), centerY + halfH)
                                    canvas.drawRoundRect(cursorRectF, cornerPx, cornerPx, bgPaint)
                                    bgPaint.style = Paint.Style.FILL
                                }
                                else -> { // Rounded Block (default 3.0dp radius, 85% alpha)
                                    bgPaint.style = Paint.Style.FILL
                                    cursorRectF.set(cx, centerY - halfH, cx + cellWidth, centerY + halfH)
                                    canvas.drawRoundRect(cursorRectF, cornerPx, cornerPx, bgPaint)
                                }
                            }
                        }

                        if (activityResumed && keepVSyncAnimating) {
                            postInvalidateOnAnimation()
                        }
                    }
                }
            }

            // Draw Selection Handles
            if (selecting && nativeGetSelectionGeometryPx(
                    scrollOffset,
                    cellWidth,
                    cellHeight,
                    width.toFloat(),
                    height.toFloat(),
                    handleRadiusPx,
                    selGeomBuf
                )
            ) {
                drawColorOs17Handle(
                    canvas,
                    selGeomBuf[0],
                    selGeomBuf[1],
                    selGeomBuf[2],
                    selGeomBuf[3],
                    selGeomBuf[4]
                )
                drawColorOs17Handle(
                    canvas,
                    selGeomBuf[5],
                    selGeomBuf[6],
                    selGeomBuf[7],
                    selGeomBuf[8],
                    selGeomBuf[9]
                )
            }
        }

        private fun drawColorOs17Handle(
            canvas: Canvas,
            stemX: Float,
            stemTopY: Float,
            stemBotY: Float,
            circleX: Float,
            circleY: Float
        ) {
            if (stemBotY < -handleRadiusPx * 2f || stemTopY > height + handleRadiusPx * 2f) return

            handlePaint.style = Paint.Style.STROKE
            handlePaint.strokeWidth = handleStemWidthPx
            handlePaint.color = COLOR_HANDLE_STEM
            canvas.drawLine(stemX, stemTopY, stemX, stemBotY, handlePaint)

            handlePaint.style = Paint.Style.FILL
            handlePaint.color = COLOR_HANDLE_RING
            canvas.drawCircle(circleX, circleY, handleRadiusPx, handlePaint)

            handlePaint.color = COLOR_HANDLE_FILL
            canvas.drawCircle(circleX, circleY, handleInnerRadiusPx, handlePaint)
        }

        private fun drawSingleRow(rc: Canvas, r: Int, top: Float) {
            val bottom = top + cellHeight
            val baseY = top + cellBaseline

            val packed = nativePackRowRender(scrollOffset, r, bgRunsBuf, glyphRunsBuf)
            val bgCount = (packed ushr 16) and 0xFFFF
            val glCount = packed and 0xFFFF

            bgPaint.style = Paint.Style.FILL
            for (i in 0 until bgCount) {
                val base = i * 3
                val startCol = bgRunsBuf[base]
                val endCol = bgRunsBuf[base + 1]
                bgPaint.color = bgRunsBuf[base + 2]
                rc.drawRect(startCol * cellWidth, top, endCol * cellWidth, bottom, bgPaint)
            }

            var lastFlags = -1
            for (i in 0 until glCount) {
                val base = i * 4
                val cp = glyphRunsBuf[base]
                val col = glyphRunsBuf[base + 1]
                val fg = glyphRunsBuf[base + 2]
                val flags = glyphRunsBuf[base + 3]
                val x = col * cellWidth

                if (cp == -1) {
                    val x0 = (flags ushr 24) and 0xFF
                    val y0 = (flags ushr 16) and 0xFF
                    val x1 = (flags ushr 8) and 0xFF
                    val y1 = flags and 0xFF
                    graphicPaint.color = fg
                    graphicPaint.style = Paint.Style.FILL
                    rc.drawRect(
                        x + cellWidth * (x0 * 0.0625f),
                        top + cellHeight * (y0 * 0.0625f),
                        x + cellWidth * (x1 * 0.0625f),
                        top + cellHeight * (y1 * 0.0625f),
                        graphicPaint
                    )
                    continue
                }

                val styleBits = flags and 5
                if (styleBits != lastFlags) {
                    lastFlags = styleBits
                    textPaint.typeface = when {
                        (styleBits and 5) == 5 -> boldItalicTypeface
                        (styleBits and 1) != 0 -> boldTypeface
                        (styleBits and 4) != 0 -> italicTypeface
                        else -> baseTypeface
                    }
                }
                textPaint.color = fg
                val charCount = Character.toChars(cp, charBuf, 0)
                rc.drawText(charBuf, 0, charCount, x, baseY, textPaint)

                if ((flags and (8 or 1024 or 2048)) != 0) {
                    graphicPaint.color = fg
                    graphicPaint.strokeWidth = max(1.5f, cellHeight * 0.05f)
                    if ((flags and 1024) != 0) {
                        graphicPaint.style = Paint.Style.STROKE
                        wavePath.reset()
                        val wy = bottom - graphicPaint.strokeWidth * 1.5f
                        val amp = max(1.5f, cellHeight * 0.06f)
                        wavePath.moveTo(x, wy)
                        wavePath.quadTo(x + cellWidth * 0.25f, wy - amp, x + cellWidth * 0.5f, wy)
                        wavePath.quadTo(x + cellWidth * 0.75f, wy + amp, x + cellWidth, wy)
                        rc.drawPath(wavePath, graphicPaint)
                        graphicPaint.style = Paint.Style.FILL
                    } else if ((flags and 2048) != 0) {
                        val u1 = bottom - graphicPaint.strokeWidth * 3f
                        val u2 = bottom - graphicPaint.strokeWidth
                        rc.drawLine(x, u1, x + cellWidth, u1, graphicPaint)
                        rc.drawLine(x, u2, x + cellWidth, u2, graphicPaint)
                    } else {
                        val u = bottom - graphicPaint.strokeWidth
                        rc.drawLine(x, u, x + cellWidth, u, graphicPaint)
                    }
                }

                if ((flags and 128) != 0) {
                    graphicPaint.color = fg
                    graphicPaint.strokeWidth = max(1.5f, cellHeight * 0.05f)
                    val sy = top + cellHeight * 0.52f
                    rc.drawLine(x, sy, x + cellWidth, sy, graphicPaint)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (nativeIsSelectingText() && event.pointerCount == 1) {
                lastDragTouchX = event.x
                lastDragTouchY = event.y
                val dragRes = nativeOnSelectionTouchPx(
                    event.actionMasked,
                    scrollOffset,
                    event.x,
                    event.y,
                    cellWidth,
                    cellHeight,
                    width.toFloat(),
                    height.toFloat(),
                    handleRadiusPx
                )
                if (dragRes == 1) {
                    isDraggingSelectionHandle = true
                    actionMode?.hide(-1)
                    val edgeZone = cellHeight * 2.2f
                    if (event.y < edgeZone || event.y > height - edgeZone) {
                        if (!isAutoScrollRunning) {
                            isAutoScrollRunning = true
                            postDelayed(selectionAutoScrollRunnable, 36L)
                        }
                    } else {
                        isAutoScrollRunning = false
                        removeCallbacks(selectionAutoScrollRunnable)
                    }
                    invalidateAllRows()
                    return true
                } else if (dragRes == 2) {
                    isDraggingSelectionHandle = false
                    isAutoScrollRunning = false
                    removeCallbacks(selectionAutoScrollRunnable)
                    actionMode?.hide(0)
                    actionMode?.invalidateContentRect()
                    invalidateAllRows()
                    return true
                }
            }

            scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
            }
            return true
        }

        override fun onCheckIsTextEditor(): Boolean = true

        /**
         * Gboard-Compatible InputConnection:
         * 1. Uses `InputType.TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or TYPE_TEXT_FLAG_NO_SUGGESTIONS`
         *    so Gboard ALWAYS displays its top suggestion bar & inline clipboard paste chip!
         * 2. Mirrors the current terminal command line before the cursor into `mirrorEditable`
         *    so when you swipe left on Gboard's Backspace key (`⌫`), Gboard selects words via
         *    `setSelection(start, end)` and we highlight those exact words in the terminal via
         *    `nativeSetPreDeleteCount(end - start)` before deleting them on release!
         */
        override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI

            val initLine = nativeGetInputLineBeforeCursor()
            outAttrs.initialSelStart = initLine.length
            outAttrs.initialSelEnd = initLine.length

            return object : BaseInputConnection(this, true) {
                private val mirrorBuf = SpannableStringBuilder(initLine)
                private var preDeleteHighlighted = 0

                private fun syncMirrorWithTerminal() {
                    val cur = nativeGetInputLineBeforeCursor()
                    mirrorBuf.replace(0, mirrorBuf.length, cur)
                    Selection.setSelection(mirrorBuf, mirrorBuf.length)
                }

                override fun getEditable(): Editable = mirrorBuf

                override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence {
                    if (preDeleteHighlighted == 0) {
                        syncMirrorWithTerminal()
                    }
                    return super.getTextBeforeCursor(length, flags) ?: ""
                }

                override fun setSelection(start: Int, end: Int): Boolean {
                    val s = start.coerceIn(0, mirrorBuf.length)
                    val e = end.coerceIn(0, mirrorBuf.length)
                    val minPos = min(s, e)
                    val maxPos = max(s, e)
                    val preCount = if (maxPos > minPos) (maxPos - minPos) else 0
                    preDeleteHighlighted = preCount
                    nativeSetPreDeleteCount(preCount)
                    return super.setSelection(s, e)
                }

                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    val committed = text?.toString() ?: ""
                    stopTextSelection()
                    if (preDeleteHighlighted > 0 && committed.isEmpty()) {
                        val toDel = preDeleteHighlighted
                        preDeleteHighlighted = 0
                        nativeSetPreDeleteCount(0)
                        for (i in 0 until toDel) {
                            nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                        }
                    } else if (committed.isNotEmpty()) {
                        if (preDeleteHighlighted > 0) {
                            val toDel = preDeleteHighlighted
                            preDeleteHighlighted = 0
                            nativeSetPreDeleteCount(0)
                            for (i in 0 until toDel) {
                                nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                            }
                        }
                        nativeSendText(committed, committed.length > 1)
                    }
                    rearmLockedModifiersIfNeeded()
                    resetScrollAndBlink()
                    syncMirrorWithTerminal()
                    return true
                }

                override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    val comp = text?.toString() ?: ""
                    if (comp.isNotEmpty()) {
                        return commitText(comp, newCursorPosition)
                    }
                    return true
                }

                override fun finishComposingText(): Boolean {
                    if (preDeleteHighlighted > 0) {
                        preDeleteHighlighted = 0
                        nativeSetPreDeleteCount(0)
                    }
                    return true
                }

                override fun replaceText(
                    start: Int,
                    end: Int,
                    text: CharSequence,
                    newCursorPosition: Int,
                    textAttribute: TextAttribute?
                ): Boolean {
                    stopTextSelection()
                    if (preDeleteHighlighted > 0) {
                        val toDel = preDeleteHighlighted
                        preDeleteHighlighted = 0
                        nativeSetPreDeleteCount(0)
                        for (i in 0 until toDel) {
                            nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                        }
                    }
                    val s = text.toString()
                    if (s.isNotEmpty()) {
                        nativeSendText(s, s.length > 1)
                        rearmLockedModifiersIfNeeded()
                        resetScrollAndBlink()
                    }
                    syncMirrorWithTerminal()
                    return true
                }

                override fun performContextMenuAction(id: Int): Boolean {
                    when (id) {
                        android.R.id.paste, android.R.id.pasteAsPlainText -> {
                            stopTextSelection()
                            pasteFromClipboard()
                            syncMirrorWithTerminal()
                            return true
                        }
                        android.R.id.copy -> {
                            copyToClipboard(nativeGetSelectedText(), true)
                            stopTextSelection()
                            return true
                        }
                        android.R.id.selectAll -> {
                            nativeSelectAll(scrollOffset)
                            showColorOsFloatingActionMode()
                            invalidateAllRows()
                            return true
                        }
                    }
                    return super.performContextMenuAction(id)
                }

                override fun deleteSurroundingText(leftLength: Int, rightLength: Int): Boolean {
                    stopTextSelection()
                    val delCount = if (preDeleteHighlighted > 0 && leftLength == 0 && rightLength == 0) {
                        preDeleteHighlighted
                    } else {
                        max(leftLength, preDeleteHighlighted)
                    }
                    preDeleteHighlighted = 0
                    nativeSetPreDeleteCount(0)
                    for (i in 0 until delCount) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                    }
                    for (i in 0 until rightLength) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, 0, 0)
                    }
                    rearmLockedModifiersIfNeeded()
                    resetScrollAndBlink()
                    syncMirrorWithTerminal()
                    return true
                }

                override fun sendKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (event.keyCode == KeyEvent.KEYCODE_DEL && preDeleteHighlighted > 0) {
                            val toDel = preDeleteHighlighted
                            preDeleteHighlighted = 0
                            nativeSetPreDeleteCount(0)
                            for (i in 0 until toDel) {
                                nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                            }
                            resetScrollAndBlink()
                            syncMirrorWithTerminal()
                            return true
                        }
                        this@TerminalSurface.onKeyDown(event.keyCode, event)
                        syncMirrorWithTerminal()
                    }
                    return true
                }
            }
        }

        override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_BACK && nativeIsSelectingText()) {
                if (event.action == KeyEvent.ACTION_UP) {
                    stopTextSelection()
                }
                return true
            }
            return super.onKeyPreIme(keyCode, event)
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            if (nativeIsSelectingText()) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    stopTextSelection()
                    return true
                }
                stopTextSelection()
            }

            if (keyCode == KeyEvent.KEYCODE_PASTE) {
                pasteFromClipboard()
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                val mask = nativeDispatchCustomKey("CTRL")
                syncModifierButtons(mask)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                val mask = nativeDispatchCustomKey("ALT")
                syncModifierButtons(mask)
                return true
            }

            val unicode = event.getUnicodeChar(event.metaState and KeyEvent.META_CTRL_MASK.inv())
            val res = nativeSendKeyEvent(keyCode, unicode, event.metaState)
            rearmLockedModifiersIfNeeded()
            resetScrollAndBlink()

            if (res == 2) {
                closeCurrentSession()
                return true
            }
            return res != 0 || super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        init {
            System.loadLibrary("termuxx-core")
        }

        @JvmStatic external fun nativeRegisterSurface(surface: View?)
        @JvmStatic external fun nativeInitAndPollBootstrap(nativeLibDir: String): Int
        @JvmStatic external fun nativeNewSession(asRoot: Boolean, rows: Int, cols: Int, cellW: Int, cellH: Int): Int
        @JvmStatic external fun nativeSwitchSession(index: Int)
        @JvmStatic external fun nativeCloseSession(index: Int): Int
        @JvmStatic external fun nativeDestroyAll()
        @JvmStatic external fun nativeGetSessionCount(): Int
        @JvmStatic external fun nativeGetActiveIndex(): Int
        @JvmStatic external fun nativeIsSessionRoot(index: Int): Boolean
        @JvmStatic external fun nativeResizeActive(rows: Int, cols: Int, cellW: Int, cellH: Int)
        @JvmStatic external fun nativeConsumeFrameDirty(): Boolean
        @JvmStatic external fun nativeConsumeOsc52Clipboard(): String?
        @JvmStatic external fun nativeDispatchCustomKey(keyLabel: String): Int
        @JvmStatic external fun nativeGetModifierMask(): Int
        @JvmStatic external fun nativeSendText(text: String, isPaste: Boolean)
        @JvmStatic external fun nativeSendKeyEvent(keyCode: Int, unicodeChar: Int, metaState: Int): Int
        @JvmStatic external fun nativeSendMouse(button: Int, col: Int, row: Int, pressed: Boolean)
        @JvmStatic external fun nativeGetActiveScrollback(): Int
        @JvmStatic external fun nativeIsRowDirty(screenRow: Int): Boolean
        @JvmStatic external fun nativeClearDirtyFlags()
        @JvmStatic external fun nativePackRowRender(
            scrollOffset: Int,
            screenRow: Int,
            outBgRuns: IntArray,
            outGlyphs: IntArray
        ): Int
        @JvmStatic external fun nativeSetPreDeleteCount(count: Int)
        @JvmStatic external fun nativeGetInputLineBeforeCursor(): String
        @JvmStatic external fun nativeSetPerformanceMode(mode: Int)
        @JvmStatic external fun nativeGetCursorState(): Int
        @JvmStatic external fun nativeGetModesMask(): Int
        @JvmStatic external fun nativeGetTranscriptText(): String
        @JvmStatic external fun nativeStartTextSelection(scrollOffset: Int, screenCol: Int, screenRow: Int)
        @JvmStatic external fun nativeOnSelectionTouchPx(
            action: Int,
            scrollOffset: Int,
            touchX: Float,
            touchY: Float,
            cellW: Float,
            cellH: Float,
            viewW: Float,
            viewH: Float,
            radiusPx: Float
        ): Int
        @JvmStatic external fun nativeGetSelectionGeometryPx(
            scrollOffset: Int,
            cellW: Float,
            cellH: Float,
            viewW: Float,
            viewH: Float,
            radiusPx: Float,
            outGeom: FloatArray
        ): Boolean
        @JvmStatic external fun nativeStopTextSelection(): Boolean
        @JvmStatic external fun nativeIsSelectingText(): Boolean
        @JvmStatic external fun nativeSelectAll(scrollOffset: Int)
        @JvmStatic external fun nativeSetDefaultCursorShape(shape: Int)
        @JvmStatic external fun nativeGetSelectedText(): String
    }
}

class KeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        val chId = "termux_session_channel"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(chId, "Termux", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )

        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TermuxActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(this, chId)
            .setContentTitle("Termux")
            .setContentText("Сессия активна")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        try {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } catch (_: Throwable) {
        }

        try {
            wakeLock = getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Termux::WakeLock")
                ?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            wifiLock = applicationContext.getSystemService(WifiManager::class.java)
                ?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Termux::WifiLock")
                ?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
        } catch (_: Throwable) {
        }
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {
        }
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
