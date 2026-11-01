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
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Material 3 Expressive Dark AMOLED Palette (Zero Green — Pure Material Dark + Blue/Coral Accents)
private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
private const val COLOR_HEADER_BG = 0xFF0A0D12.toInt()
private const val COLOR_SURFACE = 0xFF12151C.toInt()
private const val COLOR_SURFACE_PRESSED = 0xFF1E2636.toInt()
private const val COLOR_BORDER = 0xFF222834.toInt()
private const val COLOR_TEXT = 0xFFE2E8F0.toInt()
private const val COLOR_TEXT_DIM = 0xFF94A3B8.toInt()
private const val COLOR_ACCENT = 0xFFA8C7FA.toInt()
private const val COLOR_ACCENT_CONTAINER = 0xFF16243A.toInt()
private const val COLOR_ROOT = 0xFFFFB4AB.toInt()
private const val COLOR_ROOT_CONTAINER = 0xFF3B1616.toInt()

// ColorOS 17 Native Selection Handle Palette (matches system lollipop handles)
private const val COLOR_HANDLE_STEM = 0xFFD4D4DA.toInt()
private const val COLOR_HANDLE_RING = 0xFFB0B0B8.toInt()
private const val COLOR_HANDLE_FILL = 0xFFF8F8FA.toInt()

private const val PREFS_NAME = "termuxx_prefs"
private const val KEY_EXTRA_KEYS_SPEC = "extra_keys_spec_v2"
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

/**
 * Termuxx 4.0 — Ultra-Light Android 17 (API 37) Kotlin + C++23 Terminal.
 *
 * - Material 3 Dark AMOLED styling (#000000 / #0A0D12 / #12151C / #222834 / #A8C7FA / #FFB4AB).
 * - Pinned Top Session & Action Bar (strictly clipped viewport so canvas never overdraws header).
 * - Bottom Extra Keys Bar with Button Hold (continuous key auto-repeat on hold + sticky CTRL/ALT lock on hold).
 * - Event-driven zero-idle-CPU architecture: C++23 reader thread sleeps in `poll(-1)` when idle
 *   and wakes `TerminalSurface` directly via JNI `postInvalidateOnAnimation()`.
 * - ColorOS 17 native text selection handles (top-circle start stem + bottom-circle end stem +
 *   neutral gray highlight + native `ActionMode.TYPE_FLOATING` system menu) driven by C++23.
 * - Procedural Box-Drawing & Block Elements packed directly in C++23 (`nativePackRowRender`).
 */
class TermuxActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootLayout: LinearLayout
    private lateinit var headerBar: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var extraKeysRoot: LinearLayout
    internal lateinit var terminalSurface: TerminalSurface
    private val modifierButtons = ArrayList<TextView>(4)

    // Sticky hold-lock states for CTRL / ALT when held down on the bottom bar
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

        // Keep clipChildren = true and clipToPadding = true so child views never overdraw headerBar
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
        }

        // 1. Top Material 3 Session Tab & Action Bar (38dp height, always visible below status bar)
        headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_HEADER_BG)
            setPadding(dp(8), dp(4), dp(8), dp(4))
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
        headerBar.addView(makeHeaderButton("⚙") { showExtraKeysSettingsDialog() })
        headerBar.addView(makeHeaderButton("✕") { closeCurrentSession() })

        rootLayout.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38))
        )

        val topDivider = View(this).apply {
            setBackgroundColor(COLOR_BORDER)
        }
        rootLayout.addView(
            topDivider,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, max(1, dp(1)))
        )

        // 2. Main Terminal Viewport (strictly clipped to its bounds so headerBar is never covered)
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

        // 3. Bottom Material 3 Extra Keys Bar with Hold / Auto-Repeat / Sticky Modifier Lock
        extraKeysRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_HEADER_BG)
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

        // Apply systemBars, displayCutout, and IME insets directly on rootLayout
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

        rebuildTabs()
        nativeRegisterSurface(terminalSurface)
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)

        // Request POST_NOTIFICATIONS non-blockingly and start KeepAliveService
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

    private fun rebuildExtraKeysBar() {
        extraKeysRoot.removeAllViews()
        modifierButtons.clear()

        val spec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)?.trim().orEmpty()
        if (spec.isEmpty()) {
            extraKeysRoot.visibility = View.GONE
            return
        }
        extraKeysRoot.visibility = View.VISIBLE

        val divider = View(this).apply {
            setBackgroundColor(COLOR_BORDER)
        }
        extraKeysRoot.addView(
            divider,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, max(1, dp(1)))
        )

        for (rawLine in spec.split("\n")) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(COLOR_HEADER_BG)
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }

            val delimiter = if (line.contains("|")) "|" else ","
            var added = 0
            for (rawToken in line.split(delimiter)) {
                val rawLabel = rawToken.trim()
                if (rawLabel.isEmpty()) continue
                val displayLabel = when (rawLabel.uppercase()) {
                    "UP" -> "▲"
                    "DOWN" -> "▼"
                    "LEFT" -> "◀"
                    "RIGHT" -> "▶"
                    else -> rawLabel
                }
                val btn = createExtraKeyButton(displayLabel)
                if (displayLabel == "CTRL" || displayLabel == "ALT") {
                    modifierButtons.add(btn)
                }
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    setMargins(dp(1), dp(1), dp(1), dp(1))
                })
                added++
            }
            if (added > 0) {
                extraKeysRoot.addView(rowLayout)
            }
        }
        syncModifierButtons(nativeGetModifierMask())
    }

    /**
     * Dispatches an extra key from the bottom panel while respecting sticky-locked CTRL / ALT
     * modifiers activated by holding down CTRL or ALT.
     */
    internal fun dispatchExtraKeyWithHoldSupport(label: String) {
        terminalSurface.stopTextSelection()
        if (ctrlLocked && (nativeGetModifierMask() and 1) == 0) {
            nativeDispatchCustomKey("CTRL")
        }
        if (altLocked && (nativeGetModifierMask() and 2) == 0) {
            nativeDispatchCustomKey("ALT")
        }
        var mask = nativeDispatchCustomKey(label)
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

    private fun createExtraKeyButton(label: String): TextView {
        val isModifier = (label == "CTRL" || label == "ALT")
        return TextView(this).apply {
            text = label
            tag = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_TEXT)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))

            var holdTriggered = false
            val repeatRunnable = object : Runnable {
                override fun run() {
                    if (!isPressed) return
                    holdTriggered = true
                    if (isModifier) {
                        // Holding CTRL or ALT locks the modifier (Sticky Lock mode)
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (label == "CTRL") {
                            ctrlLocked = !ctrlLocked
                            if (ctrlLocked && (nativeGetModifierMask() and 1) == 0) {
                                nativeDispatchCustomKey("CTRL")
                            } else if (!ctrlLocked && (nativeGetModifierMask() and 1) != 0) {
                                nativeDispatchCustomKey("CTRL")
                            }
                        } else if (label == "ALT") {
                            altLocked = !altLocked
                            if (altLocked && (nativeGetModifierMask() and 2) == 0) {
                                nativeDispatchCustomKey("ALT")
                            } else if (!altLocked && (nativeGetModifierMask() and 2) != 0) {
                                nativeDispatchCustomKey("ALT")
                            }
                        }
                        syncModifierButtons(nativeGetModifierMask())
                    } else {
                        // Holding any action/arrow/symbol key auto-repeats it smoothly every 50ms
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        dispatchExtraKeyWithHoldSupport(label)
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
                            v.background = pillBg(COLOR_SURFACE_PRESSED, COLOR_ACCENT, dp(6))
                            (v as TextView).setTextColor(COLOR_ACCENT)
                            dispatchExtraKeyWithHoldSupport(label)
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
                                // Short tap on CTRL / ALT: unlock if locked, otherwise toggle one-shot latch
                                if (label == "CTRL" && ctrlLocked) {
                                    ctrlLocked = false
                                    if ((nativeGetModifierMask() and 1) != 0) {
                                        nativeDispatchCustomKey("CTRL")
                                    }
                                } else if (label == "ALT" && altLocked) {
                                    altLocked = false
                                    if ((nativeGetModifierMask() and 2) != 0) {
                                        nativeDispatchCustomKey("ALT")
                                    }
                                } else {
                                    nativeDispatchCustomKey(label)
                                }
                            }
                            syncModifierButtons(nativeGetModifierMask())
                        } else {
                            v.background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
                            (v as TextView).setTextColor(COLOR_TEXT)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun showExtraKeysSettingsDialog() {
        val currentSpec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)
            ?: DEFAULT_EXTRA_KEYS_SPEC

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(COLOR_HEADER_BG)
        }

        val hint = TextView(this).apply {
            text = "Формат: клавиши через | (или ,), строки через Enter.\n" +
                "Удержание клавиши — автоповтор, удержание CTRL/ALT — фиксация (Lock).\n" +
                "Доступны: ESC, TAB, CTRL, ALT, ENTER, BKSP, DEL, INS, HOME, END, PGUP, PGDN, ▲, ▼, ◀, ▶, F1..F12, CTRL+C и любые символы."
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, 0, 0, dp(8))
        }
        dialogLayout.addView(hint)

        val input = EditText(this).apply {
            setText(currentSpec)
            setTextColor(COLOR_TEXT)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            minLines = 2
            maxLines = 4
        }
        dialogLayout.addView(input)

        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        presetsRow.addView(makePresetChip("1-Row Minimal") { input.setText(DEFAULT_EXTRA_KEYS_SPEC) })
        presetsRow.addView(makePresetChip("2-Row Extended") { input.setText(EXTENDED_EXTRA_KEYS_SPEC) })
        presetsRow.addView(makePresetChip("Vim / TMUX") { input.setText(VIM_TMUX_EXTRA_KEYS_SPEC) })
        presetsRow.addView(makePresetChip("Скрыть панель") { input.setText("") })
        dialogLayout.addView(presetsRow)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Настройка нижней клавиатуры")
            .setView(dialogLayout)
            .setPositiveButton("Сохранить") { _, _ ->
                prefs.edit().putString(KEY_EXTRA_KEYS_SPEC, input.text.toString()).apply()
                rebuildExtraKeysBar()
                terminalSurface.post { terminalSurface.syncSize() }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun makePresetChip(title: String, onClick: Runnable): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(COLOR_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dp(6), 0)
            }
            setOnClickListener { onClick.run() }
        }
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
                    btn.background = pillBg(COLOR_ACCENT, COLOR_ACCENT, dp(6))
                }
                isActive -> {
                    btn.setTextColor(COLOR_ACCENT)
                    btn.background = pillBg(COLOR_ACCENT_CONTAINER, COLOR_ACCENT, dp(6))
                }
                else -> {
                    btn.setTextColor(COLOR_TEXT)
                    btn.background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(11), dp(4), dp(11), dp(4))
                val accent = if (isRoot) COLOR_ROOT else COLOR_ACCENT
                val activeFill = if (isRoot) COLOR_ROOT_CONTAINER else COLOR_ACCENT_CONTAINER
                setTextColor(if (isSelected) accent else COLOR_TEXT_DIM)
                background = pillBg(
                    if (isSelected) activeFill else COLOR_SURFACE,
                    if (isSelected) accent else COLOR_BORDER,
                    dp(7)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(5), 0)
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
        cm.setPrimaryClip(ClipData.newPlainText("Termuxx", text))
        if (showToast) {
            Toast.makeText(this, "Скопировано (${text.length} симв.)", Toast.LENGTH_SHORT).show()
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(COLOR_TEXT)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(28)).apply {
                setMargins(dp(4), 0, 0, 0)
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
            setStroke(max(1, dp(1)), stroke)
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
    // Event-Driven 165Hz TUI Surface + ColorOS 17 Native Selection Renderer
    // ========================================================================
    inner class TerminalSurface(context: Context) : View(context) {

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        private val bgPaint = Paint()
        private val graphicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val wavePath = Path()

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

        private var cursorBlinkOn = true
        private var activityResumed = false

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

        private val blinkRunnable = object : Runnable {
            override fun run() {
                if (!activityResumed) return
                if (scrollOffset == 0 && !nativeIsSelectingText()) {
                    cursorBlinkOn = !cursorBlinkOn
                    postInvalidateOnAnimation()
                }
                postDelayed(this, 530L)
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
                        if ((modes and 8) != 0) { // MODE_MOUSE_TRACKING
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
                            syncSize()
                        }
                        return true
                    }

                    override fun onScaleEnd(detector: ScaleGestureDetector) {
                        updateMetrics(true)
                        syncSize()
                    }
                }
            )
        }

        internal fun onActivityResume() {
            activityResumed = true
            removeCallbacks(blinkRunnable)
            postDelayed(blinkRunnable, 530L)
            postInvalidateOnAnimation()
        }

        internal fun onActivityPause() {
            activityResumed = false
            removeCallbacks(blinkRunnable)
            stopTextSelection()
        }

        internal fun showTranscriptCopyDialog() {
            val fullText = nativeGetTranscriptText()
            val tv = TextView(context).apply {
                text = fullText.ifEmpty { "Буфер терминала пуст." }
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
                                startActivity(Intent.createChooser(sendIntent, "Поделиться текстом"))
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
            cursorBlinkOn = true
            removeCallbacks(blinkRunnable)
            if (activityResumed) postDelayed(blinkRunnable, 530L)
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
            // Paint background strictly within TerminalSurface bounds so headerBar above is never overdrawn
            bgPaint.color = COLOR_AMOLED_BG
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val bootState = nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
            if (bootState != 2) {
                textPaint.color = COLOR_ACCENT
                textPaint.typeface = baseTypeface
                canvas.drawText(
                    "⚡ Инициализация среды Termuxx (Android 17 / C++23)...",
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

            // Draw Cursor (DECSCUSR) in Material 3 Primary Blue (#A8C7FA)
            if (scrollOffset == 0 && !selecting) {
                val cursorState = nativeGetCursorState()
                if (cursorState >= 0) {
                    val shape = (cursorState ushr 29) and 3
                    val cRow = (cursorState ushr 16) and 0x1FFF
                    val cCol = cursorState and 0xFFFF
                    if (cRow in 0 until rows && cCol in 0 until cols) {
                        val cx = cCol * cellWidth
                        val cy = cRow * cellHeight
                        bgPaint.color = if (cursorBlinkOn) 0xCCA8C7FA.toInt() else 0x44A8C7FA
                        when (shape) {
                            1 -> canvas.drawRect(cx, cy + cellHeight - dp(2), cx + cellWidth, cy + cellHeight, bgPaint)
                            2 -> canvas.drawRect(cx, cy, cx + dp(2), cy + cellHeight, bgPaint)
                            else -> canvas.drawRect(cx, cy, cx + cellWidth, cy + cellHeight, bgPaint)
                        }
                    }
                }
            }

            // Draw Native ColorOS 17 Selection Handles (Top-circle left stem + Bottom-circle right stem)
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

            // Vertical stem along character cell edge
            handlePaint.style = Paint.Style.STROKE
            handlePaint.strokeWidth = handleStemWidthPx
            handlePaint.color = COLOR_HANDLE_STEM
            canvas.drawLine(stemX, stemTopY, stemX, stemBotY, handlePaint)

            // Outer gray ring + inner white circle (ColorOS 17 Lollipop handle)
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

                // C++23 packed procedural Box-Drawing / Block Element rectangle
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

                val styleBits = flags and 5 // FLAG_BOLD (1) | FLAG_ITALIC (4)
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

                // Underline (8) / Undercurl (1024) / Double Underline (2048)
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

                // Strikethrough (128)
                if ((flags and 128) != 0) {
                    graphicPaint.color = fg
                    graphicPaint.strokeWidth = max(1.5f, cellHeight * 0.05f)
                    val sy = top + cellHeight * 0.52f
                    rc.drawLine(x, sy, x + cellWidth, sy, graphicPaint)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Route handle touch dragging directly through C++23 selection engine
            if (nativeIsSelectingText() && event.pointerCount == 1) {
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
                    actionMode?.hide(-1)
                    invalidateAllRows()
                    return true
                } else if (dragRes == 2) {
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
        @JvmStatic external fun nativeGetSelectedText(): String
    }
}

// ========================================================================
// Modern Android 17 (API 37) Keep-Alive Foreground Service (WakeLock + WifiLock)
// ========================================================================
class KeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        val chId = "termuxx_session_channel"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(chId, "Termuxx Core", NotificationManager.IMPORTANCE_LOW).apply {
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
            .setContentTitle("Termuxx (Kotlin + C++23)")
            .setContentText("Active terminal sessions running")
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
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Termuxx::CoreLock")
                ?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            wifiLock = applicationContext.getSystemService(WifiManager::class.java)
                ?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Termuxx::WifiLock")
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
