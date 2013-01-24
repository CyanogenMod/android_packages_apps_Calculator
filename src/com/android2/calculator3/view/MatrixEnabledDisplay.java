package com.android2.calculator3.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable.Factory;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android2.calculator3.R;

public class MatrixEnabledDisplay extends LinearLayout {
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int PASTE = 2;
    private String[] mMenuItemsStrings;

    CalculatorEditText mActiveEditText;
    KeyListener mKeyListener;
    Factory mFactory;

    public MatrixEnabledDisplay(Context context, AttributeSet attr) {
        super(context, attr);
        setOrientation(HORIZONTAL);
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        mActiveEditText = null;
    }

    @Override
    public void removeView(View view) {
        int index = getChildIndex(view);
        if(index == -1) return;
        super.removeViewAt(index);
        if(mActiveEditText == view) {
            if(getChildCount() == 0) mActiveEditText = null;
            else if(index == 0) getChildAt(0).requestFocus();
            else getChildAt(index - 1).requestFocus();
        }
    }

    public int getChildIndex(View view) {
        for(int i = 0; i < getChildCount(); i++) {
            if(getChildAt(i) == view) return i;
        }
        return -1;
    }

    public String getText() {
        String text = "";
        for(int i = 0; i < getChildCount(); i++) {
            text += getChildAt(i).toString();
        }
        return text;
    }

    public void clear() {
        removeAllViews();
    }

    public void insert(String delta) {
        if(mActiveEditText == null) {
            setText(delta);
        }
        else {
            if(CalculatorEditText.class.isInstance(getActiveEditText())) {
                // Logic to insert, split text if there's another view, etc
                String text = delta;
                while(!text.isEmpty()) {
                    int cursor = getActiveEditText().getSelectionStart();

                    if(MatrixView.verify(text)) {
                        final String leftText = getActiveEditText().getText().toString().substring(0, cursor);
                        final String rightText = getActiveEditText().getText().toString().substring(cursor);
                        final int index = getChildIndex(getActiveEditText());

                        getActiveEditText().setText(leftText);
                        text = MatrixView.load(text, this, index + 1);
                        CalculatorEditText.fullLoad(rightText, this, index + 2);
                        if(text.isEmpty()) {
                            getChildAt(index + 1).requestFocus();
                        }
                        else {
                            getChildAt(index + 2).requestFocus();
                        }
                    }
                    else {
                        getActiveEditText().getText().insert(cursor, text.subSequence(0, 1));
                        text = text.substring(1, text.length());
                    }
                }
            }
            else {
                int cursor = getActiveEditText().getSelectionStart();
                getActiveEditText().getText().insert(cursor, delta);
            }
        }
    }

    public void setText(String text) {
        clear();
        while(!text.isEmpty()) {
            text = MatrixView.load(text, this);

            // The default. Append the next character to the EditText (or create
            // an EditText if none exists)
            if(CalculatorEditText.class.isInstance(getLastView())) {
                ((CalculatorEditText) getLastView()).append(text.subSequence(0, 1));
                text = text.substring(1, text.length());
            }
            else {
                text = CalculatorEditText.load(text, this);
            }
        }
    }

    public void setKeyListener(KeyListener input) {
        mKeyListener = input;
    }

    public void setEditableFactory(Factory factory) {
        mFactory = factory;
    }

    public CalculatorEditText getActiveEditText() {
        return mActiveEditText;
    }

    private View getLastView() {
        if(getChildCount() == 0) return null;
        return getChildAt(getChildCount() - 1);
    }

    @Override
    public boolean performLongClick() {
        showContextMenu();
        return true;
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {
            return onTextContextMenuItem(item.getTitle());
        }
    }

    public boolean onTextContextMenuItem(CharSequence title) {
        boolean handled = false;
        if(TextUtils.equals(title, mMenuItemsStrings[CUT])) {
            cutContent();
            handled = true;
        }
        else if(TextUtils.equals(title, mMenuItemsStrings[COPY])) {
            copyContent();
            handled = true;
        }
        else if(TextUtils.equals(title, mMenuItemsStrings[PASTE])) {
            pasteContent();
            handled = true;
        }
        return handled;
    }

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
        ClipData primaryClip = getPrimaryClip();
        if(primaryClip == null || primaryClip.getItemCount() == 0 || !canPaste(primaryClip.getItemAt(0).coerceToText(getContext()))) {
            menu.getItem(PASTE).setVisible(false);
        }
    }

    private void setPrimaryClip(ClipData clip) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
    }

    private void copyContent() {
        final String text = getText();
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
        Toast.makeText(getContext(), R.string.text_copied_toast, Toast.LENGTH_SHORT).show();
    }

    private void cutContent() {
        final String text = getText();
        setPrimaryClip(ClipData.newPlainText(null, text));
        clear();
    }

    private ClipData getPrimaryClip() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboard.getPrimaryClip();
    }

    private void pasteContent() {
        ClipData clip = getPrimaryClip();
        if(clip != null) {
            for(int i = 0; i < clip.getItemCount(); i++) {
                CharSequence paste = clip.getItemAt(i).coerceToText(getContext());
                if(canPaste(paste)) {
                    insert(paste.toString());
                }
            }
        }
    }

    private boolean canPaste(CharSequence paste) {
        return paste.length() > 0;
    }
}
