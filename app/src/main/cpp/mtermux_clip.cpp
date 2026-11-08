#include <fcntl.h>
#include <stdint.h>
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

static std::string base64Encode(const std::string& in) noexcept {
    static constexpr char kTbl[] =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string out;
    out.reserve(((in.size() + 2) / 3) * 4);
    size_t i = 0;
    while (i < in.size()) {
        uint32_t a = static_cast<uint8_t>(in[i++]);
        bool hasB = (i < in.size());
        uint32_t b = hasB ? static_cast<uint8_t>(in[i++]) : 0;
        bool hasC = (i < in.size());
        uint32_t c = hasC ? static_cast<uint8_t>(in[i++]) : 0;
        uint32_t triple = (a << 16) | (b << 8) | c;
        out.push_back(kTbl[(triple >> 18) & 0x3F]);
        out.push_back(kTbl[(triple >> 12) & 0x3F]);
        out.push_back(hasB ? kTbl[(triple >> 6) & 0x3F] : '=');
        out.push_back(hasC ? kTbl[triple & 0x3F] : '=');
    }
    return out;
}

int main(int argc, char* argv[]) {
    for (int i = 1; i < argc; ++i) {
        if (!argv[i]) continue;
        if (strcmp(argv[i], "-version") == 0 ||
            strcmp(argv[i], "--version") == 0 ||
            strcmp(argv[i], "-v") == 0) {
            const char ver[] = "xclip version 0.13\n";
            writeAll(STDOUT_FILENO, ver, sizeof(ver) - 1);
            return 0;
        }
        if (strcmp(argv[i], "-o") == 0 || strcmp(argv[i], "-out") == 0) {
            return 0;
        }
    }

    std::string input;
    if (!isatty(STDIN_FILENO)) {
        char buf[4096];
        for (;;) {
            ssize_t n = read(STDIN_FILENO, buf, sizeof(buf));
            if (n <= 0) break;
            input.append(buf, static_cast<size_t>(n));
            if (input.size() >= 1024 * 1024) break;
        }
    } else {
        for (int i = 1; i < argc; ++i) {
            if (!argv[i] || argv[i][0] == '-') continue;
            if (!input.empty()) input.push_back(' ');
            input += argv[i];
        }
    }

    if (!input.empty()) {
        std::string seq = "\x1b]52;c;" + base64Encode(input) + "\x07";
        int ttyFd = open("/dev/tty", O_WRONLY | O_CLOEXEC);
        if (ttyFd >= 0) {
            writeAll(ttyFd, seq.data(), seq.size());
            close(ttyFd);
        } else {
            writeAll(STDOUT_FILENO, seq.data(), seq.size());
        }
    }
    return 0;
}
