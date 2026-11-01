package com.termuxx.app

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
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
import android.view.WindowInsetsAnimation
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
private const val COLOR_SURFACE = 0xFF0C0E12.toInt()
private const val COLOR_BORDER = 0xFF1F242D.toInt()
private const val COLOR_TEXT = 0xFFE3E8EF.toInt()
private const val COLOR_TEXT_DIM = 0xFF949ECA.toInt()
private const val COLOR_ROOT = 0xFFF28B82.toInt() // Material 3 Dark Error

// Material 3 Dark Accent Presets
private const val ACCENT_MATERIAL_BLUE = 0xFFA8C7FA.toInt()   // Material 3 Primary Blue
private const val ACCENT_MATERIAL_MONET = 0xFFD0BCFF.toInt()  // Material 3 Lavender/Purple
private const val ACCENT_MATERIAL_CYAN = 0xFF78D9EC.toInt()   // Material 3 Cyan/Teal
private const val ACCENT_MATERIAL_MINT = 0xFF81C995.toInt()   // Material 3 Sage/Mint

// ColorOS 17 Native Selection Handle Palette
private const val COLOR_HANDLE_STEM = 0xFFD4D4DA.toInt()
private const val COLOR_HANDLE_RING = 0xFFB0B0B8.toInt()
private const val COLOR_HANDLE_FILL = 0xFFF8F8FA.toInt()

// Cursor Blink Modes
private const val BLINK_SMOOTH = 0
private const val BLINK_CLASSIC = 1
private const val BLINK_STEADY = 2

private const val PREFS_NAME = "termuxx_prefs"
private const val KEY_EXTRA_KEYS_SPEC = "extra_keys_spec_v2"
private const val KEY_CURSOR_BLINK_MODE = "cursor_blink_mode"
private const val KEY_CURSOR_SHAPE = "cursor_shape"
private const val KEY_MATERIAL_ACCENT = "material_accent_color"

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
 * Termuxx — Android 17 (API 37) Material 3 Dark AMOLED Kotlin + C++23 Terminal.
 */
class TermuxActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootLayout: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var extraKeysRoot: LinearLayout
    internal lateinit var terminalSurface: TerminalSurface
    private val modifierButtons = ArrayList<TextView>(4)
    private val headerButtons = ArrayList<TextView>(4)

    internal var accentColor: Int = ACCENT_MATERIAL_BLUE
    internal var cursorBlinkMode: Int = BLINK_SMOOTH
    internal var cursorShapePref: Int = 0

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
        accentColor = prefs.getInt(KEY_MATERIAL_ACCENT, ACCENT_MATERIAL_BLUE)
        cursorBlinkMode = prefs.getInt(KEY_CURSOR_BLINK_MODE, BLINK_SMOOTH)
        cursorShapePref = prefs.getInt(KEY_CURSOR_SHAPE, 0)
        nativeSetDefaultCursorShape(cursorShapePref)

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
            clipChildren = false
            clipToPadding = false
        }

        // Android 17 Edge-to-Edge + IME Keyboard Insets Handler:
        // Keeps top header bar below statusBar/cutout and lifts bottom Extra-Keys bar above IME keyboard!
        rootLayout.setOnApplyWindowInsetsListener { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsets.Type.systemBars() or
                    WindowInsets.Type.displayCutout() or
                    WindowInsets.Type.ime()
            )
            if (v.paddingLeft != insets.left ||
                v.paddingTop != insets.top ||
                v.paddingRight != insets.right ||
                v.paddingBottom != insets.bottom
            ) {
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                terminalSurface.post { terminalSurface.syncSize() }
            }
            WindowInsets.CONSUMED
        }

        rootLayout.setWindowInsetsAnimationCallback(
            object : WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsets,
                    runningAnimations: MutableList<WindowInsetsAnimation>
                ): WindowInsets {
                    val combined = insets.getInsets(
                        WindowInsets.Type.systemBars() or
                            WindowInsets.Type.displayCutout() or
                            WindowInsets.Type.ime()
                    )
                    rootLayout.setPadding(
                        combined.left,
                        combined.top,
                        combined.right,
                        combined.bottom
                    )
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimation) {
                    terminalSurface.post { terminalSurface.syncSize() }
                }
            }
        )

        // Top Header Bar with Session Tabs & Controls (38dp)
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tabScroll.addView(tabStrip)
        headerBar.addView(
            tabScroll,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        headerButtons.clear()
        headerBar.addView(makeHeaderButton("+") { createSession(false) })
        headerBar.addView(makeHeaderButton("#") { createSession(true) })
        headerBar.addView(makeHeaderButton("⚙") { showSettingsDialog() })
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
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        )

        val viewport = FrameLayout(this).apply {
            setBackgroundColor(COLOR_AMOLED_BG)
            clipChildren = false
            clipToPadding = false
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

        extraKeysRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
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
        nativeRegisterSurface(terminalSurface)
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
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
        }
        super.onDestroy()
    }

    internal fun accentContainerColor(color: Int = accentColor): Int {
        val r = ((color ushr 16) and 0xFF) / 5
        val g = ((color ushr 8) and 0xFF) / 5
        val b = (color and 0xFF) / 5
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        )

        for (rawLine in spec.split("\n")) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(COLOR_AMOLED_BG)
                setPadding(dp(2), dp(2), dp(2), dp(2))
            }

            var added = 0
            for (rawToken in line.split("|")) {
                val label = rawToken.trim()
                if (label.isEmpty()) continue
                val btn = createExtraKeyButton(label)
                if (label == "CTRL" || label == "ALT") {
                    modifierButtons.add(btn)
                }
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, dp(34), 1f))
                added++
            }
            if (added > 0) {
                extraKeysRoot.addView(rowLayout)
            }
        }
        syncModifierButtons(nativeGetModifierMask())
    }

    private fun dispatchExtraKey(label: String) {
        terminalSurface.stopTextSelection()
        val mask = nativeDispatchCustomKey(label)
        syncModifierButtons(mask)
        terminalSurface.resetScrollAndBlink()
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
            layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                setMargins(dp(1), dp(1), dp(1), dp(1))
            }

            if (isModifier) {
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    dispatchExtraKey(label)
                }
            } else {
                // Hold-to-repeat support for arrows (▲, ▼, ◀, ▶), BKSP, DEL, PGUP, PGDN, etc.
                val repeatRunnable = object : Runnable {
                    override fun run() {
                        if (!isPressed) return
                        dispatchExtraKey(label)
                        postDelayed(this, 42L)
                    }
                }
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.isPressed = true
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            v.background = pillBg(accentContainerColor(), accentColor, dp(6))
                            (v as TextView).setTextColor(accentColor)
                            dispatchExtraKey(label)
                            v.removeCallbacks(repeatRunnable)
                            v.postDelayed(repeatRunnable, 320L)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.isPressed = false
                            v.removeCallbacks(repeatRunnable)
                            v.background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
                            (v as TextView).setTextColor(COLOR_TEXT)
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val currentSpec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)
            ?: DEFAULT_EXTRA_KEYS_SPEC
        var tempBlinkMode = cursorBlinkMode
        var tempShape = cursorShapePref
        var tempAccent = accentColor

        val scroll = ScrollView(this)
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(0xFF0A0C10.toInt())
        }
        scroll.addView(dialogLayout)

        // 1. Cursor Blink Mode Section
        dialogLayout.addView(makeSectionTitle("Режим мигания курсора"))
        val blinkRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(10))
        }
        val blinkChips = ArrayList<TextView>(3)
        fun refreshBlinkChips() {
            for (i in blinkChips.indices) {
                val active = (i == tempBlinkMode)
                blinkChips[i].setTextColor(if (active) tempAccent else COLOR_TEXT)
                blinkChips[i].background = pillBg(
                    if (active) accentContainerColor(tempAccent) else COLOR_SURFACE,
                    if (active) tempAccent else COLOR_BORDER,
                    dp(6)
                )
            }
        }
        val blinkLabels = arrayOf("Плавный (Fade)", "Обычный", "Не мигать")
        for (i in blinkLabels.indices) {
            val chip = makeOptionChip(blinkLabels[i]) {
                tempBlinkMode = i
                refreshBlinkChips()
            }
            blinkChips.add(chip)
            blinkRow.addView(chip)
        }
        dialogLayout.addView(blinkRow)

        // 2. Cursor Shape Section
        dialogLayout.addView(makeSectionTitle("Форма курсора"))
        val shapeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(10))
        }
        val shapeChips = ArrayList<TextView>(3)
        fun refreshShapeChips() {
            for (i in shapeChips.indices) {
                val active = (i == tempShape)
                shapeChips[i].setTextColor(if (active) tempAccent else COLOR_TEXT)
                shapeChips[i].background = pillBg(
                    if (active) accentContainerColor(tempAccent) else COLOR_SURFACE,
                    if (active) tempAccent else COLOR_BORDER,
                    dp(6)
                )
            }
        }
        val shapeLabels = arrayOf("Блок (█)", "Линия (_)", "Луч (▏)")
        for (i in shapeLabels.indices) {
            val chip = makeOptionChip(shapeLabels[i]) {
                tempShape = i
                refreshShapeChips()
            }
            shapeChips.add(chip)
            shapeRow.addView(chip)
        }
        dialogLayout.addView(shapeRow)

        // 3. Material You Theme Accent Section
        dialogLayout.addView(makeSectionTitle("Цвет акцента Material 3"))
        val accentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(10))
        }
        val accentOptions = arrayOf(
            "Blue" to ACCENT_MATERIAL_BLUE,
            "Monet" to ACCENT_MATERIAL_MONET,
            "Cyan" to ACCENT_MATERIAL_CYAN,
            "Mint" to ACCENT_MATERIAL_MINT
        )
        val accentChips = ArrayList<TextView>(4)
        fun refreshAccentChips() {
            for (i in accentOptions.indices) {
                val c = accentOptions[i].second
                val active = (c == tempAccent)
                accentChips[i].setTextColor(c)
                accentChips[i].background = pillBg(
                    if (active) accentContainerColor(c) else COLOR_SURFACE,
                    if (active) c else COLOR_BORDER,
                    dp(6)
                )
            }
            refreshBlinkChips()
            refreshShapeChips()
        }
        for (opt in accentOptions) {
            val chip = makeOptionChip(opt.first) {
                tempAccent = opt.second
                refreshAccentChips()
            }
            accentChips.add(chip)
            accentRow.addView(chip)
        }
        refreshAccentChips()
        dialogLayout.addView(accentRow)

        // 4. Extra-Keys Layout Section
        dialogLayout.addView(
            makeSectionTitle("Нижняя клавиатура (клавиши через | , строки через Enter)")
        )
        val input = EditText(this).apply {
            setText(currentSpec)
            setTextColor(COLOR_TEXT)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setBackgroundColor(0xFF12151C.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            minLines = 2
            maxLines = 4
        }
        dialogLayout.addView(input)

        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        presetsRow.addView(makeOptionChip("1-Row") { input.setText(DEFAULT_EXTRA_KEYS_SPEC) })
        presetsRow.addView(makeOptionChip("2-Row") { input.setText(EXTENDED_EXTRA_KEYS_SPEC) })
        presetsRow.addView(makeOptionChip("Vim/TMUX") { input.setText(VIM_TMUX_EXTRA_KEYS_SPEC) })
        presetsRow.addView(makeOptionChip("Скрыть") { input.setText("") })
        dialogLayout.addView(presetsRow)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Настройки Termuxx")
            .setView(scroll)
            .setPositiveButton("Сохранить") { _, _ ->
                cursorBlinkMode = tempBlinkMode
                cursorShapePref = tempShape
                accentColor = tempAccent
                nativeSetDefaultCursorShape(cursorShapePref)

                prefs.edit()
                    .putInt(KEY_CURSOR_BLINK_MODE, cursorBlinkMode)
                    .putInt(KEY_CURSOR_SHAPE, cursorShapePref)
                    .putInt(KEY_MATERIAL_ACCENT, accentColor)
                    .putString(KEY_EXTRA_KEYS_SPEC, input.text.toString())
                    .apply()

                rebuildTabs()
                rebuildExtraKeysBar()
                terminalSurface.updateCursorAnimatorState()
                terminalSurface.post { terminalSurface.syncSize() }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun makeSectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(4), 0, dp(2))
        }
    }

    private fun makeOptionChip(title: String, onClick: Runnable): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(COLOR_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(dp(9), dp(6), dp(9), dp(6))
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
        val ctrl = (mask and 1) != 0
        val alt = (mask and 2) != 0
        for (btn in modifierButtons) {
            val tag = btn.tag as? String ?: continue
            val active = (tag == "CTRL" && ctrl) || (tag == "ALT" && alt)
            btn.setTextColor(if (active) accentColor else COLOR_TEXT)
            btn.background = pillBg(
                if (active) accentContainerColor() else COLOR_SURFACE,
                if (active) accentColor else COLOR_BORDER,
                dp(6)
            )
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
        nativeSetDefaultCursorShape(cursorShapePref)
        rebuildTabs()
        terminalSurface.resetScrollAndBlink()
        terminalSurface.invalidateAllRows()
    }

    internal fun closeCurrentSession() {
        val active = nativeGetActiveIndex()
        if (active < 0) return
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
        val count = nativeGetSessionCount()
        val active = nativeGetActiveIndex()
        for (i in 0 until count) {
            val idx = i
            val isRoot = nativeIsSessionRoot(i)
            val isSelected = i == active
            val tabAccent = if (isRoot) COLOR_ROOT else accentColor

            val tab = TextView(this).apply {
                text = (if (isRoot) "# " else "$ ") + (i + 1)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                setTextColor(if (isSelected) tabAccent else COLOR_TEXT_DIM)
                background = pillBg(
                    if (isSelected) accentContainerColor(tabAccent) else COLOR_SURFACE,
                    if (isSelected) tabAccent else COLOR_BORDER,
                    dp(8)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(6), 0)
                }
                setOnClickListener {
                    terminalSurface.stopTextSelection()
                    nativeSwitchSession(idx)
                    terminalSurface.syncSize()
                    rebuildTabs()
                    terminalSurface.invalidateAllRows()
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
            terminalSurface.resetScrollAndBlink()
        }
    }

    internal fun hasClipboardText(): Boolean {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = cm.primaryClip ?: return false
        return clip.itemCount > 0 && !clip.getItemAt(0).coerceToText(this).isNullOrEmpty()
    }

    private fun makeHeaderButton(label: String, action: Runnable): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(COLOR_TEXT)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(28)).apply {
                setMargins(dp(4), 0, 0, 0)
            }
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                action.run()
            }
        }
    }

    private fun pillBg(fill: Int, stroke: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            setCornerRadius(radiusPx.toFloat())
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

    // ========================================================================
    // Event-Driven 165Hz TUI Surface + Smooth Material Cursor + ColorOS 17 Selection
    // ========================================================================
    inner class TerminalSurface(context: Context) : View(context) {

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        private val bgPaint = Paint()
        private val graphicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val wavePath = Path()
        private val baseTypeface: Typeface

        internal var cellWidth = 18f
        internal var cellHeight = 36f
        private var cellBaseline = 28f
        private var fontScale = 1.0f

        internal var rows = 24
        internal var cols = 80
        internal var scrollOffset = 0
        private var scrollAccum = 0f

        private var cursorAlpha = 0.88f
        private var cursorBlinkOn = true
        private var activityResumed = false

        private val bgRunsBuf = IntArray(512 * 3)
        private val glyphRunsBuf = IntArray(512 * 4)
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

        // Smooth Material Cursor Fade Animator (only invalidates cursor cell bounds)
        private val smoothCursorAnimator = ValueAnimator.ofFloat(0.14f, 0.92f).apply {
            duration = 680L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                cursorAlpha = anim.animatedValue as Float
                invalidateCursorCellOnly()
            }
        }

        private val classicBlinkRunnable = object : Runnable {
            override fun run() {
                if (!activityResumed || cursorBlinkMode != BLINK_CLASSIC) return
                if (scrollOffset == 0 && !nativeIsSelectingText()) {
                    cursorBlinkOn = !cursorBlinkOn
                    cursorAlpha = if (cursorBlinkOn) 0.90f else 0.16f
                    invalidateCursorCellOnly()
                }
                postDelayed(this, 530L)
            }
        }

        init {
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(COLOR_AMOLED_BG)

            baseTypeface = try {
                Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
            } catch (_: Throwable) {
                Typeface.MONOSPACE
            }

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
                        updateCursorAnimatorState()
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
                            updateCursorAnimatorState()
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

        private fun invalidateCursorCellOnly() {
            val cursorState = nativeGetCursorState()
            if (cursorState >= 0) {
                val cRow = (cursorState ushr 16) and 0x1FFF
                val cCol = cursorState and 0xFFFF
                val cx = (cCol * cellWidth).toInt()
                val cy = (cRow * cellHeight).toInt()
                postInvalidateOnAnimation(
                    max(0, cx - 2),
                    max(0, cy - 2),
                    min(width, (cx + cellWidth + 4).toInt()),
                    min(height, (cy + cellHeight + 4).toInt())
                )
            }
        }

        internal fun updateCursorAnimatorState() {
            removeCallbacks(classicBlinkRunnable)
            val shouldAnimate = activityResumed && scrollOffset == 0 && !nativeIsSelectingText()
            when (cursorBlinkMode) {
                BLINK_SMOOTH -> {
                    if (shouldAnimate) {
                        if (!smoothCursorAnimator.isStarted) smoothCursorAnimator.start()
                    } else {
                        if (smoothCursorAnimator.isStarted) smoothCursorAnimator.cancel()
                        cursorAlpha = 0.88f
                    }
                }
                BLINK_CLASSIC -> {
                    if (smoothCursorAnimator.isStarted) smoothCursorAnimator.cancel()
                    cursorBlinkOn = true
                    cursorAlpha = 0.90f
                    if (shouldAnimate) postDelayed(classicBlinkRunnable, 530L)
                }
                else -> {
                    if (smoothCursorAnimator.isStarted) smoothCursorAnimator.cancel()
                    cursorAlpha = 0.88f
                }
            }
            postInvalidateOnAnimation()
        }

        internal fun onActivityResume() {
            activityResumed = true
            updateCursorAnimatorState()
        }

        internal fun onActivityPause() {
            activityResumed = false
            removeCallbacks(classicBlinkRunnable)
            if (smoothCursorAnimator.isStarted) smoothCursorAnimator.cancel()
            stopTextSelection()
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
            updateCursorAnimatorState()
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
            cursorAlpha = 0.92f
            if (cursorBlinkMode == BLINK_SMOOTH && smoothCursorAnimator.isStarted) {
                smoothCursorAnimator.currentPlayTime = 680L
            }
            updateCursorAnimatorState()
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
            canvas.drawColor(COLOR_AMOLED_BG)

            val bootState = nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
            if (bootState != 2) {
                textPaint.color = accentColor
                textPaint.typeface = baseTypeface
                canvas.drawText(
                    "⚡ Инициализация среды Termuxx (Android 17 / C++23)...",
                    dp(16).toFloat(),
                    height / 2f,
                    textPaint
                )
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
                nativeSetDefaultCursorShape(cursorShapePref)
                rebuildTabs()
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

            // Draw Smooth Material Cursor (DECSCUSR) when at bottom of scrollback and not selecting
            if (scrollOffset == 0 && !selecting) {
                val cursorState = nativeGetCursorState()
                if (cursorState >= 0) {
                    val shape = (cursorState ushr 29) and 3
                    val cRow = (cursorState ushr 16) and 0x1FFF
                    val cCol = cursorState and 0xFFFF
                    if (cRow in 0 until rows && cCol in 0 until cols) {
                        val cx = cCol * cellWidth
                        val cy = cRow * cellHeight
                        val alphaInt = (cursorAlpha.coerceIn(0.05f, 1.0f) * 255f).roundToInt()
                        bgPaint.color = (alphaInt shl 24) or (accentColor and 0x00FFFFFF)
                        when (shape) {
                            1 -> canvas.drawRoundRect(
                                cx,
                                cy + cellHeight - dpF(2.5f),
                                cx + cellWidth,
                                cy + cellHeight,
                                dpF(1.2f),
                                dpF(1.2f),
                                bgPaint
                            )
                            2 -> canvas.drawRoundRect(
                                cx,
                                cy + dpF(1f),
                                cx + dpF(2.4f),
                                cy + cellHeight - dpF(1f),
                                dpF(1.2f),
                                dpF(1.2f),
                                bgPaint
                            )
                            else -> canvas.drawRoundRect(
                                cx,
                                cy + dpF(1f),
                                cx + cellWidth,
                                cy + cellHeight - dpF(1f),
                                dpF(2f),
                                dpF(2f),
                                bgPaint
                            )
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
                    val style = when {
                        (styleBits and 5) == 5 -> Typeface.BOLD_ITALIC
                        (styleBits and 1) != 0 -> Typeface.BOLD
                        (styleBits and 4) != 0 -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    }
                    textPaint.typeface = Typeface.create(baseTypeface, style)
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
                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    if (!text.isNullOrEmpty()) {
                        stopTextSelection()
                        nativeSendText(text.toString(), false)
                        syncModifierButtons(nativeGetModifierMask())
                        resetScrollAndBlink()
                    }
                    return true
                }

                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    stopTextSelection()
                    for (i in 0 until beforeLength) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                    }
                    for (i in 0 until afterLength) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, 0, 0)
                    }
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
            syncModifierButtons(nativeGetModifierMask())
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
            try {
                System.loadLibrary("termuxx-core")
            } catch (_: UnsatisfiedLinkError) {
                System.loadLibrary("termux-core")
            }
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
        @JvmStatic external fun nativeSetDefaultCursorShape(shape: Int)
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
