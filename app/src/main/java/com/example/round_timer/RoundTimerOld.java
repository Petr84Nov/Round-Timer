package com.example.round_timer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.CountDownTimer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.round_timer.utils.Step;

import java.util.Locale;

public class RoundTimerOld {

    private TextView roundView, pauseViev,roundNumberView, actualView;
    private long roundTime, pauseTime, actualTime;
    private CountDownTimer timer;
    private Step step;
    private int roundNumber, actualRoundNumber;
    private ImageButton btnStart, btnRestore;
    private SoundPool soundPool;
    private int startSound, pauseSound, endSound;
    private Context context;


    public RoundTimerOld(Context context, TextView roundView, TextView pauseViev, TextView roundNumberView) {
        this.roundView = roundView;
        this.pauseViev = pauseViev;
        this.roundNumberView = roundNumberView;
        step = Step.TIMER_FINISHED;
        actualRoundNumber = 1;
        this.context = context;
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
        textView.setText(timeToString);
    }

    public void startTimer() {

        switch (step) {
            case TIMER_FINISHED:
                actualTime = pauseTime;
                switchViews();
                roundNumberView.setText(actualRoundNumber + " / " + roundNumber);
                setTime(pauseViev);
                actualTime = roundTime;
                actualView = roundView;
                step = Step.ROUND_RUNNING;
                soundPool.play(startSound, 1,1,0,0,1);
                break;
            case PAUSE_FINISHED:
                actualTime = roundTime;
                actualView = roundView;
                step = Step.ROUND_RUNNING;
                soundPool.play(startSound, 1,1,0,0,1);
                break;
            case ROUND_FINISHED:
                actualTime = pauseTime;
                actualView = pauseViev;
                step = Step.PAUSE_RUNNING;
                soundPool.play(pauseSound, 1,1,0,0,1);
                break;
            case ROUND_PAUSED:
                step = Step.ROUND_RUNNING;
                break;
            case PAUSE_PAUSED:
                step = Step.PAUSE_RUNNING;
                break;
            default:
                break;
        }

        timer = new CountDownTimer(actualTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                actualTime = millisUntilFinished;
                setTime(actualView);
            }

            @Override
            public void onFinish() {
                switch (step) {
                    case ROUND_RUNNING:
                        step = Step.ROUND_FINISHED;
                        if (actualRoundNumber < roundNumber) {
                            startTimer();
                            actualTime = roundTime;
                            setTime(roundView);
                        } else {
                            stopTimer();
                            soundPool.play(endSound, 1,1,0,0,1);
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
        }.start();
    }

    public void pauseTimer() {
        timer.cancel();
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
    }

    public void stopTimer() {
        timer.cancel();
        actualRoundNumber = 1;
        step = Step.TIMER_FINISHED;
        actualView = roundNumberView;
        actualView.setText(actualRoundNumber + " / " + roundNumber);
        actualTime = roundTime;
        setTime(roundView);
        actualTime = pauseTime;
        setTime(pauseViev);
        switchViews();

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

    public void setBtnStart(ImageButton btnStart) {
        this.btnStart = btnStart;
    }

    public void setBtnRestore(ImageButton btnRestore) {
        this.btnRestore = btnRestore;
    }

    public Step getStep() {
        return step;
    }

    public void switchViews() {
        if(context instanceof MainActivity) {
            ((MainActivity)context).switchViews();
        }
    }
}


