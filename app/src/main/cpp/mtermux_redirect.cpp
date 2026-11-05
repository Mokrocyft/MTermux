// Termux VFS & Exec Redirector

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <ctype.h>
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <signal.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <sys/statvfs.h>
#include <sys/syscall.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#include <algorithm>
#include <string>
#include <vector>

extern char** environ;

namespace {

constexpr const char OLD_FILES_16[] = "com.termux/files";
constexpr const char NEW_FILES_16[] = "com.mtermux/file";
constexpr const char OLD_CACHE_16[] = "com.termux/cache";
constexpr const char NEW_CACHE_16[] = "com.mtermux/cach";
constexpr size_t TOKEN_16_LEN = 16;

constexpr const char REDIRECT_SO_PATH[] = "/data/data/com.mtermux/file/usr/lib/libmtermux-redirect.so";
constexpr const char USR_LIB_PATH[]     = "/data/data/com.mtermux/file/usr/lib";
constexpr const char USR_BIN_PATH[]     = "/data/data/com.mtermux/file/usr/bin";
constexpr const char USR_TMP_PATH[]     = "/data/data/com.mtermux/file/usr/tmp";
constexpr const char USR_SH_PATH[]      = "/data/data/com.mtermux/file/usr/bin/sh";

static int rawAccessAt(int dirfd, const char* path, int mode) noexcept {
    return static_cast<int>(syscall(__NR_faccessat, dirfd, path, mode, 0));
}

struct PathBuf {
    char buf[4096];
    const char* ptr;

