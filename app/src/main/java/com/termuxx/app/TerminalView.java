package com.termuxx.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.OverScroller;

/**
 * Minimalist 165Hz Dark AMOLED Console View powered by C++23 row packing & JetBrains Mono.
 *
 * Why Cursor Drift Is Impossible:
 * 1. Loads bundled JetBrainsMono[wght].ttf directly from assets/fonts/JetBrainsMono.ttf with ligatures disabled.
 * 2. C++23 NativeBridge.packRowRender() emits each visible character's exact column index `col`.
 * 3. Every glyph is drawn at `x = col * mCellWidthPx`, matching the cursor coordinate `curCol * mCellWidthPx`
 *    to the exact subpixel regardless of OS system font hooks.
 */
public final class TerminalView extends View implements Choreographer.FrameCallback {

    public interface ModifierStateProvider {
        boolean consumeCtrl();
        boolean consumeAlt();
    }

    private final Paint mBgPaint = new Paint();
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint mCursorPaint = new Paint();
    private final Paint mScrollbarPaint = new Paint();
    private final Typeface mJetBrainsMono;

    private final OverScroller mScroller;
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;

    private TermuxService.Session mSession;
    private ModifierStateProvider mModifierProvider;

    private float mTextSizePx;
    private float mCellWidthPx;
    private int mCellHeightPx;
    private int mFontAscentPx;

    private int mVisibleRows = 45;
    private int mVisibleCols = 72;
    private int mScrollOffset = 0;
    private int mLastRenderedScrollOffset = -1;
    private float mScrollRemainder = 0f;
    private boolean mAttached = false;

    private RenderNode[] mRowNodes = new RenderNode[0];
    private boolean[] mRowNodeValid = new boolean[0];

    // Pre-allocated C++23 packed buffers:
    // mBgRuns: triples [startCol, endCol, bgArgb]
    // mGlyphs: quads   [codepoint, col, fgArgb, flags]
    private int[] mBgRuns = new int[512 * 3];
    private int[] mGlyphs = new int[512 * 4];
    private final char[] mSingleGlyphBuf = new char[2];

    public TerminalView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDefaultFocusHighlightEnabled(false);

        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setAntiAlias(false);

        Typeface loadedTf;
        try {
            loadedTf = Typeface.createFromAsset(context.getAssets(), "fonts/JetBrainsMono.ttf");
        } catch (Throwable ignored) {
            loadedTf = Typeface.MONOSPACE;
        }
        mJetBrainsMono = loadedTf;
        mTextPaint.setTypeface(mJetBrainsMono);
        mTextPaint.setFontFeatureSettings("'liga' 0, 'calt' 0");

        mCursorPaint.setStyle(Paint.Style.FILL);
        mCursorPaint.setColor(0xFFD8DEE9);

        mScrollbarPaint.setStyle(Paint.Style.FILL);
        mScrollbarPaint.setColor(0x558892B0);

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
                if (mSession != null) {
                    int modes = NativeBridge.getModesMask(mSession.handle);
                    if ((modes & NativeBridge.MODE_MOUSE_TRACKING) != 0) {
                        int col = (int) (e.getX() / mCellWidthPx) + 1;
                        int row = (int) (e.getY() / mCellHeightPx) + 1;
                        NativeBridge.sendMouse(mSession.handle, 0, col, row, true);
                        NativeBridge.sendMouse(mSession.handle, 0, col, row, false);
                    }
                }
                showSoftKeyboard();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                copyTranscript();
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (mSession == null || mSession.handle == 0) return true;
                long h = mSession.handle;
                int modes = NativeBridge.getModesMask(h);

                if ((modes & NativeBridge.MODE_ALT_SCREEN) != 0 &&
                    (modes & NativeBridge.MODE_MOUSE_TRACKING) != 0) {
                    mScrollRemainder += distanceY;
                    int lines = (int) (mScrollRemainder / mCellHeightPx);
                    if (lines != 0) {
                        mScrollRemainder -= lines * mCellHeightPx;
                        int col = (int) (e2.getX() / mCellWidthPx) + 1;
                        int row = (int) (e2.getY() / mCellHeightPx) + 1;
                        int btn = lines < 0 ? 64 : 65;
                        for (int i = 0, n = Math.min(5, Math.abs(lines)); i < n; ++i) {
                            NativeBridge.sendMouse(h, btn, col, row, true);
                        }
                    }
                    return true;
                }

