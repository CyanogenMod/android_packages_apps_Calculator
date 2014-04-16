package com.android2.calculator3;

import android.os.AsyncTask;

import com.android2.calculator3.view.GraphView;

import org.achartengine.GraphicalView;
import org.achartengine.model.XYSeries;
import org.achartengine.util.MathHelper;
import org.javia.arity.SyntaxException;

import java.util.ArrayList;
import java.util.List;

public class GraphModule {
    Logic mLogic;
    GraphView mGraphView;

    GraphModule(Logic logic) {
        this.mLogic = logic;
    }

    private static void clearGraph(Graph graph) {
        graph.getData().clear();
    }

    void updateGraph(final Graph g) {
        final String eq = mLogic.getText();

        if (eq.isEmpty()) {
            mGraphView.invalidate();
            return;
        }

        if (Logic.isOperator(eq.charAt(eq.length() - 1)) || mLogic.displayContainsMatrices() || eq.endsWith("("))
            return;

        new GraphTask(g, mLogic).execute(eq);
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
                cancel(true);
                return null;
            }

            // Translate into decimal
            equation[0] = mLogic.convertToDecimal(mLogic.localize(equation[0]));
            equation[1] = mLogic.convertToDecimal(mLogic.localize(equation[1]));
            final double minY = mGraph.getRenderer().getYAxisMin();
            final double maxY = mGraph.getRenderer().getYAxisMax();
            final double minX = mGraph.getRenderer().getXAxisMin();
            final double maxX = mGraph.getRenderer().getXAxisMax();

            try {
                final List<XYSeries> series = new ArrayList<XYSeries>();
                series.add(new XYSeries(""));
                double lastX = (maxX - minX) / 2 + minX;
                double lastY = (maxY - minY) / 2 + minY;

                if (equation[0].equals(mLogic.mY) && !equation[1].contains(mLogic.mY)) {
                    for (double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                        if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                        try {
                            mLogic.mSymbols.define(mLogic.mX, x);
                            double y = mLogic.mSymbols.eval(equation[1]);

                            if (pointIsNaN(lastY, y, maxY, minY)) {
                                series.get(0).add(x, MathHelper.NULL_VALUE);
                            } else {
                                series.get(0).add(x, y);
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
                                series.get(0).add(MathHelper.NULL_VALUE, y);
                            } else {
                                series.get(0).add(x, y);
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
                                series.get(0).add(x, MathHelper.NULL_VALUE);
                            } else {
                                series.get(0).add(x, y);
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
                                series.get(0).add(MathHelper.NULL_VALUE, y);
                            } else {
                                series.get(0).add(x, y);
                            }
                            lastX = x;
                        } catch (SyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    for (double x = minX; x <= maxX; x += (0.01 * (maxX - minX))) {
                        List<Double> values = new ArrayList<Double>();
                        for (double y = maxY; y >= minY; y -= (0.01 * (maxY - minY))) {
                            if (graphChanged(mGraph, eq[0], minX, maxX, minY, maxY)) return null;

                            try {
                                mLogic.mSymbols.define(mLogic.mX, x);
                                mLogic.mSymbols.define(mLogic.mY, y);
                                Double leftSide = mLogic.mSymbols.eval(equation[0]);
                                Double rightSide = mLogic.mSymbols.eval(equation[1]);
                                // TODO increase scale of graph as zooming
                                // out
                                if (leftSide < 0 && rightSide < 0) {
                                    if (leftSide * 0.97 >= rightSide && leftSide * 1.03 <= rightSide) {
                                        values.add(y);
                                    }
                                } else {
                                    if (leftSide * 0.97 <= rightSide && leftSide * 1.03 >= rightSide) {
                                        values.add(y);
                                    }
                                }
                            } catch (SyntaxException e) {
                                e.printStackTrace();
                            }
                        }

                        int color = mGraph.getRenderer().getSeriesRendererAt(0).getColor();
                        while (values.size() > series.size()) {
                            series.add(new XYSeries(""));
                            Graph.addSeriesRenderer(color, mGraph.getRenderer());
                        }

                        for (int i = 0; i < values.size(); i++) {
                            // TODO find closest value to previous one
                            series.get(i).add(x, values.get(i));
                        }
                    }
                }

                clearGraph(mGraph);
                for (XYSeries s : series) {
                    mGraph.getDataset().addSeries(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mLogic.mGraphDisplay;
        }

        private double tolerance(double result, double truth) {
            return (100.0 * Math.abs(truth - result) / Math.abs(truth));
        }

        boolean graphChanged(Graph graph, String equation, double minX, double maxX, double minY, double maxY) {
            return !equation.equals(mLogic.getText()) || minY != graph.getRenderer().getYAxisMin() || maxY != graph.getRenderer().getYAxisMax()
                    || minX != graph.getRenderer().getXAxisMin() || maxX != graph.getRenderer().getXAxisMax();
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
