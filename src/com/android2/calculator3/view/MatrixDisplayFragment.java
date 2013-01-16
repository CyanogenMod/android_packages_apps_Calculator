package com.android2.calculator3.view;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class MatrixDisplayFragment extends Fragment {
    Context mContext;
    HorizontalScrollView s;

    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        setRetainInstance(true);

        if(s == null) {
            s = new HorizontalScrollView(mContext);
        }
        else {
            ViewGroup parent = (ViewGroup) s.getParent();
            parent.removeView(s);
        }

        return s;
    }
}
