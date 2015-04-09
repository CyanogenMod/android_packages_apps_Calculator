/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.calculator2;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

import com.android.calculator2.view.GraphView;
import com.android.calculator2.view.MultiButton;
import com.android.calculator2.view.display.AdvancedDisplay.OnTextSizeChangeListener;
import com.android.calculator2.CalculatorExpressionEvaluator.EvaluateCallback;
import com.android.calculator2.view.display.AdvancedDisplay;
import com.android.calculator2.view.DisplayOverlay;
import com.android.calculator2.view.DisplayOverlay.DisplayMode;
import com.android.calculator2.view.MatrixEditText;
import com.android.calculator2.view.MatrixInverseView;
import com.android.calculator2.view.MatrixTransposeView;
import com.android.calculator2.view.MatrixView;
import com.xlythe.math.Base;
import com.xlythe.math.Constants;
import com.xlythe.math.GraphModule;
import com.xlythe.math.History;
import com.xlythe.math.HistoryEntry;
import com.xlythe.math.Persist;

public class Calculator extends Activity
        implements OnTextSizeChangeListener, EvaluateCallback, OnLongClickListener {

    private static final String NAME = Calculator.class.getName();
    public static final String TAG = "Calculator";

    // instance state keys
    private static final String KEY_CURRENT_STATE = NAME + "_currentState";
    private static final String KEY_CURRENT_EXPRESSION = NAME + "_currentExpression";
    private static final String KEY_BASE = NAME + "_base";
    private static final String KEY_DISPLAY_MODE = NAME + "_displayMode";

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;

    private enum CalculatorState {
        INPUT, EVALUATE, RESULT, ERROR
    }

    private final TextWatcher mFormulaTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable editable) {
            setState(CalculatorState.INPUT);
            mEvaluator.evaluate(editable, Calculator.this);

            if (editable.toString().contains(mX)) {
                mEqualsGraphButton.setEnabled(R.id.graph);
            } else {
                mEqualsGraphButton.setEnabled(R.id.eq);
            }
        }
    };

    private final OnKeyListener mFormulaOnKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        View v = mEqualsGraphButton.getEnabledView();
                        mCurrentButton = v;
                        if (v != null) {
                            switch (v.getId()) {
                                case R.id.eq:
                                    onEquals();
                                    break;
                                case R.id.graph:
                                    onGraph();
                                    break;
                            }
                        }
                    }
                    // ignore all other actions
                    return true;
            }
            return false;
        }
    };

    private CalculatorState mCurrentState;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;
    private DisplayOverlay mDisplayView;
    private AdvancedDisplay mFormulaEditText;
    private AdvancedDisplay mResultEditText;
    private CalculatorPadViewPager mPadViewPager;
    private View mDeleteButton;
    private View mClearButton;
    private View mCurrentButton;
    private MultiButton mEqualsGraphButton;
    private Animator mCurrentAnimator;
    private History mHistory;
    private RecyclerView.Adapter mHistoryAdapter;
    private Persist mPersist;
    private NumberBaseManager mBaseManager;
    private String mX;
    private GraphController mGraphController;
    private FrameLayout.LayoutParams mLayoutParams =
            new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        mX = getString(R.string.X);
        mDisplayView = (DisplayOverlay) findViewById(R.id.display);
        mFormulaEditText = (AdvancedDisplay) findViewById(R.id.formula);
        mResultEditText = (AdvancedDisplay) findViewById(R.id.result);
        mPadViewPager = (CalculatorPadViewPager) findViewById(R.id.pad_pager);
        mDeleteButton = findViewById(R.id.del);
        mClearButton = findViewById(R.id.clr);
        mEqualsGraphButton =
                (MultiButton)findViewById(R.id.pad_numeric).findViewById(R.id.equals_graph);

        if (mEqualsGraphButton == null ||
                mEqualsGraphButton.getVisibility() != View.VISIBLE) {
            mEqualsGraphButton =
                    (MultiButton)findViewById(R.id.pad_operator)
                            .findViewById(R.id.equals_graph);
        }

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        savedInstanceState = savedInstanceState == null ? Bundle.EMPTY : savedInstanceState;
        setState(CalculatorState.values()[
                savedInstanceState.getInt(KEY_CURRENT_STATE, CalculatorState.INPUT.ordinal())]);

        mFormulaEditText.setSolver(mEvaluator.getSolver());
        mResultEditText.setSolver(mEvaluator.getSolver());

        Base base = Base.DECIMAL;
        int baseOrdinal = savedInstanceState.getInt(KEY_BASE, -1);
        if (baseOrdinal != -1) {
            base = Base.values()[baseOrdinal];
        }
        mBaseManager = new NumberBaseManager(base);
        if (mPadViewPager != null) {
            mPadViewPager.setBaseManager(mBaseManager);
        }
        setBase(base);

        mFormulaEditText.addTextChangedListener(mFormulaTextWatcher);
        mFormulaEditText.setOnKeyListener(mFormulaOnKeyListener);
        mFormulaEditText.setOnTextSizeChangeListener(this);
        mFormulaEditText.setText(mTokenizer.getLocalizedExpression(
                savedInstanceState.getString(KEY_CURRENT_EXPRESSION, "")));
        if (TextUtils.isEmpty(mFormulaEditText.getText())) {
            mEqualsGraphButton.setEnabled(R.id.eq);
        }

        mEvaluator.evaluate(mFormulaEditText.getText(), this);
        mFormulaEditText.setTextColor(getResources().getColor(R.color.display_formula_text_color));
        mDeleteButton.setOnLongClickListener(this);
        mResultEditText.setTextColor(getResources().getColor(R.color.display_result_text_color));
        mResultEditText.setEnabled(false);

        mFormulaEditText.registerComponent(new MatrixView.MVDisplayComponent());
        mResultEditText.registerComponents(mFormulaEditText.getComponents());

        mDisplayView.bringToFront();

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        // Rebuild constants. If the user changed their locale, it won't kill the app
        // but it might change a decimal point from . to ,
        Constants.rebuildConstants();
        Button dot = (Button) findViewById(R.id.dec_point);
        dot.setText(String.valueOf(Constants.DECIMAL_POINT));

        GraphView graphView = (GraphView)findViewById(R.id.graphView);
        GraphModule graphModule = new GraphModule(mEvaluator.getSolver());
        mGraphController = new GraphController(graphView, graphModule, mDisplayView);

        DisplayMode displayMode = DisplayMode.FORMULA;
        int modeOrdinal = savedInstanceState.getInt(KEY_DISPLAY_MODE, -1);
        if (modeOrdinal != -1) {
            displayMode = DisplayMode.values()[modeOrdinal];
        }
        mDisplayView.setMode(displayMode);
        mDisplayView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mDisplayView.getHeight() > 0) {
                            mDisplayView.initializeHistoryAndGraphView();
                            if (mDisplayView.getMode() == DisplayMode.GRAPH) {
                                mGraphController.startGraph(mFormulaEditText.getText());
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load new history
        mPersist = new Persist(this);
        mPersist.load();
        mHistory = mPersist.getHistory();

        mHistoryAdapter = new HistoryAdapter(this, mHistory,
        new HistoryAdapter.HistoryItemCallback() {
            @Override
            public void onHistoryItemSelected(HistoryEntry entry) {
                mFormulaEditText.insert(entry.getEdited());
                mDisplayView.collapseHistory();
            }
        });
        mHistory.setObserver(mHistoryAdapter);
        mDisplayView.getHistoryView().setAdapter(mHistoryAdapter);
        mDisplayView.scrollToMostRecent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPersist.save();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before it is serialized.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.end();
        }

        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_STATE, mCurrentState.ordinal());
        outState.putString(KEY_CURRENT_EXPRESSION,
                mTokenizer.getNormalizedExpression(mFormulaEditText.getText()));
        outState.putInt(KEY_BASE, mBaseManager.getNumberBase().ordinal());
        outState.putInt(KEY_DISPLAY_MODE, mDisplayView.getMode().ordinal());
    }

    private void setClearVisibility(boolean visible) {
        mClearButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        mDeleteButton.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void setState(CalculatorState state) {
        if (mCurrentState != state) {
            mCurrentState = state;
            setClearVisibility(state == CalculatorState.RESULT || state == CalculatorState.ERROR);

            if (state == CalculatorState.ERROR) {
                final int errorColor = getResources().getColor(R.color.calculator_error_color);
                mFormulaEditText.setTextColor(errorColor);
                mResultEditText.setTextColor(errorColor);
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    getWindow().setStatusBarColor(errorColor);
                }
            } else {
                mFormulaEditText.setTextColor(
                        getResources().getColor(R.color.display_formula_text_color));
                mResultEditText.setTextColor(
                        getResources().getColor(R.color.display_result_text_color));
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    getWindow().setStatusBarColor(
                            getResources().getColor(R.color.calculator_accent_color));
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mPadViewPager == null || mPadViewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first pad (or the pad is not paged),
            // allow the system to handle the Back button.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous pad.
            mPadViewPager.setCurrentItem(mPadViewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before the pending user interaction is handled.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.end();
        }
    }

    public void onButtonClick(View view) {
        mCurrentButton = view;
        switch (view.getId()) {
            case R.id.eq:
                onEquals();
                break;
            case R.id.graph:
                onGraph();
                break;
            case R.id.del:
                onDelete();
                break;
            case R.id.clr:
                onClear();
                break;
            case R.id.det:
            case R.id.fun_cos:
            case R.id.fun_ln:
            case R.id.fun_log:
            case R.id.fun_sin:
            case R.id.fun_tan:
                // Add left parenthesis after functions.
                mFormulaEditText.insert(((Button) view).getText() + "(");
                break;
            case R.id.hex:
                setBase(Base.HEXADECIMAL);
                break;
            case R.id.bin:
                setBase(Base.BINARY);
                break;
            case R.id.dec:
                setBase(Base.DECIMAL);
                break;
            case R.id.matrix:
                mFormulaEditText.insert(MatrixView.getPattern());
                break;
            case R.id.matrix_inverse:
                mFormulaEditText.insert(MatrixInverseView.PATTERN);
                break;
            case R.id.matrix_transpose:
                mFormulaEditText.insert(MatrixTransposeView.PATTERN);
                break;
            case R.id.plus_row:
                if(mFormulaEditText.getActiveEditText() instanceof MatrixEditText) {
                    ((MatrixEditText) mFormulaEditText.getActiveEditText()).getMatrixView().addRow();
                }
                break;
            case R.id.minus_row:
                if(mFormulaEditText.getActiveEditText() instanceof MatrixEditText) {
                    ((MatrixEditText) mFormulaEditText.getActiveEditText()).getMatrixView().removeRow();
                }
                break;
            case R.id.plus_col:
                if(mFormulaEditText.getActiveEditText() instanceof MatrixEditText) {
                    ((MatrixEditText) mFormulaEditText.getActiveEditText()).getMatrixView().addColumn();
                }
                break;
            case R.id.minus_col:
                if(mFormulaEditText.getActiveEditText() instanceof MatrixEditText) {
                    ((MatrixEditText) mFormulaEditText.getActiveEditText()).getMatrixView().removeColumn();
                }
                break;
            case R.id.const_x:
                mFormulaEditText.insert(((Button) view).getText());
                break;
            default:
                // Clear the input if we are currently displaying a result, and if the key pressed
                // is not a postfix or infix operator.
                CharSequence buttonText = ((Button) view).getText();
                String buttonString = buttonText.toString();
                if (mCurrentState == CalculatorState.RESULT &&
                        !( buttonString.equals(getString(R.string.op_div)) ||
                        buttonString.equals(getString(R.string.op_mul)) ||
                        buttonString.equals(getString(R.string.op_sub)) ||
                        buttonString.equals(getString(R.string.op_add)) ||
                        buttonString.equals(getString(R.string.op_pow)) ||
                        buttonString.equals(getString(R.string.op_fact)) ||
                        buttonString.equals(getString(R.string.eq)) )) {
                    mFormulaEditText.clear();
                }
                mFormulaEditText.insert(buttonText);
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        mCurrentButton = view;
        if (view.getId() == R.id.del) {
            onClear();
            return true;
        }
        return false;
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == CalculatorState.INPUT) {
            if (result == null || result.equals(mFormulaEditText.getText())) {
                mResultEditText.clear();
            }
            else {
                mResultEditText.setText(result);
            }
        } else if (errorResourceId != INVALID_RES_ID) {
            onError(errorResourceId);
        } else if (!TextUtils.isEmpty(result)) {
            mHistory.enter(expr, result);
            mDisplayView.scrollToMostRecent();
            onResult(result);
        } else if (mCurrentState == CalculatorState.EVALUATE) {
            // The current expression cannot be evaluated -> return to the input state.
            setState(CalculatorState.INPUT);
        }
    }

    @Override
    public void onTextSizeChanged(final AdvancedDisplay textView, float oldSize) {
        if (mCurrentState != CalculatorState.INPUT) {
            // Only animate text changes that occur from user input.
            return;
        }

        // Calculate the values needed to perform the scale and translation animations,
        // maintaining the same apparent baseline for the displayed text.
        final float textScale = oldSize / textView.getTextSize();
        final float translationX;
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            translationX = (1.0f - textScale) *
                    (textView.getWidth() / 2.0f - textView.getPaddingEnd());
        }
        else {
            translationX = (1.0f - textScale) *
                    (textView.getWidth() / 2.0f - textView.getPaddingRight());
        }
        final float translationY = (1.0f - textScale) *
                (textView.getHeight() / 2.0f - textView.getPaddingBottom());
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(textView, View.SCALE_X, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.SCALE_Y, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_X, translationX, 0.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, translationY, 0.0f));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void onEquals() {
        if (mCurrentState == CalculatorState.INPUT) {
            if (mFormulaEditText.hasNext()) {
                mFormulaEditText.next();
            } else {
                setState(CalculatorState.EVALUATE);
                mEvaluator.evaluate(mFormulaEditText.getText(), this);
            }
        }
    }

    private void onGraph() {
        mGraphController.startGraph(mFormulaEditText.getText());
    }

    private void onDelete() {
        // Delete works like backspace; remove the last character from the expression.
        mFormulaEditText.backspace();
    }

    private void reveal(View sourceView, int colorRes, AnimatorListener listener) {
        // Make reveal cover the display and status bar.
        final View revealView = new View(this);
        mLayoutParams.height = mDisplayView.getDisplayHeight();
        mLayoutParams.gravity = Gravity.BOTTOM;
        revealView.setLayoutParams(mLayoutParams);
        revealView.setBackgroundColor(getResources().getColor(colorRes));
        mDisplayView.addView(revealView);

        final Animator revealAnimator;
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            final int[] clearLocation = new int[2];
            sourceView.getLocationInWindow(clearLocation);
            clearLocation[0] += sourceView.getWidth() / 2;
            clearLocation[1] += sourceView.getHeight() / 2;
            final int revealCenterX = clearLocation[0] - revealView.getLeft();
            final int revealCenterY = clearLocation[1] - revealView.getTop();
            final double x1_2 = Math.pow(revealView.getLeft() - revealCenterX, 2);
            final double x2_2 = Math.pow(revealView.getRight() - revealCenterX, 2);
            final double y_2 = Math.pow(revealView.getTop() - revealCenterY, 2);
            final float revealRadius = (float) Math.max(Math.sqrt(x1_2 + y_2), Math.sqrt(x2_2 + y_2));

            revealAnimator =
                    ViewAnimationUtils.createCircularReveal(revealView,
                            revealCenterX, revealCenterY, 0.0f, revealRadius);
        }
        else {
            revealAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f, 1f);
        }
        revealAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_longAnimTime));

        final Animator alphaAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        alphaAnimator.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        alphaAnimator.addListener(listener);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(revealAnimator).before(alphaAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mDisplayView.removeView(revealView);
                mCurrentAnimator = null;
            }
        });
        mCurrentAnimator = animatorSet;
        animatorSet.start();
    }

    private void onClear() {
        if (TextUtils.isEmpty(mFormulaEditText.getText())) {
            return;
        }
        final View sourceView = mClearButton.getVisibility() == View.VISIBLE
                ? mClearButton : mDeleteButton;
        reveal(sourceView, R.color.calculator_accent_color, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFormulaEditText.clear();
            }
        });
    }

    private void onError(final int errorResourceId) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            mResultEditText.setText(errorResourceId);
            return;
        }

        reveal(mCurrentButton, R.color.calculator_error_color, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setState(CalculatorState.ERROR);
                mResultEditText.setText(errorResourceId);
            }
        });
    }

    private void onResult(final String result) {
        // Make the clear button appear immediately.
        setClearVisibility(true);

        // Calculate the values needed to perform the scale and translation animations,
        // accounting for how the scale will affect the final position of the text.
        final float resultScale =
                mFormulaEditText.getVariableTextSize(result) / mResultEditText.getTextSize();
        final float resultTranslationX;
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            resultTranslationX = (1.0f - resultScale) *
                    (mResultEditText.getWidth() / 2.0f - mResultEditText.getPaddingEnd());
        }
        else {
            resultTranslationX = (1.0f - resultScale) *
                    (mResultEditText.getWidth() / 2.0f - mResultEditText.getPaddingRight());
        }
        final float resultTranslationY = (1.0f - resultScale) *
                (mResultEditText.getHeight() / 2.0f - mResultEditText.getPaddingBottom()) +
                (mFormulaEditText.getBottom() - mResultEditText.getBottom()) +
                (mResultEditText.getPaddingBottom() - mFormulaEditText.getPaddingBottom());
        final float formulaTranslationY = -mFormulaEditText.getBottom();

        // Use a value animator to fade to the final text color over the course of the animation.
        final int resultTextColor = mResultEditText.getCurrentTextColor();
        final int formulaTextColor = mFormulaEditText.getCurrentTextColor();
        final ValueAnimator textColorAnimator =
                ValueAnimator.ofObject(new ArgbEvaluator(), resultTextColor, formulaTextColor);
        textColorAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mResultEditText.setTextColor((Integer) valueAnimator.getAnimatedValue());
            }
        });

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                textColorAnimator,
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_X, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_Y, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_X, resultTranslationX),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_Y, resultTranslationY),
                ObjectAnimator.ofFloat(mFormulaEditText, View.TRANSLATION_Y, formulaTranslationY));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset all of the values modified during the animation.
                mResultEditText.setTextColor(resultTextColor);
                mResultEditText.setScaleX(1.0f);
                mResultEditText.setScaleY(1.0f);
                mResultEditText.setTranslationX(0.0f);
                mResultEditText.setTranslationY(0.0f);
                mFormulaEditText.setTranslationY(0.0f);

                // Finally update the formula to use the current result.
                mFormulaEditText.setText(result);
                setState(CalculatorState.RESULT);
                mCurrentAnimator = null;
            }
        });

        mCurrentAnimator = animatorSet;
        animatorSet.start();
    }

    private void setBase(Base base) {
        boolean baseChanged = base != mBaseManager.getNumberBase();

        // Update the BaseManager, which handles restricting which buttons to show
        mBaseManager.setNumberBase(base);

        // Update the evaluator, which handles the math
        mEvaluator.setBase(mFormulaEditText.getText(), base, new EvaluateCallback() {
            @Override
            public void onEvaluate(String expr, String result, int errorResourceId) {
                if (errorResourceId != INVALID_RES_ID) {
                    onError(errorResourceId);
                } else {
                    mResultEditText.setText(result);
                    if (!TextUtils.isEmpty(result)) {
                        onResult(result);
                    }
                }
            }
        });
        setSelectedBaseButton(base);

        // disable any buttons that are not relevant to the current base
        for (int resId : mBaseManager.getViewIds()) {
            // TODO: handle duplicates
            // This will not work if the same resId is used on multiple pages,
            // which will be the case after adding the matrix view.
            View view = findViewById(resId);
            if (view != null) {
                view.setEnabled(!mBaseManager.isViewDisabled(resId));
            }
        }

        // TODO: preserve history
        // Ideally each history entry is tagged with the base that it was created with.
        // Then when we import a history item into the current display, we can convert the
        // base as necessary. As a short term approach, just clear the history when
        // changing the base.
        if (baseChanged && mHistory != null) {
            mHistory.clear();
        }
    }

    private void setSelectedBaseButton(Base base) {
        findViewById(R.id.hex).setSelected(base.equals(Base.HEXADECIMAL));
        findViewById(R.id.bin).setSelected(base.equals(Base.BINARY));
        findViewById(R.id.dec).setSelected(base.equals(Base.DECIMAL));
    }
}
