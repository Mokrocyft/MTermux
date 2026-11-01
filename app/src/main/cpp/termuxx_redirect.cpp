/*
 * libtermuxx-redirect.so — C++23 Transparent Path Redirector & On-The-Fly Binary Patcher
 *
 * Allows unmodified Termux .deb packages (compiled for /data/data/com.termux/files/usr)
 * to be unpacked, installed, and executed by dpkg / apt / pkg inside /data/data/com.termuxx/file/usr
 * without Root permissions.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <dlfcn.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

namespace {

constexpr const char OLD_PREFIX[] = "/data/data/com.termux/files";
constexpr const char NEW_PREFIX[] = "/data/data/com.termuxx/file";
constexpr size_t PREFIX_LEN = sizeof(OLD_PREFIX) - 1; // 27 bytes

constexpr const char OLD_CACHE[] = "/data/data/com.termux/cache";
constexpr const char NEW_CACHE[] = "/data/data/com.termuxx/cach";

constexpr const char OLD_16[] = "com.termux/files";
constexpr const char NEW_16[] = "com.termuxx/file";
constexpr size_t TOKEN_16_LEN = 16;

struct PathBuf {
    char buf[4096];
    const char* ptr;

    explicit PathBuf(const char* in) noexcept : ptr(in) {
        if (!in) return;
        const char* pos = strstr(in, "com.termux/");
        if (!pos) return;

        if (strncmp(in, OLD_PREFIX, PREFIX_LEN) == 0) {
            size_t tailLen = strlen(in + PREFIX_LEN);
            if (PREFIX_LEN + tailLen < sizeof(buf)) {
                memcpy(buf, NEW_PREFIX, PREFIX_LEN);
                memcpy(buf + PREFIX_LEN, in + PREFIX_LEN, tailLen + 1);
                ptr = buf;
            }
        } else if (strncmp(in, "./data/data/com.termux/files", PREFIX_LEN + 1) == 0) {
            size_t tailLen = strlen(in + PREFIX_LEN + 1);
            if (PREFIX_LEN + 1 + tailLen < sizeof(buf)) {
                buf[0] = '.';
                memcpy(buf + 1, NEW_PREFIX, PREFIX_LEN);
                memcpy(buf + 1 + PREFIX_LEN, in + 1 + PREFIX_LEN, tailLen + 1);
                ptr = buf;
            }
        } else if (strncmp(in, OLD_CACHE, PREFIX_LEN) == 0) {
            size_t tailLen = strlen(in + PREFIX_LEN);
            if (PREFIX_LEN + tailLen < sizeof(buf)) {
                memcpy(buf, NEW_CACHE, PREFIX_LEN);
                memcpy(buf + PREFIX_LEN, in + PREFIX_LEN, tailLen + 1);
                ptr = buf;
            }
        }
    }
};

// When dpkg finishes unpacking a file via rename("<file>.dpkg-new", "<file>"),
// patch any 16-byte "com.termux/files" occurrences to "com.termuxx/file" in-place!
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
        bool modified = false;
        uint8_t* cur = data;
        size_t rem = size;
        while (rem >= TOKEN_16_LEN) {
            auto* hit = static_cast<uint8_t*>(memmem(cur, rem, OLD_16, TOKEN_16_LEN));
            if (!hit) break;
            memcpy(hit, NEW_16, TOKEN_16_LEN);
            modified = true;
            size_t advance = static_cast<size_t>(hit - cur) + TOKEN_16_LEN;
            cur += advance;
            rem -= advance;
        }
        if (modified) {
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
    if (rc == 0) {
        patchInstalledFileIfNeeded(pbNew.ptr);
    }
    return rc;
}

EXPORT_SYM int renameat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath) {
    using fn_t = int (*)(int, const char*, int, const char*);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "renameat"));
    PathBuf pbOld(oldpath);
    PathBuf pbNew(newpath);
    int rc = real_fn(olddirfd, pbOld.ptr, newdirfd, pbNew.ptr);
    if (rc == 0) {
        patchInstalledFileIfNeeded(pbNew.ptr);
    }
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

EXPORT_SYM int execve(const char* pathname, char* const argv[], char* const envp[]) {
    using fn_t = int (*)(const char*, char* const[], char* const[]);
    static auto real_fn = reinterpret_cast<fn_t>(dlsym(RTLD_NEXT, "execve"));
    PathBuf pb(pathname);
    return real_fn(pb.ptr, argv, envp);
}

} // extern "C"
