package com.termuxx.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import android.text.InputType;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
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
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Minimalist JNI/Canvas Bridge for Termuxx (com.termuxx).
 * Delegates terminal emulation, ZIP bootstrap extraction, 16-byte ELF relocation,
 * interactive text selection (word/URL long-press + draggable handles + floating [Copy|Paste|All|✕] bar),
 * and PTY management to C++23 (libtermux-core.so + libtermuxx-redirect.so).
 */
public final class TermuxActivity extends Activity {

    static {
        System.loadLibrary("termux-core");
    }

    private static final String[] KEY_LABELS = new String[]{
        "ESC", "TAB", "CTRL", "ALT", "-", "/", "|", "▲", "END",
        "~", "HOME", "PGUP", "PGDN", "$", ":", "◀", "▼", "▶"
    };
    private static final String[] SEL_ACTIONS = new String[]{"Copy", "Paste", "All", "✕"};

    private static final int AMOLED_BLACK = 0xFF000000;
    private static final int KEY_SURFACE  = 0xFF0A0A0A;
    private static final int KEY_PRESSED  = 0xFF1A1A1A;
    private static final int KEY_ARMED    = 0xFF262626;
    private static final int TEXT_BRIGHT  = 0xFFE5E9F0;
    private static final int TEXT_MUTED   = 0xFF6E7681;

    private static final int FLAG_BOLD          = 1 << 0;
    private static final int FLAG_ITALIC        = 1 << 2;
    private static final int FLAG_UNDERLINE     = 1 << 3;
    private static final int FLAG_STRIKETHROUGH = 1 << 7;
    private static final int MODE_ALT_SCREEN     = 1 << 2;
    private static final int MODE_MOUSE_TRACKING = 1 << 3;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Typeface mJetBrainsMono;
    private LinearLayout mTabStrip;
    private TextView mStatusLabel;
    private TerminalSurface mTerminalSurface;
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

