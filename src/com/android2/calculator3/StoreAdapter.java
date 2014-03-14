package com.android2.calculator3;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.engine.theme.App;

public class StoreAdapter extends ArrayAdapter<App> {
    protected static final int textViewResourceId = R.layout.view_list_item_store_theme;

    public StoreAdapter(Context context, List<App> values) {
        super(context, textViewResourceId, values);
    }

    static class ViewHolder {
        TextView text;
        TextView detail;
        ImageView icon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View v;
        if(convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(textViewResourceId, null);
            holder = new ViewHolder();
            holder.text = (TextView) v.findViewById(R.id.text1);
            holder.detail = (TextView) v.findViewById(R.id.text2);
            holder.icon = (ImageView) v.findViewById(R.id.image1);
            v.setTag(holder);
        }
        else {
            v = convertView;
            holder = (ViewHolder) v.getTag();
        }

        final App o = getItem(position);
        holder.text.setText(o == null ? null : o.getName());
        holder.detail.setText(o == null ? null : formatPrice(o));

        return v;
    }

    private String formatPrice(App a) {
        if(App.doesPackageExists(getContext(), a.getPackageName())) {
            return getContext().getString(R.string.store_price_installed);
        }
        if(a.getPrice() == 0) {
            return getContext().getString(R.string.store_price_free);
        }
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
        return f.format(a.getPrice());
    }
}
