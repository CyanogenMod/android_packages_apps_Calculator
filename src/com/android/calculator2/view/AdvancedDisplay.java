package com.android.calculator2.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.calculator2.Clipboard;
import com.android.calculator2.R;
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

public class AdvancedDisplay extends ScrollableDisplay {
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int PASTE = 2;

    // For copy/paste
    private String[] mMenuItemsStrings;

    // Restrict keys from hardware keyboards
    private static final char[] ACCEPTED_CHARS = "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray();

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
    private final Paint mTempPaint = new TextPaint();
    private OnTextSizeChangeListener mOnTextSizeChangeListener;

    // Variables for setting custom views (like Matrices)
    private final Set<DisplayComponent> mComponents = new HashSet<DisplayComponent>();
    private final EventListener mEventListener = new EventListener() {
        @Override
        public void onEditTextChanged(EditText editText) {
            mActiveEditText = editText;
        }

        @Override
        public void onRemoveView(View view) {
            removeView(view);
        }
    };

    // Variables to apply to underlying EditTexts
    private Map<String, Sync> mRegisteredSyncs = new HashMap<String, Sync>();
    private float mMaximumTextSize;
    private float mMinimumTextSize;
    private float mStepTextSize;
    private float mTextSize;
    private int mTextColor;
    private Editable.Factory mFactory;
    private KeyListener mKeyListener;
    private final List<TextWatcher> mTextWatchers = new ArrayList<TextWatcher>();
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            CharSequence text = getText();
            for(TextWatcher watcher : mTextWatchers) {
                watcher.beforeTextChanged(text, 0, 0, text.length());
            }
            mCachedText = null;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            CharSequence text = getText();
            for(TextWatcher watcher : mTextWatchers) {
                watcher.onTextChanged(text, 0, 0, text.length());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            Editable e = new AdvancedEditable(getText());
            for(TextWatcher watcher : mTextWatchers) {
                watcher.afterTextChanged(e);
            }
        }
    };

    public AdvancedDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRoot = new Root(context);
        ScrollableDisplay.LayoutParams params = new ScrollableDisplay.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        mRoot.setLayoutParams(params);
        addView(mRoot);

        if(attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.CalculatorEditText, 0, 0);
            mTextSize = a.getDimension(
                    R.styleable.CalculatorEditText_textSize, getTextSize());
            mMaximumTextSize = a.getDimension(
                    R.styleable.CalculatorEditText_maxTextSize, getTextSize());
            mMinimumTextSize = a.getDimension(
                    R.styleable.CalculatorEditText_minTextSize, getTextSize());
            mStepTextSize = a.getDimension(R.styleable.CalculatorEditText_stepTextSize,
                    (mMaximumTextSize - mMinimumTextSize) / 3);
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

        // Step through increasing text sizes until the text would no longer fit.
        float lastFitTextSize = mMinimumTextSize;
        while (lastFitTextSize < mMaximumTextSize) {
            final float nextSize = Math.min(lastFitTextSize + mStepTextSize, mMaximumTextSize);
            mTempPaint.setTextSize(nextSize);
            if (mTempPaint.measureText(text) > mWidthConstraint) {
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
        for(TextWatcher watcher : mTextWatchers) {
            watcher.beforeTextChanged(getText(), 0, 0, 0);
            watcher.onTextChanged("", 0, 0, 0);
            watcher.afterTextChanged(new AdvancedEditable(""));
        }
        mRoot.removeAllViews();
        mActiveEditText = CalculatorEditText.getInstance(getContext(), mSolver, mEventListener);

        // Always start with a CalculatorEditText
        addView(mActiveEditText);
    }

    /**
     * Loop around when arrow keys are pressed
     * */
    View nextView(View currentView) {
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
    View previousView(View currentView) {
        boolean foundCurrentView = false;
        for(int i = mRoot.getChildCount() - 1; i >= 0; i--) {
            if(foundCurrentView) return mRoot.getChildAt(i);
            else if(currentView == mRoot.getChildAt(i)) foundCurrentView = true;
        }
        return mRoot.getChildAt(mRoot.getChildCount() - 1);
    }

    public void backspace() {
        getActiveEditText().dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
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
            if(CalculatorEditText.class.isInstance(getActiveEditText())) {
                // Logic to insert, split text if there's another view, etc
                int cursor = getActiveEditText().getSelectionStart();
                final int index = mRoot.getChildIndex(getActiveEditText());

                // Loop over the text, adding custom views when needed
                loop: while(!delta.isEmpty()) {
                    for(DisplayComponent c : mComponents) {
                        String equation = c.parse(delta);
                        if(equation != null) {
                            // We found a custom view
                            mRoot.addView(c.getView(getContext(), mSolver, equation, mEventListener));

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
                    getActiveEditText().getText().insert(cursor, delta.subSequence(0, 1));
                    delta = delta.substring(1);
                    cursor++;
                }
            }
            else {
                // We let the custom edit text handle displaying the text
                int cursor = getActiveEditText().getSelectionStart();
                getActiveEditText().getText().insert(cursor, delta);
            }
        }
    }

    private void splitText(int cursor, int index, String text) {
        // Grab the left and right strings
        final String leftText = getActiveEditText().getText().toString().substring(0, cursor);
        final String rightText = getActiveEditText().getText().toString().substring(cursor);

        // Update the left EditText
        getActiveEditText().setText(leftText);

        // Create a right EditText
        EditText et = CalculatorEditText.getInstance(getContext(), mSolver, mEventListener);
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

    protected EditText getActiveEditText() {
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
    public boolean performLongClick() {
        showContextMenu();
        return true;
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

        // Loop over the text, adding custom views when needed
        loop: while(!text.isEmpty()) {
            for(DisplayComponent c : mComponents) {
                String equation = c.parse(text);
                if(equation != null) {
                    // We found a custom view
                    mRoot.addView(c.getView(getContext(), mSolver, equation, mEventListener));

                    // Keep EditTexts in between custom views
                    addView(CalculatorEditText.getInstance(getContext(), mSolver, mEventListener));

                    // Update text and loop again
                    text = text.substring(equation.length());
                    continue loop;
                }
            }

            // Append the next character to the trailing EditText
            EditText trailingText = ((CalculatorEditText) mRoot.getLastView());
            trailingText.setText(trailingText.getText() + text.substring(0, 1));
            trailingText.setSelection(trailingText.length());
            text = text.substring(1);
        }
        mRoot.getLastView().requestFocus();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        MenuHandler handler = new MenuHandler();
        if(mMenuItemsStrings == null) {
            Resources resources = getResources();
            mMenuItemsStrings = new String[3];
            mMenuItemsStrings[CUT] = resources.getString(android.R.string.cut);
            mMenuItemsStrings[COPY] = resources.getString(android.R.string.copy);
            mMenuItemsStrings[PASTE] = resources.getString(android.R.string.paste);
        }
        for(int i = 0; i < mMenuItemsStrings.length; i++) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(handler);
        }
        if(getText().length() == 0) {
            menu.getItem(CUT).setVisible(false);
            menu.getItem(COPY).setVisible(false);
        }
        if(!Clipboard.canPaste(getContext())) {
            menu.getItem(PASTE).setVisible(false);
        }
    }

    public boolean onTextContextMenuItem(CharSequence title) {
        boolean handled = false;
        if(TextUtils.equals(title, mMenuItemsStrings[CUT])) {
            cutContent();
            handled = true;
        } else if(TextUtils.equals(title, mMenuItemsStrings[COPY])) {
            copyContent();
            handled = true;
        } else if(TextUtils.equals(title, mMenuItemsStrings[PASTE])) {
            pasteContent();
            handled = true;
        }
        return handled;
    }

    private void copyContent() {
        Clipboard.copy(getContext(), getText());
    }

    private void cutContent() {
        Clipboard.copy(getContext(), getText());
        clear();
    }

    private void pasteContent() {
        insert(Clipboard.paste(getContext()));
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {
            return onTextContextMenuItem(item.getTitle());
        }
    }

    /**
     * Declare a View as a component for AdvancedDisplay.
     *
     * A component is a custom view for math equations (Like matrices).
     * Register components with AdvancedDisplay to create a better UI when showing equations.
     * */
    public interface DisplayComponent {
        /**
         * The view to display.
         *
         * Includes the equation to display.
         *
         * Includes a copy of the solver being used,
         * because the base can change (from decimal to binary for instance).
         * Useful for adding comas, or whatever else you need.
         * */
        public View getView(Context context, Solver solver, String equation, EventListener listener);

        /**
         * Return the text you claim is yours, but only if the equation starts with it.
         *
         * For instance, [[0],[1]]+[[1],[0]] represents 2 matrices. A MatrixView would return
         * [[0],[1]] because that's 1 matrix.
         * */
        public String parse(String equation);
     }

    public static interface EventListener {
        public void onEditTextChanged(EditText editText);

        public void onRemoveView(View view);
    }

    public abstract class Sync {
        private String tag;
        Sync(String tag) {
            this.tag = tag;
        }

        public abstract void apply(TextView textView);

        @Override
        public int hashCode() {
            return tag.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Sync) {
                return ((Sync) o).tag.equals(tag);
            }
            return false;
        }
    }

    public interface OnTextSizeChangeListener {
        void onTextSizeChanged(AdvancedDisplay textView, float oldSize);
    }

    private class Root extends LinearLayout {
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
    }
}
