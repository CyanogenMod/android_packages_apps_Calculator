package com.android.calculator2;

import com.xlythe.math.Base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of the application wide number base, and manages the IDs of views to disable
 * when changing base.
 */
public class NumberBaseManager {

    private Base mBase;
    private Map<Base, Set<Integer>> mDisabledViewIds;
    private Set<Integer> mBasicViewIds;
    private Set<Integer> mHexViewIds;

    public NumberBaseManager(Base base) {
        mBase = base;

        List<Integer> hexList =
                Arrays.asList(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F);

        List<Integer> binaryList =
                Arrays.asList(R.id.digit_2, R.id.digit_3, R.id.digit_4, R.id.digit_5, R.id.digit_6,
                        R.id.digit_7, R.id.digit_8, R.id.digit_9);

        mDisabledViewIds = new HashMap<Base, Set<Integer>>();
        mDisabledViewIds.put(Base.DECIMAL, new HashSet<Integer>(hexList));
        Set<Integer> disabledForBinary = new HashSet<Integer>(binaryList);
        disabledForBinary.addAll(hexList);
        mDisabledViewIds.put(Base.BINARY, disabledForBinary);
        mDisabledViewIds.put(Base.HEXADECIMAL, new HashSet<Integer>());

        mBasicViewIds = new HashSet<Integer>();
        mBasicViewIds.addAll(binaryList);

        mHexViewIds = new HashSet<Integer>();
        mHexViewIds.addAll(hexList);

        // setup default base
        setNumberBase(mBase);
    }

    public void setNumberBase(Base base) {
        mBase = base;
    }

    public Base getNumberBase() {
        return mBase;
    }

    /**
     * @return the set of view resource IDs managed by the enabled/disabled list
     */
    public Set<Integer> getViewIds(int page) {
        if(page == -1) {
            HashSet<Integer> set = new HashSet<Integer>();
            set.addAll(mBasicViewIds);
            set.addAll(mHexViewIds);
            return set;
        }
        else if(page == 0) {
            return mBasicViewIds;
        }
        else {
            return mHexViewIds;
        }
    }

    /**
     * return true if the given view is disabled based on the current base
     *
     * @param viewResId
     * @return
     */
    public boolean isViewDisabled(int viewResId) {
        Set<Integer> disabledSet = mDisabledViewIds.get(mBase);
        return disabledSet.contains(viewResId);
    }
}