                mScrollRemainder += distanceY;
                int deltaRows = (int) (mScrollRemainder / mCellHeightPx);
                if (deltaRows != 0) {
                    mScrollRemainder -= deltaRows * mCellHeightPx;
                    int maxSb = NativeBridge.getActiveScrollback(h);
                    int next = Math.max(0, Math.min(maxSb, mScrollOffset - deltaRows));
                    if (next != mScrollOffset) {
                        mScrollOffset = next;
                        invalidateAllRowNodes();
                        postInvalidateOnAnimation();
                    }
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (mSession == null || mSession.handle == 0) return true;
                int maxSb = NativeBridge.getActiveScrollback(mSession.handle);
                if (maxSb <= 0) return true;

                mScroller.abortAnimation();
                mScroller.fling(0, mScrollOffset * mCellHeightPx, 0, (int) velocityY, 0, 0, 0, maxSb * mCellHeightPx);
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
                    invalidateAllRowNodes();
                    postInvalidateOnAnimation();
                }
                return true;
            }
        });
    }

    public Typeface getJetBrainsMono() {
        return mJetBrainsMono;
    }

    public void setModifierStateProvider(ModifierStateProvider provider) {
        mModifierProvider = provider;
    }

    public void attachSession(TermuxService.Session session) {
        mSession = session;
        mScrollOffset = 0;
        mLastRenderedScrollOffset = -1;
        recalculateGrid(getWidth(), getHeight());
        invalidateAllRowNodes();
        postInvalidateOnAnimation();
    }

    public TermuxService.Session getSession() {
        return mSession;
    }

    public void showSoftKeyboard() {
        requestFocus();
        InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
        if (imm != null) imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    public void copyTranscript() {
        if (mSession == null || mSession.handle == 0) return;
        String text = NativeBridge.getTranscriptText(mSession.handle, 4000);
        if (text == null || text.isEmpty()) return;
        ClipboardManager cm = getContext().getSystemService(ClipboardManager.class);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Termuxx", text));
    }

    public void pasteFromClipboard() {
        if (mSession == null || mSession.handle == 0) return;
        ClipboardManager cm = getContext().getSystemService(ClipboardManager.class);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(getContext());
            if (text != null && text.length() > 0) {
                NativeBridge.sendText(mSession.handle, text.toString(), true);
            }
        }
    }

    private void updateFontMetrics(float textSizePx) {
        mTextSizePx = textSizePx;
        mTextPaint.setTextSize(textSizePx);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        // Measure average over multiple ASCII glyphs to prevent any single wide glyph from skewing cell width
        float sampleAdvance = mTextPaint.measureText("aaaaaaaaaaMMMMMMMMMM0000000000") / 30f;
        mCellWidthPx = Math.max(5f, sampleAdvance);
        mCellHeightPx = Math.max(10, (int) Math.ceil(fm.descent - fm.ascent + fm.leading));
        mFontAscentPx = (int) Math.ceil(-fm.ascent);
    }

    private void ensureRowNodeCapacity(int rows, int cols) {
        if (mRowNodes.length != rows) {
            mRowNodes = new RenderNode[rows];
            mRowNodeValid = new boolean[rows];
            for (int r = 0; r < rows; ++r) {
                mRowNodes[r] = new RenderNode("Row_" + r);
            }
        } else {
            invalidateAllRowNodes();
        }
        if (mGlyphs.length < cols * 4) {
            mBgRuns = new int[(cols + 32) * 3];
            mGlyphs = new int[(cols + 32) * 4];
        }
    }

    public void invalidateAllRowNodes() {
        for (int i = 0; i < mRowNodeValid.length; ++i) {
            mRowNodeValid[i] = false;
        }
    }

    private void recalculateGrid(int widthPx, int heightPx) {
        if (widthPx <= 0 || heightPx <= 0) return;
        int cols = Math.max(8, (int) (widthPx / mCellWidthPx));
        int rows = Math.max(4, heightPx / mCellHeightPx);
        mVisibleCols = cols;
        mVisibleRows = rows;
        ensureRowNodeCapacity(rows, cols);

        if (mSession != null && mSession.handle != 0) {
            if (!mSession.started) {
                mSession.started = true;
                String nativeLibDir = getContext().getApplicationInfo().nativeLibraryDir;
                NativeBridge.startSession(
                    mSession.handle, mSession.isRoot, nativeLibDir, rows, cols, (int) mCellWidthPx, mCellHeightPx
                );
            } else {
                NativeBridge.resizeEngine(mSession.handle, rows, cols, (int) mCellWidthPx, mCellHeightPx);
            }
        }
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
        if (mSession != null && mSession.handle != 0) {
            if (NativeBridge.consumeFrameDirty(mSession.handle)) {
                if (mScrollOffset != 0) {
                    mScrollOffset = 0;
                    invalidateAllRowNodes();
                }
                invalidate();
            }
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
        if (!mScaleDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(NativeBridge.DEFAULT_BG);
        if (mSession == null || mSession.handle == 0) return;
        long h = mSession.handle;

        if (mScroller.computeScrollOffset()) {
            int maxSb = NativeBridge.getActiveScrollback(h);
            int target = Math.max(0, Math.min(maxSb, mScroller.getCurrY() / mCellHeightPx));
            if (target != mScrollOffset) {
                mScrollOffset = target;
                invalidateAllRowNodes();
            }
            postInvalidateOnAnimation();
        }

        if (mScrollOffset != mLastRenderedScrollOffset) {
            invalidateAllRowNodes();
            mLastRenderedScrollOffset = mScrollOffset;
        }

        int viewWidth = getWidth();
        boolean hwAccelerated = canvas.isHardwareAccelerated();

        for (int r = 0; r < mVisibleRows; ++r) {
            int topPx = r * mCellHeightPx;
            int bottomPx = topPx + mCellHeightPx;
            boolean dirty = (mScrollOffset == 0) ? NativeBridge.isRowDirty(h, r) : !mRowNodeValid[r];

            if (hwAccelerated && r < mRowNodes.length) {
                RenderNode node = mRowNodes[r];
                if (!mRowNodeValid[r] || dirty || !node.hasDisplayList()) {
                    node.setPosition(0, topPx, viewWidth, bottomPx);
                    RecordingCanvas rc = node.beginRecording(viewWidth, mCellHeightPx);
                    try {
                        renderPackedRow(rc, h, r, 0);
                    } finally {
                        node.endRecording();
                    }
                    mRowNodeValid[r] = true;
                }
                canvas.drawRenderNode(node);
            } else {
                renderPackedRow(canvas, h, r, topPx);
            }
        }

        if (mScrollOffset == 0) {
            NativeBridge.clearDirtyFlags(h);
        }

        // Crisp cell-locked cursor overlay
        if (mScrollOffset == 0 && NativeBridge.isCursorVisible(h)) {
            int curRow = NativeBridge.getCursorRow(h);
            int curCol = NativeBridge.getCursorCol(h);
            if (curRow >= 0 && curRow < mVisibleRows && curCol >= 0 && curCol < mVisibleCols) {
                float left = curCol * mCellWidthPx;
                float right = left + mCellWidthPx;
                float top = curRow * mCellHeightPx;
                float bottom = top + mCellHeightPx;
                canvas.drawRect(left, top, right, bottom, mCursorPaint);
            }
        }

        int maxSb = NativeBridge.getActiveScrollback(h);
        if (maxSb > 0 && mScrollOffset > 0) {
            float total = maxSb + mVisibleRows;
            float barH = Math.max(28f, getHeight() * (mVisibleRows / total));
            float frac = 1f - ((float) mScrollOffset / maxSb);
            float barTop = frac * (getHeight() - barH);
            canvas.drawRect(viewWidth - 4f, barTop, viewWidth - 1f, barTop + barH, mScrollbarPaint);
        }
    }

    private void renderPackedRow(Canvas targetCanvas, long handle, int screenRow, int yTopPx) {
        int packedCounts = NativeBridge.packRowRender(handle, mScrollOffset, screenRow, mBgRuns, mGlyphs);
        int bgRunCount = (packedCounts >>> 16) & 0xFFFF;
        int glyphCount = packedCounts & 0xFFFF;

        int yBottomPx = yTopPx + mCellHeightPx;
        int baselineY = yTopPx + mFontAscentPx;

        // Pass 1: Non-black background runs packed by C++23
        for (int i = 0; i < bgRunCount; ++i) {
            int base = i * 3;
            int startCol = mBgRuns[base];
            int endCol   = mBgRuns[base + 1];
            int bgColor  = mBgRuns[base + 2];
            mBgPaint.setColor(bgColor);
            float left = (float) Math.floor(startCol * mCellWidthPx);
            float right = (float) Math.ceil(endCol * mCellWidthPx);
            targetCanvas.drawRect(left, yTopPx, right, yBottomPx, mBgPaint);
        }

        // Pass 2: Strict cell-locked glyph rendering (every glyph anchored at col * mCellWidthPx)
        int prevFg = 0;
        int prevFlags = -1;
        for (int i = 0; i < glyphCount; ++i) {
            int base = i * 4;
            int cp    = mGlyphs[base];
            int col   = mGlyphs[base + 1];
            int fg    = mGlyphs[base + 2];
            int flags = mGlyphs[base + 3];

            if (fg != prevFg) {
                mTextPaint.setColor(fg);
                prevFg = fg;
            }
            if (flags != prevFlags) {
                mTextPaint.setFakeBoldText((flags & NativeBridge.FLAG_BOLD) != 0);
                mTextPaint.setTextSkewX((flags & NativeBridge.FLAG_ITALIC) != 0 ? -0.20f : 0f);
                mTextPaint.setUnderlineText((flags & NativeBridge.FLAG_UNDERLINE) != 0);
                mTextPaint.setStrikeThruText((flags & NativeBridge.FLAG_STRIKETHROUGH) != 0);
                prevFlags = flags;
            }

            int charCount = Character.toChars(cp, mSingleGlyphBuf, 0);
            float x = col * mCellWidthPx;
            targetCanvas.drawText(mSingleGlyphBuf, 0, charCount, x, baselineY, mTextPaint);
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
                if (mSession != null && text != null && text.length() > 0) {
                    sendCommittedChars(text);
                }
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                if (mSession != null && beforeLength > 0) {
                    for (int i = 0; i < beforeLength; ++i) {
                        NativeBridge.sendKeyCode(mSession.handle, KeyEvent.KEYCODE_DEL, 0);
                    }
                }
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                return dispatchKeyEvent(event);
            }
        };
    }

    private void sendCommittedChars(CharSequence text) {
        if (mSession == null || mSession.handle == 0) return;
        boolean ctrl = mModifierProvider != null && mModifierProvider.consumeCtrl();
        boolean alt  = mModifierProvider != null && mModifierProvider.consumeAlt();
        int i = 0;
        while (i < text.length()) {
            int cp = Character.codePointAt(text, i);
            NativeBridge.sendCodepoint(mSession.handle, cp, ctrl, alt);
            ctrl = false;
            alt = false;
            i += Character.charCount(cp);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSession == null || mSession.handle == 0) return super.onKeyDown(keyCode, event);
        long h = mSession.handle;

        if (!NativeBridge.isRunning(h) && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            if (getContext() instanceof TermuxActivity) {
                ((TermuxActivity) getContext()).closeSession(mSession);
            }
            return true;
        }

        boolean extraCtrl = mModifierProvider != null && mModifierProvider.consumeCtrl();
        boolean extraAlt  = mModifierProvider != null && mModifierProvider.consumeAlt();
        int meta = event.getMetaState()
            | (extraCtrl ? KeyEvent.META_CTRL_ON : 0)
            | (extraAlt ? KeyEvent.META_ALT_ON : 0);

        if (NativeBridge.sendKeyCode(h, keyCode, meta)) {
            return true;
        }

        int unicode = event.getUnicodeChar(meta & ~(KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON));
        if (unicode > 0) {
            boolean ctrl = (meta & KeyEvent.META_CTRL_ON) != 0;
            boolean alt  = (meta & KeyEvent.META_ALT_ON) != 0;
            NativeBridge.sendCodepoint(h, unicode, ctrl, alt);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
