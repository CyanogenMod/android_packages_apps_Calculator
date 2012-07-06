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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.achartengine.GraphicalView;
import org.achartengine.model.SeriesSelection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnClickListener, OnMenuItemClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private ViewPager mPager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Graph mGraph;
    private List<View> matricesInEquation;

    static final int GRAPH_PANEL    = 0;
    static final int FUNCTION_PANEL = 1;
    static final int BASIC_PANEL    = 2;
    static final int ADVANCED_PANEL = 3;
    static final int MATRIX_PANEL   = 4;

    private static final String LOG_TAG = "Calculator";
    private static final boolean LOG_ENABLED = false;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        
        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.main);
        mPager = (ViewPager) findViewById(R.id.panelswitch);
        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
        } else {
            // Single page UI
            final TypedArray buttons = getResources().obtainTypedArray(R.array.buttons);
            for (int i = 0; i < buttons.length(); i++) {
                setOnClickListener(null, buttons.getResourceId(i, 0));
            }
            buttons.recycle();
        }

        if (mClearButton == null) {
            mClearButton = findViewById(R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }
        if (mBackspaceButton == null) {
            mBackspaceButton = findViewById(R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        if (mPager != null) {
            mPager.setCurrentItem(state == null ? BASIC_PANEL : state.getInt(STATE_CURRENT_VIEW, BASIC_PANEL));
        }

        mListener.setHandler(this, mLogic, mPager);
        mDisplay.setOnKeyListener(mListener);

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();
        
        mGraph = new Graph(mLogic);
    }

    private void updateDeleteMode() {
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }

    void setOnClickListener(View root, int id) {
        final View target = root != null ? root.findViewById(id) : findViewById(id);
        target.setOnClickListener(mListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if(mMatrixPanel != null) mMatrixPanel.setVisible(!getMatrixVisibility());
        
        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if(mGraphPanel != null) mGraphPanel.setVisible(!getGraphVisibility());
        
        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if(mFunctionPanel != null) mFunctionPanel.setVisible(!getFunctionVisibility());
        
        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if(mBasicPanel != null) mBasicPanel.setVisible(!getBasicVisibility());
        
        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if(mAdvancedPanel != null) mAdvancedPanel.setVisible(!getAdvancedVisibility());
        
        return true;
    }


    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.overflow_menu:
                PopupMenu menu = constructPopupMenu();
                if (menu != null) {
                    menu.show();
                }
                break;
        }
    }

    private PopupMenu constructPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    private boolean getGraphVisibility() {
        return mPager != null && mPager.getCurrentItem() == GRAPH_PANEL;
    }
    
    private boolean getFunctionVisibility() {
        return mPager != null && mPager.getCurrentItem() == FUNCTION_PANEL;
    }
    
    private boolean getBasicVisibility() {
        return mPager != null && mPager.getCurrentItem() == BASIC_PANEL;
    }

    private boolean getAdvancedVisibility() {
        return mPager != null && mPager.getCurrentItem() == ADVANCED_PANEL;
    }
    
    private boolean getMatrixVisibility() {
        return mPager != null && mPager.getCurrentItem() == MATRIX_PANEL;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_history:
                mHistory.clear();
                mLogic.onClear();
                break;

            case R.id.basic:
                if (!getBasicVisibility()) {
                    mPager.setCurrentItem(BASIC_PANEL);
                }
                break;

            case R.id.advanced:
                if (!getAdvancedVisibility()) {
                    mPager.setCurrentItem(ADVANCED_PANEL);
                }
                break;

            case R.id.function:
                if (!getFunctionVisibility()) {
                    mPager.setCurrentItem(FUNCTION_PANEL);
                }
                break;

            case R.id.graph:
                if (!getGraphVisibility()) {
                    mPager.setCurrentItem(GRAPH_PANEL);
                }
                break;

            case R.id.matrix:
                if (!getMatrixVisibility()) {
                    mPager.setCurrentItem(MATRIX_PANEL);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }
    }
    
    @Override
	public Object onRetainNonConfigurationInstance() {
    	LinearLayout matrices = (LinearLayout) ((PageAdapter) mPager.getAdapter()).mMatrixPage.findViewById(R.id.matrices);
    	matrices.removeAllViews();
    	return matricesInEquation;
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK && (getAdvancedVisibility() || getFunctionVisibility())) {
            mPager.setCurrentItem(BASIC_PANEL);
            return true;
        } else {
            return super.onKeyDown(keyCode, keyEvent);
        }
    }

    static void log(String message) {
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    @Override
    public void onChange() {
        invalidateOptionsMenu();
    }

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }

    class PageAdapter extends PagerAdapter {
        private View mGraphPage;
        private View mFunctionPage;
        private View mSimplePage;
        private View mAdvancedPage;
        public View mMatrixPage;
        private GraphicalView mChartView;

        public PageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View graphPage = inflater.inflate(R.layout.graph_pad, parent, false);
            final View functionPage = inflater.inflate(R.layout.function_pad, parent, false);
            final View simplePage = inflater.inflate(R.layout.simple_pad, parent, false);
            final View advancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);
            final View matrixPage = inflater.inflate(R.layout.matrix_pad, parent, false);
            final ImageButton addMatrix = (ImageButton) matrixPage.findViewById(R.id.matrixAdd);
            addMatrix.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final AlertDialog.Builder builder = new AlertDialog.Builder(Calculator.this);
					LayoutInflater inflater = getLayoutInflater();
					View view = inflater.inflate(R.layout.matrix, null);
					final LinearLayout matrices = (LinearLayout) matrixPage.findViewById(R.id.matrices);
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
											matricesInEquation.remove(theMatrix);
										}
									});
								}
							}
							theMatrix.setFocusable(true);
							theMatrix.setFocusableInTouchMode(true);
							theMatrix.requestFocus();
							matrixPopup.removeView(theMatrix);
							matrices.addView(theMatrix, matrices.getChildCount()-1);
							matricesInEquation.add(theMatrix);
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
									matricesInEquation.remove(v);
								}
							});
							matrixButtons.removeView(plus);
							matrices.addView(plus, matrices.getChildCount()-1);
							matricesInEquation.add(plus);
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
									matricesInEquation.remove(v);
								}
							});
							matrixButtons.removeView(mul);
							matrices.addView(mul, matrices.getChildCount()-1);
							matricesInEquation.add(mul);
							alertDialog.dismiss();
						}
					});
					
					alertDialog.show();
				}
			});
            matricesInEquation = (List<View>) getLastNonConfigurationInstance();
            if(matricesInEquation == null){
            	matricesInEquation = new ArrayList<View>();
            }
            else{
				final LinearLayout matrices = (LinearLayout) matrixPage.findViewById(R.id.matrices);
            	for(View v : matricesInEquation){
					matrices.addView(v, matrices.getChildCount()-1);
					
					if(v.getId() == R.id.matrixPlus){
						v.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								matrices.removeView(v);
								matricesInEquation.remove(v);
							}
						});
					}
					else if(v.getId() == R.id.matrixMul){
						v.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								matrices.removeView(v);
								matricesInEquation.remove(v);
							}
						});
					}
					else if(v.getId() == R.id.theMatrix){
						final LinearLayout theMatrix = (LinearLayout) v;
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
										matricesInEquation.remove(theMatrix);
									}
								});
							}
						}
						theMatrix.setFocusable(true);
						theMatrix.setFocusableInTouchMode(true);
						theMatrix.requestFocus();
					}
            	}
            }
            mGraphPage = graphPage;
            mFunctionPage = functionPage;
            mSimplePage = simplePage;
            mAdvancedPage = advancedPage;
            mMatrixPage = matrixPage;

            final Resources res = getResources();
            final TypedArray simpleButtons = res.obtainTypedArray(R.array.simple_buttons);
            for (int i = 0; i < simpleButtons.length(); i++) {
                setOnClickListener(simplePage, simpleButtons.getResourceId(i, 0));
            }
            simpleButtons.recycle();

            final TypedArray advancedButtons = res.obtainTypedArray(R.array.advanced_buttons);
            for (int i = 0; i < advancedButtons.length(); i++) {
                setOnClickListener(advancedPage, advancedButtons.getResourceId(i, 0));
            }
            advancedButtons.recycle();
            
            final TypedArray functionButtons = res.obtainTypedArray(R.array.function_buttons);
            for (int i = 0; i < functionButtons.length(); i++) {
                setOnClickListener(functionPage, functionButtons.getResourceId(i, 0));
            }
            functionButtons.recycle();

            final View clearButton = simplePage.findViewById(R.id.clear);
            if (clearButton != null) {
                mClearButton = clearButton;
            }

            final View backspaceButton = simplePage.findViewById(R.id.del);
            if (backspaceButton != null) {
                mBackspaceButton = backspaceButton;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == GRAPH_PANEL){
                if (mChartView == null) {
                    mChartView = mGraph.getGraph(Calculator.this);
                    mChartView.setId(R.id.graphView);
                    mChartView.setOnClickListener(new View.OnClickListener() {
                      @Override
                      public void onClick(View v) {
                        SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                        DecimalFormat formater = new DecimalFormat("#.#");
                        if(seriesSelection != null) Toast.makeText(Calculator.this, "(" + formater.format(seriesSelection.getXValue()) + "," + formater.format(seriesSelection.getValue()) + ")", Toast.LENGTH_SHORT).show();
                      }
                    });
                    ((LinearLayout) mGraphPage).addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                } 
                else {
                    mChartView.repaint();
                }
                ((ViewGroup) container).addView(mGraphPage);
                return mGraphPage;
            }
            else if(position == FUNCTION_PANEL){
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == BASIC_PANEL){
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == ADVANCED_PANEL){
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == MATRIX_PANEL){
                ((ViewGroup) container).addView(mMatrixPage);
                return mMatrixPage;
            }
            return null;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }
}
