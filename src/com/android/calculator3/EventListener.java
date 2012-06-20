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

package com.android.calculator3;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

class EventListener implements View.OnKeyListener,
                               View.OnClickListener,
                               View.OnLongClickListener {
    Context mContext;
	Logic mHandler;
    ViewPager mPager;
    LinearLayout mGraph;
    View mMatrix;
    GraphicalView mChartView;
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYSeries mCurrentSeries;

    void setHandler(Context context, Logic handler, ViewPager pager, LinearLayout graph, View matrix) {
        mContext = context;
    	mHandler = handler;
        mPager = pager;
        mGraph = graph;
        mMatrix = matrix;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
        case R.id.del:
            mHandler.onDelete();
            break;

        case R.id.clear:
            mHandler.onClear();
            break;

        case R.id.equal:
        	if (mHandler.getText().contains("X") || mHandler.getText().contains("Y") || mHandler.getText().contains("Z")) {
                if (!mHandler.getText().contains("=")) {
                	mHandler.insert("=");
                }
                break;
            }
            mHandler.onEnter();
            break;

        default:
            if (view instanceof Button) {
                String text = ((Button) view).getText().toString();
                if ((text.equals("dx")) || (text.equals("dy")) || (text.equals("dz"))){
                    
                }
                else if(text.equals("( )")){
                    text = "(" + mHandler.getText() + ")";
                    mHandler.clear(false);
                }
                else if(text.equals("mod")){
                	if(mHandler.getText().length()>0){
                		text = "mod("+mHandler.getText()+",";
                		mHandler.clear(false);
                	}
                	else{
                		text = "mod(";
                	}
                }
                else if(text.equals("Graph")){
                    if (mPager != null && mGraph != null) {
                        mGraph.setVisibility(View.VISIBLE);
                        if (mChartView == null) {
                        	mChartView = ChartFactory.getLineChartView(mContext, mDataset, mRenderer);
                            mRenderer.setClickEnabled(true);
                            mChartView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                  double[] xy = mChartView.toRealPoint(0);
                                  mCurrentSeries.add(xy[0], xy[1]);
                                  mChartView.repaint();
                                }
                            });
                            mRenderer.setSelectableBuffer(100);
                            mChartView.addPanListener(new PanListener() {
                              public void panApplied() {
                                System.out.println("New X range=[" + mRenderer.getXAxisMin() + ", " + mRenderer.getXAxisMax()
                                    + "], Y range=[" + mRenderer.getYAxisMax() + ", " + mRenderer.getYAxisMax() + "]");
                              }
                            });
                            mGraph.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
                            String seriesTitle = "Series " + (mDataset.getSeriesCount() + 1);
                            XYSeries series = new XYSeries(seriesTitle);
                            mCurrentSeries = series;
                            mDataset.addSeries(series);
                            XYSeriesRenderer renderer = new XYSeriesRenderer();
                            mRenderer.addSeriesRenderer(renderer);
                            renderer.setPointStyle(PointStyle.CIRCLE);
                            renderer.setFillPoints(true);
                        } 
                        else {
                            mChartView.repaint();
                        }
                        mPager.setVisibility(View.GONE);
                    }
                    return;
                }
                else if(text.equals("Matrix")){
//                    if (mPager != null && mMatrix != null) {
//                        mPager.setVisibility(View.GONE);
//                        mMatrix.setVisibility(View.VISIBLE);
//                    }
                	
                	mContext.startActivity(new Intent(mContext, GraphActivity.class));
                    return;
                }
                else if(text.equals("Solve for X")){
                	if(mHandler.getText().contains("X")){
                    	mHandler.onEnter();
                	}
                    return;
                }
                else if(text.equals("Solve for Y")){
                	if(mHandler.getText().contains("Y")){
                    	mHandler.onEnter();
                	}
                    return;
                }
                else if (text.length() >= 2) {
                    // add paren after sin, cos, ln, etc. from buttons
                    text += '(';
                }
                mHandler.insert(text);
                if (mPager != null && (mPager.getCurrentItem() == Calculator.ADVANCED_PANEL || mPager.getCurrentItem() == Calculator.FUNCTION_PANEL)) {
                    mPager.setCurrentItem(Calculator.BASIC_PANEL);
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();
        if (id == R.id.del) {
            mHandler.onClear();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        int action = keyEvent.getAction();

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            boolean eat = mHandler.eatHorizontalMove(keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
            return eat;
        }

        //Work-around for spurious key event from IME, bug #1639445
        if (action == KeyEvent.ACTION_MULTIPLE && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return true; // eat it
        }
        
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (mHandler.getText().endsWith("sin(") || 
                mHandler.getText().endsWith("cos(") ||
                mHandler.getText().endsWith("tan(") ||
                mHandler.getText().endsWith("log(") || 
                mHandler.getText().endsWith("mod(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-4);
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if (mHandler.getText().endsWith("ln(")){
            	String text = mHandler.getText().substring(0, mHandler.getText().length()-3);
                mHandler.clear(false);
                mHandler.insert(text);
            }
            else if (mHandler.getText().endsWith("dx") ||
                    mHandler.getText().endsWith("dy") ||
                    mHandler.getText().endsWith("dz")){
                    String text = mHandler.getText().substring(0, mHandler.getText().length()-2);
                    mHandler.clear(false);
                    mHandler.insert(text);
                    return true;
                }
            return false;
        }

        //Calculator.log("KEY " + keyCode + "; " + action);

        if (keyEvent.getUnicodeChar() == '=') {
            if (action == KeyEvent.ACTION_UP) {
                mHandler.onEnter();
            }
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
            keyCode != KeyEvent.KEYCODE_DPAD_UP &&
            keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
            keyCode != KeyEvent.KEYCODE_ENTER) {
            if (keyEvent.isPrintingKey() && action == KeyEvent.ACTION_UP) {
                // Tell the handler that text was updated.
                mHandler.onTextChanged();
            }
            return false;
        }

        /*
           We should act on KeyEvent.ACTION_DOWN, but strangely
           sometimes the DOWN event isn't received, only the UP.
           So the workaround is to act on UP...
           http://b/issue?id=1022478
         */

        if (action == KeyEvent.ACTION_UP) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                mHandler.onEnter();
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                mHandler.onUp();
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                mHandler.onDown();
                break;
            }
        }
        return true;
    }
}
