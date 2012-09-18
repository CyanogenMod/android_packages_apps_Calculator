package com.android2.calculator3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryTextView extends TextView {
    private static final int COPY = 0;
    private static final int REMOVE = 1;
    private String[] mMenuItemsStrings;
    private HistoryEntry mHistoryEntry;
    private History mHistory;
    private LinearLayout mRowView;
    private LinearLayout mHistoryView;

    public HistoryTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean performLongClick() {
        showContextMenu();
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        MenuHandler handler = new MenuHandler();
        if (mMenuItemsStrings == null) {
            Resources resources = getResources();
            mMenuItemsStrings = new String[2];
            mMenuItemsStrings[COPY] = resources.getString(android.R.string.copy) + " \"" + getText().toString() + "\"";
            mMenuItemsStrings[REMOVE] = resources.getString(R.string.remove_from_history);
        }
        for (int i = 0; i < mMenuItemsStrings.length; i++) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(handler);
        }
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {
            return onTextContextMenuItem(item.getTitle());
        }
    }

    public boolean onTextContextMenuItem(CharSequence title) {
        boolean handled = false;
        if (TextUtils.equals(title,  mMenuItemsStrings[COPY])) {
            copyContent();
            handled = true;
        }
        else if (TextUtils.equals(title,  mMenuItemsStrings[REMOVE])) {
            removeContent();
            handled = true;
        }
        return handled;
    }

    private void copyContent() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, getText()));
        Toast.makeText(getContext(), R.string.text_copied_toast, Toast.LENGTH_SHORT).show();
    }

    private void removeContent() {
        mHistory.remove(mHistoryEntry);
        mHistoryView.removeView(mRowView);
    }

    public HistoryEntry getHistoryEntry() {
        return mHistoryEntry;
    }

    public void setHistoryEntry(HistoryEntry historyEntry) {
        this.mHistoryEntry = historyEntry;
    }

    public History getHistory() {
        return mHistory;
    }

    public void setHistory(History history) {
        this.mHistory = history;
    }

    public LinearLayout getRowView() {
        return mRowView;
    }

    public void setRowView(LinearLayout rowView) {
        this.mRowView = rowView;
    }

    public LinearLayout getHistoryView() {
        return mHistoryView;
    }

    public void setHistoryView(LinearLayout historyView) {
        this.mHistoryView = historyView;
    }
}
