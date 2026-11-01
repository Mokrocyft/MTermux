package com.termuxx.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Minimalist Pure Dark AMOLED (#000000) Activity for Termuxx (com.termuxx).
 * Includes an ultra-clean monochrome session header and a borderless AMOLED 2-row Extra Keys bar.
 */
public final class TermuxActivity extends Activity implements TerminalView.ModifierStateProvider {

    private static final String[][] EXTRA_KEYS = new String[][]{
        {"ESC", "TAB", "CTRL", "ALT", "-", "/", "|", "▲", "END"},
        {"~", "HOME", "PGUP", "PGDN", "$", ":", "◀", "▼", "▶"}
    };

    private static final int AMOLED_BLACK  = 0xFF000000;
    private static final int KEY_SURFACE   = 0xFF0A0A0A;
    private static final int KEY_PRESSED   = 0xFF1A1A1A;
    private static final int KEY_ARMED     = 0xFF262626;
    private static final int TEXT_BRIGHT   = 0xFFE5E9F0;
    private static final int TEXT_MUTED    = 0xFF6E7681;

    private final Handler mRepeatHandler = new Handler(Looper.getMainLooper());
    private TermuxService mService;
    private boolean mServiceBound = false;

    private LinearLayout mTabStrip;
    private TextView mStatusText;
    private TerminalView mTerminalView;
    private GridLayout mExtraKeysBar;
    private TermuxService.Session mActiveSession;

    private boolean mCtrlArmed = false;
    private boolean mAltArmed = false;
    private TextView mCtrlBtn;
    private TextView mAltBtn;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = ((TermuxService.LocalBinder) binder).getService();
            mServiceBound = true;

            NativeBridge.ensureBootstrapAsync(TermuxActivity.this, new NativeBridge.BootstrapCallback() {
                @Override
                public void onProgress(String msg) {
                    mStatusText.setVisibility(View.VISIBLE);
                    mStatusText.setText(msg);
                }

                @Override
                public void onReady() {
                    mStatusText.setVisibility(View.GONE);
                    if (mService.getSessions().isEmpty()) {
                        switchToSession(mService.createSession(false));
                    } else if (mActiveSession == null) {
                        switchToSession(mService.getSessions().get(0));
                    }
                    rebuildHeaderTabs();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureAmoledWindow();
        buildMinimalistAmoledUi();

        Intent serviceIntent = new Intent(this, TermuxService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceBound = false;
        }
        super.onDestroy();
    }

    private void configureAmoledWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (window == null) return;

        window.setStatusBarColor(AMOLED_BLACK);
        window.setNavigationBarColor(AMOLED_BLACK);
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        if (Build.VERSION.SDK_INT >= 30) {
            Display display = getDisplay();
            if (display != null) {
                Display.Mode best = null;
                float maxHz = 60f;
                for (Display.Mode m : display.getSupportedModes()) {
                    if (m.getRefreshRate() > maxHz) {
                        maxHz = m.getRefreshRate();
                        best = m;
                    }
                }
                if (best != null) {
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.preferredDisplayModeId = best.getModeId();
                    window.setAttributes(lp);
                }
            }
        }
    }

    private void buildMinimalistAmoledUi() {
        mTerminalView = new TerminalView(this);
        mTerminalView.setModifierStateProvider(this);
        Typeface mono = mTerminalView.getJetBrainsMono();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AMOLED_BLACK);

        // 1. Minimalist Pure-Black Header Bar (28dp height)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(AMOLED_BLACK);
        header.setPadding(dp(8), dp(2), dp(8), dp(2));

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        mTabStrip = new LinearLayout(this);
        mTabStrip.setOrientation(LinearLayout.HORIZONTAL);
        mTabStrip.setGravity(Gravity.CENTER_VERTICAL);
        tabScroll.addView(mTabStrip);
        header.addView(tabScroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mStatusText = new TextView(this);
        mStatusText.setTypeface(mono);
        mStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        mStatusText.setTextColor(TEXT_MUTED);
        mStatusText.setPadding(dp(6), 0, dp(6), 0);
        mStatusText.setVisibility(View.GONE);
        header.addView(mStatusText);

        // Minimalist monochrome action symbols: [+] new shell, [#] root shell, [cp] copy, [ps] paste, [kb] extra keys
        header.addView(makeMinimalAction("+", "New Shell", mono, v -> {
            if (mService != null) switchToSession(mService.createSession(false));
        }));
        header.addView(makeMinimalAction("#", "New Root Shell", mono, v -> {
            if (mService != null) switchToSession(mService.createSession(true));
        }));
        header.addView(makeMinimalAction("cp", "Copy", mono, v -> {
            mTerminalView.copyTranscript();
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }));
        header.addView(makeMinimalAction("ps", "Paste", mono, v -> mTerminalView.pasteFromClipboard()));
        header.addView(makeMinimalAction("kb", "Toggle Keys", mono, v -> {
            boolean vis = mExtraKeysBar.getVisibility() == View.VISIBLE;
            mExtraKeysBar.setVisibility(vis ? View.GONE : View.VISIBLE);
        }));

        root.addView(header, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(30)
        ));

        // Subtle 1px dark divider (#121212)
        View hairline = new View(this);
        hairline.setBackgroundColor(0xFF121212);
        root.addView(hairline, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // 2. 165Hz Terminal Canvas
        root.addView(mTerminalView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        // 3. Minimalist Borderless AMOLED Extra Keys Bar
        mExtraKeysBar = buildExtraKeysBar(mono);
        root.addView(mExtraKeysBar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }

    private TextView makeMinimalAction(String symbol, String tooltip, Typeface mono, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(symbol);
        tv.setTypeface(mono, Typeface.NORMAL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTextColor(TEXT_MUTED);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(2), dp(8), dp(2));
        tv.setOnClickListener(listener);
        return tv;
    }

    private GridLayout buildExtraKeysBar(Typeface mono) {
        GridLayout grid = new GridLayout(this);
        grid.setRowCount(2);
        grid.setColumnCount(9);
        grid.setBackgroundColor(AMOLED_BLACK);
        int pad = dp(2);
        grid.setPadding(pad, pad, pad, pad);

        for (int r = 0; r < EXTRA_KEYS.length; ++r) {
            for (int c = 0; c < EXTRA_KEYS[r].length; ++c) {
                final String label = EXTRA_KEYS[r][c];
                final TextView key = new TextView(this);
                key.setText(label);
                key.setGravity(Gravity.CENTER);
                key.setTypeface(mono, Typeface.NORMAL);
                key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                key.setTextColor(0xFFCCCCCC);
                key.setBackground(makeKeyDrawable(KEY_SURFACE));

                if ("CTRL".equals(label)) mCtrlBtn = key;
                if ("ALT".equals(label)) mAltBtn = key;

                final Runnable repeatTask = new Runnable() {
                    @Override
                    public void run() {
                        handleExtraKey(label);
                        mRepeatHandler.postDelayed(this, 55);
                    }
                };

                key.setOnTouchListener((v, event) -> {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            key.setBackground(makeKeyDrawable(KEY_PRESSED));
                            handleExtraKey(label);
                            if (isRepeatable(label)) {
                                mRepeatHandler.postDelayed(repeatTask, 320);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            mRepeatHandler.removeCallbacks(repeatTask);
                            updateModifierButtons();
                            if (!"CTRL".equals(label) && !"ALT".equals(label)) {
                                key.setBackground(makeKeyDrawable(KEY_SURFACE));
                            }
                            return true;
                        default:
                            return false;
                    }
                });

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(r, 1, 1f),
                    GridLayout.spec(c, 1, 1f)
                );
                lp.width = 0;
                lp.height = dp(31);
                lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                grid.addView(key, lp);
            }
        }
        return grid;
    }

    private static boolean isRepeatable(String label) {
        return "▲".equals(label) || "▼".equals(label) || "◀".equals(label) || "▶".equals(label)
            || "PGUP".equals(label) || "PGDN".equals(label);
    }

    private void handleExtraKey(String label) {
        if ("CTRL".equals(label)) {
            mCtrlArmed = !mCtrlArmed;
            updateModifierButtons();
            return;
        }
        if ("ALT".equals(label)) {
            mAltArmed = !mAltArmed;
            updateModifierButtons();
            return;
        }
        if (mActiveSession == null || mActiveSession.handle == 0) return;
        long h = mActiveSession.handle;

        int meta = (mCtrlArmed ? KeyEvent.META_CTRL_ON : 0) | (mAltArmed ? KeyEvent.META_ALT_ON : 0);
        switch (label) {
            case "ESC":  NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_ESCAPE, meta); break;
            case "TAB":  NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_TAB, meta); break;
            case "▲":    NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_DPAD_UP, meta); break;
            case "▼":    NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_DPAD_DOWN, meta); break;
            case "◀":    NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_DPAD_LEFT, meta); break;
            case "▶":    NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_DPAD_RIGHT, meta); break;
            case "HOME": NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_MOVE_HOME, meta); break;
            case "END":  NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_MOVE_END, meta); break;
            case "PGUP": NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_PAGE_UP, meta); break;
            case "PGDN": NativeBridge.sendKeyCode(h, KeyEvent.KEYCODE_PAGE_DOWN, meta); break;
            default:
                NativeBridge.sendCodepoint(h, label.codePointAt(0), mCtrlArmed, mAltArmed);
                break;
        }

        if (mCtrlArmed || mAltArmed) {
            mCtrlArmed = false;
            mAltArmed = false;
            updateModifierButtons();
        }
    }

    private void updateModifierButtons() {
        if (mCtrlBtn != null) {
            mCtrlBtn.setBackground(makeKeyDrawable(mCtrlArmed ? KEY_ARMED : KEY_SURFACE));
            mCtrlBtn.setTextColor(mCtrlArmed ? Color.WHITE : 0xFFCCCCCC);
        }
        if (mAltBtn != null) {
            mAltBtn.setBackground(makeKeyDrawable(mAltArmed ? KEY_ARMED : KEY_SURFACE));
            mAltBtn.setTextColor(mAltArmed ? Color.WHITE : 0xFFCCCCCC);
        }
    }

    @Override
    public boolean consumeCtrl() {
        if (!mCtrlArmed) return false;
        mCtrlArmed = false;
        updateModifierButtons();
        return true;
    }

    @Override
    public boolean consumeAlt() {
        if (!mAltArmed) return false;
        mAltArmed = false;
        updateModifierButtons();
        return true;
    }

    private void switchToSession(TermuxService.Session session) {
        if (session == null) return;
        mActiveSession = session;
        mTerminalView.attachSession(session);
        rebuildHeaderTabs();
        mTerminalView.requestFocus();
    }

    public void closeSession(TermuxService.Session session) {
        if (mService == null || session == null) return;
        mService.removeSession(session);
        List<TermuxService.Session> rem = mService.getSessions();
        if (rem.isEmpty()) {
            mService.stopAllAndSelf();
            finish();
        } else {
            switchToSession(rem.get(rem.size() - 1));
        }
    }

    private void rebuildHeaderTabs() {
        if (mTabStrip == null || mService == null) return;
        mTabStrip.removeAllViews();
        Typeface mono = mTerminalView.getJetBrainsMono();

        List<TermuxService.Session> sessions = mService.getSessions();
        for (int i = 0; i < sessions.size(); ++i) {
            final TermuxService.Session s = sessions.get(i);
            boolean active = (s == mActiveSession);

            TextView tab = new TextView(this);
            tab.setText((i + 1) + (s.isRoot ? "·su" : "·sh"));
            tab.setTypeface(mono, active ? Typeface.BOLD : Typeface.NORMAL);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            tab.setTextColor(active ? TEXT_BRIGHT : TEXT_MUTED);
            tab.setPadding(dp(8), dp(2), dp(8), dp(2));

            tab.setOnClickListener(v -> switchToSession(s));
            tab.setOnLongClickListener(v -> {
                closeSession(s);
                return true;
            });

            mTabStrip.addView(tab);
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

    private GradientDrawable makeKeyDrawable(int color) {
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
}
