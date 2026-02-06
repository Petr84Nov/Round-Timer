package com.example.round_timer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.round_timer.utils.Step;
import com.example.round_timer.utils.TimerService;

import java.util.Locale;

public class RoundTimer {
    private static final String TAG = "RoundTimer";

    private TextView roundView, pauseViev, roundNumberView, actualView;
    private long roundTime, pauseTime, actualTime;
    private Step step;
    private int roundNumber, actualRoundNumber;
    private SoundPool soundPool;
    private int startSound, pauseSound, endSound;
    private Context context;

    // Service related fields
    private TimerService timerService;
    private boolean bound = false;
    private boolean serviceStarted = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            bound = true;

            // Pokud máme požadavek na start časovače, ale služba nebyla připojena, spustíme ji nyní
            if (serviceStarted) {
                startTimerWithService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            bound = false;
        }
    };

    public RoundTimer(Context context, TextView roundView, TextView pauseViev, TextView roundNumberView) {
        this.roundView = roundView;
        this.pauseViev = pauseViev;
        this.roundNumberView = roundNumberView;
        step = Step.TIMER_FINISHED;
        actualRoundNumber = 1;
        this.context = context;

        // Spusť a připoj službu
        Intent intent = new Intent(context, TimerService.class);
        context.startService(intent);
        Log.d(TAG, "Starting and binding to service");
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAtributes = new AudioAttributes.Builder().build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(audioAtributes)
                    .setMaxStreams(3)
                    .build();
        } else {
            soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        }

        startSound = soundPool.load(context, R.raw.start_gong, 1);
        pauseSound = soundPool.load(context, R.raw.pause_gong, 1);
        endSound = soundPool.load(context, R.raw.end_gong, 1);
    }

    private void setTime(TextView textView) {
        int minutes = (int) (actualTime / 1000) / 60;
        int seconds = (int) (actualTime / 1000) % 60;
        String timeToString = String.format(Locale.ENGLISH, "%02d : %02d", minutes, seconds);
        Log.d(TAG, "Setting time: " + timeToString + " to view: " + textView);
        textView.setText(timeToString);
    }

    public void startTimer() {
        if (!bound) {
            Log.d(TAG, "Service not bound, marking for start when bound");
            serviceStarted = true;
            // Spusť a připoj službu znovu, pokud není připojená
            Intent intent = new Intent(context, TimerService.class);
            context.startService(intent);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            return;
        }

        serviceStarted = true;

        switch (step) {
            case TIMER_FINISHED:
                actualTime = pauseTime;
                switchViews();
                roundNumberView.setText(actualRoundNumber + " / " + roundNumber);
                setTime(pauseViev);
                actualTime = roundTime;
                actualView = roundView;
                step = Step.ROUND_RUNNING;
                soundPool.play(startSound, 1, 1, 0, 0, 1);
                startTimerWithService();
                break;
            case PAUSE_FINISHED:
                actualTime = roundTime;
                actualView = roundView;
                step = Step.ROUND_RUNNING;
                soundPool.play(startSound, 1, 1, 0, 0, 1);
                startTimerWithService();
                break;
            case ROUND_FINISHED:
                actualTime = pauseTime;
                actualView = pauseViev;
                step = Step.PAUSE_RUNNING;
                soundPool.play(pauseSound, 1, 1, 0, 0, 1);
                startTimerWithService();
                break;
            case ROUND_PAUSED:
                step = Step.ROUND_RUNNING;
                timerService.resumeTimer();
                break;
            case PAUSE_PAUSED:
                step = Step.PAUSE_RUNNING;
                timerService.resumeTimer();
                break;
            default:
                break;
        }
    }

    private void startTimerWithService() {
        Log.d(TAG, "Starting timer with service for " + actualTime + "ms");
        timerService.startTimer(actualTime, new TimerService.TimerUpdateListener() {
            @Override
            public void onTimerTick(final long millisUntilFinished) {
                Log.d(TAG, "Timer tick: " + millisUntilFinished);
                actualTime = millisUntilFinished;
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (actualView != null) {
                                setTime(actualView);
                            } else {
                                Log.e(TAG, "actualView is null");
                            }
                        }
                    });
                }
            }

            @Override
            public void onTimerFinish() {
                Log.d(TAG, "Timer finished");
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleTimerFinish();
                        }
                    });
                }
            }
        });
    }

    private void handleTimerFinish() {
        Log.d(TAG, "Handling timer finish, step: " + step);
        switch (step) {
            case ROUND_RUNNING:
                step = Step.ROUND_FINISHED;
                if (actualRoundNumber < roundNumber) {
                    startTimer();
                    actualTime = roundTime;
                    setTime(roundView);
                } else {
                    stopTimer();
                    soundPool.play(endSound, 1, 1, 0, 0, 1);
                }
                break;
            case PAUSE_RUNNING:
                step = Step.PAUSE_FINISHED;
                if (actualRoundNumber <= roundNumber) {
                    actualRoundNumber++;
                    roundNumberView.setText(actualRoundNumber + " / " + roundNumber);
                    actualTime = pauseTime;
                    setTime(pauseViev);
                    startTimer();
                }
                break;
            default:
                break;
        }
    }

    public void pauseTimer() {
        Log.d(TAG, "Pausing timer");
        if (bound) {
            timerService.pauseTimer();
            switch (step) {
                case ROUND_RUNNING:
                    step = Step.ROUND_PAUSED;
                    break;
                case PAUSE_RUNNING:
                    step = Step.PAUSE_PAUSED;
                    break;
                default:
                    break;
            }
        } else {
            Log.e(TAG, "Cannot pause timer: service not bound");
        }
    }

    public void stopTimer() {
        Log.d(TAG, "Stopping timer");
        serviceStarted = false;
        if (bound) {
            timerService.stopTimer();
            actualRoundNumber = 1;
            step = Step.TIMER_FINISHED;
            actualView = roundNumberView;
            actualView.setText(actualRoundNumber + " / " + roundNumber);
            actualTime = roundTime;
            setTime(roundView);
            actualTime = pauseTime;
            setTime(pauseViev);
            switchViews();
        } else {
            Log.e(TAG, "Cannot stop timer: service not bound");
        }
    }

    public void setRoundTime(long roundTime) {
        this.roundTime = roundTime;
    }

    public void setPauseTime(long pauseTime) {
        this.pauseTime = pauseTime;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public void setActualRoundNumber(int actualRoundNumber) {
        this.actualRoundNumber = actualRoundNumber;
    }

    public Step getStep() {
        return step;
    }

    public void switchViews() {
        if (context instanceof MainActivity) {
            ((MainActivity) context).switchViews();
        }
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up timer resources");
        serviceStarted = false;
        if (bound) {
            try {
                context.unbindService(serviceConnection);
                bound = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding service", e);
            }
        }
    }

}
