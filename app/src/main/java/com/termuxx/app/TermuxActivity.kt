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
import android.graphics.Canvas
import android.graphics.Paint
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
            startForeground(NOTIFICATION_ID, notification)
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
 * Targeted exclusively for Android 17 "CinnamonBun" (API 37) / ColorOS 17 (aarch64-v8a).
 */
class TermuxActivity : Activity() {

    companion object {
        init {
            System.loadLibrary("termuxx-core")
        }

        private const val PREFS_NAME = "termuxx_modern_prefs"
        private const val PREF_EXTRA_KEYS = "extra_keys_csv"
        private const val PREF_FONT_SIZE = "font_size_sp"
        private const val DEFAULT_EXTRA_KEYS = "ESC,TAB,CTRL,ALT,-,/,|,UP,DOWN,LEFT,RIGHT"
        private const val REQ_POST_NOTIFICATIONS = 1001

        // Material 3 Expressive Dark AMOLED Palette
        private const val COLOR_AMOLED_BG = 0xFF000000.toInt()
        private const val COLOR_HEADER_BG = 0xFF0E131F.toInt()
        private const val COLOR_SURFACE_PILL = 0xFF1A2234.toInt()
        private const val COLOR_ACCENT_PRIMARY = 0xFF6366F1.toInt()
        private const val COLOR_ACCENT_ACTIVE = 0xFF38BDF8.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFE2E8F0.toInt()
        private const val COLOR_TEXT_MUTED = 0xFF94A3B8.toInt()
        private const val COLOR_SELECTION_BG = 0x6638BDF8

        // JNI Bridge to C++23 Core Engine (libtermuxx-core.so)
        @JvmStatic external fun nativeInitEngine(dataDir: String, nativeLibDir: String)
        @JvmStatic external fun nativeBindSurface(surface: Any?)
        @JvmStatic external fun nativeGetBootstrapState(): Int
        @JvmStatic external fun nativeGetBootstrapError(): String
        @JvmStatic external fun nativeCreateSession(rows: Int, cols: Int): Int
        @JvmStatic external fun nativeSwitchSession(index: Int)
        @JvmStatic external fun nativeCloseSession(index: Int): Int
        @JvmStatic external fun nativeGetSessionCount(): Int
        @JvmStatic external fun nativeGetActiveSession(): Int
        @JvmStatic external fun nativeResizePty(rows: Int, cols: Int)
        @JvmStatic external fun nativeSendText(text: String)
        @JvmStatic external fun nativeSendKey(keyCode: Int, ctrl: Boolean, alt: Boolean)
        @JvmStatic external fun nativePollOutput(): Boolean
        @JvmStatic external fun nativeFetchDirtyRows(
            packedAttrs: LongArray,
            rowTextOut: Array<String>,
            metaOut: IntArray
        ): Int
        @JvmStatic external fun nativeGetRowText(row: Int): String
        @JvmStatic external fun nativeGetTranscriptText(): String
        @JvmStatic external fun nativeGetSelectedText(r1: Int, c1: Int, r2: Int, c2: Int): String
        @JvmStatic external fun nativeScrollViewport(deltaLines: Int): Int
        @JvmStatic external fun nativeResetScroll()
        @JvmStatic external fun nativeGetAlternateScreen(): Boolean
        @JvmStatic external fun nativeSetCtrlMod(active: Boolean)
        @JvmStatic external fun nativeSetAltMod(active: Boolean)
        @JvmStatic external fun nativeGetCtrlMod(): Boolean
        @JvmStatic external fun nativeGetAltMod(): Boolean
    }

    private lateinit var rootLayout: LinearLayout
    private lateinit var headerBar: LinearLayout
    private lateinit var tabContainer: LinearLayout
    private lateinit var terminalSurface: TerminalSurface
    private lateinit var extraKeysBar: LinearLayout

    private var ctrlLatched = false
    private var altLatched = false
    private var ctrlBtnRef: TextView? = null
    private var altBtnRef: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch non-blocking C++23 background bootstrap check/extraction immediately
        nativeInitEngine(applicationInfo.dataDir, applicationInfo.nativeLibraryDir)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialSp = prefs.getFloat(PREF_FONT_SIZE, 13.5f).coerceIn(7.0f, 30.0f)

        // Root vertical layout (strictly clipped so children never overdraw the top tab bar)
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

