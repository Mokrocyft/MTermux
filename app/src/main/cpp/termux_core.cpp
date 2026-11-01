// Termux Native Core

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <jni.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <math.h>
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
#include <zlib.h>

#include <algorithm>
#include <atomic>
#include <cmath>
#include <memory>
#include <string>
#include <utility>
#include <vector>

#if defined(__aarch64__)
#include <arm_neon.h>
#endif

extern "C" {
extern const jbyte blob[];
extern const int32_t blob_size;
}

namespace {

constexpr const char APP_ROOT_PATH[]   = "/data/data/com.termuxx";
constexpr const char FILES_REAL_PATH[] = "/data/data/com.termuxx/file";
constexpr const char FILES_LINK_PATH[] = "/data/data/com.termuxx/files";
constexpr const char PREFIX_PATH[]     = "/data/data/com.termuxx/file/usr";
constexpr const char STAGING_PATH[]    = "/data/data/com.termuxx/file/usr-staging";
constexpr const char BIN_PATH[]        = "/data/data/com.termuxx/file/usr/bin";
constexpr const char LIB_PATH[]        = "/data/data/com.termuxx/file/usr/lib";
constexpr const char TMP_PATH[]        = "/data/data/com.termuxx/file/usr/tmp";
constexpr const char HOME_PATH[]       = "/data/data/com.termuxx/file/home";
constexpr const char APT_ARCHIVES_PARTIAL[] = "/data/data/com.termuxx/cach/apt/archives/partial";
constexpr const char SECOND_STAGE_MARKER[]  = "/data/data/com.termuxx/file/usr/etc/termux/.termuxx_stage2_done";

constexpr const char OLD_FILES_16[] = "com.termux/files";
constexpr const char NEW_FILES_16[] = "com.termuxx/file";
constexpr const char OLD_CACHE_16[] = "com.termux/cache";
constexpr const char NEW_CACHE_16[] = "com.termuxx/cach";
constexpr size_t TOKEN_16_LEN = 16;

// Cell style bit flags
constexpr uint16_t FLAG_BOLD             = 1u << 0;
constexpr uint16_t FLAG_DIM              = 1u << 1;
constexpr uint16_t FLAG_ITALIC           = 1u << 2;
constexpr uint16_t FLAG_UNDERLINE        = 1u << 3;
constexpr uint16_t FLAG_BLINK            = 1u << 4;
constexpr uint16_t FLAG_INVERSE          = 1u << 5;
constexpr uint16_t FLAG_INVISIBLE        = 1u << 6;
constexpr uint16_t FLAG_STRIKETHROUGH    = 1u << 7;
constexpr uint16_t FLAG_WIDE_LEAD        = 1u << 8;
constexpr uint16_t FLAG_WIDE_TRAIL       = 1u << 9;
constexpr uint16_t FLAG_UNDERCURL        = 1u << 10;
constexpr uint16_t FLAG_DOUBLE_UNDERLINE = 1u << 11;

// Mode mask bits
constexpr int MODE_APP_CURSOR_KEYS = 1 << 0;
constexpr int MODE_BRACKETED_PASTE = 1 << 1;
constexpr int MODE_ALT_SCREEN      = 1 << 2;
constexpr int MODE_MOUSE_TRACKING  = 1 << 3;
constexpr int MODE_MOUSE_SGR       = 1 << 4;
constexpr int MODE_BELL_TRIGGERED  = 1 << 5;
constexpr int MODE_AUTOWRAP        = 1 << 6;
constexpr int MODE_SYNC_OUTPUT     = 1 << 7;
constexpr int MODE_FOCUS_EVENTS    = 1 << 8;

// Pure Dark AMOLED Palette + ColorOS 17 Neutral Gray Selection Highlight (#52525A)
constexpr uint32_t DEFAULT_FG   = 0xFFD0D6E0u;
constexpr uint32_t DEFAULT_BG   = 0xFF000000u;
constexpr uint32_t SELECTION_BG = 0xFF52525Au;
constexpr uint32_t SELECTION_FG = 0xFFFFFFFFu;

// Android KeyEvent constants
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

// Event-driven JNI UI wakeup state
static JavaVM* gJvm = nullptr;
static int gDefaultCursorShape = 0;
static pthread_mutex_t gViewMutex = PTHREAD_MUTEX_INITIALIZER;
static jobject gSurfaceView = nullptr;
static jmethodID gPostInvalidateId = nullptr;

static void notifySurfaceFromNative() noexcept {
    if (!gJvm) return;
    pthread_mutex_lock(&gViewMutex);
    jobject view = gSurfaceView;
    jmethodID mid = gPostInvalidateId;
    if (view && mid) {
        JNIEnv* env = nullptr;
        int status = gJvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
        bool attachedHere = false;
        if (status != JNI_OK || !env) {
            if (gJvm->AttachCurrentThread(&env, nullptr) == JNI_OK && env) {
                attachedHere = true;
            }
        }
        if (env) {
            env->CallVoidMethod(view, mid);
        }
        if (attachedHere) {
            gJvm->DetachCurrentThread();
        }
    }
    pthread_mutex_unlock(&gViewMutex);
}

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

    bool isDefaultBlank() const noexcept {
        return codepoint == ' ' && bg == DEFAULT_BG &&
               (flags & (FLAG_INVERSE | FLAG_UNDERLINE | FLAG_UNDERCURL | FLAG_DOUBLE_UNDERLINE |
                         FLAG_STRIKETHROUGH | FLAG_WIDE_LEAD | FLAG_WIDE_TRAIL)) == 0;
    }
};

struct Row {
    std::vector<Cell> cells;
    bool dirty = true;
    bool wrapped = false;

    explicit Row(int cols = 80) : cells(static_cast<size_t>(std::max(1, cols))) {
        clear(DEFAULT_FG, DEFAULT_BG, 0);
    }

    void resize(int newCols) {
        newCols = std::max(1, newCols);
        size_t oldSize = cells.size();
        cells.resize(static_cast<size_t>(newCols));
        for (size_t i = oldSize; i < cells.size(); ++i) {
            cells[i].reset();
        }
        dirty = true;
    }

