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

package com.android.calculator2;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.engine.theme.App;

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
