package com.android2.calculator3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android2.calculator3.view.CalculatorDisplay;
import com.android2.calculator3.view.CalculatorViewPager;
import com.xlythe.engine.theme.Theme;
import com.xlythe.floatingview.FloatingView;

public class FloatingCalculator extends FloatingView {
    // Calc logic
    private View.OnClickListener mListener;
    private CalculatorDisplay mDisplay;
    private CalculatorViewPager mPager;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;

    public View inflateButton() {
        return View.inflate(getContext(), R.layout.floating_calculator_icon, null);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up theme engine (the display uses it, but most of it should be turned off. this is just in case)
        Theme.buildResourceMap(com.android2.calculator3.R.class);
        Theme.setPackageName(CalculatorSettings.getTheme(getContext()));
    }

    public View inflateView() {
        View child = View.inflate(getContext(), R.layout.floating_calculator, null);

        mPager = (CalculatorViewPager) child.findViewById(R.id.panelswitch);

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.mHistory;

        mDisplay = (CalculatorDisplay) child.findViewById(R.id.display);
        mDisplay.setEditTextLayout(R.layout.view_calculator_edit_text_floating);
        mDisplay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyContent(mDisplay.getText());
                return true;
            }
        });

        mLogic = new Logic(this, mDisplay);
        mLogic.setHistory(mHistory);
        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());
        mLogic.resumeWithHistory();
        final ImageButton del = (ImageButton) child.findViewById(R.id.delete);
        final ImageButton clear = (ImageButton) child.findViewById(R.id.clear);
        mListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v instanceof Button) {
                    if(((Button) v).getText().toString().equals("=")) {
                        mLogic.onEnter();
                    } else if(v.getId() == R.id.parentheses) {
                        if(mLogic.isError()) mLogic.setText("");
                        mLogic.setText("(" + mLogic.getText() + ")");
                    } else if(((Button) v).getText().toString().length() >= 2) {
                        mLogic.insert(((Button) v).getText().toString() + "(");
                    } else {
                        mLogic.insert(((Button) v).getText().toString());
                    }
                } else if(v instanceof ImageButton) {
                    mLogic.onDelete();
                }
                del.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE ? View.VISIBLE : View.GONE);
                clear.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_CLEAR ? View.VISIBLE : View.GONE);
            }
        };
        del.setOnClickListener(mListener);
        del.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mLogic.onClear();
                return true;
            }
        });
        clear.setOnClickListener(mListener);
        clear.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mLogic.onClear();
                return true;
            }
        });
        del.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE ? View.VISIBLE : View.GONE);
        clear.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_CLEAR ? View.VISIBLE : View.GONE);

        FloatingCalculatorPageAdapter adapter = new FloatingCalculatorPageAdapter(getContext(), mListener, mHistory, mLogic, mDisplay);
        mPager.setAdapter(adapter);
        mPager.setCurrentItem(1);

        return child;
    }

    private void copyContent(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
        String toastText = String.format(getResources().getString(R.string.text_copied_toast), text);
        Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
    }
}