    explicit PathBuf(const char* in) noexcept : ptr(in) {
        if (!in || !in[0]) return;

        if ((strcmp(in, "/bin/sh") == 0 || strcmp(in, "/usr/bin/sh") == 0) &&
            rawAccessAt(AT_FDCWD, in, F_OK) != 0) {
            strcpy(buf, USR_SH_PATH);
            ptr = buf;
            return;
        }
        if (strcmp(in, "/system/bin/sh") == 0 && rawAccessAt(AT_FDCWD, USR_SH_PATH, R_OK) == 0) {
            strcpy(buf, USR_SH_PATH);
            ptr = buf;
            return;
        }
        if ((strcmp(in, "/usr/bin/env") == 0 || strcmp(in, "/bin/env") == 0) &&
            rawAccessAt(AT_FDCWD, in, F_OK) != 0) {
            strcpy(buf, "/data/data/com.mtermux/file/usr/bin/env");
            ptr = buf;
            return;
        }
        if (strncmp(in, "/bin/", 5) == 0 && in[5] != '\0' && rawAccessAt(AT_FDCWD, in, F_OK) != 0) {
            snprintf(buf, sizeof(buf), "%s/%s", USR_BIN_PATH, in + 5);
            ptr = buf;
            return;
        }
        if (strncmp(in, "/usr/bin/", 9) == 0 && in[9] != '\0' && rawAccessAt(AT_FDCWD, in, F_OK) != 0) {
            snprintf(buf, sizeof(buf), "%s/%s", USR_BIN_PATH, in + 9);
            ptr = buf;
            return;
        }
        if ((strcmp(in, "/tmp") == 0 || strcmp(in, "/tmp/") == 0) && rawAccessAt(AT_FDCWD, "/tmp", W_OK) != 0) {
            strcpy(buf, USR_TMP_PATH);
            ptr = buf;
            return;
        }
        if (strncmp(in, "/tmp/", 5) == 0 && rawAccessAt(AT_FDCWD, "/tmp", W_OK) != 0) {
            snprintf(buf, sizeof(buf), "%s/%s", USR_TMP_PATH, in + 5);
            ptr = buf;
            return;
        }

        const char* hit = strstr(in, "com.termux");
        if (!hit) return;

        size_t inLen = strlen(in);
        if (inLen + 64 >= sizeof(buf)) return;

        size_t outIdx = 0;
        for (size_t i = 0; i < inLen && outIdx + 24 < sizeof(buf);) {
            if (i + TOKEN_16_LEN <= inLen && memcmp(in + i, OLD_FILES_16, TOKEN_16_LEN) == 0) {
                memcpy(buf + outIdx, NEW_FILES_16, TOKEN_16_LEN);
                outIdx += TOKEN_16_LEN;
                i += TOKEN_16_LEN;
            } else if (i + TOKEN_16_LEN <= inLen && memcmp(in + i, OLD_CACHE_16, TOKEN_16_LEN) == 0) {
                memcpy(buf + outIdx, NEW_CACHE_16, TOKEN_16_LEN);
                outIdx += TOKEN_16_LEN;
                i += TOKEN_16_LEN;
            } else if (i + 10 <= inLen && memcmp(in + i, "com.termux", 10) == 0 &&
                       (in[i + 10] == '\0' || in[i + 10] == '/')) {
                // Rewrites "./data/data/com.termux" or "/data/data/com.termux/" -> ".../com.mtermux..."
                memcpy(buf + outIdx, "com.mtermux", 11);
                outIdx += 11;
                i += 10;
            } else {
                buf[outIdx++] = in[i++];
            }
        }
        buf[outIdx] = '\0';
        ptr = buf;
    }
};

// Returns true if path refers to a container prefix directory (/, /data, /data/data, /data/data/com.mtermux, etc.)
// that dpkg inspects or touches when unpacking data.tar.xz
static bool isContainerParentDir(const char* p) noexcept {
    if (!p) return false;
    while (p[0] == '.' && p[1] == '/') p += 2;
    while (p[0] == '/') p++;
    if (p[0] == '\0' || strcmp(p, ".") == 0) return true;

    char norm[256];
    size_t len = strlen(p);
    if (len >= sizeof(norm)) return false;
    memcpy(norm, p, len + 1);
    while (len > 0 && norm[len - 1] == '/') {
        norm[--len] = '\0';
    }

    return strcmp(norm, "data") == 0 ||
           strcmp(norm, "data/data") == 0 ||
           strcmp(norm, "data/user") == 0 ||
           strcmp(norm, "data/user/0") == 0 ||
           strcmp(norm, "data/data/com.mtermux") == 0 ||
           strcmp(norm, "data/data/com.mtermux/file") == 0 ||
           strcmp(norm, "data/data/com.mtermux/files") == 0 ||
           strcmp(norm, "data/data/com.mtermux/cach") == 0 ||
           strcmp(norm, "data/data/com.mtermux/cache") == 0 ||
           strcmp(norm, "data/data/com.mtermux/file/usr") == 0 ||
           strcmp(norm, "data/data/com.mtermux/files/usr") == 0 ||
           strcmp(norm, "data/user/0/com.mtermux") == 0 ||
           strcmp(norm, "data/user/0/com.mtermux/file") == 0 ||
           strcmp(norm, "data/user/0/com.mtermux/files") == 0 ||
           strcmp(norm, "data/user/0/com.mtermux/file/usr") == 0 ||
           strcmp(norm, "data/user/0/com.mtermux/files/usr") == 0;
}

static void fillSyntheticDirStat(struct stat* st) noexcept {
    if (!st) return;
    memset(st, 0, sizeof(*st));
    st->st_mode = S_IFDIR | 0700;
    st->st_nlink = 2;
    st->st_uid = getuid();
    st->st_gid = getgid();
    st->st_blksize = 4096;
}

static bool replaceAll16(uint8_t* data, size_t size, const char* oldTok, const char* newTok) noexcept {
    if (!data || size < TOKEN_16_LEN) return false;
    bool changed = false;
    uint8_t* cur = data;
    size_t rem = size;
    while (rem >= TOKEN_16_LEN) {
        auto* hit = static_cast<uint8_t*>(memmem(cur, rem, oldTok, TOKEN_16_LEN));
        if (!hit) break;
        memcpy(hit, newTok, TOKEN_16_LEN);
        changed = true;
        size_t adv = static_cast<size_t>(hit - cur) + TOKEN_16_LEN;
        cur += adv;
        rem -= adv;
    }
    return changed;
}

static int rawOpenAt(int dirfd, const char* path, int flags, mode_t mode) noexcept {
    return static_cast<int>(syscall(__NR_openat, dirfd, path, flags, mode));
}

static int rawExecveSyscall(const char* path, char* const argv[], char* const envp[]) noexcept {
    return static_cast<int>(syscall(__NR_execve, path, argv, envp));
}

static bool readAllFd(int fd, uint8_t* buf, size_t len) noexcept {
    size_t off = 0;
    while (off < len) {
        ssize_t n = read(fd, buf + off, len - off);
        if (n < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        if (n == 0) return false;
        off += static_cast<size_t>(n);
    }
    return true;
}

static bool writeAllFd(int fd, const uint8_t* buf, size_t len) noexcept {
    size_t off = 0;
    while (off < len) {
        ssize_t n = write(fd, buf + off, len - off);
        if (n < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        off += static_cast<size_t>(n);
    }
    return true;
}

static void patchInstalledFileIfNeededAt(int dirfd, const char* path) noexcept {
    if (!path) return;
    if (!strstr(path, "com.mtermux/file") && !strstr(path, "com.mtermux/files") &&
        !strstr(path, "/var/lib/dpkg/")) {
        return;
    }
    if (strstr(path, ".dpkg-")) return;

    struct stat st{};
    if (syscall(__NR_newfstatat, dirfd, path, &st, AT_SYMLINK_NOFOLLOW) != 0 ||
        !S_ISREG(st.st_mode) || st.st_size < static_cast<off_t>(TOKEN_16_LEN) ||
        st.st_size > 64 * 1024 * 1024) {
        return;
    }

    mode_t origPerms = st.st_mode & 07777;
    if ((origPerms & S_IRUSR) == 0) {
        syscall(__NR_fchmodat, dirfd, path, origPerms | S_IRUSR, 0);
    }

    int rfd = rawOpenAt(dirfd, path, O_RDONLY | O_CLOEXEC, 0);
    if (rfd < 0) return;

    size_t size = static_cast<size_t>(st.st_size);
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (!data) {
        close(rfd);
        return;
    }

    bool okRead = readAllFd(rfd, data, size);
    close(rfd);

    if (okRead) {
        bool c1 = replaceAll16(data, size, OLD_FILES_16, NEW_FILES_16);
        bool c2 = replaceAll16(data, size, OLD_CACHE_16, NEW_CACHE_16);
        if (c1 || c2) {
            syscall(__NR_fchmodat, dirfd, path, origPerms | S_IRUSR | S_IWUSR, 0);
            int wfd = rawOpenAt(dirfd, path, O_WRONLY | O_CLOEXEC, 0);
            if (wfd >= 0) {
                (void)writeAllFd(wfd, data, size);
                close(wfd);
            }
            bool isElf = (size >= 4 && data[0] == 0x7F && data[1] == 'E' && data[2] == 'L' && data[3] == 'F');
            mode_t restored = origPerms | S_IRUSR | S_IWUSR;
            if (isElf || (origPerms & 0111) != 0 || strstr(path, "/bin/") || strstr(path, "/libexec/")) {
                restored |= S_IXUSR;
            }
            syscall(__NR_fchmodat, dirfd, path, restored, 0);
        }
    }
    free(data);
}

static void patchInstalledFileIfNeeded(const char* path) noexcept {
    patchInstalledFileIfNeededAt(AT_FDCWD, path);
}

static bool isProotOrChrootExec(const char* targetPath) noexcept {
    if (!targetPath) return false;
    if (strstr(targetPath, "/installed-rootfs/") || strstr(targetPath, "/chroot/")) return true;
    size_t len = strlen(targetPath);
    if (len >= 6 && strcmp(targetPath + len - 6, "/proot") == 0) return true;
    if (len >= 7 && strcmp(targetPath + len - 7, "/chroot") == 0) return true;
    return false;
}

static std::vector<std::string> buildSanitizedEnvStrings(char* const envp[], const char* targetPath = nullptr) {
    std::vector<std::string> out;
    char* const* src = envp ? envp : environ;
    bool hasPreload = false;
    bool hasLibPath = false;
    bool hasPrefix  = false;
    bool hasTmpdir  = false;
    bool hasPath    = false;

    if (src) {
        for (size_t i = 0; src[i] != nullptr; ++i) {
            std::string entry(src[i]);
            size_t pos = 0;
            while ((pos = entry.find(OLD_FILES_16, pos)) != std::string::npos) {
                entry.replace(pos, TOKEN_16_LEN, NEW_FILES_16);
                pos += TOKEN_16_LEN;
            }
            pos = 0;
            while ((pos = entry.find(OLD_CACHE_16, pos)) != std::string::npos) {
                entry.replace(pos, TOKEN_16_LEN, NEW_CACHE_16);
                pos += TOKEN_16_LEN;
            }

            if (entry.rfind("LD_PRELOAD=", 0) == 0) {
                hasPreload = true;
                std::string val = entry.substr(11);
                if (val.find("libmtermux-redirect.so") == std::string::npos &&
                    val.find("libtermux-exec") == std::string::npos) {
                    if (val.empty()) {
                        entry = std::string("LD_PRELOAD=") + REDIRECT_SO_PATH;
                    } else {
                        entry = std::string("LD_PRELOAD=") + REDIRECT_SO_PATH + ":" + val;
                    }
                } else {
                    entry = std::string("LD_PRELOAD=") + REDIRECT_SO_PATH;
                }
            } else if (entry.rfind("LD_LIBRARY_PATH=", 0) == 0) {
                hasLibPath = true;
                if (entry.find(USR_LIB_PATH) == std::string::npos) {
                    std::string rest = entry.substr(16);
                    entry = std::string("LD_LIBRARY_PATH=") + USR_LIB_PATH + (rest.empty() ? "" : (":" + rest));
                }
            } else if (entry.rfind("PREFIX=", 0) == 0) {
                hasPrefix = true;
                entry = "PREFIX=/data/data/com.mtermux/file/usr";
            } else if (entry.rfind("TMPDIR=", 0) == 0) {
                hasTmpdir = true;
            } else if (entry.rfind("PATH=", 0) == 0) {
                hasPath = true;
                if (entry.find(USR_BIN_PATH) == std::string::npos) {
                    entry = std::string("PATH=") + USR_BIN_PATH + ":" + USR_BIN_PATH + "/applets:" + entry.substr(5);
                }
            }
            out.push_back(std::move(entry));
        }
    }

    bool skipBionicPreload = isProotOrChrootExec(targetPath);
    if (skipBionicPreload) {
        out.erase(
            std::remove_if(out.begin(), out.end(), [](const std::string& s) {
                return s.rfind("LD_PRELOAD=", 0) == 0 && s.find("libmtermux-redirect.so") != std::string::npos;
            }),
            out.end()
        );
        out.emplace_back(std::string("PROOT_TMP_DIR=") + USR_TMP_PATH);
    } else if (!hasPreload) {
        out.emplace_back(std::string("LD_PRELOAD=") + REDIRECT_SO_PATH);
    }
    if (!hasLibPath) {
        out.emplace_back(std::string("LD_LIBRARY_PATH=") + USR_LIB_PATH);
    }
    if (!hasPrefix) {
        out.emplace_back("PREFIX=/data/data/com.mtermux/file/usr");
    }
    if (!hasTmpdir) {
        out.emplace_back(std::string("TMPDIR=") + USR_TMP_PATH);
    }
    if (!hasPath) {
        out.emplace_back(std::string("PATH=") + USR_BIN_PATH + ":" + USR_BIN_PATH + "/applets:/system/bin:/system/xbin");
    }
    return out;
}

static int executeResolvedPath(const char* rawFilename, char* const argv[], char* const envp[], int depth = 0) noexcept {
    if (!rawFilename || !rawFilename[0] || depth > 4) {
        errno = ENOENT;
        return -1;
    }

    PathBuf pb(rawFilename);
    std::string targetPathStr(pb.ptr);
    if (targetPathStr[0] != '/') {
        char cwd[1024];
        if (getcwd(cwd, sizeof(cwd)) != nullptr) {
            if (targetPathStr.rfind("./", 0) == 0) {
                targetPathStr = std::string(cwd) + "/" + targetPathStr.substr(2);
            } else {
                targetPathStr = std::string(cwd) + "/" + targetPathStr;
            }
        }
    }
    const char* targetPath = targetPathStr.c_str();

    // Ensure any newly created script or binary under com.mtermux has its 16-byte tokens patched before exec
    patchInstalledFileIfNeeded(targetPath);

    std::vector<std::string> envStore = buildSanitizedEnvStrings(envp, targetPath);
    std::vector<char*> envPtrs;
    envPtrs.reserve(envStore.size() + 1);
    for (auto& s : envStore) {
        envPtrs.push_back(const_cast<char*>(s.c_str()));
    }
    envPtrs.push_back(nullptr);

    char* fallbackArgv[2] = { const_cast<char*>(targetPath), nullptr };
    char* const* effectiveArgv = (argv && argv[0]) ? argv : fallbackArgv;

    int fd = rawOpenAt(AT_FDCWD, targetPath, O_RDONLY | O_CLOEXEC, 0);
    if (fd >= 0) {
        char hdr[512] = {0};
        ssize_t n = read(fd, hdr, sizeof(hdr) - 1);
        close(fd);

        // Case 1: #! Shebang script
        if (n >= 2 && hdr[0] == '#' && hdr[1] == '!') {
            char* nl = strchr(hdr + 2, '\n');
            if (nl) *nl = '\0';
            char* cr = strchr(hdr + 2, '\r');
            if (cr) *cr = '\0';

            char* p = hdr + 2;
            while (*p && isspace(static_cast<unsigned char>(*p))) ++p;
            if (*p) {
                char* interp = p;
                while (*p && !isspace(static_cast<unsigned char>(*p))) ++p;
                char* optArg = nullptr;
                if (*p) {
                    *p++ = '\0';
                    while (*p && isspace(static_cast<unsigned char>(*p))) ++p;
                    if (*p) {
                        optArg = p;
                        char* trail = optArg + strlen(optArg) - 1;
                        while (trail >= optArg && isspace(static_cast<unsigned char>(*trail))) {
                            *trail-- = '\0';
                        }
                    }
                }

                PathBuf interpPb(interp);
                std::string resolvedInterp(interpPb.ptr);
                std::string optArgStr = (optArg && *optArg) ? std::string(optArg) : std::string();

                std::vector<char*> scriptArgs;
                scriptArgs.push_back(const_cast<char*>(resolvedInterp.c_str()));
                if (!optArgStr.empty()) {
                    scriptArgs.push_back(const_cast<char*>(optArgStr.c_str()));
                }
                scriptArgs.push_back(const_cast<char*>(targetPath));
                if (effectiveArgv && effectiveArgv[0] != nullptr) {
                    for (size_t i = 1; effectiveArgv[i] != nullptr; ++i) {
                        scriptArgs.push_back(effectiveArgv[i]);
                    }
                }
                scriptArgs.push_back(nullptr);

                return executeResolvedPath(resolvedInterp.c_str(), scriptArgs.data(), envPtrs.data(), depth + 1);
            }
        }
    }

    int rc = rawExecveSyscall(targetPath, effectiveArgv, envPtrs.data());
    if (rc < 0 && errno == EACCES) {
        syscall(__NR_fchmodat, AT_FDCWD, targetPath, 0700, 0);
        rc = rawExecveSyscall(targetPath, effectiveArgv, envPtrs.data());
        if (rc < 0 && errno == EACCES) {
            std::vector<char*> linkerArgs;
            linkerArgs.push_back(const_cast<char*>("/system/bin/linker64"));
            linkerArgs.push_back(const_cast<char*>(targetPath));
            if (effectiveArgv && effectiveArgv[0] != nullptr) {
                for (size_t i = 1; effectiveArgv[i] != nullptr; ++i) {
                    linkerArgs.push_back(effectiveArgv[i]);
                }
            }
            linkerArgs.push_back(nullptr);
            return rawExecveSyscall("/system/bin/linker64", linkerArgs.data(), envPtrs.data());
        }
    }
    return rc;
}

static int searchPathAndExecute(const char* file, char* const argv[], char* const envp[]) noexcept {
    if (!file || !*file) {
        errno = ENOENT;
        return -1;
    }
    if (strchr(file, '/') != nullptr) {
        return executeResolvedPath(file, argv, envp);
    }

    const char* pathEnv = nullptr;
    char* const* srcEnv = envp ? envp : environ;
    if (srcEnv) {
        for (size_t i = 0; srcEnv[i] != nullptr; ++i) {
            if (strncmp(srcEnv[i], "PATH=", 5) == 0) {
                pathEnv = srcEnv[i] + 5;
                break;
            }
        }
    }
    std::string pathStr = pathEnv ? pathEnv : (std::string(USR_BIN_PATH) + ":/system/bin");

    size_t start = 0;
    while (start <= pathStr.size()) {
        size_t colon = pathStr.find(':', start);
        if (colon == std::string::npos) colon = pathStr.size();
        std::string dir = pathStr.substr(start, colon - start);
        const char* dirPtr = dir.empty() ? "." : dir.c_str();
        std::string cand = std::string(dirPtr) + "/" + file;
        PathBuf pbCand(cand.c_str());
        if (rawAccessAt(AT_FDCWD, pbCand.ptr, R_OK) == 0) {
            int rc = executeResolvedPath(pbCand.ptr, argv, envp);
            if (rc == 0 || (errno != ENOENT && errno != ENOTDIR)) {
                return rc;
            }
        }
        start = colon + 1;
    }
    errno = ENOENT;
    return -1;
}

} // namespace

extern "C" {

#define EXPORT_SYM __attribute__((visibility("default")))

// ============================================================================
// Exec & System Family Hooks
// ============================================================================
EXPORT_SYM int execve(const char* filename, char* const argv[], char* const envp[]) {
    return executeResolvedPath(filename, argv, envp);
}

EXPORT_SYM int execv(const char* filename, char* const argv[]) {
    return executeResolvedPath(filename, argv, environ);
}

EXPORT_SYM int execvpe(const char* file, char* const argv[], char* const envp[]) {
    return searchPathAndExecute(file, argv, envp);
}

EXPORT_SYM int execvp(const char* file, char* const argv[]) {
    return searchPathAndExecute(file, argv, environ);
}

EXPORT_SYM int execl(const char* path, const char* arg0, ...) {
    std::vector<char*> args;
    args.push_back(const_cast<char*>(arg0));
    va_list ap;
    va_start(ap, arg0);
    for (;;) {
        char* next = va_arg(ap, char*);
        args.push_back(next);
        if (!next) break;
    }
    va_end(ap);
    return executeResolvedPath(path, args.data(), environ);
}

EXPORT_SYM int execlp(const char* file, const char* arg0, ...) {
    std::vector<char*> args;
    args.push_back(const_cast<char*>(arg0));
    va_list ap;
    va_start(ap, arg0);
    for (;;) {
        char* next = va_arg(ap, char*);
        args.push_back(next);
        if (!next) break;
    }
    va_end(ap);
    return searchPathAndExecute(file, args.data(), environ);
}

EXPORT_SYM int execle(const char* path, const char* arg0, ...) {
    std::vector<char*> args;
    args.push_back(const_cast<char*>(arg0));
    va_list ap;
    va_start(ap, arg0);
    for (;;) {
        char* next = va_arg(ap, char*);
        args.push_back(next);
        if (!next) break;
    }
    char* const* envp = va_arg(ap, char* const*);
    va_end(ap);
    return executeResolvedPath(path, args.data(), envp);
}

EXPORT_SYM int system(const char* command) {
    if (!command) return 1;
    pid_t pid = fork();
    if (pid < 0) return -1;
    if (pid == 0) {
        const char* shPath = (rawAccessAt(AT_FDCWD, USR_SH_PATH, R_OK) == 0) ? USR_SH_PATH : "/system/bin/sh";
        char* argv[] = {
            const_cast<char*>("sh"),
            const_cast<char*>("-c"),
            const_cast<char*>(command),
            nullptr
        };
        executeResolvedPath(shPath, argv, environ);
        _exit(127);
    }
    int status = 0;
    while (waitpid(pid, &status, 0) < 0) {
        if (errno != EINTR) return -1;
    }
    return status;
}

// ============================================================================
// File & Directory Hooks (with direct syscall fallbacks & container protection)
// ============================================================================
EXPORT_SYM int open(const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode = static_cast<mode_t>(va_arg(ap, int));
        va_end(ap);
    }
    PathBuf pb(pathname);
    return rawOpenAt(AT_FDCWD, pb.ptr, flags, mode);
}

EXPORT_SYM int open64(const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode = static_cast<mode_t>(va_arg(ap, int));
        va_end(ap);
    }
    PathBuf pb(pathname);
    return rawOpenAt(AT_FDCWD, pb.ptr, flags, mode);
}

EXPORT_SYM int openat(int dirfd, const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode = static_cast<mode_t>(va_arg(ap, int));
        va_end(ap);
    }
    PathBuf pb(pathname);
    return rawOpenAt(dirfd, pb.ptr, flags, mode);
}

