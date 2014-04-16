/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.os.AsyncTask;
import android.util.Log;

import com.android.calculator2.view.GraphView;

import org.javia.arity.SyntaxException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GraphModule {
    Logic mLogic;
    GraphTask mGraphTask;

    GraphModule(Logic logic) {
        this.mLogic = logic;
    }

    void updateGraph(Graph graph) {
        final String equation = mLogic.getText();

        boolean panelDisabled = !CalculatorSettings.isPageEnabled(mLogic.getContext(),
                Page.NormalPanel.GRAPH);
        boolean endsWithOperator = equation.length() != 0
                && (Logic.isOperator(equation.charAt(equation.length() - 1))
                || equation.endsWith("("));
        boolean containsMatrices = mLogic.displayContainsMatrices();
        if (panelDisabled || endsWithOperator || containsMatrices) {
            return;
        }

        if (mGraphTask != null) {
            mGraphTask.cancel(true);
        }

        mGraphTask = new GraphTask(graph, mLogic);
        mGraphTask.execute(equation);
    }

    public static class GraphTask extends AsyncTask<String, String, GraphView> {
        private final Graph mGraph;
        private final Logic mLogic;

        public GraphTask(Graph graph, Logic logic) {
            mGraph = graph;
            mLogic = logic;
        }

        @Override
        protected GraphView doInBackground(String... eq) {
            final String[] equation = eq[0].split("=");

            if (mLogic == null || mGraph == null) {
                cancel(true);
                return null;
            }

            if (equation.length != 2) {
                mGraph.setData(new LinkedList<GraphView.Point>());
                return mLogic.mGraphView;
            }

            // Translate into decimal
            equation[0] = mLogic.convertToDecimal(mLogic.localize(equation[0]));
            equation[1] = mLogic.convertToDecimal(mLogic.localize(equation[1]));
            final double minY = mLogic.mGraphView.getYAxisMin();
            final double maxY = mLogic.mGraphView.getYAxisMax();
            final double minX = mLogic.mGraphView.getXAxisMin();
            final double maxX = mLogic.mGraphView.getXAxisMax();

            final LinkedList<GraphView.Point> series = new LinkedList<GraphView.Point>();
            if (equation[0].equals(mLogic.mY) && !equation[1].contains(mLogic.mY)) {
                for (double x = minX; x <= maxX; x += 0.01 * mLogic.mGraphView.getZoomLevel()) {
                    if (graphChanged(eq[0], minX, maxX, minY, maxY)) {
                        return null;
                    }

                    try {
                        mLogic.mSymbols.define(mLogic.mX, x);
                        double y = mLogic.mSymbols.eval(equation[1]);
                        series.add(new GraphView.Point(x, y));
                    } catch(SyntaxException e) {
                        // Do nothing here
                    }
                }

                mGraph.setData(series, false);
                return mLogic.mGraphView;
            } else if (equation[0].equals(mLogic.mX) && !equation[1].contains(mLogic.mX)) {
                for (double y = minY; y <= maxY; y += 0.01 * mLogic.mGraphView.getZoomLevel()) {
                    if (graphChanged(eq[0], minX, maxX, minY, maxY)) {
                        return null;
                    }

                    try {
                        mLogic.mSymbols.define(mLogic.mY, y);
                        double x = mLogic.mSymbols.eval(equation[1]);
                        series.add(new GraphView.Point(x, y));
                    } catch (SyntaxException e) {
                        // Do nothing here
                    }
                }

                mGraph.setData(series, false);
                return mLogic.mGraphView;
            } else if (equation[1].equals(mLogic.mY) && !equation[0].contains(mLogic.mY)) {
                for (double x = minX; x <= maxX; x += 0.01 * mLogic.mGraphView.getZoomLevel()) {
                    if (graphChanged(eq[0], minX, maxX, minY, maxY)) {
                        return null;
                    }

                    try {
                        mLogic.mSymbols.define(mLogic.mX, x);
                        double y = mLogic.mSymbols.eval(equation[0]);
                        series.add(new GraphView.Point(x, y));
                    } catch(SyntaxException e) {
                        // Do nothing here
                    }
                }

                mGraph.setData(series, false);
                return mLogic.mGraphView;
            } else if (equation[1].equals(mLogic.mX) && !equation[0].contains(mLogic.mX)) {
                for (double y = minY; y <= maxY; y += 0.01 * mLogic.mGraphView.getZoomLevel()) {
                    if (graphChanged(eq[0], minX, maxX, minY, maxY)) {
                        return null;
                    }

                    try {
                        mLogic.mSymbols.define(mLogic.mY, y);
                        double x = mLogic.mSymbols.eval(equation[0]);
                        series.add(new GraphView.Point(x, y));
                    } catch (SyntaxException e) {
                        // Do nothing here
                    }
                }

                mGraph.setData(series, false);
                return mLogic.mGraphView;
            } else {
                for (double x = minX; x <= maxX; x += 0.2 * mLogic.mGraphView.getZoomLevel()) {
                    List<Double> values = new ArrayList<Double>();
                    for (double y = maxY; y >= minY; y -= 0.2 * mLogic.mGraphView.getZoomLevel()) {
                        if (graphChanged(eq[0], minX, maxX, minY, maxY)) {
                            return null;
                        }

                        try {
                            mLogic.mSymbols.define(mLogic.mX, x);
                            mLogic.mSymbols.define(mLogic.mY, y);
                            Double leftSide = mLogic.mSymbols.eval(equation[0]);
                            Double rightSide = mLogic.mSymbols.eval(equation[1]);

                            // TODO: Increase scale of graph as zooming out
                            if (leftSide < 0 && rightSide < 0) {
                                if (leftSide * 0.98 >= rightSide && leftSide * 1.02 <= rightSide) {
                                    values.add(y);
                                }
                            } else {
                                if (leftSide * 0.98 <= rightSide && leftSide * 1.02 >= rightSide) {
                                    values.add(y);
                                }
                            }
                        } catch(SyntaxException e) {
                            e.printStackTrace();
                        }
                    }

                    for (int i = 0; i < values.size(); i++) {
                        series.add(new GraphView.Point(x, values.get(i)));
                    }
                }
            }

            mGraph.setData(series);
            return mLogic.mGraphView;
        }

        boolean graphChanged(String equation, double minX, double maxX, double minY, double maxY) {
            return isCancelled() || !equation.equals(mLogic.getText())
                    || minY != mLogic.mGraphView.getYAxisMin()
                    || maxY != mLogic.mGraphView.getYAxisMax()
                    || minX != mLogic.mGraphView.getXAxisMin()
                    || maxX != mLogic.mGraphView.getXAxisMax();
        }

        @Override
        protected void onPostExecute(GraphView result) {
            super.onPostExecute(result);

            if (result != null) {
                result.invalidate();
            }
        }
    }
}
