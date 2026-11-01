package com.termuxx.app

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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.text.InputType
import android.util.TypedValue
import android.view.ActionMode
import android.view.Choreographer
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
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.termuxx.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
private const val COLOR_SURFACE = 0xFF080808.toInt()
private const val COLOR_BORDER = 0xFF161616.toInt()
private const val COLOR_TEXT = 0xFFEAEAEA.toInt()
private const val COLOR_TEXT_DIM = 0xFF8A8A8A.toInt()
private const val COLOR_ACCENT = 0xFF00E676.toInt()
private const val COLOR_ROOT = 0xFFFF5252.toInt()

private const val PREFS_NAME = "termuxx_prefs"
private const val KEY_EXTRA_KEYS_SPEC = "extra_keys_spec_v2"

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

private const val HANDLE_LEFT = 0
private const val HANDLE_RIGHT = 2

/**
 * Termuxx — Modern Kotlin + C++23 (C++2b) Minimalist Pure Dark AMOLED Terminal.
 *
 * Architecture:
 * - Zero Java files: 100% Kotlin UI layer + C++23 native engine (`libtermuxx-core.so` & `libtermuxx-redirect.so`).
 * - Authentic Original Termux Native System Text Selection (`PopupWindow` handles with
 *   `android.R.attr.textSelectHandleWindowStyle` + floating `ActionMode.TYPE_FLOATING` system toolbar).
 * - Real-time logical-line reflow on pinch-to-zoom (`ScaleGestureDetector` -> `NeoTerminalEngine::resize`).
 * - Hardware-accelerated per-row `RenderNode` display list caching for 120Hz/165Hz AMOLED displays.
 */
class TermuxActivity : Activity(), Choreographer.FrameCallback {

