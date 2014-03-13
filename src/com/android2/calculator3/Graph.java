/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    
 *            http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android2.calculator3;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import android.content.Context;
import android.graphics.Paint.Align;

import com.xlythe.engine.theme.Theme;

public class Graph {
    private static final double MAX_HEIGHT_X = 10;
    private static final double MAX_HEIGHT_Y = 10;
    private static final double MIN_HEIGHT_X = -10;
    private static final double MIN_HEIGHT_Y = -10;

    private GraphicalView mChartView;
    private XYMultipleSeriesDataset mDataset;
    private XYMultipleSeriesRenderer mRenderer;
    private final Logic mLogic;

    public Graph(Logic l) {
        mLogic = l;
    }

    public XYMultipleSeriesDataset getDataset() {
        return mDataset;
    }

    public XYMultipleSeriesRenderer getRenderer() {
        return mRenderer;
    }

    public GraphicalView getGraph(Context context) {
        String title = "";
        double[] xValues = new double[0];
        double[] yValues = new double[0];
        mRenderer = buildRenderer(context);
        mDataset = buildDataset(title, xValues, yValues);

        mLogic.setGraph(this);

        mChartView = ChartFactory.getLineChartView(context, mDataset, mRenderer);
        mChartView.addPanListener(new PanListener() {
            @Override
            public void panApplied() {
                mLogic.getGraphModule().updateGraphCatchErrors(Graph.this);
            }
        });
        mChartView.addZoomListener(new ZoomListener() {
            @Override
            public void zoomReset() {
                mLogic.getGraphModule().updateGraphCatchErrors(Graph.this);
            }

            @Override
            public void zoomApplied(ZoomEvent event) {
                mLogic.getGraphModule().updateGraphCatchErrors(Graph.this);
            }
        }, true, true);

        mLogic.getGraphModule().updateGraphCatchErrors(this);

        return mChartView;
    }

    private XYMultipleSeriesDataset buildDataset(String title, double[] xValues, double[] yValues) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        addXYSeries(dataset, title, xValues, yValues, 0);
        return dataset;
    }

    private void addXYSeries(XYMultipleSeriesDataset dataset, String title, double[] xValues, double[] yValues, int scale) {
        XYSeries series = new XYSeries(title, scale);
        int seriesLength = xValues.length;
        for(int k = 0; k < seriesLength; k++) {
            series.add(xValues[k], yValues[k]);
        }
        dataset.addSeries(series);
    }

    private XYMultipleSeriesRenderer buildRenderer(Context context) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(0);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(20);
        renderer.setLegendHeight(22);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] { 20, 30, 15, 20 });
        renderer.setMarginsColor(Theme.getColor(context, R.color.background));
        renderer.setChartTitle("");
        renderer.setXTitle(context.getResources().getString(R.string.X));
        renderer.setYTitle(context.getResources().getString(R.string.Y));
        renderer.setXAxisMin(Graph.MIN_HEIGHT_X);
        renderer.setXAxisMax(Graph.MAX_HEIGHT_X);
        renderer.setYAxisMin(Graph.MIN_HEIGHT_Y);
        renderer.setYAxisMax(Graph.MAX_HEIGHT_Y);
        renderer.setAxesColor(Theme.getColor(context, R.color.graph_axes_color));
        renderer.setLabelsColor(Theme.getColor(context, R.color.graph_labels_color));
        renderer.setGridColor(Theme.getColor(context, R.color.graph_grid_color));
        renderer.setXLabelsColor(Theme.getColor(context, R.color.graph_labels_color));
        renderer.setYLabelsColor(0, Theme.getColor(context, R.color.graph_labels_color));
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setXLabels(20);
        renderer.setYLabels(20);
        renderer.setPanEnabled(true);
        renderer.setZoomEnabled(true);
        renderer.setShowGrid(true);
        renderer.setXAxisBold(true);
        renderer.setYAxisBold(true);
        renderer.setZoomButtonsVisible(false);
        renderer.setExternalZoomEnabled(true);
        addSeriesRenderer(context.getResources().getColor(R.color.graph_color), renderer);
        return renderer;
    }

    public static void addSeriesRenderer(int color, XYMultipleSeriesRenderer renderer) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(color);
        r.setPointStyle(PointStyle.POINT);
        r.setLineWidth(4f);
        renderer.addSeriesRenderer(r);
    }
}
