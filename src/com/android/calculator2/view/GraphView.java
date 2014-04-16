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

package com.android.calculator2.view;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class GraphView extends View {
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    DecimalFormat mFormat = new DecimalFormat("#.#####");
    private PanListener mPanListener;
    private ZoomListener mZoomListener;
    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private Paint mAxisPaint;
    private Paint mGraphPaint;
    private int mOffsetX;
    private int mOffsetY;
    private int mLineMargin;
    private int mMinLineMargin;
    private int mTextPaintSize;
    private float mZoomLevel = 1;
    private LinkedList<Point> mData;
    private float mStartX;
    private float mStartY;
    private int mDragOffsetX;
    private int mDragOffsetY;
    private int mDragRemainderX;
    private int mDragRemainderY;
    private double mZoomInitDistance;
    private float mZoomInitLevel;
    private int mMode;
    private int mPointers;

    public GraphView(Context context) {
        super(context);
        setup();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.WHITE);
        mBackgroundPaint.setStyle(Style.FILL);

        mTextPaintSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16,
                getResources().getDisplayMetrics());

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(mTextPaintSize);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(Color.DKGRAY);
        mAxisPaint.setStyle(Style.STROKE);
        mAxisPaint.setStrokeWidth(2);

        mGraphPaint = new Paint();
        mGraphPaint.setColor(Color.CYAN);
        mGraphPaint.setStyle(Style.STROKE);
        mGraphPaint.setStrokeWidth(3);

        zoomReset();

        mData = new LinkedList<Point>();
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        mLineMargin = mMinLineMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

        // Center the offsets
        mOffsetX += (xOld / mLineMargin) / 2;
        mOffsetY += (yOld / mLineMargin) / 2;
        mOffsetX -= (xNew / mLineMargin) / 2;
        mOffsetY -= (yNew / mLineMargin) / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawPaint(mBackgroundPaint);

        // Draw the grid lines
        Rect bounds = new Rect();
        int previousLine = 0;
        for (int i = 1, j = mOffsetX; i * mLineMargin < getWidth(); i++, j++) {
            // Draw vertical lines
            int x = i * mLineMargin + mDragRemainderX;
            if (x < mLineMargin || x - previousLine < mMinLineMargin) {
                continue;
            }

            previousLine = x;

            if (j == 0) {
                mAxisPaint.setStrokeWidth(6);
            } else {
                mAxisPaint.setStrokeWidth(2);
            }

            canvas.drawLine(x, mLineMargin, x, getHeight(), mAxisPaint);

            // Draw label on left
            String text = mFormat.format(j * mZoomLevel);
            int textLength = ((text.startsWith("-") ? text.length() - 1 : text.length()) + 1) / 2;
            mTextPaint.setTextSize(mTextPaintSize / textLength);
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            int textWidth = bounds.right - bounds.left;
            canvas.drawText(text, x - textWidth / 2,
                    mLineMargin / 2 + mTextPaint.getTextSize() / 2, mTextPaint);
        }

        previousLine = 0;
        for (int i = 1, j = mOffsetY; i * mLineMargin < getHeight(); i++, j++) {
            // Draw horizontal lines
            int y = i * mLineMargin + mDragRemainderY;
            if (y < mLineMargin || y - previousLine < mMinLineMargin) {
                continue;
            }

            previousLine = y;

            if (j == 0) {
                mAxisPaint.setStrokeWidth(6);
            } else {
                mAxisPaint.setStrokeWidth(2);
            }

            canvas.drawLine(mLineMargin, y, getWidth(), y, mAxisPaint);

            // Draw label on left
            String text = mFormat.format(-j * mZoomLevel);
            int textLength = ((text.startsWith("-") ? text.length() - 1 : text.length()) + 1) / 2;
            mTextPaint.setTextSize(mTextPaintSize / textLength);
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            int textHeight = bounds.bottom - bounds.top;
            int textWidth = bounds.right - bounds.left;
            canvas.drawText(text, mLineMargin / 2 - textWidth / 2, y + textHeight / 2, mTextPaint);
        }

        // Restrict drawing the graph to the grid
        canvas.clipRect(mLineMargin, mLineMargin, getWidth(), getHeight());

        // Create a path to draw smooth arcs
        LinkedList<Point> data = new LinkedList<Point>(mData);
        if (data.size() != 0) {
            drawWithStraightLines(data, canvas);
        }
    }

    private void drawInArc(LinkedList<Point> data, Canvas canvas) {
        Path path = new Path();

        if (data.size() > 1) {
            for (int i = data.size() - 2; i < data.size(); i++) {
                if (i >= 0) {
                    Point point = data.get(i);

                    if (i == 0) {
                        Point next = data.get(i + 1);
                        point.dx = ((getRawX(next) - getRawX(point)) / 3);
                        point.dy = ((getRawY(next) - getRawY(point)) / 3);
                    } else if (i == data.size() - 1) {
                        Point prev = data.get(i - 1);
                        point.dx = ((getRawX(point) - getRawX(prev)) / 3);
                        point.dy = ((getRawY(point) - getRawY(prev)) / 3);
                    } else {
                        Point next = data.get(i + 1);
                        Point prev = data.get(i - 1);
                        point.dx = ((getRawX(next) - getRawX(prev)) / 3);
                        point.dy = ((getRawY(next) - getRawY(prev)) / 3);
                    }
                }
            }
        }

        boolean first = true;
        for (int i = 0; i < data.size(); i++) {
            Point point = data.get(i);

            if (first) {
                first = false;
                path.moveTo(getRawX(point), getRawY(point));
            } else {
                Point prev = data.get(i - 1);
                path.cubicTo(getRawX(prev) + prev.dx, getRawY(prev) + prev.dy, getRawX(point)
                        - point.dx, getRawY(point) - point.dy, getRawX(point), getRawY(point));
            }
        }

        canvas.drawPath(path, mGraphPaint);
    }

    private void drawWithCurvedLines(LinkedList<Point> data, Canvas canvas) {
        Path path = new Path();
        path.moveTo(getRawX(data.get(0)), getRawY(data.get(0)));

        final int n = 6;
        for (int i = 1; i < data.size() - n; i += n / 2) {
            Point a = data.get(i);
            Point b = data.get(i + 1);
            Point c = data.get(i + 2);
            int aX = getRawX(a);
            int aY = getRawY(a);
            int bX = getRawX(b);
            int bY = getRawY(b);
            int cX = getRawX(c);
            int cY = getRawY(c);

            if (tooFar(aX, aY, bX, bY)) {
                canvas.drawPath(path, mGraphPaint);
                path = new Path();
                path.moveTo(cX, cY);
                i++;

                continue;
            }

            float xc = (aX + bX) / 2;
            float yc = (aY + bY) / 2;
            path.quadTo(aX, aY, xc, yc);
        }

        canvas.drawPath(path, mGraphPaint);
    }

    private void drawWithStraightLines(LinkedList<Point> data, Canvas canvas) {
        Point previousPoint = data.remove();
        for (Point currentPoint : data) {
            int aX = getRawX(previousPoint);
            int aY = getRawY(previousPoint);
            int bX = getRawX(currentPoint);
            int bY = getRawY(currentPoint);

            previousPoint = currentPoint;

            if (aX == -1 || aY == -1 || bX == -1 || bY == -1 || tooFar(aX, aY, bX, bY)) {
                continue;
            }

            canvas.drawLine(aX, aY, bX, bY, mGraphPaint);
        }
    }

    private Point average(Point... args) {
        float x = 0;
        float y = 0;

        for (Point p : args) {
            x += p.getX();
            y += p.getY();
        }

        return new Point(x / args.length, y / args.length);
    }

    private boolean tooFar(float aX, float aY, float bX, float bY) {
        boolean outOfBounds = aX == -1 || aY == -1 || bX == -1 || bY == -1;
        if (outOfBounds) {
            return true;
        }

        boolean horzAsymptote = (aX > getXAxisMax() && bX < getXAxisMin())
                || (aX < getXAxisMin() && bX > getXAxisMax());
        boolean vertAsymptote = (aY > getYAxisMax() && bY < getYAxisMin())
                || (aY < getYAxisMin() && bY > getYAxisMax());
        return horzAsymptote || vertAsymptote;
    }

    private int getRawX(Point p) {
        if (p == null || Double.isNaN(p.getX()) || Double.isInfinite(p.getX())) {
            return -1;
        }

        // The left line is at pos
        float leftLine = mLineMargin + mDragRemainderX;
        // And equals
        float val = mOffsetX * mZoomLevel;
        // And changes at a rate of
        float slope = mLineMargin / mZoomLevel;
        // Put it all together
        int pos = (int) (slope * (p.getX() - val) + leftLine);

        return pos;
    }

    private int getRawY(Point p) {
        if (p == null || Double.isNaN(p.getY()) || Double.isInfinite(p.getY())) {
            return -1;
        }

        // The top line is at pos
        float topLine = mLineMargin + mDragRemainderY;
        // And equals
        float val = -mOffsetY * mZoomLevel;
        // And changes at a rate of
        float slope = mLineMargin / mZoomLevel;
        // Put it all together
        int pos = (int) (-slope * (p.getY() - val) + topLine);

        return pos;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Update mode if pointer count changes
        if(mPointers != event.getPointerCount()) {
            setMode(event);
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setMode(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMode == DRAG) {
                    mOffsetX += mDragOffsetX;
                    mOffsetY += mDragOffsetY;
                    mDragOffsetX = (int) (event.getX() - mStartX) / mLineMargin;
                    mDragOffsetY = (int) (event.getY() - mStartY) / mLineMargin;
                    mDragRemainderX = (int) (event.getX() - mStartX) % mLineMargin;
                    mDragRemainderY = (int) (event.getY() - mStartY) % mLineMargin;
                    mOffsetX -= mDragOffsetX;
                    mOffsetY -= mDragOffsetY;
                    if (mPanListener != null) {
                        mPanListener.panApplied();
                    }
                } else if (mMode == ZOOM) {
                    double distance = getDistance(new Point(event.getX(0), event.getY(0)),
                            new Point(event.getX(1), event.getY(1)));
                    double delta = mZoomInitDistance - distance;
                    float zoom = (float) (delta / mZoomInitDistance);
                    setZoomLevel(mZoomInitLevel + zoom);
                }
                break;
        }

        invalidate();
        return true;
    }

    private void setMode(MotionEvent e) {
        mPointers = e.getPointerCount();
        switch(e.getPointerCount()) {
            case 1:
                // Drag
                setMode(DRAG, e);
                break;
            case 2:
                // Zoom
                setMode(ZOOM, e);
                break;
        }
    }

    private void setMode(int mode, MotionEvent e) {
        mMode = mode;
        switch(mode) {
            case DRAG:
                mStartX = e.getX();
                mStartY = e.getY();
                mDragOffsetX = 0;
                mDragOffsetY = 0;
                break;
            case ZOOM:
                mZoomInitDistance = getDistance(new Point(e.getX(0), e.getY(0)),
                        new Point(e.getX(1), e.getY(1)));
                mZoomInitLevel = mZoomLevel;
                break;
        }
    }

    public float getZoomLevel() {
        return mZoomLevel;
    }

    public void setZoomLevel(float level) {
        mZoomLevel = level;
        invalidate();

        if (mZoomListener != null) {
            mZoomListener.zoomApplied(mZoomLevel);
        }
    }

    public void zoomIn() {
        setZoomLevel(mZoomLevel / 2);
    }

    public void zoomOut() {
        setZoomLevel(mZoomLevel * 2);
    }

    public void zoomReset() {
        setZoomLevel(1);
        mDragRemainderX = mDragRemainderY = mOffsetX = mOffsetY = 0;
        onSizeChanged(getWidth(), getHeight(), 0, 0);
        invalidate();

        if (mPanListener != null) {
            mPanListener.panApplied();
        }
        if (mZoomListener != null) {
            mZoomListener.zoomApplied(mZoomLevel);
        }
    }

    public float getXAxisMin() {
        return mOffsetX * mZoomLevel;
    }

    public float getXAxisMax() {
        int num = mOffsetX;
        for (int i = 1; i * mLineMargin < getWidth(); i++) {
            num++;
        }

        return num * mZoomLevel;
    }

    public float getYAxisMin() {
        return mOffsetY * mZoomLevel;
    }

    public float getYAxisMax() {
        int num = mOffsetY;
        for (int i = 1; i * mLineMargin < getHeight(); i++) {
            num++;
        }

        return num * mZoomLevel;
    }

    public void setData(List<Point> data) {
        setData(data, true);
    }

    public void setData(List<Point> data, boolean sort) {
        if (sort) {
            mData = sort(new ArrayList<Point>(data));
        } else {
            mData = new LinkedList<Point>(data);
        }
    }

    private LinkedList<Point> sort(List<Point> data) {
        LinkedList<Point> sorted = new LinkedList<Point>();
        Point key = null;
        while (!data.isEmpty()) {
            if (key == null) {
                key = data.get(0);
                data.remove(0);
                sorted.add(key);
            }

            key = findClosestPoint(key, data);
            data.remove(key);
            sorted.add(key);
        }

        return sorted;
    }

    private Point findClosestPoint(Point key, List<Point> data) {
        Point closestPoint = null;
        for (Point p : data) {
            if (closestPoint == null) {
                closestPoint = p;
            }

            if (getDistance(key, p) < getDistance(key, closestPoint)) {
                closestPoint = p;
            }
        }

        return closestPoint;
    }

    private double getDistance(Point a, Point b) {
        return Math.sqrt(square(a.getX() - b.getX()) + square(a.getY() - b.getY()));
    }

    private double square(double val) {
        return val * val;
    }

    public void setGridColor(int color) {
        mAxisPaint.setColor(color);
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
    }

    public void setGraphColor(int color) {
        mGraphPaint.setColor(color);
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundPaint.setColor(color);
    }

    public PanListener getPanListener() {
        return mPanListener;
    }

    public void setPanListener(PanListener l) {
        mPanListener = l;
    }

    public ZoomListener getZoomListener() {
        return mZoomListener;
    }

    public void setZoomListener(ZoomListener l) {
        mZoomListener = l;
    }

    public static interface PanListener {
        public void panApplied();
    }

    public static interface ZoomListener {
        public void zoomApplied(float level);
    }

    public static class Point {
        float dx, dy;
        private double mX;
        private double mY;

        public Point() {}

        public Point(double x, double y) {
            mX = x;
            mY = y;
        }

        public float getX() {
            return (float) mX;
        }

        public void setX(float x) {
            mX = x;
        }

        public float getY() {
            return (float) mY;
        }

        public void setY(float y) {
            mY = y;
        }
    }
}
