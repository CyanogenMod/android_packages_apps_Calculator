/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2009-2010 SC 4ViewSoft SRL
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.content.Context;

import com.android.calculator2.view.GraphView;
import com.xlythe.engine.theme.Theme;

import java.util.LinkedList;

public class Graph {
    private final Logic mLogic;
    private GraphView mGraphView;
    private LinkedList<GraphView.Point> mData = new LinkedList<GraphView.Point>();

    public Graph(Logic l) {
        mLogic = l;
    }

    public GraphView createGraph(Context context) {
        mLogic.setGraph(this);

        mGraphView = new GraphView(context);
        mGraphView.setPanListener(new GraphView.PanListener() {
            @Override
            public void panApplied() {
                mLogic.getGraphModule().updateGraph(Graph.this);
            }
        });

        mGraphView.setZoomListener(new GraphView.ZoomListener() {
            @Override
            public void zoomApplied(float level) {
                mLogic.getGraphModule().updateGraph(Graph.this);
            }
        });

        mGraphView.setBackgroundColor(Theme.getColor(context, R.color.graph_background));
        mGraphView.setTextColor(Theme.getColor(context, R.color.graph_labels_color));
        mGraphView.setGridColor(Theme.getColor(context, R.color.graph_grid_color));
        mGraphView.setGraphColor(Theme.getColor(context, R.color.graph_color));
        return mGraphView;
    }

    public void addData(float[] xValues, float[] yValues) {
        int seriesLength = xValues.length;
        for (int k = 0; k < seriesLength; k++) {
            mData.add(new GraphView.Point(xValues[k], yValues[k]));
        }

        mGraphView.setData(mData);
    }

    public void setData(LinkedList<GraphView.Point> data) {
        setData(data, true);
    }

    public void setData(LinkedList<GraphView.Point> data, boolean sort) {
        mData = data;
        mGraphView.setData(mData, sort);
    }

    public LinkedList<GraphView.Point> getData() {
        return mData;
    }
}
