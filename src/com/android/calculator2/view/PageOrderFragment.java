/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.view;

import java.util.List;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.android.calculator2.CalculatorSettings;
import com.android.calculator2.Page;
import com.android.calculator2.R;

public class PageOrderFragment extends ListFragment {
    private final TouchInterceptor.DragListener mDragListener
            = new TouchInterceptor.DragListener() {
        @Override
        public void drag(int from, int to) {
            Log.i(getContext().getPackageName(), "Drag from " + from + " to " + to);
        }
    };

    TouchInterceptor mListView;
    List<Page> mArray = null;
    PageAdapter mAdapter = null;
    ChangeListener mChangeListener = null;
    private final TouchInterceptor.DropListener mDropListener
            = new TouchInterceptor.DropListener() {
        @Override
        public void drop(int from, int to) {
            Log.i(getContext().getPackageName(), "Drop from " + from + " to " + to);
            doMove(from, to);
            if (mChangeListener != null) {
                mChangeListener.change(mArray);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mListView = new TouchInterceptor(getContext());
        mListView.setId(android.R.id.list);

        return mListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        populateList();
    }

    private Context getContext() {
        return getActivity();
    }

    private void populateList() {
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setCacheColorHint(0);
        ((TouchInterceptor) lv).setDragListener(mDragListener);
        ((TouchInterceptor) lv).setDropListener(mDropListener);
        lv.setDivider(null);

        mArray = Page.getAllPages(getContext());

        mAdapter = new PageAdapter(getContext(), mArray);
        setListAdapter(mAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                if (!"settings".equals(mArray.get(position).getKey())) {
                    boolean isEnabled = CalculatorSettings.isPageEnabled(getContext(),
                            mArray.get(position));
                    CalculatorSettings.setPageEnabled(getContext(), mArray.get(position),
                            !isEnabled);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void doMove(int from, int to) {
        Page obj = mArray.get(from);
        mArray.remove(from);
        mArray.add(to, obj);
        mAdapter.notifyDataSetChanged();

        // Update order
        for (int i = 0; i < mArray.size(); i++) {
            CalculatorSettings.setPageOrder(getContext(), mArray.get(i), i);
        }
    }

    public void setOnChangeListener(ChangeListener listener) {
        mChangeListener = listener;
    }

    public static interface ChangeListener {
        public void change(List<Page> sources);
    }

    static public class PageAdapter extends ArrayAdapter<Page> {
        private final Context context;
        private final List<Page> values;

        public PageAdapter(Context context, List<Page> values) {
            super(context, R.layout.view_list_item_dragable, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.view_list_item_dragable, parent, false);

            TextView textView = (TextView) rowView.findViewById(android.R.id.text1);
            textView.setText(values.get(position).getName());

            CheckBox checkView = (CheckBox) rowView.findViewById(android.R.id.checkbox);
            checkView.setClickable(false);
            checkView.setChecked(CalculatorSettings.isPageEnabled(getContext(),
                    values.get(position)));

            if ("settings".equals(values.get(position).getKey())) {
                checkView.setEnabled(false);
            }

            return rowView;
        }
    }
}
