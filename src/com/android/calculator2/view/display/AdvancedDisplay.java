package com.android.calculator2.view.display;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.calculator2.Clipboard;
import com.android.calculator2.R;
import com.android.calculator2.view.CalculatorEditable;
import com.android.calculator2.view.ScrollableDisplay;
import com.android.calculator2.view.TextUtil;
import com.xlythe.math.Constants;
import com.xlythe.math.Solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvancedDisplay extends ScrollableDisplay implements EventListener {

    // Restrict keys from hardware keyboards
    private static final char[] ACCEPTED_CHARS = "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray();

    // For cut, copy, and paste
    MenuHandler mMenuHandler = new MenuHandler(this);

    // Currently focused text box
    private EditText mActiveEditText;

    // The LinearLayout inside of this HorizontalScrollView
    private Root mRoot;

    // Math library
    private Solver mSolver;

    // A cached copy of getText so we don't calculate it every time its called
    private String mCachedText;

    // Try and use as large a text as possible, if the width allows it
    private int mWidthConstraint = -1;
    private int mHeightConstraint = -1;
    private final Paint mTempPaint = new TextPaint();
    private OnTextSizeChangeListener mOnTextSizeChangeListener;

    // Variables for setting custom views (like Matrices)
    private final Set<DisplayComponent> mComponents = new HashSet<DisplayComponent>();

    // Variables to apply to underlying EditTexts
    private Map<String, Sync> mRegisteredSyncs = new HashMap<String, Sync>();
    private float mMaximumTextSize;
    private float mMinimumTextSize;
    private float mStepTextSize;
    private float mTextSize;
    private int mTextColor;
    private Editable.Factory mFactory;
    private KeyListener mKeyListener;
    private boolean mTextIsUpdating = false;
    private final List<TextWatcher> mTextWatchers = new ArrayList<TextWatcher>();
    private final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if(mTextIsUpdating) return;

            CharSequence text = getText();
            for(TextWatcher watcher : mTextWatchers) {
                watcher.beforeTextChanged(text, 0, 0, text.length());
            }
            mCachedText = null;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(mTextIsUpdating) return;

            CharSequence text = getText();
            for(TextWatcher watcher : mTextWatchers) {
                watcher.onTextChanged(text, 0, 0, text.length());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(mTextIsUpdating) return;

            Editable e = mFactory.newEditable(getText());
            for(TextWatcher watcher : mTextWatchers) {
                watcher.afterTextChanged(e);
            }
        }
    };

    public AdvancedDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRoot = new Root(context);
        ScrollableDisplay.LayoutParams params = new ScrollableDisplay.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        mRoot.setLayoutParams(params);
        mRoot.setGravity(Gravity.RIGHT);
        mRoot.setLongClickable(true);
        addView(mRoot);

        if(attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.CalculatorEditText, 0, 0);
            mTextSize = mMaximumTextSize = mMinimumTextSize = a.getDimension(
                    R.styleable.CalculatorEditText_textSize, getTextSize());
            mMaximumTextSize = a.getDimension(
                    R.styleable.CalculatorEditText_maxTextSize, getTextSize());
            mMinimumTextSize = a.getDimension(
                    R.styleable.CalculatorEditText_minTextSize, getTextSize());
            mStepTextSize = a.getDimension(R.styleable.CalculatorEditText_stepTextSize,
                    (mMaximumTextSize - mMinimumTextSize) / 3);
            mTextColor = a.getColor(R.styleable.CalculatorEditText_textColor, 0);
            a.recycle();

            setTextSize(TypedValue.COMPLEX_UNIT_PX, mMaximumTextSize);
            setMinimumHeight((int) (mMaximumTextSize * 1.2) + getPaddingBottom() + getPaddingTop());
        }

        Editable.Factory factory = new CalculatorEditable.Factory();
        setEditableFactory(factory);

        final List<String> keywords = Arrays.asList(
                context.getString(R.string.arcsin) + "(",
                context.getString(R.string.arccos) + "(",
                context.getString(R.string.arctan) + "(",
                context.getString(R.string.fun_sin) + "(",
                context.getString(R.string.fun_cos) + "(",
                context.getString(R.string.fun_tan) + "(",
                context.getString(R.string.fun_log) + "(",
                context.getString(R.string.mod) + "(",
                context.getString(R.string.fun_ln) + "(",
                context.getString(R.string.det) + "(",
                context.getString(R.string.dx),
                context.getString(R.string.dy),
                context.getString(R.string.cbrt) + "(");
        NumberKeyListener calculatorKeyListener = new NumberKeyListener() {
            @Override
            public int getInputType() {
                return EditorInfo.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }

            @Override
            protected char[] getAcceptedChars() {
                return ACCEPTED_CHARS;
            }

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                /*
                 * the EditText should still accept letters (eg. 'sin') coming from the on-screen touch buttons, so don't filter anything.
                 */
                return null;
            }

            @Override
            public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_DEL) {
                    int selectionHandle = getSelectionStart();
                    if(selectionHandle == 0) {
                        // Remove the view in front
                        int index = getChildIndex(getActiveEditText());
                        if(index > 0) {
                            removeView(getChildAt(index - 1));
                            return true;
                        }
                    } else {
                        // Check and remove keywords
                        String textBeforeInsertionHandle = getActiveEditText().getText().toString().substring(0, selectionHandle);
                        String textAfterInsertionHandle = getActiveEditText().getText().toString().substring(selectionHandle, getActiveEditText().getText().toString().length());

                        for(String s : keywords) {
                            if(textBeforeInsertionHandle.endsWith(s)) {
                                int deletionLength = s.length();
                                String text = textBeforeInsertionHandle.substring(0, textBeforeInsertionHandle.length() - deletionLength) + textAfterInsertionHandle;
                                getActiveEditText().setText(text);
                                setSelection(selectionHandle - deletionLength);
                                return true;
                            }
                        }
                    }
                }
                return super.onKeyDown(view, content, keyCode, event);
            }
        };
        setKeyListener(calculatorKeyListener);
    }

    public void setEditableFactory(Editable.Factory factory) {
        mFactory = factory;
        registerSync(new Sync("setEditableFactory") {
            @Override
            public void apply(TextView textView) {
                textView.setEditableFactory(mFactory);
            }
        });
    }

    public void setKeyListener(KeyListener input) {
        mKeyListener = input;
        registerSync(new Sync("setKeyListener") {
            @Override
            public void apply(TextView textView) {
                textView.setKeyListener(mKeyListener);
            }
        });
    }

    public void setTextColor(int color) {
        mTextColor = color;
        registerSync(new Sync("setTextColor") {
            @Override
            public void apply(TextView textView) {
                textView.setTextColor(mTextColor);
            }
        });
    }

    public int getCurrentTextColor() {
        return mTextColor;
    }

    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(final int unit, float size) {
        final float oldTextSize = mTextSize;
        mTextSize = size;
        registerSync(new Sync("setTextSize") {
            @Override
            public void apply(TextView textView) {
                textView.setTextSize(unit, mTextSize);
            }
        });
        if (mOnTextSizeChangeListener != null && getTextSize() != oldTextSize) {
            mOnTextSizeChangeListener.onTextSizeChanged(this, oldTextSize);
        }
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setOnTextSizeChangeListener(OnTextSizeChangeListener listener) {
        mOnTextSizeChangeListener = listener;
    }

    public float getVariableTextSize(String text) {
        if (mWidthConstraint < 0 || mMaximumTextSize <= mMinimumTextSize) {
            // Not measured, bail early.
            return getTextSize();
        }

        // Count exponents, which aren't measured properly.
        int exponents = TextUtil.countOccurrences(text, '^');

        // Step through increasing text sizes until the text would no longer fit.
        float lastFitTextSize = mMinimumTextSize;
        while (lastFitTextSize < mMaximumTextSize) {
            final float nextSize = Math.min(lastFitTextSize + mStepTextSize, mMaximumTextSize);
            mTempPaint.setTextSize(nextSize);
            if (mTempPaint.measureText(text) > mWidthConstraint) {
                break;
            } else if(nextSize + nextSize * exponents / 2 > mHeightConstraint) {
                break;
            } else {
                lastFitTextSize = nextSize;
            }
        }

        return lastFitTextSize;
    }

    public void addTextChangedListener(TextWatcher watcher) {
        mTextWatchers.add(watcher);
    }

    protected void registerSync(Sync sync) {
        mRegisteredSyncs.put(sync.tag, sync);
        apply(this, sync);
    }

    private void apply(View view, Sync sync) {
        if(view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for(int i=0;i<vg.getChildCount();i++) {
                apply(vg.getChildAt(i), sync);
            }
        }
        else if(view instanceof TextView) {
            sync.apply((TextView) view);
        }
    }

    @Override
    public void removeView(View view) {
        int index = mRoot.getChildIndex(view);
        if(index == -1) return;

        // Remove the requested view
        mRoot.removeViewAt(index);

        // Combine the 2 EditTexts on either side
        CalculatorEditText leftSide = (CalculatorEditText) mRoot.getChildAt(index - 1);
        CalculatorEditText rightSide = (CalculatorEditText) mRoot.getChildAt(index);
        int cursor = leftSide.getText().length();
        leftSide.setText(leftSide.getText().toString() + rightSide.getText().toString());
        mRoot.removeViewAt(index); // Remove the second EditText
        leftSide.requestFocus();
        leftSide.setSelection(cursor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidthConstraint =
                MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        mHeightConstraint =
                MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getVariableTextSize(getText().toString()));
    }

    protected int getSelectionStart() {
        if(getActiveEditText() == null) return 0;
        return getActiveEditText().getSelectionStart();
    }

    protected void setSelection(int position) {
        getActiveEditText().setSelection(position);
    }

    /**
     * Clears the text in the display
     * */
    public void clear() {
        mCachedText = null;

        // Notify the text watcher
        mTextWatcher.beforeTextChanged(null, 0, 0, 0);

        // Clear all views
        mRoot.removeAllViews();

        // Always start with a CalculatorEditText
        mActiveEditText = CalculatorEditText.getInstance(getContext(), mSolver, this);
        addView(mActiveEditText);

        // Notify the text watcher
        mTextWatcher.onTextChanged(null, 0, 0, 0);
        mTextWatcher.afterTextChanged(null);
    }

    @Override
    public void onEditTextChanged(EditText editText) {
        mActiveEditText = editText;
    }

    @Override
    public void onRemoveView(View view) {
        removeView(view);
    }
    
    /**
     * Loop around when arrow keys are pressed
     * */
    @Override
     public View nextView(View currentView) {
        boolean foundCurrentView = false;
        for(int i = 0; i < mRoot.getChildCount(); i++) {
            if(foundCurrentView) return mRoot.getChildAt(i);
            else if(currentView == mRoot.getChildAt(i)) foundCurrentView = true;
        }
        return mRoot.getChildAt(0);
    }

    /**
     * Loop around when arrow keys are pressed
     * */
    @Override
     public View previousView(View currentView) {
        boolean foundCurrentView = false;
        for(int i = mRoot.getChildCount() - 1; i >= 0; i--) {
            if(foundCurrentView) return mRoot.getChildAt(i);
            else if(currentView == mRoot.getChildAt(i)) foundCurrentView = true;
        }
        return mRoot.getChildAt(mRoot.getChildCount() - 1);
    }

    public void next() {
        if(mActiveEditText.getSelectionStart() == mActiveEditText.getText().length()) {
            View v = mActiveEditText.focusSearch(View.FOCUS_FORWARD);
            if(v != null) v.requestFocus();
            mActiveEditText.setSelection(0);
        } else {
            mActiveEditText.setSelection(mActiveEditText.getSelectionStart() + 1);
        }
    }

    public boolean hasNext() {
        return hasNext(this);
    }

    private boolean hasNext(View view) {
        if(view instanceof AdvancedDisplayControls) {
            return ((AdvancedDisplayControls) view).hasNext();
        }
        else if(view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for(int i=0;i<vg.getChildCount();i++) {
                if(hasNext(vg.getChildAt(i))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void backspace() {
        EditText aet = getActiveEditText();
        if (aet != null)
            aet.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
    }

    /**
     * Inserts text at the cursor of the active EditText
     * */
    public void insert(CharSequence delta) {
        insert(delta.toString());
    }

    /**
     * Inserts text at the cursor of the active EditText
     * */
    public void insert(String delta) {
        if(mActiveEditText == null) {
            setText(delta);
        }
        else {
            // Notify the text watcher
            mTextWatcher.beforeTextChanged(null, 0, 0, 0);
            mTextIsUpdating = true;

            if(CalculatorEditText.class.isInstance(getActiveEditText())) {
                // Logic to insert, split text if there's another view, etc
                int cursor, cacheCursor;
                cursor = cacheCursor = getActiveEditText().getSelectionStart();
                final int index = mRoot.getChildIndex(getActiveEditText());
                StringBuilder cache = new StringBuilder();

                // Loop over the text, adding custom views when needed
                loop: while(!delta.isEmpty()) {
                    for(DisplayComponent c : mComponents) {
                        String equation = c.parse(delta);
                        if(equation != null) {
                            // Update the EditText with the cached text
                            getActiveEditText().getText().insert(cursor, cache);
                            cache.setLength(0);
                            cacheCursor = 0;

                            // We found a custom view
                            mRoot.addView(c.getView(getContext(), mSolver, equation, this));

                            // Keep EditTexts in between custom views
                            splitText(cursor, index, delta);
                            mRoot.getChildAt(index + 2).requestFocus();

                            // Update text and loop again
                            delta = delta.substring(equation.length());
                            continue loop;
                        }
                    }

                    // Don't allow leading operators
                    if(cursor == 0 && getActiveEditText() == mRoot.getChildAt(0)
                            && Solver.isOperator(delta)
                            && !delta.equals(String.valueOf(Constants.MINUS))) {
                        delta = delta.substring(1);
                        continue loop;
                    }

                    // Append the next character to the EditText
                    cache.append(delta.charAt(0));
                    delta = delta.substring(1);
                    cursor++;
                }

                // Update the EditText with the cached text
                getActiveEditText().getText().insert(cacheCursor, cache);
            }
            else {
                // We let the custom edit text handle displaying the text
                int cursor = getActiveEditText().getSelectionStart();
                getActiveEditText().getText().insert(cursor, delta);
            }

            // Notify the text watcher
            mTextIsUpdating = false;
            mTextWatcher.onTextChanged(null, 0, 0, 0);
            mTextWatcher.afterTextChanged(null);
        }
    }

    private void splitText(int cursor, int index, String text) {
        // Grab the left and right strings
        final String leftText = getActiveEditText().getText().toString().substring(0, cursor);
        final String rightText = getActiveEditText().getText().toString().substring(cursor);

        // Update the left EditText
        getActiveEditText().setText(leftText);

        // Create a right EditText
        EditText et = CalculatorEditText.getInstance(getContext(), mSolver, this);
        et.setText(rightText);
        addView(et, index + 2);

        // Decide who needs focus
        if(text.isEmpty()) {
            mRoot.getChildAt(index + 1).requestFocus();
        } else {
            mRoot.getChildAt(index + 2).requestFocus();
            ((CalculatorEditText) mRoot.getChildAt(index + 2)).setSelection(0);
        }
    }

    public void setSolver(Solver solver) {
        mSolver = solver;
    }

    public EditText getActiveEditText() {
        return mActiveEditText;
    }

    @Override
    public void addView(View child) {
        if(child == mRoot) {
            super.addView(child);
            return;
        }
        mRoot.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if(child == mRoot) {
            super.addView(child, index);
            return;
        }
        mRoot.addView(child, index);
    }

    public int getChildIndex(View child) {
        return mRoot.getChildIndex(child);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        // We only want to disable our children. So we're not calling super on purpose.
        registerSync(new Sync("setEnabled") {
            @Override
            public void apply(TextView textView) {
                textView.setEnabled(enabled);
            }
        });
    }

    /**
     * Returns the text in the display
     * */
    public String getText() {
        if(mCachedText != null) {
            return mCachedText;
        }

        String text = "";
        for(int i = 0; i < mRoot.getChildCount(); i++) {
            text += mRoot.getChildAt(i).toString();
        }
        mCachedText = text;
        return text;
    }

    /**
     * Set the text for the display
     * */
    public void setText(int resId) {
        setText(getContext().getString(resId));
    }

    /**
     * Set the text for the display
     * */
    public void setText(String text) {
        // Notify the text watcher
        mTextWatcher.beforeTextChanged(null, 0, 0, 0);
        mTextIsUpdating = true;

        // Remove existing text
        clear();

        // Clear on null
        if(text == null) return;

        // Don't allow leading operators
        while(text.length() > 0
                && Solver.isOperator(text.charAt(0))
                && !text.startsWith(String.valueOf(Constants.MINUS))) {
            text = text.substring(1);
        }

        StringBuilder cache = new StringBuilder();

        // Loop over the text, adding custom views when needed
        loop: while(!text.isEmpty()) {
            for(DisplayComponent c : mComponents) {
                String equation = c.parse(text);
                if(equation != null) {
                    // Apply the cache
                    EditText trailingText = ((CalculatorEditText) mRoot.getLastView());
                    trailingText.setText(cache);
                    cache.setLength(0);

                    // We found a custom view
                    mRoot.addView(c.getView(getContext(), mSolver, equation, this));

                    // Keep EditTexts in between custom views
                    addView(CalculatorEditText.getInstance(getContext(), mSolver, this));

                    // Update text and loop again
                    text = text.substring(equation.length());
                    continue loop;
                }
            }

            // Append the next character to the trailing EditText
            cache.append(text.charAt(0));
            text = text.substring(1);
        }

        // Apply the cache
        EditText trailingText = ((CalculatorEditText) mRoot.getLastView());
        trailingText.setText(cache);
        trailingText.setSelection(trailingText.length());
        trailingText.requestFocus();

        // Notify the text watcher
        mTextIsUpdating = false;
        mTextWatcher.onTextChanged(null, 0, 0, 0);
        mTextWatcher.afterTextChanged(null);
    }

    public void registerComponent(DisplayComponent component) {
        mComponents.add(component);
    }

    public void registerComponents(Collection<DisplayComponent> components) {
        mComponents.addAll(components);
    }

    public Set<DisplayComponent> getComponents() {
        return mComponents;
    }

    // Everything below is for copy/paste

    public interface OnTextSizeChangeListener {
        void onTextSizeChanged(AdvancedDisplay textView, float oldSize);
    }

    class Root extends LinearLayout {
        public Root(Context context) {
            this(context, null);
        }

        public Root(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOrientation(HORIZONTAL);
        }

        @Override
        public void addView(View child, int index) {
            super.addView(child, index);

            for(Map.Entry<String, Sync> sync : mRegisteredSyncs.entrySet()) {
                // Apply all our custom variables to our lovely children
                apply(child, sync.getValue());
            }

            apply(child, new Sync("addTextChangedListener") {
                @Override
                public void apply(TextView textView) {
                    textView.addTextChangedListener(mTextWatcher);
                }
            });
        }

        public View getLastView() {
            if(getChildCount() == 0) return null;
            return getChildAt(getChildCount() - 1);
        }

        /**
         * Returns the position of a view
         * */
        public int getChildIndex(View view) {
            for(int i = 0; i < getChildCount(); i++) {
                if(getChildAt(i) == view) return i;
            }
            return -1;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu) {
            mMenuHandler.onCreateContextMenu(menu);
        }
    }
}
