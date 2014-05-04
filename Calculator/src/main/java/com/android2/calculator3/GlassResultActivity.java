package com.android2.calculator3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

public class GlassResultActivity extends Activity {
    public static String EXTRA_RESULT;

    private List<Card> mCards;
    private CardScrollView mCardScrollView;
    private String mResult;
    private boolean mIsTextToSpeechInit = false;
    private TextToSpeech mTextToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResult = getIntent().getStringExtra(EXTRA_RESULT);

        createCards();

        mCardScrollView = new CardScrollView(this);
        ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(adapter);
        mCardScrollView.activate();
        mCardScrollView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                startActivity(new Intent(getBaseContext(), GlassHomeActivity.class));
                finish();
            }
        });
        setContentView(mCardScrollView);
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
            mTextToSpeech.speak(mResult, TextToSpeech.QUEUE_ADD, null);
        }
    }

    private void createCards() {
        mCards = new ArrayList<Card>();

        Card card;

        card = new Card(this);
        card.setText(mResult);
        card.setFootnote(R.string.voice_detection_repeat);
        mCards.add(card);
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
