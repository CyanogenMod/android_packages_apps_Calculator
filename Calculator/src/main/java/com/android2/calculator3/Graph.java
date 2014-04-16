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

import android.content.Context;
import android.graphics.Paint.Align;

import com.android2.calculator3.view.GraphView;
import com.xlythe.engine.theme.Theme;

import java.util.LinkedList;
import java.util.List;

public class Graph {
    public static final double MAX_HEIGHT_X = 10;
    public static final double MAX_HEIGHT_Y = 10;
    public static final double MIN_HEIGHT_X = -10;
    public static final double MIN_HEIGHT_Y = -10;
    private final Logic mLogic;
    private GraphView mGraphView;
    private LinkedList<GraphView.Point> mData;

    public Graph(Logic l) {
        mLogic = l;
    }

    public GraphView getGraph(Context context) {
        String title = "";
        double[] xValues = new double[0];
        double[] yValues = new double[0];

        mLogic.setGraph(this);

        mGraphView = new GraphView(context);
        mGraphView.setPanListener(new GraphView.PanListener() {
            @Override
            public void panApplied() {
                mLogic.getGraphModule().updateGraphCatchErrors(Graph.this);
            }
        });
        mGraphView.setZoomListener(new GraphView.ZoomListener() {
            @Override
            public void zoomApplied(int level) {
                mLogic.getGraphModule().updateGraphCatchErrors(Graph.this);
            }
        });

        mLogic.getGraphModule().updateGraphCatchErrors(this);

        mGraphView.setBackgroundColor(Theme.getColor(context, R.color.graph_background));
        mGraphView.setTextColor(Theme.getColor(context, R.color.graph_labels_color));
        mGraphView.setGridColor(Theme.getColor(context, R.color.graph_grid_color));
        mGraphView.setGraphColor(Theme.getColor(context, R.color.graph_color));
        return mGraphView;
    }

    private void addData(float[] xValues, float[] yValues) {
        int seriesLength = xValues.length;
        for (int k = 0; k < seriesLength; k++) {
            mData.add(new GraphView.Point(xValues[k], yValues[k]));
        }
        mGraphView.setData(mData);
    }

    public LinkedList<GraphView.Point> getData() {
        return mData;
    }
}
