package com.example.round_timer.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.round_timer.MainActivity;
import com.example.round_timer.R;

import java.security.Provider;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {

    private static final String TAG = "TimerService";
    private static final String CHANNEL_ID = "RoundTimerChannel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();
    private PowerManager.WakeLock wakeLock;

    private Timer timer;
    private long startTime;
    private long pausedTime;
    private long remainingTime;
    private boolean isPaused = true;

    private TimerUpdateListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    public interface TimerUpdateListener {
        void onTimerTick(long millisUntilFinished);
        void onTimerFinish();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TimerService onCreate");
        // Acquire wake lock to keep CPU running when screen is off
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "RoundTimer:WakeLock");
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "TimerService onStartCommand");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Round Timer is running"));
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "TimerService onBind");
        return binder;
    }

    public void startTimer(long durationMillis, TimerUpdateListener listener) {
        Log.d(TAG, "Starting timer for " + durationMillis + "ms");
        this.listener = listener;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        isPaused = false;
        remainingTime = durationMillis;
        startTime = SystemClock.elapsedRealtime();

        if (!wakeLock.isHeld()) {
            Log.d(TAG, "Acquiring wake lock");
            wakeLock.acquire(durationMillis + 5000); // Přidáváme 5 sekund jako buffer
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = SystemClock.elapsedRealtime();
                long elapsedTime = currentTime - startTime;

                if (elapsedTime >= remainingTime) {
                    Log.d(TAG, "Timer completed");
                    stopTimer();
                    if (listener != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onTimerFinish();
                            }
                        });
                    }
                } else {
                    final long timeLeft = remainingTime - elapsedTime;
                    Log.d(TAG, "Timer tick: " + timeLeft + "ms left");

                    if (listener != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) { // Double-check není zbytečné
                                    listener.onTimerTick(timeLeft);
                                }
                            }
                        });
                    }

                    // Aktualizuje notifikaci každých 5 sekund místo každou sekundu
                    if (elapsedTime % 5000 < 1000) {
                        updateNotification("Remaining: " + formatTime(timeLeft));
                    }
                }
            }
        }, 0, 1000);
    }

    public void pauseTimer() {
        Log.d(TAG, "Pausing timer");
        if (timer != null && !isPaused) {
            timer.cancel();
            timer = null;

            long currentTime = SystemClock.elapsedRealtime();
            long elapsedTime = currentTime - startTime;
            remainingTime = remainingTime - elapsedTime;
            pausedTime = currentTime;
            isPaused = true;

            if (wakeLock.isHeld()) {
                Log.d(TAG, "Releasing wake lock on pause");
                wakeLock.release();
            }

            updateNotification("Timer paused");
        }
    }

    public void resumeTimer() {
        Log.d(TAG, "Resuming timer with " + remainingTime + "ms remaining");
        if (isPaused) {
            startTime = SystemClock.elapsedRealtime();
            isPaused = false;

            if (!wakeLock.isHeld()) {
                Log.d(TAG, "Acquiring wake lock on resume");
                wakeLock.acquire(remainingTime + 5000);
            }

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long currentTime = SystemClock.elapsedRealtime();
                    long elapsedTime = currentTime - startTime;

                    if (elapsedTime >= remainingTime) {
                        Log.d(TAG, "Timer completed after resume");
                        stopTimer();
                        if (listener != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onTimerFinish();
                                }
                            });
                        }
                    } else {
                        final long timeLeft = remainingTime - elapsedTime;
                        Log.d(TAG, "Timer tick after resume: " + timeLeft + "ms left");

                        if (listener != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null) {
                                        listener.onTimerTick(timeLeft);
                                    }
                                }
                            });
                        }

                        // Aktualizuje notifikaci každých 5 sekund místo každou sekundu
                        if (elapsedTime % 5000 < 1000) {
                            updateNotification("Remaining: " + formatTime(timeLeft));
                        }
                    }
                }
            }, 0, 1000);
        }
    }

    public void stopTimer() {
        Log.d(TAG, "Stopping timer");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        isPaused = true;

        if (wakeLock.isHeld()) {
            Log.d(TAG, "Releasing wake lock on stop");
            wakeLock.release();
        }

        updateNotification("Timer stopped");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Round Timer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for Round Timer Service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        // Pro Android 12+ potřebujeme FLAG_IMMUTABLE
        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Round Timer")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String content) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification(content));
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "TimerService onDestroy");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
}
