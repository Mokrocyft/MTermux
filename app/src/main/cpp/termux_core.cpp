/*
 * Termuxx — All-In-One C++23 Engine (libtermux-core.so)
 *
 * Features:
 * 1. Native C++23 ZIP Decompressor (zlib) + Dual 16-Byte Relocator ("com.termux/files" & "com.termux/cache")
 * 2. Silent Second-Stage Bootstrap Initializer + APT Cache Configurator
 * 3. Logical-Line Reflow Engine on Resize/Pinch-Zoom (unwraps & re-wraps scrollback + screen lines dynamically)
 * 4. Original Termux Logical-Row Text Selection Engine (mSelX1, mSelY1, mSelX2, mSelY2 across scrollback & screen)
 * 5. Modern 2026 TUI Standards:
 *    - Synchronized Output (?2026h / ?2026l tear-free rendering)
 *    - DECSCUSR Cursor Shapes (CSI Ps SP q: Block, Underline, Vertical Beam)
 *    - Sub-parameter SGR (4:0..4:5 undercurl FLAG_UNDERCURL & double-underline, 58;2;r;g;b underline color)
 *    - OSC 52 Base64 System Clipboard Integration (works inside tmux, neovim, helix, ssh)
 *    - XTVERSION (CSI > q), Window/Pixel Size Reporting (CSI 14 t / 18 t)
 * 6. Customizable Extra-Keys Engine (supports any key label, F1-F12, arrows, CTRL+X macros, sticky CTRL/ALT)
 */

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

// Pure Dark AMOLED Palette (#000000 background, #E5E9F0 foreground, #264F78 selection highlight)
constexpr uint32_t DEFAULT_FG   = 0xFFE5E9F0u;
constexpr uint32_t DEFAULT_BG   = 0xFF000000u;
constexpr uint32_t SELECTION_BG = 0xFF264F78u;
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
        return codepoint == ' ' && bg == DEFAULT_BG && (flags & (FLAG_INVERSE | FLAG_UNDERLINE | FLAG_UNDERCURL | FLAG_STRIKETHROUGH | FLAG_WIDE_LEAD | FLAG_WIDE_TRAIL)) == 0;
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
    int outFd = open(dstPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, mode);
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
        uint32_t lfhOffset = readLe32(zipData + ptr + 42);

        if (ptr + 46 + nameLen > zipSize) break;
        std::string entryName(reinterpret_cast<const char*>(zipData + ptr + 46), nameLen);
        ptr += 46 + nameLen + extraLen + commLen;

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
            bool isExec = (entryName.rfind("bin/", 0) == 0) ||
                          (entryName.rfind("libexec", 0) == 0) ||
                          (entryName.rfind("lib/apt/apt-helper", 0) == 0) ||
                          (entryName.rfind("lib/apt/methods", 0) == 0) ||
                          (entryName.rfind("etc/termux/bootstrap/", 0) == 0) ||
                          (entryName.rfind("var/lib/dpkg/info/", 0) == 0);
            mode_t fileMode = isExec ? 0700 : 0600;
            int fd = open(dstFile.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, fileMode);
            if (fd >= 0) {
                if (!outBuf.empty()) writeAllFd(fd, outBuf.data(), outBuf.size());
                close(fd);
                if (isExec) chmod(dstFile.c_str(), 0700);
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

static void runSecondStageBootstrapSilently(const char* preloadPath) noexcept {
    const char* secondStageScript = "/data/data/com.termuxx/file/usr/etc/termux/bootstrap/termux-bootstrap-second-stage.sh";
    if (access(secondStageScript, X_OK) != 0) return;

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
        execl("/data/data/com.termuxx/file/usr/bin/sh", "sh", secondStageScript, nullptr);
        _exit(0);
    } else if (pid > 0) {
        int status = 0;
        while (waitpid(pid, &status, 0) < 0 && errno == EINTR) {}
    }
}

