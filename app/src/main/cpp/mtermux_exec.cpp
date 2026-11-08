#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <string>
#include <vector>

static constexpr const char REAL_BIN_DIR[] = "/data/data/com.mtermux/file/usr/bin/.mtermux-real";
static constexpr const char USR_SH[]       = "/data/data/com.mtermux/file/usr/bin/.mtermux-real/sh";
static constexpr const char USR_SH_FALLBACK[] = "/data/data/com.mtermux/file/usr/bin/sh";
static constexpr const char LINKER64[]     = "/system/bin/linker64";

static std::string getBasename(const char* p) noexcept {
    if (!p) return "";
    const char* slash = strrchr(p, '/');
    const char* base = slash ? (slash + 1) : p;
    if (base[0] == '-' && base[1] != '\0') base++;
    return std::string(base);
}

static bool isElfFile(const char* path) noexcept {
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    unsigned char magic[4] = {0};
    ssize_t n = read(fd, magic, 4);
    close(fd);
    return (n == 4 && magic[0] == 0x7F && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F');
}

static int execScript(const char* scriptPath, char* argv[], char* envp[]) noexcept {
    int fd = open(scriptPath, O_RDONLY | O_CLOEXEC);
    std::string interp;
    std::string optArg;
    if (fd >= 0) {
        char hdr[512] = {0};
        ssize_t n = read(fd, hdr, sizeof(hdr) - 1);
        close(fd);
        if (n >= 2 && hdr[0] == '#' && hdr[1] == '!') {
            char* nl = strchr(hdr + 2, '\n');
            if (nl) *nl = '\0';
            char* cr = strchr(hdr + 2, '\r');
            if (cr) *cr = '\0';
            char* p = hdr + 2;
            while (*p && isspace(static_cast<unsigned char>(*p))) ++p;
            if (*p) {
                char* iStart = p;
                while (*p && !isspace(static_cast<unsigned char>(*p))) ++p;
                if (*p) {
                    *p++ = '\0';
                    while (*p && isspace(static_cast<unsigned char>(*p))) ++p;
                    if (*p) {
                        char* end = p + strlen(p) - 1;
                        while (end >= p && isspace(static_cast<unsigned char>(*end))) *end-- = '\0';
                        optArg = p;
                    }
                }
                interp = iStart;
            }
        }
    }

    if (interp.empty() || interp == "/bin/sh" || interp == "/usr/bin/sh" ||
        interp == "/data/data/com.termux/files/usr/bin/sh" ||
        interp == "/data/data/com.mtermux/file/usr/bin/sh") {
        interp = (access(USR_SH, R_OK) == 0) ? USR_SH : USR_SH_FALLBACK;
    } else if (interp == "/usr/bin/env" || interp == "/bin/env") {
        std::string realEnv = std::string(REAL_BIN_DIR) + "/env";
        if (access(realEnv.c_str(), R_OK) == 0) interp = realEnv;
    }

    std::vector<char*> args;
    if (isElfFile(interp.c_str())) {
        args.push_back(const_cast<char*>(LINKER64));
        args.push_back(const_cast<char*>(interp.c_str()));
    } else {
        args.push_back(const_cast<char*>(interp.c_str()));
    }
    if (!optArg.empty()) args.push_back(const_cast<char*>(optArg.c_str()));
    args.push_back(const_cast<char*>(scriptPath));
    for (int i = 1; argv[i] != nullptr; ++i) {
        args.push_back(argv[i]);
    }
    args.push_back(nullptr);
    execve(args[0], args.data(), envp);
    return -1;
}

int main(int argc, char* argv[], char* envp[]) {
    (void)argc;
    if (!argv || !argv[0]) return 127;

    std::string base = getBasename(argv[0]);
    if (base.empty() || base == "libmtermux-exec.so") {
        if (argc < 2) return 127;
        base = getBasename(argv[1]);
        argv++;
        argc--;
    }

    std::string realPath = std::string(REAL_BIN_DIR) + "/" + base;
    if (access(realPath.c_str(), R_OK) != 0) {
        return 127;
    }

    if (isElfFile(realPath.c_str())) {
        std::vector<char*> args;
        args.push_back(const_cast<char*>(LINKER64));
        args.push_back(const_cast<char*>(realPath.c_str()));
        for (int i = 1; argv[i] != nullptr; ++i) {
            args.push_back(argv[i]);
        }
        args.push_back(nullptr);
        execve(LINKER64, args.data(), envp);
        execve(realPath.c_str(), argv, envp);
        return 126;
    }

    execScript(realPath.c_str(), argv, envp);
    return 126;
}
