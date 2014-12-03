package com.android2.calculator3.view;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;

/**
 * Created by Will on 11/9/2014.
 */
public class AdvancedEditable implements Editable {
    private String mSource;

    AdvancedEditable(String source) {
        mSource = source;
    }

    @Override
    public Editable replace(int st, int en, CharSequence source, int start, int end) {
        throw new RuntimeException("replace not supported");
    }

    @Override
    public Editable replace(int st, int en, CharSequence text) {
        throw new RuntimeException("replace not supported");
    }

    @Override
    public Editable insert(int where, CharSequence text, int start, int end) {
        throw new RuntimeException("insert not supported");
    }

    @Override
    public Editable insert(int where, CharSequence text) {
        throw new RuntimeException("insert not supported");
    }

    @Override
    public Editable delete(int st, int en) {
        throw new RuntimeException("delete not supported");
    }

    @Override
    public Editable append(CharSequence text) {
        throw new RuntimeException("append not supported");
    }

    @Override
    public Editable append(CharSequence text, int start, int end) {
        throw new RuntimeException("append not supported");
    }

    @Override
    public Editable append(char text) {
        throw new RuntimeException("append not supported");
    }

    @Override
    public void clear() {
        throw new RuntimeException("clear not supported");
    }

    @Override
    public void clearSpans() {
        throw new RuntimeException("clearSpans not supported");
    }

    @Override
    public void setFilters(InputFilter[] filters) {
        throw new RuntimeException("setFilters not supported");
    }

    @Override
    public InputFilter[] getFilters() {
        throw new RuntimeException("getFilters not supported");
    }

    @Override
    public void getChars(int start, int end, char[] dest, int destoff) {
        throw new RuntimeException("getChars not supported");
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        throw new RuntimeException("setSpan not supported");
    }

    @Override
    public void removeSpan(Object what) {
        throw new RuntimeException("removeSpan not supported");
    }

    @Override
    public <T> T[] getSpans(int start, int end, Class<T> type) {
        throw new RuntimeException("getSpans not supported");
    }

    @Override
    public int getSpanStart(Object tag) {
        throw new RuntimeException("getSpanStart not supported");
    }

    @Override
    public int getSpanEnd(Object tag) {
        throw new RuntimeException("getSpanEnd not supported");
    }

    @Override
    public int getSpanFlags(Object tag) {
        throw new RuntimeException("getSpanFlags not supported");
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        throw new RuntimeException("nextSpanTransition not supported");
    }

    @Override
    public int length() {
        return mSource.length();
    }

    @Override
    public char charAt(int index) {
        return mSource.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return mSource.subSequence(start, end);
    }

    @NonNull
    @Override
    public String toString() {
        return mSource;
    }
}