EXPORT_SYM int openat64(int dirfd, const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode = static_cast<mode_t>(va_arg(ap, int));
        va_end(ap);
    }
    PathBuf pb(pathname);
    return rawOpenAt(dirfd, pb.ptr, flags, mode);
}

EXPORT_SYM FILE* fopen(const char* pathname, const char* mode) {
    using fn_t = FILE* (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen"));
    PathBuf pb(pathname);
    if (!real_fn) return nullptr;
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM FILE* fopen64(const char* pathname, const char* mode) {
    using fn_t = FILE* (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen"));
    PathBuf pb(pathname);
    if (!real_fn) return nullptr;
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM DIR* opendir(const char* name) {
    using fn_t = DIR* (*)(const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "opendir"));
    PathBuf pb(name);
    if (!real_fn) return nullptr;
    return real_fn(pb.ptr);
}

EXPORT_SYM int stat(const char* pathname, struct stat* statbuf) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_newfstatat, AT_FDCWD, pb.ptr, statbuf, 0));
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        fillSyntheticDirStat(statbuf);
        return 0;
    }
    return rc;
}

EXPORT_SYM int stat64(const char* pathname, struct stat64* statbuf) {
    return stat(pathname, reinterpret_cast<struct stat*>(statbuf));
}

EXPORT_SYM int lstat(const char* pathname, struct stat* statbuf) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_newfstatat, AT_FDCWD, pb.ptr, statbuf, AT_SYMLINK_NOFOLLOW));
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        fillSyntheticDirStat(statbuf);
        return 0;
    }
    return rc;
}

