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

package com.android.calculator2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;

public class CalculatorEditText extends EditText {
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int PASTE = 2;
    private String[] mMenuItemsStrings;
    private ImmutableMap<String, String> sReplacementTable;
    private String[] sOperators;

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
       if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // Hack to prevent keyboard and insertion handle from showing.
           cancelLongPress();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performLongClick() {
        showContextMenu();
        return true;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        String mathText = mathParse(getText().toString());
        // Parse the string into something more "mathematical" sounding.
        if (!TextUtils.isEmpty(mathText)) {
            event.getText().clear();
            event.getText().add(mathText);
            setContentDescription(mathText);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText(mathParse(getText().toString()));
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Do nothing.
    }

    private String mathParse(String plainText) {
        String parsedText = plainText;
        if (!TextUtils.isEmpty(parsedText)) {
            // Initialize replacement table.
            initializeReplacementTable();
            for (String operator : sOperators) {
                if (sReplacementTable.containsKey(operator)) {
                    parsedText = parsedText.replace(operator, sReplacementTable.get(operator));
                }
            }
        }
        return parsedText;
    }

    private synchronized void initializeReplacementTable() {
        if (sReplacementTable == null) {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            Resources res = getContext().getResources();
            sOperators = res.getStringArray(R.array.operators);
            String[] descs = res.getStringArray(R.array.operatorDescs);
            int pos = 0;
            for (String key : sOperators) {
                builder.put(key, descs[pos]);
                pos++;
            }
            sReplacementTable = builder.build();
        }
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
        return paste.length()>0;
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
}
