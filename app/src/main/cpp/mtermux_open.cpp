#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include <string>

static void writeAll(int fd, const char* data, size_t len) noexcept {
    size_t off = 0;
    while (off < len) {
        ssize_t n = write(fd, data + off, len - off);
        if (n <= 0) break;
        off += static_cast<size_t>(n);
    }
}

int main(int argc, char* argv[]) {
    const char* url = nullptr;
    for (int i = 1; i < argc; ++i) {
        if (!argv[i]) continue;
        if (strcmp(argv[i], "--") == 0) {
            if (i + 1 < argc) url = argv[i + 1];
            break;
        }
        if (argv[i][0] == '-') continue;
        url = argv[i];
        break;
    }
    if (!url || !url[0]) {
        const char msg[] = "Usage: termux-open-url <url>\n";
        writeAll(STDERR_FILENO, msg, sizeof(msg) - 1);
        return 1;
    }

    std::string seq = std::string("\x1b]777;open;") + url + "\x07";
    int ttyFd = open("/dev/tty", O_WRONLY | O_CLOEXEC);
    if (ttyFd >= 0) {
        writeAll(ttyFd, seq.data(), seq.size());
        close(ttyFd);
    } else {
        writeAll(STDOUT_FILENO, seq.data(), seq.size());
    }
    return 0;
}
