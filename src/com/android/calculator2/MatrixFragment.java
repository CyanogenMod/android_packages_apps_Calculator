package com.android.calculator2;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MatrixFragment extends Fragment{
    Context mContext;
    ScrollView s;

    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        setRetainInstance(true);
        
        if (s == null) {
            
            s = (ScrollView) inflater.inflate(R.layout.matrix_panel, null);
            final LinearLayout matrices = (LinearLayout) s.findViewById(R.id.matrices);
            ImageButton i = (ImageButton) s.findViewById(R.id.matrixAdd);
            i.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    View view = inflater.inflate(R.layout.matrix_popup, null);
                    final LinearLayout theMatrix = (LinearLayout) view.findViewById(R.id.theMatrix);
                    final LinearLayout matrixPopup = (LinearLayout) view.findViewById(R.id.matrixPopup);
                    final LinearLayout matrixButtons = (LinearLayout) matrixPopup.findViewById(R.id.matrixButtons);
                    
                    builder.setView(view);
                    final AlertDialog alertDialog = builder.create();
                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(alertDialog.getWindow().getAttributes());
                    lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    
                    final ColorButton ok = (ColorButton) view.findViewById(R.id.ok);
                    ok.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (int i=0; i<theMatrix.getChildCount(); i++) {
                                LinearLayout layout = (LinearLayout) theMatrix.getChildAt(i);
                                for(int j=0; j<layout.getChildCount(); j++) {
                                    EditText view = (EditText) layout.getChildAt(j);
                                    if(view.getText().toString().equals("")) {
                                        view.requestFocus();
                                        return;
                                    }
                                }
                                for(int j=0; j<layout.getChildCount(); j++) {
                                    EditText view = (EditText) layout.getChildAt(j);
                                    view.setOnFocusChangeListener(new OnFocusChangeListener() {
                                        @Override
                                        public void onFocusChange(View v, boolean hasFocus) {
                                            if(hasFocus) {
                                                View theMatrix = (View) v.getParent().getParent();
                                                ViewGroup parent = (ViewGroup) theMatrix.getParent();
                                                parent.removeView(theMatrix);
                                            }
                                        }
                                    });
                                }
                            }
                            theMatrix.setFocusable(true);
                            theMatrix.setFocusableInTouchMode(true);
                            theMatrix.requestFocus();
                            ViewGroup parent = (ViewGroup) theMatrix.getParent();
                            parent.removeView(theMatrix);
                            matrices.addView(theMatrix, matrices.getChildCount()-1);
                            alertDialog.dismiss();
                        }
                    });

                    final ColorButton plus = (ColorButton) view.findViewById(R.id.matrixPlus);
                    plus.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            plus.setLayoutParams(new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                            plus.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    matrices.removeView(v);
                                }
                            });
                            matrixButtons.removeView(plus);
                            matrices.addView(plus, matrices.getChildCount()-1);
                            alertDialog.dismiss();
                        }
                    });
                    
                    final ColorButton mul = (ColorButton) view.findViewById(R.id.matrixMul);
                    mul.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mul.setLayoutParams(new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                            mul.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    matrices.removeView(v);
                                }
                            });
                            matrixButtons.removeView(mul);
                            matrices.addView(mul, matrices.getChildCount()-1);
                            alertDialog.dismiss();
                        }
                    });
                    
                    final ColorButton dot = (ColorButton) view.findViewById(R.id.matrixDot);
                    dot.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dot.setLayoutParams(new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                            dot.setPadding(15, 10, 15, 10);
                            dot.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    matrices.removeView(v);
                                }
                            });
                            matrixButtons.removeView(dot);
                            matrices.addView(dot, matrices.getChildCount()-1);
                            alertDialog.dismiss();
                        }
                    });
                    
                    final ColorButton cross = (ColorButton) view.findViewById(R.id.matrixCross);
                    cross.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cross.setLayoutParams(new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                            cross.setPadding(15, 10, 15, 10);
                            cross.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    matrices.removeView(v);
                                }
                            });
                            matrixButtons.removeView(cross);
                            matrices.addView(cross, matrices.getChildCount()-1);
                            alertDialog.dismiss();
                        }
                    });
                    int widthInDp = getResources().getInteger(R.integer.matrixInputBoxWidth);
                    int heightInDp = getResources().getInteger(R.integer.matrixInputBoxHeight);
                    final float widthInPx = LogicalDensity.convertDpToPixel(widthInDp, mContext);
                    final float heightInPx = LogicalDensity.convertDpToPixel(heightInDp, mContext);
                    final LinearLayout grip_bar_port = (LinearLayout) view.findViewById(R.id.grip_bar_port);
                    grip_bar_port.setOnTouchListener(new OnTouchListener() {
                        long distance = 0;
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                distance = 0;
                                break;
                            case MotionEvent.ACTION_UP:
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if(event.getX()>widthInPx+distance) {
                                    int rows = ((LinearLayout) theMatrix.getChildAt(0)).getChildCount();
                                    if(rows > 1) {
                                        for(int i=0; i<theMatrix.getChildCount(); i++) {
                                            LinearLayout l = (LinearLayout) theMatrix.getChildAt(i);
                                            l.removeViewAt(l.getChildCount()-1);
                                        }
                                        distance += widthInPx;
                                    }
                                }
                                else if(event.getX()<-widthInPx+distance) {
                                    for(int i=0; i<theMatrix.getChildCount(); i++) {
                                        LinearLayout l = (LinearLayout) theMatrix.getChildAt(i);
                                        EditText e = (EditText) inflater.inflate(R.layout.single_matrix_input_box, null);
                                        e.setWidth((int) widthInPx);
                                        e.setHeight((int) heightInPx);
                                        l.addView(e);
                                    }
                                    distance -= widthInPx;
                                }
                                break;
                            }
                            return true;
                        }
                    });
                    final LinearLayout grip_bar_land = (LinearLayout) view.findViewById(R.id.grip_bar_land);
                    grip_bar_land.setOnTouchListener(new OnTouchListener() {
                        long distance = 0;
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                distance = 0;
                                break;
                            case MotionEvent.ACTION_UP:
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if(event.getY()>heightInPx+distance) {
                                    int rows = ((LinearLayout) theMatrix.getChildAt(0)).getChildCount();
                                    LinearLayout l = new LinearLayout(mContext);
                                    l.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                                    l.setOrientation(LinearLayout.HORIZONTAL);
                                    for(int i=0; i<rows; i++) {
                                        EditText e = (EditText) inflater.inflate(R.layout.single_matrix_input_box, null);
                                        e.setWidth((int) widthInPx);
                                        e.setHeight((int) heightInPx);
                                        
                                        l.addView(e);
                                    }
                                    theMatrix.addView(l);
                                    distance += heightInPx;
                                }
                                else if(event.getY()<-heightInPx+distance) {
                                    int columns = theMatrix.getChildCount();
                                    if(columns > 1) {
                                        theMatrix.removeViewAt(0);
                                        distance -= heightInPx;
                                    }
                                }
                                break;
                            }
                            return true;
                        }
                    });

                    alertDialog.getWindow().setAttributes(lp);
                    alertDialog.show();
                }
            });
        }
        else{
             ViewGroup parent = (ViewGroup) s.getParent();
             parent.removeView(s);
        }
        
        return s;
    }
}