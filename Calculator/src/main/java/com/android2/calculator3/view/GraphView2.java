package com.android2.calculator3.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.android2.calculator3.Logic;

import org.javia.arity.SyntaxException;

import java.text.DecimalFormat;

public class GraphView2 extends View {
    private static final DecimalFormat mFormat = new DecimalFormat("#.#");

    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private Paint mAxisPaint;
    private Paint mGraphPaint;
    private int mOffsetX;
    private int mOffsetY;
    private int mLineMargin;
    private int mMinLineMargin;
    private float mZoomLevel = 1;
    private Rect mAxisRect;
    private Rect mGraphRect;
    private String mEquation = "Y=X";
    private Logic mLogic;

    public GraphView2(Context context) {
        super(context);
        setup();
    }

    public GraphView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public GraphView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.WHITE);
        mBackgroundPaint.setStyle(Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));

        mAxisPaint = new Paint();
        mAxisPaint.setColor(Color.DKGRAY);
        mAxisPaint.setStyle(Style.STROKE);
        mAxisPaint.setStrokeWidth(2);
        mAxisRect = new Rect();

        mGraphPaint = new Paint();
        mGraphPaint.setColor(Color.CYAN);
        mGraphPaint.setStyle(Style.STROKE);
        mGraphPaint.setStrokeWidth(6);
        mGraphRect = new Rect();

        mLogic = new Logic(getContext());

        zoomReset();
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
            mTextPaint.getTextBounds(text, 0, text.length(), mAxisRect);
            int textWidth = mAxisRect.right - mAxisRect.left;
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
            mTextPaint.getTextBounds(text, 0, text.length(), mAxisRect);
            int textHeight = mAxisRect.bottom - mAxisRect.top;
            int textWidth = mAxisRect.right - mAxisRect.left;
            canvas.drawText(text, mLineMargin / 2 - textWidth / 2, y + textHeight / 2, mTextPaint);
        }

        // TODO graph from equation text
        int size = mLineMargin/2;
        for(int i = mDragRemainderY; i < getHeight(); i+=size) {
            if(i < mLineMargin) continue;
            for(int j = mDragRemainderX; j < getWidth(); j+=size) {
                if(j < mLineMargin) continue;
                mGraphRect.top = i;
                mGraphRect.bottom = (i+size);
                mGraphRect.left = j;
                mGraphRect.right = (j+size);
                if(rectContainsEquation(mGraphRect)) canvas.drawRect(mGraphRect, mGraphPaint);
            }
        }
    }

    private boolean rectContainsEquation(Rect r) {
        final String[] equation = mEquation.split("=");
        try {
            mLogic.mSymbols.define(mLogic.mX, r.left);
            mLogic.mSymbols.define(mLogic.mY, r.top);
            double left = mLogic.mSymbols.eval(equation[0]);
        } catch (SyntaxException e) {}
        return true;
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
        mLineMargin = mMinLineMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
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

    private double square(double val) {
        return val*val;
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
    public void setBackgroundColor(int color) { mBackgroundPaint.setColor(color); }
}
