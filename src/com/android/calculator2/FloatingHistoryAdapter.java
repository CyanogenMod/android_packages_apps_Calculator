/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calculator2.view.HistoryLine;

class FloatingHistoryAdapter extends HistoryAdapter {
    private OnHistoryItemClickListener mListener;

    FloatingHistoryAdapter(Context context, History history) {
        super(context, history);
    }

    @Override
    protected HistoryLine createView() {
        HistoryLine v = (HistoryLine) View.inflate(getContext(),
                R.layout.floating_history_entry, null);
        return v;
    }

    @Override
    protected void updateView(final HistoryEntry entry, HistoryLine view) {
        TextView expr = (TextView) view.findViewById(R.id.historyExpr);
        TextView result = (TextView) view.findViewById(R.id.historyResult);

        expr.setText(formatText(entry.getBase()));
        result.setText(entry.getEdited());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onHistoryItemClick(entry);
                }
            }
        });
    }

    private void copyContent(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));

        String toastText = String.format(
                getContext().getResources().getString(R.string.text_copied_toast), text);
        Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
    }

    public void setOnHistoryItemClickListener(OnHistoryItemClickListener l) {
        mListener = l;
    }

    public static interface OnHistoryItemClickListener {
        public void onHistoryItemClick(HistoryEntry entry);
    }
}