static void initializeEnvironmentAndAutoUpdater(const char* nativeLibDir, bool justExtracted) noexcept {
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

    std::string dstSo = std::string(LIB_PATH) + "/libtermuxx-redirect.so";
    if (nativeLibDir && nativeLibDir[0] != '\0') {
        std::string srcSo = std::string(nativeLibDir) + "/libtermuxx-redirect.so";
        if (access(srcSo.c_str(), R_OK) == 0) {
            copyFileBinary(srcSo.c_str(), dstSo.c_str(), 0755);
        }
    }

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
        "  case \":$LD_PRELOAD:\" in\n"
        "    *\":/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so:\"*) ;;\n"
        "    *) export LD_PRELOAD=\"/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so${LD_PRELOAD:+:$LD_PRELOAD}\" ;;\n"
        "  esac\n"
        "fi\n";
    int pfd = open(profPath, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (pfd >= 0) {
        writeAllFd(pfd, profText, strlen(profText));
        close(pfd);
    }

    if (justExtracted) {
        runSecondStageBootstrapSilently(dstSo.c_str());
    }

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
        "  case \":$LD_PRELOAD:\" in\n"
        "    *\":$PREFIX/lib/libtermuxx-redirect.so:\"*) ;;\n"
        "    *) export LD_PRELOAD=\"$PREFIX/lib/libtermuxx-redirect.so${LD_PRELOAD:+:$LD_PRELOAD}\" ;;\n"
        "  esac\n"
        "fi\n"
        "alias ll='ls -lah --color=auto'\n"
        "alias update='pkg update -y && pkg upgrade -y'\n"
        "PS1='\\[\\e[1;37m\\]\\w\\[\\e[0m\\] \\[\\e[1;90m\\]›\\[\\e[0m\\] '\n";
    int fd = open(bashrcPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0600);
    if (fd >= 0) {
        writeAllFd(fd, rcText, strlen(rcText));
        close(fd);
    }

    unlink("/data/data/com.termuxx/file/usr/etc/motd");
}

static bool isWordChar(uint32_t cp) noexcept {
    if (cp <= 0x20) return false;
    if (cp < 0x7F) {
        const char ch = static_cast<char>(cp);
        return isalnum(static_cast<unsigned char>(ch)) ||
               ch == '_' || ch == '-' || ch == '.' || ch == '/' || ch == ':' || ch == '~' || ch == '+' || ch == '@' || ch == '?' || ch == '=' || ch == '&';
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
    int mCursorShape = 0; // 0 = Block, 1 = Underline, 2 = Bar/Beam (DECSCUSR)
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

    // Original Termux Logical-Row Text Selection State:
    // mSelY1 / mSelY2 are logical row indices: [0 .. mRows-1] for primary screen, [-mScrollbackCount .. -1] for scrollback
    bool mSelActive = false;
    int mSelX1 = -1, mSelY1 = -1;
    int mSelX2 = -1, mSelY2 = -1;

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
        return (mModes & MODE_ALT_SCREEN) ? mAltGrid : mPrimaryGrid;
    }

    void markAllDirty() noexcept {
        for (auto& r : mPrimaryGrid) r.dirty = true;
        for (auto& r : mAltGrid) r.dirty = true;
        mFrameDirty.store(true, std::memory_order_release);
    }

    const Row& getRowByLogicalY(int logicalY) const noexcept {
        const auto& grid = (mModes & MODE_ALT_SCREEN) ? mAltGrid : mPrimaryGrid;
        if ((mModes & MODE_ALT_SCREEN) || logicalY >= 0 || mScrollbackCount == 0) {
            int r = std::clamp(logicalY, 0, mRows - 1);
            return grid[static_cast<size_t>(r)];
        }
        int sbLogical = std::clamp(mScrollbackCount + logicalY, 0, mScrollbackCount - 1);
        int sbPhysical = (mScrollbackHead + sbLogical) % mMaxScrollback;
        return mScrollback[static_cast<size_t>(sbPhysical)];
    }

    const Row& getVisualRow(int scrollOffset, int screenRow) const noexcept {
        int clampedOffset = (mModes & MODE_ALT_SCREEN) ? 0 : std::clamp(scrollOffset, 0, mScrollbackCount);
        return getRowByLogicalY(screenRow - clampedOffset);
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
        int clampedOffset = (mModes & MODE_ALT_SCREEN) ? 0 : std::clamp(scrollOffset, 0, mScrollbackCount);
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
        markAllDirty();
        unlock();
    }

    void updateSelectionHandle(bool isStartHandle, int col, int logicalY) noexcept {
        lock();
        if (!mSelActive) {
            unlock();
            return;
        }
        int maxSb = (mModes & MODE_ALT_SCREEN) ? 0 : mScrollbackCount;
        col = std::clamp(col, 0, mCols - 1);
        logicalY = std::clamp(logicalY, -maxSb, mRows - 1);

        const Row& row = getRowByLogicalY(logicalY);
        if (isStartHandle) {
            mSelX1 = col;
            mSelY1 = logicalY;
            if (mSelY1 > mSelY2) mSelY1 = mSelY2;
            if (mSelY1 == mSelY2 && mSelX1 > mSelX2) mSelX1 = mSelX2;
            if (mSelX1 > 0 && mSelX1 < static_cast<int>(row.cells.size()) &&
                (row.cells[static_cast<size_t>(mSelX1)].flags & FLAG_WIDE_TRAIL)) {
                mSelX1--;
            }
        } else {
            mSelX2 = col;
            mSelY2 = logicalY;
            if (mSelY1 > mSelY2) mSelY2 = mSelY1;
            if (mSelY1 == mSelY2 && mSelX1 > mSelX2) mSelX2 = mSelX1;
            if (mSelX2 + 1 < mCols && mSelX2 < static_cast<int>(row.cells.size()) &&
                (row.cells[static_cast<size_t>(mSelX2)].flags & FLAG_WIDE_LEAD)) {
                mSelX2++;
            }
        }
        markAllDirty();
        unlock();
    }

    bool stopSelection() noexcept {
        lock();
        if (!mSelActive) {
            unlock();
            return false;
        }
        mSelActive = false;
        mSelX1 = mSelY1 = mSelX2 = mSelY2 = -1;
        markAllDirty();
        unlock();
        return true;
    }

    std::string extractSelectedText() const {
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
                // Keep explicit trailing selection if on the same single line
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

    void reset() noexcept {
        mCurFg = DEFAULT_FG;
        mCurBg = DEFAULT_BG;
        mCurFlags = 0;
        mCursorRow = 0;
        mCursorCol = 0;
        mCursorShape = 0;
        mCursorVisible = true;
        mPendingWrap = false;
        mScrollTop = 0;
        mScrollBottom = mRows - 1;
        mModes = MODE_AUTOWRAP;
        mState = State::NORMAL;
        mUtf8BytesNeeded = 0;
        mSelActive = false;
        for (auto& r : mPrimaryGrid) r.clear();
        for (auto& r : mAltGrid) r.clear();
        mFrameDirty.store(true, std::memory_order_release);
    }

    void reflowPrimaryAndScrollback(int newRows, int newCols) {
        mSelActive = false;
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

        mScrollback.assign(static_cast<size_t>(mMaxScrollback), Row(newCols));
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
        mCellWidthPx = std::max(4, cellW);
        mCellHeightPx = std::max(8, cellH);

        lock();
        if (newRows != mRows || newCols != mCols) {
            if (!(mModes & MODE_ALT_SCREEN) && newCols != mCols) {
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
            markAllDirty();
        }
        int fd = mPtyFd;
        int pid = mShellPid;
        unlock();

        if (fd >= 0) {
            struct winsize sz{};
            sz.ws_row = static_cast<unsigned short>(newRows);
            sz.ws_col = static_cast<unsigned short>(newCols);
            sz.ws_xpixel = static_cast<unsigned short>(newCols * mCellWidthPx);
            sz.ws_ypixel = static_cast<unsigned short>(newRows * mCellHeightPx);
            ioctl(fd, TIOCSWINSZ, &sz);
            if (pid > 0) {
                kill(-pid, SIGWINCH);
                kill(pid, SIGWINCH);
            }
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
                if (mSelActive) {
                    mSelY1--;
                    mSelY2--;
                    if (mSelY1 < -mScrollbackCount) {
                        mSelActive = false;
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
            case 0x07: mModes |= MODE_BELL_TRIGGERED; break;
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
                        if (i + 5 < mCsiParams.size() && i + 2 < mCsiColonSubparam.size() && mCsiColonSubparam[i + 2]) {
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
        switch (mode) {
            case 1: if (enabled) mModes |= MODE_APP_CURSOR_KEYS; else mModes &= ~MODE_APP_CURSOR_KEYS; break;
            case 7: if (enabled) mModes |= MODE_AUTOWRAP; else mModes &= ~MODE_AUTOWRAP; break;
            case 25: mCursorVisible = enabled; break;
            case 47:
            case 1047:
            case 1049: {
                bool currentlyAlt = (mModes & MODE_ALT_SCREEN) != 0;
                if (enabled && !currentlyAlt) {
                    mSavedRowPrimary = mCursorRow;
                    mSavedColPrimary = mCursorCol;
                    mModes |= MODE_ALT_SCREEN;
                    for (auto& r : mAltGrid) r.clear(DEFAULT_FG, DEFAULT_BG, 0);
                    mCursorRow = 0; mCursorCol = 0; mPendingWrap = false;
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
            case 1003: if (enabled) mModes |= MODE_MOUSE_TRACKING; else mModes &= ~MODE_MOUSE_TRACKING; break;
            case 1004: if (enabled) mModes |= MODE_FOCUS_EVENTS; else mModes &= ~MODE_FOCUS_EVENTS; break;
            case 1006:
            case 1016: if (enabled) mModes |= MODE_MOUSE_SGR; else mModes &= ~MODE_MOUSE_SGR; break;
            case 2004: if (enabled) mModes |= MODE_BRACKETED_PASTE; else mModes &= ~MODE_BRACKETED_PASTE; break;
            case 2026:
                if (enabled) {
                    mModes |= MODE_SYNC_OUTPUT;
                } else {
                    mModes &= ~MODE_SYNC_OUTPUT;
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
            if (style <= 2) mCursorShape = 0;
            else if (style <= 4) mCursorShape = 1;
            else if (style <= 6) mCursorShape = 2;
            return;
        }

        if (mCsiPrefix == '>' && finalByte == 'q') {
            reply += "\x1bP>|Termuxx-C++23(3.0)\x1b\\";
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
                    if (mode == 3) { mScrollbackHead = 0; mScrollbackCount = 0; }
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
            case 'L': if (mCursorRow >= mScrollTop && mCursorRow <= mScrollBottom) scrollDown(mCursorRow, mScrollBottom, param(0, 1)); break;
            case 'M': if (mCursorRow >= mScrollTop && mCursorRow <= mScrollBottom) scrollUp(mCursorRow, mScrollBottom, param(0, 1)); break;
            case '@': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                for (int c = mCols - 1; c >= mCursorCol + count; --c) cur.cells[static_cast<size_t>(c)] = cur.cells[static_cast<size_t>(c - count)];
                for (int c = mCursorCol; c < mCursorCol + count; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                break;
            }
            case 'P': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
                for (int c = mCursorCol; c < mCols - count; ++c) cur.cells[static_cast<size_t>(c)] = cur.cells[static_cast<size_t>(c + count)];
                for (int c = mCols - count; c < mCols; ++c) cur.cells[static_cast<size_t>(c)].reset(mCurFg, mCurBg, 0);
                break;
            }
            case 'X': {
                int count = std::min(param(0, 1), mCols - mCursorCol);
                Row& cur = grid[static_cast<size_t>(mCursorRow)];
                cur.dirty = true;
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
            case 's': mSavedRowPrimary = mCursorRow; mSavedColPrimary = mCursorCol; break;
            case 'u': mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1); mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1); mPendingWrap = false; break;
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
                            mSavedRowPrimary = mCursorRow; mSavedColPrimary = mCursorCol;
                            mSavedFg = mCurFg; mSavedBg = mCurBg; mSavedFlags = mCurFlags;
                        } else if (b == '8') {
                            mCursorRow = std::clamp(mSavedRowPrimary, 0, mRows - 1);
                            mCursorCol = std::clamp(mSavedColPrimary, 0, mCols - 1);
                            mCurFg = mSavedFg; mCurBg = mSavedBg; mCurFlags = mSavedFlags; mPendingWrap = false;
                        } else if (b == 'D') lineFeed();
                        else if (b == 'E') { mCursorCol = 0; lineFeed(); }
                        else if (b == 'M') reverseIndex();
                        else if (b == 'c') reset();
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
        if (!(mModes & MODE_SYNC_OUTPUT)) {
            mFrameDirty.store(true, std::memory_order_release);
        }
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
            int prc = poll(&pfd, 1, 50);
            if (prc < 0) {
                if (errno == EINTR) continue;
                break;
            }
            if (prc == 0) {
                if (mModes & MODE_SYNC_OUTPUT) {
                    mFrameDirty.store(true, std::memory_order_release);
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
                unlock();
                if (!reply.empty()) writeAllFd(fd, reply.data(), reply.size());
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
        mModes &= ~MODE_SYNC_OUTPUT;
        processStream(reinterpret_cast<const uint8_t*>(exitMsg), strlen(exitMsg), dummy);
        unlock();
    }

    void stopSession() noexcept {
        mRunning.store(false, std::memory_order_release);
        if (mShellPid > 0) kill(mShellPid, SIGHUP);
        if (mPtyFd >= 0) { close(mPtyFd); mPtyFd = -1; }
        if (mThreadStarted) { pthread_join(mReaderThread, nullptr); mThreadStarted = false; }
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
    envStrings.emplace_back("TERM_PROGRAM=Termuxx-C++23");
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
            rootCmd += "export LD_PRELOAD=\"" + preloadPath + "\"; ";
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

static pthread_mutex_t gGlobalMutex = PTHREAD_MUTEX_INITIALIZER;
static std::vector<NeoTerminalEngine*> gSessions;
static int gActiveIdx = -1;
static std::string gNativeLibDir;
static std::atomic<int> gBootstrapState{0};
static bool gCtrlArmed = false;
static bool gAltArmed  = false;

static NeoTerminalEngine* activeEngineLocked() noexcept {
    if (gActiveIdx >= 0 && gActiveIdx < static_cast<int>(gSessions.size())) {
        return gSessions[static_cast<size_t>(gActiveIdx)];
    }
    return nullptr;
}

static bool sendKeyCodeInternal(NeoTerminalEngine* engine, int keyCode, int metaState) noexcept {
    if (!engine) return false;
    bool appCursor = (engine->mModes & MODE_APP_CURSOR_KEYS) != 0;
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
    if (access("/data/data/com.termuxx/file/usr/bin/bash", X_OK) != 0 &&
        access("/data/data/com.termuxx/file/usr/bin/apt", X_OK) != 0 &&
        blob_size > 0) {
        extracted = extractBootstrapInCpp23(reinterpret_cast<const uint8_t*>(blob), static_cast<size_t>(blob_size));
    }
    pthread_mutex_lock(&gGlobalMutex);
    std::string libDir = gNativeLibDir;
    pthread_mutex_unlock(&gGlobalMutex);

    initializeEnvironmentAndAutoUpdater(libDir.c_str(), extracted);
    gBootstrapState.store(2, std::memory_order_release);
    return nullptr;
}

} // namespace

extern "C" {

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
            if (access("/data/data/com.termuxx/file/usr/bin/bash", X_OK) == 0) {
                initializeEnvironmentAndAutoUpdater(gNativeLibDir.c_str(), false);
                gBootstrapState.store(2, std::memory_order_release);
                pthread_mutex_unlock(&gGlobalMutex);
                return 2;
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
    auto* engine = new NeoTerminalEngine(rows, cols, 4000, asRoot == JNI_TRUE);
    if (rows > 0 && cols > 0) {
        startSessionProcess(engine, gNativeLibDir.c_str(), rows, cols, cellW, cellH);
    }
    gSessions.push_back(engine);
    gActiveIdx = static_cast<int>(gSessions.size()) - 1;
    int idx = gActiveIdx;
    pthread_mutex_unlock(&gGlobalMutex);
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
    pthread_mutex_lock(&gGlobalMutex);
    if (index >= 0 && index < static_cast<int>(gSessions.size())) {
        NeoTerminalEngine* eng = gSessions[static_cast<size_t>(index)];
        gSessions.erase(gSessions.begin() + index);
        delete eng;
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
    return rem;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeDestroyAll(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    for (auto* eng : gSessions) delete eng;
    gSessions.clear();
    gActiveIdx = -1;
    pthread_mutex_unlock(&gGlobalMutex);
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
    NeoTerminalEngine* eng = activeEngineLocked();
    if (eng) {
        startSessionProcess(eng, gNativeLibDir.c_str(), rows, cols, cellW, cellH);
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeConsumeFrameDirty(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    bool d = eng && eng->mFrameDirty.exchange(false, std::memory_order_acq_rel);
    pthread_mutex_unlock(&gGlobalMutex);
    return d ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_termuxx_app_TermuxActivity_nativeConsumeOsc52Clipboard(JNIEnv* env, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    if (!eng || !eng->mOsc52Changed) {
        pthread_mutex_unlock(&gGlobalMutex);
        return nullptr;
    }
    eng->mOsc52Changed = false;
    std::string clip = eng->mOsc52Clipboard;
    pthread_mutex_unlock(&gGlobalMutex);
    return env->NewStringUTF(clip.c_str());
}

// ============================================================================
// Original Termux Native Selection JNI Methods
// ============================================================================
JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeStartTextSelection(
    JNIEnv* /*env*/, jclass /*clazz*/, jint scrollOffset, jint screenCol, jint screenRow) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    if (eng) {
        eng->startSelectionAt(scrollOffset, screenCol, screenRow);
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeUpdateSelectionHandle(
    JNIEnv* /*env*/, jclass /*clazz*/, jboolean isStartHandle, jint col, jint logicalY) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    if (eng) {
        eng->updateSelectionHandle(isStartHandle == JNI_TRUE, col, logicalY);
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeStopTextSelection(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    bool stopped = eng && eng->stopSelection();
    pthread_mutex_unlock(&gGlobalMutex);
    return stopped ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeIsSelectingText(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    bool active = eng && eng->mSelActive;
    pthread_mutex_unlock(&gGlobalMutex);
    return active ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSelectAll(JNIEnv* /*env*/, jclass /*clazz*/, jint scrollOffset) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    if (eng) {
        eng->lock();
        int clampedOffset = (eng->mModes & MODE_ALT_SCREEN) ? 0 : std::clamp<int>(scrollOffset, 0, eng->mScrollbackCount);
        eng->mSelActive = true;
        eng->mSelY1 = -clampedOffset;
        eng->mSelX1 = 0;
        eng->mSelY2 = eng->mRows - 1 - clampedOffset;
        eng->mSelX2 = eng->mCols - 1;
        eng->markAllDirty();
        eng->unlock();
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetSelectionSelectors(JNIEnv* env, jclass /*clazz*/, jintArray outSelectors) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    jint vals[4] = {-1, -1, -1, -1};
    if (eng && eng->mSelActive) {
        vals[0] = eng->mSelY1;
        vals[1] = eng->mSelY2;
        vals[2] = eng->mSelX1;
        vals[3] = eng->mSelX2;
    }
    pthread_mutex_unlock(&gGlobalMutex);
    if (outSelectors) {
        env->SetIntArrayRegion(outSelectors, 0, 4, vals);
    }
}

JNIEXPORT jstring JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetSelectedText(JNIEnv* env, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* engine = activeEngineLocked();
    pthread_mutex_unlock(&gGlobalMutex);
    if (!engine) return env->NewStringUTF("");

    engine->lock();
    std::string out = engine->extractSelectedText();
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

    pthread_mutex_lock(&gGlobalMutex);
    if (label == "CTRL") {
        gCtrlArmed = !gCtrlArmed;
    } else if (label == "ALT") {
        gAltArmed = !gAltArmed;
    } else {
        NeoTerminalEngine* eng = activeEngineLocked();
        if (eng && !label.empty()) {
            int meta = (gCtrlArmed ? AMETA_CTRL_ON : 0) | (gAltArmed ? AMETA_ALT_ON : 0);
            if (label == "ESC") sendKeyCodeInternal(eng, AKEYCODE_ESCAPE, meta);
            else if (label == "TAB") sendKeyCodeInternal(eng, AKEYCODE_TAB, meta);
            else if (label == "ENTER" || label == "RET") sendKeyCodeInternal(eng, AKEYCODE_ENTER, meta);
            else if (label == "BKSP") sendKeyCodeInternal(eng, AKEYCODE_DEL, meta);
            else if (label == "DEL") sendKeyCodeInternal(eng, AKEYCODE_FORWARD_DEL, meta);
            else if (label == "INS") sendKeyCodeInternal(eng, AKEYCODE_INSERT, meta);
            else if (label == "▲" || label == "UP") sendKeyCodeInternal(eng, AKEYCODE_DPAD_UP, meta);
            else if (label == "▼" || label == "DOWN") sendKeyCodeInternal(eng, AKEYCODE_DPAD_DOWN, meta);
            else if (label == "◀" || label == "LEFT") sendKeyCodeInternal(eng, AKEYCODE_DPAD_LEFT, meta);
            else if (label == "▶" || label == "RIGHT") sendKeyCodeInternal(eng, AKEYCODE_DPAD_RIGHT, meta);
            else if (label == "HOME") sendKeyCodeInternal(eng, AKEYCODE_MOVE_HOME, meta);
            else if (label == "END") sendKeyCodeInternal(eng, AKEYCODE_MOVE_END, meta);
            else if (label == "PGUP") sendKeyCodeInternal(eng, AKEYCODE_PAGE_UP, meta);
            else if (label == "PGDN") sendKeyCodeInternal(eng, AKEYCODE_PAGE_DOWN, meta);
            else if (label == "F1") sendKeyCodeInternal(eng, AKEYCODE_F1, meta);
            else if (label == "F2") sendKeyCodeInternal(eng, AKEYCODE_F2, meta);
            else if (label == "F3") sendKeyCodeInternal(eng, AKEYCODE_F3, meta);
            else if (label == "F4") sendKeyCodeInternal(eng, AKEYCODE_F4, meta);
            else if (label == "F5") sendKeyCodeInternal(eng, AKEYCODE_F5, meta);
            else if (label == "F6") sendKeyCodeInternal(eng, AKEYCODE_F6, meta);
            else if (label == "F7") sendKeyCodeInternal(eng, AKEYCODE_F7, meta);
            else if (label == "F8") sendKeyCodeInternal(eng, AKEYCODE_F8, meta);
            else if (label == "F9") sendKeyCodeInternal(eng, AKEYCODE_F9, meta);
            else if (label == "F10") sendKeyCodeInternal(eng, AKEYCODE_F10, meta);
            else if (label == "F11") sendKeyCodeInternal(eng, AKEYCODE_F11, meta);
            else if (label == "F12") sendKeyCodeInternal(eng, AKEYCODE_F12, meta);
            else if (label.rfind("CTRL+", 0) == 0 && label.size() == 6) {
                sendCodepointInternal(eng, static_cast<unsigned char>(label[5]), true, gAltArmed);
            } else {
                sendCodepointInternal(eng, static_cast<unsigned char>(label[0]), gCtrlArmed, gAltArmed);
            }
        }
        gCtrlArmed = false;
        gAltArmed = false;
    }
    int mask = (gCtrlArmed ? 1 : 0) | (gAltArmed ? 2 : 0);
    pthread_mutex_unlock(&gGlobalMutex);
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
    NeoTerminalEngine* engine = activeEngineLocked();
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

    if (isPaste) {
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
        if ((engine->mModes & MODE_BRACKETED_PASTE) != 0) {
            std::string wrapped = "\x1b[200~" + sanitized + "\x1b[201~";
            engine->writeToPty(wrapped.data(), wrapped.size());
        } else {
            engine->writeToPty(sanitized.data(), sanitized.size());
        }
    } else if ((ctrl || alt) && !payload.empty()) {
        sendCodepointInternal(engine, static_cast<unsigned char>(payload[0]), ctrl, alt);
        if (payload.size() > 1) {
            engine->writeToPty(payload.data() + 1, payload.size() - 1);
        }
    } else {
        engine->writeToPty(payload.data(), payload.size());
    }
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSendKeyEvent(
    JNIEnv* /*env*/, jclass /*clazz*/, jint keyCode, jint unicodeChar, jint metaState) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* engine = activeEngineLocked();
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

    if (sendKeyCodeInternal(engine, keyCode, meta)) return 1;
    if (unicodeChar > 0) {
        sendCodepointInternal(engine, unicodeChar, (meta & AMETA_CTRL_ON) != 0, (meta & AMETA_ALT_ON) != 0);
        return 1;
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeSendMouse(
    JNIEnv* /*env*/, jclass /*clazz*/, jint button, jint col, jint row, jboolean pressed) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* engine = activeEngineLocked();
    pthread_mutex_unlock(&gGlobalMutex);
    if (!engine) return;
    if ((engine->mModes & MODE_MOUSE_TRACKING) == 0) return;
    if ((engine->mModes & MODE_MOUSE_SGR) != 0) {
        int c = std::clamp<int>(col, 1, engine->mCols);
        int r = std::clamp<int>(row, 1, engine->mRows);
        char buf[48];
        snprintf(buf, sizeof(buf), "\x1b[<%d;%d;%d%c", button, c, r, pressed ? 'M' : 'm');
        engine->writeToPty(buf, strlen(buf));
    }
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetActiveScrollback(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    int sb = (eng && !(eng->mModes & MODE_ALT_SCREEN)) ? eng->mScrollbackCount : 0;
    pthread_mutex_unlock(&gGlobalMutex);
    return sb;
}

JNIEXPORT jboolean JNICALL
Java_com_termuxx_app_TermuxActivity_nativeIsRowDirty(JNIEnv* /*env*/, jclass /*clazz*/, jint screenRow) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    bool dirty = false;
    if (eng && screenRow >= 0 && screenRow < eng->mRows) {
        dirty = eng->activeGrid()[static_cast<size_t>(screenRow)].dirty;
    }
    pthread_mutex_unlock(&gGlobalMutex);
    return dirty ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_termuxx_app_TermuxActivity_nativeClearDirtyFlags(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    if (eng) {
        eng->lock();
        for (auto& r : eng->activeGrid()) r.dirty = false;
        eng->unlock();
    }
    pthread_mutex_unlock(&gGlobalMutex);
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativePackRowRender(
    JNIEnv* env,
    jclass /*clazz*/,
    jint scrollOffset,
    jint screenRow,
    jintArray outBgRuns,
    jintArray outGlyphs) {

    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* engine = activeEngineLocked();
    pthread_mutex_unlock(&gGlobalMutex);
    if (!engine) return 0;

    engine->lock();
    int clampedOffset = (engine->mModes & MODE_ALT_SCREEN) ? 0 : std::clamp<int>(scrollOffset, 0, engine->mScrollbackCount);
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
        while (runStart < cols) {
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

        for (int c = 0; c < cols; ++c) {
            const Cell& cell = row.cells[static_cast<size_t>(c)];
            if (cell.codepoint <= ' ' || (cell.flags & (FLAG_INVISIBLE | FLAG_WIDE_TRAIL)) != 0) {
                continue;
            }
            uint32_t fg = engine->isLogicalCellSelected(logicalY, c)
                ? SELECTION_FG
                : resolveFgColor(cell.fg, cell.bg, cell.flags);
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

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetCursorState(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    int state = -1;
    if (eng && eng->mCursorVisible) {
        state = ((eng->mCursorShape & 3) << 29) |
                ((eng->mCursorRow & 0x1FFF) << 16) |
                (eng->mCursorCol & 0xFFFF);
    }
    pthread_mutex_unlock(&gGlobalMutex);
    return state;
}

JNIEXPORT jint JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetModesMask(JNIEnv* /*env*/, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* eng = activeEngineLocked();
    int m = eng ? eng->mModes : 0;
    pthread_mutex_unlock(&gGlobalMutex);
    return m;
}

JNIEXPORT jstring JNICALL
Java_com_termuxx_app_TermuxActivity_nativeGetTranscriptText(JNIEnv* env, jclass /*clazz*/) {
    pthread_mutex_lock(&gGlobalMutex);
    NeoTerminalEngine* engine = activeEngineLocked();
    pthread_mutex_unlock(&gGlobalMutex);
    if (!engine) return env->NewStringUTF("");

    std::string out;
    out.reserve(16384);

    engine->lock();
    int sbCount = (engine->mModes & MODE_ALT_SCREEN) ? 0 : engine->mScrollbackCount;
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
        if (!row.wrapped && i < engine->mRows - 1) out.push_back('\n');
    }
    engine->unlock();
    return env->NewStringUTF(out.c_str());
}

} // extern "C"
