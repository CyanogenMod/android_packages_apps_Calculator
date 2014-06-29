package com.android2.calculator3;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class GlassResultActivity extends Activity {
    public static String EXTRA_QUESTION;
    public static String EXTRA_RESULT;

    private String mQuestion;
    private String mResult;
    private boolean mIsTextToSpeechInit = false;
    private TextToSpeech mTextToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glass_result);

        mQuestion = getIntent().getStringExtra(EXTRA_QUESTION);
        mResult = getIntent().getStringExtra(EXTRA_RESULT);
        TextView resultView = (TextView) findViewById(R.id.result);
        resultView.setText(mQuestion + " = " + mResult);

        GestureDetector detector = new GestureDetector(this);
        detector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    ((AudioManager) getSystemService(AUDIO_SERVICE)).playSoundEffect(Sounds.TAP);
                    openOptionsMenu();
                    return true;
                }

                return false;
            }
        });
    }

    private void askNewQuestion() {
        startActivity(new Intent(getBaseContext(), GlassHomeActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_glass, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.repeat:
                askNewQuestion();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsTextToSpeechInit = false;
        mTextToSpeech = new TextToSpeech(getBaseContext(), new OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    mIsTextToSpeechInit = true;
                    speakResult();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mTextToSpeech != null) mTextToSpeech.shutdown();
    }

    private void speakResult() {
        if(mTextToSpeech != null && mIsTextToSpeechInit) {
            if(mResult.startsWith(String.valueOf(Logic.MINUS))) {
                // Speech can't say "-1". It says "1" instead.
                mResult = getString(R.string.speech_helper_negative, mResult.substring(1));
            }
            mTextToSpeech.speak(getString(R.string.speech_helper_equals, mQuestion, mResult), TextToSpeech.QUEUE_ADD, null);
        }
    }
}
