package com.example.round_timer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.round_timer.utils.KeyboardDetector;
import com.example.round_timer.utils.Step;
import com.example.round_timer.utils.TimerService;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, View.OnFocusChangeListener {

    private Button btnRoundsPlus, btnRoundsMinus, btnRoundMinutesPlus, btnRoundMinutesMinus, btnRoundSecondsPlus, btnRoundSecondsMinus, btnPauseMinutesPlus, btnPauseMinutesMinus,
            btnPauseSecondsPlus, btnPauseSecondsMinus, btnExit, btnReset, btnStart;

    private EditText edRounds, edRoundMinutes, edRoundSeconds, edPauseMinutes, edPauseSeconds;
    private int rounds, roundMinutes, roundSeconds, pauseMinutes, pauseSeconds;
    private TextView txtRoundNumber, txtRoundTime, txtPauseTime, txtRoundColon, txtPauseColon;
    private LinearLayout lyRoundMinutes, lyRoundSeconds, lyPauseMinutes, lyPauseSeconds;
    private DecimalFormat formatter;
    private RoundTimer timer;

    private boolean isPlus;
    private int value;

    private EditText runnableText;

    private long roundTime, pauseTime;
    private Handler handler;
    private KeyboardDetector keyboardDetector;
    //private boolean DEBUG = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        if (DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }
         */

        setVolumeControlStream(AudioManager.STREAM_MUSIC);//při stisku volume tlačítek reaguje jako media

        init();
        loadData();

        /*
        View contentView = findViewById(android.R.id.content);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            contentView.getWindowVisibleDisplayFrame(r);
            int screenHeight = contentView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            Log.d("KeyboardTest", "Keypad height: " + keypadHeight);

            if (keypadHeight > 100) {
                Toast.makeText(MainActivity.this, "Keyboard is visible", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Keyboard is invisible", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private void saveData() {
        SharedPreferences saveData = getSharedPreferences(getString(R.string.data), MODE_PRIVATE);
        SharedPreferences.Editor editor = saveData.edit();
        editor.putInt(getString(R.string.rounds_data), rounds);
        editor.putInt(getString(R.string.round_minutes), roundMinutes);
        editor.putInt(getString(R.string.round_seconds), roundSeconds);
        editor.putInt(getString(R.string.pause_minutes), pauseMinutes);
        editor.putInt(getString(R.string.pause_seconds), pauseSeconds);
        editor.apply();
    }

    private void loadData() {
        SharedPreferences loadData = getSharedPreferences(getString(R.string.data), MODE_PRIVATE);
        rounds = loadData.getInt(getString(R.string.rounds_data), 1);
        roundMinutes = loadData.getInt(getString(R.string.round_minutes), 1);
        roundSeconds = loadData.getInt(getString(R.string.round_seconds), 0);
        pauseMinutes = loadData.getInt(getString(R.string.pause_minutes), 1);
        pauseSeconds = loadData.getInt(getString(R.string.pause_seconds), 0);

        edRounds.setText(formatter.format(rounds));
        edRoundMinutes.setText(formatter.format(roundMinutes));
        edRoundSeconds.setText(formatter.format(roundSeconds));
        edPauseMinutes.setText(formatter.format(pauseMinutes));
        edPauseSeconds.setText(formatter.format(pauseSeconds));
    }

    private void saveTime() {
        rounds = Integer.parseInt(edRounds.getText().toString());
        roundMinutes = Integer.parseInt(edRoundMinutes.getText().toString());
        roundSeconds = Integer.parseInt(edRoundSeconds.getText().toString());
        pauseMinutes = Integer.parseInt(edPauseMinutes.getText().toString());
        pauseSeconds = Integer.parseInt(edPauseSeconds.getText().toString());

        saveData();

        roundTime = (roundMinutes * 60000) + (roundSeconds * 1000);
        pauseTime = (pauseMinutes * 60000) + (pauseSeconds * 1000);

        timer.setRoundNumber(rounds);
        timer.setRoundTime(roundTime);
        timer.setPauseTime(pauseTime);
    }

    private void startButtonClick() {
        unfocusEditTexts();
        if (btnRoundsPlus.getVisibility() == View.VISIBLE) {
            saveTime();
            if (roundTime == 0) {       Toast.makeText(MainActivity.this, R.string.round_time_zero, Toast.LENGTH_SHORT).show();     }
            else if (pauseTime == 0) {  Toast.makeText(MainActivity.this, R.string.pause_time_zero, Toast.LENGTH_SHORT).show();     }
            else {

                timer.startTimer();
            }
        } else {
            if (timer.getStep() == Step.TIMER_FINISHED || timer.getStep() == Step.ROUND_PAUSED || timer.getStep() == Step.PAUSE_PAUSED) {
                timer.startTimer();
                btnStart.setText(R.string.pause);
            } else {
                timer.pauseTimer();
                btnStart.setText(R.string.start);
            }
        }
    }

    private void resetButtonClick() {
        if (btnRoundsPlus.getVisibility() == View.VISIBLE) {
            edRounds.setText(R.string._01);
            edRoundMinutes.setText(R.string._01);
            edRoundSeconds.setText(R.string._00);
            edPauseMinutes.setText(R.string._01);
            edPauseSeconds.setText(R.string._00);
            saveTime();
        } else {
            timer.stopTimer();
        }
    }

    public void switchViews() {
        if (btnRoundsPlus.getVisibility() == View.VISIBLE) {
            btnRoundsPlus.setVisibility(View.GONE);
            btnRoundsMinus.setVisibility(View.GONE);
            edRounds.setVisibility(View.GONE);
            txtRoundNumber.setVisibility(View.VISIBLE);

            lyRoundMinutes.setVisibility(View.GONE);
            lyRoundSeconds.setVisibility(View.GONE);
            txtRoundColon.setVisibility(View.GONE);
            txtRoundTime.setVisibility(View.VISIBLE);

            lyPauseMinutes.setVisibility(View.GONE);
            lyPauseSeconds.setVisibility(View.GONE);
            txtPauseColon.setVisibility(View.GONE);
            txtPauseTime.setVisibility(View.VISIBLE);

            btnStart.setText(R.string.pause);
            btnExit.setVisibility(View.INVISIBLE);
        } else {
            btnRoundsPlus.setVisibility(View.VISIBLE);
            btnRoundsMinus.setVisibility(View.VISIBLE);
            edRounds.setVisibility(View.VISIBLE);
            txtRoundNumber.setVisibility(View.GONE);

            lyRoundMinutes.setVisibility(View.VISIBLE);
            lyRoundSeconds.setVisibility(View.VISIBLE);
            txtRoundColon.setVisibility(View.VISIBLE);
            txtRoundTime.setVisibility(View.GONE);

            lyPauseMinutes.setVisibility(View.VISIBLE);
            lyPauseSeconds.setVisibility(View.VISIBLE);
            txtPauseColon.setVisibility(View.VISIBLE);
            txtPauseTime.setVisibility(View.GONE);

            btnStart.setText(R.string.start);
            btnExit.setVisibility(View.VISIBLE);
        }

    }

    private Runnable runNumbers(View view) {
        Button button = (Button)view;
        runnableText = edRounds;
        isPlus = true;

        if (button.getId() == R.id.btnRoundsPlus) {
            runnableText = edRounds;
            isPlus = true;
        }
        else if (button.getId() == R.id.btnRoundsMinus) {
            runnableText = edRounds;
            isPlus = false;
        }
        else if (button.getId() == R.id.btnRoundMinutesPlus) {
            runnableText = edRoundMinutes;
            isPlus = true;
        }
        else if (button.getId() == R.id.btnRoundMinutesMinus) {
            runnableText = edRoundMinutes;
            isPlus = false;
        }
        else if (button.getId() == R.id.btnRoundSecondsPlus) {
            runnableText = edRoundSeconds;
            isPlus = true;
        }
        else if (button.getId() == R.id.btnRoundSecondsMinus) {
            runnableText = edRoundSeconds;
            isPlus = false;
        }
        else if (button.getId() == R.id.btnPauseMinutesPlus) {
            runnableText = edPauseMinutes;
            isPlus = true;
        }
        else if (button.getId() == R.id.btnPauseMinutesMinus) {
            runnableText = edPauseMinutes;
            isPlus = false;
        }
        else if (button.getId() == R.id.btnPauseSecondsPlus) {
            runnableText = edPauseSeconds;
            isPlus = true;
        }
        else if (button.getId() == R.id.btnPauseSecondsMinus) {
            runnableText = edPauseSeconds;
            isPlus = false;
        }


        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                value = Integer.parseInt(runnableText.getText().toString());
                if (isPlus) {       value++;    }
                else {      value--;    }
                if (runnableText.getId() == edRoundSeconds.getId() || runnableText.getId() == edPauseSeconds.getId()) {
                    if (value > 59) {    value = 59;   }
                }
                else if(runnableText.getId() == edRounds.getId()) {
                    if (value < 1) {       value = 1;     }
                }
                if (value < 0) {     value = 0;   }
                runnableText.setText(formatter.format(value));
                handler.postDelayed(this, 200);
            }
        };
        return runnable;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == btnStart.getId()) {
            startButtonClick();
        } else if (id == btnReset.getId()) {
            resetButtonClick();
        } else if (id == btnExit.getId()) {
            onBackPressed();
        }
    }
    private void init() {
        btnRoundsPlus = findViewById(R.id.btnRoundsPlus);
        btnRoundsMinus = findViewById(R.id.btnRoundsMinus);
        btnRoundMinutesPlus = findViewById(R.id.btnRoundMinutesPlus);
        btnRoundMinutesMinus = findViewById(R.id.btnRoundMinutesMinus);
        btnRoundSecondsPlus = findViewById(R.id.btnRoundSecondsPlus);
        btnRoundSecondsMinus = findViewById(R.id.btnRoundSecondsMinus);
        btnPauseMinutesPlus = findViewById(R.id.btnPauseMinutesPlus);
        btnPauseMinutesMinus = findViewById(R.id.btnPauseMinutesMinus);
        btnPauseSecondsPlus = findViewById(R.id.btnPauseSecondsPlus);
        btnPauseSecondsMinus = findViewById(R.id.btnPauseSecondsMinus);

        btnExit = findViewById(R.id.btnExit);
        btnReset = findViewById(R.id.btnReset);
        btnStart = findViewById(R.id.btnStart);

        edRounds = findViewById(R.id.edRounds);
        edRoundMinutes = findViewById(R.id.edRoundMinutes);
        edRoundSeconds = findViewById(R.id.edRoundSeconds);
        edPauseMinutes = findViewById(R.id.edPauseMinutes);
        edPauseSeconds = findViewById(R.id.edPauseSeconds);

        txtRoundNumber = findViewById(R.id.txtRoundNumber);
        txtRoundTime = findViewById(R.id.txtRoundTime);
        txtPauseTime = findViewById(R.id.txtPauseTime);
        txtRoundColon = findViewById(R.id.txtRoundColon);
        txtPauseColon = findViewById(R.id.txtPauseColon);

        lyRoundMinutes = findViewById(R.id.lyRoundMinutes);
        lyRoundSeconds = findViewById(R.id.lyRoundSeconds);
        lyPauseMinutes = findViewById(R.id.lyPauseMinutes);
        lyPauseSeconds = findViewById(R.id.lyPauseSeconds);

        formatter = new DecimalFormat("00");
        timer = new RoundTimer(this, txtRoundTime, txtPauseTime, txtRoundNumber);
        handler = new Handler();

        btnRoundsPlus.setOnTouchListener(this);
        btnRoundsMinus.setOnTouchListener(this);
        btnRoundMinutesPlus.setOnTouchListener(this);
        btnRoundMinutesMinus.setOnTouchListener(this);
        btnRoundMinutesMinus.setOnTouchListener(this);
        btnRoundSecondsPlus.setOnTouchListener(this);
        btnRoundSecondsMinus.setOnTouchListener(this);
        btnPauseMinutesPlus.setOnTouchListener(this);
        btnPauseMinutesMinus.setOnTouchListener(this);
        btnPauseSecondsPlus.setOnTouchListener(this);
        btnPauseSecondsMinus.setOnTouchListener(this);

        btnExit.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnStart.setOnClickListener(this);



        edRounds.setOnFocusChangeListener(this);
        edPauseMinutes.setOnFocusChangeListener(this);
        edPauseSeconds.setOnFocusChangeListener(this);
        edRoundMinutes.setOnFocusChangeListener(this);
        edRoundSeconds.setOnFocusChangeListener(this);

        keyboardDetector = new KeyboardDetector(this);
        keyboardDetector.setKeyboardListener(new KeyboardDetector.KeyboardListener() {
            @Override
            public void onKeyboardOpen(View focusedView) {
                    
            }

            @Override
            public void onKeyboardClose(View focusedView) {
                if (focusedView != null && focusedView instanceof EditText) {
                    EditText text = (EditText) focusedView;
                    if (focusedView != null) {
                        if (text.getText().toString().isEmpty()) {
                            if (text.getId() == edRounds.getId()) {
                                text.setText("01");
                            } else {
                                text.setText("00");
                            }
                        } else {
                            try {
                                int i = Integer.parseInt(text.getText().toString());
                                if (text.getId() == R.id.edRoundSeconds || text.getId() == R.id.edPauseSeconds) {   if (i > 59) {         i = 59;       }   }
                                else if (text.getId() == R.id.edRounds && i < 1) {      i = 1;     }
                                text.setText(formatter.format(i));
                            } catch (NumberFormatException e) {
                                if (text.getId() == R.id.edRounds) {        text.setText("01");     }
                                else {  text.setText("00");     }
                            }

                        }
                    }
                }
            }
        });
    }



    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                runNumbers(view).run();
                return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacksAndMessages(null);
        }
        return false;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        EditText text = (EditText) view;
        if (hasFocus) {
            //text.selectAll();

            displayKeyboard(text);
            text.setText("");
        }
          else {
            if (text.getText().toString().equals("")) {
                if (text.getId() == edRounds.getId()) {
                    text.setText("01");
                } else {
                    text.setText("00");
                }
            } else {
                int i = Integer.parseInt(text.getText().toString());
                if (view.getId() == R.id.edRoundSeconds || view.getId() == R.id.edPauseSeconds) {   if (i > 59) {         i = 59;       }   }
                text.setText(formatter.format(i));
            }
        }
    }

    private void displayKeyboard(EditText edText){
        //edText.requestFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInputFromWindow(edText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
        imm.showSoftInput(edText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);

        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void unfocusEditTexts() {
        hideKeyboard();
        edRounds.clearFocus();
        if (edRounds.getText().toString().equals("")) {       edRounds.setText("01");      }
        edRoundMinutes.clearFocus();
        if (edRoundMinutes.getText().toString().equals("")) {       edRoundMinutes.setText("00");      }
        edRoundSeconds.clearFocus();
        if (edRoundSeconds.getText().toString().equals("")) {       edRoundSeconds.setText("00");      }
        edPauseMinutes.clearFocus();
        if (edPauseMinutes.getText().toString().equals("")) {       edPauseMinutes.setText("00");      }
        edPauseSeconds.clearFocus();
        if (edPauseSeconds.getText().toString().equals("")) {       edPauseSeconds.setText("00");      }
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cleanup();
        }

        Intent intent = new Intent(this, TimerService.class);
        stopService(intent);

        if (keyboardDetector != null) {
            keyboardDetector.release();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (btnRoundsPlus.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else {
                    timer.stopTimer();
            }

        }


}