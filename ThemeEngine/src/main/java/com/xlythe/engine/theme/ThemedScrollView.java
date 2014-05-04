package com.xlythe.engine.theme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ThemedScrollView extends ScrollView {
	public ThemedScrollView(Context context) {
		super(context);
		setup(context, null);
	}

	public ThemedScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context, attrs);
	}

	public ThemedScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context, attrs);
	}

	private void setup(Context context, AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.theme);
			if (a != null) {
				// Get background
				setBackground(Theme.get(a.getResourceId(R.styleable.theme_themeBackground, 0)));

				if ("none".equals(Theme.getString(getContext(), "requiresFadingEdge"))) {
					setVerticalFadingEdgeEnabled(false);
				}

				a.recycle();
			}
		}
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void setBackground(Theme.Res res) {
		if (res != null) {
			if (Theme.COLOR.equals(res.getType())) {
				setBackgroundColor(Theme.getColor(getContext(), res.getName()));
			} else if (Theme.DRAWABLE.equals(res.getType())) {
				if (android.os.Build.VERSION.SDK_INT < 16) {
					setBackgroundDrawable(Theme.getDrawable(getContext(), res.getName()));
				} else {
					setBackground(Theme.getDrawable(getContext(), res.getName()));
				}
			}
		}
	}
}
