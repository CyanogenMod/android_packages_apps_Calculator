package com.android2.calculator3;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.model.XYSeries;
import org.achartengine.util.MathHelper;
import org.javia.arity.SyntaxException;

public class GraphModule {
    Logic mLogic;

    GraphModule(Logic logic) {
        this.mLogic = logic;
    }

    void updateGraphCatchErrors(Graph g) {
        try {
            updateGraph(g);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    void updateGraph(final Graph g) {
        if(g == null) return;
        final String eq = mLogic.getText();

        if(eq.isEmpty()) {
            XYSeries series = new XYSeries("");

            try {
                for(int i = 0; i < g.getDataset().getSeriesCount(); i++) {
                    g.getDataset().removeSeries(i);
                }
                g.getDataset().addSeries(series);
            }
            catch(NullPointerException e) {
                e.printStackTrace();
            }

            if(mLogic.mGraphDisplay != null) mLogic.mGraphDisplay.repaint();
            return;
        }

        if(Logic.isOperator(eq.charAt(eq.length() - 1)) || mLogic.displayContainsMatrices() || eq.endsWith("(")) return;

        final String[] equation = eq.split("=");

        if(equation.length != 2) return;

        // Translate into decimal
        equation[0] = mLogic.convertToDecimal(mLogic.localize(equation[0]));
        equation[1] = mLogic.convertToDecimal(mLogic.localize(equation[1]));
        final double minY = g.getRenderer().getYAxisMin();
        final double maxY = g.getRenderer().getYAxisMax();
        final double minX = g.getRenderer().getXAxisMin();
        final double maxX = g.getRenderer().getXAxisMax();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<XYSeries> series = new ArrayList<XYSeries>();
                    series.add(new XYSeries(""));
                    double lastX = (maxX - minX) / 2 + minX;
                    double lastY = (maxY - minY) / 2 + minY;

                    if(equation[0].equals(mLogic.mY) && !equation[1].contains(mLogic.mY)) {
                        for(double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mLogic.mSymbols.define(mLogic.mX, x);
                                double y = mLogic.mSymbols.eval(equation[1]);

                                if(pointIsNaN(lastY, y, maxY, minY)) {
                                    series.get(0).add(x, MathHelper.NULL_VALUE);
                                }
                                else {
                                    series.get(0).add(x, y);
                                }
                                lastY = y;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(equation[0].equals(mLogic.mX) && !equation[1].contains(mLogic.mX)) {
                        for(double y = minY; y <= maxY; y += (0.00125 * (maxY - minY))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mLogic.mSymbols.define(mLogic.mY, y);
                                double x = mLogic.mSymbols.eval(equation[1]);

                                if(pointIsNaN(lastX, x, maxX, minX)) {
                                    series.get(0).add(MathHelper.NULL_VALUE, y);
                                }
                                else {
                                    series.get(0).add(x, y);
                                }
                                lastX = x;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(equation[1].equals(mLogic.mY) && !equation[0].contains(mLogic.mY)) {
                        for(double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mLogic.mSymbols.define(mLogic.mX, x);
                                double y = mLogic.mSymbols.eval(equation[0]);

                                if(pointIsNaN(lastY, y, maxY, minY)) {
                                    series.get(0).add(x, MathHelper.NULL_VALUE);
                                }
                                else {
                                    series.get(0).add(x, y);
                                }
                                lastY = y;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(equation[1].equals(mLogic.mX) && !equation[0].contains(mLogic.mX)) {
                        for(double y = minY; y <= maxY; y += (0.00125 * (maxY - minY))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mLogic.mSymbols.define(mLogic.mY, y);
                                double x = mLogic.mSymbols.eval(equation[0]);

                                if(pointIsNaN(lastX, x, maxX, minX)) {
                                    series.get(0).add(MathHelper.NULL_VALUE, y);
                                }
                                else {
                                    series.get(0).add(x, y);
                                }
                                lastX = x;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        for(double x = minX; x <= maxX; x += (0.01 * (maxX - minX))) {
                            List<Double> values = new ArrayList<Double>();
                            for(double y = maxY; y >= minY; y -= (0.01 * (maxY - minY))) {
                                if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                                try {
                                    mLogic.mSymbols.define(mLogic.mX, x);
                                    mLogic.mSymbols.define(mLogic.mY, y);
                                    Double leftSide = mLogic.mSymbols.eval(equation[0]);
                                    Double rightSide = mLogic.mSymbols.eval(equation[1]);
                                    // TODO increase scale of graph as zooming
                                    // out
                                    if(leftSide < 0 && rightSide < 0) {
                                        if(leftSide * 0.97 >= rightSide && leftSide * 1.03 <= rightSide) {
                                            values.add(y);
                                        }
                                    }
                                    else {
                                        if(leftSide * 0.97 <= rightSide && leftSide * 1.03 >= rightSide) {
                                            values.add(y);
                                        }
                                    }
                                }
                                catch(SyntaxException e) {
                                    e.printStackTrace();
                                }
                            }

                            int color = g.getRenderer().getSeriesRendererAt(0).getColor();
                            while(values.size() > series.size()) {
                                series.add(new XYSeries(""));
                                Graph.addSeriesRenderer(color, g.getRenderer());
                            }

                            for(int i = 0; i < values.size(); i++) {
                                // TODO find closest value to previous one
                                series.get(i).add(x, values.get(i));
                            }

                            // // TODO needs a lot of work. very broken
                            // for(Double d : values) {
                            // // find closest value to previous one per
                            // // series
                            // XYSeries closestSeries = series.get(0);
                            // for(XYSeries s : series) {
                            // if(tolerance(closestSeries.getY(closestSeries.getItemCount()
                            // - 1), d) > tolerance(s.getY(s.getItemCount() -
                            // 1), d)) {
                            // closestSeries = s;
                            // }
                            // }
                            // closestSeries.add(x, d);
                            // }
                        }
                    }

                    for(int i = 0; i < g.getDataset().getSeriesCount(); i++) {
                        g.getDataset().removeSeries(0);
                    }
                    for(XYSeries s : series) {
                        g.getDataset().addSeries(s);
                    }

                    if(mLogic.mGraphDisplay != null) mLogic.mGraphDisplay.repaint();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static double tolerance(double result, double truth) {
        return(100.0 * Math.abs(truth - result) / Math.abs(truth));
    }

    boolean graphChanged(Graph graph, String equation, double minX, double maxX, double minY, double maxY) {
        return !equation.equals(mLogic.getText()) || minY != graph.getRenderer().getYAxisMin() || maxY != graph.getRenderer().getYAxisMax()
                || minX != graph.getRenderer().getXAxisMin() || maxX != graph.getRenderer().getXAxisMax();
    }

    boolean pointIsNaN(double lastV, double v, double max, double min) {
        return v == Double.NaN || v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY || lastV > max && v < min || v > max && lastV < min;
    }
}
