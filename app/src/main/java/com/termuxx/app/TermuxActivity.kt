@file:Suppress("DEPRECATION", "ClickableViewAccessibility", "SetTextI18n", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_VALUE", "NAME_SHADOWING", "RedundantOverride")

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
import android.graphics.Outline
import android.graphics.Paint
import android.view.ViewOutlineProvider
import android.provider.OpenableColumns
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.animation.LinearInterpolator
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
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.graphics.CornerPathEffect
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
import android.view.WindowInsetsAnimation
import android.text.TextUtils
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
private const val KEY_EXTRA_KEYS_SPEC = "extra_keys_spec_v3"
private const val MAX_EXTRA_KEYS_PER_ROW = 8
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
private const val KEY_AUTO_HIDE_EXTRA_KEYS = "auto_hide_extra_keys_v1"
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
    "ESC|TAB|CTRL|ALT|▲|▼|◀|▶"

private const val EXTENDED_EXTRA_KEYS_SPEC =
    "ESC|TAB|CTRL|ALT|HOME|▲|END|PGUP\n-|/|\\|_|◀|▼|▶|PGDN"

private const val VIM_TMUX_EXTRA_KEYS_SPEC =
    "ESC|:|CTRL|ALT|h|j|k|l"

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
    SeedColorOption("Monochrome", 0xFF8E9199.toInt()),
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

private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
    val authType: Int = 0, // 0 = Password / Default, 1 = Private Key (-i), 2 = SSH Agent (-A)
    val secretOrKeyPath: String = "",
    val startupCommand: String = "",
    val useCompression: Boolean = false,
    val forwardAgent: Boolean = false,
    val localForwards: String = "", // e.g. "8080:127.0.0.1:80"
    val remoteForwards: String = "", // e.g. "8080:127.0.0.1:8080"
    val dynamicSocksPort: String = "", // e.g. "1080"
    val jumpHost: String = "", // e.g. "user@bastion:22"
    val keepAliveSec: Int = 0,
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
    private lateinit var tabSlidingIndicator: View
    private var prevActiveSessionIdx: Int = 0
    private lateinit var extraKeysRoot: LinearLayout
    private var isImeCurrentlyOpen: Boolean = false
    private var isExtraKeysScrollHidden: Boolean = false
    internal var autoHideExtraKeysOnScroll: Boolean = true

    internal fun setExtraKeysScrollHidden(hidden: Boolean) {
        if (!autoHideExtraKeysOnScroll && hidden) return
        if (isExtraKeysScrollHidden == hidden) return
        isExtraKeysScrollHidden = hidden
        extraKeysRoot.animate().cancel()
        if (hidden) {
            val slideDist = (extraKeysRoot.height + dp(24)).toFloat().coerceAtLeast(dpF(56f))
            extraKeysRoot.animate()
                .translationY(slideDist)
                .alpha(0f)
                .setDuration(210L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
        } else {
            extraKeysRoot.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(230L)
                .setInterpolator(OvershootInterpolator(1.25f))
                .start()
        }
    }
    private var imeOpenFraction: Float = 0f
    private var imeTransitionAnimator: ValueAnimator? = null
    private val extraKeyRowUpdaters = ArrayList<() -> Unit>(2)
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
        autoHideExtraKeysOnScroll = prefs.getBoolean(KEY_AUTO_HIDE_EXTRA_KEYS, true)
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

        // Top Floating Capsules Row (no full-width background bar — expanding Sessions pill on left & Actions pill on right!)
        headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0x00000000)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            clipChildren = false
            clipToPadding = false
        }

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(18))
            clipChildren = false
            clipToPadding = false
        }
        val tabContainer = FrameLayout(this).apply {
            background = tonalBg(uiSurfaceLow, dp(22))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            clipChildren = false
            clipToPadding = false
        }
        tabSlidingIndicator = View(this).apply {
            background = makeLiquidGlassPillDrawable(uiAccentContainer, dp(15))
            visibility = View.GONE
        }
        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        tabContainer.addView(tabSlidingIndicator, FrameLayout.LayoutParams(0, 0))
        tabContainer.addView(tabStrip, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        tabScroll.addView(tabContainer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        headerBar.addView(
            tabScroll,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
        )

        headerBar.addView(makeHeaderConnectedActionsGroup())

        rootLayout.addView(
            headerBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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

        // Bottom Extra Keys Bar (no black background, lifted higher with thin outer pill frame like image_602ce1.png)
        extraKeysRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0x00000000)
            setPadding(0, dp(4), 0, dp(18))
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
            if (vib == null || !vib.hasVibrator()) return
            val baseNorm = (hapticAmplitude / 255f).coerceIn(0.15f, 1.0f)
            if (Build.VERSION.SDK_INT >= 31) {
                when {
                    scale <= 0.62f && vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK) -> {
                        vib.vibrate(
                            VibrationEffect.startComposition()
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, (baseNorm * 0.65f).coerceIn(0.2f, 1f))
                                .compose()
                        )
                        return
                    }
                    scale in 0.63f..0.82f && vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK) -> {
                        vib.vibrate(
                            VibrationEffect.startComposition()
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (baseNorm * 0.82f).coerceIn(0.25f, 1f))
                                .compose()
                        )
                        return
                    }
                    scale in 0.83f..1.08f && vib.areAllPrimitivesSupported(
                        VibrationEffect.Composition.PRIMITIVE_TICK,
                        VibrationEffect.Composition.PRIMITIVE_CLICK
                    ) -> {
                        // Elastic springy feel for switches, Bento buttons, and tabs
                        vib.vibrate(
                            VibrationEffect.startComposition()
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (baseNorm * 0.55f).coerceIn(0.2f, 1f))
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (baseNorm * 0.92f).coerceIn(0.3f, 1f), 14)
                                .compose()
                        )
                        return
                    }
                    scale > 1.08f && vib.areAllPrimitivesSupported(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                        VibrationEffect.Composition.PRIMITIVE_CLICK
                    ) -> {
                        vib.vibrate(
                            VibrationEffect.startComposition()
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, (baseNorm * 0.85f).coerceIn(0.3f, 1f))
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, baseNorm.coerceIn(0.4f, 1f), 18)
                                .compose()
                        )
                        return
                    }
                }
            }
            val dur = max(2L, (hapticDurationMs * scale).roundToInt().toLong())
            val amp = (hapticAmplitude * scale).roundToInt().coerceIn(1, 255)
            vib.vibrate(VibrationEffect.createOneShot(dur, amp))
        } catch (_: Throwable) {
        }
    }

    private var pendingFileImportCallback: ((android.net.Uri) -> Unit)? = null
    private var pendingPermissionGrantedCallback: (() -> Unit)? = null

    internal fun hasAllFilesAccessPermission(): Boolean {
        return try {
            Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()
        } catch (_: Throwable) {
            false
        }
    }

    internal fun hasStandardStoragePermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) {
            false
        }
    }

    internal fun setupTermuxStorageSymlinks() {
        Thread {
            try {
                val home = File("/data/data/com.termuxx/file/home")
                if (!home.exists()) home.mkdirs()
                val storageDir = File(home, "storage")
                if (!storageDir.exists()) storageDir.mkdirs()
                val sd = Environment.getExternalStorageDirectory()
                val links = listOf(
                    "shared" to sd,
                    "downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "dcim" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "movies" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                )
                for ((name, target) in links) {
                    val linkFile = File(storageDir, name)
                    if (!linkFile.exists()) {
                        try {
                            android.system.Os.symlink(target.absolutePath, linkFile.absolutePath)
                        } catch (_: Throwable) {
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }.start()
    }

    internal fun hasIgnoreBatteryOptimizationsPermission(): Boolean {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(packageName) == true
        } catch (_: Throwable) {
            false
        }
    }

    internal fun requestIgnoreBatteryOptimizationsPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (_: Throwable) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Throwable) {
            }
        }
    }

    internal fun requestAllFilesSpecialPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (_: Throwable) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } catch (_: Throwable) {
            }
        }
    }

    internal fun requestStandardStoragePermissions(onDone: (() -> Unit)? = null) {
        pendingPermissionGrantedCallback = onDone
        try {
            val perms = mutableListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (Build.VERSION.SDK_INT >= 33) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
                perms.add(Manifest.permission.READ_MEDIA_VIDEO)
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            requestPermissions(perms.toTypedArray(), 1002)
        } catch (_: Throwable) {
        }
    }

    internal fun showStoragePermissionsModal(onCompleted: () -> Unit = {}) {
        val (pDlg, pSheet) = createExpressiveDialog(430)
        val hdrBadge = FrameLayout(this).apply {
            background = tonalBg(uiAccentContainer, dp(16))
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
            addView(MaterialIconView(this@TermuxActivity, M3Icon.STORAGE, uiAccent, 26f, 2.15f))
        }
        pSheet.addView(hdrBadge)
        pSheet.addView(TextView(this).apply {
            text = tr("Доступ к файлам устройства", "Device file access")
            gravity = Gravity.CENTER
            setTextColor(COLOR_TEXT_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.5f)
        })
        pSheet.addView(TextView(this).apply {
            text = tr(
                "Выберите тип разрешения для работы с внутренней памятью (/sdcard) в терминале и проводнике SFTP.",
                "Choose the permission type to access internal storage (/sdcard) in Terminal and SFTP explorer."
            )
            gravity = Gravity.CENTER
            setTextColor(COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(dp(8), dp(6), dp(8), dp(16))
        })

        val allFilesGranted = hasAllFilesAccessPermission()
        val stdGranted = hasStandardStoragePermission()
        val bgUnrestricted = hasIgnoreBatteryOptimizationsPermission()

        // Option 1: Special All-Files Access
        val card1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = segmentedCardBg(0, 3, if (allFilesGranted) uiAccentContainer else uiSurfaceLow, 0, 20f, 5f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(3)
            }
            attachSmoothPressAnimation(this, 0.98f)
            setOnClickListener {
                triggerHaptic(0.85f)
                pDlg.dismiss()
                requestAllFilesSpecialPermission()
                setupTermuxStorageSymlinks()
                onCompleted()
            }
        }
        card1.addView(
            FrameLayout(this).apply {
                background = tonalBg(uiSurfaceContainer, dp(12))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(14)
                }
                addView(MaterialIconView(this@TermuxActivity, M3Icon.FOLDER, uiAccent, 21f, 2.0f))
            }
        )
        val c1Col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        c1Col.addView(TextView(this).apply {
            text = tr("Спец. разрешение «Доступ ко всем файлам»", "Special 'All files access' permission")
            setTextColor(COLOR_TEXT_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        })
        c1Col.addView(TextView(this).apply {
            text = if (allFilesGranted) tr("Активно • Полный доступ ко всем папкам /sdcard", "Granted • Full access to all /sdcard folders")
            else tr("Рекомендуется для терминала, SFTP и скриптов", "Recommended for terminal, SFTP, and scripts")
            setTextColor(if (allFilesGranted) uiAccent else COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), 0, 0)
        })
        card1.addView(c1Col)
        pSheet.addView(card1)

        // Option 2: Standard App Storage Permission
        val card2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = segmentedCardBg(1, 3, if (stdGranted) uiAccentContainer else uiSurfaceLow, 0, 20f, 5f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(3)
            }
            attachSmoothPressAnimation(this, 0.98f)
            setOnClickListener {
                triggerHaptic(0.85f)
                pDlg.dismiss()
                requestStandardStoragePermissions {
                    setupTermuxStorageSymlinks()
                    onCompleted()
                }
            }
        }
        card2.addView(
            FrameLayout(this).apply {
                background = tonalBg(uiSurfaceContainer, dp(12))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(14)
                }
                addView(MaterialIconView(this@TermuxActivity, M3Icon.STORAGE, uiAccent, 21f, 2.0f))
            }
        )
        val c2Col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        c2Col.addView(TextView(this).apply {
            text = tr("Обычное разрешение приложения", "Standard app storage permission")
            setTextColor(COLOR_TEXT_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        })
        c2Col.addView(TextView(this).apply {
            text = if (stdGranted) tr("Активно • Стандартный доступ к медиа и файлам", "Granted • Standard media & file access")
            else tr("Запросить системное разрешение приложения", "Request standard runtime app permission")
            setTextColor(if (stdGranted) uiAccent else COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), 0, 0)
        })
        card2.addView(c2Col)
        pSheet.addView(card2)

        // Option 3: Unrestricted Background Activity / Ignore Battery Optimizations
        val card3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = segmentedCardBg(2, 3, if (bgUnrestricted) uiAccentContainer else uiSurfaceLow, 0, 20f, 5f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            attachSmoothPressAnimation(this, 0.98f)
            setOnClickListener {
                triggerHaptic(0.85f)
                pDlg.dismiss()
                requestIgnoreBatteryOptimizationsPermission()
                onCompleted()
            }
        }
        card3.addView(
            FrameLayout(this).apply {
                background = tonalBg(uiSurfaceContainer, dp(12))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(14)
                }
                addView(MaterialIconView(this@TermuxActivity, M3Icon.PING, uiAccent, 21f, 2.0f))
            }
        )
        val c3Col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        c3Col.addView(TextView(this).apply {
            text = tr("Работа в фоне без ограничений", "Unrestricted background execution")
            setTextColor(COLOR_TEXT_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        })
        c3Col.addView(TextView(this).apply {
            text = if (bgUnrestricted) tr("Активно • Сессии и SSH не закрываются в фоне", "Active • Sessions & SSH stay alive in background")
            else tr("Отключить оптимизацию батареи для фоновой работы", "Disable battery optimization for background sessions")
            setTextColor(if (bgUnrestricted) uiAccent else COLOR_TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), 0, 0)
        })
        card3.addView(c3Col)
        pSheet.addView(card3)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(14), 0, 0)
        }
        btnRow.addView(makeExpressiveChip(tr("Закрыть", "Close"), false) { pDlg.dismiss() })
        pSheet.addView(btnRow)
        pDlg.show()
    }

    internal fun launchSystemFilePickerForImport(onPicked: (android.net.Uri) -> Unit) {
        pendingFileImportCallback = onPicked
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 1003)
        } catch (_: Throwable) {
            Toast.makeText(this, tr("Системный проводник недоступен", "System file picker unavailable"), Toast.LENGTH_SHORT).show()
        }
    }

    internal fun queryUriDisplayName(uri: android.net.Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = c.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Throwable) {
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank { "imported_file" } ?: "imported_file"
    }

    private fun ensureNotificationsAndKeepAlive() {
        startForegroundKeepAlive()
        if (hasAllFilesAccessPermission() || hasStandardStoragePermission()) {
            setupTermuxStorageSymlinks()
        }
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
        } else if (requestCode == 1002) {
            setupTermuxStorageSymlinks()
            pendingPermissionGrantedCallback?.invoke()
            pendingPermissionGrantedCallback = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1003 && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                pendingFileImportCallback?.invoke(uri)
            }
            pendingFileImportCallback = null
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
        if (activeInstance == this) {
            activeInstance = null
        }
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

    private fun restLeftR(baseR: Float, outerR: Float, morphT: Float): Float =
        baseR + (outerR - baseR) * morphT

    internal fun rebuildExtraKeysBar() {
        extraKeysRoot.removeAllViews()
        modifierButtons.clear()
        extraKeyRowUpdaters.clear()

        var rawSpec = prefs.getString(KEY_EXTRA_KEYS_SPEC, DEFAULT_EXTRA_KEYS_SPEC)?.trim().orEmpty()
        if (rawSpec == "ESC|TAB|CTRL|ALT|-|/|▲|▼|◀|▶") {
            rawSpec = DEFAULT_EXTRA_KEYS_SPEC
            prefs.edit().putString(KEY_EXTRA_KEYS_SPEC, rawSpec).apply()
        }
        if (rawSpec.isEmpty()) {
            extraKeysRoot.visibility = View.GONE
            return
        }
        extraKeysRoot.visibility = View.VISIBLE

        for (rawLine in rawSpec.split("\n")) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val delimiter = if (line.contains("|")) "|" else ","
            val tokens = line.split(delimiter)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(MAX_EXTRA_KEYS_PER_ROW)
            if (tokens.isEmpty()) continue

            val n = tokens.size
            val keyHeightPx = dp(33)
            val outerPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = uiSurfaceLow
            }
            val outerPillRect = RectF()
            val subGroupBounds = ArrayList<FloatArray>(3)

            val rowContainer = object : FrameLayout(this) {
                override fun dispatchDraw(canvas: Canvas) {
                    outerPillPaint.color = uiSurfaceLow
                    val padPx = dpF(3.5f)
                    val topY = (height - keyHeightPx) * 0.5f - padPx
                    val botY = (height + keyHeightPx) * 0.5f + padPx
                    val pillRad = (botY - topY) * 0.5f
                    for (bounds in subGroupBounds) {
                        outerPillRect.set(bounds[0] - padPx, topY, bounds[1] + padPx, botY)
                        canvas.drawRoundRect(outerPillRect, pillRad, pillRad, outerPillPaint)
                    }
                    super.dispatchDraw(canvas)
                }
            }.apply {
                setBackgroundColor(0x00000000)
                clipChildren = false
                clipToPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(41)
                )
            }

            val buttons = ArrayList<TextView>(n)
            val drawables = ArrayList<GradientDrawable>(n)
            val actionSpecs = ArrayList<String>(n)
            val pressProgress = FloatArray(n)
            val pressAnimators = arrayOfNulls<ValueAnimator>(n)

            lateinit var layoutRowNow: () -> Unit

            for (i in 0 until n) {
                val (displayLabel, actionSpec) = parseKeyToken(tokens[i])
                actionSpecs.add(actionSpec)
                val bg = makeConnectedSegmentDrawable(uiSurfaceContainer, dpF(16.5f), dpF(16.5f))
                drawables.add(bg)

                val btn = createExtraKeyButton(
                    displayLabel = displayLabel,
                    actionSpec = actionSpec,
                    bgDrawable = bg,
                    onPressStateChanged = { holding ->
                        pressAnimators[i]?.cancel()
                        val startP = pressProgress[i]
                        val targetP = if (holding) 1f else 0f
                        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                            duration = if (holding) 130L else 320L
                            interpolator = LinearInterpolator()
                            addUpdateListener { va ->
                                val t = (va.animatedValue as Float).coerceIn(0f, 1f)
                                val eased = if (holding) {
                                    1f - (1f - t) * (1f - t)
                                } else {
                                    1f - kotlin.math.exp(-3.6f * t) * kotlin.math.cos(5.8f * t)
                                }
                                pressProgress[i] = (startP + (targetP - startP) * eased).coerceIn(0f, 1.25f)
                                layoutRowNow()
                            }
                        }
                        pressAnimators[i] = anim
                        anim.start()
                    }
                )
                if (actionSpec == "CTRL" || actionSpec == "ALT") {
                    modifierButtons.add(btn)
                }
                buttons.add(btn)
                rowContainer.addView(btn, FrameLayout.LayoutParams(dp(44), keyHeightPx, Gravity.CENTER_VERTICAL))
            }

            val restXArr = FloatArray(n)
            val restLeftRArr = FloatArray(n)
            val restRightRArr = FloatArray(n)
            val deltaLeftEdge = FloatArray(n)
            val deltaRightEdge = FloatArray(n)

            layoutRowNow = {
                val totalW = rowContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
                val wF = totalW.toFloat()
                val gap = dpF(2.5f)
                val outerR = keyHeightPx * 0.5f
                val innerR = dpF(5f)
                val maxSingleKeyW = dpF(50f)

                // 1. Closed-IME geometry (shifted further from left & right edges: 22dp)
                val closedSidePad = dpF(22f)
                val usableClosedW = (wF - 2f * closedSidePad).coerceAtLeast(dpF(100f))
                val closedKeyW = min(maxSingleKeyW, (usableClosedW - gap * (n - 1)) / n)
                val closedGroupW = n * closedKeyW + (n - 1) * gap
                val closedStartX = (wF - closedGroupW) * 0.5f

                // 2. Open-IME geometry (flush side padding dp(4); split rule for < 8 keys)
                val openSidePad = dpF(4f)
                val usableOpenW = (wF - 2f * openSidePad).coerceAtLeast(dpF(100f))
                val openKeyW = if (n >= MAX_EXTRA_KEYS_PER_ROW) {
                    (usableOpenW - gap * (n - 1)) / n
                } else {
                    min(maxSingleKeyW, (usableOpenW - gap * (n - 1)) / n)
                }

                val openF = safeClamp(imeOpenFraction, 0f, 1f)
                val curBaseW = closedKeyW + (openKeyW - closedKeyW) * openF

                for (i in 0 until n) {
                    val closedX = closedStartX + i * (closedKeyW + gap)
                    val closedLeftR = if (i == 0) outerR else innerR
                    val closedRightR = if (i == n - 1) outerR else innerR

                    val openX: Float
                    val openLeftR: Float
                    val openRightR: Float

                    when {
                        n == 1 -> {
                            openX = (wF - openKeyW) * 0.5f
                            openLeftR = outerR
                            openRightR = outerR
                        }
                        n >= MAX_EXTRA_KEYS_PER_ROW -> {
                            openX = openSidePad + i * (openKeyW + gap)
                            openLeftR = if (i == 0) outerR else innerR
                            openRightR = if (i == n - 1) outerR else innerR
                        }
                        n % 2 == 0 -> {
                            val half = n / 2
                            if (i < half) {
                                openX = openSidePad + i * (openKeyW + gap)
                                openLeftR = if (i == 0) outerR else innerR
                                openRightR = if (i == half - 1) outerR else innerR
                            } else {
                                val j = i - half
                                val rightGroupW = half * openKeyW + (half - 1) * gap
                                val rightStart = wF - openSidePad - rightGroupW
                                openX = rightStart + j * (openKeyW + gap)
                                openLeftR = if (j == 0) outerR else innerR
                                openRightR = if (j == half - 1) outerR else innerR
                            }
                        }
                        else -> {
                            val sideCount = (n - 1) / 2
                            val midIdx = sideCount
                            when {
                                i < midIdx -> {
                                    openX = openSidePad + i * (openKeyW + gap)
                                    openLeftR = if (i == 0) outerR else innerR
                                    openRightR = if (i == sideCount - 1) outerR else innerR
                                }
                                i == midIdx -> {
                                    openX = (wF - openKeyW) * 0.5f
                                    openLeftR = outerR
                                    openRightR = outerR
                                }
                                else -> {
                                    val j = i - (midIdx + 1)
                                    val rightGroupW = sideCount * openKeyW + (sideCount - 1) * gap
                                    val rightStart = wF - openSidePad - rightGroupW
                                    openX = rightStart + j * (openKeyW + gap)
                                    openLeftR = if (j == 0) outerR else innerR
                                    openRightR = if (j == sideCount - 1) outerR else innerR
                                }
                            }
                        }
                    }

                    restXArr[i] = closedX + (openX - closedX) * openF
                    restLeftRArr[i] = closedLeftR + (openLeftR - closedLeftR) * openF
                    restRightRArr[i] = closedRightR + (openRightR - closedRightR) * openF
                    deltaLeftEdge[i] = 0f
                    deltaRightEdge[i] = 0f
                }

                // Zero-sum neighbor length compression:
                // When key i expands, its immediate connected neighbor(s) shrink in length by the exact same amount
                // so no other keys in the row shift sideways!
                val maxExpand = dpF(11f)
                for (i in 0 until n) {
                    val p = pressProgress[i]
                    if (p <= 0.001f) continue
                    val expPx = maxExpand * p
                    val hasLeft = (i > 0) && abs(restXArr[i] - (restXArr[i - 1] + curBaseW + gap)) < dpF(6f)
                    val hasRight = (i < n - 1) && abs(restXArr[i + 1] - (restXArr[i] + curBaseW + gap)) < dpF(6f)
                    when {
                        hasLeft && hasRight -> {
                            val halfExp = expPx * 0.5f
                            deltaLeftEdge[i] -= halfExp
                            deltaRightEdge[i] += halfExp
                            deltaRightEdge[i - 1] -= halfExp
                            deltaLeftEdge[i + 1] += halfExp
                        }
                        hasRight -> {
                            // Leftmost in group: left edge stays anchored, expands right, right neighbor shrinks from left
                            deltaRightEdge[i] += expPx
                            deltaLeftEdge[i + 1] += expPx
                        }
                        hasLeft -> {
                            // Rightmost in group: right edge stays anchored, expands left, left neighbor shrinks from right
                            deltaLeftEdge[i] -= expPx
                            deltaRightEdge[i - 1] -= expPx
                        }
                        else -> {
                            // Standalone single button
                            val halfExp = expPx * 0.5f
                            deltaLeftEdge[i] -= halfExp
                            deltaRightEdge[i] += halfExp
                        }
                    }
                }

                val modMask = nativeGetModifierMask()
                for (i in 0 until n) {
                    val spec = actionSpecs[i]
                    val isModActive = (spec == "CTRL" && (ctrlLocked || (modMask and 1) != 0)) ||
                        (spec == "ALT" && (altLocked || (modMask and 2) != 0))
                    val pSelf = max(pressProgress[i], if (isModActive) 1f else 0f)

                    val finalLeft = restXArr[i] + deltaLeftEdge[i]
                    val finalRight = restXArr[i] + curBaseW + deltaRightEdge[i]
                    val finalW = (finalRight - finalLeft).roundToInt().coerceAtLeast(dp(24))

                    val morphT = safeClamp(pSelf, 0f, 1f)
                    val curLeftR = restLeftR(restLeftRArr[i], outerR, morphT)
                    val curRightR = restLeftR(restRightRArr[i], outerR, morphT)

                    val d = drawables[i]
                    d.cornerRadii = floatArrayOf(
                        curLeftR, curLeftR,
                        curRightR, curRightR,
                        curRightR, curRightR,
                        curLeftR, curLeftR
                    )

                    val btn = buttons[i]
                    val lp = btn.layoutParams as FrameLayout.LayoutParams
                    if (lp.width != finalW) {
                        lp.width = finalW
                        btn.layoutParams = lp
                    }
                    btn.translationX = finalLeft
                }

                // Compute connected sub-group horizontal bounds for the thin outer capsule background
                subGroupBounds.clear()
                var gStart = buttons[0].translationX
                var gEnd = gStart + buttons[0].layoutParams.width
                for (i in 1 until n) {
                    val bLeft = buttons[i].translationX
                    val bRight = bLeft + buttons[i].layoutParams.width
                    if (bLeft - gEnd <= dpF(8f)) {
                        gEnd = bRight
                    } else {
                        subGroupBounds.add(floatArrayOf(gStart, gEnd))
                        gStart = bLeft
                        gEnd = bRight
                    }
                }
                subGroupBounds.add(floatArrayOf(gStart, gEnd))
                rowContainer.invalidate()
            }

            rowContainer.addOnLayoutChangeListener { _, l, _, r, _, oldL, _, oldR, _ ->
                if ((r - l) != (oldR - oldL)) {
                    rowContainer.post { layoutRowNow() }
                }
            }
            rowContainer.post { layoutRowNow() }
            extraKeyRowUpdaters.add(layoutRowNow)
            extraKeysRoot.addView(rowContainer)
        }
        syncModifierButtons(nativeGetModifierMask())
    }

    internal fun dispatchExtraKeyWithHoldSupport(actionSpec: String) {
        terminalSurface.stopTextSelection()
        if (actionSpec == "PASTE" || actionSpec == "CLIP") {
            pasteFromClipboard()
            return
        }
        if (actionSpec.startsWith("CMD:")) {
            val cmdStr = actionSpec.removePrefix("CMD:").replace("\\n", "\n")
            nativeSendText(if (cmdStr.endsWith("\n")) cmdStr else "$cmdStr\n", false)
            terminalSurface.resetScrollAndBlink()
            return
        }
        if (actionSpec.startsWith("TXT:")) {
            val txtStr = actionSpec.removePrefix("TXT:").replace("\\n", "\n")
            nativeSendText(txtStr, false)
            terminalSurface.resetScrollAndBlink()
            return
        }
        if (actionSpec.contains("\n") || actionSpec.endsWith("\\n")) {
            val cleanCmd = actionSpec.replace("\\n", "\n")
            nativeSendText(cleanCmd, false)
            terminalSurface.resetScrollAndBlink()
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

    private fun createExtraKeyButton(
        displayLabel: String,
        actionSpec: String,
        bgDrawable: GradientDrawable,
        onPressStateChanged: (Boolean) -> Unit
    ): TextView {
        val isModifier = (actionSpec == "CTRL" || actionSpec == "ALT")
        return TextView(this).apply {
            text = displayLabel
            tag = actionSpec
            gravity = Gravity.CENTER
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_TEXT_BRIGHT)
            background = bgDrawable

            var holdTriggered = false
            var downTimeMs = 0L
            val releaseRunnable = Runnable {
                onPressStateChanged(false)
                if (!isModifier) {
                    bgDrawable.setColor(uiSurfaceContainer)
                    setTextColor(COLOR_TEXT_BRIGHT)
                } else {
                    syncModifierButtons(nativeGetModifierMask())
                }
            }
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
                        v.removeCallbacks(releaseRunnable)
                        v.isPressed = true
                        holdTriggered = false
                        downTimeMs = SystemClock.uptimeMillis()
                        triggerHaptic(1.0f)
                        onPressStateChanged(true)
                        if (!isModifier) {
                            bgDrawable.setColor(uiAccentContainer)
                            (v as TextView).setTextColor(uiOnAccentContainer)
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
                        if (isModifier && wasPressed && !holdTriggered && event.actionMasked == MotionEvent.ACTION_UP) {
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
                        val elapsed = SystemClock.uptimeMillis() - downTimeMs
                        if (elapsed < 115L) {
                            v.postDelayed(releaseRunnable, (115L - elapsed).coerceIn(35L, 110L))
                        } else {
                            releaseRunnable.run()
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

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int = blendColor(c1, c2, ratio)

    private fun safeClamp(value: Float, minVal: Float, maxVal: Float): Float {
        val hi = max(minVal, maxVal)
        return value.coerceIn(minVal, hi)
    }

    private fun safeClampInt(value: Int, minVal: Int, maxVal: Int): Int {
        val hi = max(minVal, maxVal)
        return value.coerceIn(minVal, hi)
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

    /**
     * Liquid Glass Capsule Drawable with translucent top specular highlight and subtle glass edge sheen.
     */
    private fun makeLiquidGlassPillDrawable(baseColor: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(baseColor)
            setStroke(0, 0)
        }
    }

    private fun makeConnectedSegmentDrawable(
        fillColor: Int,
        leftRadiusPx: Float,
        rightRadiusPx: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(0, 0)
            cornerRadii = floatArrayOf(
                leftRadiusPx, leftRadiusPx,
                rightRadiusPx, rightRadiusPx,
                rightRadiusPx, rightRadiusPx,
                leftRadiusPx, leftRadiusPx
            )
        }
    }

    private fun triggerFolderShakeAnimation(target: View, direction: Int = 1) {
        target.animate().cancel()
        target.scaleX = 0.72f
        target.scaleY = 0.72f
        target.rotation = (if (direction >= 0) -14f else 14f)
        target.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .rotation(0f)
            .setDuration(280L)
            .setInterpolator(OvershootInterpolator(1.85f))
            .start()
    }

    // Android 17 AOSP Emphasized Spatial Curve: cubic-bezier(0.2, 0.0, 0.0, 1.0)
    private val aosp17SpatialInterpolator by lazy {
        PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    }

    private val predictiveDecelerateInterpolator = PathInterpolator(0f, 0f, 0f, 1f)
    private val predictiveCommitInterpolator = PathInterpolator(0.1f, 0.1f, 0f, 1f)

    private fun ensureCenteredPivot(target: View) {
        val w = if (target.width > 0) target.width.toFloat() else resources.displayMetrics.widthPixels.toFloat()
        val h = if (target.height > 0) target.height.toFloat() else resources.displayMetrics.heightPixels.toFloat()
        target.pivotX = w * 0.5f
        target.pivotY = h * 0.5f
    }

    private fun applyRoundedCornersProgress(target: View, radiusPx: Float) {
        if (radiusPx <= 0.5f) {
            target.clipToOutline = false
        } else {
            target.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val vw = if (view.width > 0) view.width else resources.displayMetrics.widthPixels
                    val vh = if (view.height > 0) view.height else resources.displayMetrics.heightPixels
                    outline.setRoundRect(0, 0, vw, vh, radiusPx)
                }
            }
            target.clipToOutline = true
        }
    }

    /**
     * Official Android 14–17 Material 3 Predictive Back pre-commit surface transform:
     * Shrinks the active page toward 90% around its center with 28dp rounded corners and a subtle
     * edge margin shift (`(screenWidth / 20) - 8dp`), revealing the previous page underneath.
     */
    private fun applyPredictiveBackPreview(
        foregroundView: View,
        backgroundView: View?,
        backEvent: BackEvent
    ) {
        val rawP = backEvent.progress.coerceIn(0f, 1f)
        val ep = predictiveDecelerateInterpolator.getInterpolation(rawP)
        val screenW = max(resources.displayMetrics.widthPixels.toFloat(), dpF(320f))

        ensureCenteredPivot(foregroundView)
        val maxShiftX = max(dpF(10f), (screenW / 20f) - dpF(8f))
        val dir = if (backEvent.swipeEdge == BackEvent.EDGE_RIGHT) -1f else 1f

        val scale = 1f - (0.10f * ep) // 100% -> 90%
        foregroundView.scaleX = scale
        foregroundView.scaleY = scale
        foregroundView.translationX = dir * maxShiftX * ep
        foregroundView.translationY = 0f
        foregroundView.alpha = 1f
        applyRoundedCornersProgress(foregroundView, dpF(28f) * ep)

        if (backgroundView != null) {
            backgroundView.visibility = View.VISIBLE
            ensureCenteredPivot(backgroundView)
            backgroundView.translationX = 0f
            backgroundView.translationY = 0f
            val bgScale = 0.94f + (0.06f * ep)
            backgroundView.scaleX = bgScale
            backgroundView.scaleY = bgScale
            backgroundView.alpha = 0.50f + (0.50f * ep)
        }
    }

    private fun cancelPredictiveBackPreview(foregroundView: View, backgroundView: View?) {
        ensureCenteredPivot(foregroundView)
        foregroundView.animate()
            .translationX(0f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200L)
            .setInterpolator(aosp17SpatialInterpolator)
            .withEndAction {
                applyRoundedCornersProgress(foregroundView, 0f)
            }
            .start()
        if (backgroundView != null) {
            ensureCenteredPivot(backgroundView)
            backgroundView.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(0.94f)
                .scaleY(0.94f)
                .alpha(0.50f)
                .setDuration(200L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
        }
    }

    private fun animateAosp17PageTransition(target: View, forward: Boolean = true) {
        val dist = dpF(24f)
        target.alpha = 0f
        target.translationX = if (forward) dist else -dist
        target.translationY = 0f
        target.scaleX = 1f
        target.scaleY = 1f
        target.post {
            ensureCenteredPivot(target)
        }
        target.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(220L)
            .setInterpolator(aosp17SpatialInterpolator)
            .start()
    }

    private fun animateCenteredDialogAppear(sheet: View) {
        sheet.alpha = 0f
        sheet.translationX = 0f
        sheet.translationY = dpF(10f)
        sheet.scaleX = 0.94f
        sheet.scaleY = 0.94f
        sheet.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (sheet.viewTreeObserver.isAlive) {
                    sheet.viewTreeObserver.removeOnPreDrawListener(this)
                }
                sheet.pivotX = sheet.width * 0.5f
                sheet.pivotY = sheet.height * 0.5f
                sheet.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(210L)
                    .setInterpolator(aosp17SpatialInterpolator)
                    .start()
                return true
            }
        })
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

    private fun attachSmoothPressAnimation(target: View, minScale: Float = 0.95f) {
        var downTimeMs = 0L
        val releaseAction = Runnable {
            target.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240L)
                .setInterpolator(OvershootInterpolator(2.35f))
                .start()
        }
        target.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.removeCallbacks(releaseAction)
                    downTimeMs = SystemClock.uptimeMillis()
                    v.animate()
                        .scaleX(minScale)
                        .scaleY(minScale)
                        .setDuration(85L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val elapsed = SystemClock.uptimeMillis() - downTimeMs
                    if (elapsed < 85L) {
                        v.postDelayed(releaseAction, (85L - elapsed).coerceIn(25L, 75L))
                    } else {
                        releaseAction.run()
                    }
                }
            }
            false
        }
    }

    private fun createExpressiveDialog(maxWidthDp: Int = 430): Pair<Dialog, LinearLayout> {
        val targetW = min(resources.displayMetrics.widthPixels - dp(28), dp(maxWidthDp))
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumWidth = targetW
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = tonalBg(uiSurfaceHigh, dp(28))
            layoutParams = ViewGroup.LayoutParams(targetW, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dlg.setContentView(sheet, ViewGroup.LayoutParams(targetW, ViewGroup.LayoutParams.WRAP_CONTENT))
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(targetW, ViewGroup.LayoutParams.WRAP_CONTENT)
            attributes = attributes.apply {
                width = targetW
                dimAmount = 0.65f
            }
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        dlg.setOnShowListener {
            dlg.window?.setLayout(targetW, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        animateCenteredDialogAppear(sheet)
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

        var currentIndex = safeClampInt(selectedIndex, 0, max(0, items.lastIndex))

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

            val gapStart = safeClamp(thumbX - thumbW * 0.5f - gap, minX, maxX)
            val gapEnd = safeClamp(thumbX + thumbW * 0.5f + gap, minX, maxX)

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
        var tokens: IntArray,
        val isSwatchWithCenter: Boolean = false,
        val seedCenterColor: Int = 0,
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

    private val mediumTypeface: Typeface by lazy {
        try {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } catch (_: Throwable) {
            Typeface.DEFAULT_BOLD
        }
    }

    private fun makeSectionLabel(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            setTextColor(uiAccent)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valTv = TextView(this@TermuxActivity).apply {
                text = formatValue(initialProgress)
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
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
        checkedSubtitle: String? = null,
        uncheckedSubtitle: String? = null,
        onToggle: (Boolean) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
            val cardBgDrawable = if (isMasterSwitch) {
                tonalBg(
                    if (checked) uiAccentContainer else uiSurfaceLow,
                    dp(28)
                )
            } else {
                segmentedCardBg(index, total, uiSurfaceLow, 0, 22f, 5f)
            }
            background = cardBgDrawable
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = if (isMasterSwitch) dp(12) else if (index < total - 1) dp(2) else dp(8)
            }

            val iconBoxBg = tonalBg(
                if (isMasterSwitch && checked) uiAccent else uiAccentContainer,
                dp(12)
            )
            val iconView = MaterialIconView(
                this@TermuxActivity,
                icon,
                if (isMasterSwitch && checked) uiOnAccent else uiAccent,
                20.5f,
                2.05f
            )
            val iconBox = FrameLayout(this@TermuxActivity).apply {
                background = iconBoxBg
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(14)
                }
                addView(iconView)
            }
            addView(iconBox)

            val col = LinearLayout(this@TermuxActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(12)
                }
            }
            val titleTv = TextView(this@TermuxActivity).apply {
                text = title
                setTextColor(if (isMasterSwitch && checked) uiOnAccentContainer else COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            }
            val subTv = TextView(this@TermuxActivity).apply {
                text = subtitle
                setTextColor(if (isMasterSwitch && checked) withAlpha(uiOnAccentContainer, 215) else COLOR_TEXT_DIM)
                typeface = mediumTypeface
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.8f)
                setPadding(0, dp(2), 0, 0)
            }
            col.addView(titleTv)
            col.addView(subTv)
            addView(col)

            fun animateMasterCardColors(toChecked: Boolean) {
                triggerFolderShakeAnimation(iconBox, if (toChecked) 1 else -1)
                if (checkedSubtitle != null && uncheckedSubtitle != null) {
                    subTv.text = if (toChecked) checkedSubtitle else uncheckedSubtitle
                }
                if (!isMasterSwitch) return
                val fromP = if (toChecked) 0f else 1f
                val toP = if (toChecked) 1f else 0f
                ValueAnimator.ofFloat(fromP, toP).apply {
                    duration = 180L
                    interpolator = aosp17SpatialInterpolator
                    addUpdateListener { va ->
                        val p = va.animatedValue as Float
                        cardBgDrawable.setColor(blendColor(uiSurfaceLow, uiAccentContainer, p))
                        iconBoxBg.setColor(blendColor(uiAccentContainer, uiAccent, p))
                        iconView.tintColor = blendColor(uiAccent, uiOnAccent, p)
                        iconView.invalidate()
                        titleTv.setTextColor(blendColor(COLOR_TEXT_BRIGHT, uiOnAccentContainer, p))
                        subTv.setTextColor(blendColor(COLOR_TEXT_DIM, withAlpha(uiOnAccentContainer, 215), p))
                    }
                    start()
                }
            }

            val sw = M3ExpressiveSwitchView(this@TermuxActivity, checked) { newVal ->
                animateMasterCardColors(newVal)
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
        var curSelIdx = selectedIndex
        val currentValText = items.getOrNull(curSelIdx) ?: ""
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            })
            if (!subtitle.isNullOrBlank()) {
                col.addView(TextView(this@TermuxActivity).apply {
                    text = subtitle
                    setTextColor(COLOR_TEXT_DIM)
                    typeface = mediumTypeface
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setPadding(0, dp(2), 0, 0)
                })
            }
            val valLabelTv = TextView(this@TermuxActivity).apply {
                text = currentValText
                setTextColor(uiAccent)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, 0)
                visibility = if (currentValText.isNotBlank()) View.VISIBLE else View.GONE
            }
            col.addView(valLabelTv)
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
                    selectedIndex = curSelIdx,
                    onSelected = { chosen ->
                        curSelIdx = chosen
                        valLabelTv.text = items.getOrNull(chosen) ?: ""
                        onSelected(chosen)
                    }
                )
            }
        }
    }

    // ========================================================================
    // Full-Screen AOSP Material 3 Settings Page with Predictive Back Gesture
    // (Two-layer Stage: Home Menu underneath + Sub-page on top for live preview!)
    // ========================================================================
    private fun showCategorizedSettingsDialog() {
        val dlg = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(0x00000000))
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
            ensureCenteredPivot(detailScroll)
            ensureCenteredPivot(homeScroll)
            applyRoundedCornersProgress(detailScroll, 0f)
            detailScroll.translationX = 0f
            detailScroll.translationY = 0f
            detailScroll.alpha = 0f
            detailScroll.scaleX = 0.92f
            detailScroll.scaleY = 0.92f
            detailScroll.animate()
                .translationX(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(230L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()

            homeScroll.animate()
                .translationX(0f)
                .scaleX(0.94f)
                .scaleY(0.94f)
                .alpha(0.50f)
                .setDuration(230L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
        }

        fun navigateBackToHomeMenu() {
            if (!inSubPage) {
                ensureCenteredPivot(pageRoot)
                pageRoot.animate()
                    .scaleX(0.88f)
                    .scaleY(0.88f)
                    .alpha(0f)
                    .setDuration(190L)
                    .setInterpolator(predictiveCommitInterpolator)
                    .withEndAction {
                        dlg.dismiss()
                    }
                    .start()
                return
            }
            inSubPage = false
            titleTv.text = tr("Настройки", "Settings")
            rebuildHomeMenu()
            ensureCenteredPivot(homeScroll)
            ensureCenteredPivot(detailScroll)
            homeScroll.visibility = View.VISIBLE
            homeScroll.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(210L)
                .setInterpolator(predictiveCommitInterpolator)
                .start()
            // Scale down & fade out in place (never fly off to the right!)
            detailScroll.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .alpha(0f)
                .setDuration(200L)
                .setInterpolator(predictiveCommitInterpolator)
                .withEndAction {
                    if (!inSubPage) {
                        detailScroll.visibility = View.GONE
                        detailScroll.translationX = 0f
                        detailScroll.translationY = 0f
                        detailScroll.scaleX = 1f
                        detailScroll.scaleY = 1f
                        detailScroll.alpha = 1f
                        applyRoundedCornersProgress(detailScroll, 0f)
                    }
                }
                .start()
        }

        backBtn.setOnClickListener {
            triggerHaptic(0.8f)
            navigateBackToHomeMenu()
        }

        // Official Android 14–17 Material 3 Predictive Back Callback:
        // Shrinks the active page toward 90% with rounded 28dp corners and previews the previous page underneath!
        val predictiveBackCallback = object : OnBackAnimationCallback {
            override fun onBackStarted(backEvent: BackEvent) {
                homeScroll.animate().cancel()
                detailScroll.animate().cancel()
                pageRoot.animate().cancel()
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                if (inSubPage) {
                    applyPredictiveBackPreview(detailScroll, homeScroll, backEvent)
                } else {
                    applyPredictiveBackPreview(pageRoot, null, backEvent)
                }
            }

            override fun onBackCancelled() {
                if (inSubPage) {
                    cancelPredictiveBackPreview(detailScroll, homeScroll)
                } else {
                    cancelPredictiveBackPreview(pageRoot, null)
                }
            }

            override fun onBackInvoked() {
                triggerHaptic(0.75f)
                navigateBackToHomeMenu()
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

        fun applyNewColorThemeEverywhere(animateSurface: Boolean = true) {
            val prevSurface = uiSurface
            val prevContainer = uiSurfaceContainer
            refreshColorSpec2026FromCpp()
            val nextSurface = uiSurface
            val nextContainer = uiSurfaceContainer
            val backBtnBg = tonalBg(prevContainer, dp(20))
            backBtn.background = backBtnBg
            if (animateSurface && prevSurface != nextSurface) {
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 250L
                    interpolator = aosp17SpatialInterpolator
                    addUpdateListener { va ->
                        val p = va.animatedValue as Float
                        val sCol = blendColor(prevSurface, nextSurface, p)
                        pageRoot.setBackgroundColor(sCol)
                        detailScroll.setBackgroundColor(sCol)
                        backBtnBg.setColor(blendColor(prevContainer, nextContainer, p))
                    }
                    start()
                }
            } else {
                pageRoot.setBackgroundColor(nextSurface)
                detailScroll.setBackgroundColor(nextSurface)
                backBtnBg.setColor(nextContainer)
            }
            headerActionsUpdateTheme?.invoke()
            rebuildTabs()
            rebuildExtraKeysBar()
            terminalSurface.postInvalidateOnAnimation()
        }

        // Sub-page 1: Цвета и палитра (System Dynamic Monet by default + ColorBlendr 4 styles + Monochrome)
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
                        applyNewColorThemeEverywhere(animateSurface = true)
                        detailContainer.animate()
                            .alpha(0.7f)
                            .setDuration(90L)
                            .withEndAction {
                                populateColorsPage()
                                detailContainer.animate().alpha(1f).setDuration(170L).start()
                            }
                            .start()
                    }
                )

                val styleCardBgs = ArrayList<GradientDrawable>(MONET_STYLES_2026.size)
                val styleCardTitles = ArrayList<TextView>(MONET_STYLES_2026.size)
                val styleCardSubs = ArrayList<TextView>(MONET_STYLES_2026.size)
                val styleCardCanvases = ArrayList<ColorPreviewCanvasView>(MONET_STYLES_2026.size)
                val styleCardCurFill = IntArray(MONET_STYLES_2026.size)
                val styleCardCurTitleCol = IntArray(MONET_STYLES_2026.size)
                val styleCardCurSubCol = IntArray(MONET_STYLES_2026.size)

                fun animateStyleCardsInPlace() {
                    val effSeed = if (useSystemDynamicColor) getSystemDynamicSeedColor() else colorSeedArgb
                    val startFills = styleCardCurFill.clone()
                    val startTitles = styleCardCurTitleCol.clone()
                    val startSubs = styleCardCurSubCol.clone()
                    val targetFills = IntArray(MONET_STYLES_2026.size)
                    val targetTitles = IntArray(MONET_STYLES_2026.size)
                    val targetSubs = IntArray(MONET_STYLES_2026.size)

                    for ((idx, styleOpt) in MONET_STYLES_2026.withIndex()) {
                        val sel = (monetStyleIndex == styleOpt.index)
                        val toks = nativeGenerateColorSpec2026Palette(effSeed, styleOpt.index)
                        styleCardCanvases.getOrNull(idx)?.let { cv ->
                            cv.tokens = toks
                            cv.invalidate()
                        }
                        targetFills[idx] = if (sel) toks[2] else uiSurfaceLow
                        targetTitles[idx] = if (sel) toks[3] else COLOR_TEXT_BRIGHT
                        targetSubs[idx] = if (sel) withAlpha(toks[3], 205) else COLOR_TEXT_DIM
                    }

                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 240L
                        interpolator = aosp17SpatialInterpolator
                        addUpdateListener { va ->
                            val p = va.animatedValue as Float
                            for (idx in MONET_STYLES_2026.indices) {
                                val fCol = blendColor(startFills[idx], targetFills[idx], p)
                                val tCol = blendColor(startTitles[idx], targetTitles[idx], p)
                                val sCol = blendColor(startSubs[idx], targetSubs[idx], p)
                                styleCardCurFill[idx] = fCol
                                styleCardCurTitleCol[idx] = tCol
                                styleCardCurSubCol[idx] = sCol
                                styleCardBgs.getOrNull(idx)?.setColor(fCol)
                                styleCardTitles.getOrNull(idx)?.setTextColor(tCol)
                                styleCardSubs.getOrNull(idx)?.setTextColor(sCol)
                            }
                        }
                        start()
                    }
                }

                if (!useSystemDynamicColor) {
                    detailContainer.addView(makeSectionLabel(tr("Базовый оттенок", "Seed color")))
                    val swatchCardBg = segmentedCardBg(0, 1, uiSurfaceLow, 0, 24f, 5f)
                    val swatchCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        background = swatchCardBg
                    }
                    val (swatchScroll, swatchRow) = makeHorizontalChipRow()
                    val swatchCanvases = ArrayList<Pair<Int, ColorPreviewCanvasView>>(COLORBLENDR_SEEDS.size)

                    for (seedOpt in COLORBLENDR_SEEDS) {
                        val previewTokens = nativeGenerateColorSpec2026Palette(seedOpt.seedArgb, monetStyleIndex)
                        val isSel = (colorSeedArgb == seedOpt.seedArgb)
                        val swatchCanvas = ColorPreviewCanvasView(
                            this,
                            tokens = previewTokens,
                            isSwatchWithCenter = true,
                            seedCenterColor = seedOpt.seedArgb,
                            isSwatchSelected = isSel
                        ).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
                        }
                        swatchCanvases.add(seedOpt.seedArgb to swatchCanvas)

                        val swatchWrap = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER_HORIZONTAL
                            setPadding(dp(6), dp(4), dp(6), dp(4))
                            attachSmoothPressAnimation(this, 0.90f)
                            setOnClickListener {
                                triggerHaptic(0.85f)
                                val prevLow = uiSurfaceLow
                                colorSeedArgb = seedOpt.seedArgb
                                prefs.edit().putInt(KEY_COLOR_SEED_V2, colorSeedArgb).apply()
                                applyNewColorThemeEverywhere(animateSurface = true)
                                val nextLow = uiSurfaceLow
                                ValueAnimator.ofFloat(0f, 1f).apply {
                                    duration = 230L
                                    interpolator = aosp17SpatialInterpolator
                                    addUpdateListener { va ->
                                        swatchCardBg.setColor(blendColor(prevLow, nextLow, va.animatedValue as Float))
                                    }
                                    start()
                                }
                                for ((sArgb, cv) in swatchCanvases) {
                                    cv.isSwatchSelected = (sArgb == colorSeedArgb)
                                    cv.tokens = nativeGenerateColorSpec2026Palette(sArgb, monetStyleIndex)
                                    cv.invalidate()
                                }
                                animateStyleCardsInPlace()
                            }
                        }
                        swatchWrap.addView(swatchCanvas)
                        if (seedOpt.name == "Monochrome") {
                            swatchWrap.addView(TextView(this).apply {
                                text = tr("Монохром", "Mono")
                                setTextColor(COLOR_TEXT_DIM)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
                                setPadding(0, dp(2), 0, 0)
                            })
                        }
                        swatchRow.addView(swatchWrap)
                    }
                    swatchCard.addView(swatchScroll)
                    detailContainer.addView(swatchCard)
                }

                detailContainer.addView(makeSectionLabel(tr("Стиль палитры", "Palette style")))
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
                    val fillCol = if (isSel) styleTokens[2] else uiSurfaceLow
                    val titleCol = if (isSel) styleTokens[3] else COLOR_TEXT_BRIGHT
                    val subCol = if (isSel) withAlpha(styleTokens[3], 205) else COLOR_TEXT_DIM

                    styleCardCurFill[idx] = fillCol
                    styleCardCurTitleCol[idx] = titleCol
                    styleCardCurSubCol[idx] = subCol

                    val cardBg = segmentedCardBg(
                        idx,
                        MONET_STYLES_2026.size,
                        fillCol,
                        0,
                        24f,
                        5f
                    )
                    styleCardBgs.add(cardBg)

                    val styleCard = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(18), dp(15), dp(18), dp(15))
                        background = cardBg
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = if (idx < MONET_STYLES_2026.lastIndex) dp(2) else dp(8)
                        }
                        attachSmoothPressAnimation(this, 0.98f)
                        setOnClickListener {
                            if (monetStyleIndex != styleOpt.index) {
                                triggerHaptic(0.85f)
                                monetStyleIndex = styleOpt.index
                                prefs.edit().putInt(KEY_MONET_STYLE_V2, monetStyleIndex).apply()
                                applyNewColorThemeEverywhere(animateSurface = true)
                                animateStyleCardsInPlace()
                            }
                        }
                    }

                    val previewCv = ColorPreviewCanvasView(this, tokens = styleTokens).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                            marginEnd = dp(16)
                        }
                    }
                    styleCardCanvases.add(previewCv)
                    styleCard.addView(previewCv)

                    val textCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val tTv = TextView(this).apply {
                        text = styleOpt.title
                        setTextColor(titleCol)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    }
                    val sTv = TextView(this).apply {
                        text = tr(styleOpt.description, enDescriptions[idx])
                        setTextColor(subCol)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(0, dp(3), 0, 0)
                    }
                    styleCardTitles.add(tTv)
                    styleCardSubs.add(sTv)
                    textCol.addView(tTv)
                    textCol.addView(sTv)
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
                        total = 3
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
                            tr("Ряд 1 (${row1Tokens.size} / $MAX_EXTRA_KEYS_PER_ROW)", "Row 1 (${row1Tokens.size} / $MAX_EXTRA_KEYS_PER_ROW)"),
                            tr("Ряд 2 (${row2Tokens.size} / $MAX_EXTRA_KEYS_PER_ROW)", "Row 2 (${row2Tokens.size} / $MAX_EXTRA_KEYS_PER_ROW)")
                        ),
                        selectedIndex = activeEditRow,
                        index = 1,
                        total = 3
                    ) { chosen ->
                        activeEditRow = chosen
                        populateKeyboardPage()
                    }
                )

                detailContainer.addView(
                    makeBakaSuSwitchCard(
                        icon = M3Icon.EYE,
                        title = tr("Автоскрытие при прокрутке", "Auto-hide on scroll"),
                        subtitle = tr("Плавно скрывать панель клавиш при прокрутке истории вверх", "Smoothly hide extra keys bar when scrolling up into history"),
                        checked = autoHideExtraKeysOnScroll,
                        index = 2,
                        total = 3
                    ) { enabled ->
                        autoHideExtraKeysOnScroll = enabled
                        prefs.edit().putBoolean(KEY_AUTO_HIDE_EXTRA_KEYS, enabled).apply()
                        if (!enabled) setExtraKeysScrollHidden(false)
                    }
                )

                detailContainer.addView(makeSectionLabel(tr("Кнопки ряда ${activeEditRow + 1} (${targetList.size}/$MAX_EXTRA_KEYS_PER_ROW)", "Row ${activeEditRow + 1} buttons (${targetList.size}/$MAX_EXTRA_KEYS_PER_ROW)")))
                val activeKeysCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    background = segmentedCardBg(0, 2, uiSurfaceLow, 0, 22f, 5f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(2)
                    }
                }
                if (targetList.isEmpty()) {
                    activeKeysCard.addView(TextView(this).apply {
                        text = tr("Ряд пуст — выберите клавиши из таблицы ниже", "Row is empty — tap keys from the grid below")
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                        setPadding(dp(4), dp(4), dp(4), dp(6))
                    })
                } else {
                    // Multi-row wrap grid for active row tokens (up to 4 per row so all are visible without horizontal scroll)
                    val chunkedActive = targetList.withIndex().chunked(4)
                    for (rowChunk in chunkedActive) {
                        val rLine = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = dp(6)
                            }
                        }
                        for (item in rowChunk) {
                            val idx = item.index
                            val (disp, _) = parseKeyToken(item.value)
                            rLine.addView(
                                makeExpressiveChip("$disp  ✕", true) {
                                    targetList.removeAt(idx)
                                    persistRows()
                                    populateKeyboardPage()
                                }.apply {
                                    layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                                        marginEnd = dp(6)
                                    }
                                }
                            )
                        }
                        for (rem in rowChunk.size until 4) {
                            rLine.addView(View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                                    marginEnd = dp(6)
                                }
                            })
                        }
                        activeKeysCard.addView(rLine)
                    }
                }

                val actActionsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(6), 0, 0)
                }
                actActionsRow.addView(makeExpressiveChip(tr("+ Своя клавиша / Макрос", "+ Custom key / Macro"), true) {
                    val (bindDlg, bindSheet) = createExpressiveDialog(420)
                    bindSheet.addView(TextView(this).apply {
                        text = tr("Конструктор клавиши / макроса", "Custom Key & Macro Builder")
                        setTextColor(COLOR_TEXT_BRIGHT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(0, 0, 0, dp(4))
                    })
                    bindSheet.addView(TextView(this).apply {
                        text = tr("Создайте кнопку для мгновенного ввода команды, текста или сочетания клавиш", "Create a button to send a shell command, text snippet, or key combo")
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(0, 0, 0, dp(10))
                    })

                    var macroMode = 0 // 0 = CMD (with Enter), 1 = TXT (without Enter), 2 = Key / Combo
                    val modeRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 0, 0, dp(10))
                    }
                    val lblEdit = EditText(this).apply {
                        hint = tr("Надпись на кнопке (напр. top, git, C-c)", "Button label (e.g. top, git, C-c)")
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        background = tonalBg(uiSurfaceLow, dp(14))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        setSingleLine(true)
                    }
                    val actEdit = EditText(this).apply {
                        hint = tr("Команда с авто-Enter (напр. htop)", "Shell command with Enter (e.g. htop)")
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
                            topMargin = dp(8)
                        }
                    }

                    fun rebuildModePills() {
                        modeRow.removeAllViews()
                        val modes = listOf(
                            tr("Команда ↵", "Command ↵"),
                            tr("Текст", "Text"),
                            tr("Клавиша", "Key combo")
                        )
                        for ((mIdx, mTitle) in modes.withIndex()) {
                            modeRow.addView(
                                makeExpressiveChip(mTitle, macroMode == mIdx) {
                                    macroMode = mIdx
                                    actEdit.hint = when (macroMode) {
                                        0 -> tr("Команда с авто-Enter (напр. htop)", "Shell command with Enter (e.g. htop)")
                                        1 -> tr("Текст без Enter (напр. ssh root@)", "Text snippet without Enter (e.g. ssh root@)")
                                        else -> tr("Код клавиши (напр. CTRL+C, ALT+B, F5)", "Key spec (e.g. CTRL+C, ALT+B, F5)")
                                    }
                                    rebuildModePills()
                                }.apply {
                                    layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                                        if (mIdx < 2) marginEnd = dp(6)
                                    }
                                }
                            )
                        }
                    }
                    rebuildModePills()
                    bindSheet.addView(modeRow)
                    bindSheet.addView(lblEdit)
                    bindSheet.addView(actEdit)

                    // Quick templates row inside the dialog
                    bindSheet.addView(TextView(this).apply {
                        text = tr("Готовые шаблоны (нажмите для подстановки):", "Quick templates (tap to fill):")
                        setTextColor(COLOR_TEXT_DIM)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(0, dp(10), 0, dp(6))
                    })
                    val tplGrid = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    val templates = listOf(
                        Triple("htop", "htop", 0),
                        Triple("cls", "clear", 0),
                        Triple("su", "su", 0),
                        Triple("git st", "git status", 0),
                        Triple("ssh", "ssh ", 1),
                        Triple("grep", " | grep ", 1),
                        Triple("C-x", "CTRL+X", 2),
                        Triple("A-.", "ALT+.", 2)
                    )
                    for (tChunk in templates.chunked(4)) {
                        val tRow = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = dp(6)
                            }
                        }
                        for ((tLbl, tAct, tMode) in tChunk) {
                            tRow.addView(
                                makeExpressiveChip(tLbl, false) {
                                    macroMode = tMode
                                    rebuildModePills()
                                    lblEdit.setText(tLbl)
                                    actEdit.setText(tAct)
                                }.apply {
                                    layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f).apply {
                                        marginEnd = dp(5)
                                    }
                                }
                            )
                        }
                        tplGrid.addView(tRow)
                    }
                    bindSheet.addView(tplGrid)

                    val btnBar = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(12), 0, 0)
                    }
                    btnBar.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { bindDlg.dismiss() })
                    btnBar.addView(makeExpressiveChip(tr("Добавить", "Add"), true) {
                        if (targetList.size >= MAX_EXTRA_KEYS_PER_ROW) {
                            Toast.makeText(
                                this@TermuxActivity,
                                tr("Максимум $MAX_EXTRA_KEYS_PER_ROW клавиш в одном ряду", "Maximum $MAX_EXTRA_KEYS_PER_ROW keys per row"),
                                Toast.LENGTH_SHORT
                            ).show()
                            bindDlg.dismiss()
                            return@makeExpressiveChip
                        }
                        val l = lblEdit.text.toString().trim().replace("|", "")
                        val aRaw = actEdit.text.toString().trim().replace("|", "")
                        if (l.isNotEmpty() || aRaw.isNotEmpty()) {
                            val disp = l.ifEmpty { aRaw.take(6) }
                            val body = aRaw.ifEmpty { l }
                            val finalAct = when (macroMode) {
                                0 -> if (body.startsWith("CMD:")) body else "CMD:$body"
                                1 -> if (body.startsWith("TXT:")) body else "TXT:$body"
                                else -> body
                            }
                            val tok = if (disp != finalAct) "$disp::$finalAct" else finalAct
                            targetList.add(tok)
                            persistRows()
                            populateKeyboardPage()
                        }
                        bindDlg.dismiss()
                    })
                    bindSheet.addView(btnBar)
                    bindDlg.show()
                })
                actActionsRow.addView(makeExpressiveChip(tr("Очистить", "Clear"), false) {
                    targetList.clear()
                    persistRows()
                    populateKeyboardPage()
                })
                actActionsRow.addView(makeExpressiveChip(tr("Сброс", "Reset"), false) {
                    loadPreset(DEFAULT_EXTRA_KEYS_SPEC)
                    Toast.makeText(
                        this@TermuxActivity,
                        tr("Клавиши сброшены по умолчанию", "Extra keys reset to default"),
                        Toast.LENGTH_SHORT
                    ).show()
                })
                activeKeysCard.addView(actActionsRow)
                detailContainer.addView(activeKeysCard)

                val catalogCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    background = segmentedCardBg(1, 2, uiSurfaceLow, 0, 22f, 5f)
                }
                catalogCard.addView(TextView(this).apply {
                    text = tr("Каталог специальных клавиш (нажмите для добавления)", "Special keys catalog (tap to add)")
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setPadding(0, 0, 0, dp(8))
                })

                val categorizedKeys = listOf(
                    tr("Модификаторы и ввод", "Modifiers & input") to listOf(
                        "ESC", "TAB", "CTRL", "ALT", "ENTER", "BKSP", "DEL", "SPC", "PASTE::PASTE"
                    ),
                    tr("Навигация и курсор", "Navigation & cursor") to listOf(
                        "▲", "▼", "◀", "▶", "HOME", "END", "PGUP", "PGDN"
                    ),
                    tr("Символы терминала", "Terminal symbols") to listOf(
                        "-", "/", "|", "\\", "_", "~", ":", ";", "\"", "'", "&", "$", "#", "*"
                    ),
                    tr("Сигналы и функции", "Signals & F-keys") to listOf(
                        "C-c::CTRL+C", "C-z::CTRL+Z", "C-d::CTRL+D", "C-l::CTRL+L",
                        "C-a::CTRL+A", "C-e::CTRL+E", "C-r::CTRL+R", "C-w::CTRL+W",
                        "F1", "F2", "F3", "F4", "F5", "F10", "F12"
                    )
                )

                for ((catName, keyItems) in categorizedKeys) {
                    catalogCard.addView(TextView(this).apply {
                        text = catName
                        setTextColor(uiAccent)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(dp(2), dp(6), dp(2), dp(6))
                    })
                    for (kChunk in keyItems.chunked(4)) {
                        val gridRow = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = dp(6)
                            }
                        }
                        for (k in kChunk) {
                            val (d, _) = parseKeyToken(k)
                            gridRow.addView(
                                makeExpressiveChip("+ $d", false) {
                                    if (targetList.size >= MAX_EXTRA_KEYS_PER_ROW) {
                                        Toast.makeText(
                                            this@TermuxActivity,
                                            tr("Максимум $MAX_EXTRA_KEYS_PER_ROW клавиш в одном ряду", "Maximum $MAX_EXTRA_KEYS_PER_ROW keys per row"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        targetList.add(k)
                                        persistRows()
                                        populateKeyboardPage()
                                    }
                                }.apply {
                                    layoutParams = LinearLayout.LayoutParams(0, dp(35), 1f).apply {
                                        marginEnd = dp(6)
                                    }
                                }
                            )
                        }
                        for (rem in kChunk.size until 4) {
                            gridRow.addView(View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(0, dp(35), 1f).apply {
                                    marginEnd = dp(6)
                                }
                            })
                        }
                        catalogCard.addView(gridRow)
                    }
                }
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
                        isMasterSwitch = true,
                        checkedSubtitle = tr("Вибрация активна", "Haptics enabled"),
                        uncheckedSubtitle = tr("Вибрация отключена", "Haptics disabled")
                    ) { next ->
                        hapticEnabled = next
                        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, next).apply()
                        if (next) triggerHaptic(1.0f)
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
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                    typeface = Typeface.DEFAULT_BOLD
                })
                textCol.addView(TextView(this@TermuxActivity).apply {
                    text = subtitle
                    setTextColor(COLOR_TEXT_DIM)
                    typeface = mediumTypeface
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
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

            homeContainer.addView(makeSectionLabel(tr("Память и фоновый режим", "Storage & background")))

            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Доступ к файлам", "File access permissions"),
                    subtitle = tr("Спец. доступ ко всем файлам и обычное разрешение хранилища", "All files special access and standard storage permission"),
                    icon = M3Icon.STORAGE,
                    index = 0,
                    total = 2
                ) { showStoragePermissionsModal { rebuildHomeMenu() } }
            )

            val bgActive = hasIgnoreBatteryOptimizationsPermission()
            homeContainer.addView(
                makeMainSettingsNavCard(
                    title = tr("Работа в фоне без ограничений", "Unrestricted background execution"),
                    subtitle = if (bgActive) tr("Активно • Фоновые сессии и SSH защищены от выгрузки", "Active • Background sessions and SSH are protected")
                    else tr("Разрешить работу в фоне без ограничений батареи", "Allow background activity without battery restrictions"),
                    icon = M3Icon.PING,
                    index = 1,
                    total = 2
                ) {
                    requestIgnoreBatteryOptimizationsPermission()
                }
            )
        }

        rebuildHomeMenu()
        animateCenteredDialogAppear(pageRoot)
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

        if (targetView === rootLayout) {
            var isImeAnimating = false
            var maxObservedImePx = dp(280)
            var lastAppliedBottomPx = initBot
            var lastAppliedFraction = -1f

            fun applyExtraKeysImeFractionIfChanged(frac: Float) {
                val clamped = safeClamp(frac, 0f, 1f)
                if (abs(clamped - lastAppliedFraction) < 0.004f) return
                lastAppliedFraction = clamped
                imeOpenFraction = clamped
                val botPad = (dpF(18f) * (1f - clamped) + dpF(4f) * clamped).roundToInt()
                if (extraKeysRoot.paddingBottom != botPad) {
                    extraKeysRoot.setPadding(0, dp(4), 0, botPad)
                }
                for (updater in extraKeyRowUpdaters) {
                    updater()
                }
            }

            targetView.setWindowInsetsAnimationCallback(
                object : WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onPrepare(animation: WindowInsetsAnimation) {
                        if ((animation.typeMask and WindowInsets.Type.ime()) != 0) {
                            isImeAnimating = true
                            imeTransitionAnimator?.cancel()
                            // Unhide extraKeysRoot if it was scroll-hidden
                            if (isExtraKeysScrollHidden) {
                                isExtraKeysScrollHidden = false
                                extraKeysRoot.animate().cancel()
                                extraKeysRoot.translationY = 0f
                                extraKeysRoot.alpha = 1f
                            }
                        }
                    }

                    override fun onProgress(
                        insets: WindowInsets,
                        runningAnimations: MutableList<WindowInsetsAnimation>
                    ): WindowInsets {
                        var hasImeAnim = false
                        for (a in runningAnimations) {
                            if ((a.typeMask and WindowInsets.Type.ime()) != 0) {
                                hasImeAnim = true
                                break
                            }
                        }
                        if (!hasImeAnim) return insets

                        val sysBars = insets.getInsets(
                            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                        )
                        val ime = insets.getInsets(WindowInsets.Type.ime())
                        if (ime.bottom > maxObservedImePx) {
                            maxObservedImePx = ime.bottom
                        }
                        val safeTop = max(sysBars.top, fallbackStatusBarPx) + dp(extraTopDp)
                        val curBottom = max(sysBars.bottom, ime.bottom) + dp(extraBottomDp)
                        if (curBottom != lastAppliedBottomPx) {
                            lastAppliedBottomPx = curBottom
                            targetView.setPadding(
                                sysBars.left + dp(extraLeftDp),
                                safeTop,
                                sysBars.right + dp(extraRightDp),
                                curBottom
                            )
                        }

                        val span = max(dp(160), maxObservedImePx - sysBars.bottom).toFloat()
                        val rawFrac = safeClamp((ime.bottom - sysBars.bottom).toFloat() / span, 0f, 1f)
                        applyExtraKeysImeFractionIfChanged(rawFrac)
                        return insets
                    }

                    override fun onEnd(animation: WindowInsetsAnimation) {
                        if ((animation.typeMask and WindowInsets.Type.ime()) != 0) {
                            isImeAnimating = false
                            val curInsets = targetView.rootWindowInsets
                            val sysB = curInsets?.getInsets(
                                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                            )?.bottom ?: 0
                            val imeB = curInsets?.getInsets(WindowInsets.Type.ime())?.bottom ?: 0
                            val safeTop = max(
                                curInsets?.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())?.top ?: 0,
                                fallbackStatusBarPx
                            ) + dp(extraTopDp)
                            val finalBot = max(sysB, imeB) + dp(extraBottomDp)
                            lastAppliedBottomPx = finalBot
                            targetView.setPadding(
                                (curInsets?.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())?.left ?: 0) + dp(extraLeftDp),
                                safeTop,
                                (curInsets?.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())?.right ?: 0) + dp(extraRightDp),
                                finalBot
                            )
                            val nowOpen = imeB > dp(80)
                            isImeCurrentlyOpen = nowOpen
                            applyExtraKeysImeFractionIfChanged(if (nowOpen) 1f else 0f)
                            terminalSurface.postInvalidateOnAnimation()
                        }
                    }
                }
            )

            targetView.setOnApplyWindowInsetsListener { v, insets ->
                val sysBars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                val ime = insets.getInsets(WindowInsets.Type.ime())
                if (ime.bottom > maxObservedImePx) {
                    maxObservedImePx = ime.bottom
                }
                val safeTop = max(sysBars.top, fallbackStatusBarPx) + dp(extraTopDp)
                val targetBottom = max(sysBars.bottom, ime.bottom) + dp(extraBottomDp)
                val nowOpen = ime.bottom > dp(80)

                if (!isImeAnimating) {
                    if (nowOpen != isImeCurrentlyOpen) {
                        val startBottom = lastAppliedBottomPx
                        lastAppliedBottomPx = targetBottom
                        isImeCurrentlyOpen = nowOpen
                        val startF = imeOpenFraction
                        val endF = if (nowOpen) 1f else 0f

                        imeTransitionAnimator?.cancel()
                        imeTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                            duration = 250L
                            interpolator = aosp17SpatialInterpolator
                            addUpdateListener { va ->
                                val t = safeClamp(va.animatedValue as Float, 0f, 1f)
                                val curBot = (startBottom + (targetBottom - startBottom) * t).roundToInt()
                                v.setPadding(
                                    sysBars.left + dp(extraLeftDp),
                                    safeTop,
                                    sysBars.right + dp(extraRightDp),
                                    curBot
                                )
                                applyExtraKeysImeFractionIfChanged(startF + (endF - startF) * t)
                            }
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    v.setPadding(
                                        sysBars.left + dp(extraLeftDp),
                                        safeTop,
                                        sysBars.right + dp(extraRightDp),
                                        targetBottom
                                    )
                                    applyExtraKeysImeFractionIfChanged(endF)
                                    terminalSurface.postInvalidateOnAnimation()
                                }
                            })
                            start()
                        }
                    } else {
                        lastAppliedBottomPx = targetBottom
                        v.setPadding(
                            sysBars.left + dp(extraLeftDp),
                            safeTop,
                            sysBars.right + dp(extraRightDp),
                            targetBottom
                        )
                    }
                }
                insets
            }
        } else {
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
        }
        targetView.post { targetView.requestApplyInsets() }
    }

    /**
     * Exact Material 3 Expressive LoadingIndicator morphing shape view
     * matching video #2 & user screenshots (image_89a9a8.png .. image_89a967.png).
     * Morphs between 10-star SoftBurst, 9-wavy Cookie9Sided, flat-edged Pentagon,
     * 4-lobed Cookie4Sided, tilted Oval/Pill, and rotated Sunny star!
     */
    internal inner class BakaSuMorphingIndicatorView(context: Context) : View(context) {
        var morphPhase: Float = 0f
            set(value) {
                field = value
                invalidate()
            }
        var pullProgress: Float = 1f
            set(value) {
                field = value
                invalidate()
            }
        var isIndeterminateRefreshing: Boolean = true
            set(value) {
                field = value
                invalidate()
            }

        private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            isDither = true
        }
        private val morphPath = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width * 0.5f
            val cy = height * 0.5f
            val baseR = min(cx, cy) * 0.94f
            if (baseR <= 1f) return

            // Compute exact C++26 Tangent-Fillet RoundedPolygon vertices matching video #2 (120 vertices):
            // 0: SoftBurst (10-star, image_89a9a8.png) -> 1: Cookie9Sided (9-cookie, image_89a9a2.png)
            // -> 2: Pentagon (5-gon, image_89a988.png) -> 3: Cookie4Sided (4-clover, image_89a984.png)
            // -> 4: Oval/Pill (image_89a981.png) -> 5: Sunny (10-star rotated, image_89a967.png)
            val coords = nativeComputeM3LoadingPolygon(
                morphPhase,
                pullProgress,
                isIndeterminateRefreshing,
                baseR
            )
            val n = coords.size / 2
            if (n < 3) return

            morphPath.reset()
            morphPath.moveTo(cx + coords[0], cy + coords[1])
            for (i in 1 until n) {
                morphPath.lineTo(cx + coords[i * 2], cy + coords[i * 2 + 1])
            }
            morphPath.close()
            shapePaint.color = uiOnAccentContainer
            canvas.drawPath(morphPath, shapePaint)
        }
    }

    internal inner class BakaSuPullToRefreshLayout(
        context: Context,
        val scrollView: ScrollView,
        private val onRefreshTriggered: (() -> Unit) -> Unit
    ) : FrameLayout(context) {

        // Secondary constructor for zero-arg callback callers
        constructor(context: Context, scrollView: ScrollView, simpleRefresh: () -> Unit) :
            this(context, scrollView, { finishPtr ->
                simpleRefresh()
                finishPtr()
            })

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val triggerDistancePx = dpF(64f)
        private val maxPullPx = dpF(126f)

        private var downX = 0f
        private var downY = 0f
        private var pullStartY = 0f
        private var isDragging = false
        private var isRefreshing = false
        private var thresholdCrossed = false
        private var currentPullPx = 0f
        private var refreshStartUptimeMs = 0L
        private var spinAnimator: ValueAnimator? = null

        private val indicatorBadge: FrameLayout
        private val morphIndicator: BakaSuMorphingIndicatorView

        init {
            clipChildren = false
            clipToPadding = false
            addView(
                scrollView,
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            morphIndicator = BakaSuMorphingIndicatorView(context).apply {
                isIndeterminateRefreshing = false
                pullProgress = 0f
            }
            indicatorBadge = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(uiAccentContainer)
                }
                elevation = dpF(6f)
                layoutParams = LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                translationY = -dpF(54f)
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
                addView(
                    morphIndicator,
                    LayoutParams(dp(38), dp(38), Gravity.CENTER)
                )
            }
            addView(indicatorBadge)
        }

        fun finishRefresh() {
            if (!isRefreshing) return
            val elapsed = SystemClock.uptimeMillis() - refreshStartUptimeMs
            val remainingMin = (420L - elapsed).coerceAtLeast(0L)
            postDelayed({
                if (!isRefreshing) return@postDelayed
                isRefreshing = false
                indicatorBadge.animate()
                    .translationY(-dpF(54f))
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(220L)
                    .setInterpolator(predictiveCommitInterpolator)
                    .withEndAction {
                        spinAnimator?.cancel()
                    }
                    .start()
            }, remainingMin)
        }

        private fun isScrolledToTop(): Boolean {
            return !scrollView.canScrollVertically(-1) && scrollView.scrollY <= 0
        }

        override fun onDetachedFromWindow() {
            spinAnimator?.cancel()
            super.onDetachedFromWindow()
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
                    currentPullPx = (rawDelta * 0.52f).coerceAtMost(maxPullPx)
                    val progress = (currentPullPx / triggerDistancePx).coerceIn(0f, 1.25f)

                    (indicatorBadge.background as? GradientDrawable)?.setColor(uiAccentContainer)
                    indicatorBadge.translationY = -dpF(52f) + min(currentPullPx * 0.90f, dpF(72f))
                    val pClamped = progress.coerceIn(0f, 1f)
                    val easedScale = (1f - (1f - pClamped) * (1f - pClamped)).coerceIn(0f, 1f)
                    indicatorBadge.scaleX = easedScale
                    indicatorBadge.scaleY = easedScale
                    indicatorBadge.alpha = (progress * 1.85f).coerceIn(0f, 1f)

                    morphIndicator.isIndeterminateRefreshing = false
                    morphIndicator.pullProgress = progress

                    if (progress >= 1.0f && !thresholdCrossed) {
                        thresholdCrossed = true
                        triggerHaptic(1.05f)
                    } else if (progress < 0.92f && thresholdCrossed) {
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
                            refreshStartUptimeMs = SystemClock.uptimeMillis()
                            morphIndicator.isIndeterminateRefreshing = true
                            morphIndicator.morphPhase = 0f

                            indicatorBadge.animate()
                                .translationY(dpF(16f))
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .alpha(1.0f)
                                .setDuration(200L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()

                            spinAnimator?.cancel()
                            spinAnimator = ValueAnimator.ofFloat(0f, 6f).apply {
                                duration = 3900L
                                repeatCount = ValueAnimator.INFINITE
                                interpolator = LinearInterpolator()
                                addUpdateListener { anim ->
                                    morphIndicator.morphPhase = anim.animatedValue as Float
                                }
                                start()
                            }

                            onRefreshTriggered { finishRefresh() }
                        } else {
                            indicatorBadge.animate()
                                .translationY(-dpF(54f))
                                .scaleX(0f)
                                .scaleY(0f)
                                .alpha(0f)
                                .setDuration(200L)
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

    private var headerActionsUpdateTheme: (() -> Unit)? = null

    private fun makeHeaderConnectedActionsGroup(): LinearLayout {
        val groupLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = tonalBg(uiSurfaceLow, dp(22))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            clipChildren = false
            clipToPadding = false
        }
        data class HeaderActionSpec(
            val icon: M3Icon,
            val isDanger: Boolean,
            val onClick: () -> Unit,
            val onLongClick: (() -> Unit)? = null
        )
        val specs = listOf(
            HeaderActionSpec(
                icon = M3Icon.ADD,
                isDanger = false,
                onClick = { createSession(false) },
                onLongClick = { terminalSurface.showTranscriptCopyDialog() }
            ),
            HeaderActionSpec(
                icon = M3Icon.HUB_GRID,
                isDanger = false,
                onClick = { showWorkspaceHubDialog(initialTab = 0) },
                onLongClick = { showWorkspaceHubDialog(initialTab = 1) }
            ),
            HeaderActionSpec(
                icon = M3Icon.SETTINGS,
                isDanger = false,
                onClick = { showCategorizedSettingsDialog() }
            ),
            HeaderActionSpec(
                icon = M3Icon.CLOSE,
                isDanger = true,
                onClick = { closeCurrentSession() }
            )
        )
        val n = specs.size
        val btnH = dp(31)
        val baseW = dpF(35f)
        val outerR = btnH * 0.5f
        val innerR = dpF(5f)

        val frames = ArrayList<FrameLayout>(n)
        val bgs = ArrayList<GradientDrawable>(n)
        val icons = ArrayList<MaterialIconView>(n)
        val pressProg = FloatArray(n)
        val anims = arrayOfNulls<ValueAnimator>(n)

        val deltaW = FloatArray(n)
        fun updateHeaderGroupGeometry() {
            for (i in 0 until n) deltaW[i] = 0f
            val expMax = dpF(10f)
            for (i in 0 until n) {
                val p = pressProg[i]
                if (p <= 0.001f) continue
                val e = expMax * p
                when {
                    i > 0 && i < n - 1 -> {
                        deltaW[i] += e
                        deltaW[i - 1] -= e * 0.5f
                        deltaW[i + 1] -= e * 0.5f
                    }
                    i == 0 && n > 1 -> {
                        deltaW[0] += e
                        deltaW[1] -= e
                    }
                    i == n - 1 && n > 1 -> {
                        deltaW[n - 1] += e
                        deltaW[n - 2] -= e
                    }
                }
            }
            for (i in 0 until n) {
                val pSelf = pressProg[i]
                val restL = if (i == 0) outerR else innerR
                val restR = if (i == n - 1) outerR else innerR
                val m = safeClamp(pSelf, 0f, 1f)
                val curL = restL + (outerR - restL) * m
                val curR = restR + (outerR - restR) * m

                bgs[i].cornerRadii = floatArrayOf(curL, curL, curR, curR, curR, curR, curL, curL)
                val isClose = specs[i].isDanger
                val restFill = if (isClose) blendColor(uiSurfaceContainer, COLOR_ROOT_CONTAINER, 0.32f) else uiSurfaceContainer
                val activeFill = if (isClose) COLOR_ROOT_CONTAINER else uiAccentContainer
                bgs[i].setColor(blendColor(restFill, activeFill, m))

                val restTint = if (isClose) COLOR_PASTEL_RED else COLOR_TEXT_BRIGHT
                val activeTint = if (isClose) COLOR_PASTEL_RED else uiOnAccentContainer
                icons[i].tintColor = blendColor(restTint, activeTint, m)
                icons[i].invalidate()

                val wPx = (baseW + deltaW[i]).roundToInt().coerceAtLeast(dp(24))
                val lp = frames[i].layoutParams as LinearLayout.LayoutParams
                if (lp.width != wPx) {
                    lp.width = wPx
                    frames[i].layoutParams = lp
                }
            }
        }

        headerActionsUpdateTheme = {
            groupLayout.background = tonalBg(uiSurfaceLow, dp(22))
            (tabStrip.parent as? View)?.background = tonalBg(uiSurfaceLow, dp(22))
            updateHeaderGroupGeometry()
        }

        for (i in 0 until n) {
            val sp = specs[i]
            val restL = if (i == 0) outerR else innerR
            val restR = if (i == n - 1) outerR else innerR
            val bg = makeConnectedSegmentDrawable(uiSurfaceContainer, restL, restR)
            bgs.add(bg)

            val icView = MaterialIconView(
                this@TermuxActivity,
                icon = sp.icon,
                tintColor = if (sp.isDanger) COLOR_PASTEL_RED else COLOR_TEXT_BRIGHT,
                iconSizeDp = 18.5f,
                strokeWidthDp = 2.0f
            ).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            }
            icons.add(icView)

            var downMs = 0L
            fun animatePress(holding: Boolean) {
                anims[i]?.cancel()
                val startP = pressProg[i]
                val targetP = if (holding) 1f else 0f
                val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = if (holding) 125L else 310L
                    interpolator = LinearInterpolator()
                    addUpdateListener { va ->
                        val t = (va.animatedValue as Float).coerceIn(0f, 1f)
                        val eased = if (holding) {
                            1f - (1f - t) * (1f - t)
                        } else {
                            1f - kotlin.math.exp(-3.5f * t) * kotlin.math.cos(5.8f * t)
                        }
                        pressProg[i] = (startP + (targetP - startP) * eased).coerceIn(0f, 1.25f)
                        updateHeaderGroupGeometry()
                    }
                }
                anims[i] = anim
                anim.start()
            }

            val releaseTask = Runnable { animatePress(false) }

            val frame = FrameLayout(this).apply {
                background = bg
                layoutParams = LinearLayout.LayoutParams(baseW.roundToInt(), btnH).apply {
                    setMargins(if (i > 0) dp(1) else 0, 0, if (i < n - 1) dp(1) else 0, 0)
                }
                addView(icView)
                setOnTouchListener { v, ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.removeCallbacks(releaseTask)
                            downMs = SystemClock.uptimeMillis()
                            triggerHaptic(0.85f)
                            animatePress(true)
                            false
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            val held = SystemClock.uptimeMillis() - downMs
                            if (held < 115L) {
                                v.postDelayed(releaseTask, (115L - held).coerceIn(40L, 110L))
                            } else {
                                releaseTask.run()
                            }
                            false
                        }
                        else -> false
                    }
                }
                setOnClickListener {
                    sp.onClick()
                }
                if (sp.onLongClick != null) {
                    setOnLongClickListener {
                        triggerHaptic(1.3f)
                        sp.onLongClick.invoke()
                        true
                    }
                }
            }
            frames.add(frame)
            groupLayout.addView(frame)
        }
        updateHeaderGroupGeometry()
        return groupLayout
    }

    // ========================================================================
    // SSH Server Persistence & C++23 Bridge
    // ========================================================================
    private fun loadSshServers(): MutableList<SshServerProfile> {
        val rawV3 = prefs.getString("ssh_servers_v3", null)
        val isFromV3 = rawV3 != null
        val raw = rawV3 ?: prefs.getString(KEY_SSH_SERVERS_V2, null) ?: (prefs.getString(KEY_SSH_SERVERS_V1, "") ?: "")
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
                        useCompression = if (isFromV3) (parts.getOrNull(8) == "1") else false,
                        forwardAgent = (parts.getOrNull(9) ?: "0") == "1",
                        localForwards = parts.getOrNull(10) ?: "",
                        remoteForwards = parts.getOrNull(11) ?: "",
                        dynamicSocksPort = parts.getOrNull(12) ?: "",
                        jumpHost = parts.getOrNull(13) ?: "",
                        keepAliveSec = if (isFromV3) (parts.getOrNull(14)?.toIntOrNull() ?: 0) else 0,
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
        prefs.edit().putString("ssh_servers_v3", serialized).apply()
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
    private val sftpRemoteDirCache = java.util.concurrent.ConcurrentHashMap<String, List<SftpItemEntry>>()

    private fun showWorkspaceHubDialog(initialTab: Int = 0) {
        val dlg = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(0x00000000))
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

        // Layer 1: Main Hub view (Top Nav + Tab Content)
        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(mainCol)

        // Layer 2: Full-screen Sub-Page Layer (for Material 3 SSH Server Settings Page & Code Editor)
        val subPageLayer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(uiSurface)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(subPageLayer)

        var inHubSubPage = false
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
        var newlyExpandedTreePath: String? = null
        val selectedSftpPaths = LinkedHashSet<String>()
        var lastRenderedEntries: List<SftpItemEntry> = emptyList()
        val batchClipSources = ArrayList<String>()
        var batchClipIsMove = false
        var sftpSearchActive = false
        var sftpSearchQuery = ""
        var prevSftpSourceIdx = 0
        var sftpTransitionDir = 0 // -1 = backward/left, 0 = none, +1 = forward/right

        fun openHubSubPage(populateSubPage: (LinearLayout, () -> Unit) -> Unit) {
            inHubSubPage = true
            subPageLayer.removeAllViews()
            val closeSubPage: () -> Unit = {
                if (inHubSubPage) {
                    inHubSubPage = false
                    ensureCenteredPivot(mainCol)
                    ensureCenteredPivot(subPageLayer)
                    mainCol.visibility = View.VISIBLE
                    mainCol.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(210L)
                        .setInterpolator(predictiveCommitInterpolator)
                        .start()
                    subPageLayer.animate()
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .alpha(0f)
                        .setDuration(200L)
                        .setInterpolator(predictiveCommitInterpolator)
                        .withEndAction {
                            if (!inHubSubPage) {
                                subPageLayer.visibility = View.GONE
                                subPageLayer.removeAllViews()
                                subPageLayer.translationX = 0f
                                subPageLayer.translationY = 0f
                                subPageLayer.scaleX = 1f
                                subPageLayer.scaleY = 1f
                                subPageLayer.alpha = 1f
                                applyRoundedCornersProgress(subPageLayer, 0f)
                            }
                        }
                        .start()
                }
            }
            populateSubPage(subPageLayer, closeSubPage)
            subPageLayer.visibility = View.VISIBLE
            ensureCenteredPivot(subPageLayer)
            ensureCenteredPivot(mainCol)
            applyRoundedCornersProgress(subPageLayer, 0f)
            subPageLayer.translationX = 0f
            subPageLayer.translationY = 0f
            subPageLayer.alpha = 0f
            subPageLayer.scaleX = 0.92f
            subPageLayer.scaleY = 0.92f
            subPageLayer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(230L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
            mainCol.animate()
                .translationX(0f)
                .scaleX(0.94f)
                .scaleY(0.94f)
                .alpha(0.50f)
                .setDuration(230L)
                .setInterpolator(aosp17SpatialInterpolator)
                .start()
        }

        val topNavRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(uiSurface)
            setPadding(dp(12), dp(2), dp(12), dp(6))
        }

        val sessionsOuterPillView = tabStrip.parent as? View
        val startSessionsPillWidth = (sessionsOuterPillView?.width ?: 0).takeIf { it > 0 } ?: dp(68)

        val termBackIcon = MaterialIconView(this@TermuxActivity, M3Icon.TERMINAL, uiOnAccentContainer, 18.5f, 2.0f).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                marginEnd = dp(6)
            }
        }
        val termBackLabel = TextView(this@TermuxActivity).apply {
            text = tr("Терминал", "Terminal")
            setSingleLine(true)
            setTextColor(uiOnAccentContainer)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        val termBackChip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = tonalBg(uiAccentContainer, dp(24))
            setPadding(dp(14), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(38)
            ).apply {
                marginEnd = dp(8)
            }
            addView(termBackIcon)
            addView(termBackLabel)
            attachSmoothPressAnimation(this, 0.93f)
            setOnClickListener {
                triggerHaptic(0.85f)
                // Smoothly morph "Терминал" card back into the Sessions pill width as Hub closes!
                val curChipW = width.toFloat().coerceAtLeast(dpF(80f))
                val targetSessW = startSessionsPillWidth.toFloat().coerceAtLeast(dpF(58f))
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 190L
                    interpolator = predictiveCommitInterpolator
                    addUpdateListener { va ->
                        val t = safeClamp(va.animatedValue as Float, 0f, 1f)
                        val lp = layoutParams as LinearLayout.LayoutParams
                        lp.width = (curChipW + (targetSessW - curChipW) * t).roundToInt()
                        layoutParams = lp
                        termBackLabel.alpha = (1f - t * 1.4f).coerceIn(0f, 1f)
                    }
                    start()
                }
                ensureCenteredPivot(root)
                root.animate()
                    .scaleX(0.90f)
                    .scaleY(0.90f)
                    .alpha(0f)
                    .setDuration(195L)
                    .setInterpolator(predictiveCommitInterpolator)
                    .withEndAction {
                        dlg.dismiss()
                        sessionsOuterPillView?.let { pill ->
                            pill.scaleX = 0.92f
                            pill.scaleY = 0.88f
                            pill.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(260L)
                                .setInterpolator(OvershootInterpolator(1.8f))
                                .start()
                        }
                    }
                    .start()
            }
        }
        // Morph from the top-left Sessions pill (whether 1 session or multiple sessions!) into the rounded "Терминал" capsule
        termBackChip.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (termBackChip.viewTreeObserver.isAlive) {
                    termBackChip.viewTreeObserver.removeOnPreDrawListener(this)
                }
                val finalChipW = termBackChip.width.takeIf { it > 0 } ?: dp(116)
                val fromW = startSessionsPillWidth.toFloat()
                val toW = finalChipW.toFloat()
                termBackLabel.alpha = 0f
                termBackIcon.scaleX = 0.75f
                termBackIcon.scaleY = 0.75f
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 340L
                    interpolator = LinearInterpolator()
                    addUpdateListener { va ->
                        val t = safeClamp(va.animatedValue as Float, 0f, 1f)
                        val spring = 1f - kotlin.math.exp(-3.6f * t) * kotlin.math.cos(5.6f * t)
                        val lp = termBackChip.layoutParams as LinearLayout.LayoutParams
                        lp.width = (fromW + (toW - fromW) * spring).roundToInt().coerceAtLeast(dp(48))
                        termBackChip.layoutParams = lp
                        val fadeT = ((t - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        termBackLabel.alpha = fadeT
                        termBackIcon.scaleX = (0.75f + 0.25f * spring).coerceIn(0.7f, 1.1f)
                        termBackIcon.scaleY = (0.75f + 0.25f * spring).coerceIn(0.7f, 1.1f)
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            val lp = termBackChip.layoutParams as LinearLayout.LayoutParams
                            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                            termBackChip.layoutParams = lp
                            termBackLabel.alpha = 1f
                            termBackIcon.scaleX = 1f
                            termBackIcon.scaleY = 1f
                        }
                    })
                    start()
                }
                return true
            }
        })
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

        val (tabServersCol, indServers) = makeTopNavTab(tr("Серверы", "Servers")) {
            previousTab = activeTab
            activeTab = 0
            renderActiveTab()
        }
        val (tabSftpCol, indSftp) = makeTopNavTab("SFTP") {
            previousTab = activeTab
            activeTab = 1
            renderActiveTab()
        }
        val (tabPkgsCol, indPkgs) = makeTopNavTab(tr("Пакеты", "Packages")) {
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

        // Official Android 14–17 Material 3 Predictive Back on Workspace Hub & Sub-Pages
        val hubPredictiveBack = object : OnBackAnimationCallback {
            override fun onBackStarted(backEvent: BackEvent) {
                root.animate().cancel()
                mainCol.animate().cancel()
                subPageLayer.animate().cancel()
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                if (inHubSubPage) {
                    applyPredictiveBackPreview(subPageLayer, mainCol, backEvent)
                } else {
                    applyPredictiveBackPreview(root, null, backEvent)
                }
            }

            override fun onBackCancelled() {
                if (inHubSubPage) {
                    cancelPredictiveBackPreview(subPageLayer, mainCol)
                } else {
                    cancelPredictiveBackPreview(root, null)
                }
            }

            override fun onBackInvoked() {
                triggerHaptic(0.75f)
                if (inHubSubPage) {
                    inHubSubPage = false
                    ensureCenteredPivot(mainCol)
                    ensureCenteredPivot(subPageLayer)
                    mainCol.visibility = View.VISIBLE
                    mainCol.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(210L)
                        .setInterpolator(predictiveCommitInterpolator)
                        .start()
                    subPageLayer.animate()
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .alpha(0f)
                        .setDuration(200L)
                        .setInterpolator(predictiveCommitInterpolator)
                        .withEndAction {
                            subPageLayer.visibility = View.GONE
                            subPageLayer.removeAllViews()
                            subPageLayer.translationX = 0f
                            subPageLayer.translationY = 0f
                            subPageLayer.scaleX = 1f
                            subPageLayer.scaleY = 1f
                            subPageLayer.alpha = 1f
                            applyRoundedCornersProgress(subPageLayer, 0f)
                        }
                        .start()
                } else if (activeTab == 1 && sftpSearchActive) {
                    sftpSearchActive = false
                    sftpSearchQuery = ""
                    cancelPredictiveBackPreview(root, null)
                    renderActiveTab()
                } else {
                    ensureCenteredPivot(root)
                    root.animate()
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .alpha(0f)
                        .setDuration(190L)
                        .setInterpolator(predictiveCommitInterpolator)
                        .withEndAction {
                            dlg.dismiss()
                        }
                        .start()
                }
            }
        }
        dlg.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            hubPredictiveBack
        )

        // Full-screen Sub-Page: Code Viewer & Editor
        fun openCodeViewerAndEditorSubPage(
            fileName: String,
            fullPath: String,
            initialContent: String,
            onSaveContent: (String) -> Unit
        ) {
            openHubSubPage { pageCol, closeSubPage ->
                var viewerFontScale = 1.0f

                val topBar = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(8), dp(16), dp(10))
                }
                topBar.addView(
                    FrameLayout(this).apply {
                        background = tonalBg(uiSurfaceContainer, dp(20))
                        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                            marginEnd = dp(12)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.ARROW_BACK, COLOR_TEXT_BRIGHT, 21f, 2.15f))
                        attachSmoothPressAnimation(this, 0.90f)
                        setOnClickListener {
                            triggerHaptic(0.8f)
                            closeSubPage()
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
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                })
                titleCol.addView(TextView(this).apply {
                    text = fullPath
                    setSingleLine(true)
                    setTextColor(COLOR_TEXT_DIM)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                })
                topBar.addView(titleCol)

                val editorEt = EditText(this).apply {
                    setText(initialContent)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    gravity = Gravity.TOP or Gravity.START
                    background = tonalBg(uiSurfaceLow, dp(20))
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                }

                val zoomLabel = TextView(this).apply {
                    text = "100%"
                    setTextColor(uiAccent)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setPadding(dp(6), 0, dp(6), 0)
                }
                topBar.addView(makeExpressiveChip("A-", false) {
                    viewerFontScale = (viewerFontScale - 0.1f).coerceIn(0.6f, 2.2f)
                    editorEt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * viewerFontScale)
                    zoomLabel.text = "${(viewerFontScale * 100).roundToInt()}%"
                })
                topBar.addView(zoomLabel)
                topBar.addView(makeExpressiveChip("A+", false) {
                    viewerFontScale = (viewerFontScale + 0.1f).coerceIn(0.6f, 2.2f)
                    editorEt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * viewerFontScale)
                    zoomLabel.text = "${(viewerFontScale * 100).roundToInt()}%"
                })
                topBar.addView(makeExpressiveChip(tr("Сохранить", "Save"), true) {
                    onSaveContent(editorEt.text.toString())
                    closeSubPage()
                })
                pageCol.addView(topBar)

                val vScroll = ScrollView(this).apply {
                    isFillViewport = true
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    ).apply {
                        setMargins(dp(14), dp(4), dp(14), dp(12))
                    }
                }
                vScroll.addView(editorEt)
                pageCol.addView(vScroll)
            }
        }

        // ====================================================================
        // Full-Screen Material Design 3 SSH Server Configuration Page
        // (Replaces floating dialog; default command is strictly `ssh root@ip`!)
        // ====================================================================
        fun openSshServerSubPage(existing: SshServerProfile?, onSaved: () -> Unit) {
            openHubSubPage { pageCol, closeSubPage ->
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
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        closeSubPage()
                    }
                }
                val titleTv = TextView(this).apply {
                    text = if (existing == null) tr("Новый SSH-сервер", "New SSH server") else tr("Настройка SSH-сервера", "SSH server settings")
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                toolbar.addView(backBtn)
                toolbar.addView(titleTv)

                lateinit var saveProfileAction: () -> Unit
                val saveTopBtn = makeExpressiveChip(tr("Сохранить", "Save"), true) {
                    saveProfileAction()
                }
                toolbar.addView(saveTopBtn)
                pageCol.addView(toolbar)

                val scroll = ScrollView(this).apply {
                    isVerticalScrollBarEnabled = false
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                }
                val form = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(4), dp(16), dp(40))
                }
                scroll.addView(form)
                pageCol.addView(scroll)

                lateinit var updateLivePreview: () -> Unit

                // Live Command Preview Banner (Material 3 Accent Tonal Card)
                val previewCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    background = tonalBg(uiAccentContainer, dp(22))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                }
                previewCard.addView(TextView(this).apply {
                    text = tr("Итоговая команда подключения", "Generated SSH command")
                    setTextColor(uiOnAccentContainer)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                })
                val previewTv = TextView(this).apply {
                    setTextColor(uiAccent)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, 0)
                    setTextIsSelectable(true)
                }
                previewCard.addView(previewTv)
                form.addView(previewCard)

                // Helper to create a Material 3 Segmented Input Card (like Settings cards)
                fun makeM3InputCard(
                    icon: M3Icon,
                    title: String,
                    initial: String,
                    hintStr: String,
                    index: Int,
                    total: Int,
                    isNumeric: Boolean = false,
                    isPassword: Boolean = false
                ): Pair<LinearLayout, EditText> {
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                        background = segmentedCardBg(index, total, uiSurfaceLow, 0, 20f, 5f)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = if (index < total - 1) dp(2) else dp(8)
                        }
                    }
                    card.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(uiAccentContainer, dp(12))
                            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                                marginEnd = dp(14)
                            }
                            addView(MaterialIconView(this@TermuxActivity, icon, uiAccent, 20f, 2.0f))
                        }
                    )
                    val col = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    col.addView(TextView(this).apply {
                        text = title
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    })
                    val et = EditText(this).apply {
                        setText(initial)
                        hint = hintStr
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                        setSingleLine(true)
                        background = null
                        setPadding(0, dp(3), 0, 0)
                        if (isNumeric) {
                            inputType = InputType.TYPE_CLASS_NUMBER
                        } else if (isPassword) {
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                updateLivePreview()
                            }
                        })
                    }
                    col.addView(et)
                    card.addView(col)
                    return card to et
                }

                // Section 1: Quick Command Import
                form.addView(makeSectionLabel(tr("Быстрый импорт из команды", "Quick import from command")))
                val importCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(12), dp(10))
                    background = tonalBg(uiSurfaceLow, dp(20))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                }
                val rawCmdEt = EditText(this).apply {
                    hint = "ssh -p 55222 -L 54321:127.0.0.1:54321 root@1.2.3.4"
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setSingleLine(true)
                    background = null
                    setPadding(dp(4), dp(6), dp(8), dp(6))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                importCard.addView(rawCmdEt)

                // Section 2: Main Connection Parameters
                form.addView(makeSectionLabel(tr("Подключение и авторизация", "Connection & authentication")))
                val (labelCard, labelEt) = makeM3InputCard(M3Icon.SERVER, tr("Название профиля", "Profile name"), existing?.label ?: "", tr("Мой сервер", "My Server"), 0, 6)
                val (hostCard, hostEt) = makeM3InputCard(M3Icon.PING, tr("Адрес сервера (IP или домен)", "Host (IP or domain)"), existing?.host ?: "", "192.168.1.10", 1, 6)
                val (userCard, userEt) = makeM3InputCard(M3Icon.TERMINAL, tr("Пользователь", "Username"), existing?.username ?: "root", "root", 2, 6)
                val (portCard, portEt) = makeM3InputCard(M3Icon.SLIDERS, tr("Порт SSH (-p добавляется только если != 22)", "SSH Port (-p added only if != 22)"), (existing?.port ?: 22).toString(), "22", 3, 6, isNumeric = true)

                var selectedAuth = existing?.authType ?: 0
                val defaultKeyPath = "/data/data/com.termuxx/file/home/.ssh/id_ed25519"
                val authOptions = listOf(
                    tr("Пароль или без флагов (по умолчанию)", "Password or default (no extra flags)"),
                    tr("Приватный SSH-ключ (-i)", "Private SSH key (-i)"),
                    tr("Проброс SSH-агента (-A)", "SSH Agent forwarding (-A)")
                )
                val authCardHolder = FrameLayout(this)
                val (secretCard, secretEt) = makeM3InputCard(
                    M3Icon.KEY,
                    if (selectedAuth == 1) tr("Путь к приватному ключу (-i)", "Private key path (-i)") else tr("Пароль (для авто-ввода)", "Password (for auto-login)"),
                    existing?.secretOrKeyPath ?: "",
                    if (selectedAuth == 1) defaultKeyPath else tr("Необязательно", "Optional"),
                    5,
                    6
                )

                lateinit var rebuildAuthCard: () -> Unit
                rebuildAuthCard = {
                    authCardHolder.removeAllViews()
                    authCardHolder.addView(
                        makeBakaSuChooseCard(
                            icon = M3Icon.KEY,
                            title = tr("Способ авторизации", "Authentication method"),
                            subtitle = null,
                            items = authOptions,
                            selectedIndex = selectedAuth,
                            index = 4,
                            total = 6
                        ) { chosen ->
                            selectedAuth = chosen
                            if (chosen == 1 && secretEt.text.isNullOrBlank()) {
                                secretEt.setText(defaultKeyPath)
                            }
                            rebuildAuthCard()
                            updateLivePreview()
                        }
                    )
                }
                rebuildAuthCard()

                form.addView(labelCard)
                form.addView(hostCard)
                form.addView(userCard)
                form.addView(portCard)
                form.addView(authCardHolder)
                form.addView(secretCard)

                // Section 3: Port Forwarding & Tunneling (-L, -R, -D, -J)
                form.addView(makeSectionLabel(tr("Проброс портов и туннелирование", "Port forwarding & tunneling")))

                // Quick -L rule builder card
                val lBuilderCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = segmentedCardBg(0, 5, uiSurfaceLow, 0, 20f, 5f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(2)
                    }
                }
                fun makeMiniPortField(hintTxt: String, weightVal: Float): EditText {
                    return EditText(this).apply {
                        hint = hintTxt
                        setHintTextColor(COLOR_TEXT_DIM)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setSingleLine(true)
                        background = tonalBg(uiSurfaceContainer, dp(10))
                        setPadding(dp(10), dp(7), dp(10), dp(7))
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weightVal).apply {
                            marginEnd = dp(6)
                        }
                    }
                }
                val lLocPortEt = makeMiniPortField(tr("Лок. порт", "Local"), 1f)
                val lRemHostEt = makeMiniPortField("127.0.0.1", 1.25f).apply { setText("127.0.0.1") }
                val lRemPortEt = makeMiniPortField(tr("Удал. порт", "Remote"), 1f)
                val (localFwdsCard, localFwdsEt) = makeM3InputCard(M3Icon.SLIDERS, tr("Локальный проброс портов (-L)", "Local port forwarding (-L)"), existing?.localForwards ?: "", "54321:127.0.0.1:54321", 1, 5)
                lBuilderCard.addView(lLocPortEt)
                lBuilderCard.addView(lRemHostEt)
                lBuilderCard.addView(lRemPortEt)
                lBuilderCard.addView(makeExpressiveChip("+ -L", true) {
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

                val (remoteFwdsCard, remoteFwdsEt) = makeM3InputCard(M3Icon.SLIDERS, tr("Удалённый проброс портов (-R)", "Remote port forwarding (-R)"), existing?.remoteForwards ?: "", "9000:127.0.0.1:9000", 2, 5)
                val (dynamicPortCard, dynamicPortEt) = makeM3InputCard(M3Icon.PING, tr("Динамический SOCKS5 прокси (-D)", "Dynamic SOCKS5 proxy (-D)"), existing?.dynamicSocksPort ?: "", "1080", 3, 5, isNumeric = true)
                val (jumpHostCard, jumpHostEt) = makeM3InputCard(M3Icon.SERVER, tr("Промежуточный сервер ProxyJump (-J)", "ProxyJump bastion (-J)"), existing?.jumpHost ?: "", "user@bastion:22", 4, 5)

                form.addView(lBuilderCard)
                form.addView(localFwdsCard)
                form.addView(remoteFwdsCard)
                form.addView(dynamicPortCard)
                form.addView(jumpHostCard)

                // Section 4: Optional OpenSSH Flags (ALL OFF BY DEFAULT so base command is strictly `ssh root@ip`!)
                form.addView(makeSectionLabel(tr("Опции протокола SSH (по умолчанию выключены)", "SSH protocol options (off by default)")))

                var keepAliveSec = existing?.keepAliveSec ?: 0
                var useComp = existing?.useCompression ?: false
                var fwdAgent = existing?.forwardAgent ?: false
                var tunnelOnlyN = existing?.tunnelOnlyN ?: false
                var ipv4Only = existing?.ipv4Only ?: false

                val keepAliveValues = intArrayOf(0, 15, 30, 60, 120)
                val keepAliveLabels = listOf(
                    tr("Выключено (не добавлять в команду)", "Off (do not add to command)"),
                    "15 сек (ServerAliveInterval=15)",
                    "30 сек (ServerAliveInterval=30)",
                    "60 сек (ServerAliveInterval=60)",
                    "120 сек (ServerAliveInterval=120)"
                )
                val optionsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                form.addView(optionsContainer)

                lateinit var rebuildSshOptionCards: () -> Unit
                rebuildSshOptionCards = {
                    optionsContainer.removeAllViews()
                    val kaIdx = keepAliveValues.indexOfFirst { it == keepAliveSec }.let { if (it >= 0) it else 0 }
                    optionsContainer.addView(
                        makeBakaSuChooseCard(
                            icon = M3Icon.REFRESH,
                            title = tr("Удержание соединения (ServerAliveInterval)", "Keep-alive (ServerAliveInterval)"),
                            subtitle = null,
                            items = keepAliveLabels,
                            selectedIndex = kaIdx,
                            index = 0,
                            total = 5
                        ) { chosen ->
                            keepAliveSec = keepAliveValues[chosen]
                            rebuildSshOptionCards()
                            updateLivePreview()
                        }
                    )
                    optionsContainer.addView(
                        makeBakaSuSwitchCard(
                            icon = M3Icon.SLIDERS,
                            title = tr("Сжатие трафика (-C)", "Traffic compression (-C)"),
                            subtitle = tr("Добавляет флаг -C в команду ssh", "Adds -C flag to ssh command"),
                            checked = useComp,
                            index = 1,
                            total = 5
                        ) { isChecked ->
                            useComp = isChecked
                            updateLivePreview()
                        }
                    )
                    optionsContainer.addView(
                        makeBakaSuSwitchCard(
                            icon = M3Icon.KEY,
                            title = tr("Проброс SSH-агента (-A)", "Forward SSH agent (-A)"),
                            subtitle = tr("Добавляет флаг -A в команду ssh", "Adds -A flag to ssh command"),
                            checked = fwdAgent,
                            index = 2,
                            total = 5
                        ) { isChecked ->
                            fwdAgent = isChecked
                            updateLivePreview()
                        }
                    )
                    optionsContainer.addView(
                        makeBakaSuSwitchCard(
                            icon = M3Icon.TERMINAL,
                            title = tr("Только туннель портов без оболочки (-N)", "Port tunnel only without shell (-N)"),
                            subtitle = tr("Добавляет флаг -N для фонового проброса портов", "Adds -N flag for port forwarding only"),
                            checked = tunnelOnlyN,
                            index = 3,
                            total = 5
                        ) { isChecked ->
                            tunnelOnlyN = isChecked
                            updateLivePreview()
                        }
                    )
                    optionsContainer.addView(
                        makeBakaSuSwitchCard(
                            icon = M3Icon.PING,
                            title = tr("Только IPv4 (-4)", "Force IPv4 only (-4)"),
                            subtitle = tr("Добавляет флаг -4 для подключения строго по IPv4", "Adds -4 flag to force IPv4"),
                            checked = ipv4Only,
                            index = 4,
                            total = 5
                        ) { isChecked ->
                            ipv4Only = isChecked
                            updateLivePreview()
                        }
                    )
                }
                rebuildSshOptionCards()

                // Section 5: Extra Custom Flags & Startup Command
                form.addView(makeSectionLabel(tr("Дополнительно", "Advanced")))
                val (customFlagsCard, customFlagsEt) = makeM3InputCard(M3Icon.EDIT, tr("Произвольные аргументы SSH", "Custom SSH flags"), existing?.customFlags ?: "", tr("Напр. -X или -o StrictHostKeyChecking=no", "e.g. -X or -o StrictHostKeyChecking=no"), 0, 2)
                val (startupCard, startupEt) = makeM3InputCard(M3Icon.TERMINAL, tr("Команда после входа на сервер", "Remote startup command"), existing?.startupCommand ?: "", "tmux attach || tmux", 1, 2)
                form.addView(customFlagsCard)
                form.addView(startupCard)

                importCard.addView(makeExpressiveChip(tr("Файл / Ключ", "File / Key"), false) {
                    launchSystemFilePickerForImport { uri ->
                        Thread {
                            try {
                                val fileName = queryUriDisplayName(uri)
                                val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                                if (fileName.endsWith(".xml", ignoreCase = true) || text.contains("<FileZilla", ignoreCase = true) || text.contains("<Server>", ignoreCase = true)) {
                                    val parsedLines = nativeStageKeyOrParseFileZillaXml(text, true).lines().filter { it.isNotBlank() }
                                    val first = parsedLines.firstOrNull()?.split("\t")
                                    if (first != null && first.size >= 6) {
                                        runOnUiThread {
                                            labelEt.setText(first[0])
                                            hostEt.setText(first[1])
                                            portEt.setText(first[2])
                                            userEt.setText(first[3])
                                            selectedAuth = first[4].toIntOrNull() ?: 0
                                            secretEt.setText(first[5])
                                            rebuildAuthCard()
                                            updateLivePreview()
                                            Toast.makeText(this@TermuxActivity, tr("Импортирован профиль из $fileName", "Imported profile from $fileName"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    val stagedPath = nativeStageKeyOrParseFileZillaXml(text, false)
                                    if (stagedPath.isNotEmpty()) {
                                        runOnUiThread {
                                            selectedAuth = 1
                                            secretEt.setText(stagedPath)
                                            rebuildAuthCard()
                                            updateLivePreview()
                                            Toast.makeText(this@TermuxActivity, tr("Ключ сохранён (chmod 600): $stagedPath", "Key staged (chmod 600): $stagedPath"), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this@TermuxActivity, tr("Не удалось распознать приватный ключ", "Unrecognized private key format"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Throwable) {
                                runOnUiThread {
                                    Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.start()
                    }
                })
                importCard.addView(makeExpressiveChip(tr("Разобрать", "Parse"), true) {
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
                                rebuildAuthCard()
                            }
                            tunnelOnlyN = parsed[8] == "1"
                            useComp = parsed[9] == "1"
                            fwdAgent = parsed[10] == "1"
                            ipv4Only = parsed[11] == "1"
                            if (parsed[12].isNotEmpty()) customFlagsEt.setText(parsed[12])
                            rebuildSshOptionCards()
                            updateLivePreview()
                        }
                    }
                })
                form.addView(importCard, 2)

                fun buildDraftProfile(): SshServerProfile {
                    val hostVal = hostEt.text.toString().trim()
                    val userVal = userEt.text.toString().trim().ifEmpty { "root" }
                    val portVal = portEt.text.toString().trim().toIntOrNull()?.coerceIn(1, 65535) ?: 22
                    val labelVal = labelEt.text.toString().trim().ifEmpty {
                        if (hostVal.isNotEmpty()) "$userVal@$hostVal" else tr("SSH-сервер", "SSH Server")
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
                        keepAliveSec = keepAliveSec,
                        tunnelOnlyN = tunnelOnlyN,
                        ipv4Only = ipv4Only,
                        customFlags = customFlagsEt.text.toString().trim()
                    )
                }

                updateLivePreview = {
                    val draft = buildDraftProfile()
                    val shownHost = draft.host.ifEmpty { "ip" }
                    previewTv.text = buildSshCommandViaCpp23(draft.copy(host = shownHost), withAskPass = false)
                }
                updateLivePreview()

                saveProfileAction = {
                    val profile = buildDraftProfile()
                    if (profile.host.isEmpty()) {
                        Toast.makeText(this, tr("Укажите адрес сервера (Host / IP)", "Specify server Host / IP"), Toast.LENGTH_SHORT).show()
                    } else {
                        val list = loadSshServers()
                        val idx = list.indexOfFirst { it.id == profile.id }
                        if (idx >= 0) list[idx] = profile else list.add(profile)
                        saveSshServers(list)
                        closeSubPage()
                        onSaved()
                    }
                }
            }
        }

        fun pingServerAsync(srv: SshServerProfile, badgeTv: TextView?) {
            badgeTv?.text = tr("Пинг…", "Ping…")
            badgeTv?.setTextColor(COLOR_TEXT_DIM)
            Thread {
                val rtt = nativePingHostMs(srv.host, srv.port, 2200)
                serverPingCacheMs[srv.id] = rtt
                runOnUiThread {
                    if (badgeTv != null) {
                        if (rtt >= 0) {
                            badgeTv.text = "$rtt ${tr("мс", "ms")}"
                            val col = when {
                                rtt < 85 -> COLOR_PING_GOOD
                                rtt < 200 -> COLOR_PING_WARN
                                else -> COLOR_ROOT
                            }
                            badgeTv.setTextColor(col)
                        } else {
                            badgeTv.text = tr("Не в сети", "Offline")
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

            val ptrLayout = BakaSuPullToRefreshLayout(this, scroll) { finishPtr ->
                if (servers.isEmpty()) {
                    finishPtr()
                } else {
                    var remaining = servers.size
                    for (s in servers) {
                        val badgeTv = pingBadgeViews[s.id]
                        badgeTv?.text = tr("Пинг…", "Ping…")
                        badgeTv?.setTextColor(COLOR_TEXT_DIM)
                        Thread {
                            val rtt = nativePingHostMs(s.host, s.port, 2200)
                            serverPingCacheMs[s.id] = rtt
                            runOnUiThread {
                                if (badgeTv != null) {
                                    if (rtt >= 0) {
                                        badgeTv.text = "$rtt ${tr("мс", "ms")}"
                                        val col = when {
                                            rtt < 85 -> COLOR_PING_GOOD
                                            rtt < 200 -> COLOR_PING_WARN
                                            else -> COLOR_ROOT
                                        }
                                        badgeTv.setTextColor(col)
                                    } else {
                                        badgeTv.text = tr("Не в сети", "Offline")
                                        badgeTv.setTextColor(COLOR_ROOT)
                                    }
                                }
                                remaining--
                                if (remaining <= 0) {
                                    finishPtr()
                                }
                            }
                        }.start()
                    }
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
                text = tr("SSH-серверы", "SSH Servers")
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
            titleRow.addView(makeExpressiveChip(tr("SSH-ключи", "SSH Keys"), false) {
                val sshDir = File("/data/data/com.termuxx/file/home/.ssh").apply { mkdirs() }
                val pubFile = File(sshDir, "id_ed25519.pub")
                val privFile = File(sshDir, "id_ed25519")
                val (kDlg, kSheet) = createExpressiveDialog(420)
                kSheet.addView(TextView(this).apply {
                    text = tr("SSH-ключи (ed25519)", "SSH Keys (ed25519)")
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
                        "Нажмите «+» внизу справа, чтобы открыть страницу настройки нового SSH-сервера.",
                        "Tap '+' at the bottom right to open the new SSH server configuration page."
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
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(14), dp(14), dp(14))
                        background = segmentedCardBg(idx, servers.size, uiSurfaceLow, 0, 24f, 5f)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = if (idx < servers.size - 1) dp(3) else dp(12)
                        }
                        attachSmoothPressAnimation(this, 0.985f)
                        setOnClickListener {
                            triggerHaptic(0.9f)
                            connectToSshServerInTerminal(srv, dlg)
                        }
                    }

                    // Top Row: Server Icon + Full Unclipped Label & Host:Port + More Menu
                    val topRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    topRow.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(uiAccentContainer, dp(14))
                            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                                marginEnd = dp(12)
                            }
                            addView(MaterialIconView(this@TermuxActivity, M3Icon.SERVER, uiAccent, 21f, 2.0f))
                        }
                    )

                    val infoCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginEnd = dp(8)
                        }
                    }
                    infoCol.addView(TextView(this).apply {
                        text = srv.label
                        setSingleLine(true)
                        ellipsize = TextUtils.TruncateAt.END
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    })
                    infoCol.addView(TextView(this).apply {
                        text = "${srv.username}@${srv.host}" + (if (srv.port != 22) ":${srv.port}" else "")
                        setSingleLine(true)
                        ellipsize = TextUtils.TruncateAt.MIDDLE
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.8f)
                        setPadding(0, dp(2), 0, 0)
                    })
                    topRow.addView(infoCol)

                    val cachedPing = serverPingCacheMs[srv.id]
                    val pingTv = TextView(this).apply {
                        setSingleLine(true)
                        maxLines = 1
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        if (cachedPing == null) {
                            text = tr("Пинг", "Ping")
                            setTextColor(COLOR_TEXT_BRIGHT)
                        } else if (cachedPing >= 0) {
                            text = "$cachedPing ${tr("мс", "ms")}"
                            setTextColor(if (cachedPing < 85) COLOR_PING_GOOD else if (cachedPing < 200) COLOR_PING_WARN else COLOR_ROOT)
                        } else {
                            text = tr("Оффлайн", "Offline")
                            setTextColor(COLOR_ROOT)
                        }
                    }
                    pingBadgeViews[srv.id] = pingTv

                    topRow.addView(
                        FrameLayout(this).apply {
                            background = tonalBg(uiSurfaceContainer, dp(12))
                            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                            addView(MaterialIconView(this@TermuxActivity, M3Icon.MORE_VERT, COLOR_TEXT_BRIGHT, 19.5f))
                            attachSmoothPressAnimation(this, 0.90f)
                            setOnClickListener {
                                triggerHaptic(0.85f)
                                val (mDlg, mSheet) = createExpressiveDialog(390)
                                mSheet.addView(TextView(this@TermuxActivity).apply {
                                    text = srv.label
                                    setTextColor(COLOR_TEXT_BRIGHT)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                                    typeface = Typeface.DEFAULT_BOLD
                                    setPadding(dp(4), 0, dp(4), dp(2))
                                })
                                mSheet.addView(TextView(this@TermuxActivity).apply {
                                    text = buildSshCommandViaCpp23(srv, withAskPass = false)
                                    setTextColor(uiAccent)
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
                                    Triple(M3Icon.EDIT, tr("Настроить сервер и порты", "Configure server & ports")) {
                                        openSshServerSubPage(srv) { renderServersTab() }
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
                                        val copyCmd = "mkdir -p ~/.ssh && ([ -f ~/.ssh/id_ed25519.pub ] || ssh-keygen -t ed25519 -N \"\" -f ~/.ssh/id_ed25519) && ssh-copy-id -p ${srv.port} ${srv.username}@${srv.host}\n"
                                        terminalSurface.postDelayed({ nativeSendText(copyCmd, false) }, 120L)
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
                    card.addView(topRow)

                    // Bottom Row: Auth & Port-Forward Badges on Left + Connected Ping & SFTP Buttons on Right
                    val bottomRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(10), 0, 0)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val badgeScroll = HorizontalScrollView(this).apply {
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = View.OVER_SCROLL_NEVER
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginEnd = dp(8)
                        }
                    }
                    val badgeRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    fun makeMiniBadge(txt: String) = TextView(this).apply {
                        text = txt
                        setSingleLine(true)
                        maxLines = 1
                        setTextColor(uiAccent)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                        background = tonalBg(uiAccentContainer, dp(8))
                        setPadding(dp(9), dp(4), dp(9), dp(4))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = dp(5)
                        }
                    }
                    badgeRow.addView(makeMiniBadge(when (srv.authType) {
                        0 -> tr("Пароль", "Pass")
                        1 -> tr("Ключ", "Key")
                        else -> "Agent"
                    }))
                    if (srv.localForwards.isNotBlank()) {
                        badgeRow.addView(makeMiniBadge("-L ${srv.localForwards}"))
                    }
                    if (srv.remoteForwards.isNotBlank()) {
                        badgeRow.addView(makeMiniBadge("-R ${srv.remoteForwards}"))
                    }
                    if (srv.dynamicSocksPort.isNotBlank()) {
                        badgeRow.addView(makeMiniBadge("-D ${srv.dynamicSocksPort}"))
                    }
                    badgeScroll.addView(badgeRow)
                    bottomRow.addView(badgeScroll)

                    // Connected M3 Expressive Action Group (like image_602ce1.png) for [Ping | SFTP]
                    val actionsGroup = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = tonalBg(uiSurfaceContainer, dp(16))
                        setPadding(dp(3), dp(3), dp(3), dp(3))
                    }

                    val pingPill = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        minimumWidth = dp(76)
                        background = makeConnectedSegmentDrawable(uiSurfaceHigh, dpF(13f), dpF(4f))
                        setPadding(dp(10), dp(6), dp(10), dp(6))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = dp(2)
                        }
                        attachSmoothPressAnimation(this, 0.93f)
                        setOnClickListener {
                            triggerHaptic(0.85f)
                            pingServerAsync(srv, pingTv)
                        }
                    }
                    pingPill.addView(
                        MaterialIconView(this, M3Icon.PING, uiAccent, 15f, 1.95f).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                                marginEnd = dp(5)
                            }
                        }
                    )
                    pingPill.addView(pingTv)
                    actionsGroup.addView(pingPill)

                    val sftpPill = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        background = makeConnectedSegmentDrawable(uiAccentContainer, dpF(4f), dpF(13f))
                        setPadding(dp(12), dp(6), dp(13), dp(6))
                        attachSmoothPressAnimation(this, 0.93f)
                        setOnClickListener {
                            triggerHaptic(0.9f)
                            activeSftpServer = srv
                            remoteCurrentPath = if (srv.username == "root") "/root" else "/home/${srv.username}"
                            previousTab = activeTab
                            activeTab = 1
                            renderActiveTab()
                        }
                    }
                    sftpPill.addView(
                        MaterialIconView(this, M3Icon.FOLDER, uiAccent, 14.5f, 2.0f).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(15), dp(15)).apply {
                                marginEnd = dp(5)
                            }
                        }
                    )
                    sftpPill.addView(TextView(this).apply {
                        text = "SFTP"
                        setSingleLine(true)
                        setTextColor(uiOnAccentContainer)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    })
                    actionsGroup.addView(sftpPill)

                    bottomRow.addView(actionsGroup)
                    card.addView(bottomRow)
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
                    openSshServerSubPage(null) { renderServersTab() }
                }
            }
            contentFrame.addView(addFab)
        }

        // ====================================================================
        // TAB 1: High-Speed Asynchronous SFTP & File Explorer
        // ====================================================================
        fun listLocalEntriesViaCpp23(dir: File, depth: Int = 0): List<SftpItemEntry> {
            val raw = nativeListLocalDir(
                dir.absolutePath,
                sftpShowHidden,
                if (depth == 0) sftpSearchQuery else ""
            )
            if (raw.isEmpty()) return emptyList()
            val out = ArrayList<SftpItemEntry>()
            for (line in raw.lineSequence()) {
                if (line.isEmpty()) continue
                val parts = line.split("\t")
                if (parts.size >= 6) {
                    val name = parts[0]
                    val fullPath = parts[1]
                    val isDir = parts[2] == "1"
                    val sizeB = parts[3].toLongOrNull() ?: 0L
                    val mtime = parts[4].toLongOrNull() ?: 0L
                    val perms = parts[5]
                    val isExp = isDir && sftpTreeMode && expandedTreePaths.contains(fullPath)
                    out.add(
                        SftpItemEntry(
                            name = name,
                            path = fullPath,
                            isDirectory = isDir,
                            sizeBytes = sizeB,
                            modifiedEpochMs = mtime,
                            permissions = perms,
                            depth = depth,
                            isExpanded = isExp
                        )
                    )
                    if (isExp && depth < 5) {
                        out.addAll(listLocalEntriesViaCpp23(File(fullPath), depth + 1))
                    }
                }
            }
            return out
        }

        // Ultra-fast single-pass remote SFTP directory listing powered by C++26 nativeListRemoteSftpDir
        fun listRemoteSftpEntriesViaCpp23(
            server: SshServerProfile,
            remoteDir: String,
            showHiddenFlag: Boolean,
            searchQueryStr: String
        ): Triple<String?, String, List<SftpItemEntry>> {
            val raw = nativeListRemoteSftpDir(
                server.host,
                server.port,
                server.username,
                server.authType,
                server.secretOrKeyPath,
                server.jumpHost,
                remoteDir,
                showHiddenFlag,
                searchQueryStr
            )
            if (!raw.startsWith("OK\n")) {
                val errTxt = raw.removePrefix("ERR\n").trim()
                return Triple(
                    errTxt.ifEmpty { tr("Не удалось подключиться по SFTP/SSH", "Failed to connect via SFTP/SSH") },
                    remoteDir,
                    emptyList()
                )
            }
            val lines = raw.removePrefix("OK\n").split("\n")
            val resolvedDir = if (lines.isNotEmpty() && lines[0].isNotBlank()) lines[0].trim() else remoteDir
            val items = ArrayList<SftpItemEntry>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                val parts = line.split("\t")
                if (parts.size >= 6) {
                    items.add(
                        SftpItemEntry(
                            name = parts[0],
                            path = parts[1],
                            isDirectory = parts[2] == "1",
                            sizeBytes = parts[3].toLongOrNull() ?: 0L,
                            modifiedEpochMs = parts[4].toLongOrNull() ?: 0L,
                            permissions = parts[5]
                        )
                    )
                }
            }
            return Triple(null, resolvedDir, items)
        }

        // Liquid Glass Capsule Animator:
        // Leading edge races ahead while trailing edge lags and snaps with an underdamped liquid spring,
        // compressing vertical thickness slightly in mid-flight and rebounding like a liquid glass droplet,
        // while keeping horizontal edges strictly clamped inside [0, innerWidth]!
        fun animateLiquidSegmentedPill(
            pillView: View,
            parentOuter: View,
            targetTab: View,
            animate: Boolean
        ) {
            (pillView.tag as? ValueAnimator)?.cancel()
            pillView.scaleX = 1f
            if (targetTab.width <= 0) return

            val curPillRight = pillView.translationX + max(0, pillView.layoutParams?.width ?: 0).toFloat()
            val targetTabRight = (targetTab.left + targetTab.width).toFloat()
            val rawOuterW = (parentOuter.width - parentOuter.paddingLeft - parentOuter.paddingRight).toFloat()
            val innerWidth = max(rawOuterW, max(targetTabRight, curPillRight)).coerceAtLeast(dpF(24f))

            val maxEndLeft = max(0f, innerWidth - targetTab.width.toFloat())
            val endLeft = safeClamp(targetTab.left.toFloat(), 0f, maxEndLeft)
            val endRight = safeClamp(endLeft + targetTab.width.toFloat(), endLeft + 1f, max(endLeft + 1f, innerWidth))
            val targetW = (endRight - endLeft).coerceAtLeast(1f)

            val curLpW = pillView.layoutParams?.width ?: 0
            if (!animate || curLpW <= 0) {
                pillView.scaleY = 1f
                pillView.layoutParams = FrameLayout.LayoutParams(targetTab.width, targetTab.height)
                pillView.translationX = endLeft
                return
            }

            val startLeft = safeClamp(pillView.translationX, 0f, max(0f, innerWidth - 1f))
            val startW = curLpW.toFloat().coerceAtLeast(dpF(16f))
            val startRight = safeClamp(startLeft + startW, startLeft + 1f, max(startLeft + 1f, innerWidth))

            if (abs(endLeft - startLeft) < 0.5f && abs(targetW - startW) < 0.5f) {
                pillView.scaleY = 1f
                pillView.layoutParams = FrameLayout.LayoutParams(targetTab.width, targetTab.height)
                pillView.translationX = endLeft
                return
            }

            val movingRight = endLeft >= startLeft
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300L
                interpolator = LinearInterpolator()
                addUpdateListener { va ->
                    val t = safeClamp(va.animatedValue as Float, 0f, 1f)
                    val inv = 1f - t
                    val leadT = 1f - inv * inv * inv * inv
                    val trailT = if (t < 0.28f) {
                        (t / 0.28f) * (t / 0.28f) * 0.18f
                    } else {
                        val p = (t - 0.28f) / 0.72f
                        val spring = 1f - kotlin.math.exp(-4.6f * p) * kotlin.math.cos(4.8f * p)
                        safeClamp(0.18f + 0.82f * spring, 0f, 1.04f)
                    }
                    val curLeft: Float
                    val curRight: Float
                    if (movingRight) {
                        curLeft = startLeft + (endLeft - startLeft) * trailT
                        curRight = startRight + (endRight - startRight) * leadT
                    } else {
                        curLeft = startLeft + (endLeft - startLeft) * leadT
                        curRight = startRight + (endRight - startRight) * trailT
                    }
                    val clampedLeft = safeClamp(curLeft, 0f, max(0f, innerWidth - 1f))
                    val minRight = min(clampedLeft + dpF(12f), innerWidth)
                    val clampedRight = safeClamp(curRight, minRight, max(minRight, innerWidth))
                    val curW = (clampedRight - clampedLeft).coerceAtLeast(1f)

                    val stretchRatio = safeClamp(curW / targetW, 0.85f, 1.85f)
                    val settleBounce = if (t > 0.65f) {
                        val sp = (t - 0.65f) / 0.35f
                        0.035f * kotlin.math.sin(sp * Math.PI.toFloat())
                    } else 0f
                    pillView.scaleY = safeClamp(
                        safeClamp(1f / kotlin.math.sqrt(stretchRatio), 0.88f, 1.02f) + settleBounce,
                        0.88f,
                        1.04f
                    )
                    pillView.translationX = clampedLeft
                    pillView.layoutParams = FrameLayout.LayoutParams(curW.roundToInt(), targetTab.height)
                }
            }
            pillView.tag = anim
            anim.start()
        }

        fun renderSftpTab(animateTab: Boolean = false, forceRemoteRefresh: Boolean = false) {
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

            val usrDir = File("/data/data/com.termuxx/file/usr")
            val sdDir = Environment.getExternalStorageDirectory()
            val savedServers = loadSshServers()

            fun computeSftpSourceIdx(): Int = when {
                activeSftpServer != null -> 3
                localCurrentDir.absolutePath.startsWith(usrDir.absolutePath) -> 1
                localCurrentDir.absolutePath.startsWith(sdDir.absolutePath) ||
                    localCurrentDir.absolutePath.startsWith("/sdcard") ||
                    localCurrentDir.absolutePath.startsWith("/storage") -> 2
                else -> 0
            }

            fun currentActivePathStr(): String =
                if (activeSftpServer == null) localCurrentDir.absolutePath else remoteCurrentPath

            var currentSftpSourceIdx = computeSftpSourceIdx()

            val sourceBarOuter = FrameLayout(this).apply {
                background = tonalBg(uiSurfaceLow, dp(16))
                setPadding(dp(4), dp(4), dp(4), dp(4))
                clipChildren = true
                clipToPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(14), dp(8), dp(14), dp(4))
                }
            }
            val slidingSourcePill = View(this).apply {
                background = makeLiquidGlassPillDrawable(uiAccentContainer, dp(12))
                layoutParams = FrameLayout.LayoutParams(0, 0)
            }
            sourceBarOuter.addView(slidingSourcePill)

            val sourceBarWrap = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            sourceBarOuter.addView(sourceBarWrap)

            val segTabs = ArrayList<LinearLayout>()
            val segIcons = ArrayList<MaterialIconView>()
            val segLabels = ArrayList<TextView>()

            lateinit var rebuildBreadcrumbs: (Boolean) -> Unit
            lateinit var rebuildClipBanner: () -> Unit
            lateinit var populateEntriesIntoRowsCol: (String?, List<SftpItemEntry>) -> Unit
            lateinit var refreshSftpListInPlace: (Boolean, (() -> Unit)?) -> Unit
            lateinit var openMaterialCreateItemSheet: () -> Unit
            lateinit var openMaterialImportSheet: () -> Unit

            fun updateSourceBarSelection(newIdx: Int, animate: Boolean) {
                val prevIdx = currentSftpSourceIdx
                val dir = if (newIdx >= prevIdx) 1 else -1
                currentSftpSourceIdx = newIdx
                prevSftpSourceIdx = newIdx
                for (i in 0 until segTabs.size) {
                    val isSel = (i == newIdx)
                    segIcons[i].tintColor = if (isSel) uiAccent else COLOR_TEXT_DIM
                    segIcons[i].invalidate()
                    segLabels[i].setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                    segLabels[i].typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    if (i == 3 && activeSftpServer != null) {
                        segLabels[i].text = activeSftpServer!!.label
                    } else if (i == 3) {
                        segLabels[i].text = "SSH"
                    }
                    if (animate) {
                        if (isSel && prevIdx != newIdx) {
                            triggerFolderShakeAnimation(segIcons[i], dir)
                        }
                        segTabs[i].animate()
                            .scaleX(if (isSel) 1.04f else 1.0f)
                            .scaleY(if (isSel) 1.04f else 1.0f)
                            .setDuration(220L)
                            .setInterpolator(aosp17SpatialInterpolator)
                            .start()
                    } else {
                        segTabs[i].scaleX = if (isSel) 1.04f else 1.0f
                        segTabs[i].scaleY = if (isSel) 1.04f else 1.0f
                    }
                }
                val targetTab = segTabs.getOrNull(newIdx) ?: return
                if (targetTab.width > 0) {
                    animateLiquidSegmentedPill(slidingSourcePill, sourceBarOuter, targetTab, animate)
                }
            }

            val localGroups = listOf(
                Quadruple("Home", M3Icon.FOLDER, homeDir, false),
                Quadruple("Usr", M3Icon.FOLDER, usrDir, false),
                Quadruple("Storage", M3Icon.STORAGE, sdDir, true)
            )
            for ((gIdx, grp) in localGroups.withIndex()) {
                val (lbl, ic, dir, requiresPerm) = grp
                val isSel = (currentSftpSourceIdx == gIdx)
                val icView = MaterialIconView(
                    this,
                    ic,
                    if (isSel) uiAccent else COLOR_TEXT_DIM,
                    15.5f,
                    1.95f
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                        marginEnd = dp(4)
                    }
                }
                val lblView = TextView(this).apply {
                    text = lbl
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                }
                val segTab = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(6), dp(9), dp(6), dp(9))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(icView)
                    addView(lblView)
                    attachSmoothPressAnimation(this, 0.95f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        val switchToDir = {
                            if (currentSftpSourceIdx != gIdx || localCurrentDir != dir || activeSftpServer != null) {
                                sftpTransitionDir = if (gIdx >= currentSftpSourceIdx) 1 else -1
                                activeSftpServer = null
                                localCurrentDir = dir
                                updateSourceBarSelection(gIdx, animate = true)
                                rebuildBreadcrumbs(true)
                                rebuildClipBanner()
                                refreshSftpListInPlace(false, null)
                            } else {
                                triggerFolderShakeAnimation(icView, 1)
                            }
                        }
                        if (requiresPerm && !hasAllFilesAccessPermission() && !hasStandardStoragePermission()) {
                            showStoragePermissionsModal {
                                switchToDir()
                            }
                            return@setOnClickListener
                        }
                        if (!dir.exists()) dir.mkdirs()
                        switchToDir()
                    }
                }
                segTabs.add(segTab)
                segIcons.add(icView)
                segLabels.add(lblView)
                sourceBarWrap.addView(segTab)
            }

            if (savedServers.isNotEmpty()) {
                val isRemoteSel = (currentSftpSourceIdx == 3)
                val sshIcView = MaterialIconView(
                    this,
                    M3Icon.SERVER,
                    if (isRemoteSel) uiAccent else COLOR_TEXT_DIM,
                    15.5f,
                    1.6f
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                        marginEnd = dp(4)
                    }
                }
                val sshLblView = TextView(this).apply {
                    text = if (isRemoteSel && activeSftpServer != null) activeSftpServer!!.label else "SSH"
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    typeface = if (isRemoteSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(if (isRemoteSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                }
                val sshSegTab = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(6), dp(9), dp(6), dp(9))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.08f)
                    addView(sshIcView)
                    addView(sshLblView)
                    attachSmoothPressAnimation(this, 0.95f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        val connectToSrv: (SshServerProfile) -> Unit = { srv ->
                            sftpTransitionDir = 1
                            activeSftpServer = srv
                            remoteCurrentPath = if (srv.username == "root") "/root" else "/home/${srv.username}"
                            updateSourceBarSelection(3, animate = true)
                            rebuildBreadcrumbs(true)
                            rebuildClipBanner()
                            refreshSftpListInPlace(false, null)
                        }
                        if (savedServers.size == 1 && activeSftpServer == null) {
                            connectToSrv(savedServers[0])
                        } else {
                            val curIdx = savedServers.indexOfFirst { it.id == activeSftpServer?.id }.coerceAtLeast(0)
                            showBakaSuChooseDialog(
                                title = tr("Выберите SSH-сервер", "Select SSH Server"),
                                items = savedServers.map { it.label },
                                descriptions = savedServers.map { "${it.username}@${it.host}:${it.port}" },
                                selectedIndex = curIdx
                            ) { chosen ->
                                connectToSrv(savedServers[chosen])
                            }
                        }
                    }
                }
                segTabs.add(sshSegTab)
                segIcons.add(sshIcView)
                segLabels.add(sshLblView)
                sourceBarWrap.addView(sshSegTab)
            }

            val maxSegIdx = max(0, segTabs.lastIndex)
            val fromIdx = safeClampInt(prevSftpSourceIdx, 0, maxSegIdx)
            val toIdx = safeClampInt(currentSftpSourceIdx, 0, maxSegIdx)
            prevSftpSourceIdx = toIdx
            sourceBarWrap.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (sourceBarWrap.viewTreeObserver.isAlive) {
                        sourceBarWrap.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    val targetView = segTabs.getOrNull(toIdx) ?: return true
                    val fromView = segTabs.getOrNull(fromIdx) ?: targetView
                    slidingSourcePill.layoutParams = FrameLayout.LayoutParams(fromView.width, fromView.height)
                    slidingSourcePill.translationX = fromView.left.toFloat()
                    if (fromIdx != toIdx) {
                        animateLiquidSegmentedPill(slidingSourcePill, sourceBarOuter, targetView, animate = true)
                    } else {
                        animateLiquidSegmentedPill(slidingSourcePill, sourceBarOuter, targetView, animate = false)
                    }
                    return true
                }
            })
            mainSftpCol.addView(sourceBarOuter)

            val bcScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren = false
                clipToPadding = false
                setPadding(dp(14), dp(4), dp(14), dp(6))
            }
            val bcContainer = FrameLayout(this).apply {
                clipChildren = false
                clipToPadding = false
            }
            val bcSlidingPill = View(this).apply {
                background = makeLiquidGlassPillDrawable(uiAccentContainer, dp(10))
                layoutParams = FrameLayout.LayoutParams(0, 0)
            }
            val bcRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                clipChildren = false
                clipToPadding = false
                setPadding(dp(1), dp(2), dp(1), dp(2))
            }
            bcContainer.addView(bcSlidingPill)
            bcContainer.addView(bcRow)
            bcScroll.addView(bcContainer)
            mainSftpCol.addView(bcScroll)

            var prevBreadcrumbPaths: List<String> = emptyList()

            rebuildBreadcrumbs = { animateChips ->
                val hadPrevPill = bcSlidingPill.width > 0
                bcRow.removeAllViews()
                val activePathStr = currentActivePathStr()
                val parts = activePathStr.split("/").filter { it.isNotEmpty() }
                val segments = ArrayList<Pair<String, String>>()
                segments.add("/" to "/")
                var built = ""
                for (p in parts) {
                    built += "/$p"
                    segments.add(p to built)
                }

                // Determine how many prefix segments were ALREADY present before this transition
                var commonPrefixCount = 0
                while (
                    commonPrefixCount < segments.size &&
                    commonPrefixCount < prevBreadcrumbPaths.size &&
                    segments[commonPrefixCount].second == prevBreadcrumbPaths[commonPrefixCount]
                ) {
                    commonPrefixCount++
                }
                prevBreadcrumbPaths = segments.map { it.second }

                var lastChipView: TextView? = null

                for ((idx, seg) in segments.withIndex()) {
                    val isLast = (idx == segments.lastIndex)
                    // Only animate newly added segments (never re-animate existing prefix segments!)
                    val isNewlyAddedSegment = animateChips && idx >= commonPrefixCount

                    if (idx > 0) {
                        val sepTv = TextView(this).apply {
                            text = "›"
                            setTextColor(COLOR_TEXT_DIM)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                            setPadding(dp(4), 0, dp(4), 0)
                        }
                        if (isNewlyAddedSegment) {
                            val relIdx = (idx - commonPrefixCount).coerceAtLeast(0)
                            sepTv.alpha = 0f
                            sepTv.translationX = -dpF(6f)
                            sepTv.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setStartDelay((relIdx * 20L).coerceAtMost(80L))
                                .setDuration(200L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        }
                        bcRow.addView(sepTv)
                    }
                    val segChip = TextView(this).apply {
                        text = seg.first
                        setSingleLine(true)
                        typeface = if (isLast) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        if (isLast) {
                            setTextColor(uiOnAccentContainer)
                            background = null
                            setPadding(dp(12), dp(5), dp(12), dp(5))
                        } else {
                            setTextColor(COLOR_TEXT)
                            background = tonalBg(uiSurfaceContainer, dp(10))
                            setPadding(dp(10), dp(5), dp(10), dp(5))
                        }
                        if (isNewlyAddedSegment) {
                            val relIdx = (idx - commonPrefixCount).coerceAtLeast(0)
                            alpha = 0f
                            translationX = -dpF(10f)
                            scaleX = 0.88f
                            scaleY = 0.88f
                            animate()
                                .alpha(1f)
                                .translationX(0f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setStartDelay((relIdx * 22L).coerceAtMost(90L))
                                .setDuration(220L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        }
                        attachSmoothPressAnimation(this, 0.94f)
                        setOnClickListener {
                            triggerHaptic(0.75f)
                            if (isLast) {
                                val (pDlg, pSheet) = createExpressiveDialog(390)
                                pSheet.addView(TextView(this@TermuxActivity).apply {
                                    text = tr("Перейти по пути (поддерживается root /)", "Go to path (supports root /)")
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
                                        sftpTransitionDir = 1
                                        if (activeSftpServer == null) {
                                            localCurrentDir = File(target)
                                        } else {
                                            remoteCurrentPath = target
                                        }
                                        updateSourceBarSelection(computeSftpSourceIdx(), animate = true)
                                        rebuildBreadcrumbs(true)
                                        refreshSftpListInPlace(false, null)
                                    }
                                    pDlg.dismiss()
                                })
                                pSheet.addView(bRow)
                                pDlg.show()
                            } else if (activeSftpServer == null) {
                                sftpTransitionDir = -1
                                localCurrentDir = File(seg.second)
                                updateSourceBarSelection(computeSftpSourceIdx(), animate = true)
                                rebuildBreadcrumbs(true)
                                refreshSftpListInPlace(false, null)
                            } else {
                                sftpTransitionDir = -1
                                remoteCurrentPath = seg.second
                                rebuildBreadcrumbs(true)
                                refreshSftpListInPlace(false, null)
                            }
                        }
                    }
                    if (isLast) lastChipView = segChip
                    bcRow.addView(segChip)
                }

                bcRow.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (bcRow.viewTreeObserver.isAlive) {
                            bcRow.viewTreeObserver.removeOnPreDrawListener(this)
                        }
                        val targetChip = lastChipView ?: return true
                        bcSlidingPill.translationY = (bcRow.top + targetChip.top).toFloat()
                        animateLiquidSegmentedPill(
                            pillView = bcSlidingPill,
                            parentOuter = bcContainer,
                            targetTab = targetChip,
                            animate = animateChips && hadPrevPill
                        )
                        val scrollTargetX = (targetChip.left + targetChip.width - bcScroll.width + dp(28)).coerceAtLeast(0)
                        if (animateChips) {
                            bcScroll.smoothScrollTo(scrollTargetX, 0)
                        } else {
                            bcScroll.scrollTo(scrollTargetX, 0)
                        }
                        return true
                    }
                })
            }

            val clipBannerHolder = FrameLayout(this)
            mainSftpCol.addView(clipBannerHolder)

            rebuildClipBanner = {
                clipBannerHolder.removeAllViews()
                val srv = activeSftpServer
                val activeDirStr = currentActivePathStr()

                if (selectedSftpPaths.isNotEmpty()) {
                    val (selScroll, selRow) = makeHorizontalChipRow()
                    selScroll.setPadding(dp(14), dp(2), dp(14), dp(6))
                    selRow.addView(
                        TextView(this).apply {
                            text = tr("Выбрано: ${selectedSftpPaths.size}", "Selected: ${selectedSftpPaths.size}")
                            setTextColor(uiOnAccentContainer)
                            typeface = Typeface.DEFAULT_BOLD
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            background = tonalBg(uiAccentContainer, dp(12))
                            setPadding(dp(12), dp(7), dp(12), dp(7))
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                marginEnd = dp(6)
                            }
                        }
                    )
                    selRow.addView(makeExpressiveChip(tr("Все", "All"), false) {
                        for (e in lastRenderedEntries) {
                            selectedSftpPaths.add(e.path)
                        }
                        rebuildClipBanner()
                        populateEntriesIntoRowsCol(null, lastRenderedEntries)
                    })
                    if (srv == null) {
                        selRow.addView(makeExpressiveChip(tr("Копировать", "Copy"), false) {
                            batchClipSources.clear()
                            batchClipSources.addAll(selectedSftpPaths)
                            batchClipIsMove = false
                            fileClipSource = null
                            selectedSftpPaths.clear()
                            rebuildClipBanner()
                            populateEntriesIntoRowsCol(null, lastRenderedEntries)
                        })
                        selRow.addView(makeExpressiveChip(tr("Вырезать", "Cut"), false) {
                            batchClipSources.clear()
                            batchClipSources.addAll(selectedSftpPaths)
                            batchClipIsMove = true
                            fileClipSource = null
                            selectedSftpPaths.clear()
                            rebuildClipBanner()
                            populateEntriesIntoRowsCol(null, lastRenderedEntries)
                        })
                    }
                    selRow.addView(makeExpressiveChip(tr("В .tar.gz", "Archive .tar.gz"), false) {
                        val targets = selectedSftpPaths.toList()
                        val archiveName = "archive_${System.currentTimeMillis() / 1000}.tar.gz"
                        val archivePath = if (activeDirStr == "/") "/$archiveName" else "$activeDirStr/$archiveName"
                        val relNames = targets.map { it.substringAfterLast('/') }.filter { it.isNotEmpty() }
                        val joinedArgs = relNames.joinToString(" ") { shellEscapeSingle(it) }
                        val tarCmd = "cd -- ${shellEscapeSingle(activeDirStr)} && tar -czf ${shellEscapeSingle(archivePath)} -- $joinedArgs"
                        selectedSftpPaths.clear()
                        rebuildClipBanner()
                        Thread {
                            if (srv == null) {
                                nativeExecLocalRootCmd(tarCmd)
                                runOnUiThread {
                                    Toast.makeText(this, tr("Создан архив: $archiveName", "Created: $archiveName"), Toast.LENGTH_SHORT).show()
                                    refreshSftpListInPlace(true, null)
                                }
                            } else {
                                runRemoteSftpViaCpp23(srv, tarCmd)
                                runOnUiThread {
                                    Toast.makeText(this, tr("Создан архив: $archiveName", "Created: $archiveName"), Toast.LENGTH_SHORT).show()
                                    refreshSftpListInPlace(true, null)
                                }
                            }
                        }.start()
                    })
                    selRow.addView(
                        makeExpressiveChip(
                            title = tr("Удалить (${selectedSftpPaths.size})", "Delete (${selectedSftpPaths.size})"),
                            selected = true,
                            accentColor = COLOR_ROOT,
                            containerColor = COLOR_ROOT_CONTAINER
                        ) {
                            val toDel = selectedSftpPaths.toList()
                            selectedSftpPaths.clear()
                            rebuildClipBanner()
                            val joined = toDel.joinToString(" ") { shellEscapeSingle(it) }
                            Thread {
                                if (srv == null) {
                                    var allDeleted = true
                                    for (p in toDel) {
                                        if (!File(p).deleteRecursively()) allDeleted = false
                                    }
                                    if (!allDeleted) {
                                        nativeExecLocalRootCmd("rm -rf -- $joined")
                                    }
                                    runOnUiThread { refreshSftpListInPlace(true, null) }
                                } else {
                                    runRemoteSftpViaCpp23(srv, "rm -rf -- $joined")
                                    runOnUiThread { refreshSftpListInPlace(true, null) }
                                }
                            }.start()
                        }
                    )
                    selRow.addView(makeExpressiveChip(tr("Сброс ✕", "Clear ✕"), false) {
                        selectedSftpPaths.clear()
                        rebuildClipBanner()
                        populateEntriesIntoRowsCol(null, lastRenderedEntries)
                    })
                    clipBannerHolder.addView(selScroll)
                } else if (srv == null && (fileClipSource != null || batchClipSources.isNotEmpty())) {
                    val clipBanner = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(4), dp(14), dp(4))
                    }
                    val labelTxt = if (batchClipSources.isNotEmpty()) {
                        tr("Вставить (${batchClipSources.size})", "Paste (${batchClipSources.size})")
                    } else {
                        tr("Вставить «${fileClipSource!!.name}»", "Paste '${fileClipSource!!.name}'")
                    }
                    clipBanner.addView(makeExpressiveChip(labelTxt, true) {
                        val sources = if (batchClipSources.isNotEmpty()) {
                            batchClipSources.map { File(it) }
                        } else {
                            listOfNotNull(fileClipSource)
                        }
                        val isMove = if (batchClipSources.isNotEmpty()) batchClipIsMove else fileClipIsMove
                        Thread {
                            for (src in sources) {
                                val dst = File(localCurrentDir, src.name)
                                var handledDirect = false
                                try {
                                    if (src.exists() && localCurrentDir.canWrite()) {
                                        if (src.isDirectory) src.copyRecursively(dst, overwrite = true)
                                        else src.copyTo(dst, overwrite = true)
                                        if (isMove) src.deleteRecursively()
                                        handledDirect = true
                                    }
                                } catch (_: Throwable) {}
                                if (!handledDirect) {
                                    val op = if (isMove) "mv -f" else "cp -rf"
                                    nativeExecLocalRootCmd("$op -- ${shellEscapeSingle(src.absolutePath)} ${shellEscapeSingle(dst.absolutePath)}")
                                }
                            }
                            runOnUiThread {
                                fileClipSource = null
                                batchClipSources.clear()
                                rebuildClipBanner()
                                refreshSftpListInPlace(true, null)
                            }
                        }.start()
                    })
                    clipBanner.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) {
                        fileClipSource = null
                        batchClipSources.clear()
                        rebuildClipBanner()
                    })
                    clipBannerHolder.addView(clipBanner)
                }
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

            val ptrSftp = BakaSuPullToRefreshLayout(this, listScroll) { finishPtr ->
                refreshSftpListInPlace(true, finishPtr)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            mainSftpCol.addView(ptrSftp)

            fun openEntryMenu(item: SftpItemEntry) {
                val srv = activeSftpServer
                val (mDlg, mSheet) = createExpressiveDialog(390)
                val hdrRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(2), 0, dp(2), dp(10))
                }
                val hdrIconBadge = FrameLayout(this).apply {
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
                hdrRow.addView(hdrIconBadge)
                hdrIconBadge.post { triggerFolderShakeAnimation(hdrIconBadge, 1) }

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
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                })
                hdrRow.addView(hdrCol)
                mSheet.addView(hdrRow)

                val actions = ArrayList<Triple<M3Icon, String, () -> Unit>>()
                if (!item.isDirectory) {
                    actions.add(Triple(M3Icon.EDIT, tr("Редактировать текст", "Edit file")) {
                        mDlg.dismiss()
                        if (srv == null) {
                            val f = File(item.path)
                            if (f.canRead()) {
                                val content = try {
                                    if (f.length() <= 512 * 1024) f.readText() else tr("Файл превышает 512 КБ", "File > 512 KB")
                                } catch (e: Throwable) {
                                    e.message ?: ""
                                }
                                openCodeViewerAndEditorSubPage(item.name, item.path, content) { updated ->
                                    if (f.canWrite()) {
                                        try {
                                            f.writeText(updated)
                                            refreshSftpListInPlace(false, null)
                                        } catch (e: Throwable) {
                                            Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Thread {
                                            val b64 = android.util.Base64.encodeToString(updated.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                            nativeExecLocalRootCmd("printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(item.path)}")
                                            runOnUiThread { refreshSftpListInPlace(false, null) }
                                        }.start()
                                    }
                                }
                            } else {
                                Thread {
                                    val rawRes = nativeExecLocalRootCmd("head -c 262144 -- ${shellEscapeSingle(item.path)}")
                                    val content = rawRes.removePrefix("OK\n").removePrefix("ERR\n")
                                    runOnUiThread {
                                        openCodeViewerAndEditorSubPage(item.name, item.path, content) { updated ->
                                            Thread {
                                                val b64 = android.util.Base64.encodeToString(updated.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                                nativeExecLocalRootCmd("printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(item.path)}")
                                                runOnUiThread { refreshSftpListInPlace(false, null) }
                                            }.start()
                                        }
                                    }
                                }.start()
                            }
                        } else {
                            Toast.makeText(this@TermuxActivity, tr("Загрузка содержимого…", "Loading content…"), Toast.LENGTH_SHORT).show()
                            Thread {
                                val (ok, content) = runRemoteSftpViaCpp23(srv, "head -c 262144 -- ${shellEscapeSingle(item.path)}")
                                runOnUiThread {
                                    if (ok) {
                                        openCodeViewerAndEditorSubPage(item.name, "${srv.label}:${item.path}", content) { updated ->
                                            Toast.makeText(this@TermuxActivity, tr("Сохранение на сервере…", "Saving to server…"), Toast.LENGTH_SHORT).show()
                                            Thread {
                                                val b64 = android.util.Base64.encodeToString(updated.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                                val (sOk, sErr) = runRemoteSftpViaCpp23(
                                                    srv,
                                                    "printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(item.path)}"
                                                )
                                                runOnUiThread {
                                                    if (sOk) {
                                                        Toast.makeText(this@TermuxActivity, tr("Сохранено на ${srv.label}", "Saved on ${srv.label}"), Toast.LENGTH_SHORT).show()
                                                        refreshSftpListInPlace(true, null)
                                                    } else {
                                                        Toast.makeText(this@TermuxActivity, "Error: ${sErr.take(120)}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }.start()
                                        }
                                    } else {
                                        Toast.makeText(this@TermuxActivity, "SSH error: ${content.take(120)}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }.start()
                        }
                    })
                }

                actions.add(Triple(M3Icon.CHECK, if (selectedSftpPaths.contains(item.path)) tr("Снять выделение", "Deselect") else tr("Выделить объект", "Select item")) {
                    mDlg.dismiss()
                    if (selectedSftpPaths.contains(item.path)) {
                        selectedSftpPaths.remove(item.path)
                    } else {
                        selectedSftpPaths.add(item.path)
                    }
                    rebuildClipBanner()
                    populateEntriesIntoRowsCol(null, lastRenderedEntries)
                })

                if (srv == null) {
                    actions.add(Triple(M3Icon.COPY, tr("Копировать", "Copy")) {
                        batchClipSources.clear()
                        fileClipSource = File(item.path)
                        fileClipIsMove = false
                        mDlg.dismiss()
                        rebuildClipBanner()
                    })
                    actions.add(Triple(M3Icon.FOLDER_OPEN, tr("Вырезать (Переместить)", "Cut (Move)")) {
                        batchClipSources.clear()
                        fileClipSource = File(item.path)
                        fileClipIsMove = true
                        mDlg.dismiss()
                        rebuildClipBanner()
                    })
                }

                actions.add(Triple(M3Icon.STORAGE, tr("Упаковать в .tar.gz", "Compress to .tar.gz")) {
                    mDlg.dismiss()
                    val parentDir = item.path.substringBeforeLast('/').ifEmpty { "/" }
                    val outArchive = if (parentDir == "/") "/${item.name}.tar.gz" else "$parentDir/${item.name}.tar.gz"
                    val tarCmd = "cd -- ${shellEscapeSingle(parentDir)} && tar -czf ${shellEscapeSingle(outArchive)} -- ${shellEscapeSingle(item.name)}"
                    Thread {
                        if (srv == null) {
                            nativeExecLocalRootCmd(tarCmd)
                        } else {
                            runRemoteSftpViaCpp23(srv, tarCmd)
                        }
                        runOnUiThread {
                            Toast.makeText(this@TermuxActivity, tr("Архив создан: ${item.name}.tar.gz", "Archived: ${item.name}.tar.gz"), Toast.LENGTH_SHORT).show()
                            refreshSftpListInPlace(true, null)
                        }
                    }.start()
                })

                val lowerFileName = item.name.lowercase()
                if (!item.isDirectory && (lowerFileName.endsWith(".tar.gz") || lowerFileName.endsWith(".tgz") || lowerFileName.endsWith(".tar") || lowerFileName.endsWith(".zip"))) {
                    actions.add(Triple(M3Icon.FOLDER_OPEN, tr("Распаковать архив здесь", "Extract archive here")) {
                        mDlg.dismiss()
                        val parentDir = item.path.substringBeforeLast('/').ifEmpty { "/" }
                        val extractCmd = if (lowerFileName.endsWith(".zip")) {
                            "cd -- ${shellEscapeSingle(parentDir)} && unzip -o -- ${shellEscapeSingle(item.path)}"
                        } else {
                            "cd -- ${shellEscapeSingle(parentDir)} && tar -xf ${shellEscapeSingle(item.path)}"
                        }
                        Thread {
                            if (srv == null) {
                                nativeExecLocalRootCmd(extractCmd)
                            } else {
                                runRemoteSftpViaCpp23(srv, extractCmd)
                            }
                            runOnUiThread {
                                Toast.makeText(this@TermuxActivity, tr("Распаковано: ${item.name}", "Extracted: ${item.name}"), Toast.LENGTH_SHORT).show()
                                refreshSftpListInPlace(true, null)
                            }
                        }.start()
                    })
                }

                actions.add(Triple(M3Icon.COPY, tr("Скопировать полный путь", "Copy full path")) {
                    mDlg.dismiss()
                    copyToClipboard(item.path, true)
                })

                if (!item.isDirectory) {
                    actions.add(Triple(M3Icon.DOWNLOAD, tr("Скачать в Загрузки", "Download to /sdcard/Download")) {
                        mDlg.dismiss()
                        val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!dlDir.exists()) dlDir.mkdirs()
                        val dst = File(dlDir, item.name)
                        if (srv == null) {
                            try {
                                val src = File(item.path)
                                if (src.canRead()) {
                                    src.copyTo(dst, overwrite = true)
                                    Toast.makeText(this@TermuxActivity, tr("Сохранено: ${dst.absolutePath}", "Saved: ${dst.absolutePath}"), Toast.LENGTH_SHORT).show()
                                } else {
                                    Thread {
                                        nativeExecLocalRootCmd("cp -f -- ${shellEscapeSingle(item.path)} ${shellEscapeSingle(dst.absolutePath)}")
                                        runOnUiThread {
                                            Toast.makeText(this@TermuxActivity, tr("Сохранено: ${dst.absolutePath}", "Saved: ${dst.absolutePath}"), Toast.LENGTH_SHORT).show()
                                        }
                                    }.start()
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@TermuxActivity, tr("Скачивание ${item.name}…", "Downloading ${item.name}…"), Toast.LENGTH_SHORT).show()
                            Thread {
                                val (ok, out) = runRemoteSftpViaCpp23(srv, "base64 -- ${shellEscapeSingle(item.path)}")
                                runOnUiThread {
                                    if (ok) {
                                        try {
                                            val cleanB64 = out.replace("\\s".toRegex(), "")
                                            val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                                            dst.writeBytes(bytes)
                                            Toast.makeText(this@TermuxActivity, tr("Сохранено: ${dst.absolutePath}", "Saved: ${dst.absolutePath}"), Toast.LENGTH_LONG).show()
                                        } catch (e: Throwable) {
                                            Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(this@TermuxActivity, "SSH error: ${out.take(120)}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }.start()
                        }
                    })
                }

                actions.add(Triple(M3Icon.EDIT, tr("Переименовать", "Rename")) {
                    mDlg.dismiss()
                    val (rDlg, rSheet) = createExpressiveDialog(380)
                    rSheet.addView(TextView(this@TermuxActivity).apply {
                        text = tr("Переименовать", "Rename")
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                        setPadding(0, 0, 0, dp(8))
                    })
                    val rEt = EditText(this@TermuxActivity).apply {
                        setText(item.name)
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                        setSingleLine(true)
                        background = tonalBg(uiSurfaceLow, dp(12))
                        setPadding(dp(12), dp(9), dp(12), dp(9))
                    }
                    rSheet.addView(rEt)
                    val bRow = LinearLayout(this@TermuxActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                        setPadding(0, dp(12), 0, 0)
                    }
                    bRow.addView(makeExpressiveChip(tr("Отмена", "Cancel"), false) { rDlg.dismiss() })
                    bRow.addView(makeExpressiveChip(tr("Сохранить", "Save"), true) {
                        val newName = rEt.text.toString().trim()
                        if (newName.isNotEmpty() && newName != item.name && !newName.contains("..") && !newName.contains("/")) {
                            val parentDir = item.path.substringBeforeLast('/').ifEmpty { "/" }
                            val newTarget = if (parentDir == "/") "/$newName" else "$parentDir/$newName"
                            if (srv == null) {
                                val src = File(item.path)
                                val dst = File(newTarget)
                                if (src.renameTo(dst)) {
                                    refreshSftpListInPlace(false, null)
                                } else {
                                    Thread {
                                        nativeExecLocalRootCmd("mv -f -- ${shellEscapeSingle(item.path)} ${shellEscapeSingle(newTarget)}")
                                        runOnUiThread { refreshSftpListInPlace(false, null) }
                                    }.start()
                                }
                            } else {
                                Thread {
                                    runRemoteSftpViaCpp23(srv, "mv -- ${shellEscapeSingle(item.path)} ${shellEscapeSingle(newTarget)}")
                                    runOnUiThread { refreshSftpListInPlace(true, null) }
                                }.start()
                            }
                        }
                        rDlg.dismiss()
                    })
                    rSheet.addView(bRow)
                    rDlg.show()
                })

                actions.add(Triple(M3Icon.KEY, tr("Права доступа (chmod)", "Permissions (chmod)")) {
                    mDlg.dismiss()
                    val modes = listOf("755 (rwxr-xr-x)", "644 (rw-r--r--)", "700 (rwx------)", "600 (rw-------)", "777 (rwxrwxrwx)")
                    showBakaSuChooseDialog(
                        title = "chmod: ${item.name}",
                        items = modes,
                        selectedIndex = 0
                    ) { chosen ->
                        val octal = modes[chosen].substringBefore(' ')
                        if (srv == null) {
                            Thread {
                                try {
                                    val p = Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", octal, item.path))
                                    if (p.waitFor() != 0) {
                                        nativeExecLocalRootCmd("chmod $octal -- ${shellEscapeSingle(item.path)}")
                                    }
                                } catch (_: Throwable) {
                                    nativeExecLocalRootCmd("chmod $octal -- ${shellEscapeSingle(item.path)}")
                                }
                                runOnUiThread { refreshSftpListInPlace(false, null) }
                            }.start()
                        } else {
                            Thread {
                                runRemoteSftpViaCpp23(srv, "chmod $octal -- ${shellEscapeSingle(item.path)}")
                                runOnUiThread { refreshSftpListInPlace(true, null) }
                            }.start()
                        }
                    }
                })

                actions.add(Triple(M3Icon.TERMINAL, tr("Открыть путь в Терминале", "Open path in Terminal")) {
                    mDlg.dismiss()
                    val targetDir = if (item.isDirectory) item.path else item.path.substringBeforeLast('/').ifEmpty { "/" }
                    if (srv == null) {
                        dlg.dismiss()
                        nativeSendText("cd ${shellEscapeSingle(targetDir)}\n", false)
                    } else {
                        val cdCmd = "cd ${shellEscapeSingle(targetDir)}"
                        val srvWithCd = if (srv.startupCommand.isBlank()) {
                            srv.copy(startupCommand = "$cdCmd && exec \${'$'}SHELL -l")
                        } else {
                            srv.copy(startupCommand = "$cdCmd && ${srv.startupCommand}")
                        }
                        connectToSshServerInTerminal(srvWithCd, dlg)
                    }
                })

                actions.add(Triple(M3Icon.TRASH, tr("Удалить", "Delete")) {
                    mDlg.dismiss()
                    if (srv == null) {
                        val f = File(item.path)
                        if (f.deleteRecursively()) {
                            refreshSftpListInPlace(false, null)
                        } else {
                            Thread {
                                nativeExecLocalRootCmd("rm -rf -- ${shellEscapeSingle(item.path)}")
                                runOnUiThread { refreshSftpListInPlace(false, null) }
                            }.start()
                        }
                    } else {
                        Thread {
                            runRemoteSftpViaCpp23(srv, "rm -rf -- ${shellEscapeSingle(item.path)}")
                            runOnUiThread { refreshSftpListInPlace(true, null) }
                        }.start()
                    }
                })

                for ((aIdx, opt) in actions.withIndex()) {
                    val isDel = (opt.first == M3Icon.TRASH)
                    val optRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(11), dp(12), dp(11))
                        background = segmentedCardBg(aIdx, actions.size, uiSurfaceLow, 0, 18f, 4f)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = if (aIdx < actions.lastIndex) dp(2) else 0
                        }
                        attachSmoothPressAnimation(this, 0.98f)
                        setOnClickListener {
                            triggerHaptic(0.85f)
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

            populateEntriesIntoRowsCol = { errStr, entries ->
                lastRenderedEntries = entries
                val srv = activeSftpServer
                val activePathStr = currentActivePathStr()
                val hasParentRow = (activePathStr != "/" && sftpSearchQuery.isBlank())
                val animateDir = sftpTransitionDir
                val expandedTreePrefix = newlyExpandedTreePath?.let { if (it == "/") "/" else "$it/" }
                newlyExpandedTreePath = null

                rowsCol.removeAllViews()
                if (animateDir != 0) {
                    val forward = animateDir > 0
                    sftpTransitionDir = 0
                    val shiftPx = if (forward) dpF(24f) else -dpF(24f)
                    rowsCol.alpha = 0f
                    rowsCol.translationX = shiftPx
                    rowsCol.scaleX = 0.98f
                    rowsCol.scaleY = 0.98f
                    rowsCol.post { ensureCenteredPivot(rowsCol) }
                    rowsCol.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(240L)
                        .setInterpolator(aosp17SpatialInterpolator)
                        .start()
                }
                val totalCards = entries.size + (if (hasParentRow) 1 else 0)
                var currentCardIdx = 0

                if (hasParentRow) {
                    val upIconBadge = FrameLayout(this).apply {
                        background = tonalBg(uiAccentContainer, dp(11))
                        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                            marginEnd = dp(12)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.FOLDER, uiAccent, 21f, 2.0f))
                    }
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
                            triggerFolderShakeAnimation(upIconBadge, -1)
                            selectedSftpPaths.clear()
                            rebuildClipBanner()
                            val parentPath = activePathStr.substringBeforeLast('/').ifEmpty { "/" }
                            sftpTransitionDir = -1
                            if (srv == null) {
                                localCurrentDir = File(parentPath)
                                updateSourceBarSelection(computeSftpSourceIdx(), animate = true)
                                rebuildBreadcrumbs(true)
                                refreshSftpListInPlace(false, null)
                            } else {
                                remoteCurrentPath = parentPath
                                rebuildBreadcrumbs(true)
                                refreshSftpListInPlace(false, null)
                            }
                        }
                    }
                    upCard.addView(upIconBadge)
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

                if (errStr != null) {
                    val errCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        background = tonalBg(uiSurfaceLow, dp(24))
                        setPadding(dp(22), dp(24), dp(22), dp(20))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(8)
                        }
                    }
                    val errIconBadge = FrameLayout(this).apply {
                        background = tonalBg(COLOR_ROOT_CONTAINER, dp(16))
                        layoutParams = LinearLayout.LayoutParams(dp(50), dp(50)).apply {
                            bottomMargin = dp(12)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.SERVER, COLOR_PASTEL_RED, 24f, 2.05f))
                    }
                    errCard.addView(errIconBadge)
                    errCard.addView(TextView(this).apply {
                        text = tr("Ошибка подключения SFTP", "SFTP Connection Error")
                        gravity = Gravity.CENTER
                        setTextColor(COLOR_TEXT_BRIGHT)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    })
                    errCard.addView(TextView(this).apply {
                        text = errStr
                        gravity = Gravity.CENTER
                        setTextColor(COLOR_PASTEL_RED)
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.8f)
                        background = tonalBg(COLOR_ROOT_CONTAINER, dp(14))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(10)
                            bottomMargin = dp(14)
                        }
                    })
                    errCard.addView(
                        makeExpressiveChip(
                            title = tr("Повторить попытку", "Retry connection"),
                            selected = true
                        ) {
                            refreshSftpListInPlace(true, null)
                        }
                    )
                    rowsCol.addView(errCard)
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
                    var animatedFolderCount = 0
                    for (item in entries) {
                        val isItemSelected = selectedSftpPaths.contains(item.path)
                        val iconType = when {
                            isItemSelected -> M3Icon.CHECK
                            !item.isDirectory -> M3Icon.FILE
                            item.isExpanded -> M3Icon.FOLDER_OPEN
                            else -> M3Icon.FOLDER
                        }
                        val iconTint = when {
                            isItemSelected -> uiOnAccent
                            item.isDirectory -> uiAccent
                            else -> COLOR_TEXT_BRIGHT
                        }
                        val badgeFill = when {
                            isItemSelected -> uiAccent
                            item.isDirectory -> uiAccentContainer
                            else -> uiSurfaceContainer
                        }
                        val itemIconBadge = FrameLayout(this).apply {
                            background = tonalBg(badgeFill, dp(11))
                            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                                marginEnd = dp(12)
                            }
                            addView(MaterialIconView(this@TermuxActivity, iconType, iconTint, 20.5f, 2.0f))
                        }

                        val isExpandedChild = expandedTreePrefix != null && item.path.startsWith(expandedTreePrefix)
                        if ((animateDir != 0 || isExpandedChild) && animatedFolderCount < 16) {
                            animatedFolderCount++
                            val delayMs = (animatedFolderCount * 14L).coerceAtMost(130L)
                            val shakeDir = if (animateDir != 0) animateDir else 1
                            itemIconBadge.postDelayed({
                                triggerFolderShakeAnimation(itemIconBadge, shakeDir)
                            }, delayMs)
                        }

                        val rowFill = if (isItemSelected) blendColor(uiSurfaceLow, uiAccentContainer, 0.55f) else uiSurfaceLow
                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(14 + item.depth * 14), dp(11), dp(10), dp(11))
                            background = segmentedCardBg(currentCardIdx, max(1, totalCards), rowFill, 0, 20f, 5f)
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = dp(2)
                            }
                            attachSmoothPressAnimation(this, 0.985f)
                            setOnClickListener {
                                if (selectedSftpPaths.isNotEmpty()) {
                                    triggerHaptic(0.85f)
                                    if (selectedSftpPaths.contains(item.path)) {
                                        selectedSftpPaths.remove(item.path)
                                    } else {
                                        selectedSftpPaths.add(item.path)
                                    }
                                    rebuildClipBanner()
                                    populateEntriesIntoRowsCol(null, lastRenderedEntries)
                                    return@setOnClickListener
                                }
                                triggerHaptic(0.75f)
                                if (item.isDirectory) {
                                    triggerFolderShakeAnimation(itemIconBadge, 1)
                                    if (srv == null && sftpTreeMode) {
                                        if (expandedTreePaths.contains(item.path)) {
                                            expandedTreePaths.remove(item.path)
                                            newlyExpandedTreePath = null
                                        } else {
                                            expandedTreePaths.add(item.path)
                                            newlyExpandedTreePath = item.path
                                        }
                                        refreshSftpListInPlace(false, null)
                                    } else if (srv == null) {
                                        sftpTransitionDir = 1
                                        localCurrentDir = File(item.path)
                                        updateSourceBarSelection(computeSftpSourceIdx(), animate = true)
                                        rebuildBreadcrumbs(true)
                                        refreshSftpListInPlace(false, null)
                                    } else {
                                        sftpTransitionDir = 1
                                        remoteCurrentPath = item.path
                                        rebuildBreadcrumbs(true)
                                        refreshSftpListInPlace(false, null)
                                    }
                                } else {
                                    triggerFolderShakeAnimation(itemIconBadge, 1)
                                    openEntryMenu(item)
                                }
                            }
                            setOnLongClickListener {
                                triggerHaptic(1.25f)
                                if (selectedSftpPaths.contains(item.path)) {
                                    selectedSftpPaths.remove(item.path)
                                } else {
                                    selectedSftpPaths.add(item.path)
                                }
                                rebuildClipBanner()
                                populateEntriesIntoRowsCol(null, lastRenderedEntries)
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

                        row.addView(itemIconBadge)

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
            }

            var sftpRequestGeneration = 0

            refreshSftpListInPlace = { bypassCache, onDone ->
                val reqGen = ++sftpRequestGeneration
                val srv = activeSftpServer
                if (srv == null) {
                    val targetDir = localCurrentDir
                    val pathStr = targetDir.absolutePath
                    val isRootSystemPath = pathStr == "/" ||
                        (pathStr.startsWith("/data") && !pathStr.startsWith("/data/data/com.termuxx")) ||
                        pathStr.startsWith("/system") ||
                        pathStr.startsWith("/vendor") ||
                        pathStr.startsWith("/product") ||
                        pathStr.startsWith("/apex")
                    val directRead = !isRootSystemPath && targetDir.exists() && targetDir.canRead()
                    if (directRead) {
                        val list = listLocalEntriesViaCpp23(targetDir, 0)
                        populateEntriesIntoRowsCol(null, list)
                        onDone?.invoke()
                    } else {
                        val rootCacheKey = "local_root:$pathStr:$sftpShowHidden:$sftpSearchQuery:$sftpTreeMode"
                        val cachedRoot = if (!bypassCache) sftpRemoteDirCache[rootCacheKey]?.takeIf { it.isNotEmpty() } else null
                        if (cachedRoot != null) {
                            populateEntriesIntoRowsCol(null, cachedRoot)
                        }
                        Thread {
                            val entries = listLocalEntriesViaCpp23(targetDir, 0)
                            if (entries.isNotEmpty()) {
                                sftpRemoteDirCache[rootCacheKey] = entries
                            }
                            runOnUiThread {
                                if (reqGen == sftpRequestGeneration && activeTab == 1 && activeSftpServer == null && localCurrentDir.absolutePath == pathStr) {
                                    if (cachedRoot == null || cachedRoot != entries) {
                                        populateEntriesIntoRowsCol(null, entries)
                                    }
                                }
                                onDone?.invoke()
                            }
                        }.start()
                    }
                } else {
                    val requestedPath = remoteCurrentPath.ifBlank { "/" }
                    val showHiddenSnap = sftpShowHidden
                    val searchSnap = sftpSearchQuery
                    val cacheKey = "${srv.id}:$requestedPath:$showHiddenSnap:$searchSnap"
                    val cached = if (!bypassCache) sftpRemoteDirCache[cacheKey]?.takeIf { it.isNotEmpty() } else null
                    if (cached != null) {
                        populateEntriesIntoRowsCol(null, cached)
                        onDone?.invoke()
                    } else {
                        var loadingAnim: ValueAnimator? = null
                        if (onDone == null) {
                            rowsCol.removeAllViews()
                            val loadingWrap = LinearLayout(this).apply {
                                orientation = LinearLayout.VERTICAL
                                gravity = Gravity.CENTER
                                setPadding(dp(20), dp(42), dp(20), dp(42))
                            }
                            val morphBadge = FrameLayout(this).apply {
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.OVAL
                                    setColor(uiAccentContainer)
                                }
                                elevation = dpF(4f)
                                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                                    bottomMargin = dp(10)
                                }
                            }
                            val morphView = BakaSuMorphingIndicatorView(this)
                            morphBadge.addView(morphView, FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER))
                            loadingAnim = ValueAnimator.ofFloat(0f, 6f).apply {
                                duration = 3900L
                                repeatCount = ValueAnimator.INFINITE
                                interpolator = LinearInterpolator()
                                addUpdateListener { morphView.morphPhase = it.animatedValue as Float }
                                start()
                            }
                            loadingWrap.addView(morphBadge)
                            rowsCol.addView(loadingWrap)
                        }

                        Thread {
                            val (err, resolvedPath, remoteEntries) = listRemoteSftpEntriesViaCpp23(
                                srv,
                                requestedPath,
                                showHiddenSnap,
                                searchSnap
                            )
                            if (err == null && remoteEntries.isNotEmpty()) {
                                sftpRemoteDirCache[cacheKey] = remoteEntries
                                val resolvedKey = "${srv.id}:$resolvedPath:$showHiddenSnap:$searchSnap"
                                sftpRemoteDirCache[resolvedKey] = remoteEntries
                            }
                            runOnUiThread {
                                loadingAnim?.cancel()
                                if (reqGen == sftpRequestGeneration && activeTab == 1 && activeSftpServer?.id == srv.id) {
                                    if (resolvedPath.isNotBlank() && resolvedPath != remoteCurrentPath) {
                                        remoteCurrentPath = resolvedPath
                                        rebuildBreadcrumbs(true)
                                    }
                                    populateEntriesIntoRowsCol(err, remoteEntries)
                                }
                                onDone?.invoke()
                            }
                        }.start()
                    }
                }
            }

            rebuildBreadcrumbs(false)
            rebuildClipBanner()
            refreshSftpListInPlace(forceRemoteRefresh, null)

            openMaterialCreateItemSheet = {
                val srv = activeSftpServer
                val activePathStr = currentActivePathStr()
                val (cDlg, cSheet) = createExpressiveDialog(430)
                var createMode = 0
                var selectedChmod = "755"

                val heroBadge = FrameLayout(this).apply {
                    background = tonalBg(uiAccentContainer, dp(18))
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        bottomMargin = dp(10)
                    }
                }
                val heroIcon = MaterialIconView(this, M3Icon.CREATE_FOLDER, uiAccent, 26f, 2.15f)
                heroBadge.addView(heroIcon)
                cSheet.addView(heroBadge)

                val hdrTitle = TextView(this).apply {
                    text = tr("Новая папка", "New folder")
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                cSheet.addView(hdrTitle)

                val hdrPathChip = TextView(this).apply {
                    text = activePathStr
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    setTextColor(uiAccent)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = tonalBg(uiSurfaceLow, dp(10))
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        topMargin = dp(6)
                        bottomMargin = dp(14)
                    }
                }
                cSheet.addView(hdrPathChip)

                val segBarOuter = FrameLayout(this).apply {
                    background = tonalBg(uiSurfaceLow, dp(16))
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    clipChildren = true
                    clipToPadding = true
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(12)
                    }
                }
                val segIndicatorPill = View(this).apply {
                    background = makeLiquidGlassPillDrawable(uiAccentContainer, dp(12))
                    layoutParams = FrameLayout.LayoutParams(0, 0)
                }
                segBarOuter.addView(segIndicatorPill)

                val segBarRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                segBarOuter.addView(segBarRow)
                cSheet.addView(segBarOuter)

                val nameBox = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = tonalBg(uiSurfaceLow, dp(18))
                    setPadding(dp(16), dp(11), dp(16), dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                }
                val nameBoxLabel = TextView(this).apply {
                    text = tr("Имя папки (поддерживается вложенность a/b)", "Folder name (supports nested a/b)")
                    setTextColor(uiAccent)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                val nameEt = EditText(this).apply {
                    hint = "scripts"
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setSingleLine(true)
                    background = null
                    setPadding(0, dp(4), 0, 0)
                }
                nameBox.addView(nameBoxLabel)
                nameBox.addView(nameEt)
                cSheet.addView(nameBox)

                val contentBox = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = tonalBg(uiSurfaceLow, dp(18))
                    setPadding(dp(16), dp(11), dp(16), dp(12))
                    visibility = View.GONE
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                }
                contentBox.addView(TextView(this).apply {
                    text = tr("Начальное содержимое", "Initial content")
                    setTextColor(uiAccent)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                })
                val contentEt = EditText(this).apply {
                    hint = tr("Необязательно…", "Optional…")
                    setHintTextColor(COLOR_TEXT_DIM)
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    minLines = 3
                    maxLines = 5
                    gravity = Gravity.TOP or Gravity.START
                    background = null
                    setPadding(0, dp(4), 0, 0)
                }
                contentBox.addView(contentEt)
                cSheet.addView(contentBox)

                val chmodBarOuter = FrameLayout(this).apply {
                    background = tonalBg(uiSurfaceLow, dp(14))
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    clipChildren = true
                    clipToPadding = true
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(14)
                    }
                }
                val chmodIndicatorPill = View(this).apply {
                    background = makeLiquidGlassPillDrawable(uiAccentContainer, dp(10))
                    layoutParams = FrameLayout.LayoutParams(0, 0)
                }
                chmodBarOuter.addView(chmodIndicatorPill)

                val chmodBarRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                chmodBarOuter.addView(chmodBarRow)
                cSheet.addView(chmodBarOuter)

                val btnRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val modeTabs = ArrayList<LinearLayout>(3)
                val modeIcons = ArrayList<MaterialIconView>(3)
                val modeLabels = ArrayList<TextView>(3)
                val chmodModes = listOf("755", "644", "700", "600")
                val chmodTabs = ArrayList<TextView>(4)

                fun updateChmodSelection(newChmod: String, animate: Boolean) {
                    selectedChmod = newChmod
                    val targetIdx = chmodModes.indexOf(newChmod).coerceAtLeast(0)
                    for ((idx, tv) in chmodTabs.withIndex()) {
                        val isSel = (idx == targetIdx)
                        tv.typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
                        tv.setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                        if (animate) {
                            tv.animate()
                                .scaleX(if (isSel) 1.04f else 1.0f)
                                .scaleY(if (isSel) 1.04f else 1.0f)
                                .setDuration(200L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        } else {
                            tv.scaleX = if (isSel) 1.04f else 1.0f
                            tv.scaleY = if (isSel) 1.04f else 1.0f
                        }
                    }
                    val targetTab = chmodTabs.getOrNull(targetIdx) ?: return
                    if (targetTab.width > 0) {
                        animateLiquidSegmentedPill(chmodIndicatorPill, chmodBarOuter, targetTab, animate)
                    }
                }

                fun updateModeSelection(newMode: Int, animate: Boolean) {
                    val prevMode = createMode
                    val dir = if (newMode >= prevMode) 1 else -1
                    createMode = newMode

                    for (idx in 0 until modeTabs.size) {
                        val isSel = (idx == newMode)
                        modeIcons[idx].tintColor = if (isSel) uiAccent else COLOR_TEXT_DIM
                        modeIcons[idx].invalidate()
                        modeLabels[idx].setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                        modeLabels[idx].typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        if (animate) {
                            modeTabs[idx].animate()
                                .scaleX(if (isSel) 1.04f else 1.0f)
                                .scaleY(if (isSel) 1.04f else 1.0f)
                                .setDuration(220L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        } else {
                            modeTabs[idx].scaleX = if (isSel) 1.04f else 1.0f
                            modeTabs[idx].scaleY = if (isSel) 1.04f else 1.0f
                        }
                    }

                    val targetTab = modeTabs.getOrNull(newMode)
                    if (targetTab != null && targetTab.width > 0) {
                        animateLiquidSegmentedPill(segIndicatorPill, segBarOuter, targetTab, animate)
                    }

                    val isFolder = (newMode == 0)
                    heroIcon.icon = when (newMode) {
                        0 -> M3Icon.CREATE_FOLDER
                        1 -> M3Icon.FILE
                        else -> M3Icon.TERMINAL
                    }
                    heroIcon.invalidate()

                    if (animate && prevMode != newMode) {
                        triggerFolderShakeAnimation(heroBadge, dir)

                        hdrTitle.alpha = 0.3f
                        hdrTitle.translationX = dir * dpF(14f)
                        hdrTitle.animate()
                            .alpha(1.0f)
                            .translationX(0f)
                            .setDuration(210L)
                            .setInterpolator(aosp17SpatialInterpolator)
                            .start()

                        nameBox.alpha = 0.6f
                        nameBox.translationX = dir * dpF(10f)
                        nameBox.animate()
                            .alpha(1.0f)
                            .translationX(0f)
                            .setDuration(210L)
                            .setInterpolator(aosp17SpatialInterpolator)
                            .start()
                    }

                    hdrTitle.text = when (newMode) {
                        0 -> tr("Новая папка", "New folder")
                        1 -> tr("Новый файл", "New file")
                        else -> tr("Новый исполняемый скрипт", "New shell script")
                    }
                    nameBoxLabel.text = if (isFolder) {
                        tr("Имя папки (поддерживается вложенность a/b)", "Folder name (supports nested a/b)")
                    } else {
                        tr("Имя файла", "File name")
                    }
                    nameEt.hint = when (newMode) {
                        0 -> "scripts"
                        1 -> "config.json"
                        else -> "deploy.sh"
                    }
                    contentEt.hint = when (newMode) {
                        2 -> "#!/usr/bin/env bash\nset -euo pipefail\n..."
                        else -> tr("Необязательно…", "Optional…")
                    }

                    contentBox.animate().cancel()
                    val wasVisible = (contentBox.visibility == View.VISIBLE)
                    if (isFolder) {
                        contentBox.visibility = View.GONE
                        contentBox.alpha = 1f
                        contentBox.translationY = 0f
                        if (animate && wasVisible) {
                            chmodBarOuter.translationY = -dpF(10f)
                            btnRow.translationY = -dpF(10f)
                            chmodBarOuter.animate().translationY(0f).setDuration(200L).setInterpolator(aosp17SpatialInterpolator).start()
                            btnRow.animate().translationY(0f).setDuration(200L).setInterpolator(aosp17SpatialInterpolator).start()
                        }
                    } else {
                        contentBox.visibility = View.VISIBLE
                        if (animate && !wasVisible) {
                            contentBox.alpha = 0f
                            contentBox.translationY = -dpF(10f)
                            contentBox.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(210L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        } else if (animate && prevMode != newMode) {
                            contentBox.alpha = 0.5f
                            contentBox.translationX = dir * dpF(12f)
                            contentBox.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(210L)
                                .setInterpolator(aosp17SpatialInterpolator)
                                .start()
                        }
                    }
                    cSheet.requestLayout()
                }

                val modes = listOf(
                    Triple(0, M3Icon.FOLDER, tr("Папка", "Folder")),
                    Triple(1, M3Icon.FILE, tr("Файл", "File")),
                    Triple(2, M3Icon.TERMINAL, tr("Скрипт .sh", "Script .sh"))
                )
                for ((mCode, ic, lbl) in modes) {
                    val isSel = (createMode == mCode)
                    val icView = MaterialIconView(
                        this,
                        ic,
                        if (isSel) uiAccent else COLOR_TEXT_DIM,
                        17f,
                        2.0f
                    ).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                            marginEnd = dp(5)
                        }
                    }
                    val lblView = TextView(this).apply {
                        text = lbl
                        setSingleLine(true)
                        setTextColor(if (isSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                        typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    val tab = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        setPadding(dp(8), dp(9), dp(8), dp(9))
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addView(icView)
                        addView(lblView)
                        attachSmoothPressAnimation(this, 0.95f)
                        setOnClickListener {
                            if (createMode == mCode) return@setOnClickListener
                            triggerHaptic(0.85f)
                            val targetChmod = if (mCode == 1) "644" else "755"
                            updateModeSelection(mCode, animate = true)
                            updateChmodSelection(targetChmod, animate = true)
                        }
                    }
                    modeTabs.add(tab)
                    modeIcons.add(icView)
                    modeLabels.add(lblView)
                    segBarRow.addView(tab)
                }

                for (mode in chmodModes) {
                    val isModeSel = (selectedChmod == mode)
                    val mBtn = TextView(this).apply {
                        text = "chmod $mode"
                        gravity = Gravity.CENTER
                        setSingleLine(true)
                        typeface = if (isModeSel) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setTextColor(if (isModeSel) uiOnAccentContainer else COLOR_TEXT_DIM)
                        setPadding(dp(6), dp(7), dp(6), dp(7))
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        attachSmoothPressAnimation(this, 0.95f)
                        setOnClickListener {
                            if (selectedChmod == mode) return@setOnClickListener
                            triggerHaptic(0.75f)
                            updateChmodSelection(mode, animate = true)
                        }
                    }
                    chmodTabs.add(mBtn)
                    chmodBarRow.addView(mBtn)
                }

                segBarRow.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (segBarRow.viewTreeObserver.isAlive) {
                            segBarRow.viewTreeObserver.removeOnPreDrawListener(this)
                        }
                        val firstModeTab = modeTabs.getOrNull(createMode)
                        if (firstModeTab != null) {
                            animateLiquidSegmentedPill(segIndicatorPill, segBarOuter, firstModeTab, animate = false)
                        }
                        val chmodIdx = chmodModes.indexOf(selectedChmod).coerceAtLeast(0)
                        val firstChmodTab = chmodTabs.getOrNull(chmodIdx)
                        if (firstChmodTab != null) {
                            animateLiquidSegmentedPill(chmodIndicatorPill, chmodBarOuter, firstChmodTab, animate = false)
                        }
                        return true
                    }
                })

                val cancelBtn = TextView(this).apply {
                    text = tr("Отмена", "Cancel")
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    background = tonalBg(uiSurfaceLow, dp(16))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(8)
                    }
                    attachSmoothPressAnimation(this, 0.96f)
                    setOnClickListener {
                        triggerHaptic(0.75f)
                        cDlg.dismiss()
                    }
                }
                val createBtn = TextView(this).apply {
                    text = tr("Создать", "Create")
                    gravity = Gravity.CENTER
                    setTextColor(uiOnAccentContainer)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    background = tonalBg(uiAccentContainer, dp(16))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    attachSmoothPressAnimation(this, 0.96f)
                    setOnClickListener {
                        triggerHaptic(0.9f)
                        val n = nameEt.text.toString().trim().trim('/')
                        if (n.isEmpty() || n.contains("..") || n.contains("\\")) {
                            Toast.makeText(this@TermuxActivity, tr("Недопустимое имя", "Invalid name"), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val isFolder = (createMode == 0)
                        val initTxt = contentEt.text.toString()
                        if (srv == null) {
                            val target = File(localCurrentDir, n)
                            cDlg.dismiss()
                            if (localCurrentDir.canWrite()) {
                                try {
                                    if (isFolder) {
                                        target.mkdirs()
                                    } else {
                                        target.parentFile?.mkdirs()
                                        target.writeText(initTxt)
                                    }
                                    Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", selectedChmod, target.absolutePath)).waitFor()
                                } catch (_: Throwable) {}
                                refreshSftpListInPlace(false, null)
                            } else {
                                Thread {
                                    val fullLocal = target.absolutePath
                                    if (isFolder) {
                                        nativeExecLocalRootCmd("mkdir -p -- ${shellEscapeSingle(fullLocal)} && chmod $selectedChmod -- ${shellEscapeSingle(fullLocal)}")
                                    } else if (initTxt.isEmpty()) {
                                        nativeExecLocalRootCmd("touch -- ${shellEscapeSingle(fullLocal)} && chmod $selectedChmod -- ${shellEscapeSingle(fullLocal)}")
                                    } else {
                                        val b64 = android.util.Base64.encodeToString(initTxt.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                        nativeExecLocalRootCmd("printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(fullLocal)} && chmod $selectedChmod -- ${shellEscapeSingle(fullLocal)}")
                                    }
                                    runOnUiThread { refreshSftpListInPlace(false, null) }
                                }.start()
                            }
                        } else {
                            val fullRemote = if (remoteCurrentPath == "/") "/$n" else "$remoteCurrentPath/$n"
                            cDlg.dismiss()
                            Thread {
                                if (isFolder) {
                                    runRemoteSftpViaCpp23(srv, "mkdir -p -- ${shellEscapeSingle(fullRemote)} && chmod $selectedChmod -- ${shellEscapeSingle(fullRemote)}")
                                } else if (initTxt.isEmpty()) {
                                    runRemoteSftpViaCpp23(srv, "touch -- ${shellEscapeSingle(fullRemote)} && chmod $selectedChmod -- ${shellEscapeSingle(fullRemote)}")
                                } else {
                                    val b64 = android.util.Base64.encodeToString(initTxt.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                    runRemoteSftpViaCpp23(srv, "printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(fullRemote)} && chmod $selectedChmod -- ${shellEscapeSingle(fullRemote)}")
                                }
                                runOnUiThread { refreshSftpListInPlace(true, null) }
                            }.start()
                        }
                    }
                }
                btnRow.addView(cancelBtn)
                btnRow.addView(createBtn)
                cSheet.addView(btnRow)
                cDlg.show()
            }

            fun importBytesToCurrentDestination(fileName: String, bytes: ByteArray) {
                val srv = activeSftpServer
                if (srv == null) {
                    val dst = File(localCurrentDir, fileName)
                    if (localCurrentDir.canWrite()) {
                        try {
                            dst.writeBytes(bytes)
                            Toast.makeText(this, tr("Импортировано: $fileName", "Imported: $fileName"), Toast.LENGTH_SHORT).show()
                            refreshSftpListInPlace(false, null)
                        } catch (e: Throwable) {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Thread {
                            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            nativeExecLocalRootCmd("printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(dst.absolutePath)}")
                            runOnUiThread {
                                Toast.makeText(this, tr("Импортировано: $fileName", "Imported: $fileName"), Toast.LENGTH_SHORT).show()
                                refreshSftpListInPlace(false, null)
                            }
                        }.start()
                    }
                } else {
                    Toast.makeText(this, tr("Загрузка $fileName на ${srv.label}…", "Uploading $fileName to ${srv.label}…"), Toast.LENGTH_SHORT).show()
                    Thread {
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val remoteTarget = if (remoteCurrentPath == "/") "/$fileName" else "$remoteCurrentPath/$fileName"
                        val (ok, err) = runRemoteSftpViaCpp23(
                            srv,
                            "printf '%s' ${shellEscapeSingle(b64)} | base64 -d > ${shellEscapeSingle(remoteTarget)}"
                        )
                        runOnUiThread {
                            if (ok) {
                                Toast.makeText(this, tr("Загружено: $fileName", "Uploaded: $fileName"), Toast.LENGTH_SHORT).show()
                                refreshSftpListInPlace(true, null)
                            } else {
                                Toast.makeText(this, "Error: ${err.take(120)}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                }
            }

            openMaterialImportSheet = {
                val srv = activeSftpServer
                val activePathStr = currentActivePathStr()
                val (uDlg, uSheet) = createExpressiveDialog(430)

                val heroBadge = FrameLayout(this).apply {
                    background = tonalBg(uiAccentContainer, dp(18))
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        bottomMargin = dp(10)
                    }
                    addView(MaterialIconView(this@TermuxActivity, M3Icon.UPLOAD, uiAccent, 26f, 2.15f))
                }
                uSheet.addView(heroBadge)

                uSheet.addView(TextView(this).apply {
                    text = if (srv == null) tr("Импорт файла", "Import file") else tr("Выгрузить файл на ${srv.label}", "Upload file to ${srv.label}")
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_BRIGHT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                    typeface = Typeface.DEFAULT_BOLD
                })
                uSheet.addView(TextView(this).apply {
                    text = activePathStr
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    setTextColor(uiAccent)
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, dp(4), 0, dp(14))
                })

                val sysPickerCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    background = tonalBg(uiAccentContainer, dp(20))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                    attachSmoothPressAnimation(this, 0.97f)
                    setOnClickListener {
                        triggerHaptic(0.9f)
                        uDlg.dismiss()
                        launchSystemFilePickerForImport { uri ->
                            Thread {
                                try {
                                    val name = queryUriDisplayName(uri)
                                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    if (bytes != null) {
                                        runOnUiThread {
                                            importBytesToCurrentDestination(name, bytes)
                                        }
                                    }
                                } catch (e: Throwable) {
                                    runOnUiThread {
                                        Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.start()
                        }
                    }
                }
                sysPickerCard.addView(
                    FrameLayout(this).apply {
                        background = tonalBg(blendColor(uiAccentContainer, uiAccent, 0.20f), dp(12))
                        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                            marginEnd = dp(14)
                        }
                        addView(MaterialIconView(this@TermuxActivity, M3Icon.FOLDER_OPEN, uiOnAccentContainer, 21f, 2.1f))
                    }
                )
                val spCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                spCol.addView(TextView(this).apply {
                    text = tr("Выбрать через проводник Android", "Choose via Android file picker")
                    setTextColor(uiOnAccentContainer)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                })
                spCol.addView(TextView(this).apply {
                    text = tr("Открыть системное меню выбора любого файла", "Open native system document picker")
                    setTextColor(uiAccent)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setPadding(0, dp(2), 0, 0)
                })
                sysPickerCard.addView(spCol)
                uSheet.addView(sysPickerCard)

                val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val recentDlFiles = try {
                    dlDir.listFiles()?.filter { it.isFile && it.canRead() }
                        ?.sortedByDescending { it.lastModified() }
                        ?.take(4) ?: emptyList()
                } catch (_: Throwable) {
                    emptyList()
                }

                if (recentDlFiles.isNotEmpty()) {
                    uSheet.addView(TextView(this).apply {
                        text = tr("Быстрый выбор из Загрузок (Download)", "Recent files in Download")
                        setTextColor(COLOR_TEXT_DIM)
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(dp(4), dp(2), dp(4), dp(6))
                    })
                    for ((rIdx, rf) in recentDlFiles.withIndex()) {
                        val rCard = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(14), dp(10), dp(14), dp(10))
                            background = segmentedCardBg(rIdx, recentDlFiles.size, uiSurfaceLow, 0, 18f, 4f)
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = if (rIdx < recentDlFiles.lastIndex) dp(2) else dp(10)
                            }
                            attachSmoothPressAnimation(this, 0.98f)
                            setOnClickListener {
                                triggerHaptic(0.85f)
                                uDlg.dismiss()
                                try {
                                    importBytesToCurrentDestination(rf.name, rf.readBytes())
                                } catch (e: Throwable) {
                                    Toast.makeText(this@TermuxActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        rCard.addView(
                            FrameLayout(this).apply {
                                background = tonalBg(uiSurfaceContainer, dp(10))
                                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                                    marginEnd = dp(12)
                                }
                                addView(MaterialIconView(this@TermuxActivity, M3Icon.FILE, uiAccent, 18f, 2.0f))
                            }
                        )
                        val rCol = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        rCol.addView(TextView(this).apply {
                            text = rf.name
                            setSingleLine(true)
                            setTextColor(COLOR_TEXT_BRIGHT)
                            typeface = Typeface.MONOSPACE
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                        })
                        rCol.addView(TextView(this).apply {
                            text = formatSize(rf.length())
                            setTextColor(COLOR_TEXT_DIM)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                        })
                        rCard.addView(rCol)
                        uSheet.addView(rCard)
                    }
                }

                val closeBtn = TextView(this).apply {
                    text = tr("Закрыть", "Close")
                    gravity = Gravity.CENTER
                    setTextColor(COLOR_TEXT_BRIGHT)
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    background = tonalBg(uiSurfaceLow, dp(16))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    attachSmoothPressAnimation(this, 0.97f)
                    setOnClickListener {
                        triggerHaptic(0.75f)
                        uDlg.dismiss()
                    }
                }
                uSheet.addView(closeBtn)
                uDlg.show()
            }

            val bottomBarRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
                clipChildren = false
                clipToPadding = false
                setPadding(dp(24), dp(4), dp(24), dp(24))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                )
            }

            val floatingPill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = makeM3FloatingSurfaceDrawable(dp(32), tintWithAccent = false)
                elevation = dpF(12f)
                setPadding(dp(6), dp(6), dp(6), dp(6))
                layoutParams = if (sftpSearchActive) {
                    LinearLayout.LayoutParams(0, dp(60), 1f)
                } else {
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(60))
                }
            }

            val bentoButtons = ArrayList<FrameLayout>()
            val bentoDrawables = ArrayList<GradientDrawable>()
            val bentoIcons = ArrayList<MaterialIconView>()
            val bentoActiveStates = ArrayList<Boolean>()

            val inactiveBentoColor = blendColor(uiSurfaceHigh, uiAccentContainer, 0.28f)
            val activeBentoColor = uiAccentContainer
            val primaryActionBentoColor = uiAccent

            fun baseBentoWidthPx(idx: Int): Int {
                val isAct = bentoActiveStates.getOrNull(idx) == true
                return if (isAct) dp(64) else dp(46)
            }

            fun animateBentoHoldState(
                pressedIdx: Int,
                holding: Boolean,
                newActiveState: Boolean,
                isPrimaryAccent: Boolean = false
            ) {
                val btn = bentoButtons.getOrNull(pressedIdx) ?: return
                val bg = bentoDrawables.getOrNull(pressedIdx) ?: return
                val ic = bentoIcons.getOrNull(pressedIdx) ?: return

                (btn.tag as? ValueAnimator)?.cancel()
                bentoActiveStates[pressedIdx] = newActiveState

                val baseSelfW = baseBentoWidthPx(pressedIdx).toFloat()
                val holdDeltaPx = dpF(14f)
                val curW = btn.layoutParams.width.toFloat().coerceAtLeast(dpF(34f))
                val targetW = if (holding) (baseSelfW + holdDeltaPx) else baseSelfW
                val startRad = bg.cornerRadius.takeIf { it > 0f } ?: dpF(23f)
                val targetRad = if (holding) dpF(11f) else dpF(23f)

                val targetFill = when {
                    isPrimaryAccent -> primaryActionBentoColor
                    holding || newActiveState -> activeBentoColor
                    else -> inactiveBentoColor
                }
                val targetTint = when {
                    isPrimaryAccent -> uiSurface
                    holding || newActiveState -> uiOnAccentContainer
                    else -> COLOR_TEXT_BRIGHT
                }
                bg.setColor(targetFill)
                ic.tintColor = targetTint
                ic.invalidate()

                val leftIdx = pressedIdx - 1
                val rightIdx = pressedIdx + 1
                val hasLeft = leftIdx >= 0
                val hasRight = rightIdx < bentoButtons.size
                // Exact zero-sum neighbor length compression so outer buttons never shift sideways!
                val leftShrink = if (!holding) 0f else if (hasLeft && hasRight) holdDeltaPx * 0.5f else if (hasLeft) holdDeltaPx else 0f
                val rightShrink = if (!holding) 0f else if (hasLeft && hasRight) holdDeltaPx * 0.5f else if (hasRight) holdDeltaPx else 0f
                val leftStartW = if (hasLeft) bentoButtons[leftIdx].layoutParams.width.toFloat() else 0f
                val rightStartW = if (hasRight) bentoButtons[rightIdx].layoutParams.width.toFloat() else 0f
                val leftTargetW = if (hasLeft) (baseBentoWidthPx(leftIdx).toFloat() - leftShrink).coerceAtLeast(dpF(32f)) else 0f
                val rightTargetW = if (hasRight) (baseBentoWidthPx(rightIdx).toFloat() - rightShrink).coerceAtLeast(dpF(32f)) else 0f

                val startScaleY = btn.scaleY
                val targetScaleY = if (holding) 0.93f else 1.0f

                val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = if (holding) 130L else 380L
                    interpolator = LinearInterpolator()
                    addUpdateListener { va ->
                        val t = (va.animatedValue as Float).coerceIn(0f, 1f)
                        val prog = if (holding) {
                            1f - (1f - t) * (1f - t)
                        } else {
                            // Bouncy underdamped spring for lively rebound on tap & release!
                            1f - kotlin.math.exp(-3.3f * t) * kotlin.math.cos(6.4f * t)
                        }
                        val radProg = if (holding) {
                            1f - (1f - t) * (1f - t)
                        } else {
                            (1f - kotlin.math.exp(-3.8f * t) * kotlin.math.cos(5.2f * t)).coerceIn(0f, 1.15f)
                        }

                        bg.cornerRadius = (startRad + (targetRad - startRad) * radProg).coerceAtLeast(dpF(8f))
                        btn.scaleY = (startScaleY + (targetScaleY - startScaleY) * prog).coerceIn(0.88f, 1.08f)
                        val lp = btn.layoutParams as LinearLayout.LayoutParams
                        lp.width = (curW + (targetW - curW) * prog).roundToInt().coerceAtLeast(dp(36))
                        btn.layoutParams = lp

                        if (leftIdx >= 0) {
                            val lBtn = bentoButtons[leftIdx]
                            val lLp = lBtn.layoutParams as LinearLayout.LayoutParams
                            lLp.width = (leftStartW + (leftTargetW - leftStartW) * prog).roundToInt().coerceAtLeast(dp(36))
                            lBtn.layoutParams = lLp
                        }
                        if (rightIdx < bentoButtons.size) {
                            val rBtn = bentoButtons[rightIdx]
                            val rLp = rBtn.layoutParams as LinearLayout.LayoutParams
                            rLp.width = (rightStartW + (rightTargetW - rightStartW) * prog).roundToInt().coerceAtLeast(dp(36))
                            rBtn.layoutParams = rLp
                        }
                    }
                }
                btn.tag = anim
                anim.start()
            }

            fun makeBentoPillBtn(
                icon: M3Icon,
                initialActive: Boolean = false,
                isPrimaryAccent: Boolean = false,
                onClick: (Int) -> Unit
            ): FrameLayout {
                val idx = bentoButtons.size
                bentoActiveStates.add(initialActive)
                val initFill = when {
                    isPrimaryAccent -> primaryActionBentoColor
                    initialActive -> activeBentoColor
                    else -> inactiveBentoColor
                }
                val initTint = when {
                    isPrimaryAccent -> uiSurface
                    initialActive -> uiOnAccentContainer
                    else -> COLOR_TEXT_BRIGHT
                }
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpF(23f)
                    setColor(initFill)
                }
                val icView = MaterialIconView(
                    this@TermuxActivity,
                    icon = icon,
                    tintColor = initTint,
                    iconSizeDp = 21.5f,
                    strokeWidthDp = 2.1f
                )
                var downMs = 0L
                val releaseTask = Runnable {
                    if (idx < bentoActiveStates.size) {
                        animateBentoHoldState(
                            idx,
                            holding = false,
                            newActiveState = bentoActiveStates[idx],
                            isPrimaryAccent = isPrimaryAccent
                        )
                    }
                }
                val frame = FrameLayout(this).apply {
                    background = bgDrawable
                    layoutParams = LinearLayout.LayoutParams(
                        if (initialActive) dp(64) else dp(46),
                        dp(46)
                    ).apply {
                        setMargins(dp(3), 0, dp(3), 0)
                    }
                    addView(icView)
                    setOnTouchListener { v, ev ->
                        when (ev.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                v.removeCallbacks(releaseTask)
                                downMs = SystemClock.uptimeMillis()
                                triggerHaptic(0.85f)
                                animateBentoHoldState(idx, holding = true, newActiveState = bentoActiveStates[idx], isPrimaryAccent = isPrimaryAccent)
                                false
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                v.removeCallbacks(releaseTask)
                                releaseTask.run()
                                false
                            }
                            MotionEvent.ACTION_UP -> {
                                val heldMs = SystemClock.uptimeMillis() - downMs
                                // Ensure quick taps finish expanding to peak squircle before springing back with full bounce!
                                if (heldMs < 130L) {
                                    v.postDelayed(releaseTask, (130L - heldMs).coerceIn(45L, 120L))
                                } else {
                                    v.postDelayed(releaseTask, 12L)
                                }
                                false
                            }
                            else -> false
                        }
                    }
                    setOnClickListener {
                        onClick(idx)
                    }
                }
                bentoButtons.add(frame)
                bentoDrawables.add(bgDrawable)
                bentoIcons.add(icView)
                return frame
            }

            lateinit var populateBottomFloatingPill: (Boolean) -> Unit
            populateBottomFloatingPill = { animateMorph ->
                (floatingPill.tag as? ValueAnimator)?.cancel()
                val oldW = floatingPill.width
                floatingPill.removeAllViews()
                bentoButtons.clear()
                bentoDrawables.clear()
                bentoIcons.clear()
                bentoActiveStates.clear()

                if (sftpSearchActive) {
                    floatingPill.addView(makeBentoPillBtn(M3Icon.ARROW_BACK, initialActive = false) { _ ->
                        sftpSearchActive = false
                        if (sftpSearchQuery.isNotEmpty()) {
                            sftpSearchQuery = ""
                            refreshSftpListInPlace(false, null)
                        }
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(floatingPill.windowToken, 0)
                        populateBottomFloatingPill(true)
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
                        setPadding(dp(8), 0, dp(8), 0)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                sftpSearchQuery = s?.toString() ?: ""
                                refreshSftpListInPlace(false, null)
                            }
                        })
                    }
                    floatingPill.addView(searchEt)
                    floatingPill.addView(makeBentoPillBtn(M3Icon.SEARCH, initialActive = true) { idx ->
                        bentoActiveStates[idx] = true
                        sftpSearchQuery = searchEt.text.toString()
                        refreshSftpListInPlace(false, null)
                    })
                    searchEt.post {
                        searchEt.requestFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showSoftInput(searchEt, 0)
                    }
                } else {
                    floatingPill.addView(makeBentoPillBtn(M3Icon.SEARCH, initialActive = false) { _ ->
                        sftpSearchActive = true
                        populateBottomFloatingPill(true)
                    })
                    floatingPill.addView(makeBentoPillBtn(M3Icon.TREE, initialActive = sftpTreeMode) { idx ->
                        sftpTreeMode = !sftpTreeMode
                        bentoActiveStates[idx] = sftpTreeMode
                        sftpTransitionDir = if (sftpTreeMode) 1 else -1
                        refreshSftpListInPlace(false, null)
                    })
                    floatingPill.addView(makeBentoPillBtn(M3Icon.EYE, initialActive = sftpShowHidden) { idx ->
                        sftpShowHidden = !sftpShowHidden
                        bentoActiveStates[idx] = sftpShowHidden
                        sftpTransitionDir = if (sftpShowHidden) 1 else -1
                        refreshSftpListInPlace(false, null)
                    })
                    floatingPill.addView(makeBentoPillBtn(M3Icon.CREATE_FOLDER, initialActive = false) { _ ->
                        openMaterialCreateItemSheet()
                    })
                    floatingPill.addView(makeBentoPillBtn(M3Icon.UPLOAD, initialActive = true, isPrimaryAccent = true) { _ ->
                        openMaterialImportSheet()
                    })
                }

                if (animateMorph && oldW > 0) {
                    val targetFullW = (bottomBarRow.width - bottomBarRow.paddingLeft - bottomBarRow.paddingRight)
                        .coerceAtLeast(dp(300))
                    // Measure exact compact width of all 5 Bento buttons so the right button is never clipped!
                    floatingPill.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(dp(60), View.MeasureSpec.EXACTLY)
                    )
                    val targetCompactW = floatingPill.measuredWidth.coerceAtLeast(dp(290))
                    val endW = if (sftpSearchActive) targetFullW else targetCompactW
                    floatingPill.alpha = 0.88f
                    val morphAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 280L
                        interpolator = aosp17SpatialInterpolator
                        addUpdateListener { va ->
                            val f = (va.animatedValue as Float).coerceIn(0f, 1f)
                            floatingPill.alpha = 0.88f + 0.12f * f
                            val lp = LinearLayout.LayoutParams(
                                (oldW + (endW - oldW) * f).roundToInt(),
                                dp(60)
                            )
                            floatingPill.layoutParams = lp
                        }
                        addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                floatingPill.alpha = 1f
                                floatingPill.layoutParams = if (sftpSearchActive) {
                                    LinearLayout.LayoutParams(0, dp(60), 1f)
                                } else {
                                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(60))
                                }
                                floatingPill.requestLayout()
                            }
                        })
                    }
                    floatingPill.tag = morphAnim
                    morphAnim.start()
                } else {
                    floatingPill.alpha = 1f
                    floatingPill.layoutParams = if (sftpSearchActive) {
                        LinearLayout.LayoutParams(0, dp(60), 1f)
                    } else {
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(60))
                    }
                }
            }

            populateBottomFloatingPill(false)
            bottomBarRow.addView(floatingPill)
            contentFrame.addView(bottomBarRow)
        }

        // ====================================================================
        // TAB 2: Packages (Separated User Removable vs Core Protected Packages)
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
            val allBadgeTv = TextView(this).apply {
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            }
            allBadge.addView(allBadgeTv)
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
            val sizeBadgeTv = TextView(this).apply {
                setTextColor(COLOR_TEXT_BRIGHT)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            sizeBadge.addView(sizeBadgeTv)
            summaryCard.addView(sizeBadge)

            summaryCard.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
            })

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

            fun populatePackagesInPlace() {
                val installed = parseInstalledDpkgPackagesViaCpp23()
                val totalKb = installed.sumOf { it.sizeKb }
                val userPackages = installed.filter { !it.isCore }
                val corePackages = installed.filter { it.isCore }

                allBadgeTv.text = "${tr("Все", "All")}: ${installed.size}"
                sizeBadgeTv.text = formatSize(totalKb * 1024L)

                pkgCol.removeAllViews()
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

            val ptrPackages = BakaSuPullToRefreshLayout(this, pkgScroll) { finishPtr ->
                Thread {
                    val installed = parseInstalledDpkgPackagesViaCpp23()
                    runOnUiThread {
                        populatePackagesInPlace()
                        finishPtr()
                    }
                }.start()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            outer.addView(ptrPackages)
            populatePackagesInPlace()
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
        animateCenteredDialogAppear(root)
        dlg.show()
    }

    internal fun syncModifierButtons(mask: Int) {
        val ctrl = ctrlLocked || (mask and 1) != 0
        val alt = altLocked || (mask and 2) != 0
        for (btn in modifierButtons) {
            val tag = btn.tag as? String ?: continue
            val isLocked = (tag == "CTRL" && ctrlLocked) || (tag == "ALT" && altLocked)
            val isActive = (tag == "CTRL" && ctrl) || (tag == "ALT" && alt)
            val bg = btn.background as? GradientDrawable
            when {
                isLocked -> {
                    btn.setTextColor(uiOnAccent)
                    bg?.setColor(uiAccent)
                }
                isActive -> {
                    btn.setTextColor(uiOnAccentContainer)
                    bg?.setColor(uiAccentContainer)
                }
                else -> {
                    btn.setTextColor(COLOR_TEXT_BRIGHT)
                    bg?.setColor(uiSurfaceContainer)
                }
            }
        }
        for (updater in extraKeyRowUpdaters) {
            updater()
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

    private var prevSessionCountForTabs: Int = 1

    internal fun closeCurrentSession() {
        val active = nativeGetActiveIndex()
        if (active < 0) {
            finish()
            return
        }
        terminalSurface.stopTextSelection()
        val countBefore = nativeGetSessionCount()
        if (countBefore <= 1) {
            val rem = nativeCloseSession(active)
            if (rem <= 0) finish()
            return
        }

        val closedIdx = active
        val closedWasRoot = nativeIsSessionRoot(closedIdx)
        val closedLabel = (if (closedWasRoot) "# " else "$ ") + (closedIdx + 1)

        val rem = nativeCloseSession(active)
        if (rem <= 0) {
            finish()
        } else {
            rebuildTabs(
                closedTabIndex = closedIdx,
                closedTabWasRoot = closedWasRoot,
                closedTabText = closedLabel
            )
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
        val (mDlg, mSheet) = createExpressiveDialog(360)
        mSheet.addView(TextView(this).apply {
            text = tr("Сессия ${tabIndex + 1}", "Session ${tabIndex + 1}")
            setTextColor(COLOR_TEXT_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setPadding(dp(4), 0, dp(4), dp(12))
        })
        val actions = listOf(
            Triple(M3Icon.COPY, tr("Вставить из буфера", "Paste from clipboard")) { pasteFromClipboard() },
            Triple(M3Icon.FILE, tr("История вывода", "Transcript history")) { terminalSurface.showTranscriptCopyDialog() },
            Triple(M3Icon.REFRESH, tr("Очистить экран (Ctrl+L)", "Clear screen (Ctrl+L)")) { dispatchExtraKeyWithHoldSupport("CTRL+L") },
            Triple(M3Icon.TRASH, tr("Закрыть сессию", "Close session")) { closeCurrentSession() }
        )
        for ((idx, act) in actions.withIndex()) {
            val isDel = (act.first == M3Icon.TRASH)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = segmentedCardBg(idx, actions.size, uiSurfaceLow, 0, 18f, 4f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = if (idx < actions.lastIndex) dp(2) else 0
                }
                attachSmoothPressAnimation(this, 0.98f)
                setOnClickListener {
                    triggerHaptic(0.85f)
                    mDlg.dismiss()
                    act.third()
                }
            }
            row.addView(
                FrameLayout(this).apply {
                    background = tonalBg(if (isDel) COLOR_ROOT_CONTAINER else uiAccentContainer, dp(10))
                    layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                        marginEnd = dp(12)
                    }
                    addView(MaterialIconView(this@TermuxActivity, act.first, if (isDel) COLOR_ROOT else uiAccent, 18.5f, 2.0f))
                }
            )
            row.addView(TextView(this).apply {
                text = act.second
                setTextColor(if (isDel) COLOR_ROOT else COLOR_TEXT_BRIGHT)
                typeface = if (isDel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            })
            mSheet.addView(row)
        }
        mDlg.show()
    }

    internal fun rebuildTabs(
        closedTabIndex: Int = -1,
        closedTabWasRoot: Boolean = false,
        closedTabText: String = ""
    ) {
        if (!::tabSlidingIndicator.isInitialized) return
        (tabStrip.tag as? ValueAnimator)?.cancel()
        tabSlidingIndicator.visibility = View.GONE

        val rawCount = nativeGetSessionCount()
        val count = max(1, rawCount)
        val active = safeClampInt(if (rawCount > 0) nativeGetActiveIndex() else 0, 0, count - 1)
        val fromIdx = safeClampInt(prevActiveSessionIdx, 0, count - 1)
        val countChanged = (count != prevSessionCountForTabs)
        val switchedSession = (fromIdx != active) || (closedTabIndex >= 0) || countChanged
        prevActiveSessionIdx = active
        prevSessionCountForTabs = count

        tabStrip.removeAllViews()
        val tabViews = ArrayList<TextView>(count)
        val tabBgs = ArrayList<GradientDrawable>(count)
        val tabIsRootArr = BooleanArray(count)
        val pressProg = FloatArray(count)
        val pressAnims = arrayOfNulls<ValueAnimator>(count)
        val switchProg = FloatArray(count) { idx -> if (idx == active) 1f else 0f }

        val tabH = dp(31)
        val outerR = tabH * 0.5f
        val innerR = dpF(5f)
        val inactiveTabW = dpF(46f)
        val activeTabW = dpF(60f)
        val deltaW = FloatArray(count)

        var ghostClosingTab: TextView? = null
        var ghostClosingProg: Float = if (closedTabIndex >= 0) 1f else 0f

        fun updateSessionTabsBentoGeometry() {
            for (i in 0 until count) deltaW[i] = 0f
            val holdExpMax = dpF(12f)
            for (i in 0 until count) {
                val p = pressProg[i]
                if (p <= 0.001f) continue
                val e = holdExpMax * p
                when {
                    i > 0 && i < count - 1 -> {
                        deltaW[i] += e
                        deltaW[i - 1] -= e * 0.5f
                        deltaW[i + 1] -= e * 0.5f
                    }
                    i == 0 && count > 1 -> {
                        deltaW[0] += e
                        deltaW[1] -= e
                    }
                    i == count - 1 && count > 1 -> {
                        deltaW[count - 1] += e
                        deltaW[count - 2] -= e
                    }
                    else -> {
                        deltaW[i] += e
                    }
                }
            }

            for (i in 0 until count) {
                val selF = safeClamp(switchProg[i], 0f, 1.15f)
                val pSelf = pressProg[i]
                val isRoot = tabIsRootArr[i]

                val baseW = inactiveTabW + (activeTabW - inactiveTabW) * selF
                val wPx = (baseW + deltaW[i]).roundToInt().coerceAtLeast(dp(32))

                val restL = if (i == 0 && (ghostClosingTab == null || closedTabIndex > 0 || ghostClosingProg < 0.1f)) outerR else innerR
                val restR = if (i == count - 1 && (ghostClosingTab == null || closedTabIndex <= count - 1 || ghostClosingProg < 0.1f)) outerR else innerR
                val pillMorph = safeClamp(max(selF, pSelf), 0f, 1f)
                val curL = restL + (outerR - restL) * pillMorph
                val curR = restR + (outerR - restR) * pillMorph

                val activeFill = if (isRoot) COLOR_ROOT_CONTAINER else uiAccentContainer
                val colorT = safeClamp(max(selF, pSelf * 0.85f), 0f, 1f)
                val fillCol = blendColor(uiSurfaceHigh, activeFill, colorT)
                val activeTxtCol = if (isRoot) COLOR_ROOT else uiOnAccentContainer
                val txtCol = blendColor(COLOR_TEXT_BRIGHT, activeTxtCol, safeClamp(max(selF, pSelf), 0f, 1f))

                val bg = tabBgs[i]
                bg.setColor(fillCol)
                bg.cornerRadii = floatArrayOf(curL, curL, curR, curR, curR, curR, curL, curL)

                val tv = tabViews[i]
                tv.setTextColor(txtCol)
                // Living squash-and-stretch during session switch & press!
                val midSquash = if (selF in 0.03f..0.97f) {
                    kotlin.math.sin(selF.coerceIn(0f, 1f) * Math.PI.toFloat())
                } else 0f
                val pressSquash = safeClamp(pSelf, 0f, 1f)
                tv.scaleY = (1f - 0.11f * midSquash - 0.06f * pressSquash).coerceIn(0.84f, 1.04f)
                val lp = tv.layoutParams as LinearLayout.LayoutParams
                if (lp.width != wPx) {
                    lp.width = wPx
                    tv.layoutParams = lp
                }
            }

            val ghost = ghostClosingTab
            if (ghost != null) {
                val gF = safeClamp(ghostClosingProg, 0f, 1f)
                val gW = (activeTabW * gF).roundToInt().coerceAtLeast(0)
                val gMargin = (dpF(1f) * gF).roundToInt()
                ghost.alpha = safeClamp(gF * 1.15f - 0.15f, 0f, 1f)
                ghost.scaleX = 0.75f + 0.25f * gF
                ghost.scaleY = 0.75f + 0.25f * gF
                val glp = ghost.layoutParams as LinearLayout.LayoutParams
                glp.width = gW
                glp.marginStart = gMargin
                glp.marginEnd = gMargin
                ghost.layoutParams = glp
            }
        }

        for (i in 0 until count) {
            val idx = i
            val isRoot = if (rawCount > 0) nativeIsSessionRoot(i) else false
            tabIsRootArr[i] = isRoot
            val bg = makeConnectedSegmentDrawable(uiSurfaceContainer, outerR, outerR)
            tabBgs.add(bg)

            var downMs = 0L
            fun animateTabHold(holding: Boolean) {
                pressAnims[idx]?.cancel()
                val startP = pressProg[idx]
                val targetP = if (holding) 1f else 0f
                val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = if (holding) 125L else 360L
                    interpolator = LinearInterpolator()
                    addUpdateListener { va ->
                        val t = safeClamp(va.animatedValue as Float, 0f, 1f)
                        val eased = if (holding) {
                            1f - (1f - t) * (1f - t)
                        } else {
                            1f - kotlin.math.exp(-3.3f * t) * kotlin.math.cos(6.2f * t)
                        }
                        pressProg[idx] = safeClamp(startP + (targetP - startP) * eased, 0f, 1.25f)
                        updateSessionTabsBentoGeometry()
                    }
                }
                pressAnims[idx] = anim
                anim.start()
            }

            val releaseTask = Runnable { animateTabHold(false) }

            val tab = TextView(this).apply {
                text = (if (isRoot) "# " else "$ ") + (i + 1)
                gravity = Gravity.CENTER
                setSingleLine(true)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(dp(6), 0, dp(6), 0)
                background = bg
                layoutParams = LinearLayout.LayoutParams(
                    if (i == active) activeTabW.roundToInt() else inactiveTabW.roundToInt(),
                    tabH
                ).apply {
                    setMargins(dp(1), 0, dp(1), 0)
                }

                setOnTouchListener { v, ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.removeCallbacks(releaseTask)
                            downMs = SystemClock.uptimeMillis()
                            triggerHaptic(0.85f)
                            animateTabHold(true)
                            false
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            val held = SystemClock.uptimeMillis() - downMs
                            if (held < 120L) {
                                v.postDelayed(releaseTask, safeClamp((120L - held).toFloat(), 40f, 110f).toLong())
                            } else {
                                releaseTask.run()
                            }
                            false
                        }
                        else -> false
                    }
                }

                setOnClickListener {
                    if (nativeGetSessionCount() > idx) {
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
            tabViews.add(tab)
            tabStrip.addView(tab)
        }

        // If a session was just closed, insert a collapsing ghost tab at closedTabIndex so it shrinks
        // simultaneously while the newly active tab expands in a single smooth animation!
        if (closedTabIndex >= 0) {
            val ghostFill = if (closedTabWasRoot) COLOR_ROOT_CONTAINER else uiAccentContainer
            val ghostTxt = if (closedTabWasRoot) COLOR_ROOT else uiOnAccentContainer
            val ghostBg = makeConnectedSegmentDrawable(ghostFill, outerR, outerR)
            val ghostTv = TextView(this).apply {
                text = closedTabText
                gravity = Gravity.CENTER
                setSingleLine(true)
                typeface = Typeface.DEFAULT_BOLD
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTextColor(ghostTxt)
                setPadding(dp(6), 0, dp(6), 0)
                background = ghostBg
                layoutParams = LinearLayout.LayoutParams(activeTabW.roundToInt(), tabH).apply {
                    setMargins(dp(1), 0, dp(1), 0)
                }
            }
            ghostClosingTab = ghostTv
            val insertIdx = safeClampInt(closedTabIndex, 0, tabStrip.childCount)
            tabStrip.addView(ghostTv, insertIdx)
        }

        if (switchedSession && active in 0 until count) {
            val animateOldActive = (closedTabIndex < 0 && fromIdx in 0 until count && fromIdx != active)
            if (animateOldActive) {
                switchProg[fromIdx] = 1f
            }
            switchProg[active] = 0f
            updateSessionTabsBentoGeometry()
            val switchAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 320L
                interpolator = LinearInterpolator()
                addUpdateListener { va ->
                    val t = safeClamp(va.animatedValue as Float, 0f, 1f)
                    val spring = 1f - kotlin.math.exp(-3.5f * t) * kotlin.math.cos(5.6f * t)
                    val smoothCollapse = (1f - t) * (1f - t)
                    switchProg[active] = safeClamp(spring, 0f, 1.14f)
                    if (animateOldActive) {
                        switchProg[fromIdx] = safeClamp(1f - spring, 0f, 1f)
                    }
                    if (closedTabIndex >= 0) {
                        ghostClosingProg = smoothCollapse
                    }
                    updateSessionTabsBentoGeometry()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        val g = ghostClosingTab
                        if (g != null) {
                            tabStrip.removeView(g)
                            ghostClosingTab = null
                            ghostClosingProg = 0f
                            updateSessionTabsBentoGeometry()
                        }
                    }
                })
            }
            tabStrip.tag = switchAnim
            switchAnim.start()
        } else {
            updateSessionTabsBentoGeometry()
        }

        tabStrip.post {
            val targetView = tabViews.getOrNull(active) ?: return@post
            val scrollView = (tabStrip.parent as? View)?.parent as? HorizontalScrollView
            if (scrollView != null) {
                val targetScrollX = (targetView.left - (scrollView.width - targetView.width) / 2).coerceAtLeast(0)
                scrollView.smoothScrollTo(targetScrollX, 0)
            }
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
        private var lastObservedRootState: Boolean = false

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
                    val newOffset = safeClampInt(scrollOffset + delta, 0, max(0, maxSb))
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
                            // Allow negative scrollOffset down to -(rows - 2) so the last line can be scrolled all the way to the top!
                            val minSb = -max(0, rows - 2)
                            scrollOffset = safeClampInt(scrollOffset - deltaRows, minSb, max(0, maxSb))
                            // Auto-hide extra keys bar when scrolling up into history, restore when scrolling back down
                            if (deltaRows < 0 && scrollOffset > 1) {
                                setExtraKeysScrollHidden(true)
                            } else if (deltaRows > 0 || scrollOffset <= 0) {
                                setExtraKeysScrollHidden(false)
                            }
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

        private var unoccludedHeightPx: Int = 0

        internal fun resetScrollAndBlink() {
            scrollOffset = 0
            if (isExtraKeysScrollHidden) {
                setExtraKeysScrollHidden(false)
            }
            scheduleBlinkLoop()
            invalidateAllRows()
        }

        internal fun invalidateAllRows() {
            rowNodesValid?.fill(false)
            postInvalidateOnAnimation()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (!isImeCurrentlyOpen || h > unoccludedHeightPx || w != oldw) {
                unoccludedHeightPx = max(h, unoccludedHeightPx)
            }
            syncSize()
        }

        internal fun syncSize(forceResize: Boolean = false) {
            val w = width
            val h = height
            if (w <= 0 || h <= 0) return
            if (!isImeCurrentlyOpen || h > unoccludedHeightPx) {
                unoccludedHeightPx = h
            }
            val effectiveH = max(h, unoccludedHeightPx)
            val newCols = max(10, (w / cellWidth).toInt())
            val newRows = max(4, (effectiveH / cellHeight).toInt())
            val changed = (newCols != cols || newRows != rows)
            cols = newCols
            rows = newRows
            if (changed || rowNodes == null || rowNodes?.size != rows) {
                rowNodes = arrayOfNulls(rows)
                rowNodesValid = BooleanArray(rows)
                nativeResizeActive(rows, cols, cellWidth.roundToInt(), cellHeight.roundToInt())
            } else if (forceResize) {
                rowNodesValid?.fill(false)
                nativeResizeActive(rows, cols, cellWidth.roundToInt(), cellHeight.roundToInt())
            }
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
                val curIdx = nativeGetActiveIndex()
                if (curIdx >= 0) {
                    val nowRoot = nativeIsSessionRoot(curIdx)
                    if (nowRoot != lastObservedRootState) {
                        lastObservedRootState = nowRoot
                        post { rebuildTabs() }
                    }
                }
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

            val cursorStateForPan = nativeGetCursorState()
            val curCursorRow = if (cursorStateForPan >= 0) (cursorStateForPan ushr 16) and 0x1FFF else 0
            val overscrollUpPx = if (scrollOffset < 0) (-scrollOffset) * cellHeight else 0f
            val imeOccludedPanPx = if (scrollOffset == 0 && height > 0 && (curCursorRow + 1) * cellHeight > height) {
                (curCursorRow + 1) * cellHeight - height + dpF(4f)
            } else {
                0f
            }
            val totalPanUpPx = overscrollUpPx + imeOccludedPanPx

            canvas.save()
            if (totalPanUpPx != 0f) {
                canvas.translate(0f, -totalPanUpPx)
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
            canvas.restore()

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
            if (scrollOffset <= 0 && !selecting) {
                val cursorState = nativeGetCursorState()
                if (cursorState >= 0) {
                    val rawShape = (cursorState ushr 29) and 3
                    val shape = if (cursorShapePref == 3 && rawShape == 0) 3 else if (rawShape in 1..2) rawShape else cursorShapePref
                    val cRow = (cursorState ushr 16) and 0x1FFF
                    val cCol = cursorState and 0xFFFF
                    if (cRow in 0 until rows && cCol in 0 until cols) {
                        val targetX = cCol * cellWidth
                        val targetY = cRow * cellHeight - totalPanUpPx
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

            val packed = nativePackRowRender(max(0, scrollOffset), r, bgRunsBuf, glyphRunsBuf)
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
                InputType.TYPE_TEXT_VARIATION_FILTER or
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
                val s = safeClampInt(Selection.getSelectionStart(localBuffer), 0, max(0, curLen))
                val e = safeClampInt(Selection.getSelectionEnd(localBuffer), 0, max(0, curLen))
                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.updateSelection(this@TerminalSurface, s, e, -1, -1)
                if (extractedTextRequest != null) {
                    imm?.updateExtractedText(this@TerminalSurface, extractedTextToken, buildExtractedText())
                }
            }

            private fun buildExtractedText(): ExtractedText {
                val curLen = localBuffer.length
                val s = safeClampInt(Selection.getSelectionStart(localBuffer), 0, max(0, curLen))
                val e = safeClampInt(Selection.getSelectionEnd(localBuffer), 0, max(0, curLen))
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
                val delCount = safeClampInt(terminalSentLine.length - common, 0, max(0, maxSafeDel))
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
                val s = safeClampInt(Selection.getSelectionStart(localBuffer), 0, max(0, curLen))
                val e = safeClampInt(Selection.getSelectionEnd(localBuffer), 0, max(0, curLen))
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
                val s = safeClampInt(start, 0, max(0, curLen))
                val e = safeClampInt(end, 0, max(0, curLen))
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
                    BaseInputConnection.removeComposingSpans(localBuffer)
                    applyLocalBufferDiffToPty()
                }
                rearmLockedModifiersIfNeeded()
                resetScrollAndBlink()
                return true
            }

            override fun setComposingRegion(start: Int, end: Int): Boolean {
                val curLen = localBuffer.length
                return super.setComposingRegion(safeClampInt(start, 0, max(0, curLen)), safeClampInt(end, 0, max(0, curLen)))
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
                    val s = safeClampInt(min(start, end), 0, max(0, curLen))
                    val e = safeClampInt(max(start, end), 0, max(0, curLen))
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
        @JvmStatic external fun nativeExecLocalRootCmd(shellCmd: String): String
        @JvmStatic external fun nativeListLocalDir(dirPath: String, showHidden: Boolean, searchQuery: String): String
        @JvmStatic external fun nativeListRemoteSftpDir(
            host: String,
            port: Int,
            user: String,
            authType: Int,
            secret: String,
            jumpHost: String,
            remoteDir: String,
            showHidden: Boolean,
            searchQuery: String
        ): String
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
        @JvmStatic
        external fun nativeDiscoverCustomFonts(): String

        @JvmStatic
        external fun nativeComputeM3LoadingPolygon(
            phase: Float,
            pullProgress: Float,
            isRefreshing: Boolean,
            radiusPx: Float
        ): FloatArray

        @JvmStatic
        external fun nativeStageKeyOrParseFileZillaXml(
            input: String,
            isFileZillaXml: Boolean
        ): String
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
