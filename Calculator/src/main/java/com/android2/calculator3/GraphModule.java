package com.android2.calculator3;

import android.os.AsyncTask;
import android.util.Log;

import com.android2.calculator3.view.GraphView;

import org.javia.arity.SyntaxException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GraphModule {
    Logic mLogic;

    GraphModule(Logic logic) {
        this.mLogic = logic;
    }

    void updateGraph(Graph graph) {
        final String equation = mLogic.getText();

        if ((equation.length() != 0 && Logic.isOperator(equation.charAt(equation.length() - 1))) || mLogic.displayContainsMatrices() || equation.endsWith("(")) {
            return;
        }

        new GraphTask(graph, mLogic).execute(equation);
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
            double lastX = (maxX - minX) / 2 + minX;
            double lastY = (maxY - minY) / 2 + minY;

            if (equation[0].equals(mLogic.mY) && !equation[1].contains(mLogic.mY)) {
                for (double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                    if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                    try {
                        mLogic.mSymbols.define(mLogic.mX, x);
                        double y = mLogic.mSymbols.eval(equation[1]);

                        if (pointIsNaN(lastY, y, maxY, minY)) {
                            series.add(new GraphView.Point(x, GraphView.NULL_VALUE));
                        } else {
                            series.add(new GraphView.Point(x, y));
                        }
                        lastY = y;
                    } catch (SyntaxException e) {
                        e.printStackTrace();
                    }
                }
            } else if (equation[0].equals(mLogic.mX) && !equation[1].contains(mLogic.mX)) {
                for (double y = minY; y <= maxY; y += (0.00125 * (maxY - minY))) {
                    if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                    try {
                        mLogic.mSymbols.define(mLogic.mY, y);
                        double x = mLogic.mSymbols.eval(equation[1]);

                        if (pointIsNaN(lastX, x, maxX, minX)) {
                            series.add(new GraphView.Point(GraphView.NULL_VALUE, y));
                        } else {
                            series.add(new GraphView.Point(x, y));
                        }
                        lastX = x;
                    } catch (SyntaxException e) {
                        e.printStackTrace();
                    }
                }
            } else if (equation[1].equals(mLogic.mY) && !equation[0].contains(mLogic.mY)) {
                for (double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                    if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                    try {
                        mLogic.mSymbols.define(mLogic.mX, x);
                        double y = mLogic.mSymbols.eval(equation[0]);

                        if (pointIsNaN(lastY, y, maxY, minY)) {
                            series.add(new GraphView.Point(x, GraphView.NULL_VALUE));
                        } else {
                            series.add(new GraphView.Point(x, y));
                        }
                        lastY = y;
                    } catch (SyntaxException e) {
                        e.printStackTrace();
                    }
                }
            } else if (equation[1].equals(mLogic.mX) && !equation[0].contains(mLogic.mX)) {
                for (double y = minY; y <= maxY; y += (0.00125 * (maxY - minY))) {
                    if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                    try {
                        mLogic.mSymbols.define(mLogic.mY, y);
                        double x = mLogic.mSymbols.eval(equation[0]);

                        if (pointIsNaN(lastX, x, maxX, minX)) {
                            series.add(new GraphView.Point(GraphView.NULL_VALUE, y));
                        } else {
                            series.add(new GraphView.Point(x, y));
                        }
                        lastX = x;
                    } catch (SyntaxException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                for (double x = minX; x <= maxX; x += (0.005 * (maxX - minX))) {
                    List<Double> values = new ArrayList<Double>();
                    for (double y = maxY; y >= minY; y -= (0.005 * (maxY - minY))) {
                        if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                        try {
                            mLogic.mSymbols.define(mLogic.mX, x);
                            mLogic.mSymbols.define(mLogic.mY, y);
                            Double leftSide = mLogic.mSymbols.eval(equation[0]);
                            Double rightSide = mLogic.mSymbols.eval(equation[1]);
                            // TODO increase scale of graph as zooming
                            // out
                            if (leftSide < 0 && rightSide < 0) {
                                if (leftSide * 0.99 >= rightSide && leftSide * 1.01 <= rightSide) {
                                    values.add(y);
                                }
                            } else {
                                if (leftSide * 0.99 <= rightSide && leftSide * 1.01 >= rightSide) {
                                    values.add(y);
                                }
                            }
                        } catch (SyntaxException e) {
                            e.printStackTrace();
                        }
                    }

                    for (int i = 0; i < values.size(); i++) {
                        series.add(new GraphView.Point(x, values.get(i)));
                    }
                }
            }

            System.out.println("Setting data");
            mGraph.setData(series);
            return mLogic.mGraphView;
        }

        private double tolerance(double result, double truth) {
            return (100.0 * Math.abs(truth - result) / Math.abs(truth));
        }

        boolean graphChanged(Graph graph, String equation, double minX, double maxX, double minY, double maxY) {
            return !equation.equals(mLogic.getText()) || minY != mLogic.mGraphView.getYAxisMin() || maxY != mLogic.mGraphView.getYAxisMax()
                    || minX != mLogic.mGraphView.getXAxisMin() || maxX != mLogic.mGraphView.getXAxisMax();
        }

        boolean pointIsNaN(double lastV, double v, double max, double min) {
            return v == Double.NaN || v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY || lastV > max && v < min || v > max && lastV < min;
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
