package com.android2.calculator3.view;

import java.text.DecimalFormat;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GraphView extends View {
    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private Paint mAxisPaint;
    private Paint mGraphPaint;
    private int mOffsetX;
    private int mOffsetY;
    private int mLineMargin;
    private int mMinLineMargin;
    private float mZoomLevel = 1;
    DecimalFormat mFormat = new DecimalFormat("#.#");
    private LinkedList<Point> mData;

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

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(48);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(Color.DKGRAY);
        mAxisPaint.setStyle(Style.STROKE);
        mAxisPaint.setStrokeWidth(2);

        mGraphPaint = new Paint();
        mGraphPaint.setColor(Color.CYAN);
        mGraphPaint.setStyle(Style.STROKE);
        mGraphPaint.setStrokeWidth(6);

        zoomReset();

        mData = new LinkedList<Point>();
        mData.add(new Point(0, 0));
        mData.add(new Point(1, 1));
        mData.add(new Point(2, 2));
        mData.add(new Point(2, 3));
        mData.add(new Point(3, 0));
        mData.add(new Point(7, 5));
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        // Center the offsets
        int i = 0;
        while(i * mLineMargin < xOld) {
            i++;
        }
        i--;
        mOffsetX += i / 2;
        i = 0;
        while(i * mLineMargin < yOld) {
            i++;
        }
        i--;
        mOffsetY += i / 2;
        while(i * mLineMargin < xNew) {
            i++;
        }
        i--;
        mOffsetX -= i / 2;
        i = 0;
        while(i * mLineMargin < yNew) {
            i++;
        }
        i--;
        mOffsetY -= i / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawPaint(mBackgroundPaint);

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
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            int textHeight = bounds.bottom - bounds.top;
            int textWidth = bounds.right - bounds.left;
            canvas.drawText(text, mLineMargin / 2 - textWidth / 2, y + textHeight / 2, mTextPaint);
        }

        LinkedList<Point> data = new LinkedList<Point>(mData);
        Point prev = data.remove();
        for(Point p : data) {
            int prevX = getRawX(prev);
            int prevY = getRawY(prev);
            int pX = getRawX(p);
            int pY = getRawY(p);

            prev = p;

            if(prevX == -1 || prevY == -1 || pX == -1 || pY == -1) continue;

            canvas.drawLine(prevX, prevY, pX, pY, mGraphPaint);
        }
    }

    private int getRawX(Point p) {
        for(int i = 1, j = mOffsetX; i * mLineMargin < getWidth(); i++, j++) {
            if(p.getX() >= j * mZoomLevel && p.getX() < j * mZoomLevel + 1 * mZoomLevel) {
                // Point is close
                int decimal = (int) (mLineMargin * (p.getX() - j * mZoomLevel));
                int pos = i * mLineMargin + mDragRemainderX + decimal;

                if(pos < mLineMargin) return -1;
                else return pos;
            }
        }
        return -1;
    }

    private int getRawY(Point p) {
        for(int i = 1, j = -mOffsetY; i * mLineMargin < getHeight(); i++, j--) {
            if(p.getY() >= j * mZoomLevel && p.getY() < j * mZoomLevel + 1 * mZoomLevel) {
                // Point is close
                int decimal = (int) (mLineMargin * (p.getY() - j * mZoomLevel));
                int pos = i * mLineMargin + mDragRemainderY + decimal;

                if(pos < mLineMargin) return -1;
                else return pos;
            }
        }
        return -1;
    }

    private float mStartX;
    private float mStartY;
    private int mDragOffsetX;
    private int mDragOffsetY;
    private int mDragRemainderX;
    private int mDragRemainderY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mStartX = event.getX();
            mStartY = event.getY();
            mDragOffsetX = 0;
            mDragOffsetY = 0;
            break;
        case MotionEvent.ACTION_UP:
            break;
        case MotionEvent.ACTION_MOVE:
            mOffsetX += mDragOffsetX;
            mOffsetY += mDragOffsetY;
            mDragOffsetX = (int) (event.getX() - mStartX) / mLineMargin;
            mDragOffsetY = (int) (event.getY() - mStartY) / mLineMargin;
            mDragRemainderX = (int) (event.getX() - mStartX) % mLineMargin;
            mDragRemainderY = (int) (event.getY() - mStartY) % mLineMargin;
            mOffsetX -= mDragOffsetX;
            mOffsetY -= mDragOffsetY;
            break;
        }
        invalidate();
        return true;
    }

    public void zoomIn() {
        mZoomLevel /= 2;
        invalidate();
    }

    public void zoomOut() {
        mZoomLevel *= 2;
        invalidate();
    }

    public void zoomReset() {
        mZoomLevel = 1;
        mLineMargin = 70;
        mMinLineMargin = 70;
        int i = 0;
        while(i * mLineMargin < getWidth()) {
            i++;
        }
        i--;
        mOffsetX = -i / 2;
        i = 0;
        while(i * mLineMargin < getHeight()) {
            i++;
        }
        i--;
        mOffsetY = -i / 2;
        invalidate();
    }

    public static class Point {
        private float mX;
        private float mY;

        public Point() {}

        public Point(float x, float y) {
            mX = x;
            mY = y;
        }

        public float getX() {
            return mX;
        }

        public void setX(float x) {
            mX = x;
        }

        public float getY() {
            return mY;
        }

        public void setY(float y) {
            mY = y;
        }
    }
}
