/*
 * libtermuxx-redirect.so — C++23 Transparent Path Redirector & Live Binary Patcher
 *
 * Intercepts filesystem & VFS syscalls (including statvfs/statfs used by apt for free-space checks)
 * and rewrites "/data/data/com.termux/files" -> "/data/data/com.termuxx/file" and
 * "/data/data/com.termux/cache" -> "/data/data/com.termuxx/cach".
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <dirent.h>
#include <dlfcn.h>
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

static void patchInstalledFileIfNeeded(const char* path) noexcept {
    if (!path) return;
    if (!strstr(path, "/com.termuxx/file/usr/")) return;
    if (strstr(path, ".dpkg-")) return;

    using openat_fn = int (*)(int, const char*, int, ...);
    static auto real_openat = reinterpret_cast<openat_fn>(dlsym(RTLD_NEXT, "openat"));
    if (!real_openat) return;

    struct stat st{};
    if (lstat(path, &st) != 0 || !S_ISREG(st.st_mode) || st.st_size <= 16 || st.st_size > 64 * 1024 * 1024) {
        return;
    }

    int fd = real_openat(AT_FDCWD, path, O_RDWR | O_CLOEXEC, 0);
    if (fd < 0) return;

    size_t size = static_cast<size_t>(st.st_size);
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (!data) {
        close(fd);
        return;
    }

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
    close(fd);
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
    using fn_t = int (*)(int, const char*, int, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "openat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, flags, mode);
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
    return real_fn(dirfd, pb.ptr, flags, mode);
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
    using fn_t = int (*)(const char*, mode_t);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "chmod"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, mode);
}

EXPORT_SYM int fchmodat(int dirfd, const char* pathname, mode_t mode, int flags) {
    using fn_t = int (*)(int, const char*, mode_t, int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "fchmodat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, mode, flags);
}

EXPORT_SYM int utimensat(int dirfd, const char* pathname, const struct timespec times[2], int flags) {
    using fn_t = int (*)(int, const char*, const struct timespec[2], int);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "utimensat"));
    PathBuf pb(pathname);
    return real_fn(dirfd, pb.ptr, times, flags);
}

EXPORT_SYM int execve(const char* pathname, char* const argv[], char* const envp[]) {
    using fn_t = int (*)(const char*, char* const[], char* const[]);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "execve"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, argv, envp);
}

} // extern "C"
