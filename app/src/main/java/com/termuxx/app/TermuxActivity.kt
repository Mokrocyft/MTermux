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
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.animation.ValueAnimator
import android.view.ActionMode
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
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
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.SurroundingText
import android.view.inputmethod.TextAttribute
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.termuxx.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
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
private const val KEY_COLOR_SEED_V2 = "color_seed_v2"
private const val KEY_MONET_STYLE_V2 = "monet_style_v2" // 0: Tonal Spot, 1: Neutral, 2: Vibrant, 3: Expressive (ColorSpec 2026)
private const val KEY_FONT_SCALE = "font_scale_v1"
private const val KEY_USE_SYSTEM_COLOR = "use_system_color_v1"
private const val KEY_FONT_PRESET = "font_preset_v2" // 0: System UI Default, 1: System Monospace, 2: JB Mono Regular, 3: JB Mono Light, 4: JB Mono Medium, 5: JB Mono SemiBold, 6: JB Mono Bold, 7: Custom .ttf/.otf
private const val KEY_CUSTOM_FONT_PATH = "custom_font_path_v1"
private const val KEY_FONT_LIGATURES = "font_ligatures_v1"
private const val KEY_LINE_SPACING_PCT = "line_spacing_pct_v1" // 85..135 (default 100)
private const val KEY_SCROLLBACK_LINES = "scrollback_lines_v1" // default 4000
private const val KEY_COPY_ON_SELECT = "copy_on_select_v1"
private const val KEY_KEEP_SCREEN_ON = "keep_screen_on_v1"
private const val KEY_APP_LANG = "app_lang_v1" // 0: Auto, 1: RU, 2: EN
internal const val ACTION_EXIT_APP = "com.termuxx.ACTION_EXIT_APP"
private const val COLOR_PASTEL_RED = 0xFFF28B82.toInt()

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

// ColorBlendr Basic Seed Swatches (ColorSpec 2026)
private data class SeedColorOption(
    val name: String,
    val seedArgb: Int
)

private val COLORBLENDR_SEEDS = arrayOf(
    SeedColorOption("Ocean Blue", 0xFF5179A9.toInt()),
    SeedColorOption("Emerald Mint", 0xFF34846F.toInt()),
    SeedColorOption("Royal Violet", 0xFF7A6FAB.toInt()),
    SeedColorOption("Warm Amber", 0xFFA76837.toInt()),
    SeedColorOption("Crimson Rose", 0xFFAC6166.toInt()),
    SeedColorOption("Sage Olive", 0xFF608045.toInt()),
    SeedColorOption("Lagoon Cyan", 0xFF2B8193.toInt()),
    SeedColorOption("Golden Ochre", 0xFF8B7526.toInt()),
    SeedColorOption("OrchidPlum", 0xFF9B6593.toInt())
)

// Only the 4 requested Material 3 ColorSpec 2026 Palette Styles
private data class MonetStyleOption(
    val index: Int,
    val title: String,
    val description: String
)

private val MONET_STYLES_2026 = arrayOf(
    MonetStyleOption(
        0,
        "Tonal Spot",
        "Сбалансированная палитра с мягкими тональными переходами"
    ),
    MonetStyleOption(
        1,
        "Neutral",
        "Спокойная минималистичная схема с деликатным оттенком"
    ),
    MonetStyleOption(
        2,
        "Vibrant",
        "Насыщенная палитра с высокой хроматичностью акцентов"
    ),
    MonetStyleOption(
        3,
        "Expressive",
        "Выразительное сочетание оттенков с глубоким контрастом"
    )
)

private const val KEY_SSH_SERVERS_V2 = "ssh_servers_v2"
private const val KEY_SSH_SERVERS_V1 = "ssh_servers_v1"
private const val KEY_LOCAL_STARTUP_CMD = "local_startup_cmd_v1"

private const val COLOR_EXPRESSIVE_PILL_BG = 0xFF141821.toInt()
private const val COLOR_PING_GOOD = 0xFF4CAF50.toInt()
private const val COLOR_PING_WARN = 0xFFFFB74D.toInt()

internal enum class M3Icon {
    ADD,
    ROOT_HASH,
    HUB_GRID,
    SETTINGS,
    CLOSE,
    SEARCH,
    TREE,
    EYE,
    CREATE_FOLDER,
    REFRESH,
    UPLOAD,
    DOWNLOAD,
    FOLDER,
    FOLDER_OPEN,
    FILE,
    MORE_VERT,
    ARROW_BACK,
    CHEVRON_RIGHT,
    CHEVRON_DOWN,
    TERMINAL,
    PING,
    KEY,
    CHECK,
    TRASH,
    ALL_PACKAGES,
    STORAGE,
    CURSOR,
    KEYBOARD,
    VIBRATION,
    PALETTE,
    CPU,
    SLIDERS,
    SERVER,
    EDIT,
    COPY,
    FONT
}

private data class DpkgPackageInfo(
    val name: String,
    val version: String,
    val sizeKb: Long,
    val description: String,
    val isCore: Boolean = false
)

internal data class SshServerProfile(
    val id: String,
    val label: String,
    val host: String,
    val port: Int = 22,
    val username: String = "root",
    val authType: Int = 0, // 0 = Password, 1 = Private Key, 2 = SSH Agent
    val secretOrKeyPath: String = "",
    val startupCommand: String = "",
    val useCompression: Boolean = true,
    val forwardAgent: Boolean = false,
    val localForwards: String = "", // e.g. "8080:127.0.0.1:80"
    val remoteForwards: String = "", // e.g. "8080:127.0.0.1:8080"
    val dynamicSocksPort: String = "", // e.g. "1080"
    val jumpHost: String = "", // e.g. "user@bastion:22"
    val keepAliveSec: Int = 30,
    val tunnelOnlyN: Boolean = false,
    val ipv4Only: Boolean = false,
    val customFlags: String = ""
)

internal data class SftpItemEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedEpochMs: Long,
    val permissions: String = "",
    val depth: Int = 0,
    val isExpanded: Boolean = false
)

class TermuxActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootLayout: LinearLayout
    private lateinit var headerBar: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var extraKeysRoot: LinearLayout
    internal lateinit var terminalSurface: TerminalSurface
    private val modifierButtons = ArrayList<TextView>(4)

    internal var useSystemDynamicColor: Boolean = true
    internal var appLangPref: Int = 0 // 0 = System Auto, 1 = RU, 2 = EN
    internal var copyOnSelect: Boolean = false
    internal var keepScreenOnPref: Boolean = false
    internal var colorSeedArgb: Int = COLORBLENDR_SEEDS[0].seedArgb
    internal var monetStyleIndex: Int = 0 // 0: Tonal Spot, 1: Neutral, 2: Vibrant, 3: Expressive

    internal fun isEnglish(): Boolean {
        return when (appLangPref) {
            1 -> false
            2 -> true
            else -> !Locale.getDefault().language.lowercase().startsWith("ru")
        }
    }

    internal fun tr(ru: String, en: String): String = if (isEnglish()) en else ru

    internal fun getSystemDynamicSeedColor(): Int {
        return try {
            getColor(android.R.color.system_accent1_400)
        } catch (_: Throwable) {
            COLORBLENDR_SEEDS[0].seedArgb
        }
    }
    internal var colorSpecTokens: IntArray = intArrayOf(
        0xFF8AB4F8.toInt(), // [0] primary
        0xFF0A2540.toInt(), // [1] onPrimary
        0xFF172B44.toInt(), // [2] primaryContainer
        0xFFD4E4FF.toInt(), // [3] onPrimaryContainer
        0xFFA8C7FA.toInt(), // [4] secondary
        0xFF1B2638.toInt(), // [5] secondaryContainer
        0xFFD0BCFF.toInt(), // [6] tertiary
        0xFF281E3E.toInt(), // [7] tertiaryContainer
        0xFF0A0D12.toInt(), // [8] surface
        0xFF12161F.toInt(), // [9] surfaceContainerLow
        0xFF171C26.toInt(), // [10] surfaceContainer
        0xFF202634.toInt(), // [11] surfaceContainerHigh
        0xFF2A3242.toInt(), // [12] outlineVariant
        0xFF141923.toInt()  // [13] previewSquare
    )

    internal val uiAccent: Int get() = colorSpecTokens[0]
    internal val uiOnAccent: Int get() = colorSpecTokens[1]
    internal val uiAccentContainer: Int get() = colorSpecTokens[2]
    internal val uiOnAccentContainer: Int get() = colorSpecTokens[3]
    internal val uiSecondary: Int get() = colorSpecTokens[4]
    internal val uiSecondaryContainer: Int get() = colorSpecTokens[5]
    internal val uiTertiary: Int get() = colorSpecTokens[6]
    internal val uiSurface: Int get() = colorSpecTokens[8]
    internal val uiSurfaceLow: Int get() = colorSpecTokens[9]
    internal val uiSurfaceContainer: Int get() = colorSpecTokens[10]
    internal val uiSurfaceHigh: Int get() = colorSpecTokens[11]
    internal val uiOutlineVariant: Int get() = colorSpecTokens[12]
    internal val uiPreviewSquare: Int get() = colorSpecTokens[13]

    internal fun refreshColorSpec2026FromCpp() {
        try {
            val effectiveSeed = if (useSystemDynamicColor) getSystemDynamicSeedColor() else colorSeedArgb
            val generated = nativeGenerateColorSpec2026Palette(effectiveSeed, monetStyleIndex.coerceIn(0, 3))
            if (generated.size >= 14) {
                colorSpecTokens = generated
            }
        } catch (_: Throwable) {
        }
    }

    internal var hapticEnabled = DEFAULT_HAPTIC_ENABLED
    internal var hapticAmplitude = DEFAULT_HAPTIC_AMPLITUDE
    internal var hapticDurationMs = DEFAULT_HAPTIC_DURATION_MS

    // File manager clipboard state (copy / cut)
    private var fileClipSource: File? = null
    private var fileClipIsMove: Boolean = false
    private val serverPingCacheMs = HashMap<String, Int>()

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

        activeInstance = this
        if (intent?.action == ACTION_EXIT_APP) {
            exitAppCompletely()
            return
        }
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        useSystemDynamicColor = prefs.getBoolean(KEY_USE_SYSTEM_COLOR, true)
        appLangPref = prefs.getInt(KEY_APP_LANG, 0).coerceIn(0, 2)
        copyOnSelect = prefs.getBoolean(KEY_COPY_ON_SELECT, false)
        keepScreenOnPref = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        colorSeedArgb = prefs.getInt(KEY_COLOR_SEED_V2, getSystemDynamicSeedColor())
        monetStyleIndex = prefs.getInt(KEY_MONET_STYLE_V2, 0).coerceIn(0, 3)
        refreshColorSpec2026FromCpp()
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, DEFAULT_HAPTIC_ENABLED)
        hapticAmplitude = prefs.getInt(KEY_HAPTIC_AMPLITUDE, DEFAULT_HAPTIC_AMPLITUDE).coerceIn(1, 255)
        hapticDurationMs = prefs.getInt(KEY_HAPTIC_DURATION_MS, DEFAULT_HAPTIC_DURATION_MS).coerceIn(1, 40)

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

        // Top Bar — pure #000000 seamless background, lowered safely below status bar & camera cutout
        headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_AMOLED_BG)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            clipChildren = false
            clipToPadding = false
        }

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
        }
        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(2), 0, dp(2))
        }
        tabScroll.addView(tabStrip)
        headerBar.addView(
            tabScroll,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        headerBar.addView(
            makeHeaderIconButton(
                icon = M3Icon.ADD,
                onClick = { createSession(false) },
                onLongClick = { showWorkspaceHubDialog(initialTab = 0) }
            )
        )
        headerBar.addView(
            makeHeaderIconButton(
                icon = M3Icon.ROOT_HASH,
                onClick = { createSession(true) },
                onLongClick = { terminalSurface.showTranscriptCopyDialog() }
            )
        )
        // Single unified Material Design 3 Hub icon in pure white (#FFFFFF)
        headerBar.addView(
            makeHeaderIconButton(
                icon = M3Icon.HUB_GRID,
                tintColor = 0xFFFFFFFF.toInt(),
                onClick = { showWorkspaceHubDialog(initialTab = 0) },
                onLongClick = { showWorkspaceHubDialog(initialTab = 1) }
            )
        )
        headerBar.addView(
            makeHeaderIconButton(
                icon = M3Icon.SETTINGS,
                tintColor = COLOR_TEXT_BRIGHT,
                onClick = { showCategorizedSettingsDialog() }
            )
        )
        // Close button in pastel red (#F28B82)
        headerBar.addView(
            makeHeaderIconButton(
                icon = M3Icon.CLOSE,
                tintColor = COLOR_PASTEL_RED,
                onClick = { closeCurrentSession() }
            )
        )

        rootLayout.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42))
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
            setPadding(dp(2), dp(2), dp(2), dp(2))
            clipChildren = false
            clipToPadding = false
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

        applySafeWindowInsets(rootLayout, extraTopDp = 4, extraBottomDp = 0)

        if (keepScreenOnPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val initCursorShape = prefs.getInt(KEY_CURSOR_SHAPE, DEFAULT_CURSOR_SHAPE)
        nativeSetDefaultCursorShape(min(2, initCursorShape))
        val initScrollback = prefs.getInt(KEY_SCROLLBACK_LINES, 4000)
        nativeSetMaxScrollback(initScrollback)
        rebuildTabs()
        nativeRegisterSurface(terminalSurface)
        nativeInitAndPollBootstrap(applicationInfo.nativeLibraryDir)

        // Start foreground notification immediately upon opening the app (zero delay!)
        ensureNotificationsAndKeepAlive()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_EXIT_APP) {
            exitAppCompletely()
        }
    }

    internal fun exitAppCompletely() {
        try {
            stopService(Intent(this, KeepAliveService::class.java))
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancelAll()
        } catch (_: Throwable) {
        }
        try {
            nativeDestroyAll()
        } catch (_: Throwable) {
        }
        try {
            finishAndRemoveTask()
        } catch (_: Throwable) {
            finish()
        }
        kotlin.system.exitProcess(0)
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
        // Always start KeepAliveService immediately so the notification appears right away
        startForegroundKeepAlive()
        try {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_POST_NOTIFICATIONS
                )
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
        if (actionSpec == "PASTE" || actionSpec == "CLIP") {
            pasteFromClipboard()
            return
        }
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
        terminalSurface.postDelayed({ terminalSurface.syncInputConnectionWithGrid() }, 25L)
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
    // Material 3 Expressive / ArchiveTune / BakaSU / ColorBlendr Design Engine
    // (Borderless Surfaces + Predictive Back + BakaSU Morphing Indicator + EN/RU)
    // ========================================================================
    private fun blendColor(c1: Int, c2: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val inv = 1f - t
        val a = ((c1 ushr 24) * inv + (c2 ushr 24) * t).roundToInt().coerceIn(0, 255)
        val r = (((c1 shr 16) and 0xFF) * inv + ((c2 shr 16) and 0xFF) * t).roundToInt().coerceIn(0, 255)
        val g = (((c1 shr 8) and 0xFF) * inv + ((c2 shr 8) and 0xFF) * t).roundToInt().coerceIn(0, 255)
        val b = ((c1 and 0xFF) * inv + (c2 and 0xFF) * t).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
    }

    /**
     * Borderless Tonal Surface Drawable (`stroke = 0` — no contour/outline anywhere outside Terminal!).
     */
    private fun tonalBg(fill: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx.toFloat()
        }
    }

    // Android 17 AOSP Emphasized Spatial Curve: cubic-bezier(0.2, 0.0, 0.0, 1.0)
    private val aosp17SpatialInterpolator by lazy {
        PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    }

    private fun animateAosp17PageTransition(target: View, forward: Boolean = true) {
        val dist = max(dpF(48f), resources.displayMetrics.widthPixels * 0.14f)
        target.translationX = if (forward) dist else -dist
        target.alpha = 0f
        target.scaleX = 0.975f
        target.scaleY = 0.975f
        target.animate()
            .translationX(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(270L)
            .setInterpolator(aosp17SpatialInterpolator)
            .start()
    }

    /**
     * Borderless Material 3 Expressive Floating Toolbar / FAB Surface (BakaSU / ArchiveTune style).
     */
    private fun makeM3FloatingSurfaceDrawable(
        radiusPx: Int,
        tintWithAccent: Boolean = false
    ): GradientDrawable {
        val fillCol = if (tintWithAccent) {
            uiAccentContainer
        } else {
            withAlpha(blendColor(uiSurfaceContainer, uiSurfaceHigh, 0.65f), 245)
        }
        return GradientDrawable().apply {
            setColor(fillCol)
            cornerRadius = radiusPx.toFloat()
        }
    }

    private fun attachSmoothPressAnimation(target: View, minScale: Float = 0.96f) {
        target.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(minScale)
                        .scaleY(minScale)
                        .setDuration(100L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220L)
                        .setInterpolator(OvershootInterpolator(1.9f))
                        .start()
                }
            }
            false
        }
    }

    private fun createExpressiveDialog(maxWidthDp: Int = 430): Pair<Dialog, LinearLayout> {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(
                min(resources.displayMetrics.widthPixels - dp(24), dp(maxWidthDp)),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            attributes = attributes.apply {
                dimAmount = 0.65f
            }
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = tonalBg(uiSurfaceHigh, dp(28))
        }
        dlg.setContentView(sheet)
        animateAosp17PageTransition(sheet, forward = true)
        return dlg to sheet
    }

    /**
     * BakaSU-style Material 3 Choose Dialog (`SettingsChooseDialogFrame` + `RadioButton` + `SegmentedColumn`)
     * with zero border outlines.
     */
    private fun showBakaSuChooseDialog(
        title: String,
        items: List<String>,
        descriptions: List<String?> = emptyList(),
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        if (items.isEmpty()) return
        val (dlg, sheet) = createExpressiveDialog(430)
        sheet.setPadding(dp(22), dp(22), dp(22), dp(16))

        var currentIndex = selectedIndex.coerceIn(0, items.lastIndex)

        sheet.addView(TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            setTextColor(COLOR_TEXT_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.5f)
            setPadding(dp(8), 0, dp(8), dp(16))
        })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val segCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(segCol)
        sheet.addView(scroll)

        lateinit var rebuildOptionRows: () -> Unit
        rebuildOptionRows = {
            segCol.removeAllViews()
            for (i in items.indices) {
                val isSel = (currentIndex == i)
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    val fillCol = if (isSel) uiAccentContainer else uiSurfaceLow
                    background = segmentedCardBg(i, items.size, fillCol, 0, 18f, 5f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = if (i < items.lastIndex) dp(2) else 0
                    }
                    attachSmoothPressAnimation(this, 0.98f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        currentIndex = i
                        rebuildOptionRows()
                    }
                }

                row.addView(
                    M3RadioButtonView(this, checked = isSel).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                            marginEnd = dp(14)
                        }
                    }
                )

                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                textCol.addView(TextView(this).apply {
                    text = items[i]
                    setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_BRIGHT)
                    typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                })
                val desc = descriptions.getOrNull(i)
                if (!desc.isNullOrBlank()) {
                    textCol.addView(TextView(this).apply {
                        text = desc
                        setTextColor(if (isSel) withAlpha(uiOnAccentContainer, 200) else COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                row.addView(textCol)
                segCol.addView(row)
            }
        }
        rebuildOptionRows()

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        footer.addView(TextView(this).apply {
            text = tr("Отмена", "Cancel")
            setTextColor(uiAccent)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            attachSmoothPressAnimation(this, 0.93f)
            setOnClickListener {
                triggerHaptic(0.75f)
                dlg.dismiss()
            }
        })
        footer.addView(TextView(this).apply {
            text = tr("ОК", "OK")
            setTextColor(uiOnAccent)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            background = tonalBg(uiAccent, dp(18))
            setPadding(dp(20), dp(9), dp(20), dp(9))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(6)
            }
            attachSmoothPressAnimation(this, 0.93f)
            setOnClickListener {
                triggerHaptic(0.9f)
                dlg.dismiss()
                onSelected(currentIndex)
            }
        })
        sheet.addView(footer)
        dlg.show()
    }

    internal inner class M3RadioButtonView(
        context: Context,
        var checked: Boolean
    ) : View(context) {
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width * 0.5f
            val cy = height * 0.5f
            val r = min(cx, cy) - dpF(2f)
            ringPaint.strokeWidth = dpF(2.1f)
            ringPaint.color = if (checked) uiAccent else COLOR_TEXT_DIM
            canvas.drawCircle(cx, cy, r, ringPaint)
            if (checked) {
                dotPaint.color = uiAccent
                canvas.drawCircle(cx, cy, r * 0.52f, dotPaint)
            }
        }
    }

    /**
     * Material 3 Expressive Switch (`52dp x 32dp` borderless capsule track, expanding thumb with `✓` / `✕` icons).
     */
    internal inner class M3ExpressiveSwitchView(
        context: Context,
        initialChecked: Boolean,
        private val onCheckedChange: ((Boolean) -> Unit)? = null
    ) : View(context) {
        var isCheckedState: Boolean = initialChecked
            private set

        private var progress: Float = if (initialChecked) 1f else 0f
        private var pressProgress: Float = 0f
        private val trackRect = RectF()
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val iconPath = Path()

        fun setCheckedAnimated(newChecked: Boolean, invokeCallback: Boolean = false) {
            if (isCheckedState == newChecked) return
            isCheckedState = newChecked
            ValueAnimator.ofFloat(progress, if (newChecked) 1f else 0f).apply {
                duration = 210L
                interpolator = OvershootInterpolator(1.4f)
                addUpdateListener { anim ->
                    progress = (anim.animatedValue as Float).coerceIn(0f, 1f)
                    invalidate()
                }
                start()
            }
            if (invokeCallback) {
                onCheckedChange?.invoke(newChecked)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pressProgress = 1f
                    invalidate()
                    if (onCheckedChange != null) return true
                }
                MotionEvent.ACTION_UP -> {
                    pressProgress = 0f
                    invalidate()
                    if (onCheckedChange != null) {
                        triggerHaptic(0.85f)
                        setCheckedAnimated(!isCheckedState, invokeCallback = true)
                        return true
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    pressProgress = 0f
                    invalidate()
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val pad = dpF(1.5f)
            trackRect.set(pad, pad, w - pad, h - pad)
            val corner = trackRect.height() * 0.5f

            val trackCol = blendColor(uiSurfaceHigh, uiAccent, progress)
            trackPaint.color = trackCol
            canvas.drawRoundRect(trackRect, corner, corner, trackPaint)

            val minX = pad + corner
            val maxX = w - pad - corner
            val cx = minX + (maxX - minX) * progress
            val cy = h * 0.5f
            val baseThumbR = dpF(10.5f + 1.5f * progress)
            val thumbR = baseThumbR + dpF(2.0f) * pressProgress

            thumbPaint.color = blendColor(COLOR_TEXT, uiOnAccent, progress)
            canvas.drawCircle(cx, cy, thumbR, thumbPaint)

            iconPaint.strokeWidth = dpF(1.85f)
            iconPath.reset()
            if (progress >= 0.5f) {
                iconPaint.color = uiAccent
                val s = dpF(4.2f)
                iconPath.moveTo(cx - s * 0.85f, cy + s * 0.05f)
                iconPath.lineTo(cx - s * 0.18f, cy + s * 0.72f)
                iconPath.lineTo(cx + s * 0.95f, cy - s * 0.65f)
                canvas.drawPath(iconPath, iconPaint)
            } else {
                iconPaint.color = uiSurfaceHigh
                val s = dpF(3.3f)
                canvas.drawLine(cx - s, cy - s, cx + s, cy + s, iconPaint)
                canvas.drawLine(cx + s, cy - s, cx - s, cy + s, iconPaint)
            }
        }
    }

    /**
     * BakaSU / ArchiveTune Material 3 Expressive Thick Split-Track Slider (`KeyPointSlider.kt`).
     */
    internal inner class M3ExpressiveSliderView(
        context: Context,
        initialProgress: Int,
        private val maxProgress: Int,
        private val onProgressChanged: (Int, Boolean) -> Unit,
        private val onStopTouch: (() -> Unit)? = null
    ) : View(context) {
        var progress: Int = initialProgress.coerceIn(0, max(1, maxProgress))
            set(value) {
                field = value.coerceIn(0, max(1, maxProgress))
                invalidate()
            }

        private var isDragging = false
        private var lastTickBucket = -1
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val path = Path()
        private val rect = RectF()
        private val radii = FloatArray(8)

        private fun drawSegment(
            canvas: Canvas,
            startX: Float,
            endX: Float,
            top: Float,
            height: Float,
            leftCorner: Float,
            rightCorner: Float,
            color: Int
        ) {
            if (endX <= startX + 1f) return
            val segW = endX - startX
            val lc = min(leftCorner, segW * 0.5f)
            val rc = min(rightCorner, segW * 0.5f)
            radii[0] = lc; radii[1] = lc
            radii[2] = rc; radii[3] = rc
            radii[4] = rc; radii[5] = rc
            radii[6] = lc; radii[7] = lc
            path.reset()
            rect.set(startX, top, endX, top + height)
            path.addRoundRect(rect, radii, Path.Direction.CW)
            paint.color = color
            canvas.drawPath(path, paint)
        }

        private fun updateFromTouchX(x: Float) {
            val pad = dpF(8f)
            val usable = max(1f, width - pad * 2f)
            val frac = ((x - pad) / usable).coerceIn(0f, 1f)
            val newProg = (frac * maxProgress).roundToInt().coerceIn(0, maxProgress)
            if (newProg != progress) {
                progress = newProg
                val bucket = (frac * 12f).toInt()
                if (bucket != lastTickBucket) {
                    if (lastTickBucket != -1) triggerHaptic(0.45f)
                    lastTickBucket = bucket
                }
                onProgressChanged(progress, true)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    isDragging = true
                    lastTickBucket = -1
                    updateFromTouchX(event.x)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateFromTouchX(event.x)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    isDragging = false
                    triggerHaptic(0.75f)
                    onStopTouch?.invoke()
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val centerY = h * 0.5f
            val trackH = dpF(16f)
            val trackTop = centerY - trackH * 0.5f
            val outerR = trackH * 0.5f
            val innerR = dpF(2.5f)
            val thumbW = dpF(4.2f)
            val thumbH = if (isDragging) dpF(32f) else dpF(28f)
            val gap = dpF(6f)

            val minX = dpF(6f)
            val maxX = max(minX + 1f, w - dpF(6f))
            val frac = if (maxProgress > 0) progress.toFloat() / maxProgress.toFloat() else 0f
            val thumbX = minX + (maxX - minX) * frac

            val gapStart = (thumbX - thumbW * 0.5f - gap).coerceIn(minX, maxX)
            val gapEnd = (thumbX + thumbW * 0.5f + gap).coerceIn(minX, maxX)

            drawSegment(canvas, minX, gapStart, trackTop, trackH, outerR, innerR, uiAccent)
            drawSegment(canvas, gapEnd, maxX, trackTop, trackH, innerR, outerR, uiSurfaceHigh)

            val stopX = maxX - outerR
            if (stopX > gapEnd + dpF(4f)) {
                paint.color = uiAccent
                canvas.drawCircle(stopX, centerY, dpF(2.2f), paint)
            }

            paint.color = uiAccent
            rect.set(
                thumbX - thumbW * 0.5f,
                centerY - thumbH * 0.5f,
                thumbX + thumbW * 0.5f,
                centerY + thumbH * 0.5f
            )
            canvas.drawRoundRect(rect, thumbW * 0.5f, thumbW * 0.5f, paint)
        }
    }

    internal inner class ColorPreviewCanvasView(
        context: Context,
        private val tokens: IntArray,
        private val isSwatchWithCenter: Boolean = false,
        private val seedCenterColor: Int = 0,
        var isSwatchSelected: Boolean = false
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val rect = RectF()
        private val tickPath = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val r = dpF(12f)
            rect.set(0f, 0f, w, h)
            paint.color = tokens.getOrElse(13) { uiPreviewSquare }
            canvas.drawRoundRect(rect, r, r, paint)

            val pad = if (isSwatchWithCenter) dpF(6f) else dpF(9f)
            rect.set(pad, pad, w - pad, h - pad)

            paint.color = tokens.getOrElse(0) { uiAccent }
            canvas.drawArc(rect, 180f, 180f, true, paint)

            paint.color = tokens.getOrElse(4) { uiSecondary }
            canvas.drawArc(rect, 90f, 90f, true, paint)

            paint.color = tokens.getOrElse(6) { uiTertiary }
            canvas.drawArc(rect, 0f, 90f, true, paint)

            if (isSwatchWithCenter) {
                val cx = w * 0.5f
                val cy = h * 0.5f
                val cr = dpF(10f)
                paint.color = seedCenterColor
                canvas.drawCircle(cx, cy, cr, paint)

                if (isSwatchSelected) {
                    tickPaint.strokeWidth = dpF(2.2f)
                    tickPaint.color = 0xFFFFFFFF.toInt()
                    tickPath.reset()
                    tickPath.moveTo(cx - cr * 0.45f, cy)
                    tickPath.lineTo(cx - cr * 0.12f, cy + cr * 0.34f)
                    tickPath.lineTo(cx + cr * 0.48f, cy - cr * 0.32f)
                    canvas.drawPath(tickPath, tickPaint)
                }
            }
        }
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setPadding(dp(14), dp(7), dp(14), dp(7))
            setTextColor(if (selected) accentColor else COLOR_TEXT)
            // Zero border outline (borderless tonal capsule)
            background = tonalBg(
                if (selected) containerColor else uiSurfaceLow,
                dp(12)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(2), dp(6), dp(2))
            }
            attachSmoothPressAnimation(this, 0.94f)
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
            clipChildren = false
            clipToPadding = false
            setPadding(dp(1), dp(3), dp(1), dp(3))
        }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            addView(row)
        }
        return hsv to row
    }

    private fun makeSectionLabel(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            setTextColor(uiAccent)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(dp(16), dp(14), dp(16), dp(6))
        }
    }

    private fun makeSeekBarCard(
        icon: M3Icon,
        labelPrefix: String,
        initialProgress: Int,
        maxProgress: Int,
        index: Int = 0,
        total: Int = 1,
        formatValue: (Int) -> String,
        onChanged: (Int) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(10))
            background = segmentedCardBg(index, total, uiSurfaceLow, 0, 22f, 5f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = if (index < total - 1) dp(2) else dp(8)
            }

            val topRow = LinearLayout(this@TermuxActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val iconBox = FrameLayout(this@TermuxActivity).apply {
                background = tonalBg(uiAccentContainer, dp(12))
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    marginEnd = dp(14)
                }
                addView(MaterialIconView(this@TermuxActivity, icon, uiAccent, 20f, 2.0f))
            }
            topRow.addView(iconBox)

            val titleTv = TextView(this@TermuxActivity).apply {
                text = labelPrefix
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valTv = TextView(this@TermuxActivity).apply {
                text = formatValue(initialProgress)
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                background = tonalBg(uiAccentContainer, dp(99))
                setPadding(dp(12), dp(4), dp(12), dp(4))
            }
            topRow.addView(titleTv)
            topRow.addView(valTv)
            addView(topRow)

            val m3Slider = M3ExpressiveSliderView(
                this@TermuxActivity,
                initialProgress = initialProgress,
                maxProgress = maxProgress,
                onProgressChanged = { p, fromUser ->
                    valTv.text = formatValue(p)
                    if (fromUser) onChanged(p)
                }
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply {
                    topMargin = dp(4)
                }
            }
            addView(m3Slider)
        }
    }

    private fun makeBakaSuSwitchCard(
        icon: M3Icon,
        title: String,
        subtitle: String,
        checked: Boolean,
        index: Int = 0,
        total: Int = 1,
        isMasterSwitch: Boolean = false,
        onToggle: (Boolean) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
            background = if (isMasterSwitch) {
                tonalBg(
                    if (checked) uiAccentContainer else uiSurfaceLow,
                    dp(28)
                )
            } else {
                segmentedCardBg(index, total, uiSurfaceLow, 0, 22f, 5f)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = if (isMasterSwitch) dp(12) else if (index < total - 1) dp(2) else dp(8)
            }

            addView(
                FrameLayout(this@TermuxActivity).apply {
                    background = tonalBg(
                        if (isMasterSwitch && checked) uiAccent else uiAccentContainer,
                        dp(12)
                    )
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                        marginEnd = dp(14)
                    }
                    addView(
                        MaterialIconView(
                            this@TermuxActivity,
                            icon,
                            if (isMasterSwitch && checked) uiOnAccent else uiAccent,
                            20.5f,
                            2.05f
                        )
                    )
                }
            )

            val col = LinearLayout(this@TermuxActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(12)
                }
            }
            col.addView(TextView(this@TermuxActivity).apply {
                text = title
                setTextColor(if (isMasterSwitch && checked) uiOnAccentContainer else COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            })
            col.addView(TextView(this@TermuxActivity).apply {
                text = subtitle
                setTextColor(if (isMasterSwitch && checked) withAlpha(uiOnAccentContainer, 200) else COLOR_TEXT_DIM)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(2), 0, 0)
            })
            addView(col)

            val sw = M3ExpressiveSwitchView(this@TermuxActivity, checked) { newVal ->
                onToggle(newVal)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(32))
            }
            addView(sw)

            attachSmoothPressAnimation(this, 0.98f)
            setOnClickListener {
                triggerHaptic(0.85f)
                val next = !sw.isCheckedState
                sw.setCheckedAnimated(next, invokeCallback = true)
            }
        }
    }

    private fun makeBakaSuChooseCard(
        icon: M3Icon,
        title: String,
        subtitle: String?,
        items: List<String>,
        descriptions: List<String?> = emptyList(),
        selectedIndex: Int,
        index: Int = 0,
        total: Int = 1,
        onSelected: (Int) -> Unit
    ): LinearLayout {
        val currentValText = items.getOrNull(selectedIndex) ?: ""
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
            background = segmentedCardBg(index, total, uiSurfaceLow, 0, 22f, 5f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = if (index < total - 1) dp(2) else dp(8)
            }

            addView(
                FrameLayout(this@TermuxActivity).apply {
                    background = tonalBg(uiAccentContainer, dp(12))
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                        marginEnd = dp(14)
                    }
                    addView(MaterialIconView(this@TermuxActivity, icon, uiAccent, 20.5f, 2.05f))
                }
            )

            val col = LinearLayout(this@TermuxActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(10)
                }
            }
            col.addView(TextView(this@TermuxActivity).apply {
                text = title
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            })
            if (!subtitle.isNullOrBlank()) {
                col.addView(TextView(this@TermuxActivity).apply {
                    text = subtitle
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setPadding(0, dp(2), 0, 0)
                })
            }
            if (currentValText.isNotBlank()) {
                col.addView(TextView(this@TermuxActivity).apply {
                    text = currentValText
                    setTextColor(COLOR_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(4), 0, 0)
                })
            }
            addView(col)

            addView(
                MaterialIconView(this@TermuxActivity, M3Icon.CHEVRON_RIGHT, COLOR_TEXT_DIM, 20f, 2.0f).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                }
            )

            attachSmoothPressAnimation(this, 0.98f)
            setOnClickListener {
                triggerHaptic(0.85f)
                showBakaSuChooseDialog(
                    title = title,
                    items = items,
                    descriptions = descriptions,
                    selectedIndex = selectedIndex,
                    onSelected = onSelected
                )
            }
        }
    }

    // ========================================================================
    // Full-Screen AOSP Material 3 Settings Page with Predictive Back Gesture
    // (Two-layer Stage: Home Menu underneath + Sub-page on top for live preview!)
    // ========================================================================
    private fun showCategorizedSettingsDialog() {
        val dlg = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(uiSurface))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            attributes = attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            setDecorFitsSystemWindows(false)
        }

        val pageRoot = FrameLayout(this).apply {
            setBackgroundColor(uiSurface)
        }
        applySafeWindowInsets(pageRoot, extraLeftDp = 0, extraTopDp = 8, extraRightDp = 0, extraBottomDp = 8)

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        pageRoot.addView(mainCol)

        // Minimalist Top AppToolbar (No "Material 3 Expressive" subtitle!)
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(10))
        }
        val backBtn = FrameLayout(this).apply {
            background = tonalBg(uiSurfaceContainer, dp(20))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                marginEnd = dp(14)
            }
            addView(MaterialIconView(this@TermuxActivity, M3Icon.ARROW_BACK, COLOR_TEXT_BRIGHT, 21f, 2.15f))
            attachSmoothPressAnimation(this, 0.90f)
        }
        val titleTv = TextView(this).apply {
            text = tr("Настройки", "Settings")
            setTextColor(COLOR_TEXT_BRIGHT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        toolbar.addView(backBtn)
        toolbar.addView(titleTv)
        mainCol.addView(toolbar)

        // Two-layer stage for Android 17 Predictive Back Gesture preview!
        val stageFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val homeScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val homeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(40))
        }
        homeScroll.addView(homeContainer)

        val detailScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(uiSurface)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val detailContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(40))
        }
        detailScroll.addView(detailContainer)

        stageFrame.addView(homeScroll)
        stageFrame.addView(detailScroll)
        mainCol.addView(stageFrame)

        var inSubPage = false
        lateinit var rebuildHomeMenu: () -> Unit

        fun openSubPageWithTransition(subPageTitle: String, populate: () -> Unit) {
            inSubPage = true
            titleTv.text = subPageTitle
            detailContainer.removeAllViews()
            populate()
            detailScroll.scrollTo(0, 0)
            detailScroll.visibility = View.VISIBLE
            val w = max(resources.displayMetrics.widthPixels.toFloat(), dpF(320f))
            detailScroll.translationX = w * 0.24f
            detailScroll.alpha = 0f
            detailScroll.scaleX = 0.97f
            detailScroll.scaleY = 0.97f
            detailScroll.animate()
                .translationX(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()

            homeScroll.animate()
                .translationX(-w * 0.20f)
                .scaleX(0.94f)
                .scaleY(0.94f)
                .alpha(0.45f)
                .setDuration(280L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
        }

        fun navigateBackToHomeMenu() {
            if (!inSubPage) {
                dlg.dismiss()
                return
            }
            inSubPage = false
            titleTv.text = tr("Настройки", "Settings")
            rebuildHomeMenu()
            val w = max(resources.displayMetrics.widthPixels.toFloat(), dpF(320f))
            homeScroll.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(260L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
            detailScroll.animate()
                .translationX(w * 0.85f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .alpha(0f)
                .setDuration(250L)
                .setInterpolator(aosp17SpatialInterpolator)
                .withEndAction {
                    if (!inSubPage) {
                        detailScroll.visibility = View.GONE
                        detailScroll.translationX = 0f
                        detailScroll.scaleX = 1f
                        detailScroll.scaleY = 1f
                        detailScroll.alpha = 1f
                    }
                }
                .start()
        }

        backBtn.setOnClickListener {
            triggerHaptic(0.8f)
            navigateBackToHomeMenu()
        }

        // Android 14-17 Predictive Back Animation Callback:
        // Previews the previous Settings menu underneath while swiping Back in a sub-page,
        // and returns to the previous Settings menu instead of jumping to the Terminal!
        val predictiveBackCallback = object : OnBackAnimationCallback {
            override fun onBackStarted(backEvent: BackEvent) {
                homeScroll.animate().cancel()
                detailScroll.animate().cancel()
                pageRoot.animate().cancel()
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                val p = backEvent.progress.coerceIn(0f, 1f)
                val w = max(resources.displayMetrics.widthPixels.toFloat(), dpF(320f))
                val dir = if (backEvent.swipeEdge == BackEvent.EDGE_RIGHT) -1f else 1f
                if (inSubPage) {
                    // Reveal homeScroll underneath while sliding detailScroll away!
                    detailScroll.translationX = dir * p * w * 0.68f
                    val sc = 1f - p * 0.08f
                    detailScroll.scaleX = sc
                    detailScroll.scaleY = sc
                    detailScroll.alpha = 1f - p * 0.25f

                    homeScroll.translationX = -dir * w * 0.20f * (1f - p)
                    val hSc = 0.94f + 0.06f * p
                    homeScroll.scaleX = hSc
                    homeScroll.scaleY = hSc
                    homeScroll.alpha = 0.45f + 0.55f * p
                } else {
                    // On root Settings menu: preview returning to Terminal!
                    pageRoot.translationX = dir * p * w * 0.22f
                    val rSc = 1f - p * 0.09f
                    pageRoot.scaleX = rSc
                    pageRoot.scaleY = rSc
                    pageRoot.alpha = 1f - p * 0.35f
                }
            }

            override fun onBackCancelled() {
                val w = max(resources.displayMetrics.widthPixels.toFloat(), dpF(320f))
                if (inSubPage) {
                    detailScroll.animate()
                        .translationX(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(200L)
                        .setInterpolator(aosp17SpatialInterpolator)
                        .start()
                    homeScroll.animate()
                        .translationX(-w * 0.20f)
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .alpha(0.45f)
                        .setDuration(200L)
                        .setInterpolator(aosp17SpatialInterpolator)
                        .start()
                } else {
                    pageRoot.animate()
                        .translationX(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(200L)
                        .setInterpolator(aosp17SpatialInterpolator)
                        .start()
                }
            }

            override fun onBackInvoked() {
                triggerHaptic(0.75f)
                if (inSubPage) {
                    navigateBackToHomeMenu()
                } else {
                    dlg.dismiss()
                }
            }
        }

        dlg.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            predictiveBackCallback
        )
        dlg.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (inSubPage) {
                    triggerHaptic(0.75f)
                    navigateBackToHomeMenu()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        fun applyNewColorThemeEverywhere() {
            refreshColorSpec2026FromCpp()
            pageRoot.setBackgroundColor(uiSurface)
            detailScroll.setBackgroundColor(uiSurface)
            dlg.window?.setBackgroundDrawable(ColorDrawable(uiSurface))
            backBtn.background = tonalBg(uiSurfaceContainer, dp(20))
            rebuildTabs()
            rebuildExtraKeysBar()
            terminalSurface.postInvalidateOnAnimation()
        }

        // Sub-page 1: Цвета и палитра (System Dynamic Monet by default + ColorBlendr 4 styles)
        fun showColorsAndStylesPage() {
            fun populateColorsPage() {
                detailContainer.removeAllViews()
                detailContainer.addView(makeSectionLabel(tr("Источник цвета", "Color source")))

                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.PALETTE,
                        title = tr("Системные цвета (По умолчанию)", "System dynamic colors (Default)"),
                        subtitle = tr("Использовать системную палитру обоев Android", "Use Android system wallpaper Monet colors"),
                        checked = useSystemDynamicColor,
                        isMasterSwitch = true
                    ) { enabled ->
                        useSystemDynamicColor = enabled
                        prefs.edit().putBoolean(KEY_USE_SYSTEM_COLOR, enabled).apply()
                        applyNewColorThemeEverywhere()
                        populateColorsPage()
                    }
                )

                if (!useSystemDynamicColor) {
                    detailContainer.addView(makeSectionLabel(tr("Базовый оттенок", "Seed color")))
                    val swatchCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        background = segmentedCardBg(0, 1, uiSurfaceLow, 0, 24f, 5f)
                    }
                    val (swatchScroll, swatchRow) = makeHorizontalChipRow()
                    for (seedOpt in COLORBLENDR_SEEDS) {
                        val previewTokens = nativeGenerateColorSpec2026Palette(seedOpt.seedArgb, monetStyleIndex)
                        val isSel = (colorSeedArgb == seedOpt.seedArgb)
                        val swatchWrap = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER_HORIZONTAL
                            setPadding(dp(6), dp(4), dp(6), dp(4))
                            attachSmoothPressAnimation(this, 0.90f)
                            setOnClickListener {
                                triggerHaptic(0.85f)
                                colorSeedArgb = seedOpt.seedArgb
                                prefs.edit().putInt(KEY_COLOR_SEED_V2, colorSeedArgb).apply()
                                applyNewColorThemeEverywhere()
                                populateColorsPage()
                            }
                        }
                        val swatchCanvas = ColorPreviewCanvasView(
                            this,
                            tokens = previewTokens,
                            isSwatchWithCenter = true,
                            seedCenterColor = seedOpt.seedArgb,
                            isSwatchSelected = isSel
                        ).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
                        }
                        swatchWrap.addView(swatchCanvas)
                        swatchRow.addView(swatchWrap)
                    }
                    swatchCard.addView(swatchScroll)
                    detailContainer.addView(swatchCard)
                }

                detailContainer.addView(makeSectionLabel("Palette style"))
                val effectiveSeed = if (useSystemDynamicColor) getSystemDynamicSeedColor() else colorSeedArgb
                val enDescriptions = arrayOf(
                    "Balanced palette with soft tonal transitions",
                    "Calm minimalist scheme with a subtle tint",
                    "Energetic palette with high-chroma accents",
                    "Bold hue-shifted triad with deep contrast"
                )
                for ((idx, styleOpt) in MONET_STYLES_2026.withIndex()) {
                    val isSel = (monetStyleIndex == styleOpt.index)
                    val styleTokens = nativeGenerateColorSpec2026Palette(effectiveSeed, styleOpt.index)

                    val styleCard = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(18), dp(15), dp(18), dp(15))
                        val fillCol = if (isSel) styleTokens[2] else uiSurfaceLow
                        background = segmentedCardBg(
                            idx,
                            MONET_STYLES_2026.size,
                            fillCol,
                            0,
                            24f,
                            5f
                        )
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = if (idx < MONET_STYLES_2026.lastIndex) dp(2) else dp(8)
                        }
                        attachSmoothPressAnimation(this, 0.98f)
                        setOnClickListener {
                            if (!isSel) {
                                triggerHaptic(0.9f)
                                monetStyleIndex = styleOpt.index
                                prefs.edit().putInt(KEY_MONET_STYLE_V2, monetStyleIndex).apply()
                                applyNewColorThemeEverywhere()
                                populateColorsPage()
                            }
                        }
                    }

                    styleCard.addView(
                        ColorPreviewCanvasView(this, tokens = styleTokens).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                                marginEnd = dp(16)
                            }
                        }
                    )

                    val textCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    textCol.addView(TextView(this).apply {
                        text = styleOpt.title
                        setTextColor(if (isSel) styleTokens[3] else COLOR_TEXT_BRIGHT)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    })
                    textCol.addView(TextView(this).apply {
                        text = tr(styleOpt.description, enDescriptions[idx])
                        setTextColor(if (isSel) withAlpha(styleTokens[3], 205) else COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(0, dp(3), 0, 0)
                    })
                    styleCard.addView(textCol)
                    detailContainer.addView(styleCard)
                }
            }

            openSubPageWithTransition(tr("Цвета и палитра", "Colors & palette")) {
                populateColorsPage()
            }
        }

        // Sub-page 2: Шрифт, язык и терминал (System Font + Expanded Terminal Settings + Full EN/RU)
        fun showFontAndDisplayPage() {
            fun populateFontPage() {
                detailContainer.removeAllViews()

                val discoveredRaw = nativeDiscoverCustomFonts()
                val discoveredFonts = ArrayList<Pair<String, String>>()
                for (line in discoveredRaw.lineSequence()) {
                    if (line.isBlank()) continue
                    val p = line.split("\t")
                    if (p.size >= 2) discoveredFonts.add(p[0] to p[1])
                }

                val fontTitles = mutableListOf(
                    tr("Системный шрифт устройства", "System UI font"),
                    tr("Системный моноширинный", "System Monospace"),
                    "JetBrains Mono Regular",
                    "JetBrains Mono Light",
                    "JetBrains Mono Medium",
                    "JetBrains Mono SemiBold",
                    "JetBrains Mono Bold"
                )
                val fontDescs = mutableListOf<String?>(
                    tr("Шрифт вашей системы Android по умолчанию", "Default Android system-wide font"),
                    tr("Встроенный системный моноширинный шрифт", "Built-in Android system monospace font"),
                    tr("Классическое начертание (400)", "Standard developer weight (400)"),
                    tr("Тонкое начертание (300)", "Light weight (300)"),
                    tr("Средняя насыщенность (500)", "Medium weight (500)"),
                    tr("Полужирное начертание (600)", "SemiBold weight (600)"),
                    tr("Жирное начертание (700)", "Bold weight (700)")
                )
                for ((fName, fPath) in discoveredFonts) {
                    fontTitles.add(tr("Файл: $fName", "File: $fName"))
                    fontDescs.add(fPath)
                }

                val curPreset = prefs.getInt(KEY_FONT_PRESET, 2)
                val curCustomPath = prefs.getString(KEY_CUSTOM_FONT_PATH, "") ?: ""
                val selectedIdx = if (curPreset == 7 && curCustomPath.isNotEmpty()) {
                    val found = discoveredFonts.indexOfFirst { it.second == curCustomPath }
                    if (found >= 0) 7 + found else 2
                } else {
                    curPreset.coerceIn(0, 6)
                }

                detailContainer.addView(makeSectionLabel(tr("Шрифт и язык", "Font & language")))

                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.FONT,
                        title = tr("Шрифт терминала", "Terminal font"),
                        subtitle = tr("Системный шрифт Android, JetBrains Mono или свой .ttf/.otf", "System font, JetBrains Mono, or custom .ttf/.otf"),
                        items = fontTitles,
                        descriptions = fontDescs,
                        selectedIndex = selectedIdx,
                        index = 0,
                        total = 3
                    ) { chosen ->
                        if (chosen <= 6) {
                            prefs.edit()
                                .putInt(KEY_FONT_PRESET, chosen)
                                .putString(KEY_CUSTOM_FONT_PATH, "")
                                .apply()
                        } else {
                            val pair = discoveredFonts[chosen - 7]
                            prefs.edit()
                                .putInt(KEY_FONT_PRESET, 7)
                                .putString(KEY_CUSTOM_FONT_PATH, pair.second)
                                .apply()
                        }
                        terminalSurface.reloadTerminalFont()
                        populateFontPage()
                    }
                )

                val ligsOn = prefs.getBoolean(KEY_FONT_LIGATURES, false)
                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.TERMINAL,
                        title = tr("Лигатуры шрифта", "Font ligatures"),
                        subtitle = tr("Объединять операторы !=, ->, =>, :: в единые символы", "Combine operators !=, ->, =>, :: into single glyphs"),
                        checked = ligsOn,
                        index = 1,
                        total = 3
                    ) { enabled ->
                        prefs.edit().putBoolean(KEY_FONT_LIGATURES, enabled).apply()
                        terminalSurface.reloadTerminalFont()
                    }
                )

                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.HUB_GRID,
                        title = tr("Язык интерфейса", "App language"),
                        subtitle = tr("Язык всех меню, диалогов и уведомлений", "Language of all menus, dialogs, and notifications"),
                        items = listOf(
                            tr("Системный (Авто)", "System default"),
                            "Русский",
                            "English"
                        ),
                        selectedIndex = appLangPref.coerceIn(0, 2),
                        index = 2,
                        total = 3
                    ) { chosen ->
                        appLangPref = chosen
                        prefs.edit().putInt(KEY_APP_LANG, chosen).apply()
                        titleTv.text = tr("Шрифт и терминал", "Font & terminal")
                        startForegroundKeepAlive()
                        populateFontPage()
                    }
                )

                detailContainer.addView(makeSectionLabel(tr("Масштаб и интервалы", "Scale & spacing")))

                val curScalePct = (terminalSurface.fontScale * 100f).roundToInt().coerceIn(50, 220)
                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.SLIDERS,
                        labelPrefix = tr("Масштаб шрифта", "Font scale"),
                        initialProgress = (curScalePct - 50) / 5,
                        maxProgress = 34,
                        index = 0,
                        total = 2,
                        formatValue = { "${50 + it * 5}%" },
                        onChanged = { p ->
                            val newScale = (50 + p * 5) / 100f
                            terminalSurface.setCustomFontScale(newScale)
                            prefs.edit().putFloat(KEY_FONT_SCALE, newScale).apply()
                        }
                    )
                )

                val curLineSpacing = prefs.getInt(KEY_LINE_SPACING_PCT, 100).coerceIn(85, 135)
                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.CURSOR,
                        labelPrefix = tr("Межстрочный интервал", "Line height"),
                        initialProgress = curLineSpacing - 85,
                        maxProgress = 50,
                        index = 1,
                        total = 2,
                        formatValue = { "${85 + it}%" },
                        onChanged = { p ->
                            val pct = (85 + p).coerceIn(85, 135)
                            prefs.edit().putInt(KEY_LINE_SPACING_PCT, pct).apply()
                            terminalSurface.lineSpacingPct = pct
                            terminalSurface.reloadTerminalFont()
                        }
                    )
                )

                detailContainer.addView(makeSectionLabel(tr("Поведение терминала", "Terminal behavior")))

                val sbOptions = listOf(2000, 4000, 10000, 25000)
                val curSb = prefs.getInt(KEY_SCROLLBACK_LINES, 4000)
                val sbIdx = sbOptions.indexOf(curSb).let { if (it >= 0) it else 1 }
                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.STORAGE,
                        title = tr("Буфер прокрутки (Scrollback)", "Scrollback buffer"),
                        subtitle = tr("Количество сохраняемых строк истории вывода", "Number of history lines kept in memory"),
                        items = sbOptions.map { tr("$it строк", "$it lines") },
                        selectedIndex = sbIdx,
                        index = 0,
                        total = 3
                    ) { chosen ->
                        val lines = sbOptions[chosen]
                        prefs.edit().putInt(KEY_SCROLLBACK_LINES, lines).apply()
                        nativeSetMaxScrollback(lines)
                        populateFontPage()
                    }
                )

                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.COPY,
                        title = tr("Автокопирование при выделении", "Copy on select"),
                        subtitle = tr("Автоматически копировать выделенный текст в буфер обмена", "Automatically copy selected text to clipboard"),
                        checked = copyOnSelect,
                        index = 1,
                        total = 3
                    ) { enabled ->
                        copyOnSelect = enabled
                        prefs.edit().putBoolean(KEY_COPY_ON_SELECT, enabled).apply()
                    }
                )

                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.EYE,
                        title = tr("Не выключать экран", "Keep screen on"),
                        subtitle = tr("Удерживать подсветку экрана во время работы терминала", "Prevent screen from sleeping while terminal is active"),
                        checked = keepScreenOnPref,
                        index = 2,
                        total = 3
                    ) { enabled ->
                        keepScreenOnPref = enabled
                        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
                        if (enabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                )
            }

            openSubPageWithTransition(tr("Шрифт и терминал", "Font & terminal")) {
                populateFontPage()
            }
        }

        // Sub-page 3: Курсор и анимации (VS Code)
        fun showCursorCategory() {
            fun populateCursorPage() {
                detailContainer.removeAllViews()
                detailContainer.addView(makeSectionLabel(tr("Поведение каретки", "Caret behavior")))

                val shapeNames = listOf(
                    tr("Блок (Скруглённый)", "Block (Rounded)"),
                    tr("Нижняя линия (Underline)", "Underline"),
                    tr("Вертикальный луч (Bar)", "Vertical Bar"),
                    tr("Полый контур (Outline)", "Outline Block")
                )
                body@ detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.CURSOR,
                        title = tr("Форма курсора", "Cursor shape"),
                        subtitle = tr("Геометрия отображения каретки в терминале", "Caret geometry in terminal"),
                        items = shapeNames,
                        selectedIndex = terminalSurface.cursorShapePref.coerceIn(0, 3),
                        index = 0,
                        total = 3
                    ) { chosen ->
                        terminalSurface.cursorShapePref = chosen
                        prefs.edit().putInt(KEY_CURSOR_SHAPE, chosen).apply()
                        nativeSetDefaultCursorShape(min(2, chosen))
                        terminalSurface.resetScrollAndBlink()
                        populateCursorPage()
                    }
                )

                val blinkNames = listOf(
                    "Smooth (VS Code)",
                    "Phase (VS Code)",
                    "Expand (VS Code)",
                    "Blink",
                    "Solid"
                )
                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.SLIDERS,
                        title = tr("Анимация мигания", "Cursor blinking style"),
                        subtitle = tr("Кривая затухания и пульсации каретки", "Caret fade and pulse curve"),
                        items = blinkNames,
                        selectedIndex = terminalSurface.cursorBlinkStyle.coerceIn(0, 4),
                        index = 1,
                        total = 3
                    ) { chosen ->
                        terminalSurface.cursorBlinkStyle = chosen
                        prefs.edit().putInt(KEY_CURSOR_BLINK_STYLE, chosen).apply()
                        terminalSurface.resetScrollAndBlink()
                        populateCursorPage()
                    }
                )

                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.PING,
                        title = tr("Плавное скольжение (Smooth Caret)", "Smooth caret animation"),
                        subtitle = tr("Пружинная интерполяция движения каретки как в VS Code", "Spring interpolation between characters like VS Code"),
                        checked = terminalSurface.cursorSmoothMove,
                        index = 2,
                        total = 3
                    ) { next ->
                        terminalSurface.cursorSmoothMove = next
                        prefs.edit().putBoolean(KEY_CURSOR_SMOOTH_MOVE, next).apply()
                    }
                )

                detailContainer.addView(makeSectionLabel(tr("Параметры анимации", "Animation parameters")))

                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.EYE,
                        labelPrefix = tr("Непрозрачность курсора", "Cursor opacity"),
                        initialProgress = terminalSurface.cursorAlphaPct - 15,
                        maxProgress = 85,
                        index = 0,
                        total = 4,
                        formatValue = { "${it + 15}%" },
                        onChanged = { p ->
                            val pct = (p + 15).coerceIn(15, 100)
                            terminalSurface.cursorAlphaPct = pct
                            prefs.edit().putInt(KEY_CURSOR_ALPHA_PCT, pct).apply()
                            terminalSurface.postInvalidateOnAnimation()
                        }
                    )
                )

                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.CURSOR,
                        labelPrefix = tr("Радиус скругления углов", "Corner radius"),
                        initialProgress = terminalSurface.cursorRadiusTenths,
                        maxProgress = 60,
                        index = 1,
                        total = 4,
                        formatValue = { "${it / 10}.${it % 10} dp" },
                        onChanged = { p ->
                            terminalSurface.cursorRadiusTenths = p
                            prefs.edit().putInt(KEY_CURSOR_RADIUS_TENTHS, p).apply()
                            terminalSurface.postInvalidateOnAnimation()
                        }
                    )
                )

                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.SLIDERS,
                        labelPrefix = tr("Скорость затухания", "Fade duration"),
                        initialProgress = (terminalSurface.cursorFadeMs - 200) / 10,
                        maxProgress = 100,
                        index = 2,
                        total = 4,
                        formatValue = { "${200 + it * 10} ms" },
                        onChanged = { p ->
                            val ms = 200 + p * 10
                            terminalSurface.cursorFadeMs = ms
                            prefs.edit().putInt(KEY_CURSOR_FADE_MS, ms).apply()
                            terminalSurface.resetScrollAndBlink()
                        }
                    )
                )

                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.PING,
                        labelPrefix = tr("Длительность скольжения", "Glide duration"),
                        initialProgress = (terminalSurface.cursorGlideMs - 30) / 5,
                        maxProgress = 38,
                        index = 3,
                        total = 4,
                        formatValue = { "${30 + it * 5} ms" },
                        onChanged = { p ->
                            val ms = 30 + p * 5
                            terminalSurface.cursorGlideMs = ms
                            prefs.edit().putInt(KEY_CURSOR_GLIDE_MS, ms).apply()
                        }
                    )
                )
            }

            openSubPageWithTransition(tr("Курсор и анимации", "Cursor & animations")) {
                populateCursorPage()
            }
        }

        // Sub-page 4: Клавиатура и бинды
        fun showKeyboardCategory() {
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

            fun populateKeyboardPage() {
                detailContainer.removeAllViews()
                val targetList = if (activeEditRow == 0) row1Tokens else row2Tokens

                fun loadPreset(specStr: String) {
                    row1Tokens.clear()
                    row2Tokens.clear()
                    val ls = specStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    if (ls.isNotEmpty()) row1Tokens.addAll(ls[0].split("|"))
                    if (ls.size > 1) row2Tokens.addAll(ls[1].split("|"))
                    persistRows()
                    populateKeyboardPage()
                }

                val curPresetIdx = when {
                    row1Tokens.isEmpty() && row2Tokens.isEmpty() -> 3
                    row2Tokens.isNotEmpty() -> 1
                    row1Tokens.joinToString("|") == VIM_TMUX_EXTRA_KEYS_SPEC -> 2
                    else -> 0
                }

                detailContainer.addView(makeSectionLabel(tr("Раскладка панели", "Extra keys layout")))
                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.KEYBOARD,
                        title = tr("Готовый профиль раскладки", "Layout preset"),
                        subtitle = tr("Выберите шаблон нижней панели клавиш", "Choose extra keys bar template"),
                        items = listOf(
                            tr("1 ряд (Компактная)", "1 row (Compact)"),
                            tr("2 ряда (Расширенная)", "2 rows (Extended)"),
                            "Vim / Tmux",
                            tr("Скрыть нижнюю панель", "Hide extra keys bar")
                        ),
                        selectedIndex = curPresetIdx,
                        index = 0,
                        total = 2
                    ) { chosen ->
                        when (chosen) {
                            0 -> loadPreset(DEFAULT_EXTRA_KEYS_SPEC)
                            1 -> loadPreset(EXTENDED_EXTRA_KEYS_SPEC)
                            2 -> loadPreset(VIM_TMUX_EXTRA_KEYS_SPEC)
                            else -> loadPreset("")
                        }
                    }
                )

                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.SLIDERS,
                        title = tr("Редактируемый ряд", "Edited row"),
                        subtitle = tr("Выберите ряд для настройки кнопок", "Select row to customize"),
                        items = listOf(
                            tr("Ряд 1 (${row1Tokens.size})", "Row 1 (${row1Tokens.size})"),
                            tr("Ряд 2 (${row2Tokens.size})", "Row 2 (${row2Tokens.size})")
                        ),
                        selectedIndex = activeEditRow,
                        index = 1,
                        total = 2
                    ) { chosen ->
                        activeEditRow = chosen
                        populateKeyboardPage()
                    }
                )

                detailContainer.addView(makeSectionLabel(tr("Кнопки ряда ${activeEditRow + 1}", "Row ${activeEditRow + 1} buttons")))
                val activeKeysCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    background = segmentedCardBg(0, 2, uiSurfaceLow, 0, 22f, 5f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(2)
                    }
                }
                val (curScroll, curRow) = makeHorizontalChipRow()
                if (targetList.isEmpty()) {
                    curRow.addView(TextView(this).apply {
                        text = tr("Ряд пуст", "Row is empty")
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                        setPadding(dp(4), dp(4), dp(4), dp(4))
                    })
                } else {
                    for (idx in targetList.indices) {
                        val (disp, _) = parseKeyToken(targetList[idx])
                        curRow.addView(makeExpressiveChip("$disp ✕", true) {
                            targetList.removeAt(idx)
                            persistRows()
                            populateKeyboardPage()
                        })
                    }
                }
                activeKeysCard.addView(curScroll)

                val actActionsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(8), 0, 0)
                }
                actActionsRow.addView(makeExpressiveChip(tr("+ Своя клавиша", "+ Custom key"), true) {
                    val (bindDlg, bindSheet) = createExpressiveDialog(390)
                    bindSheet.addView(TextView(this).apply {
                        text = tr("Привязка клавиши или команды", "Bind key or command")
                        setTextColor(COLOR_TEXT_BRIGHT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(0, 0, 0, dp(10))
                    })
                    val lblEdit = EditText(this).apply {
                        hint = tr("Надпись (напр. C-c или top)", "Label (e.g. C-c or top)")
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        background = tonalBg(uiSurfaceLow, dp(14))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        setSingleLine(true)
                    }
                    val actEdit = EditText(this).apply {
                        hint = tr("Действие (CTRL+C, ALT+B, htop\\n)", "Action (CTRL+C, ALT+B, htop\\n)")
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        background = tonalBg(uiSurfaceLow, dp(14))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        setSingleLine(true)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(10)
                        }
                    }
                    bindSheet.addView(lblEdit)
                    bindSheet.addView(actEdit)

                    val btnBar = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(14), 0, 0)
                    }
                    btnBar.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { bindDlg.dismiss() })
                    btnBar.addView(makeExpressiveChip(tr("Добавить", "Add"), true) {
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
                            populateKeyboardPage()
                        }
                        bindDlg.dismiss()
                    })
                    bindSheet.addView(btnBar)
                    bindDlg.show()
                })
                actActionsRow.addView(makeExpressiveChip(tr("Очистить", "Clear row"), false) {
                    targetList.clear()
                    persistRows()
                    populateKeyboardPage()
                })
                activeKeysCard.addView(actActionsRow)
                detailContainer.addView(activeKeysCard)

                val catalogCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    background = segmentedCardBg(1, 2, uiSurfaceLow, 0, 22f, 5f)
                }
                catalogCard.addView(TextView(this).apply {
                    text = tr("Быстрое добавление клавиш", "Quick add keys")
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setPadding(0, 0, 0, dp(6))
                })
                val (pScroll, pRow) = makeHorizontalChipRow()
                val keys = listOf(
                    "ESC", "TAB", "CTRL", "ALT", "ENTER", "BKSP", "DEL", "SPC",
                    "▲", "▼", "◀", "▶", "HOME", "END", "PGUP", "PGDN",
                    "-", "/", "|", "\\", "_", "~", ":", ";", "\"", "'",
                    "C-c::CTRL+C", "C-z::CTRL+Z", "C-d::CTRL+D", "C-l::CTRL+L",
                    "F1", "F2", "F3", "F4", "F5", "F10", "F12"
                )
                for (k in keys) {
                    val (d, _) = parseKeyToken(k)
                    pRow.addView(makeExpressiveChip("+ $d", false) {
                        targetList.add(k)
                        persistRows()
                        populateKeyboardPage()
                    })
                }
                catalogCard.addView(pScroll)
                detailContainer.addView(catalogCard)
            }

            openSubPageWithTransition(tr("Клавиатура и бинды", "Keyboard & bindings")) {
                populateKeyboardPage()
            }
        }

        // Sub-page 5: Тактильный отклик (Вибрация)
        fun showHapticsCategory() {
            fun populateHapticsPage() {
                detailContainer.removeAllViews()

                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.VIBRATION,
                        title = tr("Тактильный отклик", "Haptic feedback"),
                        subtitle = if (hapticEnabled) tr("Вибрация активна", "Haptics enabled") else tr("Вибрация отключена", "Haptics disabled"),
                        checked = hapticEnabled,
                        isMasterSwitch = true
                    ) { next ->
                        hapticEnabled = next
                        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, next).apply()
                        if (next) triggerHaptic(1.0f)
                        populateHapticsPage()
                    }
                )

                val curProfIdx = when {
                    hapticDurationMs == 6 && hapticAmplitude in 70..85 -> 0
                    hapticDurationMs == 8 && hapticAmplitude in 130..150 -> 1
                    hapticDurationMs == 14 && hapticAmplitude >= 220 -> 2
                    else -> 3
                }
                detailContainer.addView(
                    makeBakaSuChooseCard(
                        icon = M3Icon.PING,
                        title = tr("Профиль вибрации", "Haptic profile"),
                        subtitle = tr("Характер тактильного отклика", "Tactile response preset"),
                        items = listOf(
                            tr("Мягкий (6 мс • 30%)", "Soft (6 ms • 30%)"),
                            tr("Упругий (8 мс • 55%)", "Crisp (8 ms • 55%)"),
                            tr("Мощный (14 мс • 90%)", "Strong (14 ms • 90%)"),
                            tr("Пользовательский", "Custom")
                        ),
                        selectedIndex = curProfIdx,
                        index = 0,
                        total = 3
                    ) { chosen ->
                        when (chosen) {
                            0 -> {
                                hapticEnabled = true
                                hapticDurationMs = 6
                                hapticAmplitude = 76
                            }
                            1 -> {
                                hapticEnabled = true
                                hapticDurationMs = 8
                                hapticAmplitude = 140
                            }
                            2 -> {
                                hapticEnabled = true
                                hapticDurationMs = 14
                                hapticAmplitude = 230
                            }
                        }
                        prefs.edit()
                            .putBoolean(KEY_HAPTIC_ENABLED, hapticEnabled)
                            .putInt(KEY_HAPTIC_DURATION_MS, hapticDurationMs)
                            .putInt(KEY_HAPTIC_AMPLITUDE, hapticAmplitude)
                            .apply()
                        triggerHaptic(1.0f)
                        populateHapticsPage()
                    }
                )

                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.VIBRATION,
                        labelPrefix = tr("Мощность импульса", "Impulse strength"),
                        initialProgress = hapticAmplitude - 1,
                        maxProgress = 254,
                        index = 1,
                        total = 3,
                        formatValue = { "${((it + 1) * 100f / 255f).roundToInt()}%" },
                        onChanged = { p ->
                            hapticAmplitude = (p + 1).coerceIn(1, 255)
                            prefs.edit().putInt(KEY_HAPTIC_AMPLITUDE, hapticAmplitude).apply()
                        }
                    )
                )

                detailContainer.addView(
                    makeSeekBarCard(
                        icon = M3Icon.SLIDERS,
                        labelPrefix = tr("Длительность импульса", "Impulse duration"),
                        initialProgress = hapticDurationMs - 1,
                        maxProgress = 39,
                        index = 2,
                        total = 3,
                        formatValue = { "${it + 1} ms" },
                        onChanged = { p ->
                            hapticDurationMs = (p + 1).coerceIn(1, 40)
                            prefs.edit().putInt(KEY_HAPTIC_DURATION_MS, hapticDurationMs).apply()
                        }
                    )
                )
            }

            openSubPageWithTransition(tr("Тактильный отклик", "Haptic feedback")) {
                populateHapticsPage()
            }
        }

        // Clean borderless category card WITHOUT colored current-setting value badges!
        fun makeMainSettingsNavCard(
            title: String,
            subtitle: String,
            icon: M3Icon,
            index: Int,
            total: Int,
            onClick: () -> Unit
        ): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = segmentedCardBg(index, total, uiSurfaceLow, 0, 24f, 5f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = if (index < total - 1) dp(2) else dp(10)
                }

                addView(
                    FrameLayout(this@TermuxActivity).apply {
                        background = tonalBg(uiAccentContainer, dp(14))
                        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                            marginEnd = dp(16)
                        }
                        addView(MaterialIconView(this@TermuxActivity, icon, uiAccent, 22f, 2.1f))
                    }
                )

                val textCol = LinearLayout(this@TermuxActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(10)
                    }
                }
                textCol.addView(TextView(this@TermuxActivity).apply {
                    text = title
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    typeface = Typeface.DEFAULT_BOLD
                })
                textCol.addView(TextView(this@TermuxActivity).apply {
                    text = subtitle
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(2), 0, 0)
                })
                addView(textCol)

                addView(
                    MaterialIconView(this@TermuxActivity, M3Icon.CHEVRON_RIGHT, COLOR_TEXT_DIM, 20f, 2.0f).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
                    }
                )

                attachSmoothPressAnimation(this, 0.98f)
                setOnClickListener {
                    triggerHaptic(0.9f)
                    onClick()
                }
            }
        }

        rebuildHomeMenu = {
            homeContainer.removeAllViews()

            homeContainer.addView(makeSectionLabel(tr("Оформление и интерфейс", "Appearance & UI")))

            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Цвета и палитра", "Colors & palette"),
                    subtitle = tr("Системные цвета и стили палитры", "System colors and palette styles"),
                    icon = M3Icon.PALETTE,
                    index = 0,
                    total = 2
                ) { showColorsAndStylesPage() }
            )

            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Шрифт и терминал", "Font & terminal"),
                    subtitle = tr("Шрифт, язык, масштаб и параметры терминала", "Font, language, scale, and terminal options"),
                    icon = M3Icon.FONT,
                    index = 1,
                    total = 2
                ) { showFontAndDisplayPage() }
            )

            homeContainer.addView(makeSectionLabel(tr("Ввод и отклик", "Input & feedback")))

            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Курсор и анимации", "Cursor & animations"),
                    subtitle = tr("Форма каретки, плавное скольжение и затухание", "Caret shape, smooth glide, and fade timing"),
                    icon = M3Icon.CURSOR,
                    index = 0,
                    total = 3
                ) { showCursorCategory() }
            )

            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Клавиатура и бинды", "Keyboard & bindings"),
                    subtitle = tr("Настройка рядов нижней панели и макросов", "Customize extra keys rows and macros"),
                    icon = M3Icon.KEYBOARD,
                    index = 1,
                    total = 3
                ) { showKeyboardCategory() }
            )

            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Тактильный отклик", "Haptic feedback"),
                    subtitle = tr("Настройка мощности и длительности вибрации", "Configure vibration strength and duration"),
                    icon = M3Icon.VIBRATION,
                    index = 2,
                    total = 3
                ) { showHapticsCategory() }
            )
        }

        rebuildHomeMenu()
        animateAosp17PageTransition(homeContainer, forward = true)
        dlg.setContentView(pageRoot)
        dlg.show()
    }

    // ========================================================================
    // Safe WindowInsets, BakaSU Morphing LoadingIndicator & Material 3 Icons
    // ========================================================================
    internal fun applySafeWindowInsets(
        targetView: View,
        extraLeftDp: Int = 0,
        extraTopDp: Int = 6,
        extraRightDp: Int = 0,
        extraBottomDp: Int = 6
    ) {
        val fallbackStatusBarPx = try {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resId > 0) resources.getDimensionPixelSize(resId) else dp(34)
        } catch (_: Throwable) {
            dp(34)
        }
        val rootInsets = window?.decorView?.rootWindowInsets
        val initSys = rootInsets?.getInsets(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
        )
        val initTop = max(initSys?.top ?: 0, fallbackStatusBarPx) + dp(extraTopDp)
        val initBot = (initSys?.bottom ?: 0) + dp(extraBottomDp)
        val initLeft = (initSys?.left ?: 0) + dp(extraLeftDp)
        val initRight = (initSys?.right ?: 0) + dp(extraRightDp)
        targetView.setPadding(initLeft, initTop, initRight, initBot)

        targetView.setOnApplyWindowInsetsListener { v, insets ->
            val sysBars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsets.Type.ime())
            val safeTop = max(sysBars.top, fallbackStatusBarPx) + dp(extraTopDp)
            val safeBottom = max(sysBars.bottom, ime.bottom) + dp(extraBottomDp)
            v.setPadding(
                sysBars.left + dp(extraLeftDp),
                safeTop,
                sysBars.right + dp(extraRightDp),
                safeBottom
            )
            insets
        }
        targetView.post { targetView.requestApplyInsets() }
    }

    /**
     * Exact Material 3 Expressive `PullToRefreshDefaults.LoadingIndicator` morphing shape view
     * used in BakaSU (`DynamicManagerScreen.kt`, `ModulePage.kt`).
     * Smoothly morphs between 4-lobed clover, 7-pointed starburst cookie, 5-lobed flower,
     * 6-lobed seal, and rounded triangle while rotating inside a borderless primaryContainer circle.
     */
    internal inner class BakaSuMorphingIndicatorView(context: Context) : View(context) {
        var morphPhase: Float = 0f
            set(value) {
                field = value
                invalidate()
            }

        private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val morphPath = Path()

        // Lobe counts for Material 3 Expressive LoadingIndicator sequence
        private val lobes = intArrayOf(4, 7, 5, 6, 3)
        private val amps = floatArrayOf(0.16f, 0.18f, 0.15f, 0.14f, 0.14f)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width * 0.5f
            val cy = height * 0.5f
            val baseR = min(cx, cy) * 0.58f
            if (baseR <= 1f) return

            val totalStages = lobes.size
            val norm = ((morphPhase % totalStages) + totalStages) % totalStages
            val idx0 = norm.toInt() % totalStages
            val idx1 = (idx0 + 1) % totalStages
            val rawT = norm - norm.toInt()
            // Smoothstep interpolation between shape keyframes
            val t = rawT * rawT * (3f - 2f * rawT)

            val k0 = lobes[idx0].toFloat()
            val k1 = lobes[idx1].toFloat()
            val a0 = amps[idx0]
            val a1 = amps[idx1]
            val rotRad = morphPhase * 1.35f

            morphPath.reset()
            val steps = 72
            for (i in 0..steps) {
                val theta = (i.toFloat() / steps.toFloat()) * (2f * Math.PI.toFloat())
                val w0 = 1f + a0 * cos(k0 * theta)
                val w1 = 1f + a1 * cos(k1 * theta)
                val r = baseR * (w0 * (1f - t) + w1 * t)
                val px = cx + r * cos(theta + rotRad)
                val py = cy + r * sin(theta + rotRad)
                if (i == 0) morphPath.moveTo(px, py) else morphPath.lineTo(px, py)
            }
            morphPath.close()
            shapePaint.color = uiAccent
            canvas.drawPath(morphPath, shapePaint)
        }
    }

    internal inner class BakaSuPullToRefreshLayout(
        context: Context,
        val scrollView: ScrollView,
        private val onRefreshTriggered: () -> Unit
    ) : FrameLayout(context) {

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val triggerDistancePx = dpF(68f)
        private val maxPullPx = dpF(122f)

        private var downX = 0f
        private var downY = 0f
        private var pullStartY = 0f
        private var isDragging = false
        private var isRefreshing = false
        private var thresholdCrossed = false
        private var currentPullPx = 0f

        private val indicatorBadge: FrameLayout
        private val morphIndicator: BakaSuMorphingIndicatorView

        init {
            clipChildren = false
            clipToPadding = false
            addView(
                scrollView,
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            morphIndicator = BakaSuMorphingIndicatorView(context)
            indicatorBadge = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(uiAccentContainer)
                }
                elevation = dpF(8f)
                layoutParams = LayoutParams(dp(46), dp(46), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                translationY = -dpF(56f)
                scaleX = 0.2f
                scaleY = 0.2f
                alpha = 0f
                addView(
                    morphIndicator,
                    LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER)
                )
            }
            addView(indicatorBadge)
        }

        private fun isScrolledToTop(): Boolean {
            return !scrollView.canScrollVertically(-1) && scrollView.scrollY <= 0
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (isRefreshing) return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    downY = ev.y
                    isDragging = false
                    thresholdCrossed = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isScrolledToTop()) {
                        val dy = ev.y - downY
                        val dx = abs(ev.x - downX)
                        if (dy > touchSlop && dy > dx * 1.15f) {
                            isDragging = true
                            pullStartY = ev.y
                            return true
                        }
                    }
                }
            }
            return super.onInterceptTouchEvent(ev)
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (isRefreshing) return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) {
                        if (isScrolledToTop() && ev.y - downY > touchSlop) {
                            isDragging = true
                            pullStartY = ev.y
                        } else {
                            return super.onTouchEvent(ev)
                        }
                    }
                    val rawDelta = max(0f, ev.y - pullStartY)
                    currentPullPx = (rawDelta * 0.48f).coerceAtMost(maxPullPx)
                    val progress = (currentPullPx / triggerDistancePx).coerceIn(0f, 1.35f)

                    indicatorBadge.translationY = -dpF(48f) + currentPullPx
                    val sc = (0.25f + progress * 0.75f).coerceIn(0.25f, 1.05f)
                    indicatorBadge.scaleX = sc
                    indicatorBadge.scaleY = sc
                    indicatorBadge.alpha = (progress * 1.25f).coerceIn(0f, 1f)
                    morphIndicator.morphPhase = progress * 2.2f
                    scrollView.translationY = currentPullPx * 0.34f

                    if (progress >= 1.0f && !thresholdCrossed) {
                        thresholdCrossed = true
                        triggerHaptic(1.1f)
                    } else if (progress < 0.94f && thresholdCrossed) {
                        thresholdCrossed = false
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        isDragging = false
                        val triggered = (currentPullPx >= triggerDistancePx) && (ev.actionMasked == MotionEvent.ACTION_UP)
                        currentPullPx = 0f
                        if (triggered) {
                            isRefreshing = true
                            indicatorBadge.animate()
                                .translationY(dpF(14f))
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .alpha(1.0f)
                                .setDuration(180L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                            scrollView.animate()
                                .translationY(dpF(26f))
                                .setDuration(180L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                            val startPhase = morphIndicator.morphPhase
                            ValueAnimator.ofFloat(startPhase, startPhase + 3.6f).apply {
                                duration = 560L
                                interpolator = DecelerateInterpolator()
                                addUpdateListener { anim ->
                                    morphIndicator.morphPhase = anim.animatedValue as Float
                                }
                                start()
                            }
                            postDelayed({
                                onRefreshTriggered()
                                isRefreshing = false
                                indicatorBadge.animate()
                                    .translationY(-dpF(56f))
                                    .scaleX(0.2f)
                                    .scaleY(0.2f)
                                    .alpha(0f)
                                    .setDuration(230L)
                                    .setInterpolator(aosp17SpatialInterpolator)
                                    .start()
                                scrollView.animate()
                                    .translationY(0f)
                                    .setDuration(230L)
                                    .setInterpolator(aosp17SpatialInterpolator)
                                    .start()
                            }, 520L)
                        } else {
                            indicatorBadge.animate()
                                .translationY(-dpF(56f))
                                .scaleX(0.2f)
                                .scaleY(0.2f)
                                .alpha(0f)
                                .setDuration(220L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                            scrollView.animate()
                                .translationY(0f)
                                .setDuration(220L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        }
                        return true
                    }
                }
            }
            return super.onTouchEvent(ev)
        }
    }

    internal inner class MaterialIconView(
        context: Context,
        var icon: M3Icon,
        var tintColor: Int,
        private val iconSizeDp: Float = 22f,
        private val strokeWidthDp: Float = 2.15f
    ) : View(context) {
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val path = Path()
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val sizePx = min(width.toFloat(), height.toFloat()).coerceAtMost(dpF(iconSizeDp))
            if (sizePx <= 0f) return
            val ox = (width - sizePx) * 0.5f
            val oy = (height - sizePx) * 0.5f
            val u = sizePx / 24f

            strokePaint.color = tintColor
            strokePaint.strokeWidth = max(1.6f, dpF(strokeWidthDp))
            fillPaint.color = tintColor
            path.reset()

            fun x(v: Float) = ox + v * u
            fun y(v: Float) = oy + v * u

            when (icon) {
                M3Icon.ADD -> {
                    canvas.drawLine(x(12f), y(5f), x(12f), y(19f), strokePaint)
                    canvas.drawLine(x(5f), y(12f), x(19f), y(12f), strokePaint)
                }
                M3Icon.ROOT_HASH -> {
                    canvas.drawLine(x(9.5f), y(4.5f), x(7.5f), y(19.5f), strokePaint)
                    canvas.drawLine(x(16.5f), y(4.5f), x(14.5f), y(19.5f), strokePaint)
                    canvas.drawLine(x(4.5f), y(9.5f), x(19.5f), y(9.5f), strokePaint)
                    canvas.drawLine(x(4.5f), y(14.5f), x(19.5f), y(14.5f), strokePaint)
                }
                M3Icon.HUB_GRID -> {
                    val r = 2.2f * u
                    rect.set(x(3.5f), y(3.5f), x(10.2f), y(10.2f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(13.8f), y(3.5f), x(20.5f), y(10.2f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(3.5f), y(13.8f), x(10.2f), y(20.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(13.8f), y(13.8f), x(20.5f), y(20.5f))
                    fillPaint.alpha = 70
                    canvas.drawRoundRect(rect, r, r, fillPaint)
                    fillPaint.alpha = 255
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                }
                M3Icon.SETTINGS -> {
                    canvas.drawLine(x(4f), y(7f), x(20f), y(7f), strokePaint)
                    canvas.drawLine(x(4f), y(17f), x(20f), y(17f), strokePaint)
                    fillPaint.color = COLOR_SURFACE
                    canvas.drawCircle(x(9f), y(7f), 2.8f * u, fillPaint)
                    canvas.drawCircle(x(15f), y(17f), 2.8f * u, fillPaint)
                    canvas.drawCircle(x(9f), y(7f), 2.8f * u, strokePaint)
                    canvas.drawCircle(x(15f), y(17f), 2.8f * u, strokePaint)
                }
                M3Icon.CLOSE -> {
                    canvas.drawLine(x(6f), y(6f), x(18f), y(18f), strokePaint)
                    canvas.drawLine(x(18f), y(6f), x(6f), y(18f), strokePaint)
                }
                M3Icon.SEARCH -> {
                    canvas.drawCircle(x(10.8f), y(10.8f), 6.2f * u, strokePaint)
                    canvas.drawLine(x(15.4f), y(15.4f), x(20.2f), y(20.2f), strokePaint)
                }
                M3Icon.TREE -> {
                    val r = 1.8f * u
                    rect.set(x(3.5f), y(3.5f), x(9.5f), y(8.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(14.5f), y(8.5f), x(20.5f), y(13.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(14.5f), y(15.5f), x(20.5f), y(20.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    path.moveTo(x(6.5f), y(8.5f))
                    path.lineTo(x(6.5f), y(18f))
                    path.lineTo(x(14.5f), y(18f))
                    path.moveTo(x(6.5f), y(11f))
                    path.lineTo(x(14.5f), y(11f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.EYE -> {
                    rect.set(x(2.5f), y(6.5f), x(21.5f), y(17.5f))
                    canvas.drawOval(rect, strokePaint)
                    canvas.drawCircle(x(12f), y(12f), 2.4f * u, fillPaint)
                }
                M3Icon.CREATE_FOLDER -> {
                    path.moveTo(x(3f), y(7f))
                    path.lineTo(x(3f), y(18.5f))
                    path.quadTo(x(3f), y(20f), x(4.5f), y(20f))
                    path.lineTo(x(19.5f), y(20f))
                    path.quadTo(x(21f), y(20f), x(21f), y(18.5f))
                    path.lineTo(x(21f), y(9f))
                    path.quadTo(x(21f), y(7.5f), x(19.5f), y(7.5f))
                    path.lineTo(x(12f), y(7.5f))
                    path.lineTo(x(9.8f), y(5f))
                    path.lineTo(x(4.5f), y(5f))
                    path.quadTo(x(3f), y(5f), x(3f), y(7f))
                    path.close()
                    canvas.drawPath(path, strokePaint)
                    canvas.drawLine(x(14.5f), y(11.2f), x(14.5f), y(16.2f), strokePaint)
                    canvas.drawLine(x(12f), y(13.7f), x(17f), y(13.7f), strokePaint)
                }
                M3Icon.REFRESH -> {
                    rect.set(x(5.0f), y(5.0f), x(19.0f), y(19.0f))
                    canvas.drawArc(rect, 42f, 276f, false, strokePaint)
                    path.moveTo(x(17.6f), y(3.6f))
                    path.lineTo(x(17.6f), y(8.2f))
                    path.lineTo(x(13.0f), y(8.2f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.UPLOAD -> {
                    canvas.drawLine(x(12f), y(15.5f), x(12f), y(4.5f), strokePaint)
                    path.moveTo(x(7.2f), y(9.2f))
                    path.lineTo(x(12f), y(4.4f))
                    path.lineTo(x(16.8f), y(9.2f))
                    canvas.drawPath(path, strokePaint)
                    path.moveTo(x(4.5f), y(15.5f))
                    path.lineTo(x(4.5f), y(18.5f))
                    path.quadTo(x(4.5f), y(20f), x(6f), y(20f))
                    path.lineTo(x(18f), y(20f))
                    path.quadTo(x(19.5f), y(20f), x(19.5f), y(18.5f))
                    path.lineTo(x(19.5f), y(15.5f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.DOWNLOAD -> {
                    canvas.drawLine(x(12f), y(4.5f), x(12f), y(15.5f), strokePaint)
                    path.moveTo(x(7.2f), y(10.8f))
                    path.lineTo(x(12f), y(15.6f))
                    path.lineTo(x(16.8f), y(10.8f))
                    canvas.drawPath(path, strokePaint)
                    path.moveTo(x(4.5f), y(15.5f))
                    path.lineTo(x(4.5f), y(18.5f))
                    path.quadTo(x(4.5f), y(20f), x(6f), y(20f))
                    path.lineTo(x(18f), y(20f))
                    path.quadTo(x(19.5f), y(20f), x(19.5f), y(18.5f))
                    path.lineTo(x(19.5f), y(15.5f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.FOLDER, M3Icon.FOLDER_OPEN -> {
                    path.moveTo(x(3f), y(6.8f))
                    path.quadTo(x(3f), y(5f), x(4.8f), y(5f))
                    path.lineTo(x(9.4f), y(5f))
                    path.lineTo(x(11.6f), y(7.4f))
                    path.lineTo(x(19.2f), y(7.4f))
                    path.quadTo(x(21f), y(7.4f), x(21f), y(9.2f))
                    path.lineTo(x(21f), y(17.7f))
                    path.quadTo(x(21f), y(19.5f), x(19.2f), y(19.5f))
                    path.lineTo(x(4.8f), y(19.5f))
                    path.quadTo(x(3f), y(19.5f), x(3f), y(17.7f))
                    path.close()
                    fillPaint.color = (0x36 shl 24) or (tintColor and 0x00FFFFFF)
                    canvas.drawPath(path, fillPaint)
                    canvas.drawPath(path, strokePaint)
                    if (icon == M3Icon.FOLDER_OPEN) {
                        canvas.drawLine(x(3.5f), y(11.2f), x(20.5f), y(11.2f), strokePaint)
                    }
                }
                M3Icon.FILE -> {
                    path.moveTo(x(5.5f), y(3.5f))
                    path.lineTo(x(14f), y(3.5f))
                    path.lineTo(x(18.5f), y(8f))
                    path.lineTo(x(18.5f), y(19f))
                    path.quadTo(x(18.5f), y(20.5f), x(17f), y(20.5f))
                    path.lineTo(x(5.5f), y(20.5f))
                    path.quadTo(x(4f), y(20.5f), x(4f), y(19f))
                    path.lineTo(x(4f), y(5f))
                    path.quadTo(x(4f), y(3.5f), x(5.5f), y(3.5f))
                    path.close()
                    fillPaint.color = (0x1E shl 24) or (tintColor and 0x00FFFFFF)
                    canvas.drawPath(path, fillPaint)
                    canvas.drawPath(path, strokePaint)
                    path.reset()
                    path.moveTo(x(13.8f), y(3.8f))
                    path.lineTo(x(13.8f), y(8.2f))
                    path.lineTo(x(18.2f), y(8.2f))
                    canvas.drawPath(path, strokePaint)
                    canvas.drawLine(x(7.5f), y(12.5f), x(15f), y(12.5f), strokePaint)
                    canvas.drawLine(x(7.5f), y(16f), x(12.5f), y(16f), strokePaint)
                }
                M3Icon.MORE_VERT -> {
                    fillPaint.color = tintColor
                    val r = 1.85f * u
                    canvas.drawCircle(x(12f), y(5.5f), r, fillPaint)
                    canvas.drawCircle(x(12f), y(12f), r, fillPaint)
                    canvas.drawCircle(x(12f), y(18.5f), r, fillPaint)
                }
                M3Icon.ARROW_BACK -> {
                    canvas.drawLine(x(19.5f), y(12f), x(5f), y(12f), strokePaint)
                    path.moveTo(x(11f), y(6f))
                    path.lineTo(x(4.8f), y(12f))
                    path.lineTo(x(11f), y(18f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.CHEVRON_RIGHT -> {
                    path.moveTo(x(9f), y(6f))
                    path.lineTo(x(15f), y(12f))
                    path.lineTo(x(9f), y(18f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.CHEVRON_DOWN -> {
                    path.moveTo(x(6f), y(9f))
                    path.lineTo(x(12f), y(15f))
                    path.lineTo(x(18f), y(9f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.TERMINAL -> {
                    rect.set(x(3f), y(4.5f), x(21f), y(19.5f))
                    canvas.drawRoundRect(rect, 3f * u, 3f * u, strokePaint)
                    path.moveTo(x(7f), y(9f))
                    path.lineTo(x(10.5f), y(12f))
                    path.lineTo(x(7f), y(15f))
                    canvas.drawPath(path, strokePaint)
                    canvas.drawLine(x(12.5f), y(15f), x(17f), y(15f), strokePaint)
                }
                M3Icon.PING -> {
                    path.moveTo(x(3f), y(12f))
                    path.lineTo(x(7f), y(12f))
                    path.lineTo(x(9.5f), y(6f))
                    path.lineTo(x(14.5f), y(18f))
                    path.lineTo(x(17f), y(12f))
                    path.lineTo(x(21f), y(12f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.KEY -> {
                    canvas.drawCircle(x(7.5f), y(12f), 4.2f * u, strokePaint)
                    canvas.drawLine(x(11.7f), y(12f), x(21f), y(12f), strokePaint)
                    canvas.drawLine(x(17f), y(12f), x(17f), y(15.5f), strokePaint)
                    canvas.drawLine(x(20f), y(12f), x(20f), y(16f), strokePaint)
                }
                M3Icon.CHECK -> {
                    fillPaint.color = (0x2E shl 24) or (tintColor and 0x00FFFFFF)
                    canvas.drawCircle(x(12f), y(12f), 9f * u, fillPaint)
                    canvas.drawCircle(x(12f), y(12f), 9f * u, strokePaint)
                    path.moveTo(x(8f), y(12.3f))
                    path.lineTo(x(10.8f), y(15.2f))
                    path.lineTo(x(16.2f), y(9.2f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.TRASH -> {
                    canvas.drawLine(x(4.5f), y(6.5f), x(19.5f), y(6.5f), strokePaint)
                    path.moveTo(x(9.2f), y(6.5f))
                    path.lineTo(x(9.2f), y(4.6f))
                    path.quadTo(x(9.2f), y(3.8f), x(10.2f), y(3.8f))
                    path.lineTo(x(13.8f), y(3.8f))
                    path.quadTo(x(14.8f), y(3.8f), x(14.8f), y(4.6f))
                    path.lineTo(x(14.8f), y(6.5f))
                    canvas.drawPath(path, strokePaint)
                    path.reset()
                    path.moveTo(x(6.5f), y(6.5f))
                    path.lineTo(x(7.3f), y(18.2f))
                    path.quadTo(x(7.4f), y(20.0f), x(9.2f), y(20.0f))
                    path.lineTo(x(14.8f), y(20.0f))
                    path.quadTo(x(16.6f), y(20.0f), x(16.7f), y(18.2f))
                    path.lineTo(x(17.5f), y(6.5f))
                    canvas.drawPath(path, strokePaint)
                    canvas.drawLine(x(10.2f), y(10.2f), x(10.2f), y(16.2f), strokePaint)
                    canvas.drawLine(x(13.8f), y(10.2f), x(13.8f), y(16.2f), strokePaint)
                }
                M3Icon.ALL_PACKAGES -> {
                    val r = 1.8f * u
                    rect.set(x(3.5f), y(3.5f), x(10.2f), y(10.2f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(3.5f), y(13.8f), x(10.2f), y(20.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(13.8f), y(13.8f), x(20.5f), y(20.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    path.moveTo(x(17.1f), y(2.6f))
                    path.lineTo(x(21.4f), y(6.9f))
                    path.lineTo(x(17.1f), y(11.2f))
                    path.lineTo(x(12.8f), y(6.9f))
                    path.close()
                    fillPaint.color = (0x3A shl 24) or (tintColor and 0x00FFFFFF)
                    canvas.drawPath(path, fillPaint)
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.STORAGE -> {
                    rect.set(x(4.5f), y(4f), x(19.5f), y(9f))
                    canvas.drawOval(rect, strokePaint)
                    path.moveTo(x(4.5f), y(6.5f))
                    path.lineTo(x(4.5f), y(17.5f))
                    path.quadTo(x(12f), y(21f), x(19.5f), y(17.5f))
                    path.lineTo(x(19.5f), y(6.5f))
                    canvas.drawPath(path, strokePaint)
                    path.reset()
                    path.moveTo(x(4.5f), y(12f))
                    path.quadTo(x(12f), y(15.2f), x(19.5f), y(12f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.CURSOR -> {
                    val r = 2.2f * u
                    rect.set(x(4f), y(5f), x(12.5f), y(19f))
                    fillPaint.color = (0x38 shl 24) or (tintColor and 0x00FFFFFF)
                    canvas.drawRoundRect(rect, r, r, fillPaint)
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    canvas.drawLine(x(17.5f), y(5f), x(17.5f), y(19f), strokePaint)
                    canvas.drawLine(x(15f), y(5f), x(20f), y(5f), strokePaint)
                    canvas.drawLine(x(15f), y(19f), x(20f), y(19f), strokePaint)
                }
                M3Icon.KEYBOARD -> {
                    val r = 2.6f * u
                    rect.set(x(2.5f), y(5.5f), x(21.5f), y(18.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    fillPaint.color = tintColor
                    val dotR = 1.05f * u
                    for (cx in floatArrayOf(6.5f, 10.2f, 13.8f, 17.5f)) {
                        canvas.drawCircle(x(cx), y(9.5f), dotR, fillPaint)
                        canvas.drawCircle(x(cx), y(12.5f), dotR, fillPaint)
                    }
                    canvas.drawLine(x(8f), y(15.6f), x(16f), y(15.6f), strokePaint)
                }
                M3Icon.VIBRATION -> {
                    val r = 2.2f * u
                    rect.set(x(7.5f), y(4.5f), x(16.5f), y(19.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    canvas.drawLine(x(4.5f), y(8f), x(4.5f), y(16f), strokePaint)
                    canvas.drawLine(x(2f), y(10f), x(2f), y(14f), strokePaint)
                    canvas.drawLine(x(19.5f), y(8f), x(19.5f), y(16f), strokePaint)
                    canvas.drawLine(x(22f), y(10f), x(22f), y(14f), strokePaint)
                }
                M3Icon.PALETTE -> {
                    canvas.drawCircle(x(12f), y(12f), 8.5f * u, strokePaint)
                    fillPaint.color = tintColor
                    canvas.drawCircle(x(8.2f), y(10.2f), 1.4f * u, fillPaint)
                    canvas.drawCircle(x(11.2f), y(7.6f), 1.4f * u, fillPaint)
                    canvas.drawCircle(x(15.0f), y(8.8f), 1.4f * u, fillPaint)
                    canvas.drawCircle(x(14.5f), y(14.2f), 2.1f * u, strokePaint)
                }
                M3Icon.CPU -> {
                    val r = 2.0f * u
                    rect.set(x(6f), y(6f), x(18f), y(18f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(9.2f), y(9.2f), x(14.8f), y(14.8f))
                    fillPaint.color = (0x38 shl 24) or (tintColor and 0x00FFFFFF)
                    canvas.drawRoundRect(rect, 1.0f * u, 1.0f * u, fillPaint)
                    canvas.drawRoundRect(rect, 1.0f * u, 1.0f * u, strokePaint)
                    for (p in floatArrayOf(9f, 12f, 15f)) {
                        canvas.drawLine(x(p), y(3f), x(p), y(6f), strokePaint)
                        canvas.drawLine(x(p), y(18f), x(p), y(21f), strokePaint)
                        canvas.drawLine(x(3f), y(p), x(6f), y(p), strokePaint)
                        canvas.drawLine(x(18f), y(p), x(21f), y(p), strokePaint)
                    }
                }
                M3Icon.SLIDERS -> {
                    canvas.drawLine(x(4f), y(6.5f), x(20f), y(6.5f), strokePaint)
                    canvas.drawLine(x(4f), y(12f), x(20f), y(12f), strokePaint)
                    canvas.drawLine(x(4f), y(17.5f), x(20f), y(17.5f), strokePaint)
                    fillPaint.color = uiSurfaceLow
                    canvas.drawCircle(x(9f), y(6.5f), 2.3f * u, fillPaint)
                    canvas.drawCircle(x(15.5f), y(12f), 2.3f * u, fillPaint)
                    canvas.drawCircle(x(11f), y(17.5f), 2.3f * u, fillPaint)
                    canvas.drawCircle(x(9f), y(6.5f), 2.3f * u, strokePaint)
                    canvas.drawCircle(x(15.5f), y(12f), 2.3f * u, strokePaint)
                    canvas.drawCircle(x(11f), y(17.5f), 2.3f * u, strokePaint)
                }
                M3Icon.SERVER -> {
                    val r = 2.0f * u
                    rect.set(x(3.5f), y(4.5f), x(20.5f), y(10.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    rect.set(x(3.5f), y(13.5f), x(20.5f), y(19.5f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    fillPaint.color = tintColor
                    canvas.drawCircle(x(7.2f), y(7.5f), 1.2f * u, fillPaint)
                    canvas.drawCircle(x(7.2f), y(16.5f), 1.2f * u, fillPaint)
                    canvas.drawLine(x(11.5f), y(7.5f), x(17.2f), y(7.5f), strokePaint)
                    canvas.drawLine(x(11.5f), y(16.5f), x(17.2f), y(16.5f), strokePaint)
                }
                M3Icon.EDIT -> {
                    path.moveTo(x(4.5f), y(19.5f))
                    path.lineTo(x(8.5f), y(18.5f))
                    path.lineTo(x(18.8f), y(8.2f))
                    path.lineTo(x(15.8f), y(5.2f))
                    path.lineTo(x(5.5f), y(15.5f))
                    path.close()
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.COPY -> {
                    val r = 2.0f * u
                    rect.set(x(8.5f), y(8.5f), x(19.5f), y(20f))
                    canvas.drawRoundRect(rect, r, r, strokePaint)
                    path.moveTo(x(15.5f), y(8.5f))
                    path.lineTo(x(15.5f), y(5.8f))
                    path.quadTo(x(15.5f), y(4f), x(13.7f), y(4f))
                    path.lineTo(x(6.3f), y(4f))
                    path.quadTo(x(4.5f), y(4f), x(4.5f), y(5.8f))
                    path.lineTo(x(4.5f), y(14.2f))
                    path.quadTo(x(4.5f), y(16f), x(6.3f), y(16f))
                    path.lineTo(x(8.5f), y(16f))
                    canvas.drawPath(path, strokePaint)
                }
                M3Icon.FONT -> {
                    path.moveTo(x(3.8f), y(18.5f))
                    path.lineTo(x(8.8f), y(5.5f))
                    path.lineTo(x(13.8f), y(18.5f))
                    canvas.drawPath(path, strokePaint)
                    canvas.drawLine(x(5.5f), y(14.2f), x(12.1f), y(14.2f), strokePaint)
                    canvas.drawCircle(x(17.8f), y(15.2f), 2.8f * u, strokePaint)
                    canvas.drawLine(x(20.6f), y(12.2f), x(20.6f), y(18.5f), strokePaint)
                }
            }
        }
    }

    private fun makeHeaderIconButton(
        icon: M3Icon,
        tintColor: Int = COLOR_TEXT_BRIGHT,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ): FrameLayout {
        return FrameLayout(this).apply {
            background = pillBg(COLOR_SURFACE, COLOR_BORDER, dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(30)).apply {
                setMargins(dp(4), dp(2), 0, dp(2))
            }
            val iconView = MaterialIconView(
                this@TermuxActivity,
                icon = icon,
                tintColor = tintColor,
                iconSizeDp = 18.5f,
                strokeWidthDp = 2.0f
            ).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            }
            addView(iconView)
            attachSmoothPressAnimation(this, 0.90f)
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

    // ========================================================================
    // SSH Server Persistence & C++23 Bridge
    // ========================================================================
    private fun loadSshServers(): MutableList<SshServerProfile> {
        val rawV2 = prefs.getString(KEY_SSH_SERVERS_V2, null)
        val raw = if (!rawV2.isNullOrBlank()) rawV2 else (prefs.getString(KEY_SSH_SERVERS_V1, "") ?: "")
        if (raw.isBlank()) return mutableListOf()
        val list = mutableListOf<SshServerProfile>()
        for (line in raw.split("\n")) {
            if (line.isBlank()) continue
            val parts = line.split("\t")
            if (parts.size >= 6) {
                list.add(
                    SshServerProfile(
                        id = parts[0],
                        label = parts[1],
                        host = parts[2],
                        port = parts[3].toIntOrNull() ?: 22,
                        username = parts[4],
                        authType = parts[5].toIntOrNull() ?: 0,
                        secretOrKeyPath = parts.getOrNull(6) ?: "",
                        startupCommand = parts.getOrNull(7) ?: "",
                        useCompression = (parts.getOrNull(8) ?: "1") == "1",
                        forwardAgent = (parts.getOrNull(9) ?: "0") == "1",
                        localForwards = parts.getOrNull(10) ?: "",
                        remoteForwards = parts.getOrNull(11) ?: "",
                        dynamicSocksPort = parts.getOrNull(12) ?: "",
                        jumpHost = parts.getOrNull(13) ?: "",
                        keepAliveSec = parts.getOrNull(14)?.toIntOrNull() ?: 30,
                        tunnelOnlyN = (parts.getOrNull(15) ?: "0") == "1",
                        ipv4Only = (parts.getOrNull(16) ?: "0") == "1",
                        customFlags = parts.getOrNull(17) ?: ""
                    )
                )
            }
        }
        return list
    }

    private fun saveSshServers(servers: List<SshServerProfile>) {
        val clean = { s: String -> s.replace("\t", " ").replace("\n", " ").replace("\r", "") }
        val serialized = servers.joinToString("\n") { s ->
            listOf(
                clean(s.id),
                clean(s.label),
                clean(s.host),
                s.port.toString(),
                clean(s.username),
                s.authType.toString(),
                clean(s.secretOrKeyPath),
                clean(s.startupCommand),
                if (s.useCompression) "1" else "0",
                if (s.forwardAgent) "1" else "0",
                clean(s.localForwards),
                clean(s.remoteForwards),
                clean(s.dynamicSocksPort),
                clean(s.jumpHost),
                s.keepAliveSec.toString(),
                if (s.tunnelOnlyN) "1" else "0",
                if (s.ipv4Only) "1" else "0",
                clean(s.customFlags)
            ).joinToString("\t")
        }
        prefs.edit().putString(KEY_SSH_SERVERS_V2, serialized).apply()
    }

    private fun shellEscapeSingle(s: String): String {
        return "'" + s.replace("'", "'\"'\"'") + "'"
    }

    private fun buildSshCommandViaCpp23(server: SshServerProfile, withAskPass: Boolean): String {
        return nativeBuildSshCommand(
            server.host,
            server.port,
            server.username,
            server.authType,
            server.secretOrKeyPath,
            server.localForwards,
            server.remoteForwards,
            server.dynamicSocksPort,
            server.jumpHost,
            server.keepAliveSec,
            server.tunnelOnlyN,
            server.useCompression,
            server.forwardAgent,
            server.ipv4Only,
            server.customFlags,
            server.startupCommand,
            withAskPass
        )
    }

    private fun connectToSshServerInTerminal(server: SshServerProfile, dlgToDismiss: Dialog? = null) {
        dlgToDismiss?.dismiss()
        createSession(false, runStartupCmd = false)
        val fullCmd = buildSshCommandViaCpp23(server, withAskPass = true) + "\n"
        terminalSurface.postDelayed({
            nativeSendText(fullCmd, false)
        }, 120L)
    }

    private fun runRemoteSftpViaCpp23(server: SshServerProfile, remoteShCommand: String): Pair<Boolean, String> {
        val raw = nativeExecRemoteSftp(
            server.host,
            server.port,
            server.username,
            server.authType,
            server.secretOrKeyPath,
            server.jumpHost,
            remoteShCommand
        )
        return if (raw.startsWith("OK\n")) {
            true to raw.substring(3)
        } else if (raw.startsWith("ERR\n")) {
            false to raw.substring(4)
        } else {
            false to raw
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    private fun formatTimestamp(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(epochMs))
        } catch (_: Throwable) {
            ""
        }
    }

    private fun parseInstalledDpkgPackagesViaCpp23(): List<DpkgPackageInfo> {
        val raw = nativeGetInstalledPackages()
        if (raw.isBlank()) return emptyList()
        val out = ArrayList<DpkgPackageInfo>()
        for (line in raw.lineSequence()) {
            if (line.isBlank()) continue
            val cols = line.split("\t")
            if (cols.size >= 4) {
                out.add(
                    DpkgPackageInfo(
                        name = cols[0],
                        version = cols[1],
                        sizeKb = cols[2].toLongOrNull() ?: 0L,
                        description = cols[3],
                        isCore = (cols.getOrNull(4) ?: "0") == "1"
                    )
                )
            }
        }
        return out
    }

    // ========================================================================
    // Material 3 Expressive Workspace Hub: Servers | SFTP | Packages
    // (Borderless Tonal Design + Predictive Back + BakaSU Morphing Pull-to-Refresh)
    // ========================================================================
    private fun showWorkspaceHubDialog(initialTab: Int = 0) {
        val dlg = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(uiSurface))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            attributes = attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            setDecorFitsSystemWindows(false)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(uiSurface)
        }
        applySafeWindowInsets(root, extraLeftDp = 0, extraTopDp = 8, extraRightDp = 0, extraBottomDp = 6)

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(mainCol)

        var activeTab = initialTab.coerceIn(0, 2) // 0 = Servers, 1 = SFTP, 2 = Packages
        var previousTab = activeTab
        var activeSftpServer: SshServerProfile? = null
        val homeDir = File("/data/data/com.termuxx/file/home").let {
            if (it.exists()) it else File(applicationInfo.dataDir)
        }
        var localCurrentDir = homeDir
        var remoteCurrentPath = "/root"
        var sftpTreeMode = false
        var sftpShowHidden = true
        val expandedTreePaths = HashSet<String>()
        var sftpSearchActive = false
        var sftpSearchQuery = ""

        val topNavRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(uiSurface)
            setPadding(dp(12), dp(2), dp(12), dp(6))
        }

        val termBackChip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = tonalBg(uiAccentContainer, dp(14))
            setPadding(dp(11), dp(7), dp(12), dp(7))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            addView(
                MaterialIconView(this@TermuxActivity, M3Icon.TERMINAL, uiAccent, 18.5f, 2.0f).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                        marginEnd = dp(6)
                    }
                }
            )
            addView(TextView(this@TermuxActivity).apply {
                text = "Terminal"
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            })
            attachSmoothPressAnimation(this, 0.93f)
            setOnClickListener {
                triggerHaptic(0.8f)
                dlg.dismiss()
            }
        }
        topNavRow.addView(termBackChip)

        fun makeTopNavTab(title: String, onSelect: () -> Unit): Pair<LinearLayout, View> {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(4), dp(8), dp(4), dp(2))
                attachSmoothPressAnimation(this, 0.94f)
                setOnClickListener {
                    triggerHaptic(0.8f)
                    onSelect()
                }
            }
            val tv = TextView(this).apply {
                text = title
                gravity = Gravity.CENTER
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setPadding(0, 0, 0, dp(7))
            }
            val indicator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(4))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpF(99f)
                    setColor(uiAccent)
                }
            }
            col.addView(tv)
            col.addView(indicator)
            return col to indicator
        }

        lateinit var renderActiveTab: () -> Unit

        val (tabServersCol, indServers) = makeTopNavTab("Servers") {
            previousTab = activeTab
            activeTab = 0
            renderActiveTab()
        }
        val (tabSftpCol, indSftp) = makeTopNavTab("SFTP") {
            previousTab = activeTab
            activeTab = 1
            renderActiveTab()
        }
        val (tabPkgsCol, indPkgs) = makeTopNavTab("Packages") {
            previousTab = activeTab
            activeTab = 2
            renderActiveTab()
        }

        topNavRow.addView(tabServersCol)
        topNavRow.addView(tabSftpCol)
        topNavRow.addView(tabPkgsCol)
        mainCol.addView(topNavRow)

        val contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        mainCol.addView(contentFrame)

        // Predictive Back on Workspace Hub
        val hubPredictiveBack = object : OnBackAnimationCallback {
            override fun onBackStarted(backEvent: BackEvent) {
                root.animate().cancel()
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                val p = backEvent.progress.coerceIn(0f, 1f)
                val w = max(resources.displayMetrics.widthPixels.toFloat(), dpF(320f))
                val dir = if (backEvent.swipeEdge == BackEvent.EDGE_RIGHT) -1f else 1f
                root.translationX = dir * p * w * 0.22f
                val sc = 1f - p * 0.08f
                root.scaleX = sc
                root.scaleY = sc
                root.alpha = 1f - p * 0.3f
            }

            override fun onBackCancelled() {
                root.animate()
                    .translationX(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(200L)
                    .setInterpolator(aosp17SpatialInterpolator)
                    .start()
            }

            override fun onBackInvoked() {
                triggerHaptic(0.75f)
                if (activeTab == 1 && sftpSearchActive) {
                    sftpSearchActive = false
                    sftpSearchQuery = ""
                    root.translationX = 0f; root.scaleX = 1f; root.scaleY = 1f; root.alpha = 1f
                    renderActiveTab()
                } else {
                    dlg.dismiss()
                }
            }
        }
        dlg.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            hubPredictiveBack
        )

        fun showCodeViewerAndEditorDialog(
            fileName: String,
            fullPath: String,
            initialContent: String,
            onSaveContent: (String) -> Boolean
        ) {
            val vDlg = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
            vDlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
            vDlg.window?.apply {
                setBackgroundDrawable(ColorDrawable(uiSurface))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                attributes = attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                setDecorFitsSystemWindows(false)
            }

            var isEditMode = false
            var viewerFontScale = 1.0f
            var currentText = initialContent

            val vRoot = FrameLayout(this).apply {
                setBackgroundColor(uiSurface)
            }
            applySafeWindowInsets(vRoot, extraLeftDp = 0, extraTopDp = 8, extraRightDp = 0, extraBottomDp = 8)

            lateinit var rebuildViewer: () -> Unit
            rebuildViewer = {
                vRoot.removeAllViews()
                val col = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val hdr = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(6), dp(14), dp(10))
                }
                hdr.addView(
                    FrameLayout(this).apply {
                        background = tonalBg(uiAccentContainer, dp(12))
                        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                            marginEnd = dp(10)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.ARROW_BACK, uiAccent, 20f, 2.2f))
                        attachSmoothPressAnimation(this)
                        setOnClickListener {
                            triggerHaptic(0.8f)
                            vDlg.dismiss()
                        }
                    }
                )
                val titleCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                titleCol.addView(TextView(this).apply {
                    text = fileName
                    setSingleLine(true)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                })
                titleCol.addView(TextView(this).apply {
                    text = fullPath
                    setSingleLine(true)
                    setTextColor(COLOR_TEXT_DIM)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                })
                hdr.addView(titleCol)
                col.addView(hdr)

                if (isEditMode) {
                    val editBox = EditText(this).apply {
                        setText(currentText)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * viewerFontScale)
                        gravity = Gravity.TOP or Gravity.START
                        setBackgroundColor(uiSurfaceLow)
                        setPadding(dp(14), dp(12), dp(14), dp(96))
                    }
                    val scroll = ScrollView(this).apply {
                        isFillViewport = true
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                        addView(editBox)
                    }
                    col.addView(scroll)

                    val pillWrap = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(8), dp(14), dp(8))
                        background = makeM3FloatingSurfaceDrawable(dp(30))
                        elevation = dpF(10f)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            dp(56),
                            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        ).apply {
                            bottomMargin = dp(18)
                        }
                    }
                    pillWrap.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) {
                        isEditMode = false
                        rebuildViewer()
                    })
                    pillWrap.addView(makeExpressiveChip(tr("Сохранить", "Save"), true, uiAccent, uiAccentContainer) {
                        currentText = editBox.text.toString()
                        if (onSaveContent(currentText)) {
                            Toast.makeText(this@TermuxActivity, tr("Сохранено: $fileName", "Saved: $fileName"), Toast.LENGTH_SHORT).show()
                            isEditMode = false
                            rebuildViewer()
                        } else {
                            Toast.makeText(this@TermuxActivity, tr("Ошибка сохранения", "Failed to save"), Toast.LENGTH_SHORT).show()
                        }
                    })
                    vRoot.addView(col)
                    vRoot.addView(pillWrap)
                } else {
                    val vScroll = ScrollView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                    }
                    val codeRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(8), 0, dp(96))
                    }
                    val lines = if (currentText.endsWith("\n")) currentText.lines().dropLast(1) else currentText.lines()
                    val numSb = StringBuilder()
                    for (i in 1..max(1, lines.size)) {
                        if (i > 1) numSb.append('\n')
                        numSb.append(i)
                    }
                    val lineNumsTv = TextView(this).apply {
                        text = numSb.toString()
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.MONOSPACE
                        gravity = Gravity.END
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * viewerFontScale)
                        setPadding(dp(12), 0, dp(10), 0)
                    }
                    val hScroll = HorizontalScrollView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val codeTv = TextView(this).apply {
                        text = currentText.ifEmpty { tr("(Пустой файл)", "(Empty file)") }
                        setTextIsSelectable(true)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * viewerFontScale)
                        setPadding(dp(10), 0, dp(16), 0)
                    }
                    hScroll.addView(codeTv)
                    codeRow.addView(lineNumsTv)
                    codeRow.addView(hScroll)
                    vScroll.addView(codeRow)
                    col.addView(vScroll)
                    vRoot.addView(col)

                    val pill = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(6), dp(14), dp(6))
                        background = makeM3FloatingSurfaceDrawable(dp(30))
                        elevation = dpF(10f)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            dp(56),
                            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        ).apply {
                            bottomMargin = dp(18)
                        }
                    }
                    pill.addView(TextView(this).apply {
                        text = "${(viewerFontScale * 100).roundToInt()}%"
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(dp(6), dp(4), dp(8), dp(4))
                        setOnClickListener {
                            viewerFontScale = 1.0f
                            rebuildViewer()
                        }
                    })
                    pill.addView(makeExpressiveChip("A-", false) {
                        viewerFontScale = (viewerFontScale - 0.15f).coerceIn(0.6f, 2.5f)
                        rebuildViewer()
                    })
                    pill.addView(makeExpressiveChip("A+", false) {
                        viewerFontScale = (viewerFontScale + 0.15f).coerceIn(0.6f, 2.5f)
                        rebuildViewer()
                    })
                    pill.addView(makeExpressiveChip(tr("Копировать", "Copy"), false) {
                        copyToClipboard(currentText, true)
                    })
                    pill.addView(makeExpressiveChip(tr("Изменить", "Edit"), true, uiAccent, uiAccentContainer) {
                        isEditMode = true
                        rebuildViewer()
                    })
                    vRoot.addView(pill)
                }
            }

            rebuildViewer()
            vDlg.setContentView(vRoot)
            vDlg.show()
        }

        // ====================================================================
        // TAB 0: Servers & Advanced SSH Manager
        // ====================================================================
        fun showAddOrEditServerDialog(existing: SshServerProfile?, onSaved: () -> Unit) {
            val (sDlg, sSheet) = createExpressiveDialog(440)
            sSheet.addView(TextView(this).apply {
                text = if (existing == null) tr("Настройка SSH-сервера", "Add SSH Server") else tr("Изменить SSH-сервер", "Edit SSH Server")
                setTextColor(COLOR_TEXT_BRIGHT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(8))
            })

            val scroll = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(480)
                )
            }
            val form = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            scroll.addView(form)
            sSheet.addView(scroll)

            lateinit var updateLivePreview: () -> Unit

            fun makeLabeledField(
                label: String,
                initial: String,
                hintStr: String,
                isNumeric: Boolean = false
            ): EditText {
                form.addView(TextView(this).apply {
                    text = label
                    setTextColor(uiAccent)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setPadding(dp(2), dp(10), dp(2), dp(4))
                })
                val et = EditText(this).apply {
                    setText(initial)
                    hint = hintStr
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setSingleLine(true)
                    if (isNumeric) {
                        inputType = InputType.TYPE_CLASS_NUMBER
                    }
                    background = tonalBg(uiSurfaceLow, dp(14))
                    setPadding(dp(14), dp(9), dp(14), dp(9))
                }
                form.addView(et)
                return et
            }

            form.addView(TextView(this).apply {
                text = tr("Быстрый импорт из строки команды", "Quick import from ssh command")
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(dp(2), dp(6), dp(2), dp(4))
            })
            val importRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val rawCmdEt = EditText(this).apply {
                hint = "ssh -p 2222 -L 8080:127.0.0.1:80 user@example.com"
                setHintTextColor(COLOR_TEXT_DIM)
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setSingleLine(true)
                background = tonalBg(uiSurfaceLow, dp(12))
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(6)
                }
            }
            importRow.addView(rawCmdEt)

            val labelEt = makeLabeledField(tr("Название", "Label"), existing?.label ?: "", "Main Server")
            val hostEt = makeLabeledField("Host (IP / Domain)", existing?.host ?: "", "example.com")
            val portEt = makeLabeledField("Port (-p)", (existing?.port ?: 22).toString(), "22", isNumeric = true)
            val userEt = makeLabeledField("Username", existing?.username ?: "root", "root")

            var selectedAuth = existing?.authType ?: 0
            form.addView(TextView(this).apply {
                text = tr("Авторизация", "Authentication")
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(dp(2), dp(10), dp(2), dp(4))
            })
            val (authScroll, authRow) = makeHorizontalChipRow()
            val secretLabelTv = TextView(this).apply {
                text = if (selectedAuth == 0) "Password" else if (selectedAuth == 1) tr("Путь к ключу (-i)", "Key path (-i)") else "SSH Agent"
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(dp(2), dp(10), dp(2), dp(4))
            }
            val defaultKeyPath = "/data/data/com.termuxx/file/home/.ssh/id_ed25519"
            val secretEt = EditText(this).apply {
                setText(existing?.secretOrKeyPath ?: (if (selectedAuth == 1) defaultKeyPath else ""))
                hint = if (selectedAuth == 0) tr("Пароль", "Password") else defaultKeyPath
                setHintTextColor(COLOR_TEXT_DIM)
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setSingleLine(true)
                background = tonalBg(uiSurfaceLow, dp(14))
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }

            fun rebuildAuthChips() {
                authRow.removeAllViews()
                val opts = listOf(
                    tr("Пароль", "Password") to 0,
                    tr("SSH-Ключ (-i)", "SSH Key (-i)") to 1,
                    "SSH Agent (-A)" to 2
                )
                for ((lbl, code) in opts) {
                    authRow.addView(makeExpressiveChip(lbl, selectedAuth == code) {
                        selectedAuth = code
                        secretLabelTv.text = when (code) {
                            0 -> "Password"
                            1 -> tr("Путь к ключу (-i)", "Key path (-i)")
                            else -> "SSH Agent"
                        }
                        if (code == 1 && secretEt.text.isNullOrBlank()) {
                            secretEt.setText(defaultKeyPath)
                        }
                        rebuildAuthChips()
                        updateLivePreview()
                    })
                }
            }
            rebuildAuthChips()
            form.addView(authScroll)
            form.addView(secretLabelTv)
            form.addView(secretEt)

            val lBuilderRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            fun makeMiniPortEt(hintText: String, weight: Float): EditText {
                return EditText(this).apply {
                    hint = hintText
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setSingleLine(true)
                    background = tonalBg(uiSurfaceLow, dp(10))
                    setPadding(dp(8), dp(7), dp(8), dp(7))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight).apply {
                        marginEnd = dp(4)
                    }
                }
            }
            val lLocPortEt = makeMiniPortEt("Local", 1f)
            val lRemHostEt = makeMiniPortEt("127.0.0.1", 1.3f).apply { setText("127.0.0.1") }
            val lRemPortEt = makeMiniPortEt("Remote", 1f)
            lBuilderRow.addView(lLocPortEt)
            lBuilderRow.addView(lRemHostEt)
            lBuilderRow.addView(lRemPortEt)

            val localFwdsEt = makeLabeledField(
                "Local Port Forwarding (-L)",
                existing?.localForwards ?: "",
                "8080:127.0.0.1:80"
            )
            lBuilderRow.addView(makeExpressiveChip("+ -L", true) {
                val lp = lLocPortEt.text.toString().trim()
                val rh = lRemHostEt.text.toString().trim().ifEmpty { "127.0.0.1" }
                val rp = lRemPortEt.text.toString().trim().ifEmpty { lp }
                if (lp.isNotEmpty() && rp.isNotEmpty()) {
                    val rule = "$lp:$rh:$rp"
                    val cur = localFwdsEt.text.toString().trim()
                    localFwdsEt.setText(if (cur.isEmpty()) rule else "$cur, $rule")
                    lLocPortEt.setText("")
                    lRemPortEt.setText("")
                    updateLivePreview()
                }
            })
            val lIdx = form.indexOfChild(localFwdsEt)
            form.addView(lBuilderRow, lIdx)

            val remoteFwdsEt = makeLabeledField(
                "Remote Port Forwarding (-R)",
                existing?.remoteForwards ?: "",
                "9000:127.0.0.1:9000"
            )
            val dynamicPortEt = makeLabeledField(
                "Dynamic SOCKS5 Proxy (-D)",
                existing?.dynamicSocksPort ?: "",
                "1080",
                isNumeric = true
            )
            val jumpHostEt = makeLabeledField(
                "ProxyJump (-J)",
                existing?.jumpHost ?: "",
                "user@jumphost:22"
            )
            val keepAliveEt = makeLabeledField(
                "ServerAliveInterval (sec)",
                (existing?.keepAliveSec ?: 30).toString(),
                "30",
                isNumeric = true
            )
            val customFlagsEt = makeLabeledField(
                tr("Дополнительные флаги", "Extra SSH flags"),
                existing?.customFlags ?: "",
                "-o TCPKeepAlive=yes"
            )
            val startupEt = makeLabeledField(
                tr("Команда после входа", "Startup command"),
                existing?.startupCommand ?: "",
                "tmux attach || tmux"
            )

            var tunnelOnlyN = existing?.tunnelOnlyN ?: false
            var useComp = existing?.useCompression ?: true
            var fwdAgent = existing?.forwardAgent ?: false
            var ipv4Only = existing?.ipv4Only ?: false

            val (optScroll, optRow) = makeHorizontalChipRow()
            fun rebuildOptChips() {
                optRow.removeAllViews()
                optRow.addView(makeExpressiveChip("Tunnel only (-N)", tunnelOnlyN) {
                    tunnelOnlyN = !tunnelOnlyN
                    rebuildOptChips()
                    updateLivePreview()
                })
                optRow.addView(makeExpressiveChip("Compression (-C)", useComp) {
                    useComp = !useComp
                    rebuildOptChips()
                    updateLivePreview()
                })
                optRow.addView(makeExpressiveChip("Forward Agent (-A)", fwdAgent) {
                    fwdAgent = !fwdAgent
                    rebuildOptChips()
                    updateLivePreview()
                })
                optRow.addView(makeExpressiveChip("IPv4 only (-4)", ipv4Only) {
                    ipv4Only = !ipv4Only
                    rebuildOptChips()
                    updateLivePreview()
                })
            }
            rebuildOptChips()
            form.addView(optScroll)

            importRow.addView(makeExpressiveChip(tr("Разобрать", "Parse"), true) {
                val rawCmd = rawCmdEt.text.toString().trim()
                if (rawCmd.isNotEmpty()) {
                    val parsed = nativeParseSshCommand(rawCmd).split("\t")
                    if (parsed.size >= 13) {
                        if (parsed[0].isNotEmpty()) hostEt.setText(parsed[0])
                        if (parsed[1].isNotEmpty()) portEt.setText(parsed[1])
                        if (parsed[2].isNotEmpty()) userEt.setText(parsed[2])
                        if (parsed[3].isNotEmpty()) localFwdsEt.setText(parsed[3])
                        if (parsed[4].isNotEmpty()) remoteFwdsEt.setText(parsed[4])
                        if (parsed[5].isNotEmpty()) dynamicPortEt.setText(parsed[5])
                        if (parsed[6].isNotEmpty()) jumpHostEt.setText(parsed[6])
                        if (parsed[7].isNotEmpty()) {
                            selectedAuth = 1
                            secretEt.setText(parsed[7])
                            rebuildAuthChips()
                        }
                        tunnelOnlyN = parsed[8] == "1"
                        useComp = parsed[9] == "1"
                        fwdAgent = parsed[10] == "1"
                        ipv4Only = parsed[11] == "1"
                        if (parsed[12].isNotEmpty()) customFlagsEt.setText(parsed[12])
                        rebuildOptChips()
                        updateLivePreview()
                    }
                }
            })
            form.addView(importRow, 1)

            val previewTv = TextView(this).apply {
                setTextColor(uiAccent)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                background = tonalBg(uiAccentContainer, dp(12))
                setPadding(dp(12), dp(9), dp(12), dp(9))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            }
            form.addView(previewTv)

            fun buildDraftProfile(): SshServerProfile {
                val hostVal = hostEt.text.toString().trim()
                val userVal = userEt.text.toString().trim().ifEmpty { "root" }
                val portVal = portEt.text.toString().trim().toIntOrNull()?.coerceIn(1, 65535) ?: 22
                val labelVal = labelEt.text.toString().trim().ifEmpty {
                    if (hostVal.isNotEmpty()) "$userVal@$hostVal:$portVal" else "SSH Server"
                }
                return SshServerProfile(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    label = labelVal,
                    host = hostVal,
                    port = portVal,
                    username = userVal,
                    authType = selectedAuth,
                    secretOrKeyPath = secretEt.text.toString(),
                    startupCommand = startupEt.text.toString().trim(),
                    useCompression = useComp,
                    forwardAgent = fwdAgent,
                    localForwards = localFwdsEt.text.toString().trim(),
                    remoteForwards = remoteFwdsEt.text.toString().trim(),
                    dynamicSocksPort = dynamicPortEt.text.toString().trim(),
                    jumpHost = jumpHostEt.text.toString().trim(),
                    keepAliveSec = keepAliveEt.text.toString().trim().toIntOrNull() ?: 30,
                    tunnelOnlyN = tunnelOnlyN,
                    ipv4Only = ipv4Only,
                    customFlags = customFlagsEt.text.toString().trim()
                )
            }

            updateLivePreview = {
                previewTv.text = buildSshCommandViaCpp23(buildDraftProfile(), withAskPass = false)
            }
            updateLivePreview()

            val btnBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(12), 0, 0)
            }
            btnBar.addView(makeExpressiveChip(tr("Превью", "Preview"), false) { updateLivePreview() })
            btnBar.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { sDlg.dismiss() })
            btnBar.addView(makeExpressiveChip(tr("Сохранить", "Save"), true) {
                val profile = buildDraftProfile()
                if (profile.host.isEmpty()) {
                    Toast.makeText(this, tr("Укажите Host", "Specify Host"), Toast.LENGTH_SHORT).show()
                    return@makeExpressiveChip
                }
                val list = loadSshServers()
                val idx = list.indexOfFirst { it.id == profile.id }
                if (idx >= 0) list[idx] = profile else list.add(profile)
                saveSshServers(list)
                sDlg.dismiss()
                onSaved()
            })
            sSheet.addView(btnBar)
            sDlg.show()
        }

        fun pingServerAsync(srv: SshServerProfile, badgeTv: TextView?) {
            badgeTv?.text = "Ping…"
            badgeTv?.setTextColor(COLOR_TEXT_DIM)
            Thread {
                val rtt = nativePingHostMs(srv.host, srv.port, 2500)
                serverPingCacheMs[srv.id] = rtt
                runOnUiThread {
                    if (badgeTv != null) {
                        if (rtt >= 0) {
                            badgeTv.text = "$rtt ms"
                            val col = when {
                                rtt < 85 -> COLOR_PING_GOOD
                                rtt < 200 -> COLOR_PING_WARN
                                else -> COLOR_ROOT
                            }
                            badgeTv.setTextColor(col)
                        } else {
                            badgeTv.text = "Offline"
                            badgeTv.setTextColor(COLOR_ROOT)
                        }
                    }
                }
            }.start()
        }

        fun renderServersTab() {
            contentFrame.removeAllViews()
            animateAosp17PageTransition(contentFrame, forward = activeTab >= previousTab)

            val servers = loadSshServers()
            val pingBadgeViews = HashMap<String, TextView>()

            val scroll = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val listCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(104))
            }
            scroll.addView(listCol)

            val ptrLayout = BakaSuPullToRefreshLayout(this, scroll) {
                for (s in servers) {
                    pingServerAsync(s, pingBadgeViews[s.id])
                }
            }
            contentFrame.addView(
                ptrLayout,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(2), 0, dp(2), dp(12))
            }
            titleRow.addView(TextView(this).apply {
                text = tr("SSH Серверы", "SSH Servers")
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (servers.isNotEmpty()) {
                titleRow.addView(makeExpressiveChip(tr("Пинг всех", "Ping all"), false) {
                    for (s in servers) {
                        pingServerAsync(s, pingBadgeViews[s.id])
                    }
                })
            }
            titleRow.addView(makeExpressiveChip(tr("SSH-Ключи", "SSH Keys"), false) {
                val sshDir = File("/data/data/com.termuxx/file/home/.ssh").apply { mkdirs() }
                val pubFile = File(sshDir, "id_ed25519.pub")
                val privFile = File(sshDir, "id_ed25519")
                val (kDlg, kSheet) = createExpressiveDialog(420)
                kSheet.addView(TextView(this).apply {
                    text = "SSH Keys (ed25519)"
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, dp(8))
                })
                val pubText = if (pubFile.exists()) pubFile.readText().trim() else tr("Ключ ~/.ssh/id_ed25519.pub ещё не создан.", "Key ~/.ssh/id_ed25519.pub not generated yet.")
                kSheet.addView(TextView(this).apply {
                    text = pubText
                    setTextIsSelectable(true)
                    setTextColor(if (pubFile.exists()) uiAccent else COLOR_TEXT_DIM)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    background = tonalBg(uiSurfaceLow, dp(14))
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                })
                val actRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(12), 0, 0)
                }
                if (pubFile.exists()) {
                    actRow.addView(makeExpressiveChip(tr("Копировать", "Copy key"), false) {
                        copyToClipboard(pubText, true)
                    })
                }
                actRow.addView(makeExpressiveChip(
                    if (privFile.exists()) tr("Пересоздать", "Regenerate") else tr("Создать ed25519", "Generate ed25519"),
                    true
                ) {
                    kDlg.dismiss()
                    dlg.dismiss()
                    nativeSendText(
                        "mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_ed25519 ~/.ssh/id_ed25519.pub && ssh-keygen -t ed25519 -N \"\" -f ~/.ssh/id_ed25519 && cat ~/.ssh/id_ed25519.pub\n",
                        false
                    )
                })
                kSheet.addView(actRow)
                kDlg.show()
            })
            listCol.addView(titleRow)

            if (servers.isEmpty()) {
                val emptyBox = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(24), dp(38), dp(24), dp(38))
                    background = tonalBg(uiSurfaceLow, dp(24))
                }
                emptyBox.addView(
                    FrameLayout(this).apply {
                        background = tonalBg(uiAccentContainer, dp(16))
                        layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                            bottomMargin = dp(12)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.SERVER, uiAccent, 26f, 2.1f))
                    }
                )
                emptyBox.addView(TextView(this).apply {
                    text = tr("Нет сохранённых SSH-серверов", "No saved SSH servers")
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                })
                emptyBox.addView(TextView(this).apply {
                    text = tr(
                        "Нажмите «+» внизу справа, чтобы добавить сервер или настроить проброс портов.",
                        "Tap '+' at the bottom right to add a server or configure port forwarding."
                    )
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(6), 0, 0)
                })
                listCol.addView(emptyBox)
            } else {
                for ((idx, srv) in servers.withIndex()) {
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(13), dp(10), dp(13))
                        background = segmentedCardBg(idx, servers.size, uiSurfaceLow, 0, 22f, 5f)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = if (idx < servers.size - 1) dp(2) else dp(10)
                        }
                        attachSmoothPressAnimation(this, 0.98f)
                        setOnClickListener {
                            triggerHaptic(0.85f)
                            connectToSshServerInTerminal(srv, dlg)
                        }
                    }

                    card.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(uiAccentContainer, dp(12))
                            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                                marginEnd = dp(12)
                            }
                            addView(MaterialIconView(this@TermuxActivity, M3Icon.SERVER, uiAccent, 21f, 2.0f))
                        }
                    )

                    val infoCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    infoCol.addView(TextView(this).apply {
                        text = srv.label
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    })
                    infoCol.addView(TextView(this).apply {
                        text = "${srv.host}:${srv.port} • ${srv.username}"
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setPadding(0, dp(2), 0, dp(4))
                    })

                    val badgeRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    fun makeMiniBadge(txt: String) = TextView(this).apply {
                        text = txt
                        setTextColor(uiAccent)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
                        background = tonalBg(uiAccentContainer, dp(6))
                        setPadding(dp(7), dp(2), dp(7), dp(2))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = dp(5)
                        }
                    }
                    badgeRow.addView(makeMiniBadge(when (srv.authType) {
                        0 -> "Pass"
                        1 -> "Key"
                        else -> "Agent"
                    }))
                    if (srv.localForwards.isNotBlank()) {
                        badgeRow.addView(makeMiniBadge("-L ${srv.localForwards.take(18)}"))
                    }
                    if (srv.remoteForwards.isNotBlank()) {
                        badgeRow.addView(makeMiniBadge("-R ${srv.remoteForwards.take(14)}"))
                    }
                    if (srv.dynamicSocksPort.isNotBlank()) {
                        badgeRow.addView(makeMiniBadge("-D ${srv.dynamicSocksPort}"))
                    }
                    infoCol.addView(badgeRow)
                    card.addView(infoCol)

                    val cachedPing = serverPingCacheMs[srv.id]
                    val pingPill = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = tonalBg(uiSurfaceContainer, dp(10))
                        setPadding(dp(8), dp(5), dp(9), dp(5))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = dp(6)
                        }
                        attachSmoothPressAnimation(this, 0.92f)
                    }
                    pingPill.addView(
                        MaterialIconView(this, M3Icon.PING, uiAccent, 15f, 1.9f).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                                marginEnd = dp(4)
                            }
                        }
                    )
                    val pingTv = TextView(this).apply {
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                        if (cachedPing == null) {
                            text = "Ping"
                            setTextColor(COLOR_TEXT)
                        } else if (cachedPing >= 0) {
                            text = "$cachedPing ms"
                            setTextColor(if (cachedPing < 85) COLOR_PING_GOOD else if (cachedPing < 200) COLOR_PING_WARN else COLOR_ROOT)
                        } else {
                            text = "Offline"
                            setTextColor(COLOR_ROOT)
                        }
                    }
                    pingBadgeViews[srv.id] = pingTv
                    pingPill.addView(pingTv)
                    pingPill.setOnClickListener {
                        triggerHaptic(0.8f)
                        pingServerAsync(srv, pingTv)
                    }
                    card.addView(pingPill)

                    card.addView(makeExpressiveChip("SFTP", false) {
                        activeSftpServer = srv
                        remoteCurrentPath = if (srv.username == "root") "/root" else "/home/${srv.username}"
                        previousTab = activeTab
                        activeTab = 1
                        renderActiveTab()
                    })

                    card.addView(
                        FrameLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(34), dp(36))
                            addView(MaterialIconView(this@TermuxActivity, M3Icon.MORE_VERT, COLOR_TEXT, 20f))
                            attachSmoothPressAnimation(this)
                            setOnClickListener {
                                triggerHaptic(0.8f)
                                val (mDlg, mSheet) = createExpressiveDialog(390)
                                mSheet.addView(TextView(this@TermuxActivity).apply {
                                    text = srv.label
                                    setTextColor(COLOR_TEXT_BRIGHT)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                                    typeface = Typeface.DEFAULT_BOLD
                                    setPadding(dp(4), 0, dp(4), dp(2))
                                })
                                mSheet.addView(TextView(this@TermuxActivity).apply {
                                    text = "${srv.username}@${srv.host}:${srv.port}"
                                    setTextColor(COLOR_TEXT_DIM)
                                    typeface = Typeface.MONOSPACE
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                                    setPadding(dp(4), 0, dp(4), dp(10))
                                })
                                val actions = listOf(
                                    Triple(M3Icon.TERMINAL, tr("Подключиться в терминале", "Connect in terminal")) {
                                        connectToSshServerInTerminal(srv, dlg)
                                    },
                                    Triple(M3Icon.FOLDER, tr("Открыть проводник SFTP", "Open SFTP explorer")) {
                                        activeSftpServer = srv
                                        remoteCurrentPath = if (srv.username == "root") "/root" else "/home/${srv.username}"
                                        previousTab = activeTab
                                        activeTab = 1
                                        renderActiveTab()
                                    },
                                    Triple(M3Icon.PING, tr("Пинговать сервер", "Ping server")) {
                                        pingServerAsync(srv, pingTv)
                                    },
                                    Triple(M3Icon.COPY, tr("Скопировать команду SSH", "Copy SSH command")) {
                                        copyToClipboard(buildSshCommandViaCpp23(srv, withAskPass = false), true)
                                    },
                                    Triple(M3Icon.KEY, tr("Установить мой ключ (ssh-copy-id)", "Install my SSH key (ssh-copy-id)")) {
                                        dlg.dismiss()
                                        createSession(false, runStartupCmd = false)
                                        val copyCmd = "mkdir -p ~/.ssh && ([ -f ~/.ssh/id_ed25519.pub ] || ssh-keygen -t ed25519 -N \"\" -f ~/.ssh/id_ed25519) && ssh-copy-id -p ${srv.port} -o StrictHostKeyChecking=accept-new ${srv.username}@${srv.host}\n"
                                        terminalSurface.postDelayed({ nativeSendText(copyCmd, false) }, 120L)
                                    },
                                    Triple(M3Icon.EDIT, tr("Изменить настройки и порты", "Edit server & ports")) {
                                        showAddOrEditServerDialog(srv) { renderServersTab() }
                                    },
                                    Triple(M3Icon.TRASH, tr("Удалить сервер", "Delete server")) {
                                        val updated = loadSshServers().filter { it.id != srv.id }
                                        saveSshServers(updated)
                                        if (activeSftpServer?.id == srv.id) activeSftpServer = null
                                        renderServersTab()
                                    }
                                )
                                for ((aIdx, act) in actions.withIndex()) {
                                    val isDel = (act.first == M3Icon.TRASH)
                                    val row = LinearLayout(this@TermuxActivity).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        gravity = Gravity.CENTER_VERTICAL
                                        setPadding(dp(12), dp(11), dp(12), dp(11))
                                        background = segmentedCardBg(aIdx, actions.size, uiSurfaceLow, 0, 16f, 4f)
                                        layoutParams = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        ).apply {
                                            bottomMargin = dp(2)
                                        }
                                        attachSmoothPressAnimation(this, 0.97f)
                                        setOnClickListener {
                                            triggerHaptic(0.8f)
                                            mDlg.dismiss()
                                            act.third()
                                        }
                                    }
                                    row.addView(
                                        FrameLayout(this@TermuxActivity).apply {
                                            background = tonalBg(
                                                if (isDel) COLOR_ROOT_CONTAINER else uiAccentContainer,
                                                dp(10)
                                            )
                                            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                                                marginEnd = dp(12)
                                            }
                                            addView(
                                                MaterialIconView(
                                                    this@TermuxActivity,
                                                    act.first,
                                                    if (isDel) COLOR_ROOT else uiAccent,
                                                    18.5f,
                                                    2.0f
                                                )
                                            )
                                        }
                                    )
                                    row.addView(TextView(this@TermuxActivity).apply {
                                        text = act.second
                                        setTextColor(if (isDel) COLOR_ROOT else COLOR_TEXT_BRIGHT)
                                        typeface = if (isDel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                    })
                                    mSheet.addView(row)
                                }
                                mDlg.show()
                            }
                        }
                    )
                    listCol.addView(card)
                }
            }

            val addFab = FrameLayout(this).apply {
                background = tonalBg(uiAccentContainer, dp(20))
                elevation = dpF(8f)
                layoutParams = FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM or Gravity.END).apply {
                    marginEnd = dp(20)
                    bottomMargin = dp(18)
                }
                addView(MaterialIconView(this@TermuxActivity, M3Icon.ADD, uiAccent, 26f, 2.35f))
                attachSmoothPressAnimation(this, 0.90f)
                setOnClickListener {
                    triggerHaptic(1.0f)
                    showAddOrEditServerDialog(null) { renderServersTab() }
                }
            }
            contentFrame.addView(addFab)
        }

        // ====================================================================
        // TAB 1: SFTP & File Explorer (Segmented Source Bar + Gray/Accent Borderless Breadcrumbs + Material 3 Floating Bar)
        // ====================================================================
        fun listLocalEntriesViaCpp23(dir: File, depth: Int = 0): List<SftpItemEntry> {
            val raw = nativeListLocalDir(dir.absolutePath, sftpShowHidden, if (depth == 0) sftpSearchQuery else "")
            if (raw.isBlank()) return emptyList()
            val out = ArrayList<SftpItemEntry>()
            for (line in raw.lineSequence()) {
                if (line.isBlank()) continue
                val cols = line.split("\t")
                if (cols.size >= 6) {
                    val name = cols[0]
                    val fullPath = cols[1]
                    val isDir = cols[2] == "1"
                    val sz = cols[3].toLongOrNull() ?: 0L
                    val mt = cols[4].toLongOrNull() ?: 0L
                    val perms = cols[5]
                    val isExp = isDir && expandedTreePaths.contains(fullPath)
                    out.add(
                        SftpItemEntry(
                            name = name,
                            path = fullPath,
                            isDirectory = isDir,
                            sizeBytes = sz,
                            modifiedEpochMs = mt,
                            permissions = perms,
                            depth = depth,
                            isExpanded = isExp
                        )
                    )
                    if (sftpTreeMode && isExp && depth < 5) {
                        out.addAll(listLocalEntriesViaCpp23(File(fullPath), depth + 1))
                    }
                }
            }
            return out
        }

        fun listRemoteSftpEntriesViaCpp23(server: SshServerProfile, remoteDir: String): Pair<String?, List<SftpItemEntry>> {
            val pattern = if (sftpShowHidden) "* .[!.]* ..?*" else "*"
            val remoteScript = "cd ${shellEscapeSingle(remoteDir)} 2>/dev/null && pwd && " +
                "for f in $pattern; do " +
                "[ -e \"\$f\" ] || continue; " +
                "if [ -d \"\$f\" ]; then t=D; else t=F; fi; " +
                "sz=\$(stat -c '%s' \"\$f\" 2>/dev/null || wc -c < \"\$f\" 2>/dev/null || echo 0); " +
                "mt=\$(stat -c '%Y' \"\$f\" 2>/dev/null || echo 0); " +
                "pm=\$(stat -c '%A' \"\$f\" 2>/dev/null || echo ''); " +
                "printf '%s\\t%s\\t%s\\t%s\\t%s\\n' \"\$t\" \"\$sz\" \"\$mt\" \"\$pm\" \"\$f\"; " +
                "done"
            val (ok, output) = runRemoteSftpViaCpp23(server, remoteScript)
            if (!ok) return output.ifBlank { tr("Не удалось подключиться по SFTP/SSH", "Failed to connect via SFTP/SSH") } to emptyList()
            val lines = output.lines().filter { it.isNotBlank() }
            val resolvedPwd = lines.firstOrNull()?.trim()?.takeIf { it.startsWith("/") } ?: remoteDir
            remoteCurrentPath = resolvedPwd
            val items = ArrayList<SftpItemEntry>()
            val q = sftpSearchQuery.trim()
            for (i in 1 until lines.size) {
                val cols = lines[i].split("\t")
                if (cols.size >= 5) {
                    val isDir = cols[0] == "D"
                    val sz = cols[1].trim().toLongOrNull() ?: 0L
                    val mtSec = cols[2].trim().toLongOrNull() ?: 0L
                    val pm = cols[3].trim()
                    val name = cols[4].trim()
                    if (name == "." || name == "..") continue
                    if (q.isNotEmpty() && !name.contains(q, ignoreCase = true)) continue
                    val fullP = if (resolvedPwd == "/") "/$name" else "$resolvedPwd/$name"
                    items.add(
                        SftpItemEntry(
                            name = name,
                            path = fullP,
                            isDirectory = isDir,
                            sizeBytes = sz,
                            modifiedEpochMs = mtSec * 1000L,
                            permissions = pm
                        )
                    )
                }
            }
            items.sortWith(compareBy<SftpItemEntry>({ !it.isDirectory }, { it.name.lowercase() }))
            return null to items
        }

        fun renderSftpTab(animateTab: Boolean = false) {
            contentFrame.removeAllViews()
            if (animateTab) animateAosp17PageTransition(contentFrame, forward = activeTab >= previousTab)

            val mainSftpCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            contentFrame.addView(mainSftpCol)

            // 1. Clean Material 3 Segmented Source Group Bar (Home | Usr | SDCard | SSH) — Borderless!
            val sourceBarWrap = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = tonalBg(uiSurfaceLow, dp(16))
                setPadding(dp(4), dp(4), dp(4), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(14), dp(8), dp(14), dp(4))
                }
            }

            val usrDir = File("/data/data/com.termuxx/file/usr")
            val sdDir = Environment.getExternalStorageDirectory()
            val savedServers = loadSshServers()

            val localGroups = listOf(
                "Home (~)" to homeDir,
                "Usr" to usrDir,
                "SDCard" to sdDir
            )
            for ((lbl, dir) in localGroups) {
                val isSel = (activeSftpServer == null && localCurrentDir.absolutePath.startsWith(dir.absolutePath))
                val segBtn = TextView(this).apply {
                    text = lbl
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                    if (isSel) {
                        background = tonalBg(uiAccentContainer, dp(12))
                    }
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    attachSmoothPressAnimation(this, 0.95f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        activeSftpServer = null
                        if (!dir.exists()) dir.mkdirs()
                        if (dir.exists() && dir.canRead()) {
                            localCurrentDir = dir
                            renderSftpTab()
                        } else {
                            Toast.makeText(this@TermuxActivity, tr("Нет доступа", "Access denied"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                sourceBarWrap.addView(segBtn)
            }

            if (savedServers.isNotEmpty()) {
                val isRemoteSel = (activeSftpServer != null)
                val sshSegBtn = TextView(this).apply {
                    text = if (isRemoteSel) "SSH: ${activeSftpServer!!.label}" else "SSH ▾"
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    typeface = if (isRemoteSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setTextColor(if (isRemoteSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                    if (isRemoteSel) {
                        background = tonalBg(uiAccentContainer, dp(12))
                    }
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f)
                    attachSmoothPressAnimation(this, 0.95f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        if (savedServers.size == 1 && activeSftpServer == null) {
                            val srv = savedServers[0]
                            activeSftpServer = srv
                            remoteCurrentPath = if (srv.username == "root") "/root" else "/home/${srv.username}"
                            renderSftpTab()
                        } else {
                            val curIdx = savedServers.indexOfFirst { it.id == activeSftpServer?.id }.coerceAtLeast(0)
                            showBakaSuChooseDialog(
                                title = tr("Выберите SSH-сервер", "Select SSH Server"),
                                items = savedServers.map { it.label },
                                descriptions = savedServers.map { "${it.username}@${it.host}:${it.port}" },
                                selectedIndex = curIdx
                            ) { chosen ->
                                val srv = savedServers[chosen]
                                activeSftpServer = srv
                                remoteCurrentPath = if (srv.username == "root") "/root" else "/home/${srv.username}"
                                renderSftpTab()
                            }
                        }
                    }
                }
                sourceBarWrap.addView(sshSegBtn)
            }
            mainSftpCol.addView(sourceBarWrap)

            // 2. Material 3 Path Breadcrumbs:
            // All parent path segments have a soft gray borderless pill (`uiSurfaceContainer`),
            // and ONLY the last segment (current directory) has a theme-colored borderless pill (`uiAccentContainer`)!
            val activePathStr = if (activeSftpServer == null) localCurrentDir.absolutePath else remoteCurrentPath
            val (bcScroll, bcRow) = makeHorizontalChipRow()
            bcScroll.setPadding(dp(14), dp(4), dp(14), dp(6))

            val parts = activePathStr.split("/").filter { it.isNotEmpty() }
            val segments = ArrayList<Pair<String, String>>()
            segments.add("/" to "/")
            var built = ""
            for (p in parts) {
                built += "/$p"
                segments.add(p to built)
            }
            for ((idx, seg) in segments.withIndex()) {
                val isLast = (idx == segments.lastIndex)
                if (idx > 0) {
                    bcRow.addView(TextView(this).apply {
                        text = "›"
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                        setPadding(dp(4), 0, dp(4), 0)
                    })
                }
                val segChip = TextView(this).apply {
                    text = seg.first
                    setSingleLine(true)
                    typeface = if (isLast) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    // Borderless capsule: gray for parent segments, theme color for the current folder at the end!
                    if (isLast) {
                        setTextColor(uiAccent)
                        background = tonalBg(uiAccentContainer, dp(10))
                        setPadding(dp(12), dp(5), dp(12), dp(5))
                    } else {
                        setTextColor(COLOR_TEXT)
                        background = tonalBg(uiSurfaceContainer, dp(10))
                        setPadding(dp(10), dp(5), dp(10), dp(5))
                    }
                    attachSmoothPressAnimation(this, 0.94f)
                    setOnClickListener {
                        triggerHaptic(0.75f)
                        if (isLast) {
                            val (pDlg, pSheet) = createExpressiveDialog(390)
                            pSheet.addView(TextView(this@TermuxActivity).apply {
                                text = tr("Перейти по пути", "Go to path")
                                setTextColor(COLOR_TEXT_BRIGHT)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                typeface = Typeface.DEFAULT_BOLD
                                setPadding(0, 0, 0, dp(8))
                            })
                            val pathEt = EditText(this@TermuxActivity).apply {
                                setText(activePathStr)
                                setTextColor(COLOR_TEXT_BRIGHT)
                                typeface = Typeface.MONOSPACE
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                                setSingleLine(true)
                                background = tonalBg(uiSurfaceLow, dp(12))
                                setPadding(dp(12), dp(9), dp(12), dp(9))
                            }
                            pSheet.addView(pathEt)
                            val bRow = LinearLayout(this@TermuxActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.END
                                setPadding(0, dp(12), 0, 0)
                            }
                            bRow.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { pDlg.dismiss() })
                            bRow.addView(makeExpressiveChip(tr("Перейти", "Go"), true) {
                                val target = pathEt.text.toString().trim()
                                if (target.isNotEmpty()) {
                                    if (activeSftpServer == null) {
                                        val f = File(target)
                                        if (f.exists() && f.isDirectory) localCurrentDir = f
                                    } else {
                                        remoteCurrentPath = target
                                    }
                                    renderSftpTab()
                                }
                                pDlg.dismiss()
                            })
                            pSheet.addView(bRow)
                            pDlg.show()
                        } else if (activeSftpServer == null) {
                            val targetDir = File(seg.second)
                            if (targetDir.exists() && targetDir.canRead()) {
                                localCurrentDir = targetDir
                                renderSftpTab()
                            }
                        } else {
                            remoteCurrentPath = seg.second
                            renderSftpTab()
                        }
                    }
                }
                bcRow.addView(segChip)
            }
            bcScroll.post { bcScroll.fullScroll(View.FOCUS_RIGHT) }
            mainSftpCol.addView(bcScroll)

            val clipFile = fileClipSource
            if (activeSftpServer == null && clipFile != null && clipFile.exists()) {
                val clipBanner = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(4), dp(14), dp(4))
                }
                clipBanner.addView(makeExpressiveChip(tr("Вставить «${clipFile.name}»", "Paste '${clipFile.name}'"), true) {
                    try {
                        val dst = File(localCurrentDir, clipFile.name)
                        if (clipFile.isDirectory) clipFile.copyRecursively(dst, overwrite = true)
                        else clipFile.copyTo(dst, overwrite = true)
                        if (fileClipIsMove) clipFile.deleteRecursively()
                        fileClipSource = null
                        renderSftpTab()
                    } catch (e: Throwable) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                })
                clipBanner.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) {
                    fileClipSource = null
                    renderSftpTab()
                })
                mainSftpCol.addView(clipBanner)
            }

            val listScroll = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val rowsCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(4), dp(14), dp(104))
            }
            listScroll.addView(rowsCol)

            val ptrSftp = BakaSuPullToRefreshLayout(this, listScroll) {
                renderSftpTab()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            mainSftpCol.addView(ptrSftp)

            val srv = activeSftpServer
            val hasParentRow = (activePathStr != "/" && sftpSearchQuery.isBlank())

            val (errStr, entries) = if (srv == null) {
                null to listLocalEntriesViaCpp23(localCurrentDir, 0)
            } else {
                listRemoteSftpEntriesViaCpp23(srv, remoteCurrentPath)
            }

            val totalCards = entries.size + (if (hasParentRow) 1 else 0)
            var currentCardIdx = 0

            if (hasParentRow) {
                val upCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(11), dp(14), dp(11))
                    background = segmentedCardBg(currentCardIdx, max(1, totalCards), uiSurfaceLow, 0, 20f, 5f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(2)
                    }
                    attachSmoothPressAnimation(this, 0.98f)
                    setOnClickListener {
                        triggerHaptic(0.75f)
                        val parentPath = activePathStr.substringBeforeLast('/').ifEmpty { "/" }
                        if (srv == null) {
                            val pf = File(parentPath)
                            if (pf.exists() && pf.canRead()) {
                                localCurrentDir = pf
                                renderSftpTab()
                            }
                        } else {
                            remoteCurrentPath = parentPath
                            renderSftpTab()
                        }
                    }
                }
                upCard.addView(
                    FrameLayout(this).apply {
                        background = tonalBg(uiAccentContainer, dp(11))
                        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                            marginEnd = dp(12)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.FOLDER, uiAccent, 21f, 2.0f))
                    }
                )
                val upCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                upCol.addView(TextView(this).apply {
                    text = ".."
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                })
                upCol.addView(TextView(this).apply {
                    text = tr("Родительская папка", "Parent directory")
                    setTextColor(COLOR_TEXT_DIM)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                })
                upCard.addView(upCol)
                rowsCol.addView(upCard)
                currentCardIdx++
            }

            fun openEntryMenu(item: SftpItemEntry) {
                val (mDlg, mSheet) = createExpressiveDialog(390)
                val hdrRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(2), 0, dp(2), dp(10))
                }
                hdrRow.addView(
                    FrameLayout(this).apply {
                        background = tonalBg(
                            if (item.isDirectory) uiAccentContainer else uiSurfaceLow,
                            dp(12)
                        )
                        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                            marginEnd = dp(12)
                        }
                        addView(
                            MaterialIconView(
                                this@TermuxActivity,
                                if (item.isDirectory) M3Icon.FOLDER else M3Icon.FILE,
                                if (item.isDirectory) uiAccent else COLOR_TEXT_BRIGHT,
                                21f,
                                2.0f
                            )
                        )
                    }
                )
                val hdrCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                hdrCol.addView(TextView(this).apply {
                    text = item.name
                    setSingleLine(true)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                })
                hdrCol.addView(TextView(this).apply {
                    text = item.path
                    setSingleLine(true)
                    setTextColor(COLOR_TEXT_DIM)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                })
                hdrRow.addView(hdrCol)
                mSheet.addView(hdrRow)

                val opts = ArrayList<Triple<M3Icon, String, () -> Unit>>()
                if (!item.isDirectory) {
                    opts.add(Triple(M3Icon.EDIT, tr("Открыть / Редактировать", "Open / Edit code")) {
                        if (srv == null) {
                            val f = File(item.path)
                            val content = try {
                                if (f.length() <= 512 * 1024) f.readText() else "File > 512 KB"
                            } catch (_: Throwable) { "" }
                            showCodeViewerAndEditorDialog(item.name, item.path, content) { updated ->
                                try {
                                    f.writeText(updated)
                                    renderSftpTab()
                                    true
                                } catch (_: Throwable) { false }
                            }
                        } else {
                            val (_, content) = runRemoteSftpViaCpp23(srv, "head -c 262144 ${shellEscapeSingle(item.path)}")
                            showCodeViewerAndEditorDialog(item.name, "${srv.label}:${item.path}", content) { updated ->
                                val b64 = android.util.Base64.encodeToString(updated.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                val (ok, _) = runRemoteSftpViaCpp23(
                                    srv,
                                    "printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(item.path)}"
                                )
                                if (ok) renderSftpTab()
                                ok
                            }
                        }
                    })
                    opts.add(Triple(M3Icon.DOWNLOAD, tr("Скачать в Загрузки", "Download to /sdcard/Download")) {
                        val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
                        val dst = File(dlDir, item.name)
                        if (srv == null) {
                            try {
                                File(item.path).copyTo(dst, overwrite = true)
                                Toast.makeText(this, tr("Сохранено: ${dst.absolutePath}", "Saved: ${dst.absolutePath}"), Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Thread {
                                val (ok, b64Out) = runRemoteSftpViaCpp23(srv, "base64 ${shellEscapeSingle(item.path)}")
                                runOnUiThread {
                                    if (ok) {
                                        try {
                                            val bytes = android.util.Base64.decode(b64Out.replace("\n", "").replace("\r", ""), android.util.Base64.DEFAULT)
                                            dst.writeBytes(bytes)
                                            Toast.makeText(this, tr("Скачано: ${dst.name}", "Downloaded: ${dst.name}"), Toast.LENGTH_SHORT).show()
                                        } catch (e: Throwable) {
                                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }.start()
                        }
                    })
                } else {
                    opts.add(Triple(M3Icon.TERMINAL, tr("Открыть папку в терминале (cd)", "Open folder in terminal (cd)")) {
                        if (srv == null) {
                            dlg.dismiss()
                            nativeSendText("cd ${shellEscapeSingle(item.path)}\n", false)
                        } else {
                            connectToSshServerInTerminal(
                                srv.copy(startupCommand = "cd ${shellEscapeSingle(item.path)} && exec \$SHELL -l"),
                                dlg
                            )
                        }
                    })
                }
                opts.add(Triple(M3Icon.EDIT, tr("Переименовать", "Rename")) {
                    val (rDlg, rSheet) = createExpressiveDialog(380)
                    rSheet.addView(TextView(this).apply {
                        text = tr("Переименовать", "Rename")
                        setTextColor(COLOR_TEXT_BRIGHT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(0, 0, 0, dp(8))
                    })
                    val input = EditText(this).apply {
                        setText(item.name)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setSingleLine(true)
                        background = tonalBg(uiSurfaceLow, dp(12))
                        setPadding(dp(12), dp(9), dp(12), dp(9))
                    }
                    rSheet.addView(input)
                    val bBar = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(12), 0, 0)
                    }
                    bBar.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { rDlg.dismiss() })
                    bBar.addView(makeExpressiveChip(tr("Сохранить", "Save"), true) {
                        val newName = input.text.toString().trim()
                        if (newName.isNotEmpty() && !newName.contains('/') && newName != item.name) {
                            if (srv == null) {
                                val f = File(item.path)
                                f.renameTo(File(f.parentFile, newName))
                            } else {
                                val parent = item.path.substringBeforeLast('/').ifEmpty { "/" }
                                val newPath = if (parent == "/") "/$newName" else "$parent/$newName"
                                runRemoteSftpViaCpp23(srv, "mv ${shellEscapeSingle(item.path)} ${shellEscapeSingle(newPath)}")
                            }
                            renderSftpTab()
                        }
                        rDlg.dismiss()
                    })
                    rSheet.addView(bBar)
                    rDlg.show()
                })
                opts.add(Triple(M3Icon.KEY, tr("Права доступа (chmod)", "Permissions (chmod)")) {
                    showBakaSuChooseDialog(
                        title = "chmod: ${item.name}",
                        items = listOf("755 (rwxr-xr-x)", "700 (rwx------)", "644 (rw-r--r--)", "600 (rw-------)"),
                        selectedIndex = 0
                    ) { which ->
                        val code = arrayOf("755", "700", "644", "600")[which]
                        if (srv == null) {
                            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", code, item.path)).waitFor()
                        } else {
                            runRemoteSftpViaCpp23(srv, "chmod $code ${shellEscapeSingle(item.path)}")
                        }
                        renderSftpTab()
                    }
                })
                if (srv == null) {
                    opts.add(Triple(M3Icon.COPY, tr("Копировать", "Copy")) {
                        fileClipSource = File(item.path)
                        fileClipIsMove = false
                        renderSftpTab()
                    })
                }
                opts.add(Triple(M3Icon.COPY, tr("Скопировать путь", "Copy path")) {
                    copyToClipboard(item.path, true)
                })
                opts.add(Triple(M3Icon.TRASH, tr("Удалить", "Delete")) {
                    if (srv == null) {
                        File(item.path).deleteRecursively()
                    } else {
                        runRemoteSftpViaCpp23(srv, "rm -rf ${shellEscapeSingle(item.path)}")
                    }
                    renderSftpTab()
                })

                for ((oIdx, opt) in opts.withIndex()) {
                    val isDel = (opt.first == M3Icon.TRASH)
                    val optRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(11), dp(12), dp(11))
                        background = segmentedCardBg(oIdx, opts.size, uiSurfaceLow, 0, 16f, 4f)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(2)
                        }
                        attachSmoothPressAnimation(this, 0.97f)
                        setOnClickListener {
                            triggerHaptic(0.8f)
                            mDlg.dismiss()
                            opt.third()
                        }
                    }
                    optRow.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(
                                if (isDel) COLOR_ROOT_CONTAINER else uiAccentContainer,
                                dp(10)
                            )
                            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                                marginEnd = dp(12)
                            }
                            addView(
                                MaterialIconView(
                                    this@TermuxActivity,
                                    opt.first,
                                    if (isDel) COLOR_ROOT else uiAccent,
                                    18.5f,
                                    2.0f
                                )
                            )
                        }
                    )
                    optRow.addView(TextView(this).apply {
                        text = opt.second
                        setTextColor(if (isDel) COLOR_ROOT else COLOR_TEXT_BRIGHT)
                        typeface = if (isDel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    })
                    mSheet.addView(optRow)
                }
                mDlg.show()
            }

            if (errStr != null) {
                rowsCol.addView(TextView(this).apply {
                    text = errStr
                    setTextColor(COLOR_ROOT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(dp(18), dp(24), dp(18), dp(24))
                })
            } else if (entries.isEmpty()) {
                rowsCol.addView(TextView(this).apply {
                    text = if (sftpSearchQuery.isNotBlank()) tr("Нет файлов по запросу «$sftpSearchQuery»", "No files matching '$sftpSearchQuery'") else tr("Папка пуста", "Folder is empty")
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_DIM)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setPadding(dp(20), dp(48), dp(20), dp(48))
                })
            } else {
                for (item in entries) {
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14 + item.depth * 14), dp(11), dp(10), dp(11))
                        background = segmentedCardBg(currentCardIdx, max(1, totalCards), uiSurfaceLow, 0, 20f, 5f)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(2)
                        }
                        attachSmoothPressAnimation(this, 0.985f)
                        setOnClickListener {
                            triggerHaptic(0.75f)
                            if (item.isDirectory) {
                                if (srv == null && sftpTreeMode) {
                                    if (expandedTreePaths.contains(item.path)) {
                                        expandedTreePaths.remove(item.path)
                                    } else {
                                        expandedTreePaths.add(item.path)
                                    }
                                    renderSftpTab()
                                } else if (srv == null) {
                                    val f = File(item.path)
                                    if (f.canRead()) {
                                        localCurrentDir = f
                                        renderSftpTab()
                                    }
                                } else {
                                    remoteCurrentPath = item.path
                                    renderSftpTab()
                                }
                            } else {
                                openEntryMenu(item)
                            }
                        }
                        setOnLongClickListener {
                            triggerHaptic(1.2f)
                            openEntryMenu(item)
                            true
                        }
                    }
                    currentCardIdx++

                    if (sftpTreeMode && srv == null && item.isDirectory) {
                        row.addView(
                            MaterialIconView(
                                this,
                                if (item.isExpanded) M3Icon.CHEVRON_DOWN else M3Icon.CHEVRON_RIGHT,
                                COLOR_TEXT_DIM,
                                17f,
                                2.0f
                            ).apply {
                                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                                    marginEnd = dp(4)
                                }
                            }
                        )
                    }

                    val iconType = when {
                        !item.isDirectory -> M3Icon.FILE
                        item.isExpanded -> M3Icon.FOLDER_OPEN
                        else -> M3Icon.FOLDER
                    }
                    val iconTint = if (item.isDirectory) uiAccent else COLOR_TEXT_BRIGHT
                    row.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(
                                if (item.isDirectory) uiAccentContainer else uiSurfaceContainer,
                                dp(11)
                            )
                            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                                marginEnd = dp(12)
                            }
                            addView(MaterialIconView(this@TermuxActivity, iconType, iconTint, 20.5f, 2.0f))
                        }
                    )

                    val metaCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    metaCol.addView(TextView(this).apply {
                        text = item.name
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = if (item.isDirectory) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    })
                    val sub = buildString {
                        if (!item.isDirectory) {
                            append(formatSize(item.sizeBytes))
                        }
                        val dt = formatTimestamp(item.modifiedEpochMs)
                        if (dt.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append(dt)
                        }
                        if (item.permissions.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append(item.permissions)
                        }
                    }
                    if (sub.isNotEmpty()) {
                        metaCol.addView(TextView(this).apply {
                            text = sub
                            setSingleLine(true)
                            setTextColor(COLOR_TEXT_DIM)
                            typeface = Typeface.MONOSPACE
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                            setPadding(0, dp(2), 0, 0)
                        })
                    }
                    row.addView(metaCol)

                    row.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(uiSurfaceContainer, dp(10))
                            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                                marginStart = dp(6)
                            }
                            addView(MaterialIconView(this@TermuxActivity, M3Icon.MORE_VERT, COLOR_TEXT_DIM, 19.5f))
                            attachSmoothPressAnimation(this)
                            setOnClickListener {
                                triggerHaptic(0.8f)
                                openEntryMenu(item)
                            }
                        }
                    )
                    rowsCol.addView(row)
                }
            }

            // 4. Redesigned Material Design 3 Floating Bottom Bar in SFTP (Borderless Tonal Dock + Squircle FAB)
            val bottomBarRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
                clipChildren = false
                clipToPadding = false
                setPadding(dp(18), dp(4), dp(18), dp(16))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                )
            }

            val floatingPill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = makeM3FloatingSurfaceDrawable(dp(31), tintWithAccent = false)
                elevation = dpF(12f)
                setPadding(dp(6), dp(5), dp(6), dp(5))
                layoutParams = if (sftpSearchActive) {
                    LinearLayout.LayoutParams(0, dp(62), 1f)
                } else {
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(62))
                }
            }

            fun makePillVectorBtn(icon: M3Icon, active: Boolean = false, onClick: () -> Unit): FrameLayout {
                return FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(50)).apply {
                        setMargins(dp(2), 0, dp(2), 0)
                    }
                    if (active) {
                        background = tonalBg(uiAccentContainer, dp(25))
                    }
                    addView(
                        MaterialIconView(
                            this@TermuxActivity,
                            icon = icon,
                            tintColor = if (active) uiAccent else COLOR_TEXT_BRIGHT,
                            iconSizeDp = 23f,
                            strokeWidthDp = 2.15f
                        )
                    )
                    attachSmoothPressAnimation(this, 0.88f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        onClick()
                    }
                }
            }

            if (sftpSearchActive) {
                floatingPill.addView(makePillVectorBtn(M3Icon.ARROW_BACK) {
                    sftpSearchActive = false
                    sftpSearchQuery = ""
                    renderSftpTab()
                })
                val searchEt = EditText(this).apply {
                    setText(sftpSearchQuery)
                    hint = tr("Поиск в папке…", "Search in folder…")
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setSingleLine(true)
                    background = null
                    setPadding(dp(6), 0, dp(6), 0)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                floatingPill.addView(searchEt)
                floatingPill.addView(makePillVectorBtn(M3Icon.SEARCH, true) {
                    sftpSearchQuery = searchEt.text.toString()
                    renderSftpTab()
                })
                if (sftpSearchQuery.isNotEmpty()) {
                    floatingPill.addView(makePillVectorBtn(M3Icon.CLOSE) {
                        sftpSearchQuery = ""
                        renderSftpTab()
                    })
                }
            } else {
                floatingPill.addView(makePillVectorBtn(M3Icon.SEARCH) {
                    sftpSearchActive = true
                    renderSftpTab()
                })
                floatingPill.addView(makePillVectorBtn(M3Icon.TREE, sftpTreeMode) {
                    sftpTreeMode = !sftpTreeMode
                    renderSftpTab()
                })
                floatingPill.addView(makePillVectorBtn(M3Icon.EYE, sftpShowHidden) {
                    sftpShowHidden = !sftpShowHidden
                    renderSftpTab()
                })
                floatingPill.addView(makePillVectorBtn(M3Icon.CREATE_FOLDER) {
                    val (cDlg, cSheet) = createExpressiveDialog(420)
                    var createIsFolder = true
                    var selectedChmod = "755"

                    val hdr = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 0, 0, dp(12))
                    }
                    val hdrBadge = FrameLayout(this).apply {
                        background = tonalBg(uiAccentContainer, dp(14))
                        layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply {
                            marginEnd = dp(12)
                        }
                    }
                    val hdrIcon = MaterialIconView(this, M3Icon.CREATE_FOLDER, uiAccent, 24f, 2.15f)
                    hdrBadge.addView(hdrIcon)
                    hdr.addView(hdrBadge)

                    val hdrTextCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val hdrTitle = TextView(this).apply {
                        text = tr("Новая папка", "New folder")
                        setTextColor(COLOR_TEXT_BRIGHT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val hdrSub = TextView(this).apply {
                        text = activePathStr
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setPadding(0, dp(2), 0, 0)
                    }
                    hdrTextCol.addView(hdrTitle)
                    hdrTextCol.addView(hdrSub)
                    hdr.addView(hdrTextCol)
                    cSheet.addView(hdr)

                    val typeRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 0, 0, dp(12))
                    }

                    val nameEt = EditText(this).apply {
                        hint = tr("Введите имя", "Enter name")
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                        setSingleLine(true)
                        background = tonalBg(uiSurfaceLow, dp(16))
                        setPadding(dp(16), dp(13), dp(16), dp(13))
                    }

                    val contentLabel = TextView(this).apply {
                        text = tr("Содержимое файла (опционально)", "File content (optional)")
                        setTextColor(uiAccent)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(dp(2), dp(10), dp(2), dp(4))
                    }
                    val contentEt = EditText(this).apply {
                        hint = "#!/usr/bin/env bash\n..."
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                        minLines = 3
                        maxLines = 5
                        gravity = Gravity.TOP or Gravity.START
                        background = tonalBg(uiSurfaceLow, dp(16))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                    }

                    val chmodLabel = TextView(this).apply {
                        text = "chmod"
                        setTextColor(uiAccent)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(dp(2), dp(10), dp(2), dp(4))
                    }
                    val (chmodScroll, chmodRow) = makeHorizontalChipRow()

                    lateinit var rebuildCreateDialogState: () -> Unit
                    rebuildCreateDialogState = {
                        typeRow.removeAllViews()
                        val types = listOf(
                            Triple(true, M3Icon.FOLDER, tr("Новая папка", "New folder")),
                            Triple(false, M3Icon.FILE, tr("Новый файл", "New file"))
                        )
                        for ((isFolderOpt, ic, lbl) in types) {
                            val sel = (createIsFolder == isFolderOpt)
                            val card = LinearLayout(this).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                setPadding(dp(14), dp(12), dp(14), dp(12))
                                background = tonalBg(
                                    if (sel) uiAccentContainer else uiSurfaceLow,
                                    dp(16)
                                )
                                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                                    if (isFolderOpt) marginEnd = dp(8)
                                }
                                attachSmoothPressAnimation(this, 0.95f)
                                setOnClickListener {
                                    triggerHaptic(0.8f)
                                    createIsFolder = isFolderOpt
                                    selectedChmod = if (createIsFolder) "755" else "644"
                                    rebuildCreateDialogState()
                                }
                            }
                            card.addView(
                                MaterialIconView(
                                    this,
                                    ic,
                                    if (sel) uiAccent else COLOR_TEXT,
                                    20f,
                                    2.0f
                                ).apply {
                                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                                        marginEnd = dp(8)
                                    }
                                }
                            )
                            card.addView(TextView(this).apply {
                                text = lbl
                                setTextColor(if (sel) uiAccent else COLOR_TEXT_BRIGHT)
                                typeface = if (sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                            })
                            typeRow.addView(card)
                        }

                        hdrIcon.icon = if (createIsFolder) M3Icon.CREATE_FOLDER else M3Icon.FILE
                        hdrIcon.invalidate()
                        hdrTitle.text = if (createIsFolder) tr("Создать папку", "Create folder") else tr("Создать файл", "Create file")
                        nameEt.hint = if (createIsFolder) tr("Имя папки (напр. scripts)", "Folder name (e.g. scripts)") else tr("Имя файла (напр. main.cpp)", "File name (e.g. main.cpp)")
                        contentLabel.visibility = if (createIsFolder) View.GONE else View.VISIBLE
                        contentEt.visibility = if (createIsFolder) View.GONE else View.VISIBLE

                        chmodRow.removeAllViews()
                        for (mode in listOf("755", "644", "700", "600")) {
                            chmodRow.addView(makeExpressiveChip("chmod $mode", selectedChmod == mode) {
                                selectedChmod = mode
                                rebuildCreateDialogState()
                            })
                        }
                    }

                    cSheet.addView(typeRow)
                    cSheet.addView(nameEt)
                    cSheet.addView(contentLabel)
                    cSheet.addView(contentEt)
                    cSheet.addView(chmodLabel)
                    cSheet.addView(chmodScroll)
                    rebuildCreateDialogState()

                    val bRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(16), 0, 0)
                    }
                    bRow.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { cDlg.dismiss() })
                    bRow.addView(makeExpressiveChip(tr("Создать", "Create"), true) {
                        val n = nameEt.text.toString().trim()
                        if (n.isEmpty() || n.contains('/')) {
                            Toast.makeText(this, tr("Введите корректное имя", "Enter a valid name"), Toast.LENGTH_SHORT).show()
                            return@makeExpressiveChip
                        }
                        if (srv == null) {
                            val target = File(localCurrentDir, n)
                            if (createIsFolder) {
                                target.mkdirs()
                            } else {
                                target.writeText(contentEt.text.toString())
                            }
                            try {
                                Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", selectedChmod, target.absolutePath)).waitFor()
                            } catch (_: Throwable) {}
                        } else {
                            val fullRemote = if (remoteCurrentPath == "/") "/$n" else "$remoteCurrentPath/$n"
                            if (createIsFolder) {
                                runRemoteSftpViaCpp23(srv, "mkdir -p ${shellEscapeSingle(fullRemote)} && chmod $selectedChmod ${shellEscapeSingle(fullRemote)}")
                            } else {
                                val initTxt = contentEt.text.toString()
                                if (initTxt.isEmpty()) {
                                    runRemoteSftpViaCpp23(srv, "touch ${shellEscapeSingle(fullRemote)} && chmod $selectedChmod ${shellEscapeSingle(fullRemote)}")
                                } else {
                                    val b64 = android.util.Base64.encodeToString(initTxt.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                    runRemoteSftpViaCpp23(srv, "printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(fullRemote)} && chmod $selectedChmod ${shellEscapeSingle(fullRemote)}")
                                }
                            }
                        }
                        cDlg.dismiss()
                        renderSftpTab()
                    })
                    cSheet.addView(bRow)
                    cDlg.show()
                })
            }

            bottomBarRow.addView(floatingPill)
            bottomBarRow.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(12), dp(1))
            })

            val uploadFab = FrameLayout(this).apply {
                background = makeM3FloatingSurfaceDrawable(dp(20), tintWithAccent = true)
                elevation = dpF(12f)
                layoutParams = LinearLayout.LayoutParams(dp(62), dp(62))
                addView(MaterialIconView(this@TermuxActivity, M3Icon.UPLOAD, uiAccent, 24f, 2.25f))
                attachSmoothPressAnimation(this, 0.90f)
                setOnClickListener {
                    triggerHaptic(1.0f)
                    val (uDlg, uSheet) = createExpressiveDialog(410)
                    uSheet.addView(TextView(this@TermuxActivity).apply {
                        text = if (srv == null) tr("Импорт файла в ${localCurrentDir.name}", "Import file to ${localCurrentDir.name}") else tr("Выгрузить на ${srv.label}", "Upload to ${srv.label}")
                        setTextColor(COLOR_TEXT_BRIGHT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(0, 0, 0, dp(8))
                    })
                    val srcEt = EditText(this@TermuxActivity).apply {
                        setText("/sdcard/Download/")
                        hint = "/sdcard/Download/..."
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                        setSingleLine(true)
                        background = tonalBg(uiSurfaceLow, dp(14))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                    }
                    uSheet.addView(srcEt)
                    val bRow = LinearLayout(this@TermuxActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(14), 0, 0)
                    }
                    bRow.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { uDlg.dismiss() })
                    bRow.addView(makeExpressiveChip(tr("Загрузить", "Upload"), true) {
                        val srcFile = File(srcEt.text.toString().trim())
                        if (!srcFile.exists() || !srcFile.isFile) {
                            Toast.makeText(this@TermuxActivity, tr("Файл не найден", "File not found"), Toast.LENGTH_SHORT).show()
                            return@makeExpressiveChip
                        }
                        uDlg.dismiss()
                        if (srv == null) {
                            try {
                                srcFile.copyTo(File(localCurrentDir, srcFile.name), overwrite = true)
                                renderSftpTab()
                            } catch (e: Throwable) {
                                Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else if (srcFile.length() <= 2 * 1024 * 1024) {
                            val dstRemote = if (remoteCurrentPath == "/") "/${srcFile.name}" else "$remoteCurrentPath/${srcFile.name}"
                            Thread {
                                val b64 = android.util.Base64.encodeToString(srcFile.readBytes(), android.util.Base64.NO_WRAP)
                                val (ok, err) = runRemoteSftpViaCpp23(
                                    srv,
                                    "printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(dstRemote)}"
                                )
                                runOnUiThread {
                                    if (ok) renderSftpTab()
                                    else Toast.makeText(this@TermuxActivity, "Error: $err", Toast.LENGTH_SHORT).show()
                                }
                            }.start()
                        } else {
                            val dstRemote = if (remoteCurrentPath == "/") "/${srcFile.name}" else "$remoteCurrentPath/${srcFile.name}"
                            dlg.dismiss()
                            createSession(false, runStartupCmd = false)
                            val cmd = "scp -P ${srv.port} -o StrictHostKeyChecking=accept-new ${shellEscapeSingle(srcFile.absolutePath)} ${srv.username}@${srv.host}:${shellEscapeSingle(dstRemote)}\n"
                            terminalSurface.postDelayed({ nativeSendText(cmd, false) }, 120L)
                        }
                    })
                    uSheet.addView(bRow)
                    uDlg.show()
                }
            }
            bottomBarRow.addView(uploadFab)
            contentFrame.addView(bottomBarRow)
        }

        // ====================================================================
        // TAB 2: Packages Manager (Separated Removable vs Core System Packages + No Outlines)
        // ====================================================================
        fun renderPackagesTab() {
            contentFrame.removeAllViews()
            animateAosp17PageTransition(contentFrame, forward = activeTab >= previousTab)

            val outer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(6))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            contentFrame.addView(outer)

            val installed = parseInstalledDpkgPackagesViaCpp23()
            val totalKb = installed.sumOf { it.sizeKb }
            val userPackages = installed.filter { !it.isCore }
            val corePackages = installed.filter { it.isCore }

            val summaryCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = tonalBg(uiSurfaceLow, dp(22))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }

            val allBadge = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = tonalBg(uiAccentContainer, dp(13))
                setPadding(dp(11), dp(7), dp(13), dp(7))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(8)
                }
            }
            allBadge.addView(
                MaterialIconView(this, M3Icon.ALL_PACKAGES, uiAccent, 18.5f, 2.0f).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                        marginEnd = dp(6)
                    }
                }
            )
            allBadge.addView(TextView(this).apply {
                text = "All: ${installed.size}"
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            })
            summaryCard.addView(allBadge)

            val sizeBadge = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = tonalBg(uiSurfaceContainer, dp(13))
                setPadding(dp(11), dp(7), dp(13), dp(7))
            }
            sizeBadge.addView(
                MaterialIconView(this, M3Icon.STORAGE, uiAccent, 18.5f, 2.0f).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                        marginEnd = dp(6)
                    }
                }
            )
            sizeBadge.addView(TextView(this).apply {
                text = formatSize(totalKb * 1024L)
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })
            summaryCard.addView(sizeBadge)

            summaryCard.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
            })

            // Runs single-pass `pkg upgrade -y` (since `pkg upgrade` already runs `pkg update` once!)
            val upgradeBtn = FrameLayout(this).apply {
                background = tonalBg(uiAccentContainer, dp(13))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                addView(MaterialIconView(this@TermuxActivity, M3Icon.REFRESH, uiAccent, 21f, 2.15f))
                attachSmoothPressAnimation(this, 0.90f)
                setOnClickListener {
                    triggerHaptic(0.9f)
                    dlg.dismiss()
                    nativeSendText("pkg upgrade -y\n", false)
                }
            }
            summaryCard.addView(upgradeBtn)
            outer.addView(summaryCard)

            val pkgScroll = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val pkgCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(2), 0, dp(24))
            }
            pkgScroll.addView(pkgCol)

            val ptrPackages = BakaSuPullToRefreshLayout(this, pkgScroll) {
                renderPackagesTab()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            outer.addView(ptrPackages)

            fun makePkgCard(pkg: DpkgPackageInfo, idx: Int, total: Int, allowDelete: Boolean): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(11), dp(12), dp(11))
                    background = segmentedCardBg(idx, total, uiSurfaceLow, 0, 20f, 5f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = if (idx < total - 1) dp(2) else dp(10)
                    }

                    val infoCol = LinearLayout(this@TermuxActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginEnd = dp(10)
                        }
                    }
                    infoCol.addView(TextView(this@TermuxActivity).apply {
                        text = "${pkg.name} (${pkg.version})"
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    })
                    infoCol.addView(TextView(this@TermuxActivity).apply {
                        text = "${formatSize(pkg.sizeKb * 1024L)} • ${pkg.description}"
                        setSingleLine(true)
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                        setPadding(0, dp(2), 0, 0)
                    })
                    addView(infoCol)

                    if (allowDelete) {
                        addView(
                            FrameLayout(this@TermuxActivity).apply {
                                background = tonalBg(COLOR_ROOT_CONTAINER, dp(12))
                                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
                                addView(MaterialIconView(this@TermuxActivity, M3Icon.TRASH, COLOR_PASTEL_RED, 19.5f, 2.0f))
                                attachSmoothPressAnimation(this, 0.90f)
                                setOnClickListener {
                                    triggerHaptic(0.9f)
                                    dlg.dismiss()
                                    nativeSendText("apt remove -y ${pkg.name}\n", false)
                                }
                            }
                        )
                    }
                }
            }

            if (userPackages.isNotEmpty()) {
                pkgCol.addView(TextView(this).apply {
                    text = tr("Установленные пакеты (${userPackages.size})", "Installed packages (${userPackages.size})")
                    setTextColor(uiAccent)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setPadding(dp(8), dp(4), dp(8), dp(6))
                })
                for ((idx, pkg) in userPackages.withIndex()) {
                    pkgCol.addView(makePkgCard(pkg, idx, userPackages.size, allowDelete = true))
                }
            }

            pkgCol.addView(TextView(this).apply {
                text = tr("Основные системные пакеты (${corePackages.size})", "Core system packages (${corePackages.size})")
                setTextColor(COLOR_TEXT_DIM)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(dp(8), dp(8), dp(8), dp(6))
            })
            for ((idx, pkg) in corePackages.withIndex()) {
                pkgCol.addView(makePkgCard(pkg, idx, corePackages.size, allowDelete = false))
            }
        }

        renderActiveTab = {
            val tabs = listOf(
                Triple(tabServersCol, indServers, 0),
                Triple(tabSftpCol, indSftp, 1),
                Triple(tabPkgsCol, indPkgs, 2)
            )
            for ((col, ind, idx) in tabs) {
                val isSel = (activeTab == idx)
                val tv = col.getChildAt(0) as? TextView
                tv?.setTextColor(if (isSel) uiAccent else COLOR_TEXT_DIM)
                tv?.typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
                if (isSel) {
                    ind.visibility = View.VISIBLE
                    (ind.background as? GradientDrawable)?.setColor(uiAccent)
                    ind.scaleX = 0.35f
                    ind.alpha = 0.3f
                    ind.animate()
                        .scaleX(1f)
                        .alpha(1f)
                        .setDuration(220L)
                        .setInterpolator(OvershootInterpolator(2.0f))
                        .start()
                } else {
                    ind.visibility = View.INVISIBLE
                }
            }
            when (activeTab) {
                0 -> renderServersTab()
                1 -> renderSftpTab(animateTab = true)
                else -> renderPackagesTab()
            }
        }

        renderActiveTab()
        dlg.setContentView(root)
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

    internal fun createSession(asRoot: Boolean, runStartupCmd: Boolean = true) {
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
        if (!asRoot && runStartupCmd) {
            val startup = prefs.getString(KEY_LOCAL_STARTUP_CMD, "")?.trim().orEmpty()
            if (startup.isNotEmpty()) {
                terminalSurface.postDelayed({
                    nativeSendText("$startup\n", false)
                }, 140L)
            }
        }
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
                setPadding(dp(10), dp(4), dp(10), dp(4))
                val accent = if (isRoot) COLOR_ROOT else uiAccent
                val activeFill = if (isRoot) COLOR_ROOT_CONTAINER else uiAccentContainer
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
                    setMargins(0, dp(2), dp(4), dp(2))
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
            terminalSurface.pasteTextNatively(text.toString())
            rearmLockedModifiersIfNeeded()
            terminalSurface.resetScrollAndBlink()
        }
    }

    internal fun hasClipboardText(): Boolean {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = cm.primaryClip ?: return false
        return clip.itemCount > 0 && !clip.getItemAt(0).coerceToText(this).isNullOrEmpty()
    }

    private fun pillBg(fill: Int, stroke: Int, radiusPx: Int, strokeWidthPx: Int = -1): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx.toFloat()
            val w = if (strokeWidthPx > 0) strokeWidthPx else max(1, dp(1))
            setStroke(w, stroke)
        }
    }

    /**
     * Material 3 Expressive Segmented Shape (ListItemDefaults.segmentedShapes):
     * Top item: 22dp top corners, 4dp bottom corners.
     * Middle item: 4dp all corners.
     * Bottom item: 4dp top corners, 22dp bottom corners.
     * Single item: 22dp all corners.
     */
    private fun segmentedCardBg(
        index: Int,
        total: Int,
        fill: Int = COLOR_CARD_BG,
        stroke: Int = 0,
        outerRadiusDp: Float = 22f,
        innerRadiusDp: Float = 4f
    ): GradientDrawable {
        val outer = dpF(outerRadiusDp)
        val inner = dpF(innerRadiusDp)
        val topR = if (index <= 0) outer else inner
        val botR = if (index >= total - 1) outer else inner
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadii = floatArrayOf(topR, topR, topR, topR, botR, botR, botR, botR)
            if (stroke != 0) {
                setStroke(max(1, dp(1)), stroke)
            }
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

        private var baseTypeface: Typeface
        private var boldTypeface: Typeface
        private var italicTypeface: Typeface
        private var boldItalicTypeface: Typeface

        internal var cellWidth = 18f
        internal var cellHeight = 36f
        private var cellBaseline = 28f
        internal var fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.45f, 3.0f)
        internal var lineSpacingPct = prefs.getInt(KEY_LINE_SPACING_PCT, 100).coerceIn(85, 135)

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

            baseTypeface = Typeface.MONOSPACE
            boldTypeface = Typeface.MONOSPACE
            italicTypeface = Typeface.MONOSPACE
            boldItalicTypeface = Typeface.MONOSPACE
            reloadTerminalFont()
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
                        if (copyOnSelect) {
                            val selTxt = nativeGetSelectedText()
                            if (selTxt.isNotEmpty()) copyToClipboard(selTxt, false)
                        }
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

        internal fun reloadTerminalFont() {
            val preset = prefs.getInt(KEY_FONT_PRESET, 2)
            val customPath = prefs.getString(KEY_CUSTOM_FONT_PATH, "") ?: ""
            val ligs = prefs.getBoolean(KEY_FONT_LIGATURES, false)

            baseTypeface = try {
                when (preset) {
                    0 -> Typeface.DEFAULT // Android System UI Default Font
                    1 -> Typeface.MONOSPACE // Android System Monospace Font
                    2 -> Typeface.Builder(context.assets, "fonts/JetBrainsMono.ttf")
                        .setFontVariationSettings("'wght' 400")
                        .build() ?: Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
                    3 -> Typeface.Builder(context.assets, "fonts/JetBrainsMono.ttf")
                        .setFontVariationSettings("'wght' 300")
                        .build() ?: Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
                    4 -> Typeface.Builder(context.assets, "fonts/JetBrainsMono.ttf")
                        .setFontVariationSettings("'wght' 500")
                        .build() ?: Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
                    5 -> Typeface.Builder(context.assets, "fonts/JetBrainsMono.ttf")
                        .setFontVariationSettings("'wght' 600")
                        .build() ?: Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
                    6 -> Typeface.Builder(context.assets, "fonts/JetBrainsMono.ttf")
                        .setFontVariationSettings("'wght' 700")
                        .build() ?: Typeface.create(Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf"), Typeface.BOLD)
                    7 -> {
                        val f = File(customPath)
                        if (f.exists() && f.isFile && f.length() > 4096) {
                            Typeface.createFromFile(f)
                        } else {
                            Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
                        }
                    }
                    else -> Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono.ttf")
                }
            } catch (_: Throwable) {
                Typeface.MONOSPACE
            }
            boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)
            italicTypeface = Typeface.create(baseTypeface, Typeface.ITALIC)
            boldItalicTypeface = Typeface.create(baseTypeface, Typeface.BOLD_ITALIC)
            textPaint.typeface = baseTypeface
            textPaint.fontFeatureSettings = if (ligs) "'liga' 1, 'calt' 1" else "'liga' 0, 'calt' 0"
            if (width > 0 && height > 0) {
                updateMetrics(true)
                syncSize()
            }
        }

        internal fun setCustomFontScale(newScale: Float) {
            fontScale = newScale.coerceIn(0.45f, 3.0f)
            updateMetrics(true)
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
            postDelayed({ syncInputConnectionWithGrid() }, 30L)
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
            val rawHeight = ceil((fm.descent - fm.ascent + fm.leading).toDouble()).toFloat()
            cellHeight = max(12f, rawHeight * (lineSpacingPct / 100f))
            val extraPad = (cellHeight - rawHeight) * 0.5f
            cellBaseline = -fm.ascent + extraPad
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
        private var currentInputConnection: TerminalInputConnection? = null

        internal fun syncInputConnectionWithGrid() {
            currentInputConnection?.syncWithTerminalGrid(true)
        }

        internal fun pasteTextNatively(text: String) {
            if (text.isEmpty()) return
            stopTextSelection()
            nativeSendText(text, true)
            currentInputConnection?.onNativeTextPasted(text)
        }

        /**
         * Native Gboard-Compatible InputConnection:
         * - Uses `InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS` (WITHOUT
         *   `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, because password mode forces Gboard to treat the
         *   entire line as a single password token instead of separate words!).
         * - Tracks `terminalSentLine` vs `localBuffer` and sends only the exact character/backspace delta
         *   to the PTY (`applyLocalBufferDiffToPty()`), so Gboard's native word-by-word Backspace swipe
         *   (`1 word -> 2 words -> 3 words -> N words` proportional to swipe distance!) works 100% natively!
         */
        override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_ACTION_NONE

            val initialText = nativeGetInputLineBeforeCursor()
            outAttrs.initialSelStart = initialText.length
            outAttrs.initialSelEnd = initialText.length

            val conn = TerminalInputConnection(initialText)
            currentInputConnection = conn
            return conn
        }

        inner class TerminalInputConnection(initialText: String) : BaseInputConnection(this@TerminalSurface, true) {
            private val localBuffer = SpannableStringBuilder(initialText)
            private var terminalSentLine = initialText
            private var preDeleteCount = 0
            private var lastPreDeleteCount = 0
            private var lastPreDeleteUptimeMs = 0L
            private var batchDepth = 0
            private var lastLocalEditUptimeMs = 0L
            private var extractedTextRequest: ExtractedTextRequest? = null
            private var extractedTextToken = 0

            init {
                Selection.setSelection(localBuffer, initialText.length)
            }

            override fun getEditable(): Editable = localBuffer

            private fun notifySelectionUpdate() {
                if (batchDepth > 0) return
                val curLen = localBuffer.length
                val s = Selection.getSelectionStart(localBuffer).coerceIn(0, curLen)
                val e = Selection.getSelectionEnd(localBuffer).coerceIn(0, curLen)
                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.updateSelection(this@TerminalSurface, s, e, -1, -1)
                if (extractedTextRequest != null) {
                    imm?.updateExtractedText(this@TerminalSurface, extractedTextToken, buildExtractedText())
                }
            }

            private fun buildExtractedText(): ExtractedText {
                val curLen = localBuffer.length
                val s = Selection.getSelectionStart(localBuffer).coerceIn(0, curLen)
                val e = Selection.getSelectionEnd(localBuffer).coerceIn(0, curLen)
                return ExtractedText().apply {
                    text = localBuffer.toString()
                    startOffset = 0
                    partialStartOffset = -1
                    partialEndOffset = -1
                    selectionStart = s
                    selectionEnd = e
                    flags = 0
                }
            }

            internal fun syncWithTerminalGrid(notifyIme: Boolean) {
                if (preDeleteCount > 0 || batchDepth > 0) return
                val now = SystemClock.uptimeMillis()
                val gridLine = nativeGetInputLineBeforeCursor()
                if (gridLine.length >= localBuffer.length || (now - lastLocalEditUptimeMs) > 45L) {
                    if (localBuffer.toString() != gridLine) {
                        localBuffer.clear()
                        localBuffer.append(gridLine)
                        terminalSentLine = gridLine
                        Selection.setSelection(localBuffer, localBuffer.length)
                        if (notifyIme) {
                            notifySelectionUpdate()
                        }
                    }
                }
            }

            /**
             * Diffs `terminalSentLine` (what is currently on the PTY command line) with `localBuffer`
             * (what Gboard has edited/composed/deleted) and sends the minimal KEYCODE_DEL + text delta.
             */
            private fun applyLocalBufferDiffToPty() {
                if (batchDepth > 0) return
                val currentStr = localBuffer.toString()
                val lastNl = max(currentStr.lastIndexOf('\n'), currentStr.lastIndexOf('\r'))
                if (lastNl >= 0) {
                    // Contains newline: send everything added after terminalSentLine
                    val common = commonPrefixLength(terminalSentLine, currentStr)
                    val delCount = (terminalSentLine.length - common).coerceAtLeast(0)
                    for (i in 0 until delCount) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                    }
                    val toAdd = currentStr.substring(common)
                    if (toAdd.isNotEmpty()) {
                        nativeSendText(toAdd, toAdd.length > 1)
                    }
                    val tail = if (lastNl + 1 < currentStr.length) currentStr.substring(lastNl + 1) else ""
                    localBuffer.clear()
                    localBuffer.append(tail)
                    Selection.setSelection(localBuffer, localBuffer.length)
                    terminalSentLine = tail
                    lastLocalEditUptimeMs = SystemClock.uptimeMillis()
                    notifySelectionUpdate()
                    return
                }

                val common = commonPrefixLength(terminalSentLine, currentStr)
                val gridLine = nativeGetInputLineBeforeCursor()
                val maxSafeDel = max(terminalSentLine.length, gridLine.length)
                val delCount = (terminalSentLine.length - common).coerceIn(0, maxSafeDel)
                for (i in 0 until delCount) {
                    nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                }
                val toAdd = currentStr.substring(common)
                if (toAdd.isNotEmpty()) {
                    nativeSendText(toAdd, toAdd.length > 1)
                }
                terminalSentLine = currentStr
                lastLocalEditUptimeMs = SystemClock.uptimeMillis()
                notifySelectionUpdate()
            }

            private fun commonPrefixLength(a: String, b: String): Int {
                val n = min(a.length, b.length)
                var i = 0
                while (i < n && a[i] == b[i]) {
                    i++
                }
                return i
            }

            private fun activeOrRecentPreDeleteCount(): Int {
                if (preDeleteCount > 0) return preDeleteCount
                val now = SystemClock.uptimeMillis()
                if (lastPreDeleteCount > 0 && (now - lastPreDeleteUptimeMs) <= 75L) {
                    return lastPreDeleteCount
                }
                return 0
            }

            private fun consumeActivePreDeleteIntoLocalBuffer(): Boolean {
                val del = activeOrRecentPreDeleteCount()
                if (del <= 0) return false
                preDeleteCount = 0
                lastPreDeleteCount = 0
                nativeSetPreDeleteCount(0)
                BaseInputConnection.removeComposingSpans(localBuffer)
                val curLen = localBuffer.length
                val newLen = max(0, curLen - del)
                localBuffer.delete(newLen, curLen)
                Selection.setSelection(localBuffer, newLen)
                return true
            }

            internal fun onNativeTextPasted(pasted: String) {
                if (preDeleteCount > 0) {
                    preDeleteCount = 0
                    nativeSetPreDeleteCount(0)
                }
                val lastNl = max(pasted.lastIndexOf('\n'), pasted.lastIndexOf('\r'))
                if (lastNl >= 0) {
                    val tail = if (lastNl + 1 < pasted.length) pasted.substring(lastNl + 1) else ""
                    localBuffer.clear()
                    localBuffer.append(tail)
                } else {
                    localBuffer.append(pasted)
                }
                terminalSentLine = localBuffer.toString()
                Selection.setSelection(localBuffer, localBuffer.length)
                lastLocalEditUptimeMs = SystemClock.uptimeMillis()
                notifySelectionUpdate()
            }

            override fun beginBatchEdit(): Boolean {
                batchDepth++
                return super.beginBatchEdit()
            }

            override fun endBatchEdit(): Boolean {
                if (batchDepth > 0) batchDepth--
                val res = super.endBatchEdit()
                if (batchDepth == 0) {
                    if (preDeleteCount == 0) {
                        applyLocalBufferDiffToPty()
                    } else {
                        notifySelectionUpdate()
                    }
                }
                return res
            }

            override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
                syncWithTerminalGrid(false)
                if ((flags and InputConnection.GET_EXTRACTED_TEXT_MONITOR) != 0 && request != null) {
                    extractedTextRequest = request
                    extractedTextToken = request.token
                }
                return buildExtractedText()
            }

            override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText {
                syncWithTerminalGrid(false)
                val curLen = localBuffer.length
                val s = Selection.getSelectionStart(localBuffer).coerceIn(0, curLen)
                val e = Selection.getSelectionEnd(localBuffer).coerceIn(0, curLen)
                val minSel = min(s, e)
                val maxSel = max(s, e)
                val start = max(0, minSel - max(0, beforeLength))
                val end = min(curLen, maxSel + max(0, afterLength))
                val sub = localBuffer.subSequence(start, end)
                return SurroundingText(sub, minSel - start, maxSel - start, start)
            }

            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
                syncWithTerminalGrid(false)
                return super.getTextBeforeCursor(n, flags) ?: ""
            }

            override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
                syncWithTerminalGrid(false)
                return super.getTextAfterCursor(n, flags) ?: ""
            }

            override fun getSelectedText(flags: Int): CharSequence {
                return super.getSelectedText(flags) ?: ""
            }

            override fun setSelection(start: Int, end: Int): Boolean {
                if (preDeleteCount == 0 && batchDepth == 0) {
                    syncWithTerminalGrid(false)
                }
                val curLen = localBuffer.length
                val s = start.coerceIn(0, curLen)
                val e = end.coerceIn(0, curLen)
                if (s != e) {
                    BaseInputConnection.removeComposingSpans(localBuffer)
                }
                super.setSelection(s, e)
                val newCount = max(0, max(s, e) - min(s, e))
                val now = SystemClock.uptimeMillis()
                if (newCount == 0 && preDeleteCount > 0) {
                    lastPreDeleteCount = preDeleteCount
                    lastPreDeleteUptimeMs = now
                } else if (newCount > 0) {
                    lastPreDeleteCount = newCount
                    lastPreDeleteUptimeMs = now
                }
                if (newCount != preDeleteCount) {
                    preDeleteCount = newCount
                    nativeSetPreDeleteCount(newCount)
                    if (newCount > 0) {
                        triggerHaptic(0.5f)
                    }
                }
                notifySelectionUpdate()
                return true
            }

            override fun deleteSurroundingText(leftLength: Int, rightLength: Int): Boolean {
                stopTextSelection()
                if (activeOrRecentPreDeleteCount() > 0) {
                    consumeActivePreDeleteIntoLocalBuffer()
                    applyLocalBufferDiffToPty()
                } else if (localBuffer.isEmpty() && leftLength > 0) {
                    // Fallback for alt-screen TUI apps (vim / nano) when localBuffer is empty
                    if (nativeGetInputLineBeforeCursor().isEmpty() && (nativeGetModesMask() and 1) != 0) {
                        for (i in 0 until leftLength) {
                            nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                        }
                    }
                } else {
                    super.deleteSurroundingText(leftLength, rightLength)
                    applyLocalBufferDiffToPty()
                }
                if (rightLength > 0) {
                    for (i in 0 until rightLength) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, 0, 0)
                    }
                }
                rearmLockedModifiersIfNeeded()
                resetScrollAndBlink()
                return true
            }

            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
                return deleteSurroundingText(beforeLength, afterLength)
            }

            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                val str = text?.toString() ?: ""
                stopTextSelection()
                val shouldConsumePreDel = if (str.isEmpty()) activeOrRecentPreDeleteCount() > 0 else preDeleteCount > 0
                if (shouldConsumePreDel) {
                    consumeActivePreDeleteIntoLocalBuffer()
                    if (str.isNotEmpty()) {
                        localBuffer.append(str)
                        Selection.setSelection(localBuffer, localBuffer.length)
                    }
                    applyLocalBufferDiffToPty()
                } else {
                    lastPreDeleteCount = 0
                    super.commitText(str, newCursorPosition)
                    applyLocalBufferDiffToPty()
                }
                rearmLockedModifiersIfNeeded()
                resetScrollAndBlink()
                return true
            }

            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                val str = text?.toString() ?: ""
                stopTextSelection()
                val shouldConsumePreDel = if (str.isEmpty()) activeOrRecentPreDeleteCount() > 0 else preDeleteCount > 0
                if (shouldConsumePreDel) {
                    consumeActivePreDeleteIntoLocalBuffer()
                    if (str.isNotEmpty()) {
                        localBuffer.append(str)
                        Selection.setSelection(localBuffer, localBuffer.length)
                    }
                    applyLocalBufferDiffToPty()
                } else {
                    lastPreDeleteCount = 0
                    super.setComposingText(str, newCursorPosition)
                    applyLocalBufferDiffToPty()
                }
                rearmLockedModifiersIfNeeded()
                resetScrollAndBlink()
                return true
            }

            override fun setComposingRegion(start: Int, end: Int): Boolean {
                val curLen = localBuffer.length
                return super.setComposingRegion(start.coerceIn(0, curLen), end.coerceIn(0, curLen))
            }

            override fun finishComposingText(): Boolean {
                return super.finishComposingText()
            }

            override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = true

            override fun replaceText(
                start: Int,
                end: Int,
                text: CharSequence,
                newCursorPosition: Int,
                textAttribute: TextAttribute?
            ): Boolean {
                stopTextSelection()
                if (activeOrRecentPreDeleteCount() > 0) {
                    consumeActivePreDeleteIntoLocalBuffer()
                } else {
                    val curLen = localBuffer.length
                    val s = min(start, end).coerceIn(0, curLen)
                    val e = max(start, end).coerceIn(0, curLen)
                    if (e > s) {
                        localBuffer.delete(s, e)
                    }
                }
                val sText = text.toString()
                if (sText.isNotEmpty()) {
                    localBuffer.append(sText)
                }
                Selection.setSelection(localBuffer, localBuffer.length)
                applyLocalBufferDiffToPty()
                rearmLockedModifiersIfNeeded()
                resetScrollAndBlink()
                return true
            }

            override fun performContextMenuAction(id: Int): Boolean {
                when (id) {
                    android.R.id.paste, android.R.id.pasteAsPlainText -> {
                        stopTextSelection()
                        pasteFromClipboard()
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

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.keyCode == KeyEvent.KEYCODE_DEL) {
                        if (preDeleteCount > 0) {
                            consumeActivePreDeleteIntoLocalBuffer()
                            applyLocalBufferDiffToPty()
                        } else if (localBuffer.isNotEmpty()) {
                            BaseInputConnection.removeComposingSpans(localBuffer)
                            val newLen = max(0, localBuffer.length - 1)
                            localBuffer.delete(newLen, localBuffer.length)
                            Selection.setSelection(localBuffer, newLen)
                            applyLocalBufferDiffToPty()
                        } else {
                            nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0)
                        }
                        resetScrollAndBlink()
                        return true
                    }
                    if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        BaseInputConnection.removeComposingSpans(localBuffer)
                        localBuffer.clear()
                        terminalSentLine = ""
                        Selection.setSelection(localBuffer, 0)
                        preDeleteCount = 0
                        nativeSetPreDeleteCount(0)
                        lastLocalEditUptimeMs = SystemClock.uptimeMillis()
                        this@TerminalSurface.onKeyDown(event.keyCode, event)
                        notifySelectionUpdate()
                        return true
                    }
                    if (preDeleteCount > 0) {
                        consumeActivePreDeleteIntoLocalBuffer()
                        applyLocalBufferDiffToPty()
                    }
                    val unicode = event.getUnicodeChar(event.metaState and KeyEvent.META_CTRL_MASK.inv())
                    val isModActive = (event.metaState and (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON)) != 0 ||
                        (nativeGetModifierMask() != 0)
                    if (!isModActive) {
                        if (event.keyCode == KeyEvent.KEYCODE_SPACE) {
                            BaseInputConnection.removeComposingSpans(localBuffer)
                            localBuffer.append(' ')
                            terminalSentLine += " "
                            Selection.setSelection(localBuffer, localBuffer.length)
                            lastLocalEditUptimeMs = SystemClock.uptimeMillis()
                        } else if (unicode >= 0x20 && unicode != 0x7F) {
                            BaseInputConnection.removeComposingSpans(localBuffer)
                            val ch = unicode.toChar()
                            localBuffer.append(ch)
                            terminalSentLine += ch
                            Selection.setSelection(localBuffer, localBuffer.length)
                            lastLocalEditUptimeMs = SystemClock.uptimeMillis()
                        }
                    }
                    this@TerminalSurface.onKeyDown(event.keyCode, event)
                    notifySelectionUpdate()
                }
                return true
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
        @JvmStatic external fun nativePingHostMs(host: String, port: Int, timeoutMs: Int): Int
        @JvmStatic external fun nativeBuildSshCommand(
            host: String,
            port: Int,
            user: String,
            authType: Int,
            secret: String,
            localFwds: String,
            remoteFwds: String,
            dynamicPort: String,
            jumpHost: String,
            keepAliveSec: Int,
            tunnelOnlyN: Boolean,
            compression: Boolean,
            forwardAgent: Boolean,
            ipv4Only: Boolean,
            customFlags: String,
            startupCmd: String,
            withAskPass: Boolean
        ): String
        @JvmStatic external fun nativeParseSshCommand(rawCmd: String): String
        @JvmStatic external fun nativeListLocalDir(dirPath: String, showHidden: Boolean, searchQuery: String): String
        @JvmStatic external fun nativeExecRemoteSftp(
            host: String,
            port: Int,
            user: String,
            authType: Int,
            secret: String,
            jumpHost: String,
            remoteShCmd: String
        ): String
        @JvmStatic external fun nativeGetInstalledPackages(): String
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
        @JvmStatic var activeInstance: TermuxActivity? = null
        @JvmStatic external fun nativeSetDefaultCursorShape(shape: Int)
        @JvmStatic external fun nativeSetMaxScrollback(maxLines: Int)
        @JvmStatic external fun nativeGetSelectedText(): String
        @JvmStatic external fun nativeGenerateColorSpec2026Palette(seedArgb: Int, styleIndex: Int): IntArray
        @JvmStatic external fun nativeDiscoverCustomFonts(): String
    }
}

class KeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private fun buildMinimalNotification(): Notification {
        val chId = "termux_session_channel"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(chId, "Termux", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
        )

        val openPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TermuxActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, KeepAliveService::class.java).apply {
            action = ACTION_EXIT_APP
        }
        val exitPi = PendingIntent.getService(
            this,
            1,
            exitIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lang = prefs.getInt(KEY_APP_LANG, 0)
        val isEn = when (lang) {
            1 -> false
            2 -> true
            else -> !Locale.getDefault().language.lowercase().startsWith("ru")
        }
        val exitLabel = if (isEn) "Exit" else "Выход"

        val exitAction = Notification.Action.Builder(null, exitLabel, exitPi).build()

        return Notification.Builder(this, chId)
            .setContentTitle("Termux")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(exitAction)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(1, buildMinimalNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_EXIT_APP) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Throwable) {
            }
            try {
                val nm = getSystemService(NotificationManager::class.java)
                nm?.cancelAll()
            } catch (_: Throwable) {
            }
            stopSelf()
            TermuxActivity.activeInstance?.exitAppCompletely() ?: run {
                try {
                    TermuxActivity.nativeDestroyAll()
                } catch (_: Throwable) {
                }
                kotlin.system.exitProcess(0)
            }
            return START_NOT_STICKY
        }
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.notify(1, buildMinimalNotification())
        } catch (_: Throwable) {
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
