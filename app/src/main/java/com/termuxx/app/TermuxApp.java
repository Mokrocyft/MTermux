package com.termuxx.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Minimalist Application entry point for Termuxx (com.termuxx).
 */
public final class TermuxApp extends Application {

    public static final String NOTIFICATION_CHANNEL_ID = "termuxx_session_channel";
    public static final int NOTIFICATION_ID = 1337;

    @Override
    public void onCreate() {
        super.onCreate();
        setupCrashLogger();
        setupNotificationChannel();
    }

    private void setupCrashLogger() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File homeDir = new File(NativeBridge.HOME_PATH);
                if (!homeDir.exists()) homeDir.mkdirs();
                File crashFile = new File(homeDir, "termux_crash.log");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("=== Termuxx Crash (" + Instant.now().toString() + ") ===");
                throwable.printStackTrace(pw);
                pw.flush();
                try (FileOutputStream fos = new FileOutputStream(crashFile, true)) {
                    fos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (Throwable ignored) {
            } finally {
                if (prev != null) prev.uncaughtException(thread, throwable);
            }
        });
    }

    private void setupNotificationChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Termux Sessions",
            NotificationManager.IMPORTANCE_LOW
        );
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }
}
