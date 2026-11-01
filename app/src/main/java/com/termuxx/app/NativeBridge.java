package com.termuxx.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.system.Os;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Thin JNI Bridge to C++23 libtermux-core.so + First-Run Bootstrap Extractor.
 */
public final class NativeBridge {

    static {
        System.loadLibrary("termux-core");
    }

    public static final String FILES_REAL_PATH = "/data/data/com.termuxx/file";
    public static final String PREFIX_PATH     = FILES_REAL_PATH + "/usr";
    public static final String BIN_PATH        = PREFIX_PATH + "/bin";
    public static final String HOME_PATH       = FILES_REAL_PATH + "/home";

    // Cell style bit flags (matching termux_core.cpp)
    public static final int FLAG_BOLD          = 1 << 0;
    public static final int FLAG_DIM           = 1 << 1;
    public static final int FLAG_ITALIC        = 1 << 2;
    public static final int FLAG_UNDERLINE     = 1 << 3;
    public static final int FLAG_BLINK         = 1 << 4;
    public static final int FLAG_INVERSE       = 1 << 5;
    public static final int FLAG_INVISIBLE     = 1 << 6;
    public static final int FLAG_STRIKETHROUGH = 1 << 7;
    public static final int FLAG_WIDE_LEAD     = 1 << 8;
    public static final int FLAG_WIDE_TRAIL    = 1 << 9;

    // Terminal mode bits
    public static final int MODE_APP_CURSOR_KEYS = 1 << 0;
    public static final int MODE_BRACKETED_PASTE = 1 << 1;
    public static final int MODE_ALT_SCREEN      = 1 << 2;
    public static final int MODE_MOUSE_TRACKING  = 1 << 3;
    public static final int MODE_MOUSE_SGR       = 1 << 4;
    public static final int MODE_BELL_TRIGGERED  = 1 << 5;
    public static final int MODE_AUTOWRAP        = 1 << 6;

    // Pure Dark AMOLED Defaults
    public static final int DEFAULT_FG = 0xFFE5E9F0;
    public static final int DEFAULT_BG = 0xFF000000;

    private static volatile boolean sIsInstalling = false;

    private NativeBridge() {}

    public interface BootstrapCallback {
        void onProgress(String msg);
        void onReady();
    }

    public static boolean isBootstrapReady() {
        return new File(BIN_PATH, "bash").exists() || new File(BIN_PATH, "apt").exists();
    }

    public static void ensureBootstrapAsync(final Context context, final BootstrapCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;

        if (isBootstrapReady()) {
            finalizeBootstrapNative(nativeLibDir);
            mainHandler.post(callback::onReady);
            return;
        }
        if (sIsInstalling) return;
        sIsInstalling = true;

        Thread t = new Thread(() -> {
            try {
                mainHandler.post(() -> callback.onProgress("unpacking bootstrap..."));
                File baseDir = new File(FILES_REAL_PATH);
                if (!baseDir.exists()) baseDir.mkdirs();

                File prefixDir = new File(PREFIX_PATH);
                File stagingDir = new File(FILES_REAL_PATH, "usr-staging");
                if (stagingDir.exists()) deleteRecursive(stagingDir);
                stagingDir.mkdirs();

                ByteBuffer zipBuf = getBootstrapBuffer();
                if (zipBuf == null || zipBuf.remaining() <= 0) {
                    throw new IllegalStateException("Empty bootstrap buffer");
                }

                byte[] ioBuf = new byte[65536];
                ByteArrayOutputStream fileBuffer = new ByteArrayOutputStream(262144);
                List<String[]> symlinks = new ArrayList<>(512);

                try (ZipInputStream zis = new ZipInputStream(new ByteBufferInputStream(zipBuf.duplicate()))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        if ("SYMLINKS.txt".equals(name)) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.replace("com.termux/files", "com.termuxx/file");
                                int arrow = line.indexOf("←");
                                if (arrow > 0) {
                                    String target = line.substring(0, arrow);
                                    String linkPath = new File(stagingDir, line.substring(arrow + 1)).getAbsolutePath();
                                    symlinks.add(new String[]{target, linkPath});
                                }
                            }
                        } else {
                            File targetFile = new File(stagingDir, name);
                            if (entry.isDirectory()) {
                                targetFile.mkdirs();
                            } else {
                                File parent = targetFile.getParentFile();
                                if (parent != null && !parent.exists()) parent.mkdirs();

                                fileBuffer.reset();
                                int n;
                                while ((n = zis.read(ioBuf)) != -1) {
                                    fileBuffer.write(ioBuf, 0, n);
                                }
                                byte[] fullBytes = fileBuffer.toByteArray();
                                // C++23 16-byte in-place binary patch ("com.termux/files" -> "com.termuxx/file")
                                patchBytesInPlace(fullBytes, fullBytes.length);

                                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                                    fos.write(fullBytes);
                                }
                                if (name.startsWith("bin/") ||
                                    name.startsWith("libexec") ||
                                    name.startsWith("lib/apt/apt-helper") ||
                                    name.startsWith("lib/apt/methods") ||
                                    name.startsWith("var/lib/dpkg/info/")) {
                                    // noinspection OctalInteger
                                    Os.chmod(targetFile.getAbsolutePath(), 0700);
                                }
                            }
                        }
                    }
                }

