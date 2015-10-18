package com.xlythe.math;

import android.os.AsyncTask;

import org.javia.arity.SyntaxException;

import java.util.LinkedList;
import java.util.List;

public class GraphModule extends Module {
    private GraphTask mGraphTask;
    private double mMinY;
    private double mMaxY;
    private double mMinX;
    private double mMaxX;
    private float mZoomLevel;

    public GraphModule(Solver solver) {
        super(solver);
    }

    public void setRange(float min, float max) {
        mMinY = min;
        mMaxY = max;
    }

    public void setDomain(float min, float max) {
        mMinX = min;
        mMaxX = max;
    }

    public void setZoomLevel(float level) {
        mZoomLevel = level;
    }

    public void updateGraph(String text, OnGraphUpdatedListener l) {
        boolean endsWithOperator = text.length() != 0 &&
                (Solver.isOperator(text.charAt(text.length() - 1)) || text.endsWith("("));
        boolean containsMatrices = getSolver().displayContainsMatrices(text);
        if(endsWithOperator || containsMatrices) {
            return;
        }

        if(mGraphTask != null) mGraphTask.cancel(true);
        mGraphTask = new GraphTask(getSolver(), mMinY, mMaxY, mMinX, mMaxX, mZoomLevel, l);
        mGraphTask.execute(text);
    }

    class GraphTask extends AsyncTask<String, String, List<Point>> {
        private final Solver mSolver;
        private final OnGraphUpdatedListener mListener;
        private final double mMinY;
        private final double mMaxY;
        private final double mMinX;
        private final double mMaxX;
        private final float mZoomLevel;

        public GraphTask(Solver solver, double minY, double maxY, double minX, double maxX,
                         float zoomLevel, OnGraphUpdatedListener l) {
            mSolver = solver;
            mListener = l;
            mMinY = minY;
            mMaxY = maxY;
            mMinX = minX;
            mMaxX = maxX;
            mZoomLevel = zoomLevel;
        }

        @Override
        protected List<Point> doInBackground(String... eq) {
            try {
                return graph(mSolver.getBaseModule().updateTextToNewMode(eq[0],
                        mSolver.getBaseModule().getBase(), Base.DECIMAL));
            } catch(SyntaxException e) {
                cancel(true);
                return null;
            }
        }

        public List<Point> graph(String equation) {
            final LinkedList<Point> series = new LinkedList<Point>();

            mSolver.mSymbols.pushFrame();
            for(double x = mMinX; x <= mMaxX; x += 0.01 * mZoomLevel) {
                if(isCancelled()) {
                    return null;
                }

                try {
                    mSolver.mSymbols.define("X", x);
                    double y = mSolver.mSymbols.eval(equation);
                    series.add(new Point(x, y));
                } catch(SyntaxException e) {}
            }
            mSolver.mSymbols.popFrame();

            return series;
        }

        @Override
        protected void onPostExecute(List<Point> result) {
            mListener.onGraphUpdated(result);
        }
    }

    public static interface OnGraphUpdatedListener {
        public void onGraphUpdated(List<Point> result);
    }
}
