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
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.IBinder
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
import com.termuxx.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Lightweight Foreground Service so Android App Info shows notification permission
 * and ColorOS 17 / Android 17 keeps active terminal sessions alive in background.
 */
class KeepAliveService : Service() {
    companion object {
        private const val CHANNEL_ID = "termuxx_session_channel"
        private const val NOTIFICATION_ID = 1337
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val launchIntent = Intent(this, TermuxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Termux C++23")
            .setContentText("Терминальная сессия активна")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        try {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (_: Throwable) {
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Termux Active Sessions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps C++23 PTY shell sessions running in background"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }
}

/**
 * TermuxX Modern Edition — Pure Kotlin + C++23 Single-Activity Architecture
 * Targeted for Android 17 "CinnamonBun" (API 37) / ColorOS 17 (aarch64-v8a).
 */
class TermuxActivity : Activity() {

    companion object {
        init {
            System.loadLibrary("termuxx-core")
        }

        private const val PREFS_NAME = "termuxx_modern_prefs"
        private const val PREF_EXTRA_KEYS = "extra_keys_csv"
        private const val PREF_FONT_SIZE = "font_size_sp"
        private const val PREF_CURSOR_SHAPE = "cursor_shape"
        private const val DEFAULT_EXTRA_KEYS = "ESC,TAB,CTRL,ALT,-,/,|,~,UP,DOWN,LEFT,RIGHT"
        private const val REQ_POST_NOTIFICATIONS = 1001

        // Pure Dark AMOLED Palette + ColorOS 17 Accents
        private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
        private const val COLOR_HEADER_BG = 0xFF0E131F.toInt()
        private const val COLOR_SURFACE_PILL = 0xFF1A2234.toInt()
        private const val COLOR_ACCENT_PRIMARY = 0xFF6366F1.toInt()
        private const val COLOR_ACCENT_ACTIVE = 0xFF38BDF8.toInt()
        private const val COLOR_ACCENT_ROOT = 0xFFE06C75.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFE5E9F0.toInt()
        private const val COLOR_TEXT_MUTED = 0xFF94A3B8.toInt()

        // Cell style bit flags (synced with termux_core.cpp)
        private const val FLAG_BOLD = 1 shl 0
        private const val FLAG_ITALIC = 1 shl 2
        private const val FLAG_UNDERLINE = 1 shl 3
        private const val FLAG_STRIKETHROUGH = 1 shl 7
        private const val FLAG_UNDERCURL = 1 shl 10
        private const val FLAG_DOUBLE_UNDERLINE = 1 shl 11

        // Mode mask bits (synced with termux_core.cpp)
        private const val MODE_ALT_SCREEN = 1 shl 2
        private const val MODE_MOUSE_TRACKING = 1 shl 3

        // Primary JNI Bridge to C++23 Core Engine (libtermuxx-core.so)
        @JvmStatic external fun nativeRegisterSurface(surface: Any?)
        @JvmStatic external fun nativeInitAndPollBootstrap(nativeLibDir: String): Int
        @JvmStatic external fun nativeGetBootstrapError(): String
        @JvmStatic external fun nativeNewSession(asRoot: Boolean, rows: Int, cols: Int, cellW: Int, cellH: Int): Int
        @JvmStatic external fun nativeSwitchSession(index: Int)
        @JvmStatic external fun nativeCloseSession(index: Int): Int
        @JvmStatic external fun nativeDestroyAll()
        @JvmStatic external fun nativeGetSessionCount(): Int
        @JvmStatic external fun nativeGetActiveIndex(): Int
        @JvmStatic external fun nativeIsSessionRoot(index: Int): Boolean
        @JvmStatic external fun nativeResizeActive(rows: Int, cols: Int, cellW: Int, cellH: Int)
        @JvmStatic external fun nativeConsumeFrameDirty(): Boolean
        @JvmStatic external fun nativeConsumeBell(): Boolean
        @JvmStatic external fun nativeConsumeOsc52Clipboard(): String?
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
    }

    private lateinit var rootLayout: LinearLayout
    private lateinit var headerBar: LinearLayout
    private lateinit var tabContainer: LinearLayout
    private lateinit var terminalSurface: TerminalSurface
    private lateinit var extraKeysBar: LinearLayout

    private var ctrlBtnRef: TextView? = null
    private var altBtnRef: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch non-blocking C++23 background bootstrap check/extraction immediately
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialSp = prefs.getFloat(PREF_FONT_SIZE, 13.5f).coerceIn(7.0f, 30.0f)
        val initialCursorShape = prefs.getInt(PREF_CURSOR_SHAPE, 0).coerceIn(0, 2)
        nativeSetDefaultCursorShape(initialCursorShape)

        // Root vertical layout
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Top Session Tab & Action Header Bar (44dp height, always pinned below status bar)
        headerBar = buildHeaderBar()
        rootLayout.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
        )

        // 1px subtle separator below the header bar
        val divider = View(this).apply {
            setBackgroundColor(0xFF263147.toInt())
        }
        rootLayout.addView(
            divider,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, max(1, dp(1)))
        )

        // 2. Main Terminal Viewport
        val viewport = FrameLayout(this).apply {
            setBackgroundColor(COLOR_AMOLED_BG)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }
        terminalSurface = TerminalSurface(this, initialSp)
        viewport.addView(
            terminalSurface,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout.addView(viewport)

        // 3. Bottom Extra Keys Bar (44dp height)
        extraKeysBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(COLOR_HEADER_BG)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(5), dp(6), dp(5))
        }
        val extraScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(COLOR_HEADER_BG)
            addView(extraKeysBar)
        }
        rootLayout.addView(
            extraScroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
        )