EXPORT_SYM int lstat64(const char* pathname, struct stat64* statbuf) {
    return lstat(pathname, reinterpret_cast<struct stat*>(statbuf));
}

EXPORT_SYM int fstatat(int dirfd, const char* pathname, struct stat* statbuf, int flags) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_newfstatat, dirfd, pb.ptr, statbuf, flags));
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        fillSyntheticDirStat(statbuf);
        return 0;
    }
    return rc;
}

EXPORT_SYM int fstatat64(int dirfd, const char* pathname, struct stat64* statbuf, int flags) {
    return fstatat(dirfd, pathname, reinterpret_cast<struct stat*>(statbuf), flags);
}

EXPORT_SYM int statvfs(const char* path, struct statvfs* buf) {
    using fn_t = int (*)(const char*, struct statvfs*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statvfs"));
    PathBuf pb(path);
    if (!real_fn) { errno = ENOSYS; return -1; }
    return real_fn(pb.ptr, buf);
}

EXPORT_SYM int statvfs64(const char* path, struct statvfs64* buf) {
    using fn_t = int (*)(const char*, struct statvfs64*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statvfs64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statvfs"));
    PathBuf pb(path);
    if (!real_fn) { errno = ENOSYS; return -1; }
    return real_fn(pb.ptr, buf);
}

EXPORT_SYM int statfs(const char* path, struct statfs* buf) {
    PathBuf pb(path);
    return static_cast<int>(syscall(__NR_statfs, pb.ptr, buf));
}

EXPORT_SYM int statfs64(const char* path, struct statfs64* buf) {
    PathBuf pb(path);
    return static_cast<int>(syscall(__NR_statfs, pb.ptr, buf));
}

EXPORT_SYM int access(const char* pathname, int mode) {
    PathBuf pb(pathname);
    int rc = rawAccessAt(AT_FDCWD, pb.ptr, mode);
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int faccessat(int dirfd, const char* pathname, int mode, int flags) {
    using fn_t = int (*)(int, const char*, int, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "faccessat"));
    PathBuf pb(pathname);
    int rc = real_fn ? real_fn(dirfd, pb.ptr, mode, flags) : rawAccessAt(dirfd, pb.ptr, mode);
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int mkdir(const char* pathname, mode_t mode) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_mkdirat, AT_FDCWD, pb.ptr, mode));
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        errno = EEXIST;
        return -1;
    }
    return rc;
}

