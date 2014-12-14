/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.calculator2.view.HistoryLine;
import com.xlythe.math.EquationFormatter;
import com.xlythe.math.History;
import com.xlythe.math.HistoryEntry;

import java.util.Vector;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final Context mContext;
    private final Vector<HistoryEntry> mEntries;
    private final EquationFormatter mEquationFormatter;
    protected HistoryItemCallback mCallback;

    public interface HistoryItemCallback {
        public void onHistoryItemSelected(HistoryEntry entry);
    }

    public HistoryAdapter(Context context, History history, HistoryItemCallback callback) {
        mContext = context;
        mEntries = history.getEntries();
        mEquationFormatter = new EquationFormatter();
        mCallback = callback;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView historyExpr;
        public TextView historyResult;

        public ViewHolder(View v) {
            super(v);
            historyExpr = (TextView)v.findViewById(R.id.historyExpr);
            historyResult = (TextView)v.findViewById(R.id.historyResult);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        HistoryLine view =
                (HistoryLine)LayoutInflater.from(mContext)
                        .inflate(R.layout.history_entry, parent, false);
        return new ViewHolder(view);
    }

    protected int getLayoutResourceId() {
        return R.layout.history_entry;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        HistoryLine view = (HistoryLine)holder.itemView;
        final HistoryEntry entry = mEntries.elementAt(position);
        view.setAdapter(HistoryAdapter.this);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onHistoryItemSelected(entry);
            }
        });
        holder.historyExpr.setText(formatText(entry.getBase()));
        holder.historyResult.setText(entry.getEdited());
    }

    @Override
    public int getItemCount() {
        return mEntries.size() - 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected Spanned formatText(String text) {
        return Html.fromHtml(mEquationFormatter.insertSupScripts(text));
    }

    public Context getContext() {
        return mContext;
    }
}
