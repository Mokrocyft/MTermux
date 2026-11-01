package com.termuxx.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-File Frontend for Termuxx (com.termuxx).
 *
 * Features:
 * 1. Native Android System Text Selection (OS teardrop handles, OS magnifier, OS floating ActionMode menu).
 * 2. Minimalist Customizable Extra Keys Bar (1-row default "ESC,TAB,CTRL,ALT,◀,▼,▲,▶" + interactive customizer).
 * 3. Modern 2026 TUI Renderer:
 *    - Procedural pixel-perfect Box-Drawing (U+2500..U+257F) & Block Elements (U+2580..U+259F)
 *    - DECSCUSR cursor shapes (Block, Underline, Vertical Beam)
 *    - Wavy undercurl (4:3) & double underline (4:2) for Neovim/Helix LSP diagnostics
 *    - C++23 dynamic logical-line reflow on pinch-to-zoom
 */
public final class TermuxActivity extends Activity {

    static {
        System.loadLibrary("termux-core");
    }

    private static final String PREFS_NAME     = "termuxx_prefs";
    private static final String PREF_ROW1_KEYS = "extra_keys_row1";
    private static final String PREF_ROW2_KEYS = "extra_keys_row2";
    private static final String PREF_KEY_DP    = "extra_keys_height_dp";

    // Clean 1-row default with only the 8 essential keys
    private static final String DEFAULT_ROW1 = "ESC, TAB, CTRL, ALT, ◀, ▼, ▲, ▶";
    private static final String DEFAULT_ROW2 = "";

    private static final int AMOLED_BLACK = 0xFF000000;
    private static final int KEY_SURFACE  = 0xFF0A0A0A;
    private static final int KEY_PRESSED  = 0xFF1A1A1A;
    private static final int KEY_ARMED    = 0xFF262626;
    private static final int TEXT_BRIGHT  = 0xFFE5E9F0;
    private static final int TEXT_MUTED   = 0xFF6E7681;

    private static final int FLAG_BOLD             = 1 << 0;
    private static final int FLAG_ITALIC           = 1 << 2;
    private static final int FLAG_UNDERLINE        = 1 << 3;
    private static final int FLAG_STRIKETHROUGH    = 1 << 7;
    private static final int FLAG_UNDERCURL        = 1 << 10;
    private static final int FLAG_DOUBLE_UNDERLINE = 1 << 11;