        // 1px subtle separator below the header bar so the top bar stands out cleanly
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
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            val sysBars = insets.getInsets(WindowInsets.Type.systemBars())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            val bottomInset = max(sysBars.bottom, ime.bottom)
            rootLayout.setPadding(sysBars.left, sysBars.top, sysBars.right, bottomInset)
            insets
        }

        rebuildExtraKeys()
        rebuildTabs()
        nativeBindSurface(terminalSurface)
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
        nativeBindSurface(terminalSurface)
        terminalSurface.requestFocus()
        terminalSurface.postInvalidateOnAnimation()
    }

    override fun onDestroy() {
        nativeBindSurface(null)
        try {
            stopService(Intent(this, KeepAliveService::class.java))
        } catch (_: Throwable) {
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

            // [+] New PTY Session Button
            addView(makeHeaderButton("+") {
                if (nativeGetBootstrapState() == 2 && nativeGetSessionCount() < 8) {
                    nativeCreateSession(terminalSurface.gridRows, terminalSurface.gridCols)
                    rebuildTabs()
                    terminalSurface.invalidateFullGrid()
                }
            })

            // [#] Copy All / Select Transcript Button
            addView(makeHeaderButton("#") {
                terminalSurface.showTranscriptCopyDialog()
            })

            // [⚙] Customize Extra Keys Button
            addView(makeHeaderButton("⚙") {
                showCustomizeExtraKeysDialog()
            })

            // [✕] Close Current Session Button
            addView(makeHeaderButton("✕") {
                if (nativeGetSessionCount() > 1) {
                    nativeCloseSession(nativeGetActiveSession())
                    rebuildTabs()
                    terminalSurface.invalidateFullGrid()
                } else {
                    finish()
                }
            })
        }
    }

    private fun makeHeaderButton(symbol: String, onClick: () -> Unit): TextView {
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
        }
    }

    fun rebuildTabs() {
        tabContainer.removeAllViews()
        val count = max(1, nativeGetSessionCount())
        val active = nativeGetActiveSession()
        for (i in 0 until count) {
            val isSelected = (i == active)
            val chip = TextView(this).apply {
                text = "$ ${i + 1}"
                setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else COLOR_TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(4), dp(12), dp(4))
                background = pillDrawable(
                    if (isSelected) COLOR_ACCENT_PRIMARY else COLOR_SURFACE_PILL,
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
                        nativeSwitchSession(i)
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
                    handleExtraKeyTap(token)
                }
            }

            if (token == "CTRL") ctrlBtnRef = btn
            if (token == "ALT") altBtnRef = btn
            extraKeysBar.addView(btn)
        }
        updateModifierPills()
    }

    private fun updateModifierPills() {
        ctrlBtnRef?.background = pillDrawable(
            if (ctrlLatched) COLOR_ACCENT_ACTIVE else COLOR_SURFACE_PILL,
            dp(7).toFloat()
        )
        ctrlBtnRef?.setTextColor(if (ctrlLatched) 0xFF000000.toInt() else COLOR_TEXT_PRIMARY)

        altBtnRef?.background = pillDrawable(
            if (altLatched) COLOR_ACCENT_ACTIVE else COLOR_SURFACE_PILL,
            dp(7).toFloat()
        )
        altBtnRef?.setTextColor(if (altLatched) 0xFF000000.toInt() else COLOR_TEXT_PRIMARY)
    }

    fun consumeModifiers() {
        if (ctrlLatched || altLatched) {
            ctrlLatched = false
            altLatched = false
            nativeSetCtrlMod(false)
            nativeSetAltMod(false)
            updateModifierPills()
        }
    }

    private fun handleExtraKeyTap(token: String) {
        when (token) {
            "CTRL" -> {
                ctrlLatched = !ctrlLatched
                nativeSetCtrlMod(ctrlLatched)
                updateModifierPills()
            }
            "ALT" -> {
                altLatched = !altLatched
                nativeSetAltMod(altLatched)
                updateModifierPills()
            }
            "ESC" -> {
                nativeSendKey(KeyEvent.KEYCODE_ESCAPE, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "TAB" -> {
                nativeSendKey(KeyEvent.KEYCODE_TAB, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "UP" -> {
                nativeSendKey(KeyEvent.KEYCODE_DPAD_UP, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "DOWN" -> {
                nativeSendKey(KeyEvent.KEYCODE_DPAD_DOWN, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "LEFT" -> {
                nativeSendKey(KeyEvent.KEYCODE_DPAD_LEFT, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "RIGHT" -> {
                nativeSendKey(KeyEvent.KEYCODE_DPAD_RIGHT, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "HOME" -> {
                nativeSendKey(KeyEvent.KEYCODE_MOVE_HOME, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "END" -> {
                nativeSendKey(KeyEvent.KEYCODE_MOVE_END, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "PGUP" -> {
                nativeSendKey(KeyEvent.KEYCODE_PAGE_UP, ctrlLatched, altLatched)
                consumeModifiers()
            }
            "PGDN" -> {
                nativeSendKey(KeyEvent.KEYCODE_PAGE_DOWN, ctrlLatched, altLatched)
                consumeModifiers()
            }
            else -> {
                nativeSendText(token)
                consumeModifiers()
            }
        }
        terminalSurface.postInvalidateOnAnimation()
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
            .setMessage("Укажите клавиши через запятую (ESC, TAB, CTRL, ALT, UP, DOWN, LEFT, RIGHT, HOME, END, PGUP, PGDN, -, /, |, ~):")
            .setView(edit)
            .setPositiveButton("Сохранить") { _, _ ->
                prefs.edit().putString(PREF_EXTRA_KEYS, edit.text.toString()).apply()
                rebuildExtraKeys()
            }
            .setNeutralButton("Сброс") { _, _ ->
                prefs.edit().putString(PREF_EXTRA_KEYS, DEFAULT_EXTRA_KEYS).apply()
                rebuildExtraKeys()
            }
            .setNegativeButton("Отмена", null)
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
     * Zero-Allocation Hardware-Accelerated Terminal View with Native Android FLOATING ActionMode
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

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Typeface.MONOSPACE
        }
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Pre-allocated JNI transfer buffers (Zero GC pressure in onDraw)
        private val maxGridRows = 250
        private val maxGridCols = 400
        private val jniAttrsBuffer = LongArray(maxGridRows * maxGridCols)
        private val jniRowStrings = Array(maxGridRows) { "" }
        private val jniMeta = IntArray(8) // [rows, cols, curR, curC, curVis, scrollOff, bootState]

        // Local cached display rows for ultra-fast drawing
        private val cachedRowStrings = Array(maxGridRows) { "" }
        private val cachedRowFg = IntArray(maxGridRows) { COLOR_TEXT_PRIMARY }
        private val cachedRowBg = IntArray(maxGridRows) { COLOR_AMOLED_BG }
        private var cachedCursorR = 0
        private var cachedCursorC = 0
        private var cachedCursorVis = true
        private var sessionSpawned = false

        // Native Selection state (Row/Col endpoints + System Floating Toolbar)
        private var isSelecting = false
        private var selStartRow = 0
        private var selStartCol = 0
        private var selEndRow = 0
        private var selEndCol = 0
        private var activeActionMode: ActionMode? = null
        private val selectionContentRect = Rect()

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
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (isSelecting) {
                        stopNativeSelection()
                        return true
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
                    if (scaleDetector.isInProgress) return false
                    if (isSelecting) {
                        val r = (e2.y / charHeightPx).toInt().coerceIn(0, gridRows - 1)
                        val c = (e2.x / charWidthPx).toInt().coerceIn(0, gridCols - 1)
                        selEndRow = r
                        selEndCol = c
                        activeActionMode?.invalidateContentRect()
                        postInvalidateOnAnimation()
                        return true
                    }
                    if (!nativeGetAlternateScreen() && abs(distanceY) > 4f) {
                        val lines = (distanceY / charHeightPx).roundToInt()
                        if (lines != 0) {
                            nativeScrollViewport(lines)
                            postInvalidateOnAnimation()
                        }
                    }
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (scaleDetector.isInProgress || nativeGetBootstrapState() != 2) return
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    val r = (e.y / charHeightPx).toInt().coerceIn(0, gridRows - 1)
                    val c = (e.x / charWidthPx).toInt().coerceIn(0, gridCols - 1)
                    startNativeSelection(r, c)
                }
            }
        )

        init {
            isFocusable = true
            isFocusableInTouchMode = true
            defaultFocusHighlightEnabled = false
            recomputeFontMetrics()
        }

        private fun recomputeFontMetrics() {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                fontSizeSp,
                resources.displayMetrics
            )
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
                if (newCols != gridCols || newRows != gridRows) {
                    gridCols = newCols
                    gridRows = newRows
                    nativeResizePty(gridRows, gridCols)
                }
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

            if (nativeGetBootstrapState() == 2) {
                if (!sessionSpawned && nativeGetSessionCount() == 0) {
                    sessionSpawned = true
                    nativeCreateSession(gridRows, gridCols)
                    post { rebuildTabs() }
                } else {
                    nativeResizePty(gridRows, gridCols)
                }
            }
            postInvalidateOnAnimation()
        }

        override fun onDraw(canvas: Canvas) {
            // Paint only within this View's bounds so headerBar above is never affected
            bgPaint.color = COLOR_AMOLED_BG
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val bootState = nativeGetBootstrapState()
            if (bootState != 2) {
                textPaint.color = COLOR_ACCENT_ACTIVE
                val msg = if (bootState == 3) {
                    "Ошибка bootstrap: ${nativeGetBootstrapError()}"
                } else {
                    "⚡ Инициализация среды Termux C++23..."
                }
                canvas.drawText(msg, dp(16).toFloat(), dp(48).toFloat(), textPaint)
                if (bootState != 3) {
                    // Poll every 40ms while initializing so screen transitions automatically without waiting for touch
                    postInvalidateDelayed(40L)
                }
                return
            }

            if (!sessionSpawned && nativeGetSessionCount() == 0 && width > 0 && height > 0) {
                sessionSpawned = true
                nativeCreateSession(gridRows, gridCols)
                post { rebuildTabs() }
            }

            // Pull only dirty rows from C++23 engine
            val dirtyCount = nativeFetchDirtyRows(jniAttrsBuffer, jniRowStrings, jniMeta)
            val rows = jniMeta[0].coerceIn(1, maxGridRows)
            val cols = jniMeta[1].coerceIn(1, maxGridCols)
            cachedCursorR = jniMeta[2]
            cachedCursorC = jniMeta[3]
            cachedCursorVis = (jniMeta[4] != 0)

            if (dirtyCount > 0) {
                for (r in 0 until rows) {
                    val str = jniRowStrings[r]
                    cachedRowStrings[r] = str
                    val firstPack = jniAttrsBuffer[r * cols]
                    val fg = ((firstPack ushr 32) and 0xFFFFFFFFL).toInt()
                    val bg = (firstPack and 0xFFFFFFFFL).toInt()
                    cachedRowFg[r] = if (fg != 0) fg else COLOR_TEXT_PRIMARY
                    cachedRowBg[r] = if (bg != 0) bg else COLOR_AMOLED_BG
                }
            }

            // Normalize selection range
            var sR1 = selStartRow
            var sC1 = selStartCol
            var sR2 = selEndRow
            var sC2 = selEndCol
            if (sR1 > sR2 || (sR1 == sR2 && sC1 > sC2)) {
                val tr = sR1; sR1 = sR2; sR2 = tr
                val tc = sC1; sC1 = sC2; sC2 = tc
            }

            for (r in 0 until rows) {
                val topY = (r * charHeightPx).toFloat()
                val bottomY = topY + charHeightPx
                val baseY = topY + charBaselinePx

                if (cachedRowBg[r] != COLOR_AMOLED_BG) {
                    bgPaint.color = cachedRowBg[r]
                    canvas.drawRect(0f, topY, width.toFloat(), bottomY, bgPaint)
                }

                // Draw native text selection highlight
                if (isSelecting && r in sR1..sR2) {
                    val cStart = if (r == sR1) sC1 else 0
                    val cEnd = if (r == sR2) sC2 + 1 else cols
                    bgPaint.color = COLOR_SELECTION_BG
                    canvas.drawRect(
                        cStart * charWidthPx,
                        topY,
                        cEnd * charWidthPx,
                        bottomY,
                        bgPaint
                    )
                }

                val line = cachedRowStrings[r]
                if (line.isNotEmpty()) {
                    textPaint.color = cachedRowFg[r]
                    canvas.drawText(line, 0f, baseY, textPaint)
                }
            }

            // Draw active block cursor
            if (cachedCursorVis && !isSelecting && cachedCursorR in 0 until rows) {
                val cx = cachedCursorC * charWidthPx
                val cy = (cachedCursorR * charHeightPx).toFloat()
                bgPaint.color = COLOR_ACCENT_ACTIVE
                canvas.drawRect(cx, cy, cx + charWidthPx, cy + charHeightPx, bgPaint)
            }
        }

        private fun startNativeSelection(row: Int, col: Int) {
            isSelecting = true
            selStartRow = row
            selStartCol = col
            selEndRow = row
            selEndCol = min(gridCols - 1, col + 6)
            updateSelectionContentRect()

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
                            val selected = nativeGetSelectedText(selStartRow, selStartCol, selEndRow, selEndCol)
                            copyToClipboard(selected)
                            stopNativeSelection()
                            return true
                        }
                        2 -> {
                            selStartRow = 0
                            selStartCol = 0
                            selEndRow = gridRows - 1
                            selEndCol = gridCols - 1
                            updateSelectionContentRect()
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
                    isSelecting = false
                    activeActionMode = null
                    postInvalidateOnAnimation()
                }

                override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                    updateSelectionContentRect()
                    outRect.set(selectionContentRect)
                }
            }, ActionMode.TYPE_FLOATING)

            postInvalidateOnAnimation()
        }

        private fun updateSelectionContentRect() {
            val minR = min(selStartRow, selEndRow)
            val maxR = max(selStartRow, selEndRow)
            val minC = min(selStartCol, selEndCol)
            val maxC = max(selStartCol, selEndCol)
            selectionContentRect.set(
                (minC * charWidthPx).toInt(),
                max(0, minR * charHeightPx - charHeightPx),
                ((maxC + 2) * charWidthPx).toInt(),
                min(height, (maxR + 1) * charHeightPx)
            )
        }

        fun stopNativeSelection() {
            isSelecting = false
            activeActionMode?.finish()
            activeActionMode = null
            postInvalidateOnAnimation()
        }

        private fun copyToClipboard(text: String) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("TermuxX", text))
            Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
        }

        private fun pasteFromClipboard() {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = cm?.primaryClip?.getItemAt(0)
            val text = item?.coerceToText(context)?.toString()
            if (!text.isNullOrEmpty()) {
                nativeSendText(text)
            }
        }

        fun showTranscriptCopyDialog() {
            val fullText = nativeGetTranscriptText()
            val tv = TextView(context).apply {
                text = fullText.ifEmpty { "Буфер терминала пуст." }
                setTextIsSelectable(true)
                typeface = Typeface.MONOSPACE
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
                    copyToClipboard(fullText)
                }
                .setNeutralButton("Вставить в терминал") { _, _ ->
                    pasteFromClipboard()
                }
                .setNegativeButton("Закрыть", null)
                .show()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            if (isSelecting && event.actionMasked == MotionEvent.ACTION_MOVE && !scaleDetector.isInProgress) {
                selEndRow = (event.y / charHeightPx).toInt().coerceIn(0, gridRows - 1)
                selEndCol = (event.x / charWidthPx).toInt().coerceIn(0, gridCols - 1)
                activeActionMode?.invalidateContentRect()
                postInvalidateOnAnimation()
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

            return object : BaseInputConnection(this, true) {
                private val editableBuf = SpannableStringBuilder()

                override fun getEditable(): Editable {
                    if (editableBuf.isEmpty()) {
                        Selection.setSelection(editableBuf, 0)
                    }
                    return editableBuf
                }

                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    if (!text.isNullOrEmpty()) {
                        nativeResetScroll()
                        nativeSendText(text.toString())
                        consumeModifiers()
                        postInvalidateOnAnimation()
                    }
                    editableBuf.clear()
                    return true
                }

                override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    if (!text.isNullOrEmpty()) {
                        nativeResetScroll()
                        nativeSendText(text.toString())
                        consumeModifiers()
                        postInvalidateOnAnimation()
                    }
                    editableBuf.clear()
                    return true
                }

                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    repeat(max(1, beforeLength)) {
                        nativeSendKey(KeyEvent.KEYCODE_DEL, false, false)
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
                if (isSelecting) {
                    stopNativeSelection()
                    return true
                }
                return false
            }
            nativeResetScroll()
            val isCtrl = event.isCtrlPressed || ctrlLatched
            val isAlt = event.isAltPressed || altLatched

            val uni = event.getUnicodeChar(event.metaState and KeyEvent.META_CTRL_MASK.inv())
            if (uni != 0 && !isCtrl && !isAlt && keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_DEL && keyCode != KeyEvent.KEYCODE_TAB && keyCode != KeyEvent.KEYCODE_ESCAPE) {
                nativeSendText(String(Character.toChars(uni)))
                consumeModifiers()
                postInvalidateOnAnimation()
                return true
            }

            nativeSendKey(keyCode, isCtrl, isAlt)
            consumeModifiers()
            postInvalidateOnAnimation()
            return true
        }
    }
}
