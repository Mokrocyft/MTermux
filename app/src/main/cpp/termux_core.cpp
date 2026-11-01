/*
 * Termuxx Neo — Unified C++23 Native Core
 *
 * Subsystems implemented in C++23:
 * 1. Zero-Copy Bootstrap Access & 16-Byte Binary Relocator ("com.termux/files" -> "com.termuxx/file")
 * 2. Native Shell & Root ("su") Environment Builder & PTY Process Launcher
 * 3. Multithreaded Non-Blocking PTY Reader & Process Reaper (pthread + poll)
 * 4. ARM NEON SIMD-Accelerated VT520 / XTerm / 24-Bit TrueColor Terminal Engine
 * 5. Native Key / UTF-8 / Bracketed Paste / SGR Mouse Input Encoder
 * 6. Native Row Render Batch Packer (resolving colors & packing cell coordinates for 165Hz RenderNode UI)
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <jni.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#include <algorithm>
#include <atomic>
#include <string>
#include <vector>

#if defined(__aarch64__)
#include <arm_neon.h>
#endif

extern "C" {
extern const jbyte blob[];
extern const int32_t blob_size;
}

namespace {

// Canonical paths for com.termuxx (note: "/data/data/com.termuxx/file" is 27 bytes,
// matching "/data/data/com.termux/files" byte-for-byte in all ELF binaries!)
constexpr const char APP_ROOT_PATH[]   = "/data/data/com.termuxx";
constexpr const char FILES_REAL_PATH[] = "/data/data/com.termuxx/file";
constexpr const char FILES_LINK_PATH[] = "/data/data/com.termuxx/files";
constexpr const char PREFIX_PATH[]     = "/data/data/com.termuxx/file/usr";
constexpr const char BIN_PATH[]        = "/data/data/com.termuxx/file/usr/bin";
constexpr const char LIB_PATH[]        = "/data/data/com.termuxx/file/usr/lib";
constexpr const char TMP_PATH[]        = "/data/data/com.termuxx/file/usr/tmp";
constexpr const char HOME_PATH[]       = "/data/data/com.termuxx/file/home";
constexpr const char CACHE_PATH[]      = "/data/data/com.termuxx/cach";

constexpr const char OLD_TOKEN_16[] = "com.termux/files";
constexpr const char NEW_TOKEN_16[] = "com.termuxx/file";
constexpr size_t TOKEN_16_LEN = 16;

// Cell style bit flags
constexpr uint16_t FLAG_BOLD          = 1u << 0;
constexpr uint16_t FLAG_DIM           = 1u << 1;
constexpr uint16_t FLAG_ITALIC        = 1u << 2;
constexpr uint16_t FLAG_UNDERLINE     = 1u << 3;
constexpr uint16_t FLAG_BLINK         = 1u << 4;
constexpr uint16_t FLAG_INVERSE       = 1u << 5;
constexpr uint16_t FLAG_INVISIBLE     = 1u << 6;
constexpr uint16_t FLAG_STRIKETHROUGH = 1u << 7;
constexpr uint16_t FLAG_WIDE_LEAD     = 1u << 8;
constexpr uint16_t FLAG_WIDE_TRAIL    = 1u << 9;

// Mode mask bits
constexpr int MODE_APP_CURSOR_KEYS = 1 << 0;
constexpr int MODE_BRACKETED_PASTE = 1 << 1;
constexpr int MODE_ALT_SCREEN      = 1 << 2;
constexpr int MODE_MOUSE_TRACKING  = 1 << 3;
constexpr int MODE_MOUSE_SGR       = 1 << 4;
constexpr int MODE_BELL_TRIGGERED  = 1 << 5;
constexpr int MODE_AUTOWRAP        = 1 << 6;

// Pure Dark AMOLED Palette (#000000 background, crisp #E5E9F0 foreground)
constexpr uint32_t DEFAULT_FG = 0xFFE5E9F0u;
constexpr uint32_t DEFAULT_BG = 0xFF000000u;

// Android KeyEvent constants (matching android.view.KeyEvent)
constexpr int AKEYCODE_ENTER        = 66;
constexpr int AKEYCODE_NUMPAD_ENTER = 160;
constexpr int AKEYCODE_DEL          = 67;
constexpr int AKEYCODE_FORWARD_DEL  = 112;
constexpr int AKEYCODE_INSERT       = 124;
constexpr int AKEYCODE_TAB          = 61;
constexpr int AKEYCODE_ESCAPE       = 111;
constexpr int AKEYCODE_DPAD_UP      = 19;
constexpr int AKEYCODE_DPAD_DOWN    = 20;
constexpr int AKEYCODE_DPAD_LEFT    = 21;
constexpr int AKEYCODE_DPAD_RIGHT   = 22;
constexpr int AKEYCODE_MOVE_HOME    = 122;
constexpr int AKEYCODE_MOVE_END     = 123;
constexpr int AKEYCODE_PAGE_UP      = 92;
constexpr int AKEYCODE_PAGE_DOWN    = 93;
constexpr int AKEYCODE_F1           = 131;
constexpr int AKEYCODE_F2           = 132;
constexpr int AKEYCODE_F3           = 133;
constexpr int AKEYCODE_F4           = 134;
constexpr int AKEYCODE_F5           = 135;
constexpr int AKEYCODE_F6           = 136;
constexpr int AKEYCODE_F7           = 137;
constexpr int AKEYCODE_F8           = 138;
constexpr int AKEYCODE_F9           = 139;
constexpr int AKEYCODE_F10          = 140;
constexpr int AKEYCODE_F11          = 141;
constexpr int AKEYCODE_F12          = 142;

constexpr int AMETA_SHIFT_ON = 1;
constexpr int AMETA_ALT_ON   = 2;
constexpr int AMETA_CTRL_ON  = 4096;

struct alignas(16) Cell {
    uint32_t codepoint;
    uint32_t fg;
    uint32_t bg;
    uint16_t flags;
    uint16_t reserved;

    void reset(uint32_t defaultFg = DEFAULT_FG, uint32_t defaultBg = DEFAULT_BG, uint16_t defaultFlags = 0) noexcept {
        codepoint = ' ';
        fg = defaultFg;
        bg = defaultBg;
        flags = defaultFlags;
        reserved = 0;
    }
};

struct Row {
    std::vector<Cell> cells;
    bool dirty = true;
    bool wrapped = false;

    explicit Row(int cols = 80) : cells(static_cast<size_t>(cols)) {
        clear(DEFAULT_FG, DEFAULT_BG, 0);
    }

    void resize(int newCols) {
        size_t oldSize = cells.size();
        cells.resize(static_cast<size_t>(newCols));
        for (size_t i = oldSize; i < cells.size(); ++i) {
            cells[i].reset();
        }
        dirty = true;
    }

    void clear(uint32_t fg = DEFAULT_FG, uint32_t bg = DEFAULT_BG, uint16_t flags = 0) noexcept {
        for (auto& c : cells) {
            c.reset(fg, bg, flags);
        }
        dirty = true;
        wrapped = false;
    }
};

static int unicodeCharWidth(uint32_t ucs) noexcept {
    if (ucs == 0) return 0;
    if (ucs < 0x20 || (ucs >= 0x7F && ucs < 0xA0)) return -1;
    if (ucs < 0x300) return 1;
    if ((ucs >= 0x0300 && ucs <= 0x036F) ||
        (ucs >= 0x1AB0 && ucs <= 0x1AFF) ||
        (ucs >= 0x1DC0 && ucs <= 0x1DFF) ||
        (ucs >= 0x200B && ucs <= 0x200F) ||
        (ucs >= 0x20D0 && ucs <= 0x20FF) ||
        (ucs >= 0xFE00 && ucs <= 0xFE0F) ||
        (ucs >= 0xFE20 && ucs <= 0xFE2F)) {
        return 0;
    }
    if (ucs >= 0x1100 && (
        ucs <= 0x115F || ucs == 0x2329 || ucs == 0x232A ||
        (ucs >= 0x2E80 && ucs <= 0xA4CF && ucs != 0x303F) ||
        (ucs >= 0xAC00 && ucs <= 0xD7A3) ||
        (ucs >= 0xF900 && ucs <= 0xFAFF) ||
        (ucs >= 0xFE10 && ucs <= 0xFE19) ||
        (ucs >= 0xFE30 && ucs <= 0xFE6F) ||
        (ucs >= 0xFF00 && ucs <= 0xFF60) ||
        (ucs >= 0xFFE0 && ucs <= 0xFFE6) ||
        (ucs >= 0x1F300 && ucs <= 0x1FAFF) ||
        (ucs >= 0x20000 && ucs <= 0x3FFFD))) {
        return 2;
    }
    return 1;
}

static void appendUtf8(std::string& out, uint32_t cp) {
    if (cp <= 0x7F) {
        out.push_back(static_cast<char>(cp));
    } else if (cp <= 0x7FF) {
        out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else if (cp <= 0xFFFF) {
        out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else if (cp <= 0x10FFFF) {
        out.push_back(static_cast<char>(0xF0 | (cp >> 18)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    }
}

static inline uint32_t resolveFgColor(uint32_t fg, uint32_t bg, uint16_t flags) noexcept {
    uint32_t out = (flags & FLAG_INVERSE) ? bg : fg;
    if (flags & FLAG_DIM) {
        uint32_t r = ((out >> 16) & 0xFFu) * 2u / 3u;
        uint32_t g = ((out >> 8) & 0xFFu) * 2u / 3u;
        uint32_t b = (out & 0xFFu) * 2u / 3u;
        out = 0xFF000000u | (r << 16) | (g << 8) | b;
    }
    return out;
}

static inline uint32_t resolveBgColor(uint32_t fg, uint32_t bg, uint16_t flags) noexcept {
    return (flags & FLAG_INVERSE) ? fg : bg;
}

static void patchBufferTokensInPlace(uint8_t* data, size_t len) noexcept {
    if (!data || len < TOKEN_16_LEN) return;
    uint8_t* cur = data;
    size_t rem = len;
    while (rem >= TOKEN_16_LEN) {
        auto* hit = static_cast<uint8_t*>(memmem(cur, rem, OLD_TOKEN_16, TOKEN_16_LEN));
        if (!hit) break;
        memcpy(hit, NEW_TOKEN_16, TOKEN_16_LEN);
        size_t adv = static_cast<size_t>(hit - cur) + TOKEN_16_LEN;
        cur += adv;
        rem -= adv;
    }
}

static void copyFileBinary(const char* srcPath, const char* dstPath, mode_t mode) noexcept {
    int inFd = open(srcPath, O_RDONLY | O_CLOEXEC);
    if (inFd < 0) return;
    int outFd = open(dstPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, mode);
    if (outFd < 0) {
        close(inFd);
        return;
    }
    uint8_t buf[32768];
    ssize_t n;
    while ((n = read(inFd, buf, sizeof(buf))) > 0) {
        (void)write(outFd, buf, static_cast<size_t>(n));
    }
    close(outFd);
    close(inFd);
    chmod(dstPath, mode);
}

static void writeAllFd(int fd, const void* buf, size_t len) noexcept {
    if (fd < 0 || !buf || len == 0) return;
    const auto* p = static_cast<const uint8_t*>(buf);
    size_t left = len;
    while (left > 0) {
        ssize_t n = write(fd, p, left);
        if (n < 0) {
            if (errno == EINTR) continue;
            break;
        }
        p += n;
        left -= static_cast<size_t>(n);
    }
}

class NeoTerminalEngine {
public:
    mutable pthread_mutex_t mMutex;
    int mRows;
    int mCols;
    int mMaxScrollback;

    std::vector<Row> mPrimaryGrid;
    std::vector<Row> mAltGrid;
    std::vector<Row> mScrollback;
    int mScrollbackHead = 0;
    int mScrollbackCount = 0;

    int mCursorRow = 0;
    int mCursorCol = 0;
    bool mCursorVisible = true;
    bool mPendingWrap = false;

    int mSavedRowPrimary = 0, mSavedColPrimary = 0;
    uint32_t mSavedFg = DEFAULT_FG, mSavedBg = DEFAULT_BG;
    uint16_t mSavedFlags = 0;

    int mScrollTop = 0;
    int mScrollBottom = 0;

    uint32_t mCurFg = DEFAULT_FG;
    uint32_t mCurBg = DEFAULT_BG;
    uint16_t mCurFlags = 0;

    int mModes = MODE_AUTOWRAP;
    uint32_t mPalette[256];

    enum class State { NORMAL, ESC, CSI, OSC, OSC_ESC, CHARSET };
    State mState = State::NORMAL;
    char mCsiPrefix = 0;
    char mCsiIntermediate = 0;
    std::vector<int> mCsiParams;
    int mCurParam = -1;
    std::string mOscBuffer;

    uint32_t mUtf8Codepoint = 0;
    int mUtf8BytesNeeded = 0;

    std::string mWindowTitle;
    bool mTitleChanged = false;

    // Native PTY & Thread state
    int mPtyFd = -1;
    int mShellPid = -1;
    bool mIsRoot = false;
    std::atomic<bool> mRunning{false};
    std::atomic<bool> mFrameDirty{true};
    std::atomic<int> mExitCode{0};
    pthread_t mReaderThread{};
    bool mThreadStarted = false;

    NeoTerminalEngine(int rows, int cols, int maxScrollback)
        : mRows(std::max(2, rows)),
          mCols(std::max(4, cols)),
          mMaxScrollback(std::max(100, maxScrollback)),
          mScrollTop(0),
          mScrollBottom(std::max(2, rows) - 1) {
        pthread_mutex_init(&mMutex, nullptr);
        initPalette();
        mPrimaryGrid.reserve(static_cast<size_t>(mRows));
        mAltGrid.reserve(static_cast<size_t>(mRows));
        for (int r = 0; r < mRows; ++r) {
            mPrimaryGrid.emplace_back(mCols);
            mAltGrid.emplace_back(mCols);
        }
        mScrollback.resize(static_cast<size_t>(mMaxScrollback), Row(mCols));
    }

    ~NeoTerminalEngine() {
        stopSession();
        pthread_mutex_destroy(&mMutex);
    }

    void lock() const noexcept { pthread_mutex_lock(&mMutex); }
    void unlock() const noexcept { pthread_mutex_unlock(&mMutex); }

    void initPalette() noexcept {
        static constexpr uint32_t base16[16] = {
            0xFF000000u, // 0: Pure AMOLED Black
            0xFFE06C75u, // 1: Red
            0xFF98C379u, // 2: Green
            0xFFE5C07Bu, // 3: Yellow
            0xFF61AFEFu, // 4: Blue
            0xFFC678DDu, // 5: Magenta
            0xFF56B6C2u, // 6: Cyan
            0xFFABB2BFu, // 7: White
            0xFF5C6370u, // 8: Bright Black (Gray)
            0xFFFF7A85u, // 9: Bright Red
            0xFFB5E890u, // 10: Bright Green
            0xFFFFD68Au, // 11: Bright Yellow
            0xFF82C8FFu, // 12: Bright Blue
            0xFFDE98F5u, // 13: Bright Magenta
            0xFF70D6E3u, // 14: Bright Cyan
            0xFFFFFFFFu  // 15: Bright White
        };
        for (int i = 0; i < 16; ++i) mPalette[i] = base16[i];
        static constexpr uint8_t cube[6] = {0, 95, 135, 175, 215, 255};
        for (int i = 0; i < 216; ++i) {
            uint32_t r = cube[(i / 36) % 6];
            uint32_t g = cube[(i / 6) % 6];
            uint32_t b = cube[i % 6];
            mPalette[16 + i] = 0xFF000000u | (r << 16) | (g << 8) | b;
        }
        for (int i = 0; i < 24; ++i) {
            uint32_t v = static_cast<uint32_t>(8 + i * 10);
            mPalette[232 + i] = 0xFF000000u | (v << 16) | (v << 8) | v;
        }
    }

    std::vector<Row>& activeGrid() noexcept {
        return (mModes & MODE_ALT_SCREEN) ? mAltGrid : mPrimaryGrid;
    }

    void markAllDirty() noexcept {
        for (auto& r : mPrimaryGrid) r.dirty = true;
        for (auto& r : mAltGrid) r.dirty = true;
        mFrameDirty.store(true, std::memory_order_release);
    }

    void reset() noexcept {
        mCurFg = DEFAULT_FG;
        mCurBg = DEFAULT_BG;
        mCurFlags = 0;
        mCursorRow = 0;
        mCursorCol = 0;
        mCursorVisible = true;
        mPendingWrap = false;
        mScrollTop = 0;
        mScrollBottom = mRows - 1;
        mModes = MODE_AUTOWRAP;
        mState = State::NORMAL;
        mUtf8BytesNeeded = 0;
        for (auto& r : mPrimaryGrid) r.clear();
        for (auto& r : mAltGrid) r.clear();
        mFrameDirty.store(true, std::memory_order_release);
    }

    void resize(int newRows, int newCols, int cellW = 18, int cellH = 36) {
        newRows = std::max(2, newRows);
        newCols = std::max(4, newCols);
        lock();
        if (newRows != mRows || newCols != mCols) {
            mPrimaryGrid.resize(static_cast<size_t>(newRows), Row(newCols));
            mAltGrid.resize(static_cast<size_t>(newRows), Row(newCols));
            for (auto& r : mPrimaryGrid) r.resize(newCols);
            for (auto& r : mAltGrid) r.resize(newCols);
            for (auto& r : mScrollback) r.resize(newCols);

            mRows = newRows;
            mCols = newCols;
            mScrollTop = 0;
            mScrollBottom = mRows - 1;
            mCursorRow = std::clamp(mCursorRow, 0, mRows - 1);
            mCursorCol = std::clamp(mCursorCol, 0, mCols - 1);
            mPendingWrap = false;
            markAllDirty();
        }
        int fd = mPtyFd;
        unlock();

        if (fd >= 0) {
            struct winsize sz{};
            sz.ws_row = static_cast<unsigned short>(newRows);
            sz.ws_col = static_cast<unsigned short>(newCols);
            sz.ws_xpixel = static_cast<unsigned short>(newCols * std::max(4, cellW));
            sz.ws_ypixel = static_cast<unsigned short>(newRows * std::max(8, cellH));
            ioctl(fd, TIOCSWINSZ, &sz);
        }
    }

    void pushScrollbackRow(const Row& row) noexcept {
        if (mModes & MODE_ALT_SCREEN) return;
        int idx = (mScrollbackHead + mScrollbackCount) % mMaxScrollback;
        if (mScrollbackCount < mMaxScrollback) {
            mScrollback[static_cast<size_t>(idx)] = row;
            mScrollbackCount++;
        } else {
            mScrollback[static_cast<size_t>(mScrollbackHead)] = row;
            mScrollbackHead = (mScrollbackHead + 1) % mMaxScrollback;
        }
    }

    void scrollUp(int top, int bottom, int count) noexcept {
        auto& grid = activeGrid();
        top = std::clamp(top, 0, mRows - 1);
        bottom = std::clamp(bottom, top, mRows - 1);
        count = std::clamp(count, 1, bottom - top + 1);

        for (int n = 0; n < count; ++n) {
            if (top == 0 && !(mModes & MODE_ALT_SCREEN)) {
                pushScrollbackRow(grid[0]);
            }
            for (int r = top; r < bottom; ++r) {
                grid[static_cast<size_t>(r)] = std::move(grid[static_cast<size_t>(r + 1)]);
                grid[static_cast<size_t>(r)].dirty = true;
            }
            grid[static_cast<size_t>(bottom)] = Row(mCols);
            grid[static_cast<size_t>(bottom)].clear(mCurFg, mCurBg, 0);
        }
    }

    void scrollDown(int top, int bottom, int count) noexcept {
        auto& grid = activeGrid();
        top = std::clamp(top, 0, mRows - 1);
        bottom = std::clamp(bottom, top, mRows - 1);
        count = std::clamp(count, 1, bottom - top + 1);

        for (int n = 0; n < count; ++n) {
            for (int r = bottom; r > top; --r) {
                grid[static_cast<size_t>(r)] = std::move(grid[static_cast<size_t>(r - 1)]);
                grid[static_cast<size_t>(r)].dirty = true;
            }
            grid[static_cast<size_t>(top)] = Row(mCols);
            grid[static_cast<size_t>(top)].clear(mCurFg, mCurBg, 0);
        }
    }

    void lineFeed() noexcept {
        mPendingWrap = false;
        if (mCursorRow == mScrollBottom) {
            scrollUp(mScrollTop, mScrollBottom, 1);
        } else if (mCursorRow < mRows - 1) {
            mCursorRow++;
        }
        activeGrid()[static_cast<size_t>(mCursorRow)].dirty = true;
    }

    void reverseIndex() noexcept {
        mPendingWrap = false;
        if (mCursorRow == mScrollTop) {
            scrollDown(mScrollTop, mScrollBottom, 1);
        } else if (mCursorRow > 0) {
            mCursorRow--;
        }
        activeGrid()[static_cast<size_t>(mCursorRow)].dirty = true;
    }

    void emitCodepoint(uint32_t cp) noexcept {
        int w = unicodeCharWidth(cp);
        if (w <= 0) return;

        auto& grid = activeGrid();
        if (mPendingWrap && (mModes & MODE_AUTOWRAP)) {
            grid[static_cast<size_t>(mCursorRow)].wrapped = true;
            mCursorCol = 0;
            lineFeed();
        }

        if (w == 2 && mCursorCol == mCols - 1) {
            grid[static_cast<size_t>(mCursorRow)].cells[static_cast<size_t>(mCursorCol)].reset(mCurFg, mCurBg, mCurFlags);
            grid[static_cast<size_t>(mCursorRow)].dirty = true;
            if (mModes & MODE_AUTOWRAP) {
                grid[static_cast<size_t>(mCursorRow)].wrapped = true;
                mCursorCol = 0;
                lineFeed();
            }
        }

        Row& row = grid[static_cast<size_t>(mCursorRow)];
        row.dirty = true;

        Cell& target = row.cells[static_cast<size_t>(mCursorCol)];
        if ((target.flags & FLAG_WIDE_TRAIL) && mCursorCol > 0) {
            row.cells[static_cast<size_t>(mCursorCol - 1)].reset(mCurFg, mCurBg, 0);
        } else if ((target.flags & FLAG_WIDE_LEAD) && mCursorCol + 1 < mCols) {
            row.cells[static_cast<size_t>(mCursorCol + 1)].reset(mCurFg, mCurBg, 0);
        }

        target.codepoint = cp;
        target.fg = mCurFg;
        target.bg = mCurBg;
        target.flags = mCurFlags | (w == 2 ? FLAG_WIDE_LEAD : 0);

        if (w == 2 && mCursorCol + 1 < mCols) {
            Cell& trail = row.cells[static_cast<size_t>(mCursorCol + 1)];
            trail.codepoint = ' ';
            trail.fg = mCurFg;
            trail.bg = mCurBg;
            trail.flags = mCurFlags | FLAG_WIDE_TRAIL;
            if (mCursorCol + 2 < mCols) {
                mCursorCol += 2;
            } else {
                mCursorCol = mCols - 1;
                mPendingWrap = true;
            }
        } else {
            if (mCursorCol + 1 < mCols) {
                mCursorCol++;
            } else {
                mPendingWrap = true;
            }
        }
    }

    void handleControlChar(uint8_t b) noexcept {
        switch (b) {
            case 0x07:
                mModes |= MODE_BELL_TRIGGERED;
                break;
            case 0x08:
                mPendingWrap = false;
                if (mCursorCol > 0) mCursorCol--;
                break;
            case 0x09: {
                mPendingWrap = false;
                int nextTab = (mCursorCol & ~7) + 8;
                mCursorCol = std::min(mCols - 1, nextTab);
                break;
            }
            case 0x0A:
            case 0x0B:
            case 0x0C:
                lineFeed();
                break;
            case 0x0D:
                mPendingWrap = false;
                mCursorCol = 0;
                break;
            case 0x1B:
                mState = State::ESC;
                break;
            default:
                break;
        }
    }

    void handleSgr() noexcept {
        if (mCsiParams.empty()) {
            mCsiParams.push_back(0);
        }
        for (size_t i = 0; i < mCsiParams.size(); ++i) {
            int p = mCsiParams[i];
            if (p <= 0) {
                mCurFg = DEFAULT_FG;
                mCurBg = DEFAULT_BG;
                mCurFlags = 0;
            } else if (p == 1) {
                mCurFlags |= FLAG_BOLD;
            } else if (p == 2) {
                mCurFlags |= FLAG_DIM;
            } else if (p == 3) {
                mCurFlags |= FLAG_ITALIC;
            } else if (p == 4) {
                mCurFlags |= FLAG_UNDERLINE;
            } else if (p == 5 || p == 6) {
                mCurFlags |= FLAG_BLINK;
            } else if (p == 7) {
                mCurFlags |= FLAG_INVERSE;
            } else if (p == 8) {
                mCurFlags |= FLAG_INVISIBLE;
            } else if (p == 9) {
                mCurFlags |= FLAG_STRIKETHROUGH;
            } else if (p == 21 || p == 22) {
                mCurFlags &= ~(FLAG_BOLD | FLAG_DIM);
            } else if (p == 23) {
                mCurFlags &= ~FLAG_ITALIC;
            } else if (p == 24) {
                mCurFlags &= ~FLAG_UNDERLINE;
            } else if (p == 25) {
                mCurFlags &= ~FLAG_BLINK;
            } else if (p == 27) {
                mCurFlags &= ~FLAG_INVERSE;
            } else if (p == 28) {
                mCurFlags &= ~FLAG_INVISIBLE;
            } else if (p == 29) {
                mCurFlags &= ~FLAG_STRIKETHROUGH;
            } else if (p >= 30 && p <= 37) {
                mCurFg = mPalette[p - 30];
            } else if (p == 39) {
                mCurFg = DEFAULT_FG;
            } else if (p >= 40 && p <= 47) {
                mCurBg = mPalette[p - 40];
            } else if (p == 49) {
                mCurBg = DEFAULT_BG;
            } else if (p >= 90 && p <= 97) {
                mCurFg = mPalette[8 + (p - 90)];
            } else if (p >= 100 && p <= 107) {
                mCurBg = mPalette[8 + (p - 100)];
            } else if (p == 38 || p == 48) {
                bool isFg = (p == 38);
                if (i + 1 < mCsiParams.size()) {
                    int mode = mCsiParams[i + 1];
                    if (mode == 5 && i + 2 < mCsiParams.size()) {
                        int idx = std::clamp(mCsiParams[i + 2], 0, 255);
                        if (isFg) mCurFg = mPalette[idx];
                        else mCurBg = mPalette[idx];
                        i += 2;
                    } else if (mode == 2 && i + 4 < mCsiParams.size()) {
                        uint32_t r = static_cast<uint32_t>(std::clamp(mCsiParams[i + 2], 0, 255));
                        uint32_t g = static_cast<uint32_t>(std::clamp(mCsiParams[i + 3], 0, 255));
                        uint32_t b = static_cast<uint32_t>(std::clamp(mCsiParams[i + 4], 0, 255));
                        uint32_t color = 0xFF000000u | (r << 16) | (g << 8) | b;
                        if (isFg) mCurFg = color;
                        else mCurBg = color;
                        i += 4;
                    }
                }
            }
        }
    }

    void setDecPrivateMode(int mode, bool enabled) noexcept {
        switch (mode) {
            case 1:
                if (enabled) mModes |= MODE_APP_CURSOR_KEYS;
                else mModes &= ~MODE_APP_CURSOR_KEYS;
                break;
            case 7:
                if (enabled) mModes |= MODE_AUTOWRAP;
                else mModes &= ~MODE_AUTOWRAP;
                break;
            case 25:
                mCursorVisible = enabled;
                break;
            case 47:
            case 1047:
            case 1049: {
                bool currentlyAlt = (mModes & MODE_ALT_SCREEN) != 0;
                if (enabled && !currentlyAlt) {
                    mSavedRowPrimary = mCursorRow;
                    mSavedColPrimary = mCursorCol;
                    mModes |= MODE_ALT_SCREEN;
                    for (auto& r : mAltGrid) r.clear(DEFAULT_FG, DEFAULT_BG, 0);
                    mCursorRow = 0;
                    mCursorCol = 0;
                    mPendingWrap = false;
                    markAllDirty();
                } else if (!enabled && currentlyAlt) {
                    mModes &= ~MODE_ALT_SCREEN;
                    mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                    mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                    mPendingWrap = false;
                    markAllDirty();
                }
                break;
            }
            case 1000:
            case 1002:
            case 1003:
                if (enabled) mModes |= MODE_MOUSE_TRACKING;
                else mModes &= ~MODE_MOUSE_TRACKING;
                break;
            case 1006:
                if (enabled) mModes |= MODE_MOUSE_SGR;
                else mModes &= ~MODE_MOUSE_SGR;
                break;
            case 2004:
                if (enabled) mModes |= MODE_BRACKETED_PASTE;
                else mModes &= ~MODE_BRACKETED_PASTE;
                break;
            default:
                break;
        }
    }

    void dispatchCsi(char finalByte, std::string& reply) {
        if (mCurParam >= 0 || !mCsiParams.empty()) {
            mCsiParams.push_back(mCurParam < 0 ? 0 : mCurParam);
        }
        auto param = [&](size_t idx, int defVal) -> int {
            if (idx >= mCsiParams.size() || mCsiParams[idx] <= 0) return defVal;
            return mCsiParams[idx];
        };

        auto& grid = activeGrid();

        if (mCsiPrefix == '?') {
            if (finalByte == 'h' || finalByte == 'l') {
                bool en = (finalByte == 'h');
                for (int p : mCsiParams) setDecPrivateMode(p, en);
            }
            return;
        }

        switch (finalByte) {
            case 'A':
                mPendingWrap = false;
                mCursorRow = std::max(mScrollTop, mCursorRow - param(0, 1));
                break;
            case 'B':
                mPendingWrap = false;
                mCursorRow = std::min(mScrollBottom, mCursorRow + param(0, 1));
                break;
            case 'C':
                mPendingWrap = false;
                mCursorCol = std::min(mCols - 1, mCursorCol + param(0, 1));
                break;
            case 'D':
                mPendingWrap = false;
                mCursorCol = std::max(0, mCursorCol - param(0, 1));
                break;
            case 'E':
                mPendingWrap = false;
                mCursorCol = 0;
                mCursorRow = std::min(mScrollBottom, mCursorRow + param(0, 1));
                break;
            case 'F':
                mPendingWrap = false;
                mCursorCol = 0;
                mCursorRow = std::max(mScrollTop, mCursorRow - param(0, 1));
                break;
            case 'G':
            case '`':
                mPendingWrap = false;
                mCursorCol = std::clamp(param(0, 1) - 1, 0, mCols - 1);
                break;
            case 'H':
            case 'f':
                mPendingWrap = false;
                mCursorRow = std::clamp(param(0, 1) - 1, 0, mRows - 1);
                mCursorCol = std::clamp(param(1, 1) - 1, 0, mCols - 1);
                break;
            case 'd':
                mPendingWrap = false;
                mCursorRow = std::clamp(param(0, 1) - 1, 0, mRows - 1);
                break;
            case 'J': {
                int mode = mCsiParams.empty() ? 0 : mCsiParams[0];
                if (mode == 0) {
                    Row& cur = grid[static_cast<size_t>(mCursorRow)];
                    for (int c = mCursorCol; c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                    cur.dirty = true;
                    for (int r = mCursorRow + 1; r < mRows; ++r) grid[static_cast<size_t>(r)].clear(mCurFg, mCurBg, 0);
                } else if (mode == 1) {
                    for (int r = 0; r < mCursorRow; ++r) grid[static_cast<size_t>(r)].clear(mCurFg, mCurBg, 0);
                    Row& cur = grid[static_cast<size_t>(mCursorRow)];
                    for (int c = 0; c <= mCursorCol && c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                    cur.dirty = true;
                } else if (mode == 2 || mode == 3) {
                    for (int r = 0; r < mRows; ++r) grid[static_cast<size_t>(r)].clear(mCurFg, mCurBg, 0);
                    if (mode == 3) {
                        mScrollbackHead = 0;
                        mScrollbackCount = 0;
                    }
                }
                break;
            }
            case 'K': {
                int mode = mCsiParams.empty() ? 0 : mCsiParams[0];
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                if (mode == 0) {
                    for (int c = mCursorCol; c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                } else if (mode == 1) {
                    for (int c = 0; c <= mCursorCol && c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                } else if (mode == 2) {
                    cur.clear(mCurFg, mCurBg, 0);
                }
                break;
            }
            case 'L':
                if (mCursorRow >= mScrollTop && mCursorRow <= mScrollBottom) {
                    scrollDown(mCursorRow, mScrollBottom, param(0, 1));
                }
                break;
            case 'M':
                if (mCursorRow >= mScrollTop && mCursorRow <= mScrollBottom) {
                    scrollUp(mCursorRow, mScrollBottom, param(0, 1));
                }
                break;
            case '@': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                for (int c = mCols - 1; c >= mCursorCol + count; --c) {
                    cur.cells[static_cast<size_t>(c)] = cur.cells[static_cast<size_t>(c - count)];
                }
                for (int c = mCursorCol; c < mCursorCol + count; ++c) {
                    cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                }
                break;
            }
            case 'P': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                for (int c = mCursorCol; c < mCols - count; ++c) {
                    cur.cells[static_cast<size_t>(c)] = cur.cells[static_cast<size_t>(c + count)];
                }
                for (int c = mCols - count; c < mCols; ++c) {
                    cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                }
                break;
            }
            case 'X': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                for (int c = mCursorCol; c < mCursorCol + count; ++c) {
                    cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                }
                break;
            }
            case 'S':
                scrollUp(mScrollTop, mScrollBottom, param(0, 1));
                break;
            case 'T':
                scrollDown(mScrollTop, mScrollBottom, param(0, 1));
                break;
            case 'r': {
                int top = std::clamp(param(0, 1) - 1, 0, mRows - 1);
                int bottom = std::clamp(param(1, mRows) - 1, 0, mRows - 1);
                if (top < bottom) {
                    mScrollTop = top;
                    mScrollBottom = bottom;
                    mCursorRow = 0;
                    mCursorCol = 0;
                    mPendingWrap = false;
                }
                break;
            }
            case 'm':
                handleSgr();
                break;
            case 's':
                mSavedRowPrimary = mCursorRow;
                mSavedColPrimary = mCursorCol;
                break;
            case 'u':
                mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                mPendingWrap = false;
                break;
            case 'n': {
                int p = param(0, 0);
                if (p == 5) {
                    reply += "\x1b[0n";
                } else if (p == 6) {
                    char buf[32];
                    snprintf(buf, sizeof(buf), "\x1b[%d;%dR", mCursorRow + 1, mCursorCol + 1);
                    reply += buf;
                }
                break;
            }
            case 'c':
                if (mCsiPrefix == 0) {
                    reply += "\x1b[?62;22c";
                } else if (mCsiPrefix == '>') {
                    reply += "\x1b[>41;350;0c";
                }
                break;
            default:
                break;
        }
    }

    void handleOscComplete() noexcept {
        size_t semi = mOscBuffer.find(';');
        if (semi == std::string::npos) return;
        int code = atoi(mOscBuffer.substr(0, semi).c_str());
        if (code == 0 || code == 2) {
            mWindowTitle = mOscBuffer.substr(semi + 1);
            mTitleChanged = true;
        }
    }

    void processStream(const uint8_t* data, size_t len, std::string& reply) {
        size_t i = 0;
        while (i < len) {
            if (mState == State::NORMAL && mUtf8BytesNeeded == 0) {
#if defined(__aarch64__)
                while (i + 16 <= len && !mPendingWrap && (mCursorCol + 16 < mCols)) {
                    uint8x16_t chunk = vld1q_u8(data + i);
                    uint8x16_t ge20 = vcgeq_u8(chunk, vdupq_n_u8(0x20));
                    uint8x16_t le7e = vcleq_u8(chunk, vdupq_n_u8(0x7E));
                    uint8x16_t valid = vandq_u8(ge20, le7e);
                    if (vminvq_u8(valid) != 0xFF) break;

                    Row& row = activeGrid()[static_cast<size_t>(mCursorRow)];
                    row.dirty = true;
                    for (int k = 0; k < 16; ++k) {
                        Cell& c = row.cells[static_cast<size_t>(mCursorCol + k)];
                        c.codepoint = data[i + k];
                        c.fg = mCurFg;
                        c.bg = mCurBg;
                        c.flags = mCurFlags;
                    }
                    mCursorCol += 16;
                    i += 16;
                }
                if (i >= len) break;
#endif
                uint8_t b = data[i];
                if (b >= 0x20 && b <= 0x7E) {
                    emitCodepoint(b);
                    i++;
                    continue;
                } else if (b < 0x20 || b == 0x7F) {
                    if (b != 0x7F) handleControlChar(b);
                    i++;
                    continue;
                }
            }

            uint8_t b = data[i++];

            if (mState == State::NORMAL) {
                if ((b & 0x80) == 0) {
                    mUtf8BytesNeeded = 0;
                    if (b < 0x20) handleControlChar(b);
                    else if (b < 0x7F) emitCodepoint(b);
                } else if ((b & 0xC0) == 0x80) {
                    if (mUtf8BytesNeeded > 0) {
                        mUtf8Codepoint = (mUtf8Codepoint << 6) | (b & 0x3F);
                        if (--mUtf8BytesNeeded == 0) {
                            emitCodepoint(mUtf8Codepoint);
                        }
                    }
                } else if ((b & 0xE0) == 0xC0) {
                    mUtf8Codepoint = b & 0x1F;
                    mUtf8BytesNeeded = 1;
                } else if ((b & 0xF0) == 0xE0) {
                    mUtf8Codepoint = b & 0x0F;
                    mUtf8BytesNeeded = 2;
                } else if ((b & 0xF8) == 0xF0) {
                    mUtf8Codepoint = b & 0x07;
                    mUtf8BytesNeeded = 3;
                } else {
                    mUtf8BytesNeeded = 0;
                }
                continue;
            }

            switch (mState) {
                case State::ESC:
                    if (b == '[') {
                        mState = State::CSI;
                        mCsiPrefix = 0;
                        mCsiIntermediate = 0;
                        mCsiParams.clear();
                        mCurParam = -1;
                    } else if (b == ']') {
                        mState = State::OSC;
                        mOscBuffer.clear();
                    } else if (b == '(' || b == ')' || b == '*' || b == '+') {
                        mState = State::CHARSET;
                    } else {
                        if (b == '7') {
                            mSavedRowPrimary = mCursorRow;
                            mSavedColPrimary = mCursorCol;
                            mSavedFg = mCurFg;
                            mSavedBg = mCurBg;
                            mSavedFlags = mCurFlags;
                        } else if (b == '8') {
                            mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                            mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                            mCurFg = mSavedFg;
                            mCurBg = mSavedBg;
                            mCurFlags = mSavedFlags;
                            mPendingWrap = false;
                        } else if (b == 'D') {
                            lineFeed();
                        } else if (b == 'E') {
                            mCursorCol = 0;
                            lineFeed();
                        } else if (b == 'M') {
                            reverseIndex();
                        } else if (b == 'c') {
                            reset();
                        }
                        mState = State::NORMAL;
                    }
                    break;

                case State::CHARSET:
                    mState = State::NORMAL;
                    break;

                case State::CSI:
                    if (b == 0x1B) {
                        mState = State::ESC;
                    } else if (b >= '0' && b <= '9') {
                        mCurParam = (mCurParam < 0 ? 0 : mCurParam * 10) + (b - '0');
                    } else if (b == ';' || b == ':') {
                        mCsiParams.push_back(mCurParam < 0 ? 0 : mCurParam);
                        mCurParam = -1;
                    } else if (b >= '<' && b <= '?') {
                        mCsiPrefix = static_cast<char>(b);
                    } else if (b >= 0x20 && b <= 0x2F) {
                        mCsiIntermediate = static_cast<char>(b);
                    } else if (b >= 0x40 && b <= 0x7E) {
                        dispatchCsi(static_cast<char>(b), reply);
                        mState = State::NORMAL;
                    } else if (b < 0x20) {
                        handleControlChar(b);
                    }
                    break;

                case State::OSC:
                    if (b == 0x07) {
                        handleOscComplete();
                        mState = State::NORMAL;
                    } else if (b == 0x1B) {
                        mState = State::OSC_ESC;
                    } else if (mOscBuffer.size() < 1024) {
                        mOscBuffer.push_back(static_cast<char>(b));
                    }
                    break;

                case State::OSC_ESC:
                    if (b == '\\') {
                        handleOscComplete();
                    }
                    mState = State::NORMAL;
                    break;

                default:
                    mState = State::NORMAL;
                    break;
            }
        }
        mFrameDirty.store(true, std::memory_order_release);
    }

    const Row& getVisualRow(int scrollOffset, int screenRow) const noexcept {
        const auto& grid = (mModes & MODE_ALT_SCREEN) ? mAltGrid : mPrimaryGrid;
        if ((mModes & MODE_ALT_SCREEN) || scrollOffset <= 0 || mScrollbackCount == 0) {
            int r = std::clamp(screenRow, 0, mRows - 1);
            return grid[static_cast<size_t>(r)];
        }
        int clampedOffset = std::min(scrollOffset, mScrollbackCount);
        int virtualIndex = screenRow - clampedOffset;
        if (virtualIndex < 0) {
            int sbLogical = mScrollbackCount + virtualIndex;
            int sbPhysical = (mScrollbackHead + sbLogical) % mMaxScrollback;
            return mScrollback[static_cast<size_t>(sbPhysical)];
        }
        int r = std::clamp(virtualIndex, 0, mRows - 1);
        return grid[static_cast<size_t>(r)];
    }

    static void* readerThreadEntry(void* arg) {
        static_cast<NeoTerminalEngine*>(arg)->readerLoop();
        return nullptr;
    }

    void readerLoop() {
        uint8_t buf[65536];
        std::string reply;
        reply.reserve(64);

        while (mRunning.load(std::memory_order_acquire)) {
            int fd = mPtyFd;
            if (fd < 0) break;

            struct pollfd pfd{};
            pfd.fd = fd;
            pfd.events = POLLIN | POLLERR | POLLHUP;
            int prc = poll(&pfd, 1, 250);
            if (prc < 0) {
                if (errno == EINTR) continue;
                break;
            }
            if (prc == 0) continue;

            if (pfd.revents & (POLLIN | POLLERR | POLLHUP)) {
                ssize_t n = read(fd, buf, sizeof(buf));
                if (n <= 0) {
                    if (n < 0 && (errno == EINTR || errno == EAGAIN)) continue;
                    break;
                }
                reply.clear();
                lock();
                processStream(buf, static_cast<size_t>(n), reply);
                unlock();
                if (!reply.empty()) {
                    writeAllFd(fd, reply.data(), reply.size());
                }
            }
        }

        int status = 0;
        if (mShellPid > 0) {
            while (waitpid(mShellPid, &status, 0) < 0) {
                if (errno != EINTR) break;
            }
            if (WIFEXITED(status)) mExitCode.store(WEXITSTATUS(status), std::memory_order_release);
            else if (WIFSIGNALED(status)) mExitCode.store(-WTERMSIG(status), std::memory_order_release);
        }

        mRunning.store(false, std::memory_order_release);
        if (mPtyFd >= 0) {
            close(mPtyFd);
            mPtyFd = -1;
        }

        char exitMsg[128];
        snprintf(exitMsg, sizeof(exitMsg),
                 "\r\n\x1b[1;90m[Process exited (%d) — press Enter]\x1b[0m\r\n",
                 mExitCode.load(std::memory_order_relaxed));
        std::string dummy;
        lock();
        processStream(reinterpret_cast<const uint8_t*>(exitMsg), strlen(exitMsg), dummy);
        unlock();
    }

    void stopSession() noexcept {
        mRunning.store(false, std::memory_order_release);
        if (mShellPid > 0) {
            kill(mShellPid, SIGHUP);
        }
        if (mPtyFd >= 0) {
            close(mPtyFd);
            mPtyFd = -1;
        }
        if (mThreadStarted) {
            pthread_join(mReaderThread, nullptr);
            mThreadStarted = false;
        }
    }

    void writeToPty(const void* data, size_t len) noexcept {
        if (!mRunning.load(std::memory_order_relaxed) || mPtyFd < 0) return;
        writeAllFd(mPtyFd, data, len);
    }
};

static const char* resolveExecutable(const char* const* candidates, size_t count) noexcept {
    for (size_t i = 0; i < count; ++i) {
        if (access(candidates[i], X_OK) == 0) return candidates[i];
    }
    return candidates[count - 1];
}

static bool startSessionProcess(
    NeoTerminalEngine* engine,
    bool asRoot,
    const char* nativeLibDir,
    int rows,
    int cols,
    int cellW,
    int cellH) noexcept {

    engine->mIsRoot = asRoot;
    engine->resize(rows, cols, cellW, cellH);

    // Ensure directories exist
    mkdir(FILES_REAL_PATH, 0700);
    mkdir(HOME_PATH, 0700);
    mkdir(TMP_PATH, 0700);
    mkdir(CACHE_PATH, 0700);

    // Copy libtermuxx-redirect.so into $PREFIX/lib if not yet present
    std::string preloadPath = std::string(LIB_PATH) + "/libtermuxx-redirect.so";
    if (nativeLibDir && nativeLibDir[0] != '\0') {
        std::string apkSo = std::string(nativeLibDir) + "/libtermuxx-redirect.so";
        if (access(apkSo.c_str(), R_OK) == 0) {
            copyFileBinary(apkSo.c_str(), preloadPath.c_str(), 0755);
        }
    }

    static const char* const userShells[] = {
        "/data/data/com.termuxx/file/usr/bin/zsh",
        "/data/data/com.termuxx/file/usr/bin/bash",
        "/data/data/com.termuxx/file/usr/bin/sh",
        "/system/bin/sh"
    };
    static const char* const suBins[] = {
        "/debug_ramdisk/su",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su",
        "/data/data/com.termuxx/file/usr/bin/su"
    };

    const char* userShell = resolveExecutable(userShells, 4);
    const char* loginBin = "/data/data/com.termuxx/file/usr/bin/login";
    bool hasLogin = (access(loginBin, X_OK) == 0);

    std::vector<std::string> envStrings;
    envStrings.emplace_back("TERM=xterm-256color");
    envStrings.emplace_back("COLORTERM=truecolor");
    envStrings.emplace_back("HOME=" + std::string(HOME_PATH));
    envStrings.emplace_back("PREFIX=" + std::string(PREFIX_PATH));
    envStrings.emplace_back("TMPDIR=" + std::string(TMP_PATH));
    envStrings.emplace_back("TMP=" + std::string(TMP_PATH));
    envStrings.emplace_back("LANG=en_US.UTF-8");
    envStrings.emplace_back("PATH=" + std::string(BIN_PATH) + ":" + std::string(BIN_PATH) + "/applets:/system/bin:/system/xbin:/sbin");
    envStrings.emplace_back("LD_LIBRARY_PATH=" + std::string(LIB_PATH));
    if (access(preloadPath.c_str(), R_OK) == 0) {
        envStrings.emplace_back("LD_PRELOAD=" + preloadPath);
    }
    envStrings.emplace_back("ANDROID_DATA=/data");
    envStrings.emplace_back("ANDROID_ROOT=/system");
    envStrings.emplace_back("TERMUX_VERSION=0.119.0");
    envStrings.emplace_back("SHELL=" + std::string(userShell));

    std::string execPath;
    std::vector<std::string> argStrings;

    if (asRoot) {
        execPath = resolveExecutable(suBins, 7);
        std::string rootCmd =
            "export PATH=\"" + std::string(BIN_PATH) + ":" + std::string(BIN_PATH) + "/applets:/sbin:/system/bin:/system/xbin:$PATH\"; "
            "export LD_LIBRARY_PATH=\"" + std::string(LIB_PATH) + "\"; "
            "export PREFIX=\"" + std::string(PREFIX_PATH) + "\"; "
            "export HOME=\"" + std::string(HOME_PATH) + "\"; "
            "export TMPDIR=\"" + std::string(TMP_PATH) + "\"; "
            "export TERM=xterm-256color; export COLORTERM=truecolor; ";
        if (access(preloadPath.c_str(), R_OK) == 0) {
            rootCmd += "export LD_PRELOAD=\"" + preloadPath + "\"; ";
        }
        rootCmd += "cd \"" + std::string(HOME_PATH) + "\" 2>/dev/null || true; ";
        rootCmd += "exec \"" + std::string(userShell) + "\" -l";

        argStrings.push_back(execPath);
        argStrings.push_back("-c");
        argStrings.push_back(rootCmd);
        engine->mWindowTitle = "root #";
    } else if (hasLogin) {
        execPath = loginBin;
        argStrings.push_back("-login");
        engine->mWindowTitle = "bash";
    } else {
        execPath = userShell;
        argStrings.push_back("-sh");
        engine->mWindowTitle = "sh";
    }

    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) return false;

    if (grantpt(ptm) != 0 || unlockpt(ptm) != 0) {
        close(ptm);
        return false;
    }

    char devname[64];
    if (ptsname_r(ptm, devname, sizeof(devname)) != 0) {
        close(ptm);
        return false;
    }

    struct termios tios{};
    tcgetattr(ptm, &tios);
    tios.c_iflag |= ICRNL | IXON | IXANY | IMAXBEL | IUTF8;
    tios.c_oflag |= OPOST | ONLCR;
    tios.c_cflag |= CS8 | CREAD | HUPCL;
    tios.c_lflag |= ISIG | ICANON | ECHO | ECHOE | ECHOK | ECHOCTL | ECHOKE | IEXTEN;
    tios.c_cc[VMIN] = 1;
    tios.c_cc[VTIME] = 0;
    cfsetispeed(&tios, B38400);
    cfsetospeed(&tios, B38400);
    tcsetattr(ptm, TCSANOW, &tios);

    struct winsize sz{};
    sz.ws_row = static_cast<unsigned short>(rows);
    sz.ws_col = static_cast<unsigned short>(cols);
    sz.ws_xpixel = static_cast<unsigned short>(cols * std::max(4, cellW));
    sz.ws_ypixel = static_cast<unsigned short>(rows * std::max(8, cellH));
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) {
        close(ptm);
        return false;
    }

    if (pid == 0) {
        sigset_t mask;
        sigfillset(&mask);
        sigprocmask(SIG_UNBLOCK, &mask, nullptr);

        close(ptm);
        setsid();

        int pts = open(devname, O_RDWR);
        if (pts < 0) _exit(127);

        ioctl(pts, TIOCSCTTY, 0);
        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        DIR* dir = opendir("/proc/self/fd");
        if (dir) {
            int dfd = dirfd(dir);
            struct dirent* de;
            while ((de = readdir(dir)) != nullptr) {
                int fd = atoi(de->d_name);
                if (fd > 2 && fd != dfd) close(fd);
            }
            closedir(dir);
        }

        clearenv();
        for (const auto& s : envStrings) {
            putenv(strdup(s.c_str()));
        }

        if (chdir(HOME_PATH) != 0) {
            (void)chdir("/");
        }

        std::vector<char*> argv;
        for (const auto& a : argStrings) argv.push_back(strdup(a.c_str()));
        argv.push_back(nullptr);

        execve(execPath.c_str(), argv.data(), environ);
        _exit(127);
    }

    engine->mPtyFd = ptm;
    engine->mShellPid = static_cast<int>(pid);
    engine->mRunning.store(true, std::memory_order_release);

    if (pthread_create(&engine->mReaderThread, nullptr, &NeoTerminalEngine::readerThreadEntry, engine) == 0) {
        engine->mThreadStarted = true;
    }
    return true;
}

} // namespace

extern "C" {

JNIEXPORT jobject JNICALL
Java_com_termuxx_app_NativeBridge_getBootstrapBuffer(JNIEnv* env, jclass /*clazz*/) {
    if (blob_size <= 0) return nullptr;
    return env->NewDirectByteBuffer(const_cast<jbyte*>(blob), static_cast<jlong>(blob_size));
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_patchBytesInPlace(JNIEnv* env, jclass /*clazz*/, jbyteArray buf, jint len) {
    if (!buf || len < static_cast<jint>(TOKEN_16_LEN)) return;
    auto* raw = static_cast<uint8_t*>(env->GetPrimitiveArrayCritical(buf, nullptr));
    if (raw) {
        patchBufferTokensInPlace(raw, static_cast<size_t>(len));
        env->ReleasePrimitiveArrayCritical(buf, raw, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_finalizeBootstrapNative(JNIEnv* env, jclass /*clazz*/, jstring nativeLibDir) {
    mkdir(APP_ROOT_PATH, 0700);
    mkdir(FILES_REAL_PATH, 0700);
    mkdir(FILES_LINK_PATH, 0700);
    mkdir(HOME_PATH, 0700);
    mkdir(TMP_PATH, 0700);
    mkdir(CACHE_PATH, 0700);

    // Bridge /data/data/com.termuxx/files/{usr,home} <-> /data/data/com.termuxx/file/{usr,home}
    unlink("/data/data/com.termuxx/files/usr");
    symlink(PREFIX_PATH, "/data/data/com.termuxx/files/usr");
    unlink("/data/data/com.termuxx/files/home");
    symlink(HOME_PATH, "/data/data/com.termuxx/files/home");

    if (nativeLibDir != nullptr) {
        const char* libUtf = env->GetStringUTFChars(nativeLibDir, nullptr);
        if (libUtf) {
            std::string srcSo = std::string(libUtf) + "/libtermuxx-redirect.so";
            std::string dstSo = std::string(LIB_PATH) + "/libtermuxx-redirect.so";
            copyFileBinary(srcSo.c_str(), dstSo.c_str(), 0755);
            env->ReleaseStringUTFChars(nativeLibDir, libUtf);
        }
    }

    // Write minimalist AMOLED ~/.bashrc if not present
    std::string bashrcPath = std::string(HOME_PATH) + "/.bashrc";
    if (access(bashrcPath.c_str(), F_OK) != 0) {
        const char* rcText =
            "export TERM=xterm-256color\n"
            "export COLORTERM=truecolor\n"
            "export PREFIX=/data/data/com.termuxx/file/usr\n"
            "export HOME=/data/data/com.termuxx/file/home\n"
            "export TMPDIR=/data/data/com.termuxx/file/usr/tmp\n"
            "export PATH=\"$PREFIX/bin:$PREFIX/bin/applets:/system/bin:/system/xbin\"\n"
            "export LD_LIBRARY_PATH=\"$PREFIX/lib\"\n"
            "if [ -f \"$PREFIX/lib/libtermuxx-redirect.so\" ]; then\n"
            "  export LD_PRELOAD=\"$PREFIX/lib/libtermuxx-redirect.so\"\n"
            "fi\n"
            "alias ll='ls -lah --color=auto'\n"
            "alias update='pkg update && pkg upgrade -y'\n"
            "PS1='\\[\\e[1;37m\\]\\w\\[\\e[0m\\] \\[\\e[1;90m\\]›\\[\\e[0m\\] '\n";
        int fd = open(bashrcPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0600);
        if (fd >= 0) {
            writeAllFd(fd, rcText, strlen(rcText));
            close(fd);
        }
    }
}

JNIEXPORT jlong JNICALL
Java_com_termuxx_app_NativeBridge_createEngine(
    JNIEnv* /*env*/, jclass /*clazz*/, jint rows, jint cols, jint scrollbackRows) {
    auto* engine = new NeoTerminalEngine(rows, cols, scrollbackRows);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_NativeBridge_startSession(
    JNIEnv* env,
    jclass /*clazz*/,
    jlong handle,
    jboolean asRoot,
    jstring nativeLibDir,
    jint rows,
    jint cols,
    jint cellW,
    jint cellH) {
    if (!handle) return JNI_FALSE;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    const char* libUtf = nativeLibDir ? env->GetStringUTFChars(nativeLibDir, nullptr) : nullptr;
    bool ok = startSessionProcess(engine, asRoot == JNI_TRUE, libUtf, rows, cols, cellW, cellH);
    if (libUtf) env->ReleaseStringUTFChars(nativeLibDir, libUtf);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_destroyEngine(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    delete reinterpret_cast<NeoTerminalEngine*>(handle);
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_resizeEngine(
    JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint rows, jint cols, jint cellW, jint cellH) {
    if (!handle) return;
    reinterpret_cast<NeoTerminalEngine*>(handle)->resize(rows, cols, cellW, cellH);
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_resetEngine(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (!handle) return;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    engine->lock();
    engine->reset();
    engine->unlock();
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_NativeBridge_isRunning(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (!handle) return JNI_FALSE;
    return reinterpret_cast<NeoTerminalEngine*>(handle)->mRunning.load(std::memory_order_acquire) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_NativeBridge_consumeFrameDirty(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (!handle) return JNI_FALSE;
    return reinterpret_cast<NeoTerminalEngine*>(handle)->mFrameDirty.exchange(false, std::memory_order_acq_rel)
        ? JNI_TRUE
        : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_sendText(
    JNIEnv* env, jclass /*clazz*/, jlong handle, jstring text, jboolean isPaste) {
    if (!handle || !text) return;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    const char* utf = env->GetStringUTFChars(text, nullptr);
    if (!utf) return;

    std::string payload(utf);
    env->ReleaseStringUTFChars(text, utf);

    if (isPaste) {
        // Normalize newlines for terminal paste
        std::string sanitized;
        sanitized.reserve(payload.size());
        for (size_t i = 0; i < payload.size(); ++i) {
            if (payload[i] == '\r' && i + 1 < payload.size() && payload[i + 1] == '\n') {
                sanitized.push_back('\r');
                i++;
            } else if (payload[i] == '\n') {
                sanitized.push_back('\r');
            } else {
                sanitized.push_back(payload[i]);
            }
        }
        bool bracketed = (engine->mModes & MODE_BRACKETED_PASTE) != 0;
        if (bracketed) {
            std::string wrapped = "\x1b[200~" + sanitized + "\x1b[201~";
            engine->writeToPty(wrapped.data(), wrapped.size());
        } else {
            engine->writeToPty(sanitized.data(), sanitized.size());
        }
    } else {
        engine->writeToPty(payload.data(), payload.size());
    }
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_sendCodepoint(
    JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint cp, jboolean ctrlDown, jboolean altDown) {
    if (!handle) return;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);

    int codepoint = cp;
    if (ctrlDown) {
        if (codepoint >= 'a' && codepoint <= 'z') codepoint = codepoint - 'a' + 1;
        else if (codepoint >= 'A' && codepoint <= 'Z') codepoint = codepoint - 'A' + 1;
        else if (codepoint == ' ' || codepoint == '2' || codepoint == '@') codepoint = 0;
        else if (codepoint == '[' || codepoint == '3') codepoint = 27;
        else if (codepoint == '\\' || codepoint == '4') codepoint = 28;
        else if (codepoint == ']' || codepoint == '5') codepoint = 29;
        else if (codepoint == '^' || codepoint == '6') codepoint = 30;
        else if (codepoint == '_' || codepoint == '/' || codepoint == '7') codepoint = 31;
        else if (codepoint == '8' || codepoint == '?') codepoint = 127;
    }

    std::string out;
    if (altDown) out.push_back('\x1b');
    appendUtf8(out, static_cast<uint32_t>(codepoint));
    engine->writeToPty(out.data(), out.size());
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_NativeBridge_sendKeyCode(
    JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint keyCode, jint metaState) {
    if (!handle) return JNI_FALSE;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);

    bool appCursor = (engine->mModes & MODE_APP_CURSOR_KEYS) != 0;
    bool shift = (metaState & AMETA_SHIFT_ON) != 0;
    bool alt   = (metaState & AMETA_ALT_ON) != 0;
    bool ctrl  = (metaState & AMETA_CTRL_ON) != 0;
    int mod    = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);

    char buf[32];
    const char* seq = nullptr;

    switch (keyCode) {
        case AKEYCODE_ENTER:
        case AKEYCODE_NUMPAD_ENTER:
            seq = alt ? "\x1b\r" : "\r";
            break;
        case AKEYCODE_DEL:
            seq = alt ? "\x1b\x7f" : (ctrl ? "\x08" : "\x7f");
            break;
        case AKEYCODE_FORWARD_DEL:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[3;%d~", mod); seq = buf; }
            else seq = "\x1b[3~";
            break;
        case AKEYCODE_INSERT:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[2;%d~", mod); seq = buf; }
            else seq = "\x1b[2~";
            break;
        case AKEYCODE_TAB:
            seq = shift ? "\x1b[Z" : "\t";
            break;
        case AKEYCODE_ESCAPE:
            seq = "\x1b";
            break;
        case AKEYCODE_DPAD_UP:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[1;%dA", mod); seq = buf; }
            else seq = appCursor ? "\x1bOA" : "\x1b[A";
            break;
        case AKEYCODE_DPAD_DOWN:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[1;%dB", mod); seq = buf; }
            else seq = appCursor ? "\x1bOB" : "\x1b[B";
            break;
        case AKEYCODE_DPAD_RIGHT:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[1;%dC", mod); seq = buf; }
            else seq = appCursor ? "\x1bOC" : "\x1b[C";
            break;
        case AKEYCODE_DPAD_LEFT:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[1;%dD", mod); seq = buf; }
            else seq = appCursor ? "\x1bOD" : "\x1b[D";
            break;
        case AKEYCODE_MOVE_HOME:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[1;%dH", mod); seq = buf; }
            else seq = appCursor ? "\x1bOH" : "\x1b[H";
            break;
        case AKEYCODE_MOVE_END:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[1;%dF", mod); seq = buf; }
            else seq = appCursor ? "\x1bOF" : "\x1b[F";
            break;
        case AKEYCODE_PAGE_UP:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[5;%d~", mod); seq = buf; }
            else seq = "\x1b[5~";
            break;
        case AKEYCODE_PAGE_DOWN:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[6;%d~", mod); seq = buf; }
            else seq = "\x1b[6~";
            break;
        case AKEYCODE_F1:  seq = "\x1bOP"; break;
        case AKEYCODE_F2:  seq = "\x1bOQ"; break;
        case AKEYCODE_F3:  seq = "\x1bOR"; break;
        case AKEYCODE_F4:  seq = "\x1bOS"; break;
        case AKEYCODE_F5:  seq = "\x1b[15~"; break;
        case AKEYCODE_F6:  seq = "\x1b[17~"; break;
        case AKEYCODE_F7:  seq = "\x1b[18~"; break;
        case AKEYCODE_F8:  seq = "\x1b[19~"; break;
        case AKEYCODE_F9:  seq = "\x1b[20~"; break;
        case AKEYCODE_F10: seq = "\x1b[21~"; break;
        case AKEYCODE_F11: seq = "\x1b[23~"; break;
        case AKEYCODE_F12: seq = "\x1b[24~"; break;
        default:
            break;
    }

    if (seq) {
        engine->writeToPty(seq, strlen(seq));
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_sendMouse(
    JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint button, jint col, jint row, jboolean pressed) {
    if (!handle) return;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    if ((engine->mModes & MODE_MOUSE_TRACKING) == 0) return;
    if ((engine->mModes & MODE_MOUSE_SGR) != 0) {
        int c = std::clamp<int>(col, 1, engine->mCols);
        int r = std::clamp<int>(row, 1, engine->mRows);
        char buf[48];
        snprintf(buf, sizeof(buf), "\x1b[<%d;%d;%d%c", button, c, r, pressed ? 'M' : 'm');
        engine->writeToPty(buf, strlen(buf));
    }
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_NativeBridge_isRowDirty(
    JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint screenRow) {
    if (!handle) return JNI_FALSE;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    auto& grid = engine->activeGrid();
    if (screenRow < 0 || screenRow >= engine->mRows) return JNI_FALSE;
    return grid[static_cast<size_t>(screenRow)].dirty ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_NativeBridge_clearDirtyFlags(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (!handle) return;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    engine->lock();
    for (auto& r : engine->activeGrid()) r.dirty = false;
    engine->unlock();
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_NativeBridge_getActiveScrollback(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (!handle) return 0;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);
    return (engine->mModes & MODE_ALT_SCREEN) ? 0 : engine->mScrollbackCount;
}

/**
 * Packs a single row's render data in C++23:
 * - outBgRuns: triples [startCol, endCol, bgArgb] for non-black background spans
 * - outGlyphs: quads   [codepoint, col, fgArgb, flags] for visible non-space characters
 * Returns: (bgRunCount << 16) | glyphCount
 */
JNIEXPORT jint JNICALL
Java_com_termuxx_app_NativeBridge_packRowRender(
    JNIEnv* env,
    jclass /*clazz*/,
    jlong handle,
    jint scrollOffset,
    jint screenRow,
    jintArray outBgRuns,
    jintArray outGlyphs) {

    if (!handle) return 0;
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);

    engine->lock();
    const Row& row = engine->getVisualRow(scrollOffset, screenRow);
    int cols = std::min<int>(engine->mCols, static_cast<int>(row.cells.size()));

    auto* bgPtr = static_cast<jint*>(env->GetPrimitiveArrayCritical(outBgRuns, nullptr));
    auto* glPtr = static_cast<jint*>(env->GetPrimitiveArrayCritical(outGlyphs, nullptr));

    int bgCount = 0;
    int glCount = 0;

    if (bgPtr && glPtr) {
        int runStart = 0;
        while (runStart < cols) {
            const Cell& c0 = row.cells[static_cast<size_t>(runStart)];
            uint32_t bg = resolveBgColor(c0.fg, c0.bg, c0.flags);
            int runEnd = runStart + 1;
            while (runEnd < cols) {
                const Cell& c1 = row.cells[static_cast<size_t>(runEnd)];
                if (resolveBgColor(c1.fg, c1.bg, c1.flags) != bg) break;
                runEnd++;
            }
            if (bg != DEFAULT_BG) {
                int base = bgCount * 3;
                bgPtr[base]     = runStart;
                bgPtr[base + 1] = runEnd;
                bgPtr[base + 2] = static_cast<jint>(bg);
                bgCount++;
            }
            runStart = runEnd;
        }

        for (int c = 0; c < cols; ++c) {
            const Cell& cell = row.cells[static_cast<size_t>(c)];
            if (cell.codepoint <= ' ' || (cell.flags & (FLAG_INVISIBLE | FLAG_WIDE_TRAIL)) != 0) {
                continue;
            }
            uint32_t fg = resolveFgColor(cell.fg, cell.bg, cell.flags);
            int base = glCount * 4;
            glPtr[base]     = static_cast<jint>(cell.codepoint);
            glPtr[base + 1] = c;
            glPtr[base + 2] = static_cast<jint>(fg);
            glPtr[base + 3] = static_cast<jint>(cell.flags);
            glCount++;
        }
    }

    if (glPtr) env->ReleasePrimitiveArrayCritical(outGlyphs, glPtr, 0);
    if (bgPtr) env->ReleasePrimitiveArrayCritical(outBgRuns, bgPtr, 0);
    engine->unlock();

    return (bgCount << 16) | (glCount & 0xFFFF);
}

JNIEXPORT jstring JNICALL
Java_com_termuxx_app_NativeBridge_getTranscriptText(
    JNIEnv* env, jclass /*clazz*/, jlong handle, jint maxScrollbackLines) {
    if (!handle) return env->NewStringUTF("");
    auto* engine = reinterpret_cast<NeoTerminalEngine*>(handle);

    std::string out;
    out.reserve(16384);

    engine->lock();
    int sbCount = (engine->mModes & MODE_ALT_SCREEN) ? 0 : std::min<int>(engine->mScrollbackCount, maxScrollbackLines);
    for (int i = -sbCount; i < engine->mRows; ++i) {
        const Row& row = engine->getVisualRow(0, i);
        int lastNonSpace = -1;
        for (int c = static_cast<int>(row.cells.size()) - 1; c >= 0; --c) {
            if (row.cells[static_cast<size_t>(c)].codepoint != ' ' &&
                !(row.cells[static_cast<size_t>(c)].flags & FLAG_WIDE_TRAIL)) {
                lastNonSpace = c;
                break;
            }
        }
        for (int c = 0; c <= lastNonSpace; ++c) {
            const Cell& cell = row.cells[static_cast<size_t>(c)];
            if (cell.flags & FLAG_WIDE_TRAIL) continue;
            appendUtf8(out, cell.codepoint);
        }
        if (!row.wrapped && i < engine->mRows - 1) {
            out.push_back('\n');
        }
    }
    engine->unlock();

    return env->NewStringUTF(out.c_str());
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_NativeBridge_getCursorRow(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return handle ? reinterpret_cast<NeoTerminalEngine*>(handle)->mCursorRow : 0;
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_NativeBridge_getCursorCol(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return handle ? reinterpret_cast<NeoTerminalEngine*>(handle)->mCursorCol : 0;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_NativeBridge_isCursorVisible(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return (handle && reinterpret_cast<NeoTerminalEngine*>(handle)->mCursorVisible) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_NativeBridge_getModesMask(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return handle ? reinterpret_cast<NeoTerminalEngine*>(handle)->mModes : 0;
}

} // extern "C"
