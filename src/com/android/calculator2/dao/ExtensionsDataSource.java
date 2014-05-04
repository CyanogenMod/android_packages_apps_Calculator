package com.android.calculator2.dao;

import android.content.Context;

public class ExtensionsDataSource extends AppDataSource {

	public ExtensionsDataSource(Context context) {
		super(context);
	}

	@Override
	public String getTableName() {
		return StoreHelper.TABLE_EXTENSIONS;
	}

}
