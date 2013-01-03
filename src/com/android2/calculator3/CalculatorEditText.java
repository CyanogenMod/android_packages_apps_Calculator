/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android2.calculator3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class CalculatorEditText extends EditText {
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int PASTE = 2;
    private String[] mMenuItemsStrings;

    private String input;

    private final char power;
    private final char plus;
    private final char minus;
    private final char mul;
    private final char div;
    private final char equal;
    private final char leftParen;
    private final char rightParen;
    private final static char PLACEHOLDER = '\u200B';

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);

        power = context.getString(R.string.power).charAt(0);
        plus = context.getString(R.string.plus).charAt(0);
        minus = context.getString(R.string.minus).charAt(0);
        mul = context.getString(R.string.mul).charAt(0);
        div = context.getString(R.string.div).charAt(0);
        equal = context.getString(R.string.equal).charAt(0);
        leftParen = context.getString(R.string.leftParen).charAt(0);
        rightParen = context.getString(R.string.rightParen).charAt(0);

        addTextChangedListener(new TextWatcher() {
            boolean updating = false;
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                if(updating) return;

                input = s.toString().replace(PLACEHOLDER, power);
                final StringBuilder formattedInput = new StringBuilder();

                int sub_open = 0;
                int sub_closed = 0;
                int paren_open = 0;
                int paren_closed = 0;
                for(int i=0;i<input.length();i++) {
                    char c = input.charAt(i);
                    if(c == power) {
                        formattedInput.append("<sup>");
                        sub_open++;
                        if(i+1 == input.length()) {
                        	formattedInput.append(c);
                        	sub_open--;
                        }
                        else {
                        	formattedInput.append(PLACEHOLDER);
                        }
                        continue;
                    }

                    if(sub_open > sub_closed) {
                        if(paren_open == paren_closed) {
                            // Decide when to break the <sup> started by ^
                            if(    c == plus  // 2^3+1
                            	|| (c == minus && input.charAt(i-1) != power) // 2^3-1
                            	|| c == mul   // 2^3*1
                            	|| c == div   // 2^3/1
                            	|| c == equal // X^3=1
                            	|| (c == leftParen && (Character.isDigit(input.charAt(i-1)) || input.charAt(i-1) == rightParen)) // 2^3(1) or 2^(3-1)(0)
                            	|| (Character.isDigit(c) && input.charAt(i-1) == rightParen) // 2^(3)1
                            	|| (!Character.isDigit(c) && Character.isDigit(input.charAt(i-1)))) { // 2^3log(1)
                            	while(sub_open > sub_closed) {
                                    formattedInput.append("</sup>");
                                    sub_closed++;
                            	}
                            	paren_open = 0;
                            	paren_closed = 0;
                                if(c == leftParen) {
                                    paren_open--;
                                }
                                else if(c == rightParen) {
                                    paren_closed--;
                                }
                            }
                        }
                        if(c == leftParen) {
                            paren_open++;
                        }
                        else if(c == rightParen) {
                            paren_closed++;
                        }
                    }
                    formattedInput.append(c);
                }

                updating = true;
                int selectionHandle = getSelectionStart();
                setText(Html.fromHtml(formattedInput.toString()));
                setSelection(selectionHandle);
                updating = false;
            }
        });
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
        if (TextUtils.equals(title, mMenuItemsStrings[CUT])) {
            cutContent();
            handled = true;
        } else if (TextUtils.equals(title,  mMenuItemsStrings[COPY])) {
            copyContent();
            handled = true;
        } else if (TextUtils.equals(title,  mMenuItemsStrings[PASTE])) {
            pasteContent();
            handled = true;
        }
        return handled;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        MenuHandler handler = new MenuHandler();
        if (mMenuItemsStrings == null) {
            Resources resources = getResources();
            mMenuItemsStrings = new String[3];
            mMenuItemsStrings[CUT] = resources.getString(android.R.string.cut);
            mMenuItemsStrings[COPY] = resources.getString(android.R.string.copy);
            mMenuItemsStrings[PASTE] = resources.getString(android.R.string.paste);
        }
        for (int i = 0; i < mMenuItemsStrings.length; i++) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(handler);
        }
        if (getText().length() == 0) {
            menu.getItem(CUT).setVisible(false);
            menu.getItem(COPY).setVisible(false);
        }
        ClipData primaryClip = getPrimaryClip();
        if (primaryClip == null || primaryClip.getItemCount() == 0
                || !canPaste(primaryClip.getItemAt(0).coerceToText(getContext()))) {
            menu.getItem(PASTE).setVisible(false);
        }
    }

    private void setPrimaryClip(ClipData clip) {
        ClipboardManager clipboard = (ClipboardManager) getContext().
                getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
    }

    private void copyContent() {
        final Editable text = getText();
        int textLength = text.length();
        setSelection(0, textLength);
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
        Toast.makeText(getContext(), R.string.text_copied_toast, Toast.LENGTH_SHORT).show();
        setSelection(textLength);
    }

    private void cutContent() {
        final Editable text = getText();
        int textLength = text.length();
        setSelection(0, textLength);
        setPrimaryClip(ClipData.newPlainText(null, text));
        ((Editable) getText()).delete(0, textLength);
        setSelection(0);
    }

    private ClipData getPrimaryClip() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        return clipboard.getPrimaryClip();
    }

    private void pasteContent() {
        ClipData clip = getPrimaryClip();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence paste = clip.getItemAt(i).coerceToText(getContext());
                if (canPaste(paste)) {
                    ((Editable) getText()).insert(getSelectionEnd(), paste);
                }
            }
        }
    }

    private boolean canPaste(CharSequence paste) {
        return paste.length() > 0;
    }

    class NoTextSelectionMode implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            copyContent();
            // Prevents the selection action mode on double tap.
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {}

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

    public Editable getInput() {
        SpannableStringBuilder e = new SpannableStringBuilder();
        e.append(input);
        return e;
    }
}