    void clear(uint32_t fg = DEFAULT_FG, uint32_t bg = DEFAULT_BG, uint16_t flags = 0) noexcept {
#if defined(__aarch64__)
        Cell proto{' ', fg, bg, flags, 0};
        uint32x4_t vec = vld1q_u32(reinterpret_cast<const uint32_t*>(&proto));
        for (auto& c : cells) {
            vst1q_u32(reinterpret_cast<uint32_t*>(&c), vec);
        }
#else
        for (auto& c : cells) {
            c.reset(fg, bg, flags);
        }
#endif
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

static uint32_t decodeFirstUtf8Codepoint(const std::string& s, size_t& bytesConsumed) noexcept {
    if (s.empty()) {
        bytesConsumed = 0;
        return 0;
    }
    const auto b0 = static_cast<uint8_t>(s[0]);
    if ((b0 & 0x80) == 0) {
        bytesConsumed = 1;
        return b0;
    }
    if ((b0 & 0xE0) == 0xC0 && s.size() >= 2) {
        bytesConsumed = 2;
        if (b0 == 0xC0 && static_cast<uint8_t>(s[1]) == 0x80) return 0;
        return ((b0 & 0x1Fu) << 6) | (static_cast<uint8_t>(s[1]) & 0x3Fu);
    }
    if ((b0 & 0xF0) == 0xE0 && s.size() >= 3) {
        bytesConsumed = 3;
        return ((b0 & 0x0Fu) << 12) |
               ((static_cast<uint8_t>(s[1]) & 0x3Fu) << 6) |
               (static_cast<uint8_t>(s[2]) & 0x3Fu);
    }
    if ((b0 & 0xF8) == 0xF0 && s.size() >= 4) {
        bytesConsumed = 4;
        return ((b0 & 0x07u) << 18) |
               ((static_cast<uint8_t>(s[1]) & 0x3Fu) << 12) |
               ((static_cast<uint8_t>(s[2]) & 0x3Fu) << 6) |
               (static_cast<uint8_t>(s[3]) & 0x3Fu);
    }
    bytesConsumed = 1;
    return b0;
}

static std::string decodeBase64(const std::string& in) {
    static constexpr int8_t T[256] = {
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,62,-1,-1,-1,63,
        52,53,54,55,56,57,58,59,60,61,-1,-1,-1,-1,-1,-1,
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,
        15,16,17,18,19,20,21,22,23,24,25,-1,-1,-1,-1,-1,
        -1,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,
        41,42,43,44,45,46,47,48,49,50,51,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
    std::string out;
    out.reserve(in.size() * 3 / 4);
    int val = 0, valb = -8;
    for (unsigned char c : in) {
        if (T[c] == -1) {
            if (c == '=') break;
            continue;
        }
        val = (val << 6) + T[c];
        valb += 6;
        if (valb >= 0) {
            out.push_back(static_cast<char>((val >> valb) & 0xFF));
            valb -= 8;
        }
    }
    return out;
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

// ============================================================================
// C++23 Procedural Box-Drawing (U+2500..U+257F) & Block Elements (U+2580..U+259F)
// Encodes fractional 1/16th cell rectangles directly into outGlyphs (cp = -1)
// ============================================================================
static inline int packRect16(int x0, int y0, int x1, int y1) noexcept {
    return ((x0 & 0xFF) << 24) | ((y0 & 0xFF) << 16) | ((x1 & 0xFF) << 8) | (y1 & 0xFF);
}

static bool emitProceduralBoxOrBlockInCpp23(
    uint32_t cp,
    int col,
    uint32_t fg,
    jint* glPtr,
    int& glCount,
    int maxGlyphs) noexcept {

    if (cp < 0x2500 || cp > 0x259F) return false;

    auto pushRect = [&](int x0, int y0, int x1, int y1) {
        if (glCount >= maxGlyphs) return;
        int base = glCount * 4;
        glPtr[base]     = -1; // Procedural rect marker
        glPtr[base + 1] = col;
        glPtr[base + 2] = static_cast<jint>(fg);
        glPtr[base + 3] = packRect16(x0, y0, x1, y1);
        glCount++;
    };

    if (cp >= 0x2580 && cp <= 0x259F) {
        switch (cp) {
            case 0x2580: pushRect(0, 0, 16, 8); return true;
            case 0x2581: pushRect(0, 14, 16, 16); return true;
            case 0x2582: pushRect(0, 12, 16, 16); return true;
            case 0x2583: pushRect(0, 10, 16, 16); return true;
            case 0x2584: pushRect(0, 8, 16, 16); return true;
            case 0x2585: pushRect(0, 6, 16, 16); return true;
            case 0x2586: pushRect(0, 4, 16, 16); return true;
            case 0x2587: pushRect(0, 2, 16, 16); return true;
            case 0x2588: pushRect(0, 0, 16, 16); return true;
            case 0x2589: pushRect(0, 0, 14, 16); return true;
            case 0x258A: pushRect(0, 0, 12, 16); return true;
            case 0x258B: pushRect(0, 0, 10, 16); return true;
            case 0x258C: pushRect(0, 0, 8, 16); return true;
            case 0x258D: pushRect(0, 0, 6, 16); return true;
            case 0x258E: pushRect(0, 0, 4, 16); return true;
            case 0x258F: pushRect(0, 0, 2, 16); return true;
            case 0x2590: pushRect(8, 0, 16, 16); return true;
            case 0x2594: pushRect(0, 0, 16, 2); return true;
            case 0x2595: pushRect(14, 0, 16, 16); return true;
            default: return false;
        }
    }

    switch (cp) {
        case 0x2500: case 0x2501: pushRect(0, 7, 16, 9); return true;
        case 0x2502: case 0x2503: pushRect(7, 0, 9, 16); return true;
        case 0x250C: case 0x250F: case 0x256D:
            pushRect(7, 7, 16, 9); pushRect(7, 7, 9, 16); return true;
        case 0x2510: case 0x2513: case 0x256E:
            pushRect(0, 7, 9, 9); pushRect(7, 7, 9, 16); return true;
        case 0x2514: case 0x2517: case 0x2570:
            pushRect(7, 7, 16, 9); pushRect(7, 0, 9, 9); return true;
        case 0x2518: case 0x251B: case 0x256F:
            pushRect(0, 7, 9, 9); pushRect(7, 0, 9, 9); return true;
        case 0x251C: case 0x2523:
            pushRect(7, 0, 9, 16); pushRect(7, 7, 16, 9); return true;
        case 0x2524: case 0x252B:
            pushRect(7, 0, 9, 16); pushRect(0, 7, 9, 9); return true;
        case 0x252C: case 0x2533:
            pushRect(0, 7, 16, 9); pushRect(7, 7, 9, 16); return true;
        case 0x2534: case 0x253B:
            pushRect(0, 7, 16, 9); pushRect(7, 0, 9, 9); return true;
        case 0x253C: case 0x254B:
            pushRect(0, 7, 16, 9); pushRect(7, 0, 9, 16); return true;
        default: return false;
    }
}

static void replaceAllTokens16(uint8_t* data, size_t len, const char* oldTok, const char* newTok) noexcept {
    if (!data || len < TOKEN_16_LEN) return;
    uint8_t* cur = data;
    size_t rem = len;
    while (rem >= TOKEN_16_LEN) {
        auto* hit = static_cast<uint8_t*>(memmem(cur, rem, oldTok, TOKEN_16_LEN));
        if (!hit) break;
        memcpy(hit, newTok, TOKEN_16_LEN);
        size_t adv = static_cast<size_t>(hit - cur) + TOKEN_16_LEN;
        cur += adv;
        rem -= adv;
    }
}

static void patchBufferTokensInPlace(uint8_t* data, size_t len) noexcept {
    replaceAllTokens16(data, len, OLD_FILES_16, NEW_FILES_16);
    replaceAllTokens16(data, len, OLD_CACHE_16, NEW_CACHE_16);
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

static void copyFileBinary(const char* srcPath, const char* dstPath, mode_t mode) noexcept {
    int inFd = open(srcPath, O_RDONLY | O_CLOEXEC);
    if (inFd < 0) return;
    chmod(dstPath, 0700);
    int outFd = open(dstPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0700);
    if (outFd < 0) {
        close(inFd);
        return;
    }
    uint8_t buf[65536];
    ssize_t n;
    while ((n = read(inFd, buf, sizeof(buf))) > 0) {
        writeAllFd(outFd, buf, static_cast<size_t>(n));
    }
    close(outFd);
    close(inFd);
    chmod(dstPath, mode);
}

static void mkdirsRecursive(const char* dirPath, mode_t mode = 0700) noexcept {
    char tmp[1024];
    snprintf(tmp, sizeof(tmp), "%s", dirPath);
    size_t len = strlen(tmp);
    if (len == 0) return;
    if (tmp[len - 1] == '/') tmp[len - 1] = '\0';
    for (char* p = tmp + 1; *p; ++p) {
        if (*p == '/') {
            *p = '\0';
            mkdir(tmp, mode);
            *p = '/';
        }
    }
    mkdir(tmp, mode);
}

static void mkdirsForFile(const char* filePath, mode_t mode = 0700) noexcept {
    const char* lastSlash = strrchr(filePath, '/');
    if (!lastSlash || lastSlash == filePath) return;
    std::string parent(filePath, static_cast<size_t>(lastSlash - filePath));
    mkdirsRecursive(parent.c_str(), mode);
}

static void removeRecursiveNative(const char* path) noexcept {
    struct stat st{};
    if (lstat(path, &st) != 0) return;
    if (S_ISDIR(st.st_mode) && !S_ISLNK(st.st_mode)) {
        DIR* d = opendir(path);
        if (d) {
            struct dirent* de;
            while ((de = readdir(d)) != nullptr) {
                if (strcmp(de->d_name, ".") == 0 || strcmp(de->d_name, "..") == 0) continue;
                std::string child = std::string(path) + "/" + de->d_name;
                removeRecursiveNative(child.c_str());
            }
            closedir(d);
        }
        rmdir(path);
    } else {
        chmod(path, 0700);
        unlink(path);
    }
}

static inline uint16_t readLe16(const uint8_t* p) noexcept {
    return static_cast<uint16_t>(p[0]) | (static_cast<uint16_t>(p[1]) << 8);
}

static inline uint32_t readLe32(const uint8_t* p) noexcept {
    return static_cast<uint32_t>(p[0]) |
           (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) |
           (static_cast<uint32_t>(p[3]) << 24);
}

static bool extractBootstrapInCpp23(const uint8_t* zipData, size_t zipSize) noexcept {
    if (!zipData || zipSize < 22) return false;

    size_t minOff = (zipSize > 65557) ? (zipSize - 65557) : 0;
    size_t eocdOff = zipSize;
    for (size_t pos = zipSize - 22; pos >= minOff; --pos) {
        if (readLe32(zipData + pos) == 0x06054b50u) {
            eocdOff = pos;
            break;
        }
        if (pos == 0) break;
    }
    if (eocdOff == zipSize) return false;

    uint16_t totalEntries = readLe16(zipData + eocdOff + 10);
    uint32_t cdSize       = readLe32(zipData + eocdOff + 12);
    uint32_t cdOffset     = readLe32(zipData + eocdOff + 16);
    if (static_cast<size_t>(cdOffset) + cdSize > zipSize) return false;

    removeRecursiveNative(STAGING_PATH);
    mkdirsRecursive(STAGING_PATH, 0700);

    std::vector<uint8_t> outBuf;
    outBuf.reserve(512 * 1024);
    std::vector<std::pair<std::string, std::string>> symlinks;
    symlinks.reserve(1024);

    size_t ptr = cdOffset;
    for (uint16_t i = 0; i < totalEntries && ptr + 46 <= zipSize; ++i) {
        if (readLe32(zipData + ptr) != 0x02014b50u) break;

        uint16_t method    = readLe16(zipData + ptr + 10);
        uint32_t compSize  = readLe32(zipData + ptr + 20);
        uint32_t uncompSz  = readLe32(zipData + ptr + 24);
        uint16_t nameLen   = readLe16(zipData + ptr + 28);
        uint16_t extraLen  = readLe16(zipData + ptr + 30);
        uint16_t commLen   = readLe16(zipData + ptr + 32);
        uint32_t extAttr   = readLe32(zipData + ptr + 38);
        uint32_t lfhOffset = readLe32(zipData + ptr + 42);

        if (ptr + 46 + nameLen > zipSize) break;
        std::string entryName(reinterpret_cast<const char*>(zipData + ptr + 46), nameLen);
        ptr += 46 + nameLen + extraLen + commLen;

        if (uncompSz > 256u * 1024u * 1024u) continue;
        if (lfhOffset + 30 > zipSize || readLe32(zipData + lfhOffset) != 0x04034b50u) continue;
        uint16_t lfhNameLen  = readLe16(zipData + lfhOffset + 26);
        uint16_t lfhExtraLen = readLe16(zipData + lfhOffset + 28);
        size_t dataOffset = static_cast<size_t>(lfhOffset) + 30 + lfhNameLen + lfhExtraLen;
        if (dataOffset + compSize > zipSize) continue;

        if (!entryName.empty() && entryName.back() == '/') {
            std::string dirPath = std::string(STAGING_PATH) + "/" + entryName;
            mkdirsRecursive(dirPath.c_str(), 0700);
            continue;
        }

        outBuf.resize(uncompSz);
        if (uncompSz > 0) {
            if (method == 0 && compSize == uncompSz) {
                memcpy(outBuf.data(), zipData + dataOffset, uncompSz);
            } else if (method == Z_DEFLATED) {
                z_stream strm{};
                strm.next_in = const_cast<Bytef*>(reinterpret_cast<const Bytef*>(zipData + dataOffset));
                strm.avail_in = compSize;
                strm.next_out = reinterpret_cast<Bytef*>(outBuf.data());
                strm.avail_out = uncompSz;
                if (inflateInit2(&strm, -MAX_WBITS) != Z_OK) continue;
                int ret = inflate(&strm, Z_FINISH);
                inflateEnd(&strm);
                if (ret != Z_STREAM_END) continue;
            } else {
                continue;
            }
            patchBufferTokensInPlace(outBuf.data(), outBuf.size());
        }

        if (entryName == "SYMLINKS.txt") {
            std::string text(reinterpret_cast<const char*>(outBuf.data()), outBuf.size());
            size_t lineStart = 0;
            while (lineStart < text.size()) {
                size_t nl = text.find('\n', lineStart);
                if (nl == std::string::npos) nl = text.size();
                std::string line = text.substr(lineStart, nl - lineStart);
                if (!line.empty() && line.back() == '\r') line.pop_back();
                size_t arrow = line.find("\xE2\x86\x90");
                if (arrow != std::string::npos && arrow > 0) {
                    std::string oldTarget = line.substr(0, arrow);
                    std::string relLink   = line.substr(arrow + 3);
                    std::string fullLink  = std::string(STAGING_PATH) + "/" + relLink;
                    symlinks.emplace_back(std::move(oldTarget), std::move(fullLink));
                }
                lineStart = nl + 1;
            }
        } else {
            std::string dstFile = std::string(STAGING_PATH) + "/" + entryName;
            mkdirsForFile(dstFile.c_str(), 0700);
            uint16_t zipUnixMode = static_cast<uint16_t>((extAttr >> 16) & 0xFFFFu);
            bool isElf = (outBuf.size() >= 4 &&
                          outBuf[0] == 0x7F && outBuf[1] == 'E' && outBuf[2] == 'L' && outBuf[3] == 'F');
            bool isShebang = (outBuf.size() >= 2 && outBuf[0] == '#' && outBuf[1] == '!');
            bool isExec = isElf || isShebang ||
                          ((zipUnixMode & 0111u) != 0) ||
                          (entryName.rfind("bin/", 0) == 0) ||
                          (entryName.rfind("libexec", 0) == 0) ||
                          (entryName.rfind("lib/apt/apt-helper", 0) == 0) ||
                          (entryName.rfind("lib/apt/methods", 0) == 0) ||
                          (entryName.rfind("etc/termux/bootstrap/", 0) == 0) ||
                          (entryName.rfind("var/lib/dpkg/info/", 0) == 0);
            mode_t finalMode = isExec ? 0700 : 0600;
            int fd = open(dstFile.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0700);
            if (fd >= 0) {
                if (!outBuf.empty()) writeAllFd(fd, outBuf.data(), outBuf.size());
                close(fd);
                chmod(dstFile.c_str(), finalMode);
            }
        }
    }

    for (const auto& pair : symlinks) {
        mkdirsForFile(pair.second.c_str(), 0700);
        unlink(pair.second.c_str());
        symlink(pair.first.c_str(), pair.second.c_str());
    }

    removeRecursiveNative(PREFIX_PATH);
    if (rename(STAGING_PATH, PREFIX_PATH) != 0) {
        return false;
    }
    return true;
}

static const char* resolveExecutable(const char* const* candidates, size_t count) noexcept {
    for (size_t i = 0; i < count; ++i) {
        if (access(candidates[i], R_OK) == 0) return candidates[i];
    }
    return candidates[count - 1];
}

// ============================================================================
// Android 17 (targetSdk 37) Environment & Unified Preload Initializer
// ============================================================================
static void ensureExecutablePermissionsInDir(const char* dirPath) noexcept {
    DIR* d = opendir(dirPath);
    if (!d) return;
    struct dirent* de;
    while ((de = readdir(d)) != nullptr) {
        if (strcmp(de->d_name, ".") == 0 || strcmp(de->d_name, "..") == 0) continue;
        std::string full = std::string(dirPath) + "/" + de->d_name;
        struct stat st{};
        if (lstat(full.c_str(), &st) == 0) {
            if (S_ISDIR(st.st_mode) && !S_ISLNK(st.st_mode)) {
                chmod(full.c_str(), 0700);
                ensureExecutablePermissionsInDir(full.c_str());
            } else if (S_ISREG(st.st_mode)) {
                chmod(full.c_str(), 0700);
            }
        }
    }
    closedir(d);
}

static void installUnifiedRedirectLibrary(const char* nativeLibDir) noexcept {
    std::string dstSo = std::string(LIB_PATH) + "/libtermuxx-redirect.so";
    if (nativeLibDir && nativeLibDir[0] != '\0') {
        std::string srcSo = std::string(nativeLibDir) + "/libtermuxx-redirect.so";
        if (access(srcSo.c_str(), R_OK) == 0) {
            copyFileBinary(srcSo.c_str(), dstSo.c_str(), 0700);
        }
    }
    if (access(dstSo.c_str(), R_OK) == 0) {
        chmod(dstSo.c_str(), 0700);
        static const char* const execAliases[] = {
            "/data/data/com.termuxx/file/usr/lib/libtermux-exec.so",
            "/data/data/com.termuxx/file/usr/lib/libtermux-exec-ld-preload.so",
            "/data/data/com.termuxx/file/usr/lib/libtermux-exec-direct-ld-preload.so",
            "/data/data/com.termuxx/file/usr/lib/libtermux-exec-nosandbox-ld-preload.so"
        };
        for (const char* aliasPath : execAliases) {
            unlink(aliasPath);
            symlink("libtermuxx-redirect.so", aliasPath);
        }
    }
}

static void installBuiltInSudoWrapper() noexcept {
    const char* sudoPath = "/data/data/com.termuxx/file/usr/bin/sudo";
    if (access(sudoPath, X_OK) == 0) return;
    const char* sudoScript =
        "#!/data/data/com.termuxx/file/usr/bin/sh\n"
        "for s in /debug_ramdisk/su /sbin/su /system/bin/su /system/xbin/su /data/adb/ksu/bin/su /data/adb/ap/bin/su; do\n"
        "  if [ -x \"$s\" ]; then SU_BIN=\"$s\"; break; fi\n"
        "done\n"
        "if [ -z \"$SU_BIN\" ]; then SU_BIN=su; fi\n"
        "if [ $# -eq 0 ] || [ \"$1\" = \"-s\" ] || [ \"$1\" = \"-i\" ]; then\n"
        "  exec \"$SU_BIN\" -c \"export PATH=/data/data/com.termuxx/file/usr/bin:/data/data/com.termuxx/file/usr/bin/applets:/sbin:/system/bin:/system/xbin; export LD_LIBRARY_PATH=/data/data/com.termuxx/file/usr/lib; export PREFIX=/data/data/com.termuxx/file/usr; export HOME=/data/data/com.termuxx/file/home; export TMPDIR=/data/data/com.termuxx/file/usr/tmp; export LD_PRELOAD=/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so; exec /data/data/com.termuxx/file/usr/bin/bash -l\"\n"
        "fi\n"
        "CMD=\"\"\n"
        "for arg in \"$@\"; do\n"
        "  ESCAPED=$(printf \"%s\" \"$arg\" | sed \"s/'/'\\\\\\\\''/g\")\n"
        "  CMD=\"$CMD '$ESCAPED'\"\n"
        "done\n"
        "exec \"$SU_BIN\" -c \"export PATH=/data/data/com.termuxx/file/usr/bin:/data/data/com.termuxx/file/usr/bin/applets:/sbin:/system/bin:/system/xbin; export LD_LIBRARY_PATH=/data/data/com.termuxx/file/usr/lib; export PREFIX=/data/data/com.termuxx/file/usr; export HOME=/data/data/com.termuxx/file/home; export TMPDIR=/data/data/com.termuxx/file/usr/tmp; export LD_PRELOAD=/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so; eval $CMD\"\n";
    int fd = open(sudoPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0700);
    if (fd >= 0) {
        writeAllFd(fd, sudoScript, strlen(sudoScript));
        close(fd);
        chmod(sudoPath, 0700);
    }
}

static void runSecondStageBootstrapSilently(const char* preloadPath, bool forceRun) noexcept {
    if (!forceRun && access(SECOND_STAGE_MARKER, F_OK) == 0) return;
    const char* bashBin = "/data/data/com.termuxx/file/usr/bin/bash";
    if (access(bashBin, R_OK) != 0) return;

    pid_t pid = fork();
    if (pid == 0) {
        int devNull = open("/dev/null", O_RDWR);
        if (devNull >= 0) {
            dup2(devNull, 0);
            dup2(devNull, 1);
            dup2(devNull, 2);
            if (devNull > 2) close(devNull);
        }
        setenv("PREFIX", PREFIX_PATH, 1);
        setenv("HOME", HOME_PATH, 1);
        setenv("TMPDIR", TMP_PATH, 1);
        setenv("PATH", "/data/data/com.termuxx/file/usr/bin:/data/data/com.termuxx/file/usr/bin/applets:/system/bin", 1);
        setenv("LD_LIBRARY_PATH", LIB_PATH, 1);
        if (preloadPath && access(preloadPath, R_OK) == 0) {
            setenv("LD_PRELOAD", preloadPath, 1);
        }
        setenv("ANDROID__BUILD_VERSION_SDK", "37", 1);
        if (chdir(HOME_PATH) != 0) (void)chdir("/");
        execl(bashBin, "-bash", "-l", "-c", "true", nullptr);
        execl("/system/bin/linker64", "linker64", bashBin, "-l", "-c", "true", nullptr);
        _exit(0);
    } else if (pid > 0) {
        int status = 0;
        while (waitpid(pid, &status, 0) < 0 && errno == EINTR) {}
        mkdirsForFile(SECOND_STAGE_MARKER, 0700);
        int mfd = open(SECOND_STAGE_MARKER, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0600);
        if (mfd >= 0) close(mfd);
    }
}

static pthread_mutex_t gInitOnceMutex = PTHREAD_MUTEX_INITIALIZER;
static bool gEnvInitialized = false;

static void initializeEnvironmentAndAutoUpdater(const char* nativeLibDir, bool justExtracted) noexcept {
    pthread_mutex_lock(&gInitOnceMutex);
    if (gEnvInitialized && !justExtracted) {
        pthread_mutex_unlock(&gInitOnceMutex);
        return;
    }

    mkdirsRecursive(APP_ROOT_PATH, 0700);
    mkdirsRecursive(FILES_REAL_PATH, 0700);
    mkdirsRecursive(FILES_LINK_PATH, 0700);
    mkdirsRecursive(HOME_PATH, 0700);
    mkdirsRecursive(TMP_PATH, 0700);
    mkdirsRecursive(APT_ARCHIVES_PARTIAL, 0700);
    mkdirsRecursive("/data/data/com.termuxx/cache/apt/archives/partial", 0700);

    unlink("/data/data/com.termuxx/files/usr");
    symlink(PREFIX_PATH, "/data/data/com.termuxx/files/usr");
    unlink("/data/data/com.termuxx/files/home");
    symlink(HOME_PATH, "/data/data/com.termuxx/files/home");

    ensureExecutablePermissionsInDir(BIN_PATH);
    ensureExecutablePermissionsInDir("/data/data/com.termuxx/file/usr/libexec");
    ensureExecutablePermissionsInDir("/data/data/com.termuxx/file/usr/lib/apt");

    installUnifiedRedirectLibrary(nativeLibDir);
    installBuiltInSudoWrapper();
    std::string dstSo = std::string(LIB_PATH) + "/libtermuxx-redirect.so";

    mkdirsRecursive("/data/data/com.termuxx/file/usr/etc/apt/apt.conf.d", 0700);
    const char* aptConfPath = "/data/data/com.termuxx/file/usr/etc/apt/apt.conf.d/00termuxx";
    const char* aptConfText =
        "Dir::Cache \"/data/data/com.termuxx/cach/apt\";\n"
        "Dir::Cache::archives \"/data/data/com.termuxx/cach/apt/archives\";\n";
    int afd = open(aptConfPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (afd >= 0) {
        writeAllFd(afd, aptConfText, strlen(aptConfText));
        close(afd);
    }

    mkdirsRecursive("/data/data/com.termuxx/file/usr/etc/profile.d", 0700);
    const char* profPath = "/data/data/com.termuxx/file/usr/etc/profile.d/99-termuxx-preload.sh";
    const char* profText =
        "if [ -f \"/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so\" ]; then\n"
        "  export LD_PRELOAD=\"/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so\"\n"
        "fi\n"
        "export HISTCONTROL=ignoredups:erasedups\n"
        "export HISTSIZE=10000\n"
        "export HISTFILESIZE=20000\n";
    int pfd = open(profPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (pfd >= 0) {
        writeAllFd(pfd, profText, strlen(profText));
        close(pfd);
    }

    runSecondStageBootstrapSilently(dstSo.c_str(), justExtracted);
    installUnifiedRedirectLibrary(nativeLibDir);

    std::string bashrcPath = std::string(HOME_PATH) + "/.bashrc";
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
        "export HISTCONTROL=ignoredups:erasedups\n"
        "export HISTSIZE=10000\n"
        "export HISTFILESIZE=20000\n"
        "shopt -s histappend 2>/dev/null || true\n"
        "PROMPT_COMMAND='history -a'\n"
        "alias ll='ls -lah --color=auto'\n"
        "alias update='pkg update -y && pkg upgrade -y'\n"
        "PS1='\\[\\e[1;38;2;110;148;204m\\]\\w\\[\\e[0m\\] \\[\\e[1;90m\\]›\\[\\e[0m\\] '\n";
    int fd = open(bashrcPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0600);
    if (fd >= 0) {
        writeAllFd(fd, rcText, strlen(rcText));
        close(fd);
    }

    unlink("/data/data/com.termuxx/file/usr/etc/motd");
    gEnvInitialized = true;
    pthread_mutex_unlock(&gInitOnceMutex);
}

static bool isWordChar(uint32_t cp) noexcept {
    if (cp <= 0x20) return false;
    if (cp < 0x7F) {
        const char ch = static_cast<char>(cp);
        return isalnum(static_cast<unsigned char>(ch)) ||
               ch == '_' || ch == '-' || ch == '.' || ch == '/' || ch == ':' ||
               ch == '~' || ch == '+' || ch == '@' || ch == '?' || ch == '=' || ch == '&';
    }
    return true;
}

class NeoTerminalEngine {
public:
    mutable pthread_mutex_t mMutex;
    int mRows;
    int mCols;
    int mCellWidthPx = 18;
    int mCellHeightPx = 36;
    int mMaxScrollback;

    std::vector<Row> mPrimaryGrid;
    std::vector<Row> mAltGrid;
    std::vector<Row> mScrollback;
    int mScrollbackHead = 0;
    int mScrollbackCount = 0;

    int mCursorRow = 0;
    int mCursorCol = 0;
    int mCursorShape = gDefaultCursorShape; // 0 = Block, 1 = Underline, 2 = Bar/Beam (DECSCUSR)
    bool mCursorVisible = true;
    bool mPendingWrap = false;

    int mSavedRowPrimary = 0, mSavedColPrimary = 0;
    int mSavedRowAlt = 0, mSavedColAlt = 0;
    uint32_t mSavedFg = DEFAULT_FG, mSavedBg = DEFAULT_BG;
    uint16_t mSavedFlags = 0;

    int mScrollTop = 0;
    int mScrollBottom = 0;

    uint32_t mCurFg = DEFAULT_FG;
    uint32_t mCurBg = DEFAULT_BG;
    uint16_t mCurFlags = 0;

    std::atomic<int> mModes{MODE_AUTOWRAP};
    uint32_t mPalette[256];

    // ColorOS 17 Logical-Row Text Selection & Handle Drag State:
    bool mSelActive = false;
    int mSelX1 = -1, mSelY1 = -1;
    int mSelX2 = -1, mSelY2 = -1;
    int mDragHandle = 0; // 0 = none, 1 = start handle, 2 = end handle
    float mDragOffsetX = 0.0f;
    float mDragOffsetY = 0.0f;

    enum class State { NORMAL, ESC, CSI, OSC, OSC_ESC, CHARSET };
    State mState = State::NORMAL;
    char mCsiPrefix = 0;
    char mCsiIntermediate = 0;
    std::vector<int> mCsiParams;
    std::vector<bool> mCsiColonSubparam;
    bool mNextParamIsSub = false;
    int mCurParam = -1;
    std::string mOscBuffer;

    uint32_t mUtf8Codepoint = 0;
    int mUtf8BytesNeeded = 0;

    std::string mWindowTitle;
    bool mTitleChanged = false;
    std::string mOsc52Clipboard;
    bool mOsc52Changed = false;

    int mPtyFd = -1;
    int mShellPid = -1;
    bool mIsRoot = false;
    bool mStarted = false;
    std::atomic<bool> mRunning{false};
    std::atomic<bool> mFrameDirty{true};
    std::atomic<int> mExitCode{0};
    pthread_t mReaderThread{};
    bool mThreadStarted = false;

    NeoTerminalEngine(int rows, int cols, int maxScrollback, bool isRoot)
        : mRows(std::max(2, rows)),
          mCols(std::max(4, cols)),
          mMaxScrollback(std::max(100, maxScrollback)),
          mScrollTop(0),
          mScrollBottom(std::max(2, rows) - 1),
          mIsRoot(isRoot) {
        pthread_mutex_init(&mMutex, nullptr);
        initPalette();
        mPrimaryGrid.reserve(static_cast<size_t>(mRows));
        mAltGrid.reserve(static_cast<size_t>(mRows));
        for (int r = 0; r < mRows; ++r) {
            mPrimaryGrid.emplace_back(mCols);
            mAltGrid.emplace_back(mCols);
        }
        mScrollback.reserve(256);
    }

    ~NeoTerminalEngine() {
        stopSession();
        pthread_mutex_destroy(&mMutex);
    }

    void lock() const noexcept { pthread_mutex_lock(&mMutex); }
    void unlock() const noexcept { pthread_mutex_unlock(&mMutex); }

    void initPalette() noexcept {
        static constexpr uint32_t base16[16] = {
            0xFF000000u, 0xFFE06C75u, 0xFF98C379u, 0xFFE5C07Bu,
            0xFF61AFEFu, 0xFFC678DDu, 0xFF56B6C2u, 0xFFABB2BFu,
            0xFF5C6370u, 0xFFFF7A85u, 0xFFB5E890u, 0xFFFFD68Au,
            0xFF82C8FFu, 0xFFDE98F5u, 0xFF70D6E3u, 0xFFFFFFFFu
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
        return (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) ? mAltGrid : mPrimaryGrid;
    }

    void markAllDirtyLocked() noexcept {
        for (auto& r : mPrimaryGrid) r.dirty = true;
        for (auto& r : mAltGrid) r.dirty = true;
        mFrameDirty.store(true, std::memory_order_release);
    }

    void markAllDirty() noexcept {
        lock();
        markAllDirtyLocked();
        unlock();
    }

    const Row& getRowByLogicalY(int logicalY) const noexcept {
        int modes = mModes.load(std::memory_order_relaxed);
        const auto& grid = (modes & MODE_ALT_SCREEN) ? mAltGrid : mPrimaryGrid;
        if ((modes & MODE_ALT_SCREEN) || logicalY >= 0 || mScrollbackCount == 0) {
            int r = std::clamp(logicalY, 0, mRows - 1);
            return grid[static_cast<size_t>(r)];
        }
        int sbLogical = std::clamp(mScrollbackCount + logicalY, 0, mScrollbackCount - 1);
        int sbPhysical = (mScrollbackHead + sbLogical) % mMaxScrollback;
        return mScrollback[static_cast<size_t>(sbPhysical)];
    }

    bool isLogicalCellSelected(int logicalY, int col) const noexcept {
        if (!mSelActive) return false;
        if (logicalY < mSelY1 || logicalY > mSelY2) return false;
        if (mSelY1 == mSelY2) return col >= mSelX1 && col <= mSelX2;
        if (logicalY == mSelY1) return col >= mSelX1;
        if (logicalY == mSelY2) return col <= mSelX2;
        return true;
    }

    void startSelectionAt(int scrollOffset, int screenCol, int screenRow) noexcept {
        lock();
        int modes = mModes.load(std::memory_order_relaxed);
        int clampedOffset = (modes & MODE_ALT_SCREEN) ? 0 : std::clamp(scrollOffset, 0, mScrollbackCount);
        int logicalY = std::clamp(screenRow, 0, mRows - 1) - clampedOffset;
        int col = std::clamp(screenCol, 0, mCols - 1);

        const Row& row = getRowByLogicalY(logicalY);
        int x1 = col;
        int x2 = col;
        if (col < static_cast<int>(row.cells.size()) && isWordChar(row.cells[static_cast<size_t>(col)].codepoint)) {
            while (x1 > 0 && isWordChar(row.cells[static_cast<size_t>(x1 - 1)].codepoint)) x1--;
            while (x2 + 1 < static_cast<int>(row.cells.size()) && isWordChar(row.cells[static_cast<size_t>(x2 + 1)].codepoint)) x2++;
        }

        mSelX1 = x1;
        mSelX2 = x2;
        mSelY1 = logicalY;
        mSelY2 = logicalY;
        mSelActive = true;
        mDragHandle = 0;
        markAllDirtyLocked();
        unlock();
    }

    void updateSelectionHandle(bool isStartHandle, int col, int logicalY) noexcept {
        lock();
        updateSelectionHandleLocked(isStartHandle, col, logicalY);
        unlock();
    }

    void updateSelectionHandleLocked(bool isStartHandle, int col, int logicalY) noexcept {
        if (!mSelActive) return;
        int modes = mModes.load(std::memory_order_relaxed);
        int maxSb = (modes & MODE_ALT_SCREEN) ? 0 : mScrollbackCount;
        col = std::clamp(col, 0, mCols - 1);
        logicalY = std::clamp(logicalY, -maxSb, mRows - 1);

        const Row& row = getRowByLogicalY(logicalY);
        if (isStartHandle) {
            mSelX1 = col;
            mSelY1 = logicalY;
            if (mSelY1 > mSelY2 || (mSelY1 == mSelY2 && mSelX1 > mSelX2)) {
                std::swap(mSelX1, mSelX2);
                std::swap(mSelY1, mSelY2);
                if (mDragHandle == 1) mDragHandle = 2;
            }
            if (mSelX1 > 0 && mSelX1 < static_cast<int>(row.cells.size()) &&
                (row.cells[static_cast<size_t>(mSelX1)].flags & FLAG_WIDE_TRAIL)) {
                mSelX1--;
            }
        } else {
            mSelX2 = col;
            mSelY2 = logicalY;
            if (mSelY2 < mSelY1 || (mSelY1 == mSelY2 && mSelX2 < mSelX1)) {
                std::swap(mSelX1, mSelX2);
                std::swap(mSelY1, mSelY2);
                if (mDragHandle == 2) mDragHandle = 1;
            }
            if (mSelX2 + 1 < mCols && mSelX2 < static_cast<int>(row.cells.size()) &&
                (row.cells[static_cast<size_t>(mSelX2)].flags & FLAG_WIDE_LEAD)) {
                mSelX2++;
            }
        }
        markAllDirtyLocked();
    }

    // ColorOS 17 Lollipop Selection Geometry & Touch State Machine in C++23:
    // Start Handle: vertical stem on left edge of mSelX1, circle on TOP of stem (or bottom if rowTop1 < 2*radius)
    // End Handle:   vertical stem on right edge of mSelX2, circle on BOTTOM of stem
    bool computeSelectionGeometryLocked(
        int scrollOffset,
        float cellW,
        float cellH,
        float viewW,
        float viewH,
        float radiusPx,
        float outGeom[14]) const noexcept {

        if (!mSelActive) return false;
        cellW = std::max(1.0f, cellW);
        cellH = std::max(1.0f, cellH);
        viewW = std::max(2.0f, viewW);
        viewH = std::max(2.0f, viewH);

        int modes = mModes.load(std::memory_order_relaxed);
        int clampedOffset = (modes & MODE_ALT_SCREEN) ? 0 : std::clamp(scrollOffset, 0, mScrollbackCount);

        float x1 = static_cast<float>(mSelX1) * cellW;
        float top1 = static_cast<float>(mSelY1 + clampedOffset) * cellH;
        float bot1 = top1 + cellH;
        float circleY1 = (top1 >= radiusPx * 2.1f) ? (top1 - radiusPx) : (bot1 + radiusPx);

        float x2 = static_cast<float>(mSelX2 + 1) * cellW;
        float top2 = static_cast<float>(mSelY2 + clampedOffset) * cellH;
        float bot2 = top2 + cellH;
        float circleY2 = (bot2 + radiusPx * 2.1f <= viewH) ? (bot2 + radiusPx) : (top2 - radiusPx);

        outGeom[0] = std::clamp(x1, 1.0f, std::max(1.0f, viewW - 1.0f));
        outGeom[1] = top1;
        outGeom[2] = bot1;
        outGeom[3] = outGeom[0];
        outGeom[4] = circleY1;

        outGeom[5] = std::clamp(x2, 1.0f, std::max(1.0f, viewW - 1.0f));
        outGeom[6] = top2;
        outGeom[7] = bot2;
        outGeom[8] = outGeom[5];
        outGeom[9] = circleY2;

        float rectLeft = (mSelY1 == mSelY2) ? std::min(x1, x2) : 0.0f;
        float rectRight = (mSelY1 == mSelY2) ? std::max(x1, x2) : viewW;
        float rectTop = std::min(top1, circleY1 - radiusPx) - radiusPx * 0.6f;
        float rectBot = std::max(bot2, circleY2 + radiusPx) + radiusPx * 0.6f;

        outGeom[10] = std::clamp(rectLeft, 0.0f, std::max(1.0f, viewW));
        outGeom[11] = std::clamp(rectTop, 0.0f, std::max(1.0f, viewH));
        outGeom[12] = std::clamp(rectRight, outGeom[10] + 1.0f, std::max(outGeom[10] + 1.0f, viewW));
        outGeom[13] = std::clamp(rectBot, outGeom[11] + 1.0f, std::max(outGeom[11] + 1.0f, viewH));
        return true;
    }

    int onSelectionTouchLocked(
        int action,
        int scrollOffset,
        float touchX,
        float touchY,
        float cellW,
        float cellH,
        float viewW,
        float viewH,
        float radiusPx) noexcept {

        if (!mSelActive || !std::isfinite(touchX) || !std::isfinite(touchY)) return 0;
        cellW = std::max(1.0f, cellW);
        cellH = std::max(1.0f, cellH);

        float geom[14];
        computeSelectionGeometryLocked(scrollOffset, cellW, cellH, viewW, viewH, radiusPx, geom);

        if (action == 0) { // ACTION_DOWN
            float dx1 = touchX - geom[3];
            float dy1 = touchY - geom[4];
            float d1Sq = dx1 * dx1 + dy1 * dy1;

            float stemMid1Y = 0.5f * (geom[1] + geom[2]);
            float d1StemSq = (touchX - geom[0]) * (touchX - geom[0]) + (touchY - stemMid1Y) * (touchY - stemMid1Y);
            d1Sq = std::min(d1Sq, d1StemSq);

            float dx2 = touchX - geom[8];
            float dy2 = touchY - geom[9];
            float d2Sq = dx2 * dx2 + dy2 * dy2;
            float stemMid2Y = 0.5f * (geom[6] + geom[7]);
            float d2StemSq = (touchX - geom[5]) * (touchX - geom[5]) + (touchY - stemMid2Y) * (touchY - stemMid2Y);
            d2Sq = std::min(d2Sq, d2StemSq);

            float hitRad = radiusPx * 3.6f;
            float hitRadSq = hitRad * hitRad;

            if (d1Sq <= hitRadSq || d2Sq <= hitRadSq) {
                if (d1Sq <= d2Sq) {
                    mDragHandle = 1;
                    mDragOffsetX = touchX - (geom[0] + cellW * 0.5f);
                    mDragOffsetY = touchY - stemMid1Y;
                } else {
                    mDragHandle = 2;
                    mDragOffsetX = touchX - (geom[5] - cellW * 0.5f);
                    mDragOffsetY = touchY - stemMid2Y;
                }
                return 1;
            }
            mDragHandle = 0;
            return 0;
        } else if (action == 2) { // ACTION_MOVE
            if (mDragHandle == 0) return 0;
            int modes = mModes.load(std::memory_order_relaxed);
            int clampedOffset = (modes & MODE_ALT_SCREEN) ? 0 : std::clamp(scrollOffset, 0, mScrollbackCount);
            float targetX = touchX - mDragOffsetX;
            float targetY = touchY - mDragOffsetY;

            int screenCol = std::clamp(static_cast<int>(targetX / cellW), 0, mCols - 1);
            int screenRow = std::clamp(static_cast<int>(targetY / cellH), 0, mRows - 1);
            int logicalY = screenRow - clampedOffset;

            updateSelectionHandleLocked(mDragHandle == 1, screenCol, logicalY);
            return 1;
        } else if (action == 1 || action == 3) { // ACTION_UP / ACTION_CANCEL
            if (mDragHandle != 0) {
                mDragHandle = 0;
                return 2;
            }
        }
        return 0;
    }

    bool stopSelection() noexcept {
        lock();
        if (!mSelActive) {
            unlock();
            return false;
        }
        mSelActive = false;
        mDragHandle = 0;
        mSelX1 = mSelY1 = mSelX2 = mSelY2 = -1;
        markAllDirtyLocked();
        unlock();
        return true;
    }

    std::string extractSelectedTextLocked() const {
        if (!mSelActive) return "";
        std::string out;
        out.reserve(4096);

        for (int y = mSelY1; y <= mSelY2; ++y) {
            const Row& row = getRowByLogicalY(y);
            int xStart = (y == mSelY1) ? std::max(0, mSelX1) : 0;
            int xEnd   = (y == mSelY2) ? std::min<int>(mCols - 1, mSelX2) : (mCols - 1);

            int lastNonSpace = xStart - 1;
            for (int c = xEnd; c >= xStart; --c) {
                if (c < static_cast<int>(row.cells.size()) &&
                    row.cells[static_cast<size_t>(c)].codepoint != ' ' &&
                    !(row.cells[static_cast<size_t>(c)].flags & FLAG_WIDE_TRAIL)) {
                    lastNonSpace = c;
                    break;
                }
            }
            if (y == mSelY2 && lastNonSpace < xEnd && xEnd < static_cast<int>(row.cells.size())) {
                if (mSelY1 == mSelY2 && lastNonSpace < xStart) lastNonSpace = xEnd;
            }
            for (int c = xStart; c <= lastNonSpace; ++c) {
                const Cell& cell = row.cells[static_cast<size_t>(c)];
                if (cell.flags & FLAG_WIDE_TRAIL) continue;
                appendUtf8(out, cell.codepoint);
            }
            if (y < mSelY2 && !row.wrapped) {
                out.push_back('\n');
            }
        }
        return out;
    }

    void resetLocked() noexcept {
        mCurFg = DEFAULT_FG;
        mCurBg = DEFAULT_BG;
        mCurFlags = 0;
        mCursorRow = 0;
        mCursorCol = 0;
        mCursorShape = gDefaultCursorShape;
        mCursorVisible = true;
        mPendingWrap = false;
        mScrollTop = 0;
        mScrollBottom = mRows - 1;
        mModes.store(MODE_AUTOWRAP, std::memory_order_relaxed);
        mState = State::NORMAL;
        mUtf8BytesNeeded = 0;
        mSelActive = false;
        mDragHandle = 0;
        for (auto& r : mPrimaryGrid) r.clear();
        for (auto& r : mAltGrid) r.clear();
        mFrameDirty.store(true, std::memory_order_release);
    }

    void reflowPrimaryAndScrollback(int newRows, int newCols) {
        mSelActive = false;
        mDragHandle = 0;
        int maxActivePrimary = mCursorRow;
        for (int r = mRows - 1; r > mCursorRow; --r) {
            const Row& row = mPrimaryGrid[static_cast<size_t>(r)];
            bool nonEmpty = row.wrapped;
            if (!nonEmpty) {
                for (const auto& c : row.cells) {
                    if (!c.isDefaultBlank()) { nonEmpty = true; break; }
                }
            }
            if (nonEmpty) {
                maxActivePrimary = r;
                break;
            }
        }

        int totalInputRows = mScrollbackCount + (maxActivePrimary + 1);
        auto getInputRow = [&](int idx) -> const Row& {
            if (idx < mScrollbackCount) {
                int phys = (mScrollbackHead + idx) % mMaxScrollback;
                return mScrollback[static_cast<size_t>(phys)];
            }
            return mPrimaryGrid[static_cast<size_t>(idx - mScrollbackCount)];
        };

        int cursorInputRow = mScrollbackCount + mCursorRow;
        int newCursorGlobalRow = 0;
        int newCursorCol = mCursorCol;
        bool cursorMapped = false;

        std::vector<Row> outRows;
        outRows.reserve(static_cast<size_t>(totalInputRows + 32));
        std::vector<Cell> logicalLine;
        logicalLine.reserve(static_cast<size_t>(std::max(mCols, newCols) * 4));

        int idx = 0;
        while (idx < totalInputRows) {
            logicalLine.clear();
            int cursorOffsetInLogical = -1;

            while (idx < totalInputRows) {
                const Row& r = getInputRow(idx);
                bool isCursorRow = (idx == cursorInputRow);
                int rowCols = static_cast<int>(r.cells.size());

                int effectiveLen = rowCols;
                if (!r.wrapped) {
                    effectiveLen = 0;
                    for (int c = rowCols - 1; c >= 0; --c) {
                        if (!r.cells[static_cast<size_t>(c)].isDefaultBlank()) {
                            effectiveLen = c + 1;
                            break;
                        }
                    }
                    if (isCursorRow) {
                        effectiveLen = std::max(effectiveLen, std::min(mCursorCol, rowCols));
                    }
                }

                if (isCursorRow) {
                    cursorOffsetInLogical = static_cast<int>(logicalLine.size()) + std::min(mCursorCol, rowCols);
                }

                for (int c = 0; c < effectiveLen; ++c) {
                    logicalLine.push_back(r.cells[static_cast<size_t>(c)]);
                }

                idx++;
                if (!r.wrapped) break;
            }

            if (logicalLine.empty()) {
                Row blankRow(newCols);
                if (cursorOffsetInLogical >= 0 && !cursorMapped) {
                    newCursorGlobalRow = static_cast<int>(outRows.size());
                    newCursorCol = 0;
                    cursorMapped = true;
                }
                outRows.push_back(std::move(blankRow));
                continue;
            }

            size_t pos = 0;
            size_t totalCells = logicalLine.size();
            while (pos < totalCells) {
                Row slice(newCols);
                int dstCol = 0;
                while (pos < totalCells && dstCol < newCols) {
                    const Cell& cell = logicalLine[pos];
                    if ((cell.flags & FLAG_WIDE_LEAD) && dstCol == newCols - 1) {
                        break;
                    }
                    if (cursorOffsetInLogical >= 0 && !cursorMapped && static_cast<size_t>(cursorOffsetInLogical) == pos) {
                        newCursorGlobalRow = static_cast<int>(outRows.size());
                        newCursorCol = dstCol;
                        cursorMapped = true;
                    }
                    slice.cells[static_cast<size_t>(dstCol++)] = cell;
                    pos++;
                }
                if (cursorOffsetInLogical >= 0 && !cursorMapped && static_cast<size_t>(cursorOffsetInLogical) <= pos) {
                    newCursorGlobalRow = static_cast<int>(outRows.size());
                    newCursorCol = std::min(dstCol, newCols - 1);
                    cursorMapped = true;
                }
                slice.wrapped = (pos < totalCells);
                slice.dirty = true;
                outRows.push_back(std::move(slice));
            }
        }

        mScrollback.clear();
        mScrollbackHead = 0;
        mScrollbackCount = 0;

        mPrimaryGrid.assign(static_cast<size_t>(newRows), Row(newCols));
        int totalOut = static_cast<int>(outRows.size());
        if (totalOut <= newRows) {
            for (int r = 0; r < totalOut; ++r) {
                mPrimaryGrid[static_cast<size_t>(r)] = std::move(outRows[static_cast<size_t>(r)]);
            }
            mCursorRow = std::clamp(newCursorGlobalRow, 0, newRows - 1);
        } else {
            int sbCount = totalOut - newRows;
            for (int i = 0; i < sbCount; ++i) {
                pushScrollbackRow(outRows[static_cast<size_t>(i)]);
            }
            for (int r = 0; r < newRows; ++r) {
                mPrimaryGrid[static_cast<size_t>(r)] = std::move(outRows[static_cast<size_t>(sbCount + r)]);
            }
            mCursorRow = std::clamp(newCursorGlobalRow - sbCount, 0, newRows - 1);
        }
        mCursorCol = std::clamp(newCursorCol, 0, newCols - 1);
    }

    void resize(int newRows, int newCols, int cellW = 18, int cellH = 36) {
        newRows = std::max(2, newRows);
        newCols = std::max(4, newCols);

        lock();
        mCellWidthPx = std::max(4, cellW);
        mCellHeightPx = std::max(8, cellH);

        if (newRows != mRows || newCols != mCols) {
            int modes = mModes.load(std::memory_order_relaxed);
            if (!(modes & MODE_ALT_SCREEN) && newCols != mCols) {
                reflowPrimaryAndScrollback(newRows, newCols);
            } else {
                mPrimaryGrid.resize(static_cast<size_t>(newRows), Row(newCols));
                for (auto& r : mPrimaryGrid) r.resize(newCols);
                for (auto& r : mScrollback) r.resize(newCols);
            }

            mAltGrid.resize(static_cast<size_t>(newRows), Row(newCols));
            for (auto& r : mAltGrid) r.resize(newCols);

            mRows = newRows;
            mCols = newCols;
            mScrollTop = 0;
            mScrollBottom = mRows - 1;
            mCursorRow = std::clamp(mCursorRow, 0, mRows - 1);
            mCursorCol = std::clamp(mCursorCol, 0, mCols - 1);
            mPendingWrap = false;
            markAllDirtyLocked();
        }
        int fd = mPtyFd;
        int pid = mShellPid;
        int cw = mCellWidthPx;
        int ch = mCellHeightPx;
        unlock();

        if (fd >= 0) {
            struct winsize sz{};
            sz.ws_row = static_cast<unsigned short>(newRows);
            sz.ws_col = static_cast<unsigned short>(newCols);
            sz.ws_xpixel = static_cast<unsigned short>(newCols * cw);
            sz.ws_ypixel = static_cast<unsigned short>(newRows * ch);
            ioctl(fd, TIOCSWINSZ, &sz);
            if (pid > 0) {
                kill(-pid, SIGWINCH);
                kill(pid, SIGWINCH);
            }
        }
    }

    void pushScrollbackRow(const Row& row) noexcept {
        if (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) return;
        int idx = (mScrollbackHead + mScrollbackCount) % mMaxScrollback;
        if (mScrollbackCount < mMaxScrollback) {
            if (static_cast<int>(mScrollback.size()) <= idx) {
                mScrollback.push_back(row);
            } else {
                mScrollback[static_cast<size_t>(idx)] = row;
            }
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
        int modes = mModes.load(std::memory_order_relaxed);

        for (int n = 0; n < count; ++n) {
            if (top == 0 && !(modes & MODE_ALT_SCREEN)) {
                pushScrollbackRow(grid[0]);
                if (mSelActive) {
                    mSelY1--;
                    mSelY2--;
                    if (mSelY1 < -mScrollbackCount) {
                        mSelActive = false;
                        mDragHandle = 0;
                    }
                }
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

    static void clearWidePairIfSplit(Row& row, int col) noexcept {
        int nCols = static_cast<int>(row.cells.size());
        if (col < 0 || col >= nCols) return;
        uint16_t fl = row.cells[static_cast<size_t>(col)].flags;
        if ((fl & FLAG_WIDE_TRAIL) && col > 0) {
            row.cells[static_cast<size_t>(col - 1)].reset();
        } else if ((fl & FLAG_WIDE_LEAD) && col + 1 < nCols) {
            row.cells[static_cast<size_t>(col + 1)].reset();
        }
    }

    void emitCodepoint(uint32_t cp) noexcept {
        int w = unicodeCharWidth(cp);
        if (w <= 0) return;

        auto& grid = activeGrid();
        int modes = mModes.load(std::memory_order_relaxed);
        if (mPendingWrap && (modes & MODE_AUTOWRAP)) {
            grid[static_cast<size_t>(mCursorRow)].wrapped = true;
            mCursorCol = 0;
            lineFeed();
        }

        if (w == 2 && mCursorCol == mCols - 1) {
            clearWidePairIfSplit(grid[static_cast<size_t>(mCursorRow)], mCursorCol);
            grid[static_cast<size_t>(mCursorRow)].cells[static_cast<size_t>(mCursorCol)].reset(mCurFg, mCurBg, mCurFlags);
            grid[static_cast<size_t>(mCursorRow)].dirty = true;
            if (modes & MODE_AUTOWRAP) {
                grid[static_cast<size_t>(mCursorRow)].wrapped = true;
                mCursorCol = 0;
                lineFeed();
            }
        }

        Row& row = grid[static_cast<size_t>(mCursorRow)];
        row.dirty = true;

        clearWidePairIfSplit(row, mCursorCol);
        if (w == 2 && mCursorCol + 1 < mCols) {
            clearWidePairIfSplit(row, mCursorCol + 1);
        }

        Cell& target = row.cells[static_cast<size_t>(mCursorCol)];
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
            case 0x07: mModes.fetch_or(MODE_BELL_TRIGGERED, std::memory_order_relaxed); break;
            case 0x08: mPendingWrap = false; if (mCursorCol > 0) mCursorCol--; break;
            case 0x09: mPendingWrap = false; mCursorCol = std::min(mCols - 1, (mCursorCol & ~7) + 8); break;
            case 0x0A:
            case 0x0B:
            case 0x0C: lineFeed(); break;
            case 0x0D: mPendingWrap = false; mCursorCol = 0; break;
            case 0x1B: mState = State::ESC; break;
            default: break;
        }
    }

    void handleSgr() noexcept {
        if (mCsiParams.empty()) {
            mCsiParams.push_back(0);
            mCsiColonSubparam.push_back(false);
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
                if (i + 1 < mCsiParams.size() && i + 1 < mCsiColonSubparam.size() && mCsiColonSubparam[i + 1]) {
                    int sub = mCsiParams[++i];
                    mCurFlags &= ~(FLAG_UNDERLINE | FLAG_UNDERCURL | FLAG_DOUBLE_UNDERLINE);
                    if (sub == 1 || sub == 4 || sub == 5) mCurFlags |= FLAG_UNDERLINE;
                    else if (sub == 2) mCurFlags |= FLAG_DOUBLE_UNDERLINE;
                    else if (sub == 3) mCurFlags |= FLAG_UNDERCURL;
                } else {
                    mCurFlags |= FLAG_UNDERLINE;
                }
            } else if (p == 5 || p == 6) {
                mCurFlags |= FLAG_BLINK;
            } else if (p == 7) {
                mCurFlags |= FLAG_INVERSE;
            } else if (p == 8) {
                mCurFlags |= FLAG_INVISIBLE;
            } else if (p == 9) {
                mCurFlags |= FLAG_STRIKETHROUGH;
            } else if (p == 21) {
                mCurFlags |= FLAG_DOUBLE_UNDERLINE;
            } else if (p == 22) {
                mCurFlags &= ~(FLAG_BOLD | FLAG_DIM);
            } else if (p == 23) {
                mCurFlags &= ~FLAG_ITALIC;
            } else if (p == 24) {
                mCurFlags &= ~(FLAG_UNDERLINE | FLAG_UNDERCURL | FLAG_DOUBLE_UNDERLINE);
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
            } else if (p == 59) {
                // Reset underline color
            } else if (p >= 90 && p <= 97) {
                mCurFg = mPalette[8 + (p - 90)];
            } else if (p >= 100 && p <= 107) {
                mCurBg = mPalette[8 + (p - 100)];
            } else if (p == 38 || p == 48 || p == 58) {
                bool isFg = (p == 38);
                bool isBg = (p == 48);
                if (i + 1 < mCsiParams.size()) {
                    int mode = mCsiParams[i + 1];
                    if (mode == 5 && i + 2 < mCsiParams.size()) {
                        int idx = std::clamp(mCsiParams[i + 2], 0, 255);
                        if (isFg) mCurFg = mPalette[idx];
                        else if (isBg) mCurBg = mPalette[idx];
                        i += 2;
                    } else if (mode == 2 && i + 4 < mCsiParams.size()) {
                        size_t rIdx = i + 2;
                        if (i + 5 < mCsiParams.size() &&
                            i + 5 < mCsiColonSubparam.size() &&
                            mCsiColonSubparam[i + 2] &&
                            mCsiColonSubparam[i + 3] &&
                            mCsiColonSubparam[i + 4] &&
                            mCsiColonSubparam[i + 5]) {
                            rIdx = i + 3;
                        }
                        if (rIdx + 2 < mCsiParams.size()) {
                            uint32_t r = static_cast<uint32_t>(std::clamp(mCsiParams[rIdx], 0, 255));
                            uint32_t g = static_cast<uint32_t>(std::clamp(mCsiParams[rIdx + 1], 0, 255));
                            uint32_t b = static_cast<uint32_t>(std::clamp(mCsiParams[rIdx + 2], 0, 255));
                            uint32_t color = 0xFF000000u | (r << 16) | (g << 8) | b;
                            if (isFg) mCurFg = color;
                            else if (isBg) mCurBg = color;
                            i = rIdx + 2;
                        }
                    }
                }
            }
        }
    }

    void setDecPrivateMode(int mode, bool enabled) noexcept {
        auto setBit = [&](int bit) {
            if (enabled) mModes.fetch_or(bit, std::memory_order_relaxed);
            else mModes.fetch_and(~bit, std::memory_order_relaxed);
        };
        switch (mode) {
            case 1: setBit(MODE_APP_CURSOR_KEYS); break;
            case 7: setBit(MODE_AUTOWRAP); break;
            case 25: mCursorVisible = enabled; break;
            case 47:
            case 1047:
            case 1049: {
                bool currentlyAlt = (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) != 0;
                if (enabled && !currentlyAlt) {
                    mSavedRowPrimary = mCursorRow;
                    mSavedColPrimary = mCursorCol;
                    mModes.fetch_or(MODE_ALT_SCREEN, std::memory_order_relaxed);
                    for (auto& r : mAltGrid) r.clear(DEFAULT_FG, DEFAULT_BG, 0);
                    mCursorRow = 0; mCursorCol = 0; mPendingWrap = false;
                    markAllDirtyLocked();
                } else if (!enabled && currentlyAlt) {
                    mModes.fetch_and(~MODE_ALT_SCREEN, std::memory_order_relaxed);
                    mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                    mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                    mPendingWrap = false;
                    markAllDirtyLocked();
                }
                break;
            }
            case 1000:
            case 1002:
            case 1003: setBit(MODE_MOUSE_TRACKING); break;
            case 1004: setBit(MODE_FOCUS_EVENTS); break;
            case 1006:
            case 1016: setBit(MODE_MOUSE_SGR); break;
            case 2004: setBit(MODE_BRACKETED_PASTE); break;
            case 2026:
                if (enabled) {
                    mModes.fetch_or(MODE_SYNC_OUTPUT, std::memory_order_relaxed);
                } else {
                    mModes.fetch_and(~MODE_SYNC_OUTPUT, std::memory_order_relaxed);
                    mFrameDirty.store(true, std::memory_order_release);
                }
                break;
            default: break;
        }
    }

    void dispatchCsi(char finalByte, std::string& reply) {
        if (mCurParam >= 0 || !mCsiParams.empty()) {
            mCsiParams.push_back(mCurParam < 0 ? 0 : mCurParam);
            mCsiColonSubparam.push_back(mNextParamIsSub);
        }
        auto param = [&](size_t idx, int defVal) -> int {
            if (idx >= mCsiParams.size() || mCsiParams[idx] <= 0) return defVal;
            return mCsiParams[idx];
        };
        auto& grid = activeGrid();

        if (mCsiIntermediate == ' ' && finalByte == 'q') {
            int style = param(0, 1);
            if (style <= 2) mCursorShape = gDefaultCursorShape;
            else if (style <= 4) mCursorShape = 1;
            else if (style <= 6) mCursorShape = 2;
            return;
        }

        if (mCsiPrefix == '>' && finalByte == 'q') {
            reply += "\x1bP>|Termuxx-C++23(4.0)\x1b\\";
            return;
        }

        if (mCsiPrefix == '?') {
            if (finalByte == 'h' || finalByte == 'l') {
                bool en = (finalByte == 'h');
                for (int p : mCsiParams) setDecPrivateMode(p, en);
            }
            return;
        }

        switch (finalByte) {
            case 'A': mPendingWrap = false; mCursorRow = std::max(mScrollTop, mCursorRow - param(0, 1)); break;
            case 'B': mPendingWrap = false; mCursorRow = std::min(mScrollBottom, mCursorRow + param(0, 1)); break;
            case 'C': mPendingWrap = false; mCursorCol = std::min(mCols - 1, mCursorCol + param(0, 1)); break;
            case 'D': mPendingWrap = false; mCursorCol = std::max(0, mCursorCol - param(0, 1)); break;
            case 'E': mPendingWrap = false; mCursorCol = 0; mCursorRow = std::min(mScrollBottom, mCursorRow + param(0, 1)); break;
            case 'F': mPendingWrap = false; mCursorCol = 0; mCursorRow = std::max(mScrollTop, mCursorRow - param(0, 1)); break;
            case 'G':
            case '`': mPendingWrap = false; mCursorCol = std::clamp(param(0, 1) - 1, 0, mCols - 1); break;
            case 'H':
            case 'f':
                mPendingWrap = false;
                mCursorRow = std::clamp(param(0, 1) - 1, 0, mRows - 1);
                mCursorCol = std::clamp(param(1, 1) - 1, 0, mCols - 1);
                break;
            case 'd': mPendingWrap = false; mCursorRow = std::clamp(param(0, 1) - 1, 0, mRows - 1); break;
            case 'J': {
                int mode = mCsiParams.empty() ? 0 : mCsiParams[0];
                if (mode == 0) {
                    Row& cur = grid[static_cast<size_t>(mCursorRow)];
                    clearWidePairIfSplit(cur, mCursorCol);
                    for (int c = mCursorCol; c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                    cur.dirty = true;
                    for (int r = mCursorRow + 1; r < mRows; ++r) grid[static_cast<size_t>(r)].clear(mCurFg, mCurBg, 0);
                } else if (mode == 1) {
                    for (int r = 0; r < mCursorRow; ++r) grid[static_cast<size_t>(r)].clear(mCurFg, mCurBg, 0);
                    Row& cur = grid[static_cast<size_t>(mCursorRow)];
                    clearWidePairIfSplit(cur, mCursorCol);
                    for (int c = 0; c <= mCursorCol && c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                    cur.dirty = true;
                } else if (mode == 2 || mode == 3) {
                    for (int r = 0; r < mRows; ++r) grid[static_cast<size_t>(r)].clear(mCurFg, mCurBg, 0);
                    if (mode == 3) { mScrollback.clear(); mScrollbackHead = 0; mScrollbackCount = 0; }
                }
                break;
            }
            case 'K': {
                int mode = mCsiParams.empty() ? 0 : mCsiParams[0];
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                if (mode == 0) {
                    clearWidePairIfSplit(cur, mCursorCol);
                    for (int c = mCursorCol; c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                } else if (mode == 1) {
                    clearWidePairIfSplit(cur, mCursorCol);
                    for (int c = 0; c <= mCursorCol && c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                } else if (mode == 2) {
                    cur.clear(mCurFg, mCurBg, 0);
                }
                break;
            }
            case 'L': if (mCursorRow >= mScrollTop && mCursorRow <= mScrollBottom) scrollDown(mCursorRow, mScrollBottom, param(0, 1)); break;
            case 'M': if (mCursorRow >= mScrollTop && mCursorRow <= mScrollBottom) scrollUp(mCursorRow, mScrollBottom, param(0, 1)); break;
            case '@': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                clearWidePairIfSplit(cur, mCursorCol);
                for (int c = mCols - 1; c >= mCursorCol + count; --c) cur.cells[static_cast<size_t>(c)] = cur.cells[static_cast<size_t>(c - count)];
                for (int c = mCursorCol; c < mCursorCol + count; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                break;
            }
            case 'P': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                clearWidePairIfSplit(cur, mCursorCol);
                if (mCursorCol + count < mCols) clearWidePairIfSplit(cur, mCursorCol + count);
                for (int c = mCursorCol; c < mCols - count; ++c) cur.cells[static_cast<size_t>(c)] = cur.cells[static_cast<size_t>(c + count)];
                for (int c = mCols - count; c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                break;
            }
            case 'X': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                clearWidePairIfSplit(cur, mCursorCol);
                if (mCursorCol + count - 1 < mCols) clearWidePairIfSplit(cur, mCursorCol + count - 1);
                for (int c = mCursorCol; c < mCursorCol + count; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                break;
            }
            case 'S': scrollUp(mScrollTop, mScrollBottom, param(0, 1)); break;
            case 'T': scrollDown(mScrollTop, mScrollBottom, param(0, 1)); break;
            case 'r': {
                int top = std::clamp(param(0, 1) - 1, 0, mRows - 1);
                int bottom = std::clamp(param(1, mRows) - 1, 0, mRows - 1);
                if (top < bottom) { mScrollTop = top; mScrollBottom = bottom; mCursorRow = 0; mCursorCol = 0; mPendingWrap = false; }
                break;
            }
            case 'm': handleSgr(); break;
            case 's':
                if (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) {
                    mSavedRowAlt = mCursorRow; mSavedColAlt = mCursorCol;
                } else {
                    mSavedRowPrimary = mCursorRow; mSavedColPrimary = mCursorCol;
                }
                break;
            case 'u':
                if (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) {
                    mCursorRow = std::clamp(mSavedRowAlt, 0, mRows - 1);
                    mCursorCol = std::clamp(mSavedColAlt, 0, mCols - 1);
                } else {
                    mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                    mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                }
                mPendingWrap = false;
                break;
            case 'n': {
                int p = param(0, 0);
                if (p == 5) reply += "\x1b[0n";
                else if (p == 6) {
                    char buf[32];
                    snprintf(buf, sizeof(buf), "\x1b[%d;%dR", mCursorRow + 1, mCursorCol + 1);
                    reply += buf;
                }
                break;
            }
            case 't': {
                int op = param(0, 0);
                char buf[64];
                if (op == 14) {
                    snprintf(buf, sizeof(buf), "\x1b[4;%d;%dt", mRows * mCellHeightPx, mCols * mCellWidthPx);
                    reply += buf;
                } else if (op == 16) {
                    snprintf(buf, sizeof(buf), "\x1b[6;%d;%dt", mCellHeightPx, mCellWidthPx);
                    reply += buf;
                } else if (op == 18) {
                    snprintf(buf, sizeof(buf), "\x1b[8;%d;%dt", mRows, mCols);
                    reply += buf;
                }
                break;
            }
            case 'c':
                if (mCsiPrefix == 0) reply += "\x1b[?62;22c";
                else if (mCsiPrefix == '>') reply += "\x1b[>41;350;0c";
                break;
            default: break;
        }
    }

    void handleOscComplete() noexcept {
        size_t semi = mOscBuffer.find(';');
        if (semi == std::string::npos) return;
        int code = atoi(mOscBuffer.substr(0, semi).c_str());
        if (code == 0 || code == 2) {
            mWindowTitle = mOscBuffer.substr(semi + 1);
            mTitleChanged = true;
        } else if (code == 52) {
            size_t secondSemi = mOscBuffer.find(';', semi + 1);
            if (secondSemi != std::string::npos) {
                mOsc52Clipboard = decodeBase64(mOscBuffer.substr(secondSemi + 1));
                if (!mOsc52Clipboard.empty()) mOsc52Changed = true;
            }
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
                    clearWidePairIfSplit(row, mCursorCol);
                    clearWidePairIfSplit(row, mCursorCol + 15);
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
                        if (--mUtf8BytesNeeded == 0) emitCodepoint(mUtf8Codepoint);
                    }
                } else if ((b & 0xE0) == 0xC0) {
                    mUtf8Codepoint = b & 0x1F; mUtf8BytesNeeded = 1;
                } else if ((b & 0xF0) == 0xE0) {
                    mUtf8Codepoint = b & 0x0F; mUtf8BytesNeeded = 2;
                } else if ((b & 0xF8) == 0xF0) {
                    mUtf8Codepoint = b & 0x07; mUtf8BytesNeeded = 3;
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
                        mCsiColonSubparam.clear();
                        mNextParamIsSub = false;
                        mCurParam = -1;
                    } else if (b == ']') {
                        mState = State::OSC; mOscBuffer.clear();
                    } else if (b == '(' || b == ')' || b == '*' || b == '+') {
                        mState = State::CHARSET;
                    } else {
                        if (b == '7') {
                            if (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) {
                                mSavedRowAlt = mCursorRow; mSavedColAlt = mCursorCol;
                            } else {
                                mSavedRowPrimary = mCursorRow; mSavedColPrimary = mCursorCol;
                            }
                            mSavedFg = mCurFg; mSavedBg = mCurBg; mSavedFlags = mCurFlags;
                        } else if (b == '8') {
                            if (mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) {
                                mCursorRow = std::clamp(mSavedRowAlt, 0, mRows - 1);
                                mCursorCol = std::clamp(mSavedColAlt, 0, mCols - 1);
                            } else {
                                mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                                mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                            }
                            mCurFg = mSavedFg; mCurBg = mSavedBg; mCurFlags = mSavedFlags; mPendingWrap = false;
                        } else if (b == 'D') lineFeed();
                        else if (b == 'E') { mCursorCol = 0; lineFeed(); }
                        else if (b == 'M') reverseIndex();
                        else if (b == 'c') resetLocked();
                        mState = State::NORMAL;
                    }
                    break;
                case State::CHARSET: mState = State::NORMAL; break;
                case State::CSI:
                    if (b == 0x1B) mState = State::ESC;
                    else if (b >= '0' && b <= '9') mCurParam = (mCurParam < 0 ? 0 : mCurParam * 10) + (b - '0');
                    else if (b == ';' || b == ':') {
                        mCsiParams.push_back(mCurParam < 0 ? 0 : mCurParam);
                        mCsiColonSubparam.push_back(mNextParamIsSub);
                        mNextParamIsSub = (b == ':');
                        mCurParam = -1;
                    } else if (b >= '<' && b <= '?') mCsiPrefix = static_cast<char>(b);
                    else if (b >= 0x20 && b <= 0x2F) mCsiIntermediate = static_cast<char>(b);
                    else if (b >= 0x40 && b <= 0x7E) { dispatchCsi(static_cast<char>(b), reply); mState = State::NORMAL; }
                    else if (b < 0x20) handleControlChar(b);
                    break;
                case State::OSC:
                    if (b == 0x07) { handleOscComplete(); mState = State::NORMAL; }
                    else if (b == 0x1B) mState = State::OSC_ESC;
                    else if (mOscBuffer.size() < 8192) mOscBuffer.push_back(static_cast<char>(b));
                    break;
                case State::OSC_ESC:
                    if (b == '\\') handleOscComplete();
                    mState = State::NORMAL;
                    break;
                default: mState = State::NORMAL; break;
            }
        }
        if (!(mModes.load(std::memory_order_relaxed) & MODE_SYNC_OUTPUT)) {
            mFrameDirty.store(true, std::memory_order_release);
        }
    }

    static void* readerThreadEntry(void* arg) {
        static_cast<NeoTerminalEngine*>(arg)->readerLoop();
        return nullptr;
    }

    void readerLoop() {
        JNIEnv* threadEnv = nullptr;
        bool attached = false;
        if (gJvm && gJvm->AttachCurrentThread(&threadEnv, nullptr) == JNI_OK) {
            attached = true;
        }

        uint8_t buf[65536];
        std::string reply;
        reply.reserve(64);

        while (mRunning.load(std::memory_order_acquire)) {
            int fd = mPtyFd;
            if (fd < 0) break;

            struct pollfd pfd{};
            pfd.fd = fd;
            pfd.events = POLLIN | POLLERR | POLLHUP;
            int timeoutMs = (mModes.load(std::memory_order_relaxed) & MODE_SYNC_OUTPUT) ? 16 : -1;
            int prc = poll(&pfd, 1, timeoutMs);
            if (prc < 0) {
                if (errno == EINTR) continue;
                break;
            }
            if (prc == 0) {
                if (mModes.load(std::memory_order_relaxed) & MODE_SYNC_OUTPUT) {
                    mFrameDirty.store(true, std::memory_order_release);
                    notifySurfaceFromNative();
                }
                continue;
            }

            if (pfd.revents & (POLLIN | POLLERR | POLLHUP)) {
                ssize_t n = read(fd, buf, sizeof(buf));
                if (n <= 0) {
                    if (n < 0 && (errno == EINTR || errno == EAGAIN)) continue;
                    break;
                }
                reply.clear();
                lock();
                processStream(buf, static_cast<size_t>(n), reply);
                bool shouldWake = !(mModes.load(std::memory_order_relaxed) & MODE_SYNC_OUTPUT);
                unlock();
                if (!reply.empty()) writeAllFd(fd, reply.data(), reply.size());
                if (shouldWake) notifySurfaceFromNative();
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
        mModes.fetch_and(~MODE_SYNC_OUTPUT, std::memory_order_relaxed);
        processStream(reinterpret_cast<const uint8_t*>(exitMsg), strlen(exitMsg), dummy);
        unlock();
        notifySurfaceFromNative();

        if (attached && gJvm) {
            gJvm->DetachCurrentThread();
        }
    }

    void stopSession() noexcept {
        mRunning.store(false, std::memory_order_release);
        if (mShellPid > 0) {
            kill(-mShellPid, SIGHUP);
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

static bool startSessionProcess(
    NeoTerminalEngine* engine,
    const char* nativeLibDir,
    int rows,
    int cols,
    int cellW,
    int cellH) noexcept {

    if (engine->mStarted) {
        engine->resize(rows, cols, cellW, cellH);
        return true;
    }
    engine->mStarted = true;
    engine->resize(rows, cols, cellW, cellH);

    initializeEnvironmentAndAutoUpdater(nativeLibDir, false);
    std::string preloadPath = std::string(LIB_PATH) + "/libtermuxx-redirect.so";
    std::string fullPreload = preloadPath;

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
    bool hasLogin = (access(loginBin, R_OK) == 0);

    std::vector<std::string> envStrings;
    envStrings.emplace_back("TERM=xterm-256color");
    envStrings.emplace_back("COLORTERM=truecolor");
    envStrings.emplace_back("TERM_PROGRAM=Termuxx-C++23");
    envStrings.emplace_back("HOME=" + std::string(HOME_PATH));
    envStrings.emplace_back("PREFIX=" + std::string(PREFIX_PATH));
    envStrings.emplace_back("TMPDIR=" + std::string(TMP_PATH));
    envStrings.emplace_back("TMP=" + std::string(TMP_PATH));
    envStrings.emplace_back("LANG=en_US.UTF-8");
    envStrings.emplace_back("PATH=" + std::string(BIN_PATH) + ":" + std::string(BIN_PATH) + "/applets:/system/bin:/system/xbin:/sbin");
    envStrings.emplace_back("LD_LIBRARY_PATH=" + std::string(LIB_PATH));
    if (access(preloadPath.c_str(), R_OK) == 0) {
        envStrings.emplace_back("LD_PRELOAD=" + fullPreload);
    }
    envStrings.emplace_back("ANDROID_DATA=/data");
    envStrings.emplace_back("ANDROID_ROOT=/system");
    envStrings.emplace_back("ANDROID__BUILD_VERSION_SDK=37");
    envStrings.emplace_back("TERMUX_VERSION=0.119.0");
    envStrings.emplace_back("SHELL=" + std::string(userShell));

    std::string execPath;
    std::vector<std::string> argStrings;

    if (engine->mIsRoot) {
        execPath = resolveExecutable(suBins, 7);
        std::string rootCmd =
            "export PATH=\"" + std::string(BIN_PATH) + ":" + std::string(BIN_PATH) + "/applets:/sbin:/system/bin:/system/xbin:$PATH\"; "
            "export LD_LIBRARY_PATH=\"" + std::string(LIB_PATH) + "\"; "
            "export PREFIX=\"" + std::string(PREFIX_PATH) + "\"; "
            "export HOME=\"" + std::string(HOME_PATH) + "\"; "
            "export TMPDIR=\"" + std::string(TMP_PATH) + "\"; "
            "export TERM=xterm-256color; export COLORTERM=truecolor; ";
        if (access(preloadPath.c_str(), R_OK) == 0) {
            rootCmd += "export LD_PRELOAD=\"" + fullPreload + "\"; ";
        }
        rootCmd += "cd \"" + std::string(HOME_PATH) + "\" 2>/dev/null || true; ";
        rootCmd += "exec \"" + std::string(userShell) + "\" -l";

        argStrings.push_back(execPath);
        argStrings.push_back("-c");
        argStrings.push_back(rootCmd);
    } else if (hasLogin) {
        execPath = loginBin;
        argStrings.push_back("-login");
    } else {
        execPath = userShell;
        argStrings.push_back("-sh");
    }

    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) return false;

    if (grantpt(ptm) != 0 || unlockpt(ptm) != 0) { close(ptm); return false; }

    char devname[64];
    if (ptsname_r(ptm, devname, sizeof(devname)) != 0) { close(ptm); return false; }

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
    if (pid < 0) { close(ptm); return false; }

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
        for (const auto& s : envStrings) putenv(strdup(s.c_str()));
        if (chdir(HOME_PATH) != 0) (void)chdir("/");

        std::vector<char*> argv;
        for (const auto& a : argStrings) argv.push_back(strdup(a.c_str()));
        argv.push_back(nullptr);

        execve(execPath.c_str(), argv.data(), environ);
        if (!engine->mIsRoot) {
            char* linkerArgs[] = {
                const_cast<char*>("/system/bin/linker64"),
                const_cast<char*>(userShell),
                const_cast<char*>("-l"),
                nullptr
            };
            execve("/system/bin/linker64", linkerArgs, environ);
        }
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

static void restartEngineIfExited(NeoTerminalEngine* engine, const char* nativeLibDir) noexcept {
    if (!engine || engine->mRunning.load(std::memory_order_acquire)) return;
    engine->stopSession();
    engine->lock();
    int r = engine->mRows;
    int c = engine->mCols;
    int cw = engine->mCellWidthPx;
    int ch = engine->mCellHeightPx;
    engine->mStarted = false;
    engine->unlock();
    startSessionProcess(engine, nativeLibDir, r, c, cw, ch);
}

static pthread_mutex_t gGlobalMutex = PTHREAD_MUTEX_INITIALIZER;
static std::vector<std::shared_ptr<NeoTerminalEngine>> gSessions;
static int gActiveIdx = -1;
static std::string gNativeLibDir;
static std::atomic<int> gBootstrapState{0};
static bool gCtrlArmed = false;
static bool gAltArmed  = false;

static std::shared_ptr<NeoTerminalEngine> activeEngineLocked() noexcept {
    if (gActiveIdx >= 0 && gActiveIdx < static_cast<int>(gSessions.size())) {
        return gSessions[static_cast<size_t>(gActiveIdx)];
    }
    return nullptr;
}

static std::shared_ptr<NeoTerminalEngine> getActiveEngineSafe() noexcept {
    pthread_mutex_lock(&gGlobalMutex);
    auto eng = activeEngineLocked();
    pthread_mutex_unlock(&gGlobalMutex);
    return eng;
}

static bool sendKeyCodeInternal(NeoTerminalEngine* engine, int keyCode, int metaState) noexcept {
    if (!engine) return false;
    bool appCursor = (engine->mModes.load(std::memory_order_relaxed) & MODE_APP_CURSOR_KEYS) != 0;
    bool shift = (metaState & AMETA_SHIFT_ON) != 0;
    bool alt   = (metaState & AMETA_ALT_ON) != 0;
    bool ctrl  = (metaState & AMETA_CTRL_ON) != 0;
    int mod    = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);

    char buf[32];
    const char* seq = nullptr;

    switch (keyCode) {
        case AKEYCODE_ENTER:
        case AKEYCODE_NUMPAD_ENTER: seq = alt ? "\x1b\r" : "\r"; break;
        case AKEYCODE_DEL: seq = alt ? "\x1b\x7f" : (ctrl ? "\x08" : "\x7f"); break;
        case AKEYCODE_FORWARD_DEL:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[3;%d~", mod); seq = buf; }
            else seq = "\x1b[3~";
            break;
        case AKEYCODE_INSERT:
            if (mod > 1) { snprintf(buf, sizeof(buf), "\x1b[2;%d~", mod); seq = buf; }
            else seq = "\x1b[2~";
            break;
        case AKEYCODE_TAB: seq = shift ? "\x1b[Z" : "\t"; break;
        case AKEYCODE_ESCAPE: seq = "\x1b"; break;
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
        default: break;
    }

    if (seq) {
        engine->writeToPty(seq, strlen(seq));
        return true;
    }
    return false;
}

static void sendCodepointInternal(NeoTerminalEngine* engine, int codepoint, bool ctrlDown, bool altDown) noexcept {
    if (!engine) return;
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

static void* bootstrapWorkerThread(void* /*arg*/) {
    bool extracted = false;
    if (access("/data/data/com.termuxx/file/usr/bin/bash", R_OK) != 0 &&
        access("/data/data/com.termuxx/file/usr/bin/apt", R_OK) != 0 &&
        blob_size > 0) {
        extracted = extractBootstrapInCpp23(
            reinterpret_cast<const uint8_t*>(blob),
            static_cast<size_t>(blob_size)
        );
    }

    pthread_mutex_lock(&gGlobalMutex);
    std::string libDir = gNativeLibDir;
    pthread_mutex_unlock(&gGlobalMutex);

    initializeEnvironmentAndAutoUpdater(libDir.c_str(), extracted);
    gBootstrapState.store(2, std::memory_order_release);
    notifySurfaceFromNative();
    return nullptr;
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    gJvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeRegisterSurface(JNIEnv* env, jclass /*clazz*/, jobject surface) {
    pthread_mutex_lock(&gViewMutex);
    if (gSurfaceView) {
        env->DeleteGlobalRef(gSurfaceView);
        gSurfaceView = nullptr;
        gPostInvalidateId = nullptr;
    }
    if (surface) {
        gSurfaceView = env->NewGlobalRef(surface);
        jclass cls = env->GetObjectClass(surface);
        if (cls) {
            gPostInvalidateId = env->GetMethodID(cls, "postInvalidateOnAnimation", "()V");
        }
    }
    pthread_mutex_unlock(&gViewMutex);
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeInitAndPollBootstrap(JNIEnv* env, jclass /*clazz*/, jstring nativeLibDir) {
    int state = gBootstrapState.load(std::memory_order_acquire);
    if (state == 2) return 2;

    if (state == 0) {
        pthread_mutex_lock(&gGlobalMutex);
        if (gBootstrapState.load(std::memory_order_relaxed) == 0) {
            if (nativeLibDir) {
                const char* utf = env->GetStringUTFChars(nativeLibDir, nullptr);
                if (utf) {
                    gNativeLibDir = utf;
                    env->ReleaseStringUTFChars(nativeLibDir, utf);
                }
            }
            gBootstrapState.store(1, std::memory_order_release);
            pthread_t th{};
            pthread_create(&th, nullptr, &bootstrapWorkerThread, nullptr);
            pthread_detach(th);
        }
        pthread_mutex_unlock(&gGlobalMutex);
    }
    return gBootstrapState.load(std::memory_order_acquire);
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeNewSession(
    JNIEnv* /*env*/, jclass /*clazz*/, jboolean asRoot, jint rows, jint cols, jint cellW, jint cellH) {
    pthread_mutex_lock(&gGlobalMutex);
    auto engine = std::make_shared<NeoTerminalEngine>(rows, cols, 2000, asRoot == JNI_TRUE);
    std::string libDir = gNativeLibDir;
    gSessions.push_back(engine);
    gActiveIdx = static_cast<int>(gSessions.size()) - 1;
    int idx = gActiveIdx;
    pthread_mutex_unlock(&gGlobalMutex);

    if (rows > 0 && cols > 0) {
        startSessionProcess(engine.get(), libDir.c_str(), rows, cols, cellW, cellH);
    }
    return idx;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSwitchSession(JNIEnv* /*env*/, jclass /*clazz*/, jint index) {
    pthread_mutex_lock(&gGlobalMutex);
    if (index >= 0 && index < static_cast<int>(gSessions.size())) {
        gActiveIdx = index;
        gSessions[static_cast<size_t>(index)]->markAllDirty();
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeCloseSession(JNIEnv* /*env*/, jclass /*clazz*/, jint index) {
    std::shared_ptr<NeoTerminalEngine> toClose;
    pthread_mutex_lock(&gGlobalMutex);
    if (index >= 0 && index < static_cast<int>(gSessions.size())) {
        toClose = gSessions[static_cast<size_t>(index)];
        gSessions.erase(gSessions.begin() + index);
        if (gSessions.empty()) {
            gActiveIdx = -1;
        } else if (gActiveIdx >= static_cast<int>(gSessions.size())) {
            gActiveIdx = static_cast<int>(gSessions.size()) - 1;
        }
        if (gActiveIdx >= 0) {
            gSessions[static_cast<size_t>(gActiveIdx)]->markAllDirty();
        }
    }
    int rem = static_cast<int>(gSessions.size());
    pthread_mutex_unlock(&gGlobalMutex);

    if (toClose) {
        toClose->stopSession();
    }
    return rem;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeDestroyAll(JNIEnv* /*env*/, jclass /*clazz*/) {
    std::vector<std::shared_ptr<NeoTerminalEngine>> oldSessions;
    pthread_mutex_lock(&gGlobalMutex);
    oldSessions.swap(gSessions);
    gActiveIdx = -1;
    pthread_mutex_unlock(&gGlobalMutex);

    for (auto& eng : oldSessions) {
        if (eng) eng->stopSession();
    }
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetSessionCount(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    int c = static_cast<int>(gSessions.size());
    pthread_mutex_unlock(&gGlobalMutex);
    return c;
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetActiveIndex(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    int idx = gActiveIdx;
    pthread_mutex_unlock(&gGlobalMutex);
    return idx;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeIsSessionRoot(JNIEnv* /*env*/, jclass /*clazz*/, jint index) {
    pthread_mutex_lock(&gGlobalMutex);
    bool r = (index >= 0 && index < static_cast<int>(gSessions.size())) && gSessions[static_cast<size_t>(index)]->mIsRoot;
    pthread_mutex_unlock(&gGlobalMutex);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeResizeActive(
    JNIEnv* /*env*/, jclass /*clazz*/, jint rows, jint cols, jint cellW, jint cellH) {
    pthread_mutex_lock(&gGlobalMutex);
    auto eng = activeEngineLocked();
    std::string libDir = gNativeLibDir;
    pthread_mutex_unlock(&gGlobalMutex);
    if (eng) {
        startSessionProcess(eng.get(), libDir.c_str(), rows, cols, cellW, cellH);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeConsumeFrameDirty(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    bool d = eng && eng->mFrameDirty.exchange(false, std::memory_order_acq_rel);
    return d ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_termuxx_app_TermuxActivity_nativeConsumeOsc52Clipboard(JNIEnv* env, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    if (!eng) return nullptr;
    eng->lock();
    if (!eng->mOsc52Changed) {
        eng->unlock();
        return nullptr;
    }
    eng->mOsc52Changed = false;
    std::string clip = eng->mOsc52Clipboard;
    eng->unlock();
    return env->NewStringUTF(clip.c_str());
}

// ============================================================================
// ColorOS 17 Selection JNI Methods (C++23 Handle Geometry & Touch Dragging)
// ============================================================================
JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeStartTextSelection(
    JNIEnv* /*env*/, jclass /*clazz*/, jint scrollOffset, jint screenCol, jint screenRow) {
    auto eng = getActiveEngineSafe();
    if (eng) {
        eng->startSelectionAt(scrollOffset, screenCol, screenRow);
    }
}


JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeOnSelectionTouchPx(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jint action,
    jint scrollOffset,
    jfloat touchX,
    jfloat touchY,
    jfloat cellW,
    jfloat cellH,
    jfloat viewW,
    jfloat viewH,
    jfloat radiusPx) {
    auto eng = getActiveEngineSafe();
    int res = 0;
    if (eng) {
        eng->lock();
        res = eng->onSelectionTouchLocked(action, scrollOffset, touchX, touchY, cellW, cellH, viewW, viewH, radiusPx);
        eng->unlock();
    }
    return res;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetSelectionGeometryPx(
    JNIEnv* env,
    jclass /*clazz*/,
    jint scrollOffset,
    jfloat cellW,
    jfloat cellH,
    jfloat viewW,
    jfloat viewH,
    jfloat radiusPx,
    jfloatArray outGeom) {
    auto eng = getActiveEngineSafe();
    float geom[14] = {0};
    bool ok = false;
    if (eng) {
        eng->lock();
        ok = eng->computeSelectionGeometryLocked(scrollOffset, cellW, cellH, viewW, viewH, radiusPx, geom);
        eng->unlock();
    }
    if (ok && outGeom && env->GetArrayLength(outGeom) >= 14) {
        env->SetFloatArrayRegion(outGeom, 0, 14, geom);
    }
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeStopTextSelection(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    bool stopped = eng && eng->stopSelection();
    return stopped ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeIsSelectingText(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    if (!eng) return JNI_FALSE;
    eng->lock();
    bool active = eng->mSelActive;
    eng->unlock();
    return active ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSelectAll(JNIEnv* /*env*/, jclass /*clazz*/, jint scrollOffset) {
    auto eng = getActiveEngineSafe();
    if (eng) {
        eng->lock();
        int modes = eng->mModes.load(std::memory_order_relaxed);
        int clampedOffset = (modes & MODE_ALT_SCREEN) ? 0 : std::clamp<int>(scrollOffset, 0, eng->mScrollbackCount);
        eng->mSelActive = true;
        eng->mSelY1 = -clampedOffset;
        eng->mSelX1 = 0;
        eng->mSelY2 = eng->mRows - 1 - clampedOffset;
        eng->mSelX2 = eng->mCols - 1;
        eng->markAllDirtyLocked();
        eng->unlock();
    }
}


JNIEXPORT jstring JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetSelectedText(JNIEnv* env, jclass /*clazz*/) {
    auto engine = getActiveEngineSafe();
    if (!engine) return env->NewStringUTF("");

    engine->lock();
    std::string out = engine->extractSelectedTextLocked();
    engine->unlock();
    return env->NewStringUTF(out.c_str());
}

// ============================================================================
// Universal Customizable Extra-Key & Input JNI Methods
// ============================================================================
JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeDispatchCustomKey(JNIEnv* env, jclass /*clazz*/, jstring keyLabel) {
    if (!keyLabel) return 0;
    const char* utf = env->GetStringUTFChars(keyLabel, nullptr);
    if (!utf) return 0;
    std::string label(utf);
    env->ReleaseStringUTFChars(keyLabel, utf);

    std::shared_ptr<NeoTerminalEngine> eng;
    std::string libDir;
    int meta = 0;
    bool ctrl = false;
    bool alt = false;
    int mask = 0;

    pthread_mutex_lock(&gGlobalMutex);
    if (label == "CTRL") {
        gCtrlArmed = !gCtrlArmed;
        mask = (gCtrlArmed ? 1 : 0) | (gAltArmed ? 2 : 0);
        pthread_mutex_unlock(&gGlobalMutex);
        return mask;
    } else if (label == "ALT") {
        gAltArmed = !gAltArmed;
        mask = (gCtrlArmed ? 1 : 0) | (gAltArmed ? 2 : 0);
        pthread_mutex_unlock(&gGlobalMutex);
        return mask;
    } else {
        eng = activeEngineLocked();
        libDir = gNativeLibDir;
        ctrl = gCtrlArmed;
        alt = gAltArmed;
        meta = (ctrl ? AMETA_CTRL_ON : 0) | (alt ? AMETA_ALT_ON : 0);
        gCtrlArmed = false;
        gAltArmed = false;
        mask = 0;
    }
    pthread_mutex_unlock(&gGlobalMutex);

    if (eng && !label.empty()) {
        if (!eng->mRunning.load(std::memory_order_acquire) &&
            (label == "ENTER" || label == "RET")) {
            restartEngineIfExited(eng.get(), libDir.c_str());
            return mask;
        }
        if (label == "ESC") sendKeyCodeInternal(eng.get(), AKEYCODE_ESCAPE, meta);
        else if (label == "TAB") sendKeyCodeInternal(eng.get(), AKEYCODE_TAB, meta);
        else if (label == "ENTER" || label == "RET") sendKeyCodeInternal(eng.get(), AKEYCODE_ENTER, meta);
        else if (label == "BKSP") sendKeyCodeInternal(eng.get(), AKEYCODE_DEL, meta);
        else if (label == "DEL") sendKeyCodeInternal(eng.get(), AKEYCODE_FORWARD_DEL, meta);
        else if (label == "INS") sendKeyCodeInternal(eng.get(), AKEYCODE_INSERT, meta);
        else if (label == "▲" || label == "UP") sendKeyCodeInternal(eng.get(), AKEYCODE_DPAD_UP, meta);
        else if (label == "▼" || label == "DOWN") sendKeyCodeInternal(eng.get(), AKEYCODE_DPAD_DOWN, meta);
        else if (label == "◀" || label == "LEFT") sendKeyCodeInternal(eng.get(), AKEYCODE_DPAD_LEFT, meta);
        else if (label == "▶" || label == "RIGHT") sendKeyCodeInternal(eng.get(), AKEYCODE_DPAD_RIGHT, meta);
        else if (label == "HOME") sendKeyCodeInternal(eng.get(), AKEYCODE_MOVE_HOME, meta);
        else if (label == "END") sendKeyCodeInternal(eng.get(), AKEYCODE_MOVE_END, meta);
        else if (label == "PGUP") sendKeyCodeInternal(eng.get(), AKEYCODE_PAGE_UP, meta);
        else if (label == "PGDN") sendKeyCodeInternal(eng.get(), AKEYCODE_PAGE_DOWN, meta);
        else if (label == "F1") sendKeyCodeInternal(eng.get(), AKEYCODE_F1, meta);
        else if (label == "F2") sendKeyCodeInternal(eng.get(), AKEYCODE_F2, meta);
        else if (label == "F3") sendKeyCodeInternal(eng.get(), AKEYCODE_F3, meta);
        else if (label == "F4") sendKeyCodeInternal(eng.get(), AKEYCODE_F4, meta);
        else if (label == "F5") sendKeyCodeInternal(eng.get(), AKEYCODE_F5, meta);
        else if (label == "F6") sendKeyCodeInternal(eng.get(), AKEYCODE_F6, meta);
        else if (label == "F7") sendKeyCodeInternal(eng.get(), AKEYCODE_F7, meta);
        else if (label == "F8") sendKeyCodeInternal(eng.get(), AKEYCODE_F8, meta);
        else if (label == "F9") sendKeyCodeInternal(eng.get(), AKEYCODE_F9, meta);
        else if (label == "F10") sendKeyCodeInternal(eng.get(), AKEYCODE_F10, meta);
        else if (label == "F11") sendKeyCodeInternal(eng.get(), AKEYCODE_F11, meta);
        else if (label == "F12") sendKeyCodeInternal(eng.get(), AKEYCODE_F12, meta);
        else if (label == "SPACE" || label == "SPC") {
            sendCodepointInternal(eng.get(), ' ', ctrl, alt);
        } else if (label.rfind("CTRL+", 0) == 0 && label.size() > 5) {
            std::string sub = label.substr(5);
            if (sub == "SPACE" || sub == "SPC") {
                sendCodepointInternal(eng.get(), ' ', true, alt);
            } else {
                size_t consumed = 0;
                uint32_t cp = decodeFirstUtf8Codepoint(sub, consumed);
                sendCodepointInternal(eng.get(), static_cast<int>(cp), true, alt);
            }
        } else if (label.rfind("ALT+", 0) == 0 && label.size() > 4) {
            std::string sub = label.substr(4);
            if (sub == "BKSP" || sub == "DEL") {
                sendKeyCodeInternal(eng.get(), AKEYCODE_DEL, meta | AMETA_ALT_ON);
            } else {
                size_t consumed = 0;
                uint32_t cp = decodeFirstUtf8Codepoint(sub, consumed);
                sendCodepointInternal(eng.get(), static_cast<int>(cp), ctrl, true);
            }
        } else {
            std::string expanded;
            expanded.reserve(label.size());
            for (size_t i = 0; i < label.size(); ++i) {
                if (label[i] == '\\' && i + 1 < label.size()) {
                    char nx = label[i + 1];
                    if (nx == 'n' || nx == 'r') { expanded.push_back('\r'); ++i; continue; }
                    if (nx == 't') { expanded.push_back('\t'); ++i; continue; }
                    if (nx == 'e') { expanded.push_back('\x1b'); ++i; continue; }
                }
                expanded.push_back(label[i]);
            }
            size_t consumed = 0;
            uint32_t cp = decodeFirstUtf8Codepoint(expanded, consumed);
            sendCodepointInternal(eng.get(), static_cast<int>(cp), ctrl, alt);
            if (consumed < expanded.size()) {
                eng->writeToPty(expanded.data() + consumed, expanded.size() - consumed);
            }
        }
    }
    return mask;
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetModifierMask(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    int mask = (gCtrlArmed ? 1 : 0) | (gAltArmed ? 2 : 0);
    pthread_mutex_unlock(&gGlobalMutex);
    return mask;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSendText(
    JNIEnv* env, jclass /*clazz*/, jstring text, jboolean isPaste) {
    if (!text) return;
    pthread_mutex_lock(&gGlobalMutex);
    auto engine = activeEngineLocked();
    std::string libDir = gNativeLibDir;
    bool ctrl = gCtrlArmed;
    bool alt  = gAltArmed;
    gCtrlArmed = false;
    gAltArmed  = false;
    pthread_mutex_unlock(&gGlobalMutex);
    if (!engine) return;

    const char* utf = env->GetStringUTFChars(text, nullptr);
    if (!utf) return;
    std::string payload(utf);
    env->ReleaseStringUTFChars(text, utf);

    if (!engine->mRunning.load(std::memory_order_acquire) &&
        (payload == "\n" || payload == "\r" || payload == "\r\n")) {
        restartEngineIfExited(engine.get(), libDir.c_str());
        return;
    }

    std::string sanitized;
    sanitized.reserve(payload.size());
    for (size_t i = 0; i < payload.size(); ++i) {
        unsigned char c = static_cast<unsigned char>(payload[i]);
        if (c == 0xC2 && i + 1 < payload.size() && static_cast<unsigned char>(payload[i + 1]) == 0xA0) {
            sanitized.push_back(' ');
            i++;
        } else if (c == '\r' && i + 1 < payload.size() && payload[i + 1] == '\n') {
            sanitized.push_back('\r');
            i++;
        } else if (c == '\n') {
            sanitized.push_back('\r');
        } else if (isPaste && (c == 0x1B || (c >= 0x80 && c <= 0x9F))) {
            continue;
        } else {
            sanitized.push_back(static_cast<char>(c));
        }
    }

    if (isPaste || sanitized.size() > 1) {
        // Trim accidental leading spaces on pasted commands if at start of input so ignorespace never drops history
        size_t firstNonSpace = 0;
        while (firstNonSpace < sanitized.size() && sanitized[firstNonSpace] == ' ') {
            firstNonSpace++;
        }
        if (firstNonSpace > 0 && firstNonSpace < sanitized.size()) {
            sanitized.erase(0, firstNonSpace);
        }
    }

    if (isPaste) {
        // Only use bracketed paste if text spans multiple lines (contains an internal newline before the end)
        size_t firstCr = sanitized.find('\r');
        bool isMultiLine = (firstCr != std::string::npos && firstCr + 1 < sanitized.size());
        if (isMultiLine && (engine->mModes.load(std::memory_order_relaxed) & MODE_BRACKETED_PASTE) != 0) {
            std::string wrapped = "\x1b[200~" + sanitized + "\x1b[201~";
            engine->writeToPty(wrapped.data(), wrapped.size());
        } else {
            engine->writeToPty(sanitized.data(), sanitized.size());
        }
    } else if ((ctrl || alt) && !sanitized.empty()) {
        size_t consumed = 0;
        uint32_t cp = decodeFirstUtf8Codepoint(sanitized, consumed);
        sendCodepointInternal(engine.get(), static_cast<int>(cp), ctrl, alt);
        if (consumed < sanitized.size()) {
            engine->writeToPty(sanitized.data() + consumed, sanitized.size() - consumed);
        }
    } else if (!sanitized.empty()) {
        engine->writeToPty(sanitized.data(), sanitized.size());
    }
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSendKeyEvent(
    JNIEnv* /*env*/, jclass /*clazz*/, jint keyCode, jint unicodeChar, jint metaState) {
    pthread_mutex_lock(&gGlobalMutex);
    auto engine = activeEngineLocked();
    if (!engine) {
        pthread_mutex_unlock(&gGlobalMutex);
        return 0;
    }

    if (!engine->mRunning.load(std::memory_order_acquire) &&
        (keyCode == AKEYCODE_ENTER || keyCode == AKEYCODE_NUMPAD_ENTER)) {
        pthread_mutex_unlock(&gGlobalMutex);
        return 2;
    }

    int meta = metaState | (gCtrlArmed ? AMETA_CTRL_ON : 0) | (gAltArmed ? AMETA_ALT_ON : 0);
    gCtrlArmed = false;
    gAltArmed  = false;
    pthread_mutex_unlock(&gGlobalMutex);

    if (sendKeyCodeInternal(engine.get(), keyCode, meta)) return 1;
    if (unicodeChar > 0) {
        sendCodepointInternal(engine.get(), unicodeChar, (meta & AMETA_CTRL_ON) != 0, (meta & AMETA_ALT_ON) != 0);
        return 1;
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSendMouse(
    JNIEnv* /*env*/, jclass /*clazz*/, jint button, jint col, jint row, jboolean pressed) {
    auto engine = getActiveEngineSafe();
    if (!engine) return;
    int modes = engine->mModes.load(std::memory_order_relaxed);
    if ((modes & MODE_MOUSE_TRACKING) == 0) return;
    if ((modes & MODE_MOUSE_SGR) != 0) {
        engine->lock();
        int maxC = engine->mCols;
        int maxR = engine->mRows;
        engine->unlock();
        int c = std::clamp<int>(col, 1, maxC);
        int r = std::clamp<int>(row, 1, maxR);
        char buf[48];
        snprintf(buf, sizeof(buf), "\x1b[<%d;%d;%d%c", button, c, r, pressed ? 'M' : 'm');
        engine->writeToPty(buf, strlen(buf));
    }
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetActiveScrollback(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    if (!eng) return 0;
    eng->lock();
    int sb = (eng->mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) ? 0 : eng->mScrollbackCount;
    eng->unlock();
    return sb;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeIsRowDirty(JNIEnv* /*env*/, jclass /*clazz*/, jint screenRow) {
    auto eng = getActiveEngineSafe();
    bool dirty = false;
    if (eng) {
        eng->lock();
        if (screenRow >= 0 && screenRow < eng->mRows) {
            dirty = eng->activeGrid()[static_cast<size_t>(screenRow)].dirty;
        }
        eng->unlock();
    }
    return dirty ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeClearDirtyFlags(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    if (eng) {
        eng->lock();
        for (auto& r : eng->activeGrid()) r.dirty = false;
        eng->unlock();
    }
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativePackRowRender(
    JNIEnv* env,
    jclass /*clazz*/,
    jint scrollOffset,
    jint screenRow,
    jintArray outBgRuns,
    jintArray outGlyphs) {

    if (!outBgRuns || !outGlyphs) return 0;
    auto engine = getActiveEngineSafe();
    if (!engine) return 0;

    int maxBgRuns = static_cast<int>(env->GetArrayLength(outBgRuns)) / 3;
    int maxGlyphs = static_cast<int>(env->GetArrayLength(outGlyphs)) / 4;
    if (maxBgRuns <= 0 || maxGlyphs <= 0) return 0;

    engine->lock();
    int modes = engine->mModes.load(std::memory_order_relaxed);
    int clampedOffset = (modes & MODE_ALT_SCREEN) ? 0 : std::clamp<int>(scrollOffset, 0, engine->mScrollbackCount);
    int logicalY = screenRow - clampedOffset;
    const Row& row = engine->getRowByLogicalY(logicalY);
    int cols = std::min<int>(engine->mCols, static_cast<int>(row.cells.size()));

    auto* bgPtr = static_cast<jint*>(env->GetPrimitiveArrayCritical(outBgRuns, nullptr));
    auto* glPtr = static_cast<jint*>(env->GetPrimitiveArrayCritical(outGlyphs, nullptr));

    int bgCount = 0;
    int glCount = 0;

    if (bgPtr && glPtr) {
        auto cellBgAt = [&](int c) -> uint32_t {
            if (engine->isLogicalCellSelected(logicalY, c)) return SELECTION_BG;
            const Cell& cell = row.cells[static_cast<size_t>(c)];
            return resolveBgColor(cell.fg, cell.bg, cell.flags);
        };

        int runStart = 0;
        while (runStart < cols && bgCount < maxBgRuns) {
            uint32_t bg = cellBgAt(runStart);
            int runEnd = runStart + 1;
            while (runEnd < cols && cellBgAt(runEnd) == bg) {
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

        for (int c = 0; c < cols && glCount < maxGlyphs; ++c) {
            const Cell& cell = row.cells[static_cast<size_t>(c)];
            if (cell.codepoint <= ' ' || (cell.flags & (FLAG_INVISIBLE | FLAG_WIDE_TRAIL)) != 0) {
                continue;
            }
            uint32_t fg = engine->isLogicalCellSelected(logicalY, c)
                ? SELECTION_FG
                : resolveFgColor(cell.fg, cell.bg, cell.flags);

            if (!emitProceduralBoxOrBlockInCpp23(cell.codepoint, c, fg, glPtr, glCount, maxGlyphs)) {
                if (glCount < maxGlyphs) {
                    int base = glCount * 4;
                    glPtr[base]     = static_cast<jint>(cell.codepoint);
                    glPtr[base + 1] = c;
                    glPtr[base + 2] = static_cast<jint>(fg);
                    glPtr[base + 3] = static_cast<jint>(cell.flags);
                    glCount++;
                }
            }
        }
    }

    if (glPtr) env->ReleasePrimitiveArrayCritical(outGlyphs, glPtr, 0);
    if (bgPtr) env->ReleasePrimitiveArrayCritical(outBgRuns, bgPtr, 0);
    engine->unlock();

    return (bgCount << 16) | (glCount & 0xFFFF);
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSetDefaultCursorShape(JNIEnv* /*env*/, jclass /*clazz*/, jint shape) {
    pthread_mutex_lock(&gGlobalMutex);
    gDefaultCursorShape = std::clamp<int>(shape, 0, 2);
    for (auto& eng : gSessions) {
        if (eng) {
            eng->mCursorShape = gDefaultCursorShape;
            eng->markAllDirty();
        }
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetCursorState(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    int state = -1;
    if (eng) {
        eng->lock();
        if (eng->mCursorVisible) {
            state = ((eng->mCursorShape & 3) << 29) |
                    ((eng->mCursorRow & 0x1FFF) << 16) |
                    (eng->mCursorCol & 0xFFFF);
        }
        eng->unlock();
    }
    return state;
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetModesMask(JNIEnv* /*env*/, jclass /*clazz*/) {
    auto eng = getActiveEngineSafe();
    return eng ? eng->mModes.load(std::memory_order_relaxed) : 0;
}

JNIEXPORT jstring JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetTranscriptText(JNIEnv* env, jclass /*clazz*/) {
    auto engine = getActiveEngineSafe();
    if (!engine) return env->NewStringUTF("");

    std::string out;
    out.reserve(16384);

    engine->lock();
    int sbCount = (engine->mModes.load(std::memory_order_relaxed) & MODE_ALT_SCREEN) ? 0 : engine->mScrollbackCount;
    for (int i = -sbCount; i < engine->mRows; ++i) {
        const Row& row = engine->getRowByLogicalY(i);
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
        if (!row.wrapped && i < engine->mRows - 1) out.push_back('\n');
    }
    engine->unlock();
    return env->NewStringUTF(out.c_str());
}

} // extern "C"