EXPORT_SYM int mkdirat(int dirfd, const char* pathname, mode_t mode) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_mkdirat, dirfd, pb.ptr, mode));
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        errno = EEXIST;
        return -1;
    }
    return rc;
}

static bool isDpkgProcess() noexcept {
    static int cached = -1;
    if (cached != -1) return cached == 1;
    char comm[64] = {0};
    int fd = rawOpenAt(AT_FDCWD, "/proc/self/comm", O_RDONLY | O_CLOEXEC, 0);
    if (fd >= 0) {
        ssize_t n = read(fd, comm, sizeof(comm) - 1);
        close(fd);
        if (n > 0) {
            comm[n] = 0;
            if (strncmp(comm, "dpkg", 4) == 0) {
                cached = 1;
                return true;
            }
        }
    }
    cached = 0;
    return false;
}

EXPORT_SYM int rmdir(const char* pathname) {
    PathBuf pb(pathname);
    if (isContainerParentDir(pb.ptr)) return 0;
    int rc = static_cast<int>(syscall(__NR_unlinkat, AT_FDCWD, pb.ptr, AT_REMOVEDIR));
    if (rc < 0 && (errno == ENOTEMPTY || errno == EEXIST) && isDpkgProcess()) {
        // Prevent bogus dpkg warning when old .list and new .deb share a relocated directory
        errno = ENOENT;
    }
    return rc;
}

