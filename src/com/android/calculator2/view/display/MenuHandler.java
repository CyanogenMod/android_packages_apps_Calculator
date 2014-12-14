package com.android.calculator2.view.display;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import com.android.calculator2.Clipboard;

/**
 * Created by Will on 12/13/2014.
 */
class MenuHandler implements MenuItem.OnMenuItemClickListener {
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int PASTE = 2;

    // For copy/paste
    private String[] mMenuItemsStrings;
    private AdvancedDisplay mDisplay;

    public MenuHandler(AdvancedDisplay display) {
        mDisplay = display;

        Resources resources = getContext().getResources();
        mMenuItemsStrings = new String[3];
        mMenuItemsStrings[CUT] = resources.getString(android.R.string.cut);
        mMenuItemsStrings[COPY] = resources.getString(android.R.string.copy);
        mMenuItemsStrings[PASTE] = resources.getString(android.R.string.paste);
    }

    public void onCreateContextMenu(ContextMenu menu) {
        for(int i = 0; i < mMenuItemsStrings.length; i++) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(this);
        }
        if(getText().isEmpty()) {
            menu.getItem(CUT).setVisible(false);
            menu.getItem(COPY).setVisible(false);
        }
        if(!Clipboard.canPaste(getContext())) {
            menu.getItem(PASTE).setVisible(false);
        }
    }

    public Context getContext() {
        return mDisplay.getContext();
    }

    public String getText() {
        return mDisplay.getText();
    }

    public boolean onMenuItemClick(MenuItem item) {
        return onTextContextMenuItem(item.getTitle());
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
        mDisplay.clear();
    }

    private void pasteContent() {
        mDisplay.insert(Clipboard.paste(getContext()));
    }
}