        header.addView(makeHeaderButton("+", v -> createAndSwitchSession(false)));
        header.addView(makeHeaderButton("#", v -> createAndSwitchSession(true)));
        header.addView(makeHeaderButton("cp", v -> copySelectionOrTranscript()));
        header.addView(makeHeaderButton("ps", v -> pasteFromClipboard()));
        header.addView(makeHeaderButton("kb", v -> {
            nativeToggleExtraKeys();
            boolean vis = mExtraKeysBar.getVisibility() == View.VISIBLE;
            mExtraKeysBar.setVisibility(vis ? View.GONE : View.VISIBLE);
        }));

        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        View hairline = new View(this);
        hairline.setBackgroundColor(0xFF121212);
        root.addView(hairline, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        mTerminalSurface = new TerminalSurface(this);
        root.addView(mTerminalSurface, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        mExtraKeysBar = buildExtraKeysBar();
        root.addView(mExtraKeysBar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }

    private TextView makeHeaderButton(String label, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTypeface(mJetBrainsMono, Typeface.NORMAL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTextColor(TEXT_MUTED);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(2), dp(8), dp(2));
        tv.setOnClickListener(listener);
        return tv;
    }

    private GridLayout buildExtraKeysBar() {
        GridLayout grid = new GridLayout(this);
        grid.setRowCount(2);
        grid.setColumnCount(9);
        grid.setBackgroundColor(AMOLED_BLACK);
        int pad = dp(2);
        grid.setPadding(pad, pad, pad, pad);

        for (int idx = 0; idx < KEY_LABELS.length; ++idx) {
            final int keyId = idx;
            final String label = KEY_LABELS[idx];
            final TextView key = new TextView(this);
            key.setText(label);
            key.setGravity(Gravity.CENTER);
            key.setTypeface(mJetBrainsMono, Typeface.NORMAL);
            key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            key.setTextColor(0xFFCCCCCC);
            key.setBackground(makeKeyBg(KEY_SURFACE));

            if (keyId == 2) mCtrlKeyView = key;
            if (keyId == 3) mAltKeyView = key;

            final boolean repeatable = (keyId == 7 || keyId == 11 || keyId == 12 || keyId >= 15);
            final Runnable repeatTask = new Runnable() {
                @Override
                public void run() {
                    int mask = nativeDispatchExtraKey(keyId);
                    syncModifierVisuals(mask);
                    mHandler.postDelayed(this, 55);
                }
            };

            key.setOnTouchListener((v, ev) -> {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        key.setBackground(makeKeyBg(KEY_PRESSED));
                        int mask = nativeDispatchExtraKey(keyId);
                        syncModifierVisuals(mask);
                        if (repeatable) mHandler.postDelayed(repeatTask, 320);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mHandler.removeCallbacks(repeatTask);
                        syncModifierVisuals(nativeGetUiStateMask());
                        if (keyId != 2 && keyId != 3) key.setBackground(makeKeyBg(KEY_SURFACE));
                        return true;
                    default:
                        return false;
                }
            });

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(idx / 9, 1, 1f),
                GridLayout.spec(idx % 9, 1, 1f)
            );
            lp.width = 0;
            lp.height = dp(30);
            lp.setMargins(dp(1), dp(1), dp(1), dp(1));
            grid.addView(key, lp);
        }
        return grid;
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

    void copySelectionOrTranscript() {
        String text = nativeGetCopyText(mTerminalSurface.mScrollOffset);
        if (text != null && !text.isEmpty()) {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Termuxx", text));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }
        mTerminalSurface.invalidateRowCache();
        mTerminalSurface.postInvalidateOnAnimation();
    }

    void pasteFromClipboard() {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (text != null && text.length() > 0) {
                nativeSendText(text.toString(), true);
                syncModifierVisuals(nativeGetUiStateMask());
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
    // 165Hz Hardware RenderNode Surface + C++23 Interactive Text Selection
    // ========================================================================
    private final class TerminalSurface extends View implements Choreographer.FrameCallback {

        private final Paint mBgPaint = new Paint();
        private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final Paint mCursorPaint = new Paint();
        private final Paint mHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mActionBarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mActionBarStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mActionBarTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
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

            mCursorPaint.setStyle(Paint.Style.FILL);
            mCursorPaint.setColor(0xFFD8DEE9);

            mHandlePaint.setStyle(Paint.Style.FILL);
            mHandlePaint.setColor(0xFF61AFEF);

            mActionBarBgPaint.setStyle(Paint.Style.FILL);
            mActionBarBgPaint.setColor(0xFF141820);

            mActionBarStrokePaint.setStyle(Paint.Style.STROKE);
            mActionBarStrokePaint.setStrokeWidth(2f);
            mActionBarStrokePaint.setColor(0xFF3B4252);

            mActionBarTextPaint.setTypeface(mJetBrainsMono);
            mActionBarTextPaint.setColor(0xFFE5E9F0);
            mActionBarTextPaint.setTextAlign(Paint.Align.CENTER);

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
                    int col = (int) (e.getX() / mCellWidthPx);
                    int row = (int) (e.getY() / mCellHeightPx);
                    nativeOnLongPress(mScrollOffset, row, col);
                    invalidateRowCache();
                    postInvalidateOnAnimation();
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
                    float newSize = Math.max(16f, Math.min(68f, mTextSizePx * detector.getScaleFactor()));
                    if (Math.abs(newSize - mTextSizePx) >= 0.5f) {
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
            mActionBarTextPaint.setTextSize(Math.max(24f, textSizePx * 0.92f));
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            mCellWidthPx = Math.max(5f, mTextPaint.measureText("aaaaaaaaaaMMMMMMMMMM0000000000") / 30f);
            mCellHeightPx = Math.max(10, (int) Math.ceil(fm.descent - fm.ascent + fm.leading));
            mFontAscentPx = (int) Math.ceil(-fm.ascent);
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
            if (nativeConsumeFrameDirty()) {
                invalidateRowCache();
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
            // If C++23 Interactive Text Selection is active, let C++23 hit-test handles & floating bar first
            if ((nativeGetUiStateMask() & 8) != 0) {
                int selAction = nativeOnSelectionTouch(
                    event.getActionMasked(),
                    event.getX(),
                    event.getY(),
                    mCellWidthPx,
                    mCellHeightPx,
                    getWidth()
                );
                if (selAction == 2) { // UI_ACTION_COPY_TEXT
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    copySelectionOrTranscript();
                    return true;
                } else if (selAction == 3) { // UI_ACTION_PASTE_CLIPBOARD
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    pasteFromClipboard();
                    invalidateRowCache();
                    postInvalidateOnAnimation();
                    return true;
                } else if (selAction != 0) {
                    if (selAction == 5) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    invalidateRowCache();
                    postInvalidateOnAnimation();
                    return true;
                }
            }

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

            if (mScrollOffset == 0) {
                int cursorState = nativeGetCursorState();
                if (cursorState >= 0) {
                    int curRow = (cursorState >>> 16) & 0x7FFF;
                    int curCol = cursorState & 0xFFFF;
                    if (curRow < mVisibleRows && curCol < mVisibleCols) {
                        float left = curCol * mCellWidthPx;
                        float top = curRow * mCellHeightPx;
                        canvas.drawRect(left, top, left + mCellWidthPx, top + mCellHeightPx, mCursorPaint);
                    }
                }
            }

            // C++23 Interactive Selection Handles & Floating Action Bar Overlay
            long selBounds = nativeGetSelectionBounds();
            if (selBounds >= 0L) {
                int r1 = (int) ((selBounds >>> 48) & 0xFFFF);
                int c1 = (int) ((selBounds >>> 32) & 0xFFFF);
                int r2 = (int) ((selBounds >>> 16) & 0xFFFF);
                int c2 = (int) (selBounds & 0xFFFF);

                float h1x = c1 * mCellWidthPx;
                float h1y = (r1 + 1) * mCellHeightPx;
                float h2x = (c2 + 1) * mCellWidthPx;
                float h2y = (r2 + 1) * mCellHeightPx;
                float rad = Math.max(10f, mCellHeightPx * 0.32f);

                canvas.drawRect(h1x - 2f, r1 * mCellHeightPx, h1x + 2f, h1y + rad, mHandlePaint);
                canvas.drawRoundRect(h1x - rad, h1y, h1x + rad, h1y + rad * 2f, rad, rad, mHandlePaint);

                canvas.drawRect(h2x - 2f, r2 * mCellHeightPx, h2x + 2f, h2y + rad, mHandlePaint);
                canvas.drawRoundRect(h2x - rad, h2y, h2x + rad, h2y + rad * 2f, rad, rad, mHandlePaint);

                float barW = Math.min(viewWidth - 32f, mCellWidthPx * 26f);
                float barH = mCellHeightPx * 1.35f;
                float barLeft = Math.max(16f, Math.min(viewWidth - barW - 16f, h1x - barW * 0.25f));
                float barTop = (r1 >= 2) ? (r1 * mCellHeightPx - barH - 14f) : ((r2 + 1) * mCellHeightPx + 28f);

                canvas.drawRoundRect(barLeft, barTop, barLeft + barW, barTop + barH, 14f, 14f, mActionBarBgPaint);
                canvas.drawRoundRect(barLeft, barTop, barLeft + barW, barTop + barH, 14f, 14f, mActionBarStrokePaint);

                float segW = barW / 4f;
                float textY = barTop + barH * 0.68f;
                for (int i = 0; i < 4; ++i) {
                    float cx = barLeft + segW * (i + 0.5f);
                    canvas.drawText(SEL_ACTIONS[i].toCharArray(), 0, SEL_ACTIONS[i].length(), cx, textY, mActionBarTextPaint);
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
                target.drawText(mGlyphChars, 0, charLen, col * mCellWidthPx, baselineY, mTextPaint);
            }
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
                        syncModifierVisuals(nativeGetUiStateMask());
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
            syncModifierVisuals(nativeGetUiStateMask());
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
    private static native int nativeDispatchExtraKey(int keyId);
    private static native int nativeGetUiStateMask();
    private static native void nativeToggleExtraKeys();
    private static native void nativeOnLongPress(int scrollOffset, int row, int col);
    private static native long nativeGetSelectionBounds();
    private static native int nativeOnSelectionTouch(int action, float x, float y, float cellW, int cellH, int viewW);
    private static native void nativeSendText(String text, boolean isPaste);
    private static native int nativeSendKeyEvent(int keyCode, int unicodeChar, int metaState);
    private static native void nativeSendMouse(int button, int col, int row, boolean pressed);
    private static native int nativeGetActiveScrollback();
    private static native boolean nativeIsRowDirty(int screenRow);
    private static native void nativeClearDirtyFlags();
    private static native int nativePackRowRender(int scrollOffset, int screenRow, int[] outBgRuns, int[] outGlyphs);
    private static native int nativeGetCursorState();
    private static native int nativeGetModesMask();
    private static native String nativeGetCopyText(int scrollOffset);
}
