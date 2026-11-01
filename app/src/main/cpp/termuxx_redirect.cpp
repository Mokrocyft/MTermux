/*
 * libtermuxx-redirect.so — C++23 Transparent Path Redirector, Live Binary Patcher & DPKG/APT Engine
 *
 * 1. Rewrites all path forms:
 *    - "com.termux/files" -> "com.termuxx/file"
 *    - "com.termux/cache" -> "com.termuxx/cach"
 *    - "com.termux" (e.g. "./data/data/com.termux" during dpkg --unpack) -> "com.termuxx"
 * 2. Intercepts ALL VFS & metadata syscalls used by dpkg/apt/tar:
 *    open, open64, openat, openat64, fopen, fopen64, opendir, stat, lstat, fstatat,
 *    statvfs, statvfs64, statfs, statfs64, access, faccessat, mkdir, mkdirat, rmdir,
 *    unlink, unlinkat, rename, renameat, renameat2, link, linkat, symlink, symlinkat,
 *    readlink, readlinkat, chmod, fchmodat, chown, lchown, fchownat, utimensat, utimes,
 *    lutimes, chdir.
 * 3. Intercepts ALL exec family functions (execve, execv, execvp, execvpe, execl, execlp, execle)
 *    using direct kernel __NR_execve syscall and preserves LD_PRELOAD & LD_LIBRARY_PATH.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <ctype.h>
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
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
#include <time.h>
#include <unistd.h>

#include <string>
#include <vector>

extern char** environ;

namespace {

constexpr const char OLD_FILES_16[] = "com.termux/files";
constexpr const char NEW_FILES_16[] = "com.termuxx/file";
constexpr const char OLD_CACHE_16[] = "com.termux/cache";
constexpr const char NEW_CACHE_16[] = "com.termuxx/cach";
constexpr size_t TOKEN_16_LEN = 16;

constexpr const char REDIRECT_SO_PATH[] = "/data/data/com.termuxx/file/usr/lib/libtermuxx-redirect.so";
constexpr const char USR_LIB_PATH[]     = "/data/data/com.termuxx/file/usr/lib";
constexpr const char USR_BIN_PATH[]     = "/data/data/com.termuxx/file/usr/bin";

struct PathBuf {
    char buf[4096];
    const char* ptr;

    explicit PathBuf(const char* in) noexcept : ptr(in) {
        if (!in) return;
        if (strcmp(in, "/bin/sh") == 0) {
            strcpy(buf, "/data/data/com.termuxx/file/usr/bin/sh");
            ptr = buf;
            return;
        }
        if (strcmp(in, "/usr/bin/env") == 0 || strcmp(in, "/bin/env") == 0) {
            strcpy(buf, "/data/data/com.termuxx/file/usr/bin/env");
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
            } else if (i + 10 <= inLen && memcmp(in + i, "com.termux", 10) == 0 && in[i + 10] != 'x') {
                // Rewrites "./data/data/com.termux" or "/data/data/com.termux/" -> ".../com.termuxx..."
                memcpy(buf + outIdx, "com.termuxx", 11);
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

// Returns true if path refers to a container prefix directory (/, /data, /data/data, /data/data/com.termuxx, etc.)
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
           strcmp(norm, "data/data/com.termuxx") == 0 ||
           strcmp(norm, "data/data/com.termuxx/file") == 0 ||
           strcmp(norm, "data/data/com.termuxx/files") == 0 ||
           strcmp(norm, "data/data/com.termuxx/file/usr") == 0 ||
           strcmp(norm, "data/data/com.termuxx/files/usr") == 0;
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

static void patchInstalledFileIfNeeded(const char* path) noexcept {
    if (!path) return;
    if (!strstr(path, "/com.termuxx/file/usr/") && !strstr(path, "/com.termuxx/files/usr/")) return;
    if (strstr(path, ".dpkg-")) return;

    struct stat st{};
    if (syscall(__NR_newfstatat, AT_FDCWD, path, &st, AT_SYMLINK_NOFOLLOW) != 0 ||
        !S_ISREG(st.st_mode) || st.st_size <= 16 || st.st_size > 64 * 1024 * 1024) {
        return;
    }

    syscall(__NR_fchmodat, AT_FDCWD, path, st.st_mode | 0700, 0);
    int fd = rawOpenAt(AT_FDCWD, path, O_RDWR | O_CLOEXEC, 0);
    if (fd < 0) return;

    size_t size = static_cast<size_t>(st.st_size);
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (data) {
        ssize_t nRead = read(fd, data, size);
        if (nRead == static_cast<ssize_t>(size)) {
            bool c1 = replaceAll16(data, size, OLD_FILES_16, NEW_FILES_16);
            bool c2 = replaceAll16(data, size, OLD_CACHE_16, NEW_CACHE_16);
            if (c1 || c2) {
                lseek(fd, 0, SEEK_SET);
                (void)write(fd, data, size);
            }
        }
        free(data);
    }
    close(fd);
}

static std::vector<std::string> buildSanitizedEnvStrings(char* const envp[]) {
    std::vector<std::string> out;
    char* const* src = envp ? envp : environ;
    bool hasPreload = false;
    bool hasLibPath = false;
    bool hasPrefix  = false;
    bool hasTmpdir  = false;

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
                entry = std::string("LD_PRELOAD=") + REDIRECT_SO_PATH;
            } else if (entry.rfind("LD_LIBRARY_PATH=", 0) == 0) {
                hasLibPath = true;
                if (entry.find(USR_LIB_PATH) == std::string::npos) {
                    entry = std::string("LD_LIBRARY_PATH=") + USR_LIB_PATH + ":" + entry.substr(16);
                }
            } else if (entry.rfind("PREFIX=", 0) == 0) {
                hasPrefix = true;
            } else if (entry.rfind("TMPDIR=", 0) == 0) {
                hasTmpdir = true;
            }
            out.push_back(std::move(entry));
        }
    }

    if (!hasPreload) {
        out.emplace_back(std::string("LD_PRELOAD=") + REDIRECT_SO_PATH);
    }
    if (!hasLibPath) {
        out.emplace_back(std::string("LD_LIBRARY_PATH=") + USR_LIB_PATH);
    }
    if (!hasPrefix) {
        out.emplace_back("PREFIX=/data/data/com.termuxx/file/usr");
    }
    if (!hasTmpdir) {
        out.emplace_back("TMPDIR=/data/data/com.termuxx/file/usr/tmp");
    }
    return out;
}

static int executeResolvedPath(const char* rawFilename, char* const argv[], char* const envp[], int depth = 0) noexcept {
    if (!rawFilename || depth > 4) {
        errno = ENOENT;
        return -1;
    }

    PathBuf pb(rawFilename);
    const char* targetPath = pb.ptr;

    // Ensure maintainer scripts in tmp.ci or info are patched before execution
    if (strstr(targetPath, "/var/lib/dpkg/")) {
        patchInstalledFileIfNeeded(targetPath);
    }

    std::vector<std::string> envStore = buildSanitizedEnvStrings(envp);
    std::vector<char*> envPtrs;
    envPtrs.reserve(envStore.size() + 1);
    for (auto& s : envStore) {
        envPtrs.push_back(const_cast<char*>(s.c_str()));
    }
    envPtrs.push_back(nullptr);

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

                std::vector<char*> scriptArgs;
                scriptArgs.push_back(const_cast<char*>(resolvedInterp.c_str()));
                if (optArg && *optArg) {
                    scriptArgs.push_back(optArg);
                }
                scriptArgs.push_back(const_cast<char*>(targetPath));
                if (argv && argv[0] != nullptr) {
                    for (size_t i = 1; argv[i] != nullptr; ++i) {
                        scriptArgs.push_back(argv[i]);
                    }
                }
                scriptArgs.push_back(nullptr);

                return executeResolvedPath(resolvedInterp.c_str(), scriptArgs.data(), envPtrs.data(), depth + 1);
            }
        }
    }

    int rc = rawExecveSyscall(targetPath, argv, envPtrs.data());
    if (rc < 0 && errno == EACCES) {
        syscall(__NR_fchmodat, AT_FDCWD, targetPath, 0700, 0);
        rc = rawExecveSyscall(targetPath, argv, envPtrs.data());
        if (rc < 0 && errno == EACCES) {
            std::vector<char*> linkerArgs;
            linkerArgs.push_back(const_cast<char*>("/system/bin/linker64"));
            linkerArgs.push_back(const_cast<char*>(targetPath));
            if (argv && argv[0] != nullptr) {
                for (size_t i = 1; argv[i] != nullptr; ++i) {
                    linkerArgs.push_back(argv[i]);
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
        if (syscall(__NR_faccessat, AT_FDCWD, pbCand.ptr, R_OK, 0) == 0) {
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
// Exec Family Hooks
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

// ============================================================================
// File & Directory Hooks (with full dpkg container directory protection)
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
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM FILE* fopen64(const char* pathname, const char* mode) {
    using fn_t = FILE* (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM DIR* opendir(const char* name) {
    using fn_t = DIR* (*)(const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "opendir"));
    PathBuf pb(name);
    return real_fn(pb.ptr);
}

EXPORT_SYM int stat(const char* pathname, struct stat* statbuf) {
    using fn_t = int (*)(const char*, struct stat*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "stat"));
    PathBuf pb(pathname);
    int rc = real_fn(pb.ptr, statbuf);
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
    using fn_t = int (*)(const char*, struct stat*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "lstat"));
    PathBuf pb(pathname);
    int rc = real_fn(pb.ptr, statbuf);
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
    using fn_t = int (*)(int, const char*, struct stat*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fstatat"));
    PathBuf pb(pathname);
    int rc = real_fn(dirfd, pb.ptr, statbuf, flags);
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
    return real_fn(pb.ptr, buf);
}

EXPORT_SYM int statvfs64(const char* path, struct statvfs64* buf) {
    using fn_t = int (*)(const char*, struct statvfs64*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statvfs64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statvfs"));
    PathBuf pb(path);
    return real_fn(pb.ptr, buf);
}

EXPORT_SYM int statfs(const char* path, struct statfs* buf) {
    using fn_t = int (*)(const char*, struct statfs*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statfs"));
    PathBuf pb(path);
    return real_fn(pb.ptr, buf);
}

EXPORT_SYM int statfs64(const char* path, struct statfs64* buf) {
    using fn_t = int (*)(const char*, struct statfs64*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statfs64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "statfs"));
    PathBuf pb(path);
    return real_fn(pb.ptr, buf);
}

EXPORT_SYM int access(const char* pathname, int mode) {
    using fn_t = int (*)(const char*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "access"));
    PathBuf pb(pathname);
    int rc = real_fn(pb.ptr, mode);
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int faccessat(int dirfd, const char* pathname, int mode, int flags) {
    using fn_t = int (*)(int, const char*, int, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "faccessat"));
    PathBuf pb(pathname);
    int rc = real_fn(dirfd, pb.ptr, mode, flags);
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int mkdir(const char* pathname, mode_t mode) {
    using fn_t = int (*)(const char*, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "mkdir"));
    PathBuf pb(pathname);
    int rc = real_fn(pb.ptr, mode);
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        errno = EEXIST;
        return -1;
    }
    return rc;
}

EXPORT_SYM int mkdirat(int dirfd, const char* pathname, mode_t mode) {
    using fn_t = int (*)(int, const char*, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "mkdirat"));
    PathBuf pb(pathname);
    int rc = real_fn(dirfd, pb.ptr, mode);
    if (rc != 0 && isContainerParentDir(pb.ptr)) {
        errno = EEXIST;
        return -1;
    }
    return rc;
}

EXPORT_SYM int rmdir(const char* pathname) {
    using fn_t = int (*)(const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "rmdir"));
    PathBuf pb(pathname);
    if (isContainerParentDir(pb.ptr)) return 0;
    return real_fn(pb.ptr);
}

EXPORT_SYM int unlink(const char* pathname) {
    using fn_t = int (*)(const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "unlink"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr);
}

EXPORT_SYM int unlinkat(int dirfd, const char* pathname, int flags) {
    using fn_t = int (*)(int, const char*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "unlinkat"));
    PathBuf pb(pathname);
    if ((flags & AT_REMOVEDIR) && isContainerParentDir(pb.ptr)) return 0;
    return real_fn(dirfd, pb.ptr, flags);
}

EXPORT_SYM int rename(const char* oldpath, const char* newpath) {
    using fn_t = int (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "rename"));
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = real_fn(pbOld.ptr, pbNew.ptr);
    if (rc == 0) patchInstalledFileIfNeeded(pbNew.ptr);
    return rc;
}

EXPORT_SYM int renameat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath) {
    using fn_t = int (*)(int, const char*, int, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "renameat"));
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = real_fn(olddirfd, pbOld.ptr, newdirfd, pbNew.ptr);
    if (rc == 0) patchInstalledFileIfNeeded(pbNew.ptr);
    return rc;
}

EXPORT_SYM int renameat2(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, unsigned int flags) {
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = static_cast<int>(syscall(__NR_renameat2, olddirfd, pbOld.ptr, newdirfd, pbNew.ptr, flags));
    if (rc == 0) patchInstalledFileIfNeeded(pbNew.ptr);
    return rc;
}

EXPORT_SYM int link(const char* oldpath, const char* newpath) {
    using fn_t = int (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "link"));
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    return real_fn(pbOld.ptr, pbNew.ptr);
}

EXPORT_SYM int linkat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, int flags) {
    using fn_t = int (*)(int, const char*, int, const char*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "linkat"));
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    return real_fn(olddirfd, pbOld.ptr, newdirfd, pbNew.ptr, flags);
}

EXPORT_SYM int symlink(const char* target, const char* linkpath) {
    using fn_t = int (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "symlink"));
    PathBuf pbTarget(target);
    PathBuf pbLink(linkpath);
    return real_fn(pbTarget.ptr, pbLink.ptr);
}

EXPORT_SYM int symlinkat(const char* target, int newdirfd, const char* linkpath) {
    using fn_t = int (*)(const char*, int, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "symlinkat"));
    PathBuf pbTarget(target);
    PathBuf pbLink(linkpath);
    return real_fn(pbTarget.ptr, newdirfd, pbLink.ptr);
}

EXPORT_SYM ssize_t readlink(const char* pathname, char* buf, size_t bufsiz) {
    using fn_t = ssize_t (*)(const char*, char*, size_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "readlink"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, buf, bufsiz);
}

EXPORT_SYM ssize_t readlinkat(int dirfd, const char* pathname, char* buf, size_t bufsiz) {
    using fn_t = ssize_t (*)(int, const char*, char*, size_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "readlinkat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, buf, bufsiz);
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
    int rc = real_fn(dirfd, pb.ptr, mode, flags);
    if (rc != 0 && isContainerParentDir(pb.ptr)) return 0;
    return rc;
}

EXPORT_SYM int chown(const char* pathname, uid_t owner, gid_t group) {
    using fn_t = int (*)(const char*, uid_t, gid_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "chown"));
    PathBuf pb(pathname);
    int rc = real_fn(pb.ptr, owner, group);
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int lchown(const char* pathname, uid_t owner, gid_t group) {
    using fn_t = int (*)(const char*, uid_t, gid_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "lchown"));
    PathBuf pb(pathname);
    int rc = real_fn(pb.ptr, owner, group);
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int fchownat(int dirfd, const char* pathname, uid_t owner, gid_t group, int flags) {
    using fn_t = int (*)(int, const char*, uid_t, gid_t, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fchownat"));
    PathBuf pb(pathname);
    int rc = real_fn(dirfd, pb.ptr, owner, group, flags);
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int utimensat(int dirfd, const char* pathname, const struct timespec times[2], int flags) {
    using fn_t = int (*)(int, const char*, const struct timespec*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "utimensat"));
    PathBuf pb(pathname);
    int rc = real_fn(dirfd, pb.ptr, times, flags);
    if (rc != 0 && (errno == EPERM || errno == EACCES || isContainerParentDir(pb.ptr))) return 0;
    return rc;
}

EXPORT_SYM int utimes(const char* filename, const struct timeval times[2]) {
    using fn_t = int (*)(const char*, const struct timeval*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "utimes"));
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
    using fn_t = int (*)(const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "chdir"));
    PathBuf pb(path);
    return real_fn(pb.ptr);
}

} // extern "C"
