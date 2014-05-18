package com.android2.calculator3;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import com.android2.calculator3.view.CalculatorDisplay;

public class FloatingCalculatorPageAdapter extends PagerAdapter {
    private final Context mContext;
    private final View.OnClickListener mListener;
    private final Logic mLogic;
    private final CalculatorDisplay mDisplay;
    private final History mHistory;
    private final View[] mViews = new View[3];

    public FloatingCalculatorPageAdapter(Context context, View.OnClickListener listener, History history, Logic logic, CalculatorDisplay display) {
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

    @Override
    public void startUpdate(View container) {
    }

    @Override
    public Object instantiateItem(View container, int position) {
        View v = getViewAt(position);
        ((ViewGroup) container).addView(v);

        return v;
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        if(mViews[position] != null) mViews[position] = null;
        ((ViewGroup) container).removeView((View) object);
    }

    @Override
    public void finishUpdate(View container) {
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
    }

    public View getViewAt(int position) {
        if(mViews[position] != null) return mViews[position];
        switch(position) {
            case 0:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_history, null);
                ListView historyView = (ListView) mViews[position].findViewById(R.id.history);
                setUpHistory(historyView);
                break;
            case 1:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_basic, null);
                break;
            case 2:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_advanced, null);
                break;
        }
        applyListener(mViews[position]);
        return mViews[position];
    }

    private void applyListener(View view) {
        if(view instanceof ViewGroup) {
            for(int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                applyListener(((ViewGroup) view).getChildAt(i));
            }
        } else if(view instanceof Button) {
            view.setOnClickListener(mListener);
        } else if(view instanceof ImageButton) {
            view.setOnClickListener(mListener);
        }
    }

    private void setUpHistory(ListView historyView) {
        FloatingHistoryAdapter.OnHistoryItemClickListener listener = new FloatingHistoryAdapter.OnHistoryItemClickListener() {
            @Override
            public void onHistoryItemClick(HistoryEntry entry) {
                int deleteMode = mLogic.getDeleteMode();
                if(mDisplay.getText().isEmpty()) deleteMode = Logic.DELETE_MODE_CLEAR;
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
