package com.android.calculator2.dao;

import android.content.Context;

public class ThemesDataSource extends AppDataSource {

    public ThemesDataSource(Context context) {
        super(context);
    }

    @Override
    public String getTableName() {
        return StoreHelper.TABLE_THEMES;
    }

}
