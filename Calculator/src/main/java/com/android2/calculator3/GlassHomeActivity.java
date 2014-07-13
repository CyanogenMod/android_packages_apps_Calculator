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
                String spokenText = parseText(results.get(0));

                if(spokenText.isEmpty()) {
                    detectionFailed();
                    return;
                }

                Log.v("Calculator", "Glass user queried \"" + spokenText + "\"");

                Logic logic = new Logic(getBaseContext());
                logic.setLineLength(10);

                String result;
                try {
                    result = logic.evaluate(spokenText);
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

    private String parseText(String text) {
        EquationFormatter formatter = new EquationFormatter();
        List<String> exceptions = new LinkedList<String>();
        text = text.toLowerCase(Locale.US);
        text = text.replace("point", ".");
        text = text.replace("minus", "-");
        text = text.replace("plus", "+");
        text = text.replace("divided", "/");
        text = text.replace("over", "/");
        text = text.replace("times", "*");
        text = text.replace("x", "*");
        text = text.replace("multiplied", "*");
        text = text.replace("raise", "^");
        text = text.replace("square root", "sqrt(");
        exceptions.add("sqrt");
        text = text.replace("sign", "sin(");
        exceptions.add("sin");
        text = text.replace("cosine", "cos(");
        exceptions.add("cos");
        text = text.replace("tangent", "tan(");
        exceptions.add("tan");
        text = text.replace("pie", getString(R.string.pi));
        text = text.replace("pi", getString(R.string.pi));
        text = text.replace(" ", "");
        text = SpellContext.replaceAllWithNumbers(text);
        text = removeChars(text, exceptions);
        text = formatter.appendParenthesis(text);
        return text;
    }

    private String removeChars(String input, List<String> exceptions) {
        Pattern pattern = Pattern.compile("[a-z]");
        String text = "";
        for(int i = 0; i < input.length(); i++) {
            for(String ex : exceptions) {
                if(input.substring(i).startsWith(ex)) {
                    text += input.substring(i, i+ex.length());
                    i+=ex.length();
                    continue;
                }
            }

            Matcher matcher = pattern.matcher(input.substring(i, i+1));
            if(!matcher.matches()) text += input.substring(i, i+1);
        }
        return text;
    }
}
