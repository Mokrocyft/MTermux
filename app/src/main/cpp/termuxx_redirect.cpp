/*
 * libtermuxx-redirect.so — C++23 Transparent Path Redirector, Live Binary Patcher & Android 17 (API 37) Executor
 *
 * 1. Intercepts VFS syscalls (open, stat, statvfs, access, rename, symlink, readlink, execve, etc.)
 *    and rewrites "/data/data/com.termux/files" -> "/data/data/com.termuxx/file" and
 *    "/data/data/com.termux/cache" -> "/data/data/com.termuxx/cach".
 * 2. Enforces Android 17 (targetSdk 37) Safer Dynamic Code Loading (read-only 0500 mode on ELF binaries/libs)
 *    while transparently unlocking write permissions when dpkg/apt updates files.
 * 3. Intercepts execve() with automatic shebang (#!) resolution and /system/bin/linker64 fallback for targetSdk 37.
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
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <vector>

namespace {

constexpr const char OLD_FILES_16[] = "com.termux/files";
constexpr const char NEW_FILES_16[] = "com.termuxx/file";
constexpr const char OLD_CACHE_16[] = "com.termux/cache";
constexpr const char NEW_CACHE_16[] = "com.termuxx/cach";
constexpr size_t TOKEN_16_LEN = 16;

struct PathBuf {
    char buf[4096];
    const char* ptr;

    explicit PathBuf(const char* in) noexcept : ptr(in) {
        if (!in) return;
        if (strncmp(in, "/bin/sh", 8) == 0) {
            strcpy(buf, "/data/data/com.termuxx/file/usr/bin/sh");
            ptr = buf;
            return;
        }
        if (strncmp(in, "/usr/bin/env", 13) == 0) {
            strcpy(buf, "/data/data/com.termuxx/file/usr/bin/env");
            ptr = buf;
            return;
        }
        const char* hit = strstr(in, "com.termux/");
        if (!hit) return;

        size_t len = strlen(in);
        if (len >= sizeof(buf)) return;
        memcpy(buf, in, len + 1);

        char* p = buf;
        while ((p = strstr(p, OLD_FILES_16)) != nullptr) {
            memcpy(p, NEW_FILES_16, TOKEN_16_LEN);
            p += TOKEN_16_LEN;
        }
        p = buf;
        while ((p = strstr(p, OLD_CACHE_16)) != nullptr) {
            memcpy(p, NEW_CACHE_16, TOKEN_16_LEN);
            p += TOKEN_16_LEN;
        }
        ptr = buf;
    }
};

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

static int rawChmod(const char* path, mode_t mode) noexcept {
    using fn_t = int (*)(const char*, mode_t);
    static auto real_chmod = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "chmod"));
    return real_chmod ? real_chmod(path, mode) : -1;
}

static int rawOpenAt(int dirfd, const char* path, int flags, mode_t mode) noexcept {
    using fn_t = int (*)(int, const char*, int, mode_t);
    static auto real_openat = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "openat"));
    return real_openat ? real_openat(dirfd, path, flags, mode) : -1;
}

static bool isElfFile(const char* path) noexcept {
    int fd = rawOpenAt(AT_FDCWD, path, O_RDONLY | O_CLOEXEC, 0);
    if (fd < 0) return false;
    uint8_t magic[4] = {0};
    ssize_t n = read(fd, magic, 4);
    close(fd);
    return n == 4 && magic[0] == 0x7F && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F';
}

static void patchInstalledFileIfNeeded(const char* path) noexcept {
    if (!path) return;
    if (!strstr(path, "/com.termuxx/file/usr/")) return;
    if (strstr(path, ".dpkg-")) return;

    struct stat st{};
    if (lstat(path, &st) != 0 || !S_ISREG(st.st_mode) || st.st_size <= 4 || st.st_size > 64 * 1024 * 1024) {
        return;
    }

    // Temporarily enable owner write for in-place patching
    rawChmod(path, st.st_mode | 0700);
    int fd = rawOpenAt(AT_FDCWD, path, O_RDWR | O_CLOEXEC, 0);
    if (fd < 0) return;

    size_t size = static_cast<size_t>(st.st_size);
    auto* data = static_cast<uint8_t*>(malloc(size));
    bool isElf = false;
    if (data) {
        ssize_t nRead = read(fd, data, size);
        if (nRead == static_cast<ssize_t>(size)) {
            isElf = (size >= 4 && data[0] == 0x7F && data[1] == 'E' && data[2] == 'L' && data[3] == 'F');
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

    // Enforce read-only executable mode (0500) on ELF binaries/libraries for Android 17 Safer DCL
    if (isElf) {
        rawChmod(path, 0500);
    } else {
        rawChmod(path, st.st_mode);
    }
}

static void unlockWriteIfExistingUsrFile(const char* path, int flags) noexcept {
    if (!path) return;
    if ((flags & (O_WRONLY | O_RDWR | O_TRUNC)) == 0) return;
    if (!strstr(path, "/com.termuxx/file/usr/")) return;
    struct stat st{};
    if (lstat(path, &st) == 0 && S_ISREG(st.st_mode) && (st.st_mode & 0200) == 0) {
        rawChmod(path, st.st_mode | 0700);
    }
}

} // namespace

extern "C" {

#define EXPORT_SYM __attribute__((visibility("default")))

EXPORT_SYM int open(const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode = static_cast<mode_t>(va_arg(ap, int));
        va_end(ap);
    }
    using fn_t = int (*)(const char*, int, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "open"));
    PathBuf pb(pathname);
    unlockWriteIfExistingUsrFile(pb.ptr, flags);
    return real_fn(pb.ptr, flags, mode);
}

EXPORT_SYM int open64(const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode = static_cast<mode_t>(va_arg(ap, int));
        va_end(ap);
    }
    using fn_t = int (*)(const char*, int, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "open64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "open"));
    PathBuf pb(pathname);
    unlockWriteIfExistingUsrFile(pb.ptr, flags);
    return real_fn(pb.ptr, flags, mode);
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
    unlockWriteIfExistingUsrFile(pb.ptr, flags);
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
    using fn_t = int (*)(int, const char*, int, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "openat64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "openat"));
    PathBuf pb(pathname);
    unlockWriteIfExistingUsrFile(pb.ptr, flags);
    return real_fn(dirfd, pb.ptr, flags, mode);
}

EXPORT_SYM FILE* fopen(const char* pathname, const char* mode) {
    using fn_t = FILE* (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen"));
    PathBuf pb(pathname);
    if (mode && (strchr(mode, 'w') || strchr(mode, 'a') || strchr(mode, '+'))) {
        unlockWriteIfExistingUsrFile(pb.ptr, O_RDWR);
    }
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM FILE* fopen64(const char* pathname, const char* mode) {
    using fn_t = FILE* (*)(const char*, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen64"));
    if (!real_fn) real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fopen"));
    PathBuf pb(pathname);
    if (mode && (strchr(mode, 'w') || strchr(mode, 'a') || strchr(mode, '+'))) {
        unlockWriteIfExistingUsrFile(pb.ptr, O_RDWR);
    }
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM void* dlopen(const char* filename, int flags) {
    using fn_t = void* (*)(const char*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "dlopen"));
    if (!filename) return real_fn(nullptr, flags);
    PathBuf pb(filename);
    if (strstr(pb.ptr, "/com.termuxx/file/usr/") && isElfFile(pb.ptr)) {
        rawChmod(pb.ptr, 0500);
    }
    return real_fn(pb.ptr, flags);
}

EXPORT_SYM int execve(const char* filename, char* const argv[], char* const envp[]) {
    using fn_t = int (*)(const char*, char* const[], char* const[]);
    static auto real_execve = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "execve"));
    if (!real_execve) {
        errno = ENOENT;
        return -1;
    }

    PathBuf pb(filename);
    const char* targetPath = pb.ptr;

    // Inspect target header for #! shebang or ELF magic
    int fd = rawOpenAt(AT_FDCWD, targetPath, O_RDONLY | O_CLOEXEC, 0);
    if (fd >= 0) {
        char hdr[256] = {0};
        ssize_t n = read(fd, hdr, sizeof(hdr) - 1);
        close(fd);

        if (n >= 4 && static_cast<uint8_t>(hdr[0]) == 0x7F && hdr[1] == 'E' && hdr[2] == 'L' && hdr[3] == 'F') {
            if (strstr(targetPath, "/com.termuxx/file/usr/")) {
                rawChmod(targetPath, 0500);
            }
            int rc = real_execve(targetPath, argv, envp);
            if (rc < 0 && errno == EACCES) {
                std::vector<char*> linkerArgs;
                linkerArgs.push_back(const_cast<char*>("/system/bin/linker64"));
                linkerArgs.push_back(const_cast<char*>(targetPath));
                if (argv[0] != nullptr) {
                    for (size_t i = 1; argv[i] != nullptr; ++i) {
                        linkerArgs.push_back(argv[i]);
                    }
                }
                linkerArgs.push_back(nullptr);
                return real_execve("/system/bin/linker64", linkerArgs.data(), envp);
            }
            return rc;
        } else if (n >= 2 && hdr[0] == '#' && hdr[1] == '!') {
            char* lineEnd = strchr(hdr + 2, '\n');
            if (lineEnd) *lineEnd = '\0';
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
                if (strstr(interpPb.ptr, "/com.termuxx/file/usr/") && isElfFile(interpPb.ptr)) {
                    rawChmod(interpPb.ptr, 0500);
                }
                std::vector<char*> scriptArgs;
                scriptArgs.push_back(const_cast<char*>(interpPb.ptr));
                if (optArg && *optArg) scriptArgs.push_back(optArg);
                scriptArgs.push_back(const_cast<char*>(targetPath));
                if (argv[0] != nullptr) {
                    for (size_t i = 1; argv[i] != nullptr; ++i) {
                        scriptArgs.push_back(argv[i]);
                    }
                }
                scriptArgs.push_back(nullptr);
                int rc = real_execve(interpPb.ptr, scriptArgs.data(), envp);
                if (rc < 0 && errno == EACCES) {
                    scriptArgs.insert(scriptArgs.begin(), const_cast<char*>("/system/bin/linker64"));
                    return real_execve("/system/bin/linker64", scriptArgs.data(), envp);
                }
                return rc;
            }
        }
    }

    return real_execve(targetPath, argv, envp);
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
    return real_fn(pb.ptr, statbuf);
}

EXPORT_SYM int lstat(const char* pathname, struct stat* statbuf) {
    using fn_t = int (*)(const char*, struct stat*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "lstat"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, statbuf);
}

EXPORT_SYM int fstatat(int dirfd, const char* pathname, struct stat* statbuf, int flags) {
    using fn_t = int (*)(int, const char*, struct stat*, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fstatat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, statbuf, flags);
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
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM int faccessat(int dirfd, const char* pathname, int mode, int flags) {
    using fn_t = int (*)(int, const char*, int, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "faccessat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, mode, flags);
}

EXPORT_SYM int mkdir(const char* pathname, mode_t mode) {
    using fn_t = int (*)(const char*, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "mkdir"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM int mkdirat(int dirfd, const char* pathname, mode_t mode) {
    using fn_t = int (*)(int, const char*, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "mkdirat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, mode);
}

EXPORT_SYM int rmdir(const char* pathname) {
    using fn_t = int (*)(const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "rmdir"));
    PathBuf pb(pathname);
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
    if (strstr(pb.ptr, "/com.termuxx/file/usr/") && !strstr(pb.ptr, ".dpkg-") && isElfFile(pb.ptr)) {
        mode = 0500;
    }
    return rawChmod(pb.ptr, mode);
}

EXPORT_SYM int fchmodat(int dirfd, const char* pathname, mode_t mode, int flags) {
    using fn_t = int (*)(int, const char*, mode_t, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fchmodat"));
    PathBuf pb(pathname);
    if (strstr(pb.ptr, "/com.termuxx/file/usr/") && !strstr(pb.ptr, ".dpkg-") && isElfFile(pb.ptr)) {
        mode = 0500;
    }
    return real_fn(dirfd, pb.ptr, mode, flags);
}

} // extern "C"
