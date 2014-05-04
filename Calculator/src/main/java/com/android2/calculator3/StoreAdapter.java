package com.android2.calculator3;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.engine.theme.App;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StoreAdapter extends BitmapAdapter<App> {
	public StoreAdapter(Context context, List<App> values) {
		super(context, values);
	}

	@Override
	public View inflateView() {
		return View.inflate(getContext(), R.layout.view_list_item_store_theme, null);
	}

	@Override
	public void updateView(View convertView, App object) {
		TextView text1 = (TextView) convertView.findViewById(R.id.text1);
		TextView text2 = (TextView) convertView.findViewById(R.id.text2);
		ImageView image1 = (ImageView) convertView.findViewById(R.id.image1);

		text1.setText(object.getName());
		text2.setText(formatPrice(object));
		grabImage(convertView, image1, object.getImageUrl());
	}

	private String formatPrice(App a) {
		if (App.doesPackageExists(getContext(), a.getPackageName())) {
			return getContext().getString(R.string.store_price_installed);
		}
		if (a.getPrice() == 0) {
			return getContext().getString(R.string.store_price_free);
		}
		NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
		return f.format(a.getPrice());
	}
}
