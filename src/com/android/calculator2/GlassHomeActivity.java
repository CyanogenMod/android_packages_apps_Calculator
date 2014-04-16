/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.javia.arity.SyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class GlassHomeActivity extends Activity {
    private static final int SPEECH_REQUEST = 1000;
    private List<Card> mCards = new ArrayList<Card>();
    private CardScrollView mCardScrollView;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardScrollView = new CardScrollView(this);
        ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(adapter);
        mCardScrollView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                mCards.clear();
                mCardScrollView.getAdapter().notifyDataSetChanged();
                displaySpeechRecognizer();
            }
        });

        mCardScrollView.activate();
        setContentView(mCardScrollView);

        if(savedInstanceState == null) {
            displaySpeechRecognizer();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        onCreate(null);
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                String spokenText = results.get(0).toLowerCase(Locale.US).replace("point", ".")
                        .replace("minus", "-").replace("plus", "+").replace("divided by", "/")
                        .replace("times", "*").replace("x", "*").replace(" ", "");
                spokenText = SpellContext.replaceAllWithNumbers(spokenText);
                spokenText = spokenText.replaceAll("[a-z]", "");

                Log.v("Calculator", "Glass user queried \"" + spokenText + "\"");

                Logic logic = new Logic(getBaseContext());
                logic.setLineLength(100);

                String result;
                try {
                    result = logic.evaluate(spokenText);
                } catch (SyntaxException e) {
                    result = getString(R.string.error);
                }

                Intent intent = new Intent(this, GlassResultActivity.class);
                intent.putExtra(GlassResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
                finish();
            } else {
                detectionFailed();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void detectionFailed() {
        mCards.clear();

        Card card = new Card(this);
        card.setText(R.string.voice_detection_failed);
        mCards.add(card);
        mCardScrollView.getAdapter().notifyDataSetChanged();
    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {
        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView();
        }

        @Override
        public int getPosition(Object obj) {
            return mCards.indexOf(obj);
        }
    }
}