    private static final int MODE_ALT_SCREEN     = 1 << 2;
    private static final int MODE_MOUSE_TRACKING = 1 << 3;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Typeface mJetBrainsMono;
    private LinearLayout mTabStrip;
    private TextView mStatusLabel;
    private TerminalSurface mTerminalSurface;
    private TextView mNativeSelectionView;
    private GridLayout mExtraKeysBar;
    private TextView mCtrlKeyView;
    private TextView mAltKeyView;
    private boolean mBootstrapDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureAmoledWindow();
        try {
            mJetBrainsMono = Typeface.createFromAsset(getAssets(), "fonts/JetBrainsMono.ttf");
        } catch (Throwable ignored) {
            mJetBrainsMono = Typeface.MONOSPACE;
        }
        buildMinimalistAmoledLayout();
        startForegroundService(new Intent(this, KeepAliveService.class));
    }

    private void configureAmoledWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow();
        if (w == null) return;
        w.setStatusBarColor(AMOLED_BLACK);
        w.setNavigationBarColor(AMOLED_BLACK);
        w.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        if (Build.VERSION.SDK_INT >= 30) {
            Display d = getDisplay();
            if (d != null) {
                Display.Mode best = null;
                float maxHz = 60f;
                for (Display.Mode m : d.getSupportedModes()) {
                    if (m.getRefreshRate() > maxHz) {
                        maxHz = m.getRefreshRate();
                        best = m;
                    }
                }
                if (best != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.preferredDisplayModeId = best.getModeId();
                    w.setAttributes(lp);
                }
            }
        }
    }

    private void buildMinimalistAmoledLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AMOLED_BLACK);

        // 1. Minimalist Pure-Black Header (28dp)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(AMOLED_BLACK);
        header.setPadding(dp(8), dp(2), dp(8), dp(2));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        mTabStrip = new LinearLayout(this);
        mTabStrip.setOrientation(LinearLayout.HORIZONTAL);
        mTabStrip.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(mTabStrip);
        header.addView(scroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mStatusLabel = new TextView(this);
        mStatusLabel.setTypeface(mJetBrainsMono);
        mStatusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        mStatusLabel.setTextColor(TEXT_MUTED);
        mStatusLabel.setPadding(dp(6), 0, dp(6), 0);
        header.addView(mStatusLabel);

        header.addView(makeHeaderButton("+", v -> createAndSwitchSession(false), null));
        header.addView(makeHeaderButton("#", v -> createAndSwitchSession(true), null));
        header.addView(makeHeaderButton("cp", v -> {
            String text = nativeGetTranscriptText();
            if (text != null && !text.isEmpty()) {
                copyTextToClipboard(text);
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
            }
        }, null));
        header.addView(makeHeaderButton("ps", v -> pasteFromClipboard(), null));
        header.addView(makeHeaderButton("kb", v -> {
            boolean vis = mExtraKeysBar.getVisibility() == View.VISIBLE;
            mExtraKeysBar.setVisibility(vis ? View.GONE : View.VISIBLE);
        }, v -> {
            showExtraKeysCustomizationDialog();
            return true;
        }));

        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        View hairline = new View(this);
        hairline.setBackgroundColor(0xFF121212);
        root.addView(hairline, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // 2. Viewport Container: 165Hz TerminalSurface + Native Android System Text Selection Overlay
        FrameLayout viewportFrame = new FrameLayout(this);
        viewportFrame.setBackgroundColor(AMOLED_BLACK);

        mTerminalSurface = new TerminalSurface(this);
        viewportFrame.addView(mTerminalSurface, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        mNativeSelectionView = createNativeSelectionOverlay();
        viewportFrame.addView(mNativeSelectionView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root.addView(viewportFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 3. Minimalist Customizable Extra Keys Bar
        mExtraKeysBar = new GridLayout(this);
        mExtraKeysBar.setBackgroundColor(AMOLED_BLACK);
        rebuildExtraKeysBar();
        root.addView(mExtraKeysBar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }

    /**
     * Creates a native Android TextView overlay with setTextIsSelectable(true).
     * When triggered by long-press, Android 17 / ColorOS 17 spawns its genuine native system
     * teardrop handles, native magnifier loupe, and native floating ActionMode menu!
     */
    private TextView createNativeSelectionOverlay() {
        final TextView tv = new TextView(this);
        tv.setBackgroundColor(0xEE000000);
        tv.setTextColor(TEXT_BRIGHT);
        tv.setHighlightColor(0x663B82F6);
        tv.setTypeface(mJetBrainsMono);
        tv.setFontFeatureSettings("'liga' 0, 'calt' 0");
        tv.setIncludeFontPadding(false);
        tv.setGravity(Gravity.TOP | Gravity.LEFT);
        tv.setPadding(0, 0, 0, 0);
        tv.setTextIsSelectable(true);
        tv.setVisibility(View.GONE);

        tv.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            private static final int MENU_ID_PASTE = 9001;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(0, MENU_ID_PASTE, 99, "Paste");
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == MENU_ID_PASTE) {
                    mode.finish();
                    exitNativeSelectionMode();
                    pasteFromClipboard();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mHandler.post(() -> exitNativeSelectionMode());
            }
        });

        tv.setOnClickListener(v -> exitNativeSelectionMode());
        return tv;
    }

    private void enterNativeSelectionMode(float touchX, float touchY) {
        String viewportText = nativeGetViewportSelectionText(mTerminalSurface.mScrollOffset);
        if (viewportText == null || viewportText.isEmpty()) return;

        mNativeSelectionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTerminalSurface.mTextSizePx);
        mNativeSelectionView.setText(viewportText, TextView.BufferType.SPANNABLE);
        mNativeSelectionView.setVisibility(View.VISIBLE);
        mNativeSelectionView.requestFocus();

        // Dispatch synthetic down + long-press to trigger Android's native Editor selection handles & floating toolbar
        mNativeSelectionView.post(() -> {
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, touchX, touchY, 0);
            mNativeSelectionView.onTouchEvent(down);
            down.recycle();
            if (!mNativeSelectionView.performLongClick()) {
                CharSequence cs = mNativeSelectionView.getText();
                if (cs instanceof Spannable && cs.length() > 0) {
                    Selection.setSelection((Spannable) cs, 0, Math.min(cs.length(), 12));
                }
            }
        });
    }

    private void exitNativeSelectionMode() {
        if (mNativeSelectionView != null && mNativeSelectionView.getVisibility() == View.VISIBLE) {
            mNativeSelectionView.setVisibility(View.GONE);
            mTerminalSurface.requestFocus();
        }
    }

    private TextView makeHeaderButton(String label, View.OnClickListener click, View.OnLongClickListener longClick) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTypeface(mJetBrainsMono, Typeface.NORMAL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTextColor(TEXT_MUTED);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(2), dp(8), dp(2));
        tv.setOnClickListener(click);
        if (longClick != null) tv.setOnLongClickListener(longClick);
        return tv;
    }

    private static List<String> parseKeyList(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null) return out;
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private void rebuildExtraKeysBar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String row1Csv = prefs.getString(PREF_ROW1_KEYS, DEFAULT_ROW1);
        String row2Csv = prefs.getString(PREF_ROW2_KEYS, DEFAULT_ROW2);
        int keyHeightDp = prefs.getInt(PREF_KEY_DP, 30);

        List<String> row1 = parseKeyList(row1Csv);
        List<String> row2 = parseKeyList(row2Csv);
        if (row1.isEmpty()) row1 = parseKeyList(DEFAULT_ROW1);

        mExtraKeysBar.removeAllViews();
        mCtrlKeyView = null;
        mAltKeyView = null;

        int numRows = row2.isEmpty() ? 1 : 2;
        int numCols = Math.max(row1.size(), row2.size());
        mExtraKeysBar.setRowCount(numRows);
        mExtraKeysBar.setColumnCount(Math.max(1, numCols));
        int pad = dp(2);
        mExtraKeysBar.setPadding(pad, pad, pad, pad);

        addExtraKeyRow(0, row1, numCols, keyHeightDp);
        if (!row2.isEmpty()) {
            addExtraKeyRow(1, row2, numCols, keyHeightDp);
        }
    }

    private void addExtraKeyRow(int rowIndex, List<String> keys, int totalCols, int keyHeightDp) {
        for (int c = 0; c < keys.size(); ++c) {
            final String label = keys.get(c);
            final TextView key = new TextView(this);
            key.setText(label);
            key.setGravity(Gravity.CENTER);
            key.setTypeface(mJetBrainsMono, Typeface.NORMAL);
            key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            key.setTextColor(0xFFCCCCCC);
            key.setBackground(makeKeyBg(KEY_SURFACE));

            if ("CTRL".equals(label)) mCtrlKeyView = key;
            if ("ALT".equals(label)) mAltKeyView = key;

            final boolean repeatable = "▲".equals(label) || "▼".equals(label) || "◀".equals(label) || "▶".equals(label)
                || "UP".equals(label) || "DOWN".equals(label) || "LEFT".equals(label) || "RIGHT".equals(label)
                || "PGUP".equals(label) || "PGDN".equals(label) || "BKSP".equals(label) || "DEL".equals(label);

            final Runnable repeatTask = new Runnable() {
                @Override
                public void run() {
                    int mask = nativeDispatchCustomKey(label);
                    syncModifierVisuals(mask);
                    mHandler.postDelayed(this, 55);
                }
            };

            key.setOnTouchListener((v, ev) -> {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        key.setBackground(makeKeyBg(KEY_PRESSED));
                        int mask = nativeDispatchCustomKey(label);
                        syncModifierVisuals(mask);
                        if (repeatable) mHandler.postDelayed(repeatTask, 320);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mHandler.removeCallbacks(repeatTask);
                        syncModifierVisuals(nativeGetModifierMask());
                        if (!"CTRL".equals(label) && !"ALT".equals(label)) {
                            key.setBackground(makeKeyBg(KEY_SURFACE));
                        }
                        return true;
                    default:
                        return false;
                }
            });

            key.setOnLongClickListener(v -> {
                showExtraKeysCustomizationDialog();
                return true;
            });

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(rowIndex, 1, 1f),
                GridLayout.spec(c, 1, 1f)
            );
            lp.width = 0;
            lp.height = dp(keyHeightDp);
            lp.setMargins(dp(1), dp(1), dp(1), dp(1));
            mExtraKeysBar.addView(key, lp);
        }
    }

    /**
     * Interactive Dark AMOLED Extra Keys Customizer Dialog.
     * Opened by long-pressing "kb" in the top bar or long-pressing any non-repeating key.
     */
    private void showExtraKeysCustomizationDialog() {
        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(0xFF0A0A0A);
        int pad = dp(16);
        container.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Customize Extra Keys");
        title.setTypeface(mJetBrainsMono, Typeface.BOLD);
        title.setTextColor(TEXT_BRIGHT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        container.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Comma-separated keys (ESC, TAB, CTRL, ALT, ◀, ▼, ▲, ▶, -, /, |, ~, HOME, END, PGUP, PGDN, BKSP, DEL, F1..F12, CTRL+C). Leave Row 2 empty for 1-row bar.");
        hint.setTypeface(mJetBrainsMono);
        hint.setTextColor(TEXT_MUTED);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        hint.setPadding(0, dp(6), 0, dp(10));
        container.addView(hint);

        final EditText row1Input = new EditText(this);
        row1Input.setTypeface(mJetBrainsMono);
        row1Input.setTextColor(TEXT_BRIGHT);
        row1Input.setHintTextColor(TEXT_MUTED);
        row1Input.setHint("Row 1 keys");
        row1Input.setText(prefs.getString(PREF_ROW1_KEYS, DEFAULT_ROW1));
        container.addView(row1Input);

        final EditText row2Input = new EditText(this);
        row2Input.setTypeface(mJetBrainsMono);
        row2Input.setTextColor(TEXT_BRIGHT);
        row2Input.setHintTextColor(TEXT_MUTED);
        row2Input.setHint("Row 2 keys (optional)");
        row2Input.setText(prefs.getString(PREF_ROW2_KEYS, DEFAULT_ROW2));
        container.addView(row2Input);

        // Quick Preset Buttons
        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setPadding(0, dp(8), 0, 0);

        presetsRow.addView(makeHeaderButton("[Minimal 1R]", v -> {
            row1Input.setText("ESC, TAB, CTRL, ALT, ◀, ▼, ▲, ▶");
            row2Input.setText("");
        }, null));
        presetsRow.addView(makeHeaderButton("[Dev 1R]", v -> {
            row1Input.setText("ESC, TAB, CTRL, ALT, -, /, |, ◀, ▼, ▲, ▶");
            row2Input.setText("");
        }, null));
        presetsRow.addView(makeHeaderButton("[Full 2R]", v -> {
            row1Input.setText("ESC, TAB, CTRL, ALT, -, /, |, ▲, END");
            row2Input.setText("~, HOME, PGUP, PGDN, $, :, ◀, ▼, ▶");
        }, null));
        container.addView(presetsRow);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(container)
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit()
                    .putString(PREF_ROW1_KEYS, row1Input.getText().toString().trim())
                    .putString(PREF_ROW2_KEYS, row2Input.getText().toString().trim())
                    .apply();
                rebuildExtraKeysBar();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void syncModifierVisuals(int modMask) {
        boolean ctrl = (modMask & 1) != 0;
        boolean alt  = (modMask & 2) != 0;
        if (mCtrlKeyView != null) {
            mCtrlKeyView.setBackground(makeKeyBg(ctrl ? KEY_ARMED : KEY_SURFACE));
            mCtrlKeyView.setTextColor(ctrl ? Color.WHITE : 0xFFCCCCCC);
        }
        if (mAltKeyView != null) {
            mAltKeyView.setBackground(makeKeyBg(alt ? KEY_ARMED : KEY_SURFACE));
            mAltKeyView.setTextColor(alt ? Color.WHITE : 0xFFCCCCCC);
        }
    }

    private void createAndSwitchSession(boolean asRoot) {
        if (!mBootstrapDone) return;
        exitNativeSelectionMode();
        nativeNewSession(
            asRoot,
            mTerminalSurface.mVisibleRows,
            mTerminalSurface.mVisibleCols,
            (int) mTerminalSurface.mCellWidthPx,
            mTerminalSurface.mCellHeightPx
        );
        mTerminalSurface.resetScrollAndNodes();
        rebuildTabStrip();
    }

    private void closeSessionAt(int index) {
        exitNativeSelectionMode();
        int remaining = nativeCloseSession(index);
        if (remaining <= 0) {
            stopService(new Intent(this, KeepAliveService.class));
            finish();
        } else {
            mTerminalSurface.resetScrollAndNodes();
            rebuildTabStrip();
        }
    }

    private void rebuildTabStrip() {
        mTabStrip.removeAllViews();
        int count = nativeGetSessionCount();
        int active = nativeGetActiveIndex();
        for (int i = 0; i < count; ++i) {
            final int idx = i;
            boolean isRoot = nativeIsSessionRoot(i);
            boolean isCurrent = (i == active);

            TextView tab = new TextView(this);
            tab.setText((i + 1) + (isRoot ? "·su" : "·sh"));
            tab.setTypeface(mJetBrainsMono, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            tab.setTextColor(isCurrent ? TEXT_BRIGHT : TEXT_MUTED);
            tab.setPadding(dp(8), dp(2), dp(8), dp(2));

            tab.setOnClickListener(v -> {
                exitNativeSelectionMode();
                nativeSwitchSession(idx);
                mTerminalSurface.resetScrollAndNodes();
                rebuildTabStrip();
            });
            tab.setOnLongClickListener(v -> {
                closeSessionAt(idx);
                return true;
            });
            mTabStrip.addView(tab);
        }
    }

    void copyTextToClipboard(String text) {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Termuxx", text));
    }

    void pasteFromClipboard() {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (text != null && text.length() > 0) {
                nativeSendText(text.toString(), true);
                syncModifierVisuals(nativeGetModifierMask());
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            boolean vis = mExtraKeysBar.getVisibility() == View.VISIBLE;
            mExtraKeysBar.setVisibility(vis ? View.GONE : View.VISIBLE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private GradientDrawable makeKeyBg(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(4));
        return gd;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }

    // ========================================================================
    // 165Hz Hardware RenderNode Surface + Procedural Box/Block/Undercurl TUI
    // ========================================================================
    private final class TerminalSurface extends View implements Choreographer.FrameCallback {

        private final Paint mBgPaint = new Paint();
        private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final Paint mBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mCursorPaint = new Paint();
        private final Paint mScrollbarPaint = new Paint();

        private final OverScroller mScroller;
        private final GestureDetector mGestureDetector;
        private final ScaleGestureDetector mScaleDetector;

        float mTextSizePx;
        float mCellWidthPx;
        int mCellHeightPx;
        int mFontAscentPx;
        int mVisibleRows = 45;
        int mVisibleCols = 72;
        int mScrollOffset = 0;

        private int mLastRenderedScrollOffset = -1;
        private float mScrollRemainder = 0f;
        private boolean mAttached = false;

        private RenderNode[] mRowNodes = new RenderNode[0];
        private boolean[] mRowNodeValid = new boolean[0];
        private int[] mBgRuns = new int[512 * 3];
        private int[] mGlyphs = new int[512 * 4];
        private final char[] mGlyphChars = new char[2];

        TerminalSurface(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setDefaultFocusHighlightEnabled(false);

            mBgPaint.setStyle(Paint.Style.FILL);
            mBgPaint.setAntiAlias(false);

            mTextPaint.setTypeface(mJetBrainsMono);
            mTextPaint.setFontFeatureSettings("'liga' 0, 'calt' 0");

            mBoxPaint.setStyle(Paint.Style.STROKE);
            mBoxPaint.setStrokeCap(Paint.Cap.SQUARE);

            mCursorPaint.setStyle(Paint.Style.FILL);
            mCursorPaint.setColor(0xFFD8DEE9);

            mScrollbarPaint.setStyle(Paint.Style.FILL);
            mScrollbarPaint.setColor(0x556E7681);

            float defaultSp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 11.0f, context.getResources().getDisplayMetrics()
            );
            updateFontMetrics(defaultSp);

            mScroller = new OverScroller(context);

            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    if (!mScroller.isFinished()) mScroller.abortAnimation();
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    requestFocus();
                    if ((nativeGetModesMask() & MODE_MOUSE_TRACKING) != 0) {
                        int col = (int) (e.getX() / mCellWidthPx) + 1;
                        int row = (int) (e.getY() / mCellHeightPx) + 1;
                        nativeSendMouse(0, col, row, true);
                        nativeSendMouse(0, col, row, false);
                    }
                    InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
                    if (imm != null) imm.showSoftInput(TerminalSurface.this, InputMethodManager.SHOW_IMPLICIT);
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    enterNativeSelectionMode(e.getX(), e.getY());
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    int modes = nativeGetModesMask();
                    if ((modes & MODE_ALT_SCREEN) != 0 && (modes & MODE_MOUSE_TRACKING) != 0) {
                        mScrollRemainder += distanceY;
                        int lines = (int) (mScrollRemainder / mCellHeightPx);
                        if (lines != 0) {
                            mScrollRemainder -= lines * mCellHeightPx;
                            int col = (int) (e2.getX() / mCellWidthPx) + 1;
                            int row = (int) (e2.getY() / mCellHeightPx) + 1;
                            int btn = lines < 0 ? 64 : 65;
                            for (int i = 0, n = Math.min(5, Math.abs(lines)); i < n; ++i) {
                                nativeSendMouse(btn, col, row, true);
                            }
                        }
                        return true;
                    }

                    mScrollRemainder += distanceY;
                    int deltaRows = (int) (mScrollRemainder / mCellHeightPx);
                    if (deltaRows != 0) {
                        mScrollRemainder -= deltaRows * mCellHeightPx;
                        int maxSb = nativeGetActiveScrollback();
                        int next = Math.max(0, Math.min(maxSb, mScrollOffset - deltaRows));
                        if (next != mScrollOffset) {
                            mScrollOffset = next;
                            invalidateRowCache();
                            postInvalidateOnAnimation();
                        }
                    }
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                    int maxSb = nativeGetActiveScrollback();
                    if (maxSb <= 0) return true;
                    mScroller.abortAnimation();
                    mScroller.fling(0, mScrollOffset * mCellHeightPx, 0, (int) vy, 0, 0, 0, maxSb * mCellHeightPx);
                    postInvalidateOnAnimation();
                    return true;
                }
            });

            mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float newSize = Math.max(14f, Math.min(72f, mTextSizePx * detector.getScaleFactor()));
                    if (Math.abs(newSize - mTextSizePx) >= 0.4f) {
                        updateFontMetrics(newSize);
                        recalculateGrid(getWidth(), getHeight());
                        invalidateRowCache();
                        postInvalidateOnAnimation();
                    }
                    return true;
                }
            });
        }

        private void updateFontMetrics(float textSizePx) {
            mTextSizePx = textSizePx;
            mTextPaint.setTextSize(textSizePx);
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            mCellWidthPx = Math.max(4f, mTextPaint.measureText("aaaaaaaaaaMMMMMMMMMM0000000000") / 30f);
            mCellHeightPx = Math.max(8, (int) Math.ceil(fm.descent - fm.ascent + fm.leading));
            mFontAscentPx = (int) Math.ceil(-fm.ascent);
            mBoxPaint.setStrokeWidth(Math.max(1.5f, mCellWidthPx * 0.12f));
        }

        void resetScrollAndNodes() {
            mScrollOffset = 0;
            mLastRenderedScrollOffset = -1;
            recalculateGrid(getWidth(), getHeight());
            invalidateRowCache();
            postInvalidateOnAnimation();
        }

        void invalidateRowCache() {
            for (int i = 0; i < mRowNodeValid.length; ++i) mRowNodeValid[i] = false;
        }

        private void recalculateGrid(int widthPx, int heightPx) {
            if (widthPx <= 0 || heightPx <= 0) return;
            int cols = Math.max(8, (int) (widthPx / mCellWidthPx));
            int rows = Math.max(4, heightPx / mCellHeightPx);
            mVisibleCols = cols;
            mVisibleRows = rows;

            if (mRowNodes.length != rows) {
                mRowNodes = new RenderNode[rows];
                mRowNodeValid = new boolean[rows];
                for (int r = 0; r < rows; ++r) mRowNodes[r] = new RenderNode("R_" + r);
            } else {
                invalidateRowCache();
            }
            if (mGlyphs.length < cols * 4) {
                mBgRuns = new int[(cols + 32) * 3];
                mGlyphs = new int[(cols + 32) * 4];
            }
            nativeResizeActive(rows, cols, (int) mCellWidthPx, mCellHeightPx);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mAttached = true;
            Choreographer.getInstance().postFrameCallback(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            mAttached = false;
            Choreographer.getInstance().removeFrameCallback(this);
            if (!mScroller.isFinished()) mScroller.abortAnimation();
            super.onDetachedFromWindow();
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            if (!mAttached) return;
            if (!mBootstrapDone) {
                int st = nativeInitAndPollBootstrap(getApplicationInfo().nativeLibraryDir);
                if (st == 1) {
                    mStatusLabel.setText("init...");
                } else if (st == 2) {
                    mBootstrapDone = true;
                    mStatusLabel.setText("");
                    if (nativeGetSessionCount() == 0) {
                        createAndSwitchSession(false);
                    } else {
                        rebuildTabStrip();
                    }
                }
            }
            String osc52 = nativeConsumeOsc52Clipboard();
            if (osc52 != null && !osc52.isEmpty()) {
                copyTextToClipboard(osc52);
            }
            if (nativeConsumeFrameDirty()) {
                if (mScrollOffset != 0) {
                    mScrollOffset = 0;
                    invalidateRowCache();
                }
                invalidate();
            }
            Choreographer.getInstance().postFrameCallback(this);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            recalculateGrid(w, h);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            mScaleDetector.onTouchEvent(event);
            if (!mScaleDetector.isInProgress()) mGestureDetector.onTouchEvent(event);
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(AMOLED_BLACK);
            if (!mBootstrapDone || nativeGetSessionCount() == 0) return;

            if (mScroller.computeScrollOffset()) {
                int maxSb = nativeGetActiveScrollback();
                int target = Math.max(0, Math.min(maxSb, mScroller.getCurrY() / mCellHeightPx));
                if (target != mScrollOffset) {
                    mScrollOffset = target;
                    invalidateRowCache();
                }
                postInvalidateOnAnimation();
            }

            if (mScrollOffset != mLastRenderedScrollOffset) {
                invalidateRowCache();
                mLastRenderedScrollOffset = mScrollOffset;
            }

            int viewWidth = getWidth();
            boolean hw = canvas.isHardwareAccelerated();

            for (int r = 0; r < mVisibleRows; ++r) {
                int topPx = r * mCellHeightPx;
                int bottomPx = topPx + mCellHeightPx;
                boolean dirty = (mScrollOffset == 0) ? nativeIsRowDirty(r) : !mRowNodeValid[r];

                if (hw && r < mRowNodes.length) {
                    RenderNode node = mRowNodes[r];
                    if (!mRowNodeValid[r] || dirty || !node.hasDisplayList()) {
                        node.setPosition(0, topPx, viewWidth, bottomPx);
                        RecordingCanvas rc = node.beginRecording(viewWidth, mCellHeightPx);
                        try {
                            drawPackedRow(rc, r, 0);
                        } finally {
                            node.endRecording();
                        }
                        mRowNodeValid[r] = true;
                    }
                    canvas.drawRenderNode(node);
                } else {
                    drawPackedRow(canvas, r, topPx);
                }
            }

            if (mScrollOffset == 0) nativeClearDirtyFlags();

            // DECSCUSR Cursor Overlay (0 = Block, 1 = Underline, 2 = Vertical Beam)
            if (mScrollOffset == 0) {
                int cursorState = nativeGetCursorState();
                if (cursorState >= 0) {
                    int shape  = (cursorState >>> 29) & 3;
                    int curRow = (cursorState >>> 16) & 0x1FFF;
                    int curCol = cursorState & 0xFFFF;
                    if (curRow < mVisibleRows && curCol < mVisibleCols) {
                        float left = curCol * mCellWidthPx;
                        float top  = curRow * mCellHeightPx;
                        if (shape == 1) { // Underline
                            float h = Math.max(3f, mCellHeightPx * 0.12f);
                            canvas.drawRect(left, top + mCellHeightPx - h, left + mCellWidthPx, top + mCellHeightPx, mCursorPaint);
                        } else if (shape == 2) { // Vertical Beam
                            float w = Math.max(3f, mCellWidthPx * 0.16f);
                            canvas.drawRect(left, top, left + w, top + mCellHeightPx, mCursorPaint);
                        } else { // Block
                            canvas.drawRect(left, top, left + mCellWidthPx, top + mCellHeightPx, mCursorPaint);
                        }
                    }
                }
            }

            int maxSb = nativeGetActiveScrollback();
            if (maxSb > 0 && mScrollOffset > 0) {
                float total = maxSb + mVisibleRows;
                float barH = Math.max(24f, getHeight() * (mVisibleRows / total));
                float frac = 1f - ((float) mScrollOffset / maxSb);
                float barTop = frac * (getHeight() - barH);
                canvas.drawRect(viewWidth - 3f, barTop, viewWidth, barTop + barH, mScrollbarPaint);
            }
        }

        private void drawPackedRow(Canvas target, int screenRow, int yTopPx) {
            int counts = nativePackRowRender(mScrollOffset, screenRow, mBgRuns, mGlyphs);
            int bgCount = (counts >>> 16) & 0xFFFF;
            int glCount = counts & 0xFFFF;

            int yBottomPx = yTopPx + mCellHeightPx;
            int baselineY = yTopPx + mFontAscentPx;

            for (int i = 0; i < bgCount; ++i) {
                int b = i * 3;
                mBgPaint.setColor(mBgRuns[b + 2]);
                target.drawRect(
                    (float) Math.floor(mBgRuns[b] * mCellWidthPx),
                    yTopPx,
                    (float) Math.ceil(mBgRuns[b + 1] * mCellWidthPx),
                    yBottomPx,
                    mBgPaint
                );
            }

            int prevFg = 0;
            int prevFlags = -1;
            for (int i = 0; i < glCount; ++i) {
                int b     = i * 4;
                int cp    = mGlyphs[b];
                int col   = mGlyphs[b + 1];
                int fg    = mGlyphs[b + 2];
                int flags = mGlyphs[b + 3];

                float xLeft  = col * mCellWidthPx;
                float xRight = xLeft + mCellWidthPx;

                // Procedural Pixel-Perfect Block Elements (U+2580..U+259F) for btop/htop/TUI bars
                if (cp >= 0x2580 && cp <= 0x2590 && drawProceduralBlockElement(target, cp, xLeft, yTopPx, xRight, yBottomPx, fg)) {
                    continue;
                }

                // Procedural Pixel-Perfect Box Drawing (─, │, ┌, ┐, └, ┘, ├, ┤, ┬, ┴, ┼)
                if (cp >= 0x2500 && cp <= 0x257F && drawProceduralBoxChar(target, cp, xLeft, yTopPx, xRight, yBottomPx, fg)) {
                    continue;
                }

                if (fg != prevFg) {
                    mTextPaint.setColor(fg);
                    prevFg = fg;
                }
                if (flags != prevFlags) {
                    mTextPaint.setFakeBoldText((flags & FLAG_BOLD) != 0);
                    mTextPaint.setTextSkewX((flags & FLAG_ITALIC) != 0 ? -0.20f : 0f);
                    mTextPaint.setUnderlineText((flags & FLAG_UNDERLINE) != 0);
                    mTextPaint.setStrikeThruText((flags & FLAG_STRIKETHROUGH) != 0);
                    prevFlags = flags;
                }

                int charLen = Character.toChars(cp, mGlyphChars, 0);
                target.drawText(mGlyphChars, 0, charLen, xLeft, baselineY, mTextPaint);

                // Modern Undercurl (4:3) or Double Underline (4:2) for Neovim/Helix LSP
                if ((flags & FLAG_UNDERCURL) != 0) {
                    mBoxPaint.setColor(fg);
                    float yWave = yBottomPx - 3f;
                    float midX = (xLeft + xRight) * 0.5f;
                    target.drawLine(xLeft, yWave, midX, yWave - 3f, mBoxPaint);
                    target.drawLine(midX, yWave - 3f, xRight, yWave, mBoxPaint);
                } else if ((flags & FLAG_DOUBLE_UNDERLINE) != 0) {
                    mBoxPaint.setColor(fg);
                    target.drawLine(xLeft, yBottomPx - 4f, xRight, yBottomPx - 4f, mBoxPaint);
                    target.drawLine(xLeft, yBottomPx - 1f, xRight, yBottomPx - 1f, mBoxPaint);
                }
            }
        }

        private boolean drawProceduralBlockElement(Canvas c, int cp, float l, float t, float r, float b, int fg) {
            mBgPaint.setColor(fg);
            float h = b - t;
            float w = r - l;
            if (cp == 0x2580) { // Upper half block ▀
                c.drawRect(l, t, r, t + h * 0.5f, mBgPaint);
                return true;
            } else if (cp >= 0x2581 && cp <= 0x2588) { // Lower 1/8 .. Full block ▁▂▃▄▅▆▇█
                float frac = (cp - 0x2580) / 8.0f;
                c.drawRect(l, b - h * frac, r, b, mBgPaint);
                return true;
            } else if (cp >= 0x2589 && cp <= 0x258F) { // Left 7/8 .. 1/8 blocks ▉▊▋▌▍▎▏
                float frac = (0x2590 - cp) / 8.0f;
                c.drawRect(l, t, l + w * frac, b, mBgPaint);
                return true;
            } else if (cp == 0x2590) { // Right half block ▐
                c.drawRect(l + w * 0.5f, t, r, b, mBgPaint);
                return true;
            }
            return false;
        }

        private boolean drawProceduralBoxChar(Canvas c, int cp, float l, float t, float r, float b, int fg) {
            float cx = (l + r) * 0.5f;
            float cy = (t + b) * 0.5f;
            // Bitmask: 1=Left, 2=Right, 4=Up, 8=Down
            int mask = 0;
            switch (cp) {
                case 0x2500: case 0x2501: case 0x2550: mask = 1 | 2; break; // ─ ━ ═
                case 0x2502: case 0x2503: case 0x2551: mask = 4 | 8; break; // │ ┃ ║
                case 0x250C: case 0x250F: case 0x2554: case 0x256D: mask = 2 | 8; break; // ┌ ┏ ╔ ╭
                case 0x2510: case 0x2513: case 0x2557: case 0x256E: mask = 1 | 8; break; // ┐ ┓ ╗ ╮
                case 0x2514: case 0x2517: case 0x255A: case 0x2570: mask = 2 | 4; break; // └ ┗ ╚ ╰
                case 0x2518: case 0x251B: case 0x255D: case 0x256F: mask = 1 | 4; break; // ┘ ┛ ╝ ╯
                case 0x251C: case 0x2523: case 0x2560: mask = 2 | 4 | 8; break; // ├ ┣ ╠
                case 0x2524: case 0x252B: case 0x2563: mask = 1 | 4 | 8; break; // ┤ ┫ ╣
                case 0x252C: case 0x2533: case 0x2566: mask = 1 | 2 | 8; break; // ┬ ┳ ╦
                case 0x2534: case 0x253B: case 0x2569: mask = 1 | 2 | 4; break; // ┴ ┻ ╩
                case 0x253C: case 0x254B: case 0x256C: mask = 1 | 2 | 4 | 8; break; // ┼ ╋ ╬
                default: return false;
            }
            mBoxPaint.setColor(fg);
            if ((mask & 1) != 0) c.drawLine(l, cy, cx, cy, mBoxPaint);
            if ((mask & 2) != 0) c.drawLine(cx, cy, r, cy, mBoxPaint);
            if ((mask & 4) != 0) c.drawLine(cx, t, cx, cy, mBoxPaint);
            if ((mask & 8) != 0) c.drawLine(cx, cy, cx, b, mBoxPaint);
            return true;
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return true;
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_ACTION_NONE;

            return new BaseInputConnection(this, false) {
                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    if (text != null && text.length() > 0) {
                        nativeSendText(text.toString(), false);
                        syncModifierVisuals(nativeGetModifierMask());
                    }
                    return true;
                }

                @Override
                public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                    for (int i = 0; i < beforeLength; ++i) {
                        nativeSendKeyEvent(KeyEvent.KEYCODE_DEL, 0, 0);
                    }
                    return true;
                }

                @Override
                public boolean sendKeyEvent(KeyEvent event) {
                    return dispatchKeyEvent(event);
                }
            };
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            int meta = event.getMetaState();
            int unicode = event.getUnicodeChar(meta & ~(KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON));
            int rc = nativeSendKeyEvent(keyCode, unicode, meta);
            syncModifierVisuals(nativeGetModifierMask());
            if (rc == 2) {
                closeSessionAt(nativeGetActiveIndex());
                return true;
            }
            if (rc == 1) return true;
            return super.onKeyDown(keyCode, event);
        }
    }

    // ========================================================================
    // Minimal Background Keep-Alive Service (WakeLock + WifiLock)
    // ========================================================================
    public static final class KeepAliveService extends Service {
        private static final String CHANNEL_ID = "termuxx_bg";
        private static final String ACTION_EXIT = "com.termuxx.app.EXIT";
        private static final int NOTIF_ID = 1337;

        private PowerManager.WakeLock mWakeLock;
        private WifiManager.WifiLock mWifiLock;

        @Override
        public void onCreate() {
            super.onCreate();
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Termux", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
            acquireLocks();
            Notification n = buildNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, n);
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent != null && ACTION_EXIT.equals(intent.getAction())) {
                releaseLocks();
                nativeDestroyAll();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                System.exit(0);
                return START_NOT_STICKY;
            }
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            releaseLocks();
            super.onDestroy();
        }

        private void acquireLocks() {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && mWakeLock == null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Termuxx::Lock");
                mWakeLock.setReferenceCounted(false);
                mWakeLock.acquire();
            }
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && mWifiLock == null) {
                mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Termuxx::Wifi");
                mWifiLock.setReferenceCounted(false);
                mWifiLock.acquire();
            }
        }

        private void releaseLocks() {
            if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
            mWakeLock = null;
            if (mWifiLock != null && mWifiLock.isHeld()) mWifiLock.release();
            mWifiLock = null;
        }

        private Notification buildNotification() {
            PendingIntent openPi = PendingIntent.getActivity(
                this, 0,
                new Intent(this, TermuxActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            PendingIntent exitPi = PendingIntent.getService(
                this, 1,
                new Intent(this, KeepAliveService.class).setAction(ACTION_EXIT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentTitle("Termux")
                .setContentText("Active (WakeLock ON)")
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null, "Exit", exitPi).build())
                .build();
        }
    }

    // ========================================================================
    // C++23 Native Core JNI Declarations
    // ========================================================================
    private static native int nativeInitAndPollBootstrap(String nativeLibDir);
    private static native int nativeNewSession(boolean asRoot, int rows, int cols, int cellW, int cellH);
    private static native void nativeSwitchSession(int index);
    private static native int nativeCloseSession(int index);
    static native void nativeDestroyAll();
    private static native int nativeGetSessionCount();
    private static native int nativeGetActiveIndex();
    private static native boolean nativeIsSessionRoot(int index);
    private static native void nativeResizeActive(int rows, int cols, int cellW, int cellH);
    private static native boolean nativeConsumeFrameDirty();
    private static native String nativeConsumeOsc52Clipboard();
    private static native int nativeDispatchCustomKey(String keyLabel);
    private static native int nativeGetModifierMask();
    private static native void nativeSendText(String text, boolean isPaste);
    private static native int nativeSendKeyEvent(int keyCode, int unicodeChar, int metaState);
    private static native void nativeSendMouse(int button, int col, int row, boolean pressed);
    private static native int nativeGetActiveScrollback();
    private static native boolean nativeIsRowDirty(int screenRow);
    private static native void nativeClearDirtyFlags();
    private static native int nativePackRowRender(int scrollOffset, int screenRow, int[] outBgRuns, int[] outGlyphs);
    private static native int nativeGetCursorState();
    private static native int nativeGetModesMask();
    private static native String nativeGetViewportSelectionText(int scrollOffset);
    private static native String nativeGetTranscriptText();
}
