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
package com.android.calculator3;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;

public class Graph {
    private XYMultipleSeriesDataset mDataset;
    private XYSeries mSeries;
    private Logic mLogic;
    
    public Graph(Logic l){
    	mLogic = l;
    }
    
    public XYMultipleSeriesDataset getDataset() {
        return mDataset;
    }
    
    public XYSeries getSeries() {
    	return mSeries;
    }
    
    public void setSeries(XYSeries series) {
    	mSeries = series;
    }
    
    public GraphicalView getGraph(Context context) {
        String title = context.getResources().getString(R.string.defaultGraphTitle);
        double[] xValues = new double[0];
        double[] yValues = new double[0];
        XYMultipleSeriesRenderer renderer = buildRenderer(Color.CYAN, PointStyle.POINT);
        setChartSettings(renderer, title, "X", "Y", -10, 10, -10, 10, Color.GRAY, Color.LTGRAY);
        renderer.setXLabels(20);
        renderer.setYLabels(20);
        mDataset = buildDataset(title, xValues, yValues);
        
        mLogic.setGraph(this);
        mLogic.updateGraph(this, mLogic.getText());
        return ChartFactory.getLineChartView(context, mDataset, renderer);
    }
    
    public XYMultipleSeriesDataset buildDataset(String title, double[] xValues, double[] yValues) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        addXYSeries(dataset, title, xValues, yValues, 0);
        return dataset;
    }
    
    public void addXYSeries(XYMultipleSeriesDataset dataset, String title, double[] xValues, double[] yValues, int scale) {
        mSeries = new XYSeries(title, scale);
        int seriesLength = xValues.length;
        for (int k = 0; k < seriesLength; k++) {
        	mSeries.add(xValues[k], yValues[k]);
        }
        dataset.addSeries(mSeries);
    }
    
    /**
     * Builds an XY multiple series renderer.
     * 
     * @param colors the series rendering colors
     * @param styles the series point styles
     * @return the XY multiple series renderers
     */
    protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        setRenderer(renderer, color, style);
        return renderer;
    }

    protected void setRenderer(XYMultipleSeriesRenderer renderer, int color, PointStyle style) {
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] { 20, 30, 15, 20 });
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(color);
        r.setPointStyle(style);
        renderer.addSeriesRenderer(r);
    }

    /**
     * Sets a few of the series renderer settings.
     * 
     * @param renderer the renderer to set the properties to
     * @param title the chart title
     * @param xTitle the title for the X axis
     * @param yTitle the title for the Y axis
     * @param xMin the minimum value on the X axis
     * @param xMax the maximum value on the X axis
     * @param yMin the minimum value on the Y axis
     * @param yMax the maximum value on the Y axis
     * @param axesColor the axes color
     * @param labelsColor the labels color
     */
    protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle, 
            String yTitle, double xMin, double xMax, double yMin, double yMax, int axesColor, 
            int labelsColor) {
        renderer.setChartTitle(title);
        renderer.setXTitle(xTitle);
        renderer.setYTitle(yTitle);
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
    }
}