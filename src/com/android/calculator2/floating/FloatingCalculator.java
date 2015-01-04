package com.android.calculator2.floating;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.calculator2.Calculator;
import com.android.calculator2.CalculatorExpressionEvaluator;
import com.android.calculator2.CalculatorExpressionTokenizer;
import com.android.calculator2.Clipboard;
import com.android.calculator2.R;
import com.android.calculator2.view.display.AdvancedDisplay;
import com.xlythe.floatingview.FloatingView;
import com.xlythe.math.Constants;
import com.xlythe.math.History;
import com.xlythe.math.Persist;


public class FloatingCalculator extends FloatingView {
    // Calc logic
    private View.OnClickListener mListener;
    private AdvancedDisplay mDisplay;
    private ImageButton mDelete;
    private ImageButton mClear;
    private ViewPager mPager;
    private Persist mPersist;
    private History mHistory;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;
    private State mState;

    private enum State {
        DELETE, CLEAR, ERROR;
    }

    public View inflateButton() {
        return View.inflate(getContext(), R.layout.floating_calculator_icon, null);
    }

    public View inflateView() {
        // Rebuild constants. If the user changed their locale, it won't kill the app
        // but it might change a decimal point from . to ,
        Constants.rebuildConstants();

        View child = View.inflate(getContext(), R.layout.floating_calculator, null);

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        mPager = (ViewPager) child.findViewById(R.id.panelswitch);

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.getHistory();

        mDisplay = (AdvancedDisplay) child.findViewById(R.id.display);
        mDisplay.setSolver(mEvaluator.getSolver());
        mDisplay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyContent(mDisplay.getText());
                return true;
            }
        });

        mDelete = (ImageButton) child.findViewById(R.id.delete);
        mClear = (ImageButton) child.findViewById(R.id.clear);
        mListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId() == R.id.delete) {
                    onDelete();
                }
                else if(v.getId() == R.id.clear) {
                    onClear();
                }
                else if(v instanceof Button) {
                    if(((Button) v).getText().toString().equals("=")) {
                        mEvaluator.evaluate(mDisplay.getText(), new CalculatorExpressionEvaluator.EvaluateCallback() {
                            @Override
                            public void onEvaluate(String expr, String result, int errorResourceId) {
                                if (errorResourceId != Calculator.INVALID_RES_ID) {
                                    onError(errorResourceId);
                                } else {
                                    setText(result);
                                }
                            }
                        });
                    } else if(v.getId() == R.id.parentheses) {
                        setText("(" + mDisplay.getText() + ")");
                    } else if(((Button) v).getText().toString().length() >= 2) {
                        onInsert(((Button) v).getText().toString() + "(");
                    } else {
                        onInsert(((Button) v).getText().toString());
                    }
                }
            }
        };
        mDelete.setOnClickListener(mListener);
        mDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onClear();
                return true;
            }
        });
        mClear.setOnClickListener(mListener);

        FloatingCalculatorPageAdapter adapter = new FloatingCalculatorPageAdapter(getContext(), mListener, mHistory);
        mPager.setAdapter(adapter);
        mPager.setCurrentItem(1);

        child.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        child.findViewById(R.id.display_wrapper).bringToFront();

        setState(State.DELETE);

        return child;
    }

    private void onDelete() {
        setState(State.DELETE);
        mDisplay.backspace();
    }

    private void onClear() {
        setState(State.DELETE);
        mDisplay.clear();
    }

    private void setText(String text) {
        setState(State.CLEAR);
        mDisplay.setText(text);
    }

    private void onInsert(String text) {
        if(mState != State.DELETE) {
            setText(text);
        }
        else {
            mDisplay.insert(text);
        }

        setState(State.DELETE);
    }

    private void onError(int resId) {
        setState(State.ERROR);
        mDisplay.setText(resId);
    }

    private void setState(State state) {
        mDelete.setVisibility(state == State.DELETE ? View.VISIBLE : View.GONE);
        mClear.setVisibility(state != State.DELETE ? View.VISIBLE : View.GONE);
        if(mState != state) {
            switch (state) {
                case CLEAR:
                    break;
                case DELETE:
                    mDisplay.setTextColor(getResources().getColor(R.color.display_formula_text_color));
                    break;
                case ERROR:
                    mDisplay.setTextColor(getResources().getColor(R.color.calculator_error_color));
                    break;
            }
            mState = state;
        }
    }

    private void copyContent(String text) {
        Clipboard.copy(getContext(), text);
    }
}
