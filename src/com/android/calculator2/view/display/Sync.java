package com.android.calculator2.view.display;

import android.widget.TextView;

/**
 * Created by Will on 12/13/2014.
 */
public abstract class Sync {
    String tag;

    Sync(String tag) {
        this.tag = tag;
    }

    public abstract void apply(TextView textView);

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Sync) {
            return ((Sync) o).tag.equals(tag);
        }
        return false;
    }
}

