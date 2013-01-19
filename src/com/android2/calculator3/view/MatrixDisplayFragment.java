package com.android2.calculator3.view;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MatrixDisplayFragment extends Fragment {
    Context mContext;
    public MatrixDisplay layout;

    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        setRetainInstance(true);

        if(layout == null) {
            layout = new MatrixDisplay(mContext);
        }
        else {
            ViewGroup parent = (ViewGroup) layout.getParent();
            parent.removeView(layout);
        }

        return layout;
    }
}
