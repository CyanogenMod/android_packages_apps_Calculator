package com.android.calculator2.floating;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.calculator2.R;
import com.android.calculator2.view.AdvancedDisplay;
import com.xlythe.floatingview2.FloatingView;
import com.xlythe.math.History;
import com.xlythe.math.Persist;
import com.xlythe.math.Solver;

public class FloatingCalculator extends FloatingView {
    // Calc logic
    private View.OnClickListener mListener;
    private AdvancedDisplay mDisplay;
    private ViewPager mPager;
    private Persist mPersist;
    private History mHistory;
    private Solver mSolver;

    public View inflateButton() {
        return View.inflate(getContext(), R.layout.floating_calculator_icon, null);
    }

    public View inflateView() {
        View child = View.inflate(getContext(), R.layout.floating_calculator, null);

        mPager = (ViewPager) child.findViewById(R.id.panelswitch);

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.getHistory();

        mDisplay = (AdvancedDisplay) child.findViewById(R.id.display);
//        mDisplay.setEditTextLayout(R.layout.view_calculator_edit_text_floating);
        mDisplay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyContent(mDisplay.getText());
                return true;
            }
        });

        mSolver = new Solver();
        final ImageButton del = (ImageButton) child.findViewById(R.id.delete);
        final ImageButton clear = (ImageButton) child.findViewById(R.id.clear);
        mListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v instanceof Button) {
                    if(((Button) v).getText().toString().equals("=")) {
                        // TODO on enter
                    } else if(v.getId() == R.id.parentheses) {
                        // TODO
                    } else if(((Button) v).getText().toString().length() >= 2) {
                       // TODO ((Button) v).getText().toString() + "(";
                    } else {
                        // TODO ((Button) v).getText().toString();
                    }
                } else if(v instanceof ImageButton) {
                    // TODO onDelete();
                }
//                del.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE ? View.VISIBLE : View.GONE);
//                clear.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_CLEAR ? View.VISIBLE : View.GONE);
            }
        };
        del.setOnClickListener(mListener);
        del.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO onClear();
                return true;
            }
        });
        clear.setOnClickListener(mListener);
        clear.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO onClear();
                return true;
            }
        });
//        del.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE ? View.VISIBLE : View.GONE);
//        clear.setVisibility(mLogic.getDeleteMode() == Logic.DELETE_MODE_CLEAR ? View.VISIBLE : View.GONE);

        FloatingCalculatorPageAdapter adapter = new FloatingCalculatorPageAdapter(getContext(), mListener, mHistory);
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