EXPORT_SYM int unlink(const char* pathname) {
    PathBuf pb(pathname);
    return static_cast<int>(syscall(__NR_unlinkat, AT_FDCWD, pb.ptr, 0));
}

EXPORT_SYM int unlinkat(int dirfd, const char* pathname, int flags) {
    PathBuf pb(pathname);
    if ((flags & AT_REMOVEDIR) && isContainerParentDir(pb.ptr)) return 0;
    int rc = static_cast<int>(syscall(__NR_unlinkat, dirfd, pb.ptr, flags));
    if (rc < 0 && (flags & AT_REMOVEDIR) && (errno == ENOTEMPTY || errno == EEXIST) && isDpkgProcess()) {
        errno = ENOENT;
    }
    return rc;
}

EXPORT_SYM int rename(const char* oldpath, const char* newpath) {
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = static_cast<int>(syscall(__NR_renameat2, AT_FDCWD, pbOld.ptr, AT_FDCWD, pbNew.ptr, 0));
    if (rc == 0) patchInstalledFileIfNeededAt(AT_FDCWD, pbNew.ptr);
    return rc;
}

EXPORT_SYM int renameat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath) {
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = static_cast<int>(syscall(__NR_renameat2, olddirfd, pbOld.ptr, newdirfd, pbNew.ptr, 0));
    if (rc == 0) patchInstalledFileIfNeededAt(newdirfd, pbNew.ptr);
    return rc;
}

