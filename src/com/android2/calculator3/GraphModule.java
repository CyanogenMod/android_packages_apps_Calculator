package com.android2.calculator3;

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
                g.getDataset().removeSeries(g.getSeries());
                g.setSeries(series);
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
                    final XYSeries series = new XYSeries("");
                    double lastX = (maxX - minX) / 2 + minX;
                    double lastY = (maxY - minY) / 2 + minY;

                    if(equation[0].equals(mLogic.mY) && !equation[1].contains(mLogic.mY)) {
                        for(double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mLogic.mSymbols.define(mLogic.mX, x);
                                double y = mLogic.mSymbols.eval(equation[1]);

                                if(pointIsNaN(lastY, y, maxY, minY)) {
                                    series.add(x, MathHelper.NULL_VALUE);
                                }
                                else {
                                    series.add(x, y);
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
                                    series.add(MathHelper.NULL_VALUE, y);
                                }
                                else {
                                    series.add(x, y);
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
                                    series.add(x, MathHelper.NULL_VALUE);
                                }
                                else {
                                    series.add(x, y);
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
                                    series.add(MathHelper.NULL_VALUE, y);
                                }
                                else {
                                    series.add(x, y);
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
                            for(double y = maxY; y >= minY; y -= (0.01 * (maxY - minY))) {
                                if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                                try {
                                    mLogic.mSymbols.define(mLogic.mX, x);
                                    mLogic.mSymbols.define(mLogic.mY, y);
                                    Double leftSide = mLogic.mSymbols.eval(equation[0]);
                                    Double rightSide = mLogic.mSymbols.eval(equation[1]);
                                    if(leftSide < 0 && rightSide < 0) {
                                        if(leftSide * 0.97 >= rightSide && leftSide * 1.03 <= rightSide) {
                                            series.add(x, y);
                                            break;
                                        }
                                    }
                                    else {
                                        if(leftSide * 0.97 <= rightSide && leftSide * 1.03 >= rightSide) {
                                            series.add(x, y);
                                            break;
                                        }
                                    }
                                }
                                catch(SyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    try {
                        g.getDataset().removeSeries(g.getSeries());
                    }
                    catch(NullPointerException e) {
                        e.printStackTrace();
                    }
                    g.setSeries(series);
                    g.getDataset().addSeries(series);

                    if(mLogic.mGraphDisplay != null) mLogic.mGraphDisplay.repaint();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    boolean graphChanged(Graph graph, String equation, double minX, double maxX, double minY, double maxY) {
        return !equation.equals(mLogic.getText()) || minY != graph.getRenderer().getYAxisMin() || maxY != graph.getRenderer().getYAxisMax()
                || minX != graph.getRenderer().getXAxisMin() || maxX != graph.getRenderer().getXAxisMax();
    }

    boolean pointIsNaN(double lastV, double v, double max, double min) {
        return v == Double.NaN || v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY || lastV > max && v < min || v > max && lastV < min;
    }
}
