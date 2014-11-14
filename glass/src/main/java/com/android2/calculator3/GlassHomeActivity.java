package com.android2.calculator3;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.xlythe.math.EquationFormatter;
import com.xlythe.math.Solver;
import com.xlythe.math.Voice;

import org.javia.arity.SyntaxException;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlassHomeActivity extends Activity {
    private static final int SPEECH_REQUEST = 1000;
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glass_home);

        if(savedInstanceState == null) displaySpeechRecognizer();

        mGestureDetector = new GestureDetector(this);
        mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    ((AudioManager) getSystemService(AUDIO_SERVICE)).playSoundEffect(Sounds.TAP);
                    displaySpeechRecognizer();
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        onCreate(null);
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.glass_hint));
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SPEECH_REQUEST) {
            if(resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String spokenText = Voice.parseSpokenText(results.get(0));

                if(spokenText.isEmpty()) {
                    detectionFailed();
                    return;
                }

                Log.v("Calculator", "Glass user queried \"" + spokenText + "\"");

                Solver solver = new Solver();
                solver.setLineLength(10);

                String result;
                try {
                    result = solver.solve(spokenText);
                } catch(SyntaxException e) {
                    result = getString(R.string.error);
                }
                Intent intent = new Intent(this, GlassResultActivity.class);
                intent.putExtra(GlassResultActivity.EXTRA_QUERY, spokenText);
                intent.putExtra(GlassResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
                finish();
            }
            else if(resultCode == RESULT_CANCELED) {
                finish();
            }
            else {
                detectionFailed();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void detectionFailed() {
        findViewById(R.id.layout).setVisibility(View.VISIBLE);
    }

}
