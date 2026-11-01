package com.termuxx.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.ActionMode
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.termuxx.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Subdued Dark AMOLED Palette
private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
private const val COLOR_HEADER_BG = 0xFF06080C.toInt()
private const val COLOR_SURFACE = 0xFF0C0F14.toInt()
private const val COLOR_SURFACE_PRESSED = 0xFF151B24.toInt()
private const val COLOR_BORDER = 0xFF171B22.toInt()
private const val COLOR_TEXT = 0xFFB4BCC8.toInt()
private const val COLOR_TEXT_DIM = 0xFF68707E.toInt()
private const val COLOR_ROOT = 0xFFD97B73.toInt()
private const val COLOR_ROOT_CONTAINER = 0xFF2B1212.toInt()

// Native Selection Handle Palette
private const val COLOR_HANDLE_STEM = 0xFFC4C6CC.toInt()
private const val COLOR_HANDLE_RING = 0xFF9EA0A8.toInt()
private const val COLOR_HANDLE_FILL = 0xFFE8EAEF.toInt()

private const val PREFS_NAME = "termuxx_prefs"
private const val KEY_EXTRA_KEYS_SPEC = "extra_keys_spec_v2"
private const val KEY_UI_THEME_INDEX = "ui_theme_index_v1"
private const val KEY_CURSOR_SHAPE = "cursor_shape_v2"
private const val KEY_CURSOR_BLINK_MODE = "cursor_blink_mode_v1"
private const val KEY_CURSOR_SMOOTH_MOVE = "cursor_smooth_move_v1"
private const val KEY_ANIM_SPEED = "cursor_anim_speed_v1"