                for (String[] pair : symlinks) {
                    File linkFile = new File(pair[1]);
                    File parent = linkFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    if (linkFile.exists()) linkFile.delete();
                    Os.symlink(pair[0], pair[1]);
                }

                if (prefixDir.exists()) deleteRecursive(prefixDir);
                stagingDir.renameTo(prefixDir);
                finalizeBootstrapNative(nativeLibDir);
            } catch (Throwable ignored) {
            } finally {
                sIsInstalling = false;
                mainHandler.post(callback::onReady);
            }
        }, "TermuxxBootstrap");
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private static void deleteRecursive(File f) {
        try {
            if (!Os.lstat(f.getAbsolutePath()).toString().contains("S_IFLNK") && f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    for (File c : children) deleteRecursive(c);
                }
            }
        } catch (Exception ignored) {
        }
        f.delete();
    }

    private static final class ByteBufferInputStream extends InputStream {
        private final ByteBuffer mBuf;
        ByteBufferInputStream(ByteBuffer buf) { mBuf = buf; }
        @Override public int read() { return mBuf.hasRemaining() ? (mBuf.get() & 0xFF) : -1; }
        @Override public int read(byte[] b, int off, int len) {
            if (!mBuf.hasRemaining()) return -1;
            int n = Math.min(len, mBuf.remaining());
            mBuf.get(b, off, n);
            return n;
        }
    }

    // Native C++23 JNI Methods
    public static native ByteBuffer getBootstrapBuffer();
    public static native void patchBytesInPlace(byte[] buf, int len);
    public static native void finalizeBootstrapNative(String nativeLibDir);

    public static native long createEngine(int rows, int cols, int scrollbackRows);
    public static native boolean startSession(long handle, boolean asRoot, String nativeLibDir, int rows, int cols, int cellW, int cellH);
    public static native void destroyEngine(long handle);
    public static native void resizeEngine(long handle, int rows, int cols, int cellW, int cellH);
    public static native void resetEngine(long handle);
    public static native boolean isRunning(long handle);
    public static native boolean consumeFrameDirty(long handle);

    public static native void sendText(long handle, String text, boolean isPaste);
    public static native void sendCodepoint(long handle, int codepoint, boolean ctrlDown, boolean altDown);
    public static native boolean sendKeyCode(long handle, int keyCode, int metaState);
    public static native void sendMouse(long handle, int button, int col, int row, boolean pressed);

    public static native boolean isRowDirty(long handle, int screenRow);
    public static native void clearDirtyFlags(long handle);
    public static native int getActiveScrollback(long handle);
    public static native int packRowRender(long handle, int scrollOffset, int screenRow, int[] outBgRuns, int[] outGlyphs);
    public static native String getTranscriptText(long handle, int maxScrollbackLines);

    public static native int getCursorRow(long handle);
    public static native int getCursorCol(long handle);
    public static native boolean isCursorVisible(long handle);
    public static native int getModesMask(long handle);
}
