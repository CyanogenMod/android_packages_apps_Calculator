package com.android.calculator2;

import android.view.View;
import com.android.calculator2.view.DisplayOverlay;
import com.android.calculator2.view.DisplayOverlay.DisplayMode;
import com.android.calculator2.view.DisplayOverlay.TranslateStateListener;
import com.android.calculator2.view.DisplayOverlay.TranslateState;
import com.android.calculator2.view.GraphView;
import com.android.calculator2.view.GraphView.PanListener;
import com.android.calculator2.view.GraphView.ZoomListener;
import com.xlythe.math.GraphModule;
import com.xlythe.math.GraphModule.OnGraphUpdatedListener;
import com.xlythe.math.Point;

import java.util.ArrayList;
import java.util.List;

public class GraphController implements
        OnGraphUpdatedListener, TranslateStateListener, PanListener, ZoomListener,
        View.OnClickListener {

    private GraphView mGraphView;
    private GraphModule mGraphModule;
    private DisplayOverlay mDisplayOverlay;

    private View mExitControl;
    private View mZoomInControl;
    private View mZoomOutControl;
    private View mZoomResetControl;

    private String mEquation;
    private List<Point> mSeries;
    private boolean mIsGraphViewReady;

    public GraphController(GraphView view, GraphModule module, DisplayOverlay overlay) {
        mGraphView = view;
        mGraphModule = module;
        mDisplayOverlay = overlay;

        view.setPanListener(this);
        view.setZoomListener(this);

        mExitControl = overlay.findViewById(R.id.exitGraph);
        mExitControl.setOnClickListener(this);

        mZoomInControl = overlay.findViewById(R.id.minusZoom);
        mZoomInControl.setOnClickListener(this);

        mZoomOutControl = overlay.findViewById(R.id.plusZoom);
        mZoomOutControl.setOnClickListener(this);

        mZoomResetControl = overlay.findViewById(R.id.resetZoom);
        mZoomResetControl.setOnClickListener(this);
    }

    private void resetState() {
        mEquation = null;
        mSeries = null;
        mIsGraphViewReady = false;
        mGraphView.zoomReset();
    }

    public void startGraph(String equation) {
        resetState();
        mEquation = equation;
        setDomainAndRange();

        // start calculating series now but don't set data on graph view until display
        // overlay has settled in the expanded position.  This prevents jank while the
        // display is opening.
        mGraphModule.updateGraph(equation, this);
        mDisplayOverlay.setTranslateStateListener(this);
        mDisplayOverlay.setMode(DisplayMode.GRAPH);
        mDisplayOverlay.animateModeTransition();
    }

    private void setDomainAndRange() {
        mGraphModule.setDomain(mGraphView.getXAxisMin(), mGraphView.getXAxisMax());
        mGraphModule.setRange(mGraphView.getYAxisMin(), mGraphView.getYAxisMax());
        mGraphModule.setZoomLevel(mGraphView.getZoomLevel());
    }

    private void setGraphDataIfReady() {
        if (mIsGraphViewReady && mSeries != null) {
            mGraphView.setData(mSeries);
            mGraphView.invalidate();
        }
    }

    public void exitGraphMode() {
        mGraphView.setData(new ArrayList<Point>());
        mGraphView.invalidate();
        mDisplayOverlay.setMode(DisplayMode.FORMULA);
        mDisplayOverlay.animateModeTransition();
        resetState();
    }

    @Override
    public void onGraphUpdated(List<Point> result) {
        mSeries = result;
        setGraphDataIfReady();
    }

    @Override
    public void onTranslateStateChanged(TranslateState newState) {
        if (mDisplayOverlay.getMode() == DisplayMode.GRAPH) {
            if (newState == TranslateState.EXPANDED) {
                mIsGraphViewReady = true;
                setGraphDataIfReady();
            } else if (newState == TranslateState.COLLAPSED) {
                exitGraphMode();
            }
        }
    }

    @Override
    public void panApplied() {
        updateForPanOrZoom();
    }

    @Override
    public void zoomApplied(float level) {
        updateForPanOrZoom();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.exitGraph:
                exitGraphMode();
                break;
            case R.id.minusZoom:
                mGraphView.zoomOut();
                break;
            case R.id.plusZoom:
                mGraphView.zoomIn();
                break;
            case R.id.resetZoom:
                mGraphView.zoomReset();
                break;
        }
    }

    private void updateForPanOrZoom() {
        if (mEquation != null) {
            setDomainAndRange();
            mIsGraphViewReady = true;
            mGraphModule.updateGraph(mEquation, this);
        }
    }
}
