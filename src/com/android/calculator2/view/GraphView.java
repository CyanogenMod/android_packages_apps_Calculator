package com.android.calculator2.view;

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

import com.android.calculator2.R;

import com.xlythe.math.Point;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class GraphView extends View {
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int LINES = 1;
    private int mDrawingAlgorithm = LINES;
    private static final int DOTS = 2;
    private static final int BOX_STROKE = 6;
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
    private List<Point> mData;
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

    private void setup() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(R.color.graph_background_color);
        mBackgroundPaint.setStyle(Style.FILL);

        mTextPaintSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        mTextPaint = new Paint();
        mTextPaint.setColor(R.color.graph_labels_color);
        mTextPaint.setTextSize(mTextPaintSize);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(R.color.graph_grid_color);
        mAxisPaint.setStyle(Style.STROKE);
        mAxisPaint.setStrokeWidth(2);

        mGraphPaint = new Paint();
        mGraphPaint.setColor(R.color.graph_line_color);
        mGraphPaint.setStyle(Style.STROKE);
        mGraphPaint.setStrokeWidth(6);

        zoomReset();

        mData = new ArrayList<Point>();
    }

    public void zoomReset() {
        setZoomLevel(1);
        mDragRemainderX = mDragRemainderY = mOffsetX = mOffsetY = 0;
        onSizeChanged(getWidth(), getHeight(), 0, 0);
        invalidate();
        if(mPanListener != null) mPanListener.panApplied();
        if(mZoomListener != null) mZoomListener.zoomApplied(mZoomLevel);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private Point average(Point... args) {
        float x = 0;
        float y = 0;
        for(Point p : args) {
            x += p.getX();
            y += p.getY();
        }
        return new Point(x / args.length, y / args.length);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Update mode if pointer count changes
        if(mPointers != event.getPointerCount()) {
            setMode(event);
        }
        ;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setMode(event);
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                if(mMode == DRAG) {
                    mOffsetX += mDragOffsetX;
                    mOffsetY += mDragOffsetY;
                    mDragOffsetX = (int) (event.getX() - mStartX) / mLineMargin;
                    mDragOffsetY = (int) (event.getY() - mStartY) / mLineMargin;
                    mDragRemainderX = (int) (event.getX() - mStartX) % mLineMargin;
                    mDragRemainderY = (int) (event.getY() - mStartY) % mLineMargin;
                    mOffsetX -= mDragOffsetX;
                    mOffsetY -= mDragOffsetY;
                    if(mPanListener != null) mPanListener.panApplied();
                } else if(mMode == ZOOM) {
                    double distance = getDistance(new Point(event.getX(0), event.getY(0)), new Point(event.getX(1), event.getY(1)));
                    double delta = mZoomInitDistance - distance;
                    float zoom = (float) (delta / mZoomInitDistance);
                    setZoomLevel(mZoomInitLevel + zoom);
                }
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        mLineMargin = mMinLineMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
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

        // draw bounding box
        mAxisPaint.setStrokeWidth(BOX_STROKE);
        canvas.drawRect(mLineMargin, mLineMargin,
                getWidth() - BOX_STROKE/2, getHeight() - BOX_STROKE/2, mAxisPaint);

        // Draw the grid lines
        Rect bounds = new Rect();
        int previousLine = 0;
        for(int i = 1, j = mOffsetX; i * mLineMargin < getWidth(); i++, j++) {
            // Draw vertical lines
            int x = i * mLineMargin + mDragRemainderX;
            if(x < mLineMargin || x - previousLine < mMinLineMargin) continue;
            previousLine = x;

            if(j == 0) mAxisPaint.setStrokeWidth(6);
            else mAxisPaint.setStrokeWidth(2);
            canvas.drawLine(x, mLineMargin, x, getHeight(), mAxisPaint);

            // Draw label on left
            String text = mFormat.format(j * mZoomLevel);
            int textLength = ((text.startsWith("-") ? text.length() - 1 : text.length()) + 1) / 2;
            mTextPaint.setTextSize(mTextPaintSize / textLength);
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            int textWidth = bounds.right - bounds.left;
            canvas.drawText(text, x - textWidth / 2, mLineMargin / 2 + mTextPaint.getTextSize() / 2, mTextPaint);
        }
        previousLine = 0;
        for(int i = 1, j = mOffsetY; i * mLineMargin < getHeight(); i++, j++) {
            // Draw horizontal lines
            int y = i * mLineMargin + mDragRemainderY;
            if(y < mLineMargin || y - previousLine < mMinLineMargin) continue;
            previousLine = y;

            if(j == 0) mAxisPaint.setStrokeWidth(6);
            else mAxisPaint.setStrokeWidth(2);
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
        canvas.clipRect(mLineMargin, mLineMargin,
                getWidth() - BOX_STROKE, getHeight() - BOX_STROKE);

        // Create a path to draw smooth arcs
        if(mDrawingAlgorithm == LINES) {
            if(mData.size() != 0) {
                drawWithStraightLines(mData, canvas);
            }
        } else if(mDrawingAlgorithm == DOTS) {
            drawDots(mData, canvas);
        }
    }

    private void drawWithStraightLines(List<Point> data, Canvas canvas) {
        Point previousPoint = null;
        for(Point currentPoint : data) {
            if (previousPoint == null) {
                previousPoint = currentPoint;
                continue;
            }

            int aX = getRawX(previousPoint);
            int aY = getRawY(previousPoint);
            int bX = getRawX(currentPoint);
            int bY = getRawY(currentPoint);

            previousPoint = currentPoint;

            if(aX == -1 || aY == -1 || bX == -1 || bY == -1 || tooFar(aX, aY, bX, bY)) continue;

            canvas.drawLine(aX, aY, bX, bY, mGraphPaint);
        }
    }

    private void drawDots(List<Point> data, Canvas canvas) {
        for(Point p : data) {
            canvas.drawPoint(getRawX(p), getRawY(p), mGraphPaint);
        }
    }

    private int getRawX(Point p) {
        if(p == null || Double.isNaN(p.getX()) || Double.isInfinite(p.getX())) return -1;

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
        if(p == null || Double.isNaN(p.getY()) || Double.isInfinite(p.getY())) return -1;

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

    private boolean tooFar(float aX, float aY, float bX, float bY) {
        boolean outOfBounds = aX == -1 || aY == -1 || bX == -1 || bY == -1;
        if(outOfBounds) return true;

        boolean horzAsymptote = (aX > getXAxisMax() && bX < getXAxisMin()) || (aX < getXAxisMin() && bX > getXAxisMax());
        boolean vertAsymptote = (aY > getYAxisMax() && bY < getYAxisMin()) || (aY < getYAxisMin() && bY > getYAxisMax());
        return horzAsymptote || vertAsymptote;
    }

    public float getXAxisMin() {
        return mOffsetX * mZoomLevel;
    }

    public float getXAxisMax() {
        int num = mOffsetX;
        for(int i = 1; i * mLineMargin < getWidth(); i++, num++) ;
        return num * mZoomLevel;
    }

    public float getYAxisMin() {
        return mOffsetY * mZoomLevel;
    }

    public float getYAxisMax() {
        int num = mOffsetY;
        for(int i = 1; i * mLineMargin < getHeight(); i++, num++) ;
        return num * mZoomLevel;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundPaint.setColor(color);
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
                mZoomInitDistance = getDistance(new Point(e.getX(0), e.getY(0)), new Point(e.getX(1), e.getY(1)));
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
        if(mZoomListener != null) mZoomListener.zoomApplied(mZoomLevel);
    }

    public void zoomIn() {
        setZoomLevel(mZoomLevel / 2);
    }

    public void zoomOut() {
        setZoomLevel(mZoomLevel * 2);
    }

    public void setData(List<Point> data) {
        mData = data;
        mDrawingAlgorithm = LINES;
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
}