EXPORT_SYM int renameat2(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, unsigned int flags) {
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = static_cast<int>(syscall(__NR_renameat2, olddirfd, pbOld.ptr, newdirfd, pbNew.ptr, flags));
    if (rc == 0) patchInstalledFileIfNeededAt(newdirfd, pbNew.ptr);
    return rc;
}

EXPORT_SYM int link(const char* oldpath, const char* newpath) {
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    return static_cast<int>(syscall(__NR_linkat, AT_FDCWD, pbOld.ptr, AT_FDCWD, pbNew.ptr, 0));
}

EXPORT_SYM int linkat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, int flags) {
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    return static_cast<int>(syscall(__NR_linkat, olddirfd, pbOld.ptr, newdirfd, pbNew.ptr, flags));
}

EXPORT_SYM int symlink(const char* target, const char* linkpath) {
    PathBuf pbTarget(target);
    PathBuf pbLink(linkpath);
    return static_cast<int>(syscall(__NR_symlinkat, pbTarget.ptr, AT_FDCWD, pbLink.ptr));
}

EXPORT_SYM int symlinkat(const char* target, int newdirfd, const char* linkpath) {
    PathBuf pbTarget(target);
    PathBuf pbLink(linkpath);
    return static_cast<int>(syscall(__NR_symlinkat, pbTarget.ptr, newdirfd, pbLink.ptr));
}

