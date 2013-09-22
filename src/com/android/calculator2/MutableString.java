package com.android.calculator2;

public class MutableString {
    private String mText;

    public MutableString() {}

    public MutableString(String text) {
        this.mText = text;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public int length() {
        return mText.length();
    }

    public boolean isEmpty() {
        return mText.isEmpty();
    }

    public String substring(int start) {
        return mText.substring(start);
    }

    public String substring(int start, int end) {
        return mText.substring(start, end);
    }

    public CharSequence subSequence(int start, int end) {
        return mText.subSequence(start, end);
    }

    public boolean startsWith(String prefix) {
        return mText.startsWith(prefix);
    }
}
