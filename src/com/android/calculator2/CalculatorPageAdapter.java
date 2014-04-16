/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.android.calculator2.BaseModule.Mode;

public abstract class CalculatorPageAdapter extends PagerAdapter {
    @Override
    public void startUpdate(View container) {
        // Do nothing here
    }

    public abstract View getViewAt(int position);

    public abstract View getViewAtDontDetach(int position);

    @Override
    public Object instantiateItem(View container, int position) {
        View v = getViewAt(position);
        ((ViewGroup) container).addView(v);

        return v;
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        ((ViewGroup) container).removeView((View) object);
    }

    @Override
    public void finishUpdate(View container) {
        // Do nothing here
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        // Do nothing here
    }

    protected void applyBannedResourcesByPage(Logic logic, View page, Mode baseMode) {
        // Enable
        for (Mode key : logic.getBaseModule().mBannedResources.keySet()) {
            if (baseMode.compareTo(key) != 0) {
                List<Integer> resources = logic.getBaseModule().mBannedResources.get(key);
                for (Integer resource : resources) {
                    final int resId = resource.intValue();
                    View v = page.findViewById(resId);
                    if (v != null) {
                        v.setEnabled(true);
                    }
                }
            }
        }

        // Disable
        List<Integer> resources = logic.getBaseModule().mBannedResources.get(baseMode);
        for (Integer resource : resources) {
            final int resId = resource.intValue();
            View v = page.findViewById(resId);
            if (v != null) {
                v.setEnabled(false);
            }
        }
    }

    public abstract Iterable<View> getViewIterator();

    public abstract List<Page> getPages();
}
