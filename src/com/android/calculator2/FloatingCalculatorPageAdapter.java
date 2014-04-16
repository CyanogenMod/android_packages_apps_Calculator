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

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import com.android.calculator2.view.CalculatorDisplay;

public class FloatingCalculatorPageAdapter extends PagerAdapter {
    private final Context mContext;
    private final View.OnClickListener mListener;
    private final Logic mLogic;
    private final CalculatorDisplay mDisplay;
    private final History mHistory;
    private final View[] mViews = new View[3];

    public FloatingCalculatorPageAdapter(Context context, View.OnClickListener listener,
            History history, Logic logic, CalculatorDisplay display) {
        mContext = context;
        mListener = listener;
        mHistory = history;
        mLogic = logic;
        mDisplay = display;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return 3;
    }

    public View getViewAt(int position) {
        if (mViews[position] != null) {
            return mViews[position];
        }

        switch(position) {
            case 0:
                mViews[position] = View.inflate(
                        mContext, R.layout.floating_calculator_history, null);
                ListView historyView = (ListView) mViews[position].findViewById(R.id.history);
                setUpHistory(historyView);
                break;
            case 1:
                mViews[position] = View.inflate(
                        mContext, R.layout.floating_calculator_basic, null);
                break;
            case 2:
                mViews[position] = View.inflate(
                        mContext, R.layout.floating_calculator_advanced, null);
                break;
        }

        applyListener(mViews[position]);
        return mViews[position];
    }

    @Override
    public void startUpdate(View container) {
        // Do nothing here
    }

    @Override
    public Object instantiateItem(View container, int position) {
        View v = getViewAt(position);
        ((ViewGroup) container).addView(v);

        return v;
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        if (mViews[position] != null) {
            mViews[position] = null;
        }

        ((ViewGroup) container).removeView((View) object);
    }

    @Override
    public void finishUpdate(View container) {
        // Do nothing here
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        // Do nothing here
    }

    private void applyListener(View view) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                applyListener(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof Button) {
            view.setOnClickListener(mListener);
        } else if (view instanceof ImageButton) {
            view.setOnClickListener(mListener);
        }
    }

    private void setUpHistory(ListView historyView) {
        FloatingHistoryAdapter.OnHistoryItemClickListener listener =
                new FloatingHistoryAdapter.OnHistoryItemClickListener() {
            @Override
            public void onHistoryItemClick(HistoryEntry entry) {
                int deleteMode = mLogic.getDeleteMode();
                if (mDisplay.getText().isEmpty()) {
                    deleteMode = Logic.DELETE_MODE_CLEAR;
                }

                mDisplay.insert(entry.getEdited());
                mLogic.setDeleteMode(deleteMode);
            }
        };

        FloatingHistoryAdapter historyAdapter = new FloatingHistoryAdapter(mContext, mHistory);
        historyAdapter.setOnHistoryItemClickListener(listener);
        mHistory.setObserver(historyAdapter);
        historyView.setAdapter(historyAdapter);
        historyView.setStackFromBottom(true);
        historyView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    }
}