private const val DEFAULT_UI_THEME_INDEX = 0
private const val DEFAULT_CURSOR_SHAPE = 0 // 0: Rounded Block, 1: Underline, 2: Bar
private const val DEFAULT_CURSOR_BLINK_MODE = 0 // 0: Smooth fade, 1: Classic step, 2: Off
private const val DEFAULT_CURSOR_SMOOTH_MOVE = true
private const val DEFAULT_ANIM_SPEED = 1 // 0: Fast, 1: Normal, 2: Relaxed

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

        // Top Bar
        headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_HEADER_BG)
            setPadding(dp(6), dp(3), dp(6), dp(3))
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
        headerBar.addView(makeHeaderButton("⚙") { showSettingsDialog() })
        headerBar.addView(makeHeaderButton("✕") { closeCurrentSession() })

        rootLayout.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36))
        )

        val topDivider = View(this).apply {
            setBackgroundColor(COLOR_BORDER)
        }
        rootLayout.addView(
            topDivider,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
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

        // Bottom Extra Keys Bar (no top border line, uniform tight spacing across rows & buttons)
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
        nativeSetDefaultCursorShape(initCursorShape)
        rebuildTabs()
        nativeRegisterSurface(terminalSurface)
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)

        rootLayout.postDelayed({
            ensureNotificationsAndKeepAlive()
        }, 350L)
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
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
    // Minimalist Unified Settings Dialog (Theme, Cursor, Animations, Extra Keys)
    // ========================================================================
    private fun showSettingsDialog() {
        var selTheme = uiThemeIndex
        var selShape = prefs.getInt(KEY_CURSOR_SHAPE, DEFAULT_CURSOR_SHAPE)
        var selBlinkMode = prefs.getInt(KEY_CURSOR_BLINK_MODE, DEFAULT_CURSOR_BLINK_MODE)
        var selSmoothMove = prefs.getBoolean(KEY_CURSOR_SMOOTH_MOVE, DEFAULT_CURSOR_SMOOTH_MOVE)
        var selAnimSpeed = prefs.getInt(KEY_ANIM_SPEED, DEFAULT_ANIM_SPEED)

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
        var activeEditRow = 0 // 0 = Row 1, 1 = Row 2

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(6))
            setBackgroundColor(COLOR_HEADER_BG)
        }
        val scroll = ScrollView(this).apply {
            addView(root)
        }

        fun activeAccent(): Int = UI_THEMES[selTheme.coerceIn(0, UI_THEMES.lastIndex)].accent
        fun activeContainer(): Int = UI_THEMES[selTheme.coerceIn(0, UI_THEMES.lastIndex)].container

        fun sectionHeader(title: String): TextView {
            return TextView(this).apply {
                text = title
                setTextColor(COLOR_TEXT_DIM)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(0, dp(10), 0, dp(4))
            }
        }

        fun optionChip(title: String, selected: Boolean, onClick: () -> Unit): TextView {
            val acc = activeAccent()
            val cont = activeContainer()
            return TextView(this).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setTextColor(if (selected) acc else COLOR_TEXT)
                background = pillBg(
                    if (selected) cont else COLOR_SURFACE,
                    if (selected) acc else COLOR_BORDER,
                    dp(6)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(5), dp(4))
                }
                setOnClickListener { onClick() }
            }
        }

        val refreshAllCallbacks = ArrayList<() -> Unit>()
        fun refreshAllSections() {
            for (cb in refreshAllCallbacks) cb()
        }

        // 1. Цвета интерфейса
        root.addView(sectionHeader("Цвет интерфейса"))
        val themeScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        themeScroll.addView(themeRow)
        root.addView(themeScroll)

        refreshAllCallbacks.add {
            themeRow.removeAllViews()
            for (i in UI_THEMES.indices) {
                val preset = UI_THEMES[i]
                val isSel = (selTheme == i)
                val chip = TextView(this).apply {
                    text = preset.name
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                    setTextColor(if (isSel) preset.accent else COLOR_TEXT)
                    background = pillBg(
                        if (isSel) preset.container else COLOR_SURFACE,
                        if (isSel) preset.accent else COLOR_BORDER,
                        dp(6)
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, dp(5), dp(4))
                    }
                    setOnClickListener {
                        selTheme = i
                        refreshAllSections()
                    }
                }
                themeRow.addView(chip)
            }
        }

        // 2. Форма курсора
        root.addView(sectionHeader("Курсор"))
        val shapeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(shapeRow)

        refreshAllCallbacks.add {
            shapeRow.removeAllViews()
            val shapes = listOf("Блок" to 0, "Линия" to 1, "Луч" to 2)
            for ((label, s) in shapes) {
                shapeRow.addView(optionChip(label, selShape == s) {
                    selShape = s
                    refreshAllSections()
                })
            }
        }

        // 3. Мигание и плавное движение (VS Code)
        root.addView(sectionHeader("Анимация курсора"))
        val blinkRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(blinkRow)

        refreshAllCallbacks.add {
            blinkRow.removeAllViews()
            val modes = listOf("Плавное мигание" to 0, "Обычное" to 1, "Без мигания" to 2)
            for ((label, m) in modes) {
                blinkRow.addView(optionChip(label, selBlinkMode == m) {
                    selBlinkMode = m
                    refreshAllSections()
                })
            }
        }

        val moveRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(moveRow)

        refreshAllCallbacks.add {
            moveRow.removeAllViews()
            moveRow.addView(optionChip("Плавный переход (VS Code)", selSmoothMove) {
                selSmoothMove = !selSmoothMove
                refreshAllSections()
            })
            val speeds = listOf("Быстро" to 0, "Нормально" to 1, "Плавно" to 2)
            for ((label, spd) in speeds) {
                moveRow.addView(optionChip(label, selAnimSpeed == spd) {
                    selAnimSpeed = spd
                    refreshAllSections()
                })
            }
        }

        // 4. Настройка нижней панели клавиш
        root.addView(sectionHeader("Нижняя панель клавиш"))

        // Пресеты
        val presetsScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val presetsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        presetsScroll.addView(presetsRow)
        root.addView(presetsScroll)

        fun loadSpecIntoRows(specStr: String) {
            row1Tokens.clear()
            row2Tokens.clear()
            val ls = specStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (ls.isNotEmpty()) {
                row1Tokens.addAll(ls[0].split("|").map { it.trim() }.filter { it.isNotEmpty() })
            }
            if (ls.size > 1) {
                row2Tokens.addAll(ls[1].split("|").map { it.trim() }.filter { it.isNotEmpty() })
            }
            refreshAllSections()
        }

        refreshAllCallbacks.add {
            presetsRow.removeAllViews()
            presetsRow.addView(optionChip("1 ряд", row1Tokens.isNotEmpty() && row2Tokens.isEmpty()) {
                loadSpecIntoRows(DEFAULT_EXTRA_KEYS_SPEC)
            })
            presetsRow.addView(optionChip("2 ряда", row2Tokens.isNotEmpty()) {
                loadSpecIntoRows(EXTENDED_EXTRA_KEYS_SPEC)
            })
            presetsRow.addView(optionChip("Vim / Tmux", false) {
                loadSpecIntoRows(VIM_TMUX_EXTRA_KEYS_SPEC)
            })
            presetsRow.addView(optionChip("Скрыть", row1Tokens.isEmpty() && row2Tokens.isEmpty()) {
                loadSpecIntoRows("")
            })
        }

        // Выбор редактируемого ряда (Ряд 1 / Ряд 2) + кнопка "+ Своя кнопка"
        val rowSelectBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(2))
        }
        root.addView(rowSelectBar)

        fun targetRowList(): ArrayList<String> = if (activeEditRow == 0) row1Tokens else row2Tokens

        // Интерактивный список кнопок выбранного ряда (нажатие удаляет кнопку)
        val activeRowHint = TextView(this).apply {
            text = "Кнопки ряда (нажми на кнопку, чтобы удалить):"
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(0, dp(2), 0, dp(2))
        }
        root.addView(activeRowHint)

        val currentKeysScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val currentKeysRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = pillBg(COLOR_AMOLED_BG, COLOR_BORDER, dp(6))
        }
        currentKeysScroll.addView(currentKeysRow)
        root.addView(currentKeysScroll)

        // Палитра быстрых клавиш для добавления в 1 тап
        val addKeyHint = TextView(this).apply {
            text = "Добавить клавишу в выбранный ряд:"
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(0, dp(6), 0, dp(2))
        }
        root.addView(addKeyHint)

        val paletteScroll1 = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val paletteRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        paletteScroll1.addView(paletteRow1)
        root.addView(paletteScroll1)

        val paletteScroll2 = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val paletteRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        paletteScroll2.addView(paletteRow2)
        root.addView(paletteScroll2)

        val quickKeys1 = listOf(
            "ESC", "TAB", "CTRL", "ALT", "ENTER", "BKSP", "DEL", "SPC",
            "▲", "▼", "◀", "▶", "HOME", "END", "PGUP", "PGDN"
        )
        val quickKeys2 = listOf(
            "-", "/", "|", "\\", "_", "~", ":", ";", "\"", "'",
            "C-c::CTRL+C", "C-z::CTRL+Z", "C-d::CTRL+D", "C-l::CTRL+L", "F1", "F2", "F3", "F4", "F5", "F10", "F12"
        )

        fun showCustomBindDialog() {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(6))
                setBackgroundColor(COLOR_HEADER_BG)
            }
            val labelInput = EditText(this).apply {
                hint = "Текст на кнопке (напр. C-c или git)"
                setHintTextColor(COLOR_TEXT_DIM)
                setTextColor(COLOR_TEXT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(5))
                setPadding(dp(8), dp(6), dp(8), dp(6))
                setSingleLine(true)
            }
            val actionInput = EditText(this).apply {
                hint = "Клавиша или команда (напр. CTRL+C, ALT+B, ls -la\\n)"
                setHintTextColor(COLOR_TEXT_DIM)
                setTextColor(COLOR_TEXT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(5))
                setPadding(dp(8), dp(6), dp(8), dp(6))
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(6)
                }
            }
            box.addView(labelInput)
            box.addView(actionInput)

            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Своя клавиша")
                .setView(box)
                .setPositiveButton("Добавить") { _, _ ->
                    val lbl = labelInput.text.toString().trim().replace("|", "")
                    val act = actionInput.text.toString().trim().replace("|", "")
                    if (lbl.isNotEmpty() || act.isNotEmpty()) {
                        val token = when {
                            lbl.isNotEmpty() && act.isNotEmpty() && lbl != act -> "$lbl::$act"
                            act.isNotEmpty() -> act
                            else -> lbl
                        }
                        targetRowList().add(token)
                        refreshAllSections()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        refreshAllCallbacks.add {
            rowSelectBar.removeAllViews()
            rowSelectBar.addView(optionChip("Ряд 1 (${row1Tokens.size})", activeEditRow == 0) {
                activeEditRow = 0
                refreshAllSections()
            })
            rowSelectBar.addView(optionChip("Ряд 2 (${row2Tokens.size})", activeEditRow == 1) {
                activeEditRow = 1
                refreshAllSections()
            })
            rowSelectBar.addView(optionChip("+ Своя клавиша", false) {
                showCustomBindDialog()
            })
            rowSelectBar.addView(optionChip("Очистить ряд", false) {
                targetRowList().clear()
                refreshAllSections()
            })

            currentKeysRow.removeAllViews()
            val list = targetRowList()
            if (list.isEmpty()) {
                val emptyTv = TextView(this).apply {
                    text = "Ряд пуст"
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(dp(6), dp(4), dp(6), dp(4))
                }
                currentKeysRow.addView(emptyTv)
            } else {
                for (idx in list.indices) {
                    val raw = list[idx]
                    val (disp, _) = parseKeyToken(raw)
                    val chip = TextView(this).apply {
                        text = "$disp ✕"
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setTextColor(activeAccent())
                        setPadding(dp(9), dp(4), dp(9), dp(4))
                        background = pillBg(COLOR_SURFACE, activeAccent(), dp(5))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, dp(4), 0)
                        }
                        setOnClickListener {
                            list.removeAt(idx)
                            refreshAllSections()
                        }
                    }
                    currentKeysRow.addView(chip)
                }
            }

            paletteRow1.removeAllViews()
            for (k in quickKeys1) {
                val (disp, _) = parseKeyToken(k)
                paletteRow1.addView(optionChip("+ $disp", false) {
                    targetRowList().add(k)
                    refreshAllSections()
                })
            }

            paletteRow2.removeAllViews()
            for (k in quickKeys2) {
                val (disp, _) = parseKeyToken(k)
                paletteRow2.addView(optionChip("+ $disp", false) {
                    targetRowList().add(k)
                    refreshAllSections()
                })
            }
        }

        refreshAllSections()

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Настройки")
            .setView(scroll)
            .setPositiveButton("Сохранить") { _, _ ->
                uiThemeIndex = selTheme
                val builtSpec = buildString {
                    if (row1Tokens.isNotEmpty()) append(row1Tokens.joinToString("|"))
                    if (row2Tokens.isNotEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append(row2Tokens.joinToString("|"))
                    }
                }
                prefs.edit()
                    .putInt(KEY_UI_THEME_INDEX, selTheme)
                    .putInt(KEY_CURSOR_SHAPE, selShape)
                    .putInt(KEY_CURSOR_BLINK_MODE, selBlinkMode)
                    .putBoolean(KEY_CURSOR_SMOOTH_MOVE, selSmoothMove)
                    .putInt(KEY_ANIM_SPEED, selAnimSpeed)
                    .putString(KEY_EXTRA_KEYS_SPEC, builtSpec)
                    .apply()

                nativeSetDefaultCursorShape(selShape)
                terminalSurface.applyCursorAndAnimSettings(
                    selShape,
                    selBlinkMode,
                    selSmoothMove,
                    selAnimSpeed
                )
                rebuildTabs()
                rebuildExtraKeysBar()
                terminalSurface.post { terminalSurface.syncSize() }
            }
            .setNegativeButton("Отмена", null)
            .show()
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
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        terminalSurface.stopTextSelection()
                        nativeSwitchSession(idx)
                        terminalSurface.syncSize()
                        rebuildTabs()
                        terminalSurface.invalidateAllRows()
                    }
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
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(COLOR_TEXT)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(26)).apply {
                setMargins(dp(3), 0, 0, 0)
            }
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            if (onLongClick != null) {
                setOnLongClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
    // Terminal Surface (VS Code Smooth Cursor + Selection Auto-Scroll)
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
        private var fontScale = 1.0f

        internal var rows = 24
        internal var cols = 80
        internal var scrollOffset = 0
        private var scrollAccum = 0f

        private var activityResumed = true
        internal var cursorShapePref = prefs.getInt(KEY_CURSOR_SHAPE, DEFAULT_CURSOR_SHAPE)
        internal var cursorBlinkMode = prefs.getInt(KEY_CURSOR_BLINK_MODE, DEFAULT_CURSOR_BLINK_MODE)
        internal var cursorSmoothMove = prefs.getBoolean(KEY_CURSOR_SMOOTH_MOVE, DEFAULT_CURSOR_SMOOTH_MOVE)
        internal var animSpeedPref = prefs.getInt(KEY_ANIM_SPEED, DEFAULT_ANIM_SPEED)

        private var cursorBlinkOn = true
        private var lastInputTimeMs = SystemClock.uptimeMillis()

        // VS Code Smooth Cursor Interpolation State
        private var animCursorX = 0f
        private var animCursorY = 0f
        private var cursorPosInitialized = false
        private var lastCursorFrameNs = 0L

        // Selection Handle Auto-Scroll State
        private var isDraggingSelectionHandle = false
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
                if (!activityResumed || cursorBlinkMode != 1) return
                if (scrollOffset == 0 && !nativeIsSelectingText()) {
                    cursorBlinkOn = !cursorBlinkOn
                    postInvalidateOnAnimation()
                }
                val period = when (animSpeedPref) {
                    0 -> 360L
                    2 -> 720L
                    else -> 530L
                }
                postDelayed(this, period)
            }
        }

        private val selectionAutoScrollRunnable = object : Runnable {
            override fun run() {
                if (!isDraggingSelectionHandle || !nativeIsSelectingText()) return
                val edgeZone = cellHeight * 1.7f
                var delta = 0
                if (lastDragTouchY < edgeZone) {
                    delta = if (lastDragTouchY < 0f) 2 else 1
                } else if (lastDragTouchY > height - edgeZone) {
                    delta = if (lastDragTouchY > height) -2 else -1
                }
                if (delta != 0) {
                    val maxSb = nativeGetActiveScrollback()
                    val newOffset = (scrollOffset + delta).coerceIn(0, maxSb)
                    if (newOffset != scrollOffset) {
                        scrollOffset = newOffset
                    }
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
                    postDelayed(this, 42L)
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
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
                        updateMetrics(true)
                        cursorPosInitialized = false
                        syncSize()
                    }
                }
            )
        }

        private fun scheduleBlinkLoop() {
            removeCallbacks(stepBlinkRunnable)
            cursorBlinkOn = true
            lastInputTimeMs = SystemClock.uptimeMillis()
            if (activityResumed && cursorBlinkMode == 1) {
                val period = when (animSpeedPref) {
                    0 -> 360L
                    2 -> 720L
                    else -> 530L
                }
                postDelayed(stepBlinkRunnable, period)
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
            stopTextSelection()
        }

        internal fun applyCursorAndAnimSettings(
            shape: Int,
            blinkMode: Int,
            smoothMove: Boolean,
            animSpeed: Int
        ) {
            cursorShapePref = shape
            cursorBlinkMode = blinkMode
            cursorSmoothMove = smoothMove
            animSpeedPref = animSpeed
            scheduleBlinkLoop()
            invalidateAllRows()
        }

        internal fun showTranscriptCopyDialog() {
            val fullText = nativeGetTranscriptText()
            val tv = TextView(context).apply {
                text = fullText.ifEmpty { "Буфер пуст." }
                setTextIsSelectable(true)
                typeface = Typeface.MONOSPACE
                setTextColor(COLOR_TEXT)
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

            // Draw Rounded Cursor with VS Code Smooth Movement & Smooth/Step Blinking
            if (scrollOffset == 0 && !selecting) {
                val cursorState = nativeGetCursorState()
                if (cursorState >= 0) {
                    val rawShape = (cursorState ushr 29) and 3
                    val shape = if (rawShape in 0..2) rawShape else cursorShapePref
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
                                val rate = when (animSpeedPref) {
                                    0 -> 38f
                                    2 -> 16f
                                    else -> 25f
                                }
                                val factor = (1f - exp(-rate * dtSec)).coerceIn(0.15f, 1f)
                                animCursorX += (targetX - animCursorX) * factor
                                animCursorY += (targetY - animCursorY) * factor
                                if (abs(targetX - animCursorX) > 0.35f || abs(targetY - animCursorY) > 0.35f) {
                                    stillMoving = true
                                } else {
                                    animCursorX = targetX
                                    animCursorY = targetY
                                }
                            }
                        }
                        lastCursorFrameNs = nowNs

                        val alphaFloat: Float = when (cursorBlinkMode) {
                            0 -> { // Smooth breathing blink
                                val elapsedMs = SystemClock.uptimeMillis() - lastInputTimeMs
                                if (elapsedMs < 260L) {
                                    0.85f
                                } else {
                                    val cycleMs = when (animSpeedPref) {
                                        0 -> 780f
                                        2 -> 1450f
                                        else -> 1080f
                                    }
                                    val phase = ((elapsedMs - 260L).toFloat() / cycleMs) * (2f * Math.PI.toFloat())
                                    0.14f + 0.72f * (0.5f + 0.5f * cos(phase))
                                }
                            }
                            1 -> if (cursorBlinkOn) 0.85f else 0.0f
                            else -> 0.85f
                        }

                        if (alphaFloat > 0.02f) {
                            val aInt = (alphaFloat * 255f).roundToInt().coerceIn(0, 255)
                            bgPaint.color = (aInt shl 24) or (uiAccent and 0x00FFFFFF)
                            val cx = animCursorX
                            val cy = animCursorY
                            when (shape) {
                                1 -> { // Rounded Underline
                                    val h = dpF(2.4f)
                                    val r = dpF(1.2f)
                                    cursorRectF.set(cx, cy + cellHeight - h, cx + cellWidth, cy + cellHeight)
                                    canvas.drawRoundRect(cursorRectF, r, r, bgPaint)
                                }
                                2 -> { // Rounded Beam / Bar
                                    val w = dpF(2.2f)
                                    val r = dpF(1.1f)
                                    val vPad = dpF(1f)
                                    cursorRectF.set(cx, cy + vPad, cx + w, cy + cellHeight - vPad)
                                    canvas.drawRoundRect(cursorRectF, r, r, bgPaint)
                                }
                                else -> { // Rounded Square/Rectangle Block
                                    val r = dpF(3.2f)
                                    val vPad = dpF(0.8f)
                                    cursorRectF.set(cx, cy + vPad, cx + cellWidth, cy + cellHeight - vPad)
                                    canvas.drawRoundRect(cursorRectF, r, r, bgPaint)
                                }
                            }
                        }

                        if (stillMoving || (cursorBlinkMode == 0 && activityResumed)) {
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
                    val edgeZone = cellHeight * 1.7f
                    removeCallbacks(selectionAutoScrollRunnable)
                    if (event.y < edgeZone || event.y > height - edgeZone) {
                        postDelayed(selectionAutoScrollRunnable, 42L)
                    }
                    invalidateAllRows()
                    return true
                } else if (dragRes == 2) {
                    isDraggingSelectionHandle = false
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

        override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_ACTION_NONE

            return object : BaseInputConnection(this, false) {
                private val editableBuf = SpannableStringBuilder()
                private var lastComposing = ""

                override fun getEditable(): Editable {
                    if (editableBuf.isEmpty()) {
                        Selection.setSelection(editableBuf, 0)
                    }
                    return editableBuf
                }

                private fun applyComposingDiff(newText: String) {
                    var common = 0
                    val minLen = min(lastComposing.length, newText.length)
                    while (common < minLen && lastComposing[common] == newText[common]) {
                        common++
                    }
                    val toDelete = lastComposing.length - common
                    repeat(toDelete) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                    }
                    if (common < newText.length) {
                        nativeSendText(newText.substring(common), false)
                    }
                }

                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    val committed = text?.toString() ?: ""
                    stopTextSelection()
                    if (lastComposing.isNotEmpty()) {
                        applyComposingDiff(committed)
                        lastComposing = ""
                    } else if (committed.isNotEmpty()) {
                        nativeSendText(committed, false)
                    }
                    rearmLockedModifiersIfNeeded()
                    resetScrollAndBlink()
                    editableBuf.clear()
                    return true
                }

                override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    val composing = text?.toString() ?: ""
                    stopTextSelection()
                    applyComposingDiff(composing)
                    lastComposing = composing
                    rearmLockedModifiersIfNeeded()
                    resetScrollAndBlink()
                    return true
                }

                override fun finishComposingText(): Boolean {
                    lastComposing = ""
                    editableBuf.clear()
                    return true
                }

                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    stopTextSelection()
                    if (lastComposing.isNotEmpty()) {
                        val drop = min(beforeLength, lastComposing.length)
                        lastComposing = lastComposing.dropLast(drop)
                    }
                    for (i in 0 until beforeLength) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                    }
                    for (i in 0 until afterLength) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, 0, 0)
                    }
                    rearmLockedModifiersIfNeeded()
                    resetScrollAndBlink()
                    return true
                }

                override fun sendKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        this@TerminalSurface.onKeyDown(event.keyCode, event)
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
        @JvmStatic external fun nativeGetCursorState(): Int
        @JvmStatic external fun nativeGetModesMask(): Int
        @JvmStatic external fun nativeGetTranscriptText(): String
        @JvmStatic external fun nativeStartTextSelection(scrollOffset: Int, screenCol: Int, screenRow: Int)
        @JvmStatic external fun nativeUpdateSelectionHandle(isStartHandle: Boolean, col: Int, logicalY: Int)
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
        @JvmStatic external fun nativeGetSelectionSelectors(outSelectors: IntArray)
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
