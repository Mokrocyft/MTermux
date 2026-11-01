package com.termuxx.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight Foreground Service holding C++23 session handles and WakeLock.
 */
public final class TermuxService extends Service {

    public static final String ACTION_TOGGLE_WAKELOCK = "com.termuxx.app.TOGGLE_WAKELOCK";
    public static final String ACTION_STOP_SERVICE    = "com.termuxx.app.STOP_SERVICE";

    public static final class Session {
        public final long handle;
        public final boolean isRoot;
        public final String label;
        public boolean started = false;

        Session(long handle, boolean isRoot, String label) {
            this.handle = handle;
            this.isRoot = isRoot;
            this.label = label;
        }
    }

    public final class LocalBinder extends Binder {
        public TermuxService getService() {
            return TermuxService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();
    private final List<Session> mSessions = new ArrayList<>();
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private boolean mWakeLockHeld = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundCompat();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundCompat();
        if (intent != null && intent.getAction() != null) {
            if (ACTION_TOGGLE_WAKELOCK.equals(intent.getAction())) {
                toggleWakeLock();
            } else if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
                stopAllAndSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        for (Session s : mSessions) {
            NativeBridge.destroyEngine(s.handle);
        }
        mSessions.clear();
        super.onDestroy();
    }

    public List<Session> getSessions() {
        return Collections.unmodifiableList(mSessions);
    }

    public Session createSession(boolean asRoot) {
        long handle = NativeBridge.createEngine(45, 72, 4000);
        Session session = new Session(handle, asRoot, asRoot ? "su" : "sh");
        mSessions.add(session);
        updateNotification();
        return session;
    }

    public void removeSession(Session session) {
        if (session == null) return;
        mSessions.remove(session);
        NativeBridge.destroyEngine(session.handle);
        updateNotification();
    }

    public void toggleWakeLock() {
        if (mWakeLockHeld) {
            releaseWakeLock();
        } else {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Termuxx::WakeLock");
                mWakeLock.setReferenceCounted(false);
                mWakeLock.acquire();
            }
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Termuxx::WifiLock");
                mWifiLock.setReferenceCounted(false);
                mWifiLock.acquire();
            }
            mWakeLockHeld = true;
        }
        updateNotification();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        mWakeLock = null;
        if (mWifiLock != null && mWifiLock.isHeld()) mWifiLock.release();
        mWifiLock = null;
        mWakeLockHeld = false;
    }

    public void stopAllAndSelf() {
        releaseWakeLock();
        for (Session s : new ArrayList<>(mSessions)) {
            NativeBridge.destroyEngine(s.handle);
        }
        mSessions.clear();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void startForegroundCompat() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(TermuxApp.NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(TermuxApp.NOTIFICATION_ID, n);
        }
    }

    private void updateNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(TermuxApp.NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, TermuxActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent wakeIntent = new Intent(this, TermuxService.class).setAction(ACTION_TOGGLE_WAKELOCK);
        PendingIntent wakePi = PendingIntent.getService(
            this, 1, wakeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent exitIntent = new Intent(this, TermuxService.class).setAction(ACTION_STOP_SERVICE);
        PendingIntent exitPi = PendingIntent.getService(
            this, 2, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String text = mSessions.size() + " active" + (mWakeLockHeld ? " • WakeLock" : "");
        return new Notification.Builder(this, TermuxApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("Termux")
            .setContentText(text)
            .setContentIntent(contentPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(
                null, mWakeLockHeld ? "Unlock" : "WakeLock", wakePi).build())
            .addAction(new Notification.Action.Builder(
                null, "Exit", exitPi).build())
            .build();
    }
}