        setContentView(rootLayout)

        // Apply system bars & IME insets cleanly so headerBar sits right below status bar
        window.decorView.setOnApplyWindowInsetsListener { _, insets ->
            val sysBars = insets.getInsets(WindowInsets.Type.systemBars())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            val bottomInset = max(sysBars.bottom, ime.bottom)
            rootLayout.setPadding(sysBars.left, sysBars.top, sysBars.right, bottomInset)
            insets
        }

        rebuildExtraKeys()
        rebuildTabs()
        nativeRegisterSurface(terminalSurface)
        terminalSurface.requestFocus()

        // Request notification permission non-blockingly after UI is attached
        rootLayout.postDelayed({
            ensureNotificationsAndService()
        }, 400L)
    }

    private fun ensureNotificationsAndService() {
        try {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIFICATIONS)
            } else {
                startKeepAliveService()
            }
        } catch (_: Throwable) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            startKeepAliveService()
            terminalSurface.postInvalidateOnAnimation()
        }
    }

    private fun startKeepAliveService() {
        try {
            startForegroundService(Intent(this, KeepAliveService::class.java))
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        nativeRegisterSurface(terminalSurface)
        terminalSurface.requestFocus()
        terminalSurface.postInvalidateOnAnimation()
    }

    override fun onDestroy() {
        nativeRegisterSurface(null)
        if (isFinishing) {
            try {
                stopService(Intent(this, KeepAliveService::class.java))
            } catch (_: Throwable) {
            }
        }
        super.onDestroy()
    }

    private fun buildHeaderBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(COLOR_HEADER_BG)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))

            tabContainer = LinearLayout(this@TermuxActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val tabScroll = HorizontalScrollView(this@TermuxActivity).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                addView(tabContainer)
            }
            addView(tabScroll)

            // [+] New PTY Session Button (Tap = User shell, Long-Press = Root su session)
            addView(makeHeaderButton(
                symbol = "+",
                onClick = { spawnNewSession(asRoot = false) },
                onLongClick = { spawnNewSession(asRoot = true) }
            ))

            // [#] Copy All / Select Transcript Button
            addView(makeHeaderButton("#", onClick = {
                terminalSurface.showTranscriptCopyDialog()
            }))

            // [⚙] Customize Extra Keys & Cursor Shape Button
            addView(makeHeaderButton("⚙", onClick = {
                showCustomizeExtraKeysDialog()
            }))

            // [✕] Close Current Session Button
            addView(makeHeaderButton("✕", onClick = {
                if (nativeGetSessionCount() > 1) {
                    terminalSurface.stopNativeSelection()
                    nativeCloseSession(nativeGetActiveIndex())
                    terminalSurface.resetScrollOffset()
                    rebuildTabs()
                    terminalSurface.invalidateFullGrid()
                } else {
                    nativeDestroyAll()
                    finish()
                }
            }))
        }
    }

    private fun spawnNewSession(asRoot: Boolean) {
        val bootState = nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
        if (bootState == 2 && nativeGetSessionCount() < 8) {
            terminalSurface.stopNativeSelection()
            nativeNewSession(
                asRoot,
                terminalSurface.gridRows,
                terminalSurface.gridCols,
                terminalSurface.charWidthPx.roundToInt(),
                terminalSurface.charHeightPx
            )
            terminalSurface.resetScrollOffset()
            rebuildTabs()
            terminalSurface.invalidateFullGrid()
        }
    }

    private fun makeHeaderButton(
        symbol: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ): TextView {
        return TextView(this).apply {
            text = symbol
            setTextColor(COLOR_TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = pillDrawable(COLOR_SURFACE_PILL, dp(8).toFloat())
            val lp = LinearLayout.LayoutParams(dp(36), dp(32)).apply {
                marginStart = dp(5)
            }
            layoutParams = lp
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            if (onLongClick != null) {
                setOnLongClickListener {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                    true
                }
            }
        }
    }

    fun rebuildTabs() {
        tabContainer.removeAllViews()
        val count = max(1, nativeGetSessionCount())
        val active = nativeGetActiveIndex().coerceAtLeast(0)
        for (i in 0 until count) {
            val isSelected = (i == active)
            val isRoot = nativeGetSessionCount() > i && nativeIsSessionRoot(i)
            val suffix = if (isRoot) "su" else "sh"
            val activeBg = if (isRoot) COLOR_ACCENT_ROOT else COLOR_ACCENT_PRIMARY
            val chip = TextView(this).apply {
                text = "${i + 1}·$suffix"
                setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else COLOR_TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(4), dp(12), dp(4))
                background = pillDrawable(
                    if (isSelected) activeBg else COLOR_SURFACE_PILL,
                    dp(8).toFloat()
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(30)
                ).apply {
                    marginEnd = dp(6)
                }
                setOnClickListener {
                    if (nativeGetSessionCount() > i) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        terminalSurface.stopNativeSelection()
                        nativeSwitchSession(i)
                        terminalSurface.resetScrollOffset()
                        rebuildTabs()
                        terminalSurface.invalidateFullGrid()
                    }
                }
            }
            tabContainer.addView(chip)
        }
    }

    private fun rebuildExtraKeys() {
        extraKeysBar.removeAllViews()
        ctrlBtnRef = null
        altBtnRef = null

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawCsv = prefs.getString(PREF_EXTRA_KEYS, DEFAULT_EXTRA_KEYS) ?: DEFAULT_EXTRA_KEYS
        val tokens = rawCsv.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }

        for (token in tokens) {
            val label = when (token) {
                "UP" -> "▲"
                "DOWN" -> "▼"
                "LEFT" -> "◀"
                "RIGHT" -> "▶"
                "PGUP" -> "PgUp"
                "PGDN" -> "PgDn"
                else -> token
            }

            val btn = TextView(this).apply {
                text = label
                setTextColor(COLOR_TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(12), 0)
                background = pillDrawable(COLOR_SURFACE_PILL, dp(7).toFloat())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(32)
                ).apply {
                    marginEnd = dp(5)
                }
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    terminalSurface.resetScrollOffset()
                    nativeDispatchCustomKey(token)
                    syncModifierPillsFromNative()
                    terminalSurface.postInvalidateOnAnimation()
                }
            }

            if (token == "CTRL") ctrlBtnRef = btn
            if (token == "ALT") altBtnRef = btn
            extraKeysBar.addView(btn)
        }
        syncModifierPillsFromNative()
    }

    fun syncModifierPillsFromNative() {
        val mask = nativeGetModifierMask()
        val ctrlArmed = (mask and 1) != 0
        val altArmed = (mask and 2) != 0

        ctrlBtnRef?.background = pillDrawable(
            if (ctrlArmed) COLOR_ACCENT_ACTIVE else COLOR_SURFACE_PILL,
            dp(7).toFloat()
        )
        ctrlBtnRef?.setTextColor(if (ctrlArmed) 0xFF000000.toInt() else COLOR_TEXT_PRIMARY)

        altBtnRef?.background = pillDrawable(
            if (altArmed) COLOR_ACCENT_ACTIVE else COLOR_SURFACE_PILL,
            dp(7).toFloat()
        )
        altBtnRef?.setTextColor(if (altArmed) 0xFF000000.toInt() else COLOR_TEXT_PRIMARY)
    }

    private fun showCustomizeExtraKeysDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getString(PREF_EXTRA_KEYS, DEFAULT_EXTRA_KEYS) ?: DEFAULT_EXTRA_KEYS
        val edit = EditText(this).apply {
            setText(current)
            setTextColor(COLOR_TEXT_PRIMARY)
            setHintTextColor(COLOR_TEXT_MUTED)
            typeface = Typeface.MONOSPACE
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Настройка панели клавиш")
            .setMessage("Укажите клавиши через запятую (ESC, TAB, CTRL, ALT, UP, DOWN, LEFT, RIGHT, HOME, END, PGUP, PGDN, BKSP, DEL, -, /, |, ~):")
            .setView(edit)
            .setPositiveButton("Сохранить") { _, _ ->
                prefs.edit().putString(PREF_EXTRA_KEYS, edit.text.toString()).apply()
                rebuildExtraKeys()
            }
            .setNeutralButton("Форма курсора") { _, _ ->
                val nextShape = (prefs.getInt(PREF_CURSOR_SHAPE, 0) + 1) % 3
                prefs.edit().putInt(PREF_CURSOR_SHAPE, nextShape).apply()
                nativeSetDefaultCursorShape(nextShape)
                terminalSurface.invalidateFullGrid()
            }
            .setNegativeButton("Сброс") { _, _ ->
                prefs.edit().putString(PREF_EXTRA_KEYS, DEFAULT_EXTRA_KEYS).apply()
                rebuildExtraKeys()
            }
            .show()
    }

    private fun pillDrawable(color: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(color)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }

    /**
     * Hardware-Accelerated C++23 Terminal View with JetBrains Mono Grid Rendering,
     * Procedural Box-Drawing, ColorOS 17 Lollipop Handles & FLOATING ActionMode.
     */
    inner class TerminalSurface(context: Context, initialSp: Float) : View(context) {

        var fontSizeSp: Float = initialSp
            private set

        var charWidthPx: Float = 18f
            private set
        var charHeightPx: Int = 36
            private set
        var charBaselinePx: Int = 28
            private set

        var gridRows: Int = 24
            private set
        var gridCols: Int = 80
            private set

        var scrollOffset: Int = 0
            private set

        private val tfRegular: Typeface
        private val tfBold: Typeface
        private val tfItalic: Typeface
        private val tfBoldItalic: Typeface

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val wavePath = Path()

        // Pre-allocated JNI transfer buffers for nativePackRowRender (zero GC allocations in onDraw)
        private val maxGridRows = 250
        private val maxGridCols = 400
        private val bgRunsBuf = IntArray(maxGridCols * 3)
        private val glyphsBuf = IntArray(maxGridCols * 4 * 2)
        private val charBuf = CharArray(2)
        private val selGeomBuf = FloatArray(14)

        private var sessionSpawned = false
        private var activeActionMode: ActionMode? = null
        private val selectionContentRect = Rect()
        private var draggingSelectionHandle = false
        private var scrollAccumulatorPx = 0f
        private var pinchStartSp = fontSizeSp

        private val scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    pinchStartSp = fontSizeSp
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val targetSp = (pinchStartSp * detector.scaleFactor).coerceIn(7.0f, 30.0f)
                    if (abs(targetSp - fontSizeSp) >= 0.25f) {
                        updateFontMetricsAndReflow(targetSp)
                    }
                    return true
                }
            }
        )

        private val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    scrollAccumulatorPx = 0f
                    return true
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (nativeIsSelectingText()) {
                        stopNativeSelection()
                        return true
                    }
                    val modes = nativeGetModesMask()
                    if ((modes and MODE_MOUSE_TRACKING) != 0) {
                        val col1 = ((e.x / charWidthPx).toInt() + 1).coerceIn(1, gridCols)
                        val row1 = ((e.y / charHeightPx).toInt() + 1).coerceIn(1, gridRows)
                        nativeSendMouse(0, col1, row1, true)
                        nativeSendMouse(0, col1, row1, false)
                    }
                    requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(this@TerminalSurface, 0)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (scaleDetector.isInProgress || draggingSelectionHandle) return false
                    if (nativeIsSelectingText()) {
                        val r = (e2.y / charHeightPx).toInt().coerceIn(0, gridRows - 1)
                        val c = (e2.x / charWidthPx).toInt().coerceIn(0, gridCols - 1)
                        nativeUpdateSelectionHandle(false, c, r - scrollOffset)
                        activeActionMode?.invalidateContentRect()
                        postInvalidateOnAnimation()
                        return true
                    }
                    val modes = nativeGetModesMask()
                    if ((modes and MODE_ALT_SCREEN) == 0) {
                        scrollAccumulatorPx += distanceY
                        val deltaRows = (scrollAccumulatorPx / charHeightPx).toInt()
                        if (deltaRows != 0) {
                            scrollAccumulatorPx -= deltaRows * charHeightPx
                            val maxSb = nativeGetActiveScrollback()
                            scrollOffset = (scrollOffset - deltaRows).coerceIn(0, maxSb)
                            postInvalidateOnAnimation()
                        }
                    } else if ((modes and MODE_MOUSE_TRACKING) != 0 && abs(distanceY) >= charHeightPx * 0.5f) {
                        val col1 = ((e2.x / charWidthPx).toInt() + 1).coerceIn(1, gridCols)
                        val row1 = ((e2.y / charHeightPx).toInt() + 1).coerceIn(1, gridRows)
                        val wheelBtn = if (distanceY > 0) 65 else 64
                        nativeSendMouse(wheelBtn, col1, row1, true)
                    }
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (scaleDetector.isInProgress) return
                    if (nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir) != 2) return
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    val r = (e.y / charHeightPx).toInt().coerceIn(0, gridRows - 1)
                    val c = (e.x / charWidthPx).toInt().coerceIn(0, gridCols - 1)
                    startNativeSelection(r, c)
                }
            }
        )

        init {
            val baseTf = try {
                Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
            } catch (_: Throwable) {
                Typeface.MONOSPACE
            }
            tfRegular = baseTf
            tfBold = Typeface.create(baseTf, Typeface.BOLD)
            tfItalic = Typeface.create(baseTf, Typeface.ITALIC)
            tfBoldItalic = Typeface.create(baseTf, Typeface.BOLD_ITALIC)
            textPaint.typeface = tfRegular

            isFocusable = true
            isFocusableInTouchMode = true
            defaultFocusHighlightEnabled = false
            recomputeFontMetrics()
        }

        fun resetScrollOffset() {
            scrollOffset = 0
        }

        private fun recomputeFontMetrics() {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                fontSizeSp,
                resources.displayMetrics
            )
            textPaint.typeface = tfRegular
            textPaint.textSize = px
            charWidthPx = max(6f, textPaint.measureText("M"))
            val fm = textPaint.fontMetrics
            charHeightPx = max(12, (fm.descent - fm.ascent + fm.leading + 2f).roundToInt())
            charBaselinePx = (-fm.ascent).roundToInt()
        }

        private fun updateFontMetricsAndReflow(newSp: Float) {
            fontSizeSp = newSp
            recomputeFontMetrics()
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putFloat(PREF_FONT_SIZE, fontSizeSp)
                .apply()

            if (width > 0 && height > 0) {
                val newCols = (width / charWidthPx).toInt().coerceIn(20, maxGridCols)
                val newRows = (height / charHeightPx).coerceIn(5, maxGridRows)
                gridCols = newCols
                gridRows = newRows
                nativeResizeActive(gridRows, gridCols, charWidthPx.roundToInt(), charHeightPx)
                scrollOffset = scrollOffset.coerceIn(0, nativeGetActiveScrollback())
            }
            invalidateFullGrid()
        }

        fun invalidateFullGrid() {
            postInvalidateOnAnimation()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            val newCols = (w / charWidthPx).toInt().coerceIn(20, maxGridCols)
            val newRows = (h / charHeightPx).coerceIn(5, maxGridRows)
            gridCols = newCols
            gridRows = newRows

            if (nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir) == 2) {
                if (!sessionSpawned && nativeGetSessionCount() == 0) {
                    sessionSpawned = true
                    nativeNewSession(false, gridRows, gridCols, charWidthPx.roundToInt(), charHeightPx)
                    post { rebuildTabs() }
                } else {
                    nativeResizeActive(gridRows, gridCols, charWidthPx.roundToInt(), charHeightPx)
                }
            }
            postInvalidateOnAnimation()
        }

        override fun onDraw(canvas: Canvas) {
            bgPaint.color = COLOR_AMOLED_BG
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val bootState = nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)
            if (bootState != 2) {
                textPaint.typeface = tfRegular
                textPaint.color = COLOR_ACCENT_ACTIVE
                val msg = if (bootState == 3) {
                    "Ошибка bootstrap: ${nativeGetBootstrapError()}"
                } else {
                    "⚡ Инициализация среды Termux C++23..."
                }
                canvas.drawText(msg, dp(16).toFloat(), dp(48).toFloat(), textPaint)
                if (bootState != 3) {
                    postInvalidateDelayed(40L)
                }
                return
            }

            if (!sessionSpawned && nativeGetSessionCount() == 0 && width > 0 && height > 0) {
                sessionSpawned = true
                nativeNewSession(false, gridRows, gridCols, charWidthPx.roundToInt(), charHeightPx)
                post { rebuildTabs() }
            }

            nativeConsumeFrameDirty()

            if (nativeConsumeBell()) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            val oscClip = nativeConsumeOsc52Clipboard()
            if (!oscClip.isNullOrEmpty()) {
                copyToClipboard(oscClip, showToast = false)
            }

            val maxSb = nativeGetActiveScrollback()
            if (scrollOffset > maxSb) scrollOffset = maxSb

            val cellW = charWidthPx
            val cellH = charHeightPx.toFloat()
            val strokeW = max(1.5f, cellH * 0.06f)
            decorPaint.strokeWidth = strokeW

            for (r in 0 until gridRows) {
                val topY = r * cellH
                val bottomY = topY + cellH
                val baseY = topY + charBaselinePx

                val packedCounts = nativePackRowRender(scrollOffset, r, bgRunsBuf, glyphsBuf)
                val bgCount = (packedCounts ushr 16) and 0xFFFF
                val glCount = packedCounts and 0xFFFF

                // 1. Render background color runs (including selection highlights)
                for (i in 0 until bgCount) {
                    val base = i * 3
                    val startCol = bgRunsBuf[base]
                    val endCol = bgRunsBuf[base + 1]
                    bgPaint.color = bgRunsBuf[base + 2]
                    canvas.drawRect(startCol * cellW, topY, endCol * cellW, bottomY, bgPaint)
                }

                // 2. Render procedural box-drawing rects & grid-aligned glyphs
                for (i in 0 until glCount) {
                    val base = i * 4
                    val cp = glyphsBuf[base]
                    val col = glyphsBuf[base + 1]
                    val fg = glyphsBuf[base + 2]
                    val meta = glyphsBuf[base + 3]
                    val cellLeft = col * cellW

                    if (cp == -1) {
                        // Procedural 1/16th cell box-drawing or block element from C++23
                        val x0 = (meta ushr 24) and 0xFF
                        val y0 = (meta ushr 16) and 0xFF
                        val x1 = (meta ushr 8) and 0xFF
                        val y1 = meta and 0xFF
                        bgPaint.color = fg
                        canvas.drawRect(
                            cellLeft + (x0 / 16f) * cellW,
                            topY + (y0 / 16f) * cellH,
                            cellLeft + (x1 / 16f) * cellW,
                            topY + (y1 / 16f) * cellH,
                            bgPaint
                        )
                        continue
                    }

                    val isBold = (meta and FLAG_BOLD) != 0
                    val isItalic = (meta and FLAG_ITALIC) != 0
                    textPaint.typeface = when {
                        isBold && isItalic -> tfBoldItalic
                        isBold -> tfBold
                        isItalic -> tfItalic
                        else -> tfRegular
                    }
                    textPaint.color = fg

                    val charCount = Character.toChars(cp, charBuf, 0)
                    canvas.drawText(charBuf, 0, charCount, cellLeft, baseY, textPaint)

                    // Text decorations (underline, double underline, undercurl, strikethrough)
                    if ((meta and (FLAG_UNDERLINE or FLAG_DOUBLE_UNDERLINE or FLAG_UNDERCURL or FLAG_STRIKETHROUGH)) != 0) {
                        decorPaint.color = fg
                        val rightX = cellLeft + cellW
                        if ((meta and FLAG_UNDERLINE) != 0) {
                            val uy = bottomY - strokeW * 1.5f
                            canvas.drawLine(cellLeft, uy, rightX, uy, decorPaint)
                        }
                        if ((meta and FLAG_DOUBLE_UNDERLINE) != 0) {
                            val uy1 = bottomY - strokeW * 2.8f
                            val uy2 = bottomY - strokeW * 0.8f
                            canvas.drawLine(cellLeft, uy1, rightX, uy1, decorPaint)
                            canvas.drawLine(cellLeft, uy2, rightX, uy2, decorPaint)
                        }
                        if ((meta and FLAG_UNDERCURL) != 0) {
                            val uy = bottomY - strokeW * 1.8f
                            val amp = strokeW * 1.1f
                            wavePath.reset()
                            wavePath.moveTo(cellLeft, uy)
                            wavePath.quadTo(cellLeft + cellW * 0.25f, uy - amp, cellLeft + cellW * 0.5f, uy)
                            wavePath.quadTo(cellLeft + cellW * 0.75f, uy + amp, rightX, uy)
                            canvas.drawPath(wavePath, decorPaint)
                        }
                        if ((meta and FLAG_STRIKETHROUGH) != 0) {
                            val sy = topY + cellH * 0.52f
                            canvas.drawLine(cellLeft, sy, rightX, sy, decorPaint)
                        }
                    }
                }
            }

            nativeClearDirtyFlags()

            // 3. Render active cursor (Block = 0, Underline = 1, Bar/Beam = 2)
            val cursorState = nativeGetCursorState()
            if (cursorState >= 0 && !nativeIsSelectingText() && scrollOffset == 0) {
                val shape = (cursorState ushr 29) and 3
                val curRow = (cursorState ushr 16) and 0x1FFF
                val curCol = cursorState and 0xFFFF
                if (curRow in 0 until gridRows && curCol in 0 until gridCols) {
                    val cx = curCol * cellW
                    val cy = curRow * cellH
                    bgPaint.color = COLOR_ACCENT_ACTIVE
                    when (shape) {
                        1 -> canvas.drawRect(cx, cy + cellH - max(3f, cellH * 0.1f), cx + cellW, cy + cellH, bgPaint)
                        2 -> canvas.drawRect(cx, cy, cx + max(3f, cellW * 0.14f), cy + cellH, bgPaint)
                        else -> {
                            bgPaint.alpha = 180
                            canvas.drawRect(cx, cy, cx + cellW, cy + cellH, bgPaint)
                            bgPaint.alpha = 255
                        }
                    }
                }
            }

            // 4. Render ColorOS 17 Lollipop Selection Handles
            val handleRadius = dp(7).toFloat()
            if (nativeGetSelectionGeometryPx(
                    scrollOffset,
                    cellW,
                    cellH,
                    width.toFloat(),
                    height.toFloat(),
                    handleRadius,
                    selGeomBuf
                )
            ) {
                bgPaint.color = COLOR_ACCENT_ACTIVE
                decorPaint.color = COLOR_ACCENT_ACTIVE
                decorPaint.strokeWidth = max(2f, dp(2).toFloat())
                // Start handle stem & circle
                canvas.drawLine(selGeomBuf[0], selGeomBuf[1], selGeomBuf[0], selGeomBuf[2], decorPaint)
                canvas.drawCircle(selGeomBuf[3], selGeomBuf[4], handleRadius, bgPaint)
                // End handle stem & circle
                canvas.drawLine(selGeomBuf[5], selGeomBuf[6], selGeomBuf[5], selGeomBuf[7], decorPaint)
                canvas.drawCircle(selGeomBuf[8], selGeomBuf[9], handleRadius, bgPaint)

                selectionContentRect.set(
                    selGeomBuf[10].toInt(),
                    selGeomBuf[11].toInt(),
                    selGeomBuf[12].toInt(),
                    selGeomBuf[13].toInt()
                )
            }
        }

        private fun startNativeSelection(row: Int, col: Int) {
            nativeStartTextSelection(scrollOffset, col, row)
            val handleRadius = dp(7).toFloat()
            if (nativeGetSelectionGeometryPx(
                    scrollOffset,
                    charWidthPx,
                    charHeightPx.toFloat(),
                    width.toFloat(),
                    height.toFloat(),
                    handleRadius,
                    selGeomBuf
                )
            ) {
                selectionContentRect.set(
                    selGeomBuf[10].toInt(),
                    selGeomBuf[11].toInt(),
                    selGeomBuf[12].toInt(),
                    selGeomBuf[13].toInt()
                )
            }

            activeActionMode?.finish()
            activeActionMode = startActionMode(object : ActionMode.Callback2() {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    menu.add(0, 1, 0, "Копировать").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    menu.add(0, 2, 1, "Выделить всё").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    menu.add(0, 3, 2, "Вставить").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    when (item.itemId) {
                        1 -> {
                            val selected = nativeGetSelectedText()
                            copyToClipboard(selected, showToast = true)
                            stopNativeSelection()
                            return true
                        }
                        2 -> {
                            nativeSelectAll(scrollOffset)
                            mode.invalidateContentRect()
                            postInvalidateOnAnimation()
                            return true
                        }
                        3 -> {
                            pasteFromClipboard()
                            stopNativeSelection()
                            return true
                        }
                    }
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    nativeStopTextSelection()
                    activeActionMode = null
                    postInvalidateOnAnimation()
                }

                override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                    outRect.set(selectionContentRect)
                }
            }, ActionMode.TYPE_FLOATING)

            postInvalidateOnAnimation()
        }

        fun stopNativeSelection() {
            draggingSelectionHandle = false
            nativeStopTextSelection()
            activeActionMode?.finish()
            activeActionMode = null
            postInvalidateOnAnimation()
        }

        private fun copyToClipboard(text: String, showToast: Boolean = true) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("TermuxX", text))
            if (showToast) {
                Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
            }
        }

        private fun pasteFromClipboard() {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = cm?.primaryClip?.getItemAt(0)
            val text = item?.coerceToText(context)?.toString()
            if (!text.isNullOrEmpty()) {
                resetScrollOffset()
                nativeSendText(text, true)
                syncModifierPillsFromNative()
                postInvalidateOnAnimation()
            }
        }

        fun showTranscriptCopyDialog() {
            val fullText = nativeGetTranscriptText()
            val tv = TextView(context).apply {
                text = fullText.ifEmpty { "Буфер терминала пуст." }
                setTextIsSelectable(true)
                typeface = tfRegular
                setTextColor(COLOR_TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            val scroll = ScrollView(context).apply {
                addView(tv)
            }
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Выделение и копирование истории")
                .setView(scroll)
                .setPositiveButton("Копировать всё") { _, _ ->
                    copyToClipboard(fullText, showToast = true)
                }
                .setNeutralButton("Вставить в терминал") { _, _ ->
                    pasteFromClipboard()
                }
                .setNegativeButton("Закрыть", null)
                .show()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (nativeIsSelectingText() && event.pointerCount == 1) {
                val res = nativeOnSelectionTouchPx(
                    event.actionMasked,
                    scrollOffset,
                    event.x,
                    event.y,
                    charWidthPx,
                    charHeightPx.toFloat(),
                    width.toFloat(),
                    height.toFloat(),
                    dp(7).toFloat()
                )
                if (res == 1) {
                    draggingSelectionHandle = true
                    activeActionMode?.invalidateContentRect()
                    postInvalidateOnAnimation()
                    return true
                } else if (res == 2) {
                    draggingSelectionHandle = false
                    activeActionMode?.invalidateContentRect()
                    postInvalidateOnAnimation()
                    return true
                }
            }
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
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
                    resetScrollOffset()
                    if (lastComposing.isNotEmpty()) {
                        applyComposingDiff(committed)
                        lastComposing = ""
                    } else if (committed.isNotEmpty()) {
                        nativeSendText(committed, false)
                    }
                    syncModifierPillsFromNative()
                    postInvalidateOnAnimation()
                    editableBuf.clear()
                    return true
                }

                override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    val composing = text?.toString() ?: ""
                    resetScrollOffset()
                    applyComposingDiff(composing)
                    lastComposing = composing
                    syncModifierPillsFromNative()
                    postInvalidateOnAnimation()
                    return true
                }

                override fun finishComposingText(): Boolean {
                    lastComposing = ""
                    editableBuf.clear()
                    return true
                }

                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    resetScrollOffset()
                    if (lastComposing.isNotEmpty()) {
                        val drop = min(beforeLength, lastComposing.length)
                        lastComposing = lastComposing.dropLast(drop)
                    }
                    repeat(max(1, beforeLength)) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                    }
                    postInvalidateOnAnimation()
                    return true
                }

                override fun sendKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        handleHardwareKeyDown(event.keyCode, event)
                    }
                    return true
                }
            }
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            if (handleHardwareKeyDown(keyCode, event)) return true
            return super.onKeyDown(keyCode, event)
        }

        private fun handleHardwareKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (nativeIsSelectingText()) {
                    stopNativeSelection()
                    return true
                }
                return false
            }
            if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT ||
                keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT ||
                keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
            ) {
                return false
            }

            resetScrollOffset()
            val uni = event.getUnicodeChar(event.metaState and KeyEvent.META_CTRL_MASK.inv())
            val rc = nativeSendKeyEvent(keyCode, uni, event.metaState)
            syncModifierPillsFromNative()
            postInvalidateOnAnimation()
            return rc != 0
        }
    }
}
