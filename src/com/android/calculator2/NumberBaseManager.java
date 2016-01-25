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
    private Set<Integer> mViewIds;

    public NumberBaseManager(Base base) {
        mBase = base;

        List<Integer> hexList =
                Arrays.asList(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F);

        List<Integer> binaryList =
                Arrays.asList(R.id.digit2, R.id.digit3, R.id.digit4, R.id.digit5, R.id.digit6,
                        R.id.digit7, R.id.digit8, R.id.digit9);

        mDisabledViewIds = new HashMap<Base, Set<Integer>>();
        mDisabledViewIds.put(Base.DECIMAL, new HashSet<Integer>(hexList));
        Set<Integer> disabledForBinary = new HashSet<Integer>(binaryList);
        disabledForBinary.addAll(hexList);
        mDisabledViewIds.put(Base.BINARY, disabledForBinary);
        mDisabledViewIds.put(Base.HEXADECIMAL, new HashSet<Integer>());

        mViewIds = new HashSet<Integer>();
        mViewIds.addAll(binaryList);
        mViewIds.addAll(hexList);

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
    public Set<Integer> getViewIds() {
        return mViewIds;
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
