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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class TouchInterceptor extends ListView {
    private final Rect mTempRect = new Rect();
    private final int mTouchSlop;
    private final int mItemHeightNormal;
    private final int mItemHeightExpanded;
    private final int mItemHeightHalf;
    private ImageView mDragView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;

    // At which position is the item currently being dragged.
    // Note that this takes in to account header items.
    private int mDragPos;

    // At which position was the item being dragged originally
    private int mSrcDragPos;
    // At what x offset inside the item did the user
    private int mDragPointX;
    // At what y offset inside the item did the user
    private int mDragPointY;
    // The difference between screen coordinates and
    private int mXOffset;
    // The difference between screen coordinates and
    private int mYOffset;

    private DragListener mDragListener;
    private DropListener mDropListener;
    private int mUpperBound;
    private int mLowerBound;
    private int mHeight;
    private GestureDetector mGestureDetector;
    private Bitmap mDragBitmap;

    public TouchInterceptor(Context context) {
        super(context);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mItemHeightNormal = (int) resolveAttribute(getContext(),
                android.R.attr.listPreferredItemHeight);
        mItemHeightHalf = mItemHeightNormal / 2;
        mItemHeightExpanded = mItemHeightNormal * 2;
    }

    public float resolveAttribute(Context context, int attr) {
        TypedValue value = new TypedValue();
        DisplayMetrics metrics = new DisplayMetrics();

        context.getTheme().resolveAttribute(attr, value, true);
        ((WindowManager) (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay()
                .getMetrics(metrics);

        return TypedValue.complexToDimension(value.data, metrics);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mDragListener != null || mDropListener != null) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                int itemnum = pointToPosition(x, y);
                if (itemnum != AdapterView.INVALID_POSITION) {
                    View item = getChildAt(itemnum - getFirstVisiblePosition());
                    mDragPointX = x - item.getLeft();
                    mDragPointY = y - item.getTop();
                    mXOffset = ((int) ev.getRawX()) - x;
                    mYOffset = ((int) ev.getRawY()) - y;

                    // The left side of the item is the grabber for dragging the item
                    // TODO: Delete this but make sure clicks still work
                    if (x < getWidth() / 2) {
                        item.setDrawingCacheEnabled(true);

                        // Create a copy of the drawing cache so that it does not get recycled
                        // by the framework when the list tries to clean up memory
                        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                        startDragging(bitmap, x, y);
                        mDragPos = itemnum;
                        mSrcDragPos = mDragPos;
                        mHeight = getHeight();
                        int touchSlop = mTouchSlop;
                        mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                        mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);
                        return false;
                    }

                    stopDragging();
                }
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    /**
     * pointToPosition() doesn't consider invisible views, but we need to,
     * so implement a slightly different version.
     */
    private int myPointToPosition(int x, int y) {
        if (y < 0) {
            // When dragging off the top of the screen, calculate position
            // by going back from a visible item
            int pos = myPointToPosition(x, y + mItemHeightNormal);
            if (pos > 0) {
                return pos - 1;
            }
        }

        Rect frame = mTempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }

        return INVALID_POSITION;
    }

    private int getItemForPosition(int y) {
        int adjustedy = y - mDragPointY - mItemHeightHalf;
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0) {
            if (pos <= mSrcDragPos) {
                pos += 1;
            }
        } else if (adjustedy < 0) {
            // This shouldn't happen anymore now that myPointToPosition
            // deals with this situation
            pos = 0;
        }

        return pos;
    }

    private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }

    /**
     * Restore size and visibility for all listitems
     */
    private void unExpandViews(boolean deletion) {
        for (int i = 0;; i++) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    // HACK force update of mItemCount
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                }

                try {
                     // Force children to be recreated where
                    layoutChildren();
                    v = getChildAt(i);
                } catch(IllegalStateException ex) {
                    // layoutChildren throws this sometimes, presumably because we're
                    // in the process of being torn down but are still getting touch events
                }

                if (v == null) {
                    return;
                }
            }

            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = mItemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Adjust visibility and size to make it appear as though an item is being dragged around and
     * other items are making room for it: If dropping the item would result in it still being in
     * the same place, then make the dragged listitem's size normal, but make the item invisible.
     * Otherwise, if the dragged listitem is still on screen, make it as small as possible and
     * expand the item below the insert point. If the dragged item is not on screen, only expand the
     * item below the current insertpoint.
     */
    private void doExpansion() {
        int childnum = mDragPos - getFirstVisiblePosition();
        if (mDragPos > mSrcDragPos) {
            childnum++;
        }

        int numheaders = getHeaderViewsCount();

        View first = getChildAt(mSrcDragPos - getFirstVisiblePosition());
        for (int i = 0;; i++) {
            View vv = getChildAt(i);
            if (vv == null) {
                break;
            }

            int height = mItemHeightNormal;
            int visibility = View.VISIBLE;
            if (mDragPos < numheaders && i == numheaders) {
                // dragging on top of the header item, so adjust the
                // item below instead
                if (vv.equals(first)) {
                    visibility = View.INVISIBLE;
                } else {
                    height = mItemHeightExpanded;
                }
            } else if (vv.equals(first)) {
                // Processing the item that is being dragged
                if (mDragPos == mSrcDragPos || getPositionForView(vv) == getCount() - 1) {
                    // Hovering over the original location
                    visibility = View.INVISIBLE;
                } else {
                    // Not hovering over it.
                    // Ideally the item would be completely gone, but neither
                    // setting its size to 0 nor settings visibility to GONE
                    // has the desired effect.
                    height = 1;
                }
            } else if (i == childnum) {
                if (mDragPos >= numheaders && mDragPos < getCount() - 1) {
                    height = mItemHeightExpanded;
                }
            }

            ViewGroup.LayoutParams params = vv.getLayoutParams();
            params.height = height;
            vv.setLayoutParams(params);
            vv.setVisibility(visibility);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }

        if ((mDragListener != null || mDropListener != null) && mDragView != null) {
            int action = ev.getAction();
            switch(action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Rect r = mTempRect;
                    mDragView.getDrawingRect(r);
                    stopDragging();

                    if (mDropListener != null && mDragPos >= 0 && mDragPos < getCount()) {
                        mDropListener.drop(mSrcDragPos, mDragPos);
                    }

                    unExpandViews(false);
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);

                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0) {
                        if (action == MotionEvent.ACTION_DOWN || itemnum != mDragPos) {
                            if (mDragListener != null) {
                                mDragListener.drag(mDragPos, itemnum);
                            }

                            mDragPos = itemnum;
                            doExpansion();
                        }

                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > mLowerBound) {
                            // Scroll the list up a bit
                            if(getLastVisiblePosition() < getCount() - 1) {
                                speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
                            } else {
                                speed = 1;
                            }
                        } else if (y < mUpperBound) {
                            // Scroll the list down a bit
                            speed = y < mUpperBound / 2 ? -16 : -4;
                            if (getFirstVisiblePosition() == 0
                                    && getChildAt(0).getTop() >= getPaddingTop()) {
                                // If we're already at the top, don't try to scroll, because
                                // it causes the framework to do some extra drawing that
                                // messes up our animation
                                speed = 0;
                            }
                        }

                        if(speed != 0) {
                            smoothScrollBy(speed, 30);
                        }
                    }
                    break;
            }

            return true;
        }

        return super.onTouchEvent(ev);
    }

    private void startDragging(Bitmap bm, int x, int y) {
        stopDragging();

        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowParams.x = x - mDragPointX + mXOffset;
        mWindowParams.y = y - mDragPointY + mYOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setPadding(0, 0, 0, 0);
        v.setImageBitmap(bm);
        mDragBitmap = bm;

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    private void dragView(int x, int y) {
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointY + mYOffset;
        mWindowManager.updateViewLayout(mDragView, mWindowParams);
    }

    private void stopDragging() {
        if (mDragView != null) {
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);

            mDragView.setImageDrawable(null);
            mDragView = null;
        }

        if(mDragBitmap != null) {
            mDragBitmap.recycle();
            mDragBitmap = null;
        }
    }

    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public interface DragListener {
        void drag(int from, int to);
    }

    public interface DropListener {
        void drop(int from, int to);
    }
}