EXPORT_SYM ssize_t readlink(const char* pathname, char* buf, size_t bufsiz) {
    PathBuf pb(pathname);
    ssize_t rc = syscall(__NR_readlinkat, AT_FDCWD, pb.ptr, buf, bufsiz);
    if (rc >= static_cast<ssize_t>(TOKEN_16_LEN)) {
        replaceAll16(reinterpret_cast<uint8_t*>(buf), static_cast<size_t>(rc), OLD_FILES_16, NEW_FILES_16);
        replaceAll16(reinterpret_cast<uint8_t*>(buf), static_cast<size_t>(rc), OLD_CACHE_16, NEW_CACHE_16);
    }
    return rc;
}

EXPORT_SYM ssize_t readlinkat(int dirfd, const char* pathname, char* buf, size_t bufsiz) {
    PathBuf pb(pathname);
    ssize_t rc = syscall(__NR_readlinkat, dirfd, pb.ptr, buf, bufsiz);
    if (rc >= static_cast<ssize_t>(TOKEN_16_LEN)) {
        replaceAll16(reinterpret_cast<uint8_t*>(buf), static_cast<size_t>(rc), OLD_FILES_16, NEW_FILES_16);
        replaceAll16(reinterpret_cast<uint8_t*>(buf), static_cast<size_t>(rc), OLD_CACHE_16, NEW_CACHE_16);
    }
    return rc;
}

EXPORT_SYM char* realpath(const char* path, char* resolved_path) {
    using fn_t = char* (*)(const char*, char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "realpath"));
    if (!path) {
        errno = EINVAL;
        return nullptr;
    }
    PathBuf pb(path);
    if (real_fn) {
        char* res = real_fn(pb.ptr, resolved_path);
        if (res) return res;
    }
    if (rawAccessAt(AT_FDCWD, pb.ptr, F_OK) == 0 || isContainerParentDir(pb.ptr)) {
        if (!resolved_path) return strdup(pb.ptr);
        strncpy(resolved_path, pb.ptr, PATH_MAX - 1);
        resolved_path[PATH_MAX - 1] = '\0';
        return resolved_path;
    }
    return nullptr;
}

EXPORT_SYM char* canonicalize_file_name(const char* path) {
    return realpath(path, nullptr);
}

EXPORT_SYM int chmod(const char* pathname, mode_t mode) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_fchmodat, AT_FDCWD, pb.ptr, mode, 0));
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int fchmodat(int dirfd, const char* pathname, mode_t mode, int flags) {
    using fn_t = int (*)(int, const char*, mode_t, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fchmodat"));
    PathBuf pb(pathname);
    int rc = real_fn
        ? real_fn(dirfd, pb.ptr, mode, flags)
        : static_cast<int>(syscall(__NR_fchmodat, dirfd, pb.ptr, mode, 0));
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int chown(const char* pathname, uid_t owner, gid_t group) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_fchownat, AT_FDCWD, pb.ptr, owner, group, 0));
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int lchown(const char* pathname, uid_t owner, gid_t group) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_fchownat, AT_FDCWD, pb.ptr, owner, group, AT_SYMLINK_NOFOLLOW));
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int fchownat(int dirfd, const char* pathname, uid_t owner, gid_t group, int flags) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_fchownat, dirfd, pb.ptr, owner, group, flags));
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int utimensat(int dirfd, const char* pathname, const struct timespec times[2], int flags) {
    PathBuf pb(pathname);
    int rc = static_cast<int>(syscall(__NR_utimensat, dirfd, pb.ptr, times, flags));
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int utimes(const char* filename, const struct timeval times[2]) {
    using fn_t = int (*)(const char*, const struct timeval*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "utimes"));
    if (!real_fn) return 0;
    PathBuf pb(filename);
    int rc = real_fn(pb.ptr, times);
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int lutimes(const char* filename, const struct timeval times[2]) {
    using fn_t = int (*)(const char*, const struct timeval*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "lutimes"));
    if (!real_fn) return 0;
    PathBuf pb(filename);
    int rc = real_fn(pb.ptr, times);
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int chdir(const char* path) {
    PathBuf pb(path);
    return static_cast<int>(syscall(__NR_chdir, pb.ptr));
}

} // extern "C"
