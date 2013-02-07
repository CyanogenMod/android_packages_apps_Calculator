package com.android.calculator2;

public class MutableString {
    private String text;

    public MutableString() {

    }

    public MutableString(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int length() {
        return text.length();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public String substring(int start) {
        return text.substring(start);
    }

    public String substring(int start, int end) {
        return text.substring(start, end);
    }

    public CharSequence subSequence(int start, int end) {
        return text.subSequence(start, end);
    }

    public boolean startsWith(String prefix) {
        return text.startsWith(prefix);
    }
}
