package com.android.calculator3;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MatrixFragment extends Fragment{
    Context mContext;
    LinearLayout l;

    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        setRetainInstance(true);
        
        if (l == null) {
        	l = new LinearLayout(mContext);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            l.setLayoutParams(params);
            l.setId(R.id.matrices);
            
        	final LinearLayout matrices = new LinearLayout(mContext);
            matrices.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            matrices.setId(R.id.matrices);
            
            ImageButton i = new ImageButton(mContext);
            i.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            i.setImageResource(R.drawable.matrix_add_drawable);
            i.setId(R.id.matrixAdd);
            i.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    View view = inflater.inflate(R.layout.matrix, null);
                    final LinearLayout theMatrix = (LinearLayout) view.findViewById(R.id.theMatrix);
                    final RelativeLayout matrixPopup = (RelativeLayout) view.findViewById(R.id.matrixPopup);
                    final LinearLayout matrixButtons = (LinearLayout) matrixPopup.findViewById(R.id.matrixButtons);
                    
                    builder.setView(view);
                    final AlertDialog alertDialog = builder.create();
                    
                    final ColorButton ok = (ColorButton) view.findViewById(R.id.ok);
                    ok.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (int i=0; i<theMatrix.getChildCount(); i++) {
                                LinearLayout layout = (LinearLayout) theMatrix.getChildAt(i);
                                for(int j=0; j<layout.getChildCount(); j++) {
                                    EditText view = (EditText) layout.getChildAt(j);
                                    if(view.getText().toString().equals("")){
                                        view.requestFocus();
                                        return;
                                    }
                                    view.setOnFocusChangeListener(new OnFocusChangeListener() {
                                        @Override
                                        public void onFocusChange(View v, boolean hasFocus) {
                                        	matrices.removeView(theMatrix);
                                        }
                                    });
                                }
                            }
                            theMatrix.setFocusable(true);
                            theMatrix.setFocusableInTouchMode(true);
                            theMatrix.requestFocus();
                            matrixPopup.removeView(theMatrix);
                            matrices.addView(theMatrix, matrices.getChildCount()-1);
                            alertDialog.dismiss();
                        }
                    });

                    final ColorButton plus = (ColorButton) view.findViewById(R.id.matrixPlus);
                    plus.setOnClickListener(new OnClickListener(){
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
                    mul.setOnClickListener(new OnClickListener(){
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
                    
                    alertDialog.show();
                }
            });
            
            matrices.addView(i);
            
            l.addView(matrices);
        }
        else{
        	 ViewGroup parent = (ViewGroup) l.getParent();
        	 parent.removeView(l);
        }
        
        return l;
    }
}