    private lateinit var prefs: SharedPreferences
    private lateinit var tabStrip: LinearLayout
    private lateinit var extraKeysRoot: LinearLayout
    internal lateinit var terminalSurface: TerminalSurface
    private val modifierButtons = ArrayList<TextView>(4)
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        window.apply {
            statusBarColor = COLOR_AMOLED_BG
            navigationBarColor = COLOR_AMOLED_BG
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes = attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(true)
            }
            @Suppress("DEPRECATION")
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            )
        }

        requestHighRefreshRate()
        startForegroundKeepAlive()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
        }

        // Minimalist Pure Dark AMOLED Header Bar (32dp)
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
            setPadding(dp(6), dp(2), dp(6), dp(2))
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

        headerBar.addView(makeHeaderButton("+") { createSession(false) })
        headerBar.addView(makeHeaderButton("#") { createSession(true) })
        headerBar.addView(makeHeaderButton("⚙") { showExtraKeysSettingsDialog() })
        headerBar.addView(makeHeaderButton("✕") { closeCurrentSession() })

        root.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32))
        )

        val topDivider = View(this).apply {
            setBackgroundColor(COLOR_BORDER)
        }
        root.addView(topDivider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))

        // Terminal Viewport Container
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
        root.addView(viewport, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // Minimalist Customizable Extra Keys Container
        extraKeysRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
        }
        rebuildExtraKeysBar()
        root.addView(
            extraKeysRoot,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
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

        val lines = spec.split("\n")
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val tokens = line.split("|")
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(COLOR_AMOLED_BG)
                setPadding(dp(2), dp(2), dp(2), dp(2))
            }

            var added = 0
            for (rawToken in tokens) {
                val label = rawToken.trim()
                if (label.isEmpty()) continue
                val btn = createExtraKeyButton(label)
                if (label == "CTRL" || label == "ALT") {
                    modifierButtons.add(btn)
                }
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, dp(32), 1f))
                added++
            }
            if (added > 0) {
                extraKeysRoot.addView(rowLayout)
            }
        }
        syncModifierButtons(nativeGetModifierMask())
    }

    private fun createExtraKeyButton(label: String): TextView {
        return TextView(this).apply {
            text = label
            tag = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_TEXT_DIM)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(5))
            val lp = LinearLayout.LayoutParams(0, dp(32), 1f).apply {
                setMargins(dp(1), dp(1), dp(1), dp(1))
            }
            layoutParams = lp
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                terminalSurface.stopTextSelection()
                val mask = nativeDispatchCustomKey(label)
                syncModifierButtons(mask)
                terminalSurface.resetScrollAndBlink()
            }
        }
    }

    private fun showExtraKeysSettingsDialog() {
        val currentSpec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)
            ?: DEFAULT_EXTRA_KEYS_SPEC

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(0xFF0A0A0A.toInt())
        }

        val hint = TextView(this).apply {
            text = "Формат: клавиши через | , строки через Enter.\n" +
                "Поддерживаются: ESC, TAB, CTRL, ALT, ENTER, BKSP, DEL, INS, HOME, END, PGUP, PGDN, ▲, ▼, ◀, ▶, F1..F12, CTRL+C и любые символы."
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
            setBackgroundColor(0xFF121212.toInt())
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
            setTextColor(COLOR_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = pillBg(0xFF141414.toInt(), COLOR_BORDER, dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dp(6), 0)
            }
            setOnClickListener { onClick.run() }
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        Choreographer.getInstance().postFrameCallback(this)
        terminalSurface.requestFocus()
    }

    override fun onPause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        terminalSurface.stopTextSelection()
        super.onPause()
    }

    override fun onDestroy() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        terminalSurface.stopTextSelection()
        if (isFinishing) {
            nativeDestroyAll()
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (terminalSurface.isSelectingText()) {
            terminalSurface.stopTextSelection()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return

        val bootState = nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
        if (bootState == 2 && nativeGetSessionCount() == 0) {
            val rows = max(5, terminalSurface.rows)
            val cols = max(10, terminalSurface.cols)
            nativeNewSession(
                false,
                rows,
                cols,
                terminalSurface.cellWidth.roundToInt(),
                terminalSurface.cellHeight.roundToInt()
            )
            rebuildTabs()
            terminalSurface.invalidateAllRows()
        }

        val oscClip = nativeConsumeOsc52Clipboard()
        if (!oscClip.isNullOrEmpty()) {
            copyToClipboard(oscClip, false)
        }

        val dirty = nativeConsumeFrameDirty()
        val blinkTick = terminalSurface.updateCursorBlink(frameTimeNanos)

        if (dirty || blinkTick || bootState != 2) {
            terminalSurface.invalidateDirtyRows(dirty || bootState != 2)
        }

        if (running) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    internal fun syncModifierButtons(mask: Int) {
        val ctrl = (mask and 1) != 0
        val alt = (mask and 2) != 0
        for (btn in modifierButtons) {
            val tag = btn.tag as? String ?: continue
            val active = (tag == "CTRL" && ctrl) || (tag == "ALT" && alt)
            btn.setTextColor(if (active) COLOR_AMOLED_BG else COLOR_TEXT_DIM)
            btn.background = pillBg(
                if (active) COLOR_ACCENT else COLOR_SURFACE,
                if (active) COLOR_ACCENT else COLOR_BORDER,
                dp(5)
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
        rebuildTabs()
        terminalSurface.resetScrollAndBlink()
        terminalSurface.invalidateAllRows()
    }

    private fun closeCurrentSession() {
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

            val tab = TextView(this).apply {
                text = (if (isRoot) "# " else "$ ") + (i + 1)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(dp(10), dp(3), dp(10), dp(3))
                val accent = if (isRoot) COLOR_ROOT else COLOR_ACCENT
                setTextColor(if (isSelected) accent else COLOR_TEXT_DIM)
                background = pillBg(
                    if (isSelected) 0xFF0E0E0E.toInt() else COLOR_AMOLED_BG,
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(COLOR_TEXT_DIM)
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(24)).apply {
                setMargins(dp(3), 0, 0, 0)
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

    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val d = display ?: return
            var best: android.view.Display.Mode? = null
            for (m in d.supportedModes) {
                if (best == null || m.refreshRate > best.refreshRate) {
                    best = m
                }
            }
            if (best != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = best.modeId
                }
            }
        }
    }

    private fun startForegroundKeepAlive() {
        try {
            val i = Intent(this, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i)
            } else {
                startService(i)
            }
        } catch (_: Throwable) {
        }
    }

    // ========================================================================
    // High-Frequency 165Hz TUI Surface with Original Termux Native Selection
    // ========================================================================
    inner class TerminalSurface(context: Context) : View(context) {

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        private val bgPaint = Paint()
        private val graphicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

        private var cursorBlinkOn = true
        private var lastBlinkNs = 0L

        private val bgRunsBuf = IntArray(512 * 3)
        private val glyphRunsBuf = IntArray(512 * 4)
        private val charBuf = CharArray(2)

        private var rowNodes: Array<RenderNode?>? = null
        private var rowNodesValid: BooleanArray? = null
        private var cachedScrollOffset = Int.MIN_VALUE

        private val gestureDetector: GestureDetector
        private val scaleDetector: ScaleGestureDetector
        internal val selectionController: TextSelectionController

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

            selectionController = TextSelectionController(this)

            gestureDetector = GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        if (selectionController.isActive) {
                            stopTextSelection()
                            return true
                        }
                        requestFocus()
                        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showSoftInput(this@TerminalSurface, 0)

                        val modes = nativeGetModesMask()
                        if ((modes and 1) != 0) {
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
                        startTextSelection(e)
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

        internal fun getColumnAndRow(event: MotionEvent, adjustForFirstCharacter: Boolean): IntArray {
            var column = (event.x / cellWidth).toInt()
            var row = ((event.y - paddingTop) / cellHeight).toInt()
            if (adjustForFirstCharacter && column > 0) {
                column--
            }
            column = column.coerceIn(0, max(0, cols - 1))
            row = row.coerceIn(0, max(0, rows - 1))
            return intArrayOf(column, row)
        }

        internal fun startTextSelection(event: MotionEvent) {
            selectionController.show(event)
            invalidateAllRows()
        }

        internal fun stopTextSelection(): Boolean {
            if (!selectionController.isActive && !nativeIsSelectingText()) return false
            val stopped = selectionController.hide()
            invalidateAllRows()
            return stopped
        }

        internal fun isSelectingText(): Boolean = selectionController.isActive

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
            lastBlinkNs = System.nanoTime()
            invalidateAllRows()
        }

        internal fun updateCursorBlink(nowNs: Long): Boolean {
            if (nowNs - lastBlinkNs >= 530_000_000L) {
                lastBlinkNs = nowNs
                cursorBlinkOn = !cursorBlinkOn
                return true
            }
            return false
        }

        internal fun invalidateAllRows() {
            rowNodesValid?.fill(false)
            postInvalidateOnAnimation()
        }

        internal fun invalidateDirtyRows(checkEngineDirty: Boolean) {
            val valid = rowNodesValid
            if (valid != null && checkEngineDirty) {
                if (scrollOffset != cachedScrollOffset) {
                    valid.fill(false)
                } else {
                    for (r in 0 until min(rows, valid.size)) {
                        if (nativeIsRowDirty(r)) {
                            valid[r] = false
                        }
                    }
                    nativeClearDirtyFlags()
                }
            }
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

            if (nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir) != 2) {
                textPaint.color = COLOR_ACCENT
                textPaint.typeface = baseTypeface
                canvas.drawText(
                    "⚡ Инициализация среды Termuxx (C++23 + Latest Packages)...",
                    dp(16).toFloat(),
                    height / 2f,
                    textPaint
                )
                return
            }

            val useRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && canvas.isHardwareAccelerated
            val nodes = rowNodes
            val valid = rowNodesValid
            if (useRenderNode && (nodes == null || valid == null || nodes.size != rows)) {
                rowNodes = arrayOfNulls(rows)
                rowNodesValid = BooleanArray(rows)
            }

            if (scrollOffset != cachedScrollOffset) {
                rowNodesValid?.fill(false)
                cachedScrollOffset = scrollOffset
            }

            for (r in 0 until rows) {
                val top = r * cellHeight
                if (useRenderNode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val rNodes = rowNodes!!
                    val rValid = rowNodesValid!!
                    var node = rNodes[r]
                    if (node == null) {
                        node = RenderNode("row_$r")
                        rNodes[r] = node
                        rValid[r] = false
                    }
                    if (!rValid[r] || !node.hasDisplayList()) {
                        node.setPosition(0, 0, width, ceil(cellHeight.toDouble()).toInt())
                        val rc = node.beginRecording()
                        drawSingleRow(rc, r, 0f)
                        node.endRecording()
                        rValid[r] = true
                    }
                    canvas.save()
                    canvas.translate(0f, top)
                    canvas.drawRenderNode(node)
                    canvas.restore()
                } else {
                    drawSingleRow(canvas, r, top)
                }
            }

            // Draw Cursor (DECSCUSR) when at bottom of scrollback and not selecting text
            if (scrollOffset == 0 && !selectionController.isActive) {
                val cursorState = nativeGetCursorState()
                if (cursorState >= 0) {
                    val shape = (cursorState ushr 29) and 3
                    val cRow = (cursorState ushr 16) and 0x1FFF
                    val cCol = cursorState and 0xFFFF
                    if (cRow in 0 until rows && cCol in 0 until cols) {
                        val cx = cCol * cellWidth
                        val cy = cRow * cellHeight
                        bgPaint.color = if (cursorBlinkOn) 0xCC00E676.toInt() else 0x4400E676
                        when (shape) {
                            1 -> canvas.drawRect(cx, cy + cellHeight - dp(2), cx + cellWidth, cy + cellHeight, bgPaint)
                            2 -> canvas.drawRect(cx, cy, cx + dp(2), cy + cellHeight, bgPaint)
                            else -> canvas.drawRect(cx, cy, cx + cellWidth, cy + cellHeight, bgPaint)
                        }
                    }
                }
            }

            // Keep native selection handles and floating ActionMode synchronized with viewport
            if (selectionController.isActive) {
                selectionController.render()
            }
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

                if (!drawProceduralBoxOrBlock(rc, cp, x, top, cellWidth, cellHeight, fg)) {
                    val styleBits = flags and 3
                    if (styleBits != lastFlags) {
                        lastFlags = styleBits
                        val style = when {
                            (styleBits and 3) == 3 -> Typeface.BOLD_ITALIC
                            (styleBits and 1) != 0 -> Typeface.BOLD
                            (styleBits and 2) != 0 -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                        textPaint.typeface = Typeface.create(baseTypeface, style)
                    }
                    textPaint.color = fg
                    val charCount = Character.toChars(cp, charBuf, 0)
                    rc.drawText(charBuf, 0, charCount, x, baseY, textPaint)
                }

                // Underline / Double / Undercurl
                if ((flags and 4) != 0) {
                    graphicPaint.color = fg
                    graphicPaint.strokeWidth = max(1.5f, cellHeight * 0.05f)
                    if ((flags and 2048) != 0) {
                        graphicPaint.style = Paint.Style.STROKE
                        wavePath.reset()
                        val wy = bottom - graphicPaint.strokeWidth * 1.5f
                        val amp = max(1.5f, cellHeight * 0.06f)
                        wavePath.moveTo(x, wy)
                        wavePath.quadTo(x + cellWidth * 0.25f, wy - amp, x + cellWidth * 0.5f, wy)
                        wavePath.quadTo(x + cellWidth * 0.75f, wy + amp, x + cellWidth, wy)
                        rc.drawPath(wavePath, graphicPaint)
                        graphicPaint.style = Paint.Style.FILL
                    } else if ((flags and 4096) != 0) {
                        val u1 = bottom - graphicPaint.strokeWidth * 3f
                        val u2 = bottom - graphicPaint.strokeWidth
                        rc.drawLine(x, u1, x + cellWidth, u1, graphicPaint)
                        rc.drawLine(x, u2, x + cellWidth, u2, graphicPaint)
                    } else {
                        val u = bottom - graphicPaint.strokeWidth
                        rc.drawLine(x, u, x + cellWidth, u, graphicPaint)
                    }
                }

                // Strikethrough
                if ((flags and 64) != 0) {
                    graphicPaint.color = fg
                    graphicPaint.strokeWidth = max(1.5f, cellHeight * 0.05f)
                    val sy = top + cellHeight * 0.52f
                    rc.drawLine(x, sy, x + cellWidth, sy, graphicPaint)
                }
            }
        }

        private fun drawProceduralBoxOrBlock(
            c: Canvas,
            cp: Int,
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            color: Int
        ): Boolean {
            if (cp < 0x2500 || cp > 0x259F) return false
            graphicPaint.color = color
            graphicPaint.style = Paint.Style.FILL
            val cx = x + w * 0.5f
            val cy = y + h * 0.5f
            val lw = max(1.5f, w * 0.12f)
            val hw = lw * 0.5f

            if (cp in 0x2580..0x259F) {
                when (cp) {
                    0x2580 -> { c.drawRect(x, y, x + w, cy, graphicPaint); return true }
                    0x2581 -> { c.drawRect(x, y + h * 0.875f, x + w, y + h, graphicPaint); return true }
                    0x2582 -> { c.drawRect(x, y + h * 0.75f, x + w, y + h, graphicPaint); return true }
                    0x2583 -> { c.drawRect(x, y + h * 0.625f, x + w, y + h, graphicPaint); return true }
                    0x2584 -> { c.drawRect(x, cy, x + w, y + h, graphicPaint); return true }
                    0x2585 -> { c.drawRect(x, y + h * 0.375f, x + w, y + h, graphicPaint); return true }
                    0x2586 -> { c.drawRect(x, y + h * 0.25f, x + w, y + h, graphicPaint); return true }
                    0x2587 -> { c.drawRect(x, y + h * 0.125f, x + w, y + h, graphicPaint); return true }
                    0x2588 -> { c.drawRect(x, y, x + w, y + h, graphicPaint); return true }
                    0x2589 -> { c.drawRect(x, y, x + w * 0.875f, y + h, graphicPaint); return true }
                    0x258A -> { c.drawRect(x, y, x + w * 0.75f, y + h, graphicPaint); return true }
                    0x258B -> { c.drawRect(x, y, x + w * 0.625f, y + h, graphicPaint); return true }
                    0x258C -> { c.drawRect(x, y, cx, y + h, graphicPaint); return true }
                    0x258D -> { c.drawRect(x, y, x + w * 0.375f, y + h, graphicPaint); return true }
                    0x258E -> { c.drawRect(x, y, x + w * 0.25f, y + h, graphicPaint); return true }
                    0x258F -> { c.drawRect(x, y, x + w * 0.125f, y + h, graphicPaint); return true }
                    0x2590 -> { c.drawRect(cx, y, x + w, y + h, graphicPaint); return true }
                    0x2594 -> { c.drawRect(x, y, x + w, y + h * 0.125f, graphicPaint); return true }
                    0x2595 -> { c.drawRect(x + w * 0.875f, y, x + w, y + h, graphicPaint); return true }
                    else -> return false
                }
            }

            when (cp) {
                0x2500, 0x2501 -> { c.drawRect(x, cy - hw, x + w, cy + hw, graphicPaint); return true }
                0x2502, 0x2503 -> { c.drawRect(cx - hw, y, cx + hw, y + h, graphicPaint); return true }
                0x250C, 0x250F, 0x256D -> {
                    c.drawRect(cx - hw, cy - hw, x + w, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, cy - hw, cx + hw, y + h, graphicPaint)
                    return true
                }
                0x2510, 0x2513, 0x256E -> {
                    c.drawRect(x, cy - hw, cx + hw, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, cy - hw, cx + hw, y + h, graphicPaint)
                    return true
                }
                0x2514, 0x2517, 0x2570 -> {
                    c.drawRect(cx - hw, cy - hw, x + w, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, y, cx + hw, cy + hw, graphicPaint)
                    return true
                }
                0x2518, 0x251B, 0x256F -> {
                    c.drawRect(x, cy - hw, cx + hw, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, y, cx + hw, cy + hw, graphicPaint)
                    return true
                }
                0x251C, 0x2523 -> {
                    c.drawRect(cx - hw, y, cx + hw, y + h, graphicPaint)
                    c.drawRect(cx - hw, cy - hw, x + w, cy + hw, graphicPaint)
                    return true
                }
                0x2524, 0x252B -> {
                    c.drawRect(cx - hw, y, cx + hw, y + h, graphicPaint)
                    c.drawRect(x, cy - hw, cx + hw, cy + hw, graphicPaint)
                    return true
                }
                0x252C, 0x2533 -> {
                    c.drawRect(x, cy - hw, x + w, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, cy - hw, cx + hw, y + h, graphicPaint)
                    return true
                }
                0x2534, 0x253B -> {
                    c.drawRect(x, cy - hw, x + w, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, y, cx + hw, cy + hw, graphicPaint)
                    return true
                }
                0x253C, 0x254B -> {
                    c.drawRect(x, cy - hw, x + w, cy + hw, graphicPaint)
                    c.drawRect(cx - hw, y, cx + hw, cy + hw, graphicPaint)
                    return true
                }
                else -> return false
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
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
            if (keyCode == KeyEvent.KEYCODE_BACK && selectionController.isActive) {
                if (event.action == KeyEvent.ACTION_UP) {
                    stopTextSelection()
                }
                return true
            }
            return super.onKeyPreIme(keyCode, event)
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            if (selectionController.isActive) {
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

        override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN && event.characters != null) {
                stopTextSelection()
                nativeSendText(event.characters, false)
                resetScrollAndBlink()
                return true
            }
            return super.onKeyMultiple(keyCode, repeatCount, event)
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            if (visibility != VISIBLE) {
                stopTextSelection()
            }
        }
    }

    // ========================================================================
    // Original Termux Native System Selection Controller (PopupWindow + ActionMode.TYPE_FLOATING)
    // ========================================================================
    inner class TextSelectionController(private val surface: TerminalSurface) :
        ViewTreeObserver.OnTouchModeChangeListener {

        private val startHandle = SelectionHandleView(surface, this, HANDLE_LEFT)
        private val endHandle = SelectionHandleView(surface, this, HANDLE_RIGHT)
        private val handleHeight = max(startHandle.handleHeight, endHandle.handleHeight)
        private val selectors = IntArray(4)

        internal var isActive = false
            private set
        internal var actionMode: ActionMode? = null
            private set

        fun show(event: MotionEvent) {
            val colRow = surface.getColumnAndRow(event, true)
            nativeStartTextSelection(surface.scrollOffset, colRow[0], colRow[1])
            nativeGetSelectionSelectors(selectors)

            startHandle.positionAtCursor(selectors[2], selectors[0], true)
            endHandle.positionAtCursor(selectors[3] + 1, selectors[1], true)

            setupActionMode()
            surface.viewTreeObserver.addOnTouchModeChangeListener(this)
            isActive = true
        }

        fun hide(): Boolean {
            if (!isActive && !nativeIsSelectingText()) return false
            startHandle.hide()
            endHandle.hide()
            actionMode?.finish()
            actionMode = null
            surface.viewTreeObserver.removeOnTouchModeChangeListener(this)
            isActive = false
            return nativeStopTextSelection()
        }

        fun render() {
            if (!isActive) return
            nativeGetSelectionSelectors(selectors)
            startHandle.positionAtCursor(selectors[2], selectors[0], false)
            endHandle.positionAtCursor(selectors[3] + 1, selectors[1], false)
            actionMode?.invalidateContentRect()
        }

        private fun setupActionMode() {
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
                    if (!isActive) return true
                    when (item.itemId) {
                        ITEM_COPY -> {
                            val selected = nativeGetSelectedText()
                            copyToClipboard(selected, true)
                            surface.stopTextSelection()
                        }
                        ITEM_PASTE -> {
                            surface.stopTextSelection()
                            pasteFromClipboard()
                        }
                        ITEM_SELECT_ALL -> {
                            nativeSelectAll(surface.scrollOffset)
                            surface.invalidateAllRows()
                            render()
                        }
                        ITEM_SHARE -> {
                            val selected = nativeGetSelectedText()
                            surface.stopTextSelection()
                            if (!selected.isNullOrEmpty()) {
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

                override fun onDestroyActionMode(mode: ActionMode) {
                    // Handled explicitly via hide() so handle dragging can temporarily hide/show toolbar
                }

                override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                    nativeGetSelectionSelectors(selectors)
                    val selY1 = selectors[0]
                    val selY2 = selectors[1]
                    val selX1 = selectors[2]
                    val selX2 = selectors[3]

                    val x1 = (selX1 * surface.cellWidth).roundToInt()
                    val x2 = ((selX2 + 1) * surface.cellWidth).roundToInt()
                    val y1 = ((selY1 + surface.scrollOffset) * surface.cellHeight).roundToInt()
                    val y2 = ((selY2 + 1 + surface.scrollOffset) * surface.cellHeight).roundToInt()

                    val left = min(x1, x2).coerceIn(0, max(1, view.width))
                    val right = max(x1, x2).coerceIn(left, max(left + 1, view.width))
                    val top = (y1 + surface.paddingTop).coerceIn(0, max(1, view.height))
                    val bottom = (y2 + handleHeight + surface.paddingTop).coerceIn(top, max(top + 1, view.height))

                    outRect.set(left, top, right, bottom)
                }
            }

            actionMode = surface.startActionMode(callback, ActionMode.TYPE_FLOATING)
        }

        fun updatePosition(handle: SelectionHandleView, x: Int, y: Int) {
            nativeGetSelectionSelectors(selectors)
            var selY1 = selectors[0]
            var selY2 = selectors[1]
            var selX1 = selectors[2]
            var selX2 = selectors[3]

            var row = (y / surface.cellHeight).toInt()
            row = row.coerceIn(0, surface.rows - 1)
            val logicalRow = row - surface.scrollOffset

            var col = (x / surface.cellWidth).toInt()
            col = col.coerceIn(0, surface.cols - 1)

            val isStart = (handle === startHandle)
            nativeUpdateSelectionHandle(isStart, col, logicalRow)
            nativeGetSelectionSelectors(selectors)

            selY1 = selectors[0]
            selY2 = selectors[1]
            selX1 = selectors[2]
            selX2 = selectors[3]

            startHandle.positionAtCursor(selX1, selY1, false)
            endHandle.positionAtCursor(selX2 + 1, selY2, false)
            actionMode?.invalidateContentRect()
            surface.invalidateAllRows()
        }

        override fun onTouchModeChanged(isInTouchMode: Boolean) {
            if (!isInTouchMode) {
                surface.stopTextSelection()
            }
        }
    }

    // ========================================================================
    // Original Termux SelectionHandleView (PopupWindow with textSelectHandleWindowStyle)
    // ========================================================================
    inner class SelectionHandleView(
        private val surface: TerminalSurface,
        private val controller: TextSelectionController,
        private val initialOrientation: Int
    ) : View(surface.context) {

        private var window: PopupWindow? = null
        private val leftDrawable: Drawable = context.getDrawable(R.drawable.text_select_handle_left_material)
            ?: ColorDrawable(0)
        private val rightDrawable: Drawable = context.getDrawable(R.drawable.text_select_handle_right_material)
            ?: ColorDrawable(0)
        private var handleDrawable: Drawable = leftDrawable

        private var isDragging = false
        private val tempCoords = IntArray(2)
        private var tempRect: Rect? = null

        private var pointX = 0
        private var pointY = 0
        private var touchToWindowOffsetX = 0f
        private var touchToWindowOffsetY = 0f
        private var hotspotX = 0f
        private var touchOffsetY = 0f
        private var lastParentX = 0
        private var lastParentY = 0

        internal var handleHeight: Int = 0
            private set
        private var handleWidth: Int = 0
        private var orientation: Int = 0
        private var lastTime = 0L

        init {
            setOrientation(initialOrientation)
        }

        private fun initWindow() {
            if (window == null) {
                window = PopupWindow(surface.context, null, android.R.attr.textSelectHandleWindowStyle).apply {
                    isSplitTouchEnabled = true
                    isClippingEnabled = false
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    setBackgroundDrawable(null)
                    animationStyle = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL
                    }
                    contentView = this@SelectionHandleView
                }
            }
        }

        fun setOrientation(newOrientation: Int) {
            orientation = newOrientation
            var w = 0
            when (newOrientation) {
                HANDLE_LEFT -> {
                    handleDrawable = leftDrawable
                    w = handleDrawable.intrinsicWidth
                    hotspotX = (w * 3) / 4f
                }
                HANDLE_RIGHT -> {
                    handleDrawable = rightDrawable
                    w = handleDrawable.intrinsicWidth
                    hotspotX = w / 4f
                }
            }
            handleHeight = handleDrawable.intrinsicHeight
            handleWidth = w
            touchOffsetY = -handleHeight * 0.3f
            invalidate()
        }

        fun show() {
            if (!isPositionVisible()) {
                hide()
                return
            }
            removeFromParent()
            initWindow()
            invalidate()

            val coords = tempCoords
            surface.getLocationInWindow(coords)
            coords[0] += pointX
            coords[1] += pointY

            window?.showAtLocation(surface, Gravity.NO_GRAVITY, coords[0], coords[1])
        }

        fun hide() {
            isDragging = false
            window?.dismiss()
            removeFromParent()
            window = null
        }

        fun removeFromParent() {
            if (!isShowing) {
                (parent as? ViewGroup)?.removeView(this)
            }
        }

        fun positionAtCursor(cx: Int, logicalCy: Int, forceOrientationCheck: Boolean) {
            val x = (cx * surface.cellWidth).roundToInt()
            val y = ((logicalCy + 1 + surface.scrollOffset) * surface.cellHeight).roundToInt()
            moveTo(x, y, forceOrientationCheck)
        }

        private fun moveTo(x: Int, y: Int, forceOrientationCheck: Boolean) {
            val oldHotspotX = hotspotX
            checkChangedOrientation(x, forceOrientationCheck)
            pointX = (x - if (isShowing) oldHotspotX else hotspotX).roundToInt()
            pointY = y + surface.paddingTop

            if (isPositionVisible()) {
                var coords: IntArray? = null
                if (isShowing) {
                    coords = tempCoords
                    surface.getLocationInWindow(coords)
                    val x1 = coords[0] + pointX
                    val y1 = coords[1] + pointY
                    window?.update(x1, y1, width, height)
                } else {
                    show()
                }

                if (isDragging) {
                    if (coords == null) {
                        coords = tempCoords
                        surface.getLocationInWindow(coords)
                    }
                    if (coords[0] != lastParentX || coords[1] != lastParentY) {
                        touchToWindowOffsetX += (coords[0] - lastParentX).toFloat()
                        touchToWindowOffsetY += (coords[1] - lastParentY).toFloat()
                        lastParentX = coords[0]
                        lastParentY = coords[1]
                    }
                }
            } else {
                hide()
            }
        }

        fun changeOrientation(newOrientation: Int) {
            if (orientation != newOrientation) {
                setOrientation(newOrientation)
            }
        }

        private fun checkChangedOrientation(posX: Int, force: Boolean) {
            if (!isDragging && !force) return
            val millis = SystemClock.currentThreadTimeMillis()
            if (millis - lastTime < 50 && !force) return
            lastTime = millis

            val left = surface.left
            val right = surface.width
            val top = surface.top
            val bottom = surface.height

            val rect = tempRect ?: Rect().also { tempRect = it }
            rect.set(left, top, right, bottom)
            if (!surface.getGlobalVisibleRect(rect)) return

            if (posX - handleWidth < rect.left) {
                changeOrientation(HANDLE_RIGHT)
            } else if (posX + handleWidth > rect.right) {
                changeOrientation(HANDLE_LEFT)
            } else {
                changeOrientation(initialOrientation)
            }
        }

        private fun isPositionVisible(): Boolean {
            if (isDragging) return true
            val left = 0
            val right = surface.width
            val top = 0
            val bottom = surface.height

            val rect = tempRect ?: Rect().also { tempRect = it }
            rect.set(left + surface.paddingLeft, top + surface.paddingTop, right, bottom)

            val coords = tempCoords
            surface.getLocationInWindow(coords)
            val posX = coords[0] + pointX + hotspotX.toInt()
            val posY = coords[1] + pointY

            // Convert window-local bounds
            val winLeft = coords[0]
            val winTop = coords[1]
            val winRight = coords[0] + right
            val winBottom = coords[1] + bottom

            return posX in winLeft..winRight && posY in winTop..winBottom
        }

        override fun onDraw(c: Canvas) {
            val width = handleDrawable.intrinsicWidth
            val height = handleDrawable.intrinsicHeight
            handleDrawable.setBounds(0, 0, width, height)
            handleDrawable.draw(c)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(handleDrawable.intrinsicWidth, handleDrawable.intrinsicHeight)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            surface.getLocationOnScreen(tempCoords)
            val parentScreenX = tempCoords[0]
            val parentScreenY = tempCoords[1]
            val rawX = event.rawX
            val rawY = event.rawY

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchToWindowOffsetX = rawX - (parentScreenX + pointX)
                    touchToWindowOffsetY = rawY - (parentScreenY + pointY)
                    lastParentX = parentScreenX
                    lastParentY = parentScreenY
                    isDragging = true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newPosX = rawX - touchToWindowOffsetX - parentScreenX
                    val newPosY = rawY - touchToWindowOffsetY - parentScreenY + touchOffsetY

                    controller.actionMode?.hide(-1)
                    controller.updatePosition(
                        this,
                        (newPosX + hotspotX).roundToInt(),
                        newPosY.roundToInt()
                    )
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    controller.actionMode?.hide(0)
                    controller.actionMode?.invalidateContentRect()
                }
            }
            return true
        }

        val isShowing: Boolean
            get() = window?.isShowing == true
    }

    companion object {
        init {
            try {
                System.loadLibrary("termuxx-core")
            } catch (_: UnsatisfiedLinkError) {
                System.loadLibrary("termux-core")
            }
        }

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
        @JvmStatic external fun nativeStopTextSelection(): Boolean
        @JvmStatic external fun nativeIsSelectingText(): Boolean
        @JvmStatic external fun nativeSelectAll(scrollOffset: Int)
        @JvmStatic external fun nativeGetSelectionSelectors(outSelectors: IntArray)
        @JvmStatic external fun nativeGetSelectedText(): String
    }
}

// ========================================================================
// Minimal Keep-Alive Foreground Service (WakeLock + WifiLock)
// ========================================================================
class KeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        val chId = "termuxx_session_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val ch = NotificationChannel(chId, "Termuxx Core", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }

        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TermuxActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, chId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        nb.setContentTitle("Termuxx (Kotlin + C++23)")
            .setContentText("Active terminal sessions running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)

        try {
            startForeground(1, nb.build())
        } catch (_: Throwable) {
        }

        try {
            val pm = getSystemService(POWER_SERVICE) as? PowerManager
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Termuxx::CoreLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            if (wm != null) {
                @Suppress("DEPRECATION")
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(mode, "Termuxx::WifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
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